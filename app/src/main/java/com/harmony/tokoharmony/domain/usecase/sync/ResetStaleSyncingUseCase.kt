package com.harmony.tokoharmony.domain.usecase.sync

import com.harmony.tokoharmony.domain.repository.SyncRepository
import javax.inject.Inject

class ResetStaleSyncingUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(staleThresholdMs: Long = 5 * 60 * 1000L): Int {
        return syncRepository.resetStaleSyncing(staleThresholdMs)
    }
}
