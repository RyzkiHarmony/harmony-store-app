package com.harmony.tokoharmony.core.network

import com.harmony.tokoharmony.domain.model.BootstrapData
import com.harmony.tokoharmony.domain.model.Category
import com.harmony.tokoharmony.domain.model.DigitalTransaction
import com.harmony.tokoharmony.domain.model.MovementType
import com.harmony.tokoharmony.domain.model.PaymentMethod
import com.harmony.tokoharmony.domain.model.PaymentStatus
import com.harmony.tokoharmony.domain.model.PriceHistory
import com.harmony.tokoharmony.domain.model.PricingMethod
import com.harmony.tokoharmony.domain.model.Product
import com.harmony.tokoharmony.domain.model.ProductKind
import com.harmony.tokoharmony.domain.model.QuantityType
import com.harmony.tokoharmony.domain.model.ReferenceType
import com.harmony.tokoharmony.domain.model.Role
import com.harmony.tokoharmony.domain.model.StockAdjustment
import com.harmony.tokoharmony.domain.model.StockIn
import com.harmony.tokoharmony.domain.model.StockMovement
import com.harmony.tokoharmony.domain.model.SyncBatchResult
import com.harmony.tokoharmony.domain.model.SyncItemResult
import com.harmony.tokoharmony.domain.model.SyncQueueItem
import com.harmony.tokoharmony.domain.model.Transaction
import com.harmony.tokoharmony.domain.model.TransactionItem
import com.harmony.tokoharmony.domain.model.TransactionStatus
import com.harmony.tokoharmony.domain.model.User
import org.json.JSONArray
import org.json.JSONObject

object SyncJsonMapper {

    fun userToJson(user: User): String {
        return JSONObject().apply {
            put("userId", user.userId)
            put("displayName", user.displayName)
            put("role", user.role.name)
            put("pinHash", user.pinHash ?: JSONObject.NULL)
            put("isActive", user.isActive)
            put("createdAt", user.createdAt)
            put("updatedAt", user.updatedAt)
        }.toString()
    }

    fun categoryToJson(category: Category): String {
        return JSONObject().apply {
            put("categoryId", category.categoryId)
            put("name", category.name)
            put("isActive", category.isActive)
            put("createdAt", category.createdAt)
            put("updatedAt", category.updatedAt)
        }.toString()
    }

    fun productToJson(product: Product): String {
        return JSONObject().apply {
            put("productId", product.productId)
            put("categoryId", product.categoryId)
            put("name", product.name)
            put("barcode", product.barcode ?: JSONObject.NULL)
            put("productKind", product.productKind.name)
            put("pricingMethod", product.pricingMethod.name)
            put("quantityType", product.quantityType.name)
            put("sellingUnit", product.sellingUnit)
            put("stockUnit", product.stockUnit)
            put("purchaseUnit", product.purchaseUnit ?: JSONObject.NULL)
            put("purchaseConversionFactor", product.purchaseConversionFactor ?: JSONObject.NULL)
            put("currentPrice", product.currentPrice)
            put("minimumStock", product.minimumStock ?: JSONObject.NULL)
            put("isActive", product.isActive)
            put("createdAt", product.createdAt)
            put("updatedAt", product.updatedAt)
        }.toString()
    }

    fun priceHistoryToJson(history: PriceHistory): String {
        return JSONObject().apply {
            put("historyId", history.historyId)
            put("productId", history.productId)
            put("oldPrice", history.oldPrice)
            put("newPrice", history.newPrice)
            put("changedAt", history.changedAt)
            put("changedBy", history.changedBy)
        }.toString()
    }

