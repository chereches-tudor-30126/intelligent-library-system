package com.library.controller;

import com.library.entity.Role;
import com.library.entity.User;
import com.library.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // -------------------------------------------------------------------------
    // GET /api/v1/users/me — any authenticated user
    // -------------------------------------------------------------------------

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<User> getMe(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.getByEmail(userDetails.getUsername()));
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/users/me — update own profile
    // -------------------------------------------------------------------------

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<User> updateMe(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) String profilePictureUrl) {

        UUID userId = userService.getByEmail(userDetails.getUsername()).getId();
        return ResponseEntity.ok(
                userService.updateProfile(userId, firstName, lastName, phoneNumber, profilePictureUrl));
    }

    // -------------------------------------------------------------------------
    // PATCH /api/v1/users/me/password — change own password
    // -------------------------------------------------------------------------

    @PatchMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String currentPassword,
            @RequestParam String newPassword) {

        UUID userId = userService.getByEmail(userDetails.getUsername()).getId();
        userService.changePassword(userId, currentPassword, newPassword);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/users — ADMIN only, paginated
    // -------------------------------------------------------------------------

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<User>> getAll(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Role role) {

        Pageable pageable = PageRequest.of(page, size);
        Page<User> result = (role != null)
                ? userService.getAllByRole(role, pageable)
                : userService.getAll(pageable);
        return ResponseEntity.ok(result);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/users/{id} — ADMIN, LIBRARIAN
    // -------------------------------------------------------------------------

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<User> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    // -------------------------------------------------------------------------
    // PATCH /api/v1/users/{id}/deactivate — ADMIN only
    // -------------------------------------------------------------------------

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        userService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // PATCH /api/v1/users/{id}/activate — ADMIN only
    // -------------------------------------------------------------------------

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> activate(@PathVariable UUID id) {
        userService.activate(id);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // PATCH /api/v1/users/{id}/role — ADMIN only
    // -------------------------------------------------------------------------

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> changeRole(
            @PathVariable UUID id,
            @RequestParam Role newRole) {

        userService.changeRole(id, newRole);
        return ResponseEntity.noContent().build();
    }
}