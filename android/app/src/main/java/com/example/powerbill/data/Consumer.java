package com.example.powerbill.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "consumers",
        indices = {@Index(value = {"consumerNumber"}, unique = true)})
public class Consumer {
    @PrimaryKey(autoGenerate = true) public long id;
    @NonNull public String consumerNumber; // Unique common ID
    @NonNull public String name;           // Consumer name
    public String referenceNumber;         // Electricity reference number
    public String meterNumber;
    public String address;
    public String notes;
    public boolean active = true;          // false means archived

    public Consumer(@NonNull String consumerNumber, @NonNull String name,
                    String referenceNumber, String meterNumber, String address) {
        this.consumerNumber = consumerNumber;
        this.name = name;
        this.referenceNumber = referenceNumber;
        this.meterNumber = meterNumber;
        this.address = address;
        this.notes = "";
    }
}
