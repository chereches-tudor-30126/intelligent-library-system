package com.library.service.notification.impl;

import com.library.entity.Book;
import com.library.entity.Borrowing;
import com.library.entity.Notification;
import com.library.entity.NotificationType;
import com.library.entity.User;
import com.library.repository.BorrowingRepository;
import com.library.repository.NotificationRepository;
import com.library.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final BorrowingRepository    borrowingRepository;

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public Page<Notification> getForUser(UUID userId, Pageable pageable) {
        return notificationRepository.findAllByUserId(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Notification> getUnreadForUser(UUID userId, Pageable pageable) {
        return notificationRepository.findAllByUserIdAndIsRead(userId, false, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(UUID userId) {
        return notificationRepository.countByUserIdAndIsRead(userId, false);
    }

    // -------------------------------------------------------------------------
    // Mark as read
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        int updated = notificationRepository.markAsRead(
                notificationId, userId, OffsetDateTime.now());
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Notification not found or does not belong to user");
        }
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsReadForUser(userId, OffsetDateTime.now());
    }

    // -------------------------------------------------------------------------
    // Borrow lifecycle notifications
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void sendBorrowConfirmation(User user, Book book, OffsetDateTime dueDate) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("book_id", book.getId().toString());
        metadata.put("book_title", book.getTitle());
        metadata.put("due_date", dueDate.toString());

        save(user, NotificationType.ACCOUNT_ACTIVITY,
                "Book borrowed successfully",
                "You have borrowed \"" + book.getTitle() + "\". Due date: " + dueDate.toLocalDate(),
                metadata);
    }

    @Override
    @Transactional
    public void sendReturnConfirmation(User user, Book book) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("book_id", book.getId().toString());
        metadata.put("book_title", book.getTitle());

        save(user, NotificationType.ACCOUNT_ACTIVITY,
                "Book returned",
                "Thank you for returning \"" + book.getTitle() + "\".",
                metadata);
    }

    @Override
    @Transactional
    public void sendExtensionConfirmation(User user, Book book, OffsetDateTime newDueDate) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("book_id", book.getId().toString());
        metadata.put("new_due_date", newDueDate.toString());

        save(user, NotificationType.DUE_DATE_REMINDER,
                "Due date extended",
                "Your borrowing of \"" + book.getTitle() +
                        "\" has been extended. New due date: " + newDueDate.toLocalDate(),
                metadata);
    }

    // -------------------------------------------------------------------------
    // Scheduled — due date reminders (24h window)
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void sendDueDateReminders() {
        OffsetDateTime from = OffsetDateTime.now();
        OffsetDateTime to   = from.plusHours(24);

        List<Borrowing> upcoming = borrowingRepository.findBorrowingsDueBetween(from, to);
        for (Borrowing b : upcoming) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("book_id", b.getBook().getId().toString());
            metadata.put("due_date", b.getDueDate().toString());

            save(b.getUser(), NotificationType.DUE_DATE_REMINDER,
                    "Due tomorrow: " + b.getBook().getTitle(),
                    "Reminder: \"" + b.getBook().getTitle() +
                            "\" is due tomorrow (" + b.getDueDate().toLocalDate() + ").",
                    metadata);
        }
        log.info("Sent {} due-date reminder notifications", upcoming.size());
    }

    // -------------------------------------------------------------------------
    // Scheduled — overdue alerts
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void sendOverdueAlerts() {
        List<Borrowing> overdue = borrowingRepository.findOverdueBorrowings(OffsetDateTime.now());
        for (Borrowing b : overdue) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("book_id", b.getBook().getId().toString());
            metadata.put("due_date", b.getDueDate().toString());

            save(b.getUser(), NotificationType.OVERDUE_ALERT,
                    "Overdue: " + b.getBook().getTitle(),
                    "\"" + b.getBook().getTitle() + "\" is overdue since " +
                            b.getDueDate().toLocalDate() + ". Please return it as soon as possible.",
                    metadata);
        }
        log.warn("Sent {} overdue alert notifications", overdue.size());
    }

    // -------------------------------------------------------------------------
    // Internal helper
    // -------------------------------------------------------------------------

    private void save(User user, NotificationType type,
                      String title, String message,
                      Map<String, Object> metadata) {
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .metadata(metadata)
                .build();
        notificationRepository.save(notification);
    }
}