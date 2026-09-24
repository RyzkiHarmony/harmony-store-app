package com.harmony.tokoharmony.data.repository

import androidx.room.withTransaction
import com.harmony.tokoharmony.core.database.AppDatabase
import com.harmony.tokoharmony.core.database.dao.PriceHistoryDao
import com.harmony.tokoharmony.core.database.dao.ProductDao
import com.harmony.tokoharmony.core.database.dao.SyncQueueDao
import com.harmony.tokoharmony.core.database.entity.SyncQueueEntity
import com.harmony.tokoharmony.core.network.SyncJsonMapper
import com.harmony.tokoharmony.core.sync.SyncManager
import com.harmony.tokoharmony.data.local.mapper.toDomain
import com.harmony.tokoharmony.data.local.mapper.toEntity
import com.harmony.tokoharmony.domain.model.PriceHistory
import com.harmony.tokoharmony.domain.model.SyncEntityType
import com.harmony.tokoharmony.domain.model.SyncOperation
import com.harmony.tokoharmony.domain.model.SyncStatus
import com.harmony.tokoharmony.domain.repository.PriceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PriceRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val productDao: ProductDao,
    private val priceHistoryDao: PriceHistoryDao,
    private val syncQueueDao: SyncQueueDao,
    private val syncManager: SyncManager
) : PriceRepository {

    override fun getPriceHistory(productId: String): Flow<List<PriceHistory>> {
        return priceHistoryDao.getPriceHistoryByProductId(productId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getPriceHistoryList(productId: String): List<PriceHistory> {
        return priceHistoryDao.getPriceHistoryListByProductId(productId).map { it.toDomain() }
    }

    override suspend fun recordPriceChange(priceHistory: PriceHistory, newPrice: Long) {
        database.withTransaction {
            priceHistoryDao.insertPriceHistory(priceHistory.toEntity())
            productDao.updateProductPrice(
                productId = priceHistory.productId,
                newPrice = newPrice,
                updatedAt = priceHistory.changedAt
            )

            // Enqueue Price History
            syncQueueDao.insertSyncQueueItem(
                SyncQueueEntity(
                    queueId = UUID.randomUUID().toString(),
                    entityType = SyncEntityType.PRICE_HISTORY.name,
                    entityId = priceHistory.historyId,
                    operation = SyncOperation.CREATE.name,
                    payload = SyncJsonMapper.priceHistoryToJson(priceHistory),
                    status = SyncStatus.PENDING.name,
                    createdAt = priceHistory.changedAt
                )
            )

            // Enqueue updated Product
            val updatedProduct = productDao.getProductById(priceHistory.productId)?.toDomain()
            if (updatedProduct != null) {
                syncQueueDao.insertSyncQueueItem(
                    SyncQueueEntity(
                        queueId = UUID.randomUUID().toString(),
                        entityType = SyncEntityType.PRODUCT.name,
                        entityId = updatedProduct.productId,
                        operation = SyncOperation.UPDATE.name,
                        payload = SyncJsonMapper.productToJson(updatedProduct),
                        status = SyncStatus.PENDING.name,
                        createdAt = priceHistory.changedAt
                    )
                )
            }
        }
        syncManager.scheduleOneTimeSync()
    }
}
