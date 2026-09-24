package com.harmony.tokoharmony.feature.admin.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.usecase.auth.GetAdminUserUseCase
import com.harmony.tokoharmony.domain.usecase.transaction.FilterTransactionsUseCase
import com.harmony.tokoharmony.domain.usecase.transaction.GetAllTransactionsUseCase
import com.harmony.tokoharmony.domain.usecase.transaction.TransactionDateFilter
import com.harmony.tokoharmony.domain.usecase.transaction.TransactionPaymentFilter
import com.harmony.tokoharmony.domain.usecase.transaction.TransactionStatusFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminTransactionHistoryViewModel @Inject constructor(
    private val getAllTransactionsUseCase: GetAllTransactionsUseCase,
    private val getAdminUserUseCase: GetAdminUserUseCase,
    private val filterTransactionsUseCase: FilterTransactionsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminTransactionHistoryUiState())
    val uiState: StateFlow<AdminTransactionHistoryUiState> = _uiState.asStateFlow()

    init {
        loadTransactions()
    }

    fun loadTransactions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val admin = getAdminUserUseCase()
            if (admin == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Pengguna Admin tidak ditemukan.") }
                return@launch
            }

            when (val result = getAllTransactionsUseCase(admin)) {
                is Result.Success -> {
                    result.data.collect { list ->
                        _uiState.update { current ->
                            val filtered = filterTransactionsUseCase(
                                transactions = list,
                                dateFilter = current.dateFilter,
                                statusFilter = current.statusFilter,
                                paymentFilter = current.paymentFilter
                            )
                            current.copy(
                                rawTransactions = list,
                                transactions = filtered,
                                isLoading = false
                            )
                        }
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            errorMessage = result.error.message,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun onDateFilterChanged(filter: TransactionDateFilter) {
        _uiState.update { current ->
            val filtered = filterTransactionsUseCase(
                transactions = current.rawTransactions,
                dateFilter = filter,
                statusFilter = current.statusFilter,
                paymentFilter = current.paymentFilter
            )
            current.copy(dateFilter = filter, transactions = filtered)
        }
    }

    fun onStatusFilterChanged(filter: TransactionStatusFilter) {
        _uiState.update { current ->
            val filtered = filterTransactionsUseCase(
                transactions = current.rawTransactions,
                dateFilter = current.dateFilter,
                statusFilter = filter,
                paymentFilter = current.paymentFilter
            )
            current.copy(statusFilter = filter, transactions = filtered)
        }
    }

    fun onPaymentFilterChanged(filter: TransactionPaymentFilter) {
        _uiState.update { current ->
            val filtered = filterTransactionsUseCase(
                transactions = current.rawTransactions,
                dateFilter = current.dateFilter,
                statusFilter = current.statusFilter,
                paymentFilter = filter
            )
            current.copy(paymentFilter = filter, transactions = filtered)
        }
    }

    fun resetFilters() {
        _uiState.update { current ->
            val filtered = filterTransactionsUseCase(
                transactions = current.rawTransactions,
                dateFilter = TransactionDateFilter.ALL,
                statusFilter = TransactionStatusFilter.ALL,
                paymentFilter = TransactionPaymentFilter.ALL
            )
            current.copy(
                dateFilter = TransactionDateFilter.ALL,
                statusFilter = TransactionStatusFilter.ALL,
                paymentFilter = TransactionPaymentFilter.ALL,
                transactions = filtered
            )
        }
    }
}
