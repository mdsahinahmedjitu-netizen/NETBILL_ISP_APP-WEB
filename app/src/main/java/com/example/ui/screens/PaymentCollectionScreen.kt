package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.CustomerEntity
import com.example.data.entity.PaymentCollectionEntity
import com.example.util.AppUtils
import com.example.localization.AppTranslation
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentCollectionScreen(viewModel: MainViewModel) {
    val payments by viewModel.paymentsList.collectAsState()
    val customers by viewModel.customersList.collectAsState()
    val permissions by viewModel.currentPermissions.collectAsState()
    val currency = AppTranslation("currency_symbol")
    val currentUser by viewModel.currentUser.collectAsState()
    val staffList by viewModel.staffList.collectAsState()
    val preSelectedCustomer by viewModel.preSelectedCustomerForPayment.collectAsState()

    var searchTerm by remember { mutableStateOf("") }
    var selectedCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var amount by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("Cash") }

    // Observe pre-selected customer
    LaunchedEffect(preSelectedCustomer) {
        preSelectedCustomer?.let {
            selectedCustomer = it
            searchTerm = it.name
            amount = it.currentDue.toInt().toString()
            // Clear the pre-selection after use
            viewModel.setPreSelectedCustomerForPayment(null)
        }
    }
    val currentMonth = SimpleDateFormat("MMMM yyyy", Locale.US).format(Date())
    var billingMonth by remember { mutableStateOf(currentMonth) }
    
    val months = remember {
        val list = mutableListOf<String>()
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.US)
        repeat(6) {
            list.add(sdf.format(cal.time))
            cal.add(Calendar.MONTH, -1)
        }
        list
    }

    // Dynamic Billing Month Selection Logic
    LaunchedEffect(amount, selectedCustomer) {
        val cust = selectedCustomer ?: return@LaunchedEffect
        val totalDue = cust.currentDue
        val monthlyBill = cust.monthlyBill
        val payAmt = amount.toDoubleOrNull() ?: 0.0

        if (monthlyBill > 0) {
            val monthsDue = Math.ceil(totalDue / monthlyBill).toInt()
            if (payAmt < totalDue && monthsDue >= 2) {
                // Suggest the oldest month
                if (monthsDue <= months.size) {
                    billingMonth = months[monthsDue - 1]
                }
            } else {
                billingMonth = months[0]
            }
        }
    }
    var selectedCollector by remember { mutableStateOf(currentUser?.name ?: "Super Admin") }
    var expandedCollectorDropdown by remember { mutableStateOf(false) }
    var expandedCustomerDropdown by remember { mutableStateOf(false) }

    val filteredCustomers = remember(searchTerm, customers) {
        if (searchTerm.isEmpty()) emptyList()
        else customers.filter {
            it.name.contains(searchTerm, ignoreCase = true) ||
            it.customerCode.contains(searchTerm, ignoreCase = true) ||
            it.pppoeUsername?.contains(searchTerm, ignoreCase = true) == true
        }.take(10)
    }

    Scaffold(containerColor = SleekBg) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(44.dp))
                    .border(1.dp, SleekBorder, RoundedCornerShape(44.dp))
                    .padding(28.dp)
            ) {
                Text(
                    text = AppTranslation("payment_collection").uppercase(),
                    fontWeight = FontWeight.Black,
                    fontSize = 32.sp,
                    letterSpacing = 2.sp,
                    color = Slate900
                )
                Text(
                    text = "ENTERPRISE COLLECTION HUB • REAL-TIME SYNC",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp,
                    color = IspTealPrimary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Panel 1: New Collection Form
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(44.dp),
                color = Color.White,
                border = BorderStroke(1.dp, SleekBorder),
                shadowElevation = 12.dp
            ) {
                Column(modifier = Modifier.padding(32.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    // Collector Selector
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("COLLECTOR ASSIGNMENT", fontSize = 10.sp, fontWeight = FontWeight.Black, color = IspIndigo, letterSpacing = 2.sp)
                        ExposedDropdownMenuBox(
                            expanded = expandedCollectorDropdown,
                            onExpandedChange = { expandedCollectorDropdown = it }
                        ) {
                            OutlinedTextField(
                                value = selectedCollector.uppercase(),
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCollectorDropdown) },
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                textStyle = TextStyle(fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = 14.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFFF1F5F9),
                                    unfocusedContainerColor = Color(0xFFF1F5F9),
                                    focusedBorderColor = Color(0xFFE2E8F0),
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = expandedCollectorDropdown,
                                onDismissRequest = { expandedCollectorDropdown = false }
                            ) {
                                staffList.forEach { staff ->
                                    DropdownMenuItem(
                                        text = { Text(staff.name.uppercase(), fontWeight = FontWeight.Black, letterSpacing = 2.sp) },
                                        onClick = { selectedCollector = staff.name; expandedCollectorDropdown = false }
                                    )
                                }
                            }
                        }
                    }

                    // Subscriber Search
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("FIND SUBSCRIBER", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Slate400, letterSpacing = 3.sp)
                        OutlinedTextField(
                            value = searchTerm.uppercase(),
                            onValueChange = { searchTerm = it; expandedCustomerDropdown = true },
                            placeholder = { Text("SEARCH NAME / ID / PPPOE", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.LightGray, letterSpacing = 2.sp) },
                            shape = RoundedCornerShape(28.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                focusedBorderColor = IspTealPrimary,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                        if (searchTerm.isNotEmpty() && expandedCustomerDropdown) {
                            Surface(
                                modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp),
                                shape = RoundedCornerShape(28.dp),
                                color = Color.White,
                                shadowElevation = 16.dp,
                                border = BorderStroke(4.dp, IspTealPrimary.copy(alpha = 0.1f))
                            ) {
                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                    filteredCustomers.forEach { cust ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedCustomer = cust
                                                    amount = cust.currentDue.toInt().toString()
                                                    searchTerm = cust.name
                                                    expandedCustomerDropdown = false
                                                }
                                                .padding(20.dp)
                                        ) {
                                            Text(cust.name.uppercase(), fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 1.sp)
                                            Text("ZONE: ${cust.zone} • DUE: $currency${cust.currentDue.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = IspRose, letterSpacing = 1.sp)
                                        }
                                        HorizontalDivider(color = SleekBorder.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }

                    // Amount Input (Massive centered style)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(40.dp))
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("COLLECTION AMOUNT (৳)", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Slate500, letterSpacing = 4.sp)
                        BasicTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            textStyle = TextStyle(
                                fontWeight = FontWeight.Black,
                                color = IspTealPrimary,
                                textAlign = TextAlign.Center,
                                fontSize = 64.sp,
                                letterSpacing = 2.sp
                            ),
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    // Commit Button
                    Button(
                        onClick = {
                            selectedCustomer?.let { cust ->
                                viewModel.collectPayment(
                                    customerId = cust.id,
                                    amount = amount.toDoubleOrNull() ?: 0.0,
                                    method = method,
                                    trxId = "",
                                    remarks = "",
                                    date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
                                    collectorName = selectedCollector,
                                    billingMonth = billingMonth
                                )
                                searchTerm = ""; amount = ""; selectedCustomer = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        shape = RoundedCornerShape(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IspTealPrimary),
                        border = BorderStroke(8.dp, Color(0xFF134E4A).copy(alpha = 0.2f))
                    ) {
                        Text("COMMIT TRANSACTION", fontWeight = FontWeight.Black, letterSpacing = 4.sp, fontSize = 18.sp)
                    }
                }
            }

            // History Section
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("COLLECTION HISTORY", fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 4.sp, color = Slate800)
                    Surface(shape = RoundedCornerShape(100.dp), color = Color(0xFFF1F5F9)) {
                        Text("LATEST 20", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 10.sp, fontWeight = FontWeight.Black, color = Slate500, letterSpacing = 2.sp)
                    }
                }

                if (payments.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("NO HISTORY FOUND", color = Slate300, fontWeight = FontWeight.Black, letterSpacing = 8.sp)
                    }
                } else {
                    payments.take(20).forEach { pymt ->
                        PaymentHistoryCard(payment = pymt, currency = currency)
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun PaymentHistoryCard(payment: PaymentCollectionEntity, currency: String) {
    val methodIcon = when (payment.paymentMethod) {
        "Cash" -> Icons.Default.AccountBalanceWallet
        "Bank" -> Icons.Default.AccountBalance
        else -> Icons.Default.PhoneAndroid
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(44.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SleekBorder),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(methodIcon, contentDescription = null, tint = IspTealPrimary, modifier = Modifier.size(28.dp))
                    }
                }

                Spacer(modifier = Modifier.width(20.dp))

                Column {
                    Text(text = payment.billingMonth.uppercase(), fontWeight = FontWeight.Black, color = Slate900, fontSize = 18.sp, letterSpacing = 1.sp)
                    Text(text = "${payment.paymentMethod.uppercase()} • REF: ${payment.receiptNo.uppercase()}", color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Text(text = payment.customerName.uppercase(), color = IspIndigo, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(text = "$currency${payment.amount.toInt()}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = EmeraldSuccess, letterSpacing = 2.sp)
                Text(
                    text = payment.paymentDate.uppercase(), 
                    color = Color(0xFF0F172A), 
                    fontSize = 15.sp, 
                    fontWeight = FontWeight.ExtraBold, 
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
