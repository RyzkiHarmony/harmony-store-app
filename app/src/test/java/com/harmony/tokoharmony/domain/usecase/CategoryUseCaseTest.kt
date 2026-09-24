package com.harmony.tokoharmony.domain.usecase

import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.model.Category
import com.harmony.tokoharmony.domain.model.Role
import com.harmony.tokoharmony.domain.model.User
import com.harmony.tokoharmony.domain.repository.CategoryRepository
import com.harmony.tokoharmony.domain.usecase.category.CreateCategoryUseCase
import com.harmony.tokoharmony.domain.usecase.category.GetCategoriesUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeCategoryRepository : CategoryRepository {
    val categories = mutableMapOf<String, Category>()

    override fun getAllCategories(): Flow<List<Category>> {
        return flowOf(categories.values.toList())
    }

    override fun getActiveCategories(): Flow<List<Category>> {
        return flowOf(categories.values.filter { it.isActive })
    }

    override suspend fun getCategoryById(categoryId: String): Category? {
        return categories[categoryId]
    }

    override suspend fun saveCategory(category: Category) {
        categories[category.categoryId] = category
    }

    override suspend fun saveCategories(categories: List<Category>) {
        categories.forEach { this.categories[it.categoryId] = it }
    }

    override suspend fun updateCategory(category: Category) {
        categories[category.categoryId] = category
    }
}

class CategoryUseCaseTest {

    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var createCategoryUseCase: CreateCategoryUseCase
    private lateinit var getCategoriesUseCase: GetCategoriesUseCase

    private val adminUser = User("admin-1", "Admin", Role.ADMIN, "pin")
    private val cashierUser = User("cashier-1", "Kasir", Role.CASHIER, null)

    @Before
    fun setup() {
        categoryRepository = FakeCategoryRepository()
        createCategoryUseCase = CreateCategoryUseCase(categoryRepository)
        getCategoriesUseCase = GetCategoriesUseCase(categoryRepository)
    }

    @Test
    fun admin_canCreateCategory() = runTest {
        val result = createCategoryUseCase("Makanan Ringan", adminUser)
        assertTrue(result is Result.Success)
        val created = (result as Result.Success).data
        assertEquals("Makanan Ringan", created.name)

        val list = getCategoriesUseCase().first()
        assertEquals(1, list.size)
        assertEquals("Makanan Ringan", list[0].name)
    }

    @Test
    fun cashier_cannotCreateCategory() = runTest {
        val result = createCategoryUseCase("Minuman", cashierUser)
        assertTrue(result is Result.Error)
    }

    @Test
    fun blankCategoryName_isRejected() = runTest {
        val result = createCategoryUseCase("   ", adminUser)
        assertTrue(result is Result.Error)
    }
}
