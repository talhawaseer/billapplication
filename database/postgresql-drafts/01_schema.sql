-- PostgreSQL schema draft for the Electricity Bill application.
-- Review names/types against the actual remote database before applying.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS consumers (
    consumer_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    consumer_number VARCHAR(50) NOT NULL UNIQUE,
    full_name VARCHAR(150) NOT NULL,
    phone VARCHAR(30),
    email VARCHAR(254),
    address TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS bills (
    bill_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    consumer_id UUID NOT NULL
        REFERENCES consumers(consumer_id) ON DELETE RESTRICT,
    bill_number VARCHAR(60) NOT NULL UNIQUE,
    billing_period_start DATE NOT NULL,
    billing_period_end DATE NOT NULL,
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    payable_amount NUMERIC(19,4) NOT NULL
        CHECK (payable_amount >= 0),
    currency CHAR(3) NOT NULL DEFAULT 'PKR',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (billing_period_end >= billing_period_start),
    CHECK (due_date >= issue_date)
);

CREATE TABLE IF NOT EXISTS payments (
    payment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bill_id UUID NOT NULL
        REFERENCES bills(bill_id) ON DELETE RESTRICT,
    payment_date TIMESTAMPTZ,
    amount_paid NUMERIC(19,4) NOT NULL
        CHECK (amount_paid > 0),
    payment_method TEXT NOT NULL
        CHECK (payment_method IN (
            'cash', 'bank_transfer', 'card',
            'cheque', 'online', 'other'
        )),
    transaction_reference TEXT,
    payment_status TEXT NOT NULL DEFAULT 'pending'
        CHECK (payment_status IN (
            'pending', 'completed', 'failed', 'reversed'
        )),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (
        payment_status <> 'completed'
        OR payment_date IS NOT NULL
    )
);

CREATE INDEX IF NOT EXISTS idx_bills_consumer_date
    ON bills(consumer_id, issue_date DESC);

CREATE INDEX IF NOT EXISTS idx_bills_due_date
    ON bills(due_date);

CREATE INDEX IF NOT EXISTS idx_payments_bill_date
    ON payments(bill_id, payment_date DESC);

CREATE INDEX IF NOT EXISTS idx_payments_status_date
    ON payments(payment_status, payment_date DESC);

CREATE INDEX IF NOT EXISTS idx_consumers_name
    ON consumers(full_name);

CREATE INDEX IF NOT EXISTS idx_consumers_phone
    ON consumers(phone);

-- Optional uniqueness rule: enable only if the business rule is
-- exactly one bill per consumer per billing-period start date.
-- First check existing data for duplicates before adding this:
-- CREATE UNIQUE INDEX uq_bills_consumer_period
--     ON bills(consumer_id, billing_period_start);
