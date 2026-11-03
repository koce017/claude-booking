package com.booking.company;

/**
 * Published (synchronously, inside the deactivation transaction) when a company is
 * removed, so other modules can react without the company module depending on them.
 */
public record CompanyDeactivatedEvent(Long companyId) {
}
