package com.harmony.tokoharmony.domain.usecase

import com.harmony.tokoharmony.core.common.AppError
import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.model.DraftCart
import com.harmony.tokoharmony.domain.model.PaymentMethod
import com.harmony.tokoharmony.domain.model.PaymentStatus
import com.harmony.tokoharmony.domain.model.PricingMethod
import com.harmony.tokoharmony.domain.model.Product
import com.harmony.tokoharmony.domain.model.ProductKind
import com.harmony.tokoharmony.domain.model.QuantityType
import com.harmony.tokoharmony.domain.model.Role
import com.harmony.tokoharmony.domain.model.Transaction
import com.harmony.tokoharmony.domain.model.TransactionItem
import com.harmony.tokoharmony.domain.model.TransactionStatus
import com.harmony.tokoharmony.domain.model.User
import com.harmony.tokoharmony.domain.repository.ProductRepository
import com.harmony.tokoharmony.domain.repository.TransactionRepository
import com.harmony.tokoharmony.domain.usecase.cart.AddProductToDraftCartUseCase
import com.harmony.tokoharmony.domain.usecase.cart.ClearDraftCartUseCase
import com.harmony.tokoharmony.domain.usecase.cart.GetActiveDraftCartUseCase
import com.harmony.tokoharmony.domain.usecase.cart.GetOrCreateDraftCartUseCase
import com.harmony.tokoharmony.domain.usecase.cart.RemoveDraftCartItemUseCase
import com.harmony.tokoharmony.domain.usecase.cart.UpdateDraftCartItemQuantityUseCase
import com.harmony.tokoharmony.domain.usecase.price.ChangeProductPriceUseCase
import com.harmony.tokoharmony.domain.usecase.product.CreateProductUseCase
import com.harmony.tokoharmony.domain.usecase.product.DeactivateProductUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class FakeTransactionRepository(
    private val productRepository: ProductRepository
) : TransactionRepository {

    private var activeDraft: Transaction? = null
    private val itemsMap = mutableMapOf<String, MutableList<TransactionItem>>()
    private val draftFlow = MutableStateFlow<DraftCart?>(null)

    private fun updateFlow() {
        val draft = activeDraft
        if (draft == null) {
            draftFlow.value = null
        } else {
            val items = itemsMap[draft.transactionId] ?: emptyList()
            draftFlow.value = DraftCart(
                transaction = draft.copy(totalAmount = items.sumOf { it.subtotal }),
                items = items.toList()
            )
        }
    }

    override fun getActiveDraftCart(): Flow<DraftCart?> = draftFlow.asStateFlow()

    override suspend fun getOrCreateActiveDraft(userId: String): DraftCart {
        if (activeDraft == null) {
            val txId = UUID.randomUUID().toString()
            activeDraft = Transaction(
                transactionId = txId,
                transactionNumber = "TRX-TEST-001",
                transactionStatus = TransactionStatus.DRAFT,
                paymentStatus = PaymentStatus.UNPAID,
                totalAmount = 0L,
                createdAt = System.currentTimeMillis(),
                createdBy = userId
            )
            itemsMap[txId] = mutableListOf()
            updateFlow()
        }
        val items = itemsMap[activeDraft!!.transactionId] ?: emptyList()
        return DraftCart(activeDraft!!, items)
    }

    override suspend fun addItemToDraft(
        productId: String,
        quantity: Long,
        userId: String
    ): Result<DraftCart> {
        if (quantity <= 0) return Result.Error(AppError.Validation("Jumlah harus > 0"))

        val product = productRepository.getProductById(productId)
            ?: return Result.Error(AppError.NotFound("Produk tidak ditemukan"))

        if (!product.isActive) {
            return Result.Error(AppError.Validation("Produk nonaktif tidak dapat ditambahkan"))
        }

        val isGram = product.quantityType == QuantityType.GRAM
        if (isGram && quantity % 500L != 0L) {
            return Result.Error(AppError.Validation("Berat harus kelipatan 500g"))
        }

        val draft = getOrCreateActiveDraft(userId)
        val list = itemsMap.getOrPut(draft.transaction.transactionId) { mutableListOf() }
        val existingIndex = list.indexOfFirst { it.productId == productId }

        if (existingIndex >= 0) {
            val existing = list[existingIndex]
            val newQty = existing.quantity + quantity
            val newSubtotal = if (isGram) (newQty * existing.unitPrice) / 1000L else newQty * existing.unitPrice
            list[existingIndex] = existing.copy(quantity = newQty, subtotal = newSubtotal)
        } else {
            val subtotal = if (isGram) (quantity * product.currentPrice) / 1000L else quantity * product.currentPrice
            val newItem = TransactionItem(
                itemId = UUID.randomUUID().toString(),
                transactionId = draft.transaction.transactionId,
                productId = productId,
                productNameSnapshot = product.name,
                quantity = quantity,
                unitPrice = product.currentPrice,
                subtotal = subtotal,
                sellingUnit = product.sellingUnit
            )
            list.add(newItem)
        }

        updateFlow()
        return Result.Success(draftFlow.value!!)
    }

    override suspend fun updateDraftItemQuantity(itemId: String, newQuantity: Long): Result<DraftCart> {
        if (newQuantity <= 0) return removeDraftItem(itemId)

        val draft = activeDraft ?: return Result.Error(AppError.NotFound("Draft tidak ada"))
        val list = itemsMap[draft.transactionId] ?: return Result.Error(AppError.NotFound("Item tidak ada"))
        val index = list.indexOfFirst { it.itemId == itemId }
        if (index < 0) return Result.Error(AppError.NotFound("Item tidak ditemukan"))

        val item = list[index]
        val product = productRepository.getProductById(item.productId)
        val isGram = product?.quantityType == QuantityType.GRAM

        if (isGram && newQuantity % 500L != 0L) {
            return Result.Error(AppError.Validation("Berat harus kelipatan 500g"))
        }

        val subtotal = if (isGram) (newQuantity * item.unitPrice) / 1000L else newQuantity * item.unitPrice
        list[index] = item.copy(quantity = newQuantity, subtotal = subtotal)

        updateFlow()
        return Result.Success(draftFlow.value!!)
    }

    override suspend fun removeDraftItem(itemId: String): Result<DraftCart> {
        val draft = activeDraft ?: return Result.Error(AppError.NotFound("Draft tidak ada"))
        val list = itemsMap[draft.transactionId] ?: return Result.Error(AppError.NotFound("Item tidak ada"))
        list.removeAll { it.itemId == itemId }

        updateFlow()
        return Result.Success(draftFlow.value!!)
    }

    override suspend fun clearDraft(transactionId: String): Result<Unit> {
        itemsMap[transactionId]?.clear()
        updateFlow()
        return Result.Success(Unit)
    }

    override suspend fun getTransactionById(transactionId: String): Transaction? {
        return if (activeDraft?.transactionId == transactionId) activeDraft else null
    }

    override suspend fun getItemsForTransaction(transactionId: String): List<TransactionItem> {
        return itemsMap[transactionId] ?: emptyList()
    }

    override suspend fun completeCashTransaction(
        transactionId: String,
        amountReceived: Long,
        userId: String
    ): Result<Transaction> {
        val draft = activeDraft ?: return Result.Error(AppError.NotFound("Draft tidak ditemukan"))
        val items = itemsMap[draft.transactionId] ?: emptyList()
        val total = items.sumOf { it.subtotal }
        val completed = draft.copy(
            transactionStatus = TransactionStatus.COMPLETED,
            paymentStatus = PaymentStatus.PAID,
            paymentMethod = PaymentMethod.CASH,
            totalAmount = total,
            amountReceived = amountReceived,
            changeAmount = amountReceived - total,
            completedAt = System.currentTimeMillis()
        )
        activeDraft = null
        updateFlow()
        return Result.Success(completed)
    }

    override suspend fun completeQrisTransaction(
        transactionId: String,
        userId: String
    ): Result<Transaction> {
        val draft = activeDraft ?: return Result.Error(AppError.NotFound("Draft tidak ditemukan"))
        val items = itemsMap[draft.transactionId] ?: emptyList()
        val total = items.sumOf { it.subtotal }
        val completed = draft.copy(
            transactionStatus = TransactionStatus.COMPLETED,
            paymentStatus = PaymentStatus.PAID,
            paymentMethod = PaymentMethod.QRIS,
            totalAmount = total,
            amountReceived = total,
            changeAmount = 0L,
            completedAt = System.currentTimeMillis()
        )
        activeDraft = null
        updateFlow()
        return Result.Success(completed)
    }

    override fun getAllTransactions(): Flow<List<Transaction>> = kotlinx.coroutines.flow.flowOf(emptyList())

    override suspend fun cancelTransaction(transactionId: String, reason: String, userId: String): Result<Transaction> {
        val draft = activeDraft
        if (draft != null && draft.transactionId == transactionId) {
            val updated = draft.copy(transactionStatus = TransactionStatus.CANCELLED, cancellationReason = reason, cancelledBy = userId, cancelledAt = System.currentTimeMillis())
            activeDraft = null
            updateFlow()
            return Result.Success(updated)
        }
        return Result.Error(AppError.NotFound("Transaksi tidak ditemukan"))
    }
}

