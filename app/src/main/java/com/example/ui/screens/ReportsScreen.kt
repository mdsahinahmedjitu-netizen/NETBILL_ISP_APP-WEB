package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.PaymentAllocationEntity
import com.example.localization.AppTranslation
import com.example.ui.theme.CoralWarning
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.viewmodel.MainViewModel

@Composable
fun ReportsScreen(viewModel: MainViewModel) {
    val stats by viewModel.dashboardStats.collectAsState()
    val customers by viewModel.customersList.collectAsState()
    val invoices by viewModel.invoicesList.collectAsState()
    val allocations: List<PaymentAllocationEntity> by viewModel.paymentAllocations.collectAsState()
    val currency = AppTranslation("currency_symbol")

    // Calculations for Carry Forward Report
    val dueInvoices = invoices.filter { it.dueAmount > 0 && it.status != "Cancelled" }
    val totalCarryForwardDue = dueInvoices.sumOf { it.previousDue }
    val totalCurrentBillDue = dueInvoices.sumOf { it.billAmount }
    val totalOutstandingDue = dueInvoices.sumOf { it.dueAmount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy900)
            .padding(16.dp)
    ) {
        Text(
            text = AppTranslation("reports_analytics"),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Export Actions Bar
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { viewModel.showToast("Exporting PDF Carry Forward & Due Report...") },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export PDF", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = { viewModel.showToast("Generating CSV Payment Allocations Export...") },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export CSV", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Navy800)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Carry Forward Due & Outstanding Report", fontWeight = FontWeight.Bold, color = CyanAccent, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        DetailRow("Total Carried Forward Due (Prev Months)", "$currency ${totalCarryForwardDue.toInt()}")
                        DetailRow("Current Month Bill Due", "$currency ${totalCurrentBillDue.toInt()}")
                        DetailRow("Net Total Outstanding Balance", "$currency ${totalOutstandingDue.toInt()}")
                        DetailRow("Customers with Carry Forward Due", "${dueInvoices.distinctBy { it.customerId }.size} Customers")
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Navy800)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("FIFO Payment Allocation History Log", fontWeight = FontWeight.Bold, color = CyanAccent, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        if (allocations.isEmpty()) {
                            Text("No payment allocations recorded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        } else {
                            allocations.take(5).forEach { alloc ->
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Date: ${alloc.paymentDate} • ${alloc.paymentMethod}",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = "Allocated: $currency${alloc.allocatedAmount.toInt()}",
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldSuccess,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Text(
                                        text = "${alloc.remarks} • Collector: ${alloc.collector}",
                                        color = Color.LightGray,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Navy800)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Monthly Financial Summary", fontWeight = FontWeight.Bold, color = CyanAccent, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        DetailRow("Total Monthly Income", "$currency ${stats.monthlyCollection.toInt()}")
                        DetailRow("Total Monthly Expense", "$currency ${(stats.todaysExpense * 15).toInt()}")
                        DetailRow("Estimated Net Profit", "$currency ${(stats.monthlyCollection - stats.todaysExpense * 15).toInt()}")
                        DetailRow("Outstanding Due", "$currency ${stats.totalDue.toInt()}")
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Navy800)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Zone-wise Subscriber Distribution", fontWeight = FontWeight.Bold, color = CyanAccent, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        DetailRow("Uttara Zone", "180 Subscribers (৳1,44,000/mo)")
                        DetailRow("Mirpur Zone", "145 Subscribers (৳1,16,000/mo)")
                        DetailRow("Dhanmondi Zone", "110 Subscribers (৳88,000/mo)")
                        DetailRow("Gulshan Zone", "74 Subscribers (৳59,200/mo)")
                    }
                }
            }
        }
    }
}
