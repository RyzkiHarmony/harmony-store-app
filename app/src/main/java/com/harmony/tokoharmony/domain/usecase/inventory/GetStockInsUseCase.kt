package com.harmony.tokoharmony.domain.usecase.inventory

import com.harmony.tokoharmony.core.common.AppError
import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.model.Role
import com.harmony.tokoharmony.domain.model.StockIn
import com.harmony.tokoharmony.domain.model.User
import com.harmony.tokoharmony.domain.repository.StockRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetStockInsUseCase @Inject constructor(
    private val stockRepository: StockRepository
) {
    operator fun invoke(productId: String? = null, user: User): Result<Flow<List<StockIn>>> {
        if (user.role != Role.ADMIN) {
            return Result.Error(AppError.Unauthorized("Hanya Admin yang dapat melihat data barang masuk."))
        }

        val flow = if (productId.isNullOrBlank()) {
            stockRepository.getAllStockIns()
        } else {
            stockRepository.getStockInsForProduct(productId)
        }
        return Result.Success(flow)
    }
}
