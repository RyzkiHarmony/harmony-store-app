package com.harmony.tokoharmony.domain.usecase.sync

import com.harmony.tokoharmony.domain.model.SyncEntityType
import com.harmony.tokoharmony.domain.model.SyncOperation
import com.harmony.tokoharmony.domain.model.SyncQueueItem
import com.harmony.tokoharmony.domain.model.SyncStatus
import com.harmony.tokoharmony.domain.repository.SyncRepository
import java.util.UUID
import javax.inject.Inject

class EnqueueSyncOperationUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(
        entityType: SyncEntityType,
        entityId: String,
        operation: SyncOperation,
        payload: String
    ): Result<Unit> {
        val queueItem = SyncQueueItem(
            queueId = UUID.randomUUID().toString(),
            entityType = entityType,
            entityId = entityId,
            operation = operation,
            payload = payload,
            status = SyncStatus.PENDING,
            retryCount = 0,
            lastError = null,
            createdAt = System.currentTimeMillis(),
            syncedAt = null
        )
        return syncRepository.enqueue(queueItem)
    }
}
