package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import com.example.ui.theme.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ExpenseEntity
import com.example.localization.appTranslation
import com.example.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ExpenseScreen(viewModel: MainViewModel, onBack: () -> Unit = {}) {
    val expenses by viewModel.expensesList.collectAsState()
    val staffPayouts by viewModel.staffPayouts.collectAsState()
    val currency = appTranslation("currency_symbol")
    var showAddExpenseDialog by remember { mutableStateOf(value = false) }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = remember(expenses) {
        listOf("All", "Staff Salary") + expenses.asSequence().map { it.category }.distinct().toList()
    }

    // Filter calculations
    val totalSalaryPaid = staffPayouts.asSequence().filter { it.type == "payment" }.sumOf { it.amount }
    
    val filteredGeneralExpenses = when (selectedCategory) {
        "All" -> expenses
        "Staff Salary" -> emptyList()
        else -> expenses.filter { it.category == selectedCategory }
    }
    
    val totalGeneralExpense = filteredGeneralExpenses.sumOf { it.amount }
    
    val combinedTotal = when (selectedCategory) {
        "All" -> totalGeneralExpense + totalSalaryPaid
        "Staff Salary" -> totalSalaryPaid
        else -> totalGeneralExpense
    }

    val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    
    val todayGeneralExpense = expenses.filter { 
        (it.expenseDate == todayDate) && (selectedCategory == "All" || it.category == selectedCategory)
    }.sumOf { it.amount }
    
    val todaySalaryPaid = if (selectedCategory == "All" || selectedCategory == "Staff Salary")
                          staffPayouts.filter { it.date == todayDate && it.type == "payment" }.sumOf { it.amount }
                          else 0.0
                          
    val combinedToday = todayGeneralExpense + todaySalaryPaid

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SleekBg)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // Back Button Row
        Row(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(52.dp)
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .border(1.dp, SleekBorder, RoundedCornerShape(16.dp)),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BkashPink)
            }
        }

        // Header & Stats
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(44.dp))
                .border(1.dp, SleekBorder, RoundedCornerShape(44.dp))
                .padding(28.dp)
        ) {
            Text(
                text = appTranslation("expense_title").uppercase(),
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                letterSpacing = 2.sp,
                color = Slate900
            )
            Text(
                text = "ISP OPERATIONAL EXPENDITURE • REAL-TIME AUDIT",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp,
                color = IspRose,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            // Category Filter
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    Surface(
                        onClick = { selectedCategory = cat },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) IspRose else Color(0xFFF1F5F9),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = cat.uppercase(),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isSelected) Color.White else Slate600,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatBox(label = "TOTAL FOR: ${selectedCategory.uppercase()}", value = "$currency${combinedTotal.toInt()}", color = IspRose, modifier = Modifier.weight(1f))
                StatBox(label = "TODAY", value = "$currency${combinedToday.toInt()}", color = IspAmber, modifier = Modifier.weight(1f))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = { showAddExpenseDialog = true },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IspRose)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("LOG NEW EXPENDITURE", fontWeight = FontWeight.Black, letterSpacing = 2.sp)
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
                    Row(
                        modifier = Modifier
                            .background(Color(0xFFF8FAFC))
                            .padding(vertical = 20.dp, horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExpenseHeaderCell("DAY", 100.dp)
                        ExpenseHeaderCell("DESCRIPTION / TITLE", 250.dp, TextAlign.Left)
                        ExpenseHeaderCell("CATEGORY", 150.dp)
                        ExpenseHeaderCell("SPENT BY", 150.dp)
                        ExpenseHeaderCell("AMOUNT", 120.dp, TextAlign.Right)
                    }

                    HorizontalDivider(color = SleekBorder)

                    filteredGeneralExpenses.forEach { exp ->
                        ExpenseRow(exp, currency)
                        HorizontalDivider(color = SleekBorder.copy(alpha = 0.5f))
                    }
                    
                    if (selectedCategory == "All" || selectedCategory == "Staff Salary") {
                        val totalSalary = staffPayouts.filter { it.type == "payment" }.sumOf { it.amount }
                        if (totalSalary > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "---", modifier = Modifier.width(100.dp), textAlign = TextAlign.Center)
                                Column(modifier = Modifier.width(250.dp)) {
                                    Text(text = "STAFF SALARIES TOTAL", fontWeight = FontWeight.Black, fontSize = 16.sp, color = IspIndigo)
                                    Text(text = "TOTAL DISBURSED SALARIES", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Slate400)
                                }
                                Box(modifier = Modifier.width(150.dp), contentAlignment = Alignment.Center) {
                                    Surface(shape = RoundedCornerShape(12.dp), color = IspIndigo.copy(alpha = 0.1f), border = BorderStroke(1.dp, IspIndigo)) {
                                        Text(text = "STAFF SALARY", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = IspIndigo, fontWeight = FontWeight.Black, fontSize = 9.sp)
                                    }
                                }
                                Text(text = "SYSTEM", modifier = Modifier.width(150.dp), fontSize = 12.sp, fontWeight = FontWeight.Black, color = Slate400, textAlign = TextAlign.Center)
                                Text(text = "$currency${totalSalary.toInt()}", modifier = Modifier.width(120.dp), fontSize = 18.sp, fontWeight = FontWeight.Black, color = IspIndigo, textAlign = TextAlign.Right)
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(80.dp))
    }

    if (showAddExpenseDialog) {
        AddExpenseDialog(
            onDismiss = { showAddExpenseDialog = false }
        ) { title, category, amount, notes, date, spentBy ->
            viewModel.addExpense(title, category, amount, notes, date, spentBy)
            showAddExpenseDialog = false
        }
    }
}

