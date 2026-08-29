package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ISPRepository
import com.example.data.entity.PaymentCollectionEntity
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

data class ZoneReport(
    val zoneName: String,
    val customerCount: Int,
    val monthlyRevenue: Double,
)

data class ReportStats(
    val totalRevenue: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netProfit: Double = 0.0,
    val totalOutstanding: Double = 0.0,
    val zoneReports: List<ZoneReport> = emptyList(),
    val recentPayments: List<PaymentCollectionEntity> = emptyList(),
)

class ReportsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val repository = ISPRepository(db)

    val customers = repository.allCustomers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val payments = repository.allPayments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val expenses = repository.allExpenses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val staffPayouts = repository.allPayouts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reportStats: StateFlow<ReportStats> = combine(customers, payments, expenses, staffPayouts) { custs, pymts, exps, payouts ->
        val yearMonthStr = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
        
        val monthlyRevenue = pymts.asSequence().filter { it.paymentDate.startsWith(yearMonthStr) }.sumOf { it.amount }
        
        val monthlyGeneralExpense = exps.asSequence().filter { it.expenseDate.startsWith(yearMonthStr) }.sumOf { it.amount }
        val monthlySalaryPaid = payouts.asSequence().filter { (it.date.startsWith(yearMonthStr)) && (it.type == "payment") }.sumOf { it.amount }
        val monthlyTotalExpense = monthlyGeneralExpense + monthlySalaryPaid
        
        val outstanding = custs.sumOf { it.currentDue }

        val zones = custs.groupBy { it.zone }.entries.asSequence().map { (zone, list) ->
            ZoneReport(
                zoneName = zone.orEmpty().ifEmpty { "Unknown" },
                customerCount = list.size,
                monthlyRevenue = list.sumOf { it.monthlyBill },
            )
        }.sortedByDescending { it.monthlyRevenue }.toList()

        ReportStats(
            totalRevenue = monthlyRevenue,
            totalExpense = monthlyTotalExpense,
            netProfit = monthlyRevenue - monthlyTotalExpense,
            totalOutstanding = outstanding,
            zoneReports = zones,
            recentPayments = pymts.asSequence().sortedByDescending { it.paymentDate }.take(20).toList(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportStats())
}
