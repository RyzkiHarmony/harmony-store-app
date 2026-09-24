package com.harmony.tokoharmony.domain.usecase

import com.harmony.tokoharmony.core.common.AppError
import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.model.MovementType
import com.harmony.tokoharmony.domain.model.PaymentMethod
import com.harmony.tokoharmony.domain.model.PaymentStatus
import com.harmony.tokoharmony.domain.model.PricingMethod
import com.harmony.tokoharmony.domain.model.Product
import com.harmony.tokoharmony.domain.model.ProductKind
import com.harmony.tokoharmony.domain.model.QuantityType
import com.harmony.tokoharmony.domain.model.ReferenceType
import com.harmony.tokoharmony.domain.model.Role
import com.harmony.tokoharmony.domain.model.StockMovement
import com.harmony.tokoharmony.domain.model.Transaction
import com.harmony.tokoharmony.domain.model.TransactionItem
import com.harmony.tokoharmony.domain.model.TransactionStatus
import com.harmony.tokoharmony.domain.model.User
import com.harmony.tokoharmony.domain.repository.ProductRepository
import com.harmony.tokoharmony.domain.repository.StockRepository
import com.harmony.tokoharmony.domain.repository.TransactionRepository
import com.harmony.tokoharmony.domain.usecase.cart.AddProductToDraftCartUseCase
import com.harmony.tokoharmony.domain.usecase.cart.GetActiveDraftCartUseCase
import com.harmony.tokoharmony.domain.usecase.cart.GetOrCreateDraftCartUseCase
import com.harmony.tokoharmony.domain.usecase.payment.CalculateCashChangeUseCase
import com.harmony.tokoharmony.domain.usecase.payment.CompleteCashTransactionUseCase
import com.harmony.tokoharmony.domain.usecase.payment.CompleteQrisTransactionUseCase
import com.harmony.tokoharmony.domain.usecase.price.ChangeProductPriceUseCase
import com.harmony.tokoharmony.domain.usecase.product.CreateProductUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class FakeStockRepository : StockRepository {
    private val movements = mutableListOf<StockMovement>()
    private val movementsFlow = MutableStateFlow<List<StockMovement>>(emptyList())

    override suspend fun getCurrentStock(productId: String): Long {
        return movements.filter { it.productId == productId }.sumOf { it.quantityDelta }
    }

    override suspend fun insertStockMovement(movement: StockMovement): Result<Unit> {
        movements.add(movement)
        movementsFlow.value = movements.toList()
        return Result.Success(Unit)
    }

    override suspend fun insertStockMovements(newMovements: List<StockMovement>): Result<Unit> {
        movements.addAll(newMovements)
        movementsFlow.value = movements.toList()
        return Result.Success(Unit)
    }

    override fun getMovementsForProduct(productId: String): Flow<List<StockMovement>> {
        return movementsFlow.map { list -> list.filter { it.productId == productId } }
    }

    override fun getAllStockMovements(): Flow<List<StockMovement>> = movementsFlow
    override suspend fun hasInitialStock(productId: String): Boolean = movements.any { it.productId == productId && it.movementType == MovementType.INITIAL_STOCK }
    override suspend fun recordInitialStock(productId: String, initialStock: Long, userId: String): Result<Unit> = Result.Success(Unit)
    override suspend fun recordStockIn(productId: String, purchaseQuantity: Long, purchaseUnit: String, conversionFactor: Long, note: String?, userId: String): Result<com.harmony.tokoharmony.domain.model.StockIn> = Result.Success(com.harmony.tokoharmony.domain.model.StockIn("si-1", productId, purchaseQuantity, purchaseUnit, conversionFactor, purchaseQuantity * conversionFactor, note, 0L, userId))
    override suspend fun recordStockAdjustment(productId: String, physicalQuantity: Long, reason: String, note: String?, userId: String): Result<com.harmony.tokoharmony.domain.model.StockAdjustment> = Result.Success(com.harmony.tokoharmony.domain.model.StockAdjustment("adj-1", productId, 0L, physicalQuantity, physicalQuantity, reason, note, 0L, userId))
    override fun getAllStockIns(): Flow<List<com.harmony.tokoharmony.domain.model.StockIn>> = kotlinx.coroutines.flow.flowOf(emptyList())
    override fun getAllStockAdjustments(): Flow<List<com.harmony.tokoharmony.domain.model.StockAdjustment>> = kotlinx.coroutines.flow.flowOf(emptyList())
    override fun getStockInsForProduct(productId: String): Flow<List<com.harmony.tokoharmony.domain.model.StockIn>> = kotlinx.coroutines.flow.flowOf(emptyList())
    override fun getAdjustmentsForProduct(productId: String): Flow<List<com.harmony.tokoharmony.domain.model.StockAdjustment>> = kotlinx.coroutines.flow.flowOf(emptyList())

    fun getAllMovements(): List<StockMovement> = movements.toList()
}

