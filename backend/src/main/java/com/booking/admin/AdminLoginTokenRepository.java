package com.booking.admin;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminLoginTokenRepository extends JpaRepository<AdminLoginToken, Long> {

    /**
     * Atomically marks an unused, unexpired token as used. Returns 1 if this call
     * consumed it, 0 otherwise; so a token can be redeemed only once even under
     * concurrent requests.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update AdminLoginToken t set t.usedAt = :now
            where t.tokenHash = :hash and t.usedAt is null and t.expiresAt > :now
            """)
    int consume(@Param("hash") String hash, @Param("now") Instant now);

    @EntityGraph(attributePaths = "administrator")
    Optional<AdminLoginToken> findByTokenHash(String tokenHash);
}
