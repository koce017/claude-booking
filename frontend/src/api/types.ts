// Mirrors the backend DTOs. The backend is authoritative for every booking rule;
// these types are only used to render its responses.

export type FieldRequirement = 'REQUIRED' | 'OPTIONAL' | 'HIDDEN';

export interface CustomQuestion {
  key: string;
  label: string;
  required: boolean;
}

export interface CustomerForm {
  phone: FieldRequirement;
  email: FieldRequirement;
  address: FieldRequirement;
  questions: CustomQuestion[];
}

export interface Company {
  id: number;
  name: string;
  description: string | null;
  logoUrl: string | null;
  address: string | null;
  phone: string | null;
  email: string | null;
  website: string | null;
  timezone: string;
  customerForm: CustomerForm;
}

export interface EffectiveSettings {
  timezone: string;
  appointmentDurationMinutes: number;
  slotIntervalMinutes: number;
  maxConcurrentAppointments: number;
  bookingWindowMonths: number;
}

export interface AdminCompany {
  id: number;
  name: string;
  active: boolean;
  description: string | null;
  logoUrl: string | null;
  address: string | null;
  phone: string | null;
  email: string | null;
  website: string | null;
  fulfillmentEmail: string | null;
  timezone: string | null;
  appointmentDurationMinutes: number | null;
  slotIntervalMinutes: number | null;
  maxConcurrentAppointments: number | null;
  bookingWindowMonths: number | null;
  customerForm: CustomerForm;
  customWeeklySchedule: boolean;
  effective: EffectiveSettings;
  createdAt: string;
  updatedAt: string;
}

export interface CompanyUpdate {
  name: string;
  description: string | null;
  logoUrl: string | null;
  address: string | null;
  phone: string | null;
  email: string | null;
  website: string | null;
  fulfillmentEmail: string | null;
  timezone: string | null;
  appointmentDurationMinutes: number | null;
  slotIntervalMinutes: number | null;
  maxConcurrentAppointments: number | null;
  bookingWindowMonths: number | null;
  customerForm: CustomerForm;
}

export type DayOfWeek = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

/** Times as "HH:mm" or "HH:mm:ss". */
export interface Period {
  start: string;
  end: string;
}

export interface WeeklySchedule {
  custom: boolean;
  days: { day: DayOfWeek; periods: Period[] }[];
}

export interface ScheduleOverride {
  date: string;
  closed: boolean;
  periods: Period[];
  note: string | null;
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
  slotIntervalMinutes: number;
  bookingWindow: { firstDate: string; lastDate: string };
  days: DayAvailability[];
}

export interface CustomerInput {
  name: string;
  phone: string;
  email: string;
  address: string;
  answers: Record<string, string>;
}

export interface BookingResult {
  id: number;
  companyId: number;
  start: string;
  end: string;
  status: AppointmentStatus;
}

export type AppointmentStatus = 'PENDING' | 'APPROVED' | 'DENIED' | 'CANCELLED' | 'COMPLETED' | 'NO_SHOW';

export const APPOINTMENT_STATUSES: AppointmentStatus[] = [
  'PENDING',
  'APPROVED',
  'DENIED',
  'CANCELLED',
  'COMPLETED',
  'NO_SHOW',
];

export interface Answer {
  key: string;
  question: string;
  answer: string;
}

export interface AdminAppointment {
  id: number;
  companyId: number;
  companyName: string;
  companyTimezone: string;
  start: string;
  end: string;
  status: AppointmentStatus;
  source: 'PUBLIC' | 'ADMIN';
  customer: {
    name: string;
    phone: string | null;
    email: string | null;
    address: string | null;
    answers: Answer[];
  };
  adminNote: string | null;
  fulfilledAt: string | null;
  createdAt: string;
  updatedAt: string;
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
