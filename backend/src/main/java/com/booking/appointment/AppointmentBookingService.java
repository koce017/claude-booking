package com.booking.appointment;

import java.time.Instant;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.booking.appointment.CustomerFieldPolicy.SubmittedCustomer;
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

    /** A customer's request: created as PENDING, every required customer field must be given. */
    @Transactional
    public Appointment book(Long companyId, Instant start, SubmittedCustomer submitted) {
        Company company = lockActiveCompany(companyId);
        EffectiveCompanySettings settings = settingsResolver.resolve(company);

        CustomerInfo customer = customerFieldPolicy.validate(company, submitted);
        Slot slot = requireFreeSlot(company, settings, start);

        Appointment appointment = appointmentRepository.save(
                Appointment.pending(company, slot.start(), slot.end(), customer));
        events.publishEvent(new AppointmentEvent(appointment.getId(), AppointmentEvent.Type.REQUESTED));
        return appointment;
    }

    /**
     * An appointment entered by an administrator (e.g. booked by phone). The same slot
     * and capacity rules apply, but only the customer's name is required, and the
     * appointment is APPROVED immediately.
     */
    @Transactional
    public Appointment bookByAdmin(Long companyId, Instant start, SubmittedCustomer submitted, String adminNote) {
        Company company = lockActiveCompany(companyId);
        EffectiveCompanySettings settings = settingsResolver.resolve(company);

        CustomerInfo customer = customerFieldPolicy.validateLenient(company, submitted);
        Slot slot = requireFreeSlot(company, settings, start);

        Appointment appointment = appointmentRepository.save(
                Appointment.enteredByAdmin(company, slot.start(), slot.end(), customer, adminNote));
        events.publishEvent(new AppointmentEvent(appointment.getId(), AppointmentEvent.Type.CREATED_BY_ADMIN));
        return appointment;
    }

    private Company lockActiveCompany(Long companyId) {
        return companyRepository.findByIdForUpdate(companyId)
                .filter(Company::isActive)
                .orElseThrow(() -> new NotFoundException("Company", companyId));
    }

    /** Must run while the company lock is held. */
    private Slot requireFreeSlot(Company company, EffectiveCompanySettings settings, Instant start) {
        // End time is derived from the company's duration; clients never supply it.
        Slot slot = availabilityService.requireBookableSlot(company, settings, start);
        long consumed = appointmentRepository.countOverlapping(company.getId(),
                AppointmentStatus.CAPACITY_CONSUMING, slot.start(), slot.end(), 0L);
        if (consumed >= settings.maxConcurrentAppointments()) {
            throw new ConflictException("SLOT_UNAVAILABLE", "The requested slot is no longer available");
        }
        return slot;
    }
}
