package com.booking.schedule;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.booking.common.error.BadRequestException;
import com.booking.config.OpenApiConfig;
import com.booking.schedule.AdminScheduleController.PeriodDto;
import com.booking.schedule.WeeklyScheduleService.WeeklySchedule;

@RestController
@RequestMapping("/api/admin/companies/{companyId}/schedule/weekly")
@Tag(name = "Admin: weekly schedule")
@SecurityRequirement(name = OpenApiConfig.ADMIN_SECURITY_SCHEME)
public class AdminWeeklyScheduleController {

    private final WeeklyScheduleService weeklyScheduleService;

    public AdminWeeklyScheduleController(WeeklyScheduleService weeklyScheduleService) {
        this.weeklyScheduleService = weeklyScheduleService;
    }

    public record DayDto(@NotNull DayOfWeek day, @Valid @Size(max = 10) List<PeriodDto> periods) {
    }

    public record WeeklyRequest(@NotNull @Valid @Size(max = 7) List<DayDto> days) {
    }

    /**
     * @param custom false while the company uses the default schedule
     * @param days   all seven days, Monday first; a day without periods is closed
     */
    public record WeeklyResponse(boolean custom, List<DayDto> days) {

        static WeeklyResponse from(WeeklySchedule schedule) {
            List<DayDto> days = new ArrayList<>();
            for (DayOfWeek day : DayOfWeek.values()) {
                List<PeriodDto> periods = schedule.days().getOrDefault(day, List.of()).stream()
                        .map(r -> new PeriodDto(r.start(), r.end())).toList();
                days.add(new DayDto(day, periods));
            }
            return new WeeklyResponse(schedule.custom(), days);
        }
    }

    @GetMapping
    @Operation(summary = "Get the weekly working hours in effect")
    public WeeklyResponse get(@PathVariable Long companyId) {
        return WeeklyResponse.from(weeklyScheduleService.get(companyId));
    }

    @PutMapping
    @Operation(summary = "Replace the weekly working hours",
            description = "Several periods per day are allowed. Days that are missing or have no periods are closed.")
    public WeeklyResponse replace(@PathVariable Long companyId, @Valid @RequestBody WeeklyRequest request) {
        Map<DayOfWeek, List<TimeRange>> days = new EnumMap<>(DayOfWeek.class);
        for (DayDto day : request.days()) {
            if (days.containsKey(day.day())) {
                throw new BadRequestException("DUPLICATE_DAY", "Each day may appear only once");
            }
            List<PeriodDto> periods = day.periods() == null ? List.of() : day.periods();
            days.put(day.day(), periods.stream().map(AdminScheduleController::toRange).toList());
        }
        return WeeklyResponse.from(weeklyScheduleService.replace(companyId, days));
    }

    @DeleteMapping
    @Operation(summary = "Go back to the default schedule (Mon-Fri 09:00-17:00)")
    public WeeklyResponse reset(@PathVariable Long companyId) {
        return WeeklyResponse.from(weeklyScheduleService.reset(companyId));
    }
}
