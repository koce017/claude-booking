package com.booking.admin;

/** The authenticated administrator, stored in the security context. */
public record AdminPrincipal(Long id, String email) {
}
