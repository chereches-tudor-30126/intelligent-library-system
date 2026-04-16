package com.library.service.borrowing;

import com.library.entity.Borrowing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface BorrowingService {

    Borrowing borrowBook(UUID userId, UUID bookId);

    Borrowing returnBook(UUID borrowingId, String notes);

    Borrowing extendDueDate(UUID borrowingId);

    Borrowing getById(UUID id);

    Page<Borrowing> getByUser(UUID userId, Pageable pageable);

    Page<Borrowing> getActiveByUser(UUID userId, Pageable pageable);

    Page<Borrowing> getOverdue(Pageable pageable);

    // Called by @Scheduled job
    int markOverdueBorrowings();

    void markAsLost(UUID borrowingId, String notes);
}