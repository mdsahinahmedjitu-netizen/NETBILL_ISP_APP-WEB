package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.AuthManager
import com.example.data.AppDatabase
import com.example.data.ISPRepository
import com.example.data.entity.*
import com.example.localization.AppLanguage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    private val prefs = application.getSharedPreferences("netbill_isp_prefs", android.content.Context.MODE_PRIVATE)

    private val _currentLanguage = MutableStateFlow(
        if (prefs.getString("app_lang", "BANGLA") == "ENGLISH") AppLanguage.ENGLISH else AppLanguage.BANGLA
    )
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("is_dark_mode", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _localUser = MutableStateFlow<UserEntity?>(null)
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
        viewModelScope.launch {
            repository.seedDatabaseIfEmpty()
        }
        
        // Start sync when a user is logged in
        currentUser.onEach { user ->
            if (user != null) {
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
                    showToast("Logged in as ${user.name} (Local Only - No Cloud Auth)")
                }
                return@launch
            }

            // 3. Both failed
            val errorMsg = firebaseResult.exceptionOrNull()?.message ?: "Invalid credentials"
            _loginUiState.value = LoginUiState.Error(errorMsg)
            showToast("Login Failed: $errorMsg")
        }
    }

    fun logout() {
        authManager.signOut()
        _localUser.value = null
        _loginUiState.value = LoginUiState.Idle
        showToast("Logged Out")
    }

    fun addOrUpdateCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            repository.insertCustomer(customer)
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
            val nextStatus = if (customer.status == "Active") "Suspended" else "Active"
            repository.updateCustomer(customer.copy(status = nextStatus))
            showToast("Customer is now $nextStatus")
        }
    }

    fun recordPayment(customerId: String, amount: Double, method: String, trxId: String, remarks: String) {
        viewModelScope.launch {
            val collector = currentUser.value?.name ?: "Admin"
            val payment = repository.recordPayment(customerId, amount, method, trxId, collector, remarks)
            if (payment != null) {
                _selectedReceipt.value = payment
                showToast("Payment recorded.")
            }
        }
    }

    fun collectPayment(customerId: String, amount: Double, method: String, trxId: String, remarks: String) =
        recordPayment(customerId, amount, method, trxId, remarks)

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

    fun addExpense(title: String, category: String, amount: Double, notes: String) {
        viewModelScope.launch {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
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

    fun addStaff(name: String, mobile: String, role: String, salary: Double) {
        viewModelScope.launch {
            repository.insertStaff(StaffEntity(id = UUID.randomUUID().toString(), name = name, mobile = mobile, role = role, salary = salary, joiningDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())))
            showToast("Staff added.")
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

    fun addCustomLedgerEntry(customerId: String, type: String, amount: Double, isDebit: Boolean, description: String, referenceNo: String = "", method: String = "") {
        viewModelScope.launch {
            val entry = LedgerEntryEntity(id = UUID.randomUUID().toString(), customerId = customerId, type = type, amount = amount, isDebit = isDebit, description = description, referenceNo = referenceNo, paymentMethod = method, date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
            repository.insertLedgerEntry(entry)
            showToast("Ledger entry added.")
        }
    }

    fun saveISPSettings(ispName: String, address: String, mobile: String, support: String) {
        viewModelScope.launch {
            val current = settingsState.value ?: ISPSettingsEntity()
            repository.saveSettings(current.copy(ispName = ispName, address = address, mobileNumber = mobile, supportNumber = support))
            showToast("Settings updated.")
        }
    }

    fun clearSmsLogs() { viewModelScope.launch { repository.clearAllSmsLogs() } }
    fun resendAllFailedSms() { /* Implement if needed */ }
    fun resendFailedSms(log: SmsLogEntity) { viewModelScope.launch { repository.updateSmsLog(log) } }
    fun sendMass20thDaySmsReminders() { /* Implement if needed */ }
    fun suspendAll20thDayOverdueCustomers() { /* Implement if needed */ }
    fun sendSingleSmsNotification(id: String, code: String, name: String, mobile: String, type: String, msg: String) {
        viewModelScope.launch { repository.insertSmsLog(SmsLogEntity(id = UUID.randomUUID().toString(), customerId = id, customerCode = code, customerName = name, mobile = mobile, notificationType = type, message = msg, sentTimestamp = Date().toString(), status = "Sent")) }
    }
    fun sendSupportUpdateSms(id: String?, zone: String, msg: String) { /* Implement */ }
    fun dismiss20thDayAlert(id: String) { _dismissedExpiryAlerts.value += id }
    fun updateCustomerExpiry(id: String, date: String, time: String) {
        viewModelScope.launch {
            val cust = customersList.value.find { it.id == id } ?: return@launch
            repository.updateCustomer(cust.copy(expireDate = date, expireTime = time))
        }
    }

    fun addSmsTemplate(template: SmsTemplateEntity) { viewModelScope.launch { repository.insertSmsTemplate(template) } }
    fun updateSmsTemplate(template: SmsTemplateEntity) { viewModelScope.launch { repository.updateSmsTemplate(template) } }
    fun deleteSmsTemplate(id: String) { viewModelScope.launch { repository.deleteSmsTemplate(id) } }
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

    fun showToast(msg: String) { _toastMessage.value = msg }
    fun clearToast() { _toastMessage.value = null }
}
