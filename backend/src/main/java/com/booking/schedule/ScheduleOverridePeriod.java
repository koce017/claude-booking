package com.booking.schedule;

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

@Entity
@Table(name = "schedule_override_period")
public class ScheduleOverridePeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "override_id")
    private ScheduleOverride override;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    protected ScheduleOverridePeriod() {
    }

    ScheduleOverridePeriod(ScheduleOverride override, LocalTime startTime, LocalTime endTime) {
        this.override = override;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public TimeRange toTimeRange() {
        return new TimeRange(startTime, endTime);
    }
}
