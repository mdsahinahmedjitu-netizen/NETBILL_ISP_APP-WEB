package com.example

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val currentUser by viewModel.currentUser.collectAsState()
            val currentCustomer by viewModel.currentCustomer.collectAsState()
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            AppTheme(darkTheme = isDarkMode) {
                when {
                    currentCustomer != null -> {
                        CustomerDashboardScreen(viewModel = viewModel)
                    }
                    currentUser != null -> {
                        MainApp(viewModel = viewModel)
                    }
                    else -> {
                        LoginScreen(viewModel = viewModel, onLoginSuccess = {})
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(viewModel: MainViewModel) {
    var activePage by remember { mutableStateOf("dashboard") }
    val currentUser by viewModel.currentUser.collectAsState()
    val permissions by viewModel.currentPermissions.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    @Composable
    fun t(key: String) = com.example.localization.AppTranslation(key)
    
    val menuItems = remember(permissions, currentUser) {
        val items = mutableListOf<MenuItem>()
        items.add(MenuItem("dashboard", "dashboard_overview", Icons.Default.Dashboard))
        
        if (permissions.canAccessCustomers) {
            items.add(MenuItem("customers", "subscribers_crm", Icons.Default.People))
        }
        
        if (permissions.canAccessTickets) {
            items.add(MenuItem("crm_tickets", "support_tickets", Icons.Default.Headset))
        }
        
        if (permissions.canAccessPayments) {
            items.add(MenuItem("payments", "payment_center", Icons.Default.Payment))
        }
        
        if (permissions.canAccessReports) {
            items.add(MenuItem("reports", "collection_report", Icons.Default.Assessment))
        }
        
        if (currentUser?.role?.lowercase() == "admin") {
            items.add(MenuItem("expenses", "expense_title", Icons.Default.MoneyOff))
            items.add(MenuItem("staff", "staff_team", Icons.Default.Badge))
        }
        
        if (permissions.canAccessSalary) {
            items.add(MenuItem("salary_ledger", "salary_ledger", Icons.Default.AccountBalanceWallet))
        }
        
        if (permissions.canAccessInventory) {
            items.add(MenuItem("inventory", "inventory_stock", Icons.Default.Inventory))
        }

        if (permissions.canAccessPackages) {
            items.add(MenuItem("packages", "service_packages", Icons.Default.Wifi))
        }

        if (currentUser?.role?.lowercase() == "admin") {
            items.add(MenuItem("infrastructure", "infrastructure", Icons.Default.Router))
            items.add(MenuItem("sms_setup", "sms_setup", Icons.Default.Sms))
            items.add(MenuItem("sms_logs", "sms_history", Icons.Default.History))
            items.add(MenuItem("settings", "global_settings", Icons.Default.Settings))
        }

        items
    }

    val drawerBgColor = when (currentUser?.role?.lowercase()) {
        "admin" -> Color(0xFF0F172A)
        "customer" -> Color(0xFF064E3B)
        else -> Color(0xFF1E1B4B)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = drawerBgColor,
                drawerContentColor = Color.White,
                drawerShape = RoundedCornerShape(0.dp)
            ) {
                Spacer(Modifier.height(40.dp))
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Teal600
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Bolt, null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                    }
                    Column {
                        Text("NETBILL ISP", fontWeight = FontWeight.Black, fontSize = 22.sp, color = Color.White, letterSpacing = (-1).sp)
                        val roleTitle = when (currentUser?.role?.lowercase()) {
                            "admin" -> "Admin Portal"
                            "customer" -> "Customer Portal"
                            else -> "Staff Portal"
                        }
                        Text(roleTitle.uppercase(), fontSize = 9.sp, color = Color(0xFF2DD4BF), fontWeight = FontWeight.Black, letterSpacing = 3.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), color = Color.White.copy(alpha = 0.1f))
                Spacer(Modifier.height(16.dp))
                
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    menuItems.forEach { item ->
                        val isSelected = activePage == item.id
                        NavigationDrawerItem(
                            label = { 
                                Text(
                                    text = t(item.label).uppercase(), 
                                    fontWeight = FontWeight.Black, 
                                    fontSize = 11.sp, 
                                    letterSpacing = 1.5.sp,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f)
                                ) 
                            },
                            selected = isSelected,
                            onClick = {
                                activePage = item.id
                                scope.launch { drawerState.close() }
                            },
                            icon = { 
                                Icon(
                                    item.icon, 
                                    contentDescription = null, 
                                    tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(20.dp)
                                ) 
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = Teal600,
                                unselectedContainerColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(56.dp)
                        )
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), color = Color.White.copy(alpha = 0.1f))
                
                NavigationDrawerItem(
                    label = { 
                        Text(
                            text = t("sign_out").uppercase(), 
                            color = Color(0xFFF43F5E), 
                            fontWeight = FontWeight.Black, 
                            fontSize = 11.sp, 
                            letterSpacing = 2.sp 
                        ) 
                    },
                    selected = false,
                    onClick = { viewModel.logout() },
                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color(0xFFF43F5E)) },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
                    modifier = Modifier.padding(12.dp)
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { 
                        Text("NETBILL ISP", fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 2.sp) 
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = androidx.compose.ui.graphics.Color.White,
                        titleContentColor = Slate900
                    )
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (activePage) {
                    "dashboard" -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToCustomers = { activePage = "customers" },
                        onNavigateToPayments = { activePage = "payments" },
                        onNavigateToBilling = { activePage = "billing" },
                        onNavigateToMikroTik = { activePage = "mikrotik" },
                        onNavigateToReports = { activePage = "reports" },
                        onNavigateToNotifications = { activePage = "notifications" },
                        onNavigateToAlerts = { activePage = "alerts" },
                        onSelectCustomer = { activePage = "customers" }
                    )
                    "customers" -> CustomerManagementScreen(viewModel = viewModel, onSelectCustomer = { /* Detail */ })
                    "payments" -> PaymentCollectionScreen(viewModel = viewModel)
                    "billing" -> BillingScreen(viewModel = viewModel)
                    "reports" -> ReportsScreen(mainViewModel = viewModel)
                    "salary_ledger" -> StaffSalaryHistoryScreen(viewModel = viewModel, onBack = { activePage = "dashboard" })
                    "staff" -> StaffScreen(viewModel = viewModel, onNavigateToLedger = { activePage = "salary_ledger" })
                    "inventory" -> InventoryScreen(viewModel = viewModel, onBack = { activePage = "dashboard" })
                    "packages" -> PackageScreen(viewModel = viewModel, onBack = { activePage = "dashboard" })
                    "expenses" -> ExpenseScreen(viewModel = viewModel, onBack = { activePage = "dashboard" })
                    "crm_tickets" -> SupportTicketsScreen(viewModel = viewModel, onBack = { activePage = "dashboard" })
                    "alerts" -> AlertsScreen(viewModel = viewModel, onBack = { activePage = "dashboard" })
                    "infrastructure" -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Network Assets (Coming Soon)") }
                    "sms_setup" -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("SMS Setup (Coming Soon)") }
                    "sms_logs" -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("SMS History (Coming Soon)") }
                    "settings" -> SettingsScreen(viewModel = viewModel, onBack = { activePage = "dashboard" }, onLogout = { viewModel.logout() })
                    else -> DashboardScreen(
                        viewModel = viewModel, 
                        onNavigateToCustomers = { activePage = "customers" }, 
                        onNavigateToPayments = { activePage = "payments" }, 
                        onNavigateToBilling = { activePage = "billing" }, 
                        onNavigateToMikroTik = { activePage = "mikrotik" },
                        onNavigateToReports = { activePage = "reports" }
                    )
                }
            }
        }
    }
}

data class MenuItem(val id: String, val label: String, val icon: ImageVector)
