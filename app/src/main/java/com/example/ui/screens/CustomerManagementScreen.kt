package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import com.example.ui.theme.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.CustomerEntity
import com.example.localization.AppTranslation
import com.example.ui.components.ReadonlyDateField
import java.util.UUID
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.CoralWarning
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerManagementScreen(
    viewModel: MainViewModel,
    onSelectCustomer: (CustomerEntity) -> Unit
) {
    val customers by viewModel.filteredCustomers.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val permissions by viewModel.currentPermissions.collectAsState()
    val currency = AppTranslation("currency_symbol")

    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var customerToEdit by remember { mutableStateOf<CustomerEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            if (permissions.canAdd) {
                FloatingActionButton(
                    onClick = {
                        customerToEdit = null
                        showAddCustomerDialog = true
                    },
                    containerColor = ElectricBlue,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Customer")
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
            // Search Input with Clear Button
            OutlinedTextField(
                value = filterState.searchQuery,
                onValueChange = { viewModel.updateFilter(query = it) },
                placeholder = { Text("Search by Customer ID, Name, Phone Number, or PPPoE...", color = Slate600, fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon", tint = Teal600) },
                trailingIcon = {
                    if (filterState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateFilter(query = "") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = Slate600)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Teal600,
                    unfocusedBorderColor = Slate200,
                    focusedContainerColor = SleekCard,
                    unfocusedContainerColor = SleekCard
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Search results counter & active filters badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (filterState.searchQuery.isNotEmpty()) "Found ${customers.size} customer(s) matching \"${filterState.searchQuery}\"" else "Total Customers: ${customers.size}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (filterState.searchQuery.isNotEmpty()) Teal600 else Slate600
                )

                if (filterState.searchQuery.isNotEmpty()) {
                    TextButton(
                        onClick = { viewModel.updateFilter(query = "") },
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Reset Search", fontSize = 11.sp, color = CoralWarning)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Filters Row: Status Chips & Due Only Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("All", "Active", "Suspended").forEach { status ->
                        val isSelected = filterState.selectedStatus == status
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.updateFilter(status = status) },
                            label = { Text(status, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Teal600,
                                selectedLabelColor = Color.White,
                                containerColor = SleekCard,
                                labelColor = Slate700
                            )
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Due Only", color = CoralWarning, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Switch(
                        checked = filterState.onlyDueCustomers,
                        onCheckedChange = { viewModel.updateFilter(onlyDue = it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = CoralWarning
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Customer List
            if (customers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No customers found", color = Slate600)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(customers, key = { it.id }) { customer ->
                        CustomerCard(
                            customer = customer,
                            currency = currency,
                            permissions = permissions,
                            onClick = { onSelectCustomer(customer) },
                            onEdit = {
                                customerToEdit = customer
                                showAddCustomerDialog = true
                            },
                            onDelete = { viewModel.deleteCustomer(customer) },
                            onToggleStatus = { viewModel.toggleCustomerStatus(customer) }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Customer Dialog
    if (showAddCustomerDialog) {
        AddEditCustomerDialog(
            customer = customerToEdit,
            permissions = permissions,
            onDismiss = { showAddCustomerDialog = false },
            onSave = { newCust, choice ->
                viewModel.addOrUpdateCustomer(newCust, choice)
                showAddCustomerDialog = false
            }
        )
    }
}

@Composable
fun CustomerCard(
    customer: CustomerEntity,
    currency: String,
    permissions: com.example.data.entity.UserRolePermissions,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleStatus: () -> Unit
) {
    val isDue = customer.currentDue > 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SleekCard),
        border = BorderStroke(1.dp, SleekBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (customer.status == "Active") ElectricBlue.copy(alpha = 0.2f) else CoralWarning.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = customer.customerCode.takeLast(4),
                            fontWeight = FontWeight.Bold,
                            color = if (customer.status == "Active") Teal600 else CoralWarning,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = customer.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Slate900
                        )
                        Text(
                            text = "PPPoE: ${customer.pppoeUsername} • ${customer.zone}",
                            fontSize = 12.sp,
                            color = Slate600
                        )
                        if (permissions.canSeeMobile) {
                            Text(
                                text = "Mobile: ${customer.mobile}",
                                fontSize = 11.sp,
                                color = Teal600,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Due Tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDue) CoralWarning.copy(alpha = 0.2f) else EmeraldSuccess.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isDue) "Due: $currency ${String.format(java.util.Locale.US, "%,.0f", customer.currentDue)}" else "Paid",
                        color = if (isDue) CoralWarning else EmeraldSuccess,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            // Extra detail tags row (NID, ONU, Fiber Core, Billing Type)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (customer.billingType.orEmpty().isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Teal600.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = customer.billingType!!, fontSize = 10.sp, color = Teal600, fontWeight = FontWeight.Medium)
                    }
                }
                if (customer.expireDate.orEmpty().isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "মেয়াদ: ${customer.expireDate} ${customer.expireTime.orEmpty()}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pkg: ${customer.packageName} ($currency ${String.format(java.util.Locale.US, "%,.0f", customer.monthlyBill)}/mo)",
                    fontSize = 12.sp,
                    color = CyanAccent,
                    fontWeight = FontWeight.SemiBold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (permissions.canSuspend) {
                        IconButton(onClick = onToggleStatus, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = if (customer.status == "Active") Icons.Default.CheckCircle else Icons.Default.Block,
                                contentDescription = "Toggle Status",
                                tint = if (customer.status == "Active") EmeraldSuccess else CoralWarning,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (permissions.canEdit) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = CyanAccent, modifier = Modifier.size(18.dp))
                        }
                    }
                    if (permissions.canDelete) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CoralWarning, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // 20th Day Join Indicator
            if (customer.joinDayOfMonth > 20) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = AmberAlert, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Joined on day ${customer.joinDayOfMonth} (Special 20th Day Rule)",
                        color = AmberAlert,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AddEditCustomerDialog(
    customer: CustomerEntity?,
    permissions: com.example.data.entity.UserRolePermissions,
    onDismiss: () -> Unit,
    onSave: (CustomerEntity, String) -> Unit
) {
    var code by remember { mutableStateOf(customer?.customerCode ?: "NET-${(1000..9999).random()}") }
    var name by remember { mutableStateOf(customer?.name ?: "") }
    var mobile by remember { mutableStateOf(customer?.mobile ?: "") }
    var altMobile by remember { mutableStateOf(customer?.altMobile ?: "") }

    var address by remember { mutableStateOf(customer?.address ?: "") }
    var zone by remember { mutableStateOf(customer?.zone ?: "Uttara Zone") }
    var subZone by remember { mutableStateOf(customer?.subZone ?: "") }
    var boxId by remember { mutableStateOf(customer?.boxId ?: "") }

    var pkgName by remember { mutableStateOf(customer?.packageName ?: "20 Mbps Super") }
    var billAmt by remember { mutableStateOf(customer?.monthlyBill?.toString() ?: "800") }
    var connectionFee by remember { mutableStateOf(customer?.connectionFee?.toString() ?: "1000") }
    var billingType by remember { mutableStateOf(customer?.billingType ?: "Prepaid") }
    var joinDate by remember { mutableStateOf(customer?.joinDate?.ifEmpty { "2026-08-12" } ?: "2026-08-12") }
    var currentDue by remember { mutableStateOf(customer?.currentDue?.toString() ?: "0") }
    var expireDate by remember { mutableStateOf(customer?.expireDate?.ifEmpty { "2026-09-12" } ?: "2026-09-12") }
    var expireTime by remember { mutableStateOf(customer?.expireTime?.ifEmpty { "23:59" } ?: "23:59") }

    var connectionType by remember { mutableStateOf(customer?.connectionType ?: "PPPoE") }
    var pppoeUser by remember { mutableStateOf(customer?.pppoeUsername ?: "") }
    var pppoePass by remember { mutableStateOf(customer?.pppoePassword ?: "123456") }
    var onuMac by remember { mutableStateOf(customer?.onuMac ?: "") }
    var onuSerial by remember { mutableStateOf(customer?.onuSerial ?: "") }
    var notes by remember { mutableStateOf(customer?.notes ?: "") }

    // New state for 20th day rule billing choice
    var billChoiceFor21stPlus by remember { mutableStateOf("NextMonth") } // "CurrentMonth" or "NextMonth"

    val joinDayInt = try { joinDate.split("-").last().toInt() } catch(e: Exception) { 15 }
    val show20thDayWarning = joinDayInt > 20

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (customer == null) "নতুন গ্রাহক যোগ করুন (Add Customer)" else "গ্রাহকের তথ্য সংশোধন (Edit Customer)",
                    color = Slate900,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Text(
                    text = "Customer Code: $code",
                    color = Teal600,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section 1: Basic Info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate100),
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("👤 প্রাথমিক ও ব্যক্তিগত পরিচয় (Basic Details)", fontWeight = FontWeight.Bold, color = Slate800, fontSize = 13.sp)

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("গ্রাহকের নাম (বাংলা / English)") },
                            placeholder = { Text("যেমন: মো: রফিকুল ইসলাম / Rafiqul Islam", color = Slate600, fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            readOnly = !permissions.canEdit && customer != null
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = mobile,
                                onValueChange = { mobile = it },
                                label = { Text("মোবাইল নম্বর") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                readOnly = (!permissions.canEdit && customer != null) || !permissions.canSeeMobile
                            )
                            OutlinedTextField(
                                value = altMobile,
                                onValueChange = { altMobile = it },
                                label = { Text("বিকল্প মোবাইল") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                readOnly = (!permissions.canEdit && customer != null) || !permissions.canSeeMobile
                            )
                        }
                    }
                }

                // Section 2: Address & Location Info
                if (permissions.canSeeAddress) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Slate100),
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🏠 ঠিকানা ও এলাকা তথ্য (Address & Location)", fontWeight = FontWeight.Bold, color = Slate800, fontSize = 13.sp)

                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = { Text("পূর্ণাঙ্গ ঠিকানা") },
                                placeholder = { Text("যেমন: বাড়ি-১২, রোড-০৫, সেক্টর-৩, উত্তরা", color = Slate600, fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = !permissions.canEdit && customer != null
                            )

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = zone,
                                    onValueChange = { zone = it },
                                    label = { Text("জোন / এলাকা") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    readOnly = !permissions.canEdit && customer != null
                                )
                                OutlinedTextField(
                                    value = subZone,
                                    onValueChange = { subZone = it },
                                    label = { Text("সাব-জোন / ব্লক") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    readOnly = !permissions.canEdit && customer != null
                                )
                            }

                            OutlinedTextField(
                                value = boxId,
                                onValueChange = { boxId = it },
                                label = { Text("নেটওয়ার্ক বক্স / Splitter TJ ID") },
                                placeholder = { Text("যেমন: BOX-101", color = Slate600, fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                readOnly = !permissions.canEdit && customer != null
                            )
                        }
                    }
                }

                // Section 3: Package & Billing
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate100),
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("💰 প্যাকেজ ও বিলিং তথ্য (Package & Bill)", fontWeight = FontWeight.Bold, color = Slate800, fontSize = 13.sp)

                        OutlinedTextField(
                            value = pkgName,
                            onValueChange = { pkgName = it },
                            label = { Text("ইন্টারনেট প্যাকেজ") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            readOnly = !permissions.canModifyPricing
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = billAmt,
                                onValueChange = { billAmt = it },
                                label = { Text("মাসিক বিল (৳)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                readOnly = !permissions.canModifyPricing
                            )
                            ReadonlyDateField(
                                value = joinDate,
                                label = "যোগদানের তারিখ",
                                onDateSelected = { joinDate = it },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("বিলিং টাইপ:", fontSize = 12.sp, color = Slate700, fontWeight = FontWeight.Bold)
                            listOf("Prepaid", "Postpaid").forEach { type ->
                                FilterChip(
                                    selected = billingType == type,
                                    onClick = { billingType = type },
                                    label = { Text(type, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Teal600,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        OutlinedTextField(
                            value = currentDue,
                            onValueChange = { currentDue = it },
                            label = { Text("পূর্ববর্তী বকেয়া পরিমাণ (৳)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            readOnly = !permissions.canModifyPricing
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("⏱️ মেয়াদের তারিখ ও সময় (Expiry Date & Time)", fontWeight = FontWeight.Bold, color = Slate800, fontSize = 12.sp)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ReadonlyDateField(
                                value = expireDate,
                                label = "মেয়াদ শেষ তারিখ",
                                onDateSelected = { expireDate = it },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = expireTime,
                                onValueChange = { expireTime = it },
                                label = { Text("মেয়াদ শেষ সময় (HH:MM / AM-PM)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                readOnly = !permissions.canEdit && customer != null
                            )
                        }

                        if (permissions.canEdit || customer == null) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("দ্রুত সেট:", fontSize = 11.sp, color = Slate600, fontWeight = FontWeight.SemiBold)
                                FilterChip(
                                    selected = expireDate == "2026-08-31",
                                    onClick = {
                                        expireDate = "2026-08-31"
                                        expireTime = "11:59 PM"
                                    },
                                    label = { Text("আজ রাত 11:59", fontSize = 10.sp) }
                                )
                                FilterChip(
                                    selected = expireDate == "2026-09-08",
                                    onClick = {
                                        expireDate = "2026-09-08"
                                        expireTime = "11:59 PM"
                                    },
                                    label = { Text("+৭ দিন", fontSize = 10.sp) }
                                )
                                FilterChip(
                                    selected = expireDate == "2026-09-30",
                                    onClick = {
                                        expireDate = "2026-09-30"
                                        expireTime = "11:59 PM"
                                    },
                                    label = { Text("+৩০ দিন", fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                }

                // Section 4: Network & Technical Details
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate100),
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🌐 নেটওয়ার্ক ও টেকনিক্যাল তথ্য (Network Specs)", fontWeight = FontWeight.Bold, color = Slate800, fontSize = 13.sp)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = pppoeUser,
                                onValueChange = { pppoeUser = it },
                                label = { Text("PPPoE Username") },
                                placeholder = { Text("অটো জেনারেট হবে", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                readOnly = !permissions.canEdit && customer != null
                            )
                            if (permissions.canPasswords || customer == null) {
                                OutlinedTextField(
                                    value = pppoePass,
                                    onValueChange = { pppoePass = it },
                                    label = { Text("PPPoE Password") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    readOnly = !permissions.canEdit && customer != null
                                )
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = onuMac,
                                onValueChange = { onuMac = it },
                                label = { Text("ONU MAC") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                readOnly = !permissions.canEdit && customer != null
                            )
                            OutlinedTextField(
                                value = onuSerial,
                                onValueChange = { onuSerial = it },
                                label = { Text("ONU Serial") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                readOnly = !permissions.canEdit && customer != null
                            )
                        }

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("অন্যান্য মন্তব্য / নোট (Notes)") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = !permissions.canEdit && customer != null
                        )
                    }
                }

                // 20th Day Rule Alert Banner
                AnimatedVisibility(visible = show20thDayWarning) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AmberAlert.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = AmberAlert)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "২১ তারিখ বা তার পরে জয়েনিং। বিলিং অপশন সিলেক্ট করুন:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberAlert
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = billChoiceFor21stPlus == "CurrentMonth",
                                    onClick = { billChoiceFor21stPlus = "CurrentMonth" },
                                    colors = RadioButtonDefaults.colors(selectedColor = Teal600)
                                )
                                Text("রানিং মাসের বিল ধরুন", fontSize = 11.sp, color = Slate800)
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                RadioButton(
                                    selected = billChoiceFor21stPlus == "NextMonth",
                                    onClick = { billChoiceFor21stPlus = "NextMonth" },
                                    colors = RadioButtonDefaults.colors(selectedColor = Teal600)
                                )
                                Text("আগামী মাস থেকে বিল শুরু", fontSize = 11.sp, color = Slate800)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val asciiNamePart = name.filter { it.isLetterOrDigit() && it.code < 128 }.lowercase().replace(" ", "_")
                    val finalPppoeUser = if (pppoeUser.isNotBlank()) pppoeUser else {
                        if (asciiNamePart.isNotBlank()) "${asciiNamePart}_${code.takeLast(4)}" else "user_${code.takeLast(4)}"
                    }
                    val finalName = if (name.isNotBlank()) name else "গ্রাহক ${code.takeLast(4)}"
                    val entity = CustomerEntity(
                        id = customer?.id ?: UUID.randomUUID().toString(),
                        customerCode = code,
                        name = finalName,
                        mobile = mobile,
                        altMobile = altMobile,
                        address = address,
                        zone = zone,
                        subZone = subZone,
                        boxId = boxId,
                        packageName = pkgName,
                        monthlyBill = billAmt.toDoubleOrNull() ?: 800.0,
                        connectionFee = connectionFee.toDoubleOrNull() ?: 0.0,
                        billingType = billingType,
                        pppoeUsername = finalPppoeUser,
                        pppoePassword = pppoePass,
                        onuMac = onuMac,
                        onuSerial = onuSerial,
                        connectionType = connectionType,
                        joinDate = joinDate,
                        expireDate = expireDate,
                        expireTime = expireTime,
                        status = customer?.status ?: "Active",
                        currentDue = currentDue.toDoubleOrNull() ?: 0.0,
                        notes = notes
                    )
                    onSave(entity, billChoiceFor21stPlus)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Teal600)
            ) {
                Text("সংরক্ষণ করুন (Save Customer)", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল (Cancel)", color = Slate600)
            }
        },
        containerColor = SleekCard
    )
}
