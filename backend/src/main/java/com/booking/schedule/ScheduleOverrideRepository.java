package com.booking.schedule;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleOverrideRepository extends JpaRepository<ScheduleOverride, Long> {

    @EntityGraph(attributePaths = "periods")
    List<ScheduleOverride> findByCompanyIdAndDateBetweenOrderByDate(Long companyId, LocalDate from, LocalDate to);

    @EntityGraph(attributePaths = "periods")
    Optional<ScheduleOverride> findByCompanyIdAndDate(Long companyId, LocalDate date);
}
