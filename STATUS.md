# Project status

This file hands the project over between work sessions. It records what has been built, what remains, and the decisions already made. A new session should read this file, then `README.md` (setup, API, architecture) and `working-description.md` (the full specification).

- **Branch:** `claude/gifted-cannon-cza92g`
- **Last updated:** 2026-11-02

---

## Decisions already made with the project owner

| Topic | Decision |
|---|---|
| Default time zone | `Europe/Belgrade`, used when a company has none set |
| January 7 and 8 | Only an example for testing. Not hardcoded; set them as special dates in the admin UI. |
| Approving a request whose time has passed | Refused with 409 |
| Pending requests of a removed company | Set to `DENIED` automatically. The records are kept. |
| Docker | Not used. Tests run against a local PostgreSQL database (`booking_test`). |
| Maven | No local install needed; use the wrapper (`./mvnw` or `mvnw.cmd`). |
| Owner's environment | Windows with PowerShell. The README includes PowerShell commands. |
| Fulfillment | By email. Administrators set each company's fulfillment email; customers never see it. |
| Language | Serbian. The owner left the details to Claude; see the next table. |
| Frontend | Minimal styling; the main functions matter, not the look. |
| Production deployment | To be determined by the owner. |

### Decisions Claude made (the owner asked it to decide)

| Topic | Decision | Where to change it |
|---|---|---|
| Script | Serbian in Latin script, for the UI and emails | `frontend/src/i18n/sr.ts`, `backend/src/main/resources/mail-templates/` |
| Who can cancel | Administrators only. Customers have no accounts, so there is no self-cancel link. | — |
| New statuses | `CANCELLED`, `COMPLETED`, `NO_SHOW`. Completed and no-show only once the appointment has started. | `Appointment`, `AppointmentStatus` |
| Company without a fulfillment email | Approval still works; the appointment is marked "not delivered" so an admin hands it over. | `EmailFulfillmentService` |
| Who gets emails | Customer: received, approved, denied, cancelled, moved. All admins: each new request. Company: approved, and cancelled/moved if it had been told. | `EmailNotificationService`, `EmailFulfillmentService` |
| Admin-entered appointments | Same slot and capacity rules as public bookings, only the name required, approved immediately. | `AppointmentBookingService.bookByAdmin` |
| Moving an appointment | Allowed for pending and approved ones, to a free bookable slot of the same company; the status stays. | `AppointmentManagementService.reschedule` |
| Rate limits | Per IP: 5 login links and 20 link redemptions per 15 min, 20 bookings per hour. | `application.yml` |
| Administrator management | Added an Administratori page (not in the spec) so new admins don't need a restart. | `AdministratorService` |
| Weekly schedule | A saved schedule fully defines the week, even with no periods at all; "Vrati na podrazumevano" restores Mon–Fri 09:00–17:00. | `WeeklyScheduleService` |

---

## What is implemented

### Backend (Spring Boot 3.5, Java 21, PostgreSQL, Flyway migrations V1–V3)

- [x] **Companies:** create, list, soft delete, and edit every setting (description, address, phone, public email, website, logo URL, fulfillment email, time zone, duration, slot interval, capacity, booking window).
- [x] **Weekly schedule** API with several periods per day; default Mon–Fri 09:00–17:00 until one is saved.
- [x] **Special dates:** closed, or different hours; they beat the weekly schedule.
- [x] **Availability:** slot states, booking window, a slot interval separate from the duration (overlapping slots share capacity), daylight-saving handling, no customer data in public responses.
- [x] **Booking:** atomic, with the company row locked, so capacity can never be exceeded.
- [x] **Customer form per company:** phone, email and address required/optional/hidden, plus custom questions. Answers are stored with the question text.
- [x] **Appointment management:** approve, deny, cancel, reschedule, completed, no-show, internal notes, admin-entered appointments, list filters by company/status/time range.
- [x] **Fulfillment by email** to the company, with delivery time recorded (`fulfilledAt`). `FULFILLMENT_MODE=manual` turns it off.
- [x] **Notifications in Serbian** to customers and administrators, from editable template files (`MAIL_TEMPLATES_DIR` overrides them without rebuilding).
- [x] **Admin authentication:** magic link (hashed, single-use, 15 min), 12-hour bearer sessions, administrator management.
- [x] **Hardening:** per-IP rate limits (429), nightly cleanup of expired login tokens and sessions, `API_DOCS_ENABLED=false` hides Swagger.
- [x] RFC 7807 errors with a `code`, OpenAPI/Swagger UI.
- [x] **103 automated tests** against real PostgreSQL, including the concurrency test.

### Frontend (React 18, TypeScript, Vite; minimal styling, Serbian)

- [x] Public: company list, day view with slots, booking form that follows the company's customer form, confirmation; a slot taken meanwhile reloads the slots.
- [x] Admin: login; companies with settings, weekly hours, special dates and customer form; appointments with all actions and delivery status; new appointment; weekly calendar; administrators.
- [x] Times shown in the company's time zone everywhere.
- [x] All texts in `frontend/src/i18n/sr.ts`.
- [x] 8 Vitest tests (formatting, texts, booking page).

### Other

- [x] End-to-end browser check in `e2e/` covering the main flows (run manually; see `e2e/README.md`).
- [x] GitHub Actions workflow running backend and frontend tests.
- [x] `working-description.md` headings restored; `plan.md` marked as superseded.

---

## What remains

1. **Production deployment** (owner to decide): hosting, domain and HTTPS, a real SMTP account (`MAIL_MODE=smtp` and `MAIL_*`), `FRONTEND_URL`, `API_DOCS_ENABLED=false`, `SERVER_FORWARD_HEADERS_STRATEGY=native` behind a proxy, database backups. The frontend is built with `npm run build` and served as static files, with `/api` routed to the backend.
2. **Visual design**, if wanted later. The UI is deliberately plain.
3. **Possible later improvements**, none requested:
   - a link in customer emails to cancel without an account;
   - rate limits shared between several backend instances (they are in memory now);
   - running the e2e check in CI (needs both servers started in the workflow).

---

## Starting a new session

Tell Claude something like:

> Read `STATUS.md`, `README.md` and `working-description.md` in the repo (branch `claude/gifted-cannon-cza92g`). Then …

Update this file as work gets done.
