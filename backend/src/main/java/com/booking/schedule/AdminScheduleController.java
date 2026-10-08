package com.booking.schedule;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.booking.common.error.BadRequestException;
import com.booking.config.OpenApiConfig;

@RestController
@RequestMapping("/api/admin/companies/{companyId}/schedule/overrides")
@Tag(name = "Admin: schedule overrides")
@SecurityRequirement(name = OpenApiConfig.ADMIN_SECURITY_SCHEME)
public class AdminScheduleController {

    private final ScheduleOverrideService overrideService;

    public AdminScheduleController(ScheduleOverrideService overrideService) {
        this.overrideService = overrideService;
    }

    public record PeriodDto(@NotNull LocalTime start, @NotNull LocalTime end) {
    }

    /** An empty or missing period list means the company is closed on that date. */
    public record OverrideRequest(@Valid @Size(max = 10) List<PeriodDto> periods, @Size(max = 200) String note) {
    }

    public record OverrideResponse(LocalDate date, boolean closed, List<PeriodDto> periods, String note) {

        static OverrideResponse from(ScheduleOverride o) {
            List<PeriodDto> periods = o.getTimeRanges().stream()
                    .map(r -> new PeriodDto(r.start(), r.end())).toList();
            return new OverrideResponse(o.getDate(), o.isClosed(), periods, o.getNote());
        }
    }

    @GetMapping
    @Operation(summary = "List schedule overrides in a date range")
    public List<OverrideResponse> list(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return overrideService.list(companyId, from, to).stream().map(OverrideResponse::from).toList();
    }

    @PutMapping("/{date}")
    @Operation(summary = "Create or replace the override for a date",
            description = "Overrides take precedence over the weekly schedule. No periods = closed all day.")
    public OverrideResponse set(
            @PathVariable Long companyId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody(required = false) OverrideRequest request) {
        List<TimeRange> ranges = request == null || request.periods() == null ? List.of()
                : request.periods().stream().map(AdminScheduleController::toRange).toList();
        String note = request == null ? null : request.note();
        return OverrideResponse.from(overrideService.set(companyId, date, ranges, note));
    }

    @DeleteMapping("/{date}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove the override for a date (back to the weekly schedule)")
    public void remove(@PathVariable Long companyId,
                       @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        overrideService.remove(companyId, date);
    }

    private static TimeRange toRange(PeriodDto p) {
        if (!p.end().isAfter(p.start())) {
            throw new BadRequestException("INVALID_PERIOD", "Period end must be after start");
        }
        return new TimeRange(p.start(), p.end());
    }
}
