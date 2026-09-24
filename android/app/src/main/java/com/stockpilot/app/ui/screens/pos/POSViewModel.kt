package com.stockpilot.app.ui.screens.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockpilot.app.data.models.Sale
import com.stockpilot.app.data.models.SaleCreate
import com.stockpilot.app.data.models.SaleItemCreate
import com.stockpilot.app.data.models.Variant
import com.stockpilot.app.data.repository.StockPilotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CartItem(
    val variant: Variant,
    var quantity: Int
)

data class POSUiState(
    val variants: List<Variant> = emptyList(),
    val cart: List<CartItem> = emptyList(),
    val searchQuery: String = "",
    val customerName: String = "Walk-in Customer",
    val customerPhone: String = "",
    val paymentMethod: String = "Cash",
    val discount: Double = 0.0,
    val tax: Double = 0.0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val completedSale: Sale? = null
)

class POSViewModel(private val repository: StockPilotRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(POSUiState())
    val uiState: StateFlow<POSUiState> = _uiState.asStateFlow()

    init {
        loadVariants()
    }

    fun loadVariants() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = repository.getAllVariants(search = _uiState.value.searchQuery.ifBlank { null })
            _uiState.value = _uiState.value.copy(
                variants = result.getOrDefault(emptyList()),
                isLoading = false,
                errorMessage = result.exceptionOrNull()?.message
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        loadVariants()
    }

    fun addToCart(variant: Variant) {
        val currentCart = _uiState.value.cart.toMutableList()
        val existingIndex = currentCart.indexOfFirst { it.variant.id == variant.id }
        if (existingIndex >= 0) {
            val item = currentCart[existingIndex]
            if (item.quantity < variant.stockQuantity) {
                currentCart[existingIndex] = item.copy(quantity = item.quantity + 1)
            }
        } else {
            if (variant.stockQuantity > 0) {
                currentCart.add(CartItem(variant = variant, quantity = 1))
            }
        }
        _uiState.value = _uiState.value.copy(cart = currentCart)
    }

    fun updateCartQuantity(variantId: Int, delta: Int) {
        val currentCart = _uiState.value.cart.toMutableList()
        val index = currentCart.indexOfFirst { it.variant.id == variantId }
        if (index >= 0) {
            val item = currentCart[index]
            val newQty = item.quantity + delta
            if (newQty <= 0) {
                currentCart.removeAt(index)
            } else if (newQty <= item.variant.stockQuantity) {
                currentCart[index] = item.copy(quantity = newQty)
            }
        }
        _uiState.value = _uiState.value.copy(cart = currentCart)
    }

    fun removeFromCart(variantId: Int) {
        val currentCart = _uiState.value.cart.filterNot { it.variant.id == variantId }
        _uiState.value = _uiState.value.copy(cart = currentCart)
    }

    fun clearCart() {
        _uiState.value = _uiState.value.copy(cart = emptyList(), completedSale = null)
    }

    fun setCustomerName(name: String) {
        _uiState.value = _uiState.value.copy(customerName = name)
    }

    fun setCustomerPhone(phone: String) {
        _uiState.value = _uiState.value.copy(customerPhone = phone)
    }

    fun setPaymentMethod(method: String) {
        _uiState.value = _uiState.value.copy(paymentMethod = method)
    }

    fun setDiscount(discountAmount: Double) {
        _uiState.value = _uiState.value.copy(discount = discountAmount)
    }

    fun checkout() {
        val state = _uiState.value
        if (state.cart.isEmpty()) return

        _uiState.value = state.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            val saleItems = state.cart.map {
                SaleItemCreate(
                    variantId = it.variant.id,
                    quantity = it.quantity,
                    unitPrice = it.variant.sellingPrice
                )
            }
            val saleCreate = SaleCreate(
                customerName = state.customerName.ifBlank { "Walk-in Customer" },
                customerPhone = state.customerPhone.ifBlank { null },
                paymentMethod = state.paymentMethod,
                discount = state.discount,
                tax = state.tax,
                items = saleItems
            )

            val result = repository.createSale(saleCreate)
            if (result.isSuccess) {
                val createdSale = result.getOrNull()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    completedSale = createdSale,
                    cart = emptyList()
                )
                loadVariants() // Refresh inventory counts
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "POS Checkout failed"
                )
            }
        }
    }

    fun clearCompletedSale() {
        _uiState.value = _uiState.value.copy(completedSale = null)
    }
}
