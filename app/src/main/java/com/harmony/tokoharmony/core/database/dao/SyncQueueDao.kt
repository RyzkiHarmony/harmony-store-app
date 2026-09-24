package com.harmony.tokoharmony.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.harmony.tokoharmony.core.database.entity.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncQueueItem(item: SyncQueueEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncQueueItems(items: List<SyncQueueEntity>)

    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY created_at ASC LIMIT :limit")
    suspend fun getPendingItems(limit: Int = 50): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue WHERE queue_id = :queueId LIMIT 1")
    suspend fun getQueueItemById(queueId: String): SyncQueueEntity?

    @Query("UPDATE sync_queue SET status = 'SYNCING' WHERE queue_id IN (:queueIds)")
    suspend fun markSyncing(queueIds: List<String>)

    @Query("UPDATE sync_queue SET status = 'SYNCED', synced_at = :syncedAt, last_error = NULL WHERE queue_id = :queueId")
    suspend fun markSynced(queueId: String, syncedAt: Long)

    @Query("UPDATE sync_queue SET status = :status, retry_count = :retryCount, last_error = :error WHERE queue_id = :queueId")
    suspend fun markFailed(queueId: String, error: String, retryCount: Int, status: String)

    @Query("UPDATE sync_queue SET status = 'PENDING' WHERE status = 'SYNCING' AND created_at < :staleThreshold")
    suspend fun resetStaleSyncing(staleThreshold: Long): Int

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status IN ('PENDING', 'SYNCING')")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'FAILED'")
    fun getFailedCount(): Flow<Int>

    @Query("SELECT MAX(synced_at) FROM sync_queue WHERE status = 'SYNCED'")
    fun getLastSyncTime(): Flow<Long?>

    @Query("SELECT * FROM sync_queue ORDER BY created_at DESC")
    fun getAllQueueItems(): Flow<List<SyncQueueEntity>>

    @Query("DELETE FROM sync_queue WHERE status = 'SYNCED' AND synced_at < :beforeTimestamp")
    suspend fun deleteOldSyncedItems(beforeTimestamp: Long): Int
}
