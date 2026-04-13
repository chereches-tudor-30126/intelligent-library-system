package com.library.repository;

import com.library.entity.AuditEventType;
import com.library.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {


    Page<AuditLog> findAllByUserId(UUID userId, Pageable pageable);

    Page<AuditLog> findAllByEventType(AuditEventType eventType, Pageable pageable);

    Page<AuditLog> findAllByEntityTypeAndEntityId(String entityType, UUID entityId, Pageable pageable);

    Page<AuditLog> findAllByCreatedAtBetween(OffsetDateTime from, OffsetDateTime to, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.user.id = :userId AND a.createdAt BETWEEN :from AND :to")
    Page<AuditLog> findByUserIdAndDateRange(
            @Param("userId") UUID userId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            Pageable pageable);


    long countByEventType(AuditEventType eventType);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.createdAt >= :since")
    long countEventsSince(@Param("since") OffsetDateTime since);

    @Query("SELECT a.eventType, COUNT(a) FROM AuditLog a GROUP BY a.eventType")
    List<Object[]> countGroupedByEventType();
}