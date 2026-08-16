package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.CustomerEntity
import com.example.data.entity.InvoiceEntity
import com.example.data.entity.LedgerEntryEntity
import com.example.data.entity.PaymentCollectionEntity
import com.example.util.AppUtils
import com.example.localization.AppTranslation
import com.example.ui.components.ReadonlyDateField
import com.example.ui.theme.BkashPink
import java.text.SimpleDateFormat
import java.util.*
import com.example.ui.theme.CoralWarning
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NagadOrange
import com.example.ui.theme.Navy800
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.SleekBg
import com.example.ui.theme.SleekBorder
import com.example.ui.theme.SleekCard
import com.example.ui.theme.Teal50
import com.example.ui.theme.Teal600
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerLedgerScreen(
    customer: CustomerEntity,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val ledgerList by viewModel.getLedgerForCustomer(customer.id).collectAsState(initial = emptyList())
    val customerInvoices by viewModel.invoicesList.collectAsState()
    val customerPayments by viewModel.paymentsList.collectAsState()

    val filteredInvoices = customerInvoices.filter { it.customerId == customer.id }
    val filteredPayments = customerPayments.filter { it.customerId == customer.id }

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Timeline, 1 = Bill History, 2 = Payment & Receipts
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf("All") }
    var showAddLedgerDialog by remember { mutableStateOf(false) }

    // Selected receipt for dialog view
    val selectedReceipt by viewModel.selectedReceipt.collectAsState()

    val currency = AppTranslation("currency_symbol")

    // Calculations for Summary Cards
    val totalBills = ledgerList.filter { it.type == "Monthly Bill" || it.type == "Manual Charge" }.sumOf { it.amount }
    val totalPayments = ledgerList.filter { it.type == "Payment" }.sumOf { it.amount }
    val totalDiscounts = ledgerList.filter { it.type == "Discount" || it.type == "Waiver" }.sumOf { it.amount }
    val totalAdvance = ledgerList.filter { it.type == "Advance" }.sumOf { it.amount }
    val previousDue = ledgerList.filter { it.type == "Previous Due" }.sumOf { it.amount }
    val carryForwardDue = ledgerList.filter { it.type == "Carry Forward Due" }.sumOf { it.amount }
    val netOutstanding = customer.currentDue

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SleekBg)
            .padding(16.dp)
    ) {
        // Top Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Slate900)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = AppTranslation("customer_ledger"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Text(
                        text = "${customer.name} (${customer.customerCode})",
                        fontSize = 12.sp,
                        color = Teal600,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = { showAddLedgerDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Teal600),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(AppTranslation("add_ledger_entry"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Customer Header Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekCard),
                    border = BorderStroke(1.dp, SleekBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = customer.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Text(
                                    text = "Mobile: ${customer.mobile} | User: ${customer.username.ifEmpty { customer.pppoeUsername }}",
                                    fontSize = 12.sp,
                                    color = Slate600
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (customer.status == "Active") EmeraldSuccess.copy(alpha = 0.15f) else CoralWarning.copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = customer.status,
                                    color = if (customer.status == "Active") EmeraldSuccess else CoralWarning,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(SleekBorder)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Package & Monthly Bill", fontSize = 11.sp, color = Slate600)
                                Text(
                                    "${customer.packageName} ($currency ${customer.monthlyBill.toInt()})",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                            }
                            Column {
                                Text("Zone / Sub Zone / Box", fontSize = 11.sp, color = Slate600)
                                Text(
                                    "${customer.zone} • ${customer.subZone.ifEmpty { "Main" }}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                            }
                            Column {
                                Text("Joined Date", fontSize = 11.sp, color = Slate600)
                                Text(
                                    customer.joinDate,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                            }
                        }
                    }
                }
            }

            // Current Financial Summary Grid
            item {
                Column {
                    Text(
                        text = AppTranslation("ledger_summary"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate800,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryCard(
                            title = AppTranslation("net_outstanding"),
                            amount = "$currency ${netOutstanding.toInt()}",
                            color = CoralWarning,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryCard(
                            title = AppTranslation("current_due"),
                            amount = "$currency ${customer.currentDue.toInt()}",
                            color = CoralWarning,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryCard(
                            title = AppTranslation("total_paid"),
                            amount = "$currency ${totalPayments.toInt()}",
                            color = EmeraldSuccess,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryCard(
                            title = AppTranslation("previous_due"),
                            amount = "$currency ${previousDue.toInt()}",
                            color = Slate800,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryCard(
                            title = AppTranslation("carry_forward_due"),
                            amount = "$currency ${carryForwardDue.toInt()}",
                            color = Slate800,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryCard(
                            title = AppTranslation("total_generated_bill"),
                            amount = "$currency ${totalBills.toInt()}",
                            color = Teal600,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryCard(
                            title = AppTranslation("total_discount"),
                            amount = "$currency ${totalDiscounts.toInt()}",
                            color = ElectricBlue,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryCard(
                            title = AppTranslation("total_advance"),
                            amount = "$currency ${totalAdvance.toInt()}",
                            color = CyanAccent,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Export / Print Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppTranslation("ledger_timeline"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                Toast.makeText(context, "Exporting Ledger PDF for ${customer.name}...", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Teal600),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Teal600)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(AppTranslation("export_ledger"), fontSize = 11.sp)
                        }
                    }
                }
            }

            // Tabs Row (0 = Timeline, 1 = Bill History, 2 = Payment History)
            item {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = SleekCard,
                    contentColor = Teal600,
                    edgePadding = 0.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Teal600
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Ledger Timeline", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Bill History (${filteredInvoices.size})", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Payment History (${filteredPayments.size})", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            // TAB 0: TIMELINE LEDGER
            if (selectedTab == 0) {
                // Filters & Search Bar
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search Receipt/Bill/Ref No...", fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate600) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Teal600,
                                    unfocusedBorderColor = SleekBorder,
                                    focusedContainerColor = SleekCard,
                                    unfocusedContainerColor = SleekCard
                                )
                            )

                            // Type Filter Dropdown
                            var dropdownExpanded by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(
                                    onClick = { dropdownExpanded = true },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, SleekBorder),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = SleekCard, contentColor = Slate800)
                                ) {
                                    Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (selectedTypeFilter == "All") "Filter: All" else selectedTypeFilter, fontSize = 12.sp)
                                }

                                DropdownMenu(
                                    expanded = dropdownExpanded,
                                    onDismissRequest = { dropdownExpanded = false }
                                ) {
                                    val filterOptions = listOf(
                                        "All", "Monthly Bill", "Payment", "Discount",
                                        "Advance", "Adjustment", "Previous Due",
                                        "Carry Forward Due", "Manual Charge", "Refund", "Waiver"
                                    )
                                    filterOptions.forEach { opt ->
                                        DropdownMenuItem(
                                            text = { Text(opt) },
                                            onClick = {
                                                selectedTypeFilter = opt
                                                dropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Filter Timeline Items
                val filteredLedger = ledgerList.filter { entry ->
                    val matchQuery = searchQuery.isEmpty() ||
                            entry.referenceNo.contains(searchQuery, ignoreCase = true) ||
                            entry.description.contains(searchQuery, ignoreCase = true) ||
                            entry.type.contains(searchQuery, ignoreCase = true)
                    val matchType = selectedTypeFilter == "All" || entry.type == selectedTypeFilter
                    matchQuery && matchType
                }

                if (filteredLedger.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No ledger transaction records found.", color = Slate600, fontSize = 13.sp)
                        }
                    }
                } else {
                    items(filteredLedger, key = { it.id }) { entry ->
                        LedgerTimelineItemCard(entry = entry, currency = currency)
                    }
                }
            }

            // TAB 1: BILL HISTORY
            if (selectedTab == 1) {
                if (filteredInvoices.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No monthly bills generated yet for this customer.", color = Slate600, fontSize = 13.sp)
                        }
                    }
                } else {
                    items(filteredInvoices, key = { it.id }) { inv ->
                        BillHistoryItemCard(invoice = inv, currency = currency)
                    }
                }
            }

            // TAB 2: PAYMENT HISTORY & RECEIPT ACTIONS
            if (selectedTab == 2) {
                if (filteredPayments.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No payment receipts recorded yet.", color = Slate600, fontSize = 13.sp)
                        }
                    }
                } else {
                    items(filteredPayments, key = { it.id }) { pymt ->
                        PaymentHistoryItemCard(
                            payment = pymt,
                            currency = currency,
                            onViewReceipt = { viewModel.setSelectedReceipt(pymt) }
                        )
                    }
                }
            }
        }
    }

    // Add Custom Ledger Entry Modal Dialog
    if (showAddLedgerDialog) {
        AddLedgerEntryDialog(
            customerName = customer.name,
            onDismiss = { showAddLedgerDialog = false },
            onSubmit = { type, amount, isDebit, desc, refNo, payMethod, date ->
                viewModel.addCustomLedgerEntry(
                    customerId = customer.id,
                    type = type,
                    amount = amount,
                    isDebit = isDebit,
                    description = desc,
                    referenceNo = refNo,
                    method = payMethod,
                    date = date
                )
                showAddLedgerDialog = false
            }
        )
    }

    // Invoice/Receipt View Modal Dialog
    selectedReceipt?.let { pymt ->
        InvoiceReceiptDialog(
            payment = pymt,
            viewModel = viewModel,
            onDismiss = { viewModel.setSelectedReceipt(null) }
        )
    }
}

@Composable
fun SummaryCard(
    title: String,
    amount: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SleekCard),
        border = BorderStroke(1.dp, SleekBorder)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                fontSize = 10.sp,
                color = Slate600,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = amount,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
    }
}

