package com.booking.schedule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.booking.company.Company;

/**
 * Answers "when does this company work on a given date?". Specific-date overrides
 * take precedence over the weekly schedule. All other code (availability, booking
 * validation) goes through this service rather than reading schedule tables.
 */
@Service
public class ScheduleService {

    /** Used for companies that have no weekly schedule configured: Mon-Fri 09:00-17:00. */
    public static final Map<DayOfWeek, List<TimeRange>> DEFAULT_WEEKLY_SCHEDULE = defaultWeeklySchedule();

    private final WeeklySchedulePeriodRepository weeklyRepository;
    private final ScheduleOverrideRepository overrideRepository;

    public ScheduleService(WeeklySchedulePeriodRepository weeklyRepository,
                           ScheduleOverrideRepository overrideRepository) {
        this.weeklyRepository = weeklyRepository;
        this.overrideRepository = overrideRepository;
    }

    @Transactional(readOnly = true)
    public List<TimeRange> workingPeriods(Company company, LocalDate date) {
        return workingPeriods(company, date, date).get(date);
    }

    /**
     * Working periods for every date in [from, to], in date order. A date with an
     * empty list is a non-working day.
     */
    @Transactional(readOnly = true)
    public Map<LocalDate, List<TimeRange>> workingPeriods(Company company, LocalDate from, LocalDate to) {
        Map<DayOfWeek, List<TimeRange>> weekly = weeklySchedule(company);
        Map<LocalDate, ScheduleOverride> overrides = new LinkedHashMap<>();
        for (ScheduleOverride o : overrideRepository.findByCompanyIdAndDateBetweenOrderByDate(company.getId(), from, to)) {
            overrides.put(o.getDate(), o);
        }

        Map<LocalDate, List<TimeRange>> result = new LinkedHashMap<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            ScheduleOverride override = overrides.get(d);
            List<TimeRange> periods = override != null
                    ? override.getTimeRanges()
                    : weekly.getOrDefault(d.getDayOfWeek(), List.of());
            result.put(d, sorted(periods));
        }
        return result;
    }

    private Map<DayOfWeek, List<TimeRange>> weeklySchedule(Company company) {
        List<WeeklySchedulePeriod> rows = weeklyRepository.findByCompanyId(company.getId());
        if (rows.isEmpty()) {
            return DEFAULT_WEEKLY_SCHEDULE;
        }
        Map<DayOfWeek, List<TimeRange>> weekly = new EnumMap<>(DayOfWeek.class);
        for (WeeklySchedulePeriod row : rows) {
            weekly.computeIfAbsent(row.getDayOfWeek(), k -> new ArrayList<>()).add(row.toTimeRange());
        }
        return weekly;
    }

    private static List<TimeRange> sorted(List<TimeRange> periods) {
        return periods.stream().sorted(Comparator.comparing(TimeRange::start)).toList();
    }

    private static Map<DayOfWeek, List<TimeRange>> defaultWeeklySchedule() {
        Map<DayOfWeek, List<TimeRange>> map = new EnumMap<>(DayOfWeek.class);
        TimeRange nineToFive = new TimeRange(LocalTime.of(9, 0), LocalTime.of(17, 0));
        for (DayOfWeek d : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)) {
            map.put(d, List.of(nineToFive));
        }
        return Collections.unmodifiableMap(map);
    }
}
