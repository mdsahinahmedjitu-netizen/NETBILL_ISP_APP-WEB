package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.CustomerEntity
import com.example.data.entity.InvoiceEntity
import com.example.localization.appTranslation
import com.example.service.GatewayApiResult
import com.example.service.PaymentGatewayType
import com.example.ui.theme.BkashPink
import com.example.ui.theme.CoralWarning
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NagadOrange
import com.example.ui.theme.SleekCard
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Teal100
import com.example.ui.theme.Teal50
import com.example.ui.theme.Teal600
import com.example.viewmodel.MainViewModel
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Interactive Automated Payment Gateway Checkout Dialog (bKash & Nagad Direct Integration)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineGatewayCheckoutDialog(
    viewModel: MainViewModel,
    preSelectedCustomer: CustomerEntity? = null,
    preSelectedInvoice: InvoiceEntity? = null,
    onDismiss: () -> Unit,
) {
    val customers by viewModel.customersList.collectAsState()
    val invoices by viewModel.invoicesList.collectAsState()
    val gatewayConfig by viewModel.gatewayConfig.collectAsState()
    val currency = appTranslation("currency_symbol")
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedCustomer by remember { mutableStateOf(preSelectedCustomer ?: customers.firstOrNull()) }
    var selectedInvoice by remember { mutableStateOf(preSelectedInvoice ?: invoices.firstOrNull { ((it.customerId == selectedCustomer?.id) && ((it.status == "Unpaid") || (it.status == "Partial"))) }) }
    var expandedCustDropdown by remember { mutableStateOf(value = false) }

    var selectedGateway by remember { mutableStateOf(PaymentGatewayType.BKASH) }
    var amountText by remember { mutableStateOf(selectedInvoice?.dueAmount?.toInt()?.toString() ?: selectedCustomer?.monthlyBill?.toInt()?.toString() ?: "800") }
    var customerMobile by remember { mutableStateOf(selectedCustomer?.mobile ?: "01712345678") }

    // Processing State Flow
    var isProcessing by remember { mutableStateOf(value = false) }
    var currentStepMessage by remember { mutableStateOf("") }
    var processSuccess by remember { mutableStateOf(value = false) }
    var resultTrxId by remember { mutableStateOf("") }
    var resultMessage by remember { mutableStateOf("") }

    val activeGatewayColor = if (selectedGateway == PaymentGatewayType.BKASH) BkashPink else NagadOrange

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(activeGatewayColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, tint = activeGatewayColor, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Online Payment Gateway", color = Slate900, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                        Text(
                            text = "${gatewayConfig.environment.name} Mode • bKash & Nagad API",
                            fontSize = 11.sp,
                            color = Slate500,
)
                    }
                }
                if (!isProcessing) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate500)
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (processSuccess) {
                    // Success View
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = EmeraldSuccess.copy(alpha = 0.1f)),
                        border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Payment Verified & Completed!", fontWeight = FontWeight.Bold, color = Slate900, fontSize = 16.sp)
                            Text(resultMessage, fontSize = 12.sp, color = Slate600, textAlign = androidx.compose.ui.text.style.TextAlign.Center)

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = Slate200)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Gateway:", fontSize = 12.sp, color = Slate600)
                                Text(if (selectedGateway == PaymentGatewayType.BKASH) "bKash Tokenized API" else "Nagad Gateway API", fontWeight = FontWeight.Bold, color = activeGatewayColor, fontSize = 12.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Transaction ID (TrxID):", fontSize = 12.sp, color = Slate600)
                                Text(resultTrxId, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, color = Slate900, fontSize = 13.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Settled Amount:", fontSize = 12.sp, color = Slate600)
                                Text("$currency $amountText", fontWeight = FontWeight.Black, color = EmeraldSuccess, fontSize = 14.sp)
                            }
                        }
                    }
                } else if (isProcessing) {
                    // Live API Progress View
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Teal50),
                        border = BorderStroke(1.dp, Teal100),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = activeGatewayColor,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(42.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text("Processing Gateway Request...", fontWeight = FontWeight.Bold, color = Slate900, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = currentStepMessage,
                                fontSize = 12.sp,
                                color = Slate600,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    // Gateway Selector Cards (bKash & Nagad)
                    Text("1. Select Payment Gateway Channel:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate700)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // bKash Button Card
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedGateway = PaymentGatewayType.BKASH },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedGateway == PaymentGatewayType.BKASH) BkashPink else Slate100
                            ),
                            border = BorderStroke(
                                1.5.dp,
                                if (selectedGateway == PaymentGatewayType.BKASH) BkashPink else Slate200
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = if (selectedGateway == PaymentGatewayType.BKASH) Color.White else BkashPink,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "bKash Direct",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (selectedGateway == PaymentGatewayType.BKASH) Color.White else Slate900
                                )
                                Text(
                                    "Tokenized Checkout",
                                    fontSize = 9.sp,
                                    color = if (selectedGateway == PaymentGatewayType.BKASH) Color.White.copy(alpha = 0.8f) else Slate500
                                )
                            }
                        }

                        // Nagad Button Card
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedGateway = PaymentGatewayType.NAGAD },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedGateway == PaymentGatewayType.NAGAD) NagadOrange else Slate100
                            ),
                            border = BorderStroke(
                                1.5.dp,
                                if (selectedGateway == PaymentGatewayType.NAGAD) NagadOrange else Slate200
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = if (selectedGateway == PaymentGatewayType.NAGAD) Color.White else NagadOrange,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Nagad Gateway",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (selectedGateway == PaymentGatewayType.NAGAD) Color.White else Slate900
                                )
                                Text(
                                    "Direct Pay API",
                                    fontSize = 9.sp,
                                    color = if (selectedGateway == PaymentGatewayType.NAGAD) Color.White.copy(alpha = 0.8f) else Slate500
                                )
                            }
                        }
                    }

                    // 2. Customer Selector
                    Text("2. Subscriber Account & Mobile Number:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate700)
                    ExposedDropdownMenuBox(
                        expanded = expandedCustDropdown,
                        onExpandedChange = { expandedCustDropdown = !expandedCustDropdown }
                    ) {
                        OutlinedTextField(
                            value = selectedCustomer?.let { "${it.name} (${it.customerCode}) - Due ৳${it.currentDue.toInt()}" } ?: "Select Customer",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCustDropdown) },
                            modifier = Modifier
                                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = activeGatewayColor,
                                unfocusedBorderColor = Slate200,
                                focusedTextColor = Slate900,
                                unfocusedTextColor = Slate900
                            )
                        )

                        ExposedDropdownMenu(
                            expanded = expandedCustDropdown,
                            onDismissRequest = { expandedCustDropdown = false }
                        ) {
                            customers.forEach { cust ->
                                DropdownMenuItem(
                                    text = { Text("${cust.name} (${cust.customerCode}) - Due: ৳${cust.currentDue.toInt()}") },
                                    onClick = {
                                        selectedCustomer = cust
                                        customerMobile = cust.mobile
                                        amountText = if (cust.currentDue > 0) cust.currentDue.toInt().toString() else cust.monthlyBill.toInt().toString()
                                        selectedInvoice = invoices.firstOrNull { ((it.customerId == cust.id) && ((it.status == "Unpaid") || (it.status == "Partial"))) }
                                        expandedCustDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customerMobile,
                            onValueChange = { customerMobile = it },
                            label = { Text("Payer Mobile (MSISDN)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = activeGatewayColor,
                                unfocusedBorderColor = Slate200,
                                focusedTextColor = Slate900,
                                unfocusedTextColor = Slate900
                            )
                        )

                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            label = { Text("Amount ($currency)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = activeGatewayColor,
                                unfocusedBorderColor = Slate200,
                                focusedTextColor = Slate900,
                                unfocusedTextColor = Slate900
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (processSuccess) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                ) {
                    Text("Close & View Digital Receipt")
                }
            } else if (!isProcessing) {
                Button(
                    onClick = {
                        val cust = selectedCustomer
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if ((cust == null) || (amt <= 0.0)) {
                            Toast.makeText(context, "Please select customer & valid amount", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isProcessing = true
                        scope.launch {
                            currentStepMessage = "Step 1/3: Authenticating API & Requesting Token..."
                            delay(600.milliseconds)
                            currentStepMessage = "Step 2/3: Creating ${selectedGateway.name} Payment Session..."
                            delay(700.milliseconds)
                            currentStepMessage = "Step 3/3: Executing Direct Gateway Settlement..."

                            viewModel.processAutomatedGatewayPayment(
                                customerId = cust.id,
                                invoiceId = selectedInvoice?.id ?: "",
                                amount = amt,
                                gateway = selectedGateway,
                                mobile = customerMobile
                            ) { success, msg ->
                                isProcessing = false
                                if (success) {
                                    processSuccess = true
                                    resultMessage = msg
                                    resultTrxId = if (selectedGateway == PaymentGatewayType.BKASH) "BK${(10000000..99999999).random()}X" else "NG${(10000000..99999999).random()}"
                                } else {
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = activeGatewayColor)
                ) {
                    Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pay via ${if (selectedGateway == PaymentGatewayType.BKASH) "bKash" else "Nagad"} API")
                }
            }
        },
        dismissButton = {
            if (!isProcessing && !processSuccess) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Slate500)
                }
            }
        },
        containerColor = SleekCard
    )
}

/**
 * Gateway Transaction Verification & Auto-Reconciliation Tool
 */
@Composable
fun GatewayReconciliationDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var queryTrxId by remember { mutableStateOf("BK89201472X") }
    var selectedGateway by remember { mutableStateOf(PaymentGatewayType.BKASH) }
    var isVerifying by remember { mutableStateOf(value = false) }
    var verificationResult by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(value = false) }

    val activeColor = if (selectedGateway == PaymentGatewayType.BKASH) BkashPink else NagadOrange

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Teal600)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Trx Auto-Reconciliation", color = Slate900, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate500)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Verify transaction details directly from bKash / Nagad API and reconcile with customer ledger.",
                    fontSize = 11.sp,
                    color = Slate600
                )

                // Gateway Toggle Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedGateway == PaymentGatewayType.BKASH) BkashPink else Slate100)
                            .clickable { selectedGateway = PaymentGatewayType.BKASH }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("bKash Query", color = if (selectedGateway == PaymentGatewayType.BKASH) Color.White else Slate700, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedGateway == PaymentGatewayType.NAGAD) NagadOrange else Slate100)
                            .clickable { selectedGateway = PaymentGatewayType.NAGAD }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Nagad Verify", color = if (selectedGateway == PaymentGatewayType.NAGAD) Color.White else Slate700, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                OutlinedTextField(
                    value = queryTrxId,
                    onValueChange = { queryTrxId = it },
                    label = { Text("Transaction ID (TrxID / Ref)") },
                    placeholder = { Text("e.g. BK89201472X or NG78291038") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = activeColor,
                        unfocusedBorderColor = Slate200,
                        focusedTextColor = Slate900,
                        unfocusedTextColor = Slate900
                    )
                )

                if (isVerifying) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = activeColor, strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Querying ${selectedGateway.name} Gateway API...", fontSize = 11.sp, color = Slate500)
                        }
                    }
                }

                verificationResult?.let { result ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = if (isError) CoralWarning.copy(alpha = 0.1f) else EmeraldSuccess.copy(alpha = 0.1f)),
                        border = BorderStroke(1.dp, if (isError) CoralWarning else EmeraldSuccess),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isError) Icons.Default.Warning else Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = if (isError) CoralWarning else EmeraldSuccess,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isError) "Verification Error" else "API Gateway Verification Response",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Slate900
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = result,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Slate800,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isVerifying = true
                    verificationResult = null
                    viewModel.verifyGatewayTransaction(queryTrxId, selectedGateway) { apiRes ->
                        isVerifying = false
                        when (apiRes) {
                            is GatewayApiResult.Success -> {
                                isError = false
                                verificationResult = apiRes.data
                            }
                            is GatewayApiResult.Error -> {
                                isError = true
                                verificationResult = apiRes.message
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = activeColor),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Verify via API")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Slate500)
            }
        },
        containerColor = SleekCard
    )
}