@Composable
fun LedgerTimelineItemCard(entry: com.example.data.entity.LedgerEntity, currency: String) {
    val isCredit = !entry.isDebit
    val color = when (entry.type) {
        "Payment", "Advance", "Refund" -> EmeraldSuccess
        "Discount", "Waiver" -> ElectricBlue
        "Monthly Bill", "Manual Charge", "Carry Forward Due", "Previous Due" -> CoralWarning
        else -> Slate800
    }

    val icon = when (entry.type) {
        "Payment" -> Icons.Default.CheckCircle
        "Discount", "Waiver" -> Icons.Default.LocalOffer
        "Monthly Bill" -> Icons.Default.Receipt
        "Carry Forward Due", "Previous Due" -> Icons.Default.ArrowUpward
        else -> Icons.Default.AttachMoney
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SleekCard),
        border = BorderStroke(1.dp, SleekBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Badge
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Main Transaction Description & Ref
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(color.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = entry.type,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                    }

                    if (entry.referenceNo.isNotBlank()) {
                        Text(
                            text = "#${entry.referenceNo}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate600
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = entry.description.ifEmpty { entry.type },
                    fontSize = 12.sp,
                    color = Slate900,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${AppUtils.formatDateForDisplay(entry.date)} at ${entry.time}${if (entry.paymentMethod.isNotBlank()) " • " + entry.paymentMethod else ""}",
                    fontSize = 10.sp,
                    color = Slate600
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Amount & Running Balance
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isCredit) "-" else "+"}$currency ${entry.amount.toInt()}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCredit) EmeraldSuccess else CoralWarning
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Bal: $currency ${entry.runningBalance.toInt()}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate600
                )
            }
        }
    }
}

