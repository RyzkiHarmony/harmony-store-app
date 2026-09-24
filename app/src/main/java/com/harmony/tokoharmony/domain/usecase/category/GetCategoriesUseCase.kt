package com.harmony.tokoharmony.domain.usecase.category

import com.harmony.tokoharmony.domain.model.Category
import com.harmony.tokoharmony.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    operator fun invoke(onlyActive: Boolean = true): Flow<List<Category>> {
        return if (onlyActive) {
            categoryRepository.getActiveCategories()
        } else {
            categoryRepository.getAllCategories()
        }
    }
}
