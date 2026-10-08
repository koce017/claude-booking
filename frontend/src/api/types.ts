// Mirrors the backend DTOs. The backend is authoritative for every booking rule;
// these types are only used to render its responses.

export interface Company {
  id: number;
  name: string;
  description: string | null;
}

export interface AdminCompany {
  id: number;
  name: string;
  active: boolean;
  createdAt: string;
}

export type SlotStatus = 'AVAILABLE' | 'PENDING' | 'UNAVAILABLE' | 'NON_WORKING';
export type UnavailableReason = 'FULLY_BOOKED' | 'PAST' | 'OUTSIDE_BOOKING_WINDOW';

export interface SlotView {
  start: string;
  end: string;
  localStart: string;
  localEnd: string;
  status: SlotStatus;
  pendingRequests: boolean;
  bookable: boolean;
  reason: UnavailableReason | null;
}

export interface DayAvailability {
  date: string;
  working: boolean;
  slots: SlotView[];
}

export interface Availability {
  companyId: number;
  timezone: string;
  appointmentDurationMinutes: number;
  bookingWindow: { firstDate: string; lastDate: string };
  days: DayAvailability[];
}

export interface CustomerInput {
  name: string;
  phone: string;
  email: string;
}

export interface BookingResult {
  id: number;
  companyId: number;
  start: string;
  end: string;
  status: AppointmentStatus;
}

export type AppointmentStatus = 'PENDING' | 'APPROVED' | 'DENIED';

export interface AdminAppointment {
  id: number;
  companyId: number;
  companyName: string;
  start: string;
  end: string;
  status: AppointmentStatus;
  customer: CustomerInput;
  createdAt: string;
}

export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface Session {
  token: string;
  expiresAt: string;
  email: string;
}
