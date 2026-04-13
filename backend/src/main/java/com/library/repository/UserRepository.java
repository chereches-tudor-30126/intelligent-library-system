package com.library.repository;

import com.library.entity.Role;
import com.library.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    List<User> findAllByRole(Role role);

    List<User> findAllByIsActive(Boolean isActive);

    List<User> findAllByRoleAndIsActive(Role role, Boolean isActive);

    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 WHERE u.id = :id")
    void incrementFailedLoginAttempts(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = 0, u.lockedUntil = NULL WHERE u.id = :id")
    void resetFailedLoginAttempts(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE User u SET u.lockedUntil = :until WHERE u.id = :id")
    void lockAccountUntil(@Param("id") UUID id, @Param("until") OffsetDateTime until);

    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :loginAt WHERE u.id = :id")
    void updateLastLoginAt(@Param("id") UUID id, @Param("loginAt") OffsetDateTime loginAt);

    long countByRole(Role role);

    long countByIsActive(Boolean isActive);

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :since")
    long countNewUsersSince(@Param("since") OffsetDateTime since);
}