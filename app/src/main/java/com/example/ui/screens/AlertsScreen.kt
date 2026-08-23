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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.MainViewModel
import com.example.localization.AppTranslation

@Composable
fun AlertsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit = {}
) {
    val overdueCustomers by viewModel.expiringTomorrowCustomers.collectAsState()
    val supportTickets by viewModel.supportTickets.collectAsState()
    val totalOverdueAmount = remember(overdueCustomers) { overdueCustomers.sumOf { it.currentDue } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SleekBg)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "সিস্টেম অ্যালার্ট ও নোটিফিকেশন",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Slate900
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            
            // --- PENDING SUPPORT TICKETS SECTION ---
            val pendingTickets = supportTickets.filter { it.status != "Resolved" }
            if (pendingTickets.isNotEmpty()) {
                item {
                    SectionHeader(title = "পেন্ডিং সাপোর্ট টিকেটসমূহ (${pendingTickets.size})", icon = Icons.Default.ReportProblem, color = CoralWarning)
                }
                items(pendingTickets) { ticket ->
                    SupportTicketAlertCard(ticket = ticket, onResolve = { viewModel.updateSupportTicket(it) })
                }
            }

            // --- 20TH-DAY BILLING DEADLINE ALERTS SECTION ---
            if (overdueCustomers.isNotEmpty()) {
                item {
                    SectionHeader(title = "মেয়াদ শেষ হওয়ার সতর্কতা (${overdueCustomers.size})", icon = Icons.Default.NotificationsActive, color = Color(0xFFE11D48))
                }
                
                item {
                    ExpirySummaryCard(
                        overdueCount = overdueCustomers.size, 
                        totalAmount = totalOverdueAmount,
                        onSendWhatsApp = { viewModel.sendWhatsAppExpiryAlerts() }
                    )
                }

                items(overdueCustomers) { customer ->
                    CustomerExpiryAlertCard(
                        customer = customer,
                        onDismiss = { viewModel.dismiss20thDayAlert(it) },
                        onToggleStatus = { viewModel.toggleCustomerStatus(it) },
                        onSendSms = { id, code, name, mobile, type, msg ->
                            viewModel.sendSingleSmsNotification(id, code, name, mobile, type, msg)
                        }
                    )
                }
            }

            if (pendingTickets.isEmpty() && overdueCustomers.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(64.dp), tint = EmeraldSuccess.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("সব কিছু ঠিক আছে! কোনো নতুন অ্যালার্ট নেই।", color = Slate500, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, fontWeight = FontWeight.Bold, color = Slate900, fontSize = 16.sp)
    }
}

@Composable
fun SupportTicketAlertCard(ticket: com.example.data.entity.SupportTicketEntity, onResolve: (com.example.data.entity.SupportTicketEntity) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SleekCard),
        border = BorderStroke(1.dp, CoralWarning.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(6.dp), color = ElectricBlue.copy(alpha = 0.1f)) {
                        Text(text = ticket.customerCode, fontWeight = FontWeight.Bold, color = ElectricBlue, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = ticket.customerName, fontWeight = FontWeight.Bold, color = Slate900, fontSize = 14.sp)
                }
                Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if(ticket.status == "Pending") CoralWarning else ElectricBlue).padding(horizontal = 8.dp, vertical = 3.dp)) {
                    Text(text = ticket.status, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Issue: ${ticket.issueType}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Slate800)
            Text(text = ticket.description, fontSize = 12.sp, color = Slate600)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "📅 ${ticket.createdAt}", fontSize = 10.sp, color = Slate500)
                Button(
                    onClick = { onResolve(ticket.copy(status = "Resolved")) },
                    colors = ButtonDefaults.buttonColors(containerColor = Teal600),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mark Resolved", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun ExpirySummaryCard(overdueCount: Int, totalAmount: Double, onSendWhatsApp: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFE11D48).copy(alpha = 0.3f))) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "মেয়াদোত্তীর্ণ গ্রাহক", fontSize = 11.sp, color = Slate600)
                    Text(text = "$overdueCount জন", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE11D48))
                }
            }
            Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFE11D48).copy(alpha = 0.3f))) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "মোট বকেয়া", fontSize = 11.sp, color = Slate600)
                    Text(text = "৳ ${totalAmount.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE11D48))
                }
            }
        }
        
        Button(
            onClick = onSendWhatsApp,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("এডমিন ও স্টাফকে WhatsApp এলার্ট দিন", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CustomerExpiryAlertCard(
    customer: com.example.data.entity.CustomerEntity,
    onDismiss: (String) -> Unit,
    onToggleStatus: (com.example.data.entity.CustomerEntity) -> Unit,
    onSendSms: (String, String, String, String, String, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SleekCard),
        border = BorderStroke(1.dp, Color(0xFFFECDD3))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFFFF1F2)) {
                        Text(text = customer.customerCode, fontWeight = FontWeight.Bold, color = Color(0xFFE11D48), fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = customer.name, fontWeight = FontWeight.Bold, color = Slate900, fontSize = 14.sp)
                }
                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFFFE4E6)) {
                    Text(text = "মেয়াদ শেষ", color = Color(0xFF9F1239), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Package: ${customer.packageName}", fontSize = 12.sp, color = Slate600)
                    Text("Mobile: ${customer.mobile}", fontSize = 12.sp, color = Slate600)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("বকেয়া টাকা", fontSize = 11.sp, color = Slate500)
                    Text(text = "৳ ${customer.currentDue.toInt()}", fontWeight = FontWeight.ExtraBold, color = Color(0xFFE11D48), fontSize = 16.sp)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { onDismiss(customer.id) }) {
                    Text("বাতিল করুন", fontSize = 11.sp, color = Slate500)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { onToggleStatus(customer) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE11D48)),
                    border = BorderStroke(1.dp, Color(0xFFFECDD3)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(if (customer.status == "Active") "বন্ধ করুন" else "চালু করুন", fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        onSendSms(customer.id, customer.customerCode, customer.name, customer.mobile, "20th Day Reminder", "প্রিয় ${customer.name}, আপনার NetBill বকেয়া ৳${customer.currentDue.toInt()} পরিশোধ করার অনুরোধ করা হচ্ছে।")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Teal600),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("এসএমএস দিন", fontSize = 11.sp)
                }
            }
        }
    }
}
