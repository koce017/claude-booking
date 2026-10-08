package com.booking.admin;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminSessionRepository extends JpaRepository<AdminSession, Long> {

    @EntityGraph(attributePaths = "administrator")
    Optional<AdminSession> findByTokenHash(String tokenHash);
}
