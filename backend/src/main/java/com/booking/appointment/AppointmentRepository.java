package com.booking.appointment;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, Long>, JpaSpecificationExecutor<Appointment> {

    /** Time span and status of an appointment; used for public availability (no customer data). */
    interface OccupancyView {
        Instant getStartAt();

        Instant getEndAt();

        AppointmentStatus getStatus();
    }

    @Query("""
            select a.startAt as startAt, a.endAt as endAt, a.status as status
            from Appointment a
            where a.company.id = :companyId and a.status in :statuses
              and a.startAt < :to and a.endAt > :from
            """)
    List<OccupancyView> findOccupancy(@Param("companyId") Long companyId,
                                      @Param("statuses") Collection<AppointmentStatus> statuses,
                                      @Param("from") Instant from,
                                      @Param("to") Instant to);

    /**
     * Counts appointments with the given statuses that overlap [start, end),
     * excluding the appointment with id {@code excludeId} (pass 0 to exclude none).
     */
    @Query("""
            select count(a) from Appointment a
            where a.company.id = :companyId and a.status in :statuses
              and a.startAt < :end and a.endAt > :start
              and a.id <> :excludeId
            """)
    long countOverlapping(@Param("companyId") Long companyId,
                          @Param("statuses") Collection<AppointmentStatus> statuses,
                          @Param("start") Instant start,
                          @Param("end") Instant end,
                          @Param("excludeId") long excludeId);

    @Query("select a.company.id from Appointment a where a.id = :id")
    Optional<Long> findCompanyIdById(@Param("id") Long id);

    List<Appointment> findByCompanyIdAndStatus(Long companyId, AppointmentStatus status);

    @Override
    @EntityGraph(attributePaths = "company")
    Page<Appointment> findAll(Specification<Appointment> spec, Pageable pageable);

    @EntityGraph(attributePaths = "company")
    Optional<Appointment> findWithCompanyById(Long id);
}
