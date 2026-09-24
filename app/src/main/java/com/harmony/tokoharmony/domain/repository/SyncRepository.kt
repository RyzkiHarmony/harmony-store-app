package com.harmony.tokoharmony.domain.repository

import com.harmony.tokoharmony.domain.model.BootstrapData
import com.harmony.tokoharmony.domain.model.SyncBatchResult
import com.harmony.tokoharmony.domain.model.SyncQueueItem
import kotlinx.coroutines.flow.Flow

interface SyncRepository {
    suspend fun enqueue(item: SyncQueueItem): Result<Unit>
    suspend fun enqueueAll(items: List<SyncQueueItem>): Result<Unit>
    suspend fun getPendingItems(limit: Int = 50): List<SyncQueueItem>
    fun getPendingCount(): Flow<Int>
    fun getFailedCount(): Flow<Int>
    fun getLastSyncTime(): Flow<Long?>
    suspend fun syncPendingBatch(limit: Int = 50): SyncBatchResult
    suspend fun resetStaleSyncing(staleThresholdMs: Long = 5 * 60 * 1000L): Int
    suspend fun fetchBootstrapData(): Result<BootstrapData>
    suspend fun restoreBootstrapData(data: BootstrapData): Result<Unit>
    fun getAllQueueItems(): Flow<List<SyncQueueItem>>
}
