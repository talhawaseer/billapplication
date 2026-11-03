-- Parameterized examples. Bind values using your database client/driver.
-- PostgreSQL placeholders shown as $1, $2, etc.

-- CONSUMERS

-- Add consumer
INSERT INTO consumers (consumer_number, full_name, phone, email, address)
VALUES ($1, $2, $3, $4, $5)
RETURNING *;

-- Update consumer
UPDATE consumers
SET full_name = $2,
    phone = $3,
    email = $4,
    address = $5,
    updated_at = now()
WHERE consumer_id = $1
RETURNING *;

-- Search by number, phone, or name
SELECT *
FROM consumers
WHERE consumer_number = $1
   OR phone = $1
   OR full_name ILIKE '%' || $1 || '%'
ORDER BY full_name
LIMIT 100;

-- Delete succeeds only when no bills reference the consumer.
-- For financial records, prefer deactivation/archival over deletion.
DELETE FROM consumers
WHERE consumer_id = $1
RETURNING *;


-- BILLS

INSERT INTO bills (
    consumer_id, bill_number,
    billing_period_start, billing_period_end,
    issue_date, due_date, payable_amount, currency
)
VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
RETURNING *;

-- Example update for a non-financial field. Bill amount changes require
-- a controlled adjustment process once payments exist.
UPDATE bills
SET due_date = $2,
    updated_at = now()
WHERE bill_id = $1
RETURNING *;

SELECT *
FROM bills
WHERE bill_number = $1;

SELECT *
FROM bills
WHERE consumer_id = $1
  AND issue_date >= $2::date
  AND issue_date < $3::date
ORDER BY issue_date DESC;

-- Deletion fails when payments reference this bill.
DELETE FROM bills
WHERE bill_id = $1
RETURNING *;


-- PAYMENTS

-- Create a pending payment record. Confirm payment before marking completed.
INSERT INTO payments (
    bill_id, amount_paid, payment_method,
    transaction_reference, payment_status
)
VALUES ($1, $2, $3, $4, 'pending')
RETURNING *;

UPDATE payments
SET payment_status = 'completed',
    payment_date = $2::timestamptz,
    updated_at = now()
WHERE payment_id = $1
  AND payment_status = 'pending'
RETURNING *;

SELECT *
FROM payments
WHERE transaction_reference = $1
ORDER BY created_at DESC;

SELECT *
FROM payments
WHERE payment_status = $1
  AND payment_date >= $2::timestamptz
  AND payment_date < $3::timestamptz
ORDER BY payment_date DESC;

-- Illustrative reversal status update only. Production should require
-- an authorized reason and append-only audit event.
UPDATE payments
SET payment_status = 'reversed',
    updated_at = now()
WHERE payment_id = $1
  AND payment_status = 'completed'
RETURNING *;
