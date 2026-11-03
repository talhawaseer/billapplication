package com.example.powerbill.data;

import androidx.room.Embedded;
import androidx.room.Relation;
import java.util.List;

public class BillWithPayments {
    @Embedded public ElectricityBill bill;

    @Relation(parentColumn = "id", entityColumn = "billId")
    public List<Payment> payments;

    public long getTotalPaymentsPaisa() {
        long total = 0;
        if (payments != null) {
            for (Payment p : payments) {
                if (!p.voided) total = Math.addExact(total, p.amountPaisa);
            }
        }
        return total;
    }

    public long getBalancePaisa() {
        return Math.max(0L, bill.amountDuePaisa - getTotalPaymentsPaisa());
    }

    public boolean isPaid() {
        return getBalancePaisa() == 0;
    }
}
