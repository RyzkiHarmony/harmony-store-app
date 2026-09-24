package com.harmony.tokoharmony.domain.usecase.product

import com.harmony.tokoharmony.domain.model.Product
import com.harmony.tokoharmony.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetProductsUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    operator fun invoke(onlyActive: Boolean = true): Flow<List<Product>> {
        return if (onlyActive) {
            productRepository.getActiveProducts()
        } else {
            productRepository.getAllProducts()
        }
    }
}
