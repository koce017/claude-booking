import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { adminApi } from '../../api/client';
import type { AdminAppointment, AdminCompany } from '../../api/types';
import { errorMessage } from '../../errors';
import { addDays, formatDateWithDay, localDate, localTime, startOfWeek, todayIso } from '../../format';
import { fmt, t } from '../../i18n/sr';

const PAGE_SIZE = 500;

/**
 * A week of appointments, one column per day. Each appointment is placed on its
 * company-local date and shows company-local times.
 */
export default function AdminCalendarPage() {
  const [weekStart, setWeekStart] = useState(() => startOfWeek(todayIso()));
  const [companies, setCompanies] = useState<AdminCompany[]>([]);
  const [companyId, setCompanyId] = useState<number | undefined>();
  const [hideInactive, setHideInactive] = useState(true);
  const [appointments, setAppointments] = useState<AdminAppointment[] | null>(null);
  const [truncated, setTruncated] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const days = Array.from({ length: 7 }, (_, i) => addDays(weekStart, i));

  useEffect(() => {
    adminApi.companies(true).then(setCompanies).catch((e) => setError(errorMessage(e)));
  }, []);

  useEffect(() => {
    setAppointments(null);
    setError(null);
    // A day either side, since company time zones may differ from the browser's.
    const from = new Date(`${addDays(weekStart, -1)}T00:00:00Z`).toISOString();
    const to = new Date(`${addDays(weekStart, 8)}T00:00:00Z`).toISOString();
    adminApi
      .appointments({ companyId, from, to }, PAGE_SIZE)
      .then((page) => {
        setAppointments(page.items);
        setTruncated(page.totalElements > page.items.length);
      })
      .catch((e) => setError(errorMessage(e)));
  }, [weekStart, companyId]);

  const visible = (appointments ?? []).filter(
    (a) => !hideInactive || !['DENIED', 'CANCELLED'].includes(a.status),
  );
  const onDay = (day: string) => visible.filter((a) => localDate(a.start, a.companyTimezone) === day);

  return (
    <section className="wide">
      <h1>{t.calendar.title}</h1>
      <div className="inline-form">
        <button className="secondary" onClick={() => setWeekStart(addDays(weekStart, -7))}>
          {t.calendar.previousWeek}
        </button>
        <button className="secondary" onClick={() => setWeekStart(startOfWeek(todayIso()))}>
          {t.calendar.thisWeek}
        </button>
        <button className="secondary" onClick={() => setWeekStart(addDays(weekStart, 7))}>
          {t.calendar.nextWeek}
        </button>
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
        <label className="checkbox">
          <input type="checkbox" checked={hideInactive} onChange={(e) => setHideInactive(e.target.checked)} />
          {t.calendar.hideInactive}
        </label>
      </div>
      {error && <p className="error">{error}</p>}
      {!appointments && !error && <p>{t.common.loading}</p>}
      {truncated && <p className="muted">{fmt(t.calendar.tooMany, { count: PAGE_SIZE })}</p>}
      {appointments && (
        <div className="calendar">
          {days.map((day) => (
            <div key={day} className={`calendar-day ${day === todayIso() ? 'today' : ''}`}>
              <h3>{formatDateWithDay(day)}</h3>
              {onDay(day).length === 0 && <p className="muted">{t.calendar.noAppointments}</p>}
              {onDay(day).map((a) => (
                <Link
                  key={a.id}
                  to={`/admin/appointments?id=${a.id}`}
                  className={`calendar-item status-${a.status.toLowerCase()}`}
                >
                  <strong>
                    {localTime(a.start, a.companyTimezone)}–{localTime(a.end, a.companyTimezone)}
                  </strong>{' '}
                  {a.customer.name}
                  <br />
                  <span className="muted">{a.companyName}</span>
                  <br />
                  <span className={`badge badge-${a.status.toLowerCase()}`}>{t.status[a.status]}</span>
                </Link>
              ))}
            </div>
          ))}
        </div>
      )}
    </section>
  );
}
