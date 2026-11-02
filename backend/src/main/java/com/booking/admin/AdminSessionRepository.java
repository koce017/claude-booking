package com.booking.admin;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminSessionRepository extends JpaRepository<AdminSession, Long> {

    @EntityGraph(attributePaths = "administrator")
    Optional<AdminSession> findByTokenHash(String tokenHash);

    /** Deletes sessions that expired or were revoked before {@code cutoff}. */
    @Modifying
    @Query("delete from AdminSession s where s.expiresAt < :cutoff or s.revokedAt < :cutoff")
    int deleteEndedBefore(@Param("cutoff") Instant cutoff);
}