class FakeAtomicTransactionRepository(
    private val productRepository: ProductRepository,
    private val stockRepository: StockRepository
) : TransactionRepository {

    private val transactionsMap = mutableMapOf<String, Transaction>()
    private val itemsMap = mutableMapOf<String, MutableList<TransactionItem>>()
    private val activeDraftFlow = MutableStateFlow<com.harmony.tokoharmony.domain.model.DraftCart?>(null)

    private fun updateDraftFlow() {
        val draft = transactionsMap.values.find { it.transactionStatus == TransactionStatus.DRAFT }
        if (draft == null) {
            activeDraftFlow.value = null
        } else {
            val items = itemsMap[draft.transactionId] ?: emptyList()
            activeDraftFlow.value = com.harmony.tokoharmony.domain.model.DraftCart(
                transaction = draft.copy(totalAmount = items.sumOf { it.subtotal }),
                items = items.toList()
            )
        }
    }

    override fun getActiveDraftCart(): Flow<com.harmony.tokoharmony.domain.model.DraftCart?> = activeDraftFlow.asStateFlow()

    override suspend fun getOrCreateActiveDraft(userId: String): com.harmony.tokoharmony.domain.model.DraftCart {
        var draft = transactionsMap.values.find { it.transactionStatus == TransactionStatus.DRAFT }
        if (draft == null) {
            val txId = UUID.randomUUID().toString()
            draft = Transaction(
                transactionId = txId,
                transactionNumber = "TRX-20260924-${(100..999).random()}",
                transactionStatus = TransactionStatus.DRAFT,
                paymentStatus = PaymentStatus.UNPAID,
                totalAmount = 0L,
                createdAt = System.currentTimeMillis(),
                createdBy = userId
            )
            transactionsMap[txId] = draft
            itemsMap[txId] = mutableListOf()
            updateDraftFlow()
        }
        val items = itemsMap[draft.transactionId] ?: emptyList()
        return com.harmony.tokoharmony.domain.model.DraftCart(draft, items)
    }

    override suspend fun addItemToDraft(productId: String, quantity: Long, userId: String): Result<com.harmony.tokoharmony.domain.model.DraftCart> {
        val product = productRepository.getProductById(productId)
            ?: return Result.Error(AppError.NotFound("Produk tidak ditemukan"))
        if (!product.isActive) return Result.Error(AppError.Validation("Produk nonaktif"))

        val draftCart = getOrCreateActiveDraft(userId)
        val list = itemsMap.getOrPut(draftCart.transaction.transactionId) { mutableListOf() }
        val isGram = product.quantityType == QuantityType.GRAM

        val existingIdx = list.indexOfFirst { it.productId == productId }
        if (existingIdx >= 0) {
            val existing = list[existingIdx]
            val newQty = existing.quantity + quantity
            val newSubtotal = if (isGram) (newQty * existing.unitPrice) / 1000L else newQty * existing.unitPrice
            list[existingIdx] = existing.copy(quantity = newQty, subtotal = newSubtotal)
        } else {
            val subtotal = if (isGram) (quantity * product.currentPrice) / 1000L else quantity * product.currentPrice
            val newItem = TransactionItem(
                itemId = UUID.randomUUID().toString(),
                transactionId = draftCart.transaction.transactionId,
                productId = productId,
                productNameSnapshot = product.name,
                quantity = quantity,
                unitPrice = product.currentPrice,
                subtotal = subtotal,
                sellingUnit = product.sellingUnit
            )
            list.add(newItem)
        }

        val newTotal = list.sumOf { it.subtotal }
        transactionsMap[draftCart.transaction.transactionId] = draftCart.transaction.copy(totalAmount = newTotal)
        updateDraftFlow()
        return Result.Success(activeDraftFlow.value!!)
    }

    override suspend fun updateDraftItemQuantity(itemId: String, newQuantity: Long): Result<com.harmony.tokoharmony.domain.model.DraftCart> {
        if (newQuantity <= 0) return removeDraftItem(itemId)
        val draft = transactionsMap.values.find { it.transactionStatus == TransactionStatus.DRAFT }
            ?: return Result.Error(AppError.NotFound("Draft tidak ada"))
        val list = itemsMap[draft.transactionId] ?: return Result.Error(AppError.NotFound("Item tidak ada"))
        val idx = list.indexOfFirst { it.itemId == itemId }
        if (idx < 0) return Result.Error(AppError.NotFound("Item tidak ada"))

        val item = list[idx]
        val product = productRepository.getProductById(item.productId)
        val isGram = product?.quantityType == QuantityType.GRAM
        val subtotal = if (isGram) (newQuantity * item.unitPrice) / 1000L else newQuantity * item.unitPrice
        list[idx] = item.copy(quantity = newQuantity, subtotal = subtotal)
        val newTotal = list.sumOf { it.subtotal }
        transactionsMap[draft.transactionId] = draft.copy(totalAmount = newTotal)
        updateDraftFlow()
        return Result.Success(activeDraftFlow.value!!)
    }

    override suspend fun removeDraftItem(itemId: String): Result<com.harmony.tokoharmony.domain.model.DraftCart> {
        val draft = transactionsMap.values.find { it.transactionStatus == TransactionStatus.DRAFT }
            ?: return Result.Error(AppError.NotFound("Draft tidak ada"))
        val list = itemsMap[draft.transactionId] ?: return Result.Error(AppError.NotFound("Item tidak ada"))
        list.removeAll { it.itemId == itemId }
        val newTotal = list.sumOf { it.subtotal }
        transactionsMap[draft.transactionId] = draft.copy(totalAmount = newTotal)
        updateDraftFlow()
        return Result.Success(activeDraftFlow.value!!)
    }

    override suspend fun clearDraft(transactionId: String): Result<Unit> {
        itemsMap[transactionId]?.clear()
        transactionsMap[transactionId] = transactionsMap[transactionId]!!.copy(totalAmount = 0L)
        updateDraftFlow()
        return Result.Success(Unit)
    }

    override suspend fun getTransactionById(transactionId: String): Transaction? = transactionsMap[transactionId]

    override suspend fun getItemsForTransaction(transactionId: String): List<TransactionItem> = itemsMap[transactionId] ?: emptyList()

    override suspend fun completeCashTransaction(
        transactionId: String,
        amountReceived: Long,
        userId: String
    ): Result<Transaction> {
        return completeTransaction(transactionId, PaymentMethod.CASH, amountReceived, userId)
    }

    override suspend fun completeQrisTransaction(
        transactionId: String,
        userId: String
    ): Result<Transaction> {
        return completeTransaction(transactionId, PaymentMethod.QRIS, null, userId)
    }

    private suspend fun completeTransaction(
        transactionId: String,
        paymentMethod: PaymentMethod,
        amountReceived: Long?,
        userId: String
    ): Result<Transaction> {
        val tx = transactionsMap[transactionId]
            ?: return Result.Error(AppError.NotFound("Transaksi tidak ditemukan"))
        if (tx.transactionStatus != TransactionStatus.DRAFT) {
            return Result.Error(AppError.Validation("Hanya transaksi DRAFT yang dapat diselesaikan"))
        }

        val items = itemsMap[transactionId] ?: emptyList()
        if (items.isEmpty()) {
            return Result.Error(AppError.Validation("Keranjang kosong"))
        }

        val stockMovementsToInsert = mutableListOf<StockMovement>()
        val now = System.currentTimeMillis()

        for (item in items) {
            val product = productRepository.getProductById(item.productId)
                ?: return Result.Error(AppError.NotFound("Produk tidak ditemukan"))
            if (!product.isActive) {
                return Result.Error(AppError.Validation("Produk ${product.name} nonaktif"))
            }

            if (product.productKind == ProductKind.PHYSICAL) {
                val currentStock = stockRepository.getCurrentStock(product.productId)
                if (item.quantity > currentStock) {
                    return Result.Error(AppError.Validation("Stok tidak mencukupi untuk ${product.name}. Tersedia: $currentStock, diminta: ${item.quantity}"))
                }
                stockMovementsToInsert.add(
                    StockMovement(
                        movementId = UUID.randomUUID().toString(),
                        productId = product.productId,
                        movementType = MovementType.SALE,
                        quantityDelta = -item.quantity,
                        referenceType = ReferenceType.TRANSACTION,
                        referenceId = transactionId,
                        reason = "Penjualan transaksi ${tx.transactionNumber}",
                        createdAt = now,
                        createdBy = userId
                    )
                )
            }
        }

        val totalAmount = items.sumOf { it.subtotal }
        val finalReceived = when (paymentMethod) {
            PaymentMethod.CASH -> {
                if (amountReceived == null || amountReceived < totalAmount) {
                    return Result.Error(AppError.Validation("Uang diterima kurang dari total"))
                }
                amountReceived
            }
            PaymentMethod.QRIS -> totalAmount
        }
        val finalChange = if (paymentMethod == PaymentMethod.CASH) finalReceived - totalAmount else 0L

        // Atomic commit
        val completedTx = tx.copy(
            transactionStatus = TransactionStatus.COMPLETED,
            paymentStatus = PaymentStatus.PAID,
            paymentMethod = paymentMethod,
            totalAmount = totalAmount,
            amountReceived = finalReceived,
            changeAmount = finalChange,
            completedAt = now
        )
        transactionsMap[transactionId] = completedTx
        stockRepository.insertStockMovements(stockMovementsToInsert)
        updateDraftFlow()

        return Result.Success(completedTx)
    }

    override fun getAllTransactions(): Flow<List<Transaction>> = kotlinx.coroutines.flow.flowOf(transactionsMap.values.toList())

    override suspend fun cancelTransaction(transactionId: String, reason: String, userId: String): Result<Transaction> {
        val tx = transactionsMap[transactionId] ?: return Result.Error(AppError.NotFound("Transaksi tidak ditemukan"))
        val updated = tx.copy(transactionStatus = TransactionStatus.CANCELLED, cancellationReason = reason, cancelledBy = userId, cancelledAt = System.currentTimeMillis())
        transactionsMap[transactionId] = updated
        return Result.Success(updated)
    }
}

