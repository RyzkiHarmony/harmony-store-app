package com.harmony.tokoharmony.domain.model

data class SyncItemResult(
    val queueId: String,
    val status: String, // "SUCCESS" or "FAILED"
    val retryable: Boolean = true,
    val error: String? = null
)

data class SyncBatchResult(
    val totalProcessed: Int,
    val successCount: Int,
    val failureCount: Int,
    val itemResults: List<SyncItemResult> = emptyList(),
    val isNetworkError: Boolean = false,
    val errorMessage: String? = null
)

data class SyncStatusInfo(
    val pendingCount: Int,
    val failedCount: Int,
    val lastSyncTime: Long?
)
