package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val username: String = "",
    @SerialName("password_hash") val passwordHash: String = "",
    val name: String = "",
    val mobile: String = "",
    val role: String = "Billing Operator",
    val active: Boolean = true,
)

@Serializable
@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @SerialName("customer_code") val customerCode: String = "",
    val name: String = "",
    val mobile: String = "",
    @SerialName("alt_mobile") val altMobile: String = "",
    val address: String = "",
    val zone: String = "",
    @SerialName("sub_zone") val subZone: String = "",
    @SerialName("box_id") val boxId: String = "",
    @SerialName("package_name") val packageName: String = "",
    @SerialName("package_id") val packageId: String? = null,
    @SerialName("monthly_bill") val monthlyBill: Double = 0.0,
    @SerialName("current_due") val currentDue: Double = 0.0,
    @SerialName("advance_balance") val advanceBalance: Double = 0.0,
    @SerialName("pppoe_username") val pppoeUsername: String = "",
    @SerialName("pppoe_password") val pppoePassword: String = "",
    @SerialName("onu_mac") val onuMac: String = "",
    @SerialName("onu_serial") val onuSerial: String = "",
    @SerialName("router_id") val routerId: String = "",
    @SerialName("billing_type") val billingType: String = "MONTHLY DATE TO DATE",
    @SerialName("payment_status") val paymentStatus: String = "Unpaid",
    val status: String = "Active",
    @SerialName("subscription_type") val subscriptionType: String = "Prepaid",
    @SerialName("join_date") val joinDate: String = "",
    @SerialName("assigned_staff_id") val assignedStaffId: String = "",
    @SerialName("expire_date") val expireDate: String = "",
    @SerialName("expire_time") val expireTime: String = "",
    @SerialName("request_date") val requestDate: String = "",
    @SerialName("connection_type") val connectionType: String = "",
    @SerialName("connection_fee") val connectionFee: Double = 0.0,
    @SerialName("reference_name") val referenceName: String = "",
    @SerialName("reference_mobile") val referenceMobile: String = "",
    val notes: String = "",
    val email: String = "",
    @SerialName("nid_number") val nidNumber: String = "",
    val dob: String = "",
    @SerialName("house_owner_name") val houseOwnerName: String = "",
    @SerialName("emergency_contact") val emergencyContact: String = "",
    val discount: Double = 0.0,
    val username: String = "",
    @SerialName("ip_address") val ipAddress: String = "",
    @SerialName("mac_address") val macAddress: String = "",
    @SerialName("onu_mac_serial") val onuMacSerial: String = "",
    @SerialName("fiber_core_no") val fiberCoreNo: String = "",
    @SerialName("network_box") val networkBox: String = "",
    @SerialName("join_day_of_month") val joinDayOfMonth: Int = 1,
    @SerialName("personal_bkash_no") val personalBkashNo: String = "",
    @SerialName("personal_nagad_no") val personalNagadNo: String = ""
)

@Serializable
@Entity(tableName = "packages")
data class PackageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val speed: String = "",
    @SerialName("monthly_price") val monthlyPrice: Double = 0.0,
    val description: String = "",
    @SerialName("active_user_count") val activeUserCount: Int = 0
)

@Serializable
@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @SerialName("invoice_no") val invoiceNo: String = "",
    @SerialName("customer_id") val customerId: String = "",
    @SerialName("customer_name") val customerName: String = "",
    @SerialName("customer_code") val customerCode: String = "",
    @SerialName("package_name") val packageName: String = "",
    @SerialName("billing_month_year") val billingMonthYear: String = "",
    @SerialName("bill_amount") val billAmount: Double = 0.0,
    @SerialName("total_payable") val totalPayable: Double = 0.0,
    @SerialName("due_amount") val dueAmount: Double = 0.0,
    @SerialName("paid_amount") val paidAmount: Double = 0.0,
    @SerialName("previous_due") val previousDue: Double = 0.0,
    @SerialName("due_date") val dueDate: String = "",
    val status: String = "Unpaid",
    @SerialName("generated_date") val generatedDate: String = "",
    @SerialName("is_20th_day_override_choice") val is20thDayOverrideChoice: String = "Standard"
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
@Entity(tableName = "payment_allocations")
data class PaymentAllocationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @SerialName("payment_id") val paymentId: String = "",
    @SerialName("invoice_id") val invoiceId: String = "",
    val amount: Double = 0.0,
    @SerialName("allocated_amount") val allocatedAmount: Double = 0.0,
    @SerialName("payment_date") val paymentDate: String = "",
    @SerialName("payment_method") val paymentMethod: String = "",
    val remarks: String = "",
    val collector: String = ""
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
    val role: String = "Lineman",
    val salary: Double = 0.0,
    val balance: Double = 0.0,
    val status: String = "Active",
    val active: Boolean = true,
    @SerialName("receive_alerts") val receiveAlerts: Boolean = false,
    @SerialName("joining_date") val joiningDate: String = ""
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
@Entity(tableName = "ledger_entries")
data class LedgerEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @SerialName("customer_id") val customerId: String = "",
    val date: String = "",
    val time: String = "",
    val type: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    @SerialName("is_debit") val isDebit: Boolean = true,
    @SerialName("reference_no") val referenceNo: String = "",
    @SerialName("payment_method") val paymentMethod: String = "",
    @SerialName("collector_name") val collectorName: String = "",
    @SerialName("running_balance") val runningBalance: Double = 0.0
)

