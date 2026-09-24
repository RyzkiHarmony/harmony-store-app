package com.harmony.tokoharmony.domain.usecase.transaction

import com.harmony.tokoharmony.core.common.AppError
import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.model.DraftCart
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
import com.harmony.tokoharmony.domain.repository.TransactionRepository
import com.harmony.tokoharmony.domain.usecase.inventory.FakeInventoryStockRepository
import com.harmony.tokoharmony.domain.usecase.inventory.FakeProductRepo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeFullTransactionRepository(
    private val productRepo: FakeProductRepo,
    private val stockRepo: FakeInventoryStockRepository
) : TransactionRepository {

    val transactions = mutableMapOf<String, Transaction>()
    val transactionItems = mutableMapOf<String, MutableList<TransactionItem>>()

    override fun getActiveDraftCart(): Flow<DraftCart?> = flowOf(null)
    override suspend fun getOrCreateActiveDraft(userId: String): DraftCart = throw NotImplementedError()
    override suspend fun addItemToDraft(productId: String, quantity: Long, userId: String): Result<DraftCart> = throw NotImplementedError()
    override suspend fun updateDraftItemQuantity(itemId: String, newQuantity: Long): Result<DraftCart> = throw NotImplementedError()
    override suspend fun removeDraftItem(itemId: String): Result<DraftCart> = throw NotImplementedError()
    override suspend fun clearDraft(transactionId: String): Result<Unit> = Result.Success(Unit)

    override suspend fun getTransactionById(transactionId: String): Transaction? = transactions[transactionId]
    override suspend fun getItemsForTransaction(transactionId: String): List<TransactionItem> = transactionItems[transactionId] ?: emptyList()

    override suspend fun completeCashTransaction(transactionId: String, amountReceived: Long, userId: String): Result<Transaction> = throw NotImplementedError()
    override suspend fun completeQrisTransaction(transactionId: String, userId: String): Result<Transaction> = throw NotImplementedError()

    override fun getAllTransactions(): Flow<List<Transaction>> = flowOf(transactions.values.toList())

    override suspend fun cancelTransaction(transactionId: String, reason: String, userId: String): Result<Transaction> {
        val tx = transactions[transactionId] ?: return Result.Error(AppError.NotFound("Transaksi tidak ditemukan"))
        if (tx.transactionStatus == TransactionStatus.CANCELLED) {
            return Result.Error(AppError.Validation("Transaksi sudah dibatalkan sebelumnya."))
        }
        if (tx.transactionStatus != TransactionStatus.COMPLETED) {
            return Result.Error(AppError.Validation("Hanya transaksi selesai (COMPLETED) yang dapat dibatalkan."))
        }

        val items = transactionItems[transactionId] ?: emptyList()
        val now = System.currentTimeMillis()

        for (item in items) {
            val product = productRepo.getProductById(item.productId)
            if (product != null && product.productKind == ProductKind.PHYSICAL) {
                stockRepo.movements.add(
                    StockMovement(
                        movementId = "rev-${stockRepo.movements.size + 1}",
                        productId = product.productId,
                        movementType = MovementType.SALE_REVERSAL,
                        quantityDelta = item.quantity,
                        referenceType = ReferenceType.TRANSACTION,
                        referenceId = transactionId,
                        reason = "Pembatalan transaksi ${tx.transactionNumber}: $reason",
                        createdAt = now,
                        createdBy = userId
                    )
                )
            }
        }

        val updatedTx = tx.copy(
            transactionStatus = TransactionStatus.CANCELLED,
            cancelledAt = now,
            cancelledBy = userId,
            cancellationReason = reason
        )
        transactions[transactionId] = updatedTx
        return Result.Success(updatedTx)
    }
}

class CancelTransactionUseCaseTest {

    private lateinit var stockRepository: FakeInventoryStockRepository
    private lateinit var productRepository: FakeProductRepo
    private lateinit var transactionRepository: FakeFullTransactionRepository
    private lateinit var cancelTransactionUseCase: CancelTransactionUseCase

    private val adminUser = User("admin-1", "Admin", Role.ADMIN, "hash", true, 0L, 0L)
    private val cashierUser = User("cashier-1", "Cashier", Role.CASHIER, null, true, 0L, 0L)

