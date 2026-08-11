package com.example.data

import com.example.data.entity.CustomerEntity
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.ISPSettingsEntity
import com.example.data.entity.InvoiceEntity
import com.example.data.entity.LedgerEntryEntity
import com.example.data.entity.MikroTikRouterEntity
import com.example.data.entity.PackageEntity
import com.example.data.entity.PaymentAllocationEntity
import com.example.data.entity.PaymentCollectionEntity
import com.example.data.entity.SmsLogEntity
import com.example.data.entity.SmsTemplateEntity
import com.example.data.entity.StaffEntity
import com.example.data.entity.StaffSalaryEntity
import com.example.data.entity.UserEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ISPRepository(private val db: AppDatabase) {

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

    fun getLedgerForCustomer(customerId: Long): Flow<List<LedgerEntryEntity>> {
        return ledgerDao.getLedgerForCustomer(customerId)
    }

    fun getAllocationsForCustomer(customerId: Long): Flow<List<PaymentAllocationEntity>> {
        return paymentAllocationDao.getAllocationsForCustomer(customerId)
    }

    fun getAllocationsForInvoice(invoiceId: Long): Flow<List<PaymentAllocationEntity>> {
        return paymentAllocationDao.getAllocationsForInvoice(invoiceId)
    }

    suspend fun insertLedgerEntry(entry: LedgerEntryEntity): Long {
        return ledgerDao.insertLedgerEntry(entry)
    }

    suspend fun seedDatabaseIfEmpty() {
        seedSmsTemplatesIfEmpty()
        // Check if users exist
        val existingUser = userDao.getUserByUsernameOrMobile("admin")
        if (existingUser == null) {
            // Seed Admin User
            userDao.insertUser(
                UserEntity(
                    username = "admin",
                    passwordHash = "admin123",
                    name = "M. A. Rahman (Super Admin)",
                    mobile = "01711000000",
                    role = "Super Admin"
                )
            )
            userDao.insertUser(
                UserEntity(
                    username = "operator",
                    passwordHash = "123456",
                    name = "Sumon Hasan (Billing Operator)",
                    mobile = "01811223344",
                    role = "Billing Operator"
                )
            )

            // Seed ISP Settings
            settingsDao.saveSettings(
                ISPSettingsEntity(
                    id = 1,
                    ispName = "NetBill Broadband ISP",
                    address = "House 14, Road 7, Sector 4, Uttara, Dhaka-1230",
                    mobileNumber = "01711000000",
                    supportNumber = "01911000000",
                    currencySymbol = "৳",
                    defaultLanguage = "bn"
                )
            )

            // Seed Internet Packages
            val pkg1Id = packageDao.insertPackage(PackageEntity(name = "10 Mbps Starter", speed = "10 Mbps", monthlyPrice = 500.0, description = "Home starter connection", activeUserCount = 120))
            val pkg2Id = packageDao.insertPackage(PackageEntity(name = "20 Mbps Super", speed = "20 Mbps", monthlyPrice = 800.0, description = "Most popular home package", activeUserCount = 245))
            val pkg3Id = packageDao.insertPackage(PackageEntity(name = "50 Mbps Turbo", speed = "50 Mbps", monthlyPrice = 1500.0, description = "Gamers & heavy streaming", activeUserCount = 89))
            packageDao.insertPackage(PackageEntity(name = "100 Mbps Corporate", speed = "100 Mbps", monthlyPrice = 3000.0, description = "Dedicated IP corporate fiber", activeUserCount = 14))

            // Seed Sample Customers
            val currentDate = getCurrentDateString()
            val cust1 = CustomerEntity(
                customerCode = "NET-1001",
                name = "Rahim Uddin",
                mobile = "01712345678",
                altMobile = "01812345678",
                email = "rahim.u@gmail.com",
                nidNumber = "19922691234567891",
                dob = "1992-05-14",
                address = "House 12, Road 4, Sector 4, Uttara",
                zone = "Uttara Zone",
                subZone = "Sector 4",
                houseOwnerName = "Haji Abdul Latif (01700112233)",
                emergencyContact = "01812345678 (Brother)",
                networkBox = "TJ-Sector4-Box1",
                onuMacSerial = "HG8120C-44A1B2",
                fiberCoreNo = "Core-2 (Blue)",
                packageName = "20 Mbps Super",
                packageId = pkg2Id,
                monthlyBill = 800.0,
                discount = 50.0,
                connectionFee = 1500.0,
                billingType = "Prepaid",
                username = "rahim_uttara",
                password = "p123456password",
                pppoeUsername = "rahim_uttara",
                pppoePassword = "p123456password",
                ipAddress = "10.10.14.55",
                macAddress = "E4:8D:8C:11:22:33",
                connectionType = "PPPoE",
                joinDate = "2026-08-01",
                joinDayOfMonth = 1,
                expireDate = "2026-08-10",
                expireTime = "11:59 PM",
                status = "Active",
                referenceName = "Kabir Hossain",
                referenceMobile = "01912345678",
                currentDue = 800.0,
                notes = "Line expiring soon"
            )
            val cust1Id = customerDao.insertCustomer(cust1)

            // Customer joined after 20th day
            val cust2 = CustomerEntity(
                customerCode = "NET-1002",
                name = "Tanvir Ahmed",
                mobile = "01898765432",
                altMobile = "01798765432",
                email = "tanvir.mirpur@yahoo.com",
                nidNumber = "19882699876543210",
                dob = "1988-11-20",
                address = "Block C, Road 10, Mirpur 10, Dhaka",
                zone = "Mirpur Zone",
                subZone = "Mirpur 10",
                houseOwnerName = "Engineer Motaleb (01911998877)",
                emergencyContact = "01798765432 (Wife)",
                networkBox = "Splitter 1:8 Box-B",
                onuMacSerial = "V2801SG-88C3D4",
                fiberCoreNo = "Core-4 (Green)",
                packageName = "20 Mbps Super",
                packageId = pkg2Id,
                monthlyBill = 800.0,
                discount = 0.0,
                connectionFee = 1000.0,
                billingType = "Prepaid",
                username = "tanvir_mirpur",
                password = "p987654password",
                pppoeUsername = "tanvir_mirpur",
                pppoePassword = "p987654password",
                ipAddress = "10.10.20.12",
                macAddress = "F2:9A:12:44:55:66",
                connectionType = "PPPoE",
                joinDate = "2026-08-22",
                joinDayOfMonth = 22, // > 20th Day Rule
                expireDate = "2026-09-22",
                expireTime = "08:00 PM",
                status = "Active",
                referenceName = "Jamil Chowdhury",
                referenceMobile = "01555667788",
                currentDue = 800.0,
                notes = "Joined after 20th day of month"
            )
            val cust2Id = customerDao.insertCustomer(cust2)

            val cust3 = CustomerEntity(
                customerCode = "NET-1003",
                name = "Kamrul Islam",
                mobile = "01911223344",
                address = "House 85, Road 27, Dhanmondi, Dhaka",
                zone = "Dhanmondi Zone",
                subZone = "Road 27",
                networkBox = "TJ-Dhanmondi-Box4",
                packageName = "50 Mbps Turbo",
                packageId = pkg3Id,
                monthlyBill = 1500.0,
                username = "kamrul_dhanmondi",
                password = "p554433password",
                pppoeUsername = "kamrul_dhanmondi",
                pppoePassword = "p554433password",
                ipAddress = "10.10.30.8",
                macAddress = "88:2D:11:99:88:77",
                connectionType = "PPPoE",
                joinDate = "2026-07-15",
                joinDayOfMonth = 15,
                expireDate = "2026-08-15",
                expireTime = "06:00 PM",
                status = "Active",
                currentDue = 1500.0,
                notes = "Pending monthly payment"
            )
            val cust3Id = customerDao.insertCustomer(cust3)

            // Seed SMS Delivery Tracker Logs
            smsLogDao.insertSmsLog(
                SmsLogEntity(
                    customerId = cust1Id,
                    customerCode = "NET-1001",
                    customerName = "Rahim Uddin",
                    mobile = "01711223344",
                    notificationType = "Payment Receipt",
                    message = "ধন্যবাদ Rahim Uddin! আপনার ৳800.0 টাকা ইন্টারনেট বিল পরিশোধ সফল হয়েছে। Receipt #: REC-2026-8801",
                    sentTimestamp = "2026-08-11 09:30 AM",
                    status = "Delivered",
                    deliveryReport = "Delivered to handset (Gateway Msg ID #884920)"
                )
            )
            smsLogDao.insertSmsLog(
                SmsLogEntity(
                    customerId = cust2Id,
                    customerCode = "NET-1002",
                    customerName = "Tanvir Ahmed",
                    mobile = "01811998877",
                    notificationType = "Billing Alert",
                    message = "প্রিয় Tanvir Ahmed, আপনার NetBill ইন্টারনেট প্যাকেজ 20 Mbps Super এর মাসিক বিল ৳800 টাকা পরিশোধের শেষ সময় ২৫ আগস্ট।",
                    sentTimestamp = "2026-08-11 08:15 AM",
                    status = "Delivered",
                    deliveryReport = "Delivered to handset (Gateway Msg ID #884921)"
                )
            )
            smsLogDao.insertSmsLog(
                SmsLogEntity(
                    customerId = cust3Id,
                    customerCode = "NET-1003",
                    customerName = "Kamrul Islam",
                    mobile = "01911223344",
                    notificationType = "20th Day Reminder",
                    message = "জরুরী নোটিশ: প্রিয় Kamrul Islam, আপনার ইন্টারনেট বিল বকেয়া থাকায় সংযোগ স্থগিত এড়াতে আজই বিল পরিশোধ করুন।",
                    sentTimestamp = "2026-08-10 05:40 PM",
                    status = "Delivered",
                    deliveryReport = "Delivered to handset (Gateway Msg ID #884922)"
                )
            )
            smsLogDao.insertSmsLog(
                SmsLogEntity(
                    customerId = cust2Id,
                    customerCode = "NET-1002",
                    customerName = "Tanvir Ahmed",
                    mobile = "01811998877",
                    notificationType = "Support Update",
                    message = "নেটওয়ার্ক আপডেট: উত্তরা জোনে অপটিক্যাল ফাইবার রক্ষণাবেক্ষণ কাজ সম্পন্ন হয়েছে। আপনার ইন্টারনেট সংযোগ সক্রিয় আছে।",
                    sentTimestamp = "2026-08-10 02:10 PM",
                    status = "Delivered",
                    deliveryReport = "Delivered to handset (Gateway Msg ID #884923)"
                )
            )
            smsLogDao.insertSmsLog(
                SmsLogEntity(
                    customerId = 99L,
                    customerCode = "NET-1088",
                    customerName = "Jahid Hossain",
                    mobile = "01500000000",
                    notificationType = "Billing Alert",
                    message = "প্রিয় Jahid Hossain, আপনার বকেয়া বিল ৳1200 পরিশোধ করার জন্য অনুরোধ করা হচ্ছে।",
                    sentTimestamp = "2026-08-10 11:20 AM",
                    status = "Failed",
                    deliveryReport = "Failed: Mobile network switched off or out of coverage (Err #302)"
                )
            )
            smsLogDao.insertSmsLog(
                SmsLogEntity(
                    customerId = 100L,
                    customerCode = "NET-1090",
                    customerName = "Farhana Yeasmin",
                    mobile = "01688112233",
                    notificationType = "Support Update",
                    message = "সহযোগিতা টিকিট #TK-402: ধানমন্ডি পপ রাউটার রিবুট সম্পন্ন হয়েছে। যেকোনো সমস্যায় 01911000000 নম্বরে কল করুন।",
                    sentTimestamp = "2026-08-09 07:05 PM",
                    status = "Delivered",
                    deliveryReport = "Delivered to handset (Gateway Msg ID #884925)"
                )
            )
            smsLogDao.insertSmsLog(
                SmsLogEntity(
                    customerId = 101L,
                    customerCode = "NET-1095",
                    customerName = "Shohel Rana",
                    mobile = "01300112233",
                    notificationType = "Billing Alert",
                    message = "প্রিয় Shohel Rana, আপনার সংযোগ সচল রাখতে ৳500 টাকা বিল জমা দিন।",
                    sentTimestamp = "2026-08-09 10:00 AM",
                    status = "Failed",
                    deliveryReport = "Failed: Invalid MNO response / Gateway Timeout (Err #504)"
                )
            )

            // Seed MikroTik Routers
            mikrotikDao.insertRouter(
                MikroTikRouterEntity(
                    routerName = "Uttara Core Router (CCR2004)",
                    ipAddress = "192.168.88.1",
                    apiPort = 8728,
                    username = "admin",
                    password = "mikrotik_pass",
                    isConnected = true,
                    activePppoeCount = 312,
                    totalRxMbps = 750.4,
                    totalTxMbps = 380.2,
                    zone = "Uttara Zone"
                )
            )
            mikrotikDao.insertRouter(
                MikroTikRouterEntity(
                    routerName = "Mirpur POP Router (RB3011)",
                    ipAddress = "10.10.1.1",
                    apiPort = 8728,
                    username = "admin",
                    password = "mikrotik_pass",
                    isConnected = true,
                    activePppoeCount = 197,
                    totalRxMbps = 410.8,
                    totalTxMbps = 210.5,
                    zone = "Mirpur Zone"
                )
            )

            // Seed Invoices
            val inv1 = InvoiceEntity(
                invoiceNo = "INV-2026-08001",
                customerId = cust1Id,
                customerCode = "NET-1001",
                customerName = "Rahim Uddin",
                packageName = "20 Mbps Super",
                billingMonthYear = "August 2026",
                billAmount = 800.0,
                previousDue = 0.0,
                totalPayable = 800.0,
                paidAmount = 800.0,
                dueAmount = 0.0,
                status = "Paid",
                generatedDate = "2026-08-01",
                dueDate = "2026-08-10"
            )
            val inv1Id = invoiceDao.insertInvoice(inv1)

            val inv2 = InvoiceEntity(
                invoiceNo = "INV-2026-08002",
                customerId = cust3Id,
                customerCode = "NET-1003",
                customerName = "Kamrul Islam",
                packageName = "50 Mbps Turbo",
                billingMonthYear = "August 2026",
                billAmount = 1500.0,
                previousDue = 0.0,
                totalPayable = 1500.0,
                paidAmount = 0.0,
                dueAmount = 1500.0,
                status = "Unpaid",
                generatedDate = "2026-08-01",
                dueDate = "2026-08-10"
            )
            invoiceDao.insertInvoice(inv2)

            // Seed Payments (Cash, bKash, Nagad, Rocket, Bank Transfer)
            paymentDao.insertPayment(
                PaymentCollectionEntity(
                    receiptNo = "REC-2026-1001",
                    invoiceId = inv1Id,
                    customerId = cust1Id,
                    customerName = "Rahim Uddin",
                    customerCode = "NET-1001",
                    amount = 8500.0,
                    paymentMethod = "bKash",
                    transactionId = "BK89230X1",
                    paymentDate = currentDate,
                    collectorName = "bKash Auto Gateway",
                    remarks = "Monthly bill paid online via bKash"
                )
            )
            paymentDao.insertPayment(
                PaymentCollectionEntity(
                    receiptNo = "REC-2026-1002",
                    invoiceId = 0,
                    customerId = cust2Id,
                    customerName = "Tanvir Ahmed",
                    customerCode = "NET-1002",
                    amount = 12000.0,
                    paymentMethod = "Cash",
                    transactionId = "CASH-8812",
                    paymentDate = currentDate,
                    collectorName = "Sumon Hasan",
                    remarks = "Monthly bill collected in cash by billing staff"
                )
            )
            paymentDao.insertPayment(
                PaymentCollectionEntity(
                    receiptNo = "REC-2026-1003",
                    invoiceId = 0,
                    customerId = cust3Id,
                    customerName = "Kamrul Islam",
                    customerCode = "NET-1003",
                    amount = 3200.0,
                    paymentMethod = "Nagad",
                    transactionId = "NG77210A3",
                    paymentDate = currentDate,
                    collectorName = "Nagad Auto Merchant",
                    remarks = "Online bill payment via Nagad App"
                )
            )
            paymentDao.insertPayment(
                PaymentCollectionEntity(
                    receiptNo = "REC-2026-1004",
                    invoiceId = 0,
                    customerId = cust1Id,
                    customerName = "Rahim Uddin",
                    customerCode = "NET-1001",
                    amount = 1000.0,
                    paymentMethod = "Bank Transfer",
                    transactionId = "TRX-EBL-9011",
                    paymentDate = currentDate,
                    collectorName = "EBL Bank Online",
                    remarks = "Direct bank deposit for corporate IP"
                )
            )

            // Seed Ledger Entries
            // Customer 1: Rahim Uddin
            ledgerDao.insertLedgerEntry(LedgerEntryEntity(customerId = cust1Id, date = "2026-06-01", time = "09:00 AM", type = "Previous Due", referenceNo = "INIT-101", description = "Initial Previous Due Balance", amount = 0.0, isDebit = true, runningBalance = 0.0, monthYear = "June 2026"))
            ledgerDao.insertLedgerEntry(LedgerEntryEntity(customerId = cust1Id, date = "2026-06-01", time = "10:00 AM", type = "Monthly Bill", referenceNo = "INV-2026-06001", description = "June 2026 20 Mbps Super Package Bill", amount = 800.0, isDebit = true, runningBalance = 800.0, monthYear = "June 2026"))
            ledgerDao.insertLedgerEntry(LedgerEntryEntity(customerId = cust1Id, date = "2026-06-05", time = "11:30 AM", type = "Payment", referenceNo = "REC-2026-0601", description = "bKash Online Collection", amount = 800.0, isDebit = false, runningBalance = 0.0, monthYear = "June 2026", paymentMethod = "bKash", collector = "Online bKash Gateway"))
            ledgerDao.insertLedgerEntry(LedgerEntryEntity(customerId = cust1Id, date = "2026-07-01", time = "10:00 AM", type = "Monthly Bill", referenceNo = "INV-2026-07001", description = "July 2026 20 Mbps Super Package Bill", amount = 800.0, isDebit = true, runningBalance = 800.0, monthYear = "July 2026"))
            ledgerDao.insertLedgerEntry(LedgerEntryEntity(customerId = cust1Id, date = "2026-07-04", time = "02:15 PM", type = "Payment", referenceNo = "REC-2026-0701", description = "bKash Online Collection", amount = 800.0, isDebit = false, runningBalance = 0.0, monthYear = "July 2026", paymentMethod = "bKash", collector = "Online bKash Gateway"))
            ledgerDao.insertLedgerEntry(LedgerEntryEntity(customerId = cust1Id, date = "2026-08-01", time = "10:00 AM", type = "Monthly Bill", referenceNo = "INV-2026-08001", description = "August 2026 20 Mbps Super Package Bill", amount = 800.0, isDebit = true, runningBalance = 800.0, monthYear = "August 2026"))
            ledgerDao.insertLedgerEntry(LedgerEntryEntity(customerId = cust1Id, date = currentDate, time = "11:00 AM", type = "Payment", referenceNo = "REC-2026-1001", description = "August Bill Paid Online", amount = 800.0, isDebit = false, runningBalance = 0.0, monthYear = "August 2026", paymentMethod = "bKash", collector = "bKash Auto Gateway"))

            // Customer 2: Tanvir Ahmed
            ledgerDao.insertLedgerEntry(LedgerEntryEntity(customerId = cust2Id, date = "2026-07-01", time = "10:00 AM", type = "Previous Due", referenceNo = "BAL-002", description = "Previous Month Unpaid Bill", amount = 300.0, isDebit = true, runningBalance = 300.0, monthYear = "July 2026"))
            ledgerDao.insertLedgerEntry(LedgerEntryEntity(customerId = cust2Id, date = "2026-08-01", time = "09:00 AM", type = "Carry Forward Due", referenceNo = "CF-2026-08", description = "Carried forward unpaid balance from July", amount = 300.0, isDebit = true, runningBalance = 300.0, monthYear = "August 2026"))
            ledgerDao.insertLedgerEntry(LedgerEntryEntity(customerId = cust2Id, date = "2026-08-01", time = "10:00 AM", type = "Monthly Bill", referenceNo = "INV-2026-08003", description = "August 2026 20 Mbps Super Package Bill", amount = 800.0, isDebit = true, runningBalance = 1100.0, monthYear = "August 2026"))
            ledgerDao.insertLedgerEntry(LedgerEntryEntity(customerId = cust2Id, date = "2026-08-10", time = "03:20 PM", type = "Discount", referenceNo = "DISC-101", description = "Special Festival Discount Approved", amount = 100.0, isDebit = false, runningBalance = 1000.0, monthYear = "August 2026", collector = "Super Admin"))
            ledgerDao.insertLedgerEntry(LedgerEntryEntity(customerId = cust2Id, date = "2026-08-15", time = "04:00 PM", type = "Payment", referenceNo = "REC-2026-1002", description = "Partial Cash Payment", amount = 200.0, isDebit = false, runningBalance = 800.0, monthYear = "August 2026", paymentMethod = "Cash", collector = "Sumon Hasan"))

            // Customer 3: Kamrul Islam
            ledgerDao.insertLedgerEntry(LedgerEntryEntity(customerId = cust3Id, date = "2026-07-01", time = "10:00 AM", type = "Monthly Bill", referenceNo = "INV-2026-07002", description = "July 2026 50 Mbps Turbo Bill", amount = 1500.0, isDebit = true, runningBalance = 1500.0, monthYear = "July 2026"))
            ledgerDao.insertLedgerEntry(LedgerEntryEntity(customerId = cust3Id, date = "2026-07-10", time = "11:45 AM", type = "Payment", referenceNo = "REC-2026-0702", description = "Nagad Collection", amount = 1500.0, isDebit = false, runningBalance = 0.0, monthYear = "July 2026", paymentMethod = "Nagad", collector = "Sumon Hasan"))
            ledgerDao.insertLedgerEntry(LedgerEntryEntity(customerId = cust3Id, date = "2026-08-01", time = "10:00 AM", type = "Monthly Bill", referenceNo = "INV-2026-08002", description = "August 2026 50 Mbps Turbo Bill", amount = 1500.0, isDebit = true, runningBalance = 1500.0, monthYear = "August 2026"))


            // Seed Expenses
            expenseDao.insertExpense(
                ExpenseEntity(
                    title = "Monthly Fiber Bandwidth IIG Bill",
                    category = "Bandwidth Cost",
                    amount = 45000.0,
                    expenseDate = currentDate,
                    expenseBy = "M. A. Rahman",
                    notes = "Summit Communications IIG Fiber 1 Gbps"
                )
            )
            expenseDao.insertExpense(
                ExpenseEntity(
                    title = "Uttara Sector 4 Office Rent",
                    category = "Office Rent",
                    amount = 15000.0,
                    expenseDate = currentDate,
                    expenseBy = "M. A. Rahman",
                    notes = "Monthly rent for ISP core office"
                )
            )

            // Seed Staff
            staffDao.insertStaff(
                StaffEntity(
                    name = "Sumon Hasan",
                    mobile = "01811223344",
                    address = "Sector 4, Uttara",
                    role = "Billing Operator",
                    salary = 18000.0,
                    joiningDate = "2024-01-10"
                )
            )
            staffDao.insertStaff(
                StaffEntity(
                    name = "Mahfuz Alam",
                    mobile = "01922334455",
                    address = "Mirpur 10",
                    role = "Support Staff (Lineman)",
                    salary = 15000.0,
                    joiningDate = "2024-03-15"
                )
            )
        }
    }

    suspend fun generateAutoMonthlyInvoices(
        monthYear: String,
        overrideChoicesFor20thDay: Map<Long, String> = emptyMap()
    ): Int {
        val customersList = customerDao.getAllCustomers().firstOrNull() ?: emptyList()
        var generatedCount = 0
        val currentDate = getCurrentDateString()

        for (cust in customersList) {
            if (cust.status != "Active") continue

            // Check 20th Day Rule
            if (cust.joinDayOfMonth > 20) {
                val choice = overrideChoicesFor20thDay[cust.id] ?: "NextMonth"
                if (choice == "NextMonth") {
                    // Skip bill generation for current month as per rule
                    continue
                }
            }

            val billAmt = cust.monthlyBill

            // Calculate Previous Due (Unpaid balance carried forward from previous billing cycles)
            val existingInvoices = invoiceDao.getInvoicesForCustomer(cust.id).firstOrNull() ?: emptyList()
            val prevDue = existingInvoices
                .filter { it.status != "Paid" && it.status != "Cancelled" }
                .sumOf { it.dueAmount }

            val totalGrossPayable = billAmt + prevDue
            var paidAmt = 0.0
            var remainingAdvance = cust.advanceBalance

            // Automatically deduct any stored advance payment balance
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

            val invNo = "INV-${System.currentTimeMillis().toString().takeLast(8)}"

            val invoice = InvoiceEntity(
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
            invoiceDao.insertInvoice(invoice)

            // Update customer current due & remaining advance balance
            val updatedTotalDue = dueAmt
            customerDao.updateCustomer(
                cust.copy(
                    currentDue = updatedTotalDue,
                    advanceBalance = remainingAdvance
                )
            )

            // Auto log in Ledger
            val timeStr = SimpleDateFormat("hh:mm a", Locale.US).format(Date())

            if (prevDue > 0) {
                ledgerDao.insertLedgerEntry(
                    LedgerEntryEntity(
                        customerId = cust.id,
                        date = currentDate,
                        time = timeStr,
                        type = "Carry Forward Due",
                        referenceNo = "CF-${System.currentTimeMillis().toString().takeLast(6)}",
                        description = "Carried forward unpaid balance from previous billing cycle",
                        amount = prevDue,
                        isDebit = true,
                        runningBalance = prevDue,
                        monthYear = monthYear
                    )
                )
            }

            ledgerDao.insertLedgerEntry(
                LedgerEntryEntity(
                    customerId = cust.id,
                    date = currentDate,
                    time = timeStr,
                    type = "Monthly Bill",
                    referenceNo = invNo,
                    description = "$monthYear ${cust.packageName} Package Bill",
                    amount = billAmt,
                    isDebit = true,
                    runningBalance = totalGrossPayable,
                    monthYear = monthYear
                )
            )
            generatedCount++
        }
        return generatedCount
    }

    suspend fun recordPayment(
        customerId: Long,
        amount: Double,
        paymentMethod: String,
        transactionId: String,
        collectorName: String,
        remarks: String
    ): PaymentCollectionEntity? {
        val customer = customerDao.getCustomerById(customerId) ?: return null
        val currentDate = getCurrentDateString()
        val receiptNo = "REC-${System.currentTimeMillis().toString().takeLast(6)}"

        val payment = PaymentCollectionEntity(
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
        val paymentId = paymentDao.insertPayment(payment)

        // =========================================================
        // FIFO (First In, First Out) PAYMENT ALLOCATION ALGORITHM
        // =========================================================
        var remainingPayment = amount
        // Fetch all unpaid/partially paid invoices ordered by oldest first (id ASC)
        val unpaidInvoices = invoiceDao.getUnpaidInvoicesForCustomerAsc(customerId)

        for (inv in unpaidInvoices) {
            if (remainingPayment <= 0) break
            val invDue = inv.totalPayable - inv.paidAmount
            if (invDue <= 0) continue

            val allocateAmount = Math.min(remainingPayment, invDue)
            val newPaid = inv.paidAmount + allocateAmount
            val newDue = (inv.totalPayable - newPaid).coerceAtLeast(0.0)
            val newStatus = if (newDue <= 0.0) "Paid" else "Partially Paid"

            // Update affected invoice
            invoiceDao.updateInvoice(
                inv.copy(
                    paidAmount = newPaid,
                    dueAmount = newDue,
                    status = newStatus,
                    paymentDate = currentDate
                )
            )

            // Save Payment Allocation History
            paymentAllocationDao.insertAllocation(
                PaymentAllocationEntity(
                    paymentId = paymentId,
                    invoiceId = inv.id,
                    customerId = customerId,
                    allocatedAmount = allocateAmount,
                    remainingBillDueAfter = newDue,
                    paymentDate = currentDate,
                    paymentMethod = paymentMethod,
                    collector = collectorName,
                    remarks = "FIFO allocated to ${inv.billingMonthYear} bill (${inv.invoiceNo})"
                )
            )

            remainingPayment -= allocateAmount
        }

        // Check for Advance Payment / Over Payment
        var updatedAdvance = customer.advanceBalance
        if (remainingPayment > 0) {
            updatedAdvance += remainingPayment
            val timeStr = SimpleDateFormat("hh:mm a", Locale.US).format(Date())
            val currentMonth = SimpleDateFormat("MMMM yyyy", Locale.US).format(Date())
            ledgerDao.insertLedgerEntry(
                LedgerEntryEntity(
                    customerId = customer.id,
                    date = currentDate,
                    time = timeStr,
                    type = "Advance",
                    referenceNo = receiptNo,
                    description = "Advance payment stored for future billing cycle",
                    amount = remainingPayment,
                    isDebit = false,
                    runningBalance = 0.0,
                    monthYear = currentMonth,
                    paymentMethod = paymentMethod,
                    collector = collectorName
                )
            )
        }

        // Recalculate customer total current due from remaining unpaid invoices
        val allInvoicesAfter = invoiceDao.getInvoicesForCustomer(customerId).firstOrNull() ?: emptyList()
        val totalCurrentDue = allInvoicesAfter
            .filter { it.status != "Paid" && it.status != "Cancelled" }
            .sumOf { it.dueAmount }

        // Update customer entity
        customerDao.updateCustomer(
            customer.copy(
                currentDue = totalCurrentDue,
                advanceBalance = updatedAdvance
            )
        )

        // Log Payment in Customer Ledger
        val timeStr = SimpleDateFormat("hh:mm a", Locale.US).format(Date())
        val currentMonth = SimpleDateFormat("MMMM yyyy", Locale.US).format(Date())
        ledgerDao.insertLedgerEntry(
            LedgerEntryEntity(
                customerId = customer.id,
                date = currentDate,
                time = timeStr,
                type = "Payment",
                referenceNo = receiptNo,
                description = "Payment received via $paymentMethod${if (transactionId.isNotEmpty()) " (TxID: $transactionId)" else ""}",
                amount = amount,
                isDebit = false,
                runningBalance = totalCurrentDue,
                monthYear = currentMonth,
                paymentMethod = paymentMethod,
                collector = collectorName
            )
        )

        return payment.copy(id = paymentId)
    }

    suspend fun payInvoice(
        invoice: InvoiceEntity,
        amount: Double,
        paymentMethod: String,
        transactionId: String,
        collectorName: String,
        remarks: String
    ): PaymentCollectionEntity? {
        val payment = recordPayment(
            customerId = invoice.customerId,
            amount = amount,
            paymentMethod = paymentMethod,
            transactionId = transactionId,
            collectorName = collectorName,
            remarks = "Paid Invoice ${invoice.invoiceNo}: $remarks"
        )
        if (payment != null) {
            val updatedPaid = invoice.paidAmount + amount
            val updatedDue = (invoice.totalPayable - updatedPaid).coerceAtLeast(0.0)
            val newStatus = if (updatedDue <= 0.0) "Paid" else "Partial"
            invoiceDao.updateInvoice(
                invoice.copy(
                    paidAmount = updatedPaid,
                    dueAmount = updatedDue,
                    status = newStatus
                )
            )
        }
        return payment
    }

    val paymentGatewayService = com.example.service.PaymentGatewayService()

    suspend fun processAutomatedGatewayPayment(
        customerId: Long,
        invoiceId: Long,
        amount: Double,
        gateway: com.example.service.PaymentGatewayType,
        customerMobile: String,
        collectorName: String
    ): Pair<Boolean, PaymentCollectionEntity?> {
        val invoice = if (invoiceId > 0) invoiceDao.getInvoiceById(invoiceId) else null
        val invoiceNo = invoice?.invoiceNo ?: "INV-${System.currentTimeMillis().toString().takeLast(6)}"

        val result = paymentGatewayService.executeAutomatedCollection(
            gateway = gateway,
            amount = amount,
            customerMobile = customerMobile,
            invoiceNo = invoiceNo
        )

        return if (result is com.example.service.GatewayApiResult.Success) {
            val res = result.data
            val gatewayName = if (gateway == com.example.service.PaymentGatewayType.BKASH) "bKash" else "Nagad"

            val payment = if (invoice != null) {
                payInvoice(
                    invoice = invoice,
                    amount = amount,
                    paymentMethod = gatewayName,
                    transactionId = res.transactionId,
                    collectorName = collectorName,
                    remarks = "Auto Gateway Settlement (${res.message})"
                )
            } else {
                recordPayment(
                    customerId = customerId,
                    amount = amount,
                    paymentMethod = gatewayName,
                    transactionId = res.transactionId,
                    collectorName = collectorName,
                    remarks = "Automated $gatewayName Direct Gateway Payment"
                )
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
        if (cleanTrx.isBlank()) {
            return com.example.service.GatewayApiResult.Error("Please enter a valid bKash or Nagad Transaction ID.")
        }

        return when (gateway) {
            com.example.service.PaymentGatewayType.BKASH -> {
                val res = paymentGatewayService.bkashQueryPayment(cleanTrx)
                if (res is com.example.service.GatewayApiResult.Success) {
                    com.example.service.GatewayApiResult.Success("bKash Verified: Trx ${res.data.trxID} | Status: ${res.data.transactionStatus} | Amount: ৳${res.data.amount} | Payer: ${res.data.customerMsisdn}")
                } else {
                    com.example.service.GatewayApiResult.Error("bKash Verification Failed for Trx $cleanTrx")
                }
            }
            com.example.service.PaymentGatewayType.NAGAD -> {
                val res = paymentGatewayService.nagadVerifyPayment(cleanTrx)
                if (res is com.example.service.GatewayApiResult.Success) {
                    com.example.service.GatewayApiResult.Success("Nagad Verified: Trx ${res.data.trxId} | Status: ${res.data.status} | Amount: ৳${res.data.amount} | Ref: ${res.data.paymentRefId}")
                } else {
                    com.example.service.GatewayApiResult.Error("Nagad Verification Failed for Trx $cleanTrx")
                }
            }
        }
    }

    suspend fun seedSmsTemplatesIfEmpty() {
        val existing = smsTemplateDao.getAllTemplates().firstOrNull()
        if (existing.isNullOrEmpty()) {
            val dateStr = getCurrentDateString()
            val defaultTemplates = listOf(
                SmsTemplateEntity(
                    title = "Monthly Bill Due Reminder",
                    category = "Billing Alert",
                    messageContent = "প্রিয় {NAME} ({CUSTOMER_CODE}), আপনার {BILL_MONTH} মাসের ইন্টারনেট বিল ৳{AMOUNT} টাকা বকেয়া রয়েছে। পরিশোধের শেষ তারিখ: {DUE_DATE}। bKash/Nagad এ পরিশোধ করুন। - NetBill ISP",
                    targetAudience = "Due Customers",
                    isDefault = true,
                    isActive = true,
                    lastUpdated = dateStr
                ),
                SmsTemplateEntity(
                    title = "Payment Received Receipt",
                    category = "Payment Receipt",
                    messageContent = "ধন্যবাদ {NAME}! আপনার ৳{AMOUNT} টাকা ইন্টারনেট বিল পরিশোধ সফল হয়েছে। রশিদ নং: {RECEIPT_NO}। NetBill Broadband ISP",
                    targetAudience = "Paid Customers",
                    isDefault = true,
                    isActive = true,
                    lastUpdated = dateStr
                ),
                SmsTemplateEntity(
                    title = "Scheduled Fiber Maintenance",
                    category = "Service Downtime",
                    messageContent = "জরুরী বিজ্ঞপ্তি: {ZONE} এলাকায় নেটওয়ার্ক সংস্কার কাজের জন্য {DATE} তারিখ {START_TIME} হতে {END_TIME} পর্যন্ত ইন্টারনেট সেবা সাময়িক বন্ধ থাকবে। সাময়িক অসুবিধার জন্য অত্যন্ত দুঃখিত। - NetBill ISP",
                    targetAudience = "Zone Customers",
                    isDefault = true,
                    isActive = true,
                    lastUpdated = dateStr
                ),
                SmsTemplateEntity(
                    title = "Emergency Fiber Cut Outage Alert",
                    category = "Network Outage",
                    messageContent = "জরুরী ঘোষণা: {ZONE} এলাকায় ফাইবার অপটিক ক্যাবল কাটা পরায় সংযোগ ব্যাহত হয়েছে। আমাদের টেকনিক্যাল টিম কাজ করছে। সম্ভাব্য সচল সময়: {ESTIMATED_TIME}। সহায়তায়: {SUPPORT_PHONE}",
                    targetAudience = "Affected Zone",
                    isDefault = false,
                    isActive = true,
                    lastUpdated = dateStr
                ),
                SmsTemplateEntity(
                    title = "Service Restored Confirmation",
                    category = "Service Downtime",
                    messageContent = "সংবাদ: {ZONE} এলাকায় ফাইবার মেরামত কাজ সফলভাবে সম্পন্ন হয়েছে এবং ইন্টারনেট সংযোগ সম্পূর্ণ সচল হয়েছে। আপনার ধৈর্যের জন্য ধন্যবাদ! - NetBill ISP",
                    targetAudience = "Affected Zone",
                    isDefault = false,
                    isActive = true,
                    lastUpdated = dateStr
                ),
                SmsTemplateEntity(
                    title = "20th Day Line Suspension Warning",
                    category = "Billing Alert",
                    messageContent = "সতর্কবার্তা: প্রিয় {NAME}, ২০ তারিখে আপনার {BILL_MONTH} মাসের বকেয়া ৳{AMOUNT} টাকা না মেটালে লাইন স্বয়ংক্রিয় বন্ধ হবে। সংযোগ সচল রাখতে দ্রুত পরিশোধ করুন।",
                    targetAudience = "Unpaid Customers",
                    isDefault = false,
                    isActive = true,
                    lastUpdated = dateStr
                )
            )
            defaultTemplates.forEach { smsTemplateDao.insertTemplate(it) }
        }
    }

    suspend fun insertSmsTemplate(template: SmsTemplateEntity): Long {
        return smsTemplateDao.insertTemplate(template)
    }

    suspend fun updateSmsTemplate(template: SmsTemplateEntity) {
        smsTemplateDao.updateTemplate(template)
    }

    suspend fun deleteSmsTemplate(id: Long) {
        smsTemplateDao.deleteTemplateById(id)
    }

    private fun getCurrentDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }
}
