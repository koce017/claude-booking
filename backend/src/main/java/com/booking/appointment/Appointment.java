package com.booking.appointment;

import java.time.Instant;
import java.util.List;
import java.util.Set;

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

import com.booking.common.error.ConflictException;
import com.booking.company.Company;

/**
 * An appointment. Status changes only through the transition methods below, which
 * hold the state machine rules:
 *
 * <pre>
 * PENDING  -> APPROVED | DENIED | CANCELLED
 * APPROVED -> CANCELLED | COMPLETED | NO_SHOW
 * </pre>
 *
 * DENIED, CANCELLED, COMPLETED and NO_SHOW are final.
 */
@Entity
@Table(name = "appointment")
public class Appointment {

    /** Address and answers to company questions, stored as JSON. */
    public record CustomerExtra(String address, List<CustomerInfo.Answer> answers) {
    }

    private static final Set<AppointmentStatus> ACTIVE = Set.of(AppointmentStatus.PENDING, AppointmentStatus.APPROVED);

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AppointmentSource source = AppointmentSource.PUBLIC;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_phone")
    private String customerPhone;

    @Column(name = "customer_email")
    private String customerEmail;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "customer_extra")
    private CustomerExtra customerExtra;

    @Column(name = "admin_note")
    private String adminNote;

    @Column(name = "fulfilled_at")
    private Instant fulfilledAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Appointment() {
    }

    public static Appointment pending(Company company, Instant startAt, Instant endAt, CustomerInfo customer) {
        return create(company, startAt, endAt, customer, AppointmentStatus.PENDING, AppointmentSource.PUBLIC);
    }

    /** An appointment entered by an administrator; it is approved straight away. */
    public static Appointment enteredByAdmin(Company company, Instant startAt, Instant endAt, CustomerInfo customer,
                                             String adminNote) {
        Appointment a = create(company, startAt, endAt, customer, AppointmentStatus.APPROVED, AppointmentSource.ADMIN);
        a.adminNote = adminNote;
        return a;
    }

    private static Appointment create(Company company, Instant startAt, Instant endAt, CustomerInfo customer,
                                      AppointmentStatus status, AppointmentSource source) {
        Appointment a = new Appointment();
        a.company = company;
        a.startAt = startAt;
        a.endAt = endAt;
        a.status = status;
        a.source = source;
        a.customerName = customer.name();
        a.customerPhone = customer.phone();
        a.customerEmail = customer.email();
        if (customer.address() != null || !customer.answers().isEmpty()) {
            a.customerExtra = new CustomerExtra(customer.address(), customer.answers());
        }
        return a;
    }

    public void approve() {
        transition(Set.of(AppointmentStatus.PENDING), AppointmentStatus.APPROVED);
    }

    public void deny() {
        transition(Set.of(AppointmentStatus.PENDING), AppointmentStatus.DENIED);
    }

    public void cancel() {
        transition(ACTIVE, AppointmentStatus.CANCELLED);
    }

    public void complete() {
        transition(Set.of(AppointmentStatus.APPROVED), AppointmentStatus.COMPLETED);
    }

    public void markNoShow() {
        transition(Set.of(AppointmentStatus.APPROVED), AppointmentStatus.NO_SHOW);
    }

    /** Moves a pending or approved appointment to a new time; the status is kept. */
    public void reschedule(Instant newStart, Instant newEnd) {
        if (!isActive()) {
            throw new ConflictException("APPOINTMENT_NOT_ACTIVE", "Only pending or approved appointments can be moved");
        }
        this.startAt = newStart;
        this.endAt = newEnd;
    }

    public boolean isActive() {
        return ACTIVE.contains(status);
    }

    public void markFulfilled(Instant at) {
        this.fulfilledAt = at;
    }

    public void setAdminNote(String adminNote) {
        this.adminNote = adminNote;
    }

    private void transition(Set<AppointmentStatus> allowedFrom, AppointmentStatus target) {
        if (!allowedFrom.contains(status)) {
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

    public AppointmentSource getSource() {
        return source;
    }

    public CustomerInfo getCustomer() {
        return customerExtra == null
                ? new CustomerInfo(customerName, customerPhone, customerEmail)
                : new CustomerInfo(customerName, customerPhone, customerEmail, customerExtra.address(),
                        customerExtra.answers());
    }

    public String getAdminNote() {
        return adminNote;
    }

    public Instant getFulfilledAt() {
        return fulfilledAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
