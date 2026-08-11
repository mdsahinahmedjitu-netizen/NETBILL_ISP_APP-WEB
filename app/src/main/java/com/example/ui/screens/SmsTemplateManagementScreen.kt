package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.SmsTemplateEntity
import com.example.localization.AppTranslation
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsTemplateManagementScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit = {}
) {
    val templates by viewModel.smsTemplatesList.collectAsState()
    var selectedCategoryTab by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    var showAddEditModal by remember { mutableStateOf(false) }
    var editingTemplate by remember { mutableStateOf<SmsTemplateEntity?>(null) }

    var showTestBroadcastModal by remember { mutableStateOf(false) }
    var testingTemplate by remember { mutableStateOf<SmsTemplateEntity?>(null) }

    val categories = listOf("All", "Billing Alert", "Service Downtime", "Payment Receipt", "Network Outage", "General Notice")

    val filteredTemplates = remember(templates, selectedCategoryTab, searchQuery) {
        templates.filter { template ->
            val matchCat = if (selectedCategoryTab == "All") true else template.category == selectedCategoryTab
            val matchSearch = searchQuery.isEmpty() ||
                    template.title.contains(searchQuery, ignoreCase = true) ||
                    template.messageContent.contains(searchQuery, ignoreCase = true) ||
                    template.category.contains(searchQuery, ignoreCase = true)
            matchCat && matchSearch
        }
    }

    val totalCount = templates.size
    val billingCount = remember(templates) { templates.count { it.category == "Billing Alert" } }
    val downtimeCount = remember(templates) { templates.count { it.category == "Service Downtime" || it.category == "Network Outage" } }
    val activeCount = remember(templates) { templates.count { it.isActive } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SleekBg)
            .padding(16.dp)
    ) {
        // Top Bar Navigation & Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White, CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Teal600)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "SMS Notification Templates",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Text(
                        text = "Custom SMS alerts for billing, downtime, receipts & outages",
                        fontSize = 11.sp,
                        color = Slate600
                    )
                }
            }

            Button(
                onClick = {
                    editingTemplate = null
                    showAddEditModal = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Teal600),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Template", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Summary Metric Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SmsMetricChip(
                label = "Total Saved",
                value = "$totalCount",
                icon = Icons.Default.Email,
                color = Teal600,
                modifier = Modifier.weight(1f)
            )
            SmsMetricChip(
                label = "Billing Alerts",
                value = "$billingCount",
                icon = Icons.Default.Receipt,
                color = ElectricBlue,
                modifier = Modifier.weight(1f)
            )
            SmsMetricChip(
                label = "Downtime Alerts",
                value = "$downtimeCount",
                icon = Icons.Default.Warning,
                color = CoralWarning,
                modifier = Modifier.weight(1f)
            )
            SmsMetricChip(
                label = "Active Templates",
                value = "$activeCount",
                icon = Icons.Default.CheckCircle,
                color = SuccessGreen,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search Field & Category Filter Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search template title or text...", fontSize = 12.sp, color = Slate600) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Teal600, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Slate600)
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Teal600,
                    unfocusedBorderColor = Slate200
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Category Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { category ->
                val isSelected = selectedCategoryTab == category
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategoryTab = category },
                    label = {
                        Text(
                            text = when (category) {
                                "All" -> "All Templates ($totalCount)"
                                "Billing Alert" -> "Billing Alerts ($billingCount)"
                                "Service Downtime" -> "Downtime & Maintenance"
                                "Payment Receipt" -> "Payment Receipts"
                                "Network Outage" -> "Network Outages"
                                else -> category
                            },
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Teal600,
                        selectedLabelColor = Color.White,
                        containerColor = Color.White,
                        labelColor = Slate700
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = Slate200,
                        selectedBorderColor = Teal600
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Templates List
        if (filteredTemplates.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = Slate600,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No SMS templates found",
                        fontWeight = FontWeight.Bold,
                        color = Slate800,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Try clearing filters or tap '+ New Template' to create one.",
                        color = Slate600,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredTemplates, key = { it.id }) { template ->
                    SmsTemplateCard(
                        template = template,
                        onEdit = {
                            editingTemplate = template
                            showAddEditModal = true
                        },
                        onTestBroadcast = {
                            testingTemplate = template
                            showTestBroadcastModal = true
                        },
                        onToggleActive = { active ->
                            viewModel.updateSmsTemplate(template.copy(isActive = active))
                        },
                        onDelete = {
                            viewModel.deleteSmsTemplate(template.id)
                        }
                    )
                }
            }
        }
    }

    // Modal Dialog for Add / Edit SMS Template
    if (showAddEditModal) {
        SmsTemplateDialog(
            template = editingTemplate,
            onDismiss = { showAddEditModal = false },
            onSave = { updatedOrNew ->
                if (editingTemplate == null) {
                    viewModel.addSmsTemplate(updatedOrNew)
                } else {
                    viewModel.updateSmsTemplate(updatedOrNew)
                }
                showAddEditModal = false
            }
        )
    }

    // Modal Sheet for Test SMS Dispatch Preview
    if (showTestBroadcastModal && testingTemplate != null) {
        TestSmsBroadcastDialog(
            template = testingTemplate!!,
            onDismiss = { showTestBroadcastModal = false },
            onSendTest = { name, mobile ->
                viewModel.sendTestSmsTemplate(testingTemplate!!, recipientName = name, recipientMobile = mobile)
                showTestBroadcastModal = false
            }
        )
    }
}

