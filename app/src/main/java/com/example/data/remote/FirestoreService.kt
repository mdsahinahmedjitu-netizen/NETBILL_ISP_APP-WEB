package com.example.data.remote

import com.example.data.entity.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirestoreService {
    private val db = FirebaseFirestore.getInstance()

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
        db.collection(collectionPath).document(id).set(data, SetOptions.merge()).await()
    }

    // Customer operations
    suspend fun saveCustomer(customer: CustomerEntity) {
        customersCol.document(customer.id).set(customer).await()
    }

    suspend fun deleteCustomer(id: String) {
        customersCol.document(id).delete().await()
    }

    // Financial Transaction: Record Payment + Update Invoices + Update Customer Due + Ledger
    suspend fun recordPaymentTransaction(
        payment: PaymentCollectionEntity,
        allocations: List<PaymentAllocationEntity>,
        updatedInvoices: List<InvoiceEntity>,
        customer: CustomerEntity,
        ledgerEntries: List<LedgerEntryEntity>
    ) {
        db.runTransaction { transaction ->
            // 1. Save Payment
            transaction.set(paymentsCol.document(payment.id), payment)

            // 2. Save Allocations
            allocations.forEach { alloc ->
                transaction.set(allocationsCol.document(alloc.id), alloc)
            }

            // 3. Update Invoices
            updatedInvoices.forEach { inv ->
                transaction.set(invoicesCol.document(inv.id), inv)
            }

            // 4. Update Customer (Due and Advance)
            transaction.set(customersCol.document(customer.id), customer)

            // 5. Save Ledger Entries
            ledgerEntries.forEach { entry ->
                transaction.set(ledgerCol.document(entry.id), entry)
            }

            null
        }.await()
    }

    // Billing Transaction: Save Invoices + Update Customer Due + Ledger
    suspend fun generateBillingTransaction(
        invoices: List<InvoiceEntity>,
        updatedCustomers: List<CustomerEntity>,
        ledgerEntries: List<LedgerEntryEntity>
    ) {
        db.runBatch { batch ->
            invoices.forEach { inv ->
                batch.set(invoicesCol.document(inv.id), inv)
            }
            updatedCustomers.forEach { cust ->
                batch.set(customersCol.document(cust.id), cust)
            }
            ledgerEntries.forEach { entry ->
                batch.set(ledgerCol.document(entry.id), entry)
            }
        }.await()
    }

    suspend fun savePackage(pkg: PackageEntity) = packagesCol.document(pkg.id).set(pkg).await()
    suspend fun deletePackage(id: String) = packagesCol.document(id).delete().await()

    suspend fun saveExpense(expense: ExpenseEntity) = expensesCol.document(expense.id).set(expense).await()
    suspend fun deleteExpense(id: String) = expensesCol.document(id).delete().await()

    suspend fun saveSettings(settings: ISPSettingsEntity) = settingsCol.document(settings.id).set(settings).await()

    suspend fun saveStaff(staff: StaffEntity) = staffCol.document(staff.id).set(staff).await()
    suspend fun saveSalary(salary: StaffSalaryEntity) = salariesCol.document(salary.id).set(salary).await()

    suspend fun saveRouter(router: MikroTikRouterEntity) = routersCol.document(router.id).set(router).await()

    suspend fun saveSmsLog(log: SmsLogEntity) = smsLogsCol.document(log.id).set(log).await()
    suspend fun saveSmsTemplate(template: SmsTemplateEntity) = smsTemplatesCol.document(template.id).set(template).await()
    suspend fun deleteSmsTemplate(id: String) = smsTemplatesCol.document(id).delete().await()
}
