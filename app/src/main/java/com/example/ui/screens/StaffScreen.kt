package com.example.ui.screens

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
fun StaffScreen(viewModel: MainViewModel) {
    val staffList by viewModel.staffList.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isAdmin = currentUser?.role?.lowercase() == "admin"
    val currency = AppTranslation("currency_symbol")
    var showAddStaffDialog by remember { mutableStateOf(false) }
    var selectedStaffForSalary by remember { mutableStateOf<StaffEntity?>(null) }
    var selectedStaffForEdit by remember { mutableStateOf<StaffEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            if (isAdmin) {
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
            Text(
                text = AppTranslation("staff_management"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Slate900
            )

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
                                Text("Salary: $currency ${staff.salary.toInt()}/mo", color = Teal600, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                if (staff.receiveAlerts) {
                                    Text("✅ Receiving Alerts (WhatsApp)", color = EmeraldSuccess, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isAdmin) {
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
                    onSave = { name, mobile, role, salary, alerts, jDate, zone ->
                        viewModel.addStaff(name, mobile, role, salary, alerts, jDate, zone)
                        showAddStaffDialog = false
                    }
                )
            }

            selectedStaffForSalary?.let { staff ->
                PaySalaryDialog(
                    staff = staff,
                    onDismiss = { selectedStaffForSalary = null },
                    onPay = { month ->
                        viewModel.payStaffSalary(staff.id, staff.name, staff.salary, month)
                        selectedStaffForSalary = null
                    }
                )
            }

            selectedStaffForEdit?.let { staff ->
                EditStaffDialog(
                    staff = staff,
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
fun EditStaffDialog(staff: StaffEntity, onDismiss: () -> Unit, onSave: (StaffEntity) -> Unit) {
    var name by remember { mutableStateOf(staff.name) }
    var mobile by remember { mutableStateOf(staff.mobile) }
    var role by remember { mutableStateOf(staff.role) }
    var salary by remember { mutableStateOf(staff.salary.toInt().toString()) }
    var zone by remember { mutableStateOf(staff.zone) }
    var receiveAlerts by remember { mutableStateOf(staff.receiveAlerts) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Employee: ${staff.name}", color = Slate900, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Staff Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("Mobile Number") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text("Role / Designation") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = salary, onValueChange = { salary = it }, label = { Text("Monthly Salary (৳)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
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
                onSave(staff.copy(name = name, mobile = mobile, role = role, salary = salary.toDoubleOrNull() ?: staff.salary, zone = zone, receiveAlerts = receiveAlerts)) 
            }, colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)) {
                Text("Update Staff", color = Color.White)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Slate600) } },
        containerColor = SleekCard
    )
}

@Composable
fun AddStaffDialog(onDismiss: () -> Unit, onSave: (String, String, String, Double, Boolean, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Support Staff") }
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
            Button(onClick = { onSave(name, mobile, role, salary.toDoubleOrNull() ?: 15000.0, receiveAlerts, joiningDate, zone) }, colors = ButtonDefaults.buttonColors(containerColor = Teal600)) {
                Text("Save Staff", color = Color.White)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Slate600) } },
        containerColor = SleekCard
    )
}

@Composable
fun PaySalaryDialog(staff: StaffEntity, onDismiss: () -> Unit, onPay: (String) -> Unit) {
    var month by remember { mutableStateOf("August 2026") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Disburse Salary for ${staff.name}", color = Slate900, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Monthly Salary: ৳ ${staff.salary.toInt()}", color = Teal600, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = month, onValueChange = { month = it }, label = { Text("Salary Month (e.g. August 2026)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { onPay(month) }, colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)) {
                Text("Confirm Disburse", color = Color.White)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Slate600) } },
        containerColor = SleekCard
    )
}
