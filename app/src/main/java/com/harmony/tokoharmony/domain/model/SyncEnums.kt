package com.harmony.tokoharmony.domain.model

enum class SyncStatus {
    PENDING,
    SYNCING,
    SYNCED,
    FAILED
}

enum class SyncOperation {
    CREATE,
    UPDATE,
    CANCEL
}

enum class SyncEntityType(val code: String) {
    USER("USER"),
    CATEGORY("CATEGORY"),
    PRODUCT("PRODUCT"),
    PRICE_HISTORY("PRICE_HISTORY"),
    TRANSACTION("TRANSACTION"),
    TRANSACTION_ITEM("TRANSACTION_ITEM"),
    STOCK_MOVEMENT("STOCK_MOVEMENT"),
    STOCK_IN("STOCK_IN"),
    STOCK_ADJUSTMENT("STOCK_ADJUSTMENT"),
    DIGITAL_TRANSACTION("DIGITAL_TRANSACTION");

    companion object {
        fun fromCode(code: String): SyncEntityType {
            return entries.find { it.code.equals(code, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown sync entity type: $code")
        }
    }
}
