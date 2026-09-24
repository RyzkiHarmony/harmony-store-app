package com.harmony.tokoharmony.feature.cashier.success

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harmony.tokoharmony.domain.model.Transaction
import com.harmony.tokoharmony.domain.usecase.transaction.GetTransactionByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionSuccessUiState(
    val isLoading: Boolean = true,
    val transaction: Transaction? = null
)

@HiltViewModel
class TransactionSuccessViewModel @Inject constructor(
    private val getTransactionByIdUseCase: GetTransactionByIdUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val transactionId: String = checkNotNull(savedStateHandle["transactionId"])

    private val _uiState = MutableStateFlow(TransactionSuccessUiState())
    val uiState: StateFlow<TransactionSuccessUiState> = _uiState.asStateFlow()

    init {
        loadTransaction()
    }

    private fun loadTransaction() {
        viewModelScope.launch {
            val tx = getTransactionByIdUseCase(transactionId)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    transaction = tx
                )
            }
        }
    }
}
