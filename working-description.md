Appointment Booking Platform — Backend & MVP Frontend Specification
1. Project Overview
Build a web application that allows users to request appointments with different companies.

The application has two primary areas:

Public booking system
Users do not create accounts.
Users select a company.
Users view that company's appointment availability in a calendar-like interface.
Users select an available appointment slot.
Users provide their contact information.
The system creates a pending appointment request.
The selected appointment capacity is immediately reserved by the pending request.
Administration system
Administrators authenticate using a magic-link login.
All administrators have identical permissions.
Administrators can create and remove companies.
Administrators can view appointment requests.
Administrators can approve or deny appointment requests.
The architecture must support future editing of company settings, working hours, appointment duration, capacity, required customer information, etc.
The initial implementation is intentionally limited. The backend should be designed as a proper foundation for the complete system, but the initial frontend should only expose the functionality necessary to test the basic appointment-booking flow.

2. Technology Stack
Backend
Use:

Java
Spring Boot
Spring Web / REST
Spring Data JPA
Hibernate
PostgreSQL
Flyway for database migrations
Maven or Gradle, using whichever is more appropriate for the project
Bean Validation where appropriate
Spring Security for authentication/authorization
The backend must expose a REST API.

Use a conventional layered architecture:

controller
    ↓
service
    ↓
repository
    ↓
database
Do not put business logic directly inside controllers.

Business rules concerning appointment availability, booking capacity, working hours, etc. must live in services/domain logic so they can be reused by future clients.

Frontend
Use:

React
TypeScript
A modern React build setup
A simple, clean UI
The frontend is only an MVP/testing interface at this stage.

Do not spend significant effort on visual design.

3. Repository Structure
Use a single repository:

/
├── backend/
│   ├── src/
│   ├── pom.xml
│   └── ...
│
├── frontend/
│   ├── src/
│   ├── package.json
│   └── ...
│
└── README.md
The backend and frontend should remain independently runnable.

Document how to start both applications locally.

4. Core Domain Model
The main entities should be designed around the following concepts:

Company
Appointment
Administrator
Company working schedule
Special-date schedule overrides
Customer/booking information
Future fulfillment mechanism
Do not over-engineer the first version, but structure the code so additional functionality can be added without rewriting the core appointment logic.

5. Company
A company represents an organization for which users can request appointments.

At minimum, the database should contain a Company entity with an auto-generated database ID.

The company should be designed to eventually support:

Name
Description
Logo/image
Address
Phone
Email
Website
Time zone
Appointment duration
Maximum concurrent appointments
Booking window
Working days
Working hours
Special-date overrides
Required customer information
Active/inactive status
For the first version, all of these fields may be nullable except the minimum information necessary to identify a company.

The only company information exposed through the initial frontend needs to be:

Company name
The administrator should therefore initially be able to create a company by entering only its name.

Do not require the administrator frontend to configure the advanced settings yet.

6. Default Company Settings
Although the advanced settings are not exposed in the first frontend, the backend should have sensible defaults where the business logic requires them.

Use these defaults:

appointment duration: 1 hour
maximum concurrent appointments: 1
booking window: 1 year
However, the database fields themselves may remain nullable if necessary.

The application/service layer should resolve an unset value to the appropriate default rather than making the database schema unnecessarily restrictive.

The design must allow these values to be configured per company in the future.

7. Appointment Duration
Appointment duration is configurable per company.

Default:

60 minutes
For example, if the duration is one hour and the company works from 09:00 to 17:00, generated slots are:

09:00–10:00
10:00–11:00
11:00–12:00
12:00–13:00
13:00–14:00
14:00–15:00
15:00–16:00
16:00–17:00
For the initial system:

slot interval = appointment duration.

Do not implement independent slot intervals yet.

The architecture should nevertheless not make it impossible to introduce them later.

8. Maximum Concurrent Appointments
Each company can eventually define how many appointments can exist at the same time.

Example:

maximumConcurrentAppointments = 3
At 10:00:

Appointment 1 → APPROVED
Appointment 2 → PENDING
Appointment 3 → APPROVED
The slot is full and must be displayed as:

UNAVAILABLE
If there are only two appointments:

Appointment 1 → APPROVED
Appointment 2 → PENDING
the slot remains:

