package com.library.repository;

import com.library.entity.Borrowing;
import com.library.entity.BorrowingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BorrowingRepository extends JpaRepository<Borrowing, UUID> {

    Page<Borrowing> findAllByUserId(UUID userId, Pageable pageable);

    Page<Borrowing> findAllByBookId(UUID bookId, Pageable pageable);

    Page<Borrowing> findAllByStatus(BorrowingStatus status, Pageable pageable);

    Page<Borrowing> findAllByUserIdAndStatus(UUID userId, BorrowingStatus status, Pageable pageable);

    Optional<Borrowing> findByUserIdAndBookIdAndStatus(UUID userId, UUID bookId, BorrowingStatus status);

    boolean existsByUserIdAndBookIdAndStatus(UUID userId, UUID bookId, BorrowingStatus status);

    @Query("SELECT b FROM Borrowing b WHERE b.status = 'ACTIVE' AND b.dueDate < :now")
    List<Borrowing> findOverdueBorrowings(@Param("now") OffsetDateTime now);

    @Modifying
    @Query("UPDATE Borrowing b SET b.status = 'OVERDUE' WHERE b.status = 'ACTIVE' AND b.dueDate < :now")
    int markOverdueBorrowings(@Param("now") OffsetDateTime now);

    @Query("SELECT b FROM Borrowing b WHERE b.status = 'ACTIVE' " +
            "AND b.dueDate BETWEEN :from AND :to")
    List<Borrowing> findBorrowingsDueBetween(
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);

    long countByStatus(BorrowingStatus status);

    long countByUserId(UUID userId);

    @Query("SELECT COUNT(b) FROM Borrowing b WHERE b.borrowedAt >= :since")
    long countBorrowingsSince(@Param("since") OffsetDateTime since);

    @Query("SELECT COALESCE(SUM(b.fineAmount), 0) FROM Borrowing b WHERE b.finePaid = false AND b.fineAmount > 0")
    Double sumOutstandingFines();

    @Query("SELECT b.book.id, COUNT(b) AS borrows FROM Borrowing b " +
            "WHERE b.borrowedAt >= :since GROUP BY b.book.id ORDER BY borrows DESC")
    List<Object[]> findMostBorrowedBooksSince(@Param("since") OffsetDateTime since, Pageable pageable);
}