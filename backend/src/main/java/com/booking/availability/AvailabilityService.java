package com.booking.availability;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.booking.appointment.AppointmentRepository;
import com.booking.appointment.AppointmentRepository.OccupancyView;
import com.booking.appointment.AppointmentStatus;
import com.booking.availability.AvailabilityDtos.AvailabilityResponse;
import com.booking.availability.AvailabilityDtos.DayAvailability;
import com.booking.availability.AvailabilityDtos.SlotView;
import com.booking.common.error.BadRequestException;
import com.booking.company.Company;
import com.booking.company.CompanyService;
import com.booking.company.CompanySettingsResolver;
import com.booking.company.EffectiveCompanySettings;
import com.booking.config.BookingProperties;
import com.booking.schedule.ScheduleService;
import com.booking.schedule.TimeRange;

/**
 * The authoritative availability calculation. Both the public calendar and booking
 * validation use it, so the frontend never has to re-implement any rule.
 */
@Service
public class AvailabilityService {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private final CompanyService companyService;
    private final CompanySettingsResolver settingsResolver;
    private final ScheduleService scheduleService;
    private final BookingWindowService bookingWindowService;
    private final SlotGenerator slotGenerator;
    private final AppointmentRepository appointmentRepository;
    private final Clock clock;
    private final int maxRangeDays;

    public AvailabilityService(CompanyService companyService,
                               CompanySettingsResolver settingsResolver,
                               ScheduleService scheduleService,
                               BookingWindowService bookingWindowService,
                               SlotGenerator slotGenerator,
                               AppointmentRepository appointmentRepository,
                               Clock clock,
                               BookingProperties properties) {
        this.companyService = companyService;
        this.settingsResolver = settingsResolver;
        this.scheduleService = scheduleService;
        this.bookingWindowService = bookingWindowService;
        this.slotGenerator = slotGenerator;
        this.appointmentRepository = appointmentRepository;
        this.clock = clock;
        this.maxRangeDays = properties.maxAvailabilityRangeDays();
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse getAvailability(Long companyId, LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new BadRequestException("INVALID_RANGE", "'to' must not be before 'from'");
        }
        if (ChronoUnit.DAYS.between(from, to) + 1 > maxRangeDays) {
            throw new BadRequestException("RANGE_TOO_LARGE", "At most " + maxRangeDays + " days can be requested at once");
        }
        Company company = companyService.getActive(companyId);
        EffectiveCompanySettings settings = settingsResolver.resolve(company);
        ZoneId zone = settings.zone();
        BookingWindow window = bookingWindowService.windowFor(settings);
        Instant now = clock.instant();

        Map<LocalDate, List<TimeRange>> periodsByDate = scheduleService.workingPeriods(company, from, to);
        List<OccupancyView> occupancy = appointmentRepository.findOccupancy(companyId,
                AppointmentStatus.CAPACITY_CONSUMING,
                from.atStartOfDay(zone).toInstant(),
                to.plusDays(1).atStartOfDay(zone).toInstant());

        List<DayAvailability> days = new ArrayList<>();
        periodsByDate.forEach((date, periods) -> {
            List<SlotView> views = new ArrayList<>();
            for (Slot slot : slotGenerator.generate(date, periods, settings.appointmentDuration(), zone)) {
                views.add(slotView(slot, settings, window, now, occupancy));
            }
            views.addAll(nonWorkingBlocks(date, periods, zone));
            views.sort(Comparator.comparing(SlotView::start));
            days.add(new DayAvailability(date, !periods.isEmpty(), views));
        });

        return new AvailabilityResponse(company.getId(), zone.getId(),
                (int) settings.appointmentDuration().toMinutes(), window, days);
    }

    /**
     * Validates that {@code start} is the start of a bookable slot for this company
     * (aligned with the schedule, not in the past, inside the booking window) and
     * returns that slot. Capacity is not checked here; that happens under lock in
     * the booking service.
     */
    public Slot requireBookableSlot(Company company, EffectiveCompanySettings settings, Instant start) {
        if (!start.isAfter(clock.instant())) {
            throw new BadRequestException("SLOT_IN_PAST", "The requested time is in the past");
        }
        ZoneId zone = settings.zone();
        LocalDate localDate = LocalDate.ofInstant(start, zone);
        if (!bookingWindowService.windowFor(settings).contains(localDate)) {
            throw new BadRequestException("OUTSIDE_BOOKING_WINDOW", "The requested date is outside the booking window");
        }
        List<TimeRange> periods = scheduleService.workingPeriods(company, localDate);
        return slotGenerator.generate(localDate, periods, settings.appointmentDuration(), zone).stream()
                .filter(s -> s.start().equals(start))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("NOT_A_WORKING_SLOT",
                        "The requested time is not a valid appointment slot within working hours"));
    }

    private static SlotView slotView(Slot slot, EffectiveCompanySettings settings, BookingWindow window,
                                     Instant now, List<OccupancyView> occupancy) {
        int consumed = 0;
        boolean pending = false;
        for (OccupancyView o : occupancy) {
            if (o.getStartAt().isBefore(slot.end()) && o.getEndAt().isAfter(slot.start())) {
                consumed++;
                pending |= o.getStatus() == AppointmentStatus.PENDING;
            }
        }

        SlotStatus status;
        UnavailableReason reason = null;
        if (!slot.start().isAfter(now)) {
            status = SlotStatus.UNAVAILABLE;
            reason = UnavailableReason.PAST;
        } else if (!window.contains(slot.localStart().toLocalDate())) {
            status = SlotStatus.UNAVAILABLE;
            reason = UnavailableReason.OUTSIDE_BOOKING_WINDOW;
        } else if (consumed >= settings.maxConcurrentAppointments()) {
            status = SlotStatus.UNAVAILABLE;
            reason = UnavailableReason.FULLY_BOOKED;
        } else if (pending) {
            status = SlotStatus.PENDING;
        } else {
            status = SlotStatus.AVAILABLE;
        }
        boolean bookable = status == SlotStatus.AVAILABLE || status == SlotStatus.PENDING;
        return new SlotView(slot.start(), slot.end(), slot.localStart().format(HH_MM),
                slot.localEnd().format(HH_MM), status, pending, bookable, reason);
    }

    /** NON_WORKING blocks covering the parts of the day outside the working periods. */
    private static List<SlotView> nonWorkingBlocks(LocalDate date, List<TimeRange> periods, ZoneId zone) {
        List<SlotView> blocks = new ArrayList<>();
        LocalDateTime cursor = date.atStartOfDay();
        for (TimeRange p : periods) {
            LocalDateTime periodStart = date.atTime(p.start());
            if (periodStart.isAfter(cursor)) {
                blocks.add(nonWorking(cursor, periodStart, date, zone));
            }
            cursor = date.atTime(p.end());
        }
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        if (cursor.isBefore(endOfDay)) {
            blocks.add(nonWorking(cursor, endOfDay, date, zone));
        }
        return blocks;
    }

    private static SlotView nonWorking(LocalDateTime from, LocalDateTime to, LocalDate date, ZoneId zone) {
        String localEnd = to.toLocalDate().isAfter(date) && to.toLocalTime().equals(LocalTime.MIDNIGHT)
                ? "24:00" : to.format(HH_MM);
        return new SlotView(from.atZone(zone).toInstant(), to.atZone(zone).toInstant(),
                from.format(HH_MM), localEnd, SlotStatus.NON_WORKING, false, false, null);
    }
}
