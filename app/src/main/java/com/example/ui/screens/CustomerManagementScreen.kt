package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.CustomerEntity
import com.example.localization.AppTranslation
import com.example.ui.components.ReadonlyDateField
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerManagementScreen(
    viewModel: MainViewModel,
    onSelectCustomer: (CustomerEntity) -> Unit,
    onNavigateToPayment: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val customers by viewModel.filteredCustomers.collectAsState()
    val allCustomers by viewModel.customersList.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val permissions by viewModel.currentPermissions.collectAsState()
    val currency = AppTranslation("currency_symbol")

    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var customerToEdit by remember { mutableStateOf<CustomerEntity?>(null) }
    val selectedIds by remember { mutableStateOf(setOf<String>()) }
    var activeMenuId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        floatingActionButton = {
            if (permissions.canAdd) {
                FloatingActionButton(
                    onClick = {
                        customerToEdit = null
                        showAddCustomerDialog = true
                    },
                    containerColor = IspTealPrimary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.padding(bottom = 16.dp, end = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Customer", modifier = Modifier.size(32.dp))
                }
            }
        },
        containerColor = SleekBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = IspTealPrimary)
                }
            }

            // Header & Stats Row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(44.dp))
                    .border(1.dp, SleekBorder, RoundedCornerShape(44.dp))
                    .padding(28.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = AppTranslation("subscribers_crm").uppercase(),
                        fontWeight = FontWeight.Black,
                        fontSize = 32.sp,
                        letterSpacing = 2.sp,
                        color = Slate900
                    )
                    Text(
                        text = "ENTERPRISE SUBSCRIBER MANAGEMENT SYSTEM",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp,
                        color = IspTealPrimary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatBox(label = "TOTAL", value = allCustomers.size.toString(), color = Color(0xFF64748B), modifier = Modifier.weight(1f))
                    StatBox(label = "MARKED", value = selectedIds.size.toString(), color = IspIndigo, modifier = Modifier.weight(1f))
                    StatBox(label = "ACTIVE", value = allCustomers.count { it.status == "Active" }.toString(), color = EmeraldSuccess, modifier = Modifier.weight(1f))
                }
            }

            // Toolbar
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, SleekBorder),
                    shadowElevation = 8.dp
                ) {
                    OutlinedTextField(
                        value = filterState.searchQuery,
                        onValueChange = { viewModel.updateFilter(query = it) },
                        placeholder = { Text(AppTranslation("search_placeholder").uppercase(), color = Color.LightGray, fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate300, modifier = Modifier.size(28.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(32.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WebActionButton(label = "ADVANCED FILTER", icon = Icons.Default.FilterList, color = Color.White, textColor = IspTealPrimary, borderColor = IspTealPrimary.copy(alpha = 0.2f)) { /* Filter */ }
                    WebActionButton(label = "DOWNLOAD EXCEL", icon = Icons.Default.FileDownload, color = Color(0xFF20879E), textColor = Color.White) { /* Excel */ }
                    WebActionButton(label = "SMS BROADCAST", icon = Icons.AutoMirrored.Filled.Send, color = Color(0xFF20879E), textColor = Color.White) { /* SMS */ }
                }
            }

            // Table
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(44.dp),
                color = Color.White,
                border = BorderStroke(1.dp, SleekBorder),
                shadowElevation = 10.dp
            ) {
                Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    Column {
                        // Table Header
                        Row(
                            modifier = Modifier
                                .background(Color(0xFFF8FAFC))
                                .padding(vertical = 20.dp, horizontal = 24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TableHeaderCell("SL", width = 50.dp)
                            TableHeaderCell("ID", width = 80.dp)
                            TableHeaderCell("CUSTOMER", width = 250.dp, textAlign = TextAlign.Left)
                            TableHeaderCell("PLAN", width = 100.dp)
                            TableHeaderCell("BILL", width = 100.dp)
                            TableHeaderCell("DUE", width = 100.dp)
                            TableHeaderCell("JOIN DATE", width = 150.dp)
                            TableHeaderCell("EXPIRY", width = 150.dp)
                            TableHeaderCell("STATUS", width = 120.dp)
                            TableHeaderCell("ACTION", width = 100.dp)
                        }

                        HorizontalDivider(color = SleekBorder)

                        customers.forEachIndexed { index, customer ->
                            TableRow(
                                customer = customer,
                                index = index,
                                currency = currency,
                                activeMenuId = activeMenuId,
                                onMenuToggle = { activeMenuId = if (activeMenuId == it) null else it },
                                onPaymentClick = {
                                    viewModel.setPreSelectedCustomerForPayment(it)
                                    onNavigateToPayment()
                                },
                                onSelectCustomer = onSelectCustomer,
                                onEdit = {
                                    customerToEdit = customer
                                    showAddCustomerDialog = true
                                },
                                onDelete = { viewModel.deleteCustomer(customer) }
                            )
                            HorizontalDivider(color = SleekBorder.copy(alpha = 0.5f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    if (showAddCustomerDialog) {
        AddEditCustomerDialog(
            customer = customerToEdit,
            onDismiss = { showAddCustomerDialog = false },
            onSave = { newCust, disc, choice ->
                viewModel.addOrUpdateCustomer(newCust, disc, choice)
                showAddCustomerDialog = false
            }
        )
    }
}

@Composable
fun StatBox(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(color.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
            .border(1.dp, color.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = color.copy(alpha = 0.6f))
        Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.Black, color = color, letterSpacing = 2.sp)
    }
}

@Composable
fun TableHeaderCell(text: String, width: Dp, textAlign: TextAlign = TextAlign.Center) {
    Text(
        text = text,
        modifier = Modifier.width(width),
        fontSize = 10.sp,
        fontWeight = FontWeight.Black,
        color = Color(0xFF64748B),
        letterSpacing = 2.sp,
        textAlign = textAlign
    )
}

@Composable
fun TableRow(
    customer: CustomerEntity,
    index: Int,
    currency: String,
    activeMenuId: String?,
    onMenuToggle: (String) -> Unit,
    onPaymentClick: (CustomerEntity) -> Unit = {},
    onSelectCustomer: (CustomerEntity) -> Unit = {},
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp, horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // SL
        Text(text = (index + 1).toString(), modifier = Modifier.width(50.dp), fontSize = 10.sp, fontWeight = FontWeight.Black, color = Slate400, textAlign = TextAlign.Center)
        // ID
        Text(text = "#${customer.customerCode.takeLast(4)}", modifier = Modifier.width(80.dp), fontSize = 10.sp, fontWeight = FontWeight.Black, color = Slate600, textAlign = TextAlign.Center)
        // CUSTOMER
        Column(modifier = Modifier.width(250.dp)) {
            Text(text = customer.name.uppercase(), fontWeight = FontWeight.Black, fontSize = 18.sp, color = Slate900, letterSpacing = 1.sp)
            Text(text = customer.mobile.uppercase(), fontWeight = FontWeight.Black, fontSize = 16.sp, color = IspIndigo, letterSpacing = 1.sp)
        }
        // PLAN
        Text(text = "${customer.packageName.filter { it.isDigit() }}MB", modifier = Modifier.width(100.dp), fontSize = 18.sp, fontWeight = FontWeight.Black, color = IspTealPrimary, textAlign = TextAlign.Center)
        // BILL
        Text(text = "$currency${customer.monthlyBill.toInt()}", modifier = Modifier.width(100.dp), fontSize = 16.sp, fontWeight = FontWeight.Black, color = Slate800, textAlign = TextAlign.Center)
        // DUE
        Text(text = "$currency${customer.currentDue.toInt()}", modifier = Modifier.width(100.dp), fontSize = 16.sp, fontWeight = FontWeight.Black, color = IspRose, textAlign = TextAlign.Center)
        // JOIN DATE
        Text(text = customer.joinDate ?: "---", modifier = Modifier.width(150.dp), fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A), textAlign = TextAlign.Center, letterSpacing = 1.sp)
        // EXPIRY
        Text(text = customer.expireDate ?: "---", modifier = Modifier.width(150.dp), fontSize = 16.sp, fontWeight = FontWeight.Black, color = IspRose, textAlign = TextAlign.Center, letterSpacing = 1.sp)
        // STATUS
        Box(modifier = Modifier.width(120.dp), contentAlignment = Alignment.Center) {
            StatusBadge(text = customer.status.uppercase(), color = if (customer.status == "Active") Color(0xFFD1FAE5) else Color(0xFFFEE2E2), textColor = if (customer.status == "Active") Color(0xFF047857) else Color(0xFFB91C1C))
        }
        // ACTION
        Box(modifier = Modifier.width(100.dp), contentAlignment = Alignment.Center) {
            IconButton(
                onClick = { onMenuToggle(customer.id) },
                modifier = Modifier.size(40.dp).background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = null, tint = Slate500)
            }

            DropdownMenu(
                expanded = activeMenuId == customer.id,
                onDismissRequest = { onMenuToggle(customer.id) },
                modifier = Modifier.background(Color.White, RoundedCornerShape(32.dp)).border(1.dp, SleekBorder, RoundedCornerShape(32.dp)).padding(vertical = 8.dp)
            ) {
                ActionMenuItem(icon = Icons.Default.Payments, label = "PAYMENT", color = EmeraldSuccess) { 
                    onPaymentClick(customer)
                    onMenuToggle(customer.id) 
                }
                ActionMenuItem(icon = Icons.Default.Person, label = "FULL PROFILE", color = Color(0xFF2563EB)) { 
                    onSelectCustomer(customer)
                    onMenuToggle(customer.id) 
                }
                ActionMenuItem(icon = Icons.Default.Edit, label = "EDIT / IDENTITY", color = Slate600) { 
                    onEdit()
                    onMenuToggle(customer.id) 
                }
                ActionMenuItem(icon = Icons.Default.Delete, label = "DELETE", color = Color(0xFFEF4444)) { 
                    onDelete()
                    onMenuToggle(customer.id) 
                }
            }
        }
    }
}

@Composable
fun StatusBadge(text: String, color: Color, textColor: Color) {
    Surface(color = color, shape = RoundedCornerShape(100.dp)) {
        Text(text = text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), fontSize = 9.sp, fontWeight = FontWeight.Black, color = textColor, letterSpacing = 2.sp)
    }
}

@Composable
fun ActionMenuItem(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Black, color = color, letterSpacing = 2.sp)
            }
        },
        onClick = onClick
    )
}

