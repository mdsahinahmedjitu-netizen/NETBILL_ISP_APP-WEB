package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.CustomerEntity
import com.example.data.entity.SmsLogEntity
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerSmsLogsBottomSheet(
    customer: CustomerEntity,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val smsLogs by viewModel.smsLogsList.collectAsState()

    // Filter SMS logs for this specific customer
    val customerLogs = remember(smsLogs, customer) {
        smsLogs.filter { log ->
            (log.customerId == customer.id) ||
            log.customerCode.equals(customer.customerCode, ignoreCase = true) ||
            (log.mobile == customer.mobile)
        } // Already sorted by sentTimestamp in DAO
    }

    val lastSms = customerLogs.firstOrNull()

    var showSendSmsDialog by remember { mutableStateOf(value = false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SleekBg,
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp),
                shape = RoundedCornerShape(2.dp),
                color = Slate200,
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = ElectricBlue.copy(alpha = 0.15f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Sms,
                                contentDescription = null,
                                tint = ElectricBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = customer.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Slate900
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Slate200
                            ) {
                                Text(
                                    text = customer.customerCode,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate800,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Mobile: ${customer.mobile} • Zone: ${customer.zone}",
                            fontSize = 12.sp,
                            color = Slate600
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Slate600
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // LAST SENT SMS STATUS CARD HIGHLIGHT
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (lastSms?.status == "Failed") Color(0xFFFFF1F2) else Color(0xFFECFDF5)
                ),
                border = BorderStroke(
                    1.dp,
                    if (lastSms?.status == "Failed") Color(0xFFFECDD3) else Color(0xFFA7F3D0)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LAST SENT SMS STATUS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (lastSms?.status == "Failed") Color(0xFFE11D48) else Teal600,
                            letterSpacing = 0.5.sp
                        )

                        lastSms?.let { sms ->
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (sms.status == "Sent") Teal600 else Color(0xFFE11D48)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (sms.status == "Sent") Icons.Default.CheckCircle else Icons.Default.Error,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = sms.status.uppercase(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (lastSms != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Type: ${lastSms.notificationType}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = Slate500,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = lastSms.sentTimestamp,
                                    fontSize = 11.sp,
                                    color = Slate600
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = lastSms.message,
                            fontSize = 12.sp,
                            color = Slate800,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Status: ${lastSms.status}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (lastSms.status == "Failed") Color(0xFFE11D48) else Teal600
                        )

                        if (lastSms.status == "Failed") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.resendFailedSms(lastSms) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Resend Last Failed SMS", fontSize = 11.sp)
                            }
                        }
                    } else {
                        Text(
                            text = "No previous SMS records found for this customer.",
                            fontSize = 12.sp,
                            color = Slate600
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Row: Send New Custom SMS Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SMS Delivery History (${customerLogs.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Slate900
                )

                Button(
                    onClick = { showSendSmsDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Teal600),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Send SMS", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Customer SMS Log Timeline / List
            if (customerLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.MarkEmailRead,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No SMS notifications logged for ${customer.name}",
                            fontSize = 12.sp,
                            color = Slate500
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(customerLogs, key = { it.id }) { item ->
                        CustomerSmsItemCard(
                            log = item,
                        ) { viewModel.resendFailedSms(item) }
                    }
                }
            }
        }
    }

    if (showSendSmsDialog) {
        SendCustomerSingleSmsDialog(
            customer = customer,
            viewModel = viewModel,
        ) { showSendSmsDialog = false }
    }
}

@Composable
fun CustomerSmsItemCard(
    log: SmsLogEntity,
    onResend: () -> Unit
) {
    val isDelivered = log.status == "Sent"
    val isFailed = log.status == "Failed"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SleekCard),
        border = BorderStroke(1.dp, if (isFailed) Color(0xFFFECDD3) else SleekBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = ElectricBlue.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = log.notificationType,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricBlue,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isDelivered) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        tint = if (isDelivered) Teal600 else Color(0xFFE11D48),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = log.status,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDelivered) Teal600 else Color(0xFFE11D48)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = log.message,
                fontSize = 12.sp,
                color = Slate800
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Timestamp: ${log.sentTimestamp}",
                        fontSize = 10.sp,
                        color = Slate500
                    )
                    Text(
                        text = "Notification Type: ${log.notificationType}",
                        fontSize = 10.sp,
                        color = if (isFailed) Color(0xFFE11D48) else Teal600,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isFailed) {
                    IconButton(
                        onClick = onResend,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Resend SMS",
                            tint = Color(0xFFE11D48),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SendCustomerSingleSmsDialog(
    customer: CustomerEntity,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
) {
    var notificationType by remember { mutableStateOf("Billing Alert") }
    var smsText by remember {
        mutableStateOf("প্রিয় ${customer.name}, আপনার NetBill ইন্টারনেট বিল ৳${customer.currentDue.toInt()} টাকা বকেয়া রয়েছে। পরিশোধের জন্য bKash/Nagad ব্যবহার করুন।")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Sms, contentDescription = null, tint = Teal600)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Send SMS to ${customer.name}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Select Notification Type:", fontSize = 12.sp, color = Slate600)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Billing Alert", "Support Update", "20th Day Reminder").forEach { type ->
                        val isSel = notificationType == type
                        FilterChip(
                            selected = isSel,
                            onClick = {
                                notificationType = type
                                smsText = when (type) {
                                    "Billing Alert" -> "প্রিয় ${customer.name}, আপনার NetBill ইন্টারনেট বিল ৳${customer.currentDue.toInt()} টাকা বকেয়া রয়েছে।"
                                    "Support Update" -> "প্রিয় ${customer.name}, আপনার নেটওয়ার্ক অপটিক্যাল ফাইবার সংযোগ সক্রিয় রয়েছে।"
                                    "20th Day Reminder" -> "জরুরী নোটিশ: প্রিয় ${customer.name}, বকেয়া বিল ৳${customer.currentDue.toInt()} টাকা আজই প্রদান করুন।"
                                    else -> smsText
                                }
                            },
                            label = { Text(type, fontSize = 10.sp) }
                        )
                    }
                }

                Text("SMS Message:", fontSize = 12.sp, color = Slate600)
                OutlinedTextField(
                    value = smsText,
                    onValueChange = { smsText = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.sendSingleSmsNotification(
                        id = customer.id,
                        code = customer.customerCode,
                        name = customer.name,
                        mobile = customer.mobile,
                        type = notificationType,
                        msg = smsText
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Teal600)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Send Now", fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Slate600)
            }
        }
    )
}