AVAILABLE
because one capacity slot remains.

Both PENDING and APPROVED appointments consume capacity.

DENIED appointments do not consume capacity.

9. Appointment Status
Keep the status model simple.

Use:

PENDING
APPROVED
DENIED
Do not introduce additional statuses such as CANCELLED, COMPLETED, NO_SHOW, etc. in the first version.

The design should allow those states to be introduced later.

Status behavior
PENDING
Created when a user submits an appointment request.

A pending appointment immediately consumes one capacity slot.

APPROVED
Created when an administrator approves a pending appointment.

It continues consuming capacity.

DENIED
Created when an administrator denies a pending appointment.

It no longer consumes capacity.

10. Booking Concurrency / Race Conditions
This is a critical requirement.

When two users attempt to book the final available capacity of the same time slot simultaneously, the backend must ensure that the company's maximum concurrent appointment limit is never exceeded.

For example:

capacity = 1

User A submits 14:00
User B submits 14:00
Only one request may successfully create a PENDING appointment.

The other request must receive an appropriate conflict response indicating that the slot is no longer available.

Do not rely solely on:

SELECT COUNT(...)
if count < capacity:
    INSERT
without appropriate transaction/concurrency protection.

The booking operation must be atomic.

Use an appropriate PostgreSQL/JPA transaction and locking strategy.

The implementation should be carefully designed and tested for concurrent requests.

11. Appointment Date/Time Representation
Appointments must use a start and end datetime rather than storing only an hour.

Conceptually:

startDateTime
endDateTime
Example:

2026-11-12 14:00
2026-11-12 15:00
This is important because appointment durations are configurable.

Do not model appointments as merely:

date = 2026-11-12
hour = 14
The system must be capable of handling different durations later.

12. Time Zones
Companies can eventually operate in different time zones.

Therefore, do not build the application around a single hardcoded global timezone.

The company should have a timezone field.

The backend should use a timezone-aware representation where appropriate and consistently convert between:

stored appointment timestamps
company-local calendar time
API representations
frontend display
For the initial version, if the company's timezone is NULL, use a clearly documented default timezone rather than allowing inconsistent behavior.

Do not hardcode the timezone in multiple parts of the application.

13. Working Days and Hours
The backend must be designed to support configurable working schedules.

Conceptually:

Monday:
    09:00–17:00

Tuesday:
    09:00–17:00

Wednesday:
    CLOSED

Thursday:
    09:00–17:00

Friday:
    09:00–17:00

Saturday:
    CLOSED

Sunday:
    CLOSED
The exact persistence model can be chosen by the implementation, but it must be possible to represent:

Working day
Non-working day
Start time
End time
Potentially multiple working periods per day in the future
Do not assume every company has the same schedule.

14. Special-Date Overrides
The system must support exceptions to the normal weekly schedule.

For example:

January 7:
    CLOSED

January 8:
    CLOSED
These dates override the normal weekly schedule.

The implementation should support future cases such as:

January 7:
    CLOSED

January 8:
    09:00–13:00
The important rule is:

A specific-date override takes precedence over the normal weekly schedule.

This should be implemented as a reusable scheduling component/service rather than duplicated throughout appointment logic.

15. Booking Window
Users can book up to one year into the future.

The booking window is:

1 year
Important clarification:

The range must include the entire calendar month containing the final day of the one-year range.

For example, if the calculated one-year endpoint falls somewhere in:

October 2027
then the user should be able to view/book through:

October 31, 2027
rather than stopping at the exact anniversary date.

The booking window must not allow bookings beyond the end of that final month.

Past dates cannot be booked.

16. Availability
The backend must expose availability information for a company and date/date range.

For each generated appointment slot, the backend should determine one of these public states:

AVAILABLE
UNAVAILABLE
PENDING
However, availability must follow the capacity rules.

AVAILABLE
There is at least one unused capacity slot.

Example with capacity 3:

1 APPROVED
1 PENDING
1 available capacity
Public result:

AVAILABLE
UNAVAILABLE
All capacity is consumed by APPROVED or PENDING appointments.

Example:

2 APPROVED
1 PENDING
capacity = 3
Public result:

