# Project status

This file hands the project over between work sessions. It records what has been built, what remains, and the decisions already made. A new session should read this file, then `README.md` (setup, API, architecture) and `working-description.md` (the full specification), and then pick tasks from "What remains" below.

- **Branch:** `claude/gifted-cannon-cza92g`. No pull request has been opened yet.
- **Last updated:** 2026-10-19

---

## Decisions already made with the project owner

| Topic | Decision |
|---|---|
| Default time zone | `Europe/Belgrade`, used when a company has none set |
| January 7 and 8 | Only an example for testing. They are not hardcoded or seeded; set them as date overrides through the API. |
| Admin appointments page | Included (list, approve, deny) |
| Approving a request whose time has passed | Refused with 409 |
| Pending requests of a removed company | Set to `DENIED` automatically. The records are kept, not deleted. |
| Docker | Not used. Tests run against a local PostgreSQL database (`booking_test`). |
| Maven | No local install needed; use the wrapper (`./mvnw` or `mvnw.cmd`). |
| Owner's environment | Windows with PowerShell. The README includes PowerShell commands. |

---

## What is implemented (first version / MVP)

### Backend (Spring Boot 3.5, Java 21, PostgreSQL, Flyway)

**Companies**
- [x] Admins create a company with only a name. List and get endpoints exist for both public and admin users.
- [x] Removal is a soft delete. A removed company is hidden from the public, its history is kept, and its pending requests are set to `DENIED`.
- [x] Database columns exist for description, logo, address, phone, email, website, time zone, duration, capacity and booking window. Defaults are filled in by `CompanySettingsResolver`.

**Schedules**
- [x] Weekly schedule table, with several periods per day allowed. A company with no rows uses the default Mon–Fri 09:00–17:00.
- [x] Date overrides: a date can be closed or given its own hours, and an override beats the weekly schedule. Admin API: `PUT`, `DELETE` and `GET /api/admin/companies/{id}/schedule/overrides`.

**Availability**
- [x] Slots are generated per day, with the slot interval equal to the appointment duration and slots fitting fully inside working periods.
- [x] Each slot has a state: `AVAILABLE`, `PENDING`, `UNAVAILABLE` or `NON_WORKING`. Unavailable slots carry a reason: `FULLY_BOOKED`, `PAST` or `OUTSIDE_BOOKING_WINDOW`.
- [x] The booking window runs 12 months, extended to the end of that calendar month.
- [x] Each request can cover one date or a range of up to 62 days.
- [x] No customer data appears in public responses.
- [x] On daylight-saving change days, slots that would not last the full duration are skipped.

**Booking**
- [x] One atomic transaction locks the company row (`SELECT … FOR UPDATE`), then checks capacity and inserts the booking. Capacity can never be exceeded.
- [x] The backend validates the company is active, the slot lines up with the schedule, the time isn't past, it's inside the booking window, and the required customer fields are present. The end time is derived from the duration.
- [x] Requests are created as `PENDING`, and both `PENDING` and `APPROVED` count against capacity.

**Approval**
- [x] Admins approve or deny `PENDING` requests. Approval re-checks capacity and refuses past appointments.
- [x] After approval, `AppointmentFulfillmentService` is called once the transaction commits. The current implementation is `ManualFulfillmentService`, which only logs.
- [x] `NotificationService` is separate from fulfillment. The current implementation only logs.

**Admin authentication**
- [x] Magic-link login. Tokens are random, stored only as hashes, valid for 15 minutes and single-use. Sessions are bearer tokens lasting 12 hours.
- [x] `ADMIN_BOOTSTRAP_EMAILS` creates the first admins on startup.
- [x] In development (`MAIL_MODE=log`), magic links are printed in the log. With `MAIL_MODE=smtp` they are sent by email.

**General**
- [x] Admins can list appointments filtered by company, status and date range, with paging, and view one appointment in detail.
- [x] Errors use RFC 7807 JSON with a `code` field. No stack traces are exposed.
- [x] OpenAPI/Swagger UI at `/swagger-ui.html`.
- [x] Allowed browser origins are `FRONTEND_URL` plus `CORS_ALLOWED_ORIGINS` (default `http://127.0.0.1:5173`).
- [x] 62 automated tests: unit tests plus integration tests against real PostgreSQL. They include a concurrency test, which was shown to fail when the lock is removed.

### Frontend (React 18, TypeScript, Vite; minimal testing UI)
- [x] Public pages: company list, then a day view with previous/next day and a date picker, then slots colour-coded by state, then a booking form, then a confirmation.
- [x] When a slot is taken in the meantime (409), the page shows a message and reloads the slots. Responses for an earlier date are ignored if a newer one was requested.
- [x] Admin pages: magic-link login and verify, company create/list/remove, and appointment list with filters and approve/deny.

---

## What remains

### A. Admin features the specification defers to later versions

The specification asks for these to be possible but not yet built.

1. **Edit company settings.** Needs a backend `PUT`/`PATCH /api/admin/companies/{id}` and an admin form for name, description, contact details, logo, time zone, duration, capacity and booking window.
2. **Weekly working schedule.** Needs a backend API and a UI to set working days and hours, including several periods per day. The table already exists.
3. **Special-date overrides UI.** The API exists; only the admin form is missing.
4. **Configurable required customer fields per company,** such as address or custom questions. `CustomerFieldPolicy` currently hardcodes name, phone and email, and the `appointment.customer_extra` JSONB column is reserved for extra fields. Needs storage for each company's field configuration, validation, and dynamic fields in the booking form.
5. **Manual appointment creation by an admin** (spec §28).
6. **Admin calendar view** showing company, date, time, appointment, status and customer (spec §29).

### B. Behaviour and integrations
7. **Real fulfillment implementations,** such as email to the company, a webhook or a company API. Add a new `AppointmentFulfillmentService` bean and pick one through configuration.
8. **Real notifications to customers and admins,** for request received, approved and denied. Add a new `NotificationService` implementation.
9. **Appointment cancellation and rescheduling, and extra statuses** (`CANCELLED`, `COMPLETED`, `NO_SHOW`, …). Changes go in `AppointmentStatus.CAPACITY_CONSUMING` and the transition methods on `Appointment`.
10. **A slot interval separate from the appointment duration** (for example 30-minute steps for 60-minute appointments). Only `SlotGenerator` needs to change.
11. **Admin appointments page time zone.** It shows times in the browser's time zone; it should use the company's.

### C. Hardening and production readiness
12. **Rate limiting** on `/api/admin/auth/request-link`, and possibly on public booking.
13. **Production deployment setup.** Production configuration, HTTPS, real SMTP, turning off Swagger in production, and a cleanup job for expired login tokens and sessions.
14. **Frontend tests.** There are none. The end-to-end browser test from development (Playwright, the §57 scenario) was not committed and could be added to the repo.
15. **Continuous integration** that runs `./mvnw test` and `npm run build` automatically, for example a GitHub Actions workflow with a PostgreSQL service.
16. **Visual design.** The UI is deliberately plain.

### D. Housekeeping
17. **Pull request.** Open one from `claude/gifted-cannon-cza92g` when ready.
18. **Old files.** `plan.md` is an early summary that the specification now replaces. `working-description.md` has lost its Markdown formatting and ends with a stray `:::`. Both could be tidied up.

---

## Starting a new session

Tell Claude something like:

> Read `STATUS.md`, `README.md` and `working-description.md` in the repo (branch `claude/gifted-cannon-cza92g`). Then implement item(s) N from "What remains".

Update this file as items get done.
