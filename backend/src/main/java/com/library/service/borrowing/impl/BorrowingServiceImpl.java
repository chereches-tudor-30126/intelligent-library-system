package com.library.service.borrowing.impl;

import com.library.entity.Book;
import com.library.entity.Borrowing;
import com.library.entity.BorrowingStatus;
import com.library.entity.User;
import com.library.repository.BookRepository;
import com.library.repository.BorrowingRepository;
import com.library.repository.UserRepository;
import com.library.service.audit.AuditLogService;
import com.library.service.borrowing.BorrowingService;
import com.library.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BorrowingServiceImpl implements BorrowingService {

    private final BorrowingRepository borrowingRepository;
    private final BookRepository      bookRepository;
    private final UserRepository      userRepository;
    private final NotificationService notificationService;
    private final AuditLogService     auditLogService;

    // Configurable via application.yml
    @Value("${library.borrow.duration-days:14}")
    private int borrowDurationDays;

    @Value("${library.borrow.max-extensions:2}")
    private int maxExtensions;

    @Value("${library.borrow.extension-days:7}")
    private int extensionDays;

    @Value("${library.borrow.fine-per-day:0.50}")
    private BigDecimal finePerDay;

    @Value("${library.borrow.max-active-borrows:5}")
    private int maxActiveBorrows;

    // -------------------------------------------------------------------------
    // Borrow a book
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public Borrowing borrowBook(UUID userId, UUID bookId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Enforce active borrow limit
        long activeBorrows = borrowingRepository.countByUserId(userId);
        if (activeBorrows >= maxActiveBorrows) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Borrow limit reached (" + maxActiveBorrows + " active borrows)");
        }

        // Prevent duplicate active borrow of the same book
        if (borrowingRepository.existsByUserIdAndBookIdAndStatus(userId, bookId, BorrowingStatus.ACTIVE)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You already have this book borrowed");
        }

        // Atomic decrement — returns 0 if no copies available (race-condition safe)
        int decremented = bookRepository.decrementAvailableCopies(bookId);
        if (decremented == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No available copies for this book");
        }

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));

        // Increment the cumulative borrow counter (for trending/analytics)
        bookRepository.incrementBorrowCount(bookId);

        Borrowing borrowing = Borrowing.builder()
                .user(user)
                .book(book)
                .dueDate(OffsetDateTime.now().plusDays(borrowDurationDays))
                .status(BorrowingStatus.ACTIVE)
                .build();

        Borrowing saved = borrowingRepository.save(borrowing);
        auditLogService.logBorrowCreate(saved);
        notificationService.sendBorrowConfirmation(user, book, saved.getDueDate());

        log.info("Book '{}' borrowed by user '{}', due: {}",
                book.getTitle(), user.getEmail(), saved.getDueDate());
        return saved;
    }

    // -------------------------------------------------------------------------
    // Return a book
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public Borrowing returnBook(UUID borrowingId, String notes) {
        Borrowing borrowing = getById(borrowingId);

        if (borrowing.getStatus() == BorrowingStatus.RETURNED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Book already returned");
        }

        OffsetDateTime now = OffsetDateTime.now();
        borrowing.setReturnedAt(now);
        borrowing.setStatus(BorrowingStatus.RETURNED);
        borrowing.setNotes(notes);

        // Calculate fine if overdue
        if (now.isAfter(borrowing.getDueDate())) {
            long daysLate = ChronoUnit.DAYS.between(borrowing.getDueDate(), now);
            BigDecimal fine = finePerDay.multiply(BigDecimal.valueOf(daysLate));
            borrowing.setFineAmount(fine);
            log.info("Fine applied: {} RON ({} days late) for borrowing {}",
                    fine, daysLate, borrowingId);
        }

        // Restore the available copy count
        bookRepository.incrementAvailableCopies(borrowing.getBook().getId());

        Borrowing saved = borrowingRepository.save(borrowing);
        auditLogService.logBorrowReturn(saved);
        notificationService.sendReturnConfirmation(borrowing.getUser(), borrowing.getBook());

        log.info("Book '{}' returned by user '{}'",
                borrowing.getBook().getTitle(), borrowing.getUser().getEmail());
        return saved;
    }

    // -------------------------------------------------------------------------
    // Extend due date
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public Borrowing extendDueDate(UUID borrowingId) {
        Borrowing borrowing = getById(borrowingId);

        if (borrowing.getStatus() != BorrowingStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only active borrowings can be extended");
        }
        if (borrowing.getExtendedCount() >= maxExtensions) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Maximum extensions (" + maxExtensions + ") reached");
        }

        borrowing.setDueDate(borrowing.getDueDate().plusDays(extensionDays));
        borrowing.setExtendedCount((short) (borrowing.getExtendedCount() + 1));

        Borrowing saved = borrowingRepository.save(borrowing);
        auditLogService.logBorrowExtend(saved);
        notificationService.sendExtensionConfirmation(
                borrowing.getUser(), borrowing.getBook(), saved.getDueDate());

        log.info("Due date extended for borrowing {} → new due: {}", borrowingId, saved.getDueDate());
        return saved;
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public Borrowing getById(UUID id) {
        return borrowingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Borrowing not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Borrowing> getByUser(UUID userId, Pageable pageable) {
        return borrowingRepository.findAllByUserId(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Borrowing> getActiveByUser(UUID userId, Pageable pageable) {
        return borrowingRepository.findAllByUserIdAndStatus(userId, BorrowingStatus.ACTIVE, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Borrowing> getOverdue(Pageable pageable) {
        return borrowingRepository.findAllByStatus(BorrowingStatus.OVERDUE, pageable);
    }

    // -------------------------------------------------------------------------
    // Scheduled job — run nightly via @Scheduled in a separate component
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public int markOverdueBorrowings() {
        int count = borrowingRepository.markOverdueBorrowings(OffsetDateTime.now());
        if (count > 0) {
            log.warn("Marked {} borrowings as OVERDUE", count);
        }
        return count;
    }

    // -------------------------------------------------------------------------
    // Mark as lost (librarian action)
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void markAsLost(UUID borrowingId, String notes) {
        Borrowing borrowing = getById(borrowingId);
        borrowing.setStatus(BorrowingStatus.LOST);
        borrowing.setNotes(notes);

        // Do NOT restore available copies — the book is lost
        borrowingRepository.save(borrowing);
        auditLogService.logAdminAction("MARK_LOST", "Borrowing", borrowingId,
                "Book marked as lost: " + borrowing.getBook().getTitle());
        log.warn("Book '{}' marked as LOST (borrowing: {})",
                borrowing.getBook().getTitle(), borrowingId);
    }
}