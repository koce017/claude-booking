package com.booking.mail;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.booking.config.BookingProperties;

/**
 * Loads email texts from template files and fills in their placeholders.
 *
 * <p>Each template is a UTF-8 text file named {@code <name>.txt}. The first line is
 * {@code Subject: ...}; everything after the following blank line is the body.
 * Placeholders are written as {@code {name}}; unknown placeholders are left as they are.
 *
 * <p>The built-in Serbian texts live in {@code src/main/resources/mail-templates/}.
 * If {@code booking.mail.templates-dir} is set, a file with the same name in that
 * directory is used instead, so the texts can be changed without rebuilding. Files
 * are read on every send, so edits apply immediately.
 */
@Component
public class MailTemplates {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([a-zA-Z0-9]+)}");
    private static final String SUBJECT_PREFIX = "Subject:";

    private final Path overrideDir;

    public MailTemplates(BookingProperties properties) {
        String dir = properties.mail().templatesDir();
        this.overrideDir = dir == null || dir.isBlank() ? null : Path.of(dir);
    }

    public OutgoingMail render(String template, String to, Map<String, String> values) {
        String text = load(template).replace("\r\n", "\n");
        int firstLineEnd = text.indexOf('\n');
        String firstLine = firstLineEnd < 0 ? text : text.substring(0, firstLineEnd);
        if (!firstLine.startsWith(SUBJECT_PREFIX)) {
            throw new IllegalStateException("Mail template " + template + " must start with '" + SUBJECT_PREFIX + "'");
        }
        String subject = fill(firstLine.substring(SUBJECT_PREFIX.length()).trim(), values);
        String body = firstLineEnd < 0 ? "" : text.substring(firstLineEnd + 1).strip();
        return new OutgoingMail(to, subject, fill(body, values) + "\n");
    }

    static String fill(String text, Map<String, String> values) {
        Matcher m = PLACEHOLDER.matcher(text);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String value = values.get(m.group(1));
            m.appendReplacement(out, Matcher.quoteReplacement(value != null ? value : m.group()));
        }
        m.appendTail(out);
        return out.toString();
    }

    private String load(String template) {
        String fileName = template + ".txt";
        try {
            if (overrideDir != null) {
                Path file = overrideDir.resolve(fileName);
                if (Files.isRegularFile(file)) {
                    return Files.readString(file, StandardCharsets.UTF_8);
                }
            }
            try (InputStream in = MailTemplates.class.getResourceAsStream("/mail-templates/" + fileName)) {
                if (in == null) {
                    throw new IllegalStateException("Mail template not found: " + fileName);
                }
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read mail template " + fileName, e);
        }
    }
}
