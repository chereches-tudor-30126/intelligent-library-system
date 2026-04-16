package com.library.service.audit;

import com.library.entity.AuditLog;
import com.library.entity.AuditEventType;
import com.library.entity.Book;
import com.library.entity.Borrowing;
import com.library.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface AuditLogService {

    // Query
    Page<AuditLog> getAll(Pageable pageable);
    Page<AuditLog> getByUser(UUID userId, Pageable pageable);
    Page<AuditLog> getByEventType(AuditEventType type, Pageable pageable);
    Page<AuditLog> getByDateRange(OffsetDateTime from, OffsetDateTime to, Pageable pageable);

    // Convenience log methods called by other services
    void logUserLogin(User user);
    void logUserLogout(User user);
    void logUserRegister(User user);
    void logUserUpdate(User updated, User snapshot);
    void logBookCreate(Book book);
    void logBookUpdate(Book updated, Book snapshot);
    void logBookDelete(Book book);
    void logBorrowCreate(Borrowing borrowing);
    void logBorrowReturn(Borrowing borrowing);
    void logBorrowExtend(Borrowing borrowing);
    void logAdminAction(String action, String entityType, UUID entityId, String description);
}