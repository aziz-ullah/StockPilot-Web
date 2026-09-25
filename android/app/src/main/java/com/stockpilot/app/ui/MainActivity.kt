package com.stockpilot.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.stockpilot.app.data.local.TokenManager
import com.stockpilot.app.data.repository.StockPilotRepository
import com.stockpilot.app.ui.screens.dashboard.DashboardScreen
import com.stockpilot.app.ui.screens.dashboard.DashboardViewModel
import com.stockpilot.app.ui.screens.expenses.ExpensesScreen
import com.stockpilot.app.ui.screens.expenses.ExpensesViewModel
import com.stockpilot.app.ui.screens.inventory.InventoryScreen
import com.stockpilot.app.ui.screens.inventory.InventoryViewModel
import com.stockpilot.app.ui.screens.login.LoginScreen
import com.stockpilot.app.ui.screens.login.LoginViewModel
import com.stockpilot.app.ui.screens.pos.POSScreen
import com.stockpilot.app.ui.screens.pos.POSViewModel
import com.stockpilot.app.ui.screens.purchases.PurchasesScreen
import com.stockpilot.app.ui.screens.purchases.PurchasesViewModel
import com.stockpilot.app.ui.screens.reports.ReportsScreen
import com.stockpilot.app.ui.screens.reports.ReportsViewModel
import com.stockpilot.app.ui.screens.users.UsersScreen
import com.stockpilot.app.ui.screens.users.UsersViewModel
import com.stockpilot.app.ui.theme.StockPilotTheme
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Inventory : Screen("inventory", "Inventory", Icons.Default.Inventory)
    object POS : Screen("pos", "POS Sale", Icons.Default.PointOfSale)
    object Purchases : Screen("purchases", "Purchases", Icons.Default.LocalShipping)
    object Expenses : Screen("expenses", "Expenses", Icons.Default.ReceiptLong)
    object Reports : Screen("reports", "Reports", Icons.Default.BarChart)
    object Users : Screen("users", "Users", Icons.Default.People)
}

class MainActivity : ComponentActivity() {

    private lateinit var tokenManager: TokenManager
    private lateinit var repository: StockPilotRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tokenManager = TokenManager(this)
        repository = StockPilotRepository(tokenManager)

        setContent {
            StockPilotTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                val isLoggedIn = tokenManager.isLoggedIn()
                val startDestination = if (isLoggedIn) Screen.Dashboard.route else "login"

                val navItems = mutableListOf(
                    Screen.Dashboard,
                    Screen.Inventory,
                    Screen.POS,
                    Screen.Purchases,
                    Screen.Expenses,
                    Screen.Reports
                )
                if (tokenManager.getUserRole() == "Admin") {
                    navItems.add(Screen.Users)
                }

                val showTopAndBottomBars = currentRoute != null && currentRoute != "login"

                if (showTopAndBottomBars) {
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            ModalDrawerSheet {
                                Spacer(modifier = Modifier.height(24.dp))
                                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                                    Text(
                                        text = "StockPilot Mobile",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Logged in as: ${tokenManager.getUserName() ?: "User"} (${tokenManager.getUserRole() ?: "Staff"})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                                HorizontalDivider()

                                navItems.forEach { screen ->
                                    NavigationDrawerItem(
                                        icon = { Icon(screen.icon, contentDescription = null) },
                                        label = { Text(screen.title) },
                                        selected = currentRoute == screen.route,
                                        onClick = {
                                            scope.launch { drawerState.close() }
                                            if (currentRoute != screen.route) {
                                                navController.navigate(screen.route) {
                                                    popUpTo(navController.graph.findStartDestination().id)
                                                    launchSingleTop = true
                                                }
                                            }
                                        },
                                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                    )
                                }

                                Spacer(modifier = Modifier.weight(1f))
                                HorizontalDivider()
                                NavigationDrawerItem(
                                    icon = { Icon(Icons.Default.Logout, contentDescription = null) },
                                    label = { Text("Sign Out") },
                                    selected = false,
                                    onClick = {
                                        scope.launch { drawerState.close() }
                                        repository.logout()
                                        navController.navigate("login") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    ) {
                        Scaffold(
                            bottomBar = {
                                NavigationBar {
                                    val bottomNavItems = listOf(
                                        Screen.Dashboard,
                                        Screen.Inventory,
                                        Screen.POS,
                                        Screen.Reports
                                    )
                                    bottomNavItems.forEach { screen ->
                                        NavigationBarItem(
                                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                                            label = { Text(screen.title) },
                                            selected = currentRoute == screen.route,
                                            onClick = {
                                                if (currentRoute != screen.route) {
                                                    navController.navigate(screen.route) {
                                                        popUpTo(navController.graph.findStartDestination().id)
                                                        launchSingleTop = true
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        ) { innerPadding ->
                            AppNavHost(
                                navController = navController,
                                startDestination = startDestination,
                                repository = repository,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                } else {
                    AppNavHost(
                        navController = navController,
                        startDestination = startDestination,
                        repository = repository,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun AppNavHost(
    navController: androidx.navigation.NavHostController,
    startDestination: String,
    repository: StockPilotRepository,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable("login") {
            val loginViewModel = remember { LoginViewModel(repository) }
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            val vm = remember { DashboardViewModel(repository) }
            DashboardScreen(
                viewModel = vm,
                onNavigateToPOS = { navController.navigate(Screen.POS.route) },
                onNavigateToInventory = { navController.navigate(Screen.Inventory.route) }
            )
        }

        composable(Screen.Inventory.route) {
            val vm = remember { InventoryViewModel(repository) }
            InventoryScreen(viewModel = vm)
        }

        composable(Screen.POS.route) {
            val vm = remember { POSViewModel(repository) }
            POSScreen(viewModel = vm)
        }

        composable(Screen.Purchases.route) {
            val vm = remember { PurchasesViewModel(repository) }
            PurchasesScreen(viewModel = vm)
        }

        composable(Screen.Expenses.route) {
            val vm = remember { ExpensesViewModel(repository) }
            ExpensesScreen(viewModel = vm)
        }

        composable(Screen.Reports.route) {
            val vm = remember { ReportsViewModel(repository) }
            ReportsScreen(viewModel = vm)
        }

        composable(Screen.Users.route) {
            val vm = remember { UsersViewModel(repository) }
            UsersScreen(viewModel = vm)
        }
    }
}
