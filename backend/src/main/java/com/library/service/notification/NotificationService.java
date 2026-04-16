package com.library.service.notification;

import com.library.entity.Book;
import com.library.entity.Notification;
import com.library.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface NotificationService {

    // In-app notifications
    Page<Notification> getForUser(UUID userId, Pageable pageable);
    Page<Notification> getUnreadForUser(UUID userId, Pageable pageable);
    long countUnread(UUID userId);
    void markAsRead(UUID notificationId, UUID userId);
    void markAllAsRead(UUID userId);

    // Triggered by BorrowingService
    void sendBorrowConfirmation(User user, Book book, OffsetDateTime dueDate);
    void sendReturnConfirmation(User user, Book book);
    void sendExtensionConfirmation(User user, Book book, OffsetDateTime newDueDate);

    // Triggered by scheduler
    void sendDueDateReminders();
    void sendOverdueAlerts();
}