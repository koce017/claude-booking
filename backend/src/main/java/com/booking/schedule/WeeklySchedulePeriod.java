package com.booking.schedule;

import java.time.DayOfWeek;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.booking.company.Company;

/** One working period of the normal weekly schedule, e.g. Monday 09:00-17:00. */
@Entity
@Table(name = "weekly_schedule_period")
public class WeeklySchedulePeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id")
    private Company company;

    /** ISO day of week, 1 = Monday ... 7 = Sunday. */
    @Column(name = "day_of_week", nullable = false)
    private short dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    protected WeeklySchedulePeriod() {
    }

    public WeeklySchedulePeriod(Company company, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        this.company = company;
        this.dayOfWeek = (short) dayOfWeek.getValue();
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Long getId() {
        return id;
    }

    public DayOfWeek getDayOfWeek() {
        return DayOfWeek.of(dayOfWeek);
    }

    public TimeRange toTimeRange() {
        return new TimeRange(startTime, endTime);
    }
}
