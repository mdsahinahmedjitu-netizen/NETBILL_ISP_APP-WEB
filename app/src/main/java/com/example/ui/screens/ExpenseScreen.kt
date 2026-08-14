package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ExpenseEntity
import com.example.localization.AppTranslation
import com.example.ui.components.ReadonlyDateField
import com.example.ui.theme.BkashPink
import java.text.SimpleDateFormat
import java.util.*
import com.example.ui.theme.CoralWarning
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.viewmodel.MainViewModel

@Composable
fun ExpenseScreen(viewModel: MainViewModel) {
    val expenses by viewModel.expensesList.collectAsState()
    val currency = AppTranslation("currency_symbol")
    var showAddExpenseDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddExpenseDialog = true },
                containerColor = BkashPink,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense")
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
                text = AppTranslation("expense_management"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Slate900
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (expenses.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No expenses logged yet.", color = Slate600)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(expenses, key = { it.id }) { exp ->
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
                                Column {
                                    Text(exp.title, fontWeight = FontWeight.Bold, color = Slate900, fontSize = 15.sp)
                                    Text("Category: ${exp.category} • Date: ${exp.expenseDate}", color = Slate600, fontSize = 11.sp)
                                    Text("Logged by: ${exp.expenseBy}", color = Teal600, fontSize = 11.sp)
                                }
                                Text("$currency ${exp.amount.toInt()}", fontWeight = FontWeight.Bold, color = CoralWarning, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddExpenseDialog) {
        AddExpenseDialog(
            onDismiss = { showAddExpenseDialog = false },
            onSave = { title, category, amount, notes, date ->
                viewModel.addExpense(title, category, amount, notes, date)
                showAddExpenseDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, Double, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Bandwidth Cost") }
    var expandedCat by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var expenseDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }

    val categories = listOf(
        "Bandwidth Cost", "Staff Salary", "Electricity Bill", "Equipment Purchase", "Office Rent", "Maintenance", "Transport", "Other Expense"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log ISP Expense", color = Slate900, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Expense Title / Voucher") }, modifier = Modifier.fillMaxWidth())

                ExposedDropdownMenuBox(
                    expanded = expandedCat,
                    onExpandedChange = { expandedCat = !expandedCat }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCat) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        label = { Text("Category") }
                    )
                    ExposedDropdownMenu(expanded = expandedCat, onDismissRequest = { expandedCat = false }) {
                        categories.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat) }, onClick = { category = cat; expandedCat = false })
                        }
                    }
                }

                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount (৳ BDT)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes / Vendor") }, modifier = Modifier.fillMaxWidth())
                
                ReadonlyDateField(
                    value = expenseDate,
                    label = "ব্যয়ের তারিখ (Expense Date)",
                    onDateSelected = { expenseDate = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, category, amount.toDoubleOrNull() ?: 0.0, notes, expenseDate) },
                colors = ButtonDefaults.buttonColors(containerColor = BkashPink)
            ) {
                Text("Save Expense", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Slate600) }
        },
        containerColor = SleekCard
    )
}
