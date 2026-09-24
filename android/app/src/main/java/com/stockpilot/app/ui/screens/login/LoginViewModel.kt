package com.stockpilot.app.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockpilot.app.data.api.NetworkClient
import com.stockpilot.app.data.repository.StockPilotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val usernameInput: String = "",
    val passwordInput: String = "",
    val baseUrlInput: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

class LoginViewModel(private val repository: StockPilotRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = _uiState.value.copy(
            baseUrlInput = repository.tokenManager.getBaseUrl()
        )
    }

    fun onUsernameChanged(value: String) {
        _uiState.value = _uiState.value.copy(usernameInput = value, errorMessage = null)
    }

    fun onPasswordChanged(value: String) {
        _uiState.value = _uiState.value.copy(passwordInput = value, errorMessage = null)
    }

    fun onBaseUrlChanged(value: String) {
        _uiState.value = _uiState.value.copy(baseUrlInput = value)
    }

    fun login() {
        val state = _uiState.value
        if (state.usernameInput.isBlank() || state.passwordInput.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please enter both username/email and password.")
            return
        }

        // Apply updated Base URL if changed
        if (state.baseUrlInput.isNotBlank()) {
            repository.tokenManager.setBaseUrl(state.baseUrlInput)
            NetworkClient.resetClient()
        }

        _uiState.value = state.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            val result = repository.login(state.usernameInput.trim(), state.passwordInput)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Login failed. Check server connection."
                )
            }
        }
    }
}
