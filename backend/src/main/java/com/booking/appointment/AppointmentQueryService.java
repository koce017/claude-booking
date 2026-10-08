package com.booking.appointment;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.booking.common.error.NotFoundException;

/** Read-side queries for administrators. */
@Service
public class AppointmentQueryService {

    public record AppointmentFilter(Long companyId, AppointmentStatus status, Instant from, Instant to) {
    }

    private final AppointmentRepository appointmentRepository;

    public AppointmentQueryService(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    @Transactional(readOnly = true)
    public Page<Appointment> search(AppointmentFilter filter, Pageable pageable) {
        Specification<Appointment> spec = (root, query, cb) -> cb.conjunction();
        if (filter.companyId() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("company").get("id"), filter.companyId()));
        }
        if (filter.status() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), filter.status()));
        }
        if (filter.from() != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("startAt"), filter.from()));
        }
        if (filter.to() != null) {
            spec = spec.and((root, query, cb) -> cb.lessThan(root.get("startAt"), filter.to()));
        }
        return appointmentRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Appointment get(Long id) {
        return appointmentRepository.findWithCompanyById(id)
                .orElseThrow(() -> new NotFoundException("Appointment", id));
    }
}
