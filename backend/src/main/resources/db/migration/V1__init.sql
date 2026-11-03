-- Core schema for the appointment booking platform.
-- Company settings are nullable on purpose: NULL means "use the application default"
-- (resolved in CompanySettingsResolver), so the schema stays permissive while the
-- service layer stays authoritative.

CREATE TABLE company (
    id                           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name                         VARCHAR(200)  NOT NULL,
    description                  TEXT,
    logo_url                     VARCHAR(1000),
    address                      VARCHAR(500),
    phone                        VARCHAR(50),
    email                        VARCHAR(320),
    website                      VARCHAR(1000),
    timezone                     VARCHAR(64),
    appointment_duration_minutes INTEGER,
    max_concurrent_appointments  INTEGER,
    booking_window_months        INTEGER,
    active                       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at                   TIMESTAMPTZ   NOT NULL,
    updated_at                   TIMESTAMPTZ   NOT NULL,
    CONSTRAINT chk_company_name_not_blank CHECK (length(trim(name)) > 0),
    CONSTRAINT chk_company_duration CHECK (appointment_duration_minutes IS NULL OR appointment_duration_minutes > 0),
    CONSTRAINT chk_company_capacity CHECK (max_concurrent_appointments IS NULL OR max_concurrent_appointments > 0),
    CONSTRAINT chk_company_window CHECK (booking_window_months IS NULL OR booking_window_months > 0)
);

CREATE INDEX idx_company_active_name ON company (active, name);

-- Normal weekly schedule. Several rows per day are allowed (e.g. split shifts).
-- A company with no rows at all uses the default schedule (Mon-Fri 09:00-17:00).
CREATE TABLE weekly_schedule_period (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    company_id  BIGINT   NOT NULL REFERENCES company (id) ON DELETE CASCADE,
    day_of_week SMALLINT NOT NULL,
    start_time  TIME     NOT NULL,
    end_time    TIME     NOT NULL,
    CONSTRAINT chk_weekly_day CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT chk_weekly_times CHECK (end_time > start_time)
);

CREATE INDEX idx_weekly_schedule_company ON weekly_schedule_period (company_id, day_of_week);

-- Specific-date override. An override without periods means "closed that day";
-- with periods, they replace the weekly schedule for that date.
CREATE TABLE schedule_override (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    company_id    BIGINT       NOT NULL REFERENCES company (id) ON DELETE CASCADE,
    override_date DATE         NOT NULL,
    note          VARCHAR(200),
    CONSTRAINT uq_schedule_override_company_date UNIQUE (company_id, override_date)
);

CREATE TABLE schedule_override_period (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    override_id BIGINT NOT NULL REFERENCES schedule_override (id) ON DELETE CASCADE,
    start_time  TIME   NOT NULL,
    end_time    TIME   NOT NULL,
    CONSTRAINT chk_override_times CHECK (end_time > start_time)
);

CREATE INDEX idx_override_period_override ON schedule_override_period (override_id);

CREATE TABLE appointment (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    company_id     BIGINT       NOT NULL REFERENCES company (id),
    start_at       TIMESTAMPTZ  NOT NULL,
    end_at         TIMESTAMPTZ  NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    customer_name  VARCHAR(200) NOT NULL,
    customer_phone VARCHAR(50),
    customer_email VARCHAR(320),
    -- Reserved for company-configurable customer fields (address, custom questions, ...).
    customer_extra JSONB,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,
    CONSTRAINT chk_appointment_times CHECK (end_at > start_at),
    CONSTRAINT chk_appointment_status CHECK (status IN ('PENDING', 'APPROVED', 'DENIED'))
);

CREATE INDEX idx_appointment_company_start ON appointment (company_id, start_at);
CREATE INDEX idx_appointment_company_status_start ON appointment (company_id, status, start_at);
CREATE INDEX idx_appointment_status_start ON appointment (status, start_at);

CREATE TABLE administrator (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email      VARCHAR(320) NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL
);

-- Emails are compared case-insensitively.
CREATE UNIQUE INDEX uq_administrator_email ON administrator (lower(email));

-- Magic-link tokens: only the SHA-256 hash is stored.
CREATE TABLE admin_login_token (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    administrator_id BIGINT      NOT NULL REFERENCES administrator (id) ON DELETE CASCADE,
    token_hash       VARCHAR(64) NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL,
    expires_at       TIMESTAMPTZ NOT NULL,
    used_at          TIMESTAMPTZ,
    CONSTRAINT uq_admin_login_token_hash UNIQUE (token_hash)
);

-- Authenticated admin sessions (opaque bearer tokens, stored hashed).
CREATE TABLE admin_session (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    administrator_id BIGINT      NOT NULL REFERENCES administrator (id) ON DELETE CASCADE,
    token_hash       VARCHAR(64) NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL,
    expires_at       TIMESTAMPTZ NOT NULL,
    revoked_at       TIMESTAMPTZ,
    CONSTRAINT uq_admin_session_hash UNIQUE (token_hash)
);
