import { chromium } from 'playwright';
import fs from 'node:fs';

// End-to-end check of the main flows in a real browser. See e2e/README.md.
// Needs a running backend (MAIL_MODE=log, admin@example.com bootstrapped, empty
// database) whose log is written to BACKEND_LOG, and the frontend at BASE_URL.

const BASE = process.env.BASE_URL ?? 'http://localhost:5173';
const LOG_FILE = process.env.BACKEND_LOG ?? 'backend.log';
const log = () => fs.readFileSync(LOG_FILE, 'utf8');
const shots = process.env.SCREENSHOTS_DIR ?? 'screenshots';
fs.mkdirSync(shots, { recursive: true });

// Saturday of next week (the calendar step opens "next week"), and dates derived from it.
const iso = (d) => d.toISOString().slice(0, 10);
const day = new Date();
const daysToNextMonday = (8 - day.getUTCDay()) % 7 || 7;
day.setUTCDate(day.getUTCDate() + daysToNextMonday + 5);
const SATURDAY = iso(day);
const [y, m, d] = SATURDAY.split('-').map(Number);
const SATURDAY_SR = `${d}.${m}.${y}.`;
const holiday = new Date(day);
holiday.setUTCDate(holiday.getUTCDate() + 30);
const HOLIDAY = iso(holiday);

const browser = await chromium.launch(
  process.env.CHROMIUM_PATH ? { executablePath: process.env.CHROMIUM_PATH } : {},
);
const page = await browser.newPage({ viewport: { width: 1400, height: 1000 } });
page.on('dialog', (d) => d.accept());
page.on('pageerror', (e) => console.log('PAGE ERROR', e.message));
const step = (s) => console.log('✓', s);

// Admin login through the magic link printed in the log.
await page.goto(`${BASE}/admin/login`);
await page.getByLabel('Email').fill('admin@example.com');
await page.getByRole('button', { name: 'Pošalji link za prijavu' }).click();
await page.getByText('poslali smo link za prijavu').waitFor();
await page.waitForTimeout(500);
const link = [...log().matchAll(/(http:\/\/localhost:5173\/admin\/verify\?token=\S+)/g)].pop()[1];
await page.goto(link);
await page.getByRole('heading', { name: 'Termini' }).waitFor();
step('admin login');

// Company with settings.
await page.getByRole('link', { name: 'Firme' }).click();
await page.getByLabel('Naziv firme').fill('Frizer Ana');
await page.getByRole('button', { name: 'Dodaj firmu' }).click();
await page.getByRole('link', { name: 'Frizer Ana' }).click();
await page.getByLabel('Email za dostavu termina').fill('termini@frizer-ana.rs');
await page.getByLabel('Adresa').fill('Zmaj Jovina 1, Novi Sad');
await page.getByRole('button', { name: 'Sačuvaj' }).click();
await page.getByText('Sačuvano.').waitFor();
step('company settings saved');

await page.getByRole('tab', { name: 'Podaci o klijentu' }).click();
await page.getByLabel('Adresa').selectOption('OPTIONAL');
await page.getByRole('button', { name: '+ Dodaj pitanje' }).click();
await page.getByLabel('Pitanje').fill('Dužina kose');
await page.getByLabel('Obavezno', { exact: true }).check();
await page.getByRole('button', { name: 'Sačuvaj' }).click();
await page.getByText('Sačuvano.').waitFor();
step('customer form saved');

await page.getByRole('tab', { name: 'Radno vreme' }).click();
await page.getByText('Koristi se podrazumevano radno vreme').waitFor();
const saturday = page.locator('tr', { hasText: 'Subota' });
await saturday.getByRole('button', { name: '+ period' }).click();
await saturday.locator('input[type=time]').first().fill('10:00');
await saturday.locator('input[type=time]').last().fill('14:00');
await page.getByRole('button', { name: 'Sačuvaj' }).click();
await page.getByText('Sačuvano je posebno radno vreme.').waitFor();
await page.screenshot({ path: `${shots}/company-weekly.png`, fullPage: true });
step('weekly schedule saved');

