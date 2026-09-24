package com.harmony.tokoharmony.data.repository

import com.harmony.tokoharmony.core.database.dao.CategoryDao
import com.harmony.tokoharmony.core.database.dao.SyncQueueDao
import com.harmony.tokoharmony.core.database.entity.SyncQueueEntity
import com.harmony.tokoharmony.core.network.SyncJsonMapper
import com.harmony.tokoharmony.core.sync.SyncManager
import com.harmony.tokoharmony.data.local.mapper.toDomain
import com.harmony.tokoharmony.data.local.mapper.toEntity
import com.harmony.tokoharmony.domain.model.Category
import com.harmony.tokoharmony.domain.model.SyncEntityType
import com.harmony.tokoharmony.domain.model.SyncOperation
import com.harmony.tokoharmony.domain.model.SyncStatus
import com.harmony.tokoharmony.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
    private val syncQueueDao: SyncQueueDao,
    private val syncManager: SyncManager
) : CategoryRepository {

    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { list -> list.map { it.toDomain() } }
    }

    override fun getActiveCategories(): Flow<List<Category>> {
        return categoryDao.getActiveCategories().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getCategoryById(categoryId: String): Category? {
        return categoryDao.getCategoryById(categoryId)?.toDomain()
    }

    override suspend fun saveCategory(category: Category) {
        categoryDao.insertCategory(category.toEntity())
        syncQueueDao.insertSyncQueueItem(
            SyncQueueEntity(
                queueId = UUID.randomUUID().toString(),
                entityType = SyncEntityType.CATEGORY.name,
                entityId = category.categoryId,
                operation = SyncOperation.CREATE.name,
                payload = SyncJsonMapper.categoryToJson(category),
                status = SyncStatus.PENDING.name,
                createdAt = System.currentTimeMillis()
            )
        )
        syncManager.scheduleOneTimeSync()
    }

    override suspend fun saveCategories(categories: List<Category>) {
        categoryDao.insertCategories(categories.map { it.toEntity() })
        val items = categories.map { cat ->
            SyncQueueEntity(
                queueId = UUID.randomUUID().toString(),
                entityType = SyncEntityType.CATEGORY.name,
                entityId = cat.categoryId,
                operation = SyncOperation.CREATE.name,
                payload = SyncJsonMapper.categoryToJson(cat),
                status = SyncStatus.PENDING.name,
                createdAt = System.currentTimeMillis()
            )
        }
        syncQueueDao.insertSyncQueueItems(items)
        syncManager.scheduleOneTimeSync()
    }

    override suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category.toEntity())
        syncQueueDao.insertSyncQueueItem(
            SyncQueueEntity(
                queueId = UUID.randomUUID().toString(),
                entityType = SyncEntityType.CATEGORY.name,
                entityId = category.categoryId,
                operation = SyncOperation.UPDATE.name,
                payload = SyncJsonMapper.categoryToJson(category),
                status = SyncStatus.PENDING.name,
                createdAt = System.currentTimeMillis()
            )
        )
        syncManager.scheduleOneTimeSync()
    }
}
