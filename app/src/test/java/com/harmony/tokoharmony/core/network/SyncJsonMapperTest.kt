package com.harmony.tokoharmony.core.network

import com.harmony.tokoharmony.domain.model.Category
import com.harmony.tokoharmony.domain.model.DigitalTransaction
import com.harmony.tokoharmony.domain.model.PaymentMethod
import com.harmony.tokoharmony.domain.model.PaymentStatus
import com.harmony.tokoharmony.domain.model.PriceHistory
import com.harmony.tokoharmony.domain.model.PricingMethod
import com.harmony.tokoharmony.domain.model.Product
import com.harmony.tokoharmony.domain.model.ProductKind
import com.harmony.tokoharmony.domain.model.QuantityType
import com.harmony.tokoharmony.domain.model.Role
import com.harmony.tokoharmony.domain.model.StockAdjustment
import com.harmony.tokoharmony.domain.model.StockIn
import com.harmony.tokoharmony.domain.model.StockMovement
import com.harmony.tokoharmony.domain.model.MovementType
import com.harmony.tokoharmony.domain.model.SyncEntityType
import com.harmony.tokoharmony.domain.model.SyncOperation
import com.harmony.tokoharmony.domain.model.SyncQueueItem
import com.harmony.tokoharmony.domain.model.SyncStatus
import com.harmony.tokoharmony.domain.model.Transaction
import com.harmony.tokoharmony.domain.model.TransactionItem
import com.harmony.tokoharmony.domain.model.TransactionStatus
import com.harmony.tokoharmony.domain.model.User
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncJsonMapperTest {

    @Test
    fun userToJson_serializesCorrectly() {
        val user = User(
            userId = "usr-1",
            displayName = "Admin Toko",
            role = Role.ADMIN,
            pinHash = "hash123",
            isActive = true,
            createdAt = 1700000000000L,
            updatedAt = 1700000000000L
        )

        val jsonStr = SyncJsonMapper.userToJson(user)
        val json = JSONObject(jsonStr)

        assertEquals("usr-1", json.getString("userId"))
        assertEquals("Admin Toko", json.getString("displayName"))
        assertEquals("ADMIN", json.getString("role"))
        assertEquals("hash123", json.getString("pinHash"))
        assertTrue(json.getBoolean("isActive"))
    }

    @Test
    fun productToJson_serializesCorrectly() {
        val product = Product(
            productId = "prod-1",
            categoryId = "cat-1",
            name = "Beras Pandan Wangi",
            barcode = "8991234567890",
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_KG,
            quantityType = QuantityType.GRAM,
            sellingUnit = "kg",
            stockUnit = "gram",
            purchaseUnit = "Karung",
            purchaseConversionFactor = 25000,
            currentPrice = 14000L,
            minimumStock = 5000,
            isActive = true,
            createdAt = 1700000000000L,
            updatedAt = 1700000000000L
        )

        val jsonStr = SyncJsonMapper.productToJson(product)
        val json = JSONObject(jsonStr)

        assertEquals("prod-1", json.getString("productId"))
        assertEquals("Beras Pandan Wangi", json.getString("name"))
        assertEquals(14000L, json.getLong("currentPrice"))
        assertEquals(25000, json.getInt("purchaseConversionFactor"))
        assertEquals("GRAM", json.getString("quantityType"))
    }

    @Test
    fun transactionToJson_serializesCorrectly() {
        val tx = Transaction(
            transactionId = "tx-1",
            transactionNumber = "TRX-20260924-001",
            transactionStatus = TransactionStatus.COMPLETED,
            paymentStatus = PaymentStatus.PAID,
            paymentMethod = PaymentMethod.CASH,
            totalAmount = 50000L,
            amountReceived = 100000L,
            changeAmount = 50000L,
            createdAt = 1700000000000L,
            completedAt = 1700000005000L,
            cancelledAt = null,
            createdBy = "usr-1",
            cancelledBy = null,
            cancellationReason = null
        )

        val jsonStr = SyncJsonMapper.transactionToJson(tx)
        val json = JSONObject(jsonStr)

        assertEquals("tx-1", json.getString("transactionId"))
        assertEquals("COMPLETED", json.getString("transactionStatus"))
        assertEquals("CASH", json.getString("paymentMethod"))
        assertEquals(50000L, json.getLong("totalAmount"))
        assertEquals(100000L, json.getLong("amountReceived"))
        assertEquals(50000L, json.getLong("changeAmount"))
    }

    @Test
    fun buildBatchRequestJson_createsProperBatchStructure() {
        val items = listOf(
            SyncQueueItem(
                queueId = "q-1",
                entityType = SyncEntityType.PRODUCT,
                entityId = "prod-1",
                operation = SyncOperation.CREATE,
                payload = "{\"productId\":\"prod-1\",\"name\":\"Beras\"}",
                status = SyncStatus.PENDING,
                createdAt = 1700000000000L
            ),
            SyncQueueItem(
                queueId = "q-2",
                entityType = SyncEntityType.TRANSACTION,
                entityId = "tx-1",
                operation = SyncOperation.CREATE,
                payload = "{\"transactionId\":\"tx-1\",\"totalAmount\":50000}",
                status = SyncStatus.PENDING,
                createdAt = 1700000001000L
            )
        )

        val batchJson = SyncJsonMapper.buildBatchRequestJson(
            syncId = "batch-100",
            deviceId = "test-device",
            items = items
        )

        val root = JSONObject(batchJson)
        assertEquals("batch-100", root.getString("syncId"))
        assertEquals("test-device", root.getString("deviceId"))

        val ops = root.getJSONArray("operations")
        assertEquals(2, ops.length())

        val op1 = ops.getJSONObject(0)
        assertEquals("q-1", op1.getString("queueId"))
        assertEquals("PRODUCT", op1.getString("entityType"))
        assertEquals("prod-1", op1.getString("entityId"))
        assertEquals("CREATE", op1.getString("operation"))
        assertEquals("Beras", op1.getJSONObject("payload").getString("name"))
    }

    @Test
    fun parseBatchResponse_handlesMixedResults() {
        val responseJson = """
            {
                "status": "PARTIAL_SUCCESS",
                "syncId": "batch-100",
                "totalProcessed": 2,
                "successCount": 1,
                "failureCount": 1,
                "results": [
                    {
                        "queueId": "q-1",
                        "status": "SUCCESS",
                        "error": null,
                        "retryable": false
                    },
                    {
                        "queueId": "q-2",
                        "status": "FAILED",
                        "error": "Temporary sheet lock",
                        "retryable": true
                    }
                ]
            }
        """.trimIndent()

        val result = SyncJsonMapper.parseBatchResponse(responseJson)

        assertEquals(2, result.totalProcessed)
        assertEquals(1, result.successCount)
        assertEquals(1, result.failureCount)
        assertFalse(result.isNetworkError)

        assertEquals(2, result.itemResults.size)
        val r1 = result.itemResults[0]
        assertEquals("q-1", r1.queueId)
        assertEquals("SUCCESS", r1.status)

        val r2 = result.itemResults[1]
        assertEquals("q-2", r2.queueId)
        assertEquals("FAILED", r2.status)
        assertTrue(r2.retryable)
        assertEquals("Temporary sheet lock", r2.error)
    }

    @Test
    fun parseBootstrapResponse_parsesAllEntitiesInOrder() {
        val bootstrapJson = """
            {
                "status": "SUCCESS",
                "timestamp": 1700000000000,
                "data": {
                    "users": [
                        {
                            "userId": "usr-1",
                            "displayName": "Admin Toko",
                            "role": "ADMIN",
                            "pinHash": "123456",
                            "isActive": true,
                            "createdAt": 1700000000000,
                            "updatedAt": 1700000000000
                        }
                    ],
                    "categories": [
                        {
                            "categoryId": "cat-1",
                            "name": "Sembako",
                            "isActive": true,
                            "createdAt": 1700000000000,
                            "updatedAt": 1700000000000
                        }
                    ],
                    "products": [
                        {
                            "productId": "prod-1",
                            "categoryId": "cat-1",
                            "name": "Minyak Goreng 1L",
                            "barcode": "899001",
                            "productKind": "PHYSICAL",
                            "pricingMethod": "PER_UNIT",
                            "quantityType": "COUNT",
                            "sellingUnit": "pouch",
                            "stockUnit": "pouch",
                            "purchaseUnit": "Dus",
                            "purchaseConversionFactor": 12,
                            "currentPrice": 16000,
                            "minimumStock": 5,
                            "isActive": true,
                            "createdAt": 1700000000000,
                            "updatedAt": 1700000000000
                        }
                    ],
                    "priceHistory": [
                        {
                            "historyId": "hist-1",
                            "productId": "prod-1",
                            "oldPrice": 0,
                            "newPrice": 16000,
                            "changedAt": 1700000000000,
                            "changedBy": "usr-1"
                        }
                    ],
                    "transactions": [
                        {
                            "transactionId": "tx-1",
                            "transactionNumber": "TRX-001",
                            "transactionStatus": "COMPLETED",
                            "paymentStatus": "PAID",
                            "paymentMethod": "CASH",
                            "totalAmount": 16000,
                            "amountReceived": 20000,
                            "changeAmount": 4000,
                            "createdAt": 1700000000000,
                            "completedAt": 1700000005000,
                            "cancelledAt": null,
                            "createdBy": "usr-1",
                            "cancelledBy": null,
                            "cancellationReason": null
                        }
                    ],
                    "transactionItems": [
                        {
                            "itemId": "item-1",
                            "transactionId": "tx-1",
                            "productId": "prod-1",
                            "productNameSnapshot": "Minyak Goreng 1L",
                            "quantity": 1,
                            "unitPrice": 16000,
                            "subtotal": 16000,
                            "sellingUnit": "pouch"
                        }
                    ],
                    "stockIns": [],
                    "stockAdjustments": [],
                    "stockMovements": [
                        {
                            "movementId": "mov-1",
                            "productId": "prod-1",
                            "movementType": "INITIAL_STOCK",
                            "quantityDelta": 100,
                            "referenceType": null,
                            "referenceId": null,
                            "reason": "Stok awal",
                            "createdAt": 1700000000000,
                            "createdBy": "usr-1"
                        }
                    ],
                    "digitalTransactions": []
                }
            }
        """.trimIndent()

        val data = SyncJsonMapper.parseBootstrapResponse(bootstrapJson)

        assertEquals(1, data.users.size)
        assertEquals("usr-1", data.users[0].userId)

        assertEquals(1, data.categories.size)
        assertEquals("cat-1", data.categories[0].categoryId)

        assertEquals(1, data.products.size)
        assertEquals("prod-1", data.products[0].productId)
        assertEquals(16000L, data.products[0].currentPrice)

        assertEquals(1, data.priceHistories.size)
        assertEquals(1, data.transactions.size)
        assertEquals(1, data.transactionItems.size)
        assertEquals(1, data.stockMovements.size)
        assertEquals(100, data.stockMovements[0].quantityDelta)
    }
}
