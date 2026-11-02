package com.booking.admin;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deletes login tokens and sessions that can no longer be used. They are kept for
 * a day after they end, which is plenty for looking into a login problem.
 */
@Component
public class AdminAuthCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(AdminAuthCleanupJob.class);
    private static final Duration KEEP = Duration.ofDays(1);

    private final AdminLoginTokenRepository tokenRepository;
    private final AdminSessionRepository sessionRepository;
    private final Clock clock;

    public AdminAuthCleanupJob(AdminLoginTokenRepository tokenRepository,
                               AdminSessionRepository sessionRepository,
                               Clock clock) {
        this.tokenRepository = tokenRepository;
        this.sessionRepository = sessionRepository;
        this.clock = clock;
    }

    @Scheduled(cron = "${booking.admin.cleanup-cron:0 30 3 * * *}")
    @Transactional
    public void cleanUp() {
        Instant cutoff = clock.instant().minus(KEEP);
        int tokens = tokenRepository.deleteExpiredBefore(cutoff);
        int sessions = sessionRepository.deleteEndedBefore(cutoff);
        if (tokens > 0 || sessions > 0) {
            log.info("Removed {} expired login tokens and {} ended admin sessions", tokens, sessions);
        }
    }
}
