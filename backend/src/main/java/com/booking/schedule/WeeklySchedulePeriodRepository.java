package com.booking.schedule;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WeeklySchedulePeriodRepository extends JpaRepository<WeeklySchedulePeriod, Long> {

    List<WeeklySchedulePeriod> findByCompanyId(Long companyId);

    @Modifying(flushAutomatically = true)
    @Query("delete from WeeklySchedulePeriod w where w.company.id = :companyId")
    void deleteByCompanyId(@Param("companyId") Long companyId);
}
