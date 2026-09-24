package com.harmony.tokoharmony.domain.model

data class SyncQueueItem(
    val queueId: String,
    val entityType: SyncEntityType,
    val entityId: String,
    val operation: SyncOperation,
    val payload: String,
    val status: SyncStatus = SyncStatus.PENDING,
    val retryCount: Int = 0,
    val lastError: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null
)