    @Before
    fun setUp() {
        stockRepository = FakeInventoryStockRepository()
        productRepository = FakeProductRepo()
        transactionRepository = FakeFullTransactionRepository(productRepository, stockRepository)
        cancelTransactionUseCase = CancelTransactionUseCase(transactionRepository)

        // Seed Physical Product
        productRepository.products["prod-indomie"] = Product(
            productId = "prod-indomie",
            categoryId = "cat-1",
            name = "Indomie Goreng",
            barcode = "8991234567",
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "pcs",
            stockUnit = "pcs",
            currentPrice = 3500L,
            isActive = true,
            createdAt = 0L,
            updatedAt = 0L
        )

        // Seed Digital Product
        productRepository.products["prod-pulsa"] = Product(
            productId = "prod-pulsa",
            categoryId = "cat-digital",
            name = "Pulsa 10k",
            barcode = null,
            productKind = ProductKind.DIGITAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "voucher",
            stockUnit = "voucher",
            currentPrice = 12000L,
            isActive = true,
            createdAt = 0L,
            updatedAt = 0L
        )

        // Seed Initial Stock: 100 pcs
        stockRepository.movements.add(
            StockMovement("init-1", "prod-indomie", MovementType.INITIAL_STOCK, 100L, null, null, "Awal", 0L, "admin-1")
        )

        // Seed Completed Sale: sold 5 Indomie (-5) + 1 Pulsa (0)
        stockRepository.movements.add(
            StockMovement("sale-1", "prod-indomie", MovementType.SALE, -5L, ReferenceType.TRANSACTION, "tx-completed-1", "Jual", 1000L, "cashier-1")
        )

        // Seed Completed Transaction
        transactionRepository.transactions["tx-completed-1"] = Transaction(
            transactionId = "tx-completed-1",
            transactionNumber = "TRX-20260924-001",
            transactionStatus = TransactionStatus.COMPLETED,
            paymentStatus = PaymentStatus.PAID,
            paymentMethod = PaymentMethod.CASH,
            totalAmount = 29500L, // (5 * 3500) + 12000 = 17500 + 12000 = 29500
            amountReceived = 50000L,
            changeAmount = 20500L,
            createdAt = 1000L,
            completedAt = 1000L,
            createdBy = "cashier-1"
        )

        transactionRepository.transactionItems["tx-completed-1"] = mutableListOf(
            TransactionItem("item-1", "tx-completed-1", "prod-indomie", "Indomie Goreng", 5L, 3500L, 17500L, "pcs"),
            TransactionItem("item-2", "tx-completed-1", "prod-pulsa", "Pulsa 10k", 1L, 12000L, 12000L, "voucher")
        )

        // Seed Draft Transaction
        transactionRepository.transactions["tx-draft-1"] = Transaction(
            transactionId = "tx-draft-1",
            transactionNumber = "TRX-20260924-002",
            transactionStatus = TransactionStatus.DRAFT,
            paymentStatus = PaymentStatus.UNPAID,
            totalAmount = 7000L,
            changeAmount = 0L,
            createdAt = 2000L,
            createdBy = "cashier-1"
        )
    }

    @Test
    fun completedTransaction_cancelledByAdmin_restoresPhysicalStockOnly() = runTest {
        assertEquals(95L, stockRepository.getCurrentStock("prod-indomie")) // 100 - 5 = 95

        val result = cancelTransactionUseCase("tx-completed-1", "Pelanggan salah beli", adminUser)
        assertTrue(result is Result.Success)

        val tx = (result as Result.Success).data
        assertEquals(TransactionStatus.CANCELLED, tx.transactionStatus)
        assertEquals("admin-1", tx.cancelledBy)
        assertEquals("Pelanggan salah beli", tx.cancellationReason)
        assertNotNull(tx.cancelledAt)

        // Stock restored from 95 to 100 via SALE_REVERSAL (+5)
        assertEquals(100L, stockRepository.getCurrentStock("prod-indomie"))

        val reversals = stockRepository.movements.filter { it.movementType == MovementType.SALE_REVERSAL }
        assertEquals(1, reversals.size)
        assertEquals(5L, reversals[0].quantityDelta)
        assertEquals("prod-indomie", reversals[0].productId)

        // Digital product has 0 movements
        val digitalMovements = stockRepository.movements.filter { it.productId == "prod-pulsa" }
        assertEquals(0, digitalMovements.size)

        // Transaction record & items still exist in repository
        val preservedTx = transactionRepository.getTransactionById("tx-completed-1")
        assertNotNull(preservedTx)
        val preservedItems = transactionRepository.getItemsForTransaction("tx-completed-1")
        assertEquals(2, preservedItems.size)
    }

    @Test
    fun draftTransaction_cannotBeCancelled() = runTest {
        val result = cancelTransactionUseCase("tx-draft-1", "Batal", adminUser)
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.Validation)
    }

    @Test
    fun alreadyCancelledTransaction_cannotBeCancelledAgain() = runTest {
        val first = cancelTransactionUseCase("tx-completed-1", "Alasan 1", adminUser)
        assertTrue(first is Result.Success)

        val second = cancelTransactionUseCase("tx-completed-1", "Alasan 2", adminUser)
        assertTrue(second is Result.Error)
        assertTrue((second as Result.Error).error is AppError.Validation)

        // Ensure stock was not restored twice (should stay 100, not 105)
        assertEquals(100L, stockRepository.getCurrentStock("prod-indomie"))
    }

    @Test
    fun cashier_cannotCancelTransaction_rejectedWithAuthorization() = runTest {
        val result = cancelTransactionUseCase("tx-completed-1", "Batal", cashierUser)
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.Unauthorized)
        assertEquals(95L, stockRepository.getCurrentStock("prod-indomie"))
    }

    @Test
    fun blankReason_rejected() = runTest {
        val result = cancelTransactionUseCase("tx-completed-1", "   ", adminUser)
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.Validation)
    }
}
