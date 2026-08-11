package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val username: String = "",
    val passwordHash: String = "",
    val name: String = "",
    val mobile: String = "",
    val role: String = "", // "Super Admin", "Manager", "Billing Operator", "Support Staff"
    val active: Boolean = true,
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val customerCode: String = "", // e.g. NET-1001
    val name: String = "",
    val mobile: String = "", // 01XXXXXXXXX
    val altMobile: String = "",
    val email: String = "",
    val nidNumber: String = "", // NID Number
    val dob: String = "", // YYYY-MM-DD
    val address: String = "",
    val zone: String = "", // e.g. Uttara Zone, Dhanmondi, Mirpur
    val subZone: String = "",
    val houseOwnerName: String = "", // House owner info
    val emergencyContact: String = "", // Emergency contact phone
    val networkBox: String = "", // e.g. TJ-04 / Splitter 1:8
    val onuMacSerial: String = "", // ONU Serial / Model
    val fiberCoreNo: String = "", // Fiber core color / number
    val packageName: String = "",
    val packageId: String = "",
    val monthlyBill: Double = 0.0, // in ৳ BDT
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
    val joinDate: String = "", // YYYY-MM-DD
    val joinDayOfMonth: Int = 1, // 1 to 31 (To enforce 20th day rule)
    val expireDate: String = "",
    val expireTime: String = "23:59",
    val status: String = "Active", // "Active", "Inactive", "Suspended"
    val referenceName: String = "",
    val referenceMobile: String = "",
    val currentDue: Double = 0.0, // in ৳ BDT
    val advanceBalance: Double = 0.0, // in ৳ BDT (excess payments carried over)
    val notes: String = "",
)

@Entity(tableName = "packages")
data class PackageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "", // e.g. 10 Mbps Home
    val speed: String = "", // e.g. 10 Mbps
    val monthlyPrice: Double = 0.0, // in ৳ BDT
    val description: String = "",
    val activeUserCount: Int = 0,
)

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val invoiceNo: String = "", // e.g. INV-2026-08101
    val customerId: String = "",
    val customerCode: String = "",
    val customerName: String = "",
    val packageName: String = "",
    val billingMonthYear: String = "", // e.g. "August 2026"
    val billAmount: Double = 0.0, // ৳ (Current bill)
    val previousDue: Double = 0.0, // ৳ (Previous unpaid bills)
    val carryForwardDue: Double = 0.0, // ৳ (Carried forward balance)
    val lateFee: Double = 0.0,
    val totalPayable: Double = 0.0,
    val paidAmount: Double = 0.0,
    val dueAmount: Double = 0.0,
    val status: String = "Unpaid", // "Paid", "Partially Paid", "Due", "Overdue", "Cancelled"
    val generatedDate: String = "",
    val dueDate: String = "",
    val paymentDate: String = "",
    val is20thDayOverrideChoice: String = "Standard", // "CurrentMonth", "NextMonth", "Standard"
)

@Entity(tableName = "payment_allocations")
data class PaymentAllocationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val paymentId: String = "", // Reference to payment_collections id
    val invoiceId: String = "", // Reference to invoices id
    val customerId: String = "",
    val allocatedAmount: Double = 0.0, // ৳ allocated to this specific bill via FIFO
    val remainingBillDueAfter: Double = 0.0, // ৳ remaining due on this bill after allocation
    val paymentDate: String = "",
    val paymentMethod: String = "",
    val collector: String = "",
    val remarks: String = "",
)

@Entity(tableName = "payment_collections")
data class PaymentCollectionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val receiptNo: String = "", // e.g. REC-99201
    val invoiceId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerCode: String = "",
    val amount: Double = 0.0, // ৳ BDT
    val paymentMethod: String = "", // "bKash", "Nagad", "Rocket", "Cash", "Bank Transfer"
    val transactionId: String = "", // e.g. BK89230X1
    val paymentDate: String = "",
    val collectorName: String = "",
    val remarks: String = "",
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val category: String = "", // "Bandwidth Cost", "Staff Salary", "Electricity Bill", "Equipment Purchase", "Office Rent", "Maintenance", "Transport", "Other Expense"
    val amount: Double = 0.0, // ৳ BDT
    val expenseDate: String = "",
    val expenseBy: String = "",
    val notes: String = "",
)

