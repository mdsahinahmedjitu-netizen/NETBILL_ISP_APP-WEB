package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.outlined.LiveHelp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.CustomerEntity
import com.example.viewmodel.MainViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDashboardScreen(viewModel: MainViewModel) {
    val customer by viewModel.currentCustomer.collectAsState()
    val payments by viewModel.paymentsList.collectAsState()
    val tickets by viewModel.supportTickets.collectAsState()
    val settings by viewModel.settingsState.collectAsState()
    val currency = "৳"

    if (customer == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Teal600)
        }
        return
    }

    val cust = customer!!
    var showSupportDialog by remember { mutableStateOf(false) }
    var showPayDialog by remember { mutableStateOf(false) }
    
    // Traffic Animation Simulation
    var liveTrafficUp by remember { mutableStateOf(0.0) }
    var liveTrafficDown by remember { mutableStateOf(0.0) }
    
    LaunchedEffect(Unit) {
        while(true) {
            liveTrafficDown = (100..5000).random() / 100.0
            liveTrafficUp = (10..500).random() / 100.0
            delay(3.seconds)
        }
    }

    val myPayments = remember(payments, cust.id) {
        payments.filter { it.customerId == cust.id }.sortedByDescending { it.paymentDate }
    }
    
    val myTickets = remember(tickets, cust.id) {
        tickets.filter { it.customerId == cust.id }.sortedByDescending { it.createdAt }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text("NETBILL SELF-CARE", 
                        fontWeight = FontWeight.Black, 
                        fontSize = 16.sp, 
                        letterSpacing = 2.sp,
                        color = Teal700
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { /* Profile Action */ }) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Teal600)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.logout() }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = CoralWarning)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { PremiumHeader(cust = cust) }

            item {
                ConnectivitySection(
                    status = cust.status, 
                    up = liveTrafficUp, 
                    down = liveTrafficDown,
                    expireDate = cust.expireDate
                )
            }

            item {
                BillingActionCard(
                    due = cust.currentDue, 
                    advance = cust.advanceBalance,
                    onPayClick = { showPayDialog = true }
                )
            }

            item {
                ServiceGrid(
                    onReportClick = { showSupportDialog = true },
                    onHistoryClick = { /* Scroll Logic */ }
                )
            }

            if (myTickets.isNotEmpty()) {
                item { SectionHeader(title = "My Support Tickets", icon = Icons.AutoMirrored.Outlined.LiveHelp) }
                items(myTickets.take(3)) { ticket -> TicketCompactItem(ticket = ticket) }
            }

            if (myPayments.isNotEmpty()) {
                item { SectionHeader(title = "Recent Payments", icon = Icons.Outlined.History) }
                items(myPayments.take(5)) { pymt -> PaymentCompactItem(pymt = pymt, currency = currency) }
            }
            
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }

    if (showSupportDialog) {
        CustomerTicketDialog(
            onDismiss = { showSupportDialog = false },
            onSave = { type, desc ->
                viewModel.createSupportTicket(cust, type, desc)
                showSupportDialog = false
            }
        )
    }

    if (showPayDialog) {
        CustomerManualPayDialog(
            settings = settings,
            onDismiss = { showPayDialog = false },
            onSave = { method, trx ->
                viewModel.submitPaymentRequest(cust.id, cust.currentDue, method, trx) { success, msg ->
                    viewModel.showToast(msg)
                    if (success) showPayDialog = false
                }
            },
            currentDue = cust.currentDue
        )
    }
}

@Composable
fun PremiumHeader(cust: CustomerEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Teal600),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Teal600, Color(0xFF0D9488), Color(0xFF0F766E))))
                .padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = cust.name.take(1).uppercase(), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    Text("Assalamualikum,", color = Teal100, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(cust.name, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Fingerprint, null, tint = Teal100, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ID: ${cust.customerCode}", color = Teal50, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectivitySection(status: String, up: Double, down: Double, expireDate: String) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SpeedCard(label = "Download", value = "$down", unit = "Mbps", icon = Icons.Default.ArrowDownward, color = ElectricBlue, modifier = Modifier.weight(1f))
            SpeedCard(label = "Upload", value = "$up", unit = "Mbps", icon = Icons.Default.ArrowUpward, color = Teal600, modifier = Modifier.weight(1f))
        }
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0))) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(if(status == "Active") EmeraldSuccess else CoralWarning))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Connection: $status", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Text("Exp: $expireDate", color = CoralWarning, fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun SpeedCard(label: String, value: String, unit: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate600)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Slate900)
                Spacer(modifier = Modifier.width(4.dp))
                Text(unit, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
    }
}

