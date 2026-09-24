package com.harmony.tokoharmony.feature.admin.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harmony.tokoharmony.core.network.SyncConfig
import com.harmony.tokoharmony.core.sync.SyncManager
import com.harmony.tokoharmony.domain.usecase.sync.BootstrapDataUseCase
import com.harmony.tokoharmony.domain.usecase.sync.GetSyncStatusUseCase
import com.harmony.tokoharmony.domain.usecase.sync.SyncPendingDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminSyncViewModel @Inject constructor(
    private val getSyncStatusUseCase: GetSyncStatusUseCase,
    private val syncPendingDataUseCase: SyncPendingDataUseCase,
    private val bootstrapDataUseCase: BootstrapDataUseCase,
    private val syncConfig: SyncConfig,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AdminSyncUiState(
            endpointUrl = syncConfig.endpointUrl,
            syncToken = syncConfig.syncToken,
            isAutoSyncEnabled = syncConfig.isAutoSyncEnabled,
            isConfigured = syncConfig.isConfigured()
        )
    )
    val uiState: StateFlow<AdminSyncUiState> = _uiState.asStateFlow()

    init {
        observeSyncStatus()
    }

    private fun observeSyncStatus() {
        viewModelScope.launch {
            getSyncStatusUseCase().collect { statusInfo ->
                _uiState.update { current ->
                    current.copy(
                        pendingCount = statusInfo.pendingCount,
                        failedCount = statusInfo.failedCount,
                        lastSyncTime = statusInfo.lastSyncTime ?: if (syncConfig.lastSyncTimestamp > 0) syncConfig.lastSyncTimestamp else null
                    )
                }
            }
        }
    }

    fun onEndpointUrlChanged(url: String) {
        _uiState.update { it.copy(endpointUrl = url) }
    }

    fun onSyncTokenChanged(token: String) {
        _uiState.update { it.copy(syncToken = token) }
    }

    fun onAutoSyncToggled(enabled: Boolean) {
        syncConfig.isAutoSyncEnabled = enabled
        _uiState.update { it.copy(isAutoSyncEnabled = enabled) }
        if (enabled) {
            syncManager.schedulePeriodicSync()
        } else {
            syncManager.cancelAllSync()
        }
    }

    fun saveConfig() {
        val url = _uiState.value.endpointUrl.trim()
        val token = _uiState.value.syncToken.trim()
        syncConfig.endpointUrl = url
        syncConfig.syncToken = token
        val isConfigured = syncConfig.isConfigured()
        _uiState.update {
            it.copy(
                isConfigured = isConfigured,
                isEditingConfig = false,
                syncResultSummary = if (isConfigured) "Konfigurasi gateway Google Sheets disimpan." else "URL gateway belum valid.",
                error = null
            )
        }
        if (isConfigured && syncConfig.isAutoSyncEnabled) {
            syncManager.schedulePeriodicSync()
        }
    }

    fun toggleEditConfig() {
        _uiState.update { it.copy(isEditingConfig = !it.isEditingConfig) }
    }

    fun triggerManualSync() {
        if (!syncConfig.isConfigured()) {
            _uiState.update {
                it.copy(error = "URL Google Apps Script belum dikonfigurasi. Masukkan URL terlebih dahulu.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, error = null, syncResultSummary = null) }
            try {
                val result = syncPendingDataUseCase(batchSize = 100)
                if (result.isNetworkError) {
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            error = "Gagal terhubung ke remote Google Sheets: ${result.errorMessage ?: "Jaringan tidak tersedia"}"
                        )
                    }
                } else if (result.totalProcessed == 0) {
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            syncResultSummary = "Semua data lokal sudah tersinkronisasi (antrean kosong)."
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            syncResultSummary = "Sinkronisasi selesai: ${result.successCount} berhasil, ${result.failureCount} gagal dari ${result.totalProcessed} antrean."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        error = "Terjadi kesalahan saat sinkronisasi: ${e.message}"
                    )
                }
            }
        }
    }

    fun showRestoreConfirmation() {
        _uiState.update { it.copy(showRestoreConfirmDialog = true) }
    }

    fun dismissRestoreConfirmation() {
        _uiState.update { it.copy(showRestoreConfirmDialog = false) }
    }

    fun executeRestoreFromGoogleSheets() {
        _uiState.update { it.copy(showRestoreConfirmDialog = false, isRestoring = true, error = null, syncResultSummary = null) }
        viewModelScope.launch {
            val result = bootstrapDataUseCase()
            if (result.isSuccess) {
                val data = result.getOrThrow()
                val summary = "Pemulihan berhasil: ${data.products.size} produk, ${data.categories.size} kategori, ${data.transactions.size} transaksi, ${data.stockMovements.size} mutasi stok."
                _uiState.update {
                    it.copy(
                        isRestoring = false,
                        syncResultSummary = summary,
                        error = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isRestoring = false,
                        error = "Gagal memulihkan data dari Google Sheets: ${result.exceptionOrNull()?.message}"
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, syncResultSummary = null) }
    }
}
