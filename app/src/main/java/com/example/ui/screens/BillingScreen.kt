package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.InvoiceEntity
import com.example.localization.AppTranslation
import com.example.ui.components.CarryForwardDueCard
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.BkashPink
import com.example.ui.theme.CoralWarning
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NagadOrange
import com.example.ui.theme.SleekBg
import com.example.ui.theme.SleekBorder
import com.example.ui.theme.SleekCard
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Teal100
import com.example.ui.theme.Teal50
import com.example.ui.theme.Teal600
import com.example.ui.theme.Teal700
import com.example.viewmodel.MainViewModel

@Composable
fun BillingScreen(viewModel: MainViewModel) {
    val invoices by viewModel.invoicesList.collectAsState()
    val permissions by viewModel.currentPermissions.collectAsState()
    val context = LocalContext.current
    val currency = AppTranslation("currency_symbol")

    var selectedTab by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var showGenerateBillsDialog by remember { mutableStateOf(value = false) }
    var showOnlineGatewayDialog by remember { mutableStateOf(value = false) }
    var showReconciliationDialog by remember { mutableStateOf(value = false) }

    var selectedInvoiceForPayment by remember { mutableStateOf<InvoiceEntity?>(null) }
    var selectedInvoiceForReceipt by remember { mutableStateOf<InvoiceEntity?>(null) }

    // Summary calculations
    val totalBilled = invoices.sumOf { it.totalPayable }
    val totalCollected = invoices.sumOf { it.totalPayable - it.dueAmount }
    val totalOutstanding = invoices.sumOf { it.dueAmount }
    val unpaidCount = invoices.count { (it.status == "Unpaid") || (it.status == "Partial") }

    val filteredInvoices = invoices.filter { inv ->
        val matchTab = when (selectedTab) {
            "Unpaid" -> (inv.status == "Unpaid") || (inv.status == "Partial")
            "Paid" -> inv.status == "Paid"
            else -> true
        }
        val matchQuery = searchQuery.isEmpty() ||
                inv.customerName.contains(searchQuery, ignoreCase = true) ||
                inv.customerCode.contains(searchQuery, ignoreCase = true) ||
                inv.invoiceNo.contains(searchQuery, ignoreCase = true) ||
                inv.billingMonthYear.contains(searchQuery, ignoreCase = true)

        matchTab && matchQuery
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SleekBg)
            .padding(16.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = AppTranslation("billing_management"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Slate900,
                )
                Text(
                    text = "Automated Monthly Bills & Invoicing Engine",
                    fontSize = 11.sp,
                    color = Slate500
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (permissions.canSeeRevenue) {
                    OutlinedButton(
                        onClick = { showReconciliationDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate800),
                        border = BorderStroke(1.dp, Slate200),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp), tint = Teal600)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Verify Trx", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (permissions.canCollect) {
                    Button(
                        onClick = { showOnlineGatewayDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BkashPink,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("bKash/Nagad Pay", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (permissions.canBulkBill) {
                    Button(
                        onClick = { showGenerateBillsDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Teal600,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Auto Bills", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Billing Summary Metrics Row
        if (permissions.canSeeRevenue) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BillingSummaryCard(
                    label = "TOTAL BILLED",
                    value = "$currency ${String.format(java.util.Locale.US, "%,.0f", totalBilled)}",
                    color = Teal600,
                    modifier = Modifier.weight(1f)
                )
                BillingSummaryCard(
                    label = "COLLECTED",
                    value = "$currency ${String.format(java.util.Locale.US, "%,.0f", totalCollected)}",
                    color = EmeraldSuccess,
                    modifier = Modifier.weight(1f)
                )
                BillingSummaryCard(
                    label = "OUTSTANDING",
                    value = "$currency ${String.format(java.util.Locale.US, "%,.0f", totalOutstanding)}",
                    color = CoralWarning,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search & Filter Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by Invoice #, Customer, Code or Month...", fontSize = 12.sp, color = Slate400) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate500) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Teal600,
                unfocusedBorderColor = Slate200,
                focusedContainerColor = SleekCard,
                unfocusedContainerColor = SleekCard,
                focusedTextColor = Slate900,
                unfocusedTextColor = Slate900
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Tabs
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf("All", "Unpaid", "Paid", "Carry Forward Due").forEach { tab ->
                val isSelected = selectedTab == tab
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedTab = tab },
                    label = {
                        Text(
                            text = when (tab) {
                                "Unpaid" -> if (unpaidCount > 0) "$tab ($unpaidCount)" else tab
                                "Carry Forward Due" -> "Carry Forward (FIFO)"
                                else -> tab
                            },
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Teal600,
                        selectedLabelColor = Color.White,
                        containerColor = SleekCard,
                        labelColor = Slate600
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = Slate200,
                        selectedBorderColor = Teal600
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Content Area
        if (selectedTab == "Carry Forward Due") {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    CarryForwardDueCard(
                        viewModel = viewModel
                    ) { customer ->
                        val inv = invoices.find { (it.customerId == customer.id) && (it.dueAmount > 0) }
                        if (inv != null) {
                            selectedInvoiceForPayment = inv
                        } else {
                            viewModel.showToast("Selected customer has no unpaid invoices.")
                        }
                    }
                }
            }
        } else if (filteredInvoices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Slate400
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No invoices found for selected criteria",
                        color = Slate500,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredInvoices, key = { it.id }) { invoice ->
                    InvoiceCard(
                        invoice = invoice,
                        currency = currency,
                        permissions = permissions,
                        onCollectPayment = { selectedInvoiceForPayment = invoice },
                        onPrintInvoice = { selectedInvoiceForReceipt = invoice }
                    ) {
                        Toast.makeText(context, "SMS Bill Reminder sent to ${invoice.customerName} (${invoice.invoiceNo})", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Auto-Generate Monthly Bills Dialog
    if (showGenerateBillsDialog) {
        GenerateBillsDialog(
            onDismiss = { showGenerateBillsDialog = false }
        ) { month, _ ->
            viewModel.generateBillsForMonth(month)
            showGenerateBillsDialog = false
        }
    }

    // Online bKash & Nagad Payment Gateway Dialog
    if (showOnlineGatewayDialog) {
        OnlineGatewayCheckoutDialog(
            viewModel = viewModel,
            onDismiss = { showOnlineGatewayDialog = false }
        )
    }

    // Gateway Transaction Verification & Auto-Reconciliation Dialog
    if (showReconciliationDialog) {
        GatewayReconciliationDialog(
            viewModel = viewModel,
            onDismiss = { showReconciliationDialog = false }
        )
    }

    // Quick Invoice Payment Modal
    if (selectedInvoiceForPayment != null) {
        PayInvoiceDialog(
            invoice = selectedInvoiceForPayment!!,
            currency = currency,
            onDismiss = { selectedInvoiceForPayment = null },
            onConfirmPay = { invoice, amt, method, trxId, notes ->
                viewModel.payInvoice(
                    invoice = invoice,
                    amount = amt,
                    method = method,
                    trxId = trxId,
                    remarks = notes
                )
                selectedInvoiceForPayment = null
            }
        )
    }

    // Thermal Receipt / POS Bill Print Modal
    if (selectedInvoiceForReceipt != null) {
        PrintReceiptDialog(
            invoice = selectedInvoiceForReceipt!!,
            currency = currency,
            onDismiss = { selectedInvoiceForReceipt = null },
            onPrint = {
                Toast.makeText(context, "Printing Thermal POS Invoice Receipt...", Toast.LENGTH_SHORT).show()
                selectedInvoiceForReceipt = null
            }
        )
    }
}

@Composable
fun BillingSummaryCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SleekCard),
        border = BorderStroke(1.dp, SleekBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Slate500,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
    }
}

@Composable
fun InvoiceCard(
    invoice: InvoiceEntity,
    currency: String,
    permissions: com.example.data.entity.UserRolePermissions,
    onCollectPayment: () -> Unit,
    onPrintInvoice: () -> Unit,
    onSendReminder: () -> Unit
) {
    val isPaid = invoice.status == "Paid"
    val isPartial = invoice.status == "Partial"

    val statusBg = when {
        isPaid -> EmeraldSuccess.copy(alpha = 0.12f)
        isPartial -> AmberAlert.copy(alpha = 0.15f)
        else -> CoralWarning.copy(alpha = 0.12f)
    }

    val statusTextColor = when {
        isPaid -> EmeraldSuccess
        isPartial -> AmberAlert
        else -> CoralWarning
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SleekCard),
        border = BorderStroke(1.dp, SleekBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Teal50),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = Teal600,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = invoice.invoiceNo,
                            fontWeight = FontWeight.Bold,
                            color = Teal600,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Month: ${invoice.billingMonthYear}",
                            fontSize = 11.sp,
                            color = Slate500
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusBg)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = invoice.status.uppercase(),
                        color = statusTextColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Customer Info Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Slate100, shape = RoundedCornerShape(10.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = invoice.customerName,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "ID: ${invoice.customerCode} • Package: ${invoice.packageName}",
                        color = Slate600,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Breakdown Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Package Bill", fontSize = 10.sp, color = Slate500, fontWeight = FontWeight.Medium)
                    Text("$currency ${String.format(java.util.Locale.US, "%,.0f", invoice.billAmount)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate800)
                }
                Column {
                    Text("Previous Due", fontSize = 10.sp, color = Slate500, fontWeight = FontWeight.Medium)
                    Text("$currency ${String.format(java.util.Locale.US, "%,.0f", invoice.previousDue)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate800)
                }
                Column {
                    Text("Paid Amount", fontSize = 10.sp, color = Slate500, fontWeight = FontWeight.Medium)
                    Text("$currency ${String.format(java.util.Locale.US, "%,.0f", invoice.totalPayable - invoice.dueAmount)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Payable", fontSize = 10.sp, color = Slate500, fontWeight = FontWeight.Medium)
                    Text("$currency ${String.format(java.util.Locale.US, "%,.0f", invoice.totalPayable)}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Teal600)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isPaid) {
                    if (permissions.canCollect) {
                        Button(
                            onClick = onCollectPayment,
                            modifier = Modifier.weight(1.2f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Teal600,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pay Bill", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (permissions.canSMS) {
                        OutlinedButton(
                            onClick = onSendReminder,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Teal600),
                            border = BorderStroke(1.dp, Teal100),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SMS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .height(38.dp)
                            .background(EmeraldSuccess.copy(alpha = 0.1f), shape = RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Bill Settled", color = EmeraldSuccess, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                OutlinedButton(
                    onClick = onPrintInvoice,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate800),
                    border = BorderStroke(1.dp, Slate200),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp), tint = Slate700)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Print POS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PayInvoiceDialog(
    invoice: InvoiceEntity,
    currency: String,
    onDismiss: () -> Unit,
    onConfirmPay: (InvoiceEntity, Double, String, String, String) -> Unit
) {
    var amountText by remember { mutableStateOf(invoice.dueAmount.toInt().toString()) }
    var paymentMethod by remember { mutableStateOf("bKash") }
    var transactionId by remember { mutableStateOf("TRX${System.currentTimeMillis().toString().takeLast(6)}") }
    var notes by remember { mutableStateOf("Monthly internet bill collection") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Bill Payment Collection", color = Slate900, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Invoice: ${invoice.invoiceNo} • ${invoice.customerName}", fontSize = 12.sp, color = Slate500)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Payable Due Info Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Teal50, shape = RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Current Due Amount:", fontSize = 12.sp, color = Slate600)
                        Text("$currency ${String.format(java.util.Locale.US, "%,.0f", invoice.dueAmount)}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Teal600)
                    }
                }

                // Payment Method Selector Pills
                Text("Select Payment Gateway / Method:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate700)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("bKash", "Nagad", "Cash", "Bank").forEach { method ->
                        val isSel = paymentMethod == method
                        val chipBg = when {
                            isSel && method == "bKash" -> BkashPink
                            isSel && method == "Nagad" -> NagadOrange
                            isSel -> Teal600
                            else -> Slate100
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(chipBg)
                                .clickable { paymentMethod = method }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = method,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.White else Slate700
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Received Amount ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Teal600,
                        unfocusedBorderColor = Slate200,
                        focusedTextColor = Slate900,
                        unfocusedTextColor = Slate900
                    )
                )

                OutlinedTextField(
                    value = transactionId,
                    onValueChange = { transactionId = it },
                    label = { Text("Transaction ID / Reference") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Teal600,
                        unfocusedBorderColor = Slate200,
                        focusedTextColor = Slate900,
                        unfocusedTextColor = Slate900
                    )
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Collector Remarks") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Teal600,
                        unfocusedBorderColor = Slate200,
                        focusedTextColor = Slate900,
                        unfocusedTextColor = Slate900
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: invoice.dueAmount
                    onConfirmPay(invoice, amt, paymentMethod, transactionId, notes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Teal600)
            ) {
                Text("Confirm Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Slate500) }
        },
        containerColor = SleekCard
    )
}

@Composable
fun PrintReceiptDialog(
    invoice: InvoiceEntity,
    currency: String,
    onDismiss: () -> Unit,
    onPrint: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("POS Bill Receipt Preview", color = Slate900, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Icon(Icons.Default.Print, contentDescription = null, tint = Teal600)
            }
        },
        text = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate200, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("NetBill ISP Professional", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Teal600)
                    Text("Uttara Sector 4, Dhaka-1230", fontSize = 10.sp, color = Slate500)
                    Text("Hotline: 01711000000 • Support 24/7", fontSize = 10.sp, color = Slate500)

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Slate200)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Invoice #: ${invoice.invoiceNo}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate800)
                        Text("Date: ${invoice.generatedDate}", fontSize = 11.sp, color = Slate500)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Customer: ${invoice.customerName} (${invoice.customerCode})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate900)
                        Text("Billing Month: ${invoice.billingMonthYear}", fontSize = 11.sp, color = Slate600)
                        Text("Package: ${invoice.packageName}", fontSize = 11.sp, color = Slate600)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Slate200)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Package Bill Amount:", fontSize = 11.sp, color = Slate600)
                        Text("$currency ${String.format(java.util.Locale.US, "%,.0f", invoice.billAmount)}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Previous Balance / Due:", fontSize = 11.sp, color = Slate600)
                        Text("$currency ${String.format(java.util.Locale.US, "%,.0f", invoice.previousDue)}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Amount Paid:", fontSize = 11.sp, color = Slate600)
                        Text("$currency ${String.format(java.util.Locale.US, "%,.0f", invoice.totalPayable - invoice.dueAmount)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(color = Slate200)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("TOTAL PAYABLE DUE:", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Slate900)
                        Text("$currency ${String.format(java.util.Locale.US, "%,.0f", invoice.dueAmount)}", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Teal600)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Slate100, shape = RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "* Thank you for using NetBill ISP High Speed Fiber *",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Slate600
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onPrint,
                colors = ButtonDefaults.buttonColors(containerColor = Teal600)
            ) {
                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Print POS Bill")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = Slate500) }
        },
        containerColor = SleekCard
    )
}

@Composable
fun GenerateBillsDialog(
    onDismiss: () -> Unit,
    onGenerate: (String, String) -> Unit
) {
    var monthYear by remember { mutableStateOf("August 2026") }
    var day20Option by remember { mutableStateOf("NextMonth") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Receipt, contentDescription = null, tint = Teal600)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Batch Auto Monthly Billing", color = Slate900, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Generate monthly recurring internet bills for all active subscribers.",
                    fontSize = 12.sp,
                    color = Slate600
                )

                OutlinedTextField(
                    value = monthYear,
                    onValueChange = { monthYear = it },
                    label = { Text("Billing Month & Year (e.g. August 2026)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Teal600,
                        unfocusedBorderColor = Slate200,
                        focusedTextColor = Slate900,
                        unfocusedTextColor = Slate900
                    )
                )

                // 20th Day Special Billing Rule Option Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = AmberAlert.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AmberAlert.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = AmberAlert, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = AppTranslation("day_20_rule_title"),
                                fontWeight = FontWeight.Bold,
                                color = Slate900,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = AppTranslation("day_20_rule_desc"),
                            color = Slate600,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = day20Option == "NextMonth",
                                onClick = { day20Option = "NextMonth" },
                                colors = RadioButtonDefaults.colors(selectedColor = Teal600)
                            )
                            Text(AppTranslation("gen_next_month"), fontSize = 11.sp, color = Slate800, fontWeight = FontWeight.Medium)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = day20Option == "CurrentMonth",
                                onClick = { day20Option = "CurrentMonth" },
                                colors = RadioButtonDefaults.colors(selectedColor = Teal600)
                            )
                            Text(AppTranslation("gen_current_month"), fontSize = 11.sp, color = Slate800, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onGenerate(monthYear, day20Option) },
                colors = ButtonDefaults.buttonColors(containerColor = Teal600),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Run Auto Billing Batch", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Slate500) }
        },
        containerColor = SleekCard
    )
}
