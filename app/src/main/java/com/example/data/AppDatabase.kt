package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.*
import com.example.data.entity.*

@Database(
    entities = [
        UserEntity::class,
        CustomerEntity::class,
        PackageEntity::class,
        InvoiceEntity::class,
        PaymentAllocationEntity::class,
        PaymentCollectionEntity::class,
        ExpenseEntity::class,
        StaffEntity::class,
        ISPSettingsEntity::class,
        LedgerEntity::class,
        SmsLogEntity::class,
        SmsTemplateEntity::class,
        InventoryEntity::class,
        SupportTicketEntity::class,
        ZoneEntity::class,
        SubZoneEntity::class,
        BoxEntity::class,
        MikroTikRouterEntity::class,
        StaffPayoutEntity::class,
        StaffSalaryEntity::class,
        PaymentRequestEntity::class
    ],
    version = 14,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun customerDao(): CustomerDao
    abstract fun packageDao(): PackageDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun paymentAllocationDao(): PaymentAllocationDao
    abstract fun paymentCollectionDao(): PaymentCollectionDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun staffDao(): StaffDao
    abstract fun mikrotikDao(): MikroTikDao
    abstract fun ispSettingsDao(): ISPSettingsDao
    abstract fun ledgerDao(): LedgerDao
    abstract fun smsLogDao(): SmsLogDao
    abstract fun smsTemplateDao(): SmsTemplateDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun supportTicketDao(): SupportTicketDao
    abstract fun staffPayoutDao(): StaffPayoutDao
    abstract fun staffSalaryDao(): StaffSalaryDao
    abstract fun paymentRequestDao(): com.example.data.dao.PaymentRequestDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "netbill_isp_database.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
