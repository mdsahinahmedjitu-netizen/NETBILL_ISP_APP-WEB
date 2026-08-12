package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.Divider
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
import com.example.localization.AppTranslation
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

// Complaint Ticket Model
data class ComplaintItem(
    val id: Long,
    val customerName: String,
    val customerCode: String,
    val phone: String,
    val issueType: String,
    val details: String,
    var status: String, // "Pending", "In Progress", "Resolved"
    val date: String,
    var requestDate: String = "2026-08-08",
    var requestTime: String = "10:00 AM",
    var scheduledDate: String = "2026-08-08",
    var scheduledTime: String = "03:00 PM"
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
    onSelectCustomer: (CustomerEntity) -> Unit = {}
) {
    val stats by viewModel.dashboardStats.collectAsState()
    val payments by viewModel.paymentsList.collectAsState()
    val customersList by viewModel.customersList.collectAsState()
    val expiringCustomers by viewModel.expiringTomorrowCustomers.collectAsState()
    val currentLang by viewModel.currentLanguage.collectAsState()
    val isBangla = currentLang == com.example.localization.AppLanguage.BANGLA
    var showExpiryAlertSheet by remember { mutableStateOf(false) }
    val currency = AppTranslation("currency_symbol")
    val msgCollection = AppTranslation("grid_collection")
    val msgCollectionReport = AppTranslation("grid_collection_report")
    val msgBillSummary = AppTranslation("grid_bill_summary")

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

    // Seed initial complaint items for Complin List feature
    val complaintsList = remember {
        mutableStateListOf(
            ComplaintItem(
                id = 101,
                customerName = "Anwar Hossain",
                customerCode = "NET-1002",
                phone = "01812345678",
                issueType = "LOS Red Light (Fiber Cut)",
                details = "ONU light blinking red since morning. TJ box connection loose.",
                status = "Pending",
                date = "2026-08-08 09:30 AM",
                requestDate = "2026-08-08",
                requestTime = "09:30 AM",
                scheduledDate = "2026-08-08",
                scheduledTime = "02:00 PM"
            ),
            ComplaintItem(
                id = 102,
                customerName = "Sumon Ahmed",
                customerCode = "NET-1005",
                phone = "01512345678",
                issueType = "Slow Speed / Packet Loss",
                details = "Speed drops during evening peak hours. Requested port reset.",
                status = "In Progress",
                date = "2026-08-07 05:15 PM",
                requestDate = "2026-08-07",
                requestTime = "05:15 PM",
                scheduledDate = "2026-08-08",
                scheduledTime = "11:00 AM"
            ),
            ComplaintItem(
                id = 103,
                customerName = "Rahim Uddin",
                customerCode = "NET-1001",
                phone = "01712345678",
                issueType = "Router Reset Issue",
                details = "Customer replaced router, needs PPPoE setup.",
                status = "Resolved",
                date = "2026-08-06 11:00 AM",
                requestDate = "2026-08-06",
                requestTime = "11:00 AM",
                scheduledDate = "2026-08-06",
                scheduledTime = "01:30 PM"
            )
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(com.example.ui.theme.SleekBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Customer Expiry Alert Admin Banner
        if (expiringCustomers.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = AppTranslation("customer_expiry_alert_title"),
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 14.sp
                            )
                            val countStr = if (isBangla) com.example.ui.screens.toBanglaDigits(expiringCustomers.size) else expiringCustomers.size.toString()
                            val bodyText = if (isBangla) {
                                "$countStr জন গ্রাহকের লাইনের মেয়াদ আগামীকাল শেষ হবে।"
                            } else {
                                if (expiringCustomers.size == 1) "1 customer will expire tomorrow."
                                else "$countStr customers will expire tomorrow."
                            }
                            Text(
                                text = bodyText,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { showExpiryAlertSheet = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(AppTranslation("view_alerts"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // PRIMARY 8-CARD FEATURE GRID (Identical to uploaded screenshot)
        item {
            ISPFeatureGridSection(
                onCollectionClick = {
                    viewModel.showToast(msgCollection)
                    onNavigateToPayments()
                },
                onCollectionReportClick = {
                    viewModel.showToast(msgCollectionReport)
                    onNavigateToReports()
                },
                onListReportClick = {
                    viewModel.updateFilter(query = "", zone = "All", pkg = "All", status = "All", onlyDue = false)
                    onNavigateToCustomers()
                },
                onDueListClick = {
                    viewModel.updateFilter(query = "", zone = "All", pkg = "All", status = "All", onlyDue = true)
                    onNavigateToCustomers()
                },
                onCreateNewClick = {
                    showCreateCustomerDialog = true
                },
                onSearchClick = {
                    showSearchCustomerDialog = true
                },
                onComplinListClick = {
                    showComplaintsDialog = true
                },
                onBillSummaryClick = {
                    viewModel.showToast(msgBillSummary)
                    onNavigateToBilling()
                }
            )
        }

        // Primary Hero Collection Card (Restored TodaysCollectionCard)
        item {
            TodaysCollectionCard(
                payments = payments,
                selectedFilter = activeFilter,
                onFilterSelected = { activeFilter = it }
            )
        }

        // Hero Collection Summary Banner (Red-marked option from user screenshot)
        item {
            val displayCollection = activeSummary?.grandTotal ?: stats.todaysCollection
            val periodTitle = when (activeFilter) {
                com.example.ui.components.CollectionFilterPeriod.TODAY -> "আজকের সংগ্রহ (Today's Collection)"
                com.example.ui.components.CollectionFilterPeriod.YESTERDAY -> "গতকালকের সংগ্রহ (Yesterday's Collection)"
                com.example.ui.components.CollectionFilterPeriod.LAST_7_DAYS -> "গত ৭ দিনের সংগ্রহ (Last 7 Days Collection)"
                com.example.ui.components.CollectionFilterPeriod.THIS_MONTH -> "চলতি মাসের সংগ্রহ (This Month's Collection)"
                com.example.ui.components.CollectionFilterPeriod.CUSTOM -> "কাস্টম তারিখের সংগ্রহ (${activeSummary?.periodLabel ?: ""})"
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.Teal600)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(com.example.ui.theme.Teal600, com.example.ui.theme.Teal700)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    text = periodTitle,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = com.example.ui.theme.Teal100,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "$currency ${displayCollection.toInt()}",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column {
                                Text(
                                    text = "TARGET",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = com.example.ui.theme.Teal100
                                )
                                Text(
                                    text = "$currency 25,000",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(28.dp)
                                    .background(Color.White.copy(alpha = 0.2f))
                            )
                            Column {
                                Text(
                                    text = "TOTAL DUE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = com.example.ui.theme.Teal100
                                )
                                Text(
                                    text = "$currency ${stats.totalDue.toInt()}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // Dashboard Metrics Grid Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Row 1: Total Customers & Active Customers
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard(
                        title = AppTranslation("total_customers"),
                        value = "${stats.totalCustomers}",
                        subtitle = "509 Customers Goal",
                        icon = Icons.Default.People,
                        accentColor = ElectricBlue,
                        onClick = {
                            viewModel.updateFilter(query = "", zone = "All", pkg = "All", status = "All", onlyDue = false)
                            onNavigateToCustomers()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = AppTranslation("active_customers"),
                        value = "${stats.activeCustomers}",
                        subtitle = "Online PPPoE",
                        icon = Icons.Default.CheckCircle,
                        accentColor = EmeraldSuccess,
                        onClick = {
                            viewModel.updateFilter(query = "", zone = "All", pkg = "All", status = "Active", onlyDue = false)
                            onNavigateToCustomers()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2: Expired Customers & Inactive Customers
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard(
                        title = AppTranslation("expired_customers"),
                        value = "${stats.expiredCustomers}",
                        subtitle = "Validity Over",
                        icon = Icons.Default.NotificationsActive,
                        accentColor = Color(0xFFE11D48),
                        onClick = {
                            showExpiryAlertSheet = true
                        },
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = AppTranslation("inactive_customers"),
                        value = "${stats.inactiveCustomers}",
                        subtitle = "Suspended / Offline",
                        icon = Icons.Default.Pending,
                        accentColor = Slate600,
                        onClick = {
                            viewModel.updateFilter(query = "", zone = "All", pkg = "All", status = "Inactive", onlyDue = false)
                            onNavigateToCustomers()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 3: Running Month Expense & New Customers
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard(
                        title = AppTranslation("monthly_expense"),
                        value = "$currency ${stats.monthlyExpense.toInt()}",
                        subtitle = "Current Month Outflow",
                        icon = Icons.Default.ArrowUpward,
                        accentColor = BkashPink,
                        onClick = {
                            onNavigateToReports()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = AppTranslation("new_customers"),
                        value = "${stats.newCustomers}",
                        subtitle = "Joined this month",
                        icon = Icons.Default.PersonAdd,
                        accentColor = ElectricBlue,
                        onClick = { showNewCustomersDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 4: Bandwidth Usage
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard(
                        title = AppTranslation("bandwidth_usage"),
                        value = "${stats.bandwidthUsageMbps} Mbps",
                        subtitle = "Live IIG Traffic",
                        icon = Icons.Default.Speed,
                        accentColor = CyanAccent,
                        modifier = Modifier.weight(1f)
                    )
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }

        // Dedicated Customer Complaints Section (Replaced New Customers section)
        item {
            val unresolvedComplaints = complaintsList.filter { it.status != "Resolved" }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.SleekCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.CoralWarning.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(com.example.ui.theme.CoralWarning.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReportProblem,
                                    contentDescription = null,
                                    tint = com.example.ui.theme.CoralWarning,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "গ্রাহকের কমপ্লিন তালিকা (Complaints)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = com.example.ui.theme.Slate900
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(com.example.ui.theme.CoralWarning)
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${unresolvedComplaints.size}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                    }
                                }
                                Text(
                                    text = "সমাধান না হওয়া পর্যন্ত ড্যাসবোর্ডে দৃশ্যমান থাকবে",
                                    fontSize = 11.sp,
                                    color = com.example.ui.theme.Slate500
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = { showComplainSetupDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, com.example.ui.theme.Teal600),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null, tint = com.example.ui.theme.Teal600, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("কমপ্লিন সেটাপ", fontSize = 11.sp, color = com.example.ui.theme.Teal600, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            TextButton(onClick = { showComplaintsDialog = true }) {
                                Text("সব দেখুন", fontSize = 12.sp, color = com.example.ui.theme.CoralWarning, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    AnimatedContent(
                        targetState = unresolvedComplaints.isEmpty(),
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(400)) + expandVertically())
                                .togetherWith(fadeOut(animationSpec = tween(300)) + shrinkVertically())
                        },
                        label = "unresolvedComplaintsAnimation"
                    ) { isEmpty ->
                        if (isEmpty) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(com.example.ui.theme.Teal50)
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = com.example.ui.theme.Teal600)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "সব কমপ্লিন সমাধান করা হয়েছে! কোন অমীমাংসিত অভিযোগ নেই।",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                unresolvedComplaints.forEach { complaint ->
                                    AnimatedVisibility(
                                        visible = complaint.status != "Resolved",
                                        enter = fadeIn(animationSpec = tween(300)) + expandVertically() + slideInVertically(),
                                        exit = fadeOut(animationSpec = tween(300)) + shrinkVertically()
                                    ) {
                                        Card(
                                            shape = RoundedCornerShape(14.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                com.example.ui.theme.CoralWarning.copy(alpha = 0.4f)
                                            ),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        // Red mark / Alert indicator right next to each complaint
                                                        Box(
                                                            modifier = Modifier
                                                                .size(32.dp)
                                                                .clip(CircleShape)
                                                                .background(com.example.ui.theme.CoralWarning),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.ReportProblem,
                                                                contentDescription = "Alert Mark",
                                                                tint = Color.White,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }

                                                        Spacer(modifier = Modifier.width(10.dp))

                                                        Column {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Text(
                                                                    text = complaint.customerName,
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 14.sp,
                                                                    color = com.example.ui.theme.Slate900
                                                                )
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Box(
                                                                    modifier = Modifier
                                                                        .clip(RoundedCornerShape(4.dp))
                                                                        .background(ElectricBlue.copy(alpha = 0.15f))
                                                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                                                ) {
                                                                    Text(
                                                                        text = complaint.customerCode,
                                                                        fontSize = 10.sp,
                                                                        color = ElectricBlue,
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                }
                                                            }
                                                            Text(
                                                                text = "📱 ${complaint.phone} • 📅 ${complaint.requestTime}",
                                                                fontSize = 11.sp,
                                                                color = com.example.ui.theme.Slate600
                                                            )
                                                        }
                                                    }

                                                    // Red status tag
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(com.example.ui.theme.CoralWarning.copy(alpha = 0.15f))
                                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(6.dp)
                                                                    .clip(CircleShape)
                                                                    .background(com.example.ui.theme.CoralWarning)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = complaint.status,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = com.example.ui.theme.CoralWarning
                                                            )
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))

                                                // Issue details
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(MaterialTheme.colorScheme.surface)
                                                        .padding(8.dp)
                                                ) {
                                                    Column {
                                                        Text(
                                                            text = "⚠️ ${complaint.issueType}",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = com.example.ui.theme.CoralWarning
                                                        )
                                                        Text(
                                                            text = complaint.details,
                                                            fontSize = 11.sp,
                                                            color = com.example.ui.theme.Slate700
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(10.dp))

                                                // Quick resolution button
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.End,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Button(
                                                        onClick = {
                                                            val idx = complaintsList.indexOfFirst { it.id == complaint.id }
                                                            if (idx != -1) {
                                                                complaintsList[idx] = complaint.copy(status = "Resolved")
                                                            }
                                                            viewModel.showToast("অভিযোগটি সমাধান হিসেবে সম্পন্ন করা হয়েছে!")
                                                        },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = com.example.ui.theme.Teal600,
                                                            contentColor = Color.White
                                                        ),
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                        modifier = Modifier.height(34.dp)
                                                    ) {
                                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("সমাধান সম্পন্ন", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Income vs Expense Chart Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.SleekCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.SleekBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = AppTranslation("monthly_income_graph"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = com.example.ui.theme.Slate900
                    )
                    Text(
                        text = "Monthly Collections ($currency) vs Expense ($currency)",
                        style = MaterialTheme.typography.bodySmall,
                        color = com.example.ui.theme.Slate500
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    MonthlyIncomeExpenseCanvasChart(
                        monthlyIncome = stats.monthlyCollection,
                        monthlyExpense = stats.todaysExpense * 15
                    )
                }
            }
        }

        // Customer Growth Chart Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.SleekCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.SleekBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = AppTranslation("customer_growth"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = com.example.ui.theme.Slate900
                    )
                    Text(
                        text = "Subscriber acquisition over recent 6 months",
                        style = MaterialTheme.typography.bodySmall,
                        color = com.example.ui.theme.Slate500
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    CustomerGrowthTrendCanvasChart()
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Interactive Dialog 1: Create New Customer Dialog
    if (showCreateCustomerDialog) {
        CreateCustomerDashboardDialog(
            viewModel = viewModel,
            onDismiss = { showCreateCustomerDialog = false }
        )
    }

    // Interactive Dialog 2: Search Customer Dialog
    if (showSearchCustomerDialog) {
        SearchCustomerDashboardDialog(
            customersList = customersList,
            onSelectCustomer = { customer ->
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
            onSelectCustomer = { customer ->
                showNewCustomersDialog = false
                onSelectCustomer(customer)
            },
            onDismiss = { showNewCustomersDialog = false }
        )
    }

    // Interactive Dialog 3: Complaints / Support Ticket List Dialog
    if (showComplaintsDialog) {
        ComplaintsDashboardDialog(
            complaints = complaintsList,
            titlesList = complainTitlesList,
            onOpenSetup = { showComplainSetupDialog = true },
            onUpdateStatus = { complaint, newStatus ->
                val idx = complaintsList.indexOfFirst { it.id == complaint.id }
                if (idx != -1) {
                    complaintsList[idx] = complaint.copy(status = newStatus)
                }
                viewModel.showToast("অভিযোগ আপডেট করা হয়েছে: $newStatus")
            },
            onAddComplaint = { newComplaint ->
                complaintsList.add(0, newComplaint)
                viewModel.showToast("নতুন অভিযোগ নথিবদ্ধ করা হয়েছে!")
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

    // Customer Expiry Alert Bottom Sheet
    if (showExpiryAlertSheet) {
        ExpiryAlertBottomSheet(
            viewModel = viewModel,
            onDismiss = { showExpiryAlertSheet = false },
            onSelectCustomer = onSelectCustomer
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
        // Row 1: Collection (Blue/Violet) & Collection Report (Lime Green)
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ISPFeatureCard(
                label = AppTranslation("grid_collection"),
                modifier = Modifier.weight(1f),
                textColor = Color.White,
                gradientBrush = Brush.linearGradient(
                    listOf(Color(0xFF6200EE), Color(0xFF3700B3), Color(0xFF0052D4))
                ),
                onClick = onCollectionClick,
                iconContent = { CollectionGridIcon(Modifier.size(72.dp)) }
            )
            ISPFeatureCard(
                label = AppTranslation("grid_collection_report"),
                modifier = Modifier.weight(1f),
                textColor = Color.White,
                gradientBrush = Brush.linearGradient(
                    listOf(Color(0xFF00E65B), Color(0xFF00B33C), Color(0xFF008026))
                ),
                onClick = onCollectionReportClick,
                iconContent = { CollectionReportGridIcon(Modifier.size(72.dp)) }
            )
        }

        // Row 2: List Report (Magenta/Purple) & Due List (Orange/Red)
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ISPFeatureCard(
                label = AppTranslation("grid_list_report"),
                modifier = Modifier.weight(1f),
                textColor = Color.White,
                gradientBrush = Brush.linearGradient(
                    listOf(Color(0xFFFF007A), Color(0xFFD900FF), Color(0xFF8B00FF))
                ),
                onClick = onListReportClick,
                iconContent = { ListReportGridIcon(Modifier.size(72.dp)) }
            )
            ISPFeatureCard(
                label = AppTranslation("grid_due_list"),
                modifier = Modifier.weight(1f),
                textColor = Color.White,
                gradientBrush = Brush.linearGradient(
                    listOf(Color(0xFFFF8000), Color(0xFFFF4500), Color(0xFFE52E00))
                ),
                onClick = onDueListClick,
                iconContent = { DueListGridIcon(Modifier.size(72.dp)) }
            )
        }

        // Row 3: Create New (Cyan/Blue) & Search (Violet/Deep Purple)
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ISPFeatureCard(
                label = AppTranslation("grid_create_new"),
                modifier = Modifier.weight(1f),
                textColor = Color.White,
                gradientBrush = Brush.linearGradient(
                    listOf(Color(0xFF00D2FF), Color(0xFF0080FF), Color(0xFF0052D4))
                ),
                onClick = onCreateNewClick,
                iconContent = { CreateNewGridIcon(Modifier.size(72.dp)) }
            )
            ISPFeatureCard(
                label = AppTranslation("grid_search"),
                modifier = Modifier.weight(1f),
                textColor = Color.White,
                gradientBrush = Brush.linearGradient(
                    listOf(Color(0xFF8A00FF), Color(0xFF6200EA), Color(0xFF4A00E0))
                ),
                onClick = onSearchClick,
                iconContent = { SearchGridIcon(Modifier.size(72.dp)) }
            )
        }

        // Row 4: Complin List (Bright Yellow) & Bill Summary (Pink/Magenta)
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ISPFeatureCard(
                label = AppTranslation("grid_complaint_list"),
                modifier = Modifier.weight(1f),
                textColor = Color(0xFF5B1A00),
                gradientBrush = Brush.linearGradient(
                    listOf(Color(0xFFFFE500), Color(0xFFFFB700), Color(0xFFFF8800))
                ),
                onClick = onComplinListClick,
                iconContent = { ComplinListGridIcon(Modifier.size(72.dp)) }
            )
            ISPFeatureCard(
                label = AppTranslation("grid_bill_summary"),
                modifier = Modifier.weight(1f),
                textColor = Color.White,
                gradientBrush = Brush.linearGradient(
                    listOf(Color(0xFFFF007A), Color(0xFFE000FF), Color(0xFF8000FF))
                ),
                onClick = onBillSummaryClick,
                iconContent = { BillSummaryGridIcon(Modifier.size(72.dp)) }
            )
        }
    }
}

// INTERACTIVE DIALOG: NEW CUSTOMERS LIST
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
            it.zone.contains(query, ignoreCase = true)
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
                                            Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = com.example.ui.theme.Slate900)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("(${customer.customerCode})", fontSize = 11.sp, color = ElectricBlue, fontWeight = FontWeight.Bold)
                                        }
                                        Text("📱 ${customer.mobile} • 📍 ${customer.zone}", fontSize = 11.sp, color = com.example.ui.theme.Slate600)
                                        Text("📦 ${customer.packageName} • ৳${customer.monthlyBill.toInt()}/mo", fontSize = 11.sp, color = com.example.ui.theme.Teal600, fontWeight = FontWeight.SemiBold)
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
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp,
            pressedElevation = 10.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (gradientBrush != null) Modifier.background(gradientBrush)
                    else Modifier.background(backgroundColor)
                )
                .padding(vertical = 20.dp, horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(76.dp),
                    contentAlignment = Alignment.Center
                ) {
                    iconContent()
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = label,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

// INTERACTIVE DIALOG 1: CREATE NEW CUSTOMER
@Composable
fun CreateCustomerDashboardDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    AddEditCustomerDialog(
        customer = null,
        onDismiss = onDismiss,
        onSave = { newCustomer ->
            viewModel.addOrUpdateCustomer(newCustomer)
            onDismiss()
        }
    )
}

// INTERACTIVE DIALOG 2: SEARCH CUSTOMERS
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
                Text("বন্ধ করুন", color = com.example.ui.theme.Teal700)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, contentDescription = null, tint = com.example.ui.theme.Teal600)
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
                                        Text("Pkg: ${customer.packageName} • Due: ৳${customer.currentDue.toInt()}", fontSize = 11.sp, color = if (customer.currentDue > 0) CoralWarning else EmeraldSuccess, fontWeight = FontWeight.Bold)
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
@Composable
fun ComplaintsDashboardDialog(
    complaints: List<ComplaintItem>,
    titlesList: List<ComplainTitleItem> = emptyList(),
    onOpenSetup: () -> Unit = {},
    onUpdateStatus: (ComplaintItem, String) -> Unit,
    onAddComplaint: (ComplaintItem) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("All") }
    var showNewComplaintForm by remember { mutableStateOf(false) }

    var newCustName by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var newIssue by remember { mutableStateOf("LOS Red Light") }
    var newDetails by remember { mutableStateOf("") }
    var newReqDate by remember { mutableStateOf("2026-08-08") }
    var newReqTime by remember { mutableStateOf("10:30 AM") }
    var newSchedDate by remember { mutableStateOf("2026-08-08") }
    var newSchedTime by remember { mutableStateOf("03:00 PM") }

    var editingTicket by remember { mutableStateOf<ComplaintItem?>(null) }

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
                                            selectedContainerColor = com.example.ui.theme.Teal600,
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
                                ) { ticket ->
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
                                                            Text(currentStatus, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Issue: ${ticket.issueType}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                            Text(ticket.details, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.height(6.dp))

                                            // Display Request Date & Time and Scheduled Resolution Date & Time
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
                                                            text = "রিকোয়েস্ট তারিখ ও সময়: ${ticket.requestDate} (${ticket.requestTime})",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
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
                    // New Complaint Form View
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Log New Support Ticket / Service Request", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                        OutlinedTextField(
                            value = newCustName,
                            onValueChange = { newCustName = it },
                            label = { Text("Customer Name") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = newPhone,
                            onValueChange = { newPhone = it },
                            label = { Text("Phone Number") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth()
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
                            OutlinedTextField(
                                value = newReqDate,
                                onValueChange = { newReqDate = it },
                                label = { Text("আবেদনের তারিখ") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
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
                            OutlinedTextField(
                                value = newSchedDate,
                                onValueChange = { newSchedDate = it },
                                label = { Text("ভিজিটের তারিখ") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
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
                                            ComplaintItem(
                                                id = System.currentTimeMillis(),
                                                customerName = newCustName,
                                                customerCode = "NET-${(1000..9999).random()}",
                                                phone = newPhone,
                                                issueType = newIssue,
                                                details = newDetails,
                                                status = "Pending",
                                                date = "$newReqDate $newReqTime",
                                                requestDate = newReqDate,
                                                requestTime = newReqTime,
                                                scheduledDate = newSchedDate,
                                                scheduledTime = newSchedTime
                                            )
                                        )
                                        showNewComplaintForm = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.Teal600)
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

    // Edit Date/Time Dialog for existing support ticket request
    editingTicket?.let { ticket ->
        var editReqDate by remember { mutableStateOf(ticket.requestDate) }
        var editReqTime by remember { mutableStateOf(ticket.requestTime) }
        var editSchedDate by remember { mutableStateOf(ticket.scheduledDate) }
        var editSchedTime by remember { mutableStateOf(ticket.scheduledTime) }

        AlertDialog(
            onDismissRequest = { editingTicket = null },
            confirmButton = {
                Button(
                    onClick = {
                        ticket.requestDate = editReqDate
                        ticket.requestTime = editReqTime
                        ticket.scheduledDate = editSchedDate
                        ticket.scheduledTime = editSchedTime
                        editingTicket = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.Teal600)
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
                Text("রিকোয়েস্টের তারিখ ও সময় সংশোধন", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("গ্রাহক: ${ticket.customerName} (${ticket.customerCode})", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    Text("রিকোয়েস্ট গ্রহণের তারিখ ও সময়:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editReqDate,
                            onValueChange = { editReqDate = it },
                            label = { Text("তারিখ (YYYY-MM-DD)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = editReqTime,
                            onValueChange = { editReqTime = it },
                            label = { Text("সময় (HH:MM AM/PM)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Text("ভিজিট / সমাধানের তারিখ ও সময়:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editSchedDate,
                            onValueChange = { editSchedDate = it },
                            label = { Text("তারিখ (YYYY-MM-DD)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = editSchedTime,
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.SleekCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.SleekBorder)
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
                color = com.example.ui.theme.Slate900
            )

            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = com.example.ui.theme.Slate500,
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val months = listOf("Mar", "Apr", "May", "Jun", "Jul", "Aug")
            val incomeData = listOf(180000f, 210000f, 240000f, 280000f, 320000f, (monthlyIncome.toFloat() + 250000f).coerceAtLeast(350000f))
            val expenseData = listOf(70000f, 85000f, 90000f, 110000f, 120000f, (monthlyExpense.toFloat() + 95000f))

            val maxVal = 400000f
            val barWidth = 20.dp.toPx()
            val step = width / months.size

            months.forEachIndexed { i, _ ->
                val x = i * step + step / 4

                val incHeight = (incomeData[i] / maxVal) * (height - 30f)
                drawRect(
                    brush = Brush.verticalGradient(listOf(CyanAccent, ElectricBlue)),
                    topLeft = Offset(x, height - incHeight - 20f),
                    size = Size(barWidth, incHeight)
                )

                val expHeight = (expenseData[i] / maxVal) * (height - 30f)
                drawRect(
                    brush = Brush.verticalGradient(listOf(BkashPink, CoralWarning)),
                    topLeft = Offset(x + barWidth + 4f, height - expHeight - 20f),
                    size = Size(barWidth, expHeight)
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
            .height(120.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val points = listOf(
                Offset(0f, height * 0.8f),
                Offset(width * 0.2f, height * 0.7f),
                Offset(width * 0.4f, height * 0.55f),
                Offset(width * 0.6f, height * 0.45f),
                Offset(width * 0.8f, height * 0.3f),
                Offset(width, height * 0.15f)
            )

            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.forEach { point ->
                    lineTo(point.x, point.y)
                }
            }

            drawPath(
                path = path,
                color = EmeraldSuccess,
                style = Stroke(width = 6f)
            )

            points.forEach { point ->
                drawCircle(
                    color = Color.White,
                    radius = 8f,
                    center = point
                )
                drawCircle(
                    color = EmeraldSuccess,
                    radius = 5f,
                    center = point
                )
            }
        }
    }
}

// INTERACTIVE DIALOG 5: COMPLAIN SETUP DIALOG (Matching user screenshot)
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
                Text("বন্ধ করুন", color = com.example.ui.theme.Teal700)
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
                    color = com.example.ui.theme.Slate900
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
                Divider(color = com.example.ui.theme.SleekBorder)
                Spacer(modifier = Modifier.height(16.dp))

                // Create Complain Title Section
                Text(
                    text = "Create Complain Title",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = com.example.ui.theme.Slate900,
                    fontSize = 20.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Text input box with placeholder "Add new complain"
                OutlinedTextField(
                    value = newTitleInput,
                    onValueChange = { newTitleInput = it },
                    placeholder = { Text("Add new complain", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = com.example.ui.theme.Teal600,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Checkbox: Show in Customer Portal
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
                            checkedColor = com.example.ui.theme.Teal600
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

                // Add button
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
                Divider(color = com.example.ui.theme.SleekBorder)
                Spacer(modifier = Modifier.height(14.dp))

                // Existing Complain Titles list
                Text(
                    text = "বিদ্যমান কমপ্লিন টাইটেল তালিকা (${titlesList.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = com.example.ui.theme.Slate900
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
                            border = BorderStroke(1.dp, com.example.ui.theme.SleekBorder)
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
                                        color = com.example.ui.theme.Slate900
                                    )
                                    Text(
                                        text = if (item.showInPortal) "🌐 Customer Portal (Active)" else "🔒 Internal Only",
                                        fontSize = 10.sp,
                                        color = if (item.showInPortal) com.example.ui.theme.Teal600 else MaterialTheme.colorScheme.onSurfaceVariant
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
                                        tint = com.example.ui.theme.CoralWarning,
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
