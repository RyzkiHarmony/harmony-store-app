package com.harmony.tokoharmony.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.harmony.tokoharmony.core.database.entity.DigitalTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DigitalTransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDigitalTransaction(item: DigitalTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDigitalTransactions(items: List<DigitalTransactionEntity>)

    @Query("SELECT * FROM digital_transactions WHERE digital_transaction_id = :id LIMIT 1")
    suspend fun getDigitalTransactionById(id: String): DigitalTransactionEntity?

    @Query("SELECT * FROM digital_transactions WHERE transaction_item_id = :itemId LIMIT 1")
    suspend fun getDigitalTransactionByItemId(itemId: String): DigitalTransactionEntity?

    @Query("SELECT * FROM digital_transactions WHERE transaction_item_id = :itemId LIMIT 1")
    fun observeDigitalTransactionByItemId(itemId: String): Flow<DigitalTransactionEntity?>

    @Query("""
        SELECT dt.* FROM digital_transactions dt
        INNER JOIN transaction_items ti ON dt.transaction_item_id = ti.item_id
        WHERE ti.transaction_id = :transactionId
    """)
    suspend fun getDigitalTransactionsForTransaction(transactionId: String): List<DigitalTransactionEntity>

    @Query("""
        SELECT dt.* FROM digital_transactions dt
        INNER JOIN transaction_items ti ON dt.transaction_item_id = ti.item_id
        WHERE ti.transaction_id = :transactionId
    """)
    fun observeDigitalTransactionsForTransaction(transactionId: String): Flow<List<DigitalTransactionEntity>>

    @Query("UPDATE digital_transactions SET status = :status WHERE digital_transaction_id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("DELETE FROM digital_transactions WHERE transaction_item_id = :itemId")
    suspend fun deleteByItemId(itemId: String)

    @Query("SELECT * FROM digital_transactions ORDER BY created_at DESC")
    fun observeAll(): Flow<List<DigitalTransactionEntity>>
}
