package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.AppTranslation
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.ReportsViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ReportsScreen(
    mainViewModel: MainViewModel, 
    reportsViewModel: ReportsViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val stats by reportsViewModel.reportStats.collectAsState()
    val currency = AppTranslation("currency_symbol")

    var activeTab by remember { mutableStateOf("COLLECTION") }
    var searchTerm by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SleekBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Back Button Row
        Row(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(52.dp)
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .border(1.dp, SleekBorder, RoundedCornerShape(16.dp))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = IspIndigo)
            }
        }

        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(44.dp))
                .border(1.dp, SleekBorder, RoundedCornerShape(44.dp))
                .padding(28.dp)
        ) {
            Text(
                text = AppTranslation("reports_analytics").uppercase(),
                fontWeight = FontWeight.Black,
                fontSize = 32.sp,
                letterSpacing = 2.sp,
                color = Slate900
            )
            Text(
                text = "FINANCIAL INTELLIGENCE • REAL-TIME AUDIT",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                color = IspTealPrimary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // 4-Stat Card Row
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ReportStatCard(label = "TOTAL CUSTOMERS", value = "1,248", icon = Icons.Default.People, color = IspIndigo, modifier = Modifier.width(160.dp))
            ReportStatCard(label = "COLLECTED", value = "$currency${stats.totalRevenue.toInt()}", icon = Icons.Default.TrendingUp, color = EmeraldSuccess, modifier = Modifier.width(160.dp))
            ReportStatCard(label = "DUE BALANCE", value = "$currency${stats.totalOutstanding.toInt()}", icon = Icons.Default.Warning, color = IspRose, modifier = Modifier.width(160.dp))
            ReportStatCard(label = "TOTAL BILL", value = "$currency${stats.totalRevenue.toInt() + stats.totalOutstanding.toInt()}", icon = Icons.Default.Receipt, color = Color(0xFF3B82F6), modifier = Modifier.width(160.dp))
        }

        // Tabs
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = Color(0xFFF1F5F9),
            shadowElevation = 4.dp
        ) {
            Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("COLLECTION", "DUE LIST", "REVENUE").forEach { tab ->
                    val isSelected = activeTab == tab
                    val tabColor = when (tab) {
                        "COLLECTION" -> IspTealPrimary
                        "DUE LIST" -> IspRose
                        else -> Color(0xFF3B82F6)
                    }
                    Surface(
                        onClick = { activeTab = tab },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = if (isSelected) Color.White else Color.Transparent,
                        shadowElevation = if (isSelected) 4.dp else 0.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = tab, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = if (isSelected) tabColor else Color.Gray)
                        }
                    }
                }
            }
        }

        // Content Table
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(44.dp),
            color = Color.White,
            border = BorderStroke(1.dp, SleekBorder),
            shadowElevation = 10.dp
        ) {
            Box {
                val gradient = when (activeTab) {
                    "COLLECTION" -> androidx.compose.ui.graphics.Brush.linearGradient(listOf(IspTealPrimary, IspRose))
                    "DUE LIST" -> androidx.compose.ui.graphics.Brush.linearGradient(listOf(IspRose, Color(0xFFF97316)))
                    else -> androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF3B82F6), IspIndigo))
                }
                Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(gradient))

                Column(modifier = Modifier.padding(24.dp).padding(top = 16.dp)) {
                    Text("${activeTab} ANALYSIS", fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = 2.sp, color = Slate900)

                    Spacer(modifier = Modifier.height(24.dp))

                    when (activeTab) {
                        "COLLECTION" -> {
                            Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                Column {
                                    ReportTableHeader(listOf("#", "ID", "SUBSCRIBER", "METHOD", "AMOUNT"))
                                    stats.zoneReports.take(10).forEachIndexed { index, zone ->
                                        ReportTableRow(listOf((index + 1).toString(), "#1234", zone.zoneName.uppercase(), "CASH", "${currency}${zone.monthlyRevenue.toInt()}"))
                                    }
                                }
                            }
                        }
                        "DUE LIST" -> {
                            Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                Column {
                                    ReportTableHeader(listOf("SUBSCRIBER", "ZONE", "BILL", "DUE"))
                                    ReportTableRow(listOf("JITU ONLINE", "UTTARA", "${currency}800", "${currency}1600"))
                                }
                            }
                        }
                        "REVENUE" -> {
                            stats.zoneReports.forEach { zone ->
                                ZoneRevenueProgress(label = zone.zoneName.uppercase(), amount = zone.monthlyRevenue.toInt().toString(), count = zone.customerCount.toString(), currency = currency)
                                Spacer(modifier = Modifier.height(20.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun ReportStatCard(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(32.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SleekBorder),
        shadowElevation = 6.dp
    ) {
        Box(modifier = Modifier.padding(20.dp)) {
            Icon(icon, contentDescription = null, tint = color.copy(alpha = 0.1f), modifier = Modifier.size(48.dp).align(Alignment.TopEnd))
            Column {
                Text(text = label, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Slate400)
                Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.Black, color = color, letterSpacing = 2.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
fun ReportTableHeader(headers: List<String>) {
    Row(modifier = Modifier.background(Color(0xFFF8FAFC)).padding(vertical = 12.dp)) {
        headers.forEach { header ->
            Text(text = header, modifier = Modifier.width(120.dp), fontSize = 10.sp, fontWeight = FontWeight.Black, color = Slate400, textAlign = TextAlign.Center, letterSpacing = 2.sp)
        }
    }
    HorizontalDivider(color = SleekBorder)
}

@Composable
fun ReportTableRow(cells: List<String>) {
    Row(modifier = Modifier.padding(vertical = 16.dp)) {
        cells.forEach { cell ->
            Text(text = cell, modifier = Modifier.width(120.dp), fontSize = 13.sp, fontWeight = FontWeight.Black, color = Slate800, textAlign = TextAlign.Center, letterSpacing = 1.sp)
        }
    }
    HorizontalDivider(color = SleekBorder.copy(alpha = 0.3f))
}

@Composable
fun ZoneRevenueProgress(label: String, amount: String, count: String, currency: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Slate800, letterSpacing = 2.sp)
            Text("$currency $amount", fontSize = 16.sp, fontWeight = FontWeight.Black, color = IspTealPrimary, letterSpacing = 2.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { 0.7f },
            modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
            color = IspTealPrimary,
            trackColor = Color(0xFFF1F5F9)
        )
        Text("$count SUBSCRIBERS", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Slate400, modifier = Modifier.padding(top = 6.dp), letterSpacing = 2.sp)
    }
}
