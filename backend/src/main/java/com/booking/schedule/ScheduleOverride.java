package com.booking.schedule;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import com.booking.company.Company;

/**
 * Exception to the weekly schedule for one date. No periods = closed all day;
 * otherwise the periods replace that day's weekly schedule.
 */
@Entity
@Table(name = "schedule_override")
public class ScheduleOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "override_date", nullable = false)
    private LocalDate date;

    private String note;

    @OneToMany(mappedBy = "override", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("startTime")
    private List<ScheduleOverridePeriod> periods = new ArrayList<>();

    protected ScheduleOverride() {
    }

    public ScheduleOverride(Company company, LocalDate date) {
        this.company = company;
        this.date = date;
    }

    public void replacePeriods(List<TimeRange> ranges) {
        periods.clear();
        for (TimeRange r : ranges) {
            periods.add(new ScheduleOverridePeriod(this, r.start(), r.end()));
        }
    }

    public boolean isClosed() {
        return periods.isEmpty();
    }

    public List<TimeRange> getTimeRanges() {
        return periods.stream().map(ScheduleOverridePeriod::toTimeRange).toList();
    }

    public Long getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
