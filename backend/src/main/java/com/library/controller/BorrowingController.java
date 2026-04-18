package com.library.controller;

import com.library.dto.response.BorrowingResponse;
import com.library.service.borrowing.BorrowingService;
import com.library.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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

import com.library.entity.Borrowing;

import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/borrowings")
@RequiredArgsConstructor
public class BorrowingController {

    private final BorrowingService borrowingService;
    private final UserService      userService;

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'LIBRARIAN', 'ADMIN')")
    public ResponseEntity<BorrowingResponse> borrowBook(
            @RequestParam UUID bookId,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = resolveUserId(userDetails);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BorrowingResponse.from(borrowingService.borrowBook(userId, bookId)));
    }

    @PostMapping("/return/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BorrowingResponse> returnBook(
            @PathVariable UUID id,
            @RequestParam(required = false) String notes) {

        return ResponseEntity.ok(BorrowingResponse.from(borrowingService.returnBook(id, notes)));
    }

    @PatchMapping("/extend/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BorrowingResponse> extendDueDate(@PathVariable UUID id) {
        return ResponseEntity.ok(BorrowingResponse.from(borrowingService.extendDueDate(id)));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<BorrowingResponse>> getMyBorrowings(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(toPage(
                borrowingService.getByUser(resolveUserId(userDetails), pageable), pageable));
    }

    @GetMapping("/my/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<BorrowingResponse>> getMyActiveBorrowings(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(toPage(
                borrowingService.getActiveByUser(resolveUserId(userDetails), pageable), pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BorrowingResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(BorrowingResponse.from(borrowingService.getById(id)));
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<Page<BorrowingResponse>> getOverdue(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(toPage(borrowingService.getOverdue(pageable), pageable));
    }

    @PatchMapping("/{id}/lost")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<Void> markAsLost(
            @PathVariable UUID id,
            @RequestParam(required = false) String notes) {
        borrowingService.markAsLost(id, notes);
        return ResponseEntity.noContent().build();
    }

    private UUID resolveUserId(UserDetails userDetails) {
        return userService.getByEmail(userDetails.getUsername()).getId();
    }

    private Page<BorrowingResponse> toPage(Page<Borrowing> page, Pageable pageable) {
        return new PageImpl<>(
                page.getContent().stream()
                        .map(BorrowingResponse::from)
                        .collect(Collectors.toList()),
                pageable,
                page.getTotalElements()
        );
    }
}