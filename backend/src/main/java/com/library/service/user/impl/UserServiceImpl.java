package com.library.service.user.impl;

import com.library.entity.Role;
import com.library.entity.User;
import com.library.repository.UserRepository;
import com.library.security.RefreshTokenService;
import com.library.service.audit.AuditLogService;
import com.library.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository      userRepository;
    private final PasswordEncoder     passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final AuditLogService     auditLogService;

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public User getById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found: " + email));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> getAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> getAllByRole(Role role, Pageable pageable) {
        return userRepository.findAllByRole(role)
                .stream()
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toList(),
                        list -> new org.springframework.data.domain.PageImpl<>(list, pageable, list.size())
                ));
    }

    // -------------------------------------------------------------------------
    // Update
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public User updateProfile(UUID id, String firstName, String lastName,
                              String phoneNumber, String profilePictureUrl) {
        User user = getById(id);
        User snapshot = copySnapshot(user);

        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhoneNumber(phoneNumber);
        user.setProfilePictureUrl(profilePictureUrl);

        User saved = userRepository.save(user);
        auditLogService.logUserUpdate(saved, snapshot);
        log.info("Profile updated for user: {}", id);
        return saved;
    }

    @Override
    @Transactional
    public void changePassword(UUID id, String currentPassword, String newPassword) {
        User user = getById(id);

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Revoke all sessions — user must log in again after password change
        refreshTokenService.revokeAllSessionsForUser(id);
        log.info("Password changed and all sessions revoked for user: {}", id);
    }

    // -------------------------------------------------------------------------
    // Admin operations
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void deactivate(UUID id) {
        User user = getById(id);
        user.setIsActive(false);
        userRepository.save(user);
        refreshTokenService.revokeAllSessionsForUser(id);
        auditLogService.logAdminAction("DEACTIVATE_USER", "User", id,
                "User deactivated: " + user.getEmail());
        log.warn("User deactivated: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void activate(UUID id) {
        User user = getById(id);
        user.setIsActive(true);
        user.setFailedLoginAttempts((short) 0);
        user.setLockedUntil(null);
        userRepository.save(user);
        auditLogService.logAdminAction("ACTIVATE_USER", "User", id,
                "User activated: " + user.getEmail());
        log.info("User activated: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void changeRole(UUID id, Role newRole) {
        User user = getById(id);
        Role oldRole = user.getRole();
        user.setRole(newRole);
        userRepository.save(user);
        auditLogService.logAdminAction("CHANGE_ROLE", "User", id,
                "Role changed from " + oldRole + " to " + newRole);
        log.info("Role changed for user {}: {} → {}", id, oldRole, newRole);
    }

    // -------------------------------------------------------------------------
    // Analytics
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public long countByRole(Role role) {
        return userRepository.countByRole(role);
    }

    // -------------------------------------------------------------------------
    // Internal helper — shallow snapshot for audit diff
    // -------------------------------------------------------------------------

    private User copySnapshot(User user) {
        return User.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .build();
    }
}