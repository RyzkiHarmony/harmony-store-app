package com.harmony.tokoharmony.core.common

sealed interface Result<out T> {
    data class Success<out T>(val data: T) : Result<T>
    data class Error(val error: AppError) : Result<Nothing>
}

sealed interface AppError {
    val message: String

    data class Validation(override val message: String) : AppError
    data class NotFound(override val message: String) : AppError
    data class Unauthorized(override val message: String = "Akses ditolak. Otorisasi diperlukan.") : AppError
    data class InsufficientStock(
        val availableQuantity: Long,
        val requestedQuantity: Long,
        override val message: String = "Stok tidak mencukupi. Tersedia: $availableQuantity, diminta: $requestedQuantity"
    ) : AppError
    data class InsufficientPayment(
        val totalAmount: Long,
        val amountReceived: Long,
        val shortfall: Long = totalAmount - amountReceived,
        override val message: String = "Uang pembayaran kurang Rp$shortfall"
    ) : AppError
    data class Database(override val message: String, val cause: Throwable? = null) : AppError
    data class Network(override val message: String, val cause: Throwable? = null) : AppError
    data class Unknown(override val message: String, val cause: Throwable? = null) : AppError
}
