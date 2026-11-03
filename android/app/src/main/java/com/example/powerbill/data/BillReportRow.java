package com.example.powerbill.data;

import androidx.room.Embedded;

public class BillReportRow {
    @Embedded public ElectricityBill bill;
    public String consumerName;
    public String commonId;
    public long paidPaisa;
    public long balancePaisa;

    public String status(String today) {
        if (balancePaisa <= 0) return "PAID";
        if (paidPaisa > 0) return bill.dueDate.compareTo(today) < 0 ? "PARTIALLY PAID · OVERDUE" : "PARTIALLY PAID";
        return bill.dueDate.compareTo(today) < 0 ? "OVERDUE" : "UNPAID";
    }
}
