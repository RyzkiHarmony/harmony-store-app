package com.harmony.tokoharmony.data.repository

import androidx.room.withTransaction
import com.harmony.tokoharmony.core.common.AppError
import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.core.database.AppDatabase
import com.harmony.tokoharmony.core.database.dao.ProductDao
import com.harmony.tokoharmony.core.database.dao.StockMovementDao
import com.harmony.tokoharmony.core.database.dao.TransactionDao
import com.harmony.tokoharmony.core.database.dao.TransactionItemDao
import com.harmony.tokoharmony.core.database.entity.StockMovementEntity
import com.harmony.tokoharmony.core.database.entity.TransactionEntity
import com.harmony.tokoharmony.core.database.entity.TransactionItemEntity
import com.harmony.tokoharmony.core.database.dao.SyncQueueDao
import com.harmony.tokoharmony.core.database.entity.SyncQueueEntity
import com.harmony.tokoharmony.core.network.SyncJsonMapper
import com.harmony.tokoharmony.core.sync.SyncManager
import com.harmony.tokoharmony.domain.model.SyncEntityType
import com.harmony.tokoharmony.domain.model.SyncOperation
import com.harmony.tokoharmony.domain.model.SyncStatus
import com.harmony.tokoharmony.data.local.mapper.toDomain
import com.harmony.tokoharmony.data.local.mapper.toEntity
import com.harmony.tokoharmony.domain.model.DraftCart
import com.harmony.tokoharmony.domain.model.MovementType
import com.harmony.tokoharmony.domain.model.PaymentMethod
import com.harmony.tokoharmony.domain.model.PaymentStatus
import com.harmony.tokoharmony.domain.model.ProductKind
import com.harmony.tokoharmony.domain.model.QuantityType
import com.harmony.tokoharmony.domain.model.ReferenceType
import com.harmony.tokoharmony.domain.model.Transaction
import com.harmony.tokoharmony.domain.model.TransactionItem
import com.harmony.tokoharmony.domain.model.TransactionStatus
import com.harmony.tokoharmony.domain.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val transactionDao: TransactionDao,
    private val transactionItemDao: TransactionItemDao,
    private val productDao: ProductDao,
    private val stockMovementDao: StockMovementDao,
    private val syncQueueDao: SyncQueueDao,
    private val syncManager: SyncManager
) : TransactionRepository {

    override fun getActiveDraftCart(): Flow<DraftCart?> {
        return transactionDao.getActiveDraft().flatMapLatest { draftTx ->
            if (draftTx == null) {
                flowOf(null)
            } else {
                transactionItemDao.getItemsForTransaction(draftTx.transactionId).map { items ->
                    DraftCart(
                        transaction = draftTx.toDomain(),
                        items = items.map { it.toDomain() }
                    )
                }
            }
        }
    }

    override suspend fun getOrCreateActiveDraft(userId: String): DraftCart {
        return database.withTransaction {
            val existing = transactionDao.getActiveDraftDirect()
            if (existing != null) {
                val items = transactionItemDao.getItemsForTransactionDirect(existing.transactionId)
                DraftCart(existing.toDomain(), items.map { it.toDomain() })
            } else {
                val now = System.currentTimeMillis()
                val newTx = TransactionEntity(
                    transactionId = UUID.randomUUID().toString(),
                    transactionNumber = generateTransactionNumber(now),
                    transactionStatus = TransactionStatus.DRAFT.name,
                    paymentStatus = PaymentStatus.UNPAID.name,
                    paymentMethod = null,
                    totalAmount = 0L,
                    amountReceived = null,
                    changeAmount = 0L,
                    createdAt = now,
                    completedAt = null,
                    cancelledAt = null,
                    createdBy = userId,
                    cancelledBy = null,
                    cancellationReason = null
                )
                transactionDao.insertTransaction(newTx)
                DraftCart(newTx.toDomain(), emptyList())
            }
        }
    }

    override suspend fun addItemToDraft(
        productId: String,
        quantity: Long,
        userId: String
    ): Result<DraftCart> {
        if (quantity <= 0) {
            return Result.Error(AppError.Validation("Jumlah produk harus lebih dari 0."))
        }

        return try {
            database.withTransaction {
                val product = productDao.getProductById(productId)
                    ?: return@withTransaction Result.Error(AppError.NotFound("Produk tidak ditemukan."))

                if (!product.isActive) {
                    return@withTransaction Result.Error(AppError.Validation("Produk nonaktif tidak dapat ditambahkan ke transaksi."))
                }

                val isGram = product.quantityType == QuantityType.GRAM.name
                if (isGram && quantity % 500L != 0L) {
                    return@withTransaction Result.Error(AppError.Validation("Berat produk harus kelipatan 500 gram (0,5 kg)."))
                }

                val draft = getOrCreateActiveDraft(userId)
                val transactionId = draft.transaction.transactionId

                val existingItem = transactionItemDao.getItemByProductAndTransaction(transactionId, productId)
                if (existingItem != null) {
                    val newQty = existingItem.quantity + quantity
                    val newSubtotal = calculateSubtotal(newQty, existingItem.unitPrice, isGram)
                    transactionItemDao.updateItem(
                        existingItem.copy(
                            quantity = newQty,
                            subtotal = newSubtotal
                        )
                    )
                } else {
                    val unitPriceSnapshot = product.currentPrice
                    val subtotal = calculateSubtotal(quantity, unitPriceSnapshot, isGram)
                    val newItem = TransactionItemEntity(
                        itemId = UUID.randomUUID().toString(),
                        transactionId = transactionId,
                        productId = productId,
                        productNameSnapshot = product.name,
                        quantity = quantity,
                        unitPrice = unitPriceSnapshot,
                        subtotal = subtotal,
                        sellingUnit = product.sellingUnit
                    )
                    transactionItemDao.insertItem(newItem)
                }

                val allItems = transactionItemDao.getItemsForTransactionDirect(transactionId)
                val newTotal = allItems.sumOf { it.subtotal }
                transactionDao.updateTotalAmount(transactionId, newTotal)

                val updatedTx = transactionDao.getTransactionById(transactionId)!!
                Result.Success(
                    DraftCart(
                        transaction = updatedTx.toDomain(),
                        items = allItems.map { it.toDomain() }
                    )
                )
            }
        } catch (e: Exception) {
            Result.Error(AppError.Database("Gagal menambahkan produk ke keranjang: ${e.message}", e))
        }
    }

    override suspend fun updateDraftItemQuantity(
        itemId: String,
        newQuantity: Long
    ): Result<DraftCart> {
        if (newQuantity <= 0) {
            return removeDraftItem(itemId)
        }

        return try {
            database.withTransaction {
                val item = transactionItemDao.getItemById(itemId)
                    ?: return@withTransaction Result.Error(AppError.NotFound("Item keranjang tidak ditemukan."))

                val product = productDao.getProductById(item.productId)
                val isGram = product?.quantityType == QuantityType.GRAM.name

                if (isGram && newQuantity % 500L != 0L) {
                    return@withTransaction Result.Error(AppError.Validation("Berat produk harus kelipatan 500 gram (0,5 kg)."))
                }

                val newSubtotal = calculateSubtotal(newQuantity, item.unitPrice, isGram)
                transactionItemDao.updateItem(
                    item.copy(
                        quantity = newQuantity,
                        subtotal = newSubtotal
                    )
                )

                val allItems = transactionItemDao.getItemsForTransactionDirect(item.transactionId)
                val newTotal = allItems.sumOf { it.subtotal }
                transactionDao.updateTotalAmount(item.transactionId, newTotal)

                val updatedTx = transactionDao.getTransactionById(item.transactionId)!!
                Result.Success(
                    DraftCart(
                        transaction = updatedTx.toDomain(),
                        items = allItems.map { it.toDomain() }
                    )
                )
            }
        } catch (e: Exception) {
            Result.Error(AppError.Database("Gagal mengubah jumlah item: ${e.message}", e))
        }
    }

    override suspend fun removeDraftItem(itemId: String): Result<DraftCart> {
        return try {
            database.withTransaction {
                val item = transactionItemDao.getItemById(itemId)
                    ?: return@withTransaction Result.Error(AppError.NotFound("Item keranjang tidak ditemukan."))

                transactionItemDao.deleteItem(itemId)

                val allItems = transactionItemDao.getItemsForTransactionDirect(item.transactionId)
                val newTotal = allItems.sumOf { it.subtotal }
                transactionDao.updateTotalAmount(item.transactionId, newTotal)

                val updatedTx = transactionDao.getTransactionById(item.transactionId)!!
                Result.Success(
                    DraftCart(
                        transaction = updatedTx.toDomain(),
                        items = allItems.map { it.toDomain() }
                    )
                )
            }
        } catch (e: Exception) {
            Result.Error(AppError.Database("Gagal menghapus item dari keranjang: ${e.message}", e))
        }
    }

    override suspend fun clearDraft(transactionId: String): Result<Unit> {
        return try {
            database.withTransaction {
                transactionItemDao.deleteItemsForTransaction(transactionId)
                transactionDao.updateTotalAmount(transactionId, 0L)
                Result.Success(Unit)
            }
        } catch (e: Exception) {
            Result.Error(AppError.Database("Gagal mengosongkan keranjang: ${e.message}", e))
        }
    }

    override suspend fun getTransactionById(transactionId: String): Transaction? {
        return transactionDao.getTransactionById(transactionId)?.toDomain()
    }

    override suspend fun getItemsForTransaction(transactionId: String): List<TransactionItem> {
        return transactionItemDao.getItemsForTransactionDirect(transactionId).map { it.toDomain() }
    }

    override suspend fun completeCashTransaction(
        transactionId: String,
        amountReceived: Long,
        userId: String
    ): Result<Transaction> {
        return completeTransactionInternal(
            transactionId = transactionId,
            paymentMethod = PaymentMethod.CASH,
            amountReceived = amountReceived,
            userId = userId
        )
    }

    override suspend fun completeQrisTransaction(
        transactionId: String,
        userId: String
    ): Result<Transaction> {
        return completeTransactionInternal(
            transactionId = transactionId,
            paymentMethod = PaymentMethod.QRIS,
            amountReceived = null,
            userId = userId
        )
    }

    private suspend fun completeTransactionInternal(
        transactionId: String,
        paymentMethod: PaymentMethod,
        amountReceived: Long?,
        userId: String
    ): Result<Transaction> {
        return try {
            val res = database.withTransaction {
                // 1. Load active transaction
                val txEntity = transactionDao.getTransactionById(transactionId)
                    ?: return@withTransaction Result.Error(AppError.NotFound("Transaksi tidak ditemukan."))

                if (txEntity.transactionStatus != TransactionStatus.DRAFT.name) {
                    return@withTransaction Result.Error(AppError.Validation("Hanya transaksi DRAFT yang dapat diselesaikan."))
                }

                // 2. Load and validate items
                val items = transactionItemDao.getItemsForTransactionDirect(transactionId)
                if (items.isEmpty()) {
                    return@withTransaction Result.Error(AppError.Validation("Keranjang transaksi kosong. Tidak dapat menyelesaikan transaksi."))
                }

                // 3. Validate products still exist, are active, and validate stock for physical products
                val now = System.currentTimeMillis()
                val updatedItems = mutableListOf<TransactionItemEntity>()
                val stockMovementsToInsert = mutableListOf<StockMovementEntity>()

                for (item in items) {
                    val product = productDao.getProductById(item.productId)
                        ?: return@withTransaction Result.Error(AppError.NotFound("Produk ${item.productNameSnapshot} tidak ditemukan di database."))

                    if (!product.isActive) {
                        return@withTransaction Result.Error(AppError.Validation("Produk ${product.name} saat ini nonaktif. Hapus dari keranjang terlebih dahulu."))
                    }

                    val isGram = product.quantityType == QuantityType.GRAM.name
                    // Recalculate item subtotal using snapshot price
                    val recalculatedSubtotal = calculateSubtotal(item.quantity, item.unitPrice, isGram)
                    updatedItems.add(item.copy(subtotal = recalculatedSubtotal))

                    // Stock validation for physical products
                    if (product.productKind == ProductKind.PHYSICAL.name) {
                        val currentStock = stockMovementDao.getCurrentStock(product.productId)
                        if (item.quantity > currentStock) {
                            val unitStr = if (isGram) "gram" else product.stockUnit
                            return@withTransaction Result.Error(
                                AppError.Validation(
                                    "Stok tidak mencukupi untuk ${product.name}. Tersedia: $currentStock $unitStr, diminta: ${item.quantity} $unitStr."
                                )
                            )
                        }

                        // Prepare SALE stock movement (negative delta)
                        stockMovementsToInsert.add(
                            StockMovementEntity(
                                movementId = UUID.randomUUID().toString(),
                                productId = product.productId,
                                movementType = MovementType.SALE.name,
                                quantityDelta = -item.quantity,
                                referenceType = ReferenceType.TRANSACTION.name,
                                referenceId = transactionId,
                                reason = "Penjualan transaksi ${txEntity.transactionNumber}",
                                createdAt = now,
                                createdBy = userId
                            )
                        )
                    }
                }

                // 4. Recalculate transaction total
                val totalAmount = updatedItems.sumOf { it.subtotal }
                if (totalAmount < 0) {
                    return@withTransaction Result.Error(AppError.Validation("Total transaksi tidak valid."))
                }

                // 5. Payment validation & change calculation
                val finalAmountReceived: Long
                val finalChangeAmount: Long

                when (paymentMethod) {
                    PaymentMethod.CASH -> {
                        if (amountReceived == null || amountReceived < totalAmount) {
                            val shortage = if (amountReceived != null) totalAmount - amountReceived else totalAmount
                            return@withTransaction Result.Error(
                                AppError.Validation("Uang diterima kurang dari total transaksi. Kurang: Rp$shortage")
                            )
                        }
                        finalAmountReceived = amountReceived
                        finalChangeAmount = amountReceived - totalAmount
                    }
                    PaymentMethod.QRIS -> {
                        finalAmountReceived = totalAmount
                        finalChangeAmount = 0L
                    }
                }

                // 6. Update Transaction Entity
                val completedTx = txEntity.copy(
                    transactionStatus = TransactionStatus.COMPLETED.name,
                    paymentStatus = PaymentStatus.PAID.name,
                    paymentMethod = paymentMethod.name,
                    totalAmount = totalAmount,
                    amountReceived = finalAmountReceived,
                    changeAmount = finalChangeAmount,
                    completedAt = now
                )
                transactionDao.updateTransaction(completedTx)

                // 7. Update all items (in case of recalculated subtotals)
                for (item in updatedItems) {
                    transactionItemDao.updateItem(item)
                }

                // 8. Insert Stock Movements for physical items
                if (stockMovementsToInsert.isNotEmpty()) {
                    stockMovementDao.insertStockMovements(stockMovementsToInsert)
                }

                // 9. Atomically enqueue SyncQueue items
                val syncQueueItems = mutableListOf<SyncQueueEntity>()
                // Enqueue completed Transaction
                val completedTxDomain = completedTx.toDomain()
                syncQueueItems.add(
                    SyncQueueEntity(
                        queueId = UUID.randomUUID().toString(),
                        entityType = SyncEntityType.TRANSACTION.name,
                        entityId = completedTx.transactionId,
                        operation = SyncOperation.CREATE.name,
                        payload = SyncJsonMapper.transactionToJson(completedTxDomain),
                        status = SyncStatus.PENDING.name,
                        createdAt = now
                    )
                )

                // Enqueue Transaction Items
                for (item in updatedItems) {
                    val itemDomain = item.toDomain()
                    syncQueueItems.add(
                        SyncQueueEntity(
                            queueId = UUID.randomUUID().toString(),
                            entityType = SyncEntityType.TRANSACTION_ITEM.name,
                            entityId = item.itemId,
                            operation = SyncOperation.CREATE.name,
                            payload = SyncJsonMapper.transactionItemToJson(itemDomain),
                            status = SyncStatus.PENDING.name,
                            createdAt = now
                        )
                    )
                }

                // Enqueue Stock Movements
                for (mov in stockMovementsToInsert) {
                    val movDomain = mov.toDomain()
                    syncQueueItems.add(
                        SyncQueueEntity(
                            queueId = UUID.randomUUID().toString(),
                            entityType = SyncEntityType.STOCK_MOVEMENT.name,
                            entityId = mov.movementId,
                            operation = SyncOperation.CREATE.name,
                            payload = SyncJsonMapper.stockMovementToJson(movDomain),
                            status = SyncStatus.PENDING.name,
                            createdAt = now
                        )
                    )
                }

                syncQueueDao.insertSyncQueueItems(syncQueueItems)

                Result.Success(completedTxDomain)
            }
            if (res is Result.Success) {
                syncManager.scheduleOneTimeSync()
            }
            res
        } catch (e: Exception) {
            Result.Error(AppError.Database("Gagal menyelesaikan transaksi: ${e.message}", e))
        }
    }

    override fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllCompletedAndCancelledTransactions().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun cancelTransaction(
        transactionId: String,
        reason: String,
        userId: String
    ): Result<Transaction> {
        if (reason.isBlank()) {
            return Result.Error(AppError.Validation("Alasan pembatalan wajib diisi."))
        }

        return try {
            val res = database.withTransaction {
                val txEntity = transactionDao.getTransactionById(transactionId)
                    ?: return@withTransaction Result.Error(AppError.NotFound("Transaksi tidak ditemukan."))

                if (txEntity.transactionStatus == TransactionStatus.CANCELLED.name) {
                    return@withTransaction Result.Error(AppError.Validation("Transaksi sudah dibatalkan sebelumnya."))
                }

                if (txEntity.transactionStatus != TransactionStatus.COMPLETED.name) {
                    return@withTransaction Result.Error(AppError.Validation("Hanya transaksi selesai (COMPLETED) yang dapat dibatalkan."))
                }

                val items = transactionItemDao.getItemsForTransactionDirect(transactionId)
                val now = System.currentTimeMillis()
                val stockReversalsToInsert = mutableListOf<StockMovementEntity>()

                for (item in items) {
                    val product = productDao.getProductById(item.productId)
                    if (product != null && product.productKind == ProductKind.PHYSICAL.name) {
                        stockReversalsToInsert.add(
                            StockMovementEntity(
                                movementId = UUID.randomUUID().toString(),
                                productId = product.productId,
                                movementType = MovementType.SALE_REVERSAL.name,
                                quantityDelta = item.quantity, // positive delta to restore stock
                                referenceType = ReferenceType.TRANSACTION.name,
                                referenceId = transactionId,
                                reason = "Pembatalan transaksi ${txEntity.transactionNumber}: $reason",
                                createdAt = now,
                                createdBy = userId
                            )
                        )
                    }
                }

                val cancelledTx = txEntity.copy(
                    transactionStatus = TransactionStatus.CANCELLED.name,
                    cancelledAt = now,
                    cancelledBy = userId,
                    cancellationReason = reason
                )
                transactionDao.updateTransaction(cancelledTx)

                if (stockReversalsToInsert.isNotEmpty()) {
                    stockMovementDao.insertStockMovements(stockReversalsToInsert)
                }

                // Enqueue sync queue items atomically
                val syncQueueItems = mutableListOf<SyncQueueEntity>()
                val cancelledTxDomain = cancelledTx.toDomain()
                syncQueueItems.add(
                    SyncQueueEntity(
                        queueId = UUID.randomUUID().toString(),
                        entityType = SyncEntityType.TRANSACTION.name,
                        entityId = cancelledTx.transactionId,
                        operation = SyncOperation.CANCEL.name,
                        payload = SyncJsonMapper.transactionToJson(cancelledTxDomain),
                        status = SyncStatus.PENDING.name,
                        createdAt = now
                    )
                )

                for (mov in stockReversalsToInsert) {
                    val movDomain = mov.toDomain()
                    syncQueueItems.add(
                        SyncQueueEntity(
                            queueId = UUID.randomUUID().toString(),
                            entityType = SyncEntityType.STOCK_MOVEMENT.name,
                            entityId = mov.movementId,
                            operation = SyncOperation.CREATE.name,
                            payload = SyncJsonMapper.stockMovementToJson(movDomain),
                            status = SyncStatus.PENDING.name,
                            createdAt = now
                        )
                    )
                }

                syncQueueDao.insertSyncQueueItems(syncQueueItems)

                Result.Success(cancelledTxDomain)
            }
            if (res is Result.Success) {
                syncManager.scheduleOneTimeSync()
            }
            res
        } catch (e: Exception) {
            Result.Error(AppError.Database("Gagal membatalkan transaksi: ${e.message}", e))
        }
    }

    private fun calculateSubtotal(quantity: Long, unitPrice: Long, isGram: Boolean): Long {
        return if (isGram) {
            (quantity * unitPrice) / 1000L
        } else {
            quantity * unitPrice
        }
    }

    private fun generateTransactionNumber(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
        val datePart = dateFormat.format(Date(timestamp))
        val randomPart = (100..999).random()
        return "TRX-$datePart-$randomPart"
    }
}
