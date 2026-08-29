package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.example.data.entity.CustomerEntity
import com.example.localization.appTranslation
import com.example.ui.theme.*
import com.example.util.ExpiryUtils
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpiryAlertBottomSheet(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onSelectCustomer: (CustomerEntity) -> Unit
) {
    val expiringCustomers by viewModel.expiringTomorrowCustomers.collectAsState()
    val currentLang by viewModel.currentLanguage.collectAsState()
    val isBangla = currentLang == com.example.localization.AppLanguage.BANGLA
    val currency = appTranslation("currency_symbol")

    var customerToExtendExpiry by remember { mutableStateOf<CustomerEntity?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SleekSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
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
                        color = Color(0xFFE11D48),
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
                    Column {
                        Text(
                            text = appTranslation("customer_expiry_alert_title"),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = Slate900
                        )
                        Text(
                            text = if (isBangla) "${toBanglaDigits(expiringCustomers.size)} জন গ্রাহকের লাইনের মেয়াদ আগামীকাল শেষ হবে"
                                   else "${expiringCustomers.size} customers will expire tomorrow.",
                            fontSize = 12.sp,
                            color = Color(0xFFBE123C)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate600)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (expiringCustomers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = appTranslation("no_upcoming_expiry"),
                            color = Slate600,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxHeight(0.8f)
                ) {
                    items(expiringCustomers, key = { it.id }) { customer ->
                        ExpiryCustomerCard(
                            customer = customer,
                            currency = currency,
                            isBangla = isBangla,
                            onCardClick = {
                                onDismiss()
                                onSelectCustomer(customer)
                            },
                            onExtendExpiryClick = {
                                customerToExtendExpiry = customer
                            }
                        )
                    }
                }
            }
        }
    }

    // Extend Expiry Dialog
    customerToExtendExpiry?.let { customer ->
        ExtendExpiryDialog(
            customer = customer,
            onDismiss = { customerToExtendExpiry = null },
            onSave = { newDate, newTime ->
                viewModel.updateCustomerExpiry(customer.id, newDate, newTime)
                customerToExtendExpiry = null
            }
        )
    }
}

@Composable
fun ExpiryCustomerCard(
    customer: CustomerEntity,
    currency: String,
    isBangla: Boolean,
    onCardClick: () -> Unit,
    onExtendExpiryClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SleekCard),
        border = BorderStroke(1.dp, Color(0xFFFECDD3))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Customer Name & Expiry Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE11D48))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = customer.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Slate900
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFFFF1F2),
                    border = BorderStroke(1.dp, Color(0xFFFECDD3))
                ) {
                    Text(
                        text = if (isBangla) "🔴 আগামীকাল মেয়াদ শেষ" else "🔴 Expiring Tomorrow",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9F1239),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Details: Address & Mobile
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Slate500,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = customer.address.orEmpty().ifEmpty { "N/A" },
                    fontSize = 12.sp,
                    color = Slate600
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Phone,
                    contentDescription = null,
                    tint = Slate500,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = customer.mobile,
                    fontSize = 12.sp,
                    color = Slate800,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = SleekBorder)
            Spacer(modifier = Modifier.height(10.dp))

            // Outstanding Total Due & Expire Time Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = appTranslation("total_due"),
                        fontSize = 11.sp,
                        color = Slate500
                    )
                    Text(
                        text = "$currency ${customer.currentDue.toInt()}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFE11D48)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = appTranslation("expire_date"),
                        fontSize = 11.sp,
                        color = Slate500
                    )
                    Text(
                        text = "${customer.expireDate} ${customer.expireTime.orEmpty().ifEmpty { "11:59 PM" }}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Row: View Details & Extend Expiry
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCardClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Teal600)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isBangla) "গ্রাহকের বিস্তারিত" else "View Details", fontSize = 11.sp)
                }

                Button(
                    onClick = onExtendExpiryClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(appTranslation("extend_expiry"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ExtendExpiryDialog(
    customer: CustomerEntity,
    onDismiss: () -> Unit,
    onSave: (newDate: String, newTime: String) -> Unit
) {
    var newDate by remember { mutableStateOf(customer.expireDate.orEmpty().ifEmpty { "2026-08-31" }) }
    var newTime by remember { mutableStateOf(customer.expireTime.orEmpty().ifEmpty { "11:59 PM" }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = appTranslation("extend_expiry"),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Slate900
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "গ্রাহক: ${customer.name} (${customer.customerCode})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate800
                )

                OutlinedTextField(
                    value = newDate,
                    onValueChange = { newDate = it },
                    label = { Text(appTranslation("expire_date")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = newTime,
                    onValueChange = { newTime = it },
                    label = { Text(appTranslation("expire_time")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Quick Preset:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate600)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = false,
                        onClick = {
                            newDate = "2026-08-31"
                            newTime = "11:59 PM"
                        },
                        label = { Text("31 Aug (11:59 PM)", fontSize = 10.sp) }
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            newDate = "2026-09-08"
                            newTime = "11:59 PM"
                        },
                        label = { Text("+7 Days", fontSize = 10.sp) }
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            newDate = "2026-09-30"
                            newTime = "11:59 PM"
                        },
                        label = { Text("+30 Days", fontSize = 10.sp) }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(newDate, newTime) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
            ) {
                Text(appTranslation("update_expiry"), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun toBanglaDigits(number: Int): String {
    return number.toString().map {
        when (it) {
            '0' -> '০'; '1' -> '১'; '2' -> '২'; '3' -> '৩'; '4' -> '৪'
            '5' -> '৫'; '6' -> '৬'; '7' -> '৭'; '8' -> '৮'; '9' -> '৯'
            else -> it
        }
    }.joinToString("")
}
