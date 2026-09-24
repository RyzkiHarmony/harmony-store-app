package com.harmony.tokoharmony.data.repository

import androidx.room.withTransaction
import com.harmony.tokoharmony.core.common.AppError
import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.core.database.AppDatabase
import com.harmony.tokoharmony.core.database.dao.DigitalTransactionDao
import com.harmony.tokoharmony.core.database.dao.SyncQueueDao
import com.harmony.tokoharmony.core.database.entity.DigitalTransactionEntity
import com.harmony.tokoharmony.core.database.entity.SyncQueueEntity
import com.harmony.tokoharmony.core.network.SyncJsonMapper
import com.harmony.tokoharmony.domain.model.DigitalTransaction
import com.harmony.tokoharmony.domain.model.SyncEntityType
import com.harmony.tokoharmony.domain.model.SyncOperation
import com.harmony.tokoharmony.domain.model.SyncStatus
import com.harmony.tokoharmony.domain.repository.DigitalTransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DigitalTransactionRepositoryImpl @Inject constructor(
    private val appDatabase: AppDatabase,
    private val digitalTransactionDao: DigitalTransactionDao,
    private val syncQueueDao: SyncQueueDao
) : DigitalTransactionRepository {

    override suspend fun recordDigitalTransaction(digitalTransaction: DigitalTransaction): Result<Unit> {
        if (digitalTransaction.customerNumber.isBlank()) {
            return Result.Error(AppError.Validation("Nomor pelanggan/tujuan tidak boleh kosong."))
        }
        if (digitalTransaction.serviceType.isBlank()) {
            return Result.Error(AppError.Validation("Jenis layanan tidak boleh kosong."))
        }
        val validStatuses = listOf("PENDING", "SUCCESS", "FAILED")
        if (digitalTransaction.status !in validStatuses) {
            return Result.Error(AppError.Validation("Status transaksi digital tidak valid: ${digitalTransaction.status}"))
        }

        return try {
            appDatabase.withTransaction {
                val entity = DigitalTransactionEntity.fromDomain(digitalTransaction)
                digitalTransactionDao.insertDigitalTransaction(entity)

                val queueItem = SyncQueueEntity(
                    queueId = UUID.randomUUID().toString(),
                    entityType = SyncEntityType.DIGITAL_TRANSACTION.name,
                    entityId = digitalTransaction.digitalTransactionId,
                    operation = SyncOperation.CREATE.name,
                    payload = SyncJsonMapper.digitalTransactionToJson(digitalTransaction),
                    status = SyncStatus.PENDING.name,
                    retryCount = 0,
                    createdAt = System.currentTimeMillis()
                )
                syncQueueDao.insertSyncQueueItem(queueItem)

                Result.Success(Unit)
            }
        } catch (e: Exception) {
            Result.Error(AppError.Database("Gagal menyimpan transaksi digital: ${e.message}", e))
        }
    }

    override suspend fun getDigitalTransactionById(id: String): DigitalTransaction? {
        return digitalTransactionDao.getDigitalTransactionById(id)?.toDomain()
    }

    override suspend fun getDigitalTransactionByItemId(itemId: String): DigitalTransaction? {
        return digitalTransactionDao.getDigitalTransactionByItemId(itemId)?.toDomain()
    }

    override fun observeDigitalTransactionByItemId(itemId: String): Flow<DigitalTransaction?> {
        return digitalTransactionDao.observeDigitalTransactionByItemId(itemId).map { it?.toDomain() }
    }

    override suspend fun getDigitalTransactionsForTransaction(transactionId: String): List<DigitalTransaction> {
        return digitalTransactionDao.getDigitalTransactionsForTransaction(transactionId).map { it.toDomain() }
    }

    override fun observeDigitalTransactionsForTransaction(transactionId: String): Flow<List<DigitalTransaction>> {
        return digitalTransactionDao.observeDigitalTransactionsForTransaction(transactionId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun updateDigitalTransactionStatus(id: String, status: String): Result<Unit> {
        val validStatuses = listOf("PENDING", "SUCCESS", "FAILED")
        if (status !in validStatuses) {
            return Result.Error(AppError.Validation("Status transaksi digital tidak valid: $status"))
        }

        return try {
            appDatabase.withTransaction {
                val current = digitalTransactionDao.getDigitalTransactionById(id)
                    ?: return@withTransaction Result.Error(AppError.NotFound("Transaksi digital tidak ditemukan."))

                digitalTransactionDao.updateStatus(id, status)
                val updated = current.copy(status = status).toDomain()

                val queueItem = SyncQueueEntity(
                    queueId = UUID.randomUUID().toString(),
                    entityType = SyncEntityType.DIGITAL_TRANSACTION.name,
                    entityId = updated.digitalTransactionId,
                    operation = SyncOperation.UPDATE.name,
                    payload = SyncJsonMapper.digitalTransactionToJson(updated),
                    status = SyncStatus.PENDING.name,
                    retryCount = 0,
                    createdAt = System.currentTimeMillis()
                )
                syncQueueDao.insertSyncQueueItem(queueItem)

                Result.Success(Unit)
            }
        } catch (e: Exception) {
            Result.Error(AppError.Database("Gagal memperbarui status transaksi digital: ${e.message}", e))
        }
    }
}