await page.getByRole('tab', { name: 'Posebni datumi' }).click();
await page.getByLabel('Datum').fill(HOLIDAY);
await page.getByLabel('Napomena').fill('Božić');
await page.getByRole('button', { name: 'Sačuvaj datum' }).click();
await page.getByRole('cell', { name: 'Božić' }).waitFor();
step('special date saved');

// Public booking.
const pub = await browser.newPage({ viewport: { width: 1000, height: 1000 } });
await pub.goto(BASE);
await pub.getByRole('link', { name: 'Frizer Ana' }).click();
await pub.locator('input[type=date]').fill(SATURDAY);
await pub.getByRole('button', { name: /^10:00–11:00/ }).click();
await pub.getByLabel(/Ime i prezime/).fill('Ana Anić');
await pub.getByLabel(/Telefon/).fill('+381 60 1234567');
await pub.getByLabel(/Email/).fill('ana@example.com');
await pub.getByLabel(/Dužina kose/).fill('Duga');
await pub.screenshot({ path: `${shots}/public-booking.png`, fullPage: true });
await pub.getByRole('button', { name: 'Pošalji zahtev' }).click();
await pub.getByRole('heading', { name: 'Zahtev je poslat' }).waitFor();
step('public booking on Saturday');
await pub.waitForTimeout(500);
if (!log().includes('Primili smo vaš zahtev za termin – Frizer Ana')) throw new Error('no customer email');
if (!log().includes('Novi zahtev za termin – Frizer Ana')) throw new Error('no admin email');
step('customer + admin emails logged');

// Approve.
await page.getByRole('link', { name: 'Termini', exact: true }).click();
const row = page.locator('tr.main-row', { hasText: 'Ana Anić' });
await row.getByText('Dužina kose: Duga').waitFor();
await page.getByRole('button', { name: 'Odobri' }).click();
await page.getByText(/Dostavljeno firmi/).waitFor();
await page.waitForTimeout(300);
if (!log().includes('To: termini@frizer-ana.rs')) throw new Error('no fulfillment email');
step('approved and delivered to company');

// Reschedule to 12:00 the same day.
await page.getByRole('button', { name: 'Pomeri' }).click();
await page.getByRole('button', { name: /^12:00–13:00/ }).click();
await page.getByRole('button', { name: `Pomeri na ${SATURDAY_SR} 12:00` }).click();
await page.getByText(`${SATURDAY_SR} 12:00–13:00`).waitFor();
await page.waitForTimeout(300);
if (!log().includes('Pomeren termin – sada')) throw new Error('no reschedule email');
await page.screenshot({ path: `${shots}/admin-appointments.png`, fullPage: true });
step('rescheduled');

// Admin-entered appointment.
await page.getByRole('link', { name: 'Novi termin' }).click();
await page.getByLabel('Firma').selectOption({ label: 'Frizer Ana' });
await page.locator('input[type=date]').fill(SATURDAY);
await page.getByRole('button', { name: /^10:00–11:00/ }).click();
await page.getByLabel(/Ime i prezime/).fill('Telefonski Klijent');
await page.getByLabel('Interna napomena').fill('Zakazao telefonom');
await page.getByRole('button', { name: 'Unesi termin' }).click();
await page.getByText(/je unet/).waitFor();
step('admin-entered appointment');

// Calendar.
await page.getByRole('link', { name: 'Kalendar' }).click();
await page.getByRole('button', { name: 'Sledeća nedelja →' }).click();
await page.locator('.calendar-item', { hasText: 'Telefonski Klijent' }).waitFor();
await page.locator('.calendar-item', { hasText: 'Ana Anić' }).waitFor();
await page.screenshot({ path: `${shots}/admin-calendar.png`, fullPage: true });
step('calendar shows both');

// Cancel from the appointment page (via calendar link).
await page.locator('.calendar-item', { hasText: 'Ana Anić' }).click();
await page.getByRole('button', { name: 'Otkaži' }).click();
await page.locator('.badge', { hasText: 'Otkazan' }).waitFor();
step('cancelled');

await browser.close();
console.log('ALL OK');
