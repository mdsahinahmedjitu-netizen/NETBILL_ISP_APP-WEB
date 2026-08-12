package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ISPRepository
import com.example.data.entity.CustomerEntity
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.PaymentCollectionEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

data class ZoneReport(
    val zoneName: String,
    val customerCount: Int,
    val monthlyRevenue: Double
)

data class ReportStats(
    val totalRevenue: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netProfit: Double = 0.0,
    val totalOutstanding: Double = 0.0,
    val zoneReports: List<ZoneReport> = emptyList()
)

class ReportsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val repository = ISPRepository(db)

    val customers = repository.allCustomers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val payments = repository.allPayments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val expenses = repository.allExpenses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reportStats: StateFlow<ReportStats> = combine(customers, payments, expenses) { custs, pymts, exps ->
        val yearMonthStr = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
        
        val monthlyRevenue = pymts.filter { it.paymentDate.startsWith(yearMonthStr) }.sumOf { it.amount }
        val monthlyExpense = exps.filter { it.expenseDate.startsWith(yearMonthStr) }.sumOf { it.amount }
        val outstanding = custs.sumOf { it.currentDue }

        val zones = custs.groupBy { it.zone }.map { (zone, list) ->
            ZoneReport(
                zoneName = zone.ifEmpty { "Unknown" },
                customerCount = list.size,
                monthlyRevenue = list.sumOf { it.monthlyBill }
            )
        }.sortedByDescending { it.monthlyRevenue }

        ReportStats(
            totalRevenue = monthlyRevenue,
            totalExpense = monthlyExpense,
            netProfit = monthlyRevenue - monthlyExpense,
            totalOutstanding = outstanding,
            zoneReports = zones
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportStats())

    fun exportCustomersToCsv(context: Context) {
        viewModelScope.launch {
            val list = customers.value
            if (list.isEmpty()) return@launch

            val filename = "Customers_Report_${System.currentTimeMillis()}.csv"
            val file = File(context.getExternalFilesDir(null), filename)
            
            try {
                FileOutputStream(file).use { out ->
                    out.write("Code,Name,Mobile,Package,Zone,Status,Due\n".toByteArray())
                    list.forEach { c ->
                        val line = "${c.customerCode},${c.name},${c.mobile},${c.packageName},${c.zone},${c.status},${c.currentDue}\n"
                        out.write(line.toByteArray())
                    }
                }
                shareFile(context, file)
            } catch (e: Exception) {
                android.util.Log.e("ReportsViewModel", "Export failed", e)
            }
        }
    }

    fun exportPaymentsToCsv(context: Context) {
        viewModelScope.launch {
            val list = payments.value
            if (list.isEmpty()) return@launch

            val filename = "Payments_Report_${System.currentTimeMillis()}.csv"
            val file = File(context.getExternalFilesDir(null), filename)
            
            try {
                FileOutputStream(file).use { out ->
                    out.write("Receipt,Customer,Date,Method,Amount,Collector\n".toByteArray())
                    list.forEach { p ->
                        val line = "${p.receiptNo},${p.customerName},${p.paymentDate},${p.paymentMethod},${p.amount},${p.collectorName}\n"
                        out.write(line.toByteArray())
                    }
                }
                shareFile(context, file)
            } catch (e: Exception) {
                android.util.Log.e("ReportsViewModel", "Export failed", e)
            }
        }
    }

    private fun shareFile(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "ISP Report Export")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Report"))
    }
}
