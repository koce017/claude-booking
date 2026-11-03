// Date and time helpers. Appointment times are always shown in the company's time
// zone, whatever the time zone of the browser.

const LOCALE = 'sr-Latn-RS';

/** "YYYY-MM-DD" of an instant in a time zone. */
export function localDate(instant: string | Date, timeZone: string): string {
  // en-CA formats dates as YYYY-MM-DD.
  return new Intl.DateTimeFormat('en-CA', { timeZone, year: 'numeric', month: '2-digit', day: '2-digit' }).format(
    new Date(instant),
  );
}

/** "HH:mm" of an instant in a time zone. */
export function localTime(instant: string | Date, timeZone: string): string {
  return new Intl.DateTimeFormat('en-GB', { timeZone, hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).format(
    new Date(instant),
  );
}

/** "12.10.2026." for a "YYYY-MM-DD" date. */
export function formatDate(isoDate: string): string {
  const [y, m, d] = isoDate.split('-').map(Number);
  return `${d}.${m}.${y}.`;
}

/** "pon, 12.10.2026." for a "YYYY-MM-DD" date. */
export function formatDateWithDay(isoDate: string): string {
  const weekday = new Intl.DateTimeFormat(LOCALE, { weekday: 'short', timeZone: 'UTC' }).format(
    new Date(`${isoDate}T12:00:00Z`),
  );
  return `${weekday}, ${formatDate(isoDate)}`;
}

/** "12.10.2026. 10:00–11:00" in the company's time zone. */
export function formatRange(start: string, end: string, timeZone: string): string {
  return `${formatDate(localDate(start, timeZone))} ${localTime(start, timeZone)}–${localTime(end, timeZone)}`;
}

/** Date and time of an instant in the browser's time zone, e.g. for "created at". */
export function formatTimestamp(instant: string): string {
  return new Intl.DateTimeFormat(LOCALE, { dateStyle: 'short', timeStyle: 'short' }).format(new Date(instant));
}

/** Today in the browser's time zone as "YYYY-MM-DD". */
export function todayIso(): string {
  const d = new Date();
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

export function addDays(isoDate: string, days: number): string {
  const d = new Date(`${isoDate}T12:00:00Z`);
  d.setUTCDate(d.getUTCDate() + days);
  return d.toISOString().slice(0, 10);
}

/** Monday of the week containing the date. */
export function startOfWeek(isoDate: string): string {
  const day = new Date(`${isoDate}T12:00:00Z`).getUTCDay(); // 0 = Sunday
  return addDays(isoDate, day === 0 ? -6 : 1 - day);
}

/** "09:00:00" -> "09:00". */
export function hhmm(time: string): string {
  return time.slice(0, 5);
}
