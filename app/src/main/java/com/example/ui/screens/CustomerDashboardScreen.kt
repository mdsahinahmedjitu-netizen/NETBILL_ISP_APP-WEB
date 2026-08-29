package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import java.util.Locale
import com.example.data.entity.CustomerEntity
import com.example.viewmodel.MainViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDashboardScreen(viewModel: MainViewModel) {
    val customer by viewModel.currentCustomer.collectAsState()
    val payments by viewModel.paymentsList.collectAsState()
    val invoices by viewModel.invoicesList.collectAsState()
    val settings by viewModel.settingsState.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
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
    
    val myPayments = remember(payments, cust.id) {
        payments.filter { it.customerId == cust.id }.sortedByDescending { it.paymentDate }
    }
    
    val myInvoices = remember(invoices, cust.id) {
        invoices.filter { it.customerId == cust.id }.sortedByDescending { it.billingMonthYear }
    }

    Scaffold(
        containerColor = if (isDarkMode) Slate900 else Color(0xFFF8FAFC)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // WEB-STYLE BOLD HEADER
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(72.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = Teal600,
                            shadowElevation = 8.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Bolt, null, tint = Color.White, modifier = Modifier.size(40.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(20.dp))
                        Column {
                            Text("NETBILL ISP", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Teal600, letterSpacing = 4.sp)
                            Text(cust.name.uppercase(), fontSize = 32.sp, fontWeight = FontWeight.Black, color = if (isDarkMode) Color.White else Slate900, letterSpacing = (-2).sp, lineHeight = 32.sp)
                        }
                    }
                    IconButton(
                        onClick = { viewModel.logout() },
                        modifier = Modifier.size(56.dp).background(CoralWarning.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, null, tint = CoralWarning, modifier = Modifier.size(24.dp))
                    }
                }
            }

            // HUGE HERO DUE CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(56.dp),
                    colors = CardDefaults.cardColors(containerColor = Teal600),
                    elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Teal600, Color(0xFF0F172A))))
                            .padding(horizontal = 32.dp, vertical = 48.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text("TOTAL OUTSTANDING DUE", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.6f), letterSpacing = 5.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("$currency ${String.format(Locale.US, "%,.0f", cust.currentDue)}", fontSize = 84.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = (-4).sp, lineHeight = 80.sp)
                            
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("MONTHLY RENT", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.5f), letterSpacing = 2.sp)
                                    Text("$currency ${cust.monthlyBill.toInt()}", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
                                }
                                Box(modifier = Modifier.width(2.dp).height(40.dp).background(Color.White.copy(alpha = 0.1f)))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("EXPIRE DATE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.5f), letterSpacing = 2.sp)
                                    Text(cust.expireDate?.uppercase() ?: "NOT SET", fontSize = 22.sp, fontWeight = FontWeight.Black, color = if(cust.status == "Active") EmeraldSuccess else IspRose)
                                }
                            }

                            Spacer(modifier = Modifier.height(48.dp))

                            Button(
                                onClick = { showPayDialog = true },
                                modifier = Modifier.fillMaxWidth().height(88.dp),
                                shape = RoundedCornerShape(32.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF0F172A)),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp)
                            ) {
                                Text("PAY BILL NOW", fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = 3.sp)
                            }
                        }
                    }
                }
            }

            // NEO-BRUTALIST GRID
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    InfoSmallCard(label = "PLAN SPEED", value = cust.packageName, icon = Icons.Default.Wifi, color = ElectricBlue, modifier = Modifier.weight(1f), isDarkMode = isDarkMode)
                    InfoSmallCard(label = "USER STATUS", value = cust.status.uppercase(), icon = Icons.Default.Shield, color = if(cust.status == "Active") EmeraldSuccess else IspRose, modifier = Modifier.weight(1f), isDarkMode = isDarkMode)
                }
            }

            // BILLING TRACKER
            if (myInvoices.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("BILLING STATUS MONITOR", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Slate500, letterSpacing = 5.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            myInvoices.forEach { inv ->
                                Card(
                                    modifier = Modifier.width(160.dp),
                                    shape = RoundedCornerShape(32.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (inv.status == "Paid") EmeraldSuccess.copy(alpha = 0.08f) else IspRose.copy(alpha = 0.08f)
                                    ),
                                    border = BorderStroke(2.dp, if (inv.status == "Paid") EmeraldSuccess.copy(alpha = 0.15f) else IspRose.copy(alpha = 0.15f))
                                ) {
                                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.Start) {
                                        Text(inv.billingMonthYear.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Black, color = Slate500, letterSpacing = 2.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(inv.status.uppercase(), fontSize = 16.sp, fontWeight = FontWeight.Black, color = if (inv.status == "Paid") EmeraldSuccess else IspRose)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("$currency ${inv.totalPayable.toInt()}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = if (isDarkMode) Color.White else Slate900, letterSpacing = (-1).sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SUPPORT & HELP
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("CUSTOMER SUPPORT", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Slate500, letterSpacing = 5.sp)
                    Card(
                        onClick = { showSupportDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(40.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Slate800 else Color.White),
                        border = BorderStroke(2.dp, if (isDarkMode) Slate700 else Color(0xFFF1F5F9)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(64.dp).background(IspAmber.copy(alpha = 0.1f), RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.Help, null, tint = IspAmber, modifier = Modifier.size(32.dp))
                            }
                            Spacer(modifier = Modifier.width(20.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("REPORT AN ISSUE", fontSize = 18.sp, fontWeight = FontWeight.Black, color = if (isDarkMode) Color.White else Slate900, letterSpacing = (-1).sp)
                                Text("অভিযোগ জানাতে এখানে ক্লিক করুন", fontSize = 12.sp, color = Slate500, fontWeight = FontWeight.Bold)
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Slate300, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }

            // TRANSACTION HISTORY
            if (myPayments.isNotEmpty()) {
                item {
                    Text("RECENT TRANSACTIONS", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Slate500, letterSpacing = 5.sp)
                }
                items(myPayments) { pymt ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(32.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Slate800 else Color.White),
                        border = BorderStroke(2.dp, if (isDarkMode) Slate700 else Color(0xFFF8FAFC)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(modifier = Modifier.padding(24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(56.dp).background(EmeraldSuccess.copy(alpha = 0.08f), RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Receipt, null, tint = EmeraldSuccess, modifier = Modifier.size(28.dp))
                                }
                                Spacer(modifier = Modifier.width(20.dp))
                                Column {
                                    Text("REF: ${pymt.receiptNo}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = if (isDarkMode) Color.White else Slate900, letterSpacing = (-0.5).sp)
                                    Text("${pymt.paymentDate.uppercase()} • ${pymt.paymentMethod.uppercase()}", fontSize = 10.sp, color = Slate500, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                }
                            }
                            Text("$currency ${pymt.amount.toInt()}", fontSize = 28.sp, fontWeight = FontWeight.Black, color = EmeraldSuccess, letterSpacing = (-1.5).sp)
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(60.dp)) }
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
fun InfoSmallCard(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier, isDarkMode: Boolean) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(40.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Slate800 else Color.White),
        border = BorderStroke(2.dp, if (isDarkMode) Slate700 else Color(0xFFF1F5F9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Box(modifier = Modifier.size(56.dp).background(color.copy(alpha = 0.08f), RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(label.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Black, color = Slate500, letterSpacing = 2.sp)
            Text(value.uppercase(), fontSize = 18.sp, fontWeight = FontWeight.Black, color = if (isDarkMode) Color.White else Slate900, maxLines = 1, letterSpacing = (-1).sp)
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

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(24.dp).fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(48.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(32.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Text("BILL PAYMENT GUIDE", style = MaterialTheme.typography.headlineSmall, color = Slate900)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    listOf("bKash", "Nagad").forEach { m ->
                        val isSel = selectedMethod == m
                        val color = if (m == "bKash") BkashPink else NagadOrange
                        Surface(
                            onClick = { selectedMethod = m },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSel) color else Slate100,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                                Text(m, color = if (isSel) Color.White else Slate700, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            }
                        }
                    }
                }
                
                Card(colors = CardDefaults.cardColors(containerColor = Teal600.copy(alpha = 0.05f)), shape = RoundedCornerShape(24.dp), border = BorderStroke(2.dp, Teal600.copy(alpha = 0.1f))) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("১. আপনার $selectedMethod অ্যাপ থেকে Send Money করুন:", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Slate600, letterSpacing = 1.sp)
                        Text(paymentNumber, fontSize = 28.sp, fontWeight = FontWeight.Black, color = Teal600, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        Text("প্রদেয় পরিমাণ: ৳${String.format(Locale.US, "%,.0f", currentDue)}", fontSize = 12.sp, color = Slate800, fontWeight = FontWeight.Black)
                    }
                }
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("২. ট্রানজেকশন আইডি (TrxID) এখানে লিখুন:", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Slate600, letterSpacing = 1.sp)
                    OutlinedTextField(
                        value = trxId, 
                        onValueChange = { trxId = it }, 
                        placeholder = { Text("E.G. BK89201472X", color = Slate900.copy(alpha = 0.3f)) }, 
                        modifier = Modifier.fillMaxWidth(), 
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(56.dp)) { Text("CANCEL", fontWeight = FontWeight.Black) }
                    Button(
                        onClick = { if (trxId.length >= 8) onSave(selectedMethod, trxId) }, 
                        colors = ButtonDefaults.buttonColors(containerColor = Teal600),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1.5f).height(56.dp)
                    ) { Text("SUBMIT PAYMENT", fontWeight = FontWeight.Black) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerTicketDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var issueType by remember { mutableStateOf("Internet Slow") }
    var description by remember { mutableStateOf("") }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(24.dp).fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(48.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(32.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Text("NEW COMPLAINT", style = MaterialTheme.typography.headlineSmall, color = Slate900)
                
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("SELECT ISSUE TYPE:", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Slate500, letterSpacing = 2.sp)
                    val issues = listOf("Internet Slow", "No Internet / LOS", "Router Config", "Billing Issue")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        issues.forEach { issue ->
                            FilterChip(
                                selected = issueType == issue, 
                                onClick = { issueType = issue }, 
                                label = { Text(issue, fontSize = 11.sp, fontWeight = FontWeight.Black) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Teal600,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description, 
                    onValueChange = { description = it }, 
                    label = { Text("DESCRIBE YOUR PROBLEM", fontWeight = FontWeight.Black, fontSize = 11.sp) }, 
                    modifier = Modifier.fillMaxWidth(), 
                    minLines = 4,
                    shape = RoundedCornerShape(20.dp)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(56.dp)) { Text("CANCEL", fontWeight = FontWeight.Black) }
                    Button(
                        onClick = { if (description.isNotBlank()) onSave(issueType, description) }, 
                        colors = ButtonDefaults.buttonColors(containerColor = Teal600),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1.5f).height(56.dp)
                    ) { Text("SUBMIT ISSUE", fontWeight = FontWeight.Black) }
                }
            }
        }
    }
}
