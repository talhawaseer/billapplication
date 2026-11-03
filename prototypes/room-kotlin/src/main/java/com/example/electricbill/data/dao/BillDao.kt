package com.example.electricbill.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.electricbill.data.entity.ElectricityBillEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(bill: ElectricityBillEntity): Long

    @Update
    suspend fun update(bill: ElectricityBillEntity)

    @Query("SELECT * FROM bills WHERE id = :billId LIMIT 1")
    suspend fun getById(billId: Long): ElectricityBillEntity?

    @Query("SELECT * FROM bills WHERE id = :billId LIMIT 1")
    fun observeById(billId: Long): Flow<ElectricityBillEntity?>

    @Query("SELECT * FROM bills WHERE consumerRowId = :consumerRowId ORDER BY billingMonth DESC")
    fun observeForConsumer(consumerRowId: Long): Flow<List<ElectricityBillEntity>>

    @Query("SELECT * FROM bills ORDER BY billingMonth DESC, id DESC")
    fun observeAll(): Flow<List<ElectricityBillEntity>>

    @Query("SELECT * FROM bills WHERE consumerRowId = :consumerRowId AND billingMonth = :billingMonth LIMIT 1")
    suspend fun findByConsumerAndMonth(consumerRowId: Long, billingMonth: String): ElectricityBillEntity?

    @Query("SELECT * FROM bills WHERE dueDate IS NOT NULL AND dueDate <= :date ORDER BY dueDate ASC")
    fun observeBillsDueOnOrBefore(date: String): Flow<List<ElectricityBillEntity>>
}
