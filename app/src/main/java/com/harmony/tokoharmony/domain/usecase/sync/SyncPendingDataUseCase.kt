package com.harmony.tokoharmony.domain.usecase.sync

import com.harmony.tokoharmony.domain.model.SyncBatchResult
import com.harmony.tokoharmony.domain.repository.SyncRepository
import javax.inject.Inject

class SyncPendingDataUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(batchSize: Int = 50): SyncBatchResult {
        // Reset any stale SYNCING records first before grabbing batch
        syncRepository.resetStaleSyncing()
        return syncRepository.syncPendingBatch(limit = batchSize)
    }
}
