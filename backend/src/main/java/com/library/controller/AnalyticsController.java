package com.library.controller;

import com.library.service.analytics.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")   // all analytics endpoints are ADMIN-only
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // -------------------------------------------------------------------------
    // GET /api/v1/analytics/dashboard
    // Single aggregated call for the admin dashboard widget
    // All service methods are @Transactional(readOnly = true)
    // -------------------------------------------------------------------------

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        return ResponseEntity.ok(analyticsService.getDashboardSummary());
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/analytics/books/by-type
    // -------------------------------------------------------------------------

    @GetMapping("/books/by-type")
    public ResponseEntity<Map<String, Long>> getBooksByType() {
        return ResponseEntity.ok(analyticsService.getBookCountByType());
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/analytics/books/top-borrowed?limit=10
    // -------------------------------------------------------------------------

    @GetMapping("/books/top-borrowed")
    public ResponseEntity<List<Map<String, Object>>> getTopBorrowed(
            @RequestParam(defaultValue = "10") int limit) {

        return ResponseEntity.ok(analyticsService.getTopBorrowedBooks(limit));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/analytics/users/by-role
    // -------------------------------------------------------------------------

    @GetMapping("/users/by-role")
    public ResponseEntity<Map<String, Long>> getUsersByRole() {
        return ResponseEntity.ok(analyticsService.getUserCountByRole());
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/analytics/borrowings/trend?from=...&to=...&limit=10
    // -------------------------------------------------------------------------

    @GetMapping("/borrowings/trend")
    public ResponseEntity<List<Map<String, Object>>> getBorrowingTrend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "10") int limit) {

        return ResponseEntity.ok(analyticsService.getBorrowingTrend(from, to, limit));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/analytics/fines/outstanding
    // -------------------------------------------------------------------------

    @GetMapping("/fines/outstanding")
    public ResponseEntity<Map<String, Object>> getOutstandingFines() {
        Map<String, Object> response = Map.of(
                "totalOutstandingFines", analyticsService.getOutstandingFinesTotal(),
                "currency", "RON"
        );
        return ResponseEntity.ok(response);
    }
}