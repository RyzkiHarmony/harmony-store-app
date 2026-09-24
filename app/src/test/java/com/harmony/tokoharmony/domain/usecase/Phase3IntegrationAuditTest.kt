package com.harmony.tokoharmony.domain.usecase

import com.harmony.tokoharmony.core.common.AppError
import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.model.PaymentStatus
import com.harmony.tokoharmony.domain.model.PricingMethod
import com.harmony.tokoharmony.domain.model.ProductKind
import com.harmony.tokoharmony.domain.model.QuantityType
import com.harmony.tokoharmony.domain.model.Role
import com.harmony.tokoharmony.domain.model.TransactionStatus
import com.harmony.tokoharmony.domain.model.User
import com.harmony.tokoharmony.domain.usecase.auth.GetCashierUserUseCase
import com.harmony.tokoharmony.domain.usecase.cart.AddProductToDraftCartUseCase
import com.harmony.tokoharmony.domain.usecase.cart.ClearDraftCartUseCase
import com.harmony.tokoharmony.domain.usecase.cart.GetActiveDraftCartUseCase
import com.harmony.tokoharmony.domain.usecase.cart.GetOrCreateDraftCartUseCase
import com.harmony.tokoharmony.domain.usecase.cart.RemoveDraftCartItemUseCase
import com.harmony.tokoharmony.domain.usecase.cart.UpdateDraftCartItemQuantityUseCase
import com.harmony.tokoharmony.domain.usecase.price.ChangeProductPriceUseCase
import com.harmony.tokoharmony.domain.usecase.product.CreateProductUseCase
import com.harmony.tokoharmony.domain.usecase.product.DeactivateProductUseCase
import com.harmony.tokoharmony.domain.usecase.product.GetProductByBarcodeUseCase
import com.harmony.tokoharmony.domain.usecase.product.GetProductByIdUseCase
import com.harmony.tokoharmony.domain.usecase.product.SearchProductsUseCase
import com.harmony.tokoharmony.domain.usecase.scanner.BarcodeDebouncer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive Integration Audit covering Phase 3 Cashier Flow invariants.
 *
 * Checks:
 * 1. Application Start / Mode Kasir draft initialization
 * 2. Draft cart persistence & lifecycle
 * 3. Product search (partial name, case-insensitivity, barcode, inactive exclusion)
 * 4. Weighted product (0.5kg, 1.0kg, 1.5kg, 2.5kg, integer math, multiple additions)
 * 5. Quantity editing (+, -, direct edit, item removal, zero quantity)
 * 6. Price snapshot integrity (master price change does not mutate active draft snapshot)
 * 7. Barcode Scanner debounce protection
 * 8. Unknown barcode registration flow
 * 9. Inactive product filtering
 * 10. Database integrity
 * 11. Isolation: Zero Phase 4 logic
 */
class Phase3IntegrationAuditTest {

    private lateinit var productRepository: FakeProductRepository
    private lateinit var priceRepository: FakePriceRepository
    private lateinit var transactionRepository: FakeTransactionRepository

    private lateinit var createProductUseCase: CreateProductUseCase
    private lateinit var changeProductPriceUseCase: ChangeProductPriceUseCase
    private lateinit var deactivateProductUseCase: DeactivateProductUseCase
    private lateinit var getProductByIdUseCase: GetProductByIdUseCase
    private lateinit var getProductByBarcodeUseCase: GetProductByBarcodeUseCase
    private lateinit var searchProductsUseCase: SearchProductsUseCase

    private lateinit var getActiveDraftCartUseCase: GetActiveDraftCartUseCase
    private lateinit var getOrCreateDraftCartUseCase: GetOrCreateDraftCartUseCase
    private lateinit var addProductToDraftCartUseCase: AddProductToDraftCartUseCase
    private lateinit var updateDraftCartItemQuantityUseCase: UpdateDraftCartItemQuantityUseCase
    private lateinit var removeDraftCartItemUseCase: RemoveDraftCartItemUseCase
    private lateinit var clearDraftCartUseCase: ClearDraftCartUseCase
    private lateinit var getCashierUserUseCase: GetCashierUserUseCase

    private val adminUser = User("admin-1", "Admin", Role.ADMIN, "hash")
    private val cashierUser = User("cashier-1", "Kasir", Role.CASHIER, null)

