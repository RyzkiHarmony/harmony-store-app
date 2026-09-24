package com.harmony.tokoharmony.domain.usecase.product

import com.harmony.tokoharmony.domain.model.Product
import com.harmony.tokoharmony.domain.model.ProductKind
import com.harmony.tokoharmony.domain.model.ProductStockInfo
import com.harmony.tokoharmony.domain.model.StockLevelStatus
import com.harmony.tokoharmony.domain.repository.StockRepository
import javax.inject.Inject

class EvaluateProductStockStatusUseCase @Inject constructor(
    private val stockRepository: StockRepository
) {
    operator fun invoke(
        product: Product,
        currentStock: Long
    ): StockLevelStatus {
        if (product.productKind == ProductKind.DIGITAL) {
            return StockLevelStatus.DIGITAL
        }
        if (currentStock <= 0L) {
            return StockLevelStatus.OUT_OF_STOCK
        }
        val minStock = product.minimumStock
        if (minStock != null && currentStock <= minStock) {
            return StockLevelStatus.LOW_STOCK
        }
        return StockLevelStatus.NORMAL
    }

    suspend fun getStockInfo(product: Product): ProductStockInfo {
        if (product.productKind == ProductKind.DIGITAL) {
            return ProductStockInfo(
                productId = product.productId,
                currentStock = 0L,
                stockLevel = StockLevelStatus.DIGITAL,
                stockUnit = product.stockUnit
            )
        }
        val currentStock = stockRepository.getCurrentStock(product.productId)
        val level = invoke(product, currentStock)
        return ProductStockInfo(
            productId = product.productId,
            currentStock = currentStock,
            stockLevel = level,
            stockUnit = product.stockUnit
        )
    }
}
