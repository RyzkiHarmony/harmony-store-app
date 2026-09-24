package com.harmony.tokoharmony.data.repository

import androidx.room.withTransaction
import com.harmony.tokoharmony.core.common.AppError
import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.core.database.AppDatabase
import com.harmony.tokoharmony.core.database.dao.StockAdjustmentDao
import com.harmony.tokoharmony.core.database.dao.StockInDao
import com.harmony.tokoharmony.core.database.dao.StockMovementDao
import com.harmony.tokoharmony.core.database.dao.SyncQueueDao
import com.harmony.tokoharmony.core.database.entity.StockAdjustmentEntity
import com.harmony.tokoharmony.core.database.entity.StockInEntity
import com.harmony.tokoharmony.core.database.entity.StockMovementEntity
import com.harmony.tokoharmony.core.database.entity.SyncQueueEntity
import com.harmony.tokoharmony.core.network.SyncJsonMapper
import com.harmony.tokoharmony.core.sync.SyncManager
import com.harmony.tokoharmony.data.local.mapper.toDomain
import com.harmony.tokoharmony.data.local.mapper.toEntity
import com.harmony.tokoharmony.domain.model.MovementType
import com.harmony.tokoharmony.domain.model.ReferenceType
import com.harmony.tokoharmony.domain.model.StockAdjustment
import com.harmony.tokoharmony.domain.model.StockIn
import com.harmony.tokoharmony.domain.model.StockMovement
import com.harmony.tokoharmony.domain.model.SyncEntityType
import com.harmony.tokoharmony.domain.model.SyncOperation
import com.harmony.tokoharmony.domain.model.SyncStatus
import com.harmony.tokoharmony.domain.repository.StockRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StockRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val stockMovementDao: StockMovementDao,
    private val stockInDao: StockInDao,
    private val stockAdjustmentDao: StockAdjustmentDao,
    private val syncQueueDao: SyncQueueDao,
    private val syncManager: SyncManager
) : StockRepository {

    override suspend fun getCurrentStock(productId: String): Long {
        return stockMovementDao.getCurrentStock(productId)
    }

    override suspend fun insertStockMovement(movement: StockMovement): Result<Unit> {
        return try {
            stockMovementDao.insertStockMovement(movement.toEntity())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.Database("Gagal menyimpan mutasi stok: ${e.message}", e))
        }
    }

    override suspend fun insertStockMovements(movements: List<StockMovement>): Result<Unit> {
        return try {
            stockMovementDao.insertStockMovements(movements.map { it.toEntity() })
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.Database("Gagal menyimpan mutasi stok: ${e.message}", e))
        }
    }

    override fun getMovementsForProduct(productId: String): Flow<List<StockMovement>> {
        return stockMovementDao.getMovementsForProduct(productId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getAllStockMovements(): Flow<List<StockMovement>> {
        return stockMovementDao.getAllMovements().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun hasInitialStock(productId: String): Boolean {
        return stockMovementDao.hasInitialStock(productId)
    }

    override suspend fun recordInitialStock(
        productId: String,
        initialStock: Long,
        userId: String
    ): Result<Unit> {
        return try {
            val res = database.withTransaction {
                if (stockMovementDao.hasInitialStock(productId)) {
                    return@withTransaction Result.Error(
                        AppError.Validation("Stok awal untuk produk ini sudah pernah ditetapkan.")
                    )
                }

                val now = System.currentTimeMillis()
                val movement = StockMovementEntity(
                    movementId = UUID.randomUUID().toString(),
                    productId = productId,
                    movementType = MovementType.INITIAL_STOCK.name,
                    quantityDelta = initialStock,
                    referenceType = null,
                    referenceId = null,
                    reason = "Penetapan stok awal",
                    createdAt = now,
                    createdBy = userId
                )
                stockMovementDao.insertStockMovement(movement)

                // Enqueue Stock Movement
                syncQueueDao.insertSyncQueueItem(
                    SyncQueueEntity(
                        queueId = UUID.randomUUID().toString(),
                        entityType = SyncEntityType.STOCK_MOVEMENT.name,
                        entityId = movement.movementId,
                        operation = SyncOperation.CREATE.name,
                        payload = SyncJsonMapper.stockMovementToJson(movement.toDomain()),
                        status = SyncStatus.PENDING.name,
                        createdAt = now
                    )
                )

                Result.Success(Unit)
            }
            if (res is Result.Success) {
                syncManager.scheduleOneTimeSync()
            }
            res
        } catch (e: Exception) {
            Result.Error(AppError.Database("Gagal menetapkan stok awal: ${e.message}", e))
        }
    }

    override suspend fun recordStockIn(
        productId: String,
        purchaseQuantity: Long,
        purchaseUnit: String,
        conversionFactor: Long,
        note: String?,
        userId: String
    ): Result<StockIn> {
        return try {
            val res = database.withTransaction {
                val stockQuantity = purchaseQuantity * conversionFactor
                val now = System.currentTimeMillis()
                val stockInId = UUID.randomUUID().toString()

                val stockInEntity = StockInEntity(
                    stockInId = stockInId,
                    productId = productId,
                    purchaseQuantity = purchaseQuantity,
                    purchaseUnit = purchaseUnit,
                    conversionFactor = conversionFactor,
                    stockQuantity = stockQuantity,
                    note = note,
                    createdAt = now,
                    createdBy = userId
                )
                stockInDao.insertStockIn(stockInEntity)

                val movementEntity = StockMovementEntity(
                    movementId = UUID.randomUUID().toString(),
                    productId = productId,
                    movementType = MovementType.STOCK_IN.name,
                    quantityDelta = stockQuantity,
                    referenceType = ReferenceType.STOCK_IN.name,
                    referenceId = stockInId,
                    reason = if (!note.isNullOrBlank()) "Barang masuk ($purchaseQuantity $purchaseUnit): $note" else "Barang masuk: $purchaseQuantity $purchaseUnit",
                    createdAt = now,
                    createdBy = userId
                )
                stockMovementDao.insertStockMovement(movementEntity)

                // Enqueue Stock In and Movement
                syncQueueDao.insertSyncQueueItem(
                    SyncQueueEntity(
                        queueId = UUID.randomUUID().toString(),
                        entityType = SyncEntityType.STOCK_IN.name,
                        entityId = stockInId,
                        operation = SyncOperation.CREATE.name,
                        payload = SyncJsonMapper.stockInToJson(stockInEntity.toDomain()),
                        status = SyncStatus.PENDING.name,
                        createdAt = now
                    )
                )

                syncQueueDao.insertSyncQueueItem(
                    SyncQueueEntity(
                        queueId = UUID.randomUUID().toString(),
                        entityType = SyncEntityType.STOCK_MOVEMENT.name,
                        entityId = movementEntity.movementId,
                        operation = SyncOperation.CREATE.name,
                        payload = SyncJsonMapper.stockMovementToJson(movementEntity.toDomain()),
                        status = SyncStatus.PENDING.name,
                        createdAt = now
                    )
                )

                Result.Success(stockInEntity.toDomain())
            }
            if (res is Result.Success) {
                syncManager.scheduleOneTimeSync()
            }
            res
        } catch (e: Exception) {
            Result.Error(AppError.Database("Gagal mencatat barang masuk: ${e.message}", e))
        }
    }

    override suspend fun recordStockAdjustment(
        productId: String,
        physicalQuantity: Long,
        reason: String,
        note: String?,
        userId: String
    ): Result<StockAdjustment> {
        return try {
            val res = database.withTransaction {
                val systemQuantity = stockMovementDao.getCurrentStock(productId)
                val difference = physicalQuantity - systemQuantity
                val now = System.currentTimeMillis()
                val adjustmentId = UUID.randomUUID().toString()

                val adjustmentEntity = StockAdjustmentEntity(
                    adjustmentId = adjustmentId,
                    productId = productId,
                    systemQuantity = systemQuantity,
                    physicalQuantity = physicalQuantity,
                    difference = difference,
                    reason = reason,
                    note = note,
                    createdAt = now,
                    createdBy = userId
                )
                stockAdjustmentDao.insertStockAdjustment(adjustmentEntity)

                val movementEntity = StockMovementEntity(
                    movementId = UUID.randomUUID().toString(),
                    productId = productId,
                    movementType = MovementType.ADJUSTMENT.name,
                    quantityDelta = difference,
                    referenceType = ReferenceType.STOCK_ADJUSTMENT.name,
                    referenceId = adjustmentId,
                    reason = if (!note.isNullOrBlank()) "Koreksi stok ($reason): $note" else "Koreksi stok ($reason)",
                    createdAt = now,
                    createdBy = userId
                )
                stockMovementDao.insertStockMovement(movementEntity)

                // Enqueue Stock Adjustment and Movement
                syncQueueDao.insertSyncQueueItem(
                    SyncQueueEntity(
                        queueId = UUID.randomUUID().toString(),
                        entityType = SyncEntityType.STOCK_ADJUSTMENT.name,
                        entityId = adjustmentId,
                        operation = SyncOperation.CREATE.name,
                        payload = SyncJsonMapper.stockAdjustmentToJson(adjustmentEntity.toDomain()),
                        status = SyncStatus.PENDING.name,
                        createdAt = now
                    )
                )

                syncQueueDao.insertSyncQueueItem(
                    SyncQueueEntity(
                        queueId = UUID.randomUUID().toString(),
                        entityType = SyncEntityType.STOCK_MOVEMENT.name,
                        entityId = movementEntity.movementId,
                        operation = SyncOperation.CREATE.name,
                        payload = SyncJsonMapper.stockMovementToJson(movementEntity.toDomain()),
                        status = SyncStatus.PENDING.name,
                        createdAt = now
                    )
                )

                Result.Success(adjustmentEntity.toDomain())
            }
            if (res is Result.Success) {
                syncManager.scheduleOneTimeSync()
            }
            res
        } catch (e: Exception) {
            Result.Error(AppError.Database("Gagal mencatat penyesuaian stok: ${e.message}", e))
        }
    }

    override fun getAllStockIns(): Flow<List<StockIn>> {
        return stockInDao.getAllStockIns().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getAllStockAdjustments(): Flow<List<StockAdjustment>> {
        return stockAdjustmentDao.getAllStockAdjustments().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getStockInsForProduct(productId: String): Flow<List<StockIn>> {
        return stockInDao.getStockInsForProduct(productId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getAdjustmentsForProduct(productId: String): Flow<List<StockAdjustment>> {
        return stockAdjustmentDao.getAdjustmentsForProduct(productId).map { list ->
            list.map { it.toDomain() }
        }
    }
}
