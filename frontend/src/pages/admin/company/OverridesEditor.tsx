import { FormEvent, useCallback, useEffect, useState } from 'react';
import { adminApi } from '../../../api/client';
import type { Period, ScheduleOverride } from '../../../api/types';
import { errorMessage } from '../../../errors';
import { addDays, formatDateWithDay, hhmm, todayIso } from '../../../format';
import { t } from '../../../i18n/sr';
import PeriodsInput from './PeriodsInput';

export default function OverridesEditor({ companyId }: { companyId: number }) {
  const [overrides, setOverrides] = useState<ScheduleOverride[] | null>(null);
  const [date, setDate] = useState(todayIso());
  const [closed, setClosed] = useState(true);
  const [periods, setPeriods] = useState<Period[]>([{ start: '09:00', end: '13:00' }]);
  const [note, setNote] = useState('');
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(() => {
    const from = todayIso();
    adminApi
      .overrides(companyId, from, addDays(from, 5 * 366))
      .then(setOverrides)
      .catch((e) => setError(errorMessage(e)));
  }, [companyId]);

  useEffect(load, [load]);

  async function save(e: FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      await adminApi.saveOverride(companyId, date, closed ? [] : periods, note);
      setNote('');
      load();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  async function remove(o: ScheduleOverride) {
    setError(null);
    try {
      await adminApi.removeOverride(companyId, o.date);
      load();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  return (
    <div>
      <p className="muted">{t.overrides.help}</p>
      <form className="panel-form" onSubmit={save}>
        <label>
          {t.overrides.date}
          <input type="date" required value={date} onChange={(e) => setDate(e.target.value)} />
        </label>
        <label className="checkbox">
          <input type="checkbox" checked={closed} onChange={(e) => setClosed(e.target.checked)} />
          {t.overrides.closedAllDay}
        </label>
        {!closed && (
          <div>
            {t.overrides.hours} <PeriodsInput periods={periods} onChange={setPeriods} emptyLabel={t.weekly.closed} />
          </div>
        )}
        <label>
          {t.overrides.note}
          <input maxLength={200} value={note} onChange={(e) => setNote(e.target.value)} />
        </label>
        <div>
          <button type="submit">{t.overrides.add}</button>
        </div>
      </form>
      {error && <p className="error">{error}</p>}

      <h3>{t.overrides.upcoming}</h3>
      {overrides?.length === 0 && <p>{t.overrides.none}</p>}
      {!!overrides?.length && (
        <table>
          <tbody>
            {overrides.map((o) => (
              <tr key={o.date}>
                <td>{formatDateWithDay(o.date)}</td>
                <td>
                  {o.closed
                    ? t.weekly.closed
                    : o.periods.map((p) => `${hhmm(p.start)}–${hhmm(p.end)}`).join(', ')}
                </td>
                <td className="muted">{o.note}</td>
                <td className="actions">
                  <button className="danger" onClick={() => remove(o)}>
                    {t.common.remove}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
