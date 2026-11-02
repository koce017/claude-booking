package com.booking.schedule;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.booking.common.error.NotFoundException;
import com.booking.company.Company;
import com.booking.company.CompanyRepository;

/** Admin management of a company's normal weekly working hours. */
@Service
public class WeeklyScheduleService {

    private final WeeklySchedulePeriodRepository weeklyRepository;
    private final CompanyRepository companyRepository;
    private final ScheduleService scheduleService;

    public WeeklyScheduleService(WeeklySchedulePeriodRepository weeklyRepository,
                                 CompanyRepository companyRepository,
                                 ScheduleService scheduleService) {
        this.weeklyRepository = weeklyRepository;
        this.companyRepository = companyRepository;
        this.scheduleService = scheduleService;
    }

    public record WeeklySchedule(boolean custom, Map<DayOfWeek, List<TimeRange>> days) {
    }

    @Transactional(readOnly = true)
    public WeeklySchedule get(Long companyId) {
        Company company = load(companyId);
        return new WeeklySchedule(company.hasCustomWeeklySchedule(), scheduleService.weeklySchedule(company));
    }

    /**
     * Replaces the whole weekly schedule. Days missing from the map are closed.
     * Existing appointments are not touched.
     */
    @Transactional
    public WeeklySchedule replace(Long companyId, Map<DayOfWeek, List<TimeRange>> days) {
        days.values().forEach(TimeRanges::requireNoOverlap);
        Company company = companyRepository.findByIdForUpdate(companyId)
                .orElseThrow(() -> new NotFoundException("Company", companyId));
        weeklyRepository.deleteByCompanyId(companyId);
        days.forEach((day, periods) -> periods.forEach(p ->
                weeklyRepository.save(new WeeklySchedulePeriod(company, day, p.start(), p.end()))));
        company.setCustomWeeklySchedule(true);
        return new WeeklySchedule(true, scheduleService.weeklySchedule(company));
    }

    /** Removes the saved schedule; the default (Mon-Fri 09:00-17:00) applies again. */
    @Transactional
    public WeeklySchedule reset(Long companyId) {
        Company company = companyRepository.findByIdForUpdate(companyId)
                .orElseThrow(() -> new NotFoundException("Company", companyId));
        weeklyRepository.deleteByCompanyId(companyId);
        company.setCustomWeeklySchedule(false);
        return new WeeklySchedule(false, ScheduleService.DEFAULT_WEEKLY_SCHEDULE);
    }

    private Company load(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Company", companyId));
    }
}
