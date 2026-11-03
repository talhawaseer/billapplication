package com.example.electricbill.data.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.electricbill.data.entity.ConsumerEntity
import com.example.electricbill.data.entity.ElectricityBillEntity

data class ConsumerWithBills(
    @Embedded val consumer: ConsumerEntity,
    @Relation(parentColumn = "id", entityColumn = "consumerRowId")
    val bills: List<ElectricityBillEntity>
)