    fun transactionToJson(tx: Transaction): String {
        return JSONObject().apply {
            put("transactionId", tx.transactionId)
            put("transactionNumber", tx.transactionNumber)
            put("transactionStatus", tx.transactionStatus.name)
            put("paymentStatus", tx.paymentStatus.name)
            put("paymentMethod", tx.paymentMethod?.name ?: JSONObject.NULL)
            put("totalAmount", tx.totalAmount)
            put("amountReceived", tx.amountReceived ?: JSONObject.NULL)
            put("changeAmount", tx.changeAmount)
            put("createdAt", tx.createdAt)
            put("completedAt", tx.completedAt ?: JSONObject.NULL)
            put("cancelledAt", tx.cancelledAt ?: JSONObject.NULL)
            put("createdBy", tx.createdBy)
            put("cancelledBy", tx.cancelledBy ?: JSONObject.NULL)
            put("cancellationReason", tx.cancellationReason ?: JSONObject.NULL)
        }.toString()
    }

    fun transactionItemToJson(item: TransactionItem): String {
        return JSONObject().apply {
            put("itemId", item.itemId)
            put("transactionId", item.transactionId)
            put("productId", item.productId)
            put("productNameSnapshot", item.productNameSnapshot)
            put("quantity", item.quantity)
            put("unitPrice", item.unitPrice)
            put("subtotal", item.subtotal)
            put("sellingUnit", item.sellingUnit)
        }.toString()
    }

    fun stockInToJson(stockIn: StockIn): String {
        return JSONObject().apply {
            put("stockInId", stockIn.stockInId)
            put("productId", stockIn.productId)
            put("purchaseQuantity", stockIn.purchaseQuantity)
            put("purchaseUnit", stockIn.purchaseUnit)
            put("conversionFactor", stockIn.conversionFactor)
            put("stockQuantity", stockIn.stockQuantity)
            put("note", stockIn.note ?: JSONObject.NULL)
            put("createdAt", stockIn.createdAt)
            put("createdBy", stockIn.createdBy)
        }.toString()
    }

    fun stockAdjustmentToJson(adj: StockAdjustment): String {
        return JSONObject().apply {
            put("adjustmentId", adj.adjustmentId)
            put("productId", adj.productId)
            put("systemQuantity", adj.systemQuantity)
            put("physicalQuantity", adj.physicalQuantity)
            put("difference", adj.difference)
            put("reason", adj.reason)
            put("note", adj.note ?: JSONObject.NULL)
            put("createdAt", adj.createdAt)
            put("createdBy", adj.createdBy)
        }.toString()
    }

    fun stockMovementToJson(mov: StockMovement): String {
        return JSONObject().apply {
            put("movementId", mov.movementId)
            put("productId", mov.productId)
            put("movementType", mov.movementType.name)
            put("quantityDelta", mov.quantityDelta)
            put("referenceType", mov.referenceType?.name ?: JSONObject.NULL)
            put("referenceId", mov.referenceId ?: JSONObject.NULL)
            put("reason", mov.reason ?: JSONObject.NULL)
            put("createdAt", mov.createdAt)
            put("createdBy", mov.createdBy)
        }.toString()
    }

    fun digitalTransactionToJson(dt: DigitalTransaction): String {
        return JSONObject().apply {
            put("digitalTransactionId", dt.digitalTransactionId)
            put("transactionItemId", dt.transactionItemId)
            put("serviceType", dt.serviceType)
            put("customerNumber", dt.customerNumber)
            put("nominal", dt.nominal)
            put("providerName", dt.providerName)
            put("targetAccount", dt.targetAccount)
            put("providerReference", dt.providerReference ?: JSONObject.NULL)
            put("status", dt.status)
            put("createdAt", dt.createdAt)
        }.toString()
    }

