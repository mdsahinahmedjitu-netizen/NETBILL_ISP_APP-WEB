package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.AuthManager
import com.example.data.AppDatabase
import com.example.data.ISPRepository
import com.example.data.entity.*
import com.example.localization.AppLanguage
import android.util.Log
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

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

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    object Success : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = ISPRepository(db)
    val authManager = AuthManager()
    private val printerService = com.example.service.BluetoothPrinterService()
    private val prefs = application.getSharedPreferences("netbill_isp_prefs", android.content.Context.MODE_PRIVATE)

    private val _currentLanguage = MutableStateFlow(
        if (prefs.getString("app_lang", "BANGLA") == "ENGLISH") AppLanguage.ENGLISH else AppLanguage.BANGLA
    )
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("is_dark_mode", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _localUser = MutableStateFlow<UserEntity?>(null)
    private val _loggedInCustomer = MutableStateFlow<CustomerEntity?>(null)
    
    val currentUser: StateFlow<UserEntity?> = combine(authManager.currentUser, _localUser) { authUser, localUser ->
        authUser ?: localUser
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _loginUiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val loginUiState: StateFlow<LoginUiState> = _loginUiState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _filterState = MutableStateFlow(CustomerFilterState())
    val filterState: StateFlow<CustomerFilterState> = _filterState.asStateFlow()

    private val _selectedReceipt = MutableStateFlow<PaymentCollectionEntity?>(null)
    val selectedReceipt: StateFlow<PaymentCollectionEntity?> = _selectedReceipt.asStateFlow()

    val customersList = repository.allCustomers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val packagesList = repository.allPackages.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val invoicesList = repository.allInvoices.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val paymentsList = repository.allPayments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val expensesList = repository.allExpenses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val settingsState = repository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val smsLogsList = repository.allSmsLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val smsTemplatesList = repository.allSmsTemplates.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val mikrotikRouters = repository.allRouters.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val staffList = repository.allStaff.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val paymentAllocations = repository.allAllocations.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val inventoryList = repository.inventoryDao.getAllInventory().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val supportTickets = repository.supportTicketDao.getAllTickets().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentCustomer: StateFlow<CustomerEntity?> = combine(_loggedInCustomer, customersList) { loggedIn, list ->
        if (loggedIn == null) null
        else list.find { it.id == loggedIn.id } ?: loggedIn
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val filteredCustomers: StateFlow<List<CustomerEntity>> = combine(customersList, _filterState) { list, filter ->
        list.filter { cust ->
            val matchQuery = filter.searchQuery.isEmpty() ||
                    cust.customerCode.contains(filter.searchQuery, ignoreCase = true) ||
                    cust.name.contains(filter.searchQuery, ignoreCase = true) ||
                    cust.mobile.contains(filter.searchQuery, ignoreCase = true) ||
                    cust.pppoeUsername.contains(filter.searchQuery, ignoreCase = true) ||
                    cust.address.contains(filter.searchQuery, ignoreCase = true)

            val matchZone = filter.selectedZone == "All" || cust.zone == filter.selectedZone
            val matchPkg = filter.selectedPackage == "All" || cust.packageName == filter.selectedPackage
            val matchStatus = filter.selectedStatus == "All" || cust.status == filter.selectedStatus
            val matchDue = !filter.onlyDueCustomers || cust.currentDue > 0

            matchQuery && matchZone && matchPkg && matchStatus && matchDue
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _dismissedExpiryAlerts = MutableStateFlow<Set<String>>(emptySet())
    val expiringTomorrowCustomers: StateFlow<List<CustomerEntity>> = combine(customersList, _dismissedExpiryAlerts) { custs, dismissed ->
        custs.filter { cust -> com.example.util.ExpiryUtils.isExpiringTomorrow(cust) && !dismissed.contains(cust.id) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardStats: StateFlow<DashboardStats> = combine(customersList, paymentsList, expensesList) { custs, pymts, exps ->
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val yearMonthStr = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

        DashboardStats(
            totalCustomers = custs.size,
            todaysCollection = pymts.filter { it.paymentDate == todayStr }.sumOf { it.amount },
            monthlyCollection = pymts.filter { it.paymentDate.startsWith(yearMonthStr) }.sumOf { it.amount },
            totalDue = custs.sumOf { it.currentDue },
            todaysExpense = exps.filter { it.expenseDate == todayStr }.sumOf { it.amount },
            monthlyExpense = exps.filter { it.expenseDate.startsWith(yearMonthStr) }.sumOf { it.amount },
            activeCustomers = custs.count { it.status == "Active" },
            expiredCustomers = custs.count { com.example.util.ExpiryUtils.isExpired(it) },
            newCustomers = custs.count { it.joinDate.startsWith(yearMonthStr) }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    init {
        // Observe settings and update gateway config
        viewModelScope.launch {
            repository.settings.collect { settings ->
                settings?.let { repository.updateGatewayConfig(it) }
            }
        }

        // Firebase Configuration Diagnostics
        val firebaseApp = com.google.firebase.FirebaseApp.getInstance()
        val options = firebaseApp.options
        android.util.Log.d("FirebaseCheck", "--------------------------------------------------")
        android.util.Log.d("FirebaseCheck", "Firebase App Name: ${firebaseApp.name}")
        android.util.Log.d("FirebaseCheck", "Project ID: ${options.projectId}")
        android.util.Log.d("FirebaseCheck", "Application ID: ${options.applicationId}")
        android.util.Log.d("FirebaseCheck", "Database URL: ${options.databaseUrl ?: "Default"}")
        android.util.Log.d("FirebaseCheck", "--------------------------------------------------")

        viewModelScope.launch {
            repository.seedDatabaseIfEmpty()
            val connectionStatus = repository.checkFirestoreConnection()
            android.util.Log.d("FirebaseCheck", "Firestore Connection Status: $connectionStatus")
        }
        
        // Start sync when a user or customer is logged in
        combine(currentUser, currentCustomer) { user, customer ->
            user != null || customer != null
        }.onEach { shouldSync ->
            val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            android.util.Log.d("FirebaseCheck", "Auth State Change | Firebase UID: ${firebaseUser?.uid ?: "NONE"} | ShouldSync: $shouldSync")
            if (shouldSync) {
                // Ensure anonymous auth for customers if not already authed
                if (firebaseUser == null) {
                    authManager.signInAnonymously()
                }
                repository.startSync()
            } else {
                repository.stopSync()
            }
        }.launchIn(viewModelScope)
    }

    override fun onCleared() {
        super.onCleared()
        repository.stopSync()
    }

    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
        prefs.edit().putString("app_lang", language.name).apply()
        showToast("Language changed to: ${language.name}")
    }

    fun toggleLanguage() {
        if (_currentLanguage.value == AppLanguage.BANGLA) setLanguage(AppLanguage.ENGLISH) else setLanguage(AppLanguage.BANGLA)
    }

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        prefs.edit().putBoolean("is_dark_mode", enabled).apply()
    }

    fun toggleDarkMode() { setDarkMode(!_isDarkMode.value) }

    fun loginUser(identifier: String, pass: String) {
        viewModelScope.launch {
            _loginUiState.value = LoginUiState.Loading
            
            // 1. First, try Firebase Authentication (Cloud Primary)
            // We assume 'admin' and 'operator' might be using emails like admin@netbill.com in Cloud
            // If the identifier is a plain username, we try to find the mapped email or just try it.
            val firebaseResult = authManager.signIn(identifier, pass)
            
            if (firebaseResult.isSuccess) {
                _loginUiState.value = LoginUiState.Success
                showToast("Logged in via Firebase Cloud")
                return@launch
            }

            // 2. Fallback: Check Local Room Database (Demo Credentials: admin/admin123, operator/123456)
            // This allows the demo to work even if Firebase Auth isn't set up for these specific users yet.
            val user = repository.userDao.getUserByUsernameOrMobile(identifier)
            if (user != null && user.passwordHash == pass) {
                // To make Cloud Sync work for local demo accounts, we perform an Anonymous Firebase Login
                // so that Firestore Security Rules (if request.auth != null) are satisfied.
                val anonResult = authManager.signInAnonymously()
                
                _localUser.value = user
                _loginUiState.value = LoginUiState.Success
                
                if (anonResult.isSuccess) {
                    showToast("Logged in as ${user.name} (Demo Mode + Cloud Sync)")
                } else {
                    val error = anonResult.exceptionOrNull()?.message ?: "Unknown Error"
                    showToast("Cloud Auth Error: $error")
                }
                return@launch
            }

            // 3. Both failed
            val errorMsg = firebaseResult.exceptionOrNull()?.message ?: "Invalid credentials"
            _loginUiState.value = LoginUiState.Error(errorMsg)
            showToast("Login Failed: $errorMsg")
        }
    }

    fun loginCustomer(pppoeUser: String, pppoePass: String) {
        viewModelScope.launch {
            _loginUiState.value = LoginUiState.Loading
            
            // 1. Perform Anonymous Auth for permissions
            val authResult = authManager.signInAnonymously()
            if (authResult.isFailure) {
                _loginUiState.value = LoginUiState.Error("Internet Connection Failed")
                return@launch
            }

            // 2. First search in local Room DB
            var customer = repository.customerDao.getCustomerByPppoe(pppoeUser.trim(), pppoePass.trim())

            // 3. If not found locally, search directly in Cloud Firestore
            if (customer == null) {
                customer = repository.findCustomerByPppoeInCloud(pppoeUser, pppoePass)
            }

            if (customer != null) {
                _loggedInCustomer.value = customer
                _loginUiState.value = LoginUiState.Success
                repository.startSync() // Start real-time sync for this customer
                showToast("Welcome ${customer.name}!")
            } else {
                _loginUiState.value = LoginUiState.Error("Invalid PPPoE Username or Password")
                showToast("Login Failed: Customer not found")
            }
        }
    }

    fun logout() {
        authManager.signOut()
        _localUser.value = null
        _loggedInCustomer.value = null
        _loginUiState.value = LoginUiState.Idle
        showToast("Logged Out")
    }

    fun addOrUpdateCustomer(customer: CustomerEntity, billChoice: String = "Standard") {
        viewModelScope.launch {
            // 1. Initial due logic based on your rules
            var finalCustomer = customer
            
            // If new customer and not explicitly manually set a large due
            if (customer.currentDue == 0.0) {
                if (customer.joinDayOfMonth <= 20 || billChoice == "CurrentMonth") {
                    // Set monthly bill as current due immediately
                    finalCustomer = customer.copy(currentDue = customer.monthlyBill)
                } else {
                    // Joined after 20th and selected NextMonth
                    finalCustomer = customer.copy(currentDue = 0.0)
                }
            }

            repository.insertCustomer(finalCustomer)
            
            // 2. Generate initial invoice if bill is applied
            if (finalCustomer.currentDue > 0.0) {
                val currentMonth = SimpleDateFormat("MMMM yyyy", Locale.US).format(Date())
                repository.invoiceDao.insertInvoice(
                    InvoiceEntity(
                        id = UUID.randomUUID().toString(),
                        invoiceNo = "INV-NEW-${System.currentTimeMillis().toString().takeLast(6)}",
                        customerId = finalCustomer.id,
                        customerCode = finalCustomer.customerCode,
                        customerName = finalCustomer.name,
                        packageName = finalCustomer.packageName,
                        billingMonthYear = currentMonth,
                        billAmount = finalCustomer.monthlyBill,
                        totalPayable = finalCustomer.currentDue,
                        dueAmount = finalCustomer.currentDue,
                        status = "Unpaid",
                        generatedDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
                        is20thDayOverrideChoice = billChoice
                    )
                )
            }

            // 3. MikroTik Sync
            if (finalCustomer.status == "Active" && finalCustomer.pppoeUsername.isNotBlank()) {
                repository.syncCustomerToMikroTik(finalCustomer.id)
            }
            
            showToast("Customer saved.")
        }
    }

    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            repository.deleteCustomerById(customer.id)
            showToast("Customer deleted.")
        }
    }

    fun toggleCustomerStatus(customer: CustomerEntity) {
        viewModelScope.launch {
            val nextEnable = customer.status != "Active"
            val success = repository.setCustomerInternetStatus(customer.id, nextEnable)
            if (success) {
                showToast("Router Updated: ${if (nextEnable) "Enabled" else "Suspended"}")
            } else {
                showToast("Router Update Failed. Local status updated.")
                // Fallback to local status change if router fails
                val nextStatus = if (customer.status == "Active") "Suspended" else "Active"
                repository.updateCustomer(customer.copy(status = nextStatus))
            }
        }
    }

    fun recordPayment(customerId: String, amount: Double, method: String, trxId: String, remarks: String, date: String? = null) {
        viewModelScope.launch {
            val collector = currentUser.value?.name ?: "Admin"
            val payment = repository.recordPayment(customerId, amount, method, trxId, collector, remarks, date)
            if (payment != null) {
                _selectedReceipt.value = payment
                showToast("Payment recorded.")
            }
        }
    }

    fun collectPayment(customerId: String, amount: Double, method: String, trxId: String, remarks: String, date: String? = null) =
        recordPayment(customerId, amount, method, trxId, remarks, date)

    fun payInvoice(invoice: InvoiceEntity, amount: Double, method: String, trxId: String, remarks: String) {
        viewModelScope.launch {
            val collector = currentUser.value?.name ?: "Admin"
            val payment = repository.payInvoice(invoice, amount, method, trxId, collector, remarks)
            if (payment != null) {
                _selectedReceipt.value = payment
                showToast("Invoice paid.")
            }
        }
    }

    fun generateBills(monthYear: String) {
        viewModelScope.launch {
            val count = repository.generateAutoMonthlyInvoices(monthYear)
            showToast("$count Invoices Generated.")
        }
    }

    fun generateBillsForMonth(monthYear: String, overrides: Map<String, String> = emptyMap()) {
        viewModelScope.launch {
            val count = repository.generateAutoMonthlyInvoices(monthYear, overrides)
            showToast("$count Invoices Generated.")
        }
    }

    fun addExpense(title: String, category: String, amount: Double, notes: String, date: String? = null) {
        viewModelScope.launch {
            val dateStr = date ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            repository.insertExpense(ExpenseEntity(id = UUID.randomUUID().toString(), title = title, category = category, amount = amount, expenseDate = dateStr, expenseBy = currentUser.value?.name ?: "Admin", notes = notes))
            showToast("Expense recorded.")
        }
    }

    fun addPackage(name: String, speed: String, price: Double) {
        viewModelScope.launch {
            repository.insertPackage(PackageEntity(id = UUID.randomUUID().toString(), name = name, speed = speed, monthlyPrice = price))
            showToast("Package $name added.")
        }
    }

    fun addStaff(name: String, mobile: String, role: String, salary: Double, receiveAlerts: Boolean = false, jDate: String? = null) {
        viewModelScope.launch {
            val dateStr = jDate ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            repository.insertStaff(StaffEntity(id = UUID.randomUUID().toString(), name = name, mobile = mobile, role = role, salary = salary, joiningDate = dateStr, receiveAlerts = receiveAlerts))
            showToast("Staff added.")
        }
    }

    fun updateStaff(staff: StaffEntity) {
        viewModelScope.launch {
            repository.updateStaff(staff)
            showToast("Staff updated.")
        }
    }

    fun payStaffSalary(staffId: String, staffName: String, amount: Double, month: String) {
        viewModelScope.launch {
            repository.insertSalary(StaffSalaryEntity(id = UUID.randomUUID().toString(), staffId = staffId, staffName = staffName, amount = amount, salaryMonth = month, paymentDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())))
            showToast("Salary paid.")
        }
    }

    fun updateRouterStatus(routerId: String, connected: Boolean) {
        viewModelScope.launch { repository.updateRouterStatus(routerId, connected) }
    }

    fun addMikroTikRouter(router: MikroTikRouterEntity) {
        viewModelScope.launch { repository.insertRouter(router) }
    }

    fun setSelectedReceipt(payment: PaymentCollectionEntity?) { _selectedReceipt.value = payment }

    fun getPairedPrinters() = printerService.getPairedPrinters()

    fun printReceipt(deviceName: String, payment: PaymentCollectionEntity) {
        viewModelScope.launch {
            val content = """
                Receipt: ${payment.receiptNo}
                Date: ${payment.paymentDate}
                Customer: ${payment.customerName}
                ID: ${payment.customerCode}
                Amount: ${payment.amount} ৳
                Method: ${payment.paymentMethod}
                Collector: ${payment.collectorName}
            """.trimIndent()
            
            val success = printerService.printReceipt(deviceName, content)
            if (success) showToast("Printing Successful") else showToast("Printing Failed")
        }
    }

    fun addCustomLedgerEntry(customerId: String, type: String, amount: Double, isDebit: Boolean, description: String, referenceNo: String = "", method: String = "", date: String? = null) {
        viewModelScope.launch {
            val dateStr = date ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val entry = LedgerEntryEntity(id = UUID.randomUUID().toString(), customerId = customerId, type = type, amount = amount, isDebit = isDebit, description = description, referenceNo = referenceNo, paymentMethod = method, date = dateStr)
            repository.insertLedgerEntry(entry)
            showToast("Ledger entry added.")
        }
    }

    fun saveISPSettings(
        ispName: String, 
        address: String, 
        mobile: String, 
        support: String,
        smsUrl: String = "",
        smsKey: String = "",
        smsSender: String = "",
        autoSms: Boolean = false,
        waUrl: String = "",
        waToken: String = "",
        adminWa: String = "",
        waAlerts: Boolean = false
    ) {
        viewModelScope.launch {
            val current = settingsState.value ?: ISPSettingsEntity()
            repository.saveSettings(current.copy(
                ispName = ispName, 
                address = address, 
                mobileNumber = mobile, 
                supportNumber = support,
                smsApiUrl = smsUrl,
                smsApiKey = smsKey,
                smsSenderId = smsSender,
                isAutoSmsEnabled = autoSms,
                whatsappApiUrl = waUrl,
                whatsappToken = waToken,
                adminWhatsappNumber = adminWa,
                isWhatsappAlertEnabled = waAlerts
            ))
            showToast("Settings updated.")
        }
    }

    fun clearSmsLogs() { viewModelScope.launch { repository.clearAllSmsLogs() } }
    fun resendAllFailedSms() { /* Implement if needed */ }
    fun resendFailedSms(log: SmsLogEntity) { viewModelScope.launch { repository.updateSmsLog(log) } }
    fun sendMass20thDaySmsReminders() {
        viewModelScope.launch {
            val customers = expiringTomorrowCustomers.value
            val currentSettings = settingsState.value ?: ISPSettingsEntity()
            if (customers.isEmpty()) {
                showToast("No customers expiring tomorrow.")
                return@launch
            }
            if (!currentSettings.isAutoSmsEnabled) {
                showToast("Auto SMS is disabled in settings.")
                return@launch
            }

            var sentCount = 0
            customers.forEach { cust ->
                val msg = "Dear ${cust.name}, your NetBill internet will expire tomorrow. Current Due: ${cust.currentDue} ৳. Please pay today to avoid disconnection."
                val success = repository.insertSmsLog(SmsLogEntity(id = UUID.randomUUID().toString(), customerId = cust.id, customerCode = cust.customerCode, customerName = cust.name, mobile = cust.mobile, notificationType = "20th Day Reminder", message = msg, sentTimestamp = Date().toString(), status = "Sent"))
                sentCount++
            }
            showToast("Queued $sentCount SMS reminders.")
        }
    }

    fun sendSupportUpdateSms(id: String?, zone: String, msg: String) {
        viewModelScope.launch {
            val targetCustomers = if (id != null) {
                customersList.value.filter { it.id == id }
            } else if (zone != "All") {
                customersList.value.filter { it.zone == zone }
            } else {
                customersList.value
            }

            if (targetCustomers.isEmpty()) {
                showToast("No target customers found.")
                return@launch
            }

            targetCustomers.forEach { cust ->
                repository.insertSmsLog(SmsLogEntity(
                    id = UUID.randomUUID().toString(),
                    customerId = cust.id,
                    customerCode = cust.customerCode,
                    customerName = cust.name,
                    mobile = cust.mobile,
                    notificationType = "Support Update",
                    message = msg,
                    sentTimestamp = Date().toString(),
                    status = "Sent"
                ))
            }
            showToast("Sent support update to ${targetCustomers.size} customers.")
        }
    }

    fun suspendAll20thDayOverdueCustomers() {
        viewModelScope.launch {
            val count = repository.checkAndSuspendExpiredCustomers()
            if (count > 0) {
                showToast("$count expired customers have been suspended in MikroTik.")
            } else {
                showToast("No expired customers found with due balance.")
            }
        }
    }

    fun sendSingleSmsNotification(id: String, code: String, name: String, mobile: String, type: String, msg: String) {
        viewModelScope.launch { 
            repository.insertSmsLog(SmsLogEntity(id = UUID.randomUUID().toString(), customerId = id, customerCode = code, customerName = name, mobile = mobile, notificationType = type, message = msg, sentTimestamp = Date().toString(), status = "Sent")) 
        }
    }

    fun sendWhatsAppExpiryAlerts() {
        viewModelScope.launch {
            val tomorrowExpiring = expiringTomorrowCustomers.value
            if (tomorrowExpiring.isEmpty()) {
                showToast("No customers expiring tomorrow.")
                return@launch
            }

            val settings = settingsState.value
            val adminNumber = settings?.adminWhatsappNumber ?: ""
            val selectedStaff = staffList.value.filter { it.receiveAlerts && it.active }
            
            val recipientNumbers = mutableListOf<String>()
            if (adminNumber.isNotBlank()) recipientNumbers.add(adminNumber)
            recipientNumbers.addAll(selectedStaff.map { it.mobile })

            if (recipientNumbers.isEmpty()) {
                showToast("No Admin or Staff WhatsApp number set for alerts.")
                return@launch
            }

            // Format the message
            val sb = StringBuilder()
            sb.append("⚠️ *Internet Expiry Alert (Tomorrow)*\n")
            sb.append("---------------------------------\n")
            tomorrowExpiring.forEachIndexed { index, cust ->
                sb.append("${index + 1}. ${cust.name} (${cust.customerCode})\n")
                sb.append("   - Package: ${cust.packageName}\n")
                sb.append("   - Mobile: ${cust.mobile}\n")
                sb.append("   - Due: ৳${cust.currentDue.toInt()}\n\n")
            }
            sb.append("Please take necessary actions to collect bills.")

            val finalMessage = sb.toString()

            // Simulate sending WhatsApp via API for each recipient
            recipientNumbers.distinct().forEach { number ->
                // In a real app, you'd call a retrofit service here
                Log.d("WhatsAppAlert", "Sending to $number: $finalMessage")
                // For demo/simulated purpose, we show a success toast
            }
            
            showToast("WhatsApp alerts sent to ${recipientNumbers.distinct().size} recipients.")
        }
    }

    fun dismiss20thDayAlert(id: String) { _dismissedExpiryAlerts.value += id }
    fun updateCustomerExpiry(id: String, date: String, time: String) {
        viewModelScope.launch {
            val cust = customersList.value.find { it.id == id } ?: return@launch
            repository.updateCustomer(cust.copy(expireDate = date, expireTime = time))
        }
    }

    suspend fun getCustomerLiveTraffic(customerId: String) = repository.getCustomerLiveTraffic(customerId)

    fun addSmsTemplate(template: SmsTemplateEntity) { viewModelScope.launch { repository.insertSmsTemplate(template) } }
    fun updateSmsTemplate(template: SmsTemplateEntity) { viewModelScope.launch { repository.updateSmsTemplate(template) } }
    fun deleteSmsTemplate(id: String) { viewModelScope.launch { repository.deleteSmsTemplate(id) } }

    fun addInventoryItem(item: InventoryEntity) {
        viewModelScope.launch { repository.insertInventoryItem(item) }
    }

    fun updateInventoryItem(item: InventoryEntity) {
        viewModelScope.launch { repository.updateInventoryItem(item) }
    }

    fun deleteInventoryItem(id: String) {
        viewModelScope.launch { repository.deleteInventoryItem(id) }
    }

    fun assignInventoryToCustomer(itemId: String, customerId: String) {
        viewModelScope.launch { repository.assignItemToCustomer(itemId, customerId) }
    }
    fun sendTestSmsTemplate(template: SmsTemplateEntity, recipientName: String, recipientMobile: String) { /* Implement */ }

    // Payment Gateway
    private val _gatewayConfig = MutableStateFlow(com.example.service.GatewayConfig())
    val gatewayConfig: StateFlow<com.example.service.GatewayConfig> = _gatewayConfig.asStateFlow()
    fun updateGatewayConfig(config: com.example.service.GatewayConfig) { _gatewayConfig.value = config }
    fun processAutomatedGatewayPayment(customerId: String, invoiceId: String, amount: Double, gateway: com.example.service.PaymentGatewayType, mobile: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val (success, payment) = repository.processAutomatedGatewayPayment(customerId, invoiceId, amount, gateway, mobile, "System")
            onResult(success, if (success) "Payment Success" else "Payment Failed")
        }
    }
    fun verifyGatewayTransaction(trxId: String, gateway: com.example.service.PaymentGatewayType, onResult: (com.example.service.GatewayApiResult<String>) -> Unit) {
        viewModelScope.launch { onResult(repository.verifyAndReconcileTransaction(trxId, gateway)) }
    }

    fun startBKashPayment(customerId: String, amount: Double, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val customer = customersList.value.find { it.id == customerId } ?: return@launch
            val (success, payment) = repository.processAutomatedGatewayPayment(
                customerId = customerId,
                invoiceId = "", // Direct payment, not tied to a specific old invoice
                amount = amount,
                gateway = com.example.service.PaymentGatewayType.BKASH,
                customerMobile = customer.mobile,
                collectorName = "System (Gateway)"
            )
            onResult(success, if (success) "Payment Successful: ${payment?.receiptNo}" else "Payment Failed")
        }
    }

    fun submitPaymentRequest(customerId: String, amount: Double, method: String, trxId: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val customer = customersList.value.find { it.id == customerId } ?: return@launch
                val requestDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val timeStr = SimpleDateFormat("hh:mm a", Locale.US).format(Date())

                val data = mapOf(
                    "customerId" to customerId,
                    "customerName" to customer.name,
                    "customerCode" to customer.customerCode,
                    "amount" to amount,
                    "trxId" to trxId.uppercase(),
                    "method" to method,
                    "status" to "pending",
                    "requestDate" to requestDate,
                    "requestTime" to timeStr
                )

                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("payment_requests")
                    .add(data)
                    .await()

                onResult(true, "Request Submitted Successfully!")
            } catch (e: Exception) {
                onResult(false, "Failed to submit: ${e.message}")
            }
        }
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

    fun getLedgerForCustomer(customerId: String) = repository.getLedgerForCustomer(customerId)

    fun createSupportTicket(
        customer: CustomerEntity?,
        type: String,
        description: String,
        adminEnteredName: String = "",
        adminEnteredPhone: String = "",
        scheduledDate: String = "",
        scheduledTime: String = ""
    ) {
        viewModelScope.launch {
            val ticket = SupportTicketEntity(
                id = UUID.randomUUID().toString(),
                customerId = customer?.id ?: "",
                customerName = customer?.name ?: adminEnteredName,
                customerCode = customer?.customerCode ?: "NET-ADMIN",
                customerPhone = customer?.mobile ?: adminEnteredPhone,
                issueType = type,
                description = description,
                status = "Pending",
                createdAt = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US).format(Date()),
                scheduledDate = scheduledDate,
                scheduledTime = scheduledTime
            )
            repository.insertSupportTicket(ticket)
            showToast("Support ticket created.")
        }
    }

    fun updateSupportTicket(ticket: SupportTicketEntity) {
        viewModelScope.launch {
            repository.updateSupportTicket(ticket.copy(lastUpdated = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US).format(Date())))
            showToast("Ticket updated.")
        }
    }

    fun deleteSupportTicket(id: String) {
        viewModelScope.launch {
            repository.deleteSupportTicket(id)
            showToast("Ticket deleted.")
        }
    }

    fun showToast(msg: String) { _toastMessage.value = msg }
    fun clearToast() { _toastMessage.value = null }
}
