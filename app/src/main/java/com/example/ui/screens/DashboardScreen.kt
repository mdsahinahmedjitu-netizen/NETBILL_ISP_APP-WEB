package com.example.ui.screens

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.CustomerEntity
import com.example.data.entity.SupportTicketEntity
import com.example.localization.appTranslation
import com.example.ui.components.TodaysCollectionCard
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

// Complain Title Model for Setup
data class ComplainTitleItem(
    val id: Long,
    val title: String,
    val showInPortal: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToCustomers: () -> Unit,
    onNavigateToPayments: () -> Unit,
    onNavigateToReports: () -> Unit = {},
    onSelectCustomer: (CustomerEntity) -> Unit = {},
    openSearch: () -> Unit = {},
    openSummary: () -> Unit = {}
) {
    val permissions by viewModel.currentPermissions.collectAsState()
    val stats by viewModel.dashboardStats.collectAsState()
    val payments by viewModel.paymentsList.collectAsState()
    val paymentRequests by viewModel.paymentRequestsList.collectAsState()
    val customersList by viewModel.customersList.collectAsState()
    val supportTickets by viewModel.supportTickets.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val currency = appTranslation("currency_symbol")

    @Composable
    fun t(key: String) = appTranslation(key)

    var showCreateCustomerDialog by remember { mutableStateOf(false) }
    var showComplaintsDialog by remember { mutableStateOf(false) }
    var showComplainSetupDialog by remember { mutableStateOf(false) }
    var showNewJoinsDialog by remember { mutableStateOf(false) }
    var showExpiredDialog by remember { mutableStateOf(false) }
    var isPromiseExpanded by remember { mutableStateOf(true) }

    val complainTitlesList = remember {
        mutableStateListOf<ComplainTitleItem>(
            ComplainTitleItem(1, "LOS Red Light (Fiber Cut)", true),
            ComplainTitleItem(2, "Slow Speed / Packet Loss", true),
            ComplainTitleItem(3, "Router Config / Reset Issue", true),
            ComplainTitleItem(4, "ONU Power / Fiber Patch Problem", true),
            ComplainTitleItem(5, "Billing / Payment Disconnect", false)
        )
    }
    var activeFilter by remember { mutableStateOf(com.example.ui.components.CollectionFilterPeriod.TODAY) }
    val customDateString = remember { com.example.ui.components.getTodayDateString() }
    val activeSummary = remember(payments, activeFilter) {
        com.example.ui.components.calculateCollectionSummary(
            payments = payments,
            filter = activeFilter,
            customDateStr = customDateString
        )
    }

    Scaffold(
        containerColor = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // WEB-STYLE WELCOME HEADER
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(t("dashboard_overview").uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Black, color = Teal600, letterSpacing = 4.sp)
                        Text("WELCOME BACK ADMIN", fontSize = 24.sp, fontWeight = FontWeight.Black, color = if (isDarkMode) Color.White else Slate900, letterSpacing = (-1).sp)
                    }
                    IconButton(
                        onClick = { viewModel.logout() },
                        modifier = Modifier.background(CoralWarning.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, null, tint = CoralWarning, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // 0. BILL PROMISE REMINDERS
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val isStaff = (currentUser?.role?.lowercase() ?: "") != "admin"
            val staffId = currentUser?.id ?: ""
            val staffName = currentUser?.name ?: ""

            val billPromises = customersList.filter { 
                !it.promiseDate.isNullOrBlank() && 
                it.currentDue > 0 &&
                (!isStaff || (it.assignedStaffId?.isNotBlank() == true && (it.assignedStaffId == staffId || it.assignedStaffId == staffName)))
            }
            val todaysPromises = billPromises.filter { it.promiseDate == todayStr }
            val overduePromises = billPromises.filter { 
                try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val pDate = sdf.parse(it.promiseDate!!)
                    val tDate = sdf.parse(todayStr)
                    pDate != null && pDate.before(tDate)
                } catch(_: Exception) { false }
            }

            if (todaysPromises.isNotEmpty() || overduePromises.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Surface(
                            onClick = { isPromiseExpanded = !isPromiseExpanded },
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xFFEEF2FF),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.EventAvailable, null, tint = IspIndigo, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(text = t("bill_promise_reminders").uppercase(), fontSize = 14.sp, fontWeight = FontWeight.Black, color = IspIndigo, letterSpacing = 2.sp)
                                }
                                Icon(if (isPromiseExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = IspIndigo)
                            }
                        }

                        if (isPromiseExpanded) {
                            // Todays
                            todaysPromises.forEach { cust ->
                                PromiseReminderCard(
                                    customer = cust,
                                    isOverdue = false,
                                    isDarkMode = isDarkMode,
                                    currency = currency,
                                    onCall = { /* handle call */ },
                                    onPay = { onNavigateToPayments(); viewModel.setPreSelectedCustomerForPayment(cust) }
                                )
                            }

                            // Overdue
                            overduePromises.forEach { cust ->
                                PromiseReminderCard(
                                    customer = cust,
                                    isOverdue = true,
                                    isDarkMode = isDarkMode,
                                    currency = currency,
                                    onCall = { /* handle call */ },
                                    onPay = { onNavigateToPayments(); viewModel.setPreSelectedCustomerForPayment(cust) }
                                )
                            }
                        }
                    }
                }
            }

            // 1. TOP NOTIFICATION BAR - CUSTOMER COMPLAINTS / TICKETS
            val unresolvedTickets = supportTickets.filter { it.status == "Open" || it.status == "Pending" }
            if (unresolvedTickets.isNotEmpty() && permissions.canSeeComplaintsAlert) {
                item {
                    Card(
                        onClick = { showComplaintsDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(32.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF59E0B)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                            Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(Color(0xFFB45309)).align(Alignment.BottomCenter))

                            Row(
                                modifier = Modifier.padding(24.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.SupportAgent, null, tint = Color(0xFF0F172A), modifier = Modifier.size(28.dp))
                                    Box(
                                        modifier = Modifier.align(Alignment.TopEnd).size(22.dp).background(Color(0xFF0F172A), CircleShape).border(2.dp, Color(0xFFFBBF24), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = unresolvedTickets.size.toString(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                                Spacer(modifier = Modifier.width(20.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = t("tickets_attention").uppercase(), fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF0F172A), letterSpacing = (-1).sp)
                                    Text(text = "${unresolvedTickets.size} ${t("tickets_pending_msg")}".uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A).copy(alpha = 0.7f), letterSpacing = 1.sp)
                                }
                                Box(modifier = Modifier.size(44.dp).background(Color(0xFF0F172A), CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }

            // 2. PENDING VERIFICATION ALERTS
            val pendingRequests = paymentRequests.filter { it.status == "pending" }
            if (pendingRequests.isNotEmpty() && permissions.canSeeVerificationAlert) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(44.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F2)),
                        border = BorderStroke(4.dp, Color(0xFFF43F5E).copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(32.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFF43F5E), modifier = Modifier.size(56.dp), shadowElevation = 8.dp) {
                                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.NotificationsActive, null, tint = Color.White, modifier = Modifier.size(28.dp)) }
                                }
                                Spacer(modifier = Modifier.width(20.dp))
                                Text(text = "${t("needs_verification")}: ${pendingRequests.size}".uppercase(), fontWeight = FontWeight.Black, fontSize = 22.sp, color = Color(0xFFE11D48), letterSpacing = (-1).sp)
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                pendingRequests.forEach { req ->
                                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(32.dp), border = BorderStroke(1.dp, Color(0xFFF1F5F9))) {
                                        Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFFEEF2FF)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                                        Text(req.collectedBy.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF4F46E5), letterSpacing = 1.sp)
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text("TRX: ${req.trxId}", fontSize = 11.sp, color = Slate400, fontWeight = FontWeight.Bold)
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(req.customerName.uppercase(), fontWeight = FontWeight.Black, fontSize = 18.sp, color = Slate900, letterSpacing = (-0.5).sp)
                                                Text("৳ ${String.format(Locale.US, "%,.0f", req.amount)}", fontWeight = FontWeight.Black, color = EmeraldSuccess, fontSize = 24.sp)
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                IconButton(onClick = { viewModel.approvePaymentRequest(req) }, modifier = Modifier.background(EmeraldSuccess, RoundedCornerShape(16.dp)).size(56.dp)) { Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(28.dp)) }
                                                IconButton(onClick = { viewModel.rejectPaymentRequest(req) }, modifier = Modifier.background(Color(0xFFFFF1F2), RoundedCornerShape(16.dp)).size(56.dp)) { Icon(Icons.Default.Close, null, tint = Color(0xFFF43F5E), modifier = Modifier.size(28.dp)) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. 12-Card Feature Grid
            if (permissions.canSeeStatsCards) {
                item {
                    ISPFeatureGridSection(
                        isAdmin = currentUser?.role?.lowercase() == "admin",
                        stats = stats,
                        onCollectionClick = { onNavigateToPayments() },
                        onCollectionReportClick = { onNavigateToReports() },
                        onSubscribersClick = { onNavigateToCustomers() },
                        onTicketsClick = { showComplaintsDialog = true },
                        onAddClick = { showCreateCustomerDialog = true },
                        onSearchClick = openSearch,
                        onDueListClick = onNavigateToReports, 
                        onSummaryClick = openSummary,
                        onEditClick = { openSearch() },
                        onExpiredClick = { showExpiredDialog = true },
                        onNewJoinsClick = { showNewJoinsDialog = true }
                    )
                }
            }

            // 4. Collection Breakdown Card
            if (permissions.canSeeTodayCollection) {
                item {
                    TodaysCollectionCard(
                        payments = payments,
                        selectedFilter = activeFilter,
                        onFilterSelected = { activeFilter = it }
                    )
                }
            }

            // 5. Hero Stats Card (Teal)
            if (permissions.canSeeTotalCollection) {
                item {
                    val displayCollection = activeSummary.grandTotal
                val dueTotal = stats.totalDue
                val targetPlan = 250000.0

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(64.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D9488)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Brush.verticalGradient(listOf(Color(0xFF0D9488), Color(0xFF0F172A))))
                                .padding(40.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = t("financial_total").uppercase(),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White.copy(alpha = 0.9f),
                                            letterSpacing = 5.sp
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "$currency ${String.format(Locale.US, "%,.0f", displayCollection)}",
                                            fontSize = 72.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            letterSpacing = (-2).sp,
                                            lineHeight = 72.sp
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(RoundedCornerShape(32.dp))
                                            .background(Color.White.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = Color.White, modifier = Modifier.size(40.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(48.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(64.dp)
                                ) {
                                    Column {
                                        Text(text = t("target_plan").uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.7f), letterSpacing = 3.sp)
                                        Text(text = "$currency ${String.format(Locale.US, "%,.0f", targetPlan)}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                                    }
                                    Box(modifier = Modifier.width(1.dp).height(60.dp).background(Color.White.copy(alpha = 0.2f)))
                                    Column {
                                        Text(text = t("total_outstanding").uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFFFDE047), letterSpacing = 3.sp)
                                        Text(text = "$currency ${String.format(Locale.US, "%,.0f", dueTotal)}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFFFDE047))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // Dialogs
    if (showCreateCustomerDialog) {
        CreateCustomerDashboardDialog(viewModel = viewModel, onDismiss = { showCreateCustomerDialog = false })
    }

    if (showComplaintsDialog) {
        ComplaintsDashboardDialog(
            complaints = supportTickets,
            onUpdateStatus = { ticket, newStatus -> viewModel.updateSupportTicket(ticket.copy(status = newStatus)) },
            onDismiss = { showComplaintsDialog = false }
        )
    }

    if (showComplainSetupDialog) {
        ComplainSetupDialog(titlesList = complainTitlesList, onDismiss = { showComplainSetupDialog = false })
    }

    if (showNewJoinsDialog) {
        NewCustomersDashboardDialog(
            customersList = customersList.filter { (it.joinDate ?: "").startsWith(customDateString.substring(0, 7)) },
            onSelectCustomer = { onSelectCustomer(it); showNewJoinsDialog = false },
            onDismiss = { showNewJoinsDialog = false }
        )
    }

    if (showExpiredDialog) {
        SearchCustomerDashboardDialog(
            customersList = customersList.filter { it.status == "Expired" || it.status == "Suspended" },
            onSelectCustomer = { onSelectCustomer(it); showExpiredDialog = false },
            onDismiss = { showExpiredDialog = false }
        )
    }
}

@Composable
fun ISPFeatureGridSection(
    isAdmin: Boolean,
    stats: com.example.viewmodel.DashboardStats,
    onCollectionClick: () -> Unit,
    onCollectionReportClick: () -> Unit,
    onSubscribersClick: () -> Unit,
    onTicketsClick: () -> Unit,
    onAddClick: () -> Unit,
    onSearchClick: () -> Unit,
    onDueListClick: () -> Unit,
    onSummaryClick: () -> Unit,
    onEditClick: () -> Unit,
    onExpiredClick: () -> Unit,
    onNewJoinsClick: () -> Unit
) {
    @Composable
    fun t(key: String) = appTranslation(key)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            ISPFeatureCard(label = t("grid_collection"), modifier = Modifier.weight(1f), gradientBrush = Brush.linearGradient(listOf(Color(0xFF4F46E5), Color(0xFF2563EB))), onClick = onCollectionClick, iconContent = { Icon(Icons.Default.AccountBalanceWallet, null, tint = Color.White, modifier = Modifier.size(32.dp)) })
            ISPFeatureCard(label = t("grid_collection_report"), modifier = Modifier.weight(1f), gradientBrush = Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF059669))), onClick = onCollectionReportClick, iconContent = { Icon(Icons.Default.Assessment, null, tint = Color.White, modifier = Modifier.size(32.dp)) })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            ISPFeatureCard(label = t("grid_crm"), modifier = Modifier.weight(1f), gradientBrush = Brush.linearGradient(listOf(Color(0xFFEC4899), Color(0xFF8B5CF6))), onClick = onSubscribersClick, iconContent = { Icon(Icons.Default.People, null, tint = Color.White, modifier = Modifier.size(32.dp)) })
            ISPFeatureCard(label = t("grid_complaint_list"), modifier = Modifier.weight(1f), gradientBrush = Brush.linearGradient(listOf(Color(0xFFFBBF24), Color(0xFFF59E0B))), onClick = onTicketsClick, iconContent = { Icon(Icons.Default.ConfirmationNumber, null, tint = Color.White, modifier = Modifier.size(32.dp)) } )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            if (isAdmin) ISPFeatureCard(label = t("grid_create_new"), modifier = Modifier.weight(1f), gradientBrush = Brush.linearGradient(listOf(Color(0xFF06B6D4), Color(0xFF3B82F6))), onClick = onAddClick, iconContent = { Icon(Icons.Default.PersonAdd, null, tint = Color.White, modifier = Modifier.size(32.dp)) })
            ISPFeatureCard(label = t("grid_search"), modifier = Modifier.weight(if(isAdmin) 1f else 2f), gradientBrush = Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))), onClick = onSearchClick, iconContent = { Icon(Icons.Default.Search, null, tint = Color.White, modifier = Modifier.size(32.dp)) })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            ISPFeatureCard(label = t("grid_due_list"), modifier = Modifier.weight(1f), gradientBrush = Brush.linearGradient(listOf(Color(0xFFF97316), Color(0xFFEA580C))), onClick = onDueListClick, iconContent = { Icon(Icons.Default.RequestQuote, null, tint = Color.White, modifier = Modifier.size(32.dp)) })
            ISPFeatureCard(label = t("grid_bill_summary"), modifier = Modifier.weight(1f), gradientBrush = Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B))), onClick = onSummaryClick, iconContent = { Icon(Icons.Default.PieChart, null, tint = Color.White, modifier = Modifier.size(32.dp)) })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            if (isAdmin) ISPFeatureCard(label = t("grid_edit_customer"), modifier = Modifier.weight(1f), gradientBrush = Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706))), onClick = onEditClick, iconContent = { Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(32.dp)) })
            ISPFeatureCard(label = "${t("grid_expired_customer")} (${stats.expiredCustomers})", modifier = Modifier.weight(if(isAdmin) 1f else 2f), gradientBrush = Brush.linearGradient(listOf(Color(0xFFF43F5E), Color(0xFFBE123C))), onClick = onExpiredClick, iconContent = { Icon(Icons.Default.PersonOff, null, tint = Color.White, modifier = Modifier.size(32.dp)) })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            ISPFeatureCard(label = "${t("grid_new_customers")} (${stats.newCustomers})", modifier = Modifier.weight(1f), gradientBrush = Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF047857))), onClick = onNewJoinsClick, iconContent = { Icon(Icons.Default.HowToReg, null, tint = Color.White, modifier = Modifier.size(32.dp)) })
            Card(modifier = Modifier.weight(1f).height(120.dp), shape = RoundedCornerShape(32.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))) {
                Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                    Icon(Icons.Default.Sms, null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(40.dp).align(Alignment.TopEnd))
                    Column(modifier = Modifier.align(Alignment.BottomStart)) {
                        Text("SMS BALANCE", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.6f), letterSpacing = 2.sp)
                        Text(stats.smsBalance, fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color(0xFF2DD4BF), letterSpacing = (-1).sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ISPFeatureCard(label: String, modifier: Modifier = Modifier, backgroundColor: Color = Color.White, textColor: Color = Color.White, gradientBrush: Brush? = null, onClick: () -> Unit, iconContent: @Composable () -> Unit) {
    Card(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(44.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(120.dp).then(if (gradientBrush != null) Modifier.background(gradientBrush) else Modifier.background(backgroundColor)).padding(20.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) { iconContent() }
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = label.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Black, color = textColor, textAlign = TextAlign.Center, maxLines = 1, letterSpacing = 2.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewCustomersDashboardDialog(customersList: List<CustomerEntity>, onSelectCustomer: (CustomerEntity) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, customersList) { if (query.isBlank()) customersList else customersList.filter { it.name.contains(query, ignoreCase = true) || it.customerCode.contains(query, ignoreCase = true) || it.mobile.contains(query, ignoreCase = true) } }
    AlertDialog(onDismissRequest = onDismiss, title = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.PersonAdd, null, tint = ElectricBlue); Spacer(modifier = Modifier.width(8.dp)); Text("নতুন সংযোগ তালিকা", fontWeight = FontWeight.Black, fontSize = 17.sp) } }, text = { Column(modifier = Modifier.fillMaxWidth().height(400.dp)) { OutlinedTextField(value = query, onValueChange = { query = it }, placeholder = { Text("Search...") }, modifier = Modifier.fillMaxWidth(), singleLine = true); Spacer(modifier = Modifier.height(10.dp)); if (filtered.isEmpty()) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No customers found") } } else { LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) { items(filtered) { customer -> Card(onClick = { onSelectCustomer(customer) }, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) { Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column(modifier = Modifier.weight(1f)) { Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Slate900); Text("${customer.customerCode} • ${customer.mobile}", fontSize = 11.sp, color = Slate600) }; Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = ElectricBlue) } } } } } } }, confirmButton = { TextButton(onClick = onDismiss) { Text("Close", fontWeight = FontWeight.Black) } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchCustomerDashboardDialog(customersList: List<CustomerEntity>, onSelectCustomer: (CustomerEntity) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, customersList) { if (query.isBlank()) customersList else customersList.filter { it.name.contains(query, ignoreCase = true) || it.customerCode.contains(query, ignoreCase = true) || it.mobile.contains(query, ignoreCase = true) } }
    AlertDialog(onDismissRequest = onDismiss, title = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Search, null, tint = Teal600); Spacer(modifier = Modifier.width(8.dp)); Text("গ্রাহক খুঁজুন", fontWeight = FontWeight.Black, fontSize = 18.sp) } }, text = { Column(modifier = Modifier.fillMaxWidth().height(380.dp)) { OutlinedTextField(value = query, onValueChange = { query = it }, placeholder = { Text("নাম বা আইডি...") }, modifier = Modifier.fillMaxWidth(), singleLine = true); Spacer(modifier = Modifier.height(12.dp)); if (filtered.isEmpty()) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No matching customers found") } } else { LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) { items(filtered) { customer -> Card(onClick = { onSelectCustomer(customer) }, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) { Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column(modifier = Modifier.weight(1f)) { Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 14.sp); Text("${customer.customerCode} • ${customer.zone}", fontSize = 11.sp) }; Icon(Icons.Default.Phone, null, tint = ElectricBlue) } } } } } } }, confirmButton = { TextButton(onClick = onDismiss) { Text("বন্ধ করুন", color = Teal700, fontWeight = FontWeight.Black) } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCustomerDashboardDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    AddEditCustomerDialog(customer = null, onDismiss = onDismiss, onSave = { newCustomer, disc, choice -> viewModel.addOrUpdateCustomer(newCustomer, disc, choice); onDismiss() })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplaintsDashboardDialog(complaints: List<SupportTicketEntity>, onUpdateStatus: (SupportTicketEntity, String) -> Unit, onDismiss: () -> Unit) {
    var selectedFilter by remember { mutableStateOf("All") }
    val filteredList = remember(selectedFilter, complaints) { if (selectedFilter == "All") complaints else complaints.filter { it.status == selectedFilter } }
    AlertDialog(onDismissRequest = onDismiss, title = { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.ReportProblem, null, tint = CoralWarning); Spacer(modifier = Modifier.width(8.dp)); Text("অভিযোগ তালিকা", fontWeight = FontWeight.Black, fontSize = 18.sp) }; IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) } } }, text = { Column(modifier = Modifier.fillMaxWidth().height(480.dp)) { Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) { listOf("All", "Pending", "In Progress", "Resolved").forEach { status -> FilterChip(selected = selectedFilter == status, onClick = { selectedFilter = status }, label = { Text(status) }) } }; Spacer(modifier = Modifier.height(10.dp)); LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) { items(filteredList) { ticket -> Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = if(ticket.status=="Resolved") EmeraldSuccess.copy(alpha=0.1f) else CoralWarning.copy(alpha=0.1f)), modifier = Modifier.fillMaxWidth()) { Column(modifier = Modifier.padding(12.dp)) { Text(ticket.customerName, fontWeight = FontWeight.Black); Text(ticket.issueType, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text(ticket.description, fontSize = 11.sp); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { if(ticket.status != "Resolved") Button(onClick = { onUpdateStatus(ticket, "Resolved") }, colors=ButtonDefaults.buttonColors(containerColor=EmeraldSuccess)) { Text("Resolve", fontSize=10.sp) } } } } } } } }, confirmButton = {})
}

