package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.CustomerEntity
import com.example.viewmodel.MainViewModel
import com.example.ui.theme.*

@Composable
fun CustomerFullProfileScreen(
    viewModel: MainViewModel,
    customerId: String,
    onBack: () -> Unit
) {
    val customers by viewModel.customersList.collectAsState()
    val customer = customers.find { it.id == customerId }

    if (customer == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.PersonOff, null, modifier = Modifier.size(100.dp), tint = Color.Gray.copy(alpha = 0.3f))
                Text("SUBSCRIBER NOT FOUND", fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) {
                    Text("GO BACK")
                }
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SleekBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // HEADER
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(48.dp),
            color = SleekCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(56.dp)
                        .background(Slate100, CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.Gray)
                }
                Spacer(Modifier.width(20.dp))
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = RoundedCornerShape(32.dp),
                    color = Teal600
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Badge, null, tint = Color.White, modifier = Modifier.size(40.dp))
                    }
                }
                Spacer(Modifier.width(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(customer.name.uppercase(), fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
                    Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(color = Teal600.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Teal600.copy(alpha = 0.2f))) {
                            Text("#${customer.customerCode}", modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Black, color = Teal600)
                        }
                        Surface(
                            color = if (customer.status == "Active") EmeraldSuccess.copy(alpha = 0.1f) else CoralWarning.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (customer.status == "Active") EmeraldSuccess.copy(alpha = 0.2f) else CoralWarning.copy(alpha = 0.2f))
                        ) {
                            Text(customer.status.uppercase(), modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Black, color = if (customer.status == "Active") EmeraldSuccess else CoralWarning)
                        }
                    }
                }
            }
        }

        // IDENTITY & NETWORK SECTION
        SectionTitle(title = "Identity & Network", icon = Icons.Default.Fingerprint, color = Color(0xFF4F46E5))
        
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoBlock(label = "Full Name", value = customer.name, icon = Icons.Default.Person, color = Color(0xFF4F46E5), modifier = Modifier.weight(1f))
                InfoBlock(label = "Primary Mobile", value = customer.mobile, icon = Icons.Default.Phone, color = EmeraldSuccess, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoBlock(label = "PPPoE Username", value = customer.pppoeUsername, icon = Icons.Default.SettingsInputComponent, color = Color(0xFF2563EB), modifier = Modifier.weight(1f))
                InfoBlock(label = "PPPoE Password", value = customer.pppoePassword, icon = Icons.Default.Key, color = Color.Red, modifier = Modifier.weight(1f))
            }
            InfoBlock(label = "Permanent Address", value = customer.address, icon = Icons.Default.LocationOn, color = Color.Red)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoBlock(label = "ONU Serial", value = customer.onuSerial, icon = Icons.Default.Memory, color = Color(0xFF4F46E5), modifier = Modifier.weight(1f))
                InfoBlock(label = "ONU MAC", value = customer.onuMac, icon = Icons.Default.CastConnected, color = Color(0xFF4F46E5), modifier = Modifier.weight(1f))
            }
        }

        // FINANCIAL & LOGISTICS SECTION
        SectionTitle(title = "Financial & Logistics", icon = Icons.Default.AccountBalanceWallet, color = EmeraldSuccess)
        
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoBlock(label = "Subscription Plan", value = customer.packageName, icon = Icons.Default.Wifi, color = Teal600, modifier = Modifier.weight(1f))
                InfoBlock(label = "Monthly Bill", value = "৳ ${customer.monthlyBill}", icon = Icons.Default.Receipt, color = EmeraldSuccess, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoBlock(label = "Current Due", value = "৳ ${customer.currentDue.toInt()}", icon = Icons.Default.ReportProblem, color = Color.Red, modifier = Modifier.weight(1f))
                InfoBlock(label = "Join Date", value = customer.joinDate, icon = Icons.Default.CalendarToday, color = EmeraldSuccess, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoBlock(label = "Assigned Zone", value = customer.zone, icon = Icons.Default.Map, color = Color.Red, modifier = Modifier.weight(1f))
                InfoBlock(label = "Distribution Box", value = customer.boxId, icon = Icons.Default.Inventory2, color = Color(0xFFEA580C), modifier = Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            shape = RoundedCornerShape(40.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
        ) {
            Text("CLOSE PROFILE", fontWeight = FontWeight.Black, letterSpacing = 8.sp, fontSize = 18.sp)
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun InfoBlock(
    label: String,
    value: String?,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(40.dp),
        color = SleekCard,
        shadowElevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = Slate100
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(label.uppercase(), fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.Gray, letterSpacing = 2.sp)
                Text((value ?: "---").uppercase(), fontSize = 14.sp, fontWeight = FontWeight.Black, color = color, letterSpacing = (-0.5).sp)
            }
        }
    }
}

@Composable
fun SectionTitle(
    title: String,
    icon: ImageVector,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(12.dp),
            color = color
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(16.dp))
        Text(title.uppercase(), fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
    }
}
