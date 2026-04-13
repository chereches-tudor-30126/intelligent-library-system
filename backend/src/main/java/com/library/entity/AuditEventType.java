package com.library.entity;
implement JPA domain model and data access layer
public enum AuditEventType {
    USER_LOGIN,
    USER_LOGOUT,
    USER_REGISTER,
    USER_UPDATE,
    USER_DELETE,
    BOOK_CREATE,
    BOOK_UPDATE,
    BOOK_DELETE,
    BORROW_CREATE,
    BORROW_RETURN,
    BORROW_EXTEND,
    ADMIN_ACTION
}