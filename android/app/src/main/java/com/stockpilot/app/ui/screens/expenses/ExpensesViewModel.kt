package com.stockpilot.app.ui.screens.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockpilot.app.data.models.Expense
import com.stockpilot.app.data.models.ExpenseCreate
import com.stockpilot.app.data.repository.StockPilotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExpensesUiState(
    val expenses: List<Expense> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class ExpensesViewModel(private val repository: StockPilotRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpensesUiState())
    val uiState: StateFlow<ExpensesUiState> = _uiState.asStateFlow()

    init {
        loadExpenses()
    }

    fun loadExpenses() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = repository.getExpenses()
            _uiState.value = _uiState.value.copy(
                expenses = result.getOrDefault(emptyList()),
                isLoading = false,
                errorMessage = result.exceptionOrNull()?.message
            )
        }
    }

    fun createExpense(category: String, description: String, amount: Double) {
        viewModelScope.launch {
            val expenseCreate = ExpenseCreate(category, description, amount)
            val result = repository.createExpense(expenseCreate)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(successMessage = "Expense recorded successfully")
                loadExpenses()
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = result.exceptionOrNull()?.message)
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}
