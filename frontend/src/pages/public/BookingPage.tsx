import { FormEvent, useCallback, useEffect, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiError, publicApi } from '../../api/client';
import type { Availability, BookingResult, Company, CustomerInput, SlotView } from '../../api/types';
import { slotLabel } from './statusLabels';

function todayIso(): string {
  const d = new Date();
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

function shiftDate(iso: string, days: number): string {
  const d = new Date(`${iso}T12:00:00Z`);
  d.setUTCDate(d.getUTCDate() + days);
  return d.toISOString().slice(0, 10);
}

const emptyCustomer: CustomerInput = { name: '', phone: '', email: '' };

export default function BookingPage() {
  const companyId = Number(useParams().companyId);
  const [company, setCompany] = useState<Company | null>(null);
  const [date, setDate] = useState(todayIso());
  const [availability, setAvailability] = useState<Availability | null>(null);
  const [selected, setSelected] = useState<SlotView | null>(null);
  const [customer, setCustomer] = useState<CustomerInput>(emptyCustomer);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [booked, setBooked] = useState<{ result: BookingResult; slot: SlotView; date: string } | null>(null);

  useEffect(() => {
    publicApi.company(companyId).then(setCompany).catch((e: Error) => setError(e.message));
  }, [companyId]);

  // Only the most recent availability request may update the page; an older
  // response arriving late (e.g. after a quick date change) is ignored.
  const latestRequest = useRef(0);
  const loadAvailability = useCallback(() => {
    const requestId = ++latestRequest.current;
    setAvailability(null);
    return publicApi
      .availability(companyId, date)
      .then((result) => {
        if (requestId === latestRequest.current) setAvailability(result);
      })
      .catch((e: Error) => {
        if (requestId === latestRequest.current) setError(e.message);
      });
  }, [companyId, date]);

  useEffect(() => {
    setSelected(null);
    setError(null);
    loadAvailability();
  }, [loadAvailability]);

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (!selected) return;
    setSubmitting(true);
    setError(null);
    setFieldErrors({});
    try {
      const result = await publicApi.book(companyId, selected.start, {
        name: customer.name.trim(),
        phone: customer.phone.trim(),
        email: customer.email.trim(),
      });
      setBooked({ result, slot: selected, date });
      setCustomer(emptyCustomer);
      setSelected(null);
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.status === 409 ? 'Sorry, this slot was just taken. Please choose another time.' : err.message);
        setFieldErrors(err.fieldErrors);
        if (err.status === 409 || err.code === 'SLOT_IN_PAST') {
          setSelected(null);
          loadAvailability();
        }
      } else {
        setError('Could not submit the request. Please try again.');
      }
    } finally {
      setSubmitting(false);
    }
  }

  if (booked) {
    return (
      <section>
        <h1>Request submitted</h1>
        <p className="success">
          Your appointment request with <strong>{company?.name}</strong> for {booked.date}{' '}
          {booked.slot.localStart}–{booked.slot.localEnd} has been received (reference #{booked.result.id}).
        </p>
        <p>Its status is <strong>pending</strong>: an administrator will review it.</p>
        <button
          onClick={() => {
            setBooked(null);
            loadAvailability();
          }}
        >
          Book another time
        </button>{' '}
        <Link to="/">Back to companies</Link>
      </section>
    );
  }

  const bookingWindow = availability?.bookingWindow;
  const day = availability?.days[0];

  return (
    <section>
      <p><Link to="/">← All companies</Link></p>
      <h1>{company?.name ?? 'Loading…'}</h1>

      <div className="date-bar">
        <button onClick={() => setDate(shiftDate(date, -1))} disabled={!!bookingWindow && date <= bookingWindow.firstDate}>
          ← Previous day
        </button>
        <input
          type="date"
          value={date}
          min={bookingWindow?.firstDate}
          max={bookingWindow?.lastDate}
          onChange={(e) => e.target.value && setDate(e.target.value)}
        />
        <button onClick={() => setDate(shiftDate(date, 1))} disabled={!!bookingWindow && date >= bookingWindow.lastDate}>
          Next day →
        </button>
      </div>
      {availability && (
        <p className="muted">
          Times are shown in the company's time zone ({availability.timezone}). Appointments last{' '}
          {availability.appointmentDurationMinutes} minutes. Bookable until {bookingWindow?.lastDate}.
        </p>
      )}

      {error && <p className="error">{error}</p>}
      {!availability && !error && <p>Loading availability…</p>}

      {day && (
        <>
          {!day.working && <p>The company does not work on this day.</p>}
          <ul className="slots">
            {day.slots.map((slot) => (
              <li key={slot.start}>
                <button
                  className={`slot slot-${slot.status.toLowerCase()} ${selected?.start === slot.start ? 'selected' : ''}`}
                  disabled={!slot.bookable}
                  onClick={() => {
                    setSelected(slot);
                    setError(null);
                  }}
                >
                  <span className="slot-time">{slot.localStart}–{slot.localEnd}</span>
                  <span className="slot-status">{slotLabel(slot)}</span>
                </button>
              </li>
            ))}
          </ul>
        </>
      )}

      {selected && (
        <form className="booking-form" onSubmit={submit}>
          <h2>Request {date} {selected.localStart}–{selected.localEnd}</h2>
          {selected.pendingRequests && (
            <p className="muted">Other requests are already pending for this time; you can still request it.</p>
          )}
          <label>
            Name
            <input required maxLength={200} value={customer.name}
              onChange={(e) => setCustomer({ ...customer, name: e.target.value })} />
            {fieldErrors['customer.name'] && <span className="error">{fieldErrors['customer.name']}</span>}
          </label>
          <label>
            Phone
            <input required type="tel" maxLength={50} pattern="[0-9+()\-./ ]+" value={customer.phone}
              onChange={(e) => setCustomer({ ...customer, phone: e.target.value })} />
            {fieldErrors['customer.phone'] && <span className="error">{fieldErrors['customer.phone']}</span>}
          </label>
          <label>
            Email
            <input required type="email" maxLength={320} value={customer.email}
              onChange={(e) => setCustomer({ ...customer, email: e.target.value })} />
            {fieldErrors['customer.email'] && <span className="error">{fieldErrors['customer.email']}</span>}
          </label>
          <button type="submit" disabled={submitting}>{submitting ? 'Submitting…' : 'Submit request'}</button>{' '}
          <button type="button" className="secondary" onClick={() => setSelected(null)}>Cancel</button>
        </form>
      )}
    </section>
  );
}
