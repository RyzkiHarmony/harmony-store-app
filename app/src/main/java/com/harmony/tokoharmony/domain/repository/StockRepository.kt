package com.harmony.tokoharmony.domain.repository

import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.model.StockAdjustment
import com.harmony.tokoharmony.domain.model.StockIn
import com.harmony.tokoharmony.domain.model.StockMovement
import kotlinx.coroutines.flow.Flow

interface StockRepository {
    suspend fun getCurrentStock(productId: String): Long
    suspend fun insertStockMovement(movement: StockMovement): Result<Unit>
    suspend fun insertStockMovements(movements: List<StockMovement>): Result<Unit>
    fun getMovementsForProduct(productId: String): Flow<List<StockMovement>>
    fun getAllStockMovements(): Flow<List<StockMovement>>
    suspend fun hasInitialStock(productId: String): Boolean

    suspend fun recordInitialStock(
        productId: String,
        initialStock: Long,
        userId: String
    ): Result<Unit>

    suspend fun recordStockIn(
        productId: String,
        purchaseQuantity: Long,
        purchaseUnit: String,
        conversionFactor: Long,
        note: String?,
        userId: String
    ): Result<StockIn>

    suspend fun recordStockAdjustment(
        productId: String,
        physicalQuantity: Long,
        reason: String,
        note: String?,
        userId: String
    ): Result<StockAdjustment>

    fun getAllStockIns(): Flow<List<StockIn>>
    fun getAllStockAdjustments(): Flow<List<StockAdjustment>>
    fun getStockInsForProduct(productId: String): Flow<List<StockIn>>
    fun getAdjustmentsForProduct(productId: String): Flow<List<StockAdjustment>>
}
