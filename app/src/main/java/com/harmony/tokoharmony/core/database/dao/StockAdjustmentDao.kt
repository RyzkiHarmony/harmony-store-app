package com.harmony.tokoharmony.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.harmony.tokoharmony.core.database.entity.StockAdjustmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockAdjustmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockAdjustment(adjustment: StockAdjustmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockAdjustments(adjustments: List<StockAdjustmentEntity>)

    @Query("SELECT * FROM stock_adjustments WHERE product_id = :productId ORDER BY created_at DESC")
    fun getAdjustmentsForProduct(productId: String): Flow<List<StockAdjustmentEntity>>

    @Query("SELECT * FROM stock_adjustments ORDER BY created_at DESC")
    fun getAllStockAdjustments(): Flow<List<StockAdjustmentEntity>>

    @Query("SELECT * FROM stock_adjustments WHERE adjustment_id = :adjustmentId")
    suspend fun getAdjustmentById(adjustmentId: String): StockAdjustmentEntity?
}
