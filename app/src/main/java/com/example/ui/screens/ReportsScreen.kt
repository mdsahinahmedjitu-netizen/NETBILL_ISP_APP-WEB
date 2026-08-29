package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import java.util.Locale
import com.example.localization.appTranslation
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.ReportsViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ReportsScreen(
    viewModel: MainViewModel,
    reportsViewModel: ReportsViewModel = viewModel(),
    onBack: () -> Unit = {},
) {
    val currentUser by viewModel.currentUser.collectAsState()
    
    LaunchedEffect(currentUser) {
        reportsViewModel.setCurrentUser(currentUser)
    }

    val stats by reportsViewModel.reportStats.collectAsState()
    val currency = appTranslation("currency_symbol")

    var activeTab by remember { mutableStateOf("COLLECTION") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SleekBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
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
                text = appTranslation("reports_analytics").uppercase(),
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
            ReportStatCard(label = "TOTAL CUSTOMERS", value = String.format(Locale.US, "%,d", stats.totalCustomers), icon = Icons.Default.People, color = IspIndigo, modifier = Modifier.width(160.dp))
            ReportStatCard(label = "COLLECTED", value = "$currency${stats.totalRevenue.toInt()}", icon = Icons.AutoMirrored.Filled.TrendingUp, color = EmeraldSuccess, modifier = Modifier.width(160.dp))
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
                    Text("$activeTab ANALYSIS", fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = 2.sp, color = Slate900)

                    Spacer(modifier = Modifier.height(24.dp))

                    when (activeTab) {
                        "COLLECTION" -> {
                            Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                Column {
                                    ReportTableHeader(listOf("#", "DATE", "SUBSCRIBER", "COLLECTOR", "METHOD", "AMOUNT"))
                                    stats.recentPayments.forEachIndexed { index, payment ->
                                        val collectorColor = remember(payment.collectorName) {
                                            getCollectorColor(payment.collectorName)
                                        }
                                        val methodColor = remember(payment.paymentMethod) {
                                            getMethodColor(payment.paymentMethod)
                                        }
                                        ReportTableRow(
                                            cells = listOf(
                                                (index + 1).toString(),
                                                formatDateDisplay(payment.paymentDate),
                                                payment.customerName.uppercase(),
                                                payment.collectorName.uppercase(),
                                                payment.paymentMethod.uppercase(),
                                                "$currency${payment.amount.toInt()}"
                                            ),
                                            cellColors = mapOf(
                                                3 to collectorColor,
                                                4 to methodColor
                                            )
                                        )
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
                        if (stats.zoneReports.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No zone revenue data found.", color = Slate600, fontSize = 13.sp)
                            }
                        } else {
                            stats.zoneReports.forEach { zone ->
                                ZoneRevenueProgress(label = zone.zoneName.uppercase(), amount = zone.monthlyRevenue.toInt().toString(), count = zone.customerCount.toString(), currency = currency)
                                Spacer(modifier = Modifier.height(20.dp))
                            }
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
fun ReportTableRow(cells: List<String>, cellColors: Map<Int, Color> = emptyMap()) {
    Row(modifier = Modifier.padding(vertical = 16.dp)) {
        cells.forEachIndexed { index, cell ->
            Text(
                text = cell,
                modifier = Modifier.width(120.dp),
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = cellColors[index] ?: Slate800,
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp
            )
        }
    }
    HorizontalDivider(color = SleekBorder.copy(alpha = 0.3f))
}

fun getMethodColor(method: String): Color {
    return when (method.uppercase()) {
        "CASH" -> Color(0xFF10B981) // Emerald Success
        "BKASH" -> Color(0xFFE2136E) // bKash Pink
        "NAGAD" -> Color(0xFFF7921E) // Nagad Orange
        else -> Color(0xFF475569) // Slate 600
    }
}

fun formatDateDisplay(dateStr: String?): String {
    if (dateStr.isNullOrEmpty()) return "---"
    val parts = dateStr.split("-")
    return if ((parts.size == 3) && (parts[0].length == 4)) {
        "${parts[2]}-${parts[1]}-${parts[0]}"
    } else {
        dateStr
    }
}

fun getCollectorColor(name: String): Color {
    if (name.isEmpty() || name.equals("Admin / Direct", ignoreCase = true)) return Color(0xFF475569) // Slate 600
    
    // Explicit colors
    if (name.contains("TOMA", ignoreCase = true)) return Color(0xFFEC4899) // Pink 600
    if (name.contains("SUPER ADMIN", ignoreCase = true)) return Color(0xFF10B981) // Emerald 600
    if (name.contains("JITU", ignoreCase = true)) return Color(0xFF2563EB) // Blue 600
    
    // Improved hashing for Android as well
    var hash = 0
    for (char in name) {
        hash = (hash shl 5) - hash + char.code
    }
    
    val colors = listOf(
        Color(0xFF4F46E5), // Indigo
        Color(0xFF0D9488), // Teal
        Color(0xFFF43F5E), // Rose
        Color(0xFFF59E0B), // Amber
        Color(0xFF0284C7), // Blue
        Color(0xFF8B5CF6), // Violet
        Color(0xFFEC4899), // Pink
        Color(0xFFF97316), // Orange
        Color(0xFF06B6D4), // Cyan
        Color(0xFF10B981), // Emerald
        Color(0xFFD946EF)  // Fuchsia
    )
    return colors[kotlin.math.abs(hash) % colors.size]
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
