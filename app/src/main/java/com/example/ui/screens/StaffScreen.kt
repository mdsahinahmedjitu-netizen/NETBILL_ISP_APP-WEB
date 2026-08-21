package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.example.ui.theme.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
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
    val currency = AppTranslation("currency_symbol")
    var showAddStaffDialog by remember { mutableStateOf(false) }
    var selectedStaffForSalary by remember { mutableStateOf<StaffEntity?>(null) }
    var selectedStaffForEdit by remember { mutableStateOf<StaffEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SleekBg)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header & Stats
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(44.dp))
                .border(1.dp, SleekBorder, RoundedCornerShape(44.dp))
                .padding(28.dp)
        ) {
            Text(
                text = AppTranslation("staff_management").uppercase(),
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                letterSpacing = 2.sp,
                color = Slate900
            )
            Text(
                text = "OPERATIONAL TEAM • PERFORMANCE TRACKING",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp,
                color = IspTealPrimary,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatBox(label = "TOTAL STAFF", value = staffList.size.toString(), color = Color(0xFF64748B), modifier = Modifier.weight(1f))
                StatBox(label = "FIELD STAFF", value = staffList.count { it.role == "Lineman" }.toString(), color = IspIndigo, modifier = Modifier.weight(1f))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = { showAddStaffDialog = true },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IspIndigo)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("ONBOARD NEW STAFF", fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            }
        }

        // Staff List
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            staffList.forEach { staff ->
                StaffCard(
                    staff = staff,
                    currency = currency,
                    onEdit = { selectedStaffForEdit = staff },
                    onPay = { selectedStaffForSalary = staff }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(80.dp))
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
}

@Composable
fun StaffCard(staff: StaffEntity, currency: String, onEdit: () -> Unit, onPay: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(44.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SleekBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(28.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(64.dp).background(Color(0xFFF1F5F9), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(staff.name.take(1).uppercase(), fontSize = 24.sp, fontWeight = FontWeight.Black, color = IspTealPrimary)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(staff.name.uppercase(), fontWeight = FontWeight.Black, fontSize = 18.sp, color = Slate900, letterSpacing = 1.sp)
                        Text(staff.role.uppercase(), fontWeight = FontWeight.Black, fontSize = 10.sp, color = IspTealPrimary, letterSpacing = 2.sp)
                    }
                }
                
                IconButton(onClick = onEdit, modifier = Modifier.background(Slate100, CircleShape)) {
                    Icon(Icons.Default.Edit, null, tint = Slate600, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = SleekBorder.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("MONTHLY SALARY", fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Slate400)
                    Text("$currency ${staff.salary.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Slate800, letterSpacing = 1.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("ACCOUNT BALANCE", fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Slate400)
                    Text("$currency ${staff.balance.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = if(staff.balance >= 0) IspIndigo else IspRose, letterSpacing = 1.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onPay,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
            ) {
                Icon(Icons.Default.AttachMoney, null)
                Spacer(Modifier.width(8.dp))
                Text("PAY SALARY", fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            }
        }
    }
}

@Composable
fun EditStaffDialog(staff: StaffEntity, onDismiss: () -> Unit, onSave: (StaffEntity) -> Unit) {
    var name by remember { mutableStateOf(staff.name) }
    var mobile by remember { mutableStateOf(staff.mobile) }
    var role by remember { mutableStateOf(staff.role) }
    var password by remember { mutableStateOf(staff.password) }
    var salary by remember { mutableStateOf(staff.salary.toInt().toString()) }
    var zone by remember { mutableStateOf(staff.zone) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("EDIT STAFF IDENTITY", color = Slate900, fontWeight = FontWeight.Black, letterSpacing = 2.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("FULL NAME", fontWeight = FontWeight.Black) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("MOBILE", fontWeight = FontWeight.Black) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(16.dp))
                OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text("DESIGNATION", fontWeight = FontWeight.Black) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
            }
        },
        confirmButton = {
            Button(onClick = { 
                onSave(staff.copy(name = name, mobile = mobile, role = role, password = password, salary = salary.toDoubleOrNull() ?: staff.salary, zone = zone)) 
            }, modifier = Modifier.fillMaxWidth().height(64.dp), shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = IspTealPrimary)) {
                Text("UPDATE PROFILE", fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", fontWeight = FontWeight.Black, color = Slate400) } },
        containerColor = Color.White,
        shape = RoundedCornerShape(44.dp)
    )
}

@Composable
fun AddStaffDialog(onDismiss: () -> Unit, onSave: (String, String, String, Double, String, Boolean, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Lineman") }
    var salary by remember { mutableStateOf("15000") }
    var password by remember { mutableStateOf("123456") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("NEW STAFF ENROLLMENT", color = Slate900, fontWeight = FontWeight.Black, letterSpacing = 2.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("NAME", fontWeight = FontWeight.Black) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("MOBILE", fontWeight = FontWeight.Black) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(16.dp))
                OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text("ROLE", fontWeight = FontWeight.Black) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                OutlinedTextField(value = salary, onValueChange = { salary = it }, label = { Text("SALARY", fontWeight = FontWeight.Black) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(16.dp))
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name, mobile, role, salary.toDoubleOrNull() ?: 15000.0, password, false, "", "All") }, modifier = Modifier.fillMaxWidth().height(64.dp), shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = IspIndigo)) {
                Text("COMMIT ENROLLMENT", fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", fontWeight = FontWeight.Black, color = Slate400) } },
        containerColor = Color.White,
        shape = RoundedCornerShape(44.dp)
    )
}

@Composable
fun PaySalaryDialog(staff: StaffEntity, onDismiss: () -> Unit, onPay: (Double, String, String, String) -> Unit) {
    var amount by remember { mutableStateOf(staff.salary.toInt().toString()) }
    var month by remember { mutableStateOf("August 2026") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("FINANCIAL MANAGEMENT", color = Slate900, fontWeight = FontWeight.Black, letterSpacing = 2.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("AMOUNT", fontWeight = FontWeight.Black) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(16.dp))
                OutlinedTextField(value = month, onValueChange = { month = it }, label = { Text("MONTH", fontWeight = FontWeight.Black) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
            }
        },
        confirmButton = {
            Button(onClick = { onPay(amount.toDoubleOrNull() ?: 0.0, month, "payment", "") }, modifier = Modifier.fillMaxWidth().height(64.dp), shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)) {
                Text("CONFIRM TRANSACTION", fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", fontWeight = FontWeight.Black, color = Slate400) } },
        containerColor = Color.White,
        shape = RoundedCornerShape(44.dp)
    )
}
