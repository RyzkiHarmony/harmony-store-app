package com.harmony.tokoharmony.domain.usecase.product

import com.harmony.tokoharmony.domain.model.Product
import com.harmony.tokoharmony.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchProductsUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    operator fun invoke(query: String, onlyActive: Boolean = true): Flow<List<Product>> {
        val trimmedQuery = query.trim()
        return if (trimmedQuery.isEmpty()) {
            if (onlyActive) {
                productRepository.getActiveProducts()
            } else {
                productRepository.getAllProducts()
            }
        } else {
            productRepository.searchProducts(trimmedQuery, onlyActive)
        }
    }
}
