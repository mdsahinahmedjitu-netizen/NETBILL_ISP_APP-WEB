package com.example.data

import android.util.Log
import com.example.data.entity.*
import com.example.data.remote.FirestoreService
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ISPRepository(private val db: AppDatabase) {

    private val firestoreService = FirestoreService()
    private val firestore = FirebaseFirestore.getInstance()
    private val mikroTikApi = com.example.data.remote.MikroTikApiService()
    private val smsService = com.example.service.SmsService()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val listeners = mutableListOf<ListenerRegistration>()

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
    val allAllocations: Flow<List<PaymentAllocationEntity>> = paymentAllocationDao.getAllAllocations()
    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()
    val allStaff: Flow<List<StaffEntity>> = staffDao.getAllStaff()
    val allRouters: Flow<List<MikroTikRouterEntity>> = mikrotikDao.getAllRouters()
    val settings: Flow<ISPSettingsEntity?> = settingsDao.getSettings()
    val allLedgerEntries: Flow<List<LedgerEntryEntity>> = ledgerDao.getAllLedgerEntries()
    val allSmsLogs: Flow<List<SmsLogEntity>> = smsLogDao.getAllSmsLogs()
    val allSmsTemplates: Flow<List<SmsTemplateEntity>> = smsTemplateDao.getAllTemplates()

    private fun <T : Any> syncCollection(collection: String, clazz: Class<T>, onDelete: (String) -> Unit, onSync: (T) -> Unit) {
        val listener = firestore.collection(collection).addSnapshotListener { snapshots, e ->
            if (e != null) {
                Log.e("ISPRepository", "Listen FAILED for $collection. Project: ${firestore.app.options.projectId}", e)
                return@addSnapshotListener
            }
            
            val isFromCache = snapshots?.metadata?.isFromCache ?: false
            val hasPendingWrites = snapshots?.metadata?.hasPendingWrites() ?: false
            
            Log.d("ISPRepository", "Sync Event for $collection | FromCache: $isFromCache | PendingWrites: $hasPendingWrites")
            
            val count = snapshots?.documentChanges?.size ?: 0
            if (count > 0) {
                Log.d("ISPRepository", "Syncing $count changes for $collection")
            }
            
            snapshots?.documentChanges?.forEach { dc ->
                try {
                    if (dc.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                        onDelete(dc.document.id)
                    } else {
                        val obj = dc.document.toObject(clazz)
                        onSync(obj)
                    }
                } catch (ex: Exception) {
                    Log.e("ISPRepository", "Error parsing $collection document ${dc.document.id}", ex)
                }
            }
        }
        listeners.add(listener)
    }

    fun startSync() {
        stopSync()
        scope.launch {
            // Wait up to 5 seconds for Firebase Auth to settle if needed
            var retryCount = 0
            var user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            while (user == null && retryCount < 10) {
                kotlinx.coroutines.delay(500)
                user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                retryCount++
            }

            if (user == null) {
                Log.w("ISPRepository", "Sync skipped: User is NOT authenticated in Firebase after retries.")
                return@launch
            }
            
            Log.i("ISPRepository", "Starting Real-time Sync for User: ${user.uid}")
            
            syncCollection("customers", CustomerEntity::class.java, { scope.launch { customerDao.deleteCustomerById(it) } }) { scope.launch { customerDao.insertCustomer(it) } }
            syncCollection("packages", PackageEntity::class.java, { scope.launch { packageDao.deletePackageById(it) } }) { scope.launch { packageDao.insertPackage(it) } }
            syncCollection("invoices", InvoiceEntity::class.java, { /* handle if needed */ }) { scope.launch { invoiceDao.insertInvoice(it) } }
            syncCollection("payments", PaymentCollectionEntity::class.java, { /* handle if needed */ }) { scope.launch { paymentDao.insertPayment(it) } }
            syncCollection("allocations", PaymentAllocationEntity::class.java, { /* handle if needed */ }) { scope.launch { paymentAllocationDao.insertAllocation(it) } }
            syncCollection("expenses", ExpenseEntity::class.java, { scope.launch { expenseDao.deleteExpenseById(it) } }) { scope.launch { expenseDao.insertExpense(it) } }
            syncCollection("ledger", LedgerEntryEntity::class.java, { /* handle if needed */ }) { scope.launch { ledgerDao.insertLedgerEntry(it) } }
            syncCollection("isp_settings", ISPSettingsEntity::class.java, { /* handle if needed */ }) { scope.launch { settingsDao.saveSettings(it) } }
            syncCollection("staff", StaffEntity::class.java, { /* handle if needed */ }) { scope.launch { staffDao.insertStaff(it) } }
            syncCollection("staff_salaries", StaffSalaryEntity::class.java, { /* handle if needed */ }) { scope.launch { staffDao.insertSalary(it) } }
            syncCollection("mikrotik_routers", MikroTikRouterEntity::class.java, { /* handle if needed */ }) { scope.launch { mikrotikDao.insertRouter(it) } }
            syncCollection("sms_logs", SmsLogEntity::class.java, { scope.launch { smsLogDao.deleteSmsLogById(it) } }) { scope.launch { smsLogDao.insertSmsLog(it) } }
            syncCollection("sms_templates", SmsTemplateEntity::class.java, { scope.launch { smsTemplateDao.deleteTemplateById(it) } }) { scope.launch { smsTemplateDao.insertTemplate(it) } }
            syncCollection("inventory_items", InventoryEntity::class.java, { scope.launch { inventoryDao.deleteItemById(it) } }) { scope.launch { inventoryDao.insertItem(it) } }
            syncCollection("support_tickets", SupportTicketEntity::class.java, { scope.launch { supportTicketDao.deleteTicketById(it) } }) { scope.launch { supportTicketDao.insertTicket(it) } }
            syncCollection("users", UserEntity::class.java, { /* handle if needed */ }) { scope.launch { userDao.insertUser(it) } }
        }
    }

    fun stopSync() {
        listeners.forEach { it.remove() }
        listeners.clear()
    }

    suspend fun checkFirestoreConnection(): String {
        return firestoreService.checkConnectivity()
    }

    fun getLedgerForCustomer(customerId: String): Flow<List<LedgerEntryEntity>> {
        return ledgerDao.getLedgerForCustomer(customerId)
    }

    fun getAllocationsForCustomer(customerId: String): Flow<List<PaymentAllocationEntity>> {
        return paymentAllocationDao.getAllocationsForCustomer(customerId)
    }

    fun getAllocationsForInvoice(invoiceId: String): Flow<List<PaymentAllocationEntity>> {
        return paymentAllocationDao.getAllocationsForInvoice(invoiceId)
    }

    suspend fun insertLedgerEntry(entry: LedgerEntryEntity) {
        ledgerDao.insertLedgerEntry(entry)
        firestoreService.saveDocument("ledger", entry.id, entry)
    }

    suspend fun seedDatabaseIfEmpty() {
        seedSmsTemplatesIfEmpty()
        val existingUser = userDao.getUserByUsernameOrMobile("admin")
        if (existingUser == null) {
            // Stable IDs for demo accounts to prevent duplicates across devices
            val adminId = "demo_admin_id"
            val operatorId = "demo_operator_id"
            
            val admin = UserEntity(
                id = adminId,
                username = "admin",
                passwordHash = "admin123",
                name = "M. A. Rahman (Super Admin)",
                mobile = "01711000000",
                role = "Super Admin"
            )
            insertUser(admin)

            insertUser(
                UserEntity(
                    id = operatorId,
                    username = "operator",
                    passwordHash = "123456",
                    name = "Sumon Hasan (Billing Operator)",
                    mobile = "01811223344",
                    role = "Billing Operator"
                )
            )
            
            val settings = ISPSettingsEntity(
                id = "1",
                ispName = "NetBill Broadband ISP",
                address = "House 14, Road 7, Sector 4, Uttara, Dhaka-1230",
                mobileNumber = "01711000000",
                supportNumber = "01911000000",
                currencySymbol = "৳",
                defaultLanguage = "bn"
            )
            // Local First
            settingsDao.saveSettings(settings)
            firestoreService.saveSettings(settings)

            // Seed packages with stable IDs
            val pkg1 = PackageEntity(id = "pkg_10mbps", name = "10 Mbps Starter", speed = "10 Mbps", monthlyPrice = 500.0, description = "Home starter connection", activeUserCount = 120)
            val pkg2 = PackageEntity(id = "pkg_20mbps", name = "20 Mbps Super", speed = "20 Mbps", monthlyPrice = 800.0, description = "Most popular home package", activeUserCount = 245)
            val pkg3 = PackageEntity(id = "pkg_50mbps", name = "50 Mbps Turbo", speed = "50 Mbps", monthlyPrice = 1500.0, description = "Gamers & heavy streaming", activeUserCount = 89)
            
            packageDao.insertPackage(pkg1)
            packageDao.insertPackage(pkg2)
            packageDao.insertPackage(pkg3)
            
            firestoreService.savePackage(pkg1)
            firestoreService.savePackage(pkg2)
            firestoreService.savePackage(pkg3)

            val cust1 = CustomerEntity(
                id = "cust_demo_1001",
                customerCode = "NET-1001",
                name = "Rahim Uddin",
                mobile = "01712345678",
                packageName = pkg2.name,
                packageId = pkg2.id,
                monthlyBill = 800.0,
                joinDate = "2026-08-01",
                status = "Active",
                currentDue = 800.0
            )
            customerDao.insertCustomer(cust1)
            firestoreService.saveCustomer(cust1)
        }
    }

    suspend fun insertCustomer(customer: CustomerEntity) {
        customerDao.insertCustomer(customer)
        firestoreService.saveCustomer(customer)
    }

    suspend fun updateCustomer(customer: CustomerEntity) {
        customerDao.updateCustomer(customer)
        firestoreService.saveCustomer(customer)
    }

    suspend fun deleteCustomerById(id: String) {
        customerDao.deleteCustomerById(id)
        firestoreService.deleteCustomer(id)
    }

    suspend fun insertPackage(pkg: PackageEntity) {
        packageDao.insertPackage(pkg)
        firestoreService.savePackage(pkg)
    }

    suspend fun deletePackageById(id: String) {
        packageDao.deletePackageById(id)
        firestoreService.deletePackage(id)
    }

    suspend fun generateAutoMonthlyInvoices(
        monthYear: String,
        overrideChoicesFor20thDay: Map<String, String> = emptyMap()
    ): Int {
        val customersList = customerDao.getAllCustomers().firstOrNull() ?: emptyList()
        var generatedCount = 0
        val currentDate = getCurrentDateString()

        val invoicesToSave = mutableListOf<InvoiceEntity>()
        val customersToUpdate = mutableListOf<CustomerEntity>()
        val ledgerToSave = mutableListOf<LedgerEntryEntity>()

        for (cust in customersList) {
            if (cust.status != "Active") continue
            if (cust.joinDayOfMonth > 20) {
                val choice = overrideChoicesFor20thDay[cust.id] ?: "NextMonth"
                if (choice == "NextMonth") continue
            }

            val billAmt = cust.monthlyBill
            val existingInvoices = invoiceDao.getInvoicesForCustomer(cust.id).firstOrNull() ?: emptyList()
            val prevDue = existingInvoices
                .filter { it.status != "Paid" && it.status != "Cancelled" }
                .sumOf { it.dueAmount }

            val totalGrossPayable = billAmt + prevDue
            var paidAmt = 0.0
            var remainingAdvance = cust.advanceBalance

            if (remainingAdvance > 0) {
                val appliedAdvance = Math.min(remainingAdvance, totalGrossPayable)
                paidAmt = appliedAdvance
                remainingAdvance -= appliedAdvance
            }

            val dueAmt = (totalGrossPayable - paidAmt).coerceAtLeast(0.0)
            val invStatus = when {
                dueAmt <= 0.0 -> "Paid"
                paidAmt > 0.0 -> "Partially Paid"
                prevDue > 0.0 -> "Overdue"
                else -> "Due"
            }

            val invId = UUID.randomUUID().toString()
            val invNo = "INV-${System.currentTimeMillis().toString().takeLast(8)}"

            val invoice = InvoiceEntity(
                id = invId,
                invoiceNo = invNo,
                customerId = cust.id,
                customerCode = cust.customerCode,
                customerName = cust.name,
                packageName = cust.packageName,
                billingMonthYear = monthYear,
                billAmount = billAmt,
                previousDue = prevDue,
                carryForwardDue = prevDue,
                totalPayable = totalGrossPayable,
                paidAmount = paidAmt,
                dueAmount = dueAmt,
                status = invStatus,
                generatedDate = currentDate,
                dueDate = "$monthYear-10"
            )
            invoicesToSave.add(invoice)
            customersToUpdate.add(cust.copy(currentDue = dueAmt, advanceBalance = remainingAdvance))

            val timeStr = SimpleDateFormat("hh:mm a", Locale.US).format(Date())
            if (prevDue > 0) {
                ledgerToSave.add(LedgerEntryEntity(
                    id = UUID.randomUUID().toString(),
                    customerId = cust.id, date = currentDate, time = timeStr,
                    type = "Carry Forward Due", amount = prevDue, isDebit = true,
                    runningBalance = prevDue, monthYear = monthYear,
                    referenceNo = "CF-${System.currentTimeMillis().toString().takeLast(6)}"
                ))
            }

            ledgerToSave.add(LedgerEntryEntity(
                id = UUID.randomUUID().toString(),
                customerId = cust.id, date = currentDate, time = timeStr,
                type = "Monthly Bill", amount = billAmt, isDebit = true,
                runningBalance = totalGrossPayable, monthYear = monthYear,
                referenceNo = invNo, description = "$monthYear ${cust.packageName} Package Bill"
            ))
            generatedCount++
        }

        if (invoicesToSave.isNotEmpty()) {
            // Local First
            invoicesToSave.forEach { invoiceDao.insertInvoice(it) }
            customersToUpdate.forEach { customerDao.updateCustomer(it) }
            ledgerToSave.forEach { ledgerDao.insertLedgerEntry(it) }
            
            // Sync to Firestore
            firestoreService.generateBillingTransaction(invoicesToSave, customersToUpdate, ledgerToSave)

            // Auto SMS
            val currentSettings = settingsDao.getSettings().firstOrNull()
            if (currentSettings?.isAutoSmsEnabled == true) {
                scope.launch {
                    invoicesToSave.forEach { inv ->
                        val customer = customersToUpdate.find { it.id == inv.customerId }
                        if (customer != null) {
                            val msg = "Dear ${customer.name}, your bill for ${inv.billingMonthYear} is ${inv.billAmount} ৳. Total Due: ${inv.totalPayable} ৳. Pay by 10th. Thank you - ${currentSettings.ispName}"
                            smsService.sendSms(currentSettings.smsApiUrl, currentSettings.smsApiKey, currentSettings.smsSenderId, customer.mobile, msg)
                        }
                    }
                }
            }
        }
        return generatedCount
    }

    suspend fun recordPayment(
        customerId: String,
        amount: Double,
        paymentMethod: String,
        transactionId: String,
        collectorName: String,
        remarks: String,
        date: String? = null
    ): PaymentCollectionEntity? {
        val customer = customerDao.getCustomerById(customerId) ?: return null
        val currentDate = date ?: getCurrentDateString()
        val paymentId = UUID.randomUUID().toString()
        val receiptNo = "REC-${System.currentTimeMillis().toString().takeLast(6)}"

        val payment = PaymentCollectionEntity(
            id = paymentId,
            receiptNo = receiptNo,
            customerId = customer.id,
            customerName = customer.name,
            customerCode = customer.customerCode,
            amount = amount,
            paymentMethod = paymentMethod,
            transactionId = transactionId,
            paymentDate = currentDate,
            collectorName = collectorName,
            remarks = remarks
        )

        val allocations = mutableListOf<PaymentAllocationEntity>()
        val updatedInvoices = mutableListOf<InvoiceEntity>()
        val ledgerEntries = mutableListOf<LedgerEntryEntity>()

        var remainingPayment = amount
        val unpaidInvoices = invoiceDao.getUnpaidInvoicesForCustomerAsc(customerId)

        for (inv in unpaidInvoices) {
            if (remainingPayment <= 0) break
            val invDue = inv.totalPayable - inv.paidAmount
            if (invDue <= 0) continue

            val allocateAmount = Math.min(remainingPayment, invDue)
            val newPaid = inv.paidAmount + allocateAmount
            val newDue = (inv.totalPayable - newPaid).coerceAtLeast(0.0)
            val newStatus = if (newDue <= 0.0) "Paid" else "Partially Paid"

            updatedInvoices.add(inv.copy(paidAmount = newPaid, dueAmount = newDue, status = newStatus, paymentDate = currentDate))
            allocations.add(PaymentAllocationEntity(
                id = UUID.randomUUID().toString(), paymentId = paymentId, invoiceId = inv.id,
                customerId = customerId, allocatedAmount = allocateAmount, remainingBillDueAfter = newDue,
                paymentDate = currentDate, paymentMethod = paymentMethod, collector = collectorName,
                remarks = "FIFO allocated to ${inv.billingMonthYear} bill"
            ))
            remainingPayment -= allocateAmount
        }

        var updatedAdvance = customer.advanceBalance
        val timeStr = SimpleDateFormat("hh:mm a", Locale.US).format(Date())
        val currentMonth = SimpleDateFormat("MMMM yyyy", Locale.US).format(Date())

        if (remainingPayment > 0) {
            updatedAdvance += remainingPayment
            ledgerEntries.add(LedgerEntryEntity(
                id = UUID.randomUUID().toString(), customerId = customer.id, date = currentDate,
                time = timeStr, type = "Advance", amount = remainingPayment, isDebit = false,
                runningBalance = 0.0, monthYear = currentMonth, paymentMethod = paymentMethod,
                collector = collectorName, referenceNo = receiptNo, description = "Advance payment stored"
            ))
        }

        val totalCurrentDue = (customer.currentDue - (amount - remainingPayment)).coerceAtLeast(0.0)
        val updatedCustomer = customer.copy(currentDue = totalCurrentDue, advanceBalance = updatedAdvance)

        ledgerEntries.add(LedgerEntryEntity(
            id = UUID.randomUUID().toString(), customerId = customer.id, date = currentDate,
            time = timeStr, type = "Payment", amount = amount, isDebit = false,
            runningBalance = totalCurrentDue, monthYear = currentMonth, paymentMethod = paymentMethod,
            collector = collectorName, referenceNo = receiptNo, description = "Payment received"
        ))

        // Local First
        paymentDao.insertPayment(payment)
        allocations.forEach { paymentAllocationDao.insertAllocation(it) }
        updatedInvoices.forEach { invoiceDao.updateInvoice(it) }
        customerDao.updateCustomer(updatedCustomer)
        ledgerEntries.forEach { ledgerDao.insertLedgerEntry(it) }

        // Sync to Firestore
        firestoreService.recordPaymentTransaction(payment, allocations, updatedInvoices, updatedCustomer, ledgerEntries)

        // Auto SMS Receipt
        val currentSettings = settingsDao.getSettings().firstOrNull()
        if (currentSettings?.isAutoSmsEnabled == true) {
            scope.launch {
                val msg = "Payment Received: ${payment.amount} ৳ from ${customer.name}. Receipt: ${payment.receiptNo}. Current Due: ${updatedCustomer.currentDue} ৳. Thank you."
                smsService.sendSms(currentSettings.smsApiUrl, currentSettings.smsApiKey, currentSettings.smsSenderId, customer.mobile, msg)
            }
        }
        return payment
    }

    suspend fun payInvoice(
        invoice: InvoiceEntity,
        amount: Double,
        paymentMethod: String,
        transactionId: String,
        collectorName: String,
        remarks: String
    ): PaymentCollectionEntity? {
        return recordPayment(
            customerId = invoice.customerId,
            amount = amount,
            paymentMethod = paymentMethod,
            transactionId = transactionId,
            collectorName = collectorName,
            remarks = "Paid Invoice ${invoice.invoiceNo}: $remarks"
        )
    }

    val paymentGatewayService = com.example.service.PaymentGatewayService()

    fun updateGatewayConfig(settings: ISPSettingsEntity) {
        paymentGatewayService.config = com.example.service.GatewayConfig(
            environment = if (settings.apiMode == "Production") 
                com.example.service.GatewayEnvironment.PRODUCTION else com.example.service.GatewayEnvironment.SANDBOX,
            bkashAppKey = settings.bkashAppKey,
            bkashAppSecret = settings.bkashAppSecret,
            bkashUsername = settings.bkashUsername,
            bkashPassword = settings.bkashPassword,
            nagadMerchantId = settings.nagadMerchantId,
            nagadMerchantNumber = settings.nagadMobile
        )
        Log.d("ISPRepository", "Gateway Config Updated: Mode=${settings.apiMode}")
    }

    suspend fun processAutomatedGatewayPayment(
        customerId: String,
        invoiceId: String,
        amount: Double,
        gateway: com.example.service.PaymentGatewayType,
        customerMobile: String,
        collectorName: String
    ): Pair<Boolean, PaymentCollectionEntity?> {
        val invoice = if (invoiceId.isNotEmpty()) invoiceDao.getInvoiceById(invoiceId) else null
        val invoiceNo = invoice?.invoiceNo ?: "INV-${System.currentTimeMillis().toString().takeLast(6)}"
        val result = paymentGatewayService.executeAutomatedCollection(gateway, amount, customerMobile, invoiceNo)

        return if (result is com.example.service.GatewayApiResult.Success) {
            val res = result.data
            val gatewayName = if (gateway == com.example.service.PaymentGatewayType.BKASH) "bKash" else "Nagad"
            val payment = if (invoice != null) {
                payInvoice(invoice, amount, gatewayName, res.transactionId, collectorName, "Auto Gateway Settlement")
            } else {
                recordPayment(customerId, amount, gatewayName, res.transactionId, collectorName, "Automated Gateway Payment")
            }
            Pair(true, payment)
        } else {
            Pair(false, null)
        }
    }

    suspend fun verifyAndReconcileTransaction(
        trxId: String,
        gateway: com.example.service.PaymentGatewayType
    ): com.example.service.GatewayApiResult<String> {
        val cleanTrx = trxId.trim()
        if (cleanTrx.isBlank()) return com.example.service.GatewayApiResult.Error("Invalid Trx ID")
        return when (gateway) {
            com.example.service.PaymentGatewayType.BKASH -> {
                val res = paymentGatewayService.bkashQueryPayment(cleanTrx)
                if (res is com.example.service.GatewayApiResult.Success) {
                    com.example.service.GatewayApiResult.Success("bKash Verified: Trx ${res.data.trxID} | Status: ${res.data.transactionStatus}")
                } else com.example.service.GatewayApiResult.Error("bKash Verification Failed")
            }
            com.example.service.PaymentGatewayType.NAGAD -> {
                val res = paymentGatewayService.nagadVerifyPayment(cleanTrx)
                if (res is com.example.service.GatewayApiResult.Success) {
                    com.example.service.GatewayApiResult.Success("Nagad Verified: Trx ${res.data.trxId} | Status: ${res.data.status}")
                } else com.example.service.GatewayApiResult.Error("Nagad Verification Failed")
            }
        }
    }

    suspend fun insertUser(user: UserEntity) {
        userDao.insertUser(user)
        firestoreService.saveDocument("users", user.id, user)
    }

    suspend fun deleteUser(id: String) {
        userDao.deleteUserById(id)
        firestoreService.deleteDocument("users", id)
    }

    suspend fun insertStaff(staff: StaffEntity) {
        staffDao.insertStaff(staff)
        firestoreService.saveStaff(staff)
    }

    suspend fun updateStaff(staff: StaffEntity) {
        staffDao.insertStaff(staff)
        firestoreService.saveStaff(staff)
    }

    suspend fun insertSalary(salary: StaffSalaryEntity) {
        staffDao.insertSalary(salary)
        firestoreService.saveSalary(salary)
    }

    suspend fun deleteStaff(id: String) {
        staffDao.deleteStaffById(id)
        firestoreService.deleteDocument("staff", id)
    }

    suspend fun insertRouter(router: MikroTikRouterEntity) {
        mikrotikDao.insertRouter(router)
        firestoreService.saveRouter(router)
    }

    suspend fun updateRouterStatus(routerId: String, connected: Boolean) {
        mikrotikDao.updateRouterStatus(routerId, connected)
        val router = mikrotikDao.getAllRouters().firstOrNull()?.find { it.id == routerId } ?: return
        firestoreService.saveRouter(router.copy(isConnected = connected))
    }

    suspend fun deleteRouter(id: String) {
        mikrotikDao.deleteRouterById(id)
        firestoreService.deleteDocument("mikrotik_routers", id)
    }

    suspend fun insertSmsLog(log: SmsLogEntity) {
        smsLogDao.insertSmsLog(log)
        firestoreService.saveSmsLog(log)
    }

    suspend fun updateSmsLog(log: SmsLogEntity) {
        smsLogDao.updateSmsLog(log)
        firestoreService.saveSmsLog(log)
    }

    suspend fun clearAllSmsLogs() {
        smsLogDao.clearAllSmsLogs()
        // Batch delete in Firestore not implemented yet
    }

    suspend fun insertExpense(expense: ExpenseEntity) {
        expenseDao.insertExpense(expense)
        firestoreService.saveExpense(expense)
    }

    suspend fun deleteExpenseById(id: String) {
        expenseDao.deleteExpenseById(id)
        firestoreService.deleteExpense(id)
    }

    suspend fun saveSettings(settings: ISPSettingsEntity) {
        settingsDao.saveSettings(settings)
        firestoreService.saveSettings(settings)
    }

    suspend fun setCustomerInternetStatus(customerId: String, enable: Boolean): Boolean {
        val customer = customerDao.getCustomerById(customerId) ?: return false
        val routers = mikrotikDao.getAllRouters().firstOrNull() ?: emptyList()
        
        val router = routers.firstOrNull { it.isConnected } ?: return false
        if (customer.pppoeUsername.isBlank()) return false

        val success = mikroTikApi.setPppoeUserStatus(router, customer.pppoeUsername, enable)
        
        if (success) {
            val nextStatus = if (enable) "Active" else "Suspended"
            updateCustomer(customer.copy(status = nextStatus))
        }
        return success
    }

    /**
     * Syncs customer speed, MAC, and IP to MikroTik.
     */
    suspend fun syncCustomerToMikroTik(customerId: String): Boolean {
        val customer = customerDao.getCustomerById(customerId) ?: return false
        val routers = mikrotikDao.getAllRouters().firstOrNull() ?: emptyList()
        val router = routers.firstOrNull { it.isConnected } ?: return false
        
        if (customer.pppoeUsername.isBlank()) return false

        return mikroTikApi.updatePppoeUser(
            router = router,
            pppoeUser = customer.pppoeUsername,
            profile = customer.packageName, // Using package name as profile name
            macAddress = customer.macAddress,
            staticIp = customer.ipAddress
        )
    }

    suspend fun getCustomerLiveTraffic(customerId: String): Pair<Double, Double>? {
        val customer = customerDao.getCustomerById(customerId) ?: return null
        val routers = mikrotikDao.getAllRouters().firstOrNull() ?: emptyList()
        val router = routers.firstOrNull { it.isConnected } ?: return null
        
        if (customer.pppoeUsername.isBlank()) return null
        return mikroTikApi.getPppoeUserTraffic(router, customer.pppoeUsername)
    }

    suspend fun findCustomerByPppoeInCloud(user: String, pass: String): CustomerEntity? {
        return try {
            val snapshot = firestore.collection("customers")
                .whereEqualTo("pppoeUsername", user.trim())
                .whereEqualTo("pppoePassword", pass.trim())
                .limit(1)
                .get()
                .await()
            
            val customer = snapshot.documents.firstOrNull()?.toObject(CustomerEntity::class.java)
            if (customer != null) {
                // Save to local for future use
                customerDao.insertCustomer(customer)
            }
            customer
        } catch (e: Exception) {
            Log.e("ISPRepository", "Cloud customer search failed", e)
            null
        }
    }

    suspend fun insertSmsTemplate(template: SmsTemplateEntity) {
        smsTemplateDao.insertTemplate(template)
        firestoreService.saveSmsTemplate(template)
    }

    suspend fun updateSmsTemplate(template: SmsTemplateEntity) {
        smsTemplateDao.updateTemplate(template)
        firestoreService.saveSmsTemplate(template)
    }

    suspend fun deleteSmsTemplate(id: String) {
        smsTemplateDao.deleteTemplateById(id)
        firestoreService.deleteSmsTemplate(id)
    }

    suspend fun insertInventoryItem(item: InventoryEntity) {
        inventoryDao.insertItem(item)
        firestoreService.saveDocument("inventory_items", item.id, item)
    }

    suspend fun updateInventoryItem(item: InventoryEntity) {
        inventoryDao.updateItem(item)
        firestoreService.saveDocument("inventory_items", item.id, item)
    }

    suspend fun deleteInventoryItem(id: String) {
        inventoryDao.deleteItemById(id)
        firestoreService.deleteDocument("inventory_items", id)
    }

    suspend fun assignItemToCustomer(itemId: String, customerId: String) {
        val item = inventoryDao.getAllInventory().firstOrNull()?.find { it.id == itemId } ?: return
        val updated = item.copy(assignedToCustomerId = customerId, status = "Assigned")
        updateInventoryItem(updated)
    }

    suspend fun insertSupportTicket(ticket: SupportTicketEntity) {
        supportTicketDao.insertTicket(ticket)
        firestoreService.saveDocument("support_tickets", ticket.id, ticket)
    }

    suspend fun updateSupportTicket(ticket: SupportTicketEntity) {
        supportTicketDao.updateTicket(ticket)
        firestoreService.saveDocument("support_tickets", ticket.id, ticket)
    }

    suspend fun deleteSupportTicket(id: String) {
        supportTicketDao.deleteTicketById(id)
        firestoreService.deleteDocument("support_tickets", id)
    }

    suspend fun seedSmsTemplatesIfEmpty() {
        if (smsTemplateDao.getAllTemplates().firstOrNull().isNullOrEmpty()) {
            val template = SmsTemplateEntity(id = UUID.randomUUID().toString(), title = "Bill Reminder", category = "Billing Alert", messageContent = "Due bill alert", lastUpdated = getCurrentDateString())
            firestoreService.saveSmsTemplate(template)
        }
    }

    suspend fun checkAndSuspendExpiredCustomers(): Int {
        val today = getCurrentDateString()
        val customersList = customerDao.getAllCustomers().firstOrNull() ?: emptyList()
        var suspendedCount = 0

        for (cust in customersList) {
            // Logic: If active, has due, and expireDate is today or past
            if (cust.status == "Active" && cust.currentDue > 0 && cust.expireDate.isNotBlank()) {
                if (cust.expireDate <= today) {
                    val success = setCustomerInternetStatus(cust.id, false)
                    if (success) {
                        suspendedCount++
                        Log.i("ISPRepository", "Auto-Suspended Customer: ${cust.name} (${cust.customerCode})")
                    }
                }
            }
        }
        return suspendedCount
    }

    private fun getCurrentDateString() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}