@Composable
fun PromiseReminderCard(
    customer: CustomerEntity,
    isOverdue: Boolean,
    isDarkMode: Boolean,
    currency: String,
    onCall: () -> Unit,
    onPay: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = if (isOverdue) Color(0xFFFFF1F2) else if (isDarkMode) Color(0xFF1E293B) else Color.White),
        border = BorderStroke(1.dp, if (isOverdue) Color(0xFFF43F5E).copy(alpha = 0.3f) else if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFF1F5F9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(if (isOverdue) Color(0xFFF43F5E) else IspIndigo),
                contentAlignment = Alignment.Center
            ) {
                Icon(if (isOverdue) Icons.Default.Warning else Icons.Default.Event, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = customer.name.uppercase(), fontWeight = FontWeight.Black, fontSize = 16.sp, color = if (isDarkMode) Color.White else Slate900)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ZONE: ${customer.zone}", 
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.Black, 
                        color = Slate600
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DUE: $currency${customer.currentDue.toInt()}", 
                        fontSize = 13.sp, 
                        fontWeight = FontWeight.ExtraBold, 
                        color = if (isOverdue) Color(0xFFE11D48) else IspTealPrimary
                    )
                }
                if (!customer.promiseNote.isNullOrBlank()) {
                    Text(text = "\"${customer.promiseNote}\"", fontSize = 10.sp, color = Slate400, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onCall, modifier = Modifier.background(Color(0xFFEEF2FF), CircleShape).size(40.dp)) {
                    Icon(Icons.Default.Phone, null, tint = IspIndigo, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onPay, modifier = Modifier.background(IspTealPrimary.copy(alpha = 0.1f), CircleShape).size(40.dp)) {
                    Icon(Icons.Default.Payments, null, tint = IspTealPrimary, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplainSetupDialog(titlesList: SnapshotStateList<ComplainTitleItem>, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Complain Setup", fontWeight = FontWeight.Black) }, text = { Column { titlesList.forEach { Text(it.title, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp)) } } }, confirmButton = { TextButton(onClick = onDismiss) { Text("বন্ধ করুন", fontWeight = FontWeight.Black) } })
}
