package com.stockpilot.app.ui.screens.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockpilot.app.data.models.Category
import com.stockpilot.app.data.models.Product
import com.stockpilot.app.data.models.ProductCreate
import com.stockpilot.app.data.models.VariantCreate
import com.stockpilot.app.data.repository.StockPilotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class InventoryUiState(
    val products: List<Product> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Int? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class InventoryViewModel(private val repository: StockPilotRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            val categoriesResult = repository.getCategories()
            val productsResult = repository.getProducts(
                categoryId = _uiState.value.selectedCategoryId,
                search = _uiState.value.searchQuery.ifBlank { null }
            )

            _uiState.value = _uiState.value.copy(
                categories = categoriesResult.getOrDefault(emptyList()),
                products = productsResult.getOrDefault(emptyList()),
                isLoading = false,
                errorMessage = productsResult.exceptionOrNull()?.message
            )
        }
    }

    fun onCategorySelected(categoryId: Int?) {
        _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId)
        loadData()
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        loadData()
    }

    fun createCategory(name: String, description: String?) {
        viewModelScope.launch {
            val result = repository.createCategory(name, description)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(successMessage = "Category '$name' created successfully")
                loadData()
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = result.exceptionOrNull()?.message)
            }
        }
    }

    fun createProduct(categoryId: Int, name: String, description: String?, color: String, size: String, costPrice: Double, sellingPrice: Double, quantity: Int) {
        viewModelScope.launch {
            val variant = VariantCreate(
                color = color,
                size = size,
                costPrice = costPrice,
                sellingPrice = sellingPrice,
                stockQuantity = quantity
            )
            val productCreate = ProductCreate(
                categoryId = categoryId,
                name = name,
                description = description,
                variants = listOf(variant)
            )
            val result = repository.createProduct(productCreate)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(successMessage = "Product '$name' created successfully")
                loadData()
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = result.exceptionOrNull()?.message)
            }
        }
    }

    fun adjustStock(variantId: Int, adjustment: Int, notes: String?) {
        viewModelScope.launch {
            val result = repository.adjustStock(variantId, adjustment, notes)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(successMessage = "Stock adjusted successfully")
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
