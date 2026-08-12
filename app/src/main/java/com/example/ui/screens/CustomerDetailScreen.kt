package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import com.example.ui.theme.*
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.CustomerEntity
import com.example.localization.AppTranslation
import com.example.ui.theme.BkashPink
import com.example.ui.theme.CoralWarning
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.viewmodel.MainViewModel

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.Schedule

import com.example.ui.components.CustomerSmsLogsBottomSheet
import androidx.compose.material.icons.filled.Sms

@Composable
fun CustomerDetailScreen(
    customer: CustomerEntity,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onCollectPayment: () -> Unit,
    onViewLedger: () -> Unit = {}
) {
    val payments by viewModel.paymentsList.collectAsState()
    val invoices by viewModel.invoicesList.collectAsState()

    var showEditExpiryDialog by remember { mutableStateOf(false) }
    var showSmsLogsBottomSheet by remember { mutableStateOf(false) }

    val customerPayments = payments.filter { it.customerId == customer.id }
    val customerInvoices = invoices.filter { it.customerId == customer.id }
    val currency = AppTranslation("currency_symbol")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SleekBg)
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Slate900)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Customer Profile: ${customer.customerCode}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Slate900
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Main Customer Identity Header Card
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
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Text(
                                    text = "Joined: ${customer.joinDate} • Day ${customer.joinDayOfMonth}",
                                    fontSize = 12.sp,
                                    color = Slate600
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (customer.status == "Active") EmeraldSuccess.copy(alpha = 0.2f) else CoralWarning.copy(alpha = 0.2f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = customer.status,
                                    color = if (customer.status == "Active") EmeraldSuccess else CoralWarning,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Quick Action Button Row
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onViewLedger,
                                colors = ButtonDefaults.buttonColors(containerColor = Teal600),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Customer Ledger", fontSize = 12.sp)
                            }

                            Button(
                                onClick = onCollectPayment,
                                colors = ButtonDefaults.buttonColors(containerColor = BkashPink),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Collect Payment", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = { viewModel.toggleCustomerStatus(customer) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralWarning),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (customer.status == "Active") Icons.Default.Block else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (customer.status == "Active") "Suspend" else "Enable", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { showSmsLogsBottomSheet = true },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SMS Delivery Logs & Gateway Tracker", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Connection & PPPoE Credentials Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Navy800)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("💰 প্যাকেজ ও বিলিং বিবরণ (Package & Billing)", fontWeight = FontWeight.Bold, color = CyanAccent, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        DetailRow(label = "Package Name", value = customer.packageName)
                        DetailRow(label = "Monthly Bill", value = "$currency ${customer.monthlyBill.toInt()}/mo")
                        if (customer.discount > 0) {
                            DetailRow(label = "Monthly Discount", value = "- $currency ${customer.discount.toInt()}")
                        }
                        if (customer.connectionFee > 0) {
                            DetailRow(label = "Installation Fee", value = "$currency ${customer.connectionFee.toInt()}")
                        }
                        DetailRow(label = "Billing Type", value = customer.billingType)
                        DetailRow(
                            label = "Current Account Balance",
                            value = if (customer.currentDue > 0) "Due: $currency ${customer.currentDue.toInt()}" else if (customer.advanceBalance > 0) "Advance: $currency ${customer.advanceBalance.toInt()}" else "Clean Paid"
                        )
                    }
                }
            }

            // Expiry Date & Time Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Navy800),
                    border = BorderStroke(1.dp, Color(0xFFE11D48).copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⏱️ মেয়াদের তারিখ ও সময় (Expiry Date & Time)", fontWeight = FontWeight.Bold, color = Color(0xFFFB7185), fontSize = 14.sp)

                            Button(
                                onClick = { showEditExpiryDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("মেয়াদ পরিবর্তন", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        DetailRow(
                            label = "Expiration Date (মেয়াদ শেষ তারিখ)",
                            value = if (customer.expireDate.isNotBlank()) customer.expireDate else "Not Set"
                        )
                        DetailRow(
                            label = "Expiration Time (মেয়াদ শেষ সময়)",
                            value = if (customer.expireTime.isNotBlank()) customer.expireTime else "23:59"
                        )
                    }
                }
            }

            // Technical & Network Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Navy800)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🌐 নেটওয়ার্ক ও কারিগরি বিবরণ (Network Specs)", fontWeight = FontWeight.Bold, color = CyanAccent, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        DetailRow(label = "Connection Type", value = customer.connectionType)
                        DetailRow(label = "PPPoE Username", value = customer.pppoeUsername)
                        DetailRow(label = "PPPoE Password", value = customer.pppoePassword)
                        DetailRow(label = "IP Address", value = if (customer.ipAddress.isNotEmpty()) customer.ipAddress else "Dynamic Auto IP")
                        DetailRow(label = "MAC Address", value = if (customer.macAddress.isNotEmpty()) customer.macAddress else "N/A")
                        DetailRow(label = "ONU Serial / MAC", value = if (customer.onuMacSerial.isNotEmpty()) customer.onuMacSerial else "N/A")
                        DetailRow(label = "Fiber Cable Core", value = if (customer.fiberCoreNo.isNotEmpty()) customer.fiberCoreNo else "N/A")
                        DetailRow(label = "Network Box / TJ", value = if (customer.networkBox.isNotEmpty()) customer.networkBox else "Splitter Box 1")
                    }
                }
            }

            // Contact & Personal Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Navy800)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("👤 ব্যক্তিগত ও পরিচয় তথ্য (Personal Details)", fontWeight = FontWeight.Bold, color = CyanAccent, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        DetailRow(label = "Primary Mobile", value = customer.mobile)
                        if (customer.altMobile.isNotEmpty()) {
                            DetailRow(label = "Alt Mobile", value = customer.altMobile)
                        }
                        if (customer.email.isNotEmpty()) {
                            DetailRow(label = "Email Address", value = customer.email)
                        }
                        if (customer.nidNumber.isNotEmpty()) {
                            DetailRow(label = "NID Number", value = customer.nidNumber)
                        }
                        if (customer.dob.isNotEmpty()) {
                            DetailRow(label = "Date of Birth", value = customer.dob)
                        }
                    }
                }
            }

            // Address & Location Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Navy800)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🏠 ঠিকানা ও রেফারেন্স (Address & Contacts)", fontWeight = FontWeight.Bold, color = CyanAccent, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        DetailRow(label = "Zone / Sub Zone", value = "${customer.zone} ${if (customer.subZone.isNotEmpty()) "(${customer.subZone})" else ""}")
                        DetailRow(label = "Full Address", value = customer.address)
                        if (customer.houseOwnerName.isNotEmpty()) {
                            DetailRow(label = "House Owner Info", value = customer.houseOwnerName)
                        }
                        if (customer.emergencyContact.isNotEmpty()) {
                            DetailRow(label = "Emergency Contact", value = customer.emergencyContact)
                        }
                        if (customer.referenceName.isNotEmpty()) {
                            DetailRow(label = "Reference Person", value = "${customer.referenceName} (${customer.referenceMobile})")
                        }
                        if (customer.notes.isNotEmpty()) {
                            DetailRow(label = "Notes / Remarks", value = customer.notes)
                        }
                    }
                }
            }

            // Payment History Header
            item {
                Text(
                    text = "Payment History & Receipts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
            }

            if (customerPayments.isEmpty()) {
                item {
                    Text("No payment transactions recorded yet.", color = Slate600, fontSize = 13.sp)
                }
            } else {
                items(customerPayments, key = { it.id }) { pymt ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = SleekCard),
                        border = BorderStroke(1.dp, SleekBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = pymt.receiptNo, fontWeight = FontWeight.Bold, color = Slate900, fontSize = 13.sp)
                                Text(text = "${pymt.paymentDate} • ${pymt.paymentMethod} (${pymt.transactionId})", color = Slate600, fontSize = 11.sp)
                            }
                            Text(text = "$currency ${pymt.amount.toInt()}", fontWeight = FontWeight.Bold, color = EmeraldSuccess, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }

    if (showEditExpiryDialog) {
        var newDate by remember { mutableStateOf(customer.expireDate.ifEmpty { "2026-09-01" }) }
        var newTime by remember { mutableStateOf(customer.expireTime.ifEmpty { "23:59" }) }

        AlertDialog(
            onDismissRequest = { showEditExpiryDialog = false },
            title = {
                Text(
                    text = "গ্রাহকের মেয়াদ উত্তীর্ণের তারিখ ও সময় নির্ধারণ",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "এডমিন পছন্দ অনুযায়ী গ্রাহক (${customer.name}) এর ইন্টারনেট মেয়াদের তারিখ এবং সময় নির্ধারণ বা পরিবর্তন করতে পারবেন।",
                        fontSize = 12.sp,
                        color = Slate600
                    )

                    OutlinedTextField(
                        value = newDate,
                        onValueChange = { newDate = it },
                        label = { Text("মেয়াদ উত্তীর্ণের তারিখ (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = newTime,
                        onValueChange = { newTime = it },
                        label = { Text("মেয়াদ উত্তীর্ণের সময় (HH:MM / AM-PM)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text("দ্রুত সময় নির্বাচন করুন:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate800)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = newDate == "2026-08-31",
                            onClick = {
                                newDate = "2026-08-31"
                                newTime = "11:59 PM"
                            },
                            label = { Text("আজ রাত 11:59", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = newDate == "2026-09-08",
                            onClick = {
                                newDate = "2026-09-08"
                                newTime = "11:59 PM"
                            },
                            label = { Text("+৭ দিন", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = newDate == "2026-09-30",
                            onClick = {
                                newDate = "2026-09-30"
                                newTime = "11:59 PM"
                            },
                            label = { Text("+৩০ দিন", fontSize = 10.sp) }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addOrUpdateCustomer(
                            customer.copy(
                                expireDate = newDate,
                                expireTime = newTime
                            )
                        )
                        showEditExpiryDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                ) {
                    Text("মেয়াদ সংরক্ষণ করুন", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditExpiryDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    if (showSmsLogsBottomSheet) {
        CustomerSmsLogsBottomSheet(
            customer = customer,
            viewModel = viewModel,
            onDismiss = { showSmsLogsBottomSheet = false }
        )
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Slate600, fontSize = 13.sp)
        Text(text = value, color = Slate900, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}
