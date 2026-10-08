package com.booking.company;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.booking.common.error.NotFoundException;

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
}
