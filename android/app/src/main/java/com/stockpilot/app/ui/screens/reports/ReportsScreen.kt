package com.stockpilot.app.ui.screens.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stockpilot.app.data.models.CategorySalesPoint
import com.stockpilot.app.ui.screens.dashboard.formatCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadReportData()
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
                title = { Text("Reports & Analytics", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.loadReportData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val kpis = state.kpis

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    // Excel Export Banner Button
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF059669))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Export Excel Financial Workbook",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Generates formatted Excel with Sales, Purchases, Stock, & P&L breakdown.",
                                    color = Color.White.copy(alpha = 0.85f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            Button(
                                onClick = { viewModel.exportExcelAndShare(context) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF059669)),
                                enabled = !state.isExporting
                            ) {
                                if (state.isExporting) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color(0xFF059669))
                                } else {
                                    Icon(Icons.Default.Download, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Export", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                if (kpis != null) {
                    item {
                        Text("Profit & Loss Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                ReportSummaryLine(label = "Gross Sales Revenue", value = formatCurrency(kpis.totalSalesRevenue))
                                ReportSummaryLine(label = "Cost of Goods Sold (COGS)", value = "-${formatCurrency(kpis.totalCogs)}")
                                HorizontalDivider()
                                ReportSummaryLine(label = "Gross Profit Margin", value = formatCurrency(kpis.grossProfit), isBold = true, valueColor = Color(0xFF059669))
                                ReportSummaryLine(label = "Total Operating Expenses", value = "-${formatCurrency(kpis.totalExpenses)}")
                                HorizontalDivider()
                                ReportSummaryLine(
                                    label = "Net Financial Profit",
                                    value = formatCurrency(kpis.netProfit),
                                    isBold = true,
                                    valueColor = if (kpis.netProfit >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                                )
                            }
                        }
                    }
                }

                item {
                    Text("Category Sales Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                if (state.categorySales.isEmpty()) {
                    item {
                        Text("No sales categorized yet.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                } else {
                    items(state.categorySales) { cat ->
                        CategorySalesRow(catSales = cat)
                    }
                }

                item { Spacer(modifier = Modifier.height(40.dp)) }
            }
        }
    }
}

@Composable
fun ReportSummaryLine(
    label: String,
    value: String,
    isBold: Boolean = false,
    valueColor: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            fontSize = if (isBold) 15.sp else 14.sp
        )
        Text(
            text = value,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = if (isBold) 15.sp else 14.sp,
            color = valueColor
        )
    }
}

@Composable
fun CategorySalesRow(catSales: CategorySalesPoint) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(catSales.categoryName, fontWeight = FontWeight.Bold)
                Text(
                    text = "${catSales.quantitySold} Units Sold",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Text(
                text = formatCurrency(catSales.sales),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
