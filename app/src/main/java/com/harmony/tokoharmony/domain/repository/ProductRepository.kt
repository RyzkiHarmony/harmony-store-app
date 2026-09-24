package com.harmony.tokoharmony.domain.repository

import com.harmony.tokoharmony.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun getAllProducts(): Flow<List<Product>>
    fun getActiveProducts(): Flow<List<Product>>
    suspend fun getProductById(productId: String): Product?
    suspend fun getProductByBarcode(barcode: String): Product?
    fun searchProducts(query: String, onlyActive: Boolean = true): Flow<List<Product>>
    suspend fun saveProduct(product: Product)
    suspend fun updateProduct(product: Product)
    suspend fun setProductActiveStatus(productId: String, isActive: Boolean)
}
