package com.harmony.tokoharmony.domain.usecase.category

import com.harmony.tokoharmony.core.common.AppError
import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.model.Category
import com.harmony.tokoharmony.domain.model.Role
import com.harmony.tokoharmony.domain.model.User
import com.harmony.tokoharmony.domain.repository.CategoryRepository
import java.util.UUID
import javax.inject.Inject

class CreateCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(name: String, user: User): Result<Category> {
        if (user.role != Role.ADMIN) {
            return Result.Error(AppError.Unauthorized("Hanya Admin yang dapat membuat kategori baru."))
        }
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            return Result.Error(AppError.Validation("Nama kategori tidak boleh kosong."))
        }

        val category = Category(
            categoryId = UUID.randomUUID().toString(),
            name = trimmedName,
            isActive = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        return try {
            categoryRepository.saveCategory(category)
            Result.Success(category)
        } catch (e: Exception) {
            Result.Error(AppError.Database("Gagal menyimpan kategori: ${e.message}", e))
        }
    }
}
