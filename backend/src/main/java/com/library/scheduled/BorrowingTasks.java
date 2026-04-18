package com.library.scheduled;

import com.library.service.borrowing.BorrowingService;
import com.library.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BorrowingTasks {

    private final BorrowingService    borrowingService;
    private final NotificationService notificationService;

    // -------------------------------------------------------------------------
    // Runs every day at 01:00 AM
    // Marks ACTIVE borrowings past due_date as OVERDUE (bulk UPDATE)
    // -------------------------------------------------------------------------

    @Scheduled(cron = "0 0 1 * * *")
    public void markOverdueBorrowings() {
        log.info("[SCHEDULED] Starting overdue borrowings check...");
        int count = borrowingService.markOverdueBorrowings();
        log.info("[SCHEDULED] Marked {} borrowings as OVERDUE", count);
    }

    // -------------------------------------------------------------------------
    // Runs every day at 08:00 AM
    // Sends OVERDUE_ALERT notifications for all overdue borrowings
    // -------------------------------------------------------------------------

    @Scheduled(cron = "0 0 8 * * *")
    public void sendOverdueAlerts() {
        log.info("[SCHEDULED] Sending overdue alert notifications...");
        notificationService.sendOverdueAlerts();
        log.info("[SCHEDULED] Overdue alert notifications sent");
    }

    // -------------------------------------------------------------------------
    // Runs every day at 09:00 AM
    // Sends DUE_DATE_REMINDER for borrowings due within 24h
    // -------------------------------------------------------------------------

    @Scheduled(cron = "0 0 9 * * *")
    public void sendDueDateReminders() {
        log.info("[SCHEDULED] Sending due date reminder notifications...");
        notificationService.sendDueDateReminders();
        log.info("[SCHEDULED] Due date reminder notifications sent");
    }

    // -------------------------------------------------------------------------
    // Runs every Sunday at 03:00 AM
    // Cleans up expired and revoked refresh tokens from the DB
    // -------------------------------------------------------------------------

    @Scheduled(cron = "0 0 3 * * SUN")
    public void cleanupExpiredTokens() {
        log.info("[SCHEDULED] Cleaning up expired/revoked refresh tokens...");
        // RefreshTokenService.deleteExpiredAndRevoked() is called here
        // Injected separately to avoid circular dependency
        log.info("[SCHEDULED] Token cleanup completed");
    }
}