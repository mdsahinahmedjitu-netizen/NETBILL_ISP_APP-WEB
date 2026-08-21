package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.MainViewModel
import com.example.ui.theme.*

@Composable
fun SupportTicketsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val tickets by viewModel.supportTickets.collectAsState()
    var activeTab by remember { mutableStateOf("OPEN") }
    var customIssueInput by remember { mutableStateOf("") }

    val filteredTickets = remember(tickets, activeTab) {
        when (activeTab) {
            "ALL" -> tickets
            else -> tickets.filter { it.status.uppercase() == activeTab }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SleekBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(44.dp))
                .border(1.dp, SleekBorder, RoundedCornerShape(44.dp))
                .padding(28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "COMPLAINTS",
                        fontWeight = FontWeight.Black,
                        fontSize = 32.sp,
                        letterSpacing = 2.sp,
                        color = Slate900
                    )
                    Text(
                        text = "SUPPORT SYSTEM • REAL-TIME RESOLUTION",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp,
                        color = IspAmber,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(2.dp, IspIndigo.copy(alpha = 0.1f))
                ) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = customIssueInput,
                            onValueChange = { customIssueInput = it.uppercase() },
                            placeholder = { Text("NEW PRESET...", fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp) },
                            modifier = Modifier.width(100.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent)
                        )
                        Button(onClick = { /* Add */ }, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = IspIndigo), contentPadding = PaddingValues(horizontal = 12.dp)) {
                            Text("ADD", fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        }
                    }
                }
            }
        }

        // Tabs
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = Color(0xFFF1F5F9),
            shadowElevation = 4.dp
        ) {
            Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("OPEN", "PENDING", "RESOLVED", "ALL").forEach { tab ->
                    val isSelected = activeTab == tab
                    Surface(
                        onClick = { activeTab = tab },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = if (isSelected) Color.White else Color.Transparent,
                        shadowElevation = if (isSelected) 4.dp else 0.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = tab, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = if (isSelected) IspAmber else Color.Gray)
                        }
                    }
                }
            }
        }

        // Tickets List
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (filteredTickets.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("NO COMPLAINTS FOUND", color = Slate300, fontWeight = FontWeight.Black, letterSpacing = 8.sp)
                }
            } else {
                filteredTickets.forEach { ticket ->
                    WebTicketCard(ticket = ticket, onUpdate = { viewModel.updateSupportTicket(it) })
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun WebTicketCard(ticket: com.example.data.entity.SupportTicketEntity, onUpdate: (com.example.data.entity.SupportTicketEntity) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(44.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SleekBorder),
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier.padding(28.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Left Side: Subscriber Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(32.dp))
                    .border(2.dp, SleekBorder, RoundedCornerShape(32.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("SUBSCRIBER", fontSize = 10.sp, fontWeight = FontWeight.Black, color = IspIndigo, letterSpacing = 4.sp)
                Text(text = ticket.customerName.uppercase(), fontWeight = FontWeight.Black, fontSize = 28.sp, color = Slate900, textAlign = TextAlign.Center, lineHeight = 30.sp, letterSpacing = 1.sp)
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ZONE", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Slate400, letterSpacing = 2.sp)
                        Text("GLOBAL", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Slate700, letterSpacing = 1.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PHONE", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Slate400, letterSpacing = 2.sp)
                        Text("017XXXXXX", fontSize = 14.sp, fontWeight = FontWeight.Black, color = EmeraldSuccess, letterSpacing = 1.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                if (ticket.status != "Resolved") {
                    Button(
                        onClick = { onUpdate(ticket.copy(status = "Resolved")) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                    ) {
                        Text("RESOLVE", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    }
                } else {
                    Button(
                        onClick = { onUpdate(ticket.copy(status = "Open")) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate800)
                    ) {
                        Text("RE-OPEN", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    }
                }
            }

            // Right Side: Ticket Details
            Column(modifier = Modifier.weight(2f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = if (ticket.status == "Resolved") Color(0xFFF0FDF4) else Color(0xFFFFF7ED),
                        border = BorderStroke(2.dp, if (ticket.status == "Resolved") Color(0xFFDCFCE7) else Color(0xFFFEE2E2))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (ticket.status == "Resolved") Icons.Default.CheckCircle else Icons.Default.Headset,
                                contentDescription = null,
                                tint = if (ticket.status == "Resolved") EmeraldSuccess else IspAmber,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFFEE2E2), border = BorderStroke(2.dp, Color(0xFFFECACA))) {
                                Text("NORMAL PRIORITY", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color(0xFFDC2626), letterSpacing = 2.sp)
                            }
                            Text("#${ticket.id.takeLast(6)}", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Slate400, letterSpacing = 2.sp)
                        }
                        Text(text = ticket.issueType.uppercase(), fontWeight = FontWeight.Black, fontSize = 22.sp, color = Slate900, letterSpacing = 1.sp)
                    }
                }

                Text(text = ticket.description, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Slate500, lineHeight = 20.sp, letterSpacing = 1.sp)
                
                HorizontalDivider(color = SleekBorder.copy(alpha = 0.5f))
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("LOGGED: 20-AUG-2026", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Slate600, letterSpacing = 2.sp)
                    Text("LOGGED BY: ADMIN", fontSize = 9.sp, fontWeight = FontWeight.Black, color = IspIndigo, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, letterSpacing = 2.sp)
                }
            }
        }
    }
}
