package com.harmony.tokoharmony.domain.usecase.sync

import com.harmony.tokoharmony.domain.model.BootstrapData
import com.harmony.tokoharmony.domain.model.SyncBatchResult
import com.harmony.tokoharmony.domain.model.SyncEntityType
import com.harmony.tokoharmony.domain.model.SyncItemResult
import com.harmony.tokoharmony.domain.model.SyncOperation
import com.harmony.tokoharmony.domain.model.SyncQueueItem
import com.harmony.tokoharmony.domain.model.SyncStatus
import com.harmony.tokoharmony.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyncPendingDataUseCaseTest {

    private lateinit var fakeSyncRepository: FakeSyncRepository
    private lateinit var syncPendingDataUseCase: SyncPendingDataUseCase

    @Before
    fun setUp() {
        fakeSyncRepository = FakeSyncRepository()
        syncPendingDataUseCase = SyncPendingDataUseCase(fakeSyncRepository)
    }

    @Test
    fun invoke_resetsStaleSyncing_beforeSyncingBatch() = runTest {
        // Prepare a stale syncing item
        val staleItem = SyncQueueItem(
            queueId = "q-stale",
            entityType = SyncEntityType.PRODUCT,
            entityId = "prod-1",
            operation = SyncOperation.CREATE,
            payload = "{}",
            status = SyncStatus.SYNCING,
            createdAt = 1000L
        )
        fakeSyncRepository.items["q-stale"] = staleItem

        val result = syncPendingDataUseCase(batchSize = 10)

        // Verify stale syncing was reset
        assertTrue(fakeSyncRepository.resetStaleSyncingCalled)
        assertEquals(SyncStatus.SYNCED, fakeSyncRepository.items["q-stale"]?.status)
    }

    @Test
    fun invoke_whenNetworkError_returnsNetworkErrorResult() = runTest {
        fakeSyncRepository.shouldFailWithNetworkError = true
        fakeSyncRepository.items["q-1"] = SyncQueueItem(
            queueId = "q-1",
            entityType = SyncEntityType.TRANSACTION,
            entityId = "tx-1",
            operation = SyncOperation.CREATE,
            payload = "{}",
            status = SyncStatus.PENDING,
            createdAt = 1000L
        )

        val result = syncPendingDataUseCase(batchSize = 10)

        assertTrue(result.isNetworkError)
        assertEquals(1, result.failureCount)
        assertEquals(0, result.successCount)
        // Item should remain PENDING with incremented retry
        assertEquals(SyncStatus.PENDING, fakeSyncRepository.items["q-1"]?.status)
        assertEquals(1, fakeSyncRepository.items["q-1"]?.retryCount)
    }

    @Test
    fun invoke_whenMixedBatch_processesSuccessAndFailureCorrectly() = runTest {
        fakeSyncRepository.items["q-1"] = SyncQueueItem(
            queueId = "q-1",
            entityType = SyncEntityType.PRODUCT,
            entityId = "prod-1",
            operation = SyncOperation.CREATE,
            payload = "{}",
            status = SyncStatus.PENDING,
            createdAt = 1000L
        )
        fakeSyncRepository.items["q-2"] = SyncQueueItem(
            queueId = "q-2",
            entityType = SyncEntityType.PRODUCT,
            entityId = "prod-invalid",
            operation = SyncOperation.CREATE,
            payload = "{}",
            status = SyncStatus.PENDING,
            createdAt = 2000L
        )

        fakeSyncRepository.customItemResults["q-2"] = SyncItemResult(
            queueId = "q-2",
            status = "FAILED",
            retryable = false,
            error = "Invalid payload schema"
        )

        val result = syncPendingDataUseCase(batchSize = 10)

        assertFalse(result.isNetworkError)
        assertEquals(1, result.successCount)
        assertEquals(1, result.failureCount)

        assertEquals(SyncStatus.SYNCED, fakeSyncRepository.items["q-1"]?.status)
        assertEquals(SyncStatus.FAILED, fakeSyncRepository.items["q-2"]?.status)
    }
}

