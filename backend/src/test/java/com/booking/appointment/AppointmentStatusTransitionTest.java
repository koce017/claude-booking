package com.booking.appointment;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.booking.company.Company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppointmentStatusTransitionTest {

    private static Appointment pending() {
        return Appointment.pending(new Company("C"), Instant.parse("2026-11-12T13:00:00Z"),
                Instant.parse("2026-11-12T14:00:00Z"), new CustomerInfo("Ana", "1", "a@example.com"));
    }

    @Test
    void pendingCanBeApprovedOrDenied() {
        Appointment a = pending();
        a.approve();
        assertThat(a.getStatus()).isEqualTo(AppointmentStatus.APPROVED);

        Appointment b = pending();
        b.deny();
        assertThat(b.getStatus()).isEqualTo(AppointmentStatus.DENIED);
    }

    @Test
    void onlyPendingCanTransition() {
        Appointment approved = pending();
        approved.approve();
        assertThatThrownBy(approved::approve).isInstanceOf(InvalidStatusTransitionException.class);
        assertThatThrownBy(approved::deny).isInstanceOf(InvalidStatusTransitionException.class);

        Appointment denied = pending();
        denied.deny();
        assertThatThrownBy(denied::approve).isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void capacityConsumption() {
        assertThat(AppointmentStatus.PENDING.consumesCapacity()).isTrue();
        assertThat(AppointmentStatus.APPROVED.consumesCapacity()).isTrue();
        assertThat(AppointmentStatus.DENIED.consumesCapacity()).isFalse();
    }
}
