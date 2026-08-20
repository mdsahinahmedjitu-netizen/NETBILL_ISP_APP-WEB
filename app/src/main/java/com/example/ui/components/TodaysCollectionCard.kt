package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.PaymentCollectionEntity
import com.example.localization.AppTranslation
import com.example.ui.theme.BkashPink
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NagadOrange
import com.example.ui.theme.RocketViolet
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.SleekBorder
import com.example.ui.theme.SleekCard
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.Teal100
import com.example.ui.theme.Teal50
import com.example.ui.theme.Teal600
import com.example.ui.theme.Teal700
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.example.util.AppUtils

enum class CollectionFilterPeriod {
    TODAY,
    YESTERDAY,
    LAST_7_DAYS,
    THIS_MONTH,
    CUSTOM
}

data class PaymentMethodRowData(
    val key: String,
    val displayName: String,
    val icon: ImageVector,
    val brandColor: Color,
    val amount: Double,
    val percentage: Float
)

data class CollectionCalculatedSummary(
    val grandTotal: Double,
    val totalTransactions: Int,
    val averageCollection: Double,
    val highestSingleCollection: Double,
    val lowestSingleCollection: Double,
    val cashTotal: Double,
    val bkashTotal: Double,
    val nagadTotal: Double,
    val rocketTotal: Double,
    val bankTotal: Double,
    val otherTotal: Double,
    val methodRows: List<PaymentMethodRowData>,
    val periodLabel: String
)

@Composable
fun TodaysCollectionCard(
    payments: List<PaymentCollectionEntity>,
    modifier: Modifier = Modifier,
    selectedFilter: CollectionFilterPeriod = CollectionFilterPeriod.TODAY,
    onFilterSelected: (CollectionFilterPeriod) -> Unit = {}
) {
    var customDateString by remember { mutableStateOf(getTodayDateString()) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    val dividerColor = Slate200

    // Calculate aggregated statistics based on selected filter
    val summary = remember(payments, selectedFilter, customDateString) {
        calculateCollectionSummary(
            payments = payments,
            filter = selectedFilter,
            customDateStr = customDateString
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(44.dp),
        colors = CardDefaults.cardColors(containerColor = SleekCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp)
        ) {
            // Header Row: Title & Subtitle + Live Refresh Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(EmeraldSuccess)
                        )
                        Text(
                            text = if (selectedFilter == CollectionFilterPeriod.TODAY)
                                AppTranslation("todays_collection").uppercase()
                            else
                                "${summary.periodLabel} ${AppTranslation("todays_collection")}".uppercase(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = Slate900,
                            letterSpacing = (-1).sp
                        )
                    }
                    Text(
                        text = "${activeFilterLabel(selectedFilter)} Overview".uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Slate400,
                        letterSpacing = 2.sp
                    )
                }

                // Live Auto-Refresh Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Teal50,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Teal100)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Teal600,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "LIVE AUTO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Teal700,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Filter Chips Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChipItem(label = "Today", isSelected = selectedFilter == CollectionFilterPeriod.TODAY, onClick = { onFilterSelected(CollectionFilterPeriod.TODAY) })
                FilterChipItem(label = "Yesterday", isSelected = selectedFilter == CollectionFilterPeriod.YESTERDAY, onClick = { onFilterSelected(CollectionFilterPeriod.YESTERDAY) })
                FilterChipItem(label = "Last 7 Days", isSelected = selectedFilter == CollectionFilterPeriod.LAST_7_DAYS, onClick = { onFilterSelected(CollectionFilterPeriod.LAST_7_DAYS) })
                FilterChipItem(label = "Month", isSelected = selectedFilter == CollectionFilterPeriod.THIS_MONTH, onClick = { onFilterSelected(CollectionFilterPeriod.THIS_MONTH) })
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Payment Methods Breakdown
            Text(
                text = AppTranslation("collection_breakdown").uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = Slate400,
                letterSpacing = 4.sp,
                modifier = Modifier.padding(bottom = 16.dp).drawBehind {
                    val strokeWidth = 1.dp.toPx()
                    val y = size.height - strokeWidth
                    drawLine(
                        color = dividerColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = strokeWidth
                    )
                }.fillMaxWidth().padding(bottom = 8.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                summary.methodRows.forEach { row ->
                    PaymentMethodItemRow(row = row)
                }
            }
        }
    }

    // Custom Date Selector Dialog
    if (showDatePickerDialog) {
        CustomDatePickerDialog(
            currentDate = customDateString,
            onDismiss = { showDatePickerDialog = false },
            onDateSelected = { dateStr ->
                customDateString = dateStr
                onFilterSelected(CollectionFilterPeriod.CUSTOM)
                showDatePickerDialog = false
            }
        )
    }
}

