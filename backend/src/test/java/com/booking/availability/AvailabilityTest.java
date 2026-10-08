package com.booking.availability;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.booking.appointment.AppointmentApprovalService;
import com.booking.availability.AvailabilityDtos.DayAvailability;
import com.booking.availability.AvailabilityDtos.SlotView;
import com.booking.company.Company;
import com.booking.schedule.ScheduleOverrideService;
import com.booking.schedule.TimeRange;
import com.booking.schedule.WeeklySchedulePeriod;
import com.booking.schedule.WeeklySchedulePeriodRepository;
import com.booking.support.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AvailabilityTest extends IntegrationTest {

    @Autowired
    AvailabilityService availabilityService;

    @Autowired
    ScheduleOverrideService overrideService;

    @Autowired
    WeeklySchedulePeriodRepository weeklyRepository;

    @Autowired
    AppointmentApprovalService approvalService;

    private DayAvailability day(Company company, LocalDate date) {
        return availabilityService.getAvailability(company.getId(), date, date).days().get(0);
    }

    private static List<SlotView> appointmentSlots(DayAvailability day) {
        return day.slots().stream().filter(s -> s.status() != SlotStatus.NON_WORKING).toList();
    }

    private static SlotView slotAt(DayAvailability day, String localStart) {
        return day.slots().stream().filter(s -> s.localStart().equals(localStart)).findFirst().orElseThrow();
    }

    @Test
    void workingDayProducesDefaultHourlySlots() throws Exception {
        Company company = createCompany("Test Company");

        mvc.perform(get("/api/companies/{id}/availability", company.getId()).param("date", MONDAY.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timezone").value("Europe/Belgrade"))
                .andExpect(jsonPath("$.appointmentDurationMinutes").value(60))
                .andExpect(jsonPath("$.bookingWindow.lastDate").value("2027-10-31"))
                .andExpect(jsonPath("$.days[0].working").value(true))
                // 00:00-09:00 non-working, then 8 slots, then 17:00-24:00 non-working
                .andExpect(jsonPath("$.days[0].slots.length()").value(10))
                .andExpect(jsonPath("$.days[0].slots[0].status").value("NON_WORKING"))
                .andExpect(jsonPath("$.days[0].slots[1].localStart").value("09:00"))
                .andExpect(jsonPath("$.days[0].slots[1].localEnd").value("10:00"))
                .andExpect(jsonPath("$.days[0].slots[1].status").value("AVAILABLE"))
                .andExpect(jsonPath("$.days[0].slots[8].localStart").value("16:00"))
                .andExpect(jsonPath("$.days[0].slots[9].localEnd").value("24:00"));

        List<SlotView> slots = appointmentSlots(day(company, MONDAY));
        assertThat(slots).extracting(SlotView::localStart)
                .containsExactly("09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00");
        assertThat(slots).allMatch(SlotView::bookable);
    }

    @Test
    void weekendHasNoBookableSlots() {
        Company company = createCompany("Test Company");
        DayAvailability saturday = day(company, LocalDate.of(2026, 10, 10));
        DayAvailability sunday = day(company, LocalDate.of(2026, 10, 11));

        for (DayAvailability d : List.of(saturday, sunday)) {
            assertThat(d.working()).isFalse();
            assertThat(d.slots()).singleElement().satisfies(s -> {
                assertThat(s.status()).isEqualTo(SlotStatus.NON_WORKING);
                assertThat(s.bookable()).isFalse();
            });
        }
    }

    @Test
    void specialDateOverridesCloseWorkingDays() {
        Company company = createCompany("Test Company");
        LocalDate jan7 = LocalDate.of(2027, 1, 7);  // Thursday
        LocalDate jan8 = LocalDate.of(2027, 1, 8);  // Friday
        overrideService.set(company.getId(), jan7, List.of(), "closed");
        overrideService.set(company.getId(), jan8, List.of(), "closed");

        assertThat(day(company, jan7).working()).isFalse();
        assertThat(appointmentSlots(day(company, jan7))).isEmpty();
        assertThat(appointmentSlots(day(company, jan8))).isEmpty();
        // the following Monday still uses the weekly schedule
        assertThat(appointmentSlots(day(company, LocalDate.of(2027, 1, 11)))).hasSize(8);
    }

    @Test
    void specialDateOverrideCanReplaceHours() {
        Company company = createCompany("Test Company");
        overrideService.set(company.getId(), MONDAY,
                List.of(new TimeRange(LocalTime.of(9, 0), LocalTime.of(13, 0))), "short day");
        // An override can also open a normally closed day
        LocalDate saturday = LocalDate.of(2026, 10, 17);
        overrideService.set(company.getId(), saturday,
                List.of(new TimeRange(LocalTime.of(10, 0), LocalTime.of(12, 0))), null);

        assertThat(appointmentSlots(day(company, MONDAY))).extracting(SlotView::localStart)
                .containsExactly("09:00", "10:00", "11:00", "12:00");
        assertThat(appointmentSlots(day(company, saturday))).extracting(SlotView::localStart)
                .containsExactly("10:00", "11:00");
    }

    @Test
    void configuredWeeklyScheduleReplacesDefault() {
        Company company = createCompany("Test Company");
        weeklyRepository.save(new WeeklySchedulePeriod(company, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)));
        weeklyRepository.save(new WeeklySchedulePeriod(company, DayOfWeek.MONDAY, LocalTime.of(13, 0), LocalTime.of(15, 0)));

        DayAvailability monday = day(company, MONDAY);
        assertThat(appointmentSlots(monday)).extracting(SlotView::localStart)
                .containsExactly("08:00", "09:00", "10:00", "11:00", "13:00", "14:00");
        assertThat(slotAt(monday, "12:00").status()).isEqualTo(SlotStatus.NON_WORKING);
        // Tuesday has no rows once a schedule is configured -> closed
        assertThat(day(company, MONDAY.plusDays(1)).working()).isFalse();
    }

    @Test
    void pastSlotsAreUnavailable() {
        Company company = createCompany("Test Company");
        clock.set(at(MONDAY, 11, 30));

        DayAvailability today = day(company, MONDAY);
        assertThat(slotAt(today, "09:00").status()).isEqualTo(SlotStatus.UNAVAILABLE);
        assertThat(slotAt(today, "09:00").reason()).isEqualTo(UnavailableReason.PAST);
        assertThat(slotAt(today, "11:00").reason()).isEqualTo(UnavailableReason.PAST);
        assertThat(slotAt(today, "12:00").status()).isEqualTo(SlotStatus.AVAILABLE);
        assertThat(day(company, MONDAY.minusDays(7)).slots())
                .filteredOn(s -> s.status() != SlotStatus.NON_WORKING)
                .allMatch(s -> !s.bookable());
    }

    @Test
    void bookingWindowIsEnforced() {
        Company company = createCompany("Test Company");
        // 2027-10-29 is a Friday inside the window (window ends 2027-10-31)
        assertThat(appointmentSlots(day(company, LocalDate.of(2027, 10, 29)))).allMatch(SlotView::bookable);
        // 2027-11-01 is a Monday just outside
        assertThat(appointmentSlots(day(company, LocalDate.of(2027, 11, 1))))
                .isNotEmpty()
                .allMatch(s -> s.reason() == UnavailableReason.OUTSIDE_BOOKING_WINDOW && !s.bookable());
    }

    @Test
    void appointmentDurationIsRespected() {
        Company company = new Company("Short");
        company.setAppointmentDurationMinutes(45);
        company = companyRepository.save(company);

        List<SlotView> slots = appointmentSlots(day(company, MONDAY));
        // 09:00-17:00 in 45 min steps: last slot that fits is 15:45-16:30
        assertThat(slots).hasSize(10);
        assertThat(slots.get(0).localEnd()).isEqualTo("09:45");
        assertThat(slots.get(9).localStart()).isEqualTo("15:45");
    }

    @Test
    void capacityOneSinglePendingMakesSlotUnavailable() throws Exception {
        Company company = createCompany("Test Company", 1);
        bookViaApi(company.getId(), at(MONDAY, 10, 0), "Ana");

        SlotView slot = slotAt(day(company, MONDAY), "10:00");
        assertThat(slot.status()).isEqualTo(SlotStatus.UNAVAILABLE);
        assertThat(slot.reason()).isEqualTo(UnavailableReason.FULLY_BOOKED);
        assertThat(slotAt(day(company, MONDAY), "11:00").status()).isEqualTo(SlotStatus.AVAILABLE);
    }

    @Test
    void capacityThree() throws Exception {
        Company company = createCompany("Test Company", 3);
        long first = bookViaApi(company.getId(), at(MONDAY, 10, 0), "Ana");
        approvalService.approve(first);
        bookViaApi(company.getId(), at(MONDAY, 10, 0), "Bob");

        SlotView twoBooked = slotAt(day(company, MONDAY), "10:00");
        assertThat(twoBooked.bookable()).isTrue();
        assertThat(twoBooked.status()).isEqualTo(SlotStatus.PENDING);
        assertThat(twoBooked.pendingRequests()).isTrue();

        bookViaApi(company.getId(), at(MONDAY, 10, 0), "Cid");
        SlotView full = slotAt(day(company, MONDAY), "10:00");
        assertThat(full.status()).isEqualTo(SlotStatus.UNAVAILABLE);
        assertThat(full.bookable()).isFalse();
    }

    @Test
    void approvedOnlySlotWithCapacityLeftIsAvailable() throws Exception {
        Company company = createCompany("Test Company", 2);
        approvalService.approve(bookViaApi(company.getId(), at(MONDAY, 10, 0), "Ana"));

        SlotView slot = slotAt(day(company, MONDAY), "10:00");
        assertThat(slot.status()).isEqualTo(SlotStatus.AVAILABLE);
        assertThat(slot.pendingRequests()).isFalse();
    }

    @Test
    void deniedAppointmentsDoNotConsumeCapacity() throws Exception {
        Company company = createCompany("Test Company", 1);
        approvalService.deny(bookViaApi(company.getId(), at(MONDAY, 10, 0), "Ana"));

        assertThat(slotAt(day(company, MONDAY), "10:00").status()).isEqualTo(SlotStatus.AVAILABLE);
    }

    @Test
    void neverExposesCustomerData() throws Exception {
        Company company = createCompany("Test Company", 2);
        bookViaApi(company.getId(), at(MONDAY, 10, 0), "Secretname");

        mvc.perform(get("/api/companies/{id}/availability", company.getId()).param("date", MONDAY.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Secretname"))))
                .andExpect(content().string(not(containsString("example.com"))))
                .andExpect(content().string(not(containsString("customer"))));
    }

    @Test
    void validatesRequestedRange() throws Exception {
        Company company = createCompany("Test Company");
        mvc.perform(get("/api/companies/{id}/availability", company.getId())
                        .param("from", "2026-10-20").param("to", "2026-10-10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_RANGE"));
        mvc.perform(get("/api/companies/{id}/availability", company.getId())
                        .param("from", "2026-10-01").param("to", "2027-10-10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RANGE_TOO_LARGE"));
        mvc.perform(get("/api/companies/{id}/availability", company.getId()))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/companies/{id}/availability", company.getId())
                        .param("from", "2026-10-12").param("to", "2026-10-18"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days.length()").value(7));
    }
}
