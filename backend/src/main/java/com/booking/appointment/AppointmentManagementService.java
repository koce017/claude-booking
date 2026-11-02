package com.booking.appointment;

import java.time.Clock;
import java.time.Instant;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.booking.availability.AvailabilityService;
import com.booking.availability.Slot;
import com.booking.common.error.ConflictException;
import com.booking.common.error.NotFoundException;
import com.booking.company.Company;
import com.booking.company.CompanyRepository;
import com.booking.company.CompanySettingsResolver;
import com.booking.company.EffectiveCompanySettings;

/**
 * Administrator changes to existing appointments other than approve/deny:
 * cancelling, moving to another time, recording the outcome, and notes. Changes
 * that affect capacity take the company lock, like booking and approval.
 */
@Service
public class AppointmentManagementService {

    private final AppointmentRepository appointmentRepository;
    private final CompanyRepository companyRepository;
    private final CompanySettingsResolver settingsResolver;
    private final AvailabilityService availabilityService;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public AppointmentManagementService(AppointmentRepository appointmentRepository,
                                        CompanyRepository companyRepository,
                                        CompanySettingsResolver settingsResolver,
                                        AvailabilityService availabilityService,
                                        ApplicationEventPublisher events,
                                        Clock clock) {
        this.appointmentRepository = appointmentRepository;
        this.companyRepository = companyRepository;
        this.settingsResolver = settingsResolver;
        this.availabilityService = availabilityService;
        this.events = events;
        this.clock = clock;
    }

    /** Cancels a pending or approved appointment, which frees its capacity. */
    @Transactional
    public Appointment cancel(Long appointmentId) {
        lockCompanyOf(appointmentId);
        Appointment appointment = load(appointmentId);
        appointment.cancel();
        events.publishEvent(new AppointmentEvent(appointment.getId(), AppointmentEvent.Type.CANCELLED));
        return appointment;
    }

    /** Records that an approved appointment took place. Only once it has started. */
    @Transactional
    public Appointment complete(Long appointmentId) {
        Appointment appointment = load(appointmentId);
        requireStarted(appointment);
        appointment.complete();
        return appointment;
    }

    /** Records that the customer did not come. Only once the appointment has started. */
    @Transactional
    public Appointment markNoShow(Long appointmentId) {
        Appointment appointment = load(appointmentId);
        requireStarted(appointment);
        appointment.markNoShow();
        return appointment;
    }

    /**
     * Moves a pending or approved appointment to another slot of the same company.
     * The new slot must be bookable and have free capacity, not counting this
     * appointment itself. The status is kept.
     */
    @Transactional
    public Appointment reschedule(Long appointmentId, Instant newStart) {
        Company company = lockCompanyOf(appointmentId);
        Appointment appointment = load(appointmentId);
        if (!appointment.isActive()) {
            throw new ConflictException("APPOINTMENT_NOT_ACTIVE", "Only pending or approved appointments can be moved");
        }
        if (!company.isActive()) {
            throw new ConflictException("COMPANY_INACTIVE", "The company has been removed");
        }
        EffectiveCompanySettings settings = settingsResolver.resolve(company);
        Slot slot = availabilityService.requireBookableSlot(company, settings, newStart);
        long others = appointmentRepository.countOverlapping(company.getId(), AppointmentStatus.CAPACITY_CONSUMING,
                slot.start(), slot.end(), appointment.getId());
        if (others >= settings.maxConcurrentAppointments()) {
            throw new ConflictException("SLOT_UNAVAILABLE", "The requested slot is not available");
        }
        Instant previousStart = appointment.getStartAt();
        if (previousStart.equals(slot.start())) {
            return appointment;
        }
        appointment.reschedule(slot.start(), slot.end());
        events.publishEvent(new AppointmentEvent(appointment.getId(), AppointmentEvent.Type.RESCHEDULED, previousStart));
        return appointment;
    }

    @Transactional
    public Appointment updateNote(Long appointmentId, String note) {
        Appointment appointment = load(appointmentId);
        appointment.setAdminNote(note == null || note.isBlank() ? null : note.trim());
        return appointment;
    }

    private void requireStarted(Appointment appointment) {
        if (appointment.getStartAt().isAfter(clock.instant())) {
            throw new ConflictException("APPOINTMENT_NOT_STARTED", "The appointment has not started yet");
        }
    }

    private Company lockCompanyOf(Long appointmentId) {
        Long companyId = appointmentRepository.findCompanyIdById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment", appointmentId));
        return companyRepository.findByIdForUpdate(companyId)
                .orElseThrow(() -> new NotFoundException("Company", companyId));
    }

    private Appointment load(Long appointmentId) {
        return appointmentRepository.findWithCompanyById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment", appointmentId));
    }
}
