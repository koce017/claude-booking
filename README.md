# Appointment Booking Platform

People request appointments with companies without creating an account. Administrators sign in with a magic link, manage companies and approve or deny requests.

- `backend/`: Spring Boot 3.5 (Java 21) REST API with PostgreSQL, Flyway and Spring Security
- `frontend/`: React 18 + TypeScript (Vite). This is a minimal UI for testing the booking flow.

The full specification is in [`working-description.md`](working-description.md).

## Prerequisites

| Tool       | Version |
|------------|---------|
| Java (JDK) | 21      |
| Maven      | 3.9+    |
| Node.js    | 20+     |
| PostgreSQL | 14+     |

Docker is not required.

## 1. Create the databases

Run this once as a PostgreSQL superuser (for example `sudo -u postgres psql`):

```sql
CREATE USER booking WITH PASSWORD 'booking';
CREATE DATABASE booking OWNER booking;       -- used by the application
CREATE DATABASE booking_test OWNER booking;  -- used by the automated tests
```

Flyway creates all tables on first start. You don't need to write any schema by hand.

## 2. Start the backend

```bash
cd backend
ADMIN_BOOTSTRAP_EMAILS=admin@example.com mvn spring-boot:run
```

- The API runs at http://localhost:8080.
- Swagger UI is at http://localhost:8080/swagger-ui.html, and the OpenAPI JSON at `/v3/api-docs`.
- `ADMIN_BOOTSTRAP_EMAILS` (comma-separated) creates those administrators on startup if they don't exist. There is no admin registration.

To build a jar instead, run `mvn package`, then `java -jar target/booking-backend-0.1.0-SNAPSHOT.jar`.

## 3. Start the frontend

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173. The Vite dev server proxies `/api` to `localhost:8080`. Set `BACKEND_URL` to point it somewhere else.

## 4. Sign in as admin (local development)

1. Go to http://localhost:5173/admin and enter `admin@example.com`.
2. By default, mail mode is `log`, so no email is sent. The backend log prints:
   `[DEV MAIL] Magic login link for admin@example.com: http://localhost:5173/admin/verify?token=...`
3. Open that link. Each link expires after 15 minutes and works only once.

To send real emails, set `MAIL_MODE=smtp` and the `MAIL_*` variables below.

## Running the tests

```bash
cd backend
mvn test
```

The integration tests run against the real PostgreSQL database `booking_test` (override with `TEST_DATABASE_URL`, `TEST_DATABASE_USERNAME` and `TEST_DATABASE_PASSWORD`). Each test empties the tables first. The tests cover:

- availability: slots, weekends, special dates, past slots, the booking window, duration
- capacity: 1 and 3, and denied requests freeing capacity
- booking validation
- approval and denial rules
- magic-link authentication
- a concurrency test that races 20 threads for the same slot. It fails if the booking lock is removed; this was checked by removing the lock.

Frontend type check and production build: `cd frontend && npm run build`.

## Configuration

All settings come from environment variables. There are no secrets in the repository.

| Variable | Default | Purpose |
|---|---|---|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/booking` | JDBC URL |
| `DATABASE_USERNAME` / `DATABASE_PASSWORD` | `booking` / `booking` | DB credentials |
| `SERVER_PORT` | `8080` | HTTP port |
| `FRONTEND_URL` | `http://localhost:5173` | Base URL for magic links; allowed CORS origin |
| `ADMIN_BOOTSTRAP_EMAILS` | (empty) | Admins created on startup |
| `BOOKING_DEFAULT_TIMEZONE` | `Europe/Belgrade` | Time zone for companies that have none set |
| `MAIL_MODE` | `log` | `log` prints magic links to the log; `smtp` sends email |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | | SMTP settings (used only when `MAIL_MODE=smtp`) |
| `MAGIC_LINK_TTL` / `ADMIN_SESSION_TTL` | `15m` / `12h` | Token lifetimes |

## Business rules and defaults

A newly created company has only a name. Every other setting is `NULL` in the database and is filled in by the service layer (`CompanySettingsResolver`, `ScheduleService`):

| Setting | Default |
|---|---|
| Appointment duration | 60 minutes. The slot interval equals the duration. |
| Max concurrent appointments | 1 |
| Booking window | 12 months, extended to the end of that calendar month. Today is 2026‑10‑08, so bookings are allowed through 2027‑10‑31. |
| Time zone | `Europe/Belgrade` |
| Weekly schedule | Mon–Fri 09:00–17:00; Saturday and Sunday closed. Used only while the company has no weekly schedule rows. Once any rows exist, they fully define the week. |

Other rules:

- **Statuses.** Requests start as `PENDING`. `PENDING` and `APPROVED` consume capacity; `DENIED` doesn't.
- **Public slot states.** A slot is shown as one of:
  - `AVAILABLE`
  - `PENDING`: capacity remains but requests are already pending. It can still be booked.
  - `UNAVAILABLE`, with a reason: `FULLY_BOOKED`, `PAST` or `OUTSIDE_BOOKING_WINDOW`
  - `NON_WORKING`

  The public API never returns customer data.
