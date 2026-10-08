package com.booking.appointment;

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
 * Creates appointment requests. The whole operation is one transaction that starts
 * by locking the company row, so concurrent bookings for the same company are
 * serialized and the capacity check + insert cannot interleave.
 */
@Service
public class AppointmentBookingService {

    private final CompanyRepository companyRepository;
    private final CompanySettingsResolver settingsResolver;
    private final AvailabilityService availabilityService;
    private final CustomerFieldPolicy customerFieldPolicy;
    private final AppointmentRepository appointmentRepository;
    private final ApplicationEventPublisher events;

    public AppointmentBookingService(CompanyRepository companyRepository,
                                     CompanySettingsResolver settingsResolver,
                                     AvailabilityService availabilityService,
                                     CustomerFieldPolicy customerFieldPolicy,
                                     AppointmentRepository appointmentRepository,
                                     ApplicationEventPublisher events) {
        this.companyRepository = companyRepository;
        this.settingsResolver = settingsResolver;
        this.availabilityService = availabilityService;
        this.customerFieldPolicy = customerFieldPolicy;
        this.appointmentRepository = appointmentRepository;
        this.events = events;
    }

    @Transactional
    public Appointment book(Long companyId, Instant start, CustomerInfo customer) {
        Company company = companyRepository.findByIdForUpdate(companyId)
                .filter(Company::isActive)
                .orElseThrow(() -> new NotFoundException("Company", companyId));
        EffectiveCompanySettings settings = settingsResolver.resolve(company);

        customerFieldPolicy.validate(company, customer);
        // End time is derived from the company's duration; clients never supply it.
        Slot slot = availabilityService.requireBookableSlot(company, settings, start);

        long consumed = appointmentRepository.countOverlapping(company.getId(),
                AppointmentStatus.CAPACITY_CONSUMING, slot.start(), slot.end(), 0L);
        if (consumed >= settings.maxConcurrentAppointments()) {
            throw new ConflictException("SLOT_UNAVAILABLE", "The requested slot is no longer available");
        }

        Appointment appointment = appointmentRepository.save(
                Appointment.pending(company, slot.start(), slot.end(), customer));
        events.publishEvent(new AppointmentEvent(appointment.getId(), AppointmentEvent.Type.REQUESTED));
        return appointment;
    }
}
