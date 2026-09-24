package com.harmony.tokoharmony.core.network

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncConfig @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("toko_sync_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ENDPOINT_URL = "sync_endpoint_url"
        private const val KEY_SYNC_TOKEN = "sync_auth_token"
        private const val KEY_LAST_SYNC_TIMESTAMP = "last_sync_timestamp"
        private const val KEY_AUTO_SYNC_ENABLED = "auto_sync_enabled"
        
        // Default placeholder or mockable URL
        const val DEFAULT_ENDPOINT_URL = ""
    }

    var endpointUrl: String
        get() = prefs.getString(KEY_ENDPOINT_URL, DEFAULT_ENDPOINT_URL) ?: DEFAULT_ENDPOINT_URL
        set(value) = prefs.edit().putString(KEY_ENDPOINT_URL, value.trim()).apply()

    var syncToken: String
        get() = prefs.getString(KEY_SYNC_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SYNC_TOKEN, value.trim()).apply()

    var isAutoSyncEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SYNC_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_SYNC_ENABLED, value).apply()

    var lastSyncTimestamp: Long
        get() = prefs.getLong(KEY_LAST_SYNC_TIMESTAMP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC_TIMESTAMP, value).apply()

    fun isConfigured(): Boolean {
        return endpointUrl.isNotBlank() && (endpointUrl.startsWith("http://") || endpointUrl.startsWith("https://"))
    }
}