- **Booking validation.** The backend re-checks everything on every booking: company active, slot aligned with the schedule, not in the past, inside the booking window, required customer fields present, and capacity. The client sends only the start time; the end time comes from the company's duration.
- **Concurrency.** Booking and approval both lock the company row (`SELECT … FOR UPDATE`) inside one transaction before counting overlapping `PENDING`/`APPROVED` appointments. Two requests for the last free place therefore can't both succeed; the loser gets `409 SLOT_UNAVAILABLE`.
- **Approval** is refused with `409` if the appointment isn't `PENDING`, is already in the past, or would exceed capacity (for example, after capacity was lowered).
- **Removing a company** is a soft delete. The company disappears from public endpoints, its history is kept, and its pending requests are set to `DENIED` automatically.
- **Special dates.** Overrides for a single date take precedence over the weekly schedule. An override with no periods means the company is closed that day.

### Example: close January 7 and 8

The admin frontend has no form for special dates yet. Use the API (with a session token from `/api/admin/auth/verify`):

```bash
curl -X PUT -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
     -d '{"note":"closed"}' http://localhost:8080/api/admin/companies/1/schedule/overrides/2027-01-07
curl -X PUT -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
     -d '{"note":"closed"}' http://localhost:8080/api/admin/companies/1/schedule/overrides/2027-01-08
# Shorter hours instead of closed:
#   -d '{"periods":[{"start":"09:00","end":"13:00"}]}'
```

## API overview

All endpoints are documented in Swagger. Errors use RFC 7807 problem JSON with a machine-readable `code` and, for validation errors, an `errors` map per field.

**Public (no authentication)**

| Method | Path | Notes |
|---|---|---|
| GET | `/api/companies` | Active companies |
| GET | `/api/companies/{id}` | 404 if the company is unknown or removed |
| GET | `/api/companies/{id}/availability?date=YYYY-MM-DD` | Or `?from=&to=` (up to 62 days) |
| POST | `/api/companies/{id}/appointments` | `{"start": "...", "customer": {"name", "phone", "email"}}` → 201 PENDING / 400 / 404 / 409 |

**Admin (`Authorization: Bearer <session token>`)**

| Method | Path |
|---|---|
| POST | `/api/admin/auth/request-link` (public; always returns 202) |
| POST | `/api/admin/auth/verify` (public; exchanges the magic-link token for a session token) |
| GET / POST | `/api/admin/auth/me`, `/api/admin/auth/logout` |
| GET / POST | `/api/admin/companies` (`?includeInactive=true`) |
| GET / DELETE | `/api/admin/companies/{id}` |
| GET / PUT / DELETE | `/api/admin/companies/{id}/schedule/overrides[/{date}]` |
| GET | `/api/admin/appointments?companyId=&status=&from=&to=&page=&size=` |
| GET | `/api/admin/appointments/{id}` |
| POST | `/api/admin/appointments/{id}/approve`, `/api/admin/appointments/{id}/deny` |

The verify step is a `POST` that the frontend's `/admin/verify` page calls, rather than the `GET` the spec suggests. A `GET` that uses up a single-use token can be triggered by link scanners and prefetching.

## Architecture

The backend is a modular monolith. Controllers call services, services call repositories, and no business logic lives in controllers.

```
com.booking
├── company/        Company entity, CRUD, CompanySettingsResolver (defaults)
├── schedule/       Weekly schedule + date overrides, ScheduleService (override > weekly)
├── availability/   SlotGenerator, BookingWindowService, AvailabilityService (single source of truth)
├── appointment/    Booking (locking), approval/denial state machine, admin queries, CustomerFieldPolicy
├── fulfillment/    AppointmentFulfillmentService interface + ManualFulfillmentService (no-op)
├── notification/   NotificationService (logging) and MagicLinkSender (log / SMTP)
├── admin/          Administrators, magic-link tokens, sessions, bearer-token filter
├── config/         Security, CORS, OpenAPI, properties, Clock
└── common/error/   Exceptions and the RFC 7807 handler
```

### What happens after approval

Approving an appointment publishes an event. After the transaction commits, `AppointmentEventListener` calls `AppointmentFulfillmentService.fulfill(appointment)` and, separately, `NotificationService`. A failure in either is logged and never undoes the approval.

To change the mechanism (email the company, call a webhook or a company API, ...), replace `ManualFulfillmentService` with another implementation of `AppointmentFulfillmentService`. Booking, availability, approval and the frontend stay unchanged.

### Extension points already in place

- **Company settings.** Columns for description, contact details, logo, time zone, duration, capacity and booking window already exist. Only admin endpoints and forms for editing them are missing.
- **Weekly schedule.** The `weekly_schedule_period` table supports several periods per day. Only an admin API is missing.
- **Customer fields.** `CustomerFieldPolicy` decides which fields are required per company, and the `appointment.customer_extra` (JSONB) column is reserved for custom fields.
- **New statuses.** `AppointmentStatus.CAPACITY_CONSUMING` and the transition methods on `Appointment` are the only places to change.
- **Slot interval.** `SlotGenerator` is the only place that would need an interval separate from the duration.

## Known limitations

- There is no admin UI yet for company settings, weekly schedules or special dates (by design for this version). Special dates can be set through the API.
- On daylight-saving transition days, slots that would not last exactly the configured duration are skipped.
- The admin appointments page shows times in the browser's time zone. The public booking page shows company-local times.
- Bookings are serialized per company, which is simple and correct. Very high traffic for a single company could later use finer-grained locks without any API change.
