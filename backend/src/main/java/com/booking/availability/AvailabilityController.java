package com.booking.availability;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.booking.availability.AvailabilityDtos.AvailabilityResponse;
import com.booking.common.error.BadRequestException;

@RestController
@RequestMapping("/api/companies/{companyId}/availability")
@Tag(name = "Public: availability")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping
    @Operation(summary = "Get slot availability for a date or date range",
            description = """
                    Dates are company-local (see 'timezone' in the response). Pass 'date' for one day, or \
                    'from' and 'to' (inclusive, max 62 days). Each day lists appointment slots and NON_WORKING \
                    blocks. Only aggregate state is returned, never customer data.""")
    public AvailabilityResponse get(
            @PathVariable Long companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (date != null) {
            return availabilityService.getAvailability(companyId, date, date);
        }
        if (from == null || to == null) {
            throw new BadRequestException("MISSING_DATE",
                    "Provide either 'date' or both 'from' and 'to'");
        }
        return availabilityService.getAvailability(companyId, from, to);
    }
}
