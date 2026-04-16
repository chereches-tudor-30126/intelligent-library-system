package com.library.controller;

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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final CustomUserDetailsService userDetailsService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {

        // Check uniqueness
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

        User savedUser = userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getEmail());
        String accessToken  = jwtService.generateAccessToken(userDetails);
        String refreshToken = refreshTokenService.issueRefreshToken(savedUser, userDetails);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                buildAuthResponse(accessToken, refreshToken, savedUser)
        );
    }


    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {


        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));


        userRepository.updateLastLoginAt(user.getId(), OffsetDateTime.now());

        userRepository.resetFailedLoginAttempts(user.getId());

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken  = jwtService.generateAccessToken(userDetails);
        String refreshToken = refreshTokenService.issueRefreshToken(user, userDetails);

        return ResponseEntity.ok(buildAuthResponse(accessToken, refreshToken, user));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {


        Optional<RefreshToken> tokenOpt =
                refreshTokenService.validateRefreshToken(request.getRefreshToken());

        if (tokenOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
        }

        User user = tokenOpt.get().getUser();
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());


        String newRefreshToken = refreshTokenService.rotateRefreshToken(
                request.getRefreshToken(), user, userDetails);
        String newAccessToken = jwtService.generateAccessToken(userDetails);

        return ResponseEntity.ok(buildAuthResponse(newAccessToken, newRefreshToken, user));
    }


    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {

        Optional<RefreshToken> tokenOpt =
                refreshTokenService.validateRefreshToken(request.getRefreshToken());


        tokenOpt.ifPresent(token ->
                refreshTokenService.revokeAllSessionsForUser(token.getUser().getId()));

        return ResponseEntity.noContent().build();  // 204
    }

    private AuthResponse buildAuthResponse(String accessToken,
                                           String refreshToken,
                                           User user) {
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