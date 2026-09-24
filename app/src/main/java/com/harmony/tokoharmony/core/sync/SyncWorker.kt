package com.harmony.tokoharmony.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.harmony.tokoharmony.domain.usecase.sync.SyncPendingDataUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncPendingDataUseCase: SyncPendingDataUseCase
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val result = syncPendingDataUseCase(batchSize = 50)
            if (result.isNetworkError) {
                Result.retry()
            } else if (result.failureCount > 0 && result.successCount == 0) {
                // If every item in the batch failed, retry with backoff
                Result.retry()
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