class CompleteTransactionAtomicityTest {

    private lateinit var productRepository: FakeProductRepository
    private lateinit var priceRepository: FakePriceRepository
    private lateinit var stockRepository: FakeStockRepository
    private lateinit var transactionRepository: FakeAtomicTransactionRepository

    private lateinit var createProductUseCase: CreateProductUseCase
    private lateinit var changeProductPriceUseCase: ChangeProductPriceUseCase
    private lateinit var getActiveDraftCartUseCase: GetActiveDraftCartUseCase
    private lateinit var getOrCreateDraftCartUseCase: GetOrCreateDraftCartUseCase
    private lateinit var addProductToDraftCartUseCase: AddProductToDraftCartUseCase
    private lateinit var calculateCashChangeUseCase: CalculateCashChangeUseCase
    private lateinit var completeCashTransactionUseCase: CompleteCashTransactionUseCase
    private lateinit var completeQrisTransactionUseCase: CompleteQrisTransactionUseCase

    private val adminUser = User("admin-1", "Admin", Role.ADMIN, "hash")
    private val cashierUser = User("cashier-1", "Kasir", Role.CASHIER, null)

    @Before
    fun setup() {
        productRepository = FakeProductRepository()
        priceRepository = FakePriceRepository(productRepository)
        stockRepository = FakeStockRepository()
        transactionRepository = FakeAtomicTransactionRepository(productRepository, stockRepository)

        createProductUseCase = CreateProductUseCase(productRepository)
        changeProductPriceUseCase = ChangeProductPriceUseCase(productRepository, priceRepository)
        getActiveDraftCartUseCase = GetActiveDraftCartUseCase(transactionRepository)
        getOrCreateDraftCartUseCase = GetOrCreateDraftCartUseCase(transactionRepository)
        addProductToDraftCartUseCase = AddProductToDraftCartUseCase(transactionRepository)
        calculateCashChangeUseCase = CalculateCashChangeUseCase()
        completeCashTransactionUseCase = CompleteCashTransactionUseCase(transactionRepository)
        completeQrisTransactionUseCase = CompleteQrisTransactionUseCase(transactionRepository)
    }

