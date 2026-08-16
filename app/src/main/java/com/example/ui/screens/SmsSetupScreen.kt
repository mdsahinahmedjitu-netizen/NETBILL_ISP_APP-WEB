package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.example.data.entity.SmsTemplateEntity
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsSetupScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit = {}
) {
    val templates by viewModel.smsTemplatesList.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedTemplate by remember { mutableStateOf<SmsTemplateEntity?>(null) }

    val systemSmsTypes = listOf(
        "All Customer", "Area Wise Customer Due List", "Area Wise Customer List",
        "Auto Temporary Disable Alert", "Bill Generate", "Collection",
        "Collection (MFS) to Owner", "Collection Delete", "Collection Edit",
        "Collection to Owner", "Complain Employee", "Complain List",
        "Complain to Customer", "Create Customer", "Create Customer to Owner",
        "Customer Complaint Notification Message", "Free Customer List",
        "Inactive Customer List", "Failed to Disable at Mikrotik", "Expired Customer"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("SMS GATEWAY", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        Text("Automation & Templates", fontSize = 10.sp, color = Teal600, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Teal50)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Bal: ৳7.73", color = Teal600, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(SleekBg)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(systemSmsTypes) { type ->
                    val template = templates.find { it.title == type }
                    val isActive = template?.isActive ?: false
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(if (isActive) Teal600 else Slate200),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.PowerSettingsNew,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = type,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isActive) Slate900 else Slate400
                                )
                                Text(
                                    text = if (isActive) "AUTOMATION ENABLED" else "DISABLED",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isActive) Teal600 else Slate400,
                                    letterSpacing = 1.sp
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val isBroadcastType = type.contains("Customer", ignoreCase = true) || type.contains("List", ignoreCase = true)
                                
                                if (isBroadcastType && isActive) {
                                    IconButton(
                                        onClick = { 
                                            viewModel.broadcastSms(
                                                type = type,
                                                onlyDue = type.contains("Due", ignoreCase = true)
                                            )
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(ElectricBlue.copy(alpha = 0.1f), CircleShape)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                IconButton(
                                    onClick = { 
                                        selectedTemplate = template ?: SmsTemplateEntity(
                                            title = type,
                                            category = "System",
                                            messageContent = "Default message for $type",
                                            isActive = false
                                        )
                                        showEditDialog = true
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Teal50, CircleShape)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = Teal600, modifier = Modifier.size(18.dp))
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Switch(
                                    checked = isActive,
                                    onCheckedChange = { active ->
                                        if (template != null) {
                                            viewModel.updateSmsTemplate(template.copy(isActive = active))
                                        } else {
                                            viewModel.addSmsTemplate(SmsTemplateEntity(
                                                title = type,
                                                category = "System",
                                                messageContent = "Default message for $type",
                                                isActive = active
                                            ))
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Teal600
                                    )
                                )
                            }
                        }
                    }
                }
            }
            
            // Bottom Action Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 8.dp,
                shadowElevation = 16.dp,
                color = Color.White,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("SMS BALANCE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Slate400)
                        Text("৳ 7.73", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Teal600)
                    }
                    
                    Button(
                        onClick = { viewModel.showToast("Settings Synchronized") },
                        modifier = Modifier
                            .height(56.dp)
                            .fillMaxWidth(0.6f),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate900)
                    ) {
                        Text("SAVE CONFIG", fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                    }
                }
            }
        }
    }

    if (showEditDialog && selectedTemplate != null) {
        SmsEditDialog(
            template = selectedTemplate!!,
            onDismiss = { showEditDialog = false },
            onSave = { updated ->
                if (updated.id.isEmpty() || templates.none { it.id == updated.id }) {
                    viewModel.addSmsTemplate(updated)
                } else {
                    viewModel.updateSmsTemplate(updated)
                }
                showEditDialog = false
            }
        )
    }
}

@Composable
fun SmsEditDialog(
    template: SmsTemplateEntity,
    onDismiss: () -> Unit,
    onSave: (SmsTemplateEntity) -> Unit
) {
    var content by remember { mutableStateOf(template.messageContent) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Column {
                Text("EDIT TEMPLATE", fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text(template.title, fontSize = 10.sp, color = Teal600, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Message Content") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Teal600,
                        unfocusedBorderColor = Slate200
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Teal50, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        "TAGS: {NAME}, {TOTAL_DUE}, {AMOUNT}, {EXPIRY_DATE}, {PACKAGE_NAME}, {CUSTOMER_CODE}",
                        fontSize = 10.sp,
                        color = Teal700,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(template.copy(messageContent = content)) },
                colors = ButtonDefaults.buttonColors(containerColor = Teal600),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("UPDATE", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Slate400, fontWeight = FontWeight.Bold)
            }
        }
    )
}
