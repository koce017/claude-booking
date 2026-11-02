package com.booking.mail;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.booking.appointment.Appointment;
import com.booking.appointment.CustomerInfo;
import com.booking.company.CompanySettingsResolver;
import com.booking.config.BookingProperties;

/**
 * Builds the placeholder values available in appointment email templates. Dates and
 * times are in the company's time zone, formatted in Serbian (Latin script).
 *
 * <p>Placeholders: {appointmentId}, {company}, {companyAddress}, {companyPhone},
 * {date}, {time}, {endTime}, {previousDate}, {previousTime}, {customerName},
 * {customerPhone}, {customerEmail}, {customerAddress}, {customerAnswers},
 * {adminNote}, {adminLink}.
 */
@Component
public class AppointmentMailData {

    public static final Locale SERBIAN_LATIN = Locale.forLanguageTag("sr-Latn-RS");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEEE, d.M.yyyy.", SERBIAN_LATIN);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", SERBIAN_LATIN);
    private static final String NOT_GIVEN = "-";

    private final CompanySettingsResolver settingsResolver;
    private final String frontendUrl;

    public AppointmentMailData(CompanySettingsResolver settingsResolver, BookingProperties properties) {
        this.settingsResolver = settingsResolver;
        this.frontendUrl = properties.frontendUrl();
    }

    public Map<String, String> of(Appointment appointment) {
        return of(appointment, null);
    }

    public Map<String, String> of(Appointment appointment, Instant previousStart) {
        ZoneId zone = settingsResolver.resolve(appointment.getCompany()).zone();
        CustomerInfo customer = appointment.getCustomer();
        Map<String, String> values = new LinkedHashMap<>();
        values.put("appointmentId", String.valueOf(appointment.getId()));
        values.put("company", appointment.getCompany().getName());
        values.put("companyAddress", orDash(appointment.getCompany().getAddress()));
        values.put("companyPhone", orDash(appointment.getCompany().getPhone()));
        values.put("date", DATE.format(appointment.getStartAt().atZone(zone)));
        values.put("time", TIME.format(appointment.getStartAt().atZone(zone)));
        values.put("endTime", TIME.format(appointment.getEndAt().atZone(zone)));
        if (previousStart != null) {
            values.put("previousDate", DATE.format(previousStart.atZone(zone)));
            values.put("previousTime", TIME.format(previousStart.atZone(zone)));
        }
        values.put("customerName", customer.name());
        values.put("customerPhone", orDash(customer.phone()));
        values.put("customerEmail", orDash(customer.email()));
        values.put("customerAddress", orDash(customer.address()));
        values.put("customerAnswers", customer.answers().isEmpty() ? NOT_GIVEN
                : customer.answers().stream()
                        .map(a -> a.question() + ": " + a.answer())
                        .collect(Collectors.joining("\n")));
        values.put("adminNote", orDash(appointment.getAdminNote()));
        values.put("adminLink", frontendUrl + "/admin/appointments?id=" + appointment.getId());
        return values;
    }

    private static String orDash(String value) {
        return value == null || value.isBlank() ? NOT_GIVEN : value;
    }
}
