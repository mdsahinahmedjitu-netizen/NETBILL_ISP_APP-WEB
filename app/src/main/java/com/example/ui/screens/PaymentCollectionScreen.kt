package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import com.example.ui.theme.*
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.CustomerEntity
import com.example.data.entity.PaymentCollectionEntity
import com.example.util.AppUtils
import com.example.localization.AppTranslation
import com.example.ui.components.ReadonlyDateField
import com.example.ui.theme.BkashPink
import java.text.SimpleDateFormat
import java.util.*
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NagadOrange
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.RocketViolet
import com.example.ui.theme.Teal600
import com.example.viewmodel.MainViewModel

@Composable
fun PaymentCollectionScreen(viewModel: MainViewModel) {
    val payments by viewModel.paymentsList.collectAsState()
    val customers by viewModel.customersList.collectAsState()
    val selectedReceipt by viewModel.selectedReceipt.collectAsState()
    val currency = AppTranslation("currency_symbol")

    var showAddPaymentDialog by remember { mutableStateOf(false) }
    var showOnlineGatewayDialog by remember { mutableStateOf(false) }
    var showReconciliationDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExtendedFloatingActionButton(
                    text = { Text("bKash / Nagad Direct", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.Payment, contentDescription = null) },
                    onClick = { showOnlineGatewayDialog = true },
                    containerColor = BkashPink,
                    contentColor = Color.White
                )

                FloatingActionButton(
                    onClick = { showAddPaymentDialog = true },
                    containerColor = Teal600,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Record Cash Payment")
                }
            }
        },
        containerColor = SleekBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = AppTranslation("payment_collection"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )

                Button(
                    onClick = { showReconciliationDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Teal600,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Verify Gateway Trx", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (payments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No payment collections recorded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(payments, key = { it.id }) { pymt ->
                        PaymentCard(
                            payment = pymt,
                            currency = currency,
                            onClick = { viewModel.setSelectedReceipt(pymt) }
                        )
                    }
                }
            }
        }
    }

    if (showAddPaymentDialog) {
        RecordPaymentDialog(
            customers = customers,
            viewModel = viewModel,
            onDismiss = { showAddPaymentDialog = false },
            onSave = { customerId, amount, method, txnId, remarks, date, collector, month ->
                viewModel.collectPayment(
                    customerId = customerId,
                    amount = amount,
                    method = method,
                    trxId = txnId,
                    remarks = remarks,
                    date = date,
                    collectorName = collector,
                    billingMonth = month
                )
                showAddPaymentDialog = false
            }
        )
    }

    if (showOnlineGatewayDialog) {
        OnlineGatewayCheckoutDialog(
            viewModel = viewModel,
            onDismiss = { showOnlineGatewayDialog = false }
        )
    }

    if (showReconciliationDialog) {
        GatewayReconciliationDialog(
            viewModel = viewModel,
            onDismiss = { showReconciliationDialog = false }
        )
    }

    // Modal Receipt Dialog when selectedReceipt != null
    selectedReceipt?.let { receipt ->
        InvoiceReceiptDialog(
            payment = receipt,
            viewModel = viewModel,
            onDismiss = { viewModel.setSelectedReceipt(null) }
        )
    }
}

