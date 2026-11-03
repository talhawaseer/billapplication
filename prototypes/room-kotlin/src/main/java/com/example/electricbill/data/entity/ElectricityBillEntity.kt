package com.example.electricbill.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bills",
    foreignKeys = [
        ForeignKey(
            entity = ConsumerEntity::class,
            parentColumns = ["id"],
            childColumns = ["consumerRowId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["consumerRowId", "billingMonth"], unique = true),
        Index(value = ["billingMonth"]),
        Index(value = ["dueDate"]),
        Index(value = ["referenceNumber"])
    ]
)
data class ElectricityBillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val consumerRowId: Long,

    // Snapshots preserve the details shown on the original bill.
    val electricityProvider: String,
    val referenceNumber: String,

    val billingMonth: String, // yyyy-MM
    val billingDate: String? = null, // yyyy-MM-dd
    val meterReadingDate: String? = null,
    val issueDate: String? = null,
    val dueDate: String? = null,

    val previousMeterReading: Long? = null,
    val presentMeterReading: Long? = null,
    val unitsConsumed: Long,

    val totalPayablePaisa: Long,
    val latePaymentAmountPaisa: Long? = null,
    val latePaymentApplicableDate: String? = null,
    val payableAfterDueDatePaisa: Long? = null,
    val payableAfterDueDateApplicableDate: String? = null,

    // Store a durable app-private file path/URI, not a temporary cache path.
    val billImagePath: String? = null,
    val notes: String? = null,

    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = System.currentTimeMillis()
)
