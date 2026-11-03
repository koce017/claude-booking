import { useCallback, useEffect, useRef, useState } from 'react';
import { publicApi } from '../api/client';
import type { Availability, SlotView } from '../api/types';
import { errorMessage } from '../errors';
import { addDays, formatDate, formatDateWithDay } from '../format';
import { fmt, slotLabel, t } from '../i18n/sr';

interface Props {
  companyId: number;
  date: string;
  onDateChange: (date: string) => void;
  selected: SlotView | null;
  onSelect: (slot: SlotView | null) => void;
  /** Change this value to reload the slots (e.g. after a failed booking). */
  reloadKey?: number;
}

/** One day of a company's slots, with day navigation. Slot states come from the backend. */
export default function SlotPicker({ companyId, date, onDateChange, selected, onSelect, reloadKey = 0 }: Props) {
  const [availability, setAvailability] = useState<Availability | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Only the most recent request may update the page; an older response arriving
  // late (e.g. after a quick date change) is ignored.
  const latestRequest = useRef(0);
  const load = useCallback(() => {
    const requestId = ++latestRequest.current;
    setAvailability(null);
    setError(null);
    publicApi
      .availability(companyId, date)
      .then((result) => {
        if (requestId === latestRequest.current) setAvailability(result);
      })
      .catch((e) => {
        if (requestId === latestRequest.current) setError(errorMessage(e));
      });
  }, [companyId, date]);

  // Reload, and clear the selection, whenever the company, date or reloadKey changes.
  useEffect(() => {
    onSelect(null);
    load();
  }, [load, reloadKey]); // onSelect is deliberately not a dependency

  const bookingWindow = availability?.bookingWindow;
  const day = availability?.days[0];

  return (
    <div className="slot-picker">
      <div className="date-bar">
        <button
          type="button"
          className="secondary"
          onClick={() => onDateChange(addDays(date, -1))}
          disabled={!!bookingWindow && date <= bookingWindow.firstDate}
        >
          {t.booking.previousDay}
        </button>
        <input
          type="date"
          aria-label={formatDateWithDay(date)}
          value={date}
          min={bookingWindow?.firstDate}
          max={bookingWindow?.lastDate}
          onChange={(e) => e.target.value && onDateChange(e.target.value)}
        />
        <button
          type="button"
          className="secondary"
          onClick={() => onDateChange(addDays(date, 1))}
          disabled={!!bookingWindow && date >= bookingWindow.lastDate}
        >
          {t.booking.nextDay}
        </button>
        <strong>{formatDateWithDay(date)}</strong>
      </div>
      {availability && (
        <p className="muted">
          {fmt(t.booking.info, {
            timezone: availability.timezone,
            duration: availability.appointmentDurationMinutes,
            lastDate: formatDate(availability.bookingWindow.lastDate),
          })}
        </p>
      )}
      {error && <p className="error">{error}</p>}
      {!availability && !error && <p>{t.booking.loadingSlots}</p>}
      {day && (
        <>
          {!day.working && <p>{t.booking.closedDay}</p>}
          <ul className="slots">
            {day.slots.map((slot) => (
              <li key={`${slot.start}-${slot.status}`}>
                <button
                  type="button"
                  className={`slot slot-${slot.status.toLowerCase()} ${selected?.start === slot.start ? 'selected' : ''}`}
                  disabled={!slot.bookable}
                  aria-pressed={selected?.start === slot.start}
                  onClick={() => onSelect(slot)}
                >
                  <span className="slot-time">
                    {slot.localStart}–{slot.localEnd}
                  </span>
                  <span className="slot-status">{slotLabel(slot)}</span>
                </button>
              </li>
            ))}
          </ul>
        </>
      )}
    </div>
  );
}
