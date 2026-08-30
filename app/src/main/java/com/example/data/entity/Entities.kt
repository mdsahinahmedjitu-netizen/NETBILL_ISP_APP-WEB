package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Ignore
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.UUID

@Serializable
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val username: String = "",
    val name: String = "",
    val mobile: String = "",
    val passwordHash: String = "",
    val role: String = "operator", // admin, operator
    val balance: Double = 0.0,
    val status: String = "Active"
)

@Serializable
@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @SerialName("customer_code") val customerCode: String = "",
    val name: String = "",
    val mobile: String = "",
    @SerialName("alt_mobile") val altMobile: String? = "",
    val address: String? = "",
    val zone: String? = "",
    @SerialName("sub_zone") val subZone: String? = "",
    @SerialName("box_id") val boxId: String? = "",
    @SerialName("package_name") val packageName: String = "",
    @SerialName("package_id") val packageId: String? = null,
    @SerialName("monthly_bill") val monthlyBill: Double = 0.0,
    @SerialName("current_due") val currentDue: Double = 0.0,
    @SerialName("advance_balance") val advanceBalance: Double = 0.0,
    @SerialName("pppoe_username") val pppoeUsername: String = "",
    @SerialName("pppoe_password") val pppoePassword: String = "",
    @SerialName("onu_mac") val onuMac: String? = "",
    @SerialName("onu_serial") val onuSerial: String? = "",
    @SerialName("router_id") val routerId: String? = "",
    @SerialName("billing_type") val billingType: String? = "MONTHLY DATE TO DATE",
    @SerialName("payment_status") val paymentStatus: String? = "Unpaid",
    val status: String = "Active",
    @SerialName("subscription_type") val subscriptionType: String? = "Prepaid",
    @SerialName("join_date") val joinDate: String? = "",
    @SerialName("assigned_staff_id") val assignedStaffId: String? = "",
    @SerialName("expire_date") val expireDate: String? = "",
    @SerialName("expire_time") val expireTime: String? = "",
    @SerialName("request_date") val requestDate: String? = "",
    @SerialName("connection_type") val connectionType: String? = "",
    @SerialName("connection_fee") val connectionFee: Double = 0.0,
    @SerialName("promise_date") val promiseDate: String? = "",
    @SerialName("promise_note") val promiseNote: String? = "",
    val notes: String? = ""
) {
    @get:Ignore val joinDayOfMonth: Int
        get() {
            return try {
                if (joinDate.isNullOrBlank()) 1
                else joinDate.split("-")[2].toInt()
            } catch (_: Exception) { 1 }
        }
}

@Serializable
@Entity(tableName = "packages")
data class PackageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val speed: String = "",
    @SerialName("monthly_price") val monthlyPrice: Double = 0.0
) {
    @Ignore @Transient var activeUserCount: Int = 0
}

@Serializable
@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @SerialName("invoice_no") val invoiceNo: String = "",
    @SerialName("customer_id") val customerId: String = "",
    @SerialName("customer_code") val customerCode: String = "",
    @SerialName("customer_name") val customerName: String = "",
    @SerialName("package_name") val packageName: String = "",
    @SerialName("billing_month_year") val billingMonthYear: String = "",
    @SerialName("bill_amount") val billAmount: Double = 0.0,
    @SerialName("discount_amount") val discountAmount: Double = 0.0,
    @SerialName("previous_due") val previousDue: Double = 0.0,
    @SerialName("total_payable") val totalPayable: Double = 0.0,
    @SerialName("due_amount") val dueAmount: Double = 0.0,
    val status: String = "Unpaid", // Unpaid, Partial, Paid
    @SerialName("generated_date") val generatedDate: String = "",
    @SerialName("is_20th_day_override_choice") val is20thDayOverrideChoice: String = "Standard"
)

@Serializable
@Entity(tableName = "payment_allocations")
data class PaymentAllocationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @SerialName("payment_id") val paymentId: String = "",
    @SerialName("invoice_id") val invoiceId: String = "",
    val amount: Double = 0.0,
    @SerialName("payment_date") val paymentDate: String = "",
    @SerialName("payment_method") val paymentMethod: String = "",
    @SerialName("allocated_amount") val allocatedAmount: Double = 0.0,
    val remarks: String = "",
    val collector: String = ""
)

@Serializable
@Entity(tableName = "payments")
data class PaymentCollectionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @SerialName("receipt_no") val receiptNo: String = "",
    @SerialName("customer_id") val customerId: String = "",
    @SerialName("customer_name") val customerName: String = "",
    @SerialName("customer_code") val customerCode: String = "",
    val amount: Double = 0.0,
    @SerialName("payment_method") val paymentMethod: String = "Cash",
    @SerialName("transaction_id") val transactionId: String = "",
    @SerialName("payment_date") val paymentDate: String = "",
    @SerialName("billing_month") val billingMonth: String = "",
    @SerialName("collected_by") val collectedBy: String = "",
    @SerialName("collected_by_id") val collectedById: String = "",
    @SerialName("collector_name") val collectorName: String = "",
    val remarks: String = ""
)

