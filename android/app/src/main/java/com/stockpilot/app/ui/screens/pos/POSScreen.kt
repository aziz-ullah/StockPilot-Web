package com.stockpilot.app.ui.screens.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stockpilot.app.data.models.Sale
import com.stockpilot.app.data.models.Variant
import com.stockpilot.app.ui.screens.dashboard.formatCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun POSScreen(
    viewModel: POSViewModel,
    onOpenDrawer: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()
    var showCheckoutBottomSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadVariants()
    }

    val subtotal = state.cart.sumOf { it.variant.sellingPrice * it.quantity }
    val grandTotal = (subtotal - state.discount + state.tax).coerceAtLeast(0.0)

    Scaffold(
        topBar = {
            com.stockpilot.app.ui.components.CompactTopHeader(
                title = "POS Sale",
                onOpenDrawer = onOpenDrawer,
                actions = {
                    if (state.cart.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearCart() }) {
                            Text("Clear Cart", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (state.cart.isNotEmpty()) {
                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Cart Total (${state.cart.sumOf { it.quantity }} items)",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = formatCurrency(grandTotal),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Button(
                            onClick = { showCheckoutBottomSheet = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Default.ShoppingCartCheckout, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Proceed to Pay", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Input
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                placeholder = { Text("Scan SKU or Search Products...") },
                leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Product Selection List (Left Column)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Text(
                        text = "Available Inventory",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    if (state.isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.variants) { variant ->
                                POSVariantCard(
                                    variant = variant,
                                    onAddToCart = { viewModel.addToCart(variant) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Cart List (Right Column)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Current Cart",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (state.cart.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Tap products to add to cart",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(state.cart) { cartItem ->
                                    CartItemRow(
                                        cartItem = cartItem,
                                        onIncrease = { viewModel.updateCartQuantity(cartItem.variant.id, 1) },
                                        onDecrease = { viewModel.updateCartQuantity(cartItem.variant.id, -1) },
                                        onRemove = { viewModel.removeFromCart(cartItem.variant.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Checkout Dialog
    if (showCheckoutBottomSheet) {
        CheckoutModalDialog(
            subtotal = subtotal,
            grandTotal = grandTotal,
            state = state,
            onCustomerNameChange = viewModel::setCustomerName,
            onCustomerPhoneChange = viewModel::setCustomerPhone,
            onPaymentMethodChange = viewModel::setPaymentMethod,
            onDiscountChange = viewModel::setDiscount,
            onConfirmCheckout = {
                showCheckoutBottomSheet = false
                viewModel.checkout()
            },
            onDismiss = { showCheckoutBottomSheet = false }
        )
    }

    // Sale Completion Receipt Modal
    state.completedSale?.let { sale ->
        SaleReceiptSuccessModal(
            sale = sale,
            onDismiss = { viewModel.clearCompletedSale() }
        )
    }
}

@Composable
fun POSVariantCard(
    variant: Variant,
    onAddToCart: () -> Unit
) {
    Card(
        onClick = onAddToCart,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        enabled = variant.stockQuantity > 0
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = variant.productName ?: "Product",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "${variant.color} / ${variant.size} • Stock: ${variant.stockQuantity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Text(
                text = formatCurrency(variant.sellingPrice),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun CartItemRow(
    cartItem: CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = cartItem.variant.productName ?: "Item",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRemove, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatCurrency(cartItem.variant.sellingPrice * cartItem.quantity),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDecrease, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease")
                    }
                    Text(
                        text = "${cartItem.quantity}",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    IconButton(onClick = onIncrease, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase")
                    }
                }
            }
        }
    }
}

@Composable
fun CheckoutModalDialog(
    subtotal: Double,
    grandTotal: Double,
    state: POSUiState,
    onCustomerNameChange: (String) -> Unit,
    onCustomerPhoneChange: (String) -> Unit,
    onPaymentMethodChange: (String) -> Unit,
    onDiscountChange: (Double) -> Unit,
    onConfirmCheckout: () -> Unit,
    onDismiss: () -> Unit
) {
    var discountInput by remember { mutableStateOf(if (state.discount > 0) state.discount.toString() else "") }
    val paymentMethods = listOf("Cash", "Card", "Bank Transfer", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Complete Order Payment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = state.customerName,
                    onValueChange = onCustomerNameChange,
                    label = { Text("Customer Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.customerPhone,
                    onValueChange = onCustomerPhoneChange,
                    label = { Text("Customer Phone (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Payment Method:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    paymentMethods.forEach { method ->
                        FilterChip(
                            selected = state.paymentMethod == method,
                            onClick = { onPaymentMethodChange(method) },
                            label = { Text(method) }
                        )
                    }
                }

                OutlinedTextField(
                    value = discountInput,
                    onValueChange = {
                        discountInput = it
                        onDiscountChange(it.toDoubleOrNull() ?: 0.0)
                    },
                    label = { Text("Discount ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Subtotal:")
                    Text(formatCurrency(subtotal))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Grand Total Payable:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(formatCurrency(grandTotal), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmCheckout,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Text("Confirm & Complete Sale")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SaleReceiptSuccessModal(
    sale: Sale,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sale Successful!")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Invoice #: ${sale.invoiceNumber}", fontWeight = FontWeight.Bold)
                Text("Customer: ${sale.customerName}")
                Text("Payment Method: ${sale.paymentMethod}")
                Text("Total Paid: ${formatCurrency(sale.totalAmount)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Gross Profit: ${formatCurrency(sale.grossProfit)}", color = Color(0xFF059669))
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Done")
            }
        }
    )
}
