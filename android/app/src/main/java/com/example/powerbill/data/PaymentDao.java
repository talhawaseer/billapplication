package com.example.powerbill.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import java.util.List;

@Dao
public abstract class PaymentDao {
    @Query("SELECT * FROM payments WHERE billId=:billId ORDER BY paymentDate DESC, id DESC")
    public abstract List<Payment> historyForBill(long billId);

    @Query("SELECT * FROM payments ORDER BY paymentDate DESC, id DESC")
    public abstract List<Payment> allHistory();

    @Query("SELECT COALESCE(SUM(amountPaisa),0) FROM payments WHERE billId=:billId AND voided=0")
    public abstract long totalPaidForBill(long billId);

    @Query("SELECT MAX(0, b.amountDuePaisa - COALESCE((SELECT SUM(p.amountPaisa) FROM payments p " +
           "WHERE p.billId=b.id AND p.voided=0),0)) FROM electricity_bills b WHERE b.id=:billId")
    public abstract Long remainingBalance(long billId);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract long insertRow(Payment payment);

    @Transaction
    public long record(Payment payment) {
        if (payment == null || payment.billId <= 0) throw new IllegalArgumentException("Choose a bill.");
        if (payment.amountPaisa <= 0) throw new IllegalArgumentException("Amount must be greater than zero.");
        if (payment.paymentDate == null || payment.paymentDate.trim().isEmpty())
            throw new IllegalArgumentException("Payment date is required.");
        if (payment.method == null || payment.method.trim().isEmpty())
            throw new IllegalArgumentException("Payment method is required.");
        if (payment.receiptNumber == null || payment.receiptNumber.trim().isEmpty())
            throw new IllegalArgumentException("Receipt number is required.");
        Long balance = remainingBalance(payment.billId);
        if (balance == null) throw new IllegalArgumentException("Bill not found.");
        if (balance <= 0) throw new IllegalArgumentException("Bill is already paid.");
        if (payment.amountPaisa > balance) throw new IllegalArgumentException("Payment exceeds balance.");
        return insertRow(payment);
    }

    // No delete/update methods by design: use an audited reversal/void operation instead.
}