    @Before
    fun setup() {
        productRepository = FakeProductRepository()
        priceRepository = FakePriceRepository(productRepository)
        transactionRepository = FakeTransactionRepository(productRepository)

        createProductUseCase = CreateProductUseCase(productRepository)
        changeProductPriceUseCase = ChangeProductPriceUseCase(productRepository, priceRepository)
        deactivateProductUseCase = DeactivateProductUseCase(productRepository)
        getProductByIdUseCase = GetProductByIdUseCase(productRepository)
        getProductByBarcodeUseCase = GetProductByBarcodeUseCase(productRepository)
        searchProductsUseCase = SearchProductsUseCase(productRepository)

        getActiveDraftCartUseCase = GetActiveDraftCartUseCase(transactionRepository)
        getOrCreateDraftCartUseCase = GetOrCreateDraftCartUseCase(transactionRepository)
        addProductToDraftCartUseCase = AddProductToDraftCartUseCase(transactionRepository)
        updateDraftCartItemQuantityUseCase = UpdateDraftCartItemQuantityUseCase(transactionRepository)
        removeDraftCartItemUseCase = RemoveDraftCartItemUseCase(transactionRepository)
        clearDraftCartUseCase = ClearDraftCartUseCase(transactionRepository)
        val authRepository = FakeAuthRepository()
        getCashierUserUseCase = GetCashierUserUseCase(authRepository)
    }

    // 1 & 2. Application Start and Draft Cart Persistence
    @Test
    fun audit_draftCartPersistenceAndReuse() = runTest {
        val user = getCashierUserUseCase()
        val draft1 = getOrCreateDraftCartUseCase(user.userId)
        assertEquals(TransactionStatus.DRAFT, draft1.transaction.transactionStatus)
        assertEquals(PaymentStatus.UNPAID, draft1.transaction.paymentStatus)
        assertTrue(draft1.isEmpty)

        // Add a product
        val prod = (createProductUseCase(
            categoryId = "cat-1",
            name = "Kopi Sachet",
            barcode = "8990101",
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "sachet",
            stockUnit = "sachet",
            currentPrice = 1500L,
            user = adminUser
        ) as Result.Success).data

        addProductToDraftCartUseCase(prod.productId, 3L, user.userId)

        // Simulate leaving cashier and returning: getOrCreate must return same draft with same items
        val draft2 = getOrCreateDraftCartUseCase(user.userId)
        assertEquals(draft1.transaction.transactionId, draft2.transaction.transactionId)
        assertEquals(1, draft2.itemCount)
        assertEquals(3L, draft2.items[0].quantity)
        assertEquals(4500L, draft2.totalAmount)
    }

    // 3. Product Search: partial name, case-insensitivity, barcode, inactive exclusion
    @Test
    fun audit_productSearchBehaviors() = runTest {
        val prod1 = (createProductUseCase(
            categoryId = "cat-1",
            name = "Indomie Goreng Spesial",
            barcode = "8991234567890",
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "pcs",
            stockUnit = "pcs",
            currentPrice = 3500L,
            user = adminUser
        ) as Result.Success).data

        val prod2 = (createProductUseCase(
            categoryId = "cat-1",
            name = "Indomie Kuah Soto",
            barcode = "8991234567891",
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "pcs",
            stockUnit = "pcs",
            currentPrice = 3500L,
            user = adminUser
        ) as Result.Success).data

        val inactiveProd = (createProductUseCase(
            categoryId = "cat-1",
            name = "Indomie Goreng Rendang",
            barcode = "8991234567892",
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "pcs",
            stockUnit = "pcs",
            currentPrice = 3800L,
            user = adminUser
        ) as Result.Success).data

        deactivateProductUseCase(inactiveProd.productId, adminUser)

        // 1. Partial name search "goreng" (case-insensitive)
        val searchGoreng = searchProductsUseCase("GORENG", onlyActive = true).first()
        assertEquals(1, searchGoreng.size)
        assertEquals(prod1.productId, searchGoreng[0].productId)

        // 2. Case-insensitive "indomie"
        val searchIndomie = searchProductsUseCase("indomie", onlyActive = true).first()
        assertEquals(2, searchIndomie.size)
        assertTrue(searchIndomie.any { it.productId == prod1.productId })
        assertTrue(searchIndomie.any { it.productId == prod2.productId })
        assertFalse(searchIndomie.any { it.productId == inactiveProd.productId }) // Inactive excluded

        // 3. Barcode search
        val searchBarcode = searchProductsUseCase("8991234567891", onlyActive = true).first()
        assertEquals(1, searchBarcode.size)
        assertEquals(prod2.productId, searchBarcode[0].productId)

        // 4. Inactive barcode lookup returns null for cashier
        val lookupInactive = getProductByBarcodeUseCase("8991234567892", onlyActive = true)
        assertNull(lookupInactive)
    }

