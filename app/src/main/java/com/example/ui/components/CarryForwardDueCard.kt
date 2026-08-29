package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.CustomerEntity
import com.example.data.entity.InvoiceEntity
import com.example.localization.appTranslation
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.BkashPink
import com.example.ui.theme.CoralWarning
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SleekCard
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Teal50
import com.example.ui.theme.Teal600
import com.example.viewmodel.MainViewModel

@Composable
fun CarryForwardDueCard(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onCollectPaymentClick: (CustomerEntity) -> Unit = {}
) {
    val customers by viewModel.customersList.collectAsState()
    val invoices by viewModel.invoicesList.collectAsState()
    val currency = appTranslation("currency_symbol")

    var expandedSimulator by remember { mutableStateOf(false) }
    var selectedCustomerId by remember { mutableStateOf<String?>(null) }
    var simPaymentAmountInput by remember { mutableStateOf("1000") }
    var selectedPaymentMethod by remember { mutableStateOf("Cash") }

    // Filter customers with unpaid invoices
    val dueCustomers = remember(customers, invoices) {
        customers.filter { cust ->
            invoices.any { inv -> inv.customerId == cust.id && inv.dueAmount > 0 } || cust.currentDue > 0
        }
    }

    // Default to first due customer if not selected
    val selectedCustomer = remember(dueCustomers, selectedCustomerId) {
        dueCustomers.find { it.id == selectedCustomerId } ?: dueCustomers.firstOrNull()
    }

    // Unpaid invoices for selected customer sorted oldest first (id ASC)
    val customerUnpaidInvoices = remember(selectedCustomer, invoices) {
        if (selectedCustomer == null) emptyList()
        else invoices.filter { it.customerId == selectedCustomer.id && it.dueAmount > 0 && it.status != "Cancelled" }
            .sortedBy { it.id }
    }

    // Calculate total carried forward due and current month bill for selected customer
    val carryForwardDueTotal = remember(customerUnpaidInvoices) {
        if (customerUnpaidInvoices.size <= 1) 0.0
        else customerUnpaidInvoices.dropLast(1).sumOf { it.dueAmount }
    }

    val latestMonthBill = remember(customerUnpaidInvoices) {
        customerUnpaidInvoices.lastOrNull()?.billAmount ?: (selectedCustomer?.monthlyBill ?: 0.0)
    }

    val totalCustomerDue = remember(customerUnpaidInvoices) {
        customerUnpaidInvoices.sumOf { it.dueAmount }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SleekCard),
        border = BorderStroke(1.dp, Teal600.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title Header with Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Teal600.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = Teal600,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Carry Forward Due (FIFO Billing)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "First In First Out Allocation Engine • $currency BDT",
                            fontSize = 11.sp,
                            color = Slate500
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Teal600)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "MANDATORY FIFO",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Explanation Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Slate100)
                    .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Teal600,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "How Carry Forward Due & FIFO Payment Allocation Works:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Slate800
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "1. Unpaid balances automatically carry forward to next month's bill as Previous Due.\n" +
                                "2. Payments ALWAYS clear the oldest unpaid bills first (First In, First Out).\n" +
                                "3. Excess payment is stored as Advance Credit for future monthly bills.",
                        fontSize = 11.sp,
                        color = Slate600,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Simulator Toggle Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ElectricBlue.copy(alpha = 0.08f))
                    .clickable { expandedSimulator = !expandedSimulator }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Payment,
                        contentDescription = null,
                        tint = ElectricBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Interactive FIFO Allocation Simulator",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = ElectricBlue
                    )
                }
                Icon(
                    imageVector = if (expandedSimulator) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = ElectricBlue
                )
            }

            // Interactive Simulator Panel
            AnimatedVisibility(visible = expandedSimulator) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                        .border(1.dp, Slate200, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Select Customer to Simulate FIFO Allocation:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate600
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Customer Selector Chips
                    if (dueCustomers.isEmpty()) {
                        Text(
                            text = "No customers with unpaid bills currently.",
                            fontSize = 12.sp,
                            color = Slate500
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            dueCustomers.take(3).forEach { cust ->
                                val isSelected = selectedCustomer?.id == cust.id
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Teal600 else Slate100)
                                        .clickable { selectedCustomerId = cust.id }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "${cust.name.take(12)} ($currency${cust.currentDue.toInt()})",
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Slate800
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (selectedCustomer != null) {
                        // Customer Financial Header Card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Teal50, RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${selectedCustomer.name} (${selectedCustomer.customerCode})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Slate900
                                )
                                Text(
                                    text = "Package: ${selectedCustomer.packageName} • Monthly: $currency${selectedCustomer.monthlyBill.toInt()}",
                                    fontSize = 11.sp,
                                    color = Slate600
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Total Due: $currency ${totalCustomerDue.toInt()}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = CoralWarning
                                )
                                if (selectedCustomer.advanceBalance > 0) {
                                    Text(
                                        text = "Advance: $currency ${selectedCustomer.advanceBalance.toInt()}",
                                        fontSize = 10.sp,
                                        color = EmeraldSuccess,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Test Payment Amount Input
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = simPaymentAmountInput,
                                onValueChange = { simPaymentAmountInput = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("Payment Amount ($currency)", fontSize = 11.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Teal600,
                                    unfocusedBorderColor = Slate200
                                )
                            )

                            Button(
                                onClick = {
                                    val amt = simPaymentAmountInput.toDoubleOrNull() ?: 0.0
                                    if (amt > 0) {
                                        viewModel.recordPayment(
                                            customerId = selectedCustomer.id,
                                            amount = amt,
                                            method = selectedPaymentMethod,
                                            trxId = "FIFO-${System.currentTimeMillis().toString().takeLast(6)}",
                                            remarks = "Executed via FIFO Allocation Engine"
                                        )
                                        viewModel.showToast("৳$amt payment allocated via FIFO algorithm!")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Apply FIFO Payment", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Real-time FIFO Payment Allocation Preview:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Live FIFO Allocation breakdown list
                        val simAmount = simPaymentAmountInput.toDoubleOrNull() ?: 0.0
                        var tempRemaining = simAmount

                        customerUnpaidInvoices.forEachIndexed { index, inv ->
                            val invDue = inv.dueAmount
                            val simAllocated = if (tempRemaining > 0) Math.min(tempRemaining, invDue) else 0.0
                            val simDueAfter = (invDue - simAllocated).coerceAtLeast(0.0)
                            val simStatus = when {
                                simDueAfter <= 0.0 -> "PAID"
                                simAllocated > 0 -> "PARTIALLY PAID"
                                else -> "UNPAID"
                            }
                            tempRemaining -= simAllocated

                            val statusColor = when (simStatus) {
                                "PAID" -> EmeraldSuccess
                                "PARTIALLY PAID" -> AmberAlert
                                else -> CoralWarning
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(Slate100, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "#${index + 1} Bill: ${inv.billingMonthYear} (${inv.invoiceNo})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Slate900
                                    )
                                    Text(
                                        text = "Gross: $currency${inv.totalPayable.toInt()} • Due before: $currency${invDue.toInt()}",
                                        fontSize = 10.sp,
                                        color = Slate500
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Allocated: $currency${simAllocated.toInt()}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = Teal600
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(statusColor.copy(alpha = 0.15f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = simStatus,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = statusColor
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Due after: $currency${simDueAfter.toInt()}",
                                        fontSize = 10.sp,
                                        color = Slate600
                                    )
                                }
                            }
                        }

                        if (tempRemaining > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(EmeraldSuccess.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = EmeraldSuccess,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Excess Payment $currency${tempRemaining.toInt()} will be carried over as Advance Credit for future bills!",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldSuccess
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Outstanding Carried Forward Customers List Header
            Text(
                text = "Customers with Carried Forward Due:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Slate900
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (dueCustomers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No unpaid carried forward bills in system.", fontSize = 12.sp, color = Slate500)
                }
            } else {
                dueCustomers.take(5).forEach { cust ->
                    val custInvoices = invoices.filter { it.customerId == cust.id && it.dueAmount > 0 }
                    val prevDueCarry = if (custInvoices.size > 1) custInvoices.dropLast(1).sumOf { it.dueAmount } else 0.0
                    val currentMonthBill = custInvoices.lastOrNull()?.billAmount ?: cust.monthlyBill

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(Slate100, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = cust.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Slate900
                            )
                            Text(
                                text = "Code: ${cust.customerCode} • Pkg: ${cust.packageName}",
                                fontSize = 10.sp,
                                color = Slate500
                            )
                            Row(
                                modifier = Modifier.padding(top = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Carry Forward: $currency${prevDueCarry.toInt()}",
                                    fontSize = 10.sp,
                                    color = CoralWarning,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("•", fontSize = 10.sp, color = Slate400)
                                Text(
                                    text = "Current Bill: $currency${currentMonthBill.toInt()}",
                                    fontSize = 10.sp,
                                    color = Slate600
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$currency ${cust.currentDue.toInt()}",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = CoralWarning
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { onCollectPaymentClick(cust) },
                                colors = ButtonDefaults.buttonColors(containerColor = Teal600),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(26.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text("FIFO Pay", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
