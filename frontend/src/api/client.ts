import type {
  AdminAppointment,
  AdminCompany,
  AppointmentStatus,
  Availability,
  BookingResult,
  Company,
  CustomerInput,
  Page,
  Session,
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
  if (session) localStorage.setItem(SESSION_KEY, JSON.stringify(session));
  else localStorage.removeItem(SESSION_KEY);
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

export const publicApi = {
  companies: () => request<Company[]>('/api/companies'),
  company: (id: number) => request<Company>(`/api/companies/${id}`),
  availability: (id: number, date: string) =>
    request<Availability>(`/api/companies/${id}/availability?date=${encodeURIComponent(date)}`),
  book: (id: number, start: string, customer: CustomerInput) =>
    request<BookingResult>(`/api/companies/${id}/appointments`, {
      method: 'POST',
      body: json({ start, customer }),
    }),
};

export const adminApi = {
  requestLink: (email: string) =>
    request<void>('/api/admin/auth/request-link', { method: 'POST', body: json({ email }) }),
  verify: (token: string) =>
    request<Session>('/api/admin/auth/verify', { method: 'POST', body: json({ token }) }),
  logout: () => request<void>('/api/admin/auth/logout', { method: 'POST' }, true),
  companies: () => request<AdminCompany[]>('/api/admin/companies', {}, true),
  createCompany: (name: string) =>
    request<AdminCompany>('/api/admin/companies', { method: 'POST', body: json({ name }) }, true),
  removeCompany: (id: number) => request<void>(`/api/admin/companies/${id}`, { method: 'DELETE' }, true),
  appointments: (filter: { companyId?: number; status?: AppointmentStatus }) => {
    const params = new URLSearchParams({ size: '200' });
    if (filter.companyId) params.set('companyId', String(filter.companyId));
    if (filter.status) params.set('status', filter.status);
    return request<Page<AdminAppointment>>(`/api/admin/appointments?${params}`, {}, true);
  },
  approve: (id: number) =>
    request<AdminAppointment>(`/api/admin/appointments/${id}/approve`, { method: 'POST' }, true),
  deny: (id: number) => request<AdminAppointment>(`/api/admin/appointments/${id}/deny`, { method: 'POST' }, true),
};
