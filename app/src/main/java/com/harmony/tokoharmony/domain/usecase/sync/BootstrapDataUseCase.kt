package com.harmony.tokoharmony.domain.usecase.sync

import com.harmony.tokoharmony.domain.model.BootstrapData
import com.harmony.tokoharmony.domain.repository.SyncRepository
import javax.inject.Inject

class BootstrapDataUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(): Result<BootstrapData> {
        val fetchResult = syncRepository.fetchBootstrapData()
        if (fetchResult.isFailure) {
            return fetchResult
        }

        val data = fetchResult.getOrThrow()
        val restoreResult = syncRepository.restoreBootstrapData(data)
        if (restoreResult.isFailure) {
            return Result.failure(restoreResult.exceptionOrNull() ?: Exception("Gagal memulihkan data lokal dari snapshot"))
        }

        return Result.success(data)
    }
}
