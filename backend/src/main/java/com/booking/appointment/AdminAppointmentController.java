package com.booking.appointment;

import java.time.Instant;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.booking.appointment.AppointmentDtos.AdminAppointmentResponse;
import com.booking.appointment.AppointmentDtos.PageResponse;
import com.booking.appointment.AppointmentQueryService.AppointmentFilter;
import com.booking.config.OpenApiConfig;

@RestController
@RequestMapping("/api/admin/appointments")
@Tag(name = "Admin: appointments")
@SecurityRequirement(name = OpenApiConfig.ADMIN_SECURITY_SCHEME)
public class AdminAppointmentController {

    private final AppointmentQueryService queryService;
    private final AppointmentApprovalService approvalService;

    public AdminAppointmentController(AppointmentQueryService queryService, AppointmentApprovalService approvalService) {
        this.queryService = queryService;
        this.approvalService = approvalService;
    }

    @GetMapping
    @Operation(summary = "List appointments", description = "Filter by company, status and start time range [from, to). Sorted by start time.")
    public PageResponse<AdminAppointmentResponse> list(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("startAt").ascending().and(Sort.by("id")));
        return PageResponse.from(queryService.search(new AppointmentFilter(companyId, status, from, to), pageable)
                .map(AdminAppointmentResponse::from));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an appointment including customer information")
    public AdminAppointmentResponse get(@PathVariable Long id) {
        return AdminAppointmentResponse.from(queryService.get(id));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a pending appointment",
            description = "409 if it is not PENDING, already in the past, or approval would exceed capacity.")
    public AdminAppointmentResponse approve(@PathVariable Long id) {
        return AdminAppointmentResponse.from(approvalService.approve(id));
    }

    @PostMapping("/{id}/deny")
    @Operation(summary = "Deny a pending appointment", description = "Frees the capacity. 409 if it is not PENDING.")
    public AdminAppointmentResponse deny(@PathVariable Long id) {
        return AdminAppointmentResponse.from(approvalService.deny(id));
    }
}
