package com.library.security;

import com.library.entity.RefreshToken;
import com.library.entity.User;
import com.library.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Value("${jwt.max-sessions-per-user:5}")
    private int maxSessionsPerUser;

    @Transactional
    public String issueRefreshToken(User user, UserDetails userDetails) {
        String rawToken = jwtService.generateRefreshToken(userDetails);
        String tokenHash = hashToken(rawToken);

        OffsetDateTime expiresAt = OffsetDateTime.now()
                .plusSeconds(jwtService.getRefreshTokenExpiration() / 1000);

        RefreshToken entity = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .revoked(false)
                .build();

        refreshTokenRepository.save(entity);
        return rawToken;  // return plaintext only once — never stored
    }

    @Transactional
    public String rotateRefreshToken(String oldRawToken, User user, UserDetails userDetails) {
        String oldHash = hashToken(oldRawToken);

        int revoked = refreshTokenRepository.revokeByTokenHash(oldHash);
        if (revoked == 0) {

            refreshTokenRepository.revokeAllForUser(user.getId());
            throw new SecurityException("Refresh token reuse detected. All sessions revoked.");
        }

        return issueRefreshToken(user, userDetails);
    }

    @Transactional(readOnly = true)
    public Optional<RefreshToken> validateRefreshToken(String rawToken) {
        String tokenHash = hashToken(rawToken);
        Optional<RefreshToken> found = refreshTokenRepository.findByTokenHash(tokenHash);

        if (found.isEmpty()) {
            return Optional.empty();
        }

        RefreshToken token = found.get();

        if (token.getRevoked()) {
            return Optional.empty();
        }

        if (token.getExpiresAt().isBefore(OffsetDateTime.now())) {
            return Optional.empty();
        }

        return Optional.of(token);
    }

    @Transactional
    public void revokeAllSessionsForUser(java.util.UUID userId) {
        refreshTokenRepository.revokeAllForUser(userId);
    }

    @Transactional
    public int deleteExpiredAndRevoked() {
        return refreshTokenRepository.deleteExpiredOrRevoked(OffsetDateTime.now());
    }

    public String hashToken(String rawToken) {
        MessageDigest digest;
        digest = getDigest();
        byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hashBytes);
    }

    private MessageDigest getDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to exist in every Java SE implementation
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}