package com.booking.appointment;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.booking.common.error.ConflictException;
import com.booking.company.Company;
import com.booking.support.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Many threads try to book the same slot at the same moment against a real
 * PostgreSQL database. Capacity must never be exceeded.
 */
class ConcurrentBookingTest extends IntegrationTest {

    private static final int THREADS = 20;

    @Autowired
    AppointmentBookingService bookingService;

    @Autowired
    AppointmentApprovalService approvalService;

    @Autowired
    AppointmentRepository appointmentRepository;

    private record Outcome(int succeeded, int conflicts, int otherErrors) {
    }

    private Outcome raceToBook(Long companyId, Instant start) throws Exception {
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();
        AtomicInteger other = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                int n = i;
                futures.add(pool.submit((Callable<Void>) () -> {
                    ready.countDown();
                    go.await();
                    try {
                        bookingService.book(companyId, start, new CustomerInfo("User " + n, "123", "u" + n + "@example.com"));
                        succeeded.incrementAndGet();
                    } catch (ConflictException e) {
                        conflicts.incrementAndGet();
                    } catch (RuntimeException e) {
                        other.incrementAndGet();
                    }
                    return null;
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            go.countDown();
            for (Future<?> f : futures) {
                f.get(30, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
        return new Outcome(succeeded.get(), conflicts.get(), other.get());
    }

    private long consumingCount(Long companyId, Instant start) {
        return appointmentRepository.countOverlapping(companyId, AppointmentStatus.CAPACITY_CONSUMING,
                start, start.plusSeconds(3600), 0L);
    }

    @Test
    void capacityOneAllowsExactlyOneConcurrentBooking() throws Exception {
        Company company = createCompany("Test Company", 1);
        Instant start = at(MONDAY, 14, 0);

        Outcome outcome = raceToBook(company.getId(), start);

        assertThat(outcome.succeeded()).isEqualTo(1);
        assertThat(outcome.conflicts()).isEqualTo(THREADS - 1);
        assertThat(outcome.otherErrors()).isZero();
        assertThat(consumingCount(company.getId(), start)).isEqualTo(1);
    }

    @Test
    void capacityThreeAllowsExactlyThreeConcurrentBookings() throws Exception {
        Company company = createCompany("Test Company", 3);
        Instant start = at(MONDAY, 14, 0);

        Outcome outcome = raceToBook(company.getId(), start);

        assertThat(outcome.succeeded()).isEqualTo(3);
        assertThat(outcome.conflicts()).isEqualTo(THREADS - 3);
        assertThat(consumingCount(company.getId(), start)).isEqualTo(3);
    }

    @Test
    void concurrentBookingsOfDifferentSlotsAllSucceed() throws Exception {
        Company company = createCompany("Test Company", 1);
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Future<Appointment>> futures = new ArrayList<>();
            for (int hour = 9; hour < 17; hour++) {
                Instant start = at(MONDAY, hour, 0);
                futures.add(pool.submit(() -> {
                    go.await();
                    return bookingService.book(company.getId(), start, new CustomerInfo("U", "1", "u@example.com"));
                }));
            }
            go.countDown();
            for (Future<Appointment> f : futures) {
                assertThat(f.get(30, TimeUnit.SECONDS).getStatus()).isEqualTo(AppointmentStatus.PENDING);
            }
        } finally {
            pool.shutdownNow();
        }
        assertThat(appointmentRepository.count()).isEqualTo(8);
    }

    @Test
    void denialAndRebookingStayWithinCapacity() throws Exception {
        Company company = createCompany("Test Company", 1);
        Instant start = at(MONDAY, 14, 0);
        Appointment first = bookingService.book(company.getId(), start, new CustomerInfo("A", "1", "a@example.com"));
        approvalService.deny(first.getId());

        Outcome outcome = raceToBook(company.getId(), start);

        assertThat(outcome.succeeded()).isEqualTo(1);
        assertThat(consumingCount(company.getId(), start)).isEqualTo(1);
    }
}
