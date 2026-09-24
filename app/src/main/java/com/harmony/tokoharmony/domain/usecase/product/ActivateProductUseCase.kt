package com.harmony.tokoharmony.domain.usecase.product

import com.harmony.tokoharmony.core.common.AppError
import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.model.Role
import com.harmony.tokoharmony.domain.model.User
import com.harmony.tokoharmony.domain.repository.ProductRepository
import javax.inject.Inject

class ActivateProductUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(productId: String, user: User): Result<Unit> {
        if (user.role != Role.ADMIN) {
            return Result.Error(AppError.Unauthorized("Hanya Admin yang dapat mengaktifkan produk."))
        }

        if (productRepository.getProductById(productId) == null) {
            return Result.Error(AppError.NotFound("Produk tidak ditemukan."))
        }

        return try {
            productRepository.setProductActiveStatus(productId, true)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.Database("Gagal mengaktifkan produk: ${e.message}", e))
        }
    }
}
