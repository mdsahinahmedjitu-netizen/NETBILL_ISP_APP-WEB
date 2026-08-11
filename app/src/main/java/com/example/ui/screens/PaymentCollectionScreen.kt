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
import com.example.localization.AppTranslation
import com.example.ui.theme.BkashPink
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
            onDismiss = { showAddPaymentDialog = false },
            onSave = { customerId, amount, method, txnId, remarks ->
                viewModel.collectPayment(customerId, amount, method, txnId, remarks)
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

                Text(payment.customerName, fontWeight = FontWeight.Bold, color = Slate900, fontSize = 16.sp)
                Text("Txn: ${payment.transactionId.ifEmpty { "Cash Ref" }} • ${payment.paymentDate}", color = Slate600, fontSize = 11.sp)
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
    onDismiss: () -> Unit,
    onSave: (String, Double, String, String, String) -> Unit
) {
    var selectedCustomer by remember { mutableStateOf(customers.firstOrNull()) }
    var expandedCustomerDropdown by remember { mutableStateOf(false) }

    var amount by remember { mutableStateOf(selectedCustomer?.monthlyBill?.toString() ?: "800") }
    var method by remember { mutableStateOf("bKash") }
    var txnId by remember { mutableStateOf("BK${(100000..999999).random()}X") }
    var remarks by remember { mutableStateOf("Monthly internet bill") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Payment Collection", color = Slate900, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Customer Selector
                ExposedDropdownMenuBox(
                    expanded = expandedCustomerDropdown,
                    onExpandedChange = { expandedCustomerDropdown = !expandedCustomerDropdown }
                ) {
                    OutlinedTextField(
                        value = selectedCustomer?.let { "${it.name} (${it.customerCode})" } ?: "Select Customer",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCustomerDropdown) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedCustomerDropdown,
                        onDismissRequest = { expandedCustomerDropdown = false }
                    ) {
                        customers.forEach { cust ->
                            DropdownMenuItem(
                                text = { Text("${cust.name} (${cust.customerCode}) - Due: ৳${cust.currentDue.toInt()}") },
                                onClick = {
                                    selectedCustomer = cust
                                    amount = if (cust.currentDue > 0) cust.currentDue.toString() else cust.monthlyBill.toString()
                                    expandedCustomerDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (৳ BDT)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Method Selector Chips
                Text("Payment Method Bangladesh:", fontSize = 12.sp, color = Slate700, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("bKash", "Nagad", "Rocket", "Cash").forEach { m ->
                        val isSelected = method == m
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) ElectricBlue else Slate100)
                                .clickable { method = m }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(m, color = if (isSelected) Color.White else Slate700, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                OutlinedTextField(
                    value = txnId,
                    onValueChange = { txnId = it },
                    label = { Text("Transaction ID / Ref") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Remarks") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedCustomer?.let { cust ->
                        onSave(cust.id, amount.toDoubleOrNull() ?: 0.0, method, txnId, remarks)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BkashPink)
            ) {
                Text("Save & Print Receipt", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Slate600) }
        },
        containerColor = SleekCard
    )
}
