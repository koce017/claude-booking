# Appointment Booking Platform

People request appointments with companies without creating an account. Administrators sign in with a magic link, manage companies and their working hours, and approve or deny requests. Approved appointments are emailed to the company, and customers get email updates in Serbian.

- `backend/`: Spring Boot 3.5 (Java 21) REST API with PostgreSQL, Flyway and Spring Security
- `frontend/`: React 18 + TypeScript (Vite). A deliberately plain UI in Serbian.
- `e2e/`: an end-to-end check that drives the whole application in a browser

The full specification is in [`working-description.md`](working-description.md). What is done and what remains is in [`STATUS.md`](STATUS.md).

## Prerequisites

| Tool       | Version |
|------------|---------|
| Java (JDK) | 21      |
| Node.js    | 20+     |
| PostgreSQL | 14+     |

You don't need to install Maven: the backend includes the Maven Wrapper (`mvnw` / `mvnw.cmd`), which downloads Maven 3.9.11 on first use. Docker is not required.

On Windows, use `mvnw.cmd` wherever this README says `./mvnw`, and set environment variables with `set NAME=value` (cmd) or `$env:NAME="value"` (PowerShell).

## 1. Create the databases

Run this once as a PostgreSQL superuser (for example `sudo -u postgres psql`):

```sql
CREATE USER booking WITH PASSWORD 'booking';
CREATE DATABASE booking OWNER booking;       -- used by the application
CREATE DATABASE booking_test OWNER booking;  -- used by the automated tests
```

Flyway creates and updates all tables on start. You don't need to write any schema by hand.

## 2. Start the backend

macOS / Linux:

```bash
cd backend
ADMIN_BOOTSTRAP_EMAILS=admin@example.com ./mvnw spring-boot:run
```

Windows (PowerShell):

```powershell
cd backend
$env:ADMIN_BOOTSTRAP_EMAILS="admin@example.com"; .\mvnw.cmd spring-boot:run
```

Windows (cmd):

```bat
cd backend
set ADMIN_BOOTSTRAP_EMAILS=admin@example.com
mvnw.cmd spring-boot:run
```

Use the same email on the admin login page. Each time the backend starts, it creates the administrators listed in `ADMIN_BOOTSTRAP_EMAILS` that don't exist yet. Admins are stored in the database, so later starts don't need the variable, and more administrators can be added on the Administratori page.

- The API runs at http://localhost:8080.
- Swagger UI is at http://localhost:8080/swagger-ui.html, and the OpenAPI JSON at `/v3/api-docs`.

To build a jar instead, run `./mvnw package`, then `java -jar target/booking-backend-0.1.0-SNAPSHOT.jar`.

## 3. Start the frontend

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173. The Vite dev server proxies `/api` to `localhost:8080`. Set `BACKEND_URL` to point it somewhere else.

## 4. Sign in as admin (local development)

1. Go to http://localhost:5173/admin and enter `admin@example.com`.
2. By default, mail mode is `log`, so no email is sent. Every email, including the login link, is printed in the backend log:
   `[DEV MAIL] To: admin@example.com ... http://localhost:5173/admin/verify?token=...`
3. Open that link. Each link expires after 15 minutes and works only once.

To send real emails, set `MAIL_MODE=smtp` and the `MAIL_*` variables below.

## What administrators can do

- **Companies:** create, remove (soft delete), and edit every setting: description, address, phone, public email, website, logo URL, time zone, appointment duration, slot interval, capacity and booking window.
- **Fulfillment email:** each company can have an email address that approved appointments are sent to. It is visible only to administrators. A company without one shows "nema email za dostavu", and its approved appointments are marked as not delivered so they can be handed over manually.
- **Working hours:** a weekly schedule with several periods per day, and special dates (closed, or different hours) that take precedence.
- **Customer form:** for each company, phone, email and address can be required, optional or hidden, and custom questions can be added. The name is always required.
- **Appointments:** filter by company, status and dates; approve, deny, cancel, move to another slot, mark as completed or no-show, and add an internal note. Each approved appointment shows whether it was delivered to the company.
- **New appointment:** enter an appointment yourself (e.g. booked by phone). Only the customer's name is required; it is approved immediately and delivered to the company.
- **Calendar:** a week view of all appointments, optionally for one company.
- **Administrators:** add and remove administrators. A removed administrator is signed out immediately; you can't remove yourself.

## Emails

All emails are in Serbian (Latin script). Each one is a text file in [`backend/src/main/resources/mail-templates/`](backend/src/main/resources/mail-templates/), whose README lists when each is sent and which placeholders it can use. To change the texts without rebuilding, copy the files you want to change into a folder and set `MAIL_TEMPLATES_DIR` to it.

