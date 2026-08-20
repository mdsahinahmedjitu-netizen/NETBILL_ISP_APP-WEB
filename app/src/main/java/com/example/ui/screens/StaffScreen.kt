package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import com.example.ui.theme.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.outlined.History
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.StaffEntity
import com.example.localization.AppTranslation
import com.example.ui.components.ReadonlyDateField
import com.example.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StaffScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit = {},
    onNavigateToLedger: () -> Unit = {}
) {
    val staffList by viewModel.staffList.collectAsState()
    val permissions by viewModel.currentPermissions.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isAdmin = currentUser?.role?.lowercase() == "admin"
    val currency = AppTranslation("currency_symbol")
    var showAddStaffDialog by remember { mutableStateOf(false) }
    var selectedStaffForSalary by remember { mutableStateOf<StaffEntity?>(null) }
    var selectedStaffForEdit by remember { mutableStateOf<StaffEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            if (permissions.canManageStaff) {
                FloatingActionButton(
                    onClick = { showAddStaffDialog = true },
                    containerColor = ElectricBlue,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Staff")
                }
            }
        },
        containerColor = SleekBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = AppTranslation("staff_management"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                
                Button(
                    onClick = onNavigateToLedger,
                    enabled = permissions.canAccessSalary,
                    colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Outlined.History, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(AppTranslation("salary_ledger"), fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(staffList, key = { it.id }) { staff ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SleekCard),
                        border = BorderStroke(1.dp, SleekBorder)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(staff.name, fontWeight = FontWeight.Bold, color = Slate900, fontSize = 16.sp)
                                Text("Role: ${staff.role} • Mobile: ${staff.mobile}", color = Slate600, fontSize = 12.sp)
                                Text("Salary: $currency ${String.format(Locale.US, "%,.0f", staff.salary)}/mo", color = Teal600, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "Balance: $currency ${String.format(Locale.US, "%,.0f", staff.balance)} ${if (staff.balance >= 0) "(${AppTranslation("pao_na")})" else "(${AppTranslation("advance")})"}",
                                    color = if (staff.balance >= 0) ElectricBlue else Color(0xFFF58220),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (staff.receiveAlerts) {
                                    Text("✅ Receiving Alerts (WhatsApp)", color = EmeraldSuccess, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (permissions.canManageStaff) {
                                    IconButton(onClick = { selectedStaffForEdit = staff }) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Staff",
                                            tint = Slate600,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.updateStaff(staff.copy(receiveAlerts = !staff.receiveAlerts)) }
                                ) {
                                    Icon(
                                        imageVector = if (staff.receiveAlerts) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                                        contentDescription = "Toggle Alerts",
                                        tint = if (staff.receiveAlerts) ElectricBlue else Slate400
                                    )
                                }

                                Button(
                                    onClick = { selectedStaffForSalary = staff },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Text("Pay", fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            if (showAddStaffDialog) {
                AddStaffDialog(
                    onDismiss = { showAddStaffDialog = false },
                    onSave = { name, mobile, role, salary, password, alerts, jDate, zone ->
                        viewModel.addStaff(name, mobile, role, salary, password, alerts, jDate, zone)
                        showAddStaffDialog = false
                    }
                )
            }

            selectedStaffForSalary?.let { staff ->
                PaySalaryDialog(
                    staff = staff,
                    onDismiss = { selectedStaffForSalary = null },
                    onPay = { amount, month, type, remarks ->
                        if (type == "salary_add") {
                            viewModel.payStaffSalary(staff.id, staff.name, amount, month)
                        } else {
                            viewModel.disburseStaffPayment(staff.id, staff.name, amount, month, remarks)
                        }
                        selectedStaffForSalary = null
                    }
                )
            }

            selectedStaffForEdit?.let { staff ->
                EditStaffDialog(
                    staff = staff,
                    isAdmin = isAdmin,
                    onDismiss = { selectedStaffForEdit = null },
                    onSave = { updatedStaff ->
                        viewModel.updateStaff(updatedStaff)
                        selectedStaffForEdit = null
                    }
                )
            }
        }
    }
}

@Composable
fun EditStaffDialog(staff: StaffEntity, isAdmin: Boolean, onDismiss: () -> Unit, onSave: (StaffEntity) -> Unit) {
    var name by remember { mutableStateOf(staff.name) }
    var mobile by remember { mutableStateOf(staff.mobile) }
    var role by remember { mutableStateOf(staff.role) }
    var password by remember { mutableStateOf(staff.password) }
    var salary by remember { mutableStateOf(staff.salary.toInt().toString()) }
    var balanceStr by remember { mutableStateOf(staff.balance.toInt().toString()) }
    var zone by remember { mutableStateOf(staff.zone) }
    var receiveAlerts by remember { mutableStateOf(staff.receiveAlerts) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Employee: ${staff.name}", color = Slate900, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Staff Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("Mobile Number") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Login Password") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text("Role / Designation") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = salary, onValueChange = { salary = it }, label = { Text("Monthly Salary (৳)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                
                if (isAdmin) {
                    OutlinedTextField(value = balanceStr, onValueChange = { balanceStr = it }, label = { Text("Account Balance (৳)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
                
                OutlinedTextField(value = zone, onValueChange = { zone = it }, label = { Text("Assigned Zone") }, modifier = Modifier.fillMaxWidth())
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { receiveAlerts = !receiveAlerts }) {
                    androidx.compose.material3.Checkbox(checked = receiveAlerts, onCheckedChange = { receiveAlerts = it })
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Receive WhatsApp Alerts", fontSize = 13.sp, color = Slate800)
                }
            }
        },
        confirmButton = {
            Button(onClick = { 
                onSave(staff.copy(name = name, mobile = mobile, role = role, password = password, salary = salary.toDoubleOrNull() ?: staff.salary, balance = balanceStr.toDoubleOrNull() ?: staff.balance, zone = zone, receiveAlerts = receiveAlerts)) 
            }, colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)) {
                Text("Update Staff", color = Color.White)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Slate600) } },
        containerColor = SleekCard
    )
}

@Composable
fun AddStaffDialog(onDismiss: () -> Unit, onSave: (String, String, String, Double, String, Boolean, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Support Staff") }
    var password by remember { mutableStateOf("123456") }
    var salary by remember { mutableStateOf("15000") }
    var zone by remember { mutableStateOf("All") }
    var receiveAlerts by remember { mutableStateOf(false) }
    var joiningDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Employee", color = Slate900, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Staff Name (বাংলা / English)") },
                    placeholder = { Text("যেমন: মো: তানভীর আহমেদ / Tanvir Ahmed", fontSize = 12.sp, color = Slate600) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.None)
                )
                OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("Mobile Number") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Login Password") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text("Role / Designation") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.None))
                OutlinedTextField(value = salary, onValueChange = { salary = it }, label = { Text("Monthly Salary (৳)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = zone, onValueChange = { zone = it }, label = { Text("Assigned Zone") }, modifier = Modifier.fillMaxWidth())
                
                ReadonlyDateField(
                    value = joiningDate,
                    label = "যোগদানের তারিখ (Joining Date)",
                    onDateSelected = { joiningDate = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { receiveAlerts = !receiveAlerts }) {
                    androidx.compose.material3.Checkbox(checked = receiveAlerts, onCheckedChange = { receiveAlerts = it })
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Receive WhatsApp Alerts", fontSize = 13.sp, color = Slate800)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name, mobile, role, salary.toDoubleOrNull() ?: 15000.0, password, receiveAlerts, joiningDate, zone) }, colors = ButtonDefaults.buttonColors(containerColor = Teal600)) {
                Text("Save Staff", color = Color.White)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Slate600) } },
        containerColor = SleekCard
    )
}

@Composable
fun PaySalaryDialog(staff: StaffEntity, onDismiss: () -> Unit, onPay: (Double, String, String, String) -> Unit) {
    var month by remember { mutableStateOf("August 2026") }
    var amount by remember { mutableStateOf(staff.salary.toInt().toString()) }
    var remarks by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("salary_add") } // salary_add, payment

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Staff Payment: ${staff.name}", color = Slate900, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Type Switcher
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFFF8FAFC)).padding(4.dp)) {
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(6.dp)).background(if (type == "salary_add") Color.White else Color.Transparent).clickable { type = "salary_add" }.padding(8.dp), contentAlignment = Alignment.Center) {
                        Text("Add Salary", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (type == "salary_add") Teal600 else Slate400)
                    }
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(6.dp)).background(if (type == "payment") Color.White else Color.Transparent).clickable { type = "payment" }.padding(8.dp), contentAlignment = Alignment.Center) {
                        Text("Disburse", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (type == "payment") Teal600 else Slate400)
                    }
                }

                OutlinedTextField(value = month, onValueChange = { month = it }, label = { Text("Target Month") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount (৳)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = remarks, onValueChange = { remarks = it }, label = { Text("Remarks (Optional)") }, modifier = Modifier.fillMaxWidth())

                // Projected Balance
                val amt = amount.toDoubleOrNull() ?: 0.0
                val projected = if (type == "salary_add") staff.balance + amt else staff.balance - amt
                Card(
                    colors = CardDefaults.cardColors(containerColor = Teal50),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Projected Account Balance", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Teal700)
                        Text("৳ ${String.format(Locale.US, "%,.0f", projected)}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Teal600)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onPay(amount.toDoubleOrNull() ?: 0.0, month, type, remarks) }, colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)) {
                Text("Confirm", color = Color.White)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Slate600) } },
        containerColor = Color.White
    )
}
