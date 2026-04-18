package com.library.service.audit.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.entity.AuditEventType;
import com.library.entity.AuditLog;
import com.library.entity.Book;
import com.library.entity.Borrowing;
import com.library.entity.User;
import com.library.repository.AuditLogRepository;
import com.library.service.audit.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper       objectMapper;

    // -------------------------------------------------------------------------
    // Query
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> getAll(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> getByUser(UUID userId, Pageable pageable) {
        return auditLogRepository.findAllByUserId(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> getByEventType(AuditEventType type, Pageable pageable) {
        return auditLogRepository.findAllByEventType(type, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> getByDateRange(OffsetDateTime from, OffsetDateTime to, Pageable pageable) {
        return auditLogRepository.findAllByCreatedAtBetween(from, to, pageable);
    }

    // -------------------------------------------------------------------------
    // Auth events
    // -------------------------------------------------------------------------

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logUserLogin(User user) {
        persist(user, AuditEventType.USER_LOGIN, "User", user.getId(),
                "Login: " + user.getEmail(), null, null);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logUserLogout(User user) {
        persist(user, AuditEventType.USER_LOGOUT, "User", user.getId(),
                "Logout: " + user.getEmail(), null, null);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logUserRegister(User user) {
        persist(null, AuditEventType.USER_REGISTER, "User", user.getId(),
                "New registration: " + user.getEmail(), null, toMap(user));
    }

    // -------------------------------------------------------------------------
    // User update
    // -------------------------------------------------------------------------

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logUserUpdate(User updated, User snapshot) {
        persist(updated, AuditEventType.USER_UPDATE, "User", updated.getId(),
                "Profile updated: " + updated.getEmail(), toMap(snapshot), toMap(updated));
    }

    // -------------------------------------------------------------------------
    // Book events
    // -------------------------------------------------------------------------

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logBookCreate(Book book) {
        persist(null, AuditEventType.BOOK_CREATE, "Book", book.getId(),
                "Book created: " + book.getTitle(), null, toMap(book));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logBookUpdate(Book updated, Book snapshot) {
        persist(null, AuditEventType.BOOK_UPDATE, "Book", updated.getId(),
                "Book updated: " + updated.getTitle(), toMap(snapshot), toMap(updated));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logBookDelete(Book book) {
        persist(null, AuditEventType.BOOK_DELETE, "Book", book.getId(),
                "Book soft-deleted: " + book.getTitle(), toMap(book), null);
    }

    // -------------------------------------------------------------------------
    // Borrowing events
    // -------------------------------------------------------------------------

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logBorrowCreate(Borrowing borrowing) {
        persist(borrowing.getUser(), AuditEventType.BORROW_CREATE,
                "Borrowing", borrowing.getId(),
                "Borrowed: " + borrowing.getBook().getTitle(), null, toMap(borrowing));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logBorrowReturn(Borrowing borrowing) {
        persist(borrowing.getUser(), AuditEventType.BORROW_RETURN,
                "Borrowing", borrowing.getId(),
                "Returned: " + borrowing.getBook().getTitle(), null, toMap(borrowing));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logBorrowExtend(Borrowing borrowing) {
        persist(borrowing.getUser(), AuditEventType.BORROW_EXTEND,
                "Borrowing", borrowing.getId(),
                "Extended: " + borrowing.getBook().getTitle() +
                        " → " + borrowing.getDueDate().toLocalDate(), null, toMap(borrowing));
    }

    // -------------------------------------------------------------------------
    // Generic admin action
    // -------------------------------------------------------------------------

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAdminAction(String action, String entityType,
                               UUID entityId, String description) {
        persist(null, AuditEventType.ADMIN_ACTION,
                entityType, entityId, description, null, null);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    // REQUIRES_NEW ensures audit logs are written even if the parent transaction rolls back
    private void persist(User actor, AuditEventType eventType,
                         String entityType, UUID entityId,
                         String description,
                         Map<String, Object> oldValue,
                         Map<String, Object> newValue) {
        AuditLog entry = AuditLog.builder()
                .user(actor)
                .eventType(eventType)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .oldValue(oldValue)
                .newValue(newValue)
                .build();
        auditLogRepository.save(entry);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object obj) {
        return objectMapper.convertValue(obj, Map.class);
    }
}