@Composable
fun BillHistoryItemCard(invoice: InvoiceEntity, currency: String) {
    val statusColor = when (invoice.status) {
        "Paid" -> EmeraldSuccess
        "Partial Payment", "Partial" -> NagadOrange
        else -> CoralWarning
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SleekCard),
        border = BorderStroke(1.dp, SleekBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(invoice.invoiceNo, fontWeight = FontWeight.Bold, color = Slate900, fontSize = 14.sp)
                    Text("Month: ${invoice.billingMonthYear}", color = Slate600, fontSize = 12.sp)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(invoice.status, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SleekBorder))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Bill Amount", fontSize = 10.sp, color = Slate600)
                    Text("$currency ${invoice.billAmount.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate900)
                }
                Column {
                    Text("Paid Amount", fontSize = 10.sp, color = Slate600)
                    Text("$currency ${invoice.paidAmount.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                }
                Column {
                    Text("Remaining Due", fontSize = 10.sp, color = Slate600)
                    Text("$currency ${invoice.dueAmount.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CoralWarning)
                }
                Column {
                    Text("Due Date", fontSize = 10.sp, color = Slate600)
                    Text(AppUtils.formatDateForDisplay(invoice.dueDate), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate900)
                }
            }
        }
    }
}

@Composable
fun PaymentHistoryItemCard(
    payment: PaymentCollectionEntity,
    currency: String,
    onViewReceipt: () -> Unit
) {
    val methodColor = when (payment.paymentMethod) {
        "bKash" -> BkashPink
        "Nagad" -> NagadOrange
        "Cash" -> EmeraldSuccess
        else -> ElectricBlue
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SleekCard),
        border = BorderStroke(1.dp, SleekBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(payment.receiptNo, fontWeight = FontWeight.Bold, color = Slate900, fontSize = 14.sp)
                    Text("Collected By: ${payment.collectorName}", color = Slate600, fontSize = 11.sp)
                }

                Text(
                    text = "$currency ${payment.amount.toInt()}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = EmeraldSuccess
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(methodColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(payment.paymentMethod, color = methodColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    if (payment.transactionId.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("TrxID: ${payment.transactionId}", fontSize = 11.sp, color = Slate600)
                    }
                }

                Text(AppUtils.formatDateForDisplay(payment.paymentDate), fontSize = 11.sp, color = Slate600)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: View/Print/Share Receipt
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onViewReceipt) {
                    Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(14.dp), tint = Teal600)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(AppTranslation("download_receipt"), fontSize = 11.sp, color = Teal600, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(8.dp))

                TextButton(onClick = onViewReceipt) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp), tint = Teal600)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(AppTranslation("share_receipt"), fontSize = 11.sp, color = Teal600, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLedgerEntryDialog(
    customerName: String,
    onDismiss: () -> Unit,
    onSubmit: (type: String, amount: Double, isDebit: Boolean, desc: String, refNo: String, payMethod: String, date: String) -> Unit
) {
    var selectedType by remember { mutableStateOf("Discount") }
    var amountText by remember { mutableStateOf("") }
    var descriptionText by remember { mutableStateOf("") }
    var refNoText by remember { mutableStateOf("") }
    var paymentMethodText by remember { mutableStateOf("Cash") }
    var entryDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }

    val entryTypes = listOf(
        "Monthly Bill", "Payment", "Discount", "Advance",
        "Adjustment", "Previous Due", "Carry Forward Due",
        "Manual Charge", "Refund", "Waiver"
    )

    val isDebit = when (selectedType) {
        "Monthly Bill", "Manual Charge", "Carry Forward Due", "Previous Due" -> true
        else -> false // Payment, Discount, Waiver, Advance, Refund decrease due
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Ledger Entry for $customerName", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Select Ledger Entry Type", fontSize = 12.sp, color = Slate600, fontWeight = FontWeight.Bold)

                var typeDropdownExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { typeDropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate900)
                    ) {
                        Text("Type: $selectedType (${if (isDebit) "Debit +" else "Credit -"})", fontWeight = FontWeight.Bold)
                    }

                    DropdownMenu(
                        expanded = typeDropdownExpanded,
                        onDismissRequest = { typeDropdownExpanded = false }
                    ) {
                        entryTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    selectedType = type
                                    typeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (৳ BDT)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = descriptionText,
                    onValueChange = { descriptionText = it },
                    label = { Text("Description / Remarks") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = refNoText,
                    onValueChange = { refNoText = it },
                    label = { Text("Reference / Receipt No (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                ReadonlyDateField(
                    value = entryDate,
                    label = "লেজার তারিখ (Entry Date)",
                    onDateSelected = { entryDate = it },
                    modifier = Modifier.fillMaxWidth()
                )

                if (selectedType == "Payment" || selectedType == "Refund" || selectedType == "Advance") {
                    OutlinedTextField(
                        value = paymentMethodText,
                        onValueChange = { paymentMethodText = it },
                        label = { Text("Payment Method (Cash, bKash, Nagad, Bank)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onSubmit(selectedType, amt, isDebit, descriptionText, refNoText, paymentMethodText, entryDate)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Teal600)
            ) {
                Text("Post Entry")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Slate600)
            }
        }
    )
}
