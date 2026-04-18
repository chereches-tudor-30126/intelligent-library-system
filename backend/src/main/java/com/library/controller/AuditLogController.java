package com.library.controller;

import com.library.entity.AuditEventType;
import com.library.entity.AuditLog;
import com.library.service.audit.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")   // immutable audit trail — ADMIN eyes only
public class AuditLogController {

    private final AuditLogService auditLogService;

    // -------------------------------------------------------------------------
    // GET /api/v1/audit — full log, newest first, paginated
    // -------------------------------------------------------------------------

    @GetMapping
    public ResponseEntity<Page<AuditLog>> getAll(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {

        PageRequest pageable = PageRequest.of(page, size,
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(auditLogService.getAll(pageable));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/audit/{id}
    // -------------------------------------------------------------------------

    @GetMapping("/{id}")
    public ResponseEntity<AuditLog> getById(@PathVariable UUID id) {
        // AuditLogRepository extends JpaRepository — use findById directly
        // We expose it via service to keep the layer consistent
        return ResponseEntity.ok(auditLogService.getAll(PageRequest.of(0, 1))
                .stream()
                .filter(log -> log.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Audit log not found")));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/audit/user/{userId}
    // -------------------------------------------------------------------------

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<AuditLog>> getByUser(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {

        PageRequest pageable = PageRequest.of(page, size,
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(auditLogService.getByUser(userId, pageable));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/audit/event/{type}
    // -------------------------------------------------------------------------

    @GetMapping("/event/{type}")
    public ResponseEntity<Page<AuditLog>> getByEventType(
            @PathVariable AuditEventType type,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {

        PageRequest pageable = PageRequest.of(page, size,
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(auditLogService.getByEventType(type, pageable));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/audit/range?from=...&to=...
    // -------------------------------------------------------------------------

    @GetMapping("/range")
    public ResponseEntity<Page<AuditLog>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {

        PageRequest pageable = PageRequest.of(page, size,
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(auditLogService.getByDateRange(from, to, pageable));
    }
}