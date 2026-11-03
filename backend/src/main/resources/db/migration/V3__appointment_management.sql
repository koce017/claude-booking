-- More appointment statuses, admin-created appointments and fulfillment tracking.

ALTER TABLE appointment DROP CONSTRAINT chk_appointment_status;
ALTER TABLE appointment ADD CONSTRAINT chk_appointment_status
    CHECK (status IN ('PENDING', 'APPROVED', 'DENIED', 'CANCELLED', 'COMPLETED', 'NO_SHOW'));

-- PUBLIC = requested by a customer on the website, ADMIN = entered by an administrator.
ALTER TABLE appointment ADD COLUMN source VARCHAR(10) NOT NULL DEFAULT 'PUBLIC';
ALTER TABLE appointment ADD CONSTRAINT chk_appointment_source CHECK (source IN ('PUBLIC', 'ADMIN'));

-- Internal note visible only to administrators.
ALTER TABLE appointment ADD COLUMN admin_note VARCHAR(1000);

-- When the appointment was last delivered to the company (fulfillment email sent).
-- NULL for an approved appointment means it still has to be handed over manually.
ALTER TABLE appointment ADD COLUMN fulfilled_at TIMESTAMPTZ;

-- Admin calendar and list queries across all companies by time.
CREATE INDEX idx_appointment_start ON appointment (start_at);