class DraftCartUseCaseTest {

    private lateinit var productRepository: FakeProductRepository
    private lateinit var priceRepository: FakePriceRepository
    private lateinit var transactionRepository: FakeTransactionRepository

    private lateinit var createProductUseCase: CreateProductUseCase
    private lateinit var changeProductPriceUseCase: ChangeProductPriceUseCase
    private lateinit var deactivateProductUseCase: DeactivateProductUseCase

    private lateinit var getActiveDraftCartUseCase: GetActiveDraftCartUseCase
    private lateinit var getOrCreateDraftCartUseCase: GetOrCreateDraftCartUseCase
    private lateinit var addProductToDraftCartUseCase: AddProductToDraftCartUseCase
    private lateinit var updateDraftCartItemQuantityUseCase: UpdateDraftCartItemQuantityUseCase
    private lateinit var removeDraftCartItemUseCase: RemoveDraftCartItemUseCase
    private lateinit var clearDraftCartUseCase: ClearDraftCartUseCase

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

        getActiveDraftCartUseCase = GetActiveDraftCartUseCase(transactionRepository)
        getOrCreateDraftCartUseCase = GetOrCreateDraftCartUseCase(transactionRepository)
        addProductToDraftCartUseCase = AddProductToDraftCartUseCase(transactionRepository)
        updateDraftCartItemQuantityUseCase = UpdateDraftCartItemQuantityUseCase(transactionRepository)
        removeDraftCartItemUseCase = RemoveDraftCartItemUseCase(transactionRepository)
        clearDraftCartUseCase = ClearDraftCartUseCase(transactionRepository)
    }

    @Test
    fun getOrCreateDraft_createsNewDraftIfNoneExists_andReusesExistingDraft() = runTest {
        val draft1 = getOrCreateDraftCartUseCase("cashier-1")
        assertNotNull(draft1.transaction)
        assertEquals(TransactionStatus.DRAFT, draft1.transaction.transactionStatus)
        assertTrue(draft1.isEmpty)

        val draft2 = getOrCreateDraftCartUseCase("cashier-1")
        assertEquals(draft1.transaction.transactionId, draft2.transaction.transactionId)
    }

    @Test
    fun addProduct_capturesUnitPriceSnapshot_andMasterPriceChangeDoesNotChangeCartItemPrice() = runTest {
        val createResult = createProductUseCase(
            categoryId = "cat-1",
            name = "Indomie Goreng",
            barcode = "8991111",
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "pcs",
            stockUnit = "pcs",
            currentPrice = 3500L,
            user = adminUser
        ) as Result.Success

        val product = createResult.data

        // Add to cart at price 3500
        val addResult = addProductToDraftCartUseCase(product.productId, 2L, cashierUser.userId) as Result.Success
        val cart = addResult.data
        assertEquals(1, cart.itemCount)
        val cartItem = cart.items[0]
        assertEquals(3500L, cartItem.unitPrice)
        assertEquals(7000L, cartItem.subtotal)
        assertEquals(7000L, cart.totalAmount)

        // Admin changes master price to 4000
        val changePriceResult = changeProductPriceUseCase(product.productId, 4000L, adminUser)
        assertTrue(changePriceResult is Result.Success)

        // Verify cart item price remains snapshot 3500
        val activeCart = getActiveDraftCartUseCase().first()!!
        assertEquals(1, activeCart.itemCount)
        assertEquals(3500L, activeCart.items[0].unitPrice)
        assertEquals(7000L, activeCart.items[0].subtotal)
        assertEquals(7000L, activeCart.totalAmount)
    }

    @Test
    fun addProduct_aggregatesDuplicateCountItems() = runTest {
        val createResult = createProductUseCase(
            categoryId = "cat-1",
            name = "Aqua 600ml",
            barcode = "8992222",
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "botol",
            stockUnit = "botol",
            currentPrice = 4000L,
            user = adminUser
        ) as Result.Success

        val product = createResult.data

        // Add 1
        addProductToDraftCartUseCase(product.productId, 1L, cashierUser.userId)
        // Add 2 more
        val addAgainResult = addProductToDraftCartUseCase(product.productId, 2L, cashierUser.userId) as Result.Success

        val cart = addAgainResult.data
        assertEquals(1, cart.itemCount) // Single aggregated row
        assertEquals(3L, cart.items[0].quantity)
        assertEquals(12000L, cart.items[0].subtotal)
        assertEquals(12000L, cart.totalAmount)
    }

    @Test
    fun updateQuantity_recalculatesSubtotalAndTotal() = runTest {
        val createResult = createProductUseCase(
            categoryId = "cat-1",
            name = "Teh Pucuk",
            barcode = "8993333",
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "botol",
            stockUnit = "botol",
            currentPrice = 4000L,
            user = adminUser
        ) as Result.Success

        val addResult = addProductToDraftCartUseCase(createResult.data.productId, 2L, cashierUser.userId) as Result.Success
        val itemId = addResult.data.items[0].itemId

        // Update quantity from 2 to 5
        val updateResult = updateDraftCartItemQuantityUseCase(itemId, 5L) as Result.Success
        val updatedCart = updateResult.data
        assertEquals(5L, updatedCart.items[0].quantity)
        assertEquals(20000L, updatedCart.items[0].subtotal)
        assertEquals(20000L, updatedCart.totalAmount)

        // Update quantity to 0 removes item
        val zeroResult = updateDraftCartItemQuantityUseCase(itemId, 0L) as Result.Success
        assertTrue(zeroResult.data.isEmpty)
        assertEquals(0L, zeroResult.data.totalAmount)
    }

    @Test
    fun removeItem_andClearDraft_updatesCartCorrectly() = runTest {
        val prod1 = (createProductUseCase(
            categoryId = "cat-1",
            name = "Roti Tawar",
            barcode = "8994444",
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "bks",
            stockUnit = "bks",
            currentPrice = 15000L,
            user = adminUser
        ) as Result.Success).data

        val prod2 = (createProductUseCase(
            categoryId = "cat-1",
            name = "Selai Coklat",
            barcode = "8995555",
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "btl",
            stockUnit = "btl",
            currentPrice = 20000L,
            user = adminUser
        ) as Result.Success).data

        addProductToDraftCartUseCase(prod1.productId, 1L, cashierUser.userId)
        val add2 = addProductToDraftCartUseCase(prod2.productId, 1L, cashierUser.userId) as Result.Success

        val cart = add2.data
        assertEquals(2, cart.itemCount)
        assertEquals(35000L, cart.totalAmount)

        // Remove item 1
        val item1Id = cart.items.find { it.productId == prod1.productId }!!.itemId
        val removeResult = removeDraftCartItemUseCase(item1Id) as Result.Success
        assertEquals(1, removeResult.data.itemCount)
        assertEquals(20000L, removeResult.data.totalAmount)

        // Clear cart
        val clearResult = clearDraftCartUseCase(cart.transaction.transactionId)
        assertTrue(clearResult is Result.Success)
        val emptyCart = getActiveDraftCartUseCase().first()!!
        assertTrue(emptyCart.isEmpty)
        assertEquals(0L, emptyCart.totalAmount)
    }

    @Test
    fun inactiveProduct_cannotBeAddedToCart() = runTest {
        val createResult = (createProductUseCase(
            categoryId = "cat-1",
            name = "Barang Nonaktif",
            barcode = "8999999",
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "pcs",
            stockUnit = "pcs",
            currentPrice = 10000L,
            user = adminUser
        ) as Result.Success).data

        deactivateProductUseCase(createResult.productId, adminUser)

        val addResult = addProductToDraftCartUseCase(createResult.productId, 1L, cashierUser.userId)
        assertTrue(addResult is Result.Error)
        assertTrue((addResult as Result.Error).error is AppError.Validation)
    }

    @Test
    fun unknownBarcodeRegistrationFlow_preservesActiveDraftCart() = runTest {
        // 1. Cashier already has an active cart with an item
        val item1 = (createProductUseCase(
            categoryId = "cat-1",
            name = "Sabun Cuci",
            barcode = "899112233",
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "pcs",
            stockUnit = "pcs",
            currentPrice = 5000L,
            user = adminUser
        ) as Result.Success).data

        addProductToDraftCartUseCase(item1.productId, 1L, cashierUser.userId)

        // 2. An unknown barcode is encountered and registered by Admin
        val registered = (createProductUseCase(
            categoryId = "cat-1",
            name = "Sikat Gigi Baru",
            barcode = "8999998888",
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "pcs",
            stockUnit = "pcs",
            currentPrice = 8000L,
            user = adminUser
        ) as Result.Success).data

        // 3. Newly registered product is auto-added to the same existing draft cart
        val autoAddResult = addProductToDraftCartUseCase(registered.productId, 1L, cashierUser.userId) as Result.Success
        val finalCart = autoAddResult.data

        // 4. Cart preserved: now contains both original item and new item
        assertEquals(2, finalCart.itemCount)
        assertEquals(13000L, finalCart.totalAmount)
        assertEquals("Sabun Cuci", finalCart.items[0].productNameSnapshot)
        assertEquals("Sikat Gigi Baru", finalCart.items[1].productNameSnapshot)
    }
}
