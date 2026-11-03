-- One row per bill; payment aggregation occurs before joining to bills.
-- This avoids inflated bill totals when a bill has multiple payments.

CREATE OR REPLACE VIEW bill_balances AS
WITH payment_totals AS (
    SELECT
        bill_id,
        SUM(amount_paid) FILTER (
            WHERE payment_status = 'completed'
        ) AS total_paid
    FROM payments
    GROUP BY bill_id
)
SELECT
    b.bill_id,
    b.bill_number,
    b.consumer_id,
    b.billing_period_start,
    b.billing_period_end,
    b.issue_date,
    b.due_date,
    b.payable_amount,
    COALESCE(p.total_paid, 0::numeric) AS total_paid,
    b.payable_amount - COALESCE(p.total_paid, 0::numeric)
        AS remaining_balance,
    CASE
        WHEN COALESCE(p.total_paid, 0) >= b.payable_amount
            THEN 'fully_paid'
        WHEN b.due_date < CURRENT_DATE
             AND COALESCE(p.total_paid, 0) < b.payable_amount
            THEN 'overdue'
        WHEN COALESCE(p.total_paid, 0) > 0
            THEN 'partially_paid'
        ELSE 'unpaid'
    END AS bill_status
FROM bills b
LEFT JOIN payment_totals p ON p.bill_id = b.bill_id;