UNAVAILABLE
PENDING
This state should communicate that there are pending requests associated with the time slot while capacity remains.

If there are pending requests but capacity remains, the API may return a state indicating pending requests.

The frontend should clearly communicate that there are already requests pending, while still allowing the user to book if capacity remains.

Do not expose private information about the other customers.

For example, never expose:

John Smith has requested this slot.
The public API should only communicate aggregate availability/state.

17. Non-Working Hours
The availability API must distinguish working time from non-working time.

Non-working periods should not be treated as normal unavailable appointments.

The frontend can display them as:

NON_WORKING
or omit them from selectable appointment slots.

The backend must nevertheless understand the distinction.

A user must never be able to create an appointment outside a company's working schedule.

18. Past Slots
Any appointment slot whose start time is in the past must not be bookable.

The backend must enforce this independently of the frontend.

Do not rely on the frontend hiding past times.

19. Public Booking Flow
The intended public flow is:

1. User opens website.
2. User sees available companies.
3. User selects a company.
4. User selects a date.
5. Backend returns generated availability.
6. User sees appointment slots.
7. User selects an available slot.
8. User enters required customer information.
9. User submits the booking.
10. Backend validates the slot again.
11. Backend atomically reserves capacity.
12. Appointment is created as PENDING.
13. User receives a success response.
The frontend must not assume that a slot is still available merely because it was available when the calendar was loaded.

The backend must always revalidate availability during booking.

20. Customer Information
Users do not create accounts.

When submitting an appointment request, they provide their personal/contact information.

The initial fields are:

Name
Phone number
Email
However, the system must be designed so that each company can eventually configure which customer fields are required.

For example:

Company A:
    name
    phone
    email

Company B:
    name
    phone
    email
    address

Company C:
    name
    email
    custom question
Do not tightly couple the entire booking architecture to exactly three fields.

The first implementation may use dedicated fields for name/phone/email if that substantially simplifies the MVP, but the domain/service design should leave a clear path toward configurable fields.

21. No User Accounts
There is no public user registration or login.

A customer can make multiple appointment requests without having an account.

Do not build:

customer profiles
customer passwords
customer dashboards
customer login
customer account management
into the MVP.

22. Appointment Approval
Every user booking initially becomes:

PENDING
It is not automatically approved.

An administrator must manually approve or deny the request.

The approval operation should:

Verify that the appointment is still PENDING.
Verify that the appointment is still valid.
Verify that capacity is still available.
Change the appointment to APPROVED.
Trigger the future fulfillment mechanism.
If approval would violate the capacity limit, the backend must refuse the approval.

This situation should be handled gracefully rather than allowing capacity to be exceeded.

23. Approval/Fulfillment Architecture
This is a major architectural requirement.

What happens after an appointment is approved is intentionally undecided.

The system may eventually:

Send the appointment information to the company by email.
Send it to a company's API.
Call a webhook.
Add it to another system.
Allow an administrator to manually communicate it.
Use a completely different mechanism.
Therefore, do not hardcode the post-approval process into the Appointment service.

Create an abstraction around fulfillment.

Conceptually:

Appointment
     ↓
Admin approves
     ↓
Appointment Approval Service
     ↓
Fulfillment Service
     ↓
implementation
For example:

interface AppointmentFulfillmentService {
    void fulfill(Appointment appointment);
}
The initial implementation can be extremely simple, such as a no-op/manual implementation.

The important thing is that the rest of the booking system must not care how fulfillment works.

Later implementations might be:

ManualFulfillmentService
EmailFulfillmentService
WebhookFulfillmentService
CompanyApiFulfillmentService
The exact interface/class names can differ, but the architectural separation is required.

Changing the fulfillment mechanism later should not require rewriting:

calendar logic
appointment creation
availability logic
company management
booking frontend
appointment state machine
24. Admin Authentication
Administrators use magic-link authentication.

There is no public admin registration.

Recommended flow:

1. Admin enters email.
2. Backend checks whether the email belongs to an administrator.
3. Backend generates a cryptographically secure, short-lived, single-use token.
4. Backend sends a magic-link email.
5. Admin clicks the link.
6. Backend validates the token.
7. Backend creates an authenticated session/token.
8. Admin can access protected admin endpoints.
The exact session/JWT implementation can be selected by the developer.