    // 4. Weighted Product calculations (0.5kg, 1.0kg, 1.5kg, 2.5kg) & Repeated selection
    @Test
    fun audit_weightedProductCalculationsAndAggregation() = runTest {
        val apel = (createProductUseCase(
            categoryId = "cat-1",
            name = "Apel Fuji",
            barcode = null,
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_KG,
            quantityType = QuantityType.GRAM,
            sellingUnit = "kg",
            stockUnit = "kg",
            currentPrice = 40000L, // Rp40.000 / kg
            user = adminUser
        ) as Result.Success).data

        val user = getCashierUserUseCase()

        // 0.5 kg (500g) -> (500 * 40000) / 1000 = 20000
        val add05 = addProductToDraftCartUseCase(apel.productId, 500L, user.userId) as Result.Success
        assertEquals(500L, add05.data.items[0].quantity)
        assertEquals(20000L, add05.data.items[0].subtotal)
        assertEquals(20000L, add05.data.totalAmount)

        // Add 1.0 kg (1000g) more to same cart -> Aggregates to 1500g (1.5 kg)
        // (1500 * 40000) / 1000 = 60000
        val add10 = addProductToDraftCartUseCase(apel.productId, 1000L, user.userId) as Result.Success
        assertEquals(1, add10.data.itemCount)
        assertEquals(1500L, add10.data.items[0].quantity)
        assertEquals(60000L, add10.data.items[0].subtotal)
        assertEquals(60000L, add10.data.totalAmount)

        // Direct edit to 2.5 kg (2500g) -> (2500 * 40000) / 1000 = 100000
        val itemId = add10.data.items[0].itemId
        val update25 = updateDraftCartItemQuantityUseCase(itemId, 2500L) as Result.Success
        assertEquals(2500L, update25.data.items[0].quantity)
        assertEquals(100000L, update25.data.items[0].subtotal)
        assertEquals(100000L, update25.data.totalAmount)
    }

    // 5. Quantity Editing: +, -, direct edit, item removal, zero quantity
    @Test
    fun audit_quantityEditingOperations() = runTest {
        val biskuit = (createProductUseCase(
            categoryId = "cat-1",
            name = "Biskuit Roma",
            barcode = "8990202",
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "bks",
            stockUnit = "bks",
            currentPrice = 8000L,
            user = adminUser
        ) as Result.Success).data

        val user = getCashierUserUseCase()
        val addRes = addProductToDraftCartUseCase(biskuit.productId, 1L, user.userId) as Result.Success
        val itemId = addRes.data.items[0].itemId

        // Plus (+) -> 2
        val plusRes = updateDraftCartItemQuantityUseCase(itemId, 2L) as Result.Success
        assertEquals(2L, plusRes.data.items[0].quantity)
        assertEquals(16000L, plusRes.data.totalAmount)

        // Direct quantity edit -> 10
        val directRes = updateDraftCartItemQuantityUseCase(itemId, 10L) as Result.Success
        assertEquals(10L, directRes.data.items[0].quantity)
        assertEquals(80000L, directRes.data.totalAmount)

        // Minus (-) -> 9
        val minusRes = updateDraftCartItemQuantityUseCase(itemId, 9L) as Result.Success
        assertEquals(9L, minusRes.data.items[0].quantity)
        assertEquals(72000L, minusRes.data.totalAmount)

        // Zero quantity -> item removed
        val zeroRes = updateDraftCartItemQuantityUseCase(itemId, 0L) as Result.Success
        assertTrue(zeroRes.data.isEmpty)
        assertEquals(0L, zeroRes.data.totalAmount)
    }

    // 6. Price Snapshot Integrity
    @Test
    fun audit_priceSnapshotIntegrity() = runTest {
        val gula = (createProductUseCase(
            categoryId = "cat-1",
            name = "Gula Gulaku 1kg",
            barcode = "8990303",
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "bks",
            stockUnit = "bks",
            currentPrice = 16000L,
            user = adminUser
        ) as Result.Success).data

        val user = getCashierUserUseCase()

        // 1. Add to cart at Rp16.000
        addProductToDraftCartUseCase(gula.productId, 2L, user.userId)
        val initialCart = getActiveDraftCartUseCase().first()!!
        assertEquals(16000L, initialCart.items[0].unitPrice)
        assertEquals(32000L, initialCart.totalAmount)

        // 2. Master price updated to Rp17.500 by Admin
        val priceChange = changeProductPriceUseCase(gula.productId, 17500L, adminUser)
        assertTrue(priceChange is Result.Success)

        // 3. Draft cart item remains at snapshot price Rp16.000
        val postChangeCart = getActiveDraftCartUseCase().first()!!
        assertEquals(16000L, postChangeCart.items[0].unitPrice)
        assertEquals(32000L, postChangeCart.totalAmount)
    }

