package com.harmony.tokoharmony.domain.usecase.product

import com.harmony.tokoharmony.core.common.AppError
import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.model.PricingMethod
import com.harmony.tokoharmony.domain.model.Product
import com.harmony.tokoharmony.domain.model.ProductKind
import com.harmony.tokoharmony.domain.model.QuantityType
import com.harmony.tokoharmony.domain.model.Role
import com.harmony.tokoharmony.domain.model.User
import com.harmony.tokoharmony.domain.repository.ProductRepository
import java.util.UUID
import javax.inject.Inject

class CreateProductUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(
        categoryId: String,
        name: String,
        barcode: String?,
        productKind: ProductKind,
        pricingMethod: PricingMethod,
        quantityType: QuantityType,
        sellingUnit: String,
        stockUnit: String,
        purchaseUnit: String? = null,
        purchaseConversionFactor: Long? = null,
        currentPrice: Long,
        minimumStock: Long? = null,
        user: User
    ): Result<Product> {
        if (user.role != Role.ADMIN) {
            return Result.Error(AppError.Unauthorized("Hanya Admin yang dapat menambahkan produk baru."))
        }

        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            return Result.Error(AppError.Validation("Nama produk tidak boleh kosong."))
        }

        if (categoryId.isBlank()) {
            return Result.Error(AppError.Validation("Kategori produk wajib dipilih."))
        }

        if (currentPrice < 0) {
            return Result.Error(AppError.Validation("Harga produk tidak boleh negatif."))
        }

        if (sellingUnit.isBlank()) {
            return Result.Error(AppError.Validation("Satuan jual wajib diisi."))
        }

        if (stockUnit.isBlank()) {
            return Result.Error(AppError.Validation("Satuan stok wajib diisi."))
        }

        // Validate domain attribute combinations
        if (productKind == ProductKind.DIGITAL) {
            if (pricingMethod == PricingMethod.PER_KG || quantityType == QuantityType.GRAM) {
                return Result.Error(AppError.Validation("Produk digital hanya dapat menggunakan metode harga per satuan (PER_UNIT) dan kuantitas hitungan (COUNT)."))
            }
        }

        if (pricingMethod == PricingMethod.PER_KG && quantityType != QuantityType.GRAM) {
            return Result.Error(AppError.Validation("Produk dengan metode harga per kilogram (PER_KG) harus menggunakan tipe kuantitas gram (GRAM)."))
        }

        if (pricingMethod == PricingMethod.PER_UNIT && quantityType != QuantityType.COUNT) {
            return Result.Error(AppError.Validation("Produk dengan metode harga per satuan (PER_UNIT) harus menggunakan tipe kuantitas hitungan (COUNT)."))
        }

        val cleanPurchaseUnit = purchaseUnit?.trim()?.takeIf { it.isNotBlank() }
        val cleanConversionFactor = if (cleanPurchaseUnit != null) purchaseConversionFactor else null

        if (cleanPurchaseUnit != null && (cleanConversionFactor == null || cleanConversionFactor <= 0)) {
            return Result.Error(AppError.Validation("Faktor konversi satuan beli harus lebih dari 0."))
        }

        val trimmedBarcode = barcode?.trim()?.takeIf { it.isNotBlank() }
        if (trimmedBarcode != null) {
            val existingProduct = productRepository.getProductByBarcode(trimmedBarcode)
            if (existingProduct != null) {
                return Result.Error(AppError.Validation("Barcode '$trimmedBarcode' sudah digunakan oleh produk '${existingProduct.name}'."))
            }
        }

        val now = System.currentTimeMillis()
        val product = Product(
            productId = UUID.randomUUID().toString(),
            categoryId = categoryId,
            name = trimmedName,
            barcode = trimmedBarcode,
            productKind = productKind,
            pricingMethod = pricingMethod,
            quantityType = quantityType,
            sellingUnit = sellingUnit.trim(),
            stockUnit = stockUnit.trim(),
            purchaseUnit = cleanPurchaseUnit,
            purchaseConversionFactor = cleanConversionFactor,
            currentPrice = currentPrice,
            minimumStock = minimumStock,
            isActive = true,
            createdAt = now,
            updatedAt = now
        )

        return try {
            productRepository.saveProduct(product)
            Result.Success(product)
        } catch (e: Exception) {
            Result.Error(AppError.Database("Gagal menyimpan produk: ${e.message}", e))
        }
    }
}
