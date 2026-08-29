package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.CustomerEntity
import com.example.localization.AppLanguage
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
    var selectedProfileId by remember { mutableStateOf<String?>(null) }
    
    val currentUser by viewModel.currentUser.collectAsState()
    val permissions by viewModel.currentPermissions.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val currentLang by viewModel.currentLanguage.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Global Modals State
    var showGlobalSearch by remember { mutableStateOf(false) }
    var showSummarySearch by remember { mutableStateOf(false) }
    var showExpiryModal by remember { mutableStateOf(false) }

    val customersList by viewModel.customersList.collectAsState()
    val expiringTomorrow = remember(customersList) {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DATE, 1)
        val tomorrow = sdf.format(cal.time)
        
        customersList.filter { c -> 
            val isDue = (c.currentDue > 0)
            c.status == "Active" && isDue && c.expireDate == tomorrow
        }
    }

    @Composable
    fun t(key: String) = com.example.localization.appTranslation(key)

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

        if (permissions.canAccessExpenses) {
            items.add(MenuItem("expenses", "expense_title", Icons.Default.MoneyOff))
        }

        if (permissions.canAccessStaff) {
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

        if (permissions.canAccessInfrastructure) {
            items.add(MenuItem("infrastructure", "infrastructure", Icons.Default.Router))
        }

        if (permissions.canAccessSMS) {
            items.add(MenuItem("sms_setup", "sms_setup", Icons.Default.Sms))
        }

        if (permissions.canAccessSmsLogs) {
            items.add(MenuItem("sms_logs", "sms_history", Icons.Default.History))
        }

        if (permissions.canAccessGlobalSettings) {
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
                drawerShape = RoundedCornerShape(0.dp),
                modifier = Modifier.width(300.dp)
            ) {
                Spacer(Modifier.height(40.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF2DD4BF),
                        shadowElevation = 8.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Bolt, null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }
                    Column {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Black)) { append("NETBILL ") }
                                withStyle(style = SpanStyle(color = Color(0xFF2DD4BF), fontWeight = FontWeight.Black)) { append("ISP") }
                            },
                            fontSize = 24.sp,
                            color = Color.White,
                            letterSpacing = (-1).sp
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { scope.launch { drawerState.close() } }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, null, tint = Color(0xFF94A3B8))
                    }
                }

                Spacer(Modifier.height(10.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), color = Color.White.copy(alpha = 0.1f))
                Spacer(Modifier.height(24.dp))

                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    menuItems.forEach { item ->
                        val isSelected = activePage == item.id
                        NavigationDrawerItem(
                            label = {
                                Text(
                                    text = t(item.label).uppercase(),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    letterSpacing = 2.sp,
                                    color = if (isSelected) Color.White else Color(0xFF94A3B8)
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
                                    tint = if (isSelected) Color.White else Color(0xFF94A3B8),
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = Color(0xFF0D9488),
                                unselectedContainerColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .height(64.dp)
                                .then(if (isSelected) Modifier.shadow(20.dp, spotColor = Color(0xFF0D9488).copy(alpha = 0.5f), shape = RoundedCornerShape(20.dp)) else Modifier)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), color = Color.White.copy(alpha = 0.1f))

                NavigationDrawerItem(
                    label = {
                        Text(
                            text = t("sign_out").uppercase(),
                            color = Color(0xFFF43F5E),
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            letterSpacing = 2.sp
                        )
                    },
                    selected = false,
                    onClick = { viewModel.logout() },
                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color(0xFFF43F5E)) },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color(0xFFF43F5E).copy(alpha = 0.1f)),
                    modifier = Modifier.padding(16.dp),
                    shape = RoundedCornerShape(20.dp)
                )
                Spacer(Modifier.height(32.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                Surface(
                    shadowElevation = 8.dp,
                    color = if (isDarkMode) Color(0xFF1E293B) else Color.White,
                    border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF334155) else Color(0xFFF1F5F9))
                ) {
                    TopAppBar(
                        modifier = Modifier.height(80.dp),
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(style = SpanStyle(fontWeight = FontWeight.Black)) { append("NetBill ") }
                                        withStyle(style = SpanStyle(color = if (isDarkMode) Color.White else Color(0xFF64748B), fontWeight = FontWeight.Bold)) { append("ISP | ") }
                                    },
                                    fontSize = 16.sp,
                                    letterSpacing = (-1).sp
                                )
                                Text(
                                    text = activePage.uppercase(),
                                    color = Color(0xFF0D9488),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    letterSpacing = 1.sp
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier
                                    .padding(start = 16.dp)
                                    .size(52.dp)
                                    .background(if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFC), RoundedCornerShape(16.dp))
                            ) {
                                Icon(
                                    imageVector = if (drawerState.isOpen) Icons.AutoMirrored.Filled.FormatIndentDecrease else Icons.AutoMirrored.Filled.FormatIndentIncrease,
                                    contentDescription = "Menu",
                                    tint = Color(0xFF0D9488),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { 
                                    val newLang = if (currentLang == AppLanguage.ENGLISH) AppLanguage.BANGLA else AppLanguage.ENGLISH
                                    viewModel.setLanguage(newLang) 
                                },
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFC), RoundedCornerShape(16.dp))
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Language, null, tint = Color(0xFF0D9488), modifier = Modifier.size(14.dp))
                                    Text(if (currentLang == AppLanguage.ENGLISH) "বাংলা" else "EN", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            IconButton(
                                onClick = { viewModel.setDarkMode(!isDarkMode) },
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFC), RoundedCornerShape(16.dp))
                            ) {
                                Icon(
                                    imageVector = if (isDarkMode) Icons.Default.WbSunny else Icons.Default.NightsStay,
                                    contentDescription = "Dark Mode",
                                    tint = if (isDarkMode) Color(0xFFFBBF24) else Color(0xFF0D9488),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Row(
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFC))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(12.dp), color = Color(0xFF0D9488)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(currentUser?.name?.take(1)?.uppercase() ?: "A", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.padding(end = 4.dp)) {
                                    Text(currentUser?.name?.uppercase() ?: "ADMIN", fontSize = 11.sp, fontWeight = FontWeight.Black, color = if (isDarkMode) Color.White else Color(0xFF0F172A), letterSpacing = 1.sp)
                                    Text(
                                        if (currentUser?.role?.lowercase() == "admin") t("super_admin").uppercase() else "FIELD STAFF",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF0D9488),
                                        letterSpacing = 2.sp
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = if (isDarkMode) Color.White else Color(0xFF0F172A)
                        )
                    )
                }
            },
            floatingActionButton = {
                if (expiringTomorrow.isNotEmpty() && permissions.canSeeExpiryAlerts) {
                    FloatingActionButton(
                        onClick = { showExpiryModal = true },
                        containerColor = Color(0xFFE11D48),
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.NotificationsActive, null, modifier = Modifier.size(32.dp))
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = (-4).dp)
                                    .size(24.dp)
                                    .background(Color.White, CircleShape)
                                    .border(2.dp, Color(0xFFE11D48), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${expiringTomorrow.size}", color = Color(0xFFE11D48), fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
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
                        onSelectCustomer = { _ -> 
                            activePage = "customer_profile"
                        },
                        openSearch = { showGlobalSearch = true },
                        openSummary = { showSummarySearch = true }
                    )
                    "customers" -> CustomerManagementScreen(
                        viewModel = viewModel, 
                        onSelectCustomer = { _ -> 
                            activePage = "customer_profile"
                        },
                        onNavigateToPayment = {
                            activePage = "payments"
                        },
                        onBack = { activePage = "dashboard" }
                    )
                    "payments" -> PaymentCollectionScreen(viewModel = viewModel, onBack = { activePage = "dashboard" })
                    "billing" -> BillingScreen(viewModel = viewModel, onBack = { activePage = "dashboard" })
                    "reports" -> ReportsScreen(mainViewModel = viewModel, onBack = { activePage = "dashboard" })
                    "salary_ledger" -> StaffSalaryHistoryScreen(viewModel = viewModel, onBack = { activePage = "dashboard" })
                    "staff" -> StaffScreen(viewModel = viewModel, onNavigateToLedger = { activePage = "salary_ledger" }, onBack = { activePage = "dashboard" })
                    "inventory" -> InventoryScreen(viewModel = viewModel, onBack = { activePage = "dashboard" })
                    "packages" -> PackageScreen(viewModel = viewModel, onBack = { activePage = "dashboard" })
                    "expenses" -> ExpenseScreen(viewModel = viewModel, onBack = { activePage = "dashboard" })
                    "crm_tickets" -> SupportTicketsScreen(viewModel = viewModel, onBack = { activePage = "dashboard" })
                    "alerts" -> AlertsScreen(viewModel = viewModel, onBack = { activePage = "dashboard" })
                    "infrastructure" -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Network Assets (Coming Soon)") }
                    "sms_setup" -> SmsTemplateManagementScreen(viewModel = viewModel, onBack = { activePage = "dashboard" })
                    "sms_logs" -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("SMS History (Coming Soon)") }
                    "settings" -> SettingsScreen(viewModel = viewModel, onBack = { activePage = "dashboard" }, onLogout = { viewModel.logout() })
                    "billing_summary" -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Billing Summary (Coming Soon)") }
                    "customer_profile" -> selectedProfileId?.let { id ->
                        CustomerFullProfileScreen(viewModel = viewModel, customerId = id, onBack = { activePage = "customers" })
                    }
                    else -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToCustomers = { activePage = "customers" },
                        onNavigateToPayments = { activePage = "payments" },
                        onNavigateToBilling = { activePage = "billing" },
                        onNavigateToMikroTik = { activePage = "mikrotik" },
                        onNavigateToReports = { activePage = "reports" },
                        openSearch = { showGlobalSearch = true },
                        openSummary = { showSummarySearch = true }
                    )
                }
            }
        }
    }

    // Global Search Modal
    if (showGlobalSearch) {
        GlobalSearchDialog(
            customers = customersList,
            onSelect = { customer ->
                showGlobalSearch = false
                selectedProfileId = customer.id
                activePage = "customer_profile"
            },
            onDismiss = { showGlobalSearch = false }
        )
    }

    // Summary Search Modal
    if (showSummarySearch) {
        SummarySearchDialog(
            customers = customersList,
            onSelect = { customer ->
                showSummarySearch = false
                activePage = "billing_summary"
            },
            onDismiss = { showSummarySearch = false }
        )
    }

    // Expiry Alert Modal
    if (showExpiryModal && permissions.canSeeExpiryAlerts) {
        ExpiryAlertsDialog(
            expiringList = expiringTomorrow,
            onDismiss = { showExpiryModal = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchDialog(
    customers: List<CustomerEntity>,
    onSelect: (CustomerEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, customers) {
        if (query.isBlank()) emptyList()
        else customers.filter { 
            it.name.contains(query, ignoreCase = true) || 
            it.customerCode.contains(query, ignoreCase = true) ||
            it.mobile.contains(query)
        }.take(8)
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            shape = RoundedCornerShape(64.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            border = BorderStroke(4.dp, Color(0xFF4F46E5).copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(40.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("SUBSCRIBER SEARCH", fontWeight = FontWeight.Black, fontSize = 24.sp, letterSpacing = (-1).sp)
                    IconButton(onClick = onDismiss, modifier = Modifier.background(Color.Red.copy(alpha = 0.1f), CircleShape)) { Icon(Icons.Default.Close, null, tint = Color.Red) }
                }
                Spacer(Modifier.height(24.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("TYPE NAME, ID OR PHONE...", fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF4F46E5), modifier = Modifier.size(28.dp)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Slate100,
                        unfocusedContainerColor = Slate100,
                        focusedBorderColor = Color(0xFF4F46E5),
                        unfocusedBorderColor = Color.Transparent
                    )
                )
                Spacer(Modifier.height(24.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(filtered) { customer ->
                        Card(
                            onClick = { onSelect(customer) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(32.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(2.dp, Slate100)
                        ) {
                            Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(customer.name.uppercase(), fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = (-0.5).sp)
                                    Text("ID: ${customer.customerCode} • ${customer.mobile}", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Gray, letterSpacing = 2.sp)
                                }
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color(0xFF4F46E5), modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummarySearchDialog(
    customers: List<CustomerEntity>,
    onSelect: (CustomerEntity) -> Unit,
    onDismiss: () -> Unit
) {
    // Similar to GlobalSearch but for Billing Summary
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, customers) {
        if (query.isBlank()) emptyList()
        else customers.filter { 
            it.name.contains(query, ignoreCase = true) || 
            it.customerCode.contains(query, ignoreCase = true) ||
            it.mobile.contains(query)
        }.take(8)
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            shape = RoundedCornerShape(64.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            border = BorderStroke(4.dp, EmeraldSuccess.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(40.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("BILLING SUMMARY SEARCH", fontWeight = FontWeight.Black, fontSize = 24.sp, letterSpacing = (-1).sp)
                    IconButton(onClick = onDismiss, modifier = Modifier.background(Color.Red.copy(alpha = 0.1f), CircleShape)) { Icon(Icons.Default.Close, null, tint = Color.Red) }
                }
                Spacer(Modifier.height(24.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("SEARCH CUSTOMER FOR SUMMARY...", fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    leadingIcon = { Icon(Icons.Default.Assessment, null, tint = EmeraldSuccess, modifier = Modifier.size(28.dp)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Slate100,
                        unfocusedContainerColor = Slate100,
                        focusedBorderColor = EmeraldSuccess,
                        unfocusedBorderColor = Color.Transparent
                    )
                )
                Spacer(Modifier.height(24.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(filtered) { customer ->
                        Card(
                            onClick = { onSelect(customer) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(32.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(2.dp, Slate100)
                        ) {
                            Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(customer.name.uppercase(), fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = (-0.5).sp)
                                    Text("ID: ${customer.customerCode}", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Gray, letterSpacing = 2.sp)
                                }
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = EmeraldSuccess, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpiryAlertsDialog(
    expiringList: List<CustomerEntity>,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            shape = RoundedCornerShape(64.dp),
            color = Color.White,
            tonalElevation = 8.dp,
            border = BorderStroke(4.dp, Color(0xFFE11D48).copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(40.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("EXPIRY ALERTS", fontWeight = FontWeight.Black, fontSize = 32.sp, color = Color(0xFFE11D48), letterSpacing = (-1).sp)
                        Text("TOTAL ${expiringList.size} CUSTOMERS EXPIRING", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Gray, letterSpacing = 3.sp)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp).background(Color(0xFFE11D48).copy(alpha = 0.1f), CircleShape)) { Icon(Icons.Default.Close, null, tint = Color(0xFFE11D48), modifier = Modifier.size(28.dp)) }
                }
                Spacer(Modifier.height(32.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 450.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(expiringList) { customer ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(32.dp),
                            colors = CardDefaults.cardColors(containerColor = Slate50),
                            border = BorderStroke(2.dp, Slate100)
                        ) {
                            Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("#${customer.customerCode}", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Gray, letterSpacing = 3.sp)
                                    Text(customer.name.uppercase(), fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF0F172A), letterSpacing = (-0.5).sp)
                                    Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Surface(color = Color(0xFF4F46E5).copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp)) {
                                            Text(customer.mobile, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF4F46E5), letterSpacing = 1.sp)
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Surface(color = Color(0xFFE11D48).copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp)) {
                                            Text("DUE: ৳${customer.currentDue.toInt()}", modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFFE11D48), letterSpacing = 1.sp)
                                        }
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("EXPIRE", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Gray, letterSpacing = 2.sp)
                                    Text(customer.expireDate.orEmpty(), fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFFE11D48))
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    shape = RoundedCornerShape(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                ) {
                    Text("CLOSE PANEL", fontWeight = FontWeight.Black, letterSpacing = 5.sp, fontSize = 16.sp)
                }
            }
        }
    }
}


data class MenuItem(val id: String, val label: String, val icon: ImageVector)
