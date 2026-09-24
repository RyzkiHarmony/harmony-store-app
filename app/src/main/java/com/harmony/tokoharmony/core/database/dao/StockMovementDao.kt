package com.harmony.tokoharmony.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.harmony.tokoharmony.core.database.entity.StockMovementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockMovementDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStockMovement(movement: StockMovementEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStockMovements(movements: List<StockMovementEntity>)

    @Query("SELECT COALESCE(SUM(quantity_delta), 0) FROM stock_movements WHERE product_id = :productId")
    suspend fun getCurrentStock(productId: String): Long

    @Query("SELECT * FROM stock_movements WHERE product_id = :productId ORDER BY created_at DESC")
    fun getMovementsForProduct(productId: String): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements ORDER BY created_at DESC")
    fun getAllMovements(): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements WHERE reference_type = :referenceType AND reference_id = :referenceId")
    suspend fun getMovementsByReference(referenceType: String, referenceId: String): List<StockMovementEntity>

    @Query("SELECT COUNT(*) > 0 FROM stock_movements WHERE product_id = :productId AND movement_type = 'INITIAL_STOCK'")
    suspend fun hasInitialStock(productId: String): Boolean
}
