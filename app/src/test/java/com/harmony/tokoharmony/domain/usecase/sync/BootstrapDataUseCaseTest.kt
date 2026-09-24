package com.harmony.tokoharmony.domain.usecase.sync

import com.harmony.tokoharmony.domain.model.BootstrapData
import com.harmony.tokoharmony.domain.model.Category
import com.harmony.tokoharmony.domain.model.PricingMethod
import com.harmony.tokoharmony.domain.model.Product
import com.harmony.tokoharmony.domain.model.ProductKind
import com.harmony.tokoharmony.domain.model.QuantityType
import com.harmony.tokoharmony.domain.model.Role
import com.harmony.tokoharmony.domain.model.User
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BootstrapDataUseCaseTest {

    private lateinit var fakeSyncRepository: FakeSyncRepository
    private lateinit var bootstrapDataUseCase: BootstrapDataUseCase

    @Before
    fun setUp() {
        fakeSyncRepository = FakeSyncRepository()
        bootstrapDataUseCase = BootstrapDataUseCase(fakeSyncRepository)
    }

    @Test
    fun invoke_fetchesAndRestoresDataSuccessfully() = runTest {
        val sampleData = BootstrapData(
            users = listOf(User("usr-1", "Admin", Role.ADMIN, null, true, 0L, 0L)),
            categories = listOf(Category("cat-1", "Sembako", true, 0L, 0L)),
            products = listOf(
                Product(
                    productId = "prod-1",
                    categoryId = "cat-1",
                    name = "Beras",
                    barcode = null,
                    productKind = ProductKind.PHYSICAL,
                    pricingMethod = PricingMethod.PER_UNIT,
                    quantityType = QuantityType.COUNT,
                    sellingUnit = "pcs",
                    stockUnit = "pcs",
                    currentPrice = 10000L,
                    isActive = true,
                    createdAt = 0L,
                    updatedAt = 0L
                )
            )
        )

        fakeSyncRepository.bootstrapDataToReturn = Result.success(sampleData)

        val result = bootstrapDataUseCase()

        assertTrue(result.isSuccess)
        val returned = result.getOrThrow()
        assertEquals(1, returned.users.size)
        assertEquals(1, returned.categories.size)
        assertEquals(1, returned.products.size)
        assertEquals(sampleData, fakeSyncRepository.restoredBootstrapData)
    }
}
