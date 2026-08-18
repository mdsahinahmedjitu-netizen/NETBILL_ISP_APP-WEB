package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE (username = :identifier OR mobile = :identifier) AND status = 'Active' LIMIT 1")
    suspend fun getUserByUsernameOrMobile(identifier: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY name ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUserById(id: String)
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY id DESC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE (pppoeUsername = :identifier OR customerCode = :identifier OR mobile = :identifier) AND pppoePassword = :password LIMIT 1")
    suspend fun getCustomerByIdentifier(identifier: String, password: String): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity)

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomerById(id: String)
}

@Dao
interface PackageDao {
    @Query("SELECT * FROM packages ORDER BY name ASC")
    fun getAllPackages(): Flow<List<PackageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPackage(packageEntity: PackageEntity)

    @Query("DELETE FROM packages WHERE id = :id")
    suspend fun deletePackageById(id: String)
}

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices ORDER BY id DESC")
    fun getAllInvoices(): Flow<List<InvoiceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceEntity)

    @Update
    suspend fun updateInvoice(invoice: InvoiceEntity)

    @Query("DELETE FROM invoices WHERE id = :id")
    suspend fun deleteInvoiceById(id: String)
}

@Dao
interface PaymentCollectionDao {
    @Query("SELECT * FROM payments ORDER BY id DESC")
    fun getAllPayments(): Flow<List<PaymentCollectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentCollectionEntity)

    @Query("DELETE FROM payments WHERE id = :id")
    suspend fun deletePaymentById(id: String)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY id DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: String)
}

@Dao
interface StaffDao {
    @Query("SELECT * FROM staff ORDER BY id DESC")
    fun getAllStaff(): Flow<List<StaffEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaff(staff: StaffEntity)

    @Query("DELETE FROM staff WHERE id = :id")
    suspend fun deleteStaffById(id: String)
}

@Dao
interface ISPSettingsDao {
    @Query("SELECT * FROM isp_settings WHERE id = '1' LIMIT 1")
    fun getSettings(): Flow<ISPSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: ISPSettingsEntity)
}

@Dao
interface LedgerDao {
    @Query("SELECT * FROM ledger_entries ORDER BY date DESC, time DESC")
    fun getAllLedgerEntries(): Flow<List<LedgerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedger(entry: LedgerEntity)

    @Query("DELETE FROM ledger_entries WHERE id = :id")
    suspend fun deleteLedgerById(id: String)
}

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_items ORDER BY id DESC")
    fun getAllInventory(): Flow<List<InventoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InventoryEntity)

    @Query("DELETE FROM inventory_items WHERE id = :id")
    suspend fun deleteItemById(id: String)
}

@Dao
interface SupportTicketDao {
    @Query("SELECT * FROM support_tickets ORDER BY id DESC")
    fun getAllTickets(): Flow<List<SupportTicketEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: SupportTicketEntity)

    @Query("DELETE FROM support_tickets WHERE id = :id")
    suspend fun deleteTicketById(id: String)
}

@Dao
interface PaymentAllocationDao {
    @Query("SELECT * FROM payment_allocations ORDER BY id DESC")
    fun getAllAllocations(): Flow<List<PaymentAllocationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllocation(allocation: PaymentAllocationEntity)
}

@Dao
interface SmsLogDao {
    @Query("SELECT * FROM sms_logs ORDER BY sentTimestamp DESC")
    fun getAllLogs(): Flow<List<SmsLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SmsLogEntity)

    @Update
    suspend fun updateLog(log: SmsLogEntity)

    @Query("DELETE FROM sms_logs")
    suspend fun clearAllLogs()
}

@Dao
interface SmsTemplateDao {
    @Query("SELECT * FROM sms_templates ORDER BY title ASC")
    fun getAllTemplates(): Flow<List<SmsTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: SmsTemplateEntity)

    @Update
    suspend fun updateTemplate(template: SmsTemplateEntity)

    @Query("DELETE FROM sms_templates WHERE id = :id")
    suspend fun deleteTemplateById(id: String)
}

@Dao
interface MikroTikDao {
    @Query("SELECT * FROM mikrotik_routers ORDER BY name ASC")
    fun getAllRouters(): Flow<List<MikroTikRouterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRouter(router: MikroTikRouterEntity)

    @Query("DELETE FROM mikrotik_routers WHERE id = :id")
    suspend fun deleteRouterById(id: String)
}
