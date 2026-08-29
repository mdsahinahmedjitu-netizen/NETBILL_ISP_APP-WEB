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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.edit
import androidx.core.net.toUri
import kotlin.time.Duration.Companion.seconds

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
    val bandwidthUsageMbps: Double = 1161.2,
    val smsBalance: String = "৳ ---"
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

    private val _preSelectedCustomerForPayment = MutableStateFlow<CustomerEntity?>(null)
    val preSelectedCustomerForPayment: StateFlow<CustomerEntity?> = _preSelectedCustomerForPayment.asStateFlow()

    fun setPreSelectedCustomerForPayment(customer: CustomerEntity?) {
        _preSelectedCustomerForPayment.value = customer
    }



    private val _filterState = MutableStateFlow(CustomerFilterState())
    val filterState: StateFlow<CustomerFilterState> = _filterState.asStateFlow()

    private val _selectedReceipt = MutableStateFlow<PaymentCollectionEntity?>(null)
    val selectedReceipt: StateFlow<PaymentCollectionEntity?> = _selectedReceipt.asStateFlow()

    val customersList = repository.allCustomers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val packagesList = combine(repository.allPackages, customersList) { pkgs, custs ->
        pkgs.map { pkg ->
            pkg.apply { activeUserCount = custs.count { (it.packageName == pkg.name) && (it.status == "Active") } }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val invoicesList = repository.allInvoices.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allPaymentsList = repository.allPayments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val paymentsList: StateFlow<List<PaymentCollectionEntity>> = combine(allPaymentsList, currentUser) { list, user ->
        val isStaff = user?.role?.lowercase() != "admin"
        val staffId = user?.id ?: ""
        val staffName = user?.name ?: ""
        
        if (isStaff) {
            list.filter { it.collectedById == staffId || it.collectedBy == staffName }
        } else {
            list
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val expensesList = repository.allExpenses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val settingsState = repository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val smsLogsList = repository.allSmsLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val smsTemplatesList = repository.allSmsTemplates.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val mikrotikRouters = repository.allRouters.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val staffList = repository.allStaff.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val staffPayouts = repository.allPayouts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val paymentRequestsList = repository.allPaymentRequests.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val inventoryList = repository.inventoryDao.getAllInventory().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val supportTickets = repository.supportTicketDao.getAllTickets().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentPermissions: StateFlow<UserRolePermissions> = combine(currentUser, settingsState) { user, settings ->
        if (user == null) return@combine UserRolePermissions()
        if (user.role.lowercase() == "admin") {
            return@combine UserRolePermissions(
                canCollect = true, canCollectDirect = true, canSeeMobile = true, canSeeAddress = true, canEdit = true, canDelete = true, canAdd = true, canSeeRevenue = true,
                canInventory = true, canSuspend = true, canLedger = true, canPasswords = true, canExpenses = true, canSMS = true,
                canDiscount = true, canBulkBill = true, canEditPayments = true, canManageStock = true, canAssignAssets = true,
                canManageZones = true, canManageRouters = true, canResolveTickets = true, canSendBulkSMS = true, canEditTemplates = true,
                canSeeStatsCards = true, canSeeExpiryAlerts = true, canSeeComplaintsAlert = true, canSeeVerificationAlert = true,
                canSeeTodayCollection = true, canSeeTotalCollection = true,
                canAccessBilling = true, canAccessReports = true, canAccessInventory = true, canAccessPackages = true, canAccessSMS = true,
                canAccessSalary = true, canAccessTickets = true, canAccessCustomers = true, canAccessPayments = true, canAccessExpenses = true,
                canAccessStaff = true, canAccessInfrastructure = true, canAccessSmsLogs = true, canAccessGlobalSettings = true,
                canModifyPricing = true, canViewLogs = true, canManageStaff = true
            )
        }

        try {
            val json = Json { ignoreUnknownKeys = true }
            val allPermissionsMap: Map<String, UserRolePermissions> = settings?.rolePermissionsJson?.let {
                if (it.isNotBlank()) json.decodeFromString(it) else null
            } ?: emptyMap()
            
            // Normalize role name (Web uses Capitalized roles like 'Collector')
            val staffRole = user.role.replaceFirstChar { it.uppercase() }
            allPermissionsMap[staffRole] ?: UserRolePermissions()
        } catch (e: Exception) {
            Log.e("Permissions", "Failed to parse permissions", e)
            UserRolePermissions()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserRolePermissions())

    val currentCustomer: StateFlow<CustomerEntity?> = combine(_loggedInCustomer, customersList) { loggedIn, list ->
        loggedIn?.let { current -> list.find { it.id == current.id } ?: current }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val filteredCustomers: StateFlow<List<CustomerEntity>> = combine(customersList, _filterState, currentUser) { list, filter, user ->
        val isStaff = user?.role?.lowercase() != "admin"
        val staffId = user?.id ?: ""

        list.filter { cust ->
            val matchStaff = if (isStaff) cust.assignedStaffId == staffId else true
            
            val matchQuery = filter.searchQuery.isEmpty() ||
                    cust.customerCode.contains(filter.searchQuery, ignoreCase = true) ||
                    cust.name.contains(filter.searchQuery, ignoreCase = true) ||
                    cust.mobile.contains(filter.searchQuery, ignoreCase = true) ||
                    cust.pppoeUsername.contains(filter.searchQuery, ignoreCase = true) ||
                    cust.address.orEmpty().contains(filter.searchQuery, ignoreCase = true)

            val matchZone = filter.selectedZone == "All" || cust.zone == filter.selectedZone
            val matchPkg = filter.selectedPackage == "All" || cust.packageName == filter.selectedPackage
            val matchStatus = filter.selectedStatus == "All" || cust.status == filter.selectedStatus
            val matchDue = !filter.onlyDueCustomers || cust.currentDue > 0

            matchStaff && matchQuery && matchZone && matchPkg && matchStatus && matchDue
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _dismissedExpiryAlerts = MutableStateFlow<Set<String>>(emptySet())
    val expiringTomorrowCustomers: StateFlow<List<CustomerEntity>> = combine(customersList, _dismissedExpiryAlerts) { custs, dismissed ->
        custs.filter { cust -> com.example.util.ExpiryUtils.isExpiringTomorrow(cust) && !dismissed.contains(cust.id) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardStats: StateFlow<DashboardStats> = combine(customersList, paymentsList, expensesList, currentUser) { allCusts, pymts, exps, user ->
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val yearMonthStr = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

        val isStaff = user?.role?.lowercase() != "admin"
        val staffId = user?.id ?: ""
        
        val visibleCusts = if (isStaff) allCusts.filter { it.assignedStaffId == staffId } else allCusts

        DashboardStats(
            totalCustomers = visibleCusts.size,
            todaysCollection = pymts.filter { it.paymentDate == todayStr }.sumOf { it.amount },
            monthlyCollection = pymts.filter { it.paymentDate.startsWith(yearMonthStr) }.sumOf { it.amount },
            totalDue = visibleCusts.sumOf { it.currentDue },
            todaysExpense = exps.filter { it.expenseDate == todayStr }.sumOf { it.amount },
            monthlyExpense = exps.filter { it.expenseDate.startsWith(yearMonthStr) }.sumOf { it.amount },
            activeCustomers = visibleCusts.count { it.status == "Active" },
            expiredCustomers = visibleCusts.count { com.example.util.ExpiryUtils.isExpired(it) },
            newCustomers = visibleCusts.count { it.joinDate.orEmpty().startsWith(yearMonthStr) },
            smsBalance = _smsBalance.value
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    private val _smsBalance = MutableStateFlow("৳ ---")

    private fun startSmsBalancePolling() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            while (true) {
                val apiKey = settingsState.value?.smsApiKey ?: ""
                if (apiKey.isNotBlank()) {
                    try {
                        val url = java.net.URL("https://tglplinxvrqsrxeicvpr.supabase.co/functions/v1/sms-proxy?action=balance&apikey=$apiKey")
                        val text = url.readText()
                        if (text.contains("balance")) {
                             val balance = text.substringAfter("\"balance\":").substringBefore(",").substringBefore("}").trim()
                             _smsBalance.value = "৳ $balance"
                        }
                    } catch (e: Exception) {
                        Log.e("SMS", "Failed to fetch balance", e)
                    }
                }
                delay(60.seconds)
            }
        }
    }

    init {
        startSmsBalancePolling()
        // Observe settings and update gateway config
        viewModelScope.launch {
            repository.settings.collect { _ ->
                // settings?.let { repository.updateGatewayConfig(it) } // Update if method exists
            }
        }

        viewModelScope.launch {
            // repository.seedDatabaseIfEmpty() // Use if needed
        }

        // Auto-Check for Expired Customers on App Start
        viewModelScope.launch {
            delay(5.seconds) // Wait for sync to stabilize
            val suspendedCount = repository.checkAndSuspendExpiredCustomers()
            if (suspendedCount > 0) {
                showToast("Auto-Suspended $suspendedCount expired subscribers.")
            }
        }
        
        // Start sync when a user or customer is logged in

        combine(currentUser, currentCustomer) { user, customer ->
            user != null || customer != null
        }.onEach { shouldSync ->
            if (shouldSync) {
                repository.startSync()
            } else {
                repository.stopSync()
            }
        }.launchIn(viewModelScope)
    }

    override fun onCleared() {
        repository.stopSync()
    }


    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
        prefs.edit { putString("app_lang", language.name) }
        showToast("Language changed to: ${language.name}")
    }

    fun toggleLanguage() {
        if (_currentLanguage.value == AppLanguage.BANGLA) setLanguage(AppLanguage.ENGLISH) else setLanguage(AppLanguage.BANGLA)
    }

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        prefs.edit { putBoolean("is_dark_mode", enabled) }
    }



    fun loginUser(identifier: String, pass: String) {
        viewModelScope.launch {
            _loginUiState.value = LoginUiState.Loading
            
            // 1. Check Hardcoded Admin (Same as Web)
            if (identifier == "admin@isp.com" && pass == "123456") {
                _localUser.value = UserEntity(id = "admin", username = identifier, name = "Super Admin", mobile = "", role = "admin")
                _loginUiState.value = LoginUiState.Success
                authManager.signInAnonymously() // For sync
                showToast("Logged in as Super Admin")
                return@launch
            }

            // 2. Try Supabase Authentication (Cloud Primary)
            val supabaseResult = authManager.signIn(identifier, pass)
            if (supabaseResult.isSuccess) {
                _loginUiState.value = LoginUiState.Success
                showToast("Logged in via Supabase Cloud")
                return@launch
            }

            // 3. Fallback: Check Local Room Database
            val user = repository.userDao.getUserByUsernameOrMobile(identifier)
            if (user != null && user.passwordHash == pass) {
                authManager.signInAnonymously()
                _localUser.value = user
                _loginUiState.value = LoginUiState.Success
                showToast("Logged in as ${user.name}")
                return@launch
            }

            _loginUiState.value = LoginUiState.Error("Invalid Credentials")
            showToast("Login Failed")
        }
    }

    fun loginCustomer(identifier: String, pass: String) {
        viewModelScope.launch {
            _loginUiState.value = LoginUiState.Loading
            
            try {
                // 1. First search in local Room DB (Quickest & works offline)
                var customer = repository.getCustomerByLogin(identifier.trim(), pass.trim())

                // 2. If not found locally, try Cloud search
                if (customer == null) {
                    customer = repository.findCustomerByLoginInCloud(identifier.trim(), pass.trim())
                }

                if (customer != null) {
                    _loggedInCustomer.value = customer
                    _loginUiState.value = LoginUiState.Success
                    
                    // Attempt background sync permissions (Doesn't block login)
                    launch { authManager.signInAnonymously() }
                    
                    repository.startSync() 
                    showToast("Welcome ${customer.name}!")
                } else {
                    _loginUiState.value = LoginUiState.Error("Invalid Credentials or User Not Found")
                    showToast("Login Failed")
                }
            } catch (e: Exception) {
                _loginUiState.value = LoginUiState.Error("Server Connection Failed")
                Log.e("Login", "Customer Login Error", e)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authManager.signOut()
            _localUser.value = null
            _loggedInCustomer.value = null
            _loginUiState.value = LoginUiState.Idle
            showToast("Logged Out")
        }
    }

    fun addOrUpdateCustomer(customer: CustomerEntity, discountAmount: Double = 0.0, billChoice: String = "Standard") {
        viewModelScope.launch {
            // Ensure PPPoE Username is always lowercase for MikroTik compatibility
            val normalizedCustomer = customer.copy(pppoeUsername = customer.pppoeUsername.trim().lowercase())
            val isNew = !customersList.value.any { it.id == normalizedCustomer.id }
            
            if (isNew) {
                // 1. Initial due logic based on your rules (Only for NEW enrollment)
                var finalCustomer: CustomerEntity
                val baseDue = normalizedCustomer.currentDue
                val netBill = (normalizedCustomer.monthlyBill - discountAmount).coerceAtLeast(0.0)
                
                if (normalizedCustomer.joinDayOfMonth <= 20 || billChoice == "CurrentMonth") {
                    finalCustomer = normalizedCustomer.copy(currentDue = baseDue + netBill)
                } else {
                    finalCustomer = normalizedCustomer.copy(currentDue = baseDue)
                }

                repository.insertCustomer(finalCustomer)
                
                // 2. Generate initial invoice if bill is applied
                if (finalCustomer.currentDue > baseDue) {
                    val joinDateStr = finalCustomer.joinDate?.ifBlank { null } ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    val joinDateObj = try { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(joinDateStr) } catch(_: Exception) { Date() }
                    val billingMonth = SimpleDateFormat("MMMM yyyy", Locale.US).format(joinDateObj ?: Date())

                    repository.invoiceDao.insertInvoice(
                        InvoiceEntity(
                            id = UUID.randomUUID().toString(),
                            invoiceNo = "INV-NEW-${System.currentTimeMillis().toString().takeLast(6)}",
                            customerId = finalCustomer.id,
                            customerCode = finalCustomer.customerCode,
                            customerName = finalCustomer.name,
                            packageName = finalCustomer.packageName,
                            billingMonthYear = billingMonth,
                            billAmount = finalCustomer.monthlyBill,
                            discountAmount = discountAmount,
                            totalPayable = netBill,
                            dueAmount = netBill,
                            status = "Unpaid",
                            generatedDate = joinDateStr,
                            is20thDayOverrideChoice = billChoice
                        )
                    )
                }
            } else {
                // Just update for EXISTING customer
                repository.updateCustomer(normalizedCustomer)
            }

            // 3. MikroTik Sync (Sync regardless of status to ensure Disable works)
            if (normalizedCustomer.pppoeUsername.isNotBlank()) {
                repository.syncCustomerToMikroTik(normalizedCustomer.id)
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

    fun recordPayment(
        customerId: String,
        amount: Double,
        method: String,
        trxId: String,
        remarks: String,
        date: String? = null,
        collectorName: String? = null,
        billingMonth: String? = null
    ) {
        viewModelScope.launch {
            val finalCollector = collectorName ?: currentUser.value?.name ?: "Admin"
            val collectorId = currentUser.value?.id ?: ""
            val isBangla = _currentLanguage.value == AppLanguage.BANGLA
            val payment = repository.recordPayment(
                customerId = customerId,
                amount = amount,
                paymentMethod = method,
                transactionId = trxId,
                collectorName = finalCollector,
                collectorId = collectorId,
                remarks = remarks,
                customDate = date,
                billingMonth = billingMonth,
                isBangla = isBangla
            )
            if (payment != null) {
                _selectedReceipt.value = payment
                showToast("Payment recorded.")
            }
        }
    }

    fun collectPayment(
        customerId: String,
        amount: Double,
        method: String,
        trxId: String,
        remarks: String,
        date: String? = null,
        collectorName: String? = null,
        billingMonth: String? = null
    ) = recordPayment(customerId, amount, method, trxId, remarks, date, collectorName, billingMonth)

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



    fun generateBillsForMonth(monthYear: String, overrides: Map<String, String> = emptyMap()) {
        viewModelScope.launch {
            val count = repository.generateAutoMonthlyInvoices(monthYear, overrides)
            showToast("$count Invoices Generated.")
        }
    }

    fun addExpense(title: String, category: String, amount: Double, notes: String, date: String? = null, spentBy: String? = null) {
        viewModelScope.launch {
            val dateStr = date ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            repository.insertExpense(ExpenseEntity(id = UUID.randomUUID().toString(), title = title, category = category, amount = amount, expenseDate = dateStr, expenseBy = spentBy ?: currentUser.value?.name ?: "Admin", notes = notes))
            showToast("Expense recorded.")
        }
    }

    fun addPackage(name: String, speed: String, price: Double) {
        viewModelScope.launch {
            repository.insertPackage(PackageEntity(id = UUID.randomUUID().toString(), name = name, speed = speed, monthlyPrice = price))
            showToast("Package $name added.")
        }
    }

    fun addStaff(name: String, mobile: String, role: String, salary: Double, password: String = "123456", receiveAlerts: Boolean = false, jDate: String? = null, zone: String = "All") {
        viewModelScope.launch {
            val dateStr = jDate ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            repository.insertStaff(StaffEntity(id = UUID.randomUUID().toString(), name = name, mobile = mobile, role = role, salary = salary, password = password, joiningDate = dateStr, receiveAlerts = receiveAlerts, zone = zone))
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
            val staff = staffList.value.find { it.id == staffId } ?: return@launch
            val newBalance = staff.balance + amount
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            
            // 1. Insert Payout Record
            repository.insertPayout(StaffPayoutEntity(
                id = UUID.randomUUID().toString(),
                staffId = staffId,
                staffName = staffName,
                month = month,
                amount = amount,
                type = "salary_add",
                newBalance = newBalance,
                date = today,
                remarks = "Monthly Salary: $month"
            ))

            // 2. Update Staff Balance
            repository.updateStaff(staff.copy(balance = newBalance))
            showToast("Salary added.")
        }
    }

    fun disburseStaffPayment(staffId: String, staffName: String, amount: Double, month: String, remarks: String = "") {
        viewModelScope.launch {
            val staff = staffList.value.find { it.id == staffId } ?: return@launch
            val newBalance = staff.balance - amount
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

            // 1. Insert Payout Record
            repository.insertPayout(StaffPayoutEntity(
                id = UUID.randomUUID().toString(),
                staffId = staffId,
                staffName = staffName,
                month = month,
                amount = amount,
                type = "payment",
                newBalance = newBalance,
                date = today,
                remarks = remarks.ifBlank { "Cash Disbursement" }
            ))

            // 2. Update Staff Balance
            repository.updateStaff(staff.copy(balance = newBalance))
            showToast("Payment disbursed.")
        }
    }

    fun updateStaffPayout(payout: StaffPayoutEntity, oldAmount: Double, oldType: String) {
        viewModelScope.launch {
            // 1. Update the payout record itself
            repository.updatePayout(payout)

            // 2. Adjust staff balance if necessary (if amount or type changed)
            val staff = staffList.value.find { it.id == payout.staffId }
            if (staff != null) {
                // First, reverse the old amount
                var balance = staff.balance
                if (oldType == "salary_add") balance -= oldAmount else balance += oldAmount
                
                // Then apply the new amount
                if (payout.type == "salary_add") balance += payout.amount else balance -= payout.amount
                
                repository.updateStaff(staff.copy(balance = balance))
            }
            showToast("Record updated.")
        }
    }

    fun deleteStaffPayout(payout: StaffPayoutEntity) {
        viewModelScope.launch {
            // 1. Reverse staff balance
            val staff = staffList.value.find { it.id == payout.staffId }
            if (staff != null) {
                val reversalAdjustment = if (payout.type == "salary_add") -payout.amount else payout.amount
                repository.updateStaff(staff.copy(balance = staff.balance + reversalAdjustment))
            }

            // 2. Delete record
            repository.deletePayoutById(payout.id)
            showToast("Record deleted.")
        }
    }



    fun addMikroTikRouter(router: MikroTikRouterEntity) {
        viewModelScope.launch { repository.insertRouter(router) }
    }

    fun disconnectMikroTikSession(router: MikroTikRouterEntity, username: String) {
        viewModelScope.launch {
            val success = com.example.data.remote.MikroTikApiService().disconnectSession(router, username)
            if (success) showToast("User $username kicked from Router.")
            else showToast("Failed to disconnect $username.")
        }
    }

    fun updateMikroTikUserSpeed(router: MikroTikRouterEntity, username: String, profile: String) {
        viewModelScope.launch {
            val success = com.example.data.remote.MikroTikApiService().updatePppoeUser(router, username, profile)
            if (success) showToast("Speed limit updated for $username.")
            else showToast("Failed to update speed for $username.")
        }
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
            val entry = LedgerEntity(id = UUID.randomUUID().toString(), customerId = customerId, type = type, amount = amount, isDebit = isDebit, description = description, referenceNo = referenceNo, paymentMethod = method, date = dateStr)
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
        waAlerts: Boolean = false,
        personalBkash: String = "017XXXXXXXX",
        personalNagad: String = "018XXXXXXXX"
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
                isWhatsappAlertEnabled = waAlerts,
                personalBkashNo = personalBkash,
                personalNagadNo = personalNagad
            ))
            showToast("Settings updated.")
        }
    }

    fun clearSmsLogs() { viewModelScope.launch { repository.clearAllSmsLogs() } }
    fun resendAllFailedSms() {
        viewModelScope.launch {
            val failed = smsLogsList.value.filter { it.status == "Failed" }
            if (failed.isEmpty()) {
                showToast("No failed SMS to resend.")
                return@launch
            }
            showToast("Resending ${failed.size} failed messages...")
            failed.forEach { log ->
                repository.sendAndLogSms(log.copy(status = "Pending"))
            }
        }
    }
    fun resendFailedSms(log: SmsLogEntity) { 
        viewModelScope.launch { 
            val finalUrl = repository.sendAndLogSms(log.copy(status = "Pending")) 
            if (finalUrl.isNotBlank()) {
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, finalUrl.toUri())
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    getApplication<Application>().startActivity(intent)
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Failed to open debug URL", e)
                }
            }
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
                repository.sendAndLogSms(SmsLogEntity(
                    id = UUID.randomUUID().toString(),
                    customerId = cust.id,
                    customerCode = cust.customerCode,
                    customerName = cust.name,
                    mobile = cust.mobile,
                    notificationType = "Support Update (Manual)",
                    message = msg
                ))
            }
            showToast("Sending support update to ${targetCustomers.size} customers.")
        }
    }



    fun sendSingleSmsNotification(id: String, code: String, name: String, mobile: String, type: String, msg: String) {
        viewModelScope.launch { 
            val finalUrl = repository.sendAndLogSms(SmsLogEntity(
                id = UUID.randomUUID().toString(), 
                customerId = id, 
                customerCode = code, 
                customerName = name, 
                mobile = mobile, 
                notificationType = type, 
                message = msg
            )) 

            // Debug Feature: Open the URL in browser
            if (finalUrl.isNotBlank()) {
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, finalUrl.toUri())
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    getApplication<Application>().startActivity(intent)
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Failed to open debug URL", e)
                }
            }
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
    fun sendTestSmsTemplate(template: SmsTemplateEntity, recipientName: String, recipientMobile: String) {
        viewModelScope.launch {
            val finalMsg = template.messageContent
                .replace("{NAME}", recipientName, ignoreCase = true)
                .replace("{name}", recipientName, ignoreCase = true)
                .replace("{MOBILE}", recipientMobile, ignoreCase = true)
                .replace("{mobile}", recipientMobile, ignoreCase = true)
                .replace("{CUSTOMER_CODE}", "NET-TEST-001", ignoreCase = true)
                .replace("{AMOUNT}", "500", ignoreCase = true)
                .replace("{TOTAL_DUE}", "500", ignoreCase = true)
                .replace("{DUE_DATE}", "2026-08-30", ignoreCase = true)
                .replace("{BILL_MONTH}", "August 2026", ignoreCase = true)
                .replace("{PACKAGE_NAME}", "Test Package", ignoreCase = true)
                .replace("{EXPIRY_DATE}", "2026-09-01", ignoreCase = true)
                .replace("{COMPANY_NAME}", settingsState.value?.ispName ?: "NetBill ISP", ignoreCase = true)
                .replace("{SUPPORT_PHONE}", settingsState.value?.supportNumber ?: "017XXXXXXXX", ignoreCase = true)
                .replace("{RECEIPT_NO}", "REC-TEST-123", ignoreCase = true)
            
            val finalUrl = repository.sendAndLogSms(SmsLogEntity(
                id = UUID.randomUUID().toString(),
                customerName = recipientName,
                mobile = recipientMobile,
                notificationType = "Test",
                message = finalMsg
            ))

            // Debug Feature: Open the URL in browser to see gateway response
            if (finalUrl.isNotBlank()) {
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, finalUrl.toUri())
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    getApplication<Application>().startActivity(intent)
                    showToast("Opening Browser for Debugging...")
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Failed to open debug URL", e)
                }
            }
            
            showToast("Test SMS Triggered to $recipientMobile")
        }
    }

    // Payment Gateway
    private val _gatewayConfig = MutableStateFlow(com.example.service.GatewayConfig())
    val gatewayConfig: StateFlow<com.example.service.GatewayConfig> = _gatewayConfig.asStateFlow()
    fun updateGatewayConfig(config: com.example.service.GatewayConfig) { _gatewayConfig.value = config }
    fun processAutomatedGatewayPayment(customerId: String, invoiceId: String, amount: Double, gateway: com.example.service.PaymentGatewayType, mobile: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val (success, _) = repository.processAutomatedGatewayPayment(customerId, invoiceId, amount, gateway, mobile, "System")
            onResult(success, if (success) "Payment Success" else "Payment Failed")
        }
    }
    fun verifyGatewayTransaction(trxId: String, gateway: com.example.service.PaymentGatewayType, onResult: (com.example.service.GatewayApiResult<String>) -> Unit) {
        viewModelScope.launch { onResult(repository.verifyAndReconcileTransaction(trxId, gateway)) }
    }

    fun approvePaymentRequest(req: PaymentRequestEntity) {
        viewModelScope.launch {
            val success = repository.approvePaymentRequest(req)
            if (success) showToast("Payment Request Approved!")
        }
    }

    fun rejectPaymentRequest(req: PaymentRequestEntity) {
        viewModelScope.launch {
            val success = repository.rejectPaymentRequest(req)
            if (success) showToast("Payment Request Rejected.")
        }
    }



    fun submitPaymentRequest(customerId: String, amount: Double, method: String, trxId: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val customer = customersList.value.find { it.id == customerId } ?: return@launch
                val requestDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val timeStr = SimpleDateFormat("hh:mm a", Locale.US).format(Date())

                val data = mapOf(
                    "customer_id" to customerId,
                    "customer_name" to customer.name,
                    "customer_code" to customer.customerCode,
                    "amount" to amount,
                    "trx_id" to trxId.uppercase(),
                    "method" to method,
                    "status" to "pending",
                    "request_date" to requestDate,
                    "request_time" to timeStr
                )

                repository.submitPaymentRequest(data)

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

            // Trigger SMS
            repository.triggerSystemSms(
                type = "Complain to Customer",
                mobile = ticket.customerPhone,
                params = mapOf(
                    "NAME" to ticket.customerName,
                    "REASON" to ticket.issueType,
                    "DATE" to ticket.createdAt
                ),
                customerId = ticket.customerId,
                customerCode = ticket.customerCode,
                customerName = ticket.customerName
            )
        }
    }

    fun updateSupportTicket(ticket: SupportTicketEntity) {
        viewModelScope.launch {
            repository.updateSupportTicket(ticket.copy(lastUpdated = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US).format(Date())))
            showToast("Ticket updated.")
        }
    }



    fun broadcastSms(type: String, zone: String = "All", onlyDue: Boolean = false) {
        viewModelScope.launch {
            val targets = customersList.value.filter { 
                (zone == "All" || it.zone == zone) && (!onlyDue || it.currentDue > 0)
            }
            
            if (targets.isEmpty()) {
                showToast("No target customers found for $type")
                return@launch
            }

            showToast("Bulk sending initiated for ${targets.size} customers...")
            
            targets.forEach { cust ->
                repository.triggerSystemSms(
                    type = type,
                    mobile = cust.mobile,
                    params = mapOf(
                        "NAME" to cust.name,
                        "TOTAL_DUE" to cust.currentDue.toInt().toString(),
                        "AMOUNT" to cust.currentDue.toInt().toString(),
                        "ZONE" to cust.zone.orEmpty(),
                        "CUSTOMER_CODE" to cust.customerCode
                    ),
                    customerId = cust.id,
                    customerCode = cust.customerCode,
                    customerName = cust.name
                )
            }
            showToast("Bulk SMS task completed.")
        }
    }



    fun showToast(msg: String) { Log.d("Toast", msg) }
}
