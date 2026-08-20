package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.StaffPayoutEntity
import com.example.localization.AppTranslation
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffSalaryHistoryScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val payouts by viewModel.staffPayouts.collectAsState()
    val staffList by viewModel.staffList.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isAdmin = currentUser?.role?.lowercase() == "admin"
    
    var selectedMonth by remember { mutableStateOf("All Months") }
    var selectedStaffId by remember { mutableStateOf("all") }
    var editingPayout by remember { mutableStateOf<StaffPayoutEntity?>(null) }

    val filteredPayouts = payouts.filter { p ->
        (selectedMonth == "All Months" || p.month == selectedMonth) &&
        (selectedStaffId == "all" || p.staffId == selectedStaffId)
    }.sortedByDescending { it.date }

    val totalAdded = filteredPayouts.filter { it.type == "salary_add" }.sumOf { it.amount }
    val totalPaid = filteredPayouts.filter { it.type == "payment" }.sumOf { it.amount }
    val netBalance = totalAdded - totalPaid

    val months = remember {
        val list = mutableListOf("All Months")
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.US)
        val cal = Calendar.getInstance()
        for (i in 0 until 12) {
            list.add(sdf.format(cal.time))
            cal.add(Calendar.MONTH, -1)
        }
        list
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(AppTranslation("salary_ledger"), fontWeight = FontWeight.Black) },
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
            // Filters
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isAdmin) {
                    Box(modifier = Modifier.weight(1f)) {
                        var expanded by remember { mutableStateOf(false) }
                        OutlinedCard(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = staffList.find { it.id == selectedStaffId }?.name ?: AppTranslation("all_staff"),
                                modifier = Modifier.padding(12.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(text = { Text(AppTranslation("all_staff")) }, onClick = { selectedStaffId = "all"; expanded = false })
                            staffList.forEach { s ->
                                DropdownMenuItem(text = { Text(s.name) }, onClick = { selectedStaffId = s.id; expanded = false })
                            }
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    var expanded by remember { mutableStateOf(false) }
                    OutlinedCard(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (selectedMonth == "All Months") AppTranslation("all_months") else selectedMonth,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        months.forEach { m ->
                            DropdownMenuItem(text = { Text(if (m == "All Months") AppTranslation("all_months") else m) }, onClick = { selectedMonth = m; expanded = false })
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Summary Cards
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryCard(Modifier.weight(1f), AppTranslation("total_salary_accrued"), "৳${String.format(Locale.US, "%,.0f", totalAdded)}", Teal600)
                SummaryCard(Modifier.weight(1f), AppTranslation("total_disbursed"), "৳${String.format(Locale.US, "%,.0f", totalPaid)}", Color.Red)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = borderStroke()
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(AppTranslation("net_balance"), fontSize = 10.sp, fontWeight = FontWeight.Black, color = Slate400)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("৳${String.format(Locale.US, "%,.0f", Math.abs(netBalance))}", fontSize = 28.sp, fontWeight = FontWeight.Black, color = if (netBalance >= 0) ElectricBlue else Color(0xFFF58220))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (netBalance >= 0) "(${AppTranslation("pao_na")})" else "(${AppTranslation("advance")})",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = if (netBalance >= 0) Color(0xFF1A237E) else Color(0xFFE65100)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // List
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredPayouts, key = { it.id }) { p ->
                    PayoutItemCard(p, isAdmin, onDelete = { viewModel.deleteStaffPayout(p) }, onEdit = { editingPayout = p })
                }
                if (filteredPayouts.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            Text(AppTranslation("no_records"), color = Slate400, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    editingPayout?.let { payout ->
        EditPayoutDialog(
            payout = payout,
            staffList = staffList,
            onDismiss = { editingPayout = null },
            onUpdate = { updatedPayout ->
                viewModel.updateStaffPayout(updatedPayout, payout.amount, payout.type)
                editingPayout = null
            }
        )
    }
}

@Composable
fun SummaryCard(modifier: Modifier, label: String, value: String, color: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = borderStroke()
    ) {
        Column(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Black, color = Slate400, textAlign = TextAlign.Center)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}

@Composable
fun PayoutItemCard(p: StaffPayoutEntity, isAdmin: Boolean, onDelete: () -> Unit, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = borderStroke()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (p.type == "salary_add") Teal600 else Color.Red))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(p.staffName, fontWeight = FontWeight.Black, fontSize = 14.sp)
                }
                Text("${p.date} • ${p.month}", fontSize = 10.sp, color = Slate400)
                if (p.remarks.isNotBlank()) {
                    Text(p.remarks, fontSize = 10.sp, color = Slate600, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (p.type == "salary_add") "+" else "-"} ৳${String.format(Locale.US, "%,.0f", p.amount)}",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = if (p.type == "salary_add") Teal600 else Color.Red
                )
                Surface(
                    color = Color(0xFFF8FAFC), // Slate 50
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        "৳${String.format(Locale.US, "%,.0f", p.newBalance)}",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate600
                    )
                }
            }

            if (isAdmin) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun EditPayoutDialog(
    payout: StaffPayoutEntity,
    staffList: List<com.example.data.entity.StaffEntity>,
    onDismiss: () -> Unit,
    onUpdate: (StaffPayoutEntity) -> Unit
) {
    var amount by remember { mutableStateOf(payout.amount.toString()) }
    var remarks by remember { mutableStateOf(payout.remarks) }
    var type by remember { mutableStateOf(payout.type) }
    var staffId by remember { mutableStateOf(payout.staffId) }
    var month by remember { mutableStateOf(payout.month) }
    var date by remember { mutableStateOf(payout.date) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppTranslation("edit_salary_record"), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text(AppTranslation("amount_label")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = remarks, onValueChange = { remarks = it }, label = { Text(AppTranslation("remarks_label")) }, modifier = Modifier.fillMaxWidth())
                
                Text(AppTranslation("transaction_type"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate400)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = type == "salary_add", onClick = { type = "salary_add" }, label = { Text(AppTranslation("salary_accrued")) })
                    FilterChip(selected = type == "payment", onClick = { type = "payment" }, label = { Text(AppTranslation("cash_disbursed")) })
                }
            }
        },
        confirmButton = {
            Button(onClick = { 
                onUpdate(payout.copy(amount = amount.toDoubleOrNull() ?: payout.amount, remarks = remarks, type = type, staffId = staffId, month = month, date = date)) 
            }) {
                Text(AppTranslation("update_record"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(AppTranslation("cancel")) }
        }
    )
}

@Composable
private fun borderStroke() = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)) // Slate 100
