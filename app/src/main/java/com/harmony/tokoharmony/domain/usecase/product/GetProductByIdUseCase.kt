package com.harmony.tokoharmony.domain.usecase.product

import com.harmony.tokoharmony.domain.model.Product
import com.harmony.tokoharmony.domain.repository.ProductRepository
import javax.inject.Inject

class GetProductByIdUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(productId: String): Product? {
        return productRepository.getProductById(productId)
    }
}
