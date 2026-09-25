package com.stockpilot.app.ui.screens.purchases

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.stockpilot.app.data.models.Purchase
import com.stockpilot.app.data.models.Supplier
import com.stockpilot.app.data.models.Variant
import com.stockpilot.app.ui.screens.dashboard.formatCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchasesScreen(
    viewModel: PurchasesViewModel
) {
    val state by viewModel.uiState.collectAsState()
    var showAddSupplierDialog by remember { mutableStateOf(false) }
    var showAddPurchaseDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    LaunchedEffect(state.errorMessage, state.successMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Stock Purchases (Inward)", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showAddSupplierDialog = true }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add Supplier")
                    }
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddPurchaseDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Record Purchase")
            }
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Suppliers (${state.suppliers.size}):", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.suppliers) { s ->
                            AssistChip(
                                onClick = {},
                                label = { Text("${s.name} ${if (s.phone != null) "(${s.phone})" else ""}") },
                                leadingIcon = { Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Purchase Records", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                if (state.purchases.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Text(
                                text = "No stock purchases recorded yet.",
                                modifier = Modifier.padding(24.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    items(state.purchases) { purchase ->
                        PurchaseRowCard(purchase = purchase)
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    if (showAddSupplierDialog) {
        AddSupplierDialog(
            onDismiss = { showAddSupplierDialog = false },
            onConfirm = { name, contact, phone, email, addr ->
                viewModel.createSupplier(name, contact, phone, email, addr)
                showAddSupplierDialog = false
            }
        )
    }

    if (showAddPurchaseDialog) {
        AddPurchaseDialog(
            suppliers = state.suppliers,
            variants = state.variants,
            onDismiss = { showAddPurchaseDialog = false },
            onConfirm = { suppId, suppName, varId, qty, cost, notes ->
                viewModel.createPurchase(suppId, suppName, varId, qty, cost, notes)
                showAddPurchaseDialog = false
            }
        )
    }
}

@Composable
fun PurchaseRowCard(purchase: Purchase) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = purchase.purchaseNumber,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Surface(
                    color = Color(0xFFD1FAE5),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = purchase.paymentStatus,
                        color = Color(0xFF047857),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Supplier: ${purchase.supplierName ?: "Direct Purchase"} • Date: ${purchase.purchaseDate.take(10)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(8.dp))
            purchase.items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${item.productName ?: "Item"} (${item.color}/${item.size}) x ${item.quantity}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = formatCurrency(item.totalCost),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Cost:", fontWeight = FontWeight.Bold)
                Text(formatCurrency(purchase.totalAmount), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun AddSupplierDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, contact: String?, phone: String?, email: String?, addr: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Supplier") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Company / Supplier Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("Contact Person") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), contact.ifBlank { null }, phone.ifBlank { null }, email.ifBlank { null }, null) },
                enabled = name.isNotBlank()
            ) {
                Text("Save Supplier")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddPurchaseDialog(
    suppliers: List<Supplier>,
    variants: List<Variant>,
    onDismiss: () -> Unit,
    onConfirm: (suppId: Int?, suppName: String?, variantId: Int, qty: Int, cost: Double, notes: String?) -> Unit
) {
    var selectedSupplierId by remember { mutableStateOf<Int?>(suppliers.firstOrNull()?.id) }
    var selectedVariantId by remember { mutableStateOf(variants.firstOrNull()?.id ?: 0) }
    var quantityInput by remember { mutableStateOf("10") }
    var unitCostInput by remember { mutableStateOf("0.00") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Stock Purchase (Inward)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Supplier:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(suppliers) { s ->
                        FilterChip(
                            selected = selectedSupplierId == s.id,
                            onClick = { selectedSupplierId = s.id },
                            label = { Text(s.name) }
                        )
                    }
                }

                Text("Select Product SKU Variant:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(variants) { v ->
                        FilterChip(
                            selected = selectedVariantId == v.id,
                            onClick = {
                                selectedVariantId = v.id
                                unitCostInput = v.costPrice.toString()
                            },
                            label = { Text("${v.productName ?: "Product"} (${v.color}/${v.size})") }
                        )
                    }
                }

                OutlinedTextField(
                    value = quantityInput,
                    onValueChange = { quantityInput = it },
                    label = { Text("Quantity Purchased") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                OutlinedTextField(
                    value = unitCostInput,
                    onValueChange = { unitCostInput = it },
                    label = { Text("Unit Purchase Cost ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Reference # (Optional)") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val selectedSupp = suppliers.find { it.id == selectedSupplierId }
                    if (selectedVariantId > 0) {
                        onConfirm(
                            selectedSupplierId,
                            selectedSupp?.name,
                            selectedVariantId,
                            quantityInput.toIntOrNull() ?: 1,
                            unitCostInput.toDoubleOrNull() ?: 0.0,
                            notes.ifBlank { null }
                        )
                    }
                },
                enabled = selectedVariantId > 0
            ) {
                Text("Submit Purchase")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
