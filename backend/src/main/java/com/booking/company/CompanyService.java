package com.booking.company;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.booking.common.error.FieldValidationException;
import com.booking.common.error.NotFoundException;
import com.booking.company.CompanyDtos.CustomerFormRequest;
import com.booking.company.CompanyDtos.QuestionRequest;
import com.booking.company.CompanyDtos.UpdateCompanyRequest;
import com.booking.company.CustomerFormConfig.CustomQuestion;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final ApplicationEventPublisher events;

    public CompanyService(CompanyRepository companyRepository, ApplicationEventPublisher events) {
        this.companyRepository = companyRepository;
        this.events = events;
    }

    @Transactional
    public Company create(String name) {
        return companyRepository.save(new Company(name.trim()));
    }

    /**
     * Replaces the company's editable settings. Blank values are stored as NULL so the
     * defaults apply. Existing appointments are not changed; new settings affect only
     * availability and future bookings.
     */
    @Transactional
    public Company update(Long id, UpdateCompanyRequest request) {
        Company company = companyRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Company", id));
        company.setName(request.name().trim());
        company.setDescription(blankToNull(request.description()));
        company.setLogoUrl(blankToNull(request.logoUrl()));
        company.setAddress(blankToNull(request.address()));
        company.setPhone(blankToNull(request.phone()));
        company.setEmail(blankToNull(request.email()));
        company.setWebsite(blankToNull(request.website()));
        company.setFulfillmentEmail(blankToNull(request.fulfillmentEmail()));
        company.setTimezone(validZone(blankToNull(request.timezone())));
        company.setAppointmentDurationMinutes(request.appointmentDurationMinutes());
        company.setSlotIntervalMinutes(request.slotIntervalMinutes());
        company.setMaxConcurrentAppointments(request.maxConcurrentAppointments());
        company.setBookingWindowMonths(request.bookingWindowMonths());
        company.setCustomerForm(toConfig(request.customerForm()));
        return company;
    }

    @Transactional(readOnly = true)
    public List<Company> listActive() {
        return companyRepository.findAllByActiveTrueOrderByNameAscIdAsc();
    }

    @Transactional(readOnly = true)
    public List<Company> listAll() {
        return companyRepository.findAllByOrderByNameAscIdAsc();
    }

    /** Returns an active company; removed companies are invisible to the public. */
    @Transactional(readOnly = true)
    public Company getActive(Long id) {
        return companyRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new NotFoundException("Company", id));
    }

    @Transactional(readOnly = true)
    public Company get(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Company", id));
    }

    /**
     * Soft-deletes the company. Historical appointments are kept; pending requests
     * are denied by listeners of {@link CompanyDeactivatedEvent}. Idempotent.
     */
    @Transactional
    public void deactivate(Long id) {
        Company company = companyRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Company", id));
        if (!company.isActive()) {
            return;
        }
        company.deactivate();
        events.publishEvent(new CompanyDeactivatedEvent(company.getId()));
    }

    private static String validZone(String zone) {
        if (zone == null) {
            return null;
        }
        try {
            return ZoneId.of(zone).getId();
        } catch (DateTimeException e) {
            throw new FieldValidationException(Map.of("timezone", "is not a valid time zone"));
        }
    }

    private static CustomerFormConfig toConfig(CustomerFormRequest form) {
        if (form == null) {
            return null;
        }
        List<CustomQuestion> questions = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        List<QuestionRequest> requested = form.questions() == null ? List.of() : form.questions();
        for (int i = 0; i < requested.size(); i++) {
            QuestionRequest q = requested.get(i);
            String key = blankToNull(q.key());
            if (key == null) {
                int n = i + 1;
                while (keys.contains("q" + n) || containsKey(requested, "q" + n)) {
                    n++;
                }
                key = "q" + n;
            }
            if (!keys.add(key)) {
                throw new FieldValidationException(Map.of("customerForm.questions[" + i + "].key", "must be unique"));
            }
            questions.add(new CustomQuestion(key, q.label().trim(), q.required()));
        }
        return new CustomerFormConfig(form.phone(), form.email(), form.address(), questions);
    }

    private static boolean containsKey(List<QuestionRequest> questions, String key) {
        return questions.stream().anyMatch(q -> key.equals(q.key()));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
