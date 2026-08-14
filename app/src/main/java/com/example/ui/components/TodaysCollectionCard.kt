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
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.SleekBorder
import com.example.ui.theme.SleekCard
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SleekCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Header Row: Title & Subtitle + Live Refresh Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(EmeraldSuccess)
                        )
                        Text(
                            text = if (selectedFilter == CollectionFilterPeriod.TODAY)
                                AppTranslation("todays_collection")
                            else
                                "${summary.periodLabel} ${AppTranslation("todays_collection")}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                    }
                    Text(
                        text = summary.periodLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate500
                    )
                }

                // Live Auto-Refresh Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Teal50,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Teal100)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Teal600,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "LIVE AUTO",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Teal700
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Filter Chips Bar: [Today] [Yesterday] [Last 7 Days] [This Month] [Custom]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChipItem(
                    label = AppTranslation("todays_collection"),
                    shortLabel = "Today",
                    isSelected = selectedFilter == CollectionFilterPeriod.TODAY,
                    onClick = { onFilterSelected(CollectionFilterPeriod.TODAY) }
                )
                FilterChipItem(
                    label = AppTranslation("yesterday"),
                    shortLabel = AppTranslation("yesterday"),
                    isSelected = selectedFilter == CollectionFilterPeriod.YESTERDAY,
                    onClick = { onFilterSelected(CollectionFilterPeriod.YESTERDAY) }
                )
                FilterChipItem(
                    label = AppTranslation("last_7_days"),
                    shortLabel = AppTranslation("last_7_days"),
                    isSelected = selectedFilter == CollectionFilterPeriod.LAST_7_DAYS,
                    onClick = { onFilterSelected(CollectionFilterPeriod.LAST_7_DAYS) }
                )
                FilterChipItem(
                    label = AppTranslation("this_month"),
                    shortLabel = AppTranslation("this_month"),
                    isSelected = selectedFilter == CollectionFilterPeriod.THIS_MONTH,
                    onClick = { onFilterSelected(CollectionFilterPeriod.THIS_MONTH) }
                )
                FilterChipItem(
                    label = if (selectedFilter == CollectionFilterPeriod.CUSTOM) customDateString else AppTranslation("custom_date"),
                    shortLabel = if (selectedFilter == CollectionFilterPeriod.CUSTOM) customDateString else AppTranslation("custom_date"),
                    isSelected = selectedFilter == CollectionFilterPeriod.CUSTOM,
                    onClick = {
                        onFilterSelected(CollectionFilterPeriod.CUSTOM)
                        showDatePickerDialog = true
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Payment Methods Breakdown Table
            Text(
                text = AppTranslation("collection_breakdown"),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Slate500,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            summary.methodRows.forEach { row ->
                PaymentMethodItemRow(row = row)
                Spacer(modifier = Modifier.height(8.dp))
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
    shortLabel: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) Teal600 else Color(0xFFF1F5F9),
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, SleekBorder)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = shortLabel,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Color(0xFF0F172A)
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
                .size(36.dp)
                .clip(CircleShape)
                .background(row.brandColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = row.icon,
                contentDescription = row.displayName,
                tint = row.brandColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Method Name & Progress Bar
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = row.displayName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate900
                )
                Text(
                    text = "${(row.percentage * 100).toInt()}%",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate500
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Percentage Bar
            LinearProgressIndicator(
                progress = { row.percentage.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = row.brandColor,
                trackColor = row.brandColor.copy(alpha = 0.15f)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Amount Display formatted in Bangladeshi Taka (৳)
        Text(
            text = formatBdtCurrency(row.amount),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (row.amount > 0) Slate900 else Slate500,
            textAlign = TextAlign.End,
            modifier = Modifier.width(85.dp)
        )
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
