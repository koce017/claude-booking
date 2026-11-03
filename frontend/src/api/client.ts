import type {
  AdminAppointment,
  Administrator,
  AdminCompany,
  AppointmentStatus,
  Availability,
  BookingResult,
  Company,
  CompanyUpdate,
  CustomerInput,
  Page,
  Period,
  ScheduleOverride,
  Session,
  WeeklySchedule,
} from './types';

/** Error carrying the backend's problem-detail fields. */
export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string | undefined,
    message: string,
    public readonly fieldErrors: Record<string, string> = {},
  ) {
    super(message);
  }
}

const SESSION_KEY = 'adminSession';

export function getSession(): Session | null {
  try {
    const raw = localStorage.getItem(SESSION_KEY);
    if (!raw) return null;
    const session = JSON.parse(raw) as Session;
    return new Date(session.expiresAt) > new Date() ? session : null;
  } catch {
    return null;
  }
}

export function setSession(session: Session | null) {
  try {
    if (session) localStorage.setItem(SESSION_KEY, JSON.stringify(session));
    else localStorage.removeItem(SESSION_KEY);
  } catch {
    /* storage unavailable */
  }
}

async function request<T>(path: string, init: RequestInit = {}, admin = false): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' };
  if (init.body) headers['Content-Type'] = 'application/json';
  if (admin) {
    const session = getSession();
    if (session) headers.Authorization = `Bearer ${session.token}`;
  }
  const res = await fetch(path, { ...init, headers: { ...headers, ...(init.headers ?? {}) } });
  if (res.status === 401 && admin) setSession(null);
  if (!res.ok) {
    let body: { detail?: string; code?: string; errors?: Record<string, string> } = {};
    try {
      body = await res.json();
    } catch {
      /* non-JSON error */
    }
    throw new ApiError(res.status, body.code, body.detail ?? `Request failed (${res.status})`, body.errors);
  }
  if (res.status === 204 || res.status === 202) return undefined as T;
  return (await res.json()) as T;
}

const json = (body: unknown) => JSON.stringify(body);

/** Sends only the customer fields that have a value. */
function customerBody(customer: CustomerInput) {
  const answers = Object.fromEntries(
    Object.entries(customer.answers)
      .map(([k, v]) => [k, v.trim()])
      .filter(([, v]) => v),
  );
  const value = (s: string) => s.trim() || undefined;
  return {
    name: value(customer.name),
    phone: value(customer.phone),
    email: value(customer.email),
    address: value(customer.address),
    answers,
  };
}

export const publicApi = {
  companies: () => request<Company[]>('/api/companies'),
  company: (id: number) => request<Company>(`/api/companies/${id}`),
  availability: (id: number, date: string) =>
    request<Availability>(`/api/companies/${id}/availability?date=${encodeURIComponent(date)}`),
  book: (id: number, start: string, customer: CustomerInput) =>
    request<BookingResult>(`/api/companies/${id}/appointments`, {
      method: 'POST',
      body: json({ start, customer: customerBody(customer) }),
    }),
};

export interface AppointmentFilter {
  companyId?: number;
  status?: AppointmentStatus;
  from?: string;
  to?: string;
}

const adminAppointment = (id: number, action: string, body?: unknown) =>
  request<AdminAppointment>(
    `/api/admin/appointments/${id}/${action}`,
    { method: 'POST', body: body === undefined ? undefined : json(body) },
    true,
  );

export const adminApi = {
  requestLink: (email: string) =>
    request<void>('/api/admin/auth/request-link', { method: 'POST', body: json({ email }) }),
  verify: (token: string) =>
    request<Session>('/api/admin/auth/verify', { method: 'POST', body: json({ token }) }),
  logout: () => request<void>('/api/admin/auth/logout', { method: 'POST' }, true),

  administrators: () => request<Administrator[]>('/api/admin/administrators', {}, true),
  addAdministrator: (email: string) =>
    request<Administrator>('/api/admin/administrators', { method: 'POST', body: json({ email }) }, true),
  removeAdministrator: (id: number) =>
    request<void>(`/api/admin/administrators/${id}`, { method: 'DELETE' }, true),

  companies: (includeInactive = false) =>
    request<AdminCompany[]>(`/api/admin/companies?includeInactive=${includeInactive}`, {}, true),
  company: (id: number) => request<AdminCompany>(`/api/admin/companies/${id}`, {}, true),
  createCompany: (name: string) =>
    request<AdminCompany>('/api/admin/companies', { method: 'POST', body: json({ name }) }, true),
  updateCompany: (id: number, update: CompanyUpdate) =>
    request<AdminCompany>(`/api/admin/companies/${id}`, { method: 'PUT', body: json(update) }, true),
  removeCompany: (id: number) => request<void>(`/api/admin/companies/${id}`, { method: 'DELETE' }, true),

  weeklySchedule: (id: number) => request<WeeklySchedule>(`/api/admin/companies/${id}/schedule/weekly`, {}, true),
  saveWeeklySchedule: (id: number, days: WeeklySchedule['days']) =>
    request<WeeklySchedule>(
      `/api/admin/companies/${id}/schedule/weekly`,
      { method: 'PUT', body: json({ days }) },
      true,
    ),
  resetWeeklySchedule: (id: number) =>
    request<WeeklySchedule>(`/api/admin/companies/${id}/schedule/weekly`, { method: 'DELETE' }, true),

  overrides: (id: number, from: string, to: string) =>
    request<ScheduleOverride[]>(`/api/admin/companies/${id}/schedule/overrides?from=${from}&to=${to}`, {}, true),
  saveOverride: (id: number, date: string, periods: Period[], note: string) =>
    request<ScheduleOverride>(
      `/api/admin/companies/${id}/schedule/overrides/${date}`,
      { method: 'PUT', body: json({ periods, note: note.trim() || null }) },
      true,
    ),
  removeOverride: (id: number, date: string) =>
    request<void>(`/api/admin/companies/${id}/schedule/overrides/${date}`, { method: 'DELETE' }, true),

  appointments: (filter: AppointmentFilter, size = 200) => {
    const params = new URLSearchParams({ size: String(size) });
    if (filter.companyId) params.set('companyId', String(filter.companyId));
    if (filter.status) params.set('status', filter.status);
    if (filter.from) params.set('from', filter.from);
    if (filter.to) params.set('to', filter.to);
    return request<Page<AdminAppointment>>(`/api/admin/appointments?${params}`, {}, true);
  },
  appointment: (id: number) => request<AdminAppointment>(`/api/admin/appointments/${id}`, {}, true),
  createAppointment: (companyId: number, start: string, customer: CustomerInput, adminNote: string) =>
    request<AdminAppointment>(
      '/api/admin/appointments',
      {
        method: 'POST',
        body: json({ companyId, start, customer: customerBody(customer), adminNote: adminNote.trim() || null }),
      },
      true,
    ),
  approve: (id: number) => adminAppointment(id, 'approve'),
  deny: (id: number) => adminAppointment(id, 'deny'),
  cancel: (id: number) => adminAppointment(id, 'cancel'),
  complete: (id: number) => adminAppointment(id, 'complete'),
  noShow: (id: number) => adminAppointment(id, 'no-show'),
  reschedule: (id: number, start: string) => adminAppointment(id, 'reschedule', { start }),
  updateNote: (id: number, adminNote: string) =>
    request<AdminAppointment>(
      `/api/admin/appointments/${id}/note`,
      { method: 'PUT', body: json({ adminNote: adminNote.trim() || null }) },
      true,
    ),
};
