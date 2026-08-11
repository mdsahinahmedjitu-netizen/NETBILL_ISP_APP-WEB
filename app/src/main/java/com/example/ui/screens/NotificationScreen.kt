package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import com.example.ui.theme.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.AppTranslation
import com.example.viewmodel.MainViewModel

@Composable
fun NotificationScreen(viewModel: MainViewModel) {
    val overdueCustomers by viewModel.expiringTomorrowCustomers.collectAsState()
    val totalOverdueAmount = remember(overdueCustomers) { overdueCustomers.sumOf { it.currentDue } }

    var billReminderText by remember {
        mutableStateOf("প্রিয় {NAME}, আপনার NetBill ইন্টারনেট বিল ৳{AMOUNT} টাকা বকেয়া রয়েছে। অনুগ্রহ করে bKash/Nagad এ পরিশোধ করুন। ধন্যবাদ!")
    }
    var paymentReceivedText by remember {
        mutableStateOf("ধন্যবাদ {NAME}! আপনার ৳{AMOUNT} টাকা ইন্টারনেট বিল পরিশোধ সফল হয়েছে। Receipt #: {RECEIPT_NO}")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SleekBg)
            .padding(16.dp)
    ) {
        Text(
            text = AppTranslation("sms_notifications"),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Slate900
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // --- 20TH-DAY BILLING DEADLINE ALERTS SECTION ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = AppTranslation("day_20_deadline_alert_title"),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = AppTranslation("day_20_deadline_alert_desc"),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = AppTranslation("overdue_20th_count"),
                                        fontSize = 11.sp,
                                        color = Slate600
                                    )
                                    Text(
                                        text = "${overdueCustomers.size} Customers",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = AppTranslation("overdue_20th_amount"),
                                        fontSize = 11.sp,
                                        color = Slate600
                                    )
                                    Text(
                                        text = "৳ ${totalOverdueAmount.toInt()}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFE11D48)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.sendMass20thDaySmsReminders() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(AppTranslation("send_sms_all_20th"), fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = { viewModel.suspendAll20thDayOverdueCustomers() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF9F1239)),
                                border = BorderStroke(1.dp, Color(0xFFE11D48)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(AppTranslation("suspend_all_20th"), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // --- LIST OF OVERDUE CUSTOMERS CROSSING 20TH DAY DEADLINE ---
            if (overdueCustomers.isNotEmpty()) {
                item {
                    Text(
                        text = "Alerted Customers (${overdueCustomers.size})",
                        fontWeight = FontWeight.Bold,
                        color = Slate800,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }

                items(overdueCustomers, key = { it.id }) { customer ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SleekCard),
                        border = BorderStroke(1.dp, Color(0xFFFECDD3))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFFFF1F2)
                                    ) {
                                        Text(
                                            text = customer.customerCode,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE11D48),
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = customer.name,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate900,
                                        fontSize = 14.sp
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFFFE4E6)
                                ) {
                                    Text(
                                        text = AppTranslation("deadline_status_active"),
                                        color = Color(0xFF9F1239),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Package: ${customer.packageName}", fontSize = 12.sp, color = Slate600)
                                    Text("Mobile: ${customer.mobile}", fontSize = 12.sp, color = Slate600)
                                    Text("Zone: ${customer.zone} (Joined day ${customer.joinDayOfMonth})", fontSize = 11.sp, color = Slate500)
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Due Amount", fontSize = 11.sp, color = Slate500)
                                    Text(
                                        text = "৳ ${customer.currentDue.toInt()}",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFE11D48),
                                        fontSize = 16.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { viewModel.dismiss20thDayAlert(customer.id) }
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp), tint = Slate500)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(AppTranslation("dismiss_alert"), fontSize = 11.sp, color = Slate500)
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                OutlinedButton(
                                    onClick = { viewModel.toggleCustomerStatus(customer) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE11D48)),
                                    border = BorderStroke(1.dp, Color(0xFFFECDD3)),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(if (customer.status == "Active") "Suspend" else "Activate", fontSize = 11.sp)
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = {
                                        viewModel.showToast("SMS Reminder Sent to ${customer.name} (${customer.mobile})")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Teal600),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("SMS Reminder", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // --- SMS GATEWAY & TEMPLATE CARDS ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekCard),
                    border = BorderStroke(1.dp, SleekBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Sms, contentDescription = null, tint = Teal600)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Bangladeshi SMS Gateway Configuration", fontWeight = FontWeight.Bold, color = Slate900, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Active Provider: Greenweb / Alpha SMS BD", color = Slate600, fontSize = 12.sp)
                        Text("SMS Balance: 4,820 SMS Remaining", color = Teal600, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekCard),
                    border = BorderStroke(1.dp, SleekBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Bill Due Reminder Template (Bangla)", fontWeight = FontWeight.Bold, color = Teal600, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = billReminderText,
                            onValueChange = { billReminderText = it },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { viewModel.showToast("Mass SMS Due Reminders Sent to 142 Due Customers!") },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Send Mass SMS Reminders", fontSize = 12.sp)
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekCard),
                    border = BorderStroke(1.dp, SleekBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Payment Confirmation Template (Bangla)", fontWeight = FontWeight.Bold, color = Teal600, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = paymentReceivedText,
                            onValueChange = { paymentReceivedText = it },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }
                }
            }
        }
    }
}
