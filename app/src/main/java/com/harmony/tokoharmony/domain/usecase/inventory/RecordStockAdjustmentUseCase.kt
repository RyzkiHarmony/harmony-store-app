package com.harmony.tokoharmony.domain.usecase.inventory

import com.harmony.tokoharmony.core.common.AppError
import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.model.ProductKind
import com.harmony.tokoharmony.domain.model.Role
import com.harmony.tokoharmony.domain.model.StockAdjustment
import com.harmony.tokoharmony.domain.model.User
import com.harmony.tokoharmony.domain.repository.ProductRepository
import com.harmony.tokoharmony.domain.repository.StockRepository
import javax.inject.Inject

class RecordStockAdjustmentUseCase @Inject constructor(
    private val stockRepository: StockRepository,
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(
        productId: String,
        physicalQuantity: Long,
        reason: String,
        note: String?,
        user: User
    ): Result<StockAdjustment> {
        if (user.role != Role.ADMIN) {
            return Result.Error(AppError.Unauthorized("Hanya Admin yang dapat melakukan stock opname."))
        }

        if (physicalQuantity < 0) {
            return Result.Error(AppError.Validation("Stok fisik hasil opname tidak boleh negatif."))
        }

        val trimmedReason = reason.trim()
        if (trimmedReason.isBlank()) {
            return Result.Error(AppError.Validation("Alasan penyesuaian stok wajib dipilih/diisi."))
        }

        val product = productRepository.getProductById(productId)
            ?: return Result.Error(AppError.NotFound("Produk tidak ditemukan."))

        if (product.productKind != ProductKind.PHYSICAL) {
            return Result.Error(AppError.Validation("Produk digital tidak dapat disesuaikan stok fisiknya."))
        }

        return stockRepository.recordStockAdjustment(
            productId = productId,
            physicalQuantity = physicalQuantity,
            reason = trimmedReason,
            note = note?.trim(),
            userId = user.userId
        )
    }
}