@Serializable
@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val category: String = "",
    val amount: Double = 0.0,
    @SerialName("expense_date") val expenseDate: String = "",
    @SerialName("expense_by") val expenseBy: String = "",
    val notes: String = ""
)

@Serializable
@Entity(tableName = "staff")
data class StaffEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val mobile: String = "",
    val role: String = "operator",
    val salary: Double = 0.0,
    val password: String = "",
    val zone: String = "All",
    @SerialName("joining_date") val joiningDate: String = "",
    @SerialName("receive_alerts") val receiveAlerts: Boolean = false,
    val active: Boolean = true,
    val balance: Double = 0.0,
    val status: String = "Active"
)

@Serializable
@Entity(tableName = "staff_salary")
data class StaffSalaryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @SerialName("staff_id") val staffId: String = "",
    @SerialName("staff_name") val staffName: String = "",
    val amount: Double = 0.0,
    @SerialName("salary_month") val salaryMonth: String = "",
    @SerialName("payment_date") val paymentDate: String = ""
)

@Serializable
@Entity(tableName = "staff_payouts")
data class StaffPayoutEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @SerialName("staff_id") val staffId: String = "",
    @SerialName("staff_name") val staffName: String = "",
    val month: String = "",
    val amount: Double = 0.0,
    val type: String = "salary_add", // salary_add, payment
    @SerialName("new_balance") val newBalance: Double = 0.0,
    val date: String = "",
    val remarks: String = ""
)

@Serializable
@Entity(tableName = "isp_settings")
data class ISPSettingsEntity(
    @PrimaryKey val id: Int = 1,
    @SerialName("isp_name") val ispName: String = "NetBill ISP",
    val address: String = "",
    @SerialName("mobile_number") val mobileNumber: String = "",
    @SerialName("support_number") val supportNumber: String = "",
    @SerialName("sms_api_url") val smsApiUrl: String = "",
    @SerialName("sms_api_key") val smsApiKey: String = "",
    @SerialName("sms_sender_id") val smsSenderId: String = "",
    @SerialName("is_auto_sms_enabled") val isAutoSmsEnabled: Boolean = false,
    @SerialName("whatsapp_api_url") val whatsappApiUrl: String = "",
    @SerialName("whatsapp_token") val whatsappToken: String = "",
    @SerialName("admin_whatsapp_number") val adminWhatsappNumber: String = "",
    @SerialName("is_whatsapp_alert_enabled") val isWhatsappAlertEnabled: Boolean = false,
    @SerialName("admin_identifier") val adminIdentifier: String = "admin@isp.com",
    @SerialName("admin_password") val adminPassword: String = "123456",
    @SerialName("monthly_target") val monthlyTarget: Double = 0.0,
    @SerialName("personal_bkash_no") val personalBkashNo: String = "017XXXXXXXX",
    @SerialName("personal_nagad_no") val personalNagadNo: String = "018XXXXXXXX",
    @SerialName("role_permissions") val rolePermissionsJson: String? = null
)

@Serializable
data class UserRolePermissions(
    val canCollect: Boolean = true,
    val canCollectDirect: Boolean = false,
    val canSeeMobile: Boolean = true,
    val canSeeAddress: Boolean = true,
    val canEdit: Boolean = false,
    val canDelete: Boolean = false,
    val canAdd: Boolean = false,
    val canSeeRevenue: Boolean = false,
    val canInventory: Boolean = false,
    val canSuspend: Boolean = false,
    val canLedger: Boolean = true,
    val canPasswords: Boolean = false,
    val canExpenses: Boolean = false,
    val canSMS: Boolean = false,
    val canDiscount: Boolean = false,
    val canBulkBill: Boolean = false,
    val canEditPayments: Boolean = false,
    val canManageStock: Boolean = false,
    val canAssignAssets: Boolean = false,
    val canManageZones: Boolean = false,
    val canManageRouters: Boolean = false,
    val canResolveTickets: Boolean = true,
    val canSendBulkSMS: Boolean = false,
    val canEditTemplates: Boolean = false,
    val canSeeStatsCards: Boolean = true,
    val canSeeExpiryAlerts: Boolean = true,
    val canSeeComplaintsAlert: Boolean = true,
    val canSeeVerificationAlert: Boolean = false,
    val canSeeTodayCollection: Boolean = true,
    val canSeeTotalCollection: Boolean = true,
    val canAccessBilling: Boolean = false,
    val canAccessReports: Boolean = false,
    val canAccessInventory: Boolean = false,
    val canAccessPackages: Boolean = false,
    val canAccessSMS: Boolean = false,
    val canAccessSalary: Boolean = false,
    val canAccessTickets: Boolean = true,
    val canAccessCustomers: Boolean = true,
    val canAccessPayments: Boolean = true,
    val canAccessExpenses: Boolean = false,
    val canAccessStaff: Boolean = false,
    val canAccessInfrastructure: Boolean = false,
    val canAccessSmsLogs: Boolean = false,
    val canAccessGlobalSettings: Boolean = false,
    val canModifyPricing: Boolean = false,
    val canViewLogs: Boolean = false,
    val canManageStaff: Boolean = false
)