@Composable
private fun SmsMetricChip(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = value, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Slate900)
                Text(text = label, fontSize = 9.sp, color = Slate600, maxLines = 1)
            }
        }
    }
}

@Composable
private fun SmsTemplateCard(
    template: SmsTemplateEntity,
    onEdit: () -> Unit,
    onTestBroadcast: () -> Unit,
    onToggleActive: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val categoryColor = when (template.category) {
        "Billing Alert" -> Teal600
        "Service Downtime" -> CoralWarning
        "Payment Receipt" -> SuccessGreen
        "Network Outage" -> Color(0xFFDC2626) // Deep Red
        else -> ElectricBlue
    }

    val containsUnicode = remember(template.messageContent) {
        template.messageContent.any { it.code > 127 }
    }
    val charLimitPerSms = if (containsUnicode) 70 else 160
    val totalChars = template.messageContent.length
    val smsParts = if (totalChars == 0) 1 else ((totalChars - 1) / charLimitPerSms) + 1

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (template.isActive) Slate200 else Slate300)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Title, Category Badge & Active Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .background(categoryColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = template.category,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = categoryColor
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = template.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Slate900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (template.isDefault) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(Teal50, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("DEFAULT", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Teal600)
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (template.isActive) "Active" else "Inactive",
                        fontSize = 11.sp,
                        color = if (template.isActive) SuccessGreen else Slate500,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Switch(
                        checked = template.isActive,
                        onCheckedChange = onToggleActive,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = categoryColor
                        ),
                        modifier = Modifier.height(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Template Message Box with highlighted variable tags
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SleekBg, RoundedCornerShape(10.dp))
                    .border(1.dp, Slate200, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = template.messageContent,
                    fontSize = 13.sp,
                    color = Slate800,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Meta Details: Target Audience, Chars Count, Parts, Last Updated
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.People, contentDescription = null, tint = Slate600, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Target: ${template.targetAudience}", fontSize = 11.sp, color = Slate600)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(if (containsUnicode) Color(0xFFFFF7ED) else Teal50, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "$totalChars chars • $smsParts SMS (${if (containsUnicode) "Bangla/Unicode" else "ASCII"})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (containsUnicode) Color(0xFFC2410C) else Teal700
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Slate200)

            // Card Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (template.lastUpdated.isNotEmpty()) "Updated: ${template.lastUpdated}" else "",
                    fontSize = 10.sp,
                    color = Slate600
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onTestBroadcast,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        border = BorderStroke(1.dp, categoryColor)
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = categoryColor, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Test Send", fontSize = 11.sp, color = categoryColor, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Slate700, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit", fontSize = 11.sp, color = Slate700, fontWeight = FontWeight.Bold)
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CoralWarning, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SmsTemplateDialog(
    template: SmsTemplateEntity?,
    onDismiss: () -> Unit,
    onSave: (SmsTemplateEntity) -> Unit
) {
    var title by remember { mutableStateOf(template?.title ?: "") }
    var category by remember { mutableStateOf(template?.category ?: "Billing Alert") }
    var targetAudience by remember { mutableStateOf(template?.targetAudience ?: "All Active Customers") }
    var messageContent by remember { mutableStateOf(template?.messageContent ?: "") }
    var isActive by remember { mutableStateOf(template?.isActive ?: true) }
    var isDefault by remember { mutableStateOf(template?.isDefault ?: false) }

    val categories = listOf("Billing Alert", "Service Downtime", "Payment Receipt", "Network Outage", "General Notice")
    val audiences = listOf("All Active Customers", "Due Customers", "Zone Customers", "Unpaid Customers", "Specific Package Users")

    val availableTags = listOf(
        "{NAME}" to "Customer Name",
        "{CUSTOMER_CODE}" to "Customer ID",
        "{AMOUNT}" to "Bill Amount ৳",
        "{DUE_DATE}" to "Due Date",
        "{BILL_MONTH}" to "Billing Month",
        "{ZONE}" to "Area Zone",
        "{START_TIME}" to "Start Time",
        "{END_TIME}" to "End Time",
        "{ESTIMATED_TIME}" to "Estimated Duration",
        "{REASON}" to "Downtime Reason",
        "{SUPPORT_PHONE}" to "Support Hotline",
        "{RECEIPT_NO}" to "Receipt No"
    )

    val containsUnicode = messageContent.any { it.code > 127 }
    val charLimitPerSms = if (containsUnicode) 70 else 160
    val totalChars = messageContent.length
    val smsParts = if (totalChars == 0) 1 else ((totalChars - 1) / charLimitPerSms) + 1

    val livePreviewText = remember(messageContent) {
        if (messageContent.isEmpty()) {
            "SMS Preview will appear here as you type..."
        } else {
            messageContent
                .replace("{NAME}", "Rahim Uddin")
                .replace("{CUSTOMER_CODE}", "NET-1001")
                .replace("{AMOUNT}", "800")
                .replace("{DUE_DATE}", "2026-08-20")
                .replace("{BILL_MONTH}", "August 2026")
                .replace("{ZONE}", "Uttara Zone")
                .replace("{START_TIME}", "02:00 PM")
                .replace("{END_TIME}", "05:00 PM")
                .replace("{ESTIMATED_TIME}", "2 Hours")
                .replace("{REASON}", "Fiber Maintenance")
                .replace("{SUPPORT_PHONE}", "01911000000")
                .replace("{RECEIPT_NO}", "REC-99201")
                .replace("{DATE}", "2026-08-11")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = Teal600)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (template == null) "Create New SMS Template" else "Edit SMS Template",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Slate900
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Template Title (e.g., Monthly Bill Due Alert)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Category Selector
                Column {
                    Text("Category", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate700)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categories) { cat ->
                            val isSel = category == cat
                            FilterChip(
                                selected = isSel,
                                onClick = { category = cat },
                                label = { Text(cat, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Teal600,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // Audience Selector
                Column {
                    Text("Target Audience", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate700)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(audiences) { aud ->
                            val isSel = targetAudience == aud
                            FilterChip(
                                selected = isSel,
                                onClick = { targetAudience = aud },
                                label = { Text(aud, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricBlue,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // Dynamic Tag Insertion Bar
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tap to Insert Variables:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Teal600)
                        Text("Auto-replaces per customer", fontSize = 10.sp, color = Slate600)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(availableTags) { (tag, label) ->
                            SuggestionChip(
                                onClick = {
                                    messageContent = if (messageContent.endsWith(" ") || messageContent.isEmpty()) {
                                        messageContent + tag
                                    } else {
                                        "$messageContent $tag"
                                    }
                                },
                                label = { Text("+ $tag", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = Teal50,
                                    labelColor = Teal700
                                ),
                                border = SuggestionChipDefaults.suggestionChipBorder(
                                    enabled = true,
                                    borderColor = Teal200
                                )
                            )
                        }
                    }
                }

                // Message Text Area
                OutlinedTextField(
                    value = messageContent,
                    onValueChange = { messageContent = it },
                    label = { Text("SMS Message Template Content") },
                    placeholder = { Text("Enter SMS text using variables like {NAME}, {AMOUNT}, {ZONE}...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    maxLines = 5,
                    shape = RoundedCornerShape(10.dp)
                )

                // Live Real-Time Customer SMS Preview Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Live Customer View Preview:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate700)
                        Text(
                            text = "$totalChars Chars • $smsParts SMS (${if (containsUnicode) "Unicode" else "ASCII"})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (containsUnicode) Color(0xFFC2410C) else Teal700
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = livePreviewText,
                        fontSize = 12.sp,
                        color = Slate900,
                        lineHeight = 16.sp
                    )
                }

                // Toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = isActive,
                            onCheckedChange = { isActive = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = Teal600)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Enable Template", fontSize = 12.sp, color = Slate800, fontWeight = FontWeight.SemiBold)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isDefault,
                            onCheckedChange = { isDefault = it },
                            colors = CheckboxDefaults.colors(checkedColor = Teal600)
                        )
                        Text("Set Default", fontSize = 12.sp, color = Slate800)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank() || messageContent.isBlank()) return@Button
                    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    val newOrUpdated = SmsTemplateEntity(
                        id = template?.id ?: 0L,
                        title = title.trim(),
                        category = category,
                        targetAudience = targetAudience,
                        messageContent = messageContent.trim(),
                        isActive = isActive,
                        isDefault = isDefault,
                        lastUpdated = dateStr
                    )
                    onSave(newOrUpdated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Teal600),
                enabled = title.isNotBlank() && messageContent.isNotBlank()
            ) {
                Text(if (template == null) "Save Template" else "Update Template", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Slate600)
            }
        }
    )
}

@Composable
private fun TestSmsBroadcastDialog(
    template: SmsTemplateEntity,
    onDismiss: () -> Unit,
    onSendTest: (name: String, mobile: String) -> Unit
) {
    var recipientName by remember { mutableStateOf("Rahim Uddin (NET-1001)") }
    var recipientMobile by remember { mutableStateOf("01712345678") }

    val renderedSms = remember(template.messageContent, recipientName) {
        val cleanName = recipientName.substringBefore(" (")
        template.messageContent
            .replace("{NAME}", cleanName)
            .replace("{CUSTOMER_CODE}", "NET-1001")
            .replace("{AMOUNT}", "800")
            .replace("{DUE_DATE}", "2026-08-20")
            .replace("{BILL_MONTH}", "August 2026")
            .replace("{ZONE}", "Uttara Zone")
            .replace("{START_TIME}", "02:00 PM")
            .replace("{END_TIME}", "05:00 PM")
            .replace("{ESTIMATED_TIME}", "2 Hours")
            .replace("{REASON}", "Fiber Maintenance")
            .replace("{SUPPORT_PHONE}", "01911000000")
            .replace("{RECEIPT_NO}", "REC-99201")
            .replace("{DATE}", "2026-08-11")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Email, contentDescription = null, tint = Teal600)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Test SMS Dispatch Preview", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Template: ${template.title}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate800)

                OutlinedTextField(
                    value = recipientName,
                    onValueChange = { recipientName = it },
                    label = { Text("Test Recipient Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = recipientMobile,
                    onValueChange = { recipientMobile = it },
                    label = { Text("Recipient Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Rendered SMS Text Preview:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Teal600)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SleekBg, RoundedCornerShape(8.dp))
                        .border(1.dp, Teal200, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(renderedSms, fontSize = 12.sp, color = Slate900, lineHeight = 16.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSendTest(recipientName.substringBefore(" ("), recipientMobile) },
                colors = ButtonDefaults.buttonColors(containerColor = Teal600)
            ) {
                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Dispatch Test SMS", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Slate600)
            }
        }
    )
}
