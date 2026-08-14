import React, { useState, useEffect } from 'react';
import { db } from '../firebaseConfig';
import { collection, addDoc, doc, updateDoc, getDocs, query, where } from 'firebase/firestore';

const Billing = ({ store, t }) => {
  const [isProcessing, setIsProcessing] = useState(false);
  const currentMonth = new Intl.DateTimeFormat('en-US', { month: 'long', year: 'numeric' }).format(new Date());

  // Passive Auto-Generation Logic
  useEffect(() => {
    const today = new Date();
    const day = today.getDate();

    // Check if it's the 1st day of the month AND bills haven't been generated yet
    if (day === 1 && store.customers.length > 0) {
      triggerSilentBilling();
    }
  }, [store.customers]);

  const triggerSilentBilling = async () => {
    // Only run if not already processing and we have customers
    if (isProcessing) return;

    try {
      const activeCustomers = store.customers.filter(c => c.status === 'Active');
      const todayISO = new Date().toLocaleDateString('en-CA');
      const timeStr = new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });

      for (const cust of activeCustomers) {
        // Check if invoice already exists for this month to avoid duplicates
        const invQ = query(collection(db, "invoices"), where("customerId", "==", cust.id), where("billingMonthYear", "==", currentMonth));
        const invSnap = await getDocs(invQ);
        if (!invSnap.empty) continue;

        setIsProcessing(true); // Show syncing state

        const billAmt = parseFloat(cust.monthlyBill) || 0;
        const prevDue = parseFloat(cust.currentDue) || 0;
        let totalPayable = billAmt + prevDue;

        let advanceUsed = 0;
        let remainingAdvance = parseFloat(cust.advanceBalance) || 0;

        if (remainingAdvance > 0) {
            advanceUsed = Math.min(remainingAdvance, totalPayable);
            totalPayable -= advanceUsed;
            remainingAdvance -= advanceUsed;
        }

        const invId = "INV-" + Math.random().toString(36).substr(2, 8).toUpperCase();

        await addDoc(collection(db, "invoices"), {
          invoiceNo: invId,
          customerId: cust.id,
          customerName: cust.name,
          customerCode: cust.customerCode,
          packageName: cust.packageName,
          billingMonthYear: currentMonth,
          billAmount: billAmt,
          previousDue: prevDue,
          totalPayable: totalPayable + advanceUsed,
          paidAmount: advanceUsed,
          dueAmount: totalPayable,
          status: totalPayable <= 0 ? "Paid" : (advanceUsed > 0 ? "Partially Paid" : "Unpaid"),
          generatedDate: todayISO
        });

        await updateDoc(doc(db, "customers", cust.id), {
          currentDue: totalPayable,
          advanceBalance: remainingAdvance
        });

        await addDoc(collection(db, "ledger_entries"), {
          customerId: cust.id,
          date: todayISO,
          time: timeStr,
          type: "Monthly Bill",
          amount: billAmt,
          isDebit: true,
          description: `Auto-Bill ${currentMonth} (Adv: ${advanceUsed})`
        });
      }
    } catch (e) {
      console.error("Auto-Billing Error:", e);
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <div className="max-w-7xl mx-auto space-y-12 pb-20 uppercase font-black tracking-tighter">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-6 bg-white dark:bg-slate-800 p-10 rounded-[48px] shadow-2xl border border-slate-100 dark:border-slate-700">
        <div className="space-y-2 uppercase">
          <h3 className="text-6xl font-black text-slate-800 dark:text-white tracking-tighter uppercase leading-none">Billing Engine</h3>
          <p className="text-[10px] text-teal-600 font-bold tracking-[5px] uppercase mt-2">Fully Automated Background Billing Active</p>
        </div>
        <div className="bg-emerald-50 text-emerald-600 px-10 py-5 rounded-full text-[11px] font-black border-2 border-emerald-100 shadow-sm flex items-center tracking-[4px]">
           <i className="fas fa-robot mr-3 text-lg animate-bounce"></i> {isProcessing ? 'SYNCING BILLS...' : 'SYSTEM AUTO-SYNC'}
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-10">
        {store.invoices.length > 0 ? store.invoices.map(i => (
          <div key={i.id} className="bg-white dark:bg-slate-800 p-10 rounded-[56px] border-2 border-slate-50 dark:border-slate-700 shadow-xl space-y-8 group hover:scale-[1.02] transition-all relative overflow-hidden">
            <div className="flex justify-between items-start relative z-10">
              <div>
                <p className="text-[11px] font-black text-teal-600 tracking-widest">#{i.invoiceNo}</p>
                <h4 className="text-3xl font-black text-slate-800 dark:text-white tracking-tighter leading-none mt-2 uppercase">{i.customerName}</h4>
                <p className="text-[10px] text-slate-400 font-bold mt-2 tracking-[3px]">{i.billingMonthYear}</p>
              </div>
              <span className={`px-6 py-2.5 rounded-full text-[10px] font-black uppercase shadow-sm ${i.status === 'Paid' ? 'bg-emerald-100 text-emerald-700' : 'bg-rose-100 text-rose-700'}`}>
                {i.status}
              </span>
            </div>

            <div className="grid grid-cols-2 gap-6 relative z-10">
               <div className="bg-slate-50 dark:bg-slate-900 p-6 rounded-[32px] text-center shadow-inner">
                  <p className="text-[9px] text-slate-400 font-black mb-2 tracking-widest uppercase">Bill Amount</p>
                  <p className="text-3xl font-black text-slate-800 dark:text-white leading-none tracking-tighter">৳ {i.billAmount}</p>
               </div>
               <div className="bg-slate-50 dark:bg-slate-900 p-6 rounded-[32px] text-center shadow-inner">
                  <p className="text-[9px] text-slate-400 font-black mb-2 tracking-widest uppercase">Final Due</p>
                  <p className="text-3xl font-black text-rose-500 leading-none tracking-tighter">৳ {Math.floor(i.dueAmount)}</p>
               </div>
            </div>

            <button className="w-full bg-slate-100 dark:bg-slate-900 text-slate-500 py-6 rounded-[32px] font-black uppercase text-[10px] tracking-[5px] hover:bg-teal-600 hover:text-white transition-all shadow-sm">
              Launch Invoice Preview
            </button>
          </div>
        )) : (
          <div className="col-span-full text-center py-40 opacity-10 uppercase flex flex-col items-center">
             <i className="fas fa-file-invoice-dollar text-[120px] mb-8"></i>
             <h3 className="text-4xl font-black tracking-tighter">No Active Invoices</h3>
             <p className="text-sm font-black mt-4 tracking-[10px]">Bills will auto-generate on the 1st day</p>
          </div>
        )}
      </div>
    </div>
  );
};

export default Billing;
