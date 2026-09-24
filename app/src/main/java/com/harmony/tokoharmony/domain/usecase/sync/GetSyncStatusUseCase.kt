package com.harmony.tokoharmony.domain.usecase.sync

import com.harmony.tokoharmony.domain.model.SyncStatusInfo
import com.harmony.tokoharmony.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetSyncStatusUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    operator fun invoke(): Flow<SyncStatusInfo> {
        return combine(
            syncRepository.getPendingCount(),
            syncRepository.getFailedCount(),
            syncRepository.getLastSyncTime()
        ) { pending, failed, lastTime ->
            SyncStatusInfo(
                pendingCount = pending,
                failedCount = failed,
                lastSyncTime = lastTime
            )
        }
    }
}