@Composable
fun PaymentCard(payment: PaymentCollectionEntity, currency: String, onClick: () -> Unit) {
    val methodColor = when (payment.paymentMethod) {
        "bKash" -> BkashPink
        "Nagad" -> NagadOrange
        "Rocket" -> RocketViolet
        "Cash" -> EmeraldSuccess
        else -> ElectricBlue
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SleekCard),
        border = BorderStroke(1.dp, SleekBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(methodColor)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(payment.paymentMethod, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(payment.receiptNo, fontWeight = FontWeight.Bold, color = Teal600, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(payment.customerName, fontWeight = FontWeight.ExtraBold, color = Slate900, fontSize = 18.sp)
                Text(
                    text = "Month: ${payment.billingMonth} • ${AppUtils.formatDateForDisplay(payment.paymentDate)}", 
                    color = Slate600, 
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("Txn: ${payment.transactionId.ifEmpty { "CASH-ENTRY" }} • Col: ${payment.collectorName}", color = Teal600, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$currency ${payment.amount.toInt()}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldSuccess
                )

                Text(
                    text = "Receipt >",
                    color = Teal600,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentDialog(
    customers: List<CustomerEntity>,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onSave: (String, Double, String, String, String, String, String, String) -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val staffList by viewModel.staffList.collectAsState()
    
    var selectedCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerSearchQuery by remember { mutableStateOf("") }
    var expandedCustomerDropdown by remember { mutableStateOf(false) }

    val filteredList = remember(customerSearchQuery, customers) {
        if (customerSearchQuery.isEmpty()) emptyList()
        else customers.filter { 
            it.name.contains(customerSearchQuery, ignoreCase = true) || 
            it.customerCode.contains(customerSearchQuery, ignoreCase = true) ||
            it.pppoeUsername.contains(customerSearchQuery, ignoreCase = true)
        }.take(10)
    }

    var amount by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("Cash") }
    var txnId by remember { mutableStateOf("") }
    var remarks by remember { mutableStateOf("") }
    var paymentDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }
    
    val currentMonth = SimpleDateFormat("MMMM yyyy", Locale.US).format(Date())
    var billingMonth by remember { mutableStateOf(currentMonth) }
    var expandedMonthDropdown by remember { mutableStateOf(false) }
    
    val months = remember {
        val list = mutableListOf<String>()
        val cal = Calendar.getInstance()
        repeat(6) {
            list.add(SimpleDateFormat("MMMM yyyy", Locale.US).format(cal.time))
            cal.add(Calendar.MONTH, -1)
        }
        list
    }

    var selectedCollector by remember { 
        mutableStateOf(currentUser?.name ?: "Admin") 
    }
    var expandedCollectorDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Collection (Web Style)", color = Slate900, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.height(550.dp) // Large box like web
            ) {
                // Collector Selector (Session Lock for Staff)
                val isAdmin = currentUser?.role?.contains("Admin", ignoreCase = true) == true
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Collected By:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Teal600)
                    ExposedDropdownMenuBox(
                        expanded = expandedCollectorDropdown && isAdmin,
                        onExpandedChange = { if (isAdmin) expandedCollectorDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = selectedCollector,
                            onValueChange = {},
                            readOnly = true,
                            enabled = isAdmin,
                            trailingIcon = { if (isAdmin) ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCollectorDropdown) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        if (isAdmin) {
                            ExposedDropdownMenu(
                                expanded = expandedCollectorDropdown,
                                onDismissRequest = { expandedCollectorDropdown = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(currentUser?.name ?: "Admin") },
                                    onClick = { selectedCollector = currentUser?.name ?: "Admin"; expandedCollectorDropdown = false }
                                )
                                staffList.forEach { staff ->
                                    DropdownMenuItem(
                                        text = { Text(staff.name) },
                                        onClick = { selectedCollector = staff.name; expandedCollectorDropdown = false }
                                    )
                                }
                            }
                        }
                    }
                }

                // Searchable Customer Selector
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Find Subscriber:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate600)
                    ExposedDropdownMenuBox(
                        expanded = expandedCustomerDropdown,
                        onExpandedChange = { expandedCustomerDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = if (selectedCustomer != null && !expandedCustomerDropdown) "${selectedCustomer?.name} (${selectedCustomer?.customerCode})" else customerSearchQuery,
                            onValueChange = { 
                                customerSearchQuery = it
                                expandedCustomerDropdown = true 
                            },
                            placeholder = { Text("Search Name/ID/PPPoE") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCustomerDropdown) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )

                        if (filteredList.isNotEmpty()) {
                            ExposedDropdownMenu(
                                expanded = expandedCustomerDropdown,
                                onDismissRequest = { expandedCustomerDropdown = false }
                            ) {
                                filteredList.forEach { cust ->
                                    DropdownMenuItem(
                                        text = { 
                                            Column {
                                                Text(cust.name, fontWeight = FontWeight.Bold)
                                                Text("ID: ${cust.customerCode} • Due: ৳${cust.currentDue.toInt()}", fontSize = 11.sp)
                                            }
                                        },
                                        onClick = {
                                            selectedCustomer = cust
                                            amount = Math.floor(cust.currentDue).toInt().toString()
                                            customerSearchQuery = ""
                                            expandedCustomerDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Billing Month
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Billing Month:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate600)
                        ExposedDropdownMenuBox(
                            expanded = expandedMonthDropdown,
                            onExpandedChange = { expandedMonthDropdown = it }
                        ) {
                            OutlinedTextField(
                                value = billingMonth,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMonthDropdown) },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedMonthDropdown,
                                onDismissRequest = { expandedMonthDropdown = false }
                            ) {
                                months.forEach { m ->
                                    DropdownMenuItem(text = { Text(m) }, onClick = { billingMonth = m; expandedMonthDropdown = false })
                                }
                            }
                        }
                    }
                    // Method
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Method:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate600)
                        var expandedMethod by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expandedMethod,
                            onExpandedChange = { expandedMethod = it }
                        ) {
                            OutlinedTextField(
                                value = method,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMethod) },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedMethod,
                                onDismissRequest = { expandedMethod = false }
                            ) {
                                listOf("Cash", "bKash", "Nagad", "Rocket", "Bank").forEach { m ->
                                    DropdownMenuItem(text = { Text(m) }, onClick = { method = m; expandedMethod = false })
                                }
                            }
                        }
                    }
                }

                // Amount (BIG LIKE WEB)
                Column(
                    modifier = Modifier.fillMaxWidth().background(Slate100, RoundedCornerShape(24.dp)).padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("ENTER COLLECTION AMOUNT (৳)", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Slate600)
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        textStyle = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold, 
                            color = Teal600,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        ),
                        placeholder = { Text("0.00") },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedCustomer?.let { cust ->
                        onSave(cust.id, amount.toDoubleOrNull() ?: 0.0, method, txnId, remarks, paymentDate, selectedCollector, billingMonth)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal600)
            ) {
                Text("COMMIT PAYMENT (SYNC)", fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", fontWeight = FontWeight.Bold, color = Slate400) }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(40.dp)
    )
}
