import { useCallback, useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { adminApi } from '../../api/client';
import { APPOINTMENT_STATUSES } from '../../api/types';
import type { AdminAppointment, AdminCompany, AppointmentStatus } from '../../api/types';
import { errorMessage } from '../../errors';
import { addDays, formatRange, formatTimestamp } from '../../format';
import { fmt, t } from '../../i18n/sr';
import AppointmentActions from './AppointmentActions';

/** Midnight at the start of a "YYYY-MM-DD" date in the browser's time zone, as an ISO instant. */
const startOfLocalDay = (isoDate: string) => new Date(`${isoDate}T00:00:00`).toISOString();

export default function AdminAppointmentsPage() {
  const [params, setParams] = useSearchParams();
  const singleId = params.get('id') ? Number(params.get('id')) : null;

  const [companies, setCompanies] = useState<AdminCompany[]>([]);
  const [companyId, setCompanyId] = useState<number | undefined>();
  const [status, setStatus] = useState<AppointmentStatus | undefined>('PENDING');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [appointments, setAppointments] = useState<AdminAppointment[] | null>(null);
  const [total, setTotal] = useState(0);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    adminApi.companies(true).then(setCompanies).catch((e) => setError(errorMessage(e)));
  }, []);

  const load = useCallback(() => {
    setError(null);
    if (singleId) {
      return adminApi
        .appointment(singleId)
        .then((a) => {
          setAppointments([a]);
          setTotal(1);
        })
        .catch((e) => setError(errorMessage(e)));
    }
    return adminApi
      .appointments({
        companyId,
        status,
        from: from ? startOfLocalDay(from) : undefined,
        to: to ? startOfLocalDay(addDays(to, 1)) : undefined,
      })
      .then((page) => {
        setAppointments(page.items);
        setTotal(page.totalElements);
      })
      .catch((e) => setError(errorMessage(e)));
  }, [singleId, companyId, status, from, to]);

  useEffect(() => {
    load();
  }, [load]);

  const replace = (updated: AdminAppointment) =>
    setAppointments((list) => list?.map((a) => (a.id === updated.id ? updated : a)) ?? null);

  return (
    <section>
      <h1>{t.appointments.title}</h1>
      {singleId ? (
        <p>
          {fmt(t.appointments.single, { id: singleId })}{' '}
          <button className="secondary" onClick={() => setParams({})}>
            {t.appointments.showAll}
          </button>
        </p>
      ) : (
        <div className="inline-form">
          <label>
            {t.appointments.company}
            <select
              value={companyId ?? ''}
              onChange={(e) => setCompanyId(e.target.value ? Number(e.target.value) : undefined)}
            >
              <option value="">{t.common.all}</option>
              {companies.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </label>
          <label>
            {t.appointments.status}
            <select
              value={status ?? ''}
              onChange={(e) => setStatus((e.target.value || undefined) as AppointmentStatus | undefined)}
            >
              <option value="">{t.common.all}</option>
              {APPOINTMENT_STATUSES.map((s) => (
                <option key={s} value={s}>
                  {t.status[s]}
                </option>
              ))}
            </select>
          </label>
          <label>
            {t.appointments.from}
            <input type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
          </label>
          <label>
            {t.appointments.to}
            <input type="date" value={to} onChange={(e) => setTo(e.target.value)} />
          </label>
        </div>
      )}
      {error && <p className="error">{error}</p>}
      {!appointments && !error && <p>{t.common.loading}</p>}
      {appointments?.length === 0 && <p>{t.appointments.empty}</p>}
      {!!appointments?.length && (
        <>
          {!singleId && total > appointments.length && (
            <p className="muted">{fmt(t.appointments.showing, { count: appointments.length, total })}</p>
          )}
          <table className="appointments">
            <thead>
              <tr>
                <th>{t.appointments.colId}</th>
                <th>{t.appointments.colCompany}</th>
                <th>{t.appointments.colTime}</th>
                <th>{t.appointments.colCustomer}</th>
                <th>{t.appointments.colStatus}</th>
              </tr>
            </thead>
            <tbody>
              {appointments.map((a) => (
                <AppointmentRow key={a.id} appointment={a} onChanged={replace} />
              ))}
            </tbody>
          </table>
        </>
      )}
    </section>
  );
}

function AppointmentRow({
  appointment: a,
  onChanged,
}: {
  appointment: AdminAppointment;
  onChanged: (a: AdminAppointment) => void;
}) {
  const c = a.customer;
  return (
    <>
      <tr className="main-row">
        <td>
          <Link to={`/admin/appointments?id=${a.id}`}>{a.id}</Link>
        </td>
        <td>{a.companyName}</td>
        <td>
          {formatRange(a.start, a.end, a.companyTimezone)}
          <br />
          <span className="muted">{a.companyTimezone}</span>
        </td>
        <td>
          {c.name}
          <br />
          <span className="muted">{[c.phone, c.email].filter(Boolean).join(' · ')}</span>
          {c.address && (
            <>
              <br />
              <span className="muted">{c.address}</span>
            </>
          )}
          {c.answers.map((ans) => (
            <div key={ans.key} className="muted">
              {ans.question}: {ans.answer}
            </div>
          ))}
        </td>
        <td>
          <span className={`badge badge-${a.status.toLowerCase()}`}>{t.status[a.status]}</span>
          {a.source === 'ADMIN' && <div className="muted">{t.appointments.enteredByAdmin}</div>}
          {(a.status === 'APPROVED' || a.status === 'COMPLETED') &&
            (a.fulfilledAt ? (
              <div className="muted">{fmt(t.appointments.delivered, { time: formatTimestamp(a.fulfilledAt) })}</div>
            ) : (
              a.status === 'APPROVED' && <div className="warning">{t.appointments.notDelivered}</div>
            ))}
        </td>
      </tr>
      <tr className="actions-row">
        <td />
        <td colSpan={4}>
          {a.adminNote && (
            <p className="note">
              {t.appointments.note}: {a.adminNote}
            </p>
          )}
          <AppointmentActions appointment={a} onChanged={onChanged} />
        </td>
      </tr>
    </>
  );
}