@Composable
fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) Teal600 else SlateSurfaceVariant,
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, SleekBorder)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = if (isSelected) Color.White else Slate400,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun PaymentMethodItemRow(
    row: PaymentMethodRowData
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Badge
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SlateSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = row.icon,
                contentDescription = row.displayName,
                tint = row.brandColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(20.dp))

        // Method Name & Progress Bar
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = row.displayName.uppercase(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = Slate900,
                    letterSpacing = (-0.5).sp
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${(row.percentage * 100).toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Slate400
                    )
                    Text(
                        text = formatBdtCurrency(row.amount).uppercase(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Slate900,
                        letterSpacing = (-1).sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Percentage Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(SlateSurfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(row.percentage.coerceIn(0f, 1f))
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(row.brandColor)
                )
            }
        }
    }
}

fun activeFilterLabel(filter: CollectionFilterPeriod): String {
    return when (filter) {
        CollectionFilterPeriod.TODAY -> "Today"
        CollectionFilterPeriod.YESTERDAY -> "Yesterday"
        CollectionFilterPeriod.LAST_7_DAYS -> "Last 7 Days"
        CollectionFilterPeriod.THIS_MONTH -> "Month"
        CollectionFilterPeriod.CUSTOM -> "Custom"
    }
}

@Composable
fun ExtraMetricTile(
    label: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = label,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate500,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Slate900,
                maxLines = 1
            )
        }
    }
}

@Composable
fun CustomDatePickerDialog(
    currentDate: String,
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit
) {
    val context = LocalContext.current
    
    // We launch the native DatePickerDialog directly for better UX as requested
    LaunchedEffect(Unit) {
        com.example.util.DatePickerUtils.showDatePicker(
            context = context,
            initialDate = currentDate,
            onDateSelected = { 
                onDateSelected(it)
                onDismiss()
            }
        )
    }
}

