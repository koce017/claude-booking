package com.booking.appointment;

import java.time.Instant;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.booking.appointment.AppointmentDtos.AdminAppointmentResponse;
import com.booking.appointment.AppointmentDtos.AdminBookingRequest;
import com.booking.appointment.AppointmentDtos.NoteRequest;
import com.booking.appointment.AppointmentDtos.PageResponse;
import com.booking.appointment.AppointmentDtos.RescheduleRequest;
import com.booking.appointment.AppointmentQueryService.AppointmentFilter;
import com.booking.company.CompanySettingsResolver;
import com.booking.config.OpenApiConfig;

@RestController
@RequestMapping("/api/admin/appointments")
@Tag(name = "Admin: appointments")
@SecurityRequirement(name = OpenApiConfig.ADMIN_SECURITY_SCHEME)
public class AdminAppointmentController {

    private final AppointmentQueryService queryService;
    private final AppointmentApprovalService approvalService;
    private final AppointmentManagementService managementService;
    private final AppointmentBookingService bookingService;
    private final CompanySettingsResolver settingsResolver;

    public AdminAppointmentController(AppointmentQueryService queryService,
                                      AppointmentApprovalService approvalService,
                                      AppointmentManagementService managementService,
                                      AppointmentBookingService bookingService,
                                      CompanySettingsResolver settingsResolver) {
        this.queryService = queryService;
        this.approvalService = approvalService;
        this.managementService = managementService;
        this.bookingService = bookingService;
        this.settingsResolver = settingsResolver;
    }

    @GetMapping
    @Operation(summary = "List appointments",
            description = "Filter by company, status and start time range [from, to). Sorted by start time. "
                    + "The admin calendar uses this with a from/to range.")
    public PageResponse<AdminAppointmentResponse> list(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(500) int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("startAt").ascending().and(Sort.by("id")));
        return PageResponse.from(queryService.search(new AppointmentFilter(companyId, status, from, to), pageable)
                .map(this::toResponse));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an appointment including customer information")
    public AdminAppointmentResponse get(@PathVariable Long id) {
        return toResponse(queryService.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Enter an appointment (e.g. booked by phone)",
            description = "Same slot and capacity rules as a public booking, but only the customer's name is "
                    + "required. The appointment is APPROVED immediately and delivered to the company.")
    public AdminAppointmentResponse create(@Valid @RequestBody AdminBookingRequest request) {
        return reloaded(bookingService.bookByAdmin(request.companyId(), request.start().toInstant(),
                request.customer().toSubmitted(), request.adminNote()));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a pending appointment",
            description = "409 if it is not PENDING, already in the past, or approval would exceed capacity.")
    public AdminAppointmentResponse approve(@PathVariable Long id) {
        return reloaded(approvalService.approve(id));
    }

    @PostMapping("/{id}/deny")
    @Operation(summary = "Deny a pending appointment", description = "Frees the capacity. 409 if it is not PENDING.")
    public AdminAppointmentResponse deny(@PathVariable Long id) {
        return reloaded(approvalService.deny(id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a pending or approved appointment", description = "Frees the capacity.")
    public AdminAppointmentResponse cancel(@PathVariable Long id) {
        return reloaded(managementService.cancel(id));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Mark an approved appointment as completed", description = "Only once it has started.")
    public AdminAppointmentResponse complete(@PathVariable Long id) {
        return toResponse(managementService.complete(id));
    }

    @PostMapping("/{id}/no-show")
    @Operation(summary = "Mark an approved appointment as a no-show", description = "Only once it has started.")
    public AdminAppointmentResponse noShow(@PathVariable Long id) {
        return toResponse(managementService.markNoShow(id));
    }

    @PostMapping("/{id}/reschedule")
    @Operation(summary = "Move a pending or approved appointment to another slot",
            description = "The new start must be a bookable slot of the same company with free capacity.")
    public AdminAppointmentResponse reschedule(@PathVariable Long id, @Valid @RequestBody RescheduleRequest request) {
        return reloaded(managementService.reschedule(id, request.start().toInstant()));
    }

    @PutMapping("/{id}/note")
    @Operation(summary = "Set the internal admin note")
    public AdminAppointmentResponse updateNote(@PathVariable Long id, @Valid @RequestBody NoteRequest request) {
        return toResponse(managementService.updateNote(id, request.adminNote()));
    }

    /**
     * Re-reads the appointment after a change whose after-commit side effects may
     * have updated it (e.g. the delivery time set once the company was emailed).
     */
    private AdminAppointmentResponse reloaded(Appointment appointment) {
        return toResponse(queryService.get(appointment.getId()));
    }

    private AdminAppointmentResponse toResponse(Appointment appointment) {
        return AdminAppointmentResponse.from(appointment,
                settingsResolver.resolve(appointment.getCompany()).zone().getId());
    }
}
