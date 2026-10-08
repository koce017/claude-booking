package com.booking.appointment;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import com.booking.company.Company;

/**
 * An appointment request. Status changes only through {@link #approve()} and
 * {@link #deny()}, which hold the state machine rules.
 */
@Entity
@Table(name = "appointment")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentStatus status;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_phone")
    private String customerPhone;

    @Column(name = "customer_email")
    private String customerEmail;

    /** JSON object with additional, company-configurable customer fields. Unused for now. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "customer_extra")
    private String customerExtra;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Appointment() {
    }

    public static Appointment pending(Company company, Instant startAt, Instant endAt, CustomerInfo customer) {
        Appointment a = new Appointment();
        a.company = company;
        a.startAt = startAt;
        a.endAt = endAt;
        a.status = AppointmentStatus.PENDING;
        a.customerName = customer.name();
        a.customerPhone = customer.phone();
        a.customerEmail = customer.email();
        return a;
    }

    public void approve() {
        transition(AppointmentStatus.APPROVED);
    }

    public void deny() {
        transition(AppointmentStatus.DENIED);
    }

    private void transition(AppointmentStatus target) {
        if (status != AppointmentStatus.PENDING) {
            throw new InvalidStatusTransitionException(status, target);
        }
        status = target;
    }

    public Long getId() {
        return id;
    }

    public Company getCompany() {
        return company;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public CustomerInfo getCustomer() {
        return new CustomerInfo(customerName, customerPhone, customerEmail);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
