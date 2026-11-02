package com.booking.mail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.booking.config.BookingProperties;

import static org.assertj.core.api.Assertions.assertThat;

class MailTemplatesTest {

    private static MailTemplates templates(String dir) {
        return new MailTemplates(new BookingProperties(ZoneId.of("Europe/Belgrade"), "http://localhost:5173", List.of(),
                62, new BookingProperties.Admin(List.of(), Duration.ofMinutes(15), Duration.ofHours(12)),
                new BookingProperties.Mail("log", "no-reply@example.com", dir),
                new BookingProperties.Notifications(true, true), new BookingProperties.Fulfillment("email")));
    }

    @Test
    void rendersBuiltInTemplate() {
        OutgoingMail mail = templates(null).render("admin-login-link", "a@example.com",
                Map.of("link", "http://x/verify?token=abc", "minutes", "15"));
        assertThat(mail.to()).isEqualTo("a@example.com");
        assertThat(mail.subject()).isEqualTo("Link za prijavu u administraciju");
        assertThat(mail.body()).contains("http://x/verify?token=abc").contains("15 minuta");
    }

    @Test
    void fileInTemplatesDirTakesPrecedence(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("admin-login-link.txt"), "Subject: Prijava ({minutes})\n\nKliknite: {link} {unknown}\n");
        OutgoingMail mail = templates(dir.toString()).render("admin-login-link", "a@example.com",
                Map.of("link", "L", "minutes", "5"));
        assertThat(mail.subject()).isEqualTo("Prijava (5)");
        assertThat(mail.body()).isEqualTo("Kliknite: L {unknown}\n");

        // Templates missing from the folder fall back to the built-in ones.
        assertThat(templates(dir.toString()).render("customer-denied", "c@example.com", Map.of()).subject())
                .startsWith("Zahtev za termin nije prihvaćen");
    }

    @Test
    void replacementValuesAreInsertedLiterally() {
        assertThat(MailTemplates.fill("Cena: {x}", Map.of("x", "$1 \\ {y}"))).isEqualTo("Cena: $1 \\ {y}");
    }
}
