package com.booking.company;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    List<Company> findAllByActiveTrueOrderByNameAscIdAsc();

    List<Company> findAllByOrderByNameAscIdAsc();

    Optional<Company> findByIdAndActiveTrue(Long id);

    /**
     * Loads the company with a row-level write lock (SELECT ... FOR UPDATE).
     * Every operation that changes how much capacity is consumed (booking, approval)
     * takes this lock first, so capacity checks for one company are serialized.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Company c where c.id = :id")
    Optional<Company> findByIdForUpdate(@Param("id") Long id);
}
