package com.harmony.tokoharmony.data.repository

import com.harmony.tokoharmony.core.database.dao.ProductDao
import com.harmony.tokoharmony.core.database.dao.SyncQueueDao
import com.harmony.tokoharmony.core.database.entity.SyncQueueEntity
import com.harmony.tokoharmony.core.network.SyncJsonMapper
import com.harmony.tokoharmony.core.sync.SyncManager
import com.harmony.tokoharmony.data.local.mapper.toDomain
import com.harmony.tokoharmony.data.local.mapper.toEntity
import com.harmony.tokoharmony.domain.model.Product
import com.harmony.tokoharmony.domain.model.SyncEntityType
import com.harmony.tokoharmony.domain.model.SyncOperation
import com.harmony.tokoharmony.domain.model.SyncStatus
import com.harmony.tokoharmony.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepositoryImpl @Inject constructor(
    private val productDao: ProductDao,
    private val syncQueueDao: SyncQueueDao,
    private val syncManager: SyncManager
) : ProductRepository {

    override fun getAllProducts(): Flow<List<Product>> {
        return productDao.getAllProducts().map { list -> list.map { it.toDomain() } }
    }

    override fun getActiveProducts(): Flow<List<Product>> {
        return productDao.getActiveProducts().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getProductById(productId: String): Product? {
        return productDao.getProductById(productId)?.toDomain()
    }

    override suspend fun getProductByBarcode(barcode: String): Product? {
        return productDao.getProductByBarcode(barcode)?.toDomain()
    }

    override fun searchProducts(query: String, onlyActive: Boolean): Flow<List<Product>> {
        val flow = if (onlyActive) {
            productDao.searchActiveProducts(query)
        } else {
            productDao.searchProducts(query)
        }
        return flow.map { list -> list.map { it.toDomain() } }
    }

    override suspend fun saveProduct(product: Product) {
        productDao.insertProduct(product.toEntity())
        syncQueueDao.insertSyncQueueItem(
            SyncQueueEntity(
                queueId = UUID.randomUUID().toString(),
                entityType = SyncEntityType.PRODUCT.name,
                entityId = product.productId,
                operation = SyncOperation.CREATE.name,
                payload = SyncJsonMapper.productToJson(product),
                status = SyncStatus.PENDING.name,
                createdAt = System.currentTimeMillis()
            )
        )
        syncManager.scheduleOneTimeSync()
    }

    override suspend fun updateProduct(product: Product) {
        productDao.updateProduct(product.toEntity())
        syncQueueDao.insertSyncQueueItem(
            SyncQueueEntity(
                queueId = UUID.randomUUID().toString(),
                entityType = SyncEntityType.PRODUCT.name,
                entityId = product.productId,
                operation = SyncOperation.UPDATE.name,
                payload = SyncJsonMapper.productToJson(product),
                status = SyncStatus.PENDING.name,
                createdAt = System.currentTimeMillis()
            )
        )
        syncManager.scheduleOneTimeSync()
    }

    override suspend fun setProductActiveStatus(productId: String, isActive: Boolean) {
        val now = System.currentTimeMillis()
        productDao.setProductActiveStatus(productId, isActive, now)
        val updated = productDao.getProductById(productId)?.toDomain()
        if (updated != null) {
            syncQueueDao.insertSyncQueueItem(
                SyncQueueEntity(
                    queueId = UUID.randomUUID().toString(),
                    entityType = SyncEntityType.PRODUCT.name,
                    entityId = productId,
                    operation = SyncOperation.UPDATE.name,
                    payload = SyncJsonMapper.productToJson(updated),
                    status = SyncStatus.PENDING.name,
                    createdAt = now
                )
            )
            syncManager.scheduleOneTimeSync()
        }
    }
}
