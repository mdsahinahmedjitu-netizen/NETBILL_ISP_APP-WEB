package com.example.data.remote

import android.util.Log
import com.example.data.entity.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirestoreService {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "FirestoreService"

    private fun logInfo(action: String, collection: String, docId: String) {
        val user = auth.currentUser
        val projectId = db.app.options.projectId
        Log.d(TAG, "--------------------------------------------------")
        Log.d(TAG, "ACTION: $action")
        Log.d(TAG, "Firebase Project ID: $projectId")
        Log.d(TAG, "Firebase Auth UID: ${user?.uid ?: "NOT LOGGED IN"}")
        Log.d(TAG, "Collection Path: $collection")
        Log.d(TAG, "Document ID: $docId")
    }

    // Collections
    private val customersCol = db.collection("customers")
    private val packagesCol = db.collection("packages")
    private val invoicesCol = db.collection("invoices")
    private val paymentsCol = db.collection("payments")
    private val allocationsCol = db.collection("allocations")
    private val expensesCol = db.collection("expenses")
    private val ledgerCol = db.collection("ledger")
    private val settingsCol = db.collection("isp_settings")
    private val staffCol = db.collection("staff")
    private val salariesCol = db.collection("staff_salaries")
    private val routersCol = db.collection("mikrotik_routers")
    private val smsLogsCol = db.collection("sms_logs")
    private val smsTemplatesCol = db.collection("sms_templates")

    // Generic save method
    suspend fun <T : Any> saveDocument(collectionPath: String, id: String, data: T) {
        logInfo("SaveDocument", collectionPath, id)
        Log.d(TAG, "Data Payload: $data")
        try {
            db.collection(collectionPath).document(id).set(data, SetOptions.merge()).await()
            Log.d(TAG, "WRITE SUCCESS (LOCAL): $collectionPath/$id")
        } catch (e: Exception) {
            Log.e(TAG, "WRITE FAILED: $collectionPath/$id", e)
        }
    }

    // Customer operations
    suspend fun saveCustomer(customer: CustomerEntity) {
        logInfo("SaveCustomer", "customers", customer.id)
        try {
            customersCol.document(customer.id).set(customer).await()
            Log.d(TAG, "WRITE SUCCESS: customers/${customer.id}")
        } catch (e: Exception) {
            Log.e(TAG, "WRITE FAILED: customers/${customer.id}", e)
        }
    }

    suspend fun deleteCustomer(id: String) {
        logInfo("DeleteCustomer", "customers", id)
        try {
            customersCol.document(id).delete().await()
            Log.d(TAG, "DELETE SUCCESS: customers/$id")
        } catch (e: Exception) {
            Log.e(TAG, "DELETE FAILED: customers/$id", e)
        }
    }

    // Financial Transaction: Record Payment + Update Invoices + Update Customer Due + Ledger
    suspend fun recordPaymentTransaction(
        payment: PaymentCollectionEntity,
        allocations: List<PaymentAllocationEntity>,
        updatedInvoices: List<InvoiceEntity>,
        customer: CustomerEntity,
        ledgerEntries: List<LedgerEntryEntity>
    ) {
        logInfo("RecordPaymentTransaction", "multiple", payment.id)
        try {
            db.runTransaction { transaction ->
                transaction.set(paymentsCol.document(payment.id), payment)
                allocations.forEach { transaction.set(allocationsCol.document(it.id), it) }
                updatedInvoices.forEach { transaction.set(invoicesCol.document(it.id), it) }
                transaction.set(customersCol.document(customer.id), customer)
                ledgerEntries.forEach { transaction.set(ledgerCol.document(it.id), it) }
                null
            }.await()
            Log.d(TAG, "TRANSACTION SUCCESS: Payment ${payment.id}")
        } catch (e: Exception) {
            Log.e(TAG, "TRANSACTION FAILED", e)
        }
    }

    // Billing Transaction: Save Invoices + Update Customer Due + Ledger
    suspend fun generateBillingTransaction(
        invoices: List<InvoiceEntity>,
        updatedCustomers: List<CustomerEntity>,
        ledgerEntries: List<LedgerEntryEntity>
    ) {
        Log.d(TAG, "Starting Billing Batch Write for ${invoices.size} invoices")
        try {
            db.runBatch { batch ->
                invoices.forEach { batch.set(invoicesCol.document(it.id), it) }
                updatedCustomers.forEach { batch.set(customersCol.document(it.id), it) }
                ledgerEntries.forEach { batch.set(ledgerCol.document(it.id), it) }
            }.await()
            Log.d(TAG, "BATCH WRITE SUCCESS: Billing")
        } catch (e: Exception) {
            Log.e(TAG, "BATCH WRITE FAILED: Billing", e)
        }
    }

    suspend fun savePackage(pkg: PackageEntity) = saveDocument("packages", pkg.id, pkg)
    suspend fun deletePackage(id: String) {
        logInfo("DeletePackage", "packages", id)
        try {
            packagesCol.document(id).delete().await()
            Log.d(TAG, "DELETE SUCCESS: packages/$id")
        } catch (e: Exception) {
            Log.e(TAG, "DELETE FAILED: packages/$id", e)
        }
    }

    suspend fun saveExpense(expense: ExpenseEntity) = saveDocument("expenses", expense.id, expense)
    suspend fun deleteExpense(id: String) = saveDocument("expenses", id, "deleted")

    suspend fun saveSettings(settings: ISPSettingsEntity) = saveDocument("isp_settings", settings.id, settings)

    suspend fun saveStaff(staff: StaffEntity) = saveDocument("staff", staff.id, staff)
    suspend fun saveSalary(salary: StaffSalaryEntity) = saveDocument("staff_salaries", salary.id, salary)

    suspend fun saveRouter(router: MikroTikRouterEntity) = saveDocument("mikrotik_routers", router.id, router)

    suspend fun saveSmsLog(log: SmsLogEntity) = saveDocument("sms_logs", log.id, log)
    suspend fun saveSmsTemplate(template: SmsTemplateEntity) = saveDocument("sms_templates", template.id, template)
    suspend fun deleteSmsTemplate(id: String) = saveDocument("sms_templates", id, "deleted")

    suspend fun checkConnectivity(): String {
        return try {
            db.enableNetwork().await()
            val snapshot = db.collection("customers").limit(1).get().await()
            val metadata = snapshot.metadata
            "CONNECTED: Found ${snapshot.size()} customers | FromCache: ${metadata.isFromCache}"
        } catch (e: Exception) {
            "FAILED: ${e.message}"
        }
    }
}