| Event | Customer | Administrators | Company (fulfillment email) |
|---|---|---|---|
| Request submitted | Request received | New request | |
| Approved, or entered by an admin | Confirmed | | New appointment with customer details |
| Denied (or company removed) | Not accepted | | |
| Cancelled | Cancelled | | Cancelled, if it had been delivered |
| Moved | Moved (old and new time) | | Moved, if approved |

Customers without an email address get no emails. Sending happens after the change is saved; a failed email is logged and never undoes a booking.

## Running the tests

```bash
cd backend
./mvnw test        # Windows: .\mvnw.cmd test

cd ../frontend
npm test           # unit and component tests (Vitest)
npm run build      # type check and production build
```

The backend integration tests run against the real PostgreSQL database `booking_test` (override with `TEST_DATABASE_URL`, `TEST_DATABASE_USERNAME` and `TEST_DATABASE_PASSWORD`). Each test empties the tables first. They cover availability, slot intervals, capacity, booking validation, customer forms, approval, cancellation, rescheduling, admin-entered appointments, all emails, company settings, weekly schedules, magic-link login, rate limiting, and a concurrency test that races 20 threads for the same slot.

For the end-to-end browser check, see [`e2e/README.md`](e2e/README.md).

GitHub Actions runs the backend and frontend tests on every push (`.github/workflows/ci.yml`).

## Configuration

All settings come from environment variables. There are no secrets in the repository.

| Variable | Default | Purpose |
|---|---|---|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/booking` | JDBC URL |
| `DATABASE_USERNAME` / `DATABASE_PASSWORD` | `booking` / `booking` | DB credentials |
| `SERVER_PORT` | `8080` | HTTP port |
| `FRONTEND_URL` | `http://localhost:5173` | Base URL for links in emails; allowed CORS origin |
| `CORS_ALLOWED_ORIGINS` | `http://127.0.0.1:5173` | Extra allowed browser origins (comma-separated) |
| `ADMIN_BOOTSTRAP_EMAILS` | (empty) | Admins created on startup |
| `BOOKING_DEFAULT_TIMEZONE` | `Europe/Belgrade` | Time zone for companies that have none set |
| `MAIL_MODE` | `log` | `log` prints emails to the log; `smtp` sends them |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | | SMTP settings (used only when `MAIL_MODE=smtp`) |
| `MAIL_TEMPLATES_DIR` | (empty) | Folder with edited email texts |
| `NOTIFY_CUSTOMERS` | `true` | Email customers about their appointments |
| `NOTIFY_ADMINS` | `true` | Email administrators about new requests |
| `FULFILLMENT_MODE` | `email` | `email` sends approved appointments to the company; `manual` sends nothing |
| `MAGIC_LINK_TTL` / `ADMIN_SESSION_TTL` | `15m` / `12h` | Token lifetimes |
| `RATE_LIMIT_ENABLED` | `true` | Per-IP limits: 5 login links and 20 link redemptions per 15 minutes, 20 bookings per hour |
| `SERVER_FORWARD_HEADERS_STRATEGY` | (none) | Set to `native` behind a reverse proxy so rate limits see the real client IP |
| `API_DOCS_ENABLED` | `true` | Set to `false` in production to hide Swagger UI and the OpenAPI JSON |

Expired login tokens and sessions are deleted every night at 03:30.

## Business rules and defaults

A newly created company has only a name. Every other setting is `NULL` in the database and is filled in by the service layer (`CompanySettingsResolver`, `ScheduleService`):

| Setting | Default |
|---|---|
| Appointment duration | 60 minutes |
| Slot interval | Same as the duration. A shorter interval gives overlapping slots (09:00–10:00, 09:30–10:30, …) that share capacity. |
| Max concurrent appointments | 1 |
| Booking window | 12 months, extended to the end of that calendar month |
| Time zone | `Europe/Belgrade` |
| Weekly schedule | Mon–Fri 09:00–17:00 until an administrator saves a schedule. A saved schedule fully defines the week; a day without periods is closed. |
| Customer form | Name, phone and email required; no address; no questions |

Other rules:

- **Statuses.** `PENDING` → `APPROVED` / `DENIED` / `CANCELLED`; `APPROVED` → `CANCELLED` / `COMPLETED` / `NO_SHOW`. `DENIED` and `CANCELLED` free their capacity. `COMPLETED` and `NO_SHOW` can be set only once the appointment has started.
- **Public slot states.** `AVAILABLE`; `PENDING` (capacity remains but requests are pending; still bookable); `UNAVAILABLE` with a reason (`FULLY_BOOKED`, `PAST`, `OUTSIDE_BOOKING_WINDOW`); `NON_WORKING`. The public API never returns customer data or the fulfillment email.
- **Booking validation.** The backend re-checks everything on every booking: company active, slot aligned with the schedule, not in the past, inside the booking window, required customer fields present, and capacity. The client sends only the start time; the end time comes from the company's duration.
- **Concurrency.** Booking, approval and rescheduling lock the company row (`SELECT … FOR UPDATE`) inside one transaction before counting overlapping appointments. Two requests for the last free place can't both succeed; the loser gets `409 SLOT_UNAVAILABLE`.
- **Approval** is refused with `409` if the appointment isn't `PENDING`, is already in the past, or would exceed capacity.
- **Removing a company** is a soft delete. It disappears from public endpoints, its history is kept, and its pending requests are denied automatically.
- **Changing settings** never changes existing appointments; new settings apply to availability and future bookings.

