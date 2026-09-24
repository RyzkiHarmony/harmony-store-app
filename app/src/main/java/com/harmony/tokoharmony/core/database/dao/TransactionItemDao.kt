package com.harmony.tokoharmony.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.harmony.tokoharmony.core.database.entity.TransactionItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionItemDao {

    @Query("SELECT * FROM transaction_items WHERE transaction_id = :transactionId")
    fun getItemsForTransaction(transactionId: String): Flow<List<TransactionItemEntity>>

    @Query("SELECT * FROM transaction_items WHERE transaction_id = :transactionId")
    suspend fun getItemsForTransactionDirect(transactionId: String): List<TransactionItemEntity>

    @Query("SELECT * FROM transaction_items WHERE transaction_id = :transactionId AND product_id = :productId LIMIT 1")
    suspend fun getItemByProductAndTransaction(transactionId: String, productId: String): TransactionItemEntity?

    @Query("SELECT * FROM transaction_items WHERE item_id = :itemId")
    suspend fun getItemById(itemId: String): TransactionItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: TransactionItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<TransactionItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionItems(items: List<TransactionItemEntity>)

    @Update
    suspend fun updateItem(item: TransactionItemEntity)

    @Query("DELETE FROM transaction_items WHERE item_id = :itemId")
    suspend fun deleteItem(itemId: String)

    @Query("DELETE FROM transaction_items WHERE transaction_id = :transactionId")
    suspend fun deleteItemsForTransaction(transactionId: String)
}
