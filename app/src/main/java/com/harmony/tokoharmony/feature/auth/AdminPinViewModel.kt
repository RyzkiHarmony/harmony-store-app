package com.harmony.tokoharmony.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.usecase.auth.AuthenticateAdminUseCase
import com.harmony.tokoharmony.domain.usecase.auth.HasAdminPinUseCase
import com.harmony.tokoharmony.domain.usecase.auth.SetAdminPinUseCase
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

data class AdminPinUiState(
    val isPinConfigured: Boolean? = null,
    val pin: String = "",
    val confirmPin: String = "",
    val isConfirmStep: Boolean = false,
    val errorMessage: String? = null,
    val isLoading: Boolean = false
)

sealed interface AdminPinEvent {
    data object AuthSuccess : AdminPinEvent
}

@HiltViewModel
class AdminPinViewModel @Inject constructor(
    private val hasAdminPinUseCase: HasAdminPinUseCase,
    private val authenticateAdminUseCase: AuthenticateAdminUseCase,
    private val setAdminPinUseCase: SetAdminPinUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminPinUiState())
    val uiState: StateFlow<AdminPinUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<AdminPinEvent>()
    val eventFlow: SharedFlow<AdminPinEvent> = _eventFlow.asSharedFlow()

    init {
        checkPinStatus()
    }

    private fun checkPinStatus() {
        viewModelScope.launch {
            val hasPin = hasAdminPinUseCase()
            _uiState.update { it.copy(isPinConfigured = hasPin) }
        }
    }

    fun onDigitEntered(digit: Char) {
        val current = _uiState.value
        if (current.isPinConfigured == true) {
            // Authentication mode (6 digits)
            if (current.pin.length < 6) {
                val newPin = current.pin + digit
                _uiState.update { it.copy(pin = newPin, errorMessage = null) }
                if (newPin.length == 6) {
                    submitAuth(newPin)
                }
            }
        } else {
            // Setup mode
            if (!current.isConfirmStep) {
                if (current.pin.length < 6) {
                    val newPin = current.pin + digit
                    _uiState.update { it.copy(pin = newPin, errorMessage = null) }
                    if (newPin.length == 6) {
                        _uiState.update { it.copy(isConfirmStep = true) }
                    }
                }
            } else {
                if (current.confirmPin.length < 6) {
                    val newConfirm = current.confirmPin + digit
                    _uiState.update { it.copy(confirmPin = newConfirm, errorMessage = null) }
                    if (newConfirm.length == 6) {
                        submitSetup(current.pin, newConfirm)
                    }
                }
            }
        }
    }

    fun onDeleteDigit() {
        _uiState.update { state ->
            if (state.isPinConfigured == true) {
                state.copy(pin = state.pin.dropLast(1), errorMessage = null)
            } else {
                if (state.isConfirmStep) {
                    if (state.confirmPin.isEmpty()) {
                        state.copy(isConfirmStep = false, errorMessage = null)
                    } else {
                        state.copy(confirmPin = state.confirmPin.dropLast(1), errorMessage = null)
                    }
                } else {
                    state.copy(pin = state.pin.dropLast(1), errorMessage = null)
                }
            }
        }
    }

    fun onClear() {
        _uiState.update {
            it.copy(
                pin = "",
                confirmPin = "",
                isConfirmStep = false,
                errorMessage = null
            )
        }
    }

    private fun submitAuth(pin: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val isValid = authenticateAdminUseCase(pin)
            _uiState.update { it.copy(isLoading = false) }
            if (isValid) {
                _eventFlow.emit(AdminPinEvent.AuthSuccess)
            } else {
                _uiState.update { it.copy(pin = "", errorMessage = "PIN salah. Silakan coba lagi.") }
            }
        }
    }

    private fun submitSetup(pin: String, confirmPin: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = setAdminPinUseCase(pin, confirmPin)
            _uiState.update { it.copy(isLoading = false) }
            when (result) {
                is Result.Success -> {
                    _eventFlow.emit(AdminPinEvent.AuthSuccess)
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            pin = "",
                            confirmPin = "",
                            isConfirmStep = false,
                            errorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }
}