@Composable
fun BillingActionCard(due: Double, advance: Double, onPayClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(32.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), border = BorderStroke(1.dp, Color(0xFFE2E8F0))) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("মোট বকেয়া (Due)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CoralWarning)
                    Text("৳ ${due.toInt()}", fontSize = 36.sp, fontWeight = FontWeight.Black, color = if(due > 0) CoralWarning else EmeraldSuccess)
                }
                Button(onClick = onPayClick, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = BkashPink), modifier = Modifier.height(50.dp).padding(horizontal = 4.dp)) {
                    Icon(Icons.Outlined.Payments, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PAY NOW", fontWeight = FontWeight.Black)
                }
            }
            if (advance > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(color = EmeraldSuccess.copy(alpha = 0.05f), shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Savings, null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("You have ৳ ${advance.toInt()} advance in account.", fontSize = 11.sp, color = EmeraldSuccess, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceGrid(onReportClick: () -> Unit, onHistoryClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        ServiceItem(label = "Report Issue", icon = Icons.Outlined.NetworkCheck, color = Color(0xFFF59E0B), onClick = onReportClick, modifier = Modifier.weight(1f))
        ServiceItem(label = "Bill History", icon = Icons.Outlined.History, color = ElectricBlue, onClick = onHistoryClick, modifier = Modifier.weight(1f))
    }
}

@Composable
fun ServiceItem(label: String, icon: ImageVector, color: Color, onClick: () -> Unit, modifier: Modifier) {
    Card(modifier = modifier.clip(RoundedCornerShape(24.dp)).clickable { onClick() }, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)), border = BorderStroke(1.dp, color.copy(alpha = 0.2f))) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Black, color = color, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
        Icon(icon, null, tint = Slate600, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, fontWeight = FontWeight.Black, color = Slate900, fontSize = 14.sp, letterSpacing = 1.sp)
    }
}

@Composable
fun TicketCompactItem(ticket: com.example.data.entity.SupportTicketEntity) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFF1F5F9))) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(if(ticket.status == "Resolved") EmeraldSuccess.copy(alpha = 0.1f) else Color(0xFFFEF3C7)), contentAlignment = Alignment.Center) {
                Icon(if(ticket.status == "Resolved") Icons.Default.CheckCircle else Icons.Default.Pending, null, tint = if(ticket.status == "Resolved") EmeraldSuccess else Color(0xFFD97706), modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(ticket.issueType, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Slate900)
                Text(ticket.description, fontSize = 12.sp, color = Slate500, maxLines = 1)
            }
            Text(ticket.status, fontWeight = FontWeight.Black, fontSize = 10.sp, color = if(ticket.status == "Resolved") EmeraldSuccess else Color(0xFFD97706))
        }
    }
}

@Composable
fun PaymentCompactItem(pymt: com.example.data.entity.PaymentCollectionEntity, currency: String) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFF1F5F9))) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Teal50), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Receipt, null, tint = Teal600, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(pymt.billingMonth, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Slate900)
                    Text("${pymt.paymentDate} • ${pymt.paymentMethod}", fontSize = 11.sp, color = Slate500)
                }
            }
            Text("$currency ${pymt.amount.toInt()}", fontWeight = FontWeight.Black, color = EmeraldSuccess, fontSize = 16.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerManualPayDialog(
    settings: com.example.data.entity.ISPSettingsEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    currentDue: Double
) {
    var selectedMethod by remember { mutableStateOf("bKash") }
    var trxId by remember { mutableStateOf("") }
    val paymentNumber = if (selectedMethod == "bKash") settings?.personalBkashNo ?: "017XXXXXXXX" else settings?.personalNagadNo ?: "018XXXXXXXX"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("বিল পরিশোধ নির্দেশিকা", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("bKash", "Nagad").forEach { m ->
                        val isSel = selectedMethod == m
                        val color = if (m == "bKash") BkashPink else NagadOrange
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (isSel) color else Slate100).clickable { selectedMethod = m }.padding(12.dp), contentAlignment = Alignment.Center) {
                            Text(m, color = if (isSel) Color.White else Slate700, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Card(colors = CardDefaults.cardColors(containerColor = Teal50), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("১. আপনার $selectedMethod অ্যাপ থেকে নিচের নাম্বারে Send Money করুন।", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate800)
                        Text(paymentNumber, fontSize = 22.sp, fontWeight = FontWeight.Black, color = Teal700, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        Text("পরিমাণ: ৳${currentDue.toInt()}", fontSize = 11.sp, color = Slate600)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("২. টাকা পাঠানোর পর ট্রানজেকশন আইডি (TrxID) দিন:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate800)
                    OutlinedTextField(value = trxId, onValueChange = { trxId = it }, placeholder = { Text("E.g. BK89201472X") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (trxId.length >= 8) onSave(selectedMethod, trxId) }, colors = ButtonDefaults.buttonColors(containerColor = Teal600)) { Text("পেমেন্ট সাবমিট করুন") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerTicketDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var issueType by remember { mutableStateOf("Internet Slow") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("নতুন অভিযোগ দিন (New Complaint)", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("সমস্যার ধরণ নির্বাচন করুন:", fontSize = 12.sp, color = Slate600)
                val issues = listOf("Internet Slow", "No Internet / LOS", "Router Config", "Billing Issue")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    issues.take(2).forEach { issue ->
                        FilterChip(selected = issueType == issue, onClick = { issueType = issue }, label = { Text(issue, fontSize = 10.sp) })
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    issues.takeLast(2).forEach { issue ->
                        FilterChip(selected = issueType == issue, onClick = { issueType = issue }, label = { Text(issue, fontSize = 10.sp) })
                    }
                }
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("সমস্যার বিস্তারিত লিখুন") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            }
        },
        confirmButton = {
            Button(onClick = { if (description.isNotBlank()) onSave(issueType, description) }, colors = ButtonDefaults.buttonColors(containerColor = Teal600)) { Text("অভিযোগ জমা দিন") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}
