package com.booking.admin;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import com.booking.common.error.ApiException;
import com.booking.config.BookingProperties;
import com.booking.notification.MagicLinkSender;

/**
 * Magic-link authentication: request a link by email, redeem the single-use
 * token for a session token, then authenticate requests with that session token.
 */
@Service
public class AdminAuthService {

    private static final Logger log = LoggerFactory.getLogger(AdminAuthService.class);

    public record IssuedSession(String token, Instant expiresAt, String email) {
    }

    public static class InvalidLoginTokenException extends ApiException {
        public InvalidLoginTokenException() {
            super(HttpStatus.UNAUTHORIZED, "INVALID_LOGIN_TOKEN", "The login link is invalid, expired or already used");
        }
    }

    private final AdministratorRepository administratorRepository;
    private final AdminLoginTokenRepository loginTokenRepository;
    private final AdminSessionRepository sessionRepository;
    private final MagicLinkSender magicLinkSender;
    private final BookingProperties properties;
    private final Clock clock;

    public AdminAuthService(AdministratorRepository administratorRepository,
                            AdminLoginTokenRepository loginTokenRepository,
                            AdminSessionRepository sessionRepository,
                            MagicLinkSender magicLinkSender,
                            BookingProperties properties,
                            Clock clock) {
        this.administratorRepository = administratorRepository;
        this.loginTokenRepository = loginTokenRepository;
        this.sessionRepository = sessionRepository;
        this.magicLinkSender = magicLinkSender;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Sends a magic link if the email belongs to an active administrator. Behaves
     * identically otherwise, so the endpoint cannot be used to discover admins.
     */
    @Transactional
    public void requestLink(String email) {
        Optional<Administrator> admin = administratorRepository.findByEmailIgnoreCase(normalize(email))
                .filter(Administrator::isActive);
        if (admin.isEmpty()) {
            log.info("Magic link requested for an unknown or inactive email");
            return;
        }
        String token = SecureTokens.generate();
        Instant now = clock.instant();
        loginTokenRepository.save(new AdminLoginToken(admin.get(), SecureTokens.sha256(token), now,
                now.plus(properties.admin().magicLinkTtl())));
        String link = UriComponentsBuilder.fromUriString(properties.frontendUrl())
                .path("/admin/verify")
                .queryParam("token", token)
                .build()
                .toUriString();
        try {
            magicLinkSender.send(admin.get().getEmail(), link);
        } catch (RuntimeException e) {
            log.error("Failed to send magic link", e);
        }
    }

    /** Redeems a magic-link token (single use) and opens a session. */
    @Transactional
    public IssuedSession verify(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidLoginTokenException();
        }
        String hash = SecureTokens.sha256(token);
        Instant now = clock.instant();
        if (loginTokenRepository.consume(hash, now) != 1) {
            throw new InvalidLoginTokenException();
        }
        Administrator admin = loginTokenRepository.findByTokenHash(hash)
                .map(AdminLoginToken::getAdministrator)
                .filter(Administrator::isActive)
                .orElseThrow(InvalidLoginTokenException::new);

        String sessionToken = SecureTokens.generate();
        Instant expiresAt = now.plus(properties.admin().sessionTtl());
        sessionRepository.save(new AdminSession(admin, SecureTokens.sha256(sessionToken), now, expiresAt));
        return new IssuedSession(sessionToken, expiresAt, admin.getEmail());
    }

    @Transactional(readOnly = true)
    public Optional<AdminPrincipal> authenticate(String sessionToken) {
        Instant now = clock.instant();
        return sessionRepository.findByTokenHash(SecureTokens.sha256(sessionToken))
                .filter(s -> s.isValidAt(now))
                .map(s -> new AdminPrincipal(s.getAdministrator().getId(), s.getAdministrator().getEmail()));
    }

    @Transactional
    public void logout(String sessionToken) {
        sessionRepository.findByTokenHash(SecureTokens.sha256(sessionToken))
                .ifPresent(s -> s.revoke(clock.instant()));
    }

    static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
