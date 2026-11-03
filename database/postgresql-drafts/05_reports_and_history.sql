-- MONTHLY ELECTRICITY EXPENSES (based on bills issued, not cash received)
-- Last 12 calendar months including the current month.
SELECT
    date_trunc('month', issue_date)::date AS month,
    COUNT(*) AS bills_issued,
    SUM(payable_amount) AS electricity_expenses,
    AVG(payable_amount) AS average_bill_amount
FROM bills
WHERE issue_date >= date_trunc('month', CURRENT_DATE) - INTERVAL '11 months'
  AND issue_date <  date_trunc('month', CURRENT_DATE) + INTERVAL '1 month'
GROUP BY date_trunc('month', issue_date)
ORDER BY month;


-- YEARLY EXPENSES
SELECT
    EXTRACT(YEAR FROM issue_date)::int AS year,
    COUNT(*) AS bill_count,
    SUM(payable_amount) AS yearly_expenses,
    AVG(payable_amount) AS average_bill_amount
FROM bills
GROUP BY EXTRACT(YEAR FROM issue_date)
ORDER BY year DESC;


-- AVERAGE BILL AMOUNT AND AVERAGE DUE DATE
SELECT
    COUNT(*) AS bill_count,
    AVG(payable_amount) AS average_bill_amount,
    (
        DATE '2000-01-01'
        + ROUND(AVG(due_date - DATE '2000-01-01'))::int
    ) AS average_due_date
FROM bills;


-- TOTAL COMPLETED PAYMENTS RECEIVED BY MONTH (last 12 calendar months)
SELECT
    date_trunc('month', payment_date)::date AS month,
    COUNT(*) AS completed_payment_count,
    SUM(amount_paid) AS total_payments_received
FROM payments
WHERE payment_status = 'completed'
  AND payment_date >= date_trunc('month', CURRENT_DATE) - INTERVAL '11 months'
  AND payment_date <  date_trunc('month', CURRENT_DATE) + INTERVAL '1 month'
GROUP BY date_trunc('month', payment_date)
ORDER BY month;


-- UNPAID BILLS (no completed payments) and overdue unpaid bills
SELECT
    c.consumer_number,
    c.full_name,
    b.bill_number,
    b.due_date,
    b.payable_amount,
    bb.remaining_balance,
    bb.bill_status
FROM bill_balances bb
JOIN consumers c ON c.consumer_id = bb.consumer_id
JOIN bills b ON b.bill_id = bb.bill_id
WHERE bb.total_paid = 0
  AND bb.bill_status IN ('unpaid', 'overdue')
ORDER BY b.due_date;


-- PARTIALLY PAID OR OVERDUE-PARTIAL BILLS
SELECT
    c.consumer_number,
    c.full_name,
    b.bill_number,
    b.due_date,
    b.payable_amount,
    bb.total_paid,
    bb.remaining_balance,
    bb.bill_status
FROM bill_balances bb
JOIN consumers c ON c.consumer_id = bb.consumer_id
JOIN bills b ON b.bill_id = bb.bill_id
WHERE bb.total_paid > 0
  AND bb.remaining_balance > 0
ORDER BY b.due_date;


-- TOTAL OUTSTANDING BALANCES
SELECT
    COUNT(*) FILTER (WHERE remaining_balance > 0) AS bills_with_balance,
    COUNT(*) FILTER (WHERE bill_status = 'overdue') AS overdue_bills,
    SUM(remaining_balance) FILTER (WHERE remaining_balance > 0)
        AS total_outstanding_balance,
    SUM(remaining_balance) FILTER (WHERE bill_status = 'overdue')
        AS total_overdue_balance
FROM bill_balances;


-- ALL BILLS FOR A CONSUMER, WITH PAYMENT TOTALS AND BALANCES
-- Bind $1 to the consumer UUID.
SELECT
    c.consumer_number,
    c.full_name,
    b.bill_id,
    b.bill_number,
    b.billing_period_start,
    b.billing_period_end,
    b.issue_date,
    b.due_date,
    b.payable_amount,
    bb.total_paid,
    bb.remaining_balance,
    bb.bill_status
FROM bills b
JOIN consumers c ON c.consumer_id = b.consumer_id
JOIN bill_balances bb ON bb.bill_id = b.bill_id
WHERE c.consumer_id = $1::uuid
ORDER BY b.issue_date DESC, b.bill_number;


-- COMPLETE PAYMENT HISTORY FOR A CONSUMER, including pending/failed/reversed
SELECT
    c.consumer_number,
    c.full_name,
    b.bill_number,
    b.payable_amount,
    p.payment_id,
    p.payment_date,
    p.amount_paid,
    p.payment_method,
    p.transaction_reference,
    p.payment_status,
    p.created_at
FROM consumers c
JOIN bills b ON b.consumer_id = c.consumer_id
JOIN payments p ON p.bill_id = b.bill_id
WHERE c.consumer_id = $1::uuid
ORDER BY p.payment_date DESC NULLS LAST, p.created_at DESC, p.payment_id;


-- COMPLETE PAYMENT HISTORY FOR A SINGLE BILL
-- Bind $1 to the bill UUID.
SELECT *
FROM payments
WHERE bill_id = $1::uuid
ORDER BY payment_date DESC NULLS LAST, created_at DESC;


-- BILLS FROM THE LAST 12 MONTHS
SELECT
    c.consumer_number,
    c.full_name,
    b.bill_number,
    b.issue_date,
    b.due_date,
    b.payable_amount,
    bb.total_paid,
    bb.remaining_balance,
    bb.bill_status
FROM bill_balances bb
JOIN bills b ON b.bill_id = bb.bill_id
JOIN consumers c ON c.consumer_id = bb.consumer_id
WHERE b.issue_date >= CURRENT_DATE - INTERVAL '12 months'
ORDER BY b.issue_date DESC;


-- Include bills with no payment records in a history screen by using LEFT JOIN:
SELECT
    b.bill_id,
    b.bill_number,
    b.issue_date,
    p.payment_id,
    p.payment_date,
    p.amount_paid,
    p.payment_status
FROM bills b
LEFT JOIN payments p ON p.bill_id = b.bill_id
WHERE b.consumer_id = $1::uuid
ORDER BY b.issue_date DESC, p.payment_date DESC NULLS LAST;
