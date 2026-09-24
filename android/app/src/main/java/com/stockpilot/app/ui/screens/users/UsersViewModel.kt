package com.stockpilot.app.ui.screens.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockpilot.app.data.models.User
import com.stockpilot.app.data.models.UserCreate
import com.stockpilot.app.data.repository.StockPilotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UsersUiState(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class UsersViewModel(private val repository: StockPilotRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(UsersUiState())
    val uiState: StateFlow<UsersUiState> = _uiState.asStateFlow()

    init {
        loadUsers()
    }

    fun loadUsers() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = repository.listUsers()
            _uiState.value = _uiState.value.copy(
                users = result.getOrDefault(emptyList()),
                isLoading = false,
                errorMessage = result.exceptionOrNull()?.message
            )
        }
    }

    fun createUser(email: String, username: String, pass: String, fullName: String, role: String) {
        viewModelScope.launch {
            val userCreate = UserCreate(email, username, pass, fullName, role)
            val result = repository.createUser(userCreate)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(successMessage = "User '$username' created successfully")
                loadUsers()
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = result.exceptionOrNull()?.message)
            }
        }
    }

    fun updateUserStatus(userId: Int, role: String?, isActive: Boolean?) {
        viewModelScope.launch {
            val result = repository.updateUser(userId, role, isActive)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(successMessage = "User updated successfully")
                loadUsers()
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = result.exceptionOrNull()?.message)
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}
