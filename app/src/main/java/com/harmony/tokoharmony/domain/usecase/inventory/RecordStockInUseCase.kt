package com.harmony.tokoharmony.domain.usecase.inventory

import com.harmony.tokoharmony.core.common.AppError
import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.model.ProductKind
import com.harmony.tokoharmony.domain.model.Role
import com.harmony.tokoharmony.domain.model.StockIn
import com.harmony.tokoharmony.domain.model.User
import com.harmony.tokoharmony.domain.repository.ProductRepository
import com.harmony.tokoharmony.domain.repository.StockRepository
import javax.inject.Inject

class RecordStockInUseCase @Inject constructor(
    private val stockRepository: StockRepository,
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(
        productId: String,
        purchaseQuantity: Long,
        purchaseUnit: String,
        conversionFactor: Long,
        note: String?,
        user: User
    ): Result<StockIn> {
        if (user.role != Role.ADMIN) {
            return Result.Error(AppError.Unauthorized("Hanya Admin yang dapat mencatat barang masuk."))
        }

        if (purchaseQuantity <= 0) {
            return Result.Error(AppError.Validation("Jumlah pembelian harus lebih dari 0."))
        }

        if (conversionFactor <= 0) {
            return Result.Error(AppError.Validation("Faktor konversi harus lebih dari 0."))
        }

        if (purchaseUnit.isBlank()) {
            return Result.Error(AppError.Validation("Satuan pembelian wajib diisi."))
        }

        val product = productRepository.getProductById(productId)
            ?: return Result.Error(AppError.NotFound("Produk tidak ditemukan."))

        if (product.productKind != ProductKind.PHYSICAL) {
            return Result.Error(AppError.Validation("Produk digital tidak dapat menerima stok fisik."))
        }

        return stockRepository.recordStockIn(
            productId = productId,
            purchaseQuantity = purchaseQuantity,
            purchaseUnit = purchaseUnit,
            conversionFactor = conversionFactor,
            note = note,
            userId = user.userId
        )
    }
}