open class FakeSyncRepository : SyncRepository {
    val items = mutableMapOf<String, SyncQueueItem>()
    var resetStaleSyncingCalled = false
    var shouldFailWithNetworkError = false
    val customItemResults = mutableMapOf<String, SyncItemResult>()
    var restoredBootstrapData: BootstrapData? = null
    var bootstrapDataToReturn: Result<BootstrapData> = Result.success(BootstrapData())

    private val pendingCountFlow = MutableStateFlow(0)
    private val failedCountFlow = MutableStateFlow(0)
    private val lastSyncTimeFlow = MutableStateFlow<Long?>(null)

    override suspend fun enqueue(item: SyncQueueItem): Result<Unit> {
        items[item.queueId] = item
        return Result.success(Unit)
    }

    override suspend fun enqueueAll(items: List<SyncQueueItem>): Result<Unit> {
        items.forEach { this.items[it.queueId] = it }
        return Result.success(Unit)
    }

    override suspend fun getPendingItems(limit: Int): List<SyncQueueItem> {
        return items.values.filter { it.status == SyncStatus.PENDING }.sortedBy { it.createdAt }.take(limit)
    }

    override fun getPendingCount(): Flow<Int> = pendingCountFlow.asStateFlow()

    override fun getFailedCount(): Flow<Int> = failedCountFlow.asStateFlow()

    override fun getLastSyncTime(): Flow<Long?> = lastSyncTimeFlow.asStateFlow()

    override fun getAllQueueItems(): Flow<List<SyncQueueItem>> = MutableStateFlow(items.values.toList())

    override suspend fun resetStaleSyncing(staleThresholdMs: Long): Int {
        resetStaleSyncingCalled = true
        var count = 0
        items.values.forEach { item ->
            if (item.status == SyncStatus.SYNCING) {
                items[item.queueId] = item.copy(status = SyncStatus.PENDING)
                count++
            }
        }
        return count
    }

    override suspend fun syncPendingBatch(limit: Int): SyncBatchResult {
        val pending = getPendingItems(limit)
        if (pending.isEmpty()) {
            return SyncBatchResult(0, 0, 0)
        }

        if (shouldFailWithNetworkError) {
            pending.forEach {
                items[it.queueId] = it.copy(
                    retryCount = it.retryCount + 1,
                    lastError = "Network connection timeout",
                    status = SyncStatus.PENDING
                )
            }
            return SyncBatchResult(
                totalProcessed = pending.size,
                successCount = 0,
                failureCount = pending.size,
                isNetworkError = true,
                errorMessage = "Network connection timeout"
            )
        }

        var success = 0
        var failure = 0
        val results = mutableListOf<SyncItemResult>()

        pending.forEach { item ->
            val custom = customItemResults[item.queueId]
            if (custom != null) {
                if (custom.status == "SUCCESS") {
                    items[item.queueId] = item.copy(status = SyncStatus.SYNCED, syncedAt = System.currentTimeMillis())
                    success++
                } else {
                    val nextStatus = if (custom.retryable) SyncStatus.PENDING else SyncStatus.FAILED
                    items[item.queueId] = item.copy(
                        status = nextStatus,
                        retryCount = item.retryCount + 1,
                        lastError = custom.error
                    )
                    failure++
                }
                results.add(custom)
            } else {
                items[item.queueId] = item.copy(status = SyncStatus.SYNCED, syncedAt = System.currentTimeMillis())
                success++
                results.add(SyncItemResult(item.queueId, "SUCCESS"))
            }
        }

        return SyncBatchResult(
            totalProcessed = pending.size,
            successCount = success,
            failureCount = failure,
            itemResults = results
        )
    }

    override suspend fun fetchBootstrapData(): Result<BootstrapData> {
        return bootstrapDataToReturn
    }

    override suspend fun restoreBootstrapData(data: BootstrapData): Result<Unit> {
        restoredBootstrapData = data
        return Result.success(Unit)
    }
}
