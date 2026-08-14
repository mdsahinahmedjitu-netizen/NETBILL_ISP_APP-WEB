package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.localization.AppLanguage
import com.example.localization.AppTranslation
import com.example.localization.LocalAppLanguage
import com.example.ui.screens.AlertsScreen
import com.example.ui.screens.BackupScreen
import com.example.ui.screens.BillingScreen
import com.example.ui.screens.CustomerDashboardScreen
import com.example.ui.screens.CustomerDetailScreen
import com.example.ui.screens.CustomerLedgerScreen
import com.example.ui.screens.CustomerManagementScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExpenseScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MikroTikScreen
import com.example.ui.screens.NotificationScreen
import com.example.ui.screens.PackageScreen
import com.example.ui.screens.PaymentCollectionScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SmsTemplateManagementScreen
import com.example.ui.screens.StaffScreen
import com.example.ui.theme.AppTheme
import com.example.ui.theme.CoralWarning
import com.example.ui.theme.IspAmberTertiary
import com.example.ui.theme.Navy800
import com.example.ui.theme.Slate700
import com.example.ui.theme.Teal100
import com.example.ui.theme.Teal50
import com.example.ui.theme.Teal600
import com.example.ui.theme.Teal700
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val currentLang by viewModel.currentLanguage.collectAsState()
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            CompositionLocalProvider(LocalAppLanguage provides currentLang) {
                AppTheme(darkTheme = isDarkMode) {
                    NetBillISPApp(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetBillISPApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val currentUser by viewModel.currentUser.collectAsState()
    val currentCustomer by viewModel.currentCustomer.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val currentLang by viewModel.currentLanguage.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val expiringCustomers by viewModel.expiringTomorrowCustomers.collectAsState()
    val supportTickets by viewModel.supportTickets.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route

    // Handle ViewModel Toast Notifications
    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    if (currentUser == null && currentCustomer == null) {
        LoginScreen(
            viewModel = viewModel,
            onLoginSuccess = {
                // This callback is generic, navigation happens automatically due to state change
            }
        )
        return
    }

    if (currentCustomer != null) {
        CustomerDashboardScreen(viewModel = viewModel)
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerContentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Teal600, Teal700)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Wifi, contentDescription = null, tint = Teal600)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "NetBill ISP",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Logged in: ${currentUser?.name} (${currentUser?.role})",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                // Wrap scrollable items in a weighted column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(10.dp))

                    val drawerItems = listOf(
                        Triple("dashboard", "dashboard_title", Icons.Default.Dashboard),
                        Triple("customers", "customer_crm", Icons.Default.People),
                        Triple("mikrotik", "mikrotik_title", Icons.Default.Router),
                        Triple("billing", "billing_title", Icons.Default.Receipt),
                        Triple("payments", "payments_title", Icons.Default.AttachMoney),
                        Triple("expenses", "expense_title", Icons.Default.Receipt),
                        Triple("staff", "staff_title", Icons.Default.People),
                        Triple("inventory", "Inventory & Stock", Icons.Default.Router),
                        Triple("packages", "packages_title", Icons.Default.Wifi),
                        Triple("reports", "reports_title", Icons.Default.Receipt),
                        Triple("notifications", "sms_title", Icons.Default.Receipt),
                        Triple("sms_templates", "SMS Templates", Icons.Default.Edit),
                        Triple("backup", "backup_title", Icons.Default.Receipt),
                        Triple("settings", "settings_title", Icons.Default.Settings)
                    )

                    drawerItems.forEach { (route, labelKey, icon) ->
                        val isSelected = currentDestination == route
                        NavigationDrawerItem(
                            icon = { Icon(icon, contentDescription = null, tint = if (isSelected) Teal600 else MaterialTheme.colorScheme.onSurfaceVariant) },
                            label = { 
                                Text(
                                    text = AppTranslation(labelKey), 
                                    color = if (isSelected) Teal600 else MaterialTheme.colorScheme.onSurface, 
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                ) 
                            },
                            selected = isSelected,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = if (isDarkMode) Navy800 else Teal50,
                                unselectedContainerColor = Color.Transparent
                            ),
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Quick Night Mode Toggle inside Drawer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleDarkMode() }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.WbSunny else Icons.Default.DarkMode,
                                contentDescription = null,
                                tint = if (isDarkMode) IspAmberTertiary else Teal600,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isDarkMode) "Light Mode / লাইট মোড" else "Night Mode / নাইট মোড",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "ISP Field Dark Vision",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { viewModel.setDarkMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Teal600
                            )
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // Logout Button at the bottom
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Logout, contentDescription = null, tint = Color(0xFFE11D48)) },
                    label = { 
                        Text(
                            text = "Logout / লগ আউট", 
                            color = Color(0xFFE11D48), 
                            fontWeight = FontWeight.Bold 
                        ) 
                    },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.logout()
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "NetBill ISP",
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 18.sp,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "|",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = AppTranslation("dashboard_title"),
                                fontWeight = FontWeight.Bold,
                                color = Teal600,
                                fontSize = 16.sp,
                                maxLines = 1
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    actions = {
                        val overdueCount = expiringCustomers.size
                        val unresolvedTicketsCount = supportTickets.count { it.status != "Resolved" }
                        val totalNotifications = overdueCount + unresolvedTicketsCount

                        // Global Quick Theme Toggle Button
                        IconButton(
                            onClick = { viewModel.toggleDarkMode() },
                            modifier = Modifier.padding(end = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.WbSunny else Icons.Default.DarkMode,
                                contentDescription = if (isDarkMode) "Switch to Light Mode" else "Switch to Night Mode",
                                tint = if (isDarkMode) IspAmberTertiary else Teal600
                            )
                        }

                        IconButton(
                            onClick = {
                                navController.navigate("alerts") {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        ) {
                            if (totalNotifications > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = Color(0xFFE11D48),
                                            contentColor = Color.White
                                        ) {
                                            Text("$totalNotifications", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Notifications,
                                        contentDescription = "Alerts",
                                        tint = if (unresolvedTicketsCount > 0) CoralWarning else Color(0xFFE11D48)
                                    )
                                }
                            } else {
                                Icon(
                                    Icons.Default.NotificationsNone,
                                    contentDescription = "Notifications",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.toggleLanguage() },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isDarkMode) Navy800 else Teal50,
                                contentColor = Teal600
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) Slate700 else Teal100),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp), tint = Teal600)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (currentLang == AppLanguage.BANGLA) "EN" else "বাংলা", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Teal600)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 8.dp
                ) {
                    val bottomNavs = listOf(
                        Triple("dashboard", "nav_dashboard", Icons.Default.Dashboard),
                        Triple("customers", "nav_customers", Icons.Default.People),
                        Triple("billing", "nav_billing", Icons.Default.Receipt),
                        Triple("payments", "nav_payments", Icons.Default.AttachMoney),
                        Triple("mikrotik", "nav_router", Icons.Default.Router)
                    )

                    bottomNavs.forEach { (route, labelKey, icon) ->
                        val isSelected = currentDestination == route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Teal600 else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            label = {
                                Text(
                                    text = AppTranslation(labelKey),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isSelected) Teal600 else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = if (isDarkMode) Navy800 else Teal50,
                                selectedIconColor = Teal600,
                                selectedTextColor = Teal600,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = "dashboard"
                ) {
                    composable("dashboard") {
                        DashboardScreen(
                            viewModel = viewModel,
                            onNavigateToCustomers = { navController.navigate("customers") },
                            onNavigateToPayments = { navController.navigate("payments") },
                            onNavigateToBilling = { navController.navigate("billing") },
                            onNavigateToMikroTik = { navController.navigate("mikrotik") },
                            onNavigateToReports = { navController.navigate("reports") },
                            onNavigateToNotifications = { navController.navigate("notifications") },
                            onNavigateToAlerts = { navController.navigate("alerts") },
                            onSelectCustomer = { customer ->
                                navController.navigate("customer_detail/${customer.id}")
                            }
                        )
                    }

                    composable("customers") {
                        CustomerManagementScreen(
                            viewModel = viewModel,
                            onSelectCustomer = { customer ->
                                navController.navigate("customer_detail/${customer.id}")
                            }
                        )
                    }

                    composable(
                        route = "customer_detail/{customerId}",
                        arguments = listOf(navArgument("customerId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val customerId = backStackEntry.arguments?.getString("customerId") ?: ""
                        val customerList by viewModel.customersList.collectAsState()
                        val customer = customerList.find { it.id == customerId }

                        if (customer != null) {
                            CustomerDetailScreen(
                                customer = customer,
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onCollectPayment = { navController.navigate("payments") },
                                onViewLedger = { navController.navigate("customer_ledger/${customer.id}") }
                            )
                        }
                    }

                    composable(
                        route = "customer_ledger/{customerId}",
                        arguments = listOf(navArgument("customerId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val customerId = backStackEntry.arguments?.getString("customerId") ?: ""
                        val customerList by viewModel.customersList.collectAsState()
                        val customer = customerList.find { it.id == customerId }

                        if (customer != null) {
                            CustomerLedgerScreen(
                                customer = customer,
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }

                    composable("mikrotik") {
                        MikroTikScreen(viewModel = viewModel)
                    }

                    composable("billing") {
                        BillingScreen(viewModel = viewModel)
                    }

                    composable("payments") {
                        PaymentCollectionScreen(viewModel = viewModel)
                    }

                    composable("expenses") {
                        ExpenseScreen(viewModel = viewModel)
                    }

                    composable("staff") {
                        StaffScreen(viewModel = viewModel)
                    }

                    composable("packages") {
                        PackageScreen(viewModel = viewModel)
                    }

                    composable("inventory") {
                        InventoryScreen(viewModel = viewModel)
                    }

                    composable("reports") {
                        ReportsScreen(mainViewModel = viewModel)
                    }

                    composable("notifications") {
                        NotificationScreen(
                            viewModel = viewModel,
                            onNavigateToTemplates = { navController.navigate("sms_templates") }
                        )
                    }

                    composable("alerts") {
                        AlertsScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("sms_templates") {
                        SmsTemplateManagementScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("backup") {
                        BackupScreen(viewModel = viewModel)
                    }

                    composable("settings") {
                        SettingsScreen(
                            viewModel = viewModel,
                            onLogout = {
                                navController.navigate("dashboard") {
                                    popUpTo(0)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
