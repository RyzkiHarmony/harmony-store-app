package com.harmony.tokoharmony.domain.usecase.product

import com.harmony.tokoharmony.domain.model.Product
import com.harmony.tokoharmony.domain.repository.ProductRepository
import javax.inject.Inject

class GetProductByBarcodeUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(barcode: String, onlyActive: Boolean = true): Product? {
        val trimmed = barcode.trim()
        if (trimmed.isEmpty()) return null
        val product = productRepository.getProductByBarcode(trimmed) ?: return null
        if (onlyActive && !product.isActive) {
            return null
        }
        return product
    }
}