@Composable
fun WebActionButton(label: String, icon: ImageVector, color: Color, textColor: Color, borderColor: Color? = null, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = color,
        border = if (borderColor != null) BorderStroke(2.dp, borderColor) else null,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = textColor)
        }
    }
}

@Composable
fun AddEditCustomerDialog(
    customer: CustomerEntity?,
    onDismiss: () -> Unit,
    onSave: (CustomerEntity, Double, String) -> Unit
) {
    val code by remember { mutableStateOf(customer?.customerCode ?: "NET-${(1000..9999).random()}") }
    var name by remember { mutableStateOf(customer?.name ?: "") }
    var mobile by remember { mutableStateOf(customer?.mobile ?: "") }
    val altMobile by remember { mutableStateOf(customer?.altMobile ?: "") }

    var address by remember { mutableStateOf(customer?.address ?: "") }
    val zone by remember { mutableStateOf(customer?.zone ?: "Uttara Zone") }
    val subZone by remember { mutableStateOf(customer?.subZone ?: "") }
    val boxId by remember { mutableStateOf(customer?.boxId ?: "") }

    var pkgName by remember { mutableStateOf(customer?.packageName ?: "20 Mbps Super") }
    var billAmt by remember { mutableStateOf(customer?.monthlyBill?.toString() ?: "800") }
    val discountAmt by remember { mutableStateOf("0") }
    var currentDue by remember { mutableStateOf(customer?.currentDue?.toString() ?: "0") }
    val connectionFee by remember { mutableStateOf(customer?.connectionFee?.toString() ?: "1000") }
    val billingType by remember { mutableStateOf(customer?.billingType ?: "Prepaid") }
    
    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
    var joinDate by remember { mutableStateOf(customer?.joinDate?.ifBlank { today } ?: today) }
    var requestDate by remember { mutableStateOf(customer?.requestDate?.ifBlank { today } ?: today) }
    var expireDate by remember { mutableStateOf(customer?.expireDate?.ifBlank { "" } ?: "") }
    var expireTime by remember { mutableStateOf(customer?.expireTime?.ifBlank { "23:59" } ?: "23:59") }

    val connectionType by remember { mutableStateOf(customer?.connectionType ?: "PPPoE") }
    var pppoeUser by remember { mutableStateOf(customer?.pppoeUsername ?: "") }
    val pppoePass by remember { mutableStateOf(customer?.pppoePassword ?: "123456") }
    val onuMac by remember { mutableStateOf(customer?.onuMac ?: "") }
    val onuSerial by remember { mutableStateOf(customer?.onuSerial ?: "") }
    val notes by remember { mutableStateOf(customer?.notes ?: "") }
    var status by remember { mutableStateOf(customer?.status ?: "Active") }

    val billChoiceFor21stPlus by remember { mutableStateOf("NextMonth") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (customer == null) "নতুন গ্রাহক (Add Customer)" else "সংশোধন (Edit Customer)",
                    color = Slate900,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    letterSpacing = 2.sp
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
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("NAME", fontWeight = FontWeight.Black) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("MOBILE", fontWeight = FontWeight.Black) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("ADDRESS", fontWeight = FontWeight.Black) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = pkgName, onValueChange = { pkgName = it }, label = { Text("PACKAGE", fontWeight = FontWeight.Black) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = billAmt, onValueChange = { billAmt = it }, label = { Text("MONTHLY BILL", fontWeight = FontWeight.Black) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = discountAmt, onValueChange = { _ -> }, label = { Text("DISCOUNT (৳)", fontWeight = FontWeight.Black) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = currentDue, onValueChange = { currentDue = it }, label = { Text("PREVIOUS DUE (৳)", fontWeight = FontWeight.Black) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                
                ReadonlyDateField(
                    value = joinDate,
                    label = "JOIN DATE (যোগদানের তারিখ)",
                    onDateSelected = { joinDate = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("STATUS: ", fontWeight = FontWeight.Black, fontSize = 12.sp)
                    listOf("Active", "Inactive").forEach { s ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { status = s }) {
                            RadioButton(selected = status == s, onClick = { status = s })
                            Text(s, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                }

                ReadonlyDateField(
                    value = requestDate,
                    label = "REQUEST DATE (আবেদনের তারিখ)",
                    onDateSelected = { requestDate = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ReadonlyDateField(
                        value = expireDate,
                        label = "EXPIRE DATE",
                        onDateSelected = { expireDate = it },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = expireTime,
                        onValueChange = { expireTime = it },
                        label = { Text("TIME") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(value = pppoeUser, onValueChange = { pppoeUser = it }, label = { Text("PPPOE USERNAME", fontWeight = FontWeight.Black) }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val entity = CustomerEntity(
                        id = customer?.id ?: UUID.randomUUID().toString(),
                        customerCode = code,
                        name = name,
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
                        pppoeUsername = pppoeUser,
                        pppoePassword = pppoePass,
                        onuMac = onuMac,
                        onuSerial = onuSerial,
                        connectionType = connectionType,
                        joinDate = joinDate,
                        expireDate = expireDate,
                        expireTime = expireTime,
                        requestDate = requestDate,
                        status = status,
                        currentDue = currentDue.toDoubleOrNull() ?: 0.0,
                        notes = notes
                    )
                    onSave(entity, discountAmt.toDoubleOrNull() ?: 0.0, billChoiceFor21stPlus)
                },
                colors = ButtonDefaults.buttonColors(containerColor = IspTealPrimary),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("SAVE CLOUD", fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Slate600)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(44.dp)
    )
}
