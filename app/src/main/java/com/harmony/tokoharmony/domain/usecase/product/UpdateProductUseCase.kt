package com.harmony.tokoharmony.domain.usecase.product

import com.harmony.tokoharmony.core.common.AppError
import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.model.PriceHistory
import com.harmony.tokoharmony.domain.model.PricingMethod
import com.harmony.tokoharmony.domain.model.Product
import com.harmony.tokoharmony.domain.model.ProductKind
import com.harmony.tokoharmony.domain.model.QuantityType
import com.harmony.tokoharmony.domain.model.Role
import com.harmony.tokoharmony.domain.model.User
import com.harmony.tokoharmony.domain.repository.PriceRepository
import com.harmony.tokoharmony.domain.repository.ProductRepository
import java.util.UUID
import javax.inject.Inject

class UpdateProductUseCase @Inject constructor(
    private val productRepository: ProductRepository,
    private val priceRepository: PriceRepository
) {
    suspend operator fun invoke(
        product: Product,
        user: User
    ): Result<Product> {
        if (user.role != Role.ADMIN) {
            return Result.Error(AppError.Unauthorized("Hanya Admin yang dapat mengubah data produk."))
        }

        val existing = productRepository.getProductById(product.productId)
            ?: return Result.Error(AppError.NotFound("Produk tidak ditemukan."))

        val trimmedName = product.name.trim()
        if (trimmedName.isBlank()) {
            return Result.Error(AppError.Validation("Nama produk tidak boleh kosong."))
        }

        if (product.categoryId.isBlank()) {
            return Result.Error(AppError.Validation("Kategori produk wajib dipilih."))
        }

        if (product.currentPrice < 0) {
            return Result.Error(AppError.Validation("Harga produk tidak boleh negatif."))
        }

        if (product.sellingUnit.isBlank()) {
            return Result.Error(AppError.Validation("Satuan jual wajib diisi."))
        }

        if (product.stockUnit.isBlank()) {
            return Result.Error(AppError.Validation("Satuan stok wajib diisi."))
        }

        // Validate domain attribute combinations
        if (product.productKind == ProductKind.DIGITAL) {
            if (product.pricingMethod == PricingMethod.PER_KG || product.quantityType == QuantityType.GRAM) {
                return Result.Error(AppError.Validation("Produk digital hanya dapat menggunakan metode harga per satuan (PER_UNIT) dan kuantitas hitungan (COUNT)."))
            }
        }

        if (product.pricingMethod == PricingMethod.PER_KG && product.quantityType != QuantityType.GRAM) {
            return Result.Error(AppError.Validation("Produk dengan metode harga per kilogram (PER_KG) harus menggunakan tipe kuantitas gram (GRAM)."))
        }

        if (product.pricingMethod == PricingMethod.PER_UNIT && product.quantityType != QuantityType.COUNT) {
            return Result.Error(AppError.Validation("Produk dengan metode harga per satuan (PER_UNIT) harus menggunakan tipe kuantitas hitungan (COUNT)."))
        }

        val cleanPurchaseUnit = product.purchaseUnit?.trim()?.takeIf { it.isNotBlank() }
        val cleanConversionFactor = if (cleanPurchaseUnit != null) product.purchaseConversionFactor else null

        if (cleanPurchaseUnit != null && (cleanConversionFactor == null || cleanConversionFactor <= 0)) {
            return Result.Error(AppError.Validation("Faktor konversi satuan beli harus lebih dari 0."))
        }

        val trimmedBarcode = product.barcode?.trim()?.takeIf { it.isNotBlank() }
        if (trimmedBarcode != null) {
            val duplicateProduct = productRepository.getProductByBarcode(trimmedBarcode)
            if (duplicateProduct != null && duplicateProduct.productId != product.productId) {
                return Result.Error(AppError.Validation("Barcode '$trimmedBarcode' sudah digunakan oleh produk '${duplicateProduct.name}'."))
            }
        }

        val now = System.currentTimeMillis()
        val updatedProduct = product.copy(
            name = trimmedName,
            barcode = trimmedBarcode,
            sellingUnit = product.sellingUnit.trim(),
            stockUnit = product.stockUnit.trim(),
            purchaseUnit = cleanPurchaseUnit,
            purchaseConversionFactor = cleanConversionFactor,
            updatedAt = now
        )

        return try {
            if (existing.currentPrice != updatedProduct.currentPrice) {
                val priceHistory = PriceHistory(
                    historyId = UUID.randomUUID().toString(),
                    productId = updatedProduct.productId,
                    oldPrice = existing.currentPrice,
                    newPrice = updatedProduct.currentPrice,
                    changedAt = now,
                    changedBy = user.userId
                )
                priceRepository.recordPriceChange(priceHistory, updatedProduct.currentPrice)
            }
            productRepository.updateProduct(updatedProduct)
            Result.Success(updatedProduct)
        } catch (e: Exception) {
            Result.Error(AppError.Database("Gagal mengubah data produk: ${e.message}", e))
        }
    }
}