    fun buildBatchRequestJson(
        syncId: String,
        deviceId: String,
        items: List<SyncQueueItem>,
        syncToken: String = ""
    ): String {
        val root = JSONObject()
        root.put("syncId", syncId)
        root.put("deviceId", deviceId)
        if (syncToken.isNotBlank()) {
            root.put("syncToken", syncToken)
        }
        root.put("timestamp", System.currentTimeMillis())

        val operationsArray = JSONArray()
        for (item in items) {
            val opObj = JSONObject()
            opObj.put("queueId", item.queueId)
            opObj.put("entityType", item.entityType.name)
            opObj.put("entityId", item.entityId)
            opObj.put("operation", item.operation.name)

            // Try to parse item.payload as JSONObject, fallback to raw string if already object
            try {
                opObj.put("payload", JSONObject(item.payload))
            } catch (e: Exception) {
                opObj.put("payload", item.payload)
            }
            operationsArray.put(opObj)
        }
        root.put("operations", operationsArray)

        return root.toString()
    }

    fun parseBatchResponse(jsonString: String): SyncBatchResult {
        return try {
            val root = JSONObject(jsonString)
            val status = root.optString("status", "ERROR")
            val resultsArray = root.optJSONArray("results") ?: JSONArray()
            val itemResults = mutableListOf<SyncItemResult>()

            var success = 0
            var failure = 0

            for (i in 0 until resultsArray.length()) {
                val itemObj = resultsArray.getJSONObject(i)
                val qId = itemObj.getString("queueId")
                val itemStatus = itemObj.optString("status", "FAILED")
                val retryable = itemObj.optBoolean("retryable", true)
                val error = if (itemObj.isNull("error")) null else itemObj.optString("error")

                if (itemStatus == "SUCCESS") {
                    success++
                } else {
                    failure++
                }

                itemResults.add(
                    SyncItemResult(
                        queueId = qId,
                        status = itemStatus,
                        retryable = retryable,
                        error = error
                    )
                )
            }

            SyncBatchResult(
                totalProcessed = resultsArray.length(),
                successCount = success,
                failureCount = failure,
                itemResults = itemResults,
                isNetworkError = false,
                errorMessage = if (status == "ERROR" && resultsArray.length() == 0) root.optString("error", "Unknown server error") else null
            )
        } catch (e: Exception) {
            SyncBatchResult(
                totalProcessed = 0,
                successCount = 0,
                failureCount = 0,
                itemResults = emptyList(),
                isNetworkError = false,
                errorMessage = "Malformed response: ${e.message}"
            )
        }
    }

