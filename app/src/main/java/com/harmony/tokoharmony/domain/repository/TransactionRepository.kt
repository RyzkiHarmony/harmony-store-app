package com.harmony.tokoharmony.domain.repository

import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.model.DraftCart
import com.harmony.tokoharmony.domain.model.Transaction
import com.harmony.tokoharmony.domain.model.TransactionItem
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getActiveDraftCart(): Flow<DraftCart?>
    suspend fun getOrCreateActiveDraft(userId: String): DraftCart
    suspend fun addItemToDraft(productId: String, quantity: Long, userId: String): Result<DraftCart>
    suspend fun updateDraftItemQuantity(itemId: String, newQuantity: Long): Result<DraftCart>
    suspend fun removeDraftItem(itemId: String): Result<DraftCart>
    suspend fun clearDraft(transactionId: String): Result<Unit>
    suspend fun getTransactionById(transactionId: String): Transaction?
    suspend fun getItemsForTransaction(transactionId: String): List<TransactionItem>
    suspend fun completeCashTransaction(transactionId: String, amountReceived: Long, userId: String): Result<Transaction>
    suspend fun completeQrisTransaction(transactionId: String, userId: String): Result<Transaction>
    fun getAllTransactions(): Flow<List<Transaction>>
    suspend fun cancelTransaction(transactionId: String, reason: String, userId: String): Result<Transaction>
}
