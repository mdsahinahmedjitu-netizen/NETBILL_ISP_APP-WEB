package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.InventoryEntity
import com.example.viewmodel.MainViewModel
import com.example.ui.theme.*
import com.example.ui.components.ReadonlyDateField
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(viewModel: MainViewModel) {
    val inventory by viewModel.inventoryList.collectAsState()
    val customers by viewModel.customersList.collectAsState()

    var showAddItemDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<InventoryEntity?>(null) }
    var itemToAssign by remember { mutableStateOf<InventoryEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddItemDialog = true },
                containerColor = ElectricBlue,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Item")
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
                text = "Inventory & Stock Management",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Slate900
            )
            Text(
                text = "Track ONU, Routers, Cables & Equipment",
                fontSize = 12.sp,
                color = Slate600
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Stats Row
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InventoryStatCard("Total Items", "${inventory.size}", ElectricBlue, Modifier.weight(1f))
                InventoryStatCard("In Stock", "${inventory.count { it.status == "In Stock" }}", EmeraldSuccess, Modifier.weight(1f))
                InventoryStatCard("Assigned", "${inventory.count { it.status == "Assigned" }}", Teal600, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (inventory.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No inventory items found. Add your stock items.", color = Slate500)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(inventory, key = { it.id }) { item ->
                        InventoryItemCard(
                            item = item,
                            customerName = customers.find { it.id == item.assignedToCustomerId }?.name,
                            onEdit = { itemToEdit = item },
                            onAssign = { itemToAssign = item },
                            onDelete = { viewModel.deleteInventoryItem(item.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddItemDialog || itemToEdit != null) {
        AddEditInventoryDialog(
            item = itemToEdit,
            onDismiss = {
                showAddItemDialog = false
                itemToEdit = null
            },
            onSave = { newItem ->
                if (itemToEdit != null) viewModel.updateInventoryItem(newItem)
                else viewModel.addInventoryItem(newItem)
                showAddItemDialog = false
                itemToEdit = null
            }
        )
    }

    if (itemToAssign != null) {
        AssignInventoryDialog(
            item = itemToAssign!!,
            customers = customers,
            onDismiss = { itemToAssign = null },
            onAssign = { customerId ->
                viewModel.assignInventoryToCustomer(itemToAssign!!.id, customerId)
                itemToAssign = null
            }
        )
    }
}

@Composable
fun InventoryStatCard(label: String, value: String, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SleekCard),
        border = BorderStroke(1.dp, SleekBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, fontSize = 10.sp, color = Slate600, fontWeight = FontWeight.Bold)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = color)
        }
    }
}

@Composable
fun InventoryItemCard(
    item: InventoryEntity,
    customerName: String?,
    onEdit: () -> Unit,
    onAssign: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when (item.status) {
        "In Stock" -> EmeraldSuccess
        "Assigned" -> ElectricBlue
        else -> CoralWarning
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SleekCard),
        border = BorderStroke(1.dp, SleekBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (item.itemName.contains("Cable", true)) Icons.Default.LinearScale else Icons.Default.Router,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(item.itemName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Slate900)
                        Text("${item.brand} • SN: ${item.serialNumber}", fontSize = 11.sp, color = Slate600)
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(item.status, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (item.status == "Assigned" && customerName != null) {
                Surface(
                    color = ElectricBlue.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Assigned to: $customerName", fontSize = 12.sp, color = Slate800, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Price: ৳${item.costPrice.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Teal600)
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (item.status == "In Stock") {
                        IconButton(onClick = onAssign, modifier = Modifier.size(32.dp).background(ElectricBlue.copy(alpha = 0.1f), CircleShape)) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Assign", tint = ElectricBlue, modifier = Modifier.size(16.dp))
                        }
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp).background(Teal100, CircleShape)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Teal600, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp).background(CoralWarning.copy(alpha = 0.1f), CircleShape)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CoralWarning, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditInventoryDialog(
    item: InventoryEntity?,
    onDismiss: () -> Unit,
    onSave: (InventoryEntity) -> Unit
) {
    var name by remember { mutableStateOf(item?.itemName ?: "") }
    var brand by remember { mutableStateOf(item?.brand ?: "") }
    var sn by remember { mutableStateOf(item?.serialNumber ?: "") }
    var price by remember { mutableStateOf(item?.costPrice?.toString() ?: "") }
    var category by remember { mutableStateOf(item?.category ?: "ONU") }
    var purchaseDate by remember { mutableStateOf(item?.purchaseDate?.ifEmpty { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) } ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "Add Stock Item" else "Edit Stock Item", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Item Name (e.g. ONU, Router)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand / Manufacturer") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = sn, onValueChange = { sn = it }, label = { Text("Serial Number / MAC") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Cost Price (৳)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                
                ReadonlyDateField(
                    value = purchaseDate,
                    label = "ক্রয়ের তারিখ (Purchase Date)",
                    onDateSelected = { purchaseDate = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Category:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ONU", "Router", "Cable", "Other").forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(item?.copy(itemName = name, brand = brand, serialNumber = sn, costPrice = price.toDoubleOrNull() ?: 0.0, category = category, purchaseDate = purchaseDate)
                        ?: InventoryEntity(id = UUID.randomUUID().toString(), itemName = name, brand = brand, serialNumber = sn, costPrice = price.toDoubleOrNull() ?: 0.0, category = category, purchaseDate = purchaseDate))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Teal600)
            ) {
                Text("Save Item")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignInventoryDialog(
    item: InventoryEntity,
    customers: List<com.example.data.entity.CustomerEntity>,
    onDismiss: () -> Unit,
    onAssign: (String) -> Unit
) {
    var selectedCust by remember { mutableStateOf(customers.firstOrNull()) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign ${item.itemName} to Customer", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Assign Serial: ${item.serialNumber}", fontSize = 12.sp, color = Slate600)
                Spacer(modifier = Modifier.height(12.dp))
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedCust?.name ?: "Select Customer",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        customers.forEach { cust ->
                            DropdownMenuItem(
                                text = { Text("${cust.name} (${cust.customerCode})") },
                                onClick = {
                                    selectedCust = cust
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedCust?.let { onAssign(it.id) } },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
            ) {
                Text("Confirm Assignment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
