package com.example.data

import android.util.Log
import com.example.data.entity.*
import com.example.data.remote.SupabaseClient
import com.example.data.remote.MikroTikApiService
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.channel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ISPRepository(private val db: AppDatabase) {

    val supabase = SupabaseClient.client
    private val scope = CoroutineScope(Dispatchers.IO)

    val userDao = db.userDao()
    val customerDao = db.customerDao()
    val packageDao = db.packageDao()
    val invoiceDao = db.invoiceDao()
    val paymentDao = db.paymentCollectionDao()
    val expenseDao = db.expenseDao()
    val staffDao = db.staffDao()
    val settingsDao = db.ispSettingsDao()
    val ledgerDao = db.ledgerDao()
    val inventoryDao = db.inventoryDao()
    val supportTicketDao = db.supportTicketDao()
    val allocationDao = db.paymentAllocationDao()
    val smsLogDao = db.smsLogDao()
    val smsTemplateDao = db.smsTemplateDao()
    val mikrotikDao = db.mikrotikDao()
    private val smsService = com.example.service.SmsService()

    val allCustomers: Flow<List<CustomerEntity>> = customerDao.getAllCustomers()
    val allPackages: Flow<List<PackageEntity>> = packageDao.getAllPackages()
    val allInvoices: Flow<List<InvoiceEntity>> = invoiceDao.getAllInvoices()
    val allPayments: Flow<List<PaymentCollectionEntity>> = paymentDao.getAllPayments()
    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()
    val allStaff: Flow<List<StaffEntity>> = staffDao.getAllStaff()
    val settings: Flow<ISPSettingsEntity?> = settingsDao.getSettings()
    val allAllocations: Flow<List<PaymentAllocationEntity>> = allocationDao.getAllAllocations()
    val allSmsLogs: Flow<List<SmsLogEntity>> = smsLogDao.getAllLogs()
    val allSmsTemplates: Flow<List<SmsTemplateEntity>> = smsTemplateDao.getAllTemplates()
    val allRouters: Flow<List<MikroTikRouterEntity>> = mikrotikDao.getAllRouters()

    suspend fun submitPaymentRequest(data: Map<String, Any>) {
        supabase.postgrest.from("payment_requests").insert(data)
    }

    suspend inline fun <reified T : Any> syncTable(
        tableName: String,
        noinline onDelete: (String) -> Unit,
        noinline onSync: (T) -> Unit
    ) {
        try {
            val data = supabase.postgrest.from(tableName).select().decodeList<T>()
            data.forEach { onSync(it) }
        } catch (e: Exception) {
            Log.e("ISPRepository", "Initial fetch failed for $tableName", e)
        }

        val syncChannel = supabase.realtime.channel("$tableName-sync")
        syncChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = tableName
        }.onStart {
            Log.d("ISPRepository", "Started Real-time Sync for $tableName")
            syncChannel.subscribe()
        }.collectLatest { action ->
            when (action) {
                is PostgresAction.Insert -> onSync(action.decodeRecord<T>())
                is PostgresAction.Update -> onSync(action.decodeRecord<T>())
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
            launch { syncTable<CustomerEntity>("customers", { id -> scope.launch { customerDao.deleteCustomerById(id) } }) { entity -> scope.launch { customerDao.insertCustomer(entity) } } }
            launch { syncTable<PackageEntity>("packages", { id -> scope.launch { packageDao.deletePackageById(id) } }) { entity -> scope.launch { packageDao.insertPackage(entity) } } }
            launch { syncTable<PaymentCollectionEntity>("payments", { id -> scope.launch { paymentDao.deletePaymentById(id) } }) { entity -> scope.launch { paymentDao.insertPayment(entity) } } }
            launch { syncTable<ExpenseEntity>("expenses", { id -> scope.launch { expenseDao.deleteExpenseById(id) } }) { entity -> scope.launch { expenseDao.insertExpense(entity) } } }
            launch { syncTable<StaffEntity>("staff", { id -> scope.launch { staffDao.deleteStaffById(id) } }) { entity -> scope.launch { staffDao.insertStaff(entity) } } }
            launch { syncTable<LedgerEntity>("ledger_entries", { id -> scope.launch { ledgerDao.deleteLedgerById(id) } }) { entity -> scope.launch { ledgerDao.insertLedger(entity) } } }
            launch { syncTable<SmsTemplateEntity>("sms_templates", { id -> scope.launch { smsTemplateDao.deleteTemplateById(id) } }) { entity -> scope.launch { smsTemplateDao.insertTemplate(entity) } } }
        }
    }

    fun stopSync() {}

    suspend fun triggerSystemSms(
        type: String,
        mobile: String,
        params: Map<String, String>,
        customerId: String = "",
        customerCode: String = "",
        customerName: String = ""
    ) {
        val template = smsTemplateDao.getAllTemplates().firstOrNull()?.find { it.title == type }
        if (template == null || !template.isActive) return

        var message = template.messageContent
        
        // Base params
        val allParams = params.toMutableMap()
        
        // Auto-fill common tags if customerId is provided
        if (customerId.isNotBlank()) {
            customerDao.getCustomerById(customerId)?.let { c ->
                if (!allParams.containsKey("NAME")) allParams["NAME"] = c.name
                if (!allParams.containsKey("CUSTOMER_CODE")) allParams["CUSTOMER_CODE"] = c.customerCode
                if (!allParams.containsKey("TOTAL_DUE")) allParams["TOTAL_DUE"] = c.currentDue.toInt().toString()
                if (!allParams.containsKey("AMOUNT")) allParams["AMOUNT"] = c.currentDue.toInt().toString()
                if (!allParams.containsKey("PACKAGE_NAME")) allParams["PACKAGE_NAME"] = c.packageName
                if (!allParams.containsKey("EXPIRY_DATE")) allParams["EXPIRY_DATE"] = c.expireDate ?: ""
            }
        }
        
        // Global Company Info
        settingsDao.getSettings().firstOrNull()?.let { s ->
            if (!allParams.containsKey("COMPANY_NAME")) allParams["COMPANY_NAME"] = s.ispName
            if (!allParams.containsKey("SUPPORT_PHONE")) allParams["SUPPORT_PHONE"] = s.supportNumber
        }

        allParams.forEach { (key, value) ->
            message = message.replace("{$key}", value, ignoreCase = true)
        }

        sendAndLogSms(SmsLogEntity(
            id = UUID.randomUUID().toString(),
            customerId = customerId,
            customerCode = customerCode,
            customerName = customerName,
            mobile = mobile,
            notificationType = type,
            message = message
        ))
    }

    suspend fun insertCustomer(customer: CustomerEntity) {
        customerDao.insertCustomer(customer)
        try { supabase.postgrest.from("customers").insert(customer) } catch (e: Exception) { Log.e("ISPRepository", "Supabase Customer insert failed", e) }
        
        // Trigger SMS
        triggerSystemSms(
            type = "Create Customer",
            mobile = customer.mobile,
            params = mapOf("NAME" to customer.name, "CUSTOMER_CODE" to customer.customerCode),
            customerId = customer.id,
            customerCode = customer.customerCode,
            customerName = customer.name
        )
    }

    suspend fun updateCustomer(customer: CustomerEntity) {
        customerDao.updateCustomer(customer)
        try { supabase.postgrest.from("customers").update(customer) { filter { eq("id", customer.id) } } } catch (e: Exception) { Log.e("ISPRepository", "Supabase Customer update failed", e) }
    }

    suspend fun deleteCustomerById(id: String) {
        customerDao.deleteCustomerById(id)
        try { supabase.postgrest.from("customers").delete { filter { eq("id", id) } } } catch (e: Exception) { Log.e("ISPRepository", "Supabase Customer delete failed", e) }
    }

    suspend fun insertSmsLog(log: SmsLogEntity) {
        smsLogDao.insertLog(log)
        try { supabase.postgrest.from("sms_logs").insert(log) } catch (e: Exception) { Log.e("ISPRepository", "Supabase SMS Log insert failed", e) }
    }

    suspend fun sendAndLogSms(log: SmsLogEntity) {
        val s = settingsDao.getSettings().firstOrNull()
        if (s == null) {
            insertSmsLog(log.copy(status = "Error: Settings Missing"))
            return
        }
        
        if (!s.isAutoSmsEnabled && !log.notificationType.contains("Manual", ignoreCase = true) && log.notificationType != "Test") {
            insertSmsLog(log.copy(status = "Auto SMS Disabled"))
            return
        }

        val success = smsService.sendSms(
            apiUrl = s.smsApiUrl,
            apiKey = s.smsApiKey,
            senderId = s.smsSenderId,
            mobile = log.mobile,
            message = log.message
        )

        val finalLog = log.copy(
            status = if (success) "Sent" else "Failed",
            sentTimestamp = SimpleDateFormat("yyyy-MM-dd HH:mm a", Locale.US).format(Date())
        )
        insertSmsLog(finalLog)
    }

    suspend fun updateSmsLog(log: SmsLogEntity) {
        smsLogDao.updateLog(log)
        try { supabase.postgrest.from("sms_logs").update(log) { filter { eq("id", log.id) } } } catch (e: Exception) { Log.e("ISPRepository", "Supabase SMS Log update failed", e) }
    }

    suspend fun clearAllSmsLogs() {
        smsLogDao.clearAllLogs()
        try { supabase.postgrest.from("sms_logs").delete { filter { eq("status", "Sent") } } } catch (e: Exception) { Log.e("ISPRepository", "Supabase SMS Log clear failed", e) }
    }

    suspend fun insertSmsTemplate(template: SmsTemplateEntity) {
        smsTemplateDao.insertTemplate(template)
        try { supabase.postgrest.from("sms_templates").insert(template) } catch (e: Exception) { Log.e("ISPRepository", "Supabase SMS Template insert failed", e) }
    }

    suspend fun updateSmsTemplate(template: SmsTemplateEntity) {
        smsTemplateDao.updateTemplate(template)
        try { supabase.postgrest.from("sms_templates").update(template) { filter { eq("id", template.id) } } } catch (e: Exception) { Log.e("ISPRepository", "Supabase SMS Template update failed", e) }
    }

    suspend fun deleteSmsTemplate(id: String) {
        smsTemplateDao.deleteTemplateById(id)
        try { supabase.postgrest.from("sms_templates").delete { filter { eq("id", id) } } } catch (e: Exception) { Log.e("ISPRepository", "Supabase SMS Template delete failed", e) }
    }

    suspend fun insertInventoryItem(item: InventoryEntity) {
        inventoryDao.insertItem(item)
        try { supabase.postgrest.from("inventory_items").insert(item) } catch (e: Exception) { Log.e("ISPRepository", "Supabase Inventory insert failed", e) }
    }

    suspend fun updateInventoryItem(item: InventoryEntity) {
        inventoryDao.insertItem(item)
        try { supabase.postgrest.from("inventory_items").update(item) { filter { eq("id", item.id) } } } catch (e: Exception) { Log.e("ISPRepository", "Supabase Inventory update failed", e) }
    }

    suspend fun deleteInventoryItem(id: String) {
        inventoryDao.deleteItemById(id)
        try { supabase.postgrest.from("inventory_items").delete { filter { eq("id", id) } } } catch (e: Exception) { Log.e("ISPRepository", "Supabase Inventory delete failed", e) }
    }

    suspend fun assignItemToCustomer(itemId: String, customerId: String) {
        val items = inventoryDao.getAllInventory().first()
        val foundItem = items.find { it.id == itemId } ?: return
        inventoryDao.insertItem(foundItem.copy(assignedToCustomerId = customerId, status = "Assigned"))
    }

    suspend fun getCustomerLiveTraffic(customerId: String): Pair<Double, Double>? {
        val customer = customerDao.getCustomerById(customerId) ?: return null
        val routers = mikrotikDao.getAllRouters().first()
        val router = routers.find { it.id == customer.routerId } ?: routers.firstOrNull() ?: return null
        return MikroTikApiService().getPppoeUserTraffic(router, customer.pppoeUsername)
    }

    suspend fun processAutomatedGatewayPayment(
        customerId: String, invoiceId: String, amount: Double, gateway: Any, customerMobile: String, collectorName: String
    ): Pair<Boolean, PaymentCollectionEntity?> { return true to null }

    suspend fun verifyAndReconcileTransaction(trxId: String, gateway: com.example.service.PaymentGatewayType): com.example.service.GatewayApiResult<String> { 
        return com.example.service.GatewayApiResult.Success("Verified") 
    }
    suspend fun checkAndSuspendExpiredCustomers(): Int { 
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val expiredCustomers = customerDao.getAllCustomers().first().filter { 
            it.status == "Active" && it.expireDate?.let { date -> date.isNotBlank() && date < todayStr } == true
        }

        expiredCustomers.forEach { customer ->
            // 1. Update Status to Suspended
            val updated = customer.copy(status = "Suspended")
            updateCustomer(updated)

            // 2. Trigger Expired Customer SMS
            triggerSystemSms(
                type = "Expired Customer",
                mobile = customer.mobile,
                params = mapOf(
                    "NAME" to customer.name,
                    "CUSTOMER_CODE" to customer.customerCode,
                    "AMOUNT" to customer.currentDue.toInt().toString()
                ),
                customerId = customer.id,
                customerCode = customer.customerCode,
                customerName = customer.name
            )
        }
        return expiredCustomers.size
    }

    suspend fun getCustomerByLogin(identifier: String, password: String): CustomerEntity? {
        return customerDao.getCustomerByIdentifier(identifier, password)
    }

    suspend fun findCustomerByLoginInCloud(identifier: String, password: String): CustomerEntity? {
        return try {
            val response = supabase.postgrest.from("customers")
                .select() {
                    filter {
                        or {
                            eq("pppoe_username", identifier)
                            eq("customer_code", identifier)
                            eq("mobile", identifier)
                        }
                        eq("pppoe_password", password)
                    }
                }
                .decodeSingleOrNull<CustomerEntity>()
            response
        } catch (e: Exception) {
            Log.e("ISPRepository", "Cloud Customer search failed for $identifier", e)
            null
        }
    }

    suspend fun syncCustomerToMikroTik(customerId: String) {
        // Implementation
    }

    suspend fun setCustomerInternetStatus(customerId: String, active: Boolean): Boolean {
        // Implementation
        return true
    }

    suspend fun payInvoice(
        invoice: InvoiceEntity,
        amount: Double,
        method: String,
        trxId: String,
        collector: String,
        remarks: String
    ): PaymentCollectionEntity? {
        // Implementation
        return null
    }

    suspend fun generateAutoMonthlyInvoices(monthYear: String, overrides: Map<String, String> = emptyMap()): Int {
        // Implementation
        return 0
    }
    suspend fun insertExpense(expense: ExpenseEntity) { expenseDao.insertExpense(expense) }
    suspend fun insertPackage(pkg: PackageEntity) { packageDao.insertPackage(pkg) }
    suspend fun insertStaff(staff: StaffEntity) { staffDao.insertStaff(staff) }
    suspend fun updateStaff(staff: StaffEntity) { staffDao.insertStaff(staff) }
    suspend fun insertSalary(salary: StaffSalaryEntity) {}
    suspend fun updateRouterStatus(routerId: String, isConnected: Boolean) {}
    suspend fun insertRouter(router: MikroTikRouterEntity) { mikrotikDao.insertRouter(router) }
    suspend fun insertLedgerEntry(entry: LedgerEntity) { ledgerDao.insertLedger(entry) }
    suspend fun saveSettings(settings: ISPSettingsEntity) { settingsDao.saveSettings(settings) }
    fun getLedgerForCustomer(customerId: String): Flow<List<LedgerEntity>> { 
        return ledgerDao.getAllLedgerEntries().map { list -> list.filter { it.customerId == customerId } }
    }
    suspend fun insertSupportTicket(ticket: SupportTicketEntity) { supportTicketDao.insertTicket(ticket) }
    suspend fun updateSupportTicket(ticket: SupportTicketEntity) { supportTicketDao.insertTicket(ticket) }
    suspend fun deleteSupportTicket(id: String) { supportTicketDao.deleteTicketById(id) }

    suspend fun recordPayment(
        customerId: String,
        amount: Double,
        paymentMethod: String,
        transactionId: String,
        collectorName: String,
        collectorId: String,
        remarks: String,
        customDate: String? = null,
        billingMonth: String? = null
    ): PaymentCollectionEntity? {
        val customer = customerDao.getCustomerById(customerId) ?: return null
        val todayISO = customDate ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val timeStr = SimpleDateFormat("hh:mm a", Locale.US).format(Date())
        val paymentId = UUID.randomUUID().toString()
        val receiptNo = "REC-${System.currentTimeMillis().toString().takeLast(6)}"
        val finalBillingMonth = billingMonth ?: SimpleDateFormat("MMMM yyyy", Locale.US).format(Date())

        val payment = PaymentCollectionEntity(
            id = paymentId,
            receiptNo = receiptNo,
            customerId = customer.id,
            customerName = customer.name,
            customerCode = customer.customerCode,
            amount = amount,
            paymentMethod = paymentMethod,
            transactionId = transactionId,
            paymentDate = todayISO,
            billingMonth = finalBillingMonth,
            collectedBy = collectorName,
            collectedById = collectorId,
            collectorName = collectorName,
            remarks = remarks
        )

        var newDue = customer.currentDue
        var newAdvance = customer.advanceBalance

        if (amount > newDue) {
            val excess = amount - newDue
            newAdvance += excess
            newDue = 0.0
        } else {
            newDue -= amount
        }

        val updatedCustomer = customer.copy(
            currentDue = newDue,
            advanceBalance = newAdvance,
            paymentStatus = if (newDue <= 0) "Paid" else "Unpaid"
        )

        val ledgerEntry = LedgerEntity(
            customerId = customer.id,
            date = todayISO,
            time = timeStr,
            type = "Payment",
            description = "Payment for $finalBillingMonth",
            amount = amount,
            isDebit = false,
            referenceNo = receiptNo,
            paymentMethod = paymentMethod,
            collectorName = collectorName,
            runningBalance = newDue
        )

        try {
            // Local Updates First
            paymentDao.insertPayment(payment)
            customerDao.updateCustomer(updatedCustomer)
            ledgerDao.insertLedger(ledgerEntry)

            // Supabase Sync
            supabase.postgrest.from("payments").insert(payment)
            supabase.postgrest.from("customers").update(mapOf(
                "current_due" to newDue,
                "advance_balance" to newAdvance,
                "payment_status" to updatedCustomer.paymentStatus
            )) { filter { eq("id", customer.id) } }
            supabase.postgrest.from("ledger_entries").insert(ledgerEntry)

            // Send Collection SMS via Template System
            triggerSystemSms(
                type = "Collection",
                mobile = customer.mobile,
                params = mapOf(
                    "NAME" to customer.name,
                    "AMOUNT" to amount.toInt().toString(),
                    "RECEIPT_NO" to receiptNo,
                    "DUE_DATE" to todayISO,
                    "BILL_MONTH" to finalBillingMonth
                ),
                customerId = customer.id,
                customerCode = customer.customerCode,
                customerName = customer.name
            )

            return payment
        } catch (e: Exception) {
            Log.e("ISPRepository", "Failed to record payment", e)
            return null
        }
    }
}