Security requirements:

Tokens must be cryptographically secure.
Tokens must expire.
Tokens must be single-use.
Tokens should not be stored in plaintext if avoidable.
Admin endpoints must require authentication.
Public booking endpoints must not require authentication.
For local development/testing, provide a practical way to create the initial administrator, such as an environment variable or database seed.

Do not build an admin registration interface.

25. Administrators
All administrators have identical permissions.

There are no roles such as:

SUPER_ADMIN
COMPANY_ADMIN
EDITOR
VIEWER
for the first version.

Any authenticated administrator can perform all administrator operations.

26. Company Management
The initial admin functionality must support:

Create company
Admin enters:

Company name
and creates a company.

All other settings can remain NULL/default.

List companies
Admin can see existing companies.

Remove company
Admin can remove a company.

Prefer a soft-delete/inactive approach if practical so that historical appointment information is not unnecessarily destroyed.

A removed company should no longer appear to public users.

Historical appointments should remain internally consistent.

27. Admin Appointment Management
The backend must support administrators viewing appointment requests.

At minimum, administrators should be able to:

List appointments
Filter by company
Filter by status
Approve pending appointments
Deny pending appointments
View customer information
View appointment date/time
The initial frontend does not need to expose every advanced admin feature.

The backend should nevertheless be structured so they can be added easily.

28. Admin Manual Appointment Creation
The complete architecture should eventually support administrators creating appointments manually.

However, this is not necessary for the initial frontend.

If implementing the backend API now, it is acceptable to include it if it can be done cleanly without unnecessary complexity.

Do not let this feature complicate the core booking system.

29. Admin Calendar
The complete system should eventually provide administrators with a calendar showing:

company
date
time
appointment
status
customer
The initial frontend does not need to implement the full admin calendar.

30. REST API
Design a clean REST API.

The exact endpoint naming can be chosen by the implementation, but the API should conceptually contain:

Public
GET /api/companies
GET /api/companies/{companyId}
GET /api/companies/{companyId}/availability
POST /api/companies/{companyId}/appointments
The availability endpoint should support requesting a date or date range.

The appointment creation endpoint should accept:

company
start time
customer information
and derive/validate the end time using the company's appointment duration.

Do not trust a client-provided end time if it could contradict company configuration.

31. Admin API
Conceptually:

POST   /api/admin/auth/request-link
GET    /api/admin/auth/verify
POST   /api/admin/companies
GET    /api/admin/companies
DELETE /api/admin/companies/{id}

GET    /api/admin/appointments
GET    /api/admin/appointments/{id}
POST   /api/admin/appointments/{id}/approve
POST   /api/admin/appointments/{id}/deny
Exact endpoint naming may differ.

Keep public and admin endpoints clearly separated.

32. Validation
Validate all requests on the backend.

Examples:

Company
name must not be blank
Appointment
company must exist
company must be active
start time must be valid
start time must not be in the past
slot must fall within working hours
slot must respect special-date overrides
slot must align with appointment duration
slot must fall within booking window
capacity must be available
customer information must satisfy company requirements
Do not trust frontend validation as a security/business-rule mechanism.

33. Database
Use PostgreSQL.

Use JPA/Hibernate for persistence.

Use Flyway for schema migrations.

Do not use:

spring.jpa.hibernate.ddl-auto=create
as the primary production schema-management mechanism.

Use migrations for database structure.

The initial migrations should create all required tables, indexes, constraints, and relationships.

34. Database IDs
Database IDs should be automatically generated.

Do not require the client to supply IDs for:

companies
appointments
administrators
schedule records
other persistent entities
The exact ID type can be selected by the developer, but use a conventional generated identifier.

35. Suggested Entity Structure
A reasonable starting point is:

Company
- id
- name
- description
- logo
- address
- phone
- email
- website
- timezone
- appointmentDuration
- maxConcurrentAppointments
- bookingWindow
- active
- createdAt
- updatedAt
Appointment
- id
- company
- startDateTime
- endDateTime
- status
- customerName
- customerPhone
- customerEmail
- createdAt
- updatedAt
Administrator
- id
- email
- active
- createdAt
Working schedules and special dates should be represented by their own appropriate entities rather than storing a large amount of scheduling logic directly inside Company.