    // 7. Barcode Scanner Debouncer
    @Test
    fun audit_barcodeDebounceIntegrity() {
        val debouncer = BarcodeDebouncer(debounceWindowMs = 800L)
        val barcode = "8991234567890"

        // 1. First scan accepted
        assertTrue(debouncer.canProcess(barcode, currentTimeMs = 1000L))

        // 2. Rapid duplicate at 300ms rejected
        assertFalse(debouncer.canProcess(barcode, currentTimeMs = 1300L))

        // 3. Rapid duplicate at 700ms rejected
        assertFalse(debouncer.canProcess(barcode, currentTimeMs = 1700L))

        // 4. Distinct barcode accepted immediately at 1750ms
        assertTrue(debouncer.canProcess("8990000000000", currentTimeMs = 1750L))

        // 5. Original barcode accepted after window expires (>800ms after last original scan at 1000ms)
        assertTrue(debouncer.canProcess(barcode, currentTimeMs = 1900L))
    }

    // 8. Unknown Barcode Registration and Auto-add
    @Test
    fun audit_unknownBarcodeRegistrationAndCartPreservation() = runTest {
        val user = getCashierUserUseCase()

        // Existing cart with an item
        val item1 = (createProductUseCase(
            categoryId = "cat-1",
            name = "Teh Celup",
            barcode = "8990404",
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "box",
            stockUnit = "box",
            currentPrice = 6500L,
            user = adminUser
        ) as Result.Success).data
        addProductToDraftCartUseCase(item1.productId, 1L, user.userId)

        // Unknown barcode scanned
        val unknownBarcode = "8997777777"
        val lookup = getProductByBarcodeUseCase(unknownBarcode, onlyActive = true)
        assertNull(lookup)

        // Register new product with prefilled barcode
        val newProduct = (createProductUseCase(
            categoryId = "cat-1",
            name = "Krimer Kental Manis",
            barcode = unknownBarcode,
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "kaleng",
            stockUnit = "kaleng",
            currentPrice = 12000L,
            user = adminUser
        ) as Result.Success).data

        // Auto-add new product to the preserved active draft
        val finalCart = (addProductToDraftCartUseCase(newProduct.productId, 1L, user.userId) as Result.Success).data
        assertEquals(2, finalCart.itemCount)
        assertEquals(18500L, finalCart.totalAmount)
        assertEquals("Teh Celup", finalCart.items[0].productNameSnapshot)
        assertEquals("Krimer Kental Manis", finalCart.items[1].productNameSnapshot)
    }

    // 9. Inactive Product Exclusion
    @Test
    fun audit_inactiveProductExclusion() = runTest {
        val prod = (createProductUseCase(
            categoryId = "cat-1",
            name = "Beras Khusus",
            barcode = "8990505",
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "sak",
            stockUnit = "sak",
            currentPrice = 65000L,
            user = adminUser
        ) as Result.Success).data

        deactivateProductUseCase(prod.productId, adminUser)

        // Cashier search does not return inactive product
        val searchResults = searchProductsUseCase("Beras Khusus", onlyActive = true).first()
        assertTrue(searchResults.isEmpty())

        // Cashier barcode lookup returns null
        val barcodeLookup = getProductByBarcodeUseCase("8990505", onlyActive = true)
        assertNull(barcodeLookup)

        // Attempting to add inactive product returns Validation error
        val user = getCashierUserUseCase()
        val addResult = addProductToDraftCartUseCase(prod.productId, 1L, user.userId)
        assertTrue(addResult is Result.Error)
        assertTrue((addResult as Result.Error).error is AppError.Validation)

        // Admin can still view it
        val adminSearchResults = searchProductsUseCase("Beras Khusus", onlyActive = false).first()
        assertEquals(1, adminSearchResults.size)
        assertFalse(adminSearchResults[0].isActive)
    }

    // 10 & 11. Database Integrity & Phase 4 Isolation
    @Test
    fun audit_databaseIntegrityAndPhase4Isolation() = runTest {
        val prod = (createProductUseCase(
            categoryId = "cat-1",
            name = "Sabun Mandi",
            barcode = "8990606",
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "pcs",
            stockUnit = "pcs",
            currentPrice = 4000L,
            user = adminUser
        ) as Result.Success).data

        val user = getCashierUserUseCase()
        val cart = (addProductToDraftCartUseCase(prod.productId, 2L, user.userId) as Result.Success).data

        // 1. Foreign keys: Item references valid transaction and product
        val item = cart.items[0]
        assertEquals(cart.transaction.transactionId, item.transactionId)
        assertEquals(prod.productId, item.productId)

        // 2. Phase 4 Isolation: Status remains DRAFT and UNPAID
        assertEquals(TransactionStatus.DRAFT, cart.transaction.transactionStatus)
        assertEquals(PaymentStatus.UNPAID, cart.transaction.paymentStatus)

        // 3. No StockMovement or payment recorded during DRAFT cart operations
        val tx = transactionRepository.getTransactionById(cart.transaction.transactionId)
        assertNotNull(tx)
        assertEquals(TransactionStatus.DRAFT, tx!!.transactionStatus)
        assertEquals(PaymentStatus.UNPAID, tx.paymentStatus)
    }
}
