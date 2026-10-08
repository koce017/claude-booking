package com.booking.company;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * A company users can request appointments with. Settings columns are nullable:
 * NULL means "use the default" and is resolved by {@link CompanySettingsResolver}.
 */
@Entity
@Table(name = "company")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    private String description;

    @Column(name = "logo_url")
    private String logoUrl;

    private String address;

    private String phone;

    private String email;

    private String website;

    /** IANA zone id, e.g. "Europe/Belgrade". */
    private String timezone;

    @Column(name = "appointment_duration_minutes")
    private Integer appointmentDurationMinutes;

    @Column(name = "max_concurrent_appointments")
    private Integer maxConcurrentAppointments;

    @Column(name = "booking_window_months")
    private Integer bookingWindowMonths;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Company() {
    }

    public Company(String name) {
        this.name = name;
    }

    public void deactivate() {
        this.active = false;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Integer getAppointmentDurationMinutes() {
        return appointmentDurationMinutes;
    }

    public void setAppointmentDurationMinutes(Integer appointmentDurationMinutes) {
        this.appointmentDurationMinutes = appointmentDurationMinutes;
    }

    public Integer getMaxConcurrentAppointments() {
        return maxConcurrentAppointments;
    }

    public void setMaxConcurrentAppointments(Integer maxConcurrentAppointments) {
        this.maxConcurrentAppointments = maxConcurrentAppointments;
    }

    public Integer getBookingWindowMonths() {
        return bookingWindowMonths;
    }

    public void setBookingWindowMonths(Integer bookingWindowMonths) {
        this.bookingWindowMonths = bookingWindowMonths;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
