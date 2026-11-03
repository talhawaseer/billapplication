package com.example.electricbill.data.repository

import com.example.electricbill.data.dao.BillDao
import com.example.electricbill.data.entity.ElectricityBillEntity
import kotlinx.coroutines.flow.Flow

class InvalidBillException(message: String) : IllegalArgumentException(message)

interface BillRepository {
    fun observeAllBills(): Flow<List<ElectricityBillEntity>>
    fun observeBillsForConsumer(consumerRowId: Long): Flow<List<ElectricityBillEntity>>
    fun observeBill(billId: Long): Flow<ElectricityBillEntity?>
    suspend fun getBill(billId: Long): ElectricityBillEntity?
    suspend fun addBill(bill: ElectricityBillEntity): Long
    suspend fun editBill(bill: ElectricityBillEntity)
    suspend fun findBillForMonth(consumerRowId: Long, billingMonth: String): ElectricityBillEntity?
    fun observeBillsDueOnOrBefore(date: String): Flow<List<ElectricityBillEntity>>
}

class RoomBillRepository(private val dao: BillDao) : BillRepository {
    override fun observeAllBills() = dao.observeAll()
    override fun observeBillsForConsumer(consumerRowId: Long) = dao.observeForConsumer(consumerRowId)
    override fun observeBill(billId: Long) = dao.observeById(billId)
    override suspend fun getBill(billId: Long) = dao.getById(billId)
    override suspend fun findBillForMonth(consumerRowId: Long, billingMonth: String) =
        dao.findByConsumerAndMonth(consumerRowId, billingMonth)
    override fun observeBillsDueOnOrBefore(date: String) = dao.observeBillsDueOnOrBefore(date)

    override suspend fun addBill(bill: ElectricityBillEntity): Long {
        validate(bill)
        if (dao.findByConsumerAndMonth(bill.consumerRowId, bill.billingMonth) != null) {
            throw InvalidBillException("A bill already exists for this consumer and billing month.")
        }
        // SQLite unique index remains the final authority for concurrent inserts.
        return dao.insert(bill.normalized())
    }

    override suspend fun editBill(bill: ElectricityBillEntity) {
        require(bill.id > 0) { "A saved bill ID is required for editing." }
        validate(bill)
        val existing = dao.getById(bill.id)
            ?: throw InvalidBillException("Bill not found.")
        val duplicate = dao.findByConsumerAndMonth(bill.consumerRowId, bill.billingMonth)
        if (duplicate != null && duplicate.id != bill.id) {
            throw InvalidBillException("Another bill already exists for this consumer and billing month.")
        }
        dao.update(
            bill.normalized().copy(
                createdAtEpochMillis = existing.createdAtEpochMillis,
                updatedAtEpochMillis = System.currentTimeMillis()
            )
        )
    }

    private fun validate(b: ElectricityBillEntity) {
        require(b.consumerRowId > 0) { "Select a valid consumer." }
        require(b.electricityProvider.isNotBlank()) { "Electricity provider is required." }
        require(b.referenceNumber.isNotBlank()) { "Reference number is required." }
        require(b.billingMonth.matches(Regex("\\d{4}-(0[1-9]|1[0-2])"))) {
            "Billing month must use yyyy-MM format."
        }
        require(b.unitsConsumed >= 0) { "Units consumed cannot be negative." }
        require(b.previousMeterReading == null || b.previousMeterReading >= 0) {
            "Previous meter reading cannot be negative."
        }
        require(b.presentMeterReading == null || b.presentMeterReading >= 0) {
            "Present meter reading cannot be negative."
        }
        if (b.previousMeterReading != null && b.presentMeterReading != null) {
            require(b.presentMeterReading >= b.previousMeterReading) {
                "Present meter reading cannot be less than previous reading."
            }
        }
        require(b.totalPayablePaisa >= 0) { "Total payable amount cannot be negative." }
        require(b.latePaymentAmountPaisa == null || b.latePaymentAmountPaisa >= 0) {
            "Late-payment amount cannot be negative."
        }
        require(b.payableAfterDueDatePaisa == null || b.payableAfterDueDatePaisa >= 0) {
            "Payable-after-due-date amount cannot be negative."
        }
        require(b.electricityProvider.trim().length <= 100) { "Provider must be 100 characters or fewer." }
        require(b.referenceNumber.trim().length <= 100) { "Reference number must be 100 characters or fewer." }
        require((b.billImagePath?.length ?: 0) <= 2048) { "Bill image path is too long." }
    }

    private fun ElectricityBillEntity.normalized() = copy(
        electricityProvider = electricityProvider.trim(),
        referenceNumber = referenceNumber.trim(),
        billImagePath = billImagePath?.trim()?.ifEmpty { null },
        notes = notes?.trim()?.ifEmpty { null }
    )
}
