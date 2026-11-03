package com.example.powerbill.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Transaction;
import androidx.room.OnConflictStrategy;
import androidx.room.Ignore;
import androidx.lifecycle.LiveData;
import java.util.List;

@Dao
public abstract class BillDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    public abstract long insertConsumer(Consumer consumer);

    @Update
    public abstract int updateConsumer(Consumer consumer);

    @Query("DELETE FROM consumers WHERE id = :id")
    public abstract int deleteConsumer(long id);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    public abstract long insertBill(ElectricityBill bill);

    @Update
    public abstract int updateBill(ElectricityBill bill);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract long insertPaymentRow(Payment payment);

    // Intentionally no DELETE query for payments or bills. Use archive/void workflows.
    @Query("SELECT COUNT(*) FROM consumers WHERE active = 1")
    public abstract int countConsumers();

    @Query("SELECT COUNT(*) FROM electricity_bills")
    public abstract int countBills();

    @Query("SELECT COUNT(*) FROM electricity_bills b WHERE b.amountDuePaisa <= " +
           "(SELECT COALESCE(SUM(p.amountPaisa),0) FROM payments p WHERE p.billId=b.id AND p.voided=0)")
    public abstract int countPaidBills();

    @Query("SELECT COUNT(*) FROM electricity_bills b WHERE b.amountDuePaisa > " +
           "(SELECT COALESCE(SUM(p.amountPaisa),0) FROM payments p WHERE p.billId=b.id AND p.voided=0)")
    public abstract int countUnpaidBills();

    @Query("SELECT COALESCE(SUM(amountDuePaisa),0) FROM electricity_bills")
    public abstract long totalPayablePaisa();

    @Query("SELECT COALESCE(SUM(amountPaisa),0) FROM payments WHERE voided=0")
    public abstract long totalPaymentsPaisa();

    @Query("SELECT COALESCE(SUM(MAX(0, b.amountDuePaisa - " +
           "(SELECT COALESCE(SUM(p.amountPaisa),0) FROM payments p WHERE p.billId=b.id AND p.voided=0))),0) " +
           "FROM electricity_bills b")
    public abstract long outstandingPaisa();

    @Query("SELECT COALESCE(SUM(amountPaisa),0) FROM payments WHERE billId=:billId AND voided=0")
    public abstract long totalPaymentsForBillPaisa(long billId);

    @Query("SELECT MAX(0, b.amountDuePaisa - COALESCE(" +
           "(SELECT SUM(p.amountPaisa) FROM payments p WHERE p.billId=b.id AND p.voided=0),0)) " +
           "FROM electricity_bills b WHERE b.id=:billId")
    public abstract Long balanceForBillPaisa(long billId);

    @Query("SELECT * FROM electricity_bills ORDER BY createdAt DESC")
    public abstract List<ElectricityBill> getAllBills();

    @Query("SELECT * FROM consumers WHERE consumerNumber = :number LIMIT 1")
    public abstract Consumer findConsumer(String number);

    @Query("SELECT * FROM consumers WHERE id = :id LIMIT 1")
    public abstract Consumer getConsumerById(long id);

    @Query("SELECT * FROM consumers WHERE (:showArchived = 1 OR active = 1) " +
           "AND (consumerNumber LIKE '%' || :query || '%' OR name LIKE '%' || :query || '%' " +
           "OR referenceNumber LIKE '%' || :query || '%') " +
           "ORDER BY active DESC, name COLLATE NOCASE")
    public abstract List<Consumer> searchConsumers(String query, boolean showArchived);

    @Query("SELECT * FROM consumers WHERE active = 1 ORDER BY name COLLATE NOCASE")
    public abstract List<Consumer> getActiveConsumers();

    @Query("SELECT * FROM electricity_bills WHERE consumerId = :consumerId ORDER BY billingMonth DESC")
    public abstract List<ElectricityBill> getBillsForConsumer(long consumerId);

    @Query("SELECT id FROM electricity_bills WHERE consumerId = :consumerId AND billingMonth = :month LIMIT 1")
    public abstract Long findBillForConsumerAndMonth(long consumerId, String month);

    @Transaction
    @Query("SELECT * FROM electricity_bills WHERE id = :billId LIMIT 1")
    public abstract BillWithPayments getBillWithPayments(long billId);

    @Transaction
    @Query("SELECT * FROM electricity_bills WHERE id = :billId LIMIT 1")
    public abstract LiveData<BillWithPayments> observeBillWithPayments(long billId);

    @Transaction
    @Query("SELECT * FROM consumers WHERE id = :consumerId LIMIT 1")
    public abstract ConsumerWithBills getConsumerWithBills(long consumerId);

    @Query("SELECT p.* FROM payments p WHERE p.billId = :billId ORDER BY p.paymentDate DESC, p.id DESC")
    public abstract List<Payment> getPaymentHistory(long billId);

    @Query("SELECT p.* FROM payments p WHERE p.voided = 0 ORDER BY p.paymentDate DESC, p.id DESC")
    public abstract List<Payment> getAllPayments();

    @Transaction
    public long recordPayment(Payment payment) {
        if (payment == null) throw new IllegalArgumentException("Payment is required.");
        if (payment.billId <= 0) throw new IllegalArgumentException("Select a valid bill.");
        if (payment.amountPaisa <= 0) throw new IllegalArgumentException("Payment amount must be greater than zero.");
        if (payment.paymentDate == null || payment.paymentDate.trim().isEmpty())
            throw new IllegalArgumentException("Payment date is required.");
        if (payment.method == null || payment.method.trim().isEmpty())
            throw new IllegalArgumentException("Payment method is required.");
        if (payment.receiptNumber == null || payment.receiptNumber.trim().isEmpty())
            throw new IllegalArgumentException("Receipt number is required.");

        Long balance = balanceForBillPaisa(payment.billId);
        if (balance == null) throw new IllegalArgumentException("Bill does not exist.");
        if (balance <= 0) throw new IllegalArgumentException("This bill is already fully paid.");
        if (payment.amountPaisa > balance)
            throw new IllegalArgumentException("Payment exceeds remaining balance.");
        return insertPaymentRow(payment);
    }


    // Date strings are ISO-8601 yyyy-MM-dd so lexical date comparisons are safe.
    @Query("SELECT b.*, c.name AS consumerName, c.consumerNumber AS commonId, " +
           "COALESCE((SELECT SUM(p.amountPaisa) FROM payments p WHERE p.billId=b.id AND p.voided=0),0) AS paidPaisa, " +
           "MAX(0, b.amountDuePaisa - COALESCE((SELECT SUM(p.amountPaisa) FROM payments p WHERE p.billId=b.id AND p.voided=0),0)) AS balancePaisa " +
           "FROM electricity_bills b JOIN consumers c ON c.id=b.consumerId " +
           "WHERE b.dueDate = :today AND b.amountDuePaisa > COALESCE((SELECT SUM(p.amountPaisa) FROM payments p WHERE p.billId=b.id AND p.voided=0),0) " +
           "ORDER BY b.dueDate, c.name")
    public abstract List<BillReportRow> billsDueToday(String today);

    @Query("SELECT b.*, c.name AS consumerName, c.consumerNumber AS commonId, " +
           "COALESCE((SELECT SUM(p.amountPaisa) FROM payments p WHERE p.billId=b.id AND p.voided=0),0) AS paidPaisa, " +
           "MAX(0, b.amountDuePaisa - COALESCE((SELECT SUM(p.amountPaisa) FROM payments p WHERE p.billId=b.id AND p.voided=0),0)) AS balancePaisa " +
           "FROM electricity_bills b JOIN consumers c ON c.id=b.consumerId " +
           "WHERE b.dueDate > :today AND b.dueDate <= :upToDate AND b.amountDuePaisa > COALESCE((SELECT SUM(p.amountPaisa) FROM payments p WHERE p.billId=b.id AND p.voided=0),0) " +
           "ORDER BY b.dueDate, c.name")
    public abstract List<BillReportRow> upcomingBills(String today, String upToDate);

    @Query("SELECT b.*, c.name AS consumerName, c.consumerNumber AS commonId, " +
           "COALESCE((SELECT SUM(p.amountPaisa) FROM payments p WHERE p.billId=b.id AND p.voided=0),0) AS paidPaisa, " +
           "MAX(0, b.amountDuePaisa - COALESCE((SELECT SUM(p.amountPaisa) FROM payments p WHERE p.billId=b.id AND p.voided=0),0)) AS balancePaisa " +
           "FROM electricity_bills b JOIN consumers c ON c.id=b.consumerId " +
           "WHERE b.dueDate < :today AND b.amountDuePaisa > COALESCE((SELECT SUM(p.amountPaisa) FROM payments p WHERE p.billId=b.id AND p.voided=0),0) " +
           "ORDER BY b.dueDate, c.name")
    public abstract List<BillReportRow> overdueBills(String today);

    @Query("SELECT b.*, c.name AS consumerName, c.consumerNumber AS commonId, " +
           "COALESCE((SELECT SUM(p.amountPaisa) FROM payments p WHERE p.billId=b.id AND p.voided=0),0) AS paidPaisa, " +
           "MAX(0, b.amountDuePaisa - COALESCE((SELECT SUM(p.amountPaisa) FROM payments p WHERE p.billId=b.id AND p.voided=0),0)) AS balancePaisa " +
           "FROM electricity_bills b JOIN consumers c ON c.id=b.consumerId " +
           "WHERE b.amountDuePaisa <= COALESCE((SELECT SUM(p.amountPaisa) FROM payments p WHERE p.billId=b.id AND p.voided=0),0) " +
           "ORDER BY b.billingMonth DESC, c.name")
    public abstract List<BillReportRow> paidBills();

    @Query("SELECT b.*, c.name AS consumerName, c.consumerNumber AS commonId, " +
           "COALESCE((SELECT SUM(p.amountPaisa) FROM payments p WHERE p.billId=b.id AND p.voided=0),0) AS paidPaisa, " +
           "MAX(0, b.amountDuePaisa - COALESCE((SELECT SUM(p.amountPaisa) FROM payments p WHERE p.billId=b.id AND p.voided=0),0)) AS balancePaisa " +
           "FROM electricity_bills b JOIN consumers c ON c.id=b.consumerId " +
           "WHERE COALESCE((SELECT SUM(p.amountPaisa) FROM payments p WHERE p.billId=b.id AND p.voided=0),0)=0 " +
           "AND b.amountDuePaisa > 0 ORDER BY b.dueDate, c.name")
    public abstract List<BillReportRow> unpaidBills();

    @Query("SELECT b.*, c.name AS consumerName, c.consumerNumber AS commonId, " +
           "COALESCE((SELECT SUM(p.amountPaisa) FROM payments p WHERE p.billId=b.id AND p.voided=0),0) AS paidPaisa, " +
           "MAX(0, b.amountDuePaisa - COALESCE((SELECT SUM(p.amountPaisa) FROM payments p WHERE p.billId=b.id AND p.voided=0),0)) AS balancePaisa " +
           "FROM electricity_bills b JOIN consumers c ON c.id=b.consumerId " +
           "WHERE COALESCE((SELECT SUM(p.amountPaisa) FROM payments p WHERE p.billId=b.id AND p.voided=0),0)>0 " +
           "AND COALESCE((SELECT SUM(p.amountPaisa) FROM payments p WHERE p.billId=b.id AND p.voided=0),0)<b.amountDuePaisa " +
           "ORDER BY b.dueDate, c.name")
    public abstract List<BillReportRow> partiallyPaidBills();

    @Query("SELECT b.billingMonth AS month, SUM(b.amountDuePaisa) AS payablePaisa, " +
           "COALESCE(SUM((SELECT SUM(p.amountPaisa) FROM payments p WHERE p.billId=b.id AND p.voided=0)),0) AS paymentPaisa, " +
           "SUM(MAX(0, b.amountDuePaisa - COALESCE((SELECT SUM(p.amountPaisa) FROM payments p WHERE p.billId=b.id AND p.voided=0),0))) AS outstandingPaisa, " +
           "COUNT(*) AS billCount FROM electricity_bills b " +
           "WHERE b.consumerId=:consumerId AND b.billingMonth >= :fromMonth AND b.billingMonth <= :toMonth " +
           "GROUP BY b.billingMonth ORDER BY b.billingMonth")
    public abstract List<MonthlySummaryRow> consumerMonthlyHistory(long consumerId, String fromMonth, String toMonth);

    @Query("SELECT b.billingMonth AS month, SUM(b.amountDuePaisa) AS payablePaisa, " +
           "COALESCE(SUM((SELECT SUM(p.amountPaisa) FROM payments p WHERE p.billId=b.id AND p.voided=0)),0) AS paymentPaisa, " +
           "SUM(MAX(0, b.amountDuePaisa - COALESCE((SELECT SUM(p.amountPaisa) FROM payments p WHERE p.billId=b.id AND p.voided=0),0))) AS outstandingPaisa, " +
           "COUNT(*) AS billCount FROM electricity_bills b " +
           "WHERE b.billingMonth >= :fromMonth AND b.billingMonth <= :toMonth " +
           "GROUP BY b.billingMonth ORDER BY b.billingMonth")
    public abstract List<MonthlySummaryRow> monthlyReport(String fromMonth, String toMonth);

    @Query("SELECT COALESCE(AVG(amountDuePaisa),0) FROM electricity_bills " +
           "WHERE consumerId=:consumerId AND billingMonth >= :fromMonth AND billingMonth <= :toMonth")
    public abstract double averageMonthlyBillPaisa(long consumerId, String fromMonth, String toMonth);

    @Query("SELECT COALESCE(SUM(amountDuePaisa),0) FROM electricity_bills WHERE billingMonth >= :fromMonth AND billingMonth <= :toMonth")
    public abstract long totalPayableForPeriod(String fromMonth, String toMonth);

    @Query("SELECT COALESCE(SUM(p.amountPaisa),0) FROM payments p JOIN electricity_bills b ON b.id=p.billId " +
           "WHERE p.voided=0 AND p.paymentDate >= :fromDate AND p.paymentDate <= :toDate")
    public abstract long totalPaymentsForPeriod(String fromDate, String toDate);

    @Transaction
    public long saveConsumerAndBill(Consumer consumer, ElectricityBill bill) {
        long consumerId = insertConsumer(consumer);
        bill.consumerId = consumerId;
        return insertBill(bill);
    }
}