The exact entity decomposition is up to the implementation as long as the required behavior is preserved.

36. Indexing
Add appropriate database indexes.

At minimum, appointment queries will frequently filter by:

company
startDateTime
status
Consider indexes that efficiently support:

company + startDateTime
company + startDateTime + status
Do not blindly add indexes to every column.

37. Availability Algorithm
The availability service should roughly perform the following process:

Input:
    company
    date/range

1. Load company settings.
2. Determine company's timezone.
3. Determine applicable booking window.
4. Determine normal weekly schedule.
5. Apply special-date overrides.
6. Generate appointment slots based on appointment duration.
7. Remove slots outside the booking window.
8. Remove past slots.
9. Query appointments for the relevant time range.
10. Count PENDING + APPROVED appointments for each slot.
11. Compare count against maximum capacity.
12. Return public availability state.
This logic should be centralized.

Do not implement one version in the frontend and another in the backend.

The backend is authoritative.

38. Slot Alignment
If the working period is:

09:00–17:00
and duration is:

60 minutes
generate:

09:00–10:00
10:00–11:00
...
16:00–17:00
Do not generate:

09:30–10:30
unless the configured slot interval later explicitly allows this.

An appointment must fit entirely inside the working period.

For example:

16:30–17:30
must not be generated when working hours end at 17:00.

39. Booking Transaction
The appointment creation service should execute as a database transaction.

Conceptually:

BEGIN TRANSACTION

validate company

validate requested slot

determine appointment end time

validate working schedule

validate booking window

lock/check relevant capacity

count existing PENDING + APPROVED appointments

if capacity is full:
    reject

create PENDING appointment

COMMIT
The implementation must account for concurrent requests.

This is one of the most important correctness requirements in the project.

40. Public Privacy
The public availability endpoint must never reveal customer information.

For example, the public response may contain:

{
  "start": "...",
  "end": "...",
  "status": "AVAILABLE",
  "pendingRequests": true
}
but never:

{
  "customerName": "John Smith",
  "customerEmail": "..."
}
Customer information is administrator-only.

41. Initial Frontend
The first frontend is intentionally very simple.

Do not build the complete production website.

The frontend exists primarily to test that the backend booking functionality works.

It should contain:

Public page
A page where the user can:

See available companies.
Select a company.
Select/view a date.
See available appointment slots.
Select a slot.
Enter:
name
phone
email
Submit the appointment.
See a confirmation that the request was successfully submitted.
The interface should clearly distinguish:

Available
Unavailable
Pending
Non-working
The exact visual styling is not important.

42. Admin Frontend
The initial admin frontend only needs enough functionality to test company management.

It should include:

Admin login
A simple magic-link login flow.

Company management
A form:

Company name: [____________]

[Create company]
and a list of companies with the ability to remove them.

Do not build advanced company settings into the initial frontend.

Do not expose forms for:

working hours
working days
timezone
appointment duration
booking window
capacity
special dates
required customer fields
These should exist architecturally in the backend but can remain NULL/default for this testing version.

43. Testing Defaults
Because the initial frontend does not expose advanced company settings, the application needs a practical way to test appointments.

For a newly created company, use the following defaults unless explicitly configured:

Appointment duration:
60 minutes

Maximum concurrent appointments:
1

Booking window:
1 year

Working schedule:
Monday–Friday
09:00–17:00

Saturday:
closed

Sunday:
closed
Include special-date support in the backend.

The example special-date behavior should be demonstrable for:

January 7 → closed
January 8 → closed
Do not permanently hardcode those dates into the business logic. They should be represented through the special-date scheduling mechanism.

44. Error Handling
The API should return appropriate HTTP status codes.

Examples:

400 Bad Request
for invalid input.

401 Unauthorized
for unauthenticated admin requests.

403 Forbidden
when authentication exists but authorization fails.

404 Not Found
for missing resources.

409 Conflict
when a requested appointment slot has become unavailable due to another booking.

500 Internal Server Error
only for unexpected server failures.

Return structured JSON error responses.

Do not expose stack traces or internal database errors to clients.

45. Frontend/Backend Separation
The frontend must communicate with the backend through the REST API.

