package com.booking.appointment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.booking.appointment.AppointmentDtos.BookingRequest;
import com.booking.appointment.AppointmentDtos.BookingResponse;

@RestController
@RequestMapping("/api/companies/{companyId}/appointments")
@Tag(name = "Public: appointments")
public class PublicAppointmentController {

    private final AppointmentBookingService bookingService;

    public PublicAppointmentController(AppointmentBookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Request an appointment",
            description = "Creates a PENDING appointment and reserves capacity. 'start' must be the start of a slot "
                    + "returned by the availability endpoint; the end is derived from the company's duration.")
    @ApiResponse(responseCode = "201", description = "Request created (PENDING)")
    @ApiResponse(responseCode = "400", description = "Invalid input, not a working slot, in the past or outside the booking window")
    @ApiResponse(responseCode = "404", description = "Company not found or removed")
    @ApiResponse(responseCode = "409", description = "Slot is no longer available (capacity used up)")
    public BookingResponse book(@PathVariable Long companyId, @Valid @RequestBody BookingRequest request) {
        Appointment appointment = bookingService.book(companyId, request.start().toInstant(),
                request.customer().toCustomerInfo());
        return BookingResponse.from(appointment);
    }
}
