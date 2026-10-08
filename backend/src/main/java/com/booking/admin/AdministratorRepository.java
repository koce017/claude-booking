package com.booking.admin;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdministratorRepository extends JpaRepository<Administrator, Long> {

    Optional<Administrator> findByEmailIgnoreCase(String email);
}
