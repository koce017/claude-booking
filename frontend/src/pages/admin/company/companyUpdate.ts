import type { AdminCompany, CompanyUpdate } from '../../../api/types';

/** The company's current settings in the shape the update endpoint expects. */
export function toUpdate(c: AdminCompany): CompanyUpdate {
  return {
    name: c.name,
    description: c.description,
    logoUrl: c.logoUrl,
    address: c.address,
    phone: c.phone,
    email: c.email,
    website: c.website,
    fulfillmentEmail: c.fulfillmentEmail,
    timezone: c.timezone,
    appointmentDurationMinutes: c.appointmentDurationMinutes,
    slotIntervalMinutes: c.slotIntervalMinutes,
    maxConcurrentAppointments: c.maxConcurrentAppointments,
    bookingWindowMonths: c.bookingWindowMonths,
    customerForm: c.customerForm,
  };
}
