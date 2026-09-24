package com.harmony.tokoharmony.feature.admin.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.repository.TransactionRepository
import com.harmony.tokoharmony.domain.usecase.auth.GetAdminUserUseCase
import com.harmony.tokoharmony.domain.usecase.transaction.CancelTransactionUseCase
import com.harmony.tokoharmony.domain.usecase.transaction.GetTransactionByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminTransactionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTransactionByIdUseCase: GetTransactionByIdUseCase,
    private val transactionRepository: TransactionRepository,
    private val cancelTransactionUseCase: CancelTransactionUseCase,
    private val getAdminUserUseCase: GetAdminUserUseCase
) : ViewModel() {

    private val transactionId: String = checkNotNull(savedStateHandle["transactionId"])

    private val _uiState = MutableStateFlow(AdminTransactionDetailUiState())
    val uiState: StateFlow<AdminTransactionDetailUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<AdminTransactionEvent>()
    val eventFlow: SharedFlow<AdminTransactionEvent> = _eventFlow.asSharedFlow()

    init {
        loadTransactionDetail()
    }

    fun loadTransactionDetail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val tx = getTransactionByIdUseCase(transactionId)
            val items = transactionRepository.getItemsForTransaction(transactionId)

            _uiState.update {
                it.copy(
                    transaction = tx,
                    items = items,
                    isLoading = false
                )
            }
        }
    }

    fun onReasonSelected(reason: String) {
        _uiState.update { it.copy(cancellationReasonInput = reason) }
    }

    fun onNoteChanged(note: String) {
        _uiState.update { it.copy(noteInput = note) }
    }

    fun cancelTransaction() {
        val selectedReason = _uiState.value.cancellationReasonInput
        val note = _uiState.value.noteInput.trim()
        val fullReason = if (note.isNotBlank()) "$selectedReason: $note" else selectedReason

        viewModelScope.launch {
            val admin = getAdminUserUseCase()
            if (admin == null) {
                _uiState.update { it.copy(errorMessage = "Pengguna Admin tidak ditemukan.") }
                return@launch
            }

            _uiState.update { it.copy(isCancelling = true) }
            when (val result = cancelTransactionUseCase(
                transactionId = transactionId,
                reason = fullReason,
                user = admin
            )) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isCancelling = false,
                            transaction = result.data,
                            successMessage = "Transaksi berhasil dibatalkan dan stok fisik telah dipulihkan."
                        )
                    }
                    _eventFlow.emit(AdminTransactionEvent.CancelSuccess("Transaksi dibatalkan."))
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isCancelling = false,
                            errorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
