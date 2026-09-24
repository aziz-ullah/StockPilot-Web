package com.stockpilot.app.ui.screens.reports

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockpilot.app.data.models.CategorySalesPoint
import com.stockpilot.app.data.models.DashboardKPIs
import com.stockpilot.app.data.models.TrendChartPoint
import com.stockpilot.app.data.repository.StockPilotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

data class ReportsUiState(
    val kpis: DashboardKPIs? = null,
    val salesTrend: List<TrendChartPoint> = emptyList(),
    val categorySales: List<CategorySalesPoint> = emptyList(),
    val isLoading: Boolean = false,
    val isExporting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class ReportsViewModel(private val repository: StockPilotRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        loadReportData()
    }

    fun loadReportData() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            val kpisRes = repository.getDashboardKPIs()
            val trendRes = repository.getSalesTrend(30)
            val catRes = repository.getCategorySales()

            _uiState.value = _uiState.value.copy(
                kpis = kpisRes.getOrNull(),
                salesTrend = trendRes.getOrDefault(emptyList()),
                categorySales = catRes.getOrDefault(emptyList()),
                isLoading = false,
                errorMessage = kpisRes.exceptionOrNull()?.message
            )
        }
    }

    fun exportExcelAndShare(context: Context) {
        _uiState.value = _uiState.value.copy(isExporting = true, errorMessage = null)

        viewModelScope.launch {
            val streamResult = repository.exportExcelStream()
            if (streamResult.isSuccess) {
                try {
                    val inputStream = streamResult.getOrNull()!!
                    val file = File(context.cacheDir, "StockPilot_Financial_Report.xlsx")
                    FileOutputStream(file).use { output ->
                        inputStream.copyTo(output)
                    }

                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )

                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share StockPilot Report Excel"))

                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        successMessage = "Excel exported successfully!"
                    )
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        errorMessage = "Failed to save Excel report file: ${e.localizedMessage}"
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    errorMessage = streamResult.exceptionOrNull()?.message ?: "Excel export failed"
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}
