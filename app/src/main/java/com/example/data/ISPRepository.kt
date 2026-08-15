package com.example.data

import android.util.Log
import com.example.data.entity.*
import com.example.data.remote.SupabaseClient
import io.github.jan_tennert.supabase.postgrest.from
import io.github.jan_tennert.supabase.realtime.PostgresAction
import io.github.jan_tennert.supabase.realtime.decodeRecord
import io.github.jan_tennert.supabase.realtime.postgresChangeFlow
import io.github.jan_tennert.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ISPRepository(private val db: AppDatabase) {

    private val supabase = SupabaseClient.client
    private val mikroTikApi = com.example.data.remote.MikroTikApiService()
    private val smsService = com.example.service.SmsService()
    private val scope = CoroutineScope(Dispatchers.IO)

    val userDao = db.userDao()
    val customerDao = db.customerDao()
    val packageDao = db.packageDao()
    val invoiceDao = db.invoiceDao()
    val paymentAllocationDao = db.paymentAllocationDao()
    val paymentDao = db.paymentCollectionDao()
    val expenseDao = db.expenseDao()
    val staffDao = db.staffDao()
    val mikrotikDao = db.mikrotikDao()
    val settingsDao = db.ispSettingsDao()
    val ledgerDao = db.ledgerDao()
    val smsLogDao = db.smsLogDao()
    val smsTemplateDao = db.smsTemplateDao()
    val inventoryDao = db.inventoryDao()
    val supportTicketDao = db.supportTicketDao()

    val allCustomers: Flow<List<CustomerEntity>> = customerDao.getAllCustomers()
    val allPackages: Flow<List<PackageEntity>> = packageDao.getAllPackages()
    val allInvoices: Flow<List<InvoiceEntity>> = invoiceDao.getAllInvoices()
    val allPayments: Flow<List<PaymentCollectionEntity>> = paymentDao.getAllPayments()
    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()
    val allStaff: Flow<List<StaffEntity>> = staffDao.getAllStaff()
    val settings: Flow<ISPSettingsEntity?> = settingsDao.getSettings()

    private suspend fun <T : Any> syncTable(
        tableName: String,
        clazz: kotlinx.serialization.KSerializer<T>,
        onDelete: (String) -> Unit,
        onSync: (T) -> Unit
    ) {
        try {
            val data = supabase.from(tableName).select().decodeList(clazz)
            data.forEach { onSync(it) }
        } catch (e: Exception) {
            Log.e("ISPRepository", "Initial fetch failed for $tableName", e)
        }

        supabase.realtime.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = tableName
        }.onStart {
            Log.d("ISPRepository", "Started Real-time Sync for $tableName")
        }.collectLatest { action ->
            when (action) {
                is PostgresAction.Insert -> onSync(action.decodeRecord(clazz))
                is PostgresAction.Update -> onSync(action.decodeRecord(clazz))
                is PostgresAction.Delete -> {
                    val id = action.oldRecord["id"]?.toString()?.replace("\"", "")
                    if (id != null) onDelete(id)
                }
                else -> {}
            }
        }
    }

    fun startSync() {
        scope.launch {
            launch { syncTable("customers", CustomerEntity.serializer(), { scope.launch { customerDao.deleteCustomerById(it) } }) { scope.launch { customerDao.insertCustomer(it) } } }
            launch { syncTable("packages", PackageEntity.serializer(), { scope.launch { packageDao.deletePackageById(it) } }) { scope.launch { packageDao.insertPackage(it) } } }
            launch { syncTable("payments", PaymentCollectionEntity.serializer(), { /* delete handle */ }) { scope.launch { paymentDao.insertPayment(it) } } }
            launch { syncTable("expenses", ExpenseEntity.serializer(), { scope.launch { expenseDao.deleteExpenseById(it) } }) { scope.launch { expenseDao.insertExpense(it) } } }
            launch { syncTable("staff", StaffEntity.serializer(), { /* delete handle */ }) { scope.launch { staffDao.insertStaff(it) } } }
        }
    }

    fun stopSync() {}

    suspend fun insertCustomer(customer: CustomerEntity) {
        customerDao.insertCustomer(customer)
        supabase.from("customers").upsert(customer)
    }

    suspend fun updateCustomer(customer: CustomerEntity) {
        customerDao.updateCustomer(customer)
        supabase.from("customers").update(customer).eq("id", customer.id)
    }

    suspend fun deleteCustomerById(id: String) {
        customerDao.deleteCustomerById(id)
        supabase.from("customers").delete().eq("id", id)
    }

    suspend fun recordPayment(
        customerId: String,
        amount: Double,
        paymentMethod: String,
        transactionId: String,
        collectorName: String,
        remarks: String,
        date: String? = null,
        billingMonth: String? = null
    ): PaymentCollectionEntity? {
        val customer = customerDao.getCustomerById(customerId) ?: return null
        val currentDate = date ?: getCurrentDateString()
        val paymentId = UUID.randomUUID().toString()
        val receiptNo = "REC-${System.currentTimeMillis().toString().takeLast(6)}"
        val finalBillingMonth = billingMonth ?: SimpleDateFormat("MMMM yyyy", Locale.US).format(Date())

        val payment = PaymentCollectionEntity(
            id = paymentId,
            receipt_no = receiptNo,
            customer_id = customer.id,
            customer_name = customer.name,
            customer_code = customer.customerCode,
            amount = amount,
            payment_method = paymentMethod,
            transaction_id = transactionId,
            payment_date = currentDate,
            billing_month = finalBillingMonth,
            collected_by = collectorName,
            remarks = remarks
        )

        val totalCurrentDue = (customer.current_due - amount).coerceAtLeast(0.0)
        val updatedCustomer = customer.copy(current_due = totalCurrentDue)

        // Local First
        paymentDao.insertPayment(payment)
        customerDao.updateCustomer(updatedCustomer)

        // Supabase Sync
        supabase.from("payments").insert(payment)
        supabase.from("customers").update(mapOf("current_due" to totalCurrentDue)).eq("id", customer.id)

        return payment
    }

    suspend fun setCustomerInternetStatus(customerId: String, enable: Boolean): Boolean {
        val customer = customerDao.getCustomerById(customerId) ?: return false
        val nextStatus = if (enable) "Active" else "Suspended"
        updateCustomer(customer.copy(status = nextStatus))
        return true
    }

    suspend fun syncCustomerToMikroTik(customerId: String): Boolean = true
    suspend fun getCustomerLiveTraffic(customerId: String): Pair<Double, Double>? = null
    
    private fun getCurrentDateString() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}
