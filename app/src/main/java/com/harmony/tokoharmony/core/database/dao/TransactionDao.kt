package com.harmony.tokoharmony.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.harmony.tokoharmony.core.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions WHERE transaction_status = 'DRAFT' ORDER BY created_at DESC LIMIT 1")
    fun getActiveDraft(): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions WHERE transaction_status = 'DRAFT' ORDER BY created_at DESC LIMIT 1")
    suspend fun getActiveDraftDirect(): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE transaction_id = :transactionId")
    suspend fun getTransactionById(transactionId: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("UPDATE transactions SET total_amount = :totalAmount WHERE transaction_id = :transactionId")
    suspend fun updateTotalAmount(transactionId: String, totalAmount: Long)

    @Query("SELECT * FROM transactions WHERE transaction_status != 'DRAFT' ORDER BY created_at DESC")
    fun getAllCompletedAndCancelledTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY created_at DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("DELETE FROM transactions WHERE transaction_id = :transactionId")
    suspend fun deleteTransaction(transactionId: String)
}
