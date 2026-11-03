-- Company settings that administrators can now edit.

-- Where approved appointments are emailed. Admin-only: never exposed by public endpoints.
ALTER TABLE company ADD COLUMN fulfillment_email VARCHAR(320);

-- Step between slot start times. NULL = same as the appointment duration.
ALTER TABLE company ADD COLUMN slot_interval_minutes INTEGER;
ALTER TABLE company ADD CONSTRAINT chk_company_slot_interval
    CHECK (slot_interval_minutes IS NULL OR slot_interval_minutes > 0);

-- Which customer fields the booking form asks for (see CustomerFormConfig). NULL = defaults.
ALTER TABLE company ADD COLUMN customer_form JSONB;

-- TRUE once an administrator has saved a weekly schedule. Until then the default
-- Mon-Fri 09:00-17:00 applies. A saved schedule may have no periods at all.
ALTER TABLE company ADD COLUMN custom_weekly_schedule BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE company c SET custom_weekly_schedule = TRUE
WHERE EXISTS (SELECT 1 FROM weekly_schedule_period w WHERE w.company_id = c.id);
