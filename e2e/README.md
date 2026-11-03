# End-to-end browser check

`booking-flow.mjs` drives the real application in Chromium: admin login with the
magic link, company settings, weekly hours, a special date, the customer form, a
public booking, approval with delivery to the company, rescheduling, an
admin-entered appointment, the calendar and cancellation. It also checks that the
expected emails appear in the backend log.

It needs an empty database, because it creates its own company.

```bash
# 1. Backend with log mail mode, writing its log to a file
cd backend
ADMIN_BOOTSTRAP_EMAILS=admin@example.com ./mvnw spring-boot:run > ../e2e/backend.log 2>&1 &

# 2. Frontend
cd ../frontend && npm run dev &

# 3. The check
cd ../e2e
npm install
npx playwright install chromium   # once
BACKEND_LOG=backend.log npm test
```

Screenshots are saved in `e2e/screenshots/`. Set `BASE_URL` if the frontend is
not at http://localhost:5173, and `CHROMIUM_PATH` to use an existing Chromium.
