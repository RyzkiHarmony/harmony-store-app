package com.harmony.tokoharmony.data.local.mapper

import com.harmony.tokoharmony.core.database.entity.SyncQueueEntity
import com.harmony.tokoharmony.domain.model.SyncEntityType
import com.harmony.tokoharmony.domain.model.SyncOperation
import com.harmony.tokoharmony.domain.model.SyncQueueItem
import com.harmony.tokoharmony.domain.model.SyncStatus

fun SyncQueueEntity.toDomain(): SyncQueueItem {
    return SyncQueueItem(
        queueId = queueId,
        entityType = SyncEntityType.fromCode(entityType),
        entityId = entityId,
        operation = SyncOperation.valueOf(operation),
        payload = payload,
        status = SyncStatus.valueOf(status),
        retryCount = retryCount,
        lastError = lastError,
        createdAt = createdAt,
        syncedAt = syncedAt
    )
}

fun SyncQueueItem.toEntity(): SyncQueueEntity {
    return SyncQueueEntity(
        queueId = queueId,
        entityType = entityType.code,
        entityId = entityId,
        operation = operation.name,
        payload = payload,
        status = status.name,
        retryCount = retryCount,
        lastError = lastError,
        createdAt = createdAt,
        syncedAt = syncedAt
    )
}