    // 1. cash payment exact amount
    @Test
    fun cashPayment_exactAmount_completesSuccessfullyWithZeroChange() = runTest {
        val prod = createPhysicalProduct("Indomie", 3500L, initialStock = 10L)
        val draft = getOrCreateDraftCartUseCase(cashierUser.userId)
        addProductToDraftCartUseCase(prod.productId, 2L, cashierUser.userId) // Total 7000

        val result = completeCashTransactionUseCase(draft.transaction.transactionId, 7000L, cashierUser.userId)
        assertTrue(result is Result.Success)
        val completed = (result as Result.Success).data

        assertEquals(TransactionStatus.COMPLETED, completed.transactionStatus)
        assertEquals(PaymentStatus.PAID, completed.paymentStatus)
        assertEquals(PaymentMethod.CASH, completed.paymentMethod)
        assertEquals(7000L, completed.totalAmount)
        assertEquals(7000L, completed.amountReceived)
        assertEquals(0L, completed.changeAmount)
        assertNotNull(completed.completedAt)
    }

    // 2. cash payment with change
    @Test
    fun cashPayment_withChange_completesSuccessfullyWithAccurateChange() = runTest {
        val prod = createPhysicalProduct("Minyak 1L", 18000L, initialStock = 10L)
        val draft = getOrCreateDraftCartUseCase(cashierUser.userId)
        addProductToDraftCartUseCase(prod.productId, 1L, cashierUser.userId)

        val result = completeCashTransactionUseCase(draft.transaction.transactionId, 50000L, cashierUser.userId)
        assertTrue(result is Result.Success)
        val completed = (result as Result.Success).data

        assertEquals(18000L, completed.totalAmount)
        assertEquals(50000L, completed.amountReceived)
        assertEquals(32000L, completed.changeAmount)
    }

