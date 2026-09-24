package com.harmony.tokoharmony.domain.repository

import com.harmony.tokoharmony.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAllCategories(): Flow<List<Category>>
    fun getActiveCategories(): Flow<List<Category>>
    suspend fun getCategoryById(categoryId: String): Category?
    suspend fun saveCategory(category: Category)
    suspend fun saveCategories(categories: List<Category>)
    suspend fun updateCategory(category: Category)
}
