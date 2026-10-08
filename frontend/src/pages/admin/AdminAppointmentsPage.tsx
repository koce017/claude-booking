import { useCallback, useEffect, useState } from 'react';
import { adminApi } from '../../api/client';
import type { AdminAppointment, AdminCompany, AppointmentStatus } from '../../api/types';

const STATUSES: AppointmentStatus[] = ['PENDING', 'APPROVED', 'DENIED'];

function formatRange(a: AdminAppointment): string {
  const start = new Date(a.start);
  const end = new Date(a.end);
  return `${start.toLocaleDateString()} ${start.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}–${end.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`;
}

export default function AdminAppointmentsPage() {
  const [companies, setCompanies] = useState<AdminCompany[]>([]);
  const [companyId, setCompanyId] = useState<number | undefined>();
  const [status, setStatus] = useState<AppointmentStatus | undefined>('PENDING');
  const [appointments, setAppointments] = useState<AdminAppointment[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    adminApi.companies().then(setCompanies).catch((e: Error) => setError(e.message));
  }, []);

  const load = useCallback(() => {
    return adminApi
      .appointments({ companyId, status })
      .then((page) => setAppointments(page.items))
      .catch((e: Error) => setError(e.message));
  }, [companyId, status]);

  useEffect(() => {
    load();
  }, [load]);

  async function decide(a: AdminAppointment, action: 'approve' | 'deny') {
    setError(null);
    try {
      await (action === 'approve' ? adminApi.approve(a.id) : adminApi.deny(a.id));
      await load();
    } catch (err) {
      setError((err as Error).message);
    }
  }

  return (
    <section>
      <h1>Appointment requests</h1>
      <div className="inline-form">
        <label>
          Company
          <select value={companyId ?? ''} onChange={(e) => setCompanyId(e.target.value ? Number(e.target.value) : undefined)}>
            <option value="">All</option>
            {companies.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </label>
        <label>
          Status
          <select value={status ?? ''} onChange={(e) => setStatus((e.target.value || undefined) as AppointmentStatus | undefined)}>
            <option value="">All</option>
            {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
          </select>
        </label>
      </div>
      {error && <p className="error">{error}</p>}
      {appointments.length === 0 ? (
        <p>No appointments match.</p>
      ) : (
        <table>
          <thead>
            <tr><th>ID</th><th>Company</th><th>Time</th><th>Customer</th><th>Status</th><th /></tr>
          </thead>
          <tbody>
            {appointments.map((a) => (
              <tr key={a.id}>
                <td>{a.id}</td>
                <td>{a.companyName}</td>
                <td>{formatRange(a)}</td>
                <td>{a.customer.name}<br /><span className="muted">{a.customer.phone} · {a.customer.email}</span></td>
                <td><span className={`badge badge-${a.status.toLowerCase()}`}>{a.status}</span></td>
                <td>
                  {a.status === 'PENDING' && (
                    <>
                      <button onClick={() => decide(a, 'approve')}>Approve</button>{' '}
                      <button className="danger" onClick={() => decide(a, 'deny')}>Deny</button>
                    </>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
