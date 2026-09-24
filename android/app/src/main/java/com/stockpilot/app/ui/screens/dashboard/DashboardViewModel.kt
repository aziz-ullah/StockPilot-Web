package com.stockpilot.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockpilot.app.data.models.DashboardKPIs
import com.stockpilot.app.data.models.LowStockItem
import com.stockpilot.app.data.repository.StockPilotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val kpis: DashboardKPIs? = null,
    val lowStockItems: List<LowStockItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class DashboardViewModel(private val repository: StockPilotRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            val kpiResult = repository.getDashboardKPIs()
            val lowStockResult = repository.getLowStockItems()

            if (kpiResult.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    kpis = kpiResult.getOrNull(),
                    lowStockItems = lowStockResult.getOrDefault(emptyList()),
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = kpiResult.exceptionOrNull()?.message ?: "Failed to load dashboard data"
                )
            }
        }
    }
}
