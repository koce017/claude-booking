package com.booking.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.booking.config.BookingProperties;

/**
 * Creates administrators listed in booking.admin.bootstrap-emails
 * (env ADMIN_BOOTSTRAP_EMAILS) if they do not exist yet. There is no admin
 * registration endpoint.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AdministratorRepository administratorRepository;
    private final BookingProperties properties;

    public AdminBootstrap(AdministratorRepository administratorRepository, BookingProperties properties) {
        this.administratorRepository = administratorRepository;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (String raw : properties.admin().bootstrapEmails()) {
            String email = AdminAuthService.normalize(raw);
            if (email.isEmpty() || administratorRepository.findByEmailIgnoreCase(email).isPresent()) {
                continue;
            }
            administratorRepository.save(new Administrator(email));
            log.info("Created bootstrap administrator {}", email);
        }
    }
}