    // 3. cash payment insufficient
    @Test
    fun cashPayment_insufficientAmount_rejectedAndRemainsDraft() = runTest {
        val prod = createPhysicalProduct("Beras 5kg", 65000L, initialStock = 10L)
        val draft = getOrCreateDraftCartUseCase(cashierUser.userId)
        addProductToDraftCartUseCase(prod.productId, 1L, cashierUser.userId)

        val result = completeCashTransactionUseCase(draft.transaction.transactionId, 60000L, cashierUser.userId)
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.Validation)

        // Verify transaction remains DRAFT & UNPAID
        val tx = transactionRepository.getTransactionById(draft.transaction.transactionId)!!
        assertEquals(TransactionStatus.DRAFT, tx.transactionStatus)
        assertEquals(PaymentStatus.UNPAID, tx.paymentStatus)
        assertEquals(10L, stockRepository.getCurrentStock(prod.productId)) // Stock unchanged
    }

    // 4. QRIS remains unpaid before confirmation
    @Test
    fun qris_beforeConfirmation_remainsDraftAndUnpaid() = runTest {
        val prod = createPhysicalProduct("Teh Pucuk", 4000L, initialStock = 20L)
        val draft = getOrCreateDraftCartUseCase(cashierUser.userId)
        addProductToDraftCartUseCase(prod.productId, 2L, cashierUser.userId)

        // Selecting QRIS in UI does not call complete until cashier clicks "PEMBAYARAN BERHASIL"
        val tx = transactionRepository.getTransactionById(draft.transaction.transactionId)!!
        assertEquals(TransactionStatus.DRAFT, tx.transactionStatus)
        assertEquals(PaymentStatus.UNPAID, tx.paymentStatus)
        assertEquals(20L, stockRepository.getCurrentStock(prod.productId))
    }

    // 5. QRIS completes after confirmation
    @Test
    fun qris_afterConfirmation_completesWithPaidStatus() = runTest {
        val prod = createPhysicalProduct("Susu UHT", 6000L, initialStock = 10L)
        val draft = getOrCreateDraftCartUseCase(cashierUser.userId)
        addProductToDraftCartUseCase(prod.productId, 2L, cashierUser.userId) // Total 12000

        val result = completeQrisTransactionUseCase(draft.transaction.transactionId, cashierUser.userId)
        assertTrue(result is Result.Success)
        val completed = (result as Result.Success).data

        assertEquals(TransactionStatus.COMPLETED, completed.transactionStatus)
        assertEquals(PaymentStatus.PAID, completed.paymentStatus)
        assertEquals(PaymentMethod.QRIS, completed.paymentMethod)
        assertEquals(12000L, completed.totalAmount)
        assertEquals(12000L, completed.amountReceived)
        assertEquals(0L, completed.changeAmount)
    }

    // 6. empty cart cannot complete
    @Test
    fun completeTransaction_emptyCart_rejected() = runTest {
        val draft = getOrCreateDraftCartUseCase(cashierUser.userId)
        val result = completeCashTransactionUseCase(draft.transaction.transactionId, 10000L, cashierUser.userId)
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.Validation)
    }

    // 7. insufficient physical stock cannot complete
    @Test
    fun completeTransaction_insufficientStock_rejectedAndStockUnaffected() = runTest {
        val prod = createPhysicalProduct("Kecap", 10000L, initialStock = 2L)
        val draft = getOrCreateDraftCartUseCase(cashierUser.userId)
        // Add 5 items to cart (but store only has 2)
        addProductToDraftCartUseCase(prod.productId, 5L, cashierUser.userId)

        val result = completeCashTransactionUseCase(draft.transaction.transactionId, 50000L, cashierUser.userId)
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.Validation)

        // Transaction remains draft and stock remains 2
        val tx = transactionRepository.getTransactionById(draft.transaction.transactionId)!!
        assertEquals(TransactionStatus.DRAFT, tx.transactionStatus)
        assertEquals(2L, stockRepository.getCurrentStock(prod.productId))
    }

    // 8. completed sale creates SALE movement
    @Test
    fun completeSale_createsNegativeSaleStockMovement() = runTest {
        val prod = createPhysicalProduct("Sabun Mandi", 4000L, initialStock = 20L)
        val draft = getOrCreateDraftCartUseCase(cashierUser.userId)
        addProductToDraftCartUseCase(prod.productId, 3L, cashierUser.userId)

        val result = completeCashTransactionUseCase(draft.transaction.transactionId, 12000L, cashierUser.userId)
        assertTrue(result is Result.Success)

        // Stock decreased by 3 -> now 17
        assertEquals(17L, stockRepository.getCurrentStock(prod.productId))

        val movements = stockRepository.getAllMovements().filter { it.productId == prod.productId }
        assertEquals(2, movements.size) // 1 INITIAL_STOCK (+20), 1 SALE (-3)
        val sale = movements.find { it.movementType == MovementType.SALE }!!
        assertEquals(-3L, sale.quantityDelta)
        assertEquals(ReferenceType.TRANSACTION, sale.referenceType)
        assertEquals(draft.transaction.transactionId, sale.referenceId)
    }

    // 9. digital product creates no SALE movement
    @Test
    fun completeSale_digitalProduct_createsNoStockMovement() = runTest {
        val digitalProd = createDigitalProduct("Token PLN 50rb", 52000L)
        val draft = getOrCreateDraftCartUseCase(cashierUser.userId)
        addProductToDraftCartUseCase(digitalProd.productId, 1L, cashierUser.userId)

        val result = completeCashTransactionUseCase(draft.transaction.transactionId, 52000L, cashierUser.userId)
        assertTrue(result is Result.Success)

        // Zero stock movements created for digital product
        val movements = stockRepository.getAllMovements().filter { it.productId == digitalProd.productId }
        assertTrue(movements.isEmpty())
    }

    // 10. mixed physical + digital transaction
    @Test
    fun completeSale_mixedPhysicalAndDigital_createsStockMovementOnlyForPhysical() = runTest {
        val physicalProd = createPhysicalProduct("Roti", 15000L, initialStock = 10L)
        val digitalProd = createDigitalProduct("Pulsa Telkomsel 10rb", 12000L)

        val draft = getOrCreateDraftCartUseCase(cashierUser.userId)
        addProductToDraftCartUseCase(physicalProd.productId, 2L, cashierUser.userId)
        addProductToDraftCartUseCase(digitalProd.productId, 1L, cashierUser.userId) // Total = 30000 + 12000 = 42000

        val result = completeCashTransactionUseCase(draft.transaction.transactionId, 50000L, cashierUser.userId)
        assertTrue(result is Result.Success)
        val completed = (result as Result.Success).data
        assertEquals(42000L, completed.totalAmount)
        assertEquals(8000L, completed.changeAmount)

        // Physical stock reduced by 2 -> 8
        assertEquals(8L, stockRepository.getCurrentStock(physicalProd.productId))
        // Digital has 0 movements
        assertEquals(0, stockRepository.getAllMovements().filter { it.productId == digitalProd.productId }.size)
    }

    // 11, 12, 13, 14, 15, 16. Atomicity, Snapshot, PAID & COMPLETED status, Draft rotation
    @Test
    fun completeSale_fullLifecycleAndInvariants() = runTest {
        val telur = (createProductUseCase(
            categoryId = "cat-1",
            name = "Telur Curah",
            barcode = null,
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_KG,
            quantityType = QuantityType.GRAM,
            sellingUnit = "kg",
            stockUnit = "kg",
            currentPrice = 30000L,
            user = adminUser
        ) as Result.Success).data
        stockRepository.insertStockMovement(
            StockMovement(
                movementId = UUID.randomUUID().toString(),
                productId = telur.productId,
                movementType = MovementType.INITIAL_STOCK,
                quantityDelta = 10000L, // 10 kg
                createdAt = System.currentTimeMillis(),
                createdBy = adminUser.userId
            )
        )

        val draft = getOrCreateDraftCartUseCase(cashierUser.userId)
        // Add 1.5 kg (1500g) -> (1500 * 30000) / 1000 = 45000
        addProductToDraftCartUseCase(telur.productId, 1500L, cashierUser.userId)

        // Master price changed after adding to cart
        changeProductPriceUseCase(telur.productId, 35000L, adminUser)

        // Complete sale with 50000 cash
        val result = completeCashTransactionUseCase(draft.transaction.transactionId, 50000L, cashierUser.userId)
        assertTrue(result is Result.Success)
        val completed = (result as Result.Success).data

        // 13. Snapshot price preserved: subtotal was 45000, NOT 52500
        assertEquals(45000L, completed.totalAmount)
        assertEquals(5000L, completed.changeAmount)

        // 14 & 15. Status invariants
        assertEquals(TransactionStatus.COMPLETED, completed.transactionStatus)
        assertEquals(PaymentStatus.PAID, completed.paymentStatus)

        // Stock reduced from 10000g by 1500g -> 8500g (8.5 kg)
        assertEquals(8500L, stockRepository.getCurrentStock(telur.productId))

        // 16. Active draft is no longer the completed transaction
        val activeDraft = getActiveDraftCartUseCase().first()
        assertNull(activeDraft)

        // Next cashier transaction creates a brand new draft
        val newDraft = getOrCreateDraftCartUseCase(cashierUser.userId)
        assertTrue(newDraft.transaction.transactionId != completed.transactionId)
        assertTrue(newDraft.isEmpty)
    }

    private suspend fun createPhysicalProduct(name: String, price: Long, initialStock: Long): Product {
        val prod = (createProductUseCase(
            categoryId = "cat-1",
            name = name,
            barcode = "899${(100000..999999).random()}",
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "pcs",
            stockUnit = "pcs",
            currentPrice = price,
            user = adminUser
        ) as Result.Success).data

        if (initialStock > 0) {
            stockRepository.insertStockMovement(
                StockMovement(
                    movementId = UUID.randomUUID().toString(),
                    productId = prod.productId,
                    movementType = MovementType.INITIAL_STOCK,
                    quantityDelta = initialStock,
                    createdAt = System.currentTimeMillis(),
                    createdBy = adminUser.userId
                )
            )
        }
        return prod
    }

    private suspend fun createDigitalProduct(name: String, price: Long): Product {
        return (createProductUseCase(
            categoryId = "cat-1",
            name = name,
            barcode = null,
            productKind = ProductKind.DIGITAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "trx",
            stockUnit = "trx",
            currentPrice = price,
            user = adminUser
        ) as Result.Success).data
    }
}
