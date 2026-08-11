package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val passwordHash: String,
    val name: String,
    val mobile: String,
    val role: String, // "Super Admin", "Manager", "Billing Operator", "Support Staff"
    val active: Boolean = true
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerCode: String, // e.g. NET-1001
    val name: String,
    val mobile: String, // 01XXXXXXXXX
    val altMobile: String = "",
    val email: String = "",
    val nidNumber: String = "", // NID Number
    val dob: String = "", // YYYY-MM-DD
    val address: String,
    val zone: String, // e.g. Uttara Zone, Dhanmondi, Mirpur
    val subZone: String = "",
    val houseOwnerName: String = "", // House owner info
    val emergencyContact: String = "", // Emergency contact phone
    val networkBox: String = "", // e.g. TJ-04 / Splitter 1:8
    val onuMacSerial: String = "", // ONU Serial / Model
    val fiberCoreNo: String = "", // Fiber core color / number
    val packageName: String,
    val packageId: Long = 0,
    val monthlyBill: Double, // in ৳ BDT
    val discount: Double = 0.0, // Monthly discount ৳
    val connectionFee: Double = 0.0, // One-time connection fee ৳
    val billingType: String = "Prepaid", // "Prepaid", "Postpaid"
    val username: String = "",
    val password: String = "",
    val pppoeUsername: String = "",
    val pppoePassword: String = "",
    val ipAddress: String = "",
    val macAddress: String = "",
    val connectionType: String = "PPPoE", // "PPPoE", "Static IP", "Hotspot"
    val joinDate: String, // YYYY-MM-DD
    val joinDayOfMonth: Int = 1, // 1 to 31 (To enforce 20th day rule)
    val expireDate: String = "",
    val expireTime: String = "23:59",
    val status: String = "Active", // "Active", "Inactive", "Suspended"
    val referenceName: String = "",
    val referenceMobile: String = "",
    val currentDue: Double = 0.0, // in ৳ BDT
    val advanceBalance: Double = 0.0, // in ৳ BDT (excess payments carried over)
    val notes: String = ""
)

@Entity(tableName = "packages")
data class PackageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, // e.g. 10 Mbps Home
    val speed: String, // e.g. 10 Mbps
    val monthlyPrice: Double, // in ৳ BDT
    val description: String = "",
    val activeUserCount: Int = 0
)

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNo: String, // e.g. INV-2026-08101
    val customerId: Long,
    val customerCode: String,
    val customerName: String,
    val packageName: String,
    val billingMonthYear: String, // e.g. "August 2026"
    val billAmount: Double, // ৳ (Current bill)
    val previousDue: Double = 0.0, // ৳ (Previous unpaid bills)
    val carryForwardDue: Double = 0.0, // ৳ (Carried forward balance)
    val lateFee: Double = 0.0,
    val totalPayable: Double,
    val paidAmount: Double = 0.0,
    val dueAmount: Double = 0.0,
    val status: String = "Unpaid", // "Paid", "Partially Paid", "Due", "Overdue", "Cancelled"
    val generatedDate: String,
    val dueDate: String,
    val paymentDate: String = "",
    val is20thDayOverrideChoice: String = "Standard" // "CurrentMonth", "NextMonth", "Standard"
)

@Entity(tableName = "payment_allocations")
data class PaymentAllocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val paymentId: Long, // Reference to payment_collections id
    val invoiceId: Long, // Reference to invoices id
    val customerId: Long,
    val allocatedAmount: Double, // ৳ allocated to this specific bill via FIFO
    val remainingBillDueAfter: Double, // ৳ remaining due on this bill after allocation
    val paymentDate: String,
    val paymentMethod: String,
    val collector: String,
    val remarks: String = ""
)

@Entity(tableName = "payment_collections")
data class PaymentCollectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val receiptNo: String, // e.g. REC-99201
    val invoiceId: Long = 0,
    val customerId: Long,
    val customerName: String,
    val customerCode: String,
    val amount: Double, // ৳ BDT
    val paymentMethod: String, // "bKash", "Nagad", "Rocket", "Cash", "Bank Transfer"
    val transactionId: String = "", // e.g. BK89230X1
    val paymentDate: String,
    val collectorName: String,
    val remarks: String = ""
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String, // "Bandwidth Cost", "Staff Salary", "Electricity Bill", "Equipment Purchase", "Office Rent", "Maintenance", "Transport", "Other Expense"
    val amount: Double, // ৳ BDT
    val expenseDate: String,
    val expenseBy: String,
    val notes: String = ""
)

@Entity(tableName = "staff")
data class StaffEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val mobile: String,
    val address: String,
    val role: String, // "Super Admin", "Manager", "Billing Operator", "Support Staff", "Lineman"
    val salary: Double, // ৳ BDT
    val joiningDate: String,
    val active: Boolean = true
)

@Entity(tableName = "staff_salaries")
data class StaffSalaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val staffId: Long,
    val staffName: String,
    val amount: Double,
    val salaryMonth: String, // "August 2026"
    val paymentDate: String,
    val paymentMethod: String = "Cash",
    val remarks: String = ""
)

@Entity(tableName = "mikrotik_routers")
data class MikroTikRouterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routerName: String, // e.g. Main Core Router - Uttara
    val ipAddress: String, // e.g. 192.168.88.1
    val apiPort: Int = 8728,
    val username: String = "admin",
    val password: String = "",
    val isConnected: Boolean = true,
    val activePppoeCount: Int = 412,
    val totalRxMbps: Double = 850.5,
    val totalTxMbps: Double = 420.2,
    val zone: String = "Uttara Zone"
)

@Entity(tableName = "isp_settings")
data class ISPSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val ispName: String = "NetBill Broadband ISP",
    val address: String = "House 14, Road 7, Sector 4, Uttara, Dhaka-1230",
    val mobileNumber: String = "01711000000",
    val supportNumber: String = "01911000000",
    val logoUrl: String = "",
    val currencySymbol: String = "৳",
    val defaultLanguage: String = "bn", // "bn" or "en"
    val autoInvoiceDayOfMonth: Int = 1,
    val lateFeeAmount: Double = 50.0
)

@Entity(tableName = "ledger_entries")
data class LedgerEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val date: String, // YYYY-MM-DD
    val time: String = "10:00 AM",
    val type: String, // "Monthly Bill", "Payment", "Discount", "Advance", "Adjustment", "Previous Due", "Carry Forward Due", "Manual Charge", "Refund", "Waiver"
    val referenceNo: String = "",
    val description: String = "",
    val amount: Double,
    val isDebit: Boolean, // true if increases customer due (+), false if decreases customer due (-)
    val runningBalance: Double = 0.0,
    val monthYear: String = "", // e.g. "August 2026"
    val paymentMethod: String = "", // Cash, bKash, Nagad, Rocket, Bank
    val collector: String = ""
)

@Entity(tableName = "sms_logs")
data class SmsLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long = 0,
    val customerCode: String,
    val customerName: String,
    val mobile: String,
    val notificationType: String, // "Billing Alert", "Support Update", "Payment Receipt", "20th Day Reminder"
    val message: String,
    val sentTimestamp: String,
    val status: String, // "Delivered", "Failed", "Pending", "Sent"
    val deliveryReport: String = "",
    val gatewayProvider: String = "Greenweb Gateway"
)