    fun parseBootstrapResponse(jsonString: String): BootstrapData {
        val root = JSONObject(jsonString)
        val data = root.optJSONObject("data") ?: JSONObject()

        // 1. Users
        val users = mutableListOf<User>()
        val usersArray = data.optJSONArray("users") ?: JSONArray()
        for (i in 0 until usersArray.length()) {
            val obj = usersArray.getJSONObject(i)
            users.add(
                User(
                    userId = obj.getString("userId"),
                    displayName = obj.getString("displayName"),
                    role = Role.valueOf(obj.optString("role", "CASHIER")),
                    pinHash = if (obj.isNull("pinHash")) null else obj.optString("pinHash"),
                    isActive = obj.optBoolean("isActive", true),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                )
            )
        }

        // 2. Categories
        val categories = mutableListOf<Category>()
        val categoriesArray = data.optJSONArray("categories") ?: JSONArray()
        for (i in 0 until categoriesArray.length()) {
            val obj = categoriesArray.getJSONObject(i)
            categories.add(
                Category(
                    categoryId = obj.getString("categoryId"),
                    name = obj.getString("name"),
                    isActive = obj.optBoolean("isActive", true),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                )
            )
        }

        // 3. Products
        val products = mutableListOf<Product>()
        val productsArray = data.optJSONArray("products") ?: JSONArray()
        for (i in 0 until productsArray.length()) {
            val obj = productsArray.getJSONObject(i)
            products.add(
                Product(
                    productId = obj.getString("productId"),
                    categoryId = obj.getString("categoryId"),
                    name = obj.getString("name"),
                    barcode = if (obj.isNull("barcode")) null else obj.optString("barcode"),
                    productKind = ProductKind.valueOf(obj.optString("productKind", "PHYSICAL")),
                    pricingMethod = PricingMethod.valueOf(obj.optString("pricingMethod", "PER_UNIT")),
                    quantityType = QuantityType.valueOf(obj.optString("quantityType", "COUNT")),
                    sellingUnit = obj.optString("sellingUnit", "pcs"),
                    stockUnit = obj.optString("stockUnit", "pcs"),
                    purchaseUnit = if (obj.isNull("purchaseUnit")) null else obj.optString("purchaseUnit"),
                    purchaseConversionFactor = if (obj.isNull("purchaseConversionFactor")) null else obj.optLong("purchaseConversionFactor"),
                    currentPrice = obj.optLong("currentPrice", 0L),
                    minimumStock = if (obj.isNull("minimumStock")) null else obj.optLong("minimumStock"),
                    isActive = obj.optBoolean("isActive", true),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                )
            )
        }

        // 4. Price History
        val priceHistories = mutableListOf<PriceHistory>()
        val priceHistoriesArray = data.optJSONArray("priceHistory") ?: JSONArray()
        for (i in 0 until priceHistoriesArray.length()) {
            val obj = priceHistoriesArray.getJSONObject(i)
            priceHistories.add(
                PriceHistory(
                    historyId = obj.getString("historyId"),
                    productId = obj.getString("productId"),
                    oldPrice = obj.optLong("oldPrice", 0L),
                    newPrice = obj.optLong("newPrice", 0L),
                    changedAt = obj.optLong("changedAt", System.currentTimeMillis()),
                    changedBy = obj.getString("changedBy")
                )
            )
        }

        // 5. Transactions
        val transactions = mutableListOf<Transaction>()
        val transactionsArray = data.optJSONArray("transactions") ?: JSONArray()
        for (i in 0 until transactionsArray.length()) {
            val obj = transactionsArray.getJSONObject(i)
            transactions.add(
                Transaction(
                    transactionId = obj.getString("transactionId"),
                    transactionNumber = obj.getString("transactionNumber"),
                    transactionStatus = TransactionStatus.valueOf(obj.optString("transactionStatus", "COMPLETED")),
                    paymentStatus = PaymentStatus.valueOf(obj.optString("paymentStatus", "PAID")),
                    paymentMethod = if (obj.isNull("paymentMethod")) null else PaymentMethod.valueOf(obj.getString("paymentMethod")),
                    totalAmount = obj.optLong("totalAmount", 0L),
                    amountReceived = if (obj.isNull("amountReceived")) null else obj.optLong("amountReceived"),
                    changeAmount = obj.optLong("changeAmount", 0L),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    completedAt = if (obj.isNull("completedAt")) null else obj.optLong("completedAt"),
                    cancelledAt = if (obj.isNull("cancelledAt")) null else obj.optLong("cancelledAt"),
                    createdBy = obj.getString("createdBy"),
                    cancelledBy = if (obj.isNull("cancelledBy")) null else obj.getString("cancelledBy"),
                    cancellationReason = if (obj.isNull("cancellationReason")) null else obj.getString("cancellationReason")
                )
            )
        }

        // 6. Transaction Items
        val transactionItems = mutableListOf<TransactionItem>()
        val transactionItemsArray = data.optJSONArray("transactionItems") ?: JSONArray()
        for (i in 0 until transactionItemsArray.length()) {
            val obj = transactionItemsArray.getJSONObject(i)
            transactionItems.add(
                TransactionItem(
                    itemId = obj.getString("itemId"),
                    transactionId = obj.getString("transactionId"),
                    productId = obj.getString("productId"),
                    productNameSnapshot = obj.getString("productNameSnapshot"),
                    quantity = obj.optLong("quantity", 1L),
                    unitPrice = obj.optLong("unitPrice", 0L),
                    subtotal = obj.optLong("subtotal", 0L),
                    sellingUnit = obj.optString("sellingUnit", "pcs")
                )
            )
        }

        // 7. Stock Ins
        val stockIns = mutableListOf<StockIn>()
        val stockInsArray = data.optJSONArray("stockIns") ?: JSONArray()
        for (i in 0 until stockInsArray.length()) {
            val obj = stockInsArray.getJSONObject(i)
            stockIns.add(
                StockIn(
                    stockInId = obj.getString("stockInId"),
                    productId = obj.getString("productId"),
                    purchaseQuantity = obj.optLong("purchaseQuantity", 0L),
                    purchaseUnit = obj.getString("purchaseUnit"),
                    conversionFactor = obj.optLong("conversionFactor", 1L),
                    stockQuantity = obj.optLong("stockQuantity", 0L),
                    note = if (obj.isNull("note")) null else obj.optString("note"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    createdBy = obj.getString("createdBy")
                )
            )
        }

        // 8. Stock Adjustments
        val stockAdjustments = mutableListOf<StockAdjustment>()
        val stockAdjustmentsArray = data.optJSONArray("stockAdjustments") ?: JSONArray()
        for (i in 0 until stockAdjustmentsArray.length()) {
            val obj = stockAdjustmentsArray.getJSONObject(i)
            stockAdjustments.add(
                StockAdjustment(
                    adjustmentId = obj.getString("adjustmentId"),
                    productId = obj.getString("productId"),
                    systemQuantity = obj.optLong("systemQuantity", 0L),
                    physicalQuantity = obj.optLong("physicalQuantity", 0L),
                    difference = obj.optLong("difference", 0L),
                    reason = obj.getString("reason"),
                    note = if (obj.isNull("note")) null else obj.optString("note"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    createdBy = obj.getString("createdBy")
                )
            )
        }

        // 9. Stock Movements
        val stockMovements = mutableListOf<StockMovement>()
        val stockMovementsArray = data.optJSONArray("stockMovements") ?: JSONArray()
        for (i in 0 until stockMovementsArray.length()) {
            val obj = stockMovementsArray.getJSONObject(i)
            stockMovements.add(
                StockMovement(
                    movementId = obj.getString("movementId"),
                    productId = obj.getString("productId"),
                    movementType = MovementType.valueOf(obj.getString("movementType")),
                    quantityDelta = obj.optLong("quantityDelta", 0L),
                    referenceType = if (obj.isNull("referenceType")) null else ReferenceType.valueOf(obj.getString("referenceType")),
                    referenceId = if (obj.isNull("referenceId")) null else obj.optString("referenceId"),
                    reason = if (obj.isNull("reason")) null else obj.optString("reason"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    createdBy = obj.getString("createdBy")
                )
            )
        }

        // 10. Digital Transactions
        val digitalTransactions = mutableListOf<DigitalTransaction>()
        val digitalTransactionsArray = data.optJSONArray("digitalTransactions") ?: JSONArray()
        for (i in 0 until digitalTransactionsArray.length()) {
            val obj = digitalTransactionsArray.getJSONObject(i)
            val serviceType = obj.optString("serviceType", obj.optString("providerName", "PULSA"))
            val customerNumber = obj.optString("customerNumber", obj.optString("targetAccount", ""))
            val nominal = obj.optLong("nominal", 0L)
            digitalTransactions.add(
                DigitalTransaction(
                    digitalTransactionId = obj.getString("digitalTransactionId"),
                    transactionItemId = obj.getString("transactionItemId"),
                    serviceType = serviceType,
                    customerNumber = customerNumber,
                    nominal = nominal,
                    providerReference = if (obj.isNull("providerReference")) null else obj.optString("providerReference"),
                    status = obj.optString("status", "PENDING"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        return BootstrapData(
            users = users,
            categories = categories,
            products = products,
            priceHistories = priceHistories,
            transactions = transactions,
            transactionItems = transactionItems,
            stockIns = stockIns,
            stockAdjustments = stockAdjustments,
            stockMovements = stockMovements,
            digitalTransactions = digitalTransactions
        )
    }
}
