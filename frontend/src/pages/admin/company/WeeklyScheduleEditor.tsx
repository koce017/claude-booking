import { useEffect, useState } from 'react';
import { adminApi } from '../../../api/client';
import type { WeeklySchedule } from '../../../api/types';
import { errorMessage } from '../../../errors';
import { hhmm } from '../../../format';
import { t } from '../../../i18n/sr';
import PeriodsInput from './PeriodsInput';

export default function WeeklyScheduleEditor({ companyId }: { companyId: number }) {
  const [schedule, setSchedule] = useState<WeeklySchedule | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const show = (s: WeeklySchedule) =>
    setSchedule({ ...s, days: s.days.map((d) => ({ ...d, periods: d.periods.map((p) => ({ start: hhmm(p.start), end: hhmm(p.end) })) })) });

  useEffect(() => {
    adminApi.weeklySchedule(companyId).then(show).catch((e) => setError(errorMessage(e)));
  }, [companyId]);

  if (!schedule) return <p>{error ?? t.common.loading}</p>;

  async function save() {
    setError(null);
    setMessage(null);
    try {
      show(await adminApi.saveWeeklySchedule(companyId, schedule!.days));
      setMessage(t.common.saved);
    } catch (e) {
      setError(errorMessage(e));
    }
  }

  async function reset() {
    if (!confirm(t.weekly.resetConfirm)) return;
    setError(null);
    setMessage(null);
    try {
      show(await adminApi.resetWeeklySchedule(companyId));
    } catch (e) {
      setError(errorMessage(e));
    }
  }

  return (
    <div>
      <p className="muted">{t.weekly.help}</p>
      <p>{schedule.custom ? t.weekly.custom : t.weekly.usingDefault}</p>
      <table className="weekly">
        <tbody>
          {schedule.days.map((d, i) => (
            <tr key={d.day}>
              <th>{t.days[d.day]}</th>
              <td>
                <PeriodsInput
                  periods={d.periods}
                  emptyLabel={t.weekly.closed}
                  onChange={(periods) =>
                    setSchedule({ ...schedule, days: schedule.days.map((x, j) => (j === i ? { ...x, periods } : x)) })
                  }
                />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <p>
        <button onClick={save}>{t.common.save}</button>{' '}
        {schedule.custom && (
          <button className="secondary" onClick={reset}>
            {t.weekly.reset}
          </button>
        )}
      </p>
      {message && <p className="success">{message}</p>}
      {error && <p className="error">{error}</p>}
    </div>
  );
}
