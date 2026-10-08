package com.booking.appointment;

import java.time.Clock;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.booking.common.error.ConflictException;
import com.booking.common.error.NotFoundException;
import com.booking.company.Company;
import com.booking.company.CompanyDeactivatedEvent;
import com.booking.company.CompanyRepository;
import com.booking.company.CompanySettingsResolver;

/**
 * Admin decisions on appointment requests. Takes the same company lock as booking
 * so approvals and bookings for one company never race on capacity. What happens
 * after approval is delegated to the fulfillment abstraction via
 * {@link AppointmentEventListener}.
 */
@Service
public class AppointmentApprovalService {

    private final AppointmentRepository appointmentRepository;
    private final CompanyRepository companyRepository;
    private final CompanySettingsResolver settingsResolver;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public AppointmentApprovalService(AppointmentRepository appointmentRepository,
                                      CompanyRepository companyRepository,
                                      CompanySettingsResolver settingsResolver,
                                      ApplicationEventPublisher events,
                                      Clock clock) {
        this.appointmentRepository = appointmentRepository;
        this.companyRepository = companyRepository;
        this.settingsResolver = settingsResolver;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public Appointment approve(Long appointmentId) {
        Company company = lockCompanyOf(appointmentId);
        // Loaded after the lock, so the status read here is current.
        Appointment appointment = load(appointmentId);
        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new InvalidStatusTransitionException(appointment.getStatus(), AppointmentStatus.APPROVED);
        }
        if (!company.isActive()) {
            throw new ConflictException("COMPANY_INACTIVE", "The company has been removed");
        }
        if (!appointment.getStartAt().isAfter(clock.instant())) {
            throw new ConflictException("APPOINTMENT_IN_PAST", "The appointment time has already passed");
        }
        long others = appointmentRepository.countOverlapping(company.getId(), AppointmentStatus.CAPACITY_CONSUMING,
                appointment.getStartAt(), appointment.getEndAt(), appointment.getId());
        if (others >= settingsResolver.resolve(company).maxConcurrentAppointments()) {
            throw new ConflictException("CAPACITY_EXCEEDED", "Approving this appointment would exceed the slot capacity");
        }
        appointment.approve();
        events.publishEvent(new AppointmentEvent(appointment.getId(), AppointmentEvent.Type.APPROVED));
        return appointment;
    }

    @Transactional
    public Appointment deny(Long appointmentId) {
        lockCompanyOf(appointmentId);
        Appointment appointment = load(appointmentId);
        appointment.deny();
        events.publishEvent(new AppointmentEvent(appointment.getId(), AppointmentEvent.Type.DENIED));
        return appointment;
    }

    /** Pending requests of a removed company are denied automatically (in the removal transaction). */
    @EventListener
    public void onCompanyDeactivated(CompanyDeactivatedEvent event) {
        for (Appointment a : appointmentRepository.findByCompanyIdAndStatus(event.companyId(), AppointmentStatus.PENDING)) {
            a.deny();
            events.publishEvent(new AppointmentEvent(a.getId(), AppointmentEvent.Type.DENIED));
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
