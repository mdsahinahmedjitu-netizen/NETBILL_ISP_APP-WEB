import React, { useState, useEffect } from 'react';
import { supabase } from '../supabaseClient';

const Billing = ({ store, t, setActivePage }) => {
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
    if (isProcessing) return;
    setIsProcessing(true);

    try {
      const activeCustomers = store.customers.filter(c => c.status === 'Active');
      const todayISO = new Date().toLocaleDateString('en-CA');
      const timeStr = new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });

      let count = 0;

      for (const cust of activeCustomers) {
        // Check if invoice already exists for this month to avoid duplicates
        const { data: existingInv } = await supabase
            .from('invoices')
            .select('id')
            .eq('customer_id', cust.id)
            .eq('billing_month_year', currentMonth)
            .maybeSingle();

        if (existingInv) continue;

        const billAmt = parseFloat(cust.monthlyBill) || 0;
        const prevDue = parseFloat(cust.currentDue || cust.current_due || 0);
        let totalPayable = billAmt + prevDue;

        let advanceUsed = 0;
        let remainingAdvance = parseFloat(cust.advanceBalance || cust.advance_balance || 0);

        if (remainingAdvance > 0) {
            advanceUsed = Math.min(remainingAdvance, totalPayable);
            totalPayable -= advanceUsed;
            remainingAdvance -= advanceUsed;
        }

        const invNo = "INV-" + Math.random().toString(36).substr(2, 8).toUpperCase();

        const { error: invErr } = await supabase.from('invoices').insert({
          invoice_no: invNo,
          customer_id: cust.id,
          customer_name: cust.name,
          billing_month_year: currentMonth,
          bill_amount: billAmt,
          total_payable: billAmt + prevDue, // Total including previous due
          due_amount: totalPayable,
          status: totalPayable <= 0 ? "Paid" : (advanceUsed > 0 ? "Partially Paid" : "Unpaid"),
          generated_date: todayISO
        });

        if (invErr) continue;

        await supabase.from('customers').update({
          current_due: totalPayable,
          advance_balance: remainingAdvance,
          payment_status: totalPayable <= 0 ? 'Paid' : 'Unpaid'
        }).eq('id', cust.id);

        await supabase.from('ledger_entries').insert({
          customer_id: cust.id,
          date: todayISO,
          time: timeStr,
          type: "Monthly Bill",
          amount: billAmt,
          is_debit: true,
          description: `Auto-Bill ${currentMonth} (Adv: ${advanceUsed})`,
          monthly_rent: billAmt,
          total_due_balance: totalPayable
        });

        count++;
      }
      if (count > 0) alert(`${count} Invoices generated for ${currentMonth}`);
    } catch (e) {
      console.error("Auto-Billing Error:", e);
      alert("Billing process failed!");
    } finally {
      setIsProcessing(false);
    }
  };


  return (
    <div className="max-w-7xl mx-auto space-y-6 md:space-y-12 pb-20 uppercase font-black tracking-tighter transition-all px-2 md:px-0">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 bg-white dark:bg-slate-800 p-6 md:p-10 rounded-[32px] md:rounded-[48px] shadow-2xl border border-slate-100 dark:border-slate-700">
        <div className="flex items-center space-x-4 md:space-x-6">
           <button onClick={() => setActivePage('dashboard')} className="w-10 h-10 md:w-16 md:h-16 bg-slate-50 dark:bg-slate-900 rounded-2xl md:rounded-3xl flex items-center justify-center text-teal-600 hover:bg-teal-600 hover:text-white transition-all shadow-sm">
              <i className="fas fa-arrow-left"></i>
           </button>
           <div className="space-y-1 md:space-y-2 uppercase">
              <h3 className="text-2xl md:text-6xl font-black text-slate-800 dark:text-white tracking-tighter uppercase leading-none">Billing Engine</h3>
              <p className="text-[8px] md:text-[10px] text-teal-600 font-bold tracking-[3px] md:tracking-[5px] uppercase">Fully Automated Background Billing Active</p>
           </div>
        </div>
        <div className="flex items-center space-x-4">
            <button
                onClick={triggerSilentBilling}
                disabled={isProcessing}
                className="bg-teal-600 text-white px-6 py-3 md:px-8 md:py-4 rounded-2xl font-black text-[10px] md:text-xs tracking-widest shadow-lg hover:scale-105 active:scale-95 transition-all"
            >
                {isProcessing ? 'PROCESSING...' : 'GENERATE ALL BILLS'}
            </button>
            <div className="bg-emerald-50 text-emerald-600 px-6 py-3 md:px-10 md:py-5 rounded-full text-[9px] md:text-[11px] font-black border-2 border-emerald-100 shadow-sm flex items-center tracking-widest whitespace-nowrap">
                <i className="fas fa-robot mr-2 md:mr-3 text-sm md:text-lg animate-bounce"></i> AUTO-SYNC
            </div>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 md:gap-10">
        {store.invoices.length > 0 ? store.invoices.map(i => (
          <div key={i.id} className="bg-white dark:bg-slate-800 p-6 md:p-10 rounded-[32px] md:rounded-[56px] border-2 border-slate-50 dark:border-slate-700 shadow-xl space-y-6 md:space-y-8 group hover:scale-[1.02] transition-all relative overflow-hidden">
            <div className="flex justify-between items-start relative z-10">
              <div>
                <p className="text-[9px] md:text-[11px] font-black text-teal-600 tracking-widest leading-none">#{i.invoiceNo}</p>
                <h4 className="text-xl md:text-3xl font-black text-slate-800 dark:text-white tracking-tighter leading-tight mt-1 md:mt-2 uppercase">{i.customerName}</h4>
                <p className="text-[8px] md:text-[10px] text-slate-400 font-bold mt-1 md:mt-2 tracking-[2px] md:tracking-[3px]">{i.billingMonthYear}</p>
              </div>
              <span className={`px-4 py-1.5 md:px-6 md:py-2.5 rounded-full text-[8px] md:text-[10px] font-black uppercase shadow-sm ${i.status === 'Paid' ? 'bg-emerald-100 text-emerald-700' : 'bg-rose-100 text-rose-700'}`}>
                {i.status}
              </span>
            </div>

            <div className="grid grid-cols-2 gap-4 md:gap-6 relative z-10">
               <div className="bg-slate-50 dark:bg-slate-900 p-4 md:p-6 rounded-2xl md:rounded-[32px] text-center shadow-inner">
                  <p className="text-[8px] md:text-[9px] text-slate-400 font-black mb-1 md:mb-2 tracking-widest uppercase leading-none">Bill</p>
                  <p className="text-xl md:text-3xl font-black text-slate-800 dark:text-white leading-none tracking-tighter">৳{i.billAmount}</p>
               </div>
               <div className="bg-slate-50 dark:bg-slate-900 p-4 md:p-6 rounded-2xl md:rounded-[32px] text-center shadow-inner">
                  <p className="text-[8px] md:text-[9px] text-slate-400 font-black mb-1 md:mb-2 tracking-widest uppercase leading-none">Due</p>
                  <p className="text-xl md:text-3xl font-black text-rose-500 leading-none tracking-tighter">৳{Math.floor(i.dueAmount)}</p>
               </div>
            </div>

            <button className="w-full bg-slate-100 dark:bg-slate-950 text-slate-500 py-5 md:py-6 rounded-[24px] md:rounded-[32px] font-black uppercase text-[9px] md:text-[10px] tracking-[4px] md:tracking-[5px] hover:bg-teal-600 hover:text-white transition-all shadow-sm">
              Invoice Preview
            </button>
          </div>
        )) : (
          <div className="col-span-full text-center py-24 md:py-40 opacity-10 uppercase flex flex-col items-center">
             <i className="fas fa-file-invoice-dollar text-8xl md:text-[120px] mb-6 md:mb-8"></i>
             <h3 className="text-2xl md:text-4xl font-black tracking-tighter">No Active Invoices</h3>
             <p className="text-[10px] md:text-sm font-black mt-2 md:mt-4 tracking-[6px] md:tracking-[10px]">Monthly auto-billing active</p>
          </div>
        )}
      </div>
    </div>
  );
};

export default Billing;
