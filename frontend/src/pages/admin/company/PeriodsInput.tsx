import type { Period } from '../../../api/types';
import { t } from '../../../i18n/sr';

interface Props {
  periods: Period[];
  onChange: (periods: Period[]) => void;
  emptyLabel: string;
}

/** Editable list of working periods ("from–to") for one day. */
export default function PeriodsInput({ periods, onChange, emptyLabel }: Props) {
  const update = (i: number, change: Partial<Period>) =>
    onChange(periods.map((p, j) => (j === i ? { ...p, ...change } : p)));

  return (
    <div className="periods">
      {periods.length === 0 && <span className="muted">{emptyLabel}</span>}
      {periods.map((p, i) => (
        <span key={i} className="period">
          <input type="time" required value={p.start} onChange={(e) => update(i, { start: e.target.value })} />–
          <input type="time" required value={p.end} onChange={(e) => update(i, { end: e.target.value })} />
          <button
            type="button"
            className="secondary small"
            aria-label={t.common.remove}
            onClick={() => onChange(periods.filter((_, j) => j !== i))}
          >
            ×
          </button>
        </span>
      ))}
      <button
        type="button"
        className="secondary small"
        onClick={() => {
          const last = periods[periods.length - 1];
          onChange([...periods, last ? { start: last.end, end: last.end } : { start: '09:00', end: '17:00' }]);
        }}
      >
        {t.weekly.addPeriod}
      </button>
    </div>
  );
}
