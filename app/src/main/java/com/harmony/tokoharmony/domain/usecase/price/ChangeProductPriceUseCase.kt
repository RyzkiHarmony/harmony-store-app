package com.harmony.tokoharmony.domain.usecase.price

import com.harmony.tokoharmony.core.common.AppError
import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.model.PriceHistory
import com.harmony.tokoharmony.domain.model.Role
import com.harmony.tokoharmony.domain.model.User
import com.harmony.tokoharmony.domain.repository.PriceRepository
import com.harmony.tokoharmony.domain.repository.ProductRepository
import java.util.UUID
import javax.inject.Inject

class ChangeProductPriceUseCase @Inject constructor(
    private val productRepository: ProductRepository,
    private val priceRepository: PriceRepository
) {
    suspend operator fun invoke(
        productId: String,
        newPrice: Long,
        user: User
    ): Result<PriceHistory> {
        if (user.role != Role.ADMIN) {
            return Result.Error(AppError.Unauthorized("Hanya Admin yang dapat mengubah harga master produk."))
        }

        if (newPrice < 0) {
            return Result.Error(AppError.Validation("Harga produk baru tidak boleh negatif."))
        }

        val product = productRepository.getProductById(productId)
            ?: return Result.Error(AppError.NotFound("Produk dengan ID $productId tidak ditemukan."))

        if (product.currentPrice == newPrice) {
            return Result.Error(AppError.Validation("Harga baru tidak boleh sama dengan harga saat ini."))
        }

        val priceHistory = PriceHistory(
            historyId = UUID.randomUUID().toString(),
            productId = productId,
            oldPrice = product.currentPrice,
            newPrice = newPrice,
            changedAt = System.currentTimeMillis(),
            changedBy = user.userId
        )

        return try {
            priceRepository.recordPriceChange(priceHistory, newPrice)
            Result.Success(priceHistory)
        } catch (e: Exception) {
            Result.Error(AppError.Database("Gagal mengubah harga produk: ${e.message}", e))
        }
    }
}
