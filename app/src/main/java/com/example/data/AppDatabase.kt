package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.CustomerDao
import com.example.data.dao.ExpenseDao
import com.example.data.dao.ISPSettingsDao
import com.example.data.dao.InvoiceDao
import com.example.data.dao.LedgerDao
import com.example.data.dao.MikroTikDao
import com.example.data.dao.PackageDao
import com.example.data.dao.PaymentAllocationDao
import com.example.data.dao.PaymentCollectionDao
import com.example.data.dao.SmsLogDao
import com.example.data.dao.StaffDao
import com.example.data.dao.UserDao
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
import com.example.data.entity.StaffEntity
import com.example.data.entity.StaffSalaryEntity
import com.example.data.entity.UserEntity

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
        StaffSalaryEntity::class,
        MikroTikRouterEntity::class,
        ISPSettingsEntity::class,
        LedgerEntryEntity::class,
        SmsLogEntity::class
    ],
    version = 6,
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
