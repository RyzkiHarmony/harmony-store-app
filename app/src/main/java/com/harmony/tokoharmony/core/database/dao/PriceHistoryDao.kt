package com.harmony.tokoharmony.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.harmony.tokoharmony.core.database.entity.PriceHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PriceHistoryDao {

    @Query("SELECT * FROM price_history WHERE product_id = :productId ORDER BY changed_at DESC")
    fun getPriceHistoryByProductId(productId: String): Flow<List<PriceHistoryEntity>>

    @Query("SELECT * FROM price_history WHERE product_id = :productId ORDER BY changed_at DESC")
    suspend fun getPriceHistoryListByProductId(productId: String): List<PriceHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPriceHistory(priceHistory: PriceHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceHistories(priceHistories: List<PriceHistoryEntity>)
}