// Business Logic helper for processing payments
fun calculateCollectionSummary(
    payments: List<PaymentCollectionEntity>,
    filter: CollectionFilterPeriod,
    customDateStr: String
): CollectionCalculatedSummary {
    val todayStr = getTodayDateString()
    val yesterdayStr = getYesterdayDateString()
    val sevenDaysAgoStr = getNDaysAgoDateString(6)
    val thisMonthPrefix = getThisMonthPrefix()

    val filteredPayments = payments.filter { payment ->
        val pDate = payment.paymentDate
        when (filter) {
            CollectionFilterPeriod.TODAY -> pDate == todayStr
            CollectionFilterPeriod.YESTERDAY -> pDate == yesterdayStr
            CollectionFilterPeriod.LAST_7_DAYS -> pDate >= sevenDaysAgoStr && pDate <= todayStr
            CollectionFilterPeriod.THIS_MONTH -> pDate.startsWith(thisMonthPrefix)
            CollectionFilterPeriod.CUSTOM -> pDate == customDateStr
        }
    }

    var cashTotal = 0.0
    var bkashTotal = 0.0
    var nagadTotal = 0.0
    var rocketTotal = 0.0
    var bankTotal = 0.0
    var otherTotal = 0.0

    filteredPayments.forEach { payment ->
        val method = payment.paymentMethod
        when {
            method.contains("Cash", ignoreCase = true) || method.contains("ক্যাশ", ignoreCase = true) -> {
                cashTotal += payment.amount
            }
            method.contains("bKash", ignoreCase = true) || method.contains("বিকাশ", ignoreCase = true) -> {
                bkashTotal += payment.amount
            }
            method.contains("Nagad", ignoreCase = true) || method.contains("নগদ", ignoreCase = true) -> {
                nagadTotal += payment.amount
            }
            method.contains("Rocket", ignoreCase = true) || method.contains("রকেট", ignoreCase = true) -> {
                rocketTotal += payment.amount
            }
            method.contains("Bank", ignoreCase = true) || method.contains("ব্যাংক", ignoreCase = true) -> {
                bankTotal += payment.amount
            }
            else -> {
                otherTotal += payment.amount
            }
        }
    }

    val grandTotal = cashTotal + bkashTotal + nagadTotal + rocketTotal + bankTotal + otherTotal
    val totalTxns = filteredPayments.size
    val avgCollection = if (totalTxns > 0) grandTotal / totalTxns else 0.0

    val amounts = filteredPayments.map { it.amount }
    val highestSingle = if (amounts.isNotEmpty()) amounts.maxOrNull() ?: 0.0 else 0.0
    val lowestSingle = if (amounts.isNotEmpty()) amounts.minOrNull() ?: 0.0 else 0.0

    val safeGrandTotal = if (grandTotal > 0) grandTotal else 1.0

    val rows = listOf(
        PaymentMethodRowData(
            key = "Cash",
            displayName = "Cash",
            icon = Icons.Default.Payments,
            brandColor = EmeraldSuccess,
            amount = cashTotal,
            percentage = (cashTotal / safeGrandTotal).toFloat()
        ),
        PaymentMethodRowData(
            key = "bKash",
            displayName = "bKash",
            icon = Icons.Default.PhoneAndroid,
            brandColor = BkashPink,
            amount = bkashTotal,
            percentage = (bkashTotal / safeGrandTotal).toFloat()
        ),
        PaymentMethodRowData(
            key = "Nagad",
            displayName = "Nagad",
            icon = Icons.Default.CreditCard,
            brandColor = NagadOrange,
            amount = nagadTotal,
            percentage = (nagadTotal / safeGrandTotal).toFloat()
        ),
        PaymentMethodRowData(
            key = "Rocket",
            displayName = "Rocket",
            icon = Icons.Default.RocketLaunch,
            brandColor = RocketViolet,
            amount = rocketTotal,
            percentage = (rocketTotal / safeGrandTotal).toFloat()
        ),
        PaymentMethodRowData(
            key = "Bank Transfer",
            displayName = "Bank Transfer",
            icon = Icons.Default.AccountBalance,
            brandColor = Teal600,
            amount = bankTotal,
            percentage = (bankTotal / safeGrandTotal).toFloat()
        ),
        PaymentMethodRowData(
            key = "Other",
            displayName = "Other",
            icon = Icons.Default.MoreHoriz,
            brandColor = Color(0xFF64748B),
            amount = otherTotal,
            percentage = (otherTotal / safeGrandTotal).toFloat()
        )
    )

    val periodLabelStr = when (filter) {
        CollectionFilterPeriod.TODAY -> "Today (${AppUtils.formatDateForDisplay(todayStr)})"
        CollectionFilterPeriod.YESTERDAY -> "Yesterday (${AppUtils.formatDateForDisplay(yesterdayStr)})"
        CollectionFilterPeriod.LAST_7_DAYS -> "Last 7 Days"
        CollectionFilterPeriod.THIS_MONTH -> "This Month (${getThisMonthName()})"
        CollectionFilterPeriod.CUSTOM -> "Date (${AppUtils.formatDateForDisplay(customDateStr)})"
    }

    return CollectionCalculatedSummary(
        grandTotal = grandTotal,
        totalTransactions = totalTxns,
        averageCollection = avgCollection,
        highestSingleCollection = highestSingle,
        lowestSingleCollection = lowestSingle,
        cashTotal = cashTotal,
        bkashTotal = bkashTotal,
        nagadTotal = nagadTotal,
        rocketTotal = rocketTotal,
        bankTotal = bankTotal,
        otherTotal = otherTotal,
        methodRows = rows,
        periodLabel = periodLabelStr
    )
}

// Number Formatter in Bangladeshi Taka Currency
fun formatBdtCurrency(amount: Double): String {
    val longVal = amount.toLong()
    return "৳" + NumberFormat.getNumberInstance(Locale.US).format(longVal)
}

fun getTodayDateString(): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}

private fun getYesterdayDateString(): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DATE, -1)
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
}

private fun getNDaysAgoDateString(n: Int): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DATE, -n)
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
}

private fun getThisMonthPrefix(): String {
    return SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
}

private fun getThisMonthName(): String {
    return SimpleDateFormat("MMMM yyyy", Locale.US).format(Date())
}
