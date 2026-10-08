package com.booking.schedule;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WeeklySchedulePeriodRepository extends JpaRepository<WeeklySchedulePeriod, Long> {

    List<WeeklySchedulePeriod> findByCompanyId(Long companyId);
}
