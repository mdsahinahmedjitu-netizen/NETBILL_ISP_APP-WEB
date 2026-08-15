package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val username: String = "",
    val passwordHash: String = "",
    val name: String = "",
    val mobile: String = "",
    val role: String = "Billing Operator", // e.g. "Super Admin", "Billing Operator", "Support Staff"
    val active: Boolean = true,
)

@Serializable
@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val customer_code: String = "", 
    val name: String = "",
    val mobile: String = "",
    val alt_mobile: String = "",
    val address: String = "",
    val zone: String = "",
    val sub_zone: String = "",
    val box_id: String = "",
    val package_name: String = "",
    val package_id: String? = null,
    val monthly_bill: Double = 0.0,
    val current_due: Double = 0.0,
    val advance_balance: Double = 0.0,
    val pppoe_username: String = "",
    val pppoe_password: String = "",
    val onu_mac: String = "",
    val onu_serial: String = "",
    val router_id: String = "",
    val billing_type: String = "MONTHLY DATE TO DATE",
    val status: String = "Active",
    val subscription_type: String = "Prepaid",
    val join_date: String = "",
    val assigned_staff_id: String = "",
    val expire_date: String = "",
    val request_date: String = "",
    val reference_name: String = "",
    val reference_mobile: String = "",
    val notes: String = ""
) {
    // CamelCase helpers for UI compatibility
    val customerCode get() = customer_code
    val packageName get() = package_name
    val monthlyBill get() = monthly_bill
    val currentDue get() = current_due
    val pppoeUsername get() = pppoe_username
    val joinDayOfMonth: Int get() = try {
        val parts = join_date.split("-")
        if (parts.size == 3) parts[2].toInt() else 1
    } catch (e: Exception) { 1 }
}

@Serializable
@Entity(tableName = "packages")
data class PackageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val speed: String = "",
    val monthly_price: Double = 0.0,
    val description: String = ""
) {
    val monthlyPrice get() = monthly_price
}

@Serializable
@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val invoice_no: String = "",
    val customer_id: String = "",
    val customer_name: String = "",
    val billing_month_year: String = "",
    val bill_amount: Double = 0.0,
    val total_payable: Double = 0.0,
    val due_amount: Double = 0.0,
    val status: String = "Unpaid",
    val generated_date: String = ""
) {
    val invoiceNo get() = invoice_no
    val billingMonthYear get() = billing_month_year
    val totalPayable get() = total_payable
    val dueAmount get() = due_amount
    val billAmount get() = bill_amount
}

@Serializable
@Entity(tableName = "payments")
data class PaymentCollectionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val receipt_no: String = "",
    val customer_id: String = "",
    val customer_name: String = "",
    val customer_code: String = "",
    val amount: Double = 0.0,
    val payment_method: String = "Cash",
    val transaction_id: String = "",
    val payment_date: String = "",
    val billing_month: String = "",
    val collected_by: String = "",
    val remarks: String = ""
) {
    val receiptNo get() = receipt_no
    val customerName get() = customer_name
    val customerCode get() = customer_code
    val paymentMethod get() = payment_method
    val transactionId get() = transaction_id
    val paymentDate get() = payment_date
    val billingMonth get() = billing_month
}

@Serializable
@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val category: String = "",
    val amount: Double = 0.0,
    val expense_date: String = "",
    val expense_by: String = "",
    val notes: String = ""
) {
    val expenseDate get() = expense_date
}

@Serializable
@Entity(tableName = "staff")
data class StaffEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val mobile: String = "",
    val role: String = "Lineman",
    val salary: Double = 0.0,
    val balance: Double = 0.0,
    val status: String = "Active"
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
    val zone_id: String = "",
    val name: String = ""
)

@Serializable
@Entity(tableName = "boxes")
data class BoxEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sub_zone_id: String = "",
    val name: String = ""
)

@Serializable
@Entity(tableName = "payment_allocations")
data class PaymentAllocationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val paymentId: String = "",
    val invoiceId: String = "",
    val customerId: String = "",
    val allocatedAmount: Double = 0.0
)

@Serializable
@Entity(tableName = "sms_logs")
data class SmsLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val customerId: String = "",
    val message: String = "",
    val status: String = "Sent"
)

@Serializable
@Entity(tableName = "sms_templates")
data class SmsTemplateEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val messageContent: String = ""
)

@Serializable
@Entity(tableName = "inventory_items")
data class InventoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val status: String = "In Stock"
)

@Serializable
@Entity(tableName = "support_tickets")
data class SupportTicketEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val customer_id: String = "",
    val description: String = "",
    val status: String = "Pending"
)

@Serializable
data class ISPSettingsEntity(
    val id: String = "1",
    val company_name: String = "NetBill ISP",
    val company_phone: String = ""
)
