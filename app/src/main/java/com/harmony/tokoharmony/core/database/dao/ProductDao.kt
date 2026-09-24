package com.harmony.tokoharmony.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.harmony.tokoharmony.core.database.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE is_active = 1 ORDER BY name ASC")
    fun getActiveProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE product_id = :productId LIMIT 1")
    suspend fun getProductById(productId: String): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): ProductEntity?

    @Query(
        """
        SELECT * FROM products 
        WHERE name LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%'
        ORDER BY name ASC
        """
    )
    fun searchProducts(query: String): Flow<List<ProductEntity>>

    @Query(
        """
        SELECT * FROM products 
        WHERE is_active = 1 AND (name LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%')
        ORDER BY name ASC
        """
    )
    fun searchActiveProducts(query: String): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("UPDATE products SET current_price = :newPrice, updated_at = :updatedAt WHERE product_id = :productId")
    suspend fun updateProductPrice(productId: String, newPrice: Long, updatedAt: Long)

    @Query("UPDATE products SET is_active = :isActive, updated_at = :updatedAt WHERE product_id = :productId")
    suspend fun setProductActiveStatus(productId: String, isActive: Boolean, updatedAt: Long)
}
