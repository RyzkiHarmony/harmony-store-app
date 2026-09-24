package com.harmony.tokoharmony.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.harmony.tokoharmony.core.database.entity.StockInEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockInDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockIn(stockIn: StockInEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockIns(stockIns: List<StockInEntity>)

    @Query("SELECT * FROM stock_ins WHERE product_id = :productId ORDER BY created_at DESC")
    fun getStockInsForProduct(productId: String): Flow<List<StockInEntity>>

    @Query("SELECT * FROM stock_ins ORDER BY created_at DESC")
    fun getAllStockIns(): Flow<List<StockInEntity>>

    @Query("SELECT * FROM stock_ins WHERE stock_in_id = :stockInId")
    suspend fun getStockInById(stockInId: String): StockInEntity?
}
