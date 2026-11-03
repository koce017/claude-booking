import { describe, expect, it } from 'vitest';
import { addDays, formatDate, formatRange, localDate, localTime, startOfWeek } from '../format';

describe('format', () => {
  it('shows instants in the company time zone, not the browser one', () => {
    const instant = '2026-11-14T23:30:00Z';
    expect(localDate(instant, 'Europe/Belgrade')).toBe('2026-11-15');
    expect(localTime(instant, 'Europe/Belgrade')).toBe('00:30');
    expect(localDate(instant, 'America/New_York')).toBe('2026-11-14');
    expect(localTime(instant, 'America/New_York')).toBe('18:30');
  });

  it('formats dates the Serbian way', () => {
    expect(formatDate('2026-11-09')).toBe('9.11.2026.');
    expect(formatRange('2026-11-14T09:00:00Z', '2026-11-14T10:00:00Z', 'Europe/Belgrade')).toBe(
      '14.11.2026. 10:00–11:00',
    );
  });

  it('does date arithmetic on calendar dates', () => {
    expect(addDays('2026-12-31', 1)).toBe('2027-01-01');
    expect(addDays('2026-03-29', -1)).toBe('2026-03-28');
    expect(startOfWeek('2026-11-14')).toBe('2026-11-09');
    expect(startOfWeek('2026-11-15')).toBe('2026-11-09');
    expect(startOfWeek('2026-11-09')).toBe('2026-11-09');
  });
});