@Composable
fun ExpenseHeaderCell(text: String, width: Dp, textAlign: TextAlign = TextAlign.Center) {
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
fun ExpenseRow(expense: ExpenseEntity, currency: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp, horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = expense.expenseDate, modifier = Modifier.width(100.dp), fontSize = 12.sp, fontWeight = FontWeight.Black, color = Slate500, textAlign = TextAlign.Center)
        Column(modifier = Modifier.width(250.dp)) {
            Text(text = expense.title.uppercase(), fontWeight = FontWeight.Black, fontSize = 16.sp, color = Slate900, letterSpacing = 1.sp)
            Text(text = expense.notes.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Black, color = Slate400, letterSpacing = 1.sp)
        }
        Box(modifier = Modifier.width(150.dp), contentAlignment = Alignment.Center) {
            Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFF1F5F9), border = BorderStroke(1.dp, SleekBorder)) {
                Text(text = expense.category.uppercase(), modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = Slate600, fontWeight = FontWeight.Black, fontSize = 9.sp, letterSpacing = 1.sp)
            }
        }
        Text(text = expense.expenseBy.uppercase(), modifier = Modifier.width(150.dp), fontSize = 12.sp, fontWeight = FontWeight.Black, color = IspIndigo, textAlign = TextAlign.Center, letterSpacing = 1.sp)
        Text(text = "$currency${expense.amount.toInt()}", modifier = Modifier.width(120.dp), fontSize = 18.sp, fontWeight = FontWeight.Black, color = IspRose, textAlign = TextAlign.Right, letterSpacing = 1.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, Double, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Bandwidth Cost") }
    var amount by remember { mutableStateOf("") }
    var spentBy by remember { mutableStateOf("Admin") }
    var notes by remember { mutableStateOf("") }
    var expenseDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ISP EXPENDITURE LOG", color = Slate900, fontWeight = FontWeight.Black, letterSpacing = 2.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("VOUCHER TITLE", fontWeight = FontWeight.Black) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("AMOUNT (৳)", fontWeight = FontWeight.Black) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("CATEGORY", fontWeight = FontWeight.Black) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                OutlinedTextField(value = spentBy, onValueChange = { spentBy = it }, label = { Text("SPENT BY", fontWeight = FontWeight.Black) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
            }
        },
        confirmButton = {
            Button(onClick = { onSave(title, category, amount.toDoubleOrNull() ?: 0.0, notes, expenseDate, spentBy) }, modifier = Modifier.fillMaxWidth().height(64.dp), shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = IspRose)) {
                Text("LOG EXPENDITURE", fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", fontWeight = FontWeight.Black, color = Slate400) } },
        containerColor = Color.White,
        shape = RoundedCornerShape(44.dp)
    )
}
