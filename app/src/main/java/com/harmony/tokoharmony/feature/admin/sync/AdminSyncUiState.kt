package com.harmony.tokoharmony.feature.admin.sync

data class AdminSyncUiState(
    val endpointUrl: String = "",
    val syncToken: String = "",
    val isAutoSyncEnabled: Boolean = true,
    val isConfigured: Boolean = false,
    val pendingCount: Int = 0,
    val failedCount: Int = 0,
    val lastSyncTime: Long? = null,
    val isSyncing: Boolean = false,
    val isRestoring: Boolean = false,
    val syncResultSummary: String? = null,
    val error: String? = null,
    val showRestoreConfirmDialog: Boolean = false,
    val isEditingConfig: Boolean = false
)
