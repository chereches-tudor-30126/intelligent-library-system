package com.library.service.auth.impl;

import com.library.dto.request.LoginRequest;
import com.library.dto.request.RefreshTokenRequest;
import com.library.dto.request.RegisterRequest;
import com.library.dto.response.AuthResponse;
import com.library.entity.RefreshToken;
import com.library.entity.User;
import com.library.repository.UserRepository;
import com.library.security.CustomUserDetailsService;
import com.library.security.JwtService;
import com.library.security.RefreshTokenService;
import com.library.service.audit.AuditLogService;
import com.library.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager  authenticationManager;
    private final UserRepository         userRepository;
    private final PasswordEncoder        passwordEncoder;
    private final JwtService             jwtService;
    private final RefreshTokenService    refreshTokenService;
    private final CustomUserDetailsService userDetailsService;
    private final AuditLogService        auditLogService;

    // -------------------------------------------------------------------------
    // Register
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();

        User saved = userRepository.save(user);
        log.info("New user registered: {}", saved.getEmail());

        auditLogService.logUserRegister(saved);

        UserDetails userDetails = userDetailsService.loadUserByUsername(saved.getEmail());
        String accessToken  = jwtService.generateAccessToken(userDetails);
        String refreshToken = refreshTokenService.issueRefreshToken(saved, userDetails);

        return buildResponse(accessToken, refreshToken, saved);
    }

    // -------------------------------------------------------------------------
    // Login
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Throws AuthenticationException on bad credentials — caught by GlobalExceptionHandler
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        userRepository.updateLastLoginAt(user.getId(), OffsetDateTime.now());
        userRepository.resetFailedLoginAttempts(user.getId());
        log.info("User logged in: {}", user.getEmail());

        auditLogService.logUserLogin(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken  = jwtService.generateAccessToken(userDetails);
        String refreshToken = refreshTokenService.issueRefreshToken(user, userDetails);

        return buildResponse(accessToken, refreshToken, user);
    }

    // -------------------------------------------------------------------------
    // Refresh
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        Optional<RefreshToken> tokenOpt =
                refreshTokenService.validateRefreshToken(request.getRefreshToken());

        if (tokenOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Invalid or expired refresh token");
        }

        User user = tokenOpt.get().getUser();
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

        String newRefresh = refreshTokenService.rotateRefreshToken(
                request.getRefreshToken(), user, userDetails);
        String newAccess  = jwtService.generateAccessToken(userDetails);

        return buildResponse(newAccess, newRefresh, user);
    }

    // -------------------------------------------------------------------------
    // Logout (single session)
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenService.validateRefreshToken(request.getRefreshToken())
                .ifPresent(token -> {
                    refreshTokenService.revokeAllSessionsForUser(token.getUser().getId());
                    auditLogService.logUserLogout(token.getUser());
                    log.info("User logged out: {}", token.getUser().getEmail());
                });
    }

    // -------------------------------------------------------------------------
    // Logout everywhere (password change, suspicious activity)
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void logoutAllSessions(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            refreshTokenService.revokeAllSessionsForUser(user.getId());
            log.warn("All sessions revoked for user: {}", email);
        });
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private AuthResponse buildResponse(String accessToken, String refreshToken, User user) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtService.getAccessTokenExpiration() / 1000)
                .userId(user.getId().toString())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole())
                .build();
    }
}