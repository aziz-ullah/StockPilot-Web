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
import androidx.compose.ui.unit.sp
import java.util.Locale
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
                            ModalDrawerSheet(
                                drawerContainerColor = androidx.compose.ui.graphics.Color(0xFF1E222D),
                                drawerContentColor = androidx.compose.ui.graphics.Color.White
                            ) {
                                Spacer(modifier = Modifier.height(16.dp))
                                // Top Header Row matching reference design (Avatar + Name + MoreVert)
                                Row(
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 8.dp)
                                ) {
                                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                        Surface(
                                            shape = androidx.compose.foundation.shape.CircleShape,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                                                Text(
                                                    text = (tokenManager.getUserName() ?: "U").take(1).uppercase(Locale.getDefault()),
                                                    fontWeight = FontWeight.Bold,
                                                    color = androidx.compose.ui.graphics.Color.White,
                                                    fontSize = 16.sp
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = tokenManager.getUserName() ?: "User",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = androidx.compose.ui.graphics.Color.White
                                            )
                                            Text(
                                                text = tokenManager.getUserRole() ?: "Staff",
                                                fontSize = 12.sp,
                                                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                    IconButton(onClick = { }) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Options",
                                            tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.12f))
                                Spacer(modifier = Modifier.height(12.dp))

                                navItems.forEach { screen ->
                                    val isSelected = currentRoute == screen.route
                                    NavigationDrawerItem(
                                        icon = {
                                            Icon(
                                                screen.icon,
                                                contentDescription = null,
                                                tint = if (isSelected) androidx.compose.ui.graphics.Color(0xFF00E5FF) else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
                                            )
                                        },
                                        label = {
                                            Text(
                                                screen.title,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f)
                                            )
                                        },
                                        selected = isSelected,
                                        colors = NavigationDrawerItemDefaults.colors(
                                            selectedContainerColor = androidx.compose.ui.graphics.Color(0xFF00E5FF).copy(alpha = 0.2f),
                                            unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                                        ),
                                        onClick = {
                                            scope.launch { drawerState.close() }
                                            if (currentRoute != screen.route) {
                                                navController.navigate(screen.route) {
                                                    popUpTo(navController.graph.findStartDestination().id)
                                                    launchSingleTop = true
                                                }
                                            }
                                        },
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.weight(1f))
                                HorizontalDivider(color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.12f))
                                NavigationDrawerItem(
                                    icon = { Icon(Icons.Default.Logout, contentDescription = null, tint = androidx.compose.ui.graphics.Color(0xFFFF5252)) },
                                    label = { Text("Sign Out", color = androidx.compose.ui.graphics.Color(0xFFFF5252)) },
                                    selected = false,
                                    onClick = {
                                        scope.launch { drawerState.close() }
                                        repository.logout()
                                        navController.navigate("login") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
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
                                onOpenDrawer = { scope.launch { drawerState.open() } },
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
    onOpenDrawer: (() -> Unit)? = null,
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
                onNavigateToInventory = { navController.navigate(Screen.Inventory.route) },
                onOpenDrawer = onOpenDrawer
            )
        }

        composable(Screen.Inventory.route) {
            val vm = remember { InventoryViewModel(repository) }
            InventoryScreen(viewModel = vm, onOpenDrawer = onOpenDrawer)
        }

        composable(Screen.POS.route) {
            val vm = remember { POSViewModel(repository) }
            POSScreen(viewModel = vm, onOpenDrawer = onOpenDrawer)
        }

        composable(Screen.Purchases.route) {
            val vm = remember { PurchasesViewModel(repository) }
            PurchasesScreen(viewModel = vm, onOpenDrawer = onOpenDrawer)
        }

        composable(Screen.Expenses.route) {
            val vm = remember { ExpensesViewModel(repository) }
            ExpensesScreen(viewModel = vm, onOpenDrawer = onOpenDrawer)
        }

        composable(Screen.Reports.route) {
            val vm = remember { ReportsViewModel(repository) }
            ReportsScreen(viewModel = vm, onOpenDrawer = onOpenDrawer)
        }

        composable(Screen.Users.route) {
            val vm = remember { UsersViewModel(repository) }
            UsersScreen(viewModel = vm, onOpenDrawer = onOpenDrawer)
        }
    }
}
