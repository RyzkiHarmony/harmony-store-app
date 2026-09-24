package com.harmony.tokoharmony.domain.usecase.inventory

import com.harmony.tokoharmony.core.common.AppError
import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.model.Role
import com.harmony.tokoharmony.domain.model.StockMovement
import com.harmony.tokoharmony.domain.model.User
import com.harmony.tokoharmony.domain.repository.StockRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetStockMovementsUseCase @Inject constructor(
    private val stockRepository: StockRepository
) {
    operator fun invoke(productId: String? = null, user: User): Result<Flow<List<StockMovement>>> {
        if (user.role != Role.ADMIN) {
            return Result.Error(AppError.Unauthorized("Hanya Admin yang dapat melihat riwayat pergerakan stok."))
        }

        val flow = if (productId.isNullOrBlank()) {
            stockRepository.getAllStockMovements()
        } else {
            stockRepository.getMovementsForProduct(productId)
        }
        return Result.Success(flow)
    }
}