Do not duplicate booking business logic in React.

The frontend can:

render availability
validate obvious form input
display errors
manage UI state
The backend must remain responsible for:

schedule calculation
booking window
capacity
appointment duration
appointment conflicts
appointment creation
approval
authorization
all business rules
46. Configuration
Application configuration should use environment variables/configuration files rather than hardcoded secrets.

Examples:

DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD

MAIL_HOST
MAIL_USERNAME
MAIL_PASSWORD

FRONTEND_URL
Magic-link email configuration should be configurable.

For local development, provide a documented development mode or mail configuration that makes it possible to test authentication without requiring a production email service.

Do not commit secrets.

47. Email / Notifications
Notifications are intentionally not finalized.

The architecture should not depend heavily on email.

The following may eventually exist:

Booking request received
Booking approved
Booking denied
and admin notifications.

However, notification behavior should be isolated behind a service/interface so it can be replaced later.

Do not make the booking transaction dependent on successfully sending an email unless there is a deliberate future requirement to do so.

48. Fulfillment vs Notifications
Keep these concepts separate.

Notification means communicating with a user/admin.

Fulfillment means delivering an approved appointment to the company or downstream system.

For example:

Appointment approved
       |
       +----> notify customer
       |
       +----> fulfill appointment for company
These should not be one giant service.

This separation is important because both mechanisms are subject to future change.

49. API Documentation
Provide clear API documentation.

Preferably use OpenAPI/Swagger.

Document:

public endpoints
admin endpoints
request bodies
response bodies
error responses
authentication requirements
This will make future frontend development significantly easier.

50. Automated Tests
The backend must contain automated tests for the most important business logic.

At minimum, test:

Company
Create company.
Retrieve company.
Remove/deactivate company.
Availability
Working day produces slots.
Non-working day produces no bookable slots.
Special-date override replaces weekly schedule.
Past slots are unavailable.
Booking window is enforced.
Appointment duration is respected.
Capacity
For capacity = 1:

one pending booking → unavailable
For capacity = 3:

two bookings → available
three bookings → unavailable
Denied bookings do not consume capacity.

Booking
Valid booking creates PENDING appointment.
Booking outside working hours fails.
Booking in the past fails.
Booking beyond booking window fails.
Booking an unavailable slot fails.
Booking against an inactive company fails.
Approval
PENDING → APPROVED works.
PENDING → DENIED works.
APPROVED cannot be approved again.
DENIED cannot be approved.
Approval cannot exceed capacity.
Concurrency
Include a test or appropriate integration-level validation that concurrent booking attempts cannot exceed the company's capacity.

This is especially important.

51. Code Quality
Use clear names and conventional Spring Boot practices.

Prefer:

CompanyController
CompanyService
CompanyRepository
and:

AppointmentController
AppointmentService
AppointmentRepository
AvailabilityService
Avoid putting everything into a single service.

Business logic should be decomposed into logical services.

Use DTOs for API requests/responses rather than exposing JPA entities directly from controllers.

52. Do Not Over-Engineer
This is an MVP.

Do not implement unnecessarily complex features such as:

microservices
event-driven architecture
Kafka
Redis
Kubernetes
distributed locks
complex role hierarchies
customer accounts
advanced analytics
payment processing
SMS
multi-language support
production-grade audit systems
unless they are genuinely required for the core functionality.

A modular monolith is the appropriate architecture.

The backend should be a single Spring Boot application.

53. Future-Proofing Requirements
Even though many features are not implemented in the first frontend, the backend design must leave room for:

Configurable company settings
Configurable working hours
Special dates
Different appointment durations
Multiple simultaneous appointments
Configurable customer fields
Email notifications
SMS notifications
Company-specific integrations
Webhooks
Manual fulfillment
Automatic fulfillment
Appointment cancellation
Appointment rescheduling
Additional appointment statuses
Rich administrator calendar
Multiple frontend clients
Do not implement all of these now.

The goal is to make the core architecture extensible without making the MVP unnecessarily complicated.

54. Definition of Done
The first version is considered complete when all of the following work:

Backend
Spring Boot application starts successfully.
PostgreSQL connection works.
Flyway migrations execute successfully.
Company entity exists.
Admin entity/authentication exists.
Companies can be created.
Companies can be removed/deactivated.
Public users can retrieve companies.
Public users can retrieve availability.
Appointment slots are generated according to the schedule.
Appointment duration defaults to one hour.
Capacity defaults to one appointment.
Booking window is one year plus the remainder of the final month.
Past appointments cannot be booked.
January 7 and January 8 can be represented as special non-working dates.
Users can submit appointment requests.
Requests become PENDING.
PENDING appointments consume capacity.
APPROVED appointments consume capacity.
DENIED appointments release capacity.
Concurrent requests cannot exceed capacity.
Administrators can approve requests.
Administrators can deny requests.
Fulfillment is isolated behind an abstraction.
Customer information is not exposed publicly.
API errors are handled correctly.
Automated tests cover core booking behavior.
Frontend
Public page displays companies.
User can select a company.
User can view calendar/date availability.
User can select an available slot.
User can enter name, phone and email.
User can submit an appointment.
User sees a successful booking confirmation.
User cannot book an unavailable slot.
Admin can log in using magic link.
Admin can create a company.
Admin can view companies.
Admin can remove a company.
The frontend does not need advanced company configuration yet.

55. Important Architectural Principle
The most important design principle of this project is:

The appointment-booking system must not depend on what happens after approval.

The core flow should be:

                    ┌──────────────────────┐
                    │      Public User     │
                    └──────────┬───────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │   Availability       │
                    │      Service         │
                    └──────────┬───────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │ Create Appointment    │
                    │       PENDING         │
                    └──────────┬───────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │       Admin          │
                    │ Approve / Deny        │
                    └──────────┬───────────┘
                               │
                         APPROVED
                               │
                               ▼
                    ┌──────────────────────┐
                    │  Fulfillment         │
                    │     Interface        │
                    └──────────┬───────────┘
                               │
             ┌─────────────────┼──────────────────┐
             ▼                 ▼                  ▼
        Manual later       Email later       Company API
The final part can be replaced without changing the rest of the system.

56. Implementation Approach
Implement the project incrementally.

Recommended order:

Initialize Spring Boot backend.
Configure PostgreSQL.
Configure Flyway.
Create core database schema.
Implement Company domain and API.
Implement scheduling model.
Implement availability calculation.
Implement Appointment domain.
Implement transactional booking/capacity protection.
Implement admin authentication.
Implement admin appointment approval/denial.
Implement fulfillment abstraction.
Add automated backend tests.
Initialize React frontend.
Implement public company selection.
Implement availability/calendar UI.
Implement booking form.
Implement basic admin company management.
Connect frontend to backend.
Test the complete end-to-end flow.
At every stage, keep the application runnable.

Do not build the entire frontend before testing the backend booking logic.

57. Expected End-to-End Test
The following scenario should work from beginning to end:

Setup
Administrator logs in.

Administrator creates:

Company: Test Company
The company receives defaults:

Monday-Friday
09:00-17:00
1 hour appointments
1 concurrent appointment
1-year booking window
User
User opens the public site.

They select:

Test Company
They select a future working day.

They see:

09:00 Available
10:00 Available
11:00 Available
...
They select:

10:00
They enter:

Name
Phone
Email
They submit.

The backend creates:

Appointment
status = PENDING
start = 10:00
end = 11:00
The 10:00 slot now becomes unavailable because capacity is 1.

Administrator
Admin views the pending appointment.

Admin approves it.

The appointment becomes:

APPROVED
The fulfillment service is invoked through the abstraction.

The actual fulfillment implementation can currently do nothing.

Second user
A second user attempts to book 10:00.

The backend rejects the booking because capacity is already consumed.

The second user can still book another available time.

Denial test
If the admin instead denies the first request:

PENDING → DENIED
the 10:00 capacity becomes available again.

A new user can then request 10:00.

58. Final Implementation Constraint
When making implementation decisions not explicitly specified in this document, prefer:

Simplicity
Correctness
Clear separation of responsibilities
Extensibility
Conventional Spring Boot/JPA practices
Do not introduce unnecessary infrastructure.

The application should be a clean modular monolith with a strong appointment-domain core.

The first version is primarily a functional backend + minimal testing frontend, not a polished production website. :::