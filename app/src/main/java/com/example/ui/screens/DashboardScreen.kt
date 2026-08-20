package com.example.ui.screens

import java.util.Locale
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.CustomerEntity
import com.example.data.entity.SupportTicketEntity
import com.example.data.entity.UserRolePermissions
import com.example.localization.AppTranslation
import com.example.ui.components.ReadonlyDateField
import com.example.ui.components.BillSummaryGridIcon
import com.example.ui.components.CollectionGridIcon
import com.example.ui.components.CollectionReportGridIcon
import com.example.ui.components.ComplinListGridIcon
import com.example.ui.components.CreateNewGridIcon
import com.example.ui.components.DueListGridIcon
import com.example.ui.components.ListReportGridIcon
import com.example.ui.components.SearchGridIcon
import com.example.ui.components.TodaysCollectionCard
import com.example.ui.theme.BkashPink
import com.example.ui.theme.CoralWarning
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ElectricBlue
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.OutlinedButton
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
    onNavigateToBilling: () -> Unit,
    onNavigateToMikroTik: () -> Unit,
    onNavigateToReports: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToAlerts: () -> Unit = {},
    onSelectCustomer: (CustomerEntity) -> Unit = {}
) {
    val permissions by viewModel.currentPermissions.collectAsState()
    val stats by viewModel.dashboardStats.collectAsState()
    val payments by viewModel.paymentsList.collectAsState()
    val paymentRequests by viewModel.paymentRequestsList.collectAsState()
    val customersList by viewModel.customersList.collectAsState()
    val supportTickets by viewModel.supportTickets.collectAsState()
    val currentLang by viewModel.currentLanguage.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    
    val currency = AppTranslation("currency_symbol")
    
    @Composable
    fun t(key: String) = AppTranslation(key)

    // State for interactive Dialogs triggered by Grid Actions
    var showCreateCustomerDialog by remember { mutableStateOf(false) }
    var showSearchCustomerDialog by remember { mutableStateOf(false) }
    var showComplaintsDialog by remember { mutableStateOf(false) }
    var showComplainSetupDialog by remember { mutableStateOf(false) }
    var showNewCustomersDialog by remember { mutableStateOf(false) }

    val complainTitlesList = remember {
        mutableStateListOf(
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFC))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. TOP NOTIFICATION BAR - CUSTOMER COMPLAINTS / TICKETS
        val unresolvedTickets = supportTickets.filter { it.status == "Open" || it.status == "Pending" }
        if (unresolvedTickets.isNotEmpty() && permissions.canSeeComplaintsAlert) {
            item {
                Card(
                    onClick = { showComplaintsDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFBBF24)),
                    border = BorderStroke(1.dp, Color(0xFFB45309)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Bottom Border Simulation (border-b-4 border-amber-700)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(Color(0xFFB45309))
                                .align(Alignment.BottomCenter)
                        )
                        
                        Row(
                            modifier = Modifier.padding(24.dp).padding(bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.Black.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.SupportAgent, null, tint = Color(0xFF0F172A), modifier = Modifier.size(28.dp))
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(22.dp)
                                        .background(Color(0xFF0F172A), CircleShape)
                                        .border(2.dp, Color(0xFFFBBF24), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "${unresolvedTickets.size}", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                            }
                            Spacer(modifier = Modifier.width(20.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = t("tickets_attention").uppercase(),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = Color(0xFF0F172A),
                                    letterSpacing = (-1).sp
                                )
                                Text(
                                    text = "${unresolvedTickets.size} ${t("tickets_pending_msg")}".uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A).copy(alpha = 0.7f),
                                    letterSpacing = 1.sp
                                )
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
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.6f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Reverse),
                    label = "alpha"
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(44.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F2).copy(alpha = alpha)),
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
                                            Text("৳ ${String.format(java.util.Locale.US, "%,.0f", req.amount)}", fontWeight = FontWeight.Black, color = EmeraldSuccess, fontSize = 24.sp)
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

        // 3. Stats Grid (FeatureCard Grid)
        if (permissions.canSeeStatsCards) {
            item {
                ISPFeatureGridSection(
                    onCollectionClick = { onNavigateToPayments() },
                    onCollectionReportClick = { onNavigateToReports() },
                    onListReportClick = { onNavigateToCustomers() },
                    onDueListClick = { onNavigateToCustomers() },
                    onCreateNewClick = { showCreateCustomerDialog = true },
                    onSearchClick = { showSearchCustomerDialog = true },
                    onComplinListClick = { showComplaintsDialog = true },
                    onBillSummaryClick = { onNavigateToBilling() }
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

        // 5. Main Stats Summary (Hero Summary)
        if (permissions.canSeeTotalCollection) {
            item {
                val displayCollection = activeSummary?.grandTotal ?: stats.todaysCollection
                val periodTitle = when (activeFilter) {
                    com.example.ui.components.CollectionFilterPeriod.TODAY -> t("today")
                    com.example.ui.components.CollectionFilterPeriod.YESTERDAY -> t("yesterday")
                    com.example.ui.components.CollectionFilterPeriod.LAST_7_DAYS -> t("last_7_days")
                    com.example.ui.components.CollectionFilterPeriod.THIS_MONTH -> t("this_month")
                    com.example.ui.components.CollectionFilterPeriod.CUSTOM -> t("custom_date")
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(44.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D9488)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Color(0xFF0D9488), Color(0xFF0F766E))))
                            .padding(32.dp)
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
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White.copy(alpha = 0.8f),
                                        letterSpacing = 4.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "$currency ${String.format(java.util.Locale.US, "%,.0f", displayCollection)}",
                                        fontSize = 56.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        letterSpacing = (-2).sp,
                                        lineHeight = 56.sp
                                    )
                                    Text(
                                        text = periodTitle.uppercase(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White.copy(alpha = 0.5f),
                                        letterSpacing = 2.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color.White.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.TrendingUp, null, tint = Color.White, modifier = Modifier.size(32.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(40.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(40.dp)
                            ) {
                                Column {
                                    Text(text = t("target_plan").uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.6f), letterSpacing = 2.sp)
                                    Text(text = "$currency 25,000", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                                }
                                Box(modifier = Modifier.width(1.dp).height(48.dp).background(Color.White.copy(alpha = 0.2f)))
                                Column {
                                    Text(text = t("total_outstanding").uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFFFDE047), letterSpacing = 2.sp)
                                    Text(text = "$currency ${String.format(java.util.Locale.US, "%,.0f", stats.totalDue)}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFFFDE047))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Interactive Dialog 1: Create New Customer Dialog
    if (showCreateCustomerDialog) {
        CreateCustomerDashboardDialog(
            viewModel = viewModel,
            permissions = permissions,
            onDismiss = { showCreateCustomerDialog = false }
        )
    }

    // Interactive Dialog 2: Search Customer Dialog
    if (showSearchCustomerDialog) {
        SearchCustomerDashboardDialog(
            customersList = customersList,
            onSelectCustomer = { customer: CustomerEntity ->
                showSearchCustomerDialog = false
                onSelectCustomer(customer)
            },
            onDismiss = { showSearchCustomerDialog = false }
        )
    }

    // Interactive Dialog: New Customers List Dialog
    if (showNewCustomersDialog) {
        NewCustomersDashboardDialog(
            customersList = customersList,
            onSelectCustomer = { customer: CustomerEntity ->
                showNewCustomersDialog = false
                onSelectCustomer(customer)
            },
            onDismiss = { showNewCustomersDialog = false }
        )
    }

    // Interactive Dialog 3: Complaints / Support Ticket List Dialog
    if (showComplaintsDialog) {
        ComplaintsDashboardDialog(
            complaints = supportTickets,
            allCustomers = customersList,
            titlesList = complainTitlesList,
            onOpenSetup = { showComplainSetupDialog = true },
            onUpdateStatus = { ticket: SupportTicketEntity, newStatus: String ->
                viewModel.updateSupportTicket(ticket.copy(status = newStatus))
            },
            onAddComplaint = { type: String, desc: String, name: String, phone: String, sDate: String, sTime: String, customer: CustomerEntity? ->
                viewModel.createSupportTicket(
                    customer = customer,
                    type = type,
                    description = desc,
                    adminEnteredName = name,
                    adminEnteredPhone = phone,
                    scheduledDate = sDate,
                    scheduledTime = sTime
                )
            },
            onDismiss = { showComplaintsDialog = false }
        )
    }

    // Interactive Dialog 4: Complain Setup Dialog (Create Complain Title)
    if (showComplainSetupDialog) {
        ComplainSetupDialog(
            titlesList = complainTitlesList,
            onDismiss = { showComplainSetupDialog = false },
            onToast = { viewModel.showToast(it) }
        )
    }
}

// PRIMARY 8-CARD FEATURE GRID SECTION
@Composable
fun ISPFeatureGridSection(
    onCollectionClick: () -> Unit,
    onCollectionReportClick: () -> Unit,
    onListReportClick: () -> Unit,
    onDueListClick: () -> Unit,
    onCreateNewClick: () -> Unit,
    onSearchClick: () -> Unit,
    onComplinListClick: () -> Unit,
    onBillSummaryClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Row 1: Collection (grad-collection) & Report (grad-invoices)
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ISPFeatureCard(
                label = AppTranslation("grid_collection"),
                modifier = Modifier.weight(1f),
                gradientBrush = Brush.linearGradient(listOf(Color(0xFF4F46E5), Color(0xFF2563EB))),
                onClick = onCollectionClick,
                iconContent = { CollectionGridIcon(Modifier.size(52.dp)) }
            )
            ISPFeatureCard(
                label = AppTranslation("grid_collection_report"),
                modifier = Modifier.weight(1f),
                gradientBrush = Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF059669))),
                onClick = onCollectionReportClick,
                iconContent = { CollectionReportGridIcon(Modifier.size(52.dp)) }
            )
        }

        // Row 2: CRM (grad-subscribers) & Tickets (grad-tickets)
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ISPFeatureCard(
                label = AppTranslation("grid_list_report"),
                modifier = Modifier.weight(1f),
                gradientBrush = Brush.linearGradient(listOf(Color(0xFFEC4899), Color(0xFF8B5CF6))),
                onClick = onListReportClick,
                iconContent = { ListReportGridIcon(Modifier.size(52.dp)) }
            )
            ISPFeatureCard(
                label = AppTranslation("grid_complaint_list"),
                modifier = Modifier.weight(1f),
                gradientBrush = Brush.linearGradient(listOf(Color(0xFFFBBF24), Color(0xFFF59E0B))),
                onClick = onComplinListClick,
                iconContent = { ComplinListGridIcon(Modifier.size(52.dp)) }
            )
        }

        // Row 3: Create New (grad-create) & Search (grad-search)
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ISPFeatureCard(
                label = AppTranslation("grid_create_new"),
                modifier = Modifier.weight(1f),
                gradientBrush = Brush.linearGradient(listOf(Color(0xFF06B6D4), Color(0xFF3B82F6))),
                onClick = onCreateNewClick,
                iconContent = { CreateNewGridIcon(Modifier.size(52.dp)) }
            )
            ISPFeatureCard(
                label = AppTranslation("grid_search"),
                modifier = Modifier.weight(1f),
                gradientBrush = Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))),
                onClick = onSearchClick,
                iconContent = { SearchGridIcon(Modifier.size(52.dp)) }
            )
        }

        // Row 4: Due List (grad-due) & Summary (grad-summary)
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ISPFeatureCard(
                label = AppTranslation("grid_due_list"),
                modifier = Modifier.weight(1f),
                gradientBrush = Brush.linearGradient(listOf(Color(0xFFF97316), Color(0xFFEA580C))),
                onClick = onDueListClick,
                iconContent = { DueListGridIcon(Modifier.size(52.dp)) }
            )
            ISPFeatureCard(
                label = AppTranslation("grid_bill_summary"),
                modifier = Modifier.weight(1f),
                gradientBrush = Brush.linearGradient(listOf(Color(0xFFF43F5E), Color(0xFF9D174D))),
                onClick = onBillSummaryClick,
                iconContent = { BillSummaryGridIcon(Modifier.size(52.dp)) }
            )
        }
    }
}

// INTERACTIVE DIALOG: NEW CUSTOMERS LIST
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewCustomersDashboardDialog(
    customersList: List<CustomerEntity>,
    onSelectCustomer: (CustomerEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, customersList) {
        if (query.isBlank()) customersList else customersList.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.customerCode.contains(query, ignoreCase = true) ||
            it.mobile.contains(query, ignoreCase = true) ||
            it.zone.orEmpty().contains(query, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = ElectricBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("নতুন কাস্টমারদের তালিকা (New Customers)", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search by name, code or phone...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No customers found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filtered) { customer ->
                            Card(
                                onClick = { onSelectCustomer(customer) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Slate900)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("(${customer.customerCode})", fontSize = 11.sp, color = ElectricBlue, fontWeight = FontWeight.Bold)
                                        }
                                        Text("📱 ${customer.mobile} • 📍 ${customer.zone}", fontSize = 11.sp, color = Slate600)
                                        Text("📦 ${customer.packageName} • ৳${String.format(java.util.Locale.US, "%,.0f", customer.monthlyBill)}/mo", fontSize = 11.sp, color = Teal600, fontWeight = FontWeight.SemiBold)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(ElectricBlue)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("View Details >", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ISPFeatureCard(
    label: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    borderColor: Color = Color.Transparent,
    textColor: Color = Color.White,
    gradientBrush: Brush? = null,
    onClick: () -> Unit,
    iconContent: @Composable () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(44.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .then(
                    if (gradientBrush != null) Modifier.background(gradientBrush)
                    else Modifier.background(backgroundColor)
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    iconContent()
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = label.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = textColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

// INTERACTIVE DIALOG 1: CREATE NEW CUSTOMER
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCustomerDashboardDialog(
    viewModel: MainViewModel,
    permissions: UserRolePermissions,
    onDismiss: () -> Unit
) {
    AddEditCustomerDialog(
        customer = null,
        permissions = permissions,
        onDismiss = onDismiss,
        onSave = { newCustomer, choice ->
            viewModel.addOrUpdateCustomer(newCustomer, choice)
            onDismiss()
        }
    )
}

// INTERACTIVE DIALOG 2: SEARCH CUSTOMERS
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchCustomerDashboardDialog(
    customersList: List<CustomerEntity>,
    onSelectCustomer: (CustomerEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val context = LocalContext.current

    val filtered = remember(query, customersList) {
        if (query.isBlank()) customersList else customersList.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.customerCode.contains(query, ignoreCase = true) ||
            it.mobile.contains(query, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("বন্ধ করুন", color = Teal700)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, contentDescription = null, tint = Teal600)
                Spacer(modifier = Modifier.width(8.dp))
                Text(AppTranslation("grid_search") + " (Search Customer)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search by name, code, or phone...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No matching customers found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filtered) { customer ->
                            Card(
                                onClick = { onSelectCustomer(customer) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("${customer.customerCode} • ${customer.zone}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("Pkg: ${customer.packageName} • Due: ৳${String.format(java.util.Locale.US, "%,.0f", customer.currentDue)}", fontSize = 11.sp, color = if (customer.currentDue > 0) CoralWarning else EmeraldSuccess, fontWeight = FontWeight.Bold)
                                    }
                                    IconButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.mobile}"))
                                            context.startActivity(intent)
                                        }
                                    ) {
                                        Icon(Icons.Default.Phone, contentDescription = "Call", tint = ElectricBlue)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

// INTERACTIVE DIALOG 3: COMPLAINTS / SUPPORT TICKETS LIST
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplaintsDashboardDialog(
    complaints: List<SupportTicketEntity>,
    allCustomers: List<CustomerEntity> = emptyList(),
    titlesList: List<ComplainTitleItem> = emptyList(),
    onOpenSetup: () -> Unit = {},
    onUpdateStatus: (SupportTicketEntity, String) -> Unit,
    onAddComplaint: (String, String, String, String, String, String, CustomerEntity?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("All") }
    var showNewComplaintForm by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var newCustName by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var newIssue by remember { mutableStateOf("LOS Red Light") }
    var newDetails by remember { mutableStateOf("") }
    var newReqDate by remember { mutableStateOf("2026-08-08") }
    var newReqTime by remember { mutableStateOf("10:30 AM") }
    var newSchedDate by remember { mutableStateOf("2026-08-08") }
    var newSchedTime by remember { mutableStateOf("03:00 PM") }

    var editingTicket by remember { mutableStateOf<SupportTicketEntity?>(null) }

    val filteredCustomers = remember(searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else allCustomers.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.customerCode.contains(searchQuery, ignoreCase = true) ||
            it.mobile.contains(searchQuery, ignoreCase = true)
        }.take(3)
    }

    val filteredList = remember(selectedFilter, complaints) {
        if (selectedFilter == "All") complaints else complaints.filter { it.status == selectedFilter }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ReportProblem, contentDescription = null, tint = CoralWarning)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(AppTranslation("grid_complaint_list"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenSetup) {
                        Icon(Icons.Default.Settings, contentDescription = "Complain Setup", tint = Teal600)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp)
            ) {
                AnimatedContent(
                    targetState = showNewComplaintForm,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(300)) + slideInVertically(animationSpec = tween(300)))
                            .togetherWith(fadeOut(animationSpec = tween(200)) + slideOutVertically(animationSpec = tween(200)))
                    },
                    label = "dialogFormToggleAnimation"
                ) { isFormVisible ->
                    if (!isFormVisible) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf("All", "Pending", "In Progress", "Resolved").forEach { status ->
                                    FilterChip(
                                        selected = selectedFilter == status,
                                        onClick = { selectedFilter = status },
                                        label = { Text(status, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Teal600,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = { showNewComplaintForm = true },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add New Support Ticket / Request")
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(
                                    items = filteredList,
                                    key = { it.id }
                                ) { ticket: SupportTicketEntity ->
                                    val animatedBgColor by animateColorAsState(
                                        targetValue = when (ticket.status) {
                                            "Resolved" -> EmeraldSuccess.copy(alpha = 0.15f)
                                            "In Progress" -> ElectricBlue.copy(alpha = 0.15f)
                                            else -> CoralWarning.copy(alpha = 0.15f)
                                        },
                                        animationSpec = tween(durationMillis = 400),
                                        label = "cardBgColorAnimation"
                                    )

                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = animatedBgColor),
                                        border = BorderStroke(
                                            1.dp,
                                            when (ticket.status) {
                                                "Resolved" -> EmeraldSuccess.copy(alpha = 0.5f)
                                                "In Progress" -> ElectricBlue.copy(alpha = 0.5f)
                                                else -> CoralWarning.copy(alpha = 0.5f)
                                            }
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(ticket.customerName + " (${ticket.customerCode})", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                                                AnimatedContent(
                                                    targetState = ticket.status,
                                                    transitionSpec = {
                                                        (fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.8f))
                                                            .togetherWith(fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.8f))
                                                    },
                                                    label = "statusBadgeAnimation"
                                                ) { currentStatus ->
                                                    val (chipBg, chipIcon) = when (currentStatus) {
                                                        "Resolved" -> EmeraldSuccess to Icons.Default.CheckCircle
                                                        "In Progress" -> ElectricBlue to Icons.Default.Pending
                                                        else -> CoralWarning to Icons.Default.ReportProblem
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(chipBg)
                                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(
                                                                imageVector = chipIcon,
                                                                contentDescription = null,
                                                                tint = Color.White,
                                                                modifier = Modifier.size(12.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            androidx.compose.material3.Text(
                                                    text = ticket.status,
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                        }
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Issue: ${ticket.issueType}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                            Text(ticket.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.height(6.dp))

                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(6.dp)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Schedule, contentDescription = null, tint = Teal600, modifier = Modifier.size(13.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "ক্রিয়েটেড ডেট: ${ticket.createdAt}",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                    if (ticket.scheduledDate.orEmpty().isNotEmpty()) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(Icons.Default.Schedule, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(13.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = "ভিজিট/সমাধান সময়: ${ticket.scheduledDate} (${ticket.scheduledTime})",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = ElectricBlue
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedButton(
                                                    onClick = { editingTicket = ticket },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text("তারিখ/সময় পরিবর্তন", fontSize = 10.sp)
                                                }

                                                AnimatedVisibility(
                                                    visible = ticket.status != "Resolved",
                                                    enter = fadeIn() + expandHorizontally(),
                                                    exit = fadeOut() + shrinkHorizontally()
                                                ) {
                                                    Button(
                                                        onClick = { onUpdateStatus(ticket, "Resolved") },
                                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(13.dp))
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                        Text("Resolved", fontSize = 10.sp)
                                                    }
                                                }

                                                AnimatedVisibility(
                                                    visible = ticket.status == "Pending",
                                                    enter = fadeIn() + expandHorizontally(),
                                                    exit = fadeOut() + shrinkHorizontally()
                                                ) {
                                                    Button(
                                                        onClick = { onUpdateStatus(ticket, "In Progress") },
                                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Icon(Icons.Default.Pending, contentDescription = null, modifier = Modifier.size(13.dp))
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                        Text("In Progress", fontSize = 10.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Log New Support Ticket / Service Request", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Search & Select Customer:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Teal600)
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("ID, Name or Phone...", fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                
                                if (filteredCustomers.isNotEmpty()) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Column {
                                            filteredCustomers.forEach { cust ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            selectedCustomer = cust
                                                            newCustName = cust.name
                                                            newPhone = cust.mobile
                                                            searchQuery = ""
                                                        }
                                                        .padding(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = Teal600)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("${cust.name} (${cust.customerCode})", fontSize = 12.sp)
                                                }
                                                if (cust != filteredCustomers.last()) HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp), color = Slate200)
                                            }
                                        }
                                    }
                                }
                            }

                            if (selectedCustomer != null) {
                                Surface(
                                    color = EmeraldSuccess.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Selected: ${selectedCustomer?.name}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                                        Spacer(modifier = Modifier.weight(1f))
                                        TextButton(onClick = { selectedCustomer = null; newCustName = ""; newPhone = "" }) {
                                            Text("Clear", color = CoralWarning, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = newCustName,
                                onValueChange = { newCustName = it },
                                label = { Text("Customer Name") },
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = selectedCustomer != null
                            )

                            OutlinedTextField(
                                value = newPhone,
                                onValueChange = { newPhone = it },
                                label = { Text("Phone Number") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = selectedCustomer != null
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("কমপ্লিন টাইটেল সিলেক্ট করুন:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    TextButton(onClick = onOpenSetup) {
                                        Text("+ Complain Setup", fontSize = 11.sp, color = Teal600, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (titlesList.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        titlesList.forEach { titleItem ->
                                            FilterChip(
                                                selected = newIssue == titleItem.title,
                                                onClick = { newIssue = titleItem.title },
                                                label = { Text(titleItem.title, fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = Teal600,
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = newIssue,
                                onValueChange = { newIssue = it },
                                label = { Text("Issue Type (e.g. LOS Red Light / Speed)") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = newDetails,
                                onValueChange = { newDetails = it },
                                label = { Text("Details / Staff Notes") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text("📌 রিকোয়েস্টের তারিখ ও সময় (Request Date & Time)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ReadonlyDateField(
                                    value = newReqDate,
                                    label = "আবেদনের তারিখ",
                                    onDateSelected = { newReqDate = it },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = newReqTime,
                                    onValueChange = { newReqTime = it },
                                    label = { Text("আবেদনের সময়") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }

                            Text("⏱️ টেকনিশিয়ান ভিজিটের নির্ধারিত সময় (Scheduled Visit)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ReadonlyDateField(
                                    value = newSchedDate,
                                    label = "ভিজিটের তারিখ",
                                    onDateSelected = { newSchedDate = it },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = newSchedTime,
                                    onValueChange = { newSchedTime = it },
                                    label = { Text("ভিজিটের সময়") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TextButton(onClick = { showNewComplaintForm = false }) {
                                    Text("Back")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (newCustName.isNotBlank()) {
                                            onAddComplaint(
                                                newIssue,
                                                newDetails,
                                                newCustName,
                                                newPhone,
                                                newSchedDate,
                                                newSchedTime,
                                                selectedCustomer
                                            )
                                            showNewComplaintForm = false
                                            selectedCustomer = null
                                            newCustName = ""
                                            newPhone = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Teal600)
                                ) {
                                    Text("Submit Ticket")
                                }
                            }
                        }
                    }
                }
            }
        }
    )

    editingTicket?.let { ticket ->
        var editSchedDate by remember { mutableStateOf(ticket.scheduledDate) }
        var editSchedTime by remember { mutableStateOf(ticket.scheduledTime) }

        AlertDialog(
            onDismissRequest = { editingTicket = null },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateStatus(ticket.copy(scheduledDate = editSchedDate, scheduledTime = editSchedTime), ticket.status)
                        editingTicket = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Teal600)
                ) {
                    Text("আপডেট করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingTicket = null }) {
                    Text("বাতিল")
                }
            },
            title = {
                Text("ভিজিট তারিখ ও সময় সংশোধন", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("গ্রাহক: ${ticket.customerName} (${ticket.customerCode})", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    Text("ভিজিট / সমাধানের তারিখ ও সময়:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReadonlyDateField(
                            value = editSchedDate.orEmpty(),
                            label = "তারিখ",
                            onDateSelected = { editSchedDate = it },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editSchedTime.orEmpty(),
                            onValueChange = { editSchedTime = it },
                            label = { Text("সময় (HH:MM AM/PM)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = if (onClick != null) modifier.clickable { onClick() } else modifier,
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = SleekCard),
        border = BorderStroke(1.dp, SleekBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Slate900
            )

            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Slate500,
                maxLines = 1
            )

            Text(
                text = subtitle,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = accentColor
            )
        }
    }
}

@Composable
fun MonthlyIncomeExpenseCanvasChart(monthlyIncome: Double, monthlyExpense: Double) {
    val gridColor = Slate200
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val months = listOf("Mar", "Apr", "May", "Jun", "Jul", "Aug")
            val incomeData = listOf(180000f, 210000f, 240000f, 280000f, 320000f, (monthlyIncome.toFloat() + 250000f).coerceAtLeast(350000f))
            val expenseData = listOf(70000f, 85000f, 90000f, 110000f, 120000f, (monthlyExpense.toFloat() + 95000f))

            val maxVal = 400000f
            val barWidth = 24.dp.toPx()
            val step = width / months.size

            // Draw Y-axis grid lines
            for (i in 0..4) {
                val y = height - (i * height / 5) - 30f
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
            }

            months.forEachIndexed { i, _ ->
                val x = i * step + step / 6

                val incHeight = (incomeData[i] / maxVal) * (height - 50f)
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(CyanAccent, ElectricBlue)),
                    topLeft = Offset(x, height - incHeight - 30f),
                    size = Size(barWidth, incHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                )

                val expHeight = (expenseData[i] / maxVal) * (height - 50f)
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(BkashPink, CoralWarning)),
                    topLeft = Offset(x + barWidth + 6f, height - expHeight - 30f),
                    size = Size(barWidth, expHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                )
            }
        }
    }
}

@Composable
fun CustomerGrowthTrendCanvasChart() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val dataPoints = listOf(0.8f, 0.7f, 0.55f, 0.45f, 0.3f, 0.15f)
            val step = width / (dataPoints.size - 1)
            
            val points = dataPoints.mapIndexed { i, valPct ->
                Offset(i * step, height * valPct)
            }

            // Draw Gradient Area under the curve
            val areaPath = Path().apply {
                moveTo(0f, height)
                lineTo(points.first().x, points.first().y)
                for (i in 0 until points.size - 1) {
                    val p1 = points[i]
                    val p2 = points[i+1]
                    val controlX = (p1.x + p2.x) / 2
                    cubicTo(controlX, p1.y, controlX, p2.y, p2.x, p2.y)
                }
                lineTo(width, height)
                close()
            }
            
            drawPath(
                path = areaPath,
                brush = Brush.verticalGradient(
                    colors = listOf(EmeraldSuccess.copy(alpha = 0.3f), Color.Transparent)
                )
            )

            // Draw Smooth Curve Line
            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 0 until points.size - 1) {
                    val p1 = points[i]
                    val p2 = points[i+1]
                    val controlX = (p1.x + p2.x) / 2
                    cubicTo(controlX, p1.y, controlX, p2.y, p2.x, p2.y)
                }
            }

            drawPath(
                path = linePath,
                color = EmeraldSuccess,
                style = Stroke(width = 8f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )

            // Draw indicator points
            points.forEach { point ->
                drawCircle(
                    color = Color.White,
                    radius = 10f,
                    center = point
                )
                drawCircle(
                    color = EmeraldSuccess,
                    radius = 6f,
                    center = point
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplainSetupDialog(
    titlesList: SnapshotStateList<ComplainTitleItem>,
    onDismiss: () -> Unit,
    onToast: (String) -> Unit = {}
) {
    var newTitleInput by remember { mutableStateOf("") }
    var showInPortalChecked by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("বন্ধ করুন", color = Teal700)
            }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Complain Setup",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Slate900
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                HorizontalDivider(color = SleekBorder)
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Create Complain Title",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    fontSize = 20.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = newTitleInput,
                    onValueChange = { newTitleInput = it },
                    placeholder = { Text("Add new complain", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Teal600,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showInPortalChecked = !showInPortalChecked }
                ) {
                    Checkbox(
                        checked = showInPortalChecked,
                        onCheckedChange = { showInPortalChecked = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Teal600
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Show in Customer Portal",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "Applies only if you have Customer Portal access.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 40.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (newTitleInput.isNotBlank()) {
                            titlesList.add(
                                ComplainTitleItem(
                                    id = System.currentTimeMillis(),
                                    title = newTitleInput.trim(),
                                    showInPortal = showInPortalChecked
                                )
                            )
                            onToast("নতুন কমপ্লিন টাইটেল যোগ করা হয়েছে: ${newTitleInput.trim()}")
                            newTitleInput = ""
                            showInPortalChecked = false
                        } else {
                            onToast("কমপ্লিন টাইটেল লিখুন")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1B859A)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Add",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = SleekBorder)
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "বিদ্যমান কমপ্লিন টাইটেল তালিকা (${titlesList.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Slate900
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (titlesList.isEmpty()) {
                    Text(
                        text = "কোন টাইটেল যোগ করা হয়নি",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    titlesList.forEachIndexed { idx, item ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, SleekBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${idx + 1}. ${item.title}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Slate900
                                    )
                                    Text(
                                        text = if (item.showInPortal) "🌐 Customer Portal (Active)" else "🔒 Internal Only",
                                        fontSize = 10.sp,
                                        color = if (item.showInPortal) Teal600 else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        titlesList.remove(item)
                                        onToast("টাইটেল মুছে ফেলা হয়েছে")
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = CoralWarning,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}
