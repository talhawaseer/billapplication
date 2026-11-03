package com.example.powerbill.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "electricity_bills",
    foreignKeys = @ForeignKey(entity = Consumer.class,
        parentColumns = "id", childColumns = "consumerId",
        onDelete = ForeignKey.RESTRICT),
    indices = {
        @Index("consumerId"),
        @Index(value = {"billNumber"}, unique = true),
        @Index(value = {"consumerId", "billingMonth"}, unique = true)
    })
public class ElectricityBill {
    @PrimaryKey(autoGenerate = true) public long id;
    public long consumerId;
    public String billNumber;
    public String billingMonth;
    public long previousReading;
    public long currentReading;
    public long unitsConsumed;
    public long amountDuePaisa;
    public long lateAmountPaisa;
    public String issueDate;
    public String dueDate;
    public String notes;
    public String billImageUri; // Optional local content URI; never delete it with a bill record.
    public boolean verifiedByUser;
    public long createdAt;

    public ElectricityBill(long consumerId, String billNumber, String billingMonth,
            long previousReading, long currentReading, long amountDuePaisa,
            long lateAmountPaisa, String issueDate, String dueDate,
            boolean verifiedByUser) {
        this.consumerId = consumerId;
        this.billNumber = billNumber;
        this.billingMonth = billingMonth;
        this.previousReading = previousReading;
        this.currentReading = currentReading;
        this.unitsConsumed = currentReading - previousReading;
        this.amountDuePaisa = amountDuePaisa;
        this.lateAmountPaisa = lateAmountPaisa;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.notes = "";
        this.billImageUri = null;
        this.verifiedByUser = verifiedByUser;
        this.createdAt = System.currentTimeMillis();
    }
}
