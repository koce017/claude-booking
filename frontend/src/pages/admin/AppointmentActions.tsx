import { useState } from 'react';
import { adminApi } from '../../api/client';
import type { AdminAppointment, SlotView } from '../../api/types';
import SlotPicker from '../../components/SlotPicker';
import { errorMessage } from '../../errors';
import { formatDate, localDate } from '../../format';
import { fmt, t } from '../../i18n/sr';

interface Props {
  appointment: AdminAppointment;
  onChanged: (updated: AdminAppointment) => void;
}

/** The actions an administrator can take on an appointment in its current status. */
export default function AppointmentActions({ appointment: a, onChanged }: Props) {
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [rescheduling, setRescheduling] = useState(false);
  const [date, setDate] = useState(() => localDate(a.start, a.companyTimezone));
  const [slot, setSlot] = useState<SlotView | null>(null);

  const started = new Date(a.start) <= new Date();
  const active = a.status === 'PENDING' || a.status === 'APPROVED';

  async function run(action: () => Promise<AdminAppointment>, confirmText?: string) {
    if (confirmText && !confirm(confirmText)) return;
    setBusy(true);
    setError(null);
    try {
      onChanged(await action());
      setRescheduling(false);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  function editNote() {
    const note = prompt(t.appointments.note, a.adminNote ?? '');
    if (note !== null) run(() => adminApi.updateNote(a.id, note));
  }

  return (
    <div className="appointment-actions">
      <div className="buttons">
        {a.status === 'PENDING' && (
          <>
            <button disabled={busy} onClick={() => run(() => adminApi.approve(a.id))}>
              {t.appointments.approve}
            </button>
            <button
              className="danger"
              disabled={busy}
              onClick={() => run(() => adminApi.deny(a.id), fmt(t.appointments.denyConfirm, { id: a.id }))}
            >
              {t.appointments.deny}
            </button>
          </>
        )}
        {a.status === 'APPROVED' && started && (
          <>
            <button disabled={busy} onClick={() => run(() => adminApi.complete(a.id))}>
              {t.appointments.complete}
            </button>
            <button className="secondary" disabled={busy} onClick={() => run(() => adminApi.noShow(a.id))}>
              {t.appointments.noShow}
            </button>
          </>
        )}
        {active && (
          <>
            <button className="secondary" disabled={busy} onClick={() => setRescheduling(!rescheduling)}>
              {t.appointments.reschedule}
            </button>
            <button
              className="danger"
              disabled={busy}
              onClick={() => run(() => adminApi.cancel(a.id), fmt(t.appointments.cancelConfirm, { id: a.id }))}
            >
              {t.appointments.cancel}
            </button>
          </>
        )}
        <button className="secondary" disabled={busy} onClick={editNote}>
          {t.appointments.editNote}
        </button>
      </div>
      {error && <p className="error">{error}</p>}
      {rescheduling && (
        <div className="panel-form">
          <h3>{fmt(t.appointments.rescheduleTitle, { id: a.id })}</h3>
          <SlotPicker companyId={a.companyId} date={date} onDateChange={setDate} selected={slot} onSelect={setSlot} />
          <div>
            <button disabled={!slot || busy} onClick={() => slot && run(() => adminApi.reschedule(a.id, slot.start))}>
              {slot
                ? fmt(t.appointments.rescheduleConfirm, { date: formatDate(date), time: slot.localStart })
                : t.newAppointment.chooseSlot}
            </button>{' '}
            <button className="secondary" onClick={() => setRescheduling(false)}>
              {t.common.cancel}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
