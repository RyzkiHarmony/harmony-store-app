package com.harmony.tokoharmony.domain.usecase.inventory

import com.harmony.tokoharmony.core.common.AppError
import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.model.ProductKind
import com.harmony.tokoharmony.domain.model.Role
import com.harmony.tokoharmony.domain.model.User
import com.harmony.tokoharmony.domain.repository.ProductRepository
import com.harmony.tokoharmony.domain.repository.StockRepository
import javax.inject.Inject

class RecordInitialStockUseCase @Inject constructor(
    private val stockRepository: StockRepository,
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(
        productId: String,
        initialStock: Long,
        user: User
    ): Result<Unit> {
        if (user.role != Role.ADMIN) {
            return Result.Error(AppError.Unauthorized("Hanya Admin yang dapat menetapkan stok awal."))
        }

        if (initialStock < 0) {
            return Result.Error(AppError.Validation("Stok awal tidak boleh bernilai negatif."))
        }

        val product = productRepository.getProductById(productId)
            ?: return Result.Error(AppError.NotFound("Produk tidak ditemukan."))

        if (product.productKind != ProductKind.PHYSICAL) {
            return Result.Error(AppError.Validation("Produk digital tidak memiliki stok fisik."))
        }

        if (stockRepository.hasInitialStock(productId)) {
            return Result.Error(AppError.Validation("Stok awal untuk produk ini sudah pernah ditetapkan."))
        }

        return stockRepository.recordInitialStock(productId, initialStock, user.userId)
    }
}