@Serializable
@Entity(tableName = "ledger_entries")
data class LedgerEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @SerialName("customer_id") val customerId: String = "",
    val date: String = "",
    val time: String = "",
    val type: String = "", // Payment, Bill, Adjustment
    val amount: Double = 0.0,
    @SerialName("is_debit") val isDebit: Boolean = false,
    val description: String = "",
    @SerialName("reference_no") val referenceNo: String = "",
    @SerialName("payment_method") val paymentMethod: String = "",
    @SerialName("collector_name") val collectorName: String = "",
    @SerialName("running_balance") val runningBalance: Double = 0.0,
    @SerialName("monthly_rent") val monthlyRent: Double = 0.0,
    @SerialName("discount_amount") val discountAmount: Double = 0.0,
    @SerialName("paid_amount") val paidAmount: Double = 0.0,
    @SerialName("total_due_balance") val totalDueBalance: Double = 0.0
)

@Serializable
@Entity(tableName = "sms_logs")
data class SmsLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @SerialName("customer_id") val customerId: String = "",
    @SerialName("customer_code") val customerCode: String = "",
    @SerialName("customer_name") val customerName: String = "",
    val mobile: String = "",
    @SerialName("notification_type") val notificationType: String = "",
    val message: String = "",
    val status: String = "Pending", // Sent, Failed
    @SerialName("sent_timestamp") val sentTimestamp: String = ""
)

@Serializable
@Entity(tableName = "sms_templates")
data class SmsTemplateEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val category: String = "General",
    @SerialName("message_content") val messageContent: String = "",
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("is_default") val isDefault: Boolean = false,
    @SerialName("target_audience") val targetAudience: String = "All",
    @SerialName("last_updated") val lastUpdated: String = ""
)

@Serializable
@Entity(tableName = "inventory_items")
data class InventoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val category: String = "",
    val brand: String = "",
    @SerialName("serial_number") val serialNumber: String = "",
    @SerialName("cost_price") val costPrice: Double = 0.0,
    val quantity: Int = 0,
    @SerialName("assigned_to_customer_id") val assignedToCustomerId: String? = null,
    val status: String = "In Stock" // In Stock, Assigned, Faulty
)

@Serializable
@Entity(tableName = "support_tickets")
data class SupportTicketEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @SerialName("customer_id") val customerId: String = "",
    @SerialName("customer_name") val customerName: String = "",
    @SerialName("customer_code") val customerCode: String = "",
    @SerialName("customer_phone") val customerPhone: String = "",
    @SerialName("issue_type") val issueType: String = "",
    val description: String = "",
    val status: String = "Pending", // Open, Pending, Resolved
    val priority: String = "Normal", // Low, Normal, High
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("scheduled_date") val scheduledDate: String? = "",
    @SerialName("scheduled_time") val scheduledTime: String? = "",
    @SerialName("last_updated") val lastUpdated: String? = ""
)

@Serializable
@Entity(tableName = "zones")
data class ZoneEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = ""
)

@Serializable
@Entity(tableName = "sub_zones")
data class SubZoneEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @SerialName("zone_id") val zoneId: String = "",
    val name: String = ""
)

@Serializable
@Entity(tableName = "boxes")
data class BoxEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @SerialName("sub_zone_id") val subZoneId: String = "",
    val name: String = ""
)

@Serializable
@Entity(tableName = "mikrotik_routers")
data class MikroTikRouterEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val host: String = "",
    val port: Int = 8728,
    @SerialName("api_user") val apiUser: String = "",
    @SerialName("api_pass") val apiPass: String = "",
    @SerialName("is_connected") val isConnected: Boolean = false
)

@Serializable
@Entity(tableName = "payment_requests")
data class PaymentRequestEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @SerialName("customer_id") val customerId: String = "",
    @SerialName("customer_name") val customerName: String = "",
    @SerialName("customer_code") val customerCode: String = "",
    val amount: Double = 0.0,
    val method: String = "",
    @SerialName("trx_id") val trxId: String = "",
    val status: String = "pending", // pending, approved, rejected
    @SerialName("request_date") val requestDate: String = "",
    @SerialName("request_time") val requestTime: String = "",
    @SerialName("collected_by") val collectedBy: String = ""
)
