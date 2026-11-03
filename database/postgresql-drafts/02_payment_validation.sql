-- Prevent completed payments for a bill from exceeding its payable amount.
-- The bill row lock serializes payment writes for the same bill.
-- Apply after creating consumers, bills, and payments.

CREATE OR REPLACE FUNCTION validate_bill_payment()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_payable NUMERIC(19,4);
    v_existing NUMERIC(19,4);
BEGIN
    IF TG_OP = 'UPDATE'
       AND NEW.bill_id <> OLD.bill_id THEN
        RAISE EXCEPTION
            'Moving a payment to another bill is prohibited';
    END IF;

    SELECT payable_amount
      INTO v_payable
      FROM bills
     WHERE bill_id = NEW.bill_id
     FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Bill does not exist';
    END IF;

    IF NEW.payment_status = 'completed' THEN
        SELECT COALESCE(SUM(amount_paid), 0)
          INTO v_existing
          FROM payments
         WHERE bill_id = NEW.bill_id
           AND payment_status = 'completed'
           AND payment_id IS DISTINCT FROM NEW.payment_id;

        IF v_existing + NEW.amount_paid > v_payable THEN
            RAISE EXCEPTION
                'Payment exceeds outstanding bill balance. '
                'Payable: %, previously paid: %, attempted: %',
                v_payable, v_existing, NEW.amount_paid;
        END IF;
    END IF;

    NEW.updated_at := now();
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_validate_bill_payment ON payments;

CREATE TRIGGER trg_validate_bill_payment
BEFORE INSERT OR UPDATE ON payments
FOR EACH ROW
EXECUTE FUNCTION validate_bill_payment();

-- Production note: also prevent uncontrolled changes to bills.payable_amount
-- when completed payments exist. Use an authorized adjustment procedure and
-- immutable audit records for bill adjustments, refunds, and reversals.
