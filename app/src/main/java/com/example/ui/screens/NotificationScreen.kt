package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import com.example.ui.theme.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.CustomerEntity
import com.example.data.entity.SmsLogEntity
import com.example.data.entity.SupportTicketEntity
import com.example.localization.appTranslation
import com.example.ui.components.CustomerSmsLogsBottomSheet
import com.example.viewmodel.MainViewModel

@Composable
fun NotificationScreen(
    viewModel: MainViewModel,
    onNavigateToTemplates: () -> Unit = {}
) {
    val allCustomers by viewModel.customersList.collectAsState()
    val smsLogs by viewModel.smsLogsList.collectAsState()

    var statusFilter by remember { mutableStateOf("All") }
    var typeFilter by remember { mutableStateOf("All") }

    var showSupportSmsModal by remember { mutableStateOf(false) }
    var selectedCustomerForLogs by remember { mutableStateOf<CustomerEntity?>(null) }

    val totalSentCount = smsLogs.size
    val deliveredCount = remember(smsLogs) { smsLogs.count { it.status == "Sent" } }
    val failedCount = remember(smsLogs) { smsLogs.count { it.status == "Failed" } }
    val successRate = remember(smsLogs) {
        if (smsLogs.isNotEmpty()) (deliveredCount.toDouble() / smsLogs.size * 100).toInt() else 100
    }

    val filteredLogs = remember(smsLogs, statusFilter, typeFilter) {
        smsLogs.filter { log ->
            val matchStatus = when (statusFilter) {
                "Sent" -> log.status == "Sent"
                "Failed" -> log.status.startsWith("Failed")
                else -> true
            }
            val matchType = when (typeFilter) {
                "Billing Alert" -> log.notificationType.contains("Bill", ignoreCase = true) || log.notificationType.contains("Collection", ignoreCase = true)
                "Support Update" -> log.notificationType.contains("Support", ignoreCase = true) || log.notificationType.contains("Complain", ignoreCase = true)
                "Payment Receipt" -> log.notificationType.contains("Receipt", ignoreCase = true) || log.notificationType.contains("Collection", ignoreCase = true)
                "20th Day Reminder" -> log.notificationType.contains("20th", ignoreCase = true) || log.notificationType.contains("Expired", ignoreCase = true)
                else -> true
            }
            matchStatus && matchType
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SleekBg)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appTranslation("sms_notifications"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Text(
                    text = appTranslation("sms_tracker_desc"),
                    fontSize = 11.sp,
                    color = Slate600,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = onNavigateToTemplates,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Teal600),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = Teal600, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SMS Templates", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Teal600)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Teal600.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, Teal600.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Teal600, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Greenweb Gateway",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Teal600
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {

            // --- AUTOMATED SMS STATUS TRACKER SUMMARY CARD ---
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = ElectricBlue.copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.MarkEmailRead,
                                            contentDescription = null,
                                            tint = ElectricBlue,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = appTranslation("sms_tracker_title"),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Slate900
                                    )
                                    Text(
                                        text = "Automated Billing & Support Delivery Reports",
                                        fontSize = 11.sp,
                                        color = Slate500
                                    )
                                }
                            }

                            IconButton(onClick = { viewModel.clearSmsLogs() }) {
                                Icon(
                                    Icons.Default.DeleteSweep,
                                    contentDescription = appTranslation("clear_logs"),
                                    tint = Slate500,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Metric Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = Slate100,
                                border = BorderStroke(1.dp, SleekBorder)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(appTranslation("total_sent_sms"), fontSize = 9.sp, color = Slate600)
                                    Text("$totalSentCount", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Slate900)
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFECFDF5),
                                border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(appTranslation("status_delivered"), fontSize = 9.sp, color = Teal600)
                                    Text("$deliveredCount", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Teal600)
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFFF1F2),
                                border = BorderStroke(1.dp, Color(0xFFFECDD3))
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(appTranslation("status_failed"), fontSize = 9.sp, color = Color(0xFFE11D48))
                                    Text("$failedCount", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE11D48))
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFEFF6FF),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(appTranslation("delivered_rate"), fontSize = 9.sp, color = ElectricBlue)
                                    Text("$successRate%", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ElectricBlue)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showSupportSmsModal = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Teal600),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(appTranslation("send_support_sms"), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }

                            if (failedCount > 0) {
                                OutlinedButton(
                                    onClick = { viewModel.resendAllFailedSms() },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE11D48)),
                                    border = BorderStroke(1.dp, Color(0xFFFECDD3)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${appTranslation("resend_failed")} ($failedCount)", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }

            // --- STATUS & TYPE FILTERS FOR TRACKER LOGS ---
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Filter Delivery Status:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate600)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val statusOptions = listOf("All", "Sent", "Failed")
                        items(statusOptions) { status ->
                            val isSel = statusFilter == status
                            FilterChip(
                                selected = isSel,
                                onClick = { statusFilter = status },
                                label = {
                                    val txt = when (status) {
                                        "Sent" -> "Sent (সফল)"
                                        "Failed" -> appTranslation("status_failed")
                                        else -> appTranslation("status_all")
                                    }
                                    Text(txt, fontSize = 11.sp)
                                },
                                leadingIcon = if (isSel) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                                } else null
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text("Filter Notification Type:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate600)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val typeOptions = listOf("All", "Billing Alert", "Support Update", "Payment Receipt", "20th Day Reminder")
                        items(typeOptions) { type ->
                            val isSel = typeFilter == type
                            FilterChip(
                                selected = isSel,
                                onClick = { typeFilter = type },
                                label = {
                                    val txt = when (type) {
                                        "Billing Alert" -> appTranslation("type_billing_alert")
                                        "Support Update" -> appTranslation("type_support_update")
                                        "Payment Receipt" -> appTranslation("type_payment_receipt")
                                        "20th Day Reminder" -> appTranslation("type_20th_reminder")
                                        else -> appTranslation("type_all")
                                    }
                                    Text(txt, fontSize = 11.sp)
                                }
                            )
                        }
                    }
                }
            }

            // --- SMS DELIVERY LOGS CARDS ---
            if (filteredLogs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SleekCard)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No SMS logs found matching selected filters.", color = Slate500, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                items(filteredLogs, key = { it.id }) { log ->
                    SmsDeliveryLogCard(
                        log = log,
                        onResend = { viewModel.resendFailedSms(log) },
                        onViewCustomerLogs = {
                            val target = allCustomers.find { it.id == log.customerId || it.customerCode.equals(log.customerCode, ignoreCase = true) }
                                ?: CustomerEntity(
                                    id = log.customerId,
                                    customerCode = log.customerCode,
                                    name = log.customerName,
                                    mobile = log.mobile,
                                    address = "N/A",
                                    zone = "General",
                                    packageName = "Standard",
                                    monthlyBill = 800.0,
                                    connectionFee = 0.0,
                                    joinDate = "2026-01-01",
                                    billingType = "Postpaid",
                                    currentDue = 0.0,
                                    advanceBalance = 0.0,
                                    status = "Active",
                                    expireDate = "2026-08-25",
                                    expireTime = "11:59 PM",
                                    pppoeUsername = log.customerCode.lowercase(),
                                    pppoePassword = "123"
                                )
                            selectedCustomerForLogs = target
                        }
                    )
                }
            }

        }
    }

    if (showSupportSmsModal) {
        SendSupportUpdateSmsDialog(
            viewModel = viewModel,
            onDismiss = { showSupportSmsModal = false }
        )
    }

    selectedCustomerForLogs?.let { customer ->
        CustomerSmsLogsBottomSheet(
            customer = customer,
            viewModel = viewModel,
            onDismiss = { selectedCustomerForLogs = null }
        )
    }
}

