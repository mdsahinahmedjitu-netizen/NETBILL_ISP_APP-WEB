package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.CustomerEntity
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.ISPSettingsEntity
import com.example.data.entity.InvoiceEntity
import com.example.data.entity.LedgerEntryEntity
import com.example.data.entity.MikroTikRouterEntity
import com.example.data.entity.PackageEntity
import com.example.data.entity.PaymentAllocationEntity
import com.example.data.entity.PaymentCollectionEntity
import com.example.data.entity.StaffEntity
import com.example.data.entity.StaffSalaryEntity
import com.example.data.entity.SmsLogEntity
import com.example.data.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE (username = :identifier OR mobile = :identifier) AND active = 1 LIMIT 1")
    suspend fun getUserByUsernameOrMobile(identifier: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY name ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY id DESC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: Long): CustomerEntity?

    @Query("""
        SELECT * FROM customers 
        WHERE (:zone = 'All' OR zone = :zone)
        AND (:packageName = 'All' OR packageName = :packageName)
        AND (:status = 'All' OR status = :status)
        AND (:onlyDue = 0 OR currentDue > 0)
        AND (name LIKE '%' || :query || '%' OR customerCode LIKE '%' || :query || '%' OR mobile LIKE '%' || :query || '%' OR pppoeUsername LIKE '%' || :query || '%')
        ORDER BY id DESC
    """)
    fun searchAndFilterCustomers(
        query: String,
        zone: String,
        packageName: String,
        status: String,
        onlyDue: Int
    ): Flow<List<CustomerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomerById(id: Long)

    @Query("SELECT COUNT(*) FROM customers")
    fun getTotalCustomerCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM customers WHERE status = 'Active'")
    fun getActiveCustomerCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM customers WHERE status = 'Inactive'")
    fun getInactiveCustomerCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(currentDue), 0.0) FROM customers")
    fun getTotalDueAmount(): Flow<Double>
}

@Dao
interface PackageDao {
    @Query("SELECT * FROM packages ORDER BY monthlyPrice ASC")
    fun getAllPackages(): Flow<List<PackageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPackage(packageEntity: PackageEntity): Long

    @Query("DELETE FROM packages WHERE id = :id")
    suspend fun deletePackageById(id: Long)
}

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices ORDER BY id DESC")
    fun getAllInvoices(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE customerId = :customerId ORDER BY id DESC")
    fun getInvoicesForCustomer(customerId: Long): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE customerId = :customerId AND status != 'Paid' AND status != 'Cancelled' ORDER BY id ASC")
    suspend fun getUnpaidInvoicesForCustomerAsc(customerId: Long): List<InvoiceEntity>

    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    suspend fun getInvoiceById(id: Long): InvoiceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceEntity): Long

    @Update
    suspend fun updateInvoice(invoice: InvoiceEntity)

    @Query("SELECT COALESCE(SUM(totalPayable - paidAmount), 0.0) FROM invoices WHERE status != 'Paid'")
    fun getTotalUnpaidInvoiceAmount(): Flow<Double>
}

@Dao
interface PaymentAllocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllocation(allocation: PaymentAllocationEntity): Long

    @Query("SELECT * FROM payment_allocations WHERE customerId = :customerId ORDER BY id DESC")
    fun getAllocationsForCustomer(customerId: Long): Flow<List<PaymentAllocationEntity>>

    @Query("SELECT * FROM payment_allocations WHERE invoiceId = :invoiceId ORDER BY id DESC")
    fun getAllocationsForInvoice(invoiceId: Long): Flow<List<PaymentAllocationEntity>>

    @Query("SELECT * FROM payment_allocations ORDER BY id DESC")
    fun getAllAllocations(): Flow<List<PaymentAllocationEntity>>
}

@Dao
interface PaymentCollectionDao {
    @Query("SELECT * FROM payment_collections ORDER BY id DESC")
    fun getAllPayments(): Flow<List<PaymentCollectionEntity>>

    @Query("SELECT * FROM payment_collections WHERE customerId = :customerId ORDER BY id DESC")
    fun getPaymentsForCustomer(customerId: Long): Flow<List<PaymentCollectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentCollectionEntity): Long

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM payment_collections WHERE paymentDate = :date")
    fun getTodaysCollection(date: String): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM payment_collections WHERE paymentDate LIKE :yearMonth || '%'")
    fun getMonthlyCollection(yearMonth: String): Flow<Double>
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY id DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Long)

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE expenseDate = :date")
    fun getTodaysExpense(date: String): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE expenseDate LIKE :yearMonth || '%'")
    fun getMonthlyExpense(yearMonth: String): Flow<Double>
}

@Dao
interface StaffDao {
    @Query("SELECT * FROM staff ORDER BY id DESC")
    fun getAllStaff(): Flow<List<StaffEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaff(staff: StaffEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalary(salary: StaffSalaryEntity): Long

    @Query("SELECT * FROM staff_salaries ORDER BY id DESC")
    fun getAllSalaries(): Flow<List<StaffSalaryEntity>>
}

@Dao
interface MikroTikDao {
    @Query("SELECT * FROM mikrotik_routers ORDER BY id ASC")
    fun getAllRouters(): Flow<List<MikroTikRouterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRouter(router: MikroTikRouterEntity): Long

    @Query("UPDATE mikrotik_routers SET isConnected = :connected WHERE id = :routerId")
    suspend fun updateRouterStatus(routerId: Long, connected: Boolean)
}

@Dao
interface ISPSettingsDao {
    @Query("SELECT * FROM isp_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<ISPSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: ISPSettingsEntity)
}

@Dao
interface LedgerDao {
    @Query("SELECT * FROM ledger_entries WHERE customerId = :customerId ORDER BY id ASC")
    fun getLedgerForCustomer(customerId: Long): Flow<List<LedgerEntryEntity>>

    @Query("SELECT * FROM ledger_entries ORDER BY id DESC")
    fun getAllLedgerEntries(): Flow<List<LedgerEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerEntry(entry: LedgerEntryEntity): Long

    @Query("DELETE FROM ledger_entries WHERE customerId = :customerId")
    suspend fun deleteLedgerForCustomer(customerId: Long)
}

@Dao
interface SmsLogDao {
    @Query("SELECT * FROM sms_logs ORDER BY id DESC")
    fun getAllSmsLogs(): Flow<List<SmsLogEntity>>

    @Query("SELECT * FROM sms_logs WHERE customerId = :customerId ORDER BY id DESC")
    fun getSmsLogsForCustomer(customerId: Long): Flow<List<SmsLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSmsLog(log: SmsLogEntity): Long

    @Update
    suspend fun updateSmsLog(log: SmsLogEntity)

    @Query("DELETE FROM sms_logs WHERE id = :id")
    suspend fun deleteSmsLogById(id: Long)

    @Query("DELETE FROM sms_logs")
    suspend fun clearAllSmsLogs()
}

