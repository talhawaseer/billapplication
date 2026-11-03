package com.example.powerbill.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "payments",
    foreignKeys = @ForeignKey(entity = ElectricityBill.class,
        parentColumns = "id", childColumns = "billId",
        onDelete = ForeignKey.RESTRICT),
    indices = {
        @Index("billId"),
        @Index(value = {"receiptNumber"}, unique = true),
        @Index(value = {"transactionReference"})
    })
public class Payment {
    @PrimaryKey(autoGenerate = true) public long id;
    public long billId;
    public long amountPaisa;
    @NonNull public String paymentDate;
    @NonNull public String method;
    @NonNull public String receiptNumber;
    public String transactionReference;
    // Retained for schema compatibility with database version 1/2.
    public String reference;
    public String notes;
    public long createdAt;
    // Payments are immutable financial events. Reverse them with a void/reversal record,
    // never by deleting or editing the original row.
    public boolean voided;
    public String voidReason;

    public Payment(long billId, long amountPaisa, @NonNull String paymentDate,
                   @NonNull String method, @NonNull String receiptNumber,
                   String transactionReference, String notes) {
        this.billId = billId;
        this.amountPaisa = amountPaisa;
        this.paymentDate = paymentDate;
        this.method = method;
        this.receiptNumber = receiptNumber;
        this.transactionReference = transactionReference;
        this.reference = transactionReference;
        this.notes = notes;
        this.createdAt = System.currentTimeMillis();
        this.voided = false;
    }
}
