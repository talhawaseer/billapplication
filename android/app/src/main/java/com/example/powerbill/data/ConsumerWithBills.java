package com.example.powerbill.data;

import androidx.room.Embedded;
import androidx.room.Relation;
import java.util.List;

public class ConsumerWithBills {
    @Embedded public Consumer consumer;

    @Relation(parentColumn = "id", entityColumn = "consumerId")
    public List<ElectricityBill> bills;
}