@Entity(tableName = "staff")
data class StaffEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val mobile: String = "",
    val address: String = "",
    val role: String = "", // "Super Admin", "Manager", "Billing Operator", "Support Staff", "Lineman"
    val salary: Double = 0.0, // ৳ BDT
    val joiningDate: String = "",
    val active: Boolean = true,
)

@Entity(tableName = "staff_salaries")
data class StaffSalaryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val staffId: String = "",
    val staffName: String = "",
    val amount: Double = 0.0,
    val salaryMonth: String = "", // "August 2026"
    val paymentDate: String = "",
    val paymentMethod: String = "Cash",
    val remarks: String = "",
)

@Entity(tableName = "mikrotik_routers")
data class MikroTikRouterEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val routerName: String = "", // e.g. Main Core Router - Uttara
    val ipAddress: String = "", // e.g. 192.168.88.1
    val apiPort: Int = 8728,
    val username: String = "admin",
    val password: String = "",
    val isConnected: Boolean = true,
    val activePppoeCount: Int = 0,
    val totalRxMbps: Double = 0.0,
    val totalTxMbps: Double = 0.0,
    val zone: String = "",
)

@Entity(tableName = "isp_settings")
data class ISPSettingsEntity(
    @PrimaryKey val id: String = "1",
    val ispName: String = "NetBill Broadband ISP",
    val address: String = "House 14, Road 7, Sector 4, Uttara, Dhaka-1230",
    val mobileNumber: String = "01711000000",
    val supportNumber: String = "01911000000",
    val logoUrl: String = "",
    val currencySymbol: String = "৳",
    val defaultLanguage: String = "bn", // "bn" or "en"
    val autoInvoiceDayOfMonth: Int = 1,
    val lateFeeAmount: Double = 50.0,
)

@Entity(tableName = "ledger_entries")
data class LedgerEntryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val customerId: String = "",
    val date: String = "", // YYYY-MM-DD
    val time: String = "10:00 AM",
    val type: String = "", // "Monthly Bill", "Payment", "Discount", "Advance", "Adjustment", "Previous Due", "Carry Forward Due", "Manual Charge", "Refund", "Waiver"
    val referenceNo: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val isDebit: Boolean = true, // true if increases customer due (+), false if decreases customer due (-)
    val runningBalance: Double = 0.0,
    val monthYear: String = "", // e.g. "August 2026"
    val paymentMethod: String = "", // Cash, bKash, Nagad, Rocket, Bank
    val collector: String = "",
)

@Entity(tableName = "sms_logs")
data class SmsLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val customerId: String = "",
    val customerCode: String = "",
    val customerName: String = "",
    val mobile: String = "",
    val notificationType: String = "", // "Billing Alert", "Support Update", "Payment Receipt", "20th Day Reminder"
    val message: String = "",
    val sentTimestamp: String = "",
    val status: String = "", // "Delivered", "Failed", "Pending", "Sent"
    val deliveryReport: String = "",
    val gatewayProvider: String = "Greenweb Gateway"
)

@Entity(tableName = "sms_templates")
data class SmsTemplateEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String = "", // e.g. "Monthly Bill Due Alert", "Scheduled Fiber Downtime"
    val category: String = "", // "Billing Alert", "Service Downtime", "Payment Receipt", "Network Outage", "General Notice"
    val messageContent: String = "", // Template string with placeholders like {NAME}, {AMOUNT}, {ZONE}, {DUE_DATE}, etc.
    val targetAudience: String = "All Active Customers",
    val isDefault: Boolean = false,
    val isActive: Boolean = true,
    val lastUpdated: String = ""
)
