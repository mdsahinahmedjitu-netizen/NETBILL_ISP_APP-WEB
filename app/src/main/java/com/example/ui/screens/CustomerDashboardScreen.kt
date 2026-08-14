package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.foundation.lazy.items
import com.example.viewmodel.MainViewModel
import com.example.ui.theme.*
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDashboardScreen(viewModel: MainViewModel) {
    val customer by viewModel.currentCustomer.collectAsState()
    val invoices by viewModel.invoicesList.collectAsState()
    val payments by viewModel.paymentsList.collectAsState()
    val tickets by viewModel.supportTickets.collectAsState()
    val settings by viewModel.settingsState.collectAsState()
    val currency = "৳"

    if (customer == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val cust = customer!!
    var showSupportDialog by remember { mutableStateOf(false) }
    var showPayDialog by remember { mutableStateOf(false) }

    val myPayments = remember(payments, cust.id) {
        payments.filter { it.customerId == cust.id }.sortedByDescending { it.paymentDate }
    }
    
    val myTickets = remember(tickets, cust.id) {
        tickets.filter { it.customerId == cust.id }
    }
    
    // Find latest invoice for this customer to show detailed breakdown
    val latestInvoice = remember(invoices, cust.id) {
        invoices.filter { it.customerId == cust.id }
            .sortedByDescending { it.generatedDate }
            .firstOrNull()
    }

    val currentMonthBill = latestInvoice?.billAmount ?: cust.monthlyBill
    val previousDue = latestInvoice?.previousDue ?: (cust.currentDue - currentMonthBill).coerceAtLeast(0.0)
    val totalOutstanding = cust.currentDue
    
    val paidUpTo = remember(invoices, cust.id) {
        invoices.filter { it.customerId == cust.id && it.status == "Paid" }
            .sortedByDescending { it.generatedDate }
            .firstOrNull()?.billingMonthYear ?: "No Records"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Internet Portal", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.logout() }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout", tint = CoralWarning)
                    }
                }
            )
        },
        containerColor = SleekBg
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Welcome Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Teal600)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(Teal600, Teal700)))
                            .padding(24.dp)
                    ) {
                        Column {
                            Text("Welcome Back,", color = Teal100, fontSize = 14.sp)
                            Text(cust.name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "Customer ID: ${cust.customerCode}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Connection Status & Expiry Row
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatusCard(
                        label = "Connection",
                        value = cust.status,
                        icon = Icons.Default.Wifi,
                        color = if (cust.status == "Active") EmeraldSuccess else CoralWarning,
                        modifier = Modifier.weight(1f)
                    )
                    StatusCard(
                        label = "Expires On",
                        value = cust.expireDate.ifEmpty { "Not Set" },
                        icon = Icons.Default.Timer,
                        color = ElectricBlue,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Billing Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekCard),
                    border = BorderStroke(1.dp, SleekBorder)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("বিল ও বকেয়ার হিসাব (Billing Summary)", fontWeight = FontWeight.Bold, color = Slate900, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Current Month Bill
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("চলতি মাসের বিল (Current Month)", fontSize = 13.sp, color = Slate600)
                            Text("$currency ${currentMonthBill.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate900)
                        }
                        
                        // Previous Due (if any)
                        if (previousDue > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("পূর্বের বকেয়া (Previous Due)", fontSize = 13.sp, color = Slate600)
                                Text("$currency ${previousDue.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CoralWarning)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = SleekBorder)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Total Outstanding
                        Row(
                            modifier = Modifier.fillMaxWidth(), 
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("মোট প্রদেয় (Total Outstanding)", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Slate900)
                            Text(
                                text = "$currency ${totalOutstanding.toInt()}",
                                fontSize = 22.sp, 
                                fontWeight = FontWeight.Black, 
                                color = if (totalOutstanding > 0) CoralWarning else EmeraldSuccess
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = SleekBorder)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Advance Balance & Paid Up To
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("অগ্রীম জমা (Advance)", fontSize = 12.sp, color = Slate600)
                                Text("$currency ${cust.advanceBalance.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("বিল পরিশোধ (Paid Up To)", fontSize = 12.sp, color = Slate600)
                                Text(paidUpTo, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Teal600)
                            }
                        }

                        if (cust.advanceBalance > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = EmeraldSuccess.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "আপনার অ্যাকাউন্টে $currency ${cust.advanceBalance.toInt()} টাকা অগ্রিম জমা আছে।",
                                    modifier = Modifier.padding(10.dp),
                                    color = EmeraldSuccess,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Package Details
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekCard),
                    border = BorderStroke(1.dp, SleekBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(CyanAccent.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = CyanAccent)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Current Package", fontSize = 12.sp, color = Slate600)
                            Text(cust.packageName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Slate900)
                        }
                    }
                }
            }

            // Pay Bill Button
            item {
                Button(
                    onClick = { 
                        if (totalOutstanding > 0) {
                            showPayDialog = true
                        } else {
                            viewModel.showToast("No outstanding balance to pay.")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BkashPink)
                ) {
                    Icon(Icons.Default.Payment, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pay Bill Now", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            // Support Ticket
            item {
                OutlinedButton(
                    onClick = { showSupportDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Teal600)
                ) {
                    Icon(Icons.Default.ReportProblem, contentDescription = null, tint = Teal600)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Report a Problem / অভিযোগ দিন", fontWeight = FontWeight.Bold, color = Teal600)
                }
            }

            // Payment History Section
            if (myPayments.isNotEmpty()) {
                item {
                    Text("Payment History / বিল প্রদানের ইতিহাস", fontWeight = FontWeight.Bold, color = Slate900, fontSize = 16.sp)
                }
                items(myPayments) { pymt ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SleekCard),
                        border = BorderStroke(1.dp, SleekBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(pymt.receiptNo, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("${pymt.paymentDate} • ${pymt.paymentMethod}", fontSize = 11.sp, color = Slate600)
                            }
                            Text("$currency ${pymt.amount.toInt()}", fontWeight = FontWeight.Bold, color = EmeraldSuccess, fontSize = 15.sp)
                        }
                    }
                }
            }

            // Active Support Tickets Section
            if (myTickets.isNotEmpty()) {
                item {
                    Text("My Support Tickets / অভিযোগের অবস্থা", fontWeight = FontWeight.Bold, color = Slate900, fontSize = 16.sp)
                }
                items(myTickets) { ticket ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SleekCard),
                        border = BorderStroke(1.dp, SleekBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(ticket.issueType, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Box(
                                    modifier = Modifier.clip(RoundedCornerShape(4.dp))
                                        .background(if(ticket.status == "Resolved") EmeraldSuccess.copy(alpha = 0.1f) else CoralWarning.copy(alpha = 0.1f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(ticket.status, color = if(ticket.status == "Resolved") EmeraldSuccess else CoralWarning, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(ticket.description, fontSize = 11.sp, color = Slate600)
                            Text("Posted: ${ticket.createdAt}", fontSize = 10.sp, color = Slate500)
                        }
                    }
                }
            }
        }
    }

    if (showSupportDialog) {
        CustomerTicketDialog(
            customer = cust,
            onDismiss = { showSupportDialog = false },
            onSave = { type, desc ->
                viewModel.createSupportTicket(cust, type, desc)
                showSupportDialog = false
            }
        )
    }

    if (showPayDialog) {
        CustomerManualPayDialog(
            customer = cust,
            settings = settings,
            onDismiss = { showPayDialog = false },
            onSave = { method, trx ->
                viewModel.submitPaymentRequest(cust.id, totalOutstanding, method, trx) { success, msg ->
                    viewModel.showToast(msg)
                    if (success) showPayDialog = false
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerManualPayDialog(
    customer: CustomerEntity,
    settings: com.example.data.entity.ISPSettingsEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var selectedMethod by remember { mutableStateOf("bKash") }
    var trxId by remember { mutableStateOf("") }
    val paymentNumber = if (selectedMethod == "bKash") settings?.personalBkashNo ?: "017XXXXXXXX" else settings?.personalNagadNo ?: "018XXXXXXXX"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("বিল পরিশোধ নির্দেশিকা", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Method Selection
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("bKash", "Nagad").forEach { m ->
                        val isSel = selectedMethod == m
                        val color = if (m == "bKash") BkashPink else NagadOrange
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) color else Slate100)
                                .clickable { selectedMethod = m }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(m, color = if (isSel) Color.White else Slate700, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Teal50),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("১. আপনার $selectedMethod অ্যাপ থেকে নিচের নাম্বারে Send Money করুন।", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate800)
                        Text(paymentNumber, fontSize = 22.sp, fontWeight = FontWeight.Black, color = Teal700, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        Text("পরিমাণ: ৳${customer.currentDue.toInt()}", fontSize = 11.sp, color = Slate600)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("২. টাকা পাঠানোর পর ট্রানজেকশন আইডি (TrxID) দিন:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate800)
                    OutlinedTextField(
                        value = trxId,
                        onValueChange = { trxId = it },
                        placeholder = { Text("E.g. BK89201472X") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (trxId.length >= 8) onSave(selectedMethod, trxId) },
                colors = ButtonDefaults.buttonColors(containerColor = Teal600)
            ) {
                Text("পেমেন্ট সাবমিট করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerTicketDialog(
    customer: CustomerEntity,
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
                        FilterChip(
                            selected = issueType == issue,
                            onClick = { issueType = issue },
                            label = { Text(issue, fontSize = 10.sp) }
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    issues.takeLast(2).forEach { issue ->
                        FilterChip(
                            selected = issueType == issue,
                            onClick = { issueType = issue },
                            label = { Text(issue, fontSize = 10.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("সমস্যার বিস্তারিত লিখুন") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (description.isNotBlank()) onSave(issueType, description) },
                colors = ButtonDefaults.buttonColors(containerColor = Teal600)
            ) {
                Text("অভিযোগ জমা দিন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@Composable
fun StatusCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SleekCard),
        border = BorderStroke(1.dp, SleekBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, fontSize = 10.sp, color = Slate600, fontWeight = FontWeight.Bold)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate900)
        }
    }
}
