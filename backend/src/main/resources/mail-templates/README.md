# Email texts

Every email the application sends is one file here. Edit the text freely; keep the
first line as `Subject: ...` followed by a blank line and the message body.

Words in curly braces are filled in automatically. Remove any you don't need.

| File | Sent to | When |
|---|---|---|
| `customer-request-received.txt` | Customer | A request is submitted on the website |
| `customer-approved.txt` | Customer | The request is approved, or an admin enters an appointment |
| `customer-denied.txt` | Customer | The request is denied (also when its company is removed) |
| `customer-cancelled.txt` | Customer | An admin cancels the appointment |
| `customer-rescheduled.txt` | Customer | An admin moves the appointment |
| `admin-new-request.txt` | All administrators | A new request needs a decision |
| `company-new-appointment.txt` | The company's fulfillment email | An appointment is approved |
| `company-appointment-cancelled.txt` | The company's fulfillment email | An appointment the company was told about is cancelled |
| `company-appointment-rescheduled.txt` | The company's fulfillment email | An approved appointment is moved |
| `admin-login-link.txt` | Administrator | Admin login (magic link) |

Placeholders for appointment emails: `{appointmentId}`, `{company}`, `{companyAddress}`,
`{companyPhone}`, `{date}`, `{time}`, `{endTime}`, `{previousDate}`, `{previousTime}`
(moved appointments only), `{customerName}`, `{customerPhone}`, `{customerEmail}`,
`{customerAddress}`, `{customerAnswers}`, `{adminNote}`, `{adminLink}`.

Placeholders for `admin-login-link.txt`: `{link}`, `{minutes}`.

## Changing texts without rebuilding

Set `MAIL_TEMPLATES_DIR` to a folder and put edited copies of any of these files
there (same file names). Files in that folder take precedence; files missing there
fall back to the ones here. Changes apply to the next email, no restart needed.