@Serializable
@Entity(tableName = "inventory_items")
data class InventoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val status: String = "In Stock",
    @SerialName("item_name") val itemName: String = "",
    val brand: String = "",
    @SerialName("serial_number") val serialNumber: String = "",
    @SerialName("cost_price") val costPrice: Double = 0.0,
    val category: String = "",
    @SerialName("purchase_date") val purchaseDate: String = "",
    @SerialName("assigned_to_customer_id") val assignedToCustomerId: String = ""
)

@Serializable
@Entity(tableName = "support_tickets")
data class SupportTicketEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @SerialName("customer_id") val customerId: String = "",
    val description: String = "",
    val status: String = "Pending",
    @SerialName("customer_name") val customerName: String = "",
    @SerialName("customer_code") val customerCode: String = "",
    @SerialName("customer_phone") val customerPhone: String = "",
    @SerialName("issue_type") val issueType: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("scheduled_date") val scheduledDate: String = "",
    @SerialName("scheduled_time") val scheduledTime: String = "",
    @SerialName("last_updated") val lastUpdated: String = ""
)

@Serializable
@Entity(tableName = "isp_settings")
data class ISPSettingsEntity(
    @PrimaryKey val id: String = "1",
    @SerialName("company_name") val companyName: String = "NetBill ISP",
    @SerialName("company_phone") val companyPhone: String = "",
    @SerialName("is_auto_sms_enabled") val isAutoSmsEnabled: Boolean = false,
    @SerialName("admin_whatsapp_number") val adminWhatsappNumber: String = "",
    @SerialName("isp_name") val ispName: String = "",
    val address: String = "",
    @SerialName("mobile_number") val mobileNumber: String = "",
    @SerialName("support_number") val supportNumber: String = "",
    @SerialName("sms_api_url") val smsApiUrl: String = "",
    @SerialName("sms_api_key") val smsApiKey: String = "",
    @SerialName("sms_sender_id") val smsSenderId: String = "",
    @SerialName("whatsapp_api_url") val whatsappApiUrl: String = "",
    @SerialName("whatsapp_instance_id") val whatsappInstanceId: String = "",
    @SerialName("whatsapp_token") val whatsappToken: String = "",
    @SerialName("is_whatsapp_alert_enabled") val isWhatsappAlertEnabled: Boolean = false,
    @SerialName("personal_bkash_no") val personalBkashNo: String = "",
    @SerialName("personal_nagad_no") val personalNagadNo: String = ""
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
    @SerialName("sent_timestamp") val sentTimestamp: String = "",
    val status: String = "Sent",
    @SerialName("delivery_report") val deliveryReport: String = ""
)

@Serializable
@Entity(tableName = "sms_templates")
data class SmsTemplateEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val category: String = "",
    @SerialName("target_audience") val targetAudience: String = "",
    @SerialName("message_content") val messageContent: String = "",
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("is_default") val isDefault: Boolean = false,
    @SerialName("last_updated") val lastUpdated: String = ""
)

@Serializable
@Entity(tableName = "mikrotik_routers")
data class MikroTikRouterEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    @SerialName("ip_address") val ipAddress: String = "",
    @SerialName("api_port") val apiPort: Int = 8728,
    val username: String = "admin",
    val password: String = "",
    val status: String = "Unknown",
    @SerialName("router_name") val routerName: String = "",
    val zone: String = "",
    @SerialName("is_connected") val isConnected: Boolean = false,
    @SerialName("active_pppoe_count") val activePppoeCount: Int = 0,
    @SerialName("total_rx_mbps") val totalRxMbps: Double = 0.0,
    @SerialName("total_tx_mbps") val totalTxMbps: Double = 0.0
)

@Serializable
@Entity(tableName = "staff_salaries")
data class StaffSalaryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @SerialName("staff_id") val staffId: String = "",
    @SerialName("staff_name") val staffName: String = "",
    @SerialName("salary_month") val salaryMonth: String = "",
    @SerialName("payment_date") val paymentDate: String = "",
    val amount: Double = 0.0,
    val date: String = ""
)

@Serializable
data class LedgerEntryEntity(
    val id: String = UUID.randomUUID().toString(),
    @SerialName("customer_id") val customerId: String = "",
    val date: String = "",
    val time: String = "",
    val type: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    @SerialName("is_debit") val isDebit: Boolean = true,
    @SerialName("reference_no") val referenceNo: String = "",
    @SerialName("payment_method") val paymentMethod: String = "",
    @SerialName("collector_name") val collectorName: String = "",
    @SerialName("running_balance") val runningBalance: Double = 0.0
)
