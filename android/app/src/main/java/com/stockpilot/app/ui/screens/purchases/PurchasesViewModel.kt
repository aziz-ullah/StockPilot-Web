package com.stockpilot.app.ui.screens.purchases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockpilot.app.data.models.*
import com.stockpilot.app.data.repository.StockPilotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PurchasesUiState(
    val purchases: List<Purchase> = emptyList(),
    val suppliers: List<Supplier> = emptyList(),
    val variants: List<Variant> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class PurchasesViewModel(private val repository: StockPilotRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(PurchasesUiState())
    val uiState: StateFlow<PurchasesUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            val purchasesRes = repository.getPurchases()
            val suppliersRes = repository.getSuppliers()
            val variantsRes = repository.getAllVariants()

            _uiState.value = _uiState.value.copy(
                purchases = purchasesRes.getOrDefault(emptyList()),
                suppliers = suppliersRes.getOrDefault(emptyList()),
                variants = variantsRes.getOrDefault(emptyList()),
                isLoading = false,
                errorMessage = purchasesRes.exceptionOrNull()?.message
            )
        }
    }

    fun createSupplier(name: String, contactPerson: String?, phone: String?, email: String?, address: String?) {
        viewModelScope.launch {
            val supplierCreate = SupplierCreate(name, contactPerson, phone, email, address)
            val result = repository.createSupplier(supplierCreate)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(successMessage = "Supplier '$name' added successfully")
                loadData()
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = result.exceptionOrNull()?.message)
            }
        }
    }

    fun createPurchase(supplierId: Int?, supplierName: String?, variantId: Int, quantity: Int, unitCost: Double, notes: String?) {
        viewModelScope.launch {
            val item = PurchaseItemCreate(variantId = variantId, quantity = quantity, unitCost = unitCost)
            val purchaseCreate = PurchaseCreate(
                supplierId = supplierId,
                supplierName = supplierName,
                notes = notes,
                items = listOf(item)
            )

            val result = repository.createPurchase(purchaseCreate)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(successMessage = "Stock purchase recorded successfully")
                loadData()
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = result.exceptionOrNull()?.message)
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}
