package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.viewmodel.MainViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportTicketsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val tickets by viewModel.supportTickets.collectAsState()
    var selectedFilter by remember { mutableStateOf("All") }

    val filteredTickets = remember(tickets, selectedFilter) {
        if (selectedFilter == "All") tickets else tickets.filter { it.status == selectedFilter }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("সাপোর্ট টিকিট (Complaints)", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = SleekBg
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            // Filter Chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "Pending", "Resolved").forEach { status ->
                    FilterChip(
                        selected = selectedFilter == status,
                        onClick = { selectedFilter = status },
                        label = { Text(status) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredTickets.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("কোন অভিযোগ পাওয়া যায়নি", color = Slate400)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(filteredTickets) { ticket ->
                        TicketItemCard(ticket = ticket, onUpdate = { viewModel.updateSupportTicket(it) })
                    }
                }
            }
        }
    }
}

@Composable
fun TicketItemCard(ticket: com.example.data.entity.SupportTicketEntity, onUpdate: (com.example.data.entity.SupportTicketEntity) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(ticket.customerName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(ticket.status, color = if(ticket.status == "Resolved") EmeraldSuccess else CoralWarning, fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
            Text("Issue: ${ticket.issueType}", fontSize = 14.sp, color = Slate700)
            Text(ticket.description, fontSize = 12.sp, color = Slate500)
            Spacer(modifier = Modifier.height(8.dp))
            if (ticket.status != "Resolved") {
                Button(
                    onClick = { onUpdate(ticket.copy(status = "Resolved")) },
                    colors = ButtonDefaults.buttonColors(containerColor = Teal600)
                ) {
                    Text("Mark Resolved", fontSize = 12.sp)
                }
            }
        }
    }
}
