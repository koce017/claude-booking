package com.booking.admin;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.booking.support.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

class AdminAuthCleanupJobTest extends IntegrationTest {

    @Autowired
    AdminAuthCleanupJob job;

    @Autowired
    AdminLoginTokenRepository tokenRepository;

    @Autowired
    AdminSessionRepository sessionRepository;

    @Test
    void removesOnlyLongExpiredTokensAndSessions() throws Exception {
        adminToken();
        assertThat(tokenRepository.count()).isEqualTo(1);
        assertThat(sessionRepository.count()).isEqualTo(1);

        job.cleanUp();
        assertThat(tokenRepository.count()).isEqualTo(1);
        assertThat(sessionRepository.count()).isEqualTo(1);

        clock.advance(Duration.ofHours(12).plusDays(1).plusMinutes(1));
        job.cleanUp();
        assertThat(tokenRepository.count()).isZero();
        assertThat(sessionRepository.count()).isZero();
    }
}
