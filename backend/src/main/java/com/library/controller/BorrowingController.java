package com.library.controller;

import com.library.entity.Borrowing;
import com.library.service.borrowing.BorrowingService;
import com.library.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/borrowings")
@RequiredArgsConstructor
public class BorrowingController {

    private final BorrowingService borrowingService;
    private final UserService      userService;

    // -------------------------------------------------------------------------
    // POST /api/v1/borrowings?bookId=... — authenticated users (STUDENT+)
    // Race-condition-safe: service uses atomic SQL decrement
    // -------------------------------------------------------------------------

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'LIBRARIAN', 'ADMIN')")
    public ResponseEntity<Borrowing> borrowBook(
            @RequestParam UUID bookId,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = resolveUserId(userDetails);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(borrowingService.borrowBook(userId, bookId));
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/borrowings/return/{id}
    // Fine calculated automatically in service layer
    // -------------------------------------------------------------------------

    @PostMapping("/return/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Borrowing> returnBook(
            @PathVariable UUID id,
            @RequestParam(required = false) String notes) {

        return ResponseEntity.ok(borrowingService.returnBook(id, notes));
    }

    // -------------------------------------------------------------------------
    // PATCH /api/v1/borrowings/extend/{id}
    // -------------------------------------------------------------------------

    @PatchMapping("/extend/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Borrowing> extendDueDate(@PathVariable UUID id) {
        return ResponseEntity.ok(borrowingService.extendDueDate(id));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/borrowings/my — current user's full history
    // -------------------------------------------------------------------------

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<Borrowing>> getMyBorrowings(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                borrowingService.getByUser(resolveUserId(userDetails), pageable));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/borrowings/my/active
    // -------------------------------------------------------------------------

    @GetMapping("/my/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<Borrowing>> getMyActiveBorrowings(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                borrowingService.getActiveByUser(resolveUserId(userDetails), pageable));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/borrowings/{id}
    // -------------------------------------------------------------------------

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Borrowing> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(borrowingService.getById(id));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/borrowings/overdue — LIBRARIAN, ADMIN
    // -------------------------------------------------------------------------

    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<Page<Borrowing>> getOverdue(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(
                borrowingService.getOverdue(PageRequest.of(page, size)));
    }

    // -------------------------------------------------------------------------
    // PATCH /api/v1/borrowings/{id}/lost — LIBRARIAN, ADMIN
    // -------------------------------------------------------------------------

    @PatchMapping("/{id}/lost")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<Void> markAsLost(
            @PathVariable UUID id,
            @RequestParam(required = false) String notes) {

        borrowingService.markAsLost(id, notes);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Helper — resolve UUID from JWT principal (email stored as username)
    // -------------------------------------------------------------------------

    private UUID resolveUserId(UserDetails userDetails) {
        return userService.getByEmail(userDetails.getUsername()).getId();
    }
}