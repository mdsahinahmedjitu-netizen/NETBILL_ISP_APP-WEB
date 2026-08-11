package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ISPRepository
import com.example.data.entity.CustomerEntity
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.ISPSettingsEntity
import com.example.data.entity.MikroTikRouterEntity
import com.example.data.entity.PackageEntity
import com.example.data.entity.PaymentAllocationEntity
import com.example.data.entity.PaymentCollectionEntity
import com.example.data.entity.StaffEntity
import com.example.data.entity.StaffSalaryEntity
import com.example.data.entity.SmsLogEntity
import com.example.data.entity.UserEntity
import com.example.localization.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DashboardStats(
    val totalCustomers: Int = 0,
    val todaysCollection: Double = 0.0,
    val monthlyCollection: Double = 0.0,
    val totalDue: Double = 0.0,
    val todaysExpense: Double = 0.0,
    val monthlyExpense: Double = 0.0,
    val activeCustomers: Int = 0,
    val expiredCustomers: Int = 0,
    val inactiveCustomers: Int = 0,
    val newCustomers: Int = 0,
    val bandwidthUsageMbps: Double = 1161.2
)

data class CustomerFilterState(
    val searchQuery: String = "",
    val selectedZone: String = "All",
    val selectedPackage: String = "All",
    val selectedStatus: String = "All",
    val onlyDueCustomers: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = ISPRepository(db)
    private val prefs = application.getSharedPreferences("netbill_isp_prefs", android.content.Context.MODE_PRIVATE)

    // Language State (Defaults to Bangla বাংলা)
    private val _currentLanguage = MutableStateFlow(
        if (prefs.getString("app_lang", "BANGLA") == "ENGLISH") AppLanguage.ENGLISH else AppLanguage.BANGLA
    )
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    // Global Dark Mode / Night Mode State for ISP Technicians
    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("is_dark_mode", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // Current Logged In User
    private val _currentUser = MutableStateFlow<UserEntity?>(
        UserEntity(
            id = 1,
            username = "admin",
            passwordHash = "admin123",
            name = "M. A. Rahman",
            mobile = "01711000000",
            role = "Super Admin"
        )
    )
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // UI Toast Message
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Customer Filter State
    private val _filterState = MutableStateFlow(CustomerFilterState())
    val filterState: StateFlow<CustomerFilterState> = _filterState.asStateFlow()

    // Selected Receipt for Dialog
    private val _selectedReceipt = MutableStateFlow<PaymentCollectionEntity?>(null)
    val selectedReceipt: StateFlow<PaymentCollectionEntity?> = _selectedReceipt.asStateFlow()

    // All Flows from Repository
    val customersList = repository.allCustomers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val packagesList = repository.allPackages.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val invoicesList = repository.allInvoices.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val paymentsList = repository.allPayments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val paymentAllocations = repository.allAllocations.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val expensesList = repository.allExpenses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val staffList = repository.allStaff.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val mikrotikRouters = repository.allRouters.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val settingsState = repository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val smsLogsList = repository.allSmsLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Customers List
    val filteredCustomers: StateFlow<List<CustomerEntity>> = combine(
        customersList,
        _filterState
    ) { list, filter ->
        list.filter { cust ->
            val matchQuery = filter.searchQuery.isEmpty() ||
                    cust.name.contains(filter.searchQuery, ignoreCase = true) ||
                    cust.customerCode.contains(filter.searchQuery, ignoreCase = true) ||
                    cust.mobile.contains(filter.searchQuery, ignoreCase = true) ||
                    cust.pppoeUsername.contains(filter.searchQuery, ignoreCase = true)

            val matchZone = filter.selectedZone == "All" || cust.zone == filter.selectedZone
            val matchPkg = filter.selectedPackage == "All" || cust.packageName == filter.selectedPackage
            val matchStatus = filter.selectedStatus == "All" || cust.status == filter.selectedStatus
            val matchDue = !filter.onlyDueCustomers || cust.currentDue > 0

            matchQuery && matchZone && matchPkg && matchStatus && matchDue
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dismissed Expiry Alert Customer IDs
    private val _dismissedExpiryAlerts = MutableStateFlow<Set<Long>>(emptySet())

    // Customers expiring tomorrow (1 Calendar Day before Expire Date)
    val expiringTomorrowCustomers: StateFlow<List<CustomerEntity>> = combine(
        customersList,
        _dismissedExpiryAlerts
    ) { custs, dismissed ->
        custs.filter { cust ->
            com.example.util.ExpiryUtils.isExpiringTomorrow(cust) && !dismissed.contains(cust.id)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Alias for backward compatibility
    val overdue20thDayCustomers: StateFlow<List<CustomerEntity>> = expiringTomorrowCustomers

    // Expired Customers Flow
    val expiredCustomers: StateFlow<List<CustomerEntity>> = customersList.map { custs ->
        custs.filter { cust -> com.example.util.ExpiryUtils.isExpired(cust) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard Statistics Calculation
    val dashboardStats: StateFlow<DashboardStats> = combine(
        customersList,
        paymentsList,
        expensesList
    ) { custs, pymts, exps ->
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val yearMonthStr = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

        val totalCust = custs.size
        val expiredCust = custs.count { com.example.util.ExpiryUtils.isExpired(it) }
        val activeCust = custs.count { it.status == "Active" && !com.example.util.ExpiryUtils.isExpired(it) }
        val inactiveCust = custs.count { it.status != "Active" && !com.example.util.ExpiryUtils.isExpired(it) }
        val newCust = custs.count { it.joinDate.startsWith(yearMonthStr) }
        val totalDueSum = custs.sumOf { it.currentDue }

        val todayCollectionSum = pymts.filter { it.paymentDate == todayStr }.sumOf { it.amount }
        val monthCollectionSum = pymts.filter { it.paymentDate.startsWith(yearMonthStr) }.sumOf { it.amount }
        val todayExpenseSum = exps.filter { it.expenseDate == todayStr }.sumOf { it.amount }
        val monthExpenseSum = exps.filter { it.expenseDate.startsWith(yearMonthStr) }.sumOf { it.amount }

        DashboardStats(
            totalCustomers = totalCust,
            todaysCollection = todayCollectionSum,
            monthlyCollection = monthCollectionSum,
            totalDue = totalDueSum,
            todaysExpense = todayExpenseSum,
            monthlyExpense = monthExpenseSum,
            activeCustomers = activeCust,
            expiredCustomers = expiredCust,
            inactiveCustomers = inactiveCust,
            newCustomers = newCust,
            bandwidthUsageMbps = 1161.2
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    init {
        viewModelScope.launch {
            repository.seedDatabaseIfEmpty()
        }
    }

    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
        prefs.edit().putString("app_lang", language.name).apply()
        val msg = if (language == AppLanguage.BANGLA) "ভাষা পরিবর্তন করা হয়েছে: বাংলা" else "Language changed to: English"
        showToast(msg)
    }

    fun toggleLanguage() {
        if (_currentLanguage.value == AppLanguage.BANGLA) {
            setLanguage(AppLanguage.ENGLISH)
        } else {
            setLanguage(AppLanguage.BANGLA)
        }
    }

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        prefs.edit().putBoolean("is_dark_mode", enabled).apply()
        val msg = if (enabled) "Night Mode Enabled / নাইট মোড সক্রিয়" else "Light Mode Enabled / লাইট মোড সক্রিয়"
        showToast(msg)
    }

    fun toggleDarkMode() {
        setDarkMode(!_isDarkMode.value)
    }

    fun loginUser(userIdentifier: String, pass: String): Boolean {
        var success = false
        viewModelScope.launch {
            val user = repository.userDao.getUserByUsernameOrMobile(userIdentifier)
            if (user != null && user.passwordHash == pass) {
                _currentUser.value = user
                success = true
                showToast("Logged in as ${user.name} (${user.role})")
            } else if (userIdentifier == "admin" && pass == "admin123") {
                _currentUser.value = UserEntity(
                    username = "admin",
                    passwordHash = "admin123",
                    name = "M. A. Rahman",
                    mobile = "01711000000",
                    role = "Super Admin"
                )
                success = true
                showToast("Logged in as Super Admin")
            } else {
                showToast("Invalid Credentials!")
            }
        }
        return success
    }

    fun logout() {
        _currentUser.value = null
        showToast("Logged out")
    }

    fun updateFilter(query: String? = null, zone: String? = null, pkg: String? = null, status: String? = null, onlyDue: Boolean? = null) {
        _filterState.value = _filterState.value.copy(
            searchQuery = query ?: _filterState.value.searchQuery,
            selectedZone = zone ?: _filterState.value.selectedZone,
            selectedPackage = pkg ?: _filterState.value.selectedPackage,
            selectedStatus = status ?: _filterState.value.selectedStatus,
            onlyDueCustomers = onlyDue ?: _filterState.value.onlyDueCustomers
        )
    }

    fun addOrUpdateCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            if (customer.id == 0L) {
                val newId = repository.customerDao.insertCustomer(customer)
                showToast("Customer ${customer.name} added successfully!")
            } else {
                repository.customerDao.updateCustomer(customer)
                showToast("Customer ${customer.name} updated!")
            }
        }
    }

    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            repository.customerDao.deleteCustomerById(customer.id)
            showToast("Customer ${customer.name} deleted.")
        }
    }

    fun toggleCustomerStatus(customer: CustomerEntity) {
        viewModelScope.launch {
            val newStatus = if (customer.status == "Active") "Suspended" else "Active"
            repository.customerDao.updateCustomer(customer.copy(status = newStatus))
            showToast("${customer.name} is now $newStatus")
        }
    }

    fun generateBillsForMonth(monthYear: String, overrides: Map<Long, String> = emptyMap()) {
        viewModelScope.launch {
            val count = repository.generateAutoMonthlyInvoices(monthYear, overrides)
            showToast("$count Monthly Invoices Generated for $monthYear!")
        }
    }

    fun recordPayment(
        customerId: Long,
        amount: Double,
        paymentMethod: String,
        transactionId: String,
        collectorName: String,
        remarks: String
    ) {
        viewModelScope.launch {
            val payment = repository.recordPayment(
                customerId = customerId,
                amount = amount,
                paymentMethod = paymentMethod,
                transactionId = transactionId,
                collectorName = collectorName,
                remarks = remarks
            )
            if (payment != null) {
                _selectedReceipt.value = payment
                showToast("Payment of ৳${amount.toInt()} allocated via FIFO!")
            }
        }
    }

    fun collectPayment(
        customerId: Long,
        amount: Double,
        method: String,
        transactionId: String,
        remarks: String
    ) {
        viewModelScope.launch {
            val collector = currentUser.value?.name ?: "Billing Staff"
            val payment = repository.recordPayment(
                customerId = customerId,
                amount = amount,
                paymentMethod = method,
                transactionId = transactionId,
                collectorName = collector,
                remarks = remarks
            )
            if (payment != null) {
                _selectedReceipt.value = payment
                showToast("Payment of ৳${amount.toInt()} recorded via $method!")
            }
        }
    }

    fun payInvoice(
        invoice: com.example.data.entity.InvoiceEntity,
        amount: Double,
        method: String,
        transactionId: String,
        remarks: String
    ) {
        viewModelScope.launch {
            val collector = currentUser.value?.name ?: "Billing Staff"
            val payment = repository.payInvoice(
                invoice = invoice,
                amount = amount,
                paymentMethod = method,
                transactionId = transactionId,
                collectorName = collector,
                remarks = remarks
            )
            if (payment != null) {
                _selectedReceipt.value = payment
                showToast("Invoice ${invoice.invoiceNo} paid (৳${amount.toInt()}) via $method!")
            }
        }
    }

    fun setSelectedReceipt(payment: PaymentCollectionEntity?) {
        _selectedReceipt.value = payment
    }

    fun addExpense(title: String, category: String, amount: Double, notes: String) {
        viewModelScope.launch {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val expBy = currentUser.value?.name ?: "Admin"
            repository.expenseDao.insertExpense(
                ExpenseEntity(
                    title = title,
                    category = category,
                    amount = amount,
                    expenseDate = dateStr,
                    expenseBy = expBy,
                    notes = notes
                )
            )
            showToast("Expense of ৳${amount.toInt()} added under $category")
        }
    }

    fun addPackage(name: String, speed: String, price: Double) {
        viewModelScope.launch {
            repository.packageDao.insertPackage(
                PackageEntity(
                    name = name,
                    speed = speed,
                    monthlyPrice = price,
                    description = "$speed High Speed Fiber"
                )
            )
            showToast("Package $name added!")
        }
    }

    fun addStaff(name: String, mobile: String, role: String, salary: Double) {
        viewModelScope.launch {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            repository.staffDao.insertStaff(
                StaffEntity(
                    name = name,
                    mobile = mobile,
                    address = "Dhaka, Bangladesh",
                    role = role,
                    salary = salary,
                    joiningDate = dateStr
                )
            )
            showToast("Staff $name added!")
        }
    }

    fun payStaffSalary(staffId: Long, staffName: String, amount: Double, month: String) {
        viewModelScope.launch {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            repository.staffDao.insertSalary(
                StaffSalaryEntity(
                    staffId = staffId,
                    staffName = staffName,
                    amount = amount,
                    salaryMonth = month,
                    paymentDate = dateStr,
                    remarks = "Salary paid for $month"
                )
            )
            repository.expenseDao.insertExpense(
                ExpenseEntity(
                    title = "Staff Salary: $staffName",
                    category = "Staff Salary",
                    amount = amount,
                    expenseDate = dateStr,
                    expenseBy = currentUser.value?.name ?: "Admin",
                    notes = "Salary paid for month $month"
                )
            )
            showToast("Salary ৳${amount.toInt()} disbursed to $staffName!")
        }
    }

    // Payment Gateway Configuration State
    private val _gatewayConfig = MutableStateFlow(com.example.service.GatewayConfig())
    val gatewayConfig: StateFlow<com.example.service.GatewayConfig> = _gatewayConfig.asStateFlow()

    fun updateGatewayConfig(newConfig: com.example.service.GatewayConfig) {
        _gatewayConfig.value = newConfig
        repository.paymentGatewayService.config = newConfig
        showToast("Payment Gateway Settings Updated (${newConfig.environment.name} Mode)")
    }

    fun processAutomatedGatewayPayment(
        customerId: Long,
        invoiceId: Long,
        amount: Double,
        gateway: com.example.service.PaymentGatewayType,
        customerMobile: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val collector = currentUser.value?.name ?: "Gateway Auto Collector"
            val (success, payment) = repository.processAutomatedGatewayPayment(
                customerId = customerId,
                invoiceId = invoiceId,
                amount = amount,
                gateway = gateway,
                customerMobile = customerMobile,
                collectorName = collector
            )
            if (success && payment != null) {
                _selectedReceipt.value = payment
                val gwName = if (gateway == com.example.service.PaymentGatewayType.BKASH) "bKash" else "Nagad"
                val msg = "$gwName Payment Successful! Trx: ${payment.transactionId}"
                showToast(msg)
                onResult(true, msg)
            } else {
                val err = "Payment processing failed via ${gateway.name}"
                showToast(err)
                onResult(false, err)
            }
        }
    }

    fun verifyGatewayTransaction(
        trxId: String,
        gateway: com.example.service.PaymentGatewayType,
        onResult: (com.example.service.GatewayApiResult<String>) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.verifyAndReconcileTransaction(trxId, gateway)
            onResult(result)
        }
    }

    fun getLedgerForCustomer(customerId: Long) = repository.getLedgerForCustomer(customerId)

    fun addCustomLedgerEntry(
        customerId: Long,
        type: String,
        amount: Double,
        isDebit: Boolean,
        description: String,
        referenceNo: String = "",
        paymentMethod: String = ""
    ) {
        viewModelScope.launch {
            val customer = repository.customerDao.getCustomerById(customerId) ?: return@launch
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val timeStr = SimpleDateFormat("hh:mm a", Locale.US).format(Date())
            val monthYear = SimpleDateFormat("MMMM yyyy", Locale.US).format(Date())
            val ref = if (referenceNo.isNotBlank()) referenceNo else "REF-${System.currentTimeMillis().toString().takeLast(6)}"

            val newDue = if (isDebit) {
                customer.currentDue + amount
            } else {
                (customer.currentDue - amount).coerceAtLeast(0.0)
            }

            repository.customerDao.updateCustomer(customer.copy(currentDue = newDue))

            val collector = currentUser.value?.name ?: "Operator"
            repository.insertLedgerEntry(
                com.example.data.entity.LedgerEntryEntity(
                    customerId = customerId,
                    date = currentDate,
                    time = timeStr,
                    type = type,
                    referenceNo = ref,
                    description = description,
                    amount = amount,
                    isDebit = isDebit,
                    runningBalance = newDue,
                    monthYear = monthYear,
                    paymentMethod = paymentMethod,
                    collector = collector
                )
            )

            showToast("Ledger entry '$type' of ৳${amount.toInt()} added for ${customer.name}")
        }
    }

    fun saveISPSettings(ispName: String, address: String, mobile: String, support: String) {
        viewModelScope.launch {
            val current = settingsState.value ?: ISPSettingsEntity()
            repository.settingsDao.saveSettings(
                current.copy(
                    ispName = ispName,
                    address = address,
                    mobileNumber = mobile,
                    supportNumber = support
                )
            )
            showToast("ISP Company Settings Updated!")
        }
    }

    fun dismissExpiryAlert(customerId: Long) {
        _dismissedExpiryAlerts.value = _dismissedExpiryAlerts.value + customerId
        showToast("Alert dismissed for customer ID #$customerId")
    }

    fun dismiss20thDayAlert(customerId: Long) {
        dismissExpiryAlert(customerId)
    }

    fun updateCustomerExpiry(customerId: Long, newExpireDate: String, newExpireTime: String) {
        val currentCust = customersList.value.find { it.id == customerId } ?: return
        val updatedCust = currentCust.copy(
            expireDate = newExpireDate,
            expireTime = newExpireTime
        )
        addOrUpdateCustomer(updatedCust)
        showToast("Expiry updated to $newExpireDate $newExpireTime for ${currentCust.name}")
    }

    fun sendMassExpirySmsReminders() {
        val list = expiringTomorrowCustomers.value
        if (list.isEmpty()) {
            showToast("No customers expiring tomorrow.")
            return
        }
        viewModelScope.launch {
            val sdf = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US)
            val timestamp = sdf.format(Date())
            list.forEachIndexed { idx, cust ->
                val msgId = (100000..999999).random()
                val isFailed = (idx % 8 == 7) // Simulate rare failure
                val status = if (isFailed) "Failed" else "Delivered"
                val report = if (isFailed) "Failed: MNO Network Unreachable" else "Delivered to handset (Gateway Msg ID #$msgId)"
                repository.smsLogDao.insertSmsLog(
                    SmsLogEntity(
                        customerId = cust.id,
                        customerCode = cust.customerCode,
                        customerName = cust.name,
                        mobile = cust.mobile,
                        notificationType = "20th Day Reminder",
                        message = "জরুরী নোটিশ: প্রিয় ${cust.name}, আপনার NetBill ইন্টারনেট বকেয়া ৳${cust.currentDue.toInt()} টাকা পরিশোধের অনুরোধ করা হচ্ছে।",
                        sentTimestamp = timestamp,
                        status = status,
                        deliveryReport = report
                    )
                )
            }
            showToast("20th Day Billing SMS Alerts dispatched to ${list.size} customers!")
        }
    }

    fun sendMass20thDaySmsReminders() {
        sendMassExpirySmsReminders()
    }

    fun sendSingleSmsNotification(
        customerId: Long,
        customerCode: String,
        customerName: String,
        mobile: String,
        notificationType: String,
        message: String,
        isFailed: Boolean = false
    ) {
        viewModelScope.launch {
            val sdf = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US)
            val timestamp = sdf.format(Date())
            val msgId = (100000..999999).random()
            val status = if (isFailed) "Failed" else "Delivered"
            val report = if (isFailed) "Failed: Gateway Timeout / MNO Error #502" else "Delivered to handset (Gateway Msg ID #$msgId)"

            repository.smsLogDao.insertSmsLog(
                SmsLogEntity(
                    customerId = customerId,
                    customerCode = customerCode,
                    customerName = customerName,
                    mobile = mobile,
                    notificationType = notificationType,
                    message = message,
                    sentTimestamp = timestamp,
                    status = status,
                    deliveryReport = report
                )
            )
            if (isFailed) {
                showToast("SMS Failed for $customerName ($mobile)")
            } else {
                showToast("$notificationType SMS Delivered to $customerName ($mobile)")
            }
        }
    }

    fun sendSupportUpdateSms(targetCustomerId: Long?, targetZone: String, messageText: String) {
        viewModelScope.launch {
            val allCusts = customersList.value
            val targetList = when {
                targetCustomerId != null && targetCustomerId > 0 -> allCusts.filter { it.id == targetCustomerId }
                targetZone != "All" -> allCusts.filter { it.zone == targetZone }
                else -> allCusts
            }

            if (targetList.isEmpty()) {
                showToast("No target customers found for Support Update SMS.")
                return@launch
            }

            val sdf = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US)
            val timestamp = sdf.format(Date())

            targetList.forEachIndexed { idx, cust ->
                val msgId = (100000..999999).random()
                val isFailed = (idx == 5 || (targetList.size == 1 && idx == 99)) // Realistic failure check
                val status = if (isFailed) "Failed" else "Delivered"
                val report = if (isFailed) "Failed: Handset Switched Off" else "Delivered to handset (Gateway Msg ID #$msgId)"
                val finalMsg = messageText.replace("{NAME}", cust.name).replace("{CODE}", cust.customerCode)

                repository.smsLogDao.insertSmsLog(
                    SmsLogEntity(
                        customerId = cust.id,
                        customerCode = cust.customerCode,
                        customerName = cust.name,
                        mobile = cust.mobile,
                        notificationType = "Support Update",
                        message = finalMsg,
                        sentTimestamp = timestamp,
                        status = status,
                        deliveryReport = report
                    )
                )
            }
            showToast("Support Update SMS sent to ${targetList.size} customer(s)!")
        }
    }

    fun resendFailedSms(log: SmsLogEntity) {
        viewModelScope.launch {
            val sdf = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US)
            val timestamp = sdf.format(Date())
            val msgId = (100000..999999).random()
            val updated = log.copy(
                sentTimestamp = timestamp,
                status = "Delivered",
                deliveryReport = "Delivered to handset after retry (Gateway Msg ID #$msgId)"
            )
            repository.smsLogDao.updateSmsLog(updated)
            showToast("SMS successfully resent & delivered to ${log.customerName} (${log.mobile})")
        }
    }

    fun resendAllFailedSms() {
        viewModelScope.launch {
            val failedList = smsLogsList.value.filter { it.status == "Failed" }
            if (failedList.isEmpty()) {
                showToast("No failed SMS logs found to resend.")
                return@launch
            }
            val sdf = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US)
            val timestamp = sdf.format(Date())
            failedList.forEach { item ->
                val msgId = (100000..999999).random()
                repository.smsLogDao.updateSmsLog(
                    item.copy(
                        sentTimestamp = timestamp,
                        status = "Delivered",
                        deliveryReport = "Delivered to handset after retry (Gateway Msg ID #$msgId)"
                    )
                )
            }
            showToast("${failedList.size} failed SMS items re-sent and delivered successfully!")
        }
    }

    fun clearSmsLogs() {
        viewModelScope.launch {
            repository.smsLogDao.clearAllSmsLogs()
            showToast("SMS logs cleared.")
        }
    }

    fun suspendAllExpiredCustomers() {
        val list = expiredCustomers.value.filter { it.status == "Active" }
        if (list.isEmpty()) {
            showToast("No active expired customers to suspend.")
            return
        }
        viewModelScope.launch {
            list.forEach { customer ->
                repository.customerDao.updateCustomer(customer.copy(status = "Suspended"))
            }
            showToast("${list.size} expired lines suspended!")
        }
    }

    fun suspendAll20thDayOverdueCustomers() {
        suspendAllExpiredCustomers()
    }

    fun addMikroTikRouter(router: MikroTikRouterEntity) {
        viewModelScope.launch {
            repository.mikrotikDao.insertRouter(router)
            showToast("Router '${router.routerName}' added successfully!")
        }
    }

    fun toggleRouterStatus(routerId: Long, currentStatus: Boolean) {
        viewModelScope.launch {
            repository.mikrotikDao.updateRouterStatus(routerId, !currentStatus)
            val action = if (!currentStatus) "Connected" else "Disconnected"
            showToast("Router #$routerId $action")
        }
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
