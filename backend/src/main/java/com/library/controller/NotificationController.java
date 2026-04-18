package com.library.controller;

import com.library.entity.Notification;
import com.library.service.notification.NotificationService;
import com.library.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService         userService;

    // -------------------------------------------------------------------------
    // GET /api/v1/notifications — current user's notifications
    // -------------------------------------------------------------------------

    @GetMapping
    public ResponseEntity<Page<Notification>> getAll(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID userId = resolveUserId(userDetails);
        return ResponseEntity.ok(
                notificationService.getForUser(userId, PageRequest.of(page, size)));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/notifications/unread
    // -------------------------------------------------------------------------

    @GetMapping("/unread")
    public ResponseEntity<Page<Notification>> getUnread(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID userId = resolveUserId(userDetails);
        return ResponseEntity.ok(
                notificationService.getUnreadForUser(userId, PageRequest.of(page, size)));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/notifications/unread/count — used by frontend badge
    // -------------------------------------------------------------------------

    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Long>> countUnread(
            @AuthenticationPrincipal UserDetails userDetails) {

        long count = notificationService.countUnread(resolveUserId(userDetails));
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    // -------------------------------------------------------------------------
    // PATCH /api/v1/notifications/{id}/read
    // -------------------------------------------------------------------------

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {

        notificationService.markAsRead(id, resolveUserId(userDetails));
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // PATCH /api/v1/notifications/read-all
    // -------------------------------------------------------------------------

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal UserDetails userDetails) {

        notificationService.markAllAsRead(resolveUserId(userDetails));
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private UUID resolveUserId(UserDetails userDetails) {
        return userService.getByEmail(userDetails.getUsername()).getId();
    }
}