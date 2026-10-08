package com.booking.schedule;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.booking.common.error.BadRequestException;
import com.booking.common.error.NotFoundException;
import com.booking.company.Company;
import com.booking.company.CompanyService;

/** Admin management of specific-date schedule overrides (e.g. holidays). */
@Service
public class ScheduleOverrideService {

    private final ScheduleOverrideRepository overrideRepository;
    private final CompanyService companyService;

    public ScheduleOverrideService(ScheduleOverrideRepository overrideRepository, CompanyService companyService) {
        this.overrideRepository = overrideRepository;
        this.companyService = companyService;
    }

    @Transactional(readOnly = true)
    public List<ScheduleOverride> list(Long companyId, LocalDate from, LocalDate to) {
        companyService.get(companyId);
        return overrideRepository.findByCompanyIdAndDateBetweenOrderByDate(companyId, from, to);
    }

    /**
     * Creates or replaces the override for a date. An empty period list marks the
     * date as closed. Existing appointments on that date are not touched.
     */
    @Transactional
    public ScheduleOverride set(Long companyId, LocalDate date, List<TimeRange> periods, String note) {
        validateNoOverlap(periods);
        Company company = companyService.get(companyId);
        ScheduleOverride override = overrideRepository.findByCompanyIdAndDate(companyId, date)
                .orElseGet(() -> new ScheduleOverride(company, date));
        override.replacePeriods(periods);
        override.setNote(note);
        return overrideRepository.save(override);
    }

    @Transactional
    public void remove(Long companyId, LocalDate date) {
        ScheduleOverride override = overrideRepository.findByCompanyIdAndDate(companyId, date)
                .orElseThrow(() -> new NotFoundException("Schedule override", date));
        overrideRepository.delete(override);
    }

    private static void validateNoOverlap(List<TimeRange> periods) {
        for (int i = 0; i < periods.size(); i++) {
            for (int j = i + 1; j < periods.size(); j++) {
                if (periods.get(i).overlaps(periods.get(j))) {
                    throw new BadRequestException("OVERLAPPING_PERIODS", "Working periods must not overlap");
                }
            }
        }
    }
}