## API overview

All endpoints are documented in Swagger. Errors use RFC 7807 problem JSON with a machine-readable `code` and, for validation errors, an `errors` map per field.

**Public (no authentication)**

| Method | Path | Notes |
|---|---|---|
| GET | `/api/companies` | Active companies, including each one's customer form |
| GET | `/api/companies/{id}` | 404 if the company is unknown or removed |
| GET | `/api/companies/{id}/availability?date=YYYY-MM-DD` | Or `?from=&to=` (up to 62 days) |
| POST | `/api/companies/{id}/appointments` | `{"start", "customer": {"name", "phone", "email", "address", "answers": {key: text}}}` → 201 PENDING / 400 / 404 / 409 / 429 |

**Admin (`Authorization: Bearer <session token>`)**

| Method | Path |
|---|---|
| POST | `/api/admin/auth/request-link` (public; always 202), `/api/admin/auth/verify` (public) |
| GET / POST | `/api/admin/auth/me`, `/api/admin/auth/logout` |
| GET / POST / DELETE | `/api/admin/administrators[/{id}]` |
| GET / POST | `/api/admin/companies` (`?includeInactive=true`) |
| GET / PUT / DELETE | `/api/admin/companies/{id}` |
| GET / PUT / DELETE | `/api/admin/companies/{id}/schedule/weekly` (DELETE = back to the default) |
| GET / PUT / DELETE | `/api/admin/companies/{id}/schedule/overrides[/{date}]` |
| GET / POST | `/api/admin/appointments` (`?companyId=&status=&from=&to=&page=&size=`; POST enters an appointment) |
| GET | `/api/admin/appointments/{id}` |
| POST | `/api/admin/appointments/{id}/approve`, `/deny`, `/cancel`, `/complete`, `/no-show`, `/reschedule` (`{"start"}`) |
| PUT | `/api/admin/appointments/{id}/note` |

The verify step is a `POST` that the frontend's `/admin/verify` page calls, rather than a `GET`, because a `GET` that uses up a single-use token can be triggered by link scanners and prefetching.

## Architecture

The backend is a modular monolith. Controllers call services, services call repositories, and no business logic lives in controllers.

```
com.booking
├── company/        Company entity and settings, CompanySettingsResolver (defaults), customer form config
├── schedule/       Weekly schedule + date overrides, ScheduleService (override > weekly)
├── availability/   SlotGenerator, BookingWindowService, AvailabilityService (single source of truth)
├── appointment/    Booking (locking), approval, cancellation/rescheduling, admin queries, CustomerFieldPolicy
├── fulfillment/    AppointmentFulfillmentService: EmailFulfillmentService (default) or ManualFulfillmentService
├── notification/   NotificationService (customer/admin emails), MagicLinkSender
├── mail/           MailGateway (log / SMTP), MailTemplates, placeholder values for appointments
├── admin/          Administrators, magic-link tokens, sessions, bearer-token filter, cleanup job
├── ratelimit/      Per-IP request limits
├── config/         Security, CORS, OpenAPI, properties, Clock, scheduling
└── common/error/   Exceptions and the RFC 7807 handler
```

### What happens after a change

Each appointment change publishes an event. After the transaction commits, `AppointmentEventListener` calls the fulfillment service (delivery to the company) and, separately, the notification service (emails to customers and admins). A failure in either is logged and never undoes the change. When fulfillment reports that the company was informed, the appointment's `fulfilledAt` is set.

To deliver appointments another way (a webhook, a company API, ...), add another implementation of `AppointmentFulfillmentService` and select it with `FULFILLMENT_MODE`. Booking, availability, approval and the frontend stay unchanged.

### Frontend

- All texts are in [`frontend/src/i18n/sr.ts`](frontend/src/i18n/sr.ts), including messages for the backend's error codes.
- Times are always shown in the company's time zone.
- `components/SlotPicker` (one day of slots) and `components/CustomerFields` (the company's customer form) are shared by the public booking page and the admin pages.

## Known limitations

- On daylight-saving transition days, slots that would not last exactly the configured duration are skipped.
- Bookings are serialized per company, which is simple and correct. Very high traffic for a single company could later use finer-grained locks without any API change.
- Rate limits are counted in memory, per backend instance.
- Production deployment (hosting, HTTPS, real SMTP account) is still to be decided; see `STATUS.md`.