@Composable
fun SmsDeliveryLogCard(
    log: SmsLogEntity,
    onResend: () -> Unit,
    onViewCustomerLogs: (() -> Unit)? = null
) {
    val isDelivered = log.status == "Sent"
    val isFailed = log.status == "Failed"

    val statusBg = if (isDelivered) Color(0xFFECFDF5) else Color(0xFFFFF1F2)
    val statusBorder = if (isDelivered) Color(0xFFA7F3D0) else Color(0xFFFECDD3)
    val statusText = if (isDelivered) Teal600 else Color(0xFFE11D48)
    val statusIcon = if (isDelivered) Icons.Default.CheckCircle else Icons.Default.Cancel

    val typeColor = when (log.notificationType) {
        "Billing Alert" -> Color(0xFFD97706)
        "Support Update" -> ElectricBlue
        "Payment Receipt" -> Teal600
        "20th Day Reminder" -> Color(0xFFE11D48)
        else -> Slate800
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SleekCard),
        border = BorderStroke(1.dp, if (isFailed) Color(0xFFFECDD3) else SleekBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = typeColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = log.customerCode,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = typeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = log.customerName,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        fontSize = 14.sp
                    )
                }

                // Delivery Status Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = statusBg,
                    border = BorderStroke(1.dp, statusBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            statusIcon,
                            contentDescription = null,
                            tint = statusText,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isDelivered) "Sent" else appTranslation("status_failed"),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Type and Mobile Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PhoneIphone, contentDescription = null, modifier = Modifier.size(12.dp), tint = Slate500)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(log.mobile, fontSize = 12.sp, color = Slate600, fontWeight = FontWeight.Medium)
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = typeColor.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = log.notificationType,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = typeColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Message Bubble
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Slate100,
                border = BorderStroke(1.dp, SleekBorder)
            ) {
                Text(
                    text = log.message,
                    fontSize = 12.sp,
                    color = Slate800,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer: Sent time & Gateway Report
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = log.sentTimestamp,
                        fontSize = 10.sp,
                        color = Slate500
                    )
                    Text(
                        text = "Status: ${log.status}",
                        fontSize = 10.sp,
                        color = if (isFailed) Color(0xFFE11D48) else Teal600,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (onViewCustomerLogs != null) {
                        OutlinedButton(
                            onClick = onViewCustomerLogs,
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.ManageSearch, contentDescription = null, modifier = Modifier.size(12.dp), tint = Slate700)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Customer Sheet", fontSize = 10.sp, color = Slate700)
                        }
                    }

                    if (isFailed) {
                        Button(
                            onClick = onResend,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(appTranslation("resend_now"), fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SendSupportUpdateSmsDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var selectedZone by remember { mutableStateOf("All") }
    var smsContent by remember {
        mutableStateOf("নেটওয়ার্ক আপডেট: উত্তরা এবং আশেপাশের এলাকায় অপটিক্যাল ফাইবার সচল করা হয়েছে। সহায়তার জন্য কল করুন 01911000000।")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Campaign, contentDescription = null, tint = Teal600)
                Spacer(modifier = Modifier.width(8.dp))
                Text(appTranslation("send_support_sms"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Select Target Zone:", fontSize = 12.sp, color = Slate600)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("All", "Uttara Zone", "Dhanmondi Zone", "Mirpur Zone").forEach { zone ->
                        val isSel = selectedZone == zone
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedZone = zone },
                            label = { Text(zone, fontSize = 10.sp) }
                        )
                    }
                }

                Text("Quick Templates:", fontSize = 12.sp, color = Slate600)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SuggestionChip(
                        onClick = {
                            smsContent = "রক্ষণাবেক্ষণ বার্তা: আপনার এলাকায় আজ রাত ১২টা থেকে ১টা পর্যন্ত ফাইবার অপটিক নেটওয়ার্ক সিস্টেম আপডেট করা হবে।"
                        },
                        label = { Text("Maintenance", fontSize = 10.sp) }
                    )
                    SuggestionChip(
                        onClick = {
                            smsContent = "ধন্যবাদ! নেটওয়ার্ক সমস্যা সমাধান করা হয়েছে এবং সংযোগ সম্পূর্ণ সক্রিয় আছে।"
                        },
                        label = { Text("Restored", fontSize = 10.sp) }
                    )
                }

                Text("Message Content (Bangla / English):", fontSize = 12.sp, color = Slate600)
                OutlinedTextField(
                    value = smsContent,
                    onValueChange = { smsContent = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.sendSupportUpdateSms(
                        id = null,
                        zone = selectedZone,
                        msg = smsContent
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Teal600)
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Send Support Update", fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Slate600)
            }
        }
    )
}
