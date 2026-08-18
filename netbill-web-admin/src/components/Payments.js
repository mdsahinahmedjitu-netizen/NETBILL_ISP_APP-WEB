import React, { useState, useMemo, useEffect } from 'react';
import { supabase } from '../supabaseClient';

const Payments = ({ store, session, t }) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedCustomerId, setSelectedCustomerId] = useState('');
  const [amount, setAmount] = useState('');
  const [method, setMethod] = useState('Cash');
  const [billingMonth, setBillingMonth] = useState(new Date().toLocaleString('default', { month: 'long', year: 'numeric' }));
  const [isProcessing, setIsProcessing] = useState(false);
  const [customerPayments, setCustomerPayments] = useState([]);
  const [selectedCollector, setSelectedCollector] = useState({
    id: session?.data?.id || 'admin',
    name: session?.data?.name || 'Super Admin'
  });

  const [editingPayment, setEditingPayment] = useState(null);
  const [editAmount, setEditEditAmount] = useState('');
  const [showEditModal, setShowEditModal] = useState(false);

  // Expiry Extension States
  const [showExtensionModal, setShowExtensionModal] = useState(false);
  const [extensionData, setExtensionData] = useState({
    customerId: '', currentExpire: '', nextExpire: '', amount: 0, customerName: '', newDue: 0, newAdvance: 0
  });

  const formatDateDisplay = (dateStr) => {
    if (!dateStr) return '';
    const [year, month, day] = dateStr.split('-');
    return `${day}-${month}-${year}`;
  };

  const months = useMemo(() => {
    const result = [];
    for (let i = 0; i < 6; i++) {
      const d = new Date();
      d.setMonth(d.getMonth() - i);
      result.push(d.toLocaleString('default', { month: 'long', year: 'numeric' }));
    }
    return result;
  }, []);

  useEffect(() => {
    setCustomerPayments([]);
    if (!selectedCustomerId) return;
    const fetchPayments = async () => {
      const { data, error } = await supabase.from('payments').select('*').eq('customer_id', selectedCustomerId).order('payment_date', { ascending: false }).limit(20);
      if (!error && data) {
        setCustomerPayments(data.map(item => {
          const newObj = {};
          Object.keys(item).forEach(key => {
            const camelKey = key.replace(/(_\w)/g, m => m[1].toUpperCase());
            newObj[camelKey] = item[key];
          });
          return newObj;
        }));
      }
    };
    fetchPayments();
    const channel = supabase.channel(`cust-payments-${selectedCustomerId}`).on('postgres_changes', { event: '*', schema: 'public', table: 'payments', filter: `customer_id=eq.${selectedCustomerId}` }, () => fetchPayments()).subscribe();
    return () => supabase.removeChannel(channel);
  }, [selectedCustomerId]);

  const filteredCustomers = store.customers.filter(c =>
    c.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    c.customerCode?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    c.mobile?.includes(searchTerm) ||
    c.pppoeUsername?.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const handlePayment = async (e) => {
    e.preventDefault();
    if (!selectedCustomerId || !amount) return;
    const customer = store.customers.find(c => c.id === selectedCustomerId);
    if (!customer) return;

    setIsProcessing(true);
    const todayISO = new Date().toLocaleDateString('en-CA');

    try {
      const payAmt = parseFloat(amount);
      let newDue = (customer.currentDue || customer.current_due || 0);
      let newAdvance = (customer.advanceBalance || customer.advance_balance || 0);

      if (payAmt > newDue) {
          const excess = payAmt - newDue;
          newAdvance += excess;
          newDue = 0;
      } else {
          newDue -= payAmt;
      }

      const parseAnyDate = (dStr) => {
          if (!dStr) return null;
          if (dStr.includes('-') && dStr.split('-')[0].length === 4) {
              const [y, m, d] = dStr.split('-').map(Number);
              return new Date(y, m - 1, d);
          } else if (dStr.includes('-')) {
              const parts = dStr.split('-');
              const mArr = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
              return new Date(parseInt(parts[2]), mArr.indexOf(parts[1]), parseInt(parts[0]));
          }
          return null;
      };

      let currentExpireDateStr = customer.expireDate || customer.expire_date;
      let currentExpireObj = parseAnyDate(currentExpireDateStr);
      let currentRequestObj = parseAnyDate(customer.requestDate || customer.request_date);
      let now = new Date();

      const isDisconnected = (currentExpireObj && currentExpireObj < now) &&
                            (!currentRequestObj || currentRequestObj < now);

      if (newDue <= 0 && isDisconnected) {
          const nextMonth = new Date();
          nextMonth.setMonth(nextMonth.getMonth() + 1);
          const monthsArr = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
          const defaultNextExpire = `${nextMonth.getDate().toString().padStart(2, '0')}-${monthsArr[nextMonth.getMonth()]}-${nextMonth.getFullYear()}`;

          setExtensionData({
            customerId: selectedCustomerId,
            customerName: customer.name,
            currentExpire: currentExpireDateStr || 'N/A',
            nextExpire: defaultNextExpire,
            amount: payAmt,
            newDue, newAdvance
          });
          setShowExtensionModal(true);
          setIsProcessing(false);
          return;
      }

      let nextExpireDate = currentExpireDateStr;
      if (newDue <= 0) {
          let baseDate = now;
          if (currentExpireObj && (currentExpireObj > now || (currentRequestObj && currentRequestObj > now))) {
              baseDate = currentExpireObj;
          }
          baseDate.setMonth(baseDate.getMonth() + 1);
          const monthsArr = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
          nextExpireDate = `${baseDate.getDate().toString().padStart(2, '0')}-${monthsArr[baseDate.getMonth()]}-${baseDate.getFullYear()}`;
      }

      commitFinalPayment(selectedCustomerId, payAmt, newDue, newAdvance, nextExpireDate);

    } catch (e) { alert("Error processing payment!"); setIsProcessing(false); }
  };

  const commitFinalPayment = async (custId, payAmt, newDue, newAdvance, finalExpireDate) => {
    const customer = store.customers.find(c => c.id === custId);
    const todayISO = new Date().toLocaleDateString('en-CA');
    const timeStr = new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });

    try {
      const { data: newPmt, error: pmtErr } = await supabase.from('payments').insert({
        customer_id: custId, customer_name: customer.name, customer_code: customer.customerCode || customer.customer_code,
        amount: payAmt, payment_method: method, payment_date: todayISO, billing_month: billingMonth,
        receipt_no: `REC-${Date.now().toString().slice(-6)}`, remarks: `${billingMonth} Bill`,
        collected_by: selectedCollector.name, collected_by_id: selectedCollector.id
      }).select().single();

      if (pmtErr) throw pmtErr;

      const updatePayload = { current_due: newDue, advance_balance: newAdvance, payment_status: newDue <= 0 ? 'Paid' : 'Unpaid' };
      if (newDue <= 0 && finalExpireDate) updatePayload.expire_date = finalExpireDate;

      const { error: custUpdateErr } = await supabase.from('customers').update(updatePayload).eq('id', custId);
      if (custUpdateErr) throw custUpdateErr;

      await supabase.from('ledger_entries').insert({
        customer_id: custId, date: todayISO, time: timeStr, type: "Payment",
        description: `Payment for ${billingMonth}`, amount: payAmt, is_debit: false,
        reference_no: newPmt.receipt_no, running_balance: newDue, collector_name: selectedCollector.name,
        paid_amount: payAmt, total_due_balance: newDue
      });

      alert(t.success_payment);
      setAmount(''); setSelectedCustomerId(''); setSearchTerm(''); setShowExtensionModal(false);
    } catch (e) { alert("Finalizing payment failed!"); }
    finally { setIsProcessing(false); }
  };

  const openEditModal = (p) => { setEditingPayment(p); setEditEditAmount(p.amount.toString()); setShowEditModal(true); };

  const handleUpdatePayment = async (e) => {
    e.preventDefault();
    if (!editingPayment || !editAmount) return;
    setIsProcessing(true);
    try {
      const oldAmount = editingPayment.amount;
      const newAmount = parseFloat(editAmount);
      const diff = newAmount - oldAmount;
      const { error: pmtErr } = await supabase.from('payments').update({ amount: newAmount, remarks: (editingPayment.remarks || '') + " (Edited)" }).eq('id', editingPayment.id);
      if (pmtErr) throw pmtErr;
      const customer = store.customers.find(c => c.id === editingPayment.customerId);
      if (customer) {
        const newDue = (customer.currentDue || 0) - diff;
        await supabase.from('customers').update({ current_due: newDue, payment_status: newDue <= 0 ? 'Paid' : 'Unpaid' }).eq('id', customer.id);
        await supabase.from('ledger_entries').update({ amount: newAmount, running_balance: newDue }).eq('reference_no', editingPayment.receiptNo);
      }
      alert("Collection updated!");
      setShowEditModal(false);
    } catch (err) { alert("Failed!"); }
    finally { setIsProcessing(false); }
  };

  return (
    <div className="max-w-6xl mx-auto space-y-4 pb-10 uppercase font-black tracking-tighter transition-all">
      <div className="space-y-0.5"><h3 className="text-2xl font-black text-slate-800 dark:text-white uppercase leading-none">{t.payment_center}</h3><p className="text-[9px] text-teal-600 tracking-[3px] font-black uppercase italic opacity-70">Collection Hub</p></div>
      <div className="grid grid-cols-1 xl:grid-cols-2 gap-6 items-start">
        <div className="bg-white dark:bg-slate-800 p-8 rounded-[48px] shadow-2xl border border-slate-100 dark:border-slate-700 font-black h-fit">
           <div className="mb-6 bg-indigo-50 dark:bg-indigo-900/20 p-5 rounded-3xl border-2 border-indigo-100 flex items-center justify-between">
              <label className="text-[10px] font-black text-indigo-600 uppercase tracking-[2px] ml-2">Collector:</label>
              <select value={selectedCollector.id} onChange={(e) => { const id = e.target.value; const name = e.target.options[e.target.selectedIndex].text; setSelectedCollector({ id, name }); }} className="bg-white dark:bg-slate-800 border-none px-4 py-2 rounded-xl font-black text-[11px] uppercase shadow-sm outline-none cursor-pointer min-w-[200px]">
                <option value={session?.data?.id || 'admin'}>{session?.data?.name || 'Super Admin'} (YOU)</option>
                {store.staff?.filter(s => s.id !== session?.data?.id).map(s => (<option key={s.id} value={s.id}>{s.name}</option>))}
              </select>
           </div>
           <form onSubmit={handlePayment} className="space-y-6">
              <div className="space-y-3">
                <label className="text-[11px] font-black text-slate-400 uppercase tracking-[3px] ml-4">{t.find_subscriber}</label>
                <div className="relative space-y-3">
                  <input type="text" placeholder={t.search_placeholder} value={searchTerm} onChange={(e) => setSearchTerm(e.target.value)} className="w-full bg-slate-50 dark:bg-slate-900 border-none p-5 rounded-[28px] font-black text-xl shadow-inner outline-none" />
                  <select value={selectedCustomerId} size={searchTerm.length > 0 ? 5 : 1} onChange={(e) => { const id = e.target.value; setSelectedCustomerId(id); const cust = store.customers.find(c => c.id === id); if (cust) { setAmount(Math.floor(cust.currentDue || cust.current_due || 0).toString()); setSearchTerm(''); } }} className="w-full bg-white dark:bg-slate-800 border-4 border-teal-500/10 p-4 rounded-[28px] font-black text-lg uppercase shadow-xl outline-none cursor-pointer">
                    <option value="">-- {filteredCustomers.length} Results --</option>
                    {filteredCustomers.map(c => (<option key={c.id} value={c.id}>{c.name} - {c.zone || 'Global'} - DUE: ৳{Math.floor(c.currentDue || c.current_due || 0)}</option>))}
                  </select>
                </div>
              </div>
              <div className="grid grid-cols-2 gap-6">
                 <div className="space-y-3"><label className="text-[11px] font-black text-slate-400 uppercase tracking-[3px] ml-4">{t.billing_month}</label><select value={billingMonth} onChange={(e) => setBillingMonth(e.target.value)} className="w-full bg-slate-50 dark:bg-slate-900 border-none p-4 rounded-2xl font-black uppercase text-xs cursor-pointer shadow-inner">{months.map(m => <option key={m} value={m}>{m}</option>)}</select></div>
                 <div className="space-y-3"><label className="text-[11px] font-black text-slate-400 uppercase tracking-[3px] ml-4">{t.method}</label><select value={method} onChange={(e) => setMethod(e.target.value)} className="w-full bg-slate-50 dark:bg-slate-900 border-none p-4 rounded-2xl font-black uppercase text-xs cursor-pointer shadow-inner"><option value="Cash">Cash</option><option value="bKash">bKash</option><option value="Nagad">Nagad</option><option value="Rocket">Rocket</option><option value="Bank">Bank</option></select></div>
              </div>
              <div className="space-y-3"><label className="text-[11px] text-slate-400 uppercase tracking-[4px] ml-4">ENTER AMOUNT (৳)</label><input type="number" value={amount} onChange={(e) => setAmount(e.target.value)} placeholder="0.00" className="w-full bg-slate-100 dark:bg-slate-900 border-none p-8 rounded-[40px] font-black text-6xl text-teal-600 tracking-tighter shadow-inner text-center" required /></div>
              <button type="submit" disabled={isProcessing} className={`w-full py-8 rounded-[40px] font-black uppercase tracking-[10px] shadow-2xl transition-all ${isProcessing ? 'bg-slate-400' : 'bg-[#0D9488] text-white border-b-8 border-teal-900 shadow-teal-500/30'}`}>{isProcessing ? 'COMMITING...' : 'COMMIT PAYMENT'}</button>
           </form>
        </div>
        <div className="space-y-6 font-black uppercase">
           <div className="flex justify-between items-center ml-4 border-b pb-4"><h4 className="text-xl font-black text-slate-800 dark:text-white uppercase tracking-[4px]">History</h4><span className="bg-slate-100 dark:bg-slate-900 px-4 py-1.5 rounded-full text-[10px] font-black tracking-widest leading-none">LATEST 20</span></div>
           <div className="space-y-4 max-h-[700px] overflow-y-auto pr-2 custom-scrollbar">
              {customerPayments.length > 0 ? customerPayments.map(p => (
                <div key={p.id} className="bg-white dark:bg-slate-800 p-6 rounded-[32px] shadow-xl border border-slate-100 dark:border-slate-700 flex justify-between items-center group transition-all font-black leading-none">
                   <div className="flex items-center space-x-6">
                      <div className="w-14 h-14 rounded-2xl bg-teal-50 dark:bg-slate-900 text-teal-600 flex items-center justify-center text-2xl shadow-inner"><i className={`fas ${p.paymentMethod === 'Cash' ? 'fa-wallet' : p.paymentMethod === 'Bank' ? 'fa-building-columns' : 'fa-mobile-screen-button'}`}></i></div>
                      <div className="space-y-2">
                         <p className="text-xl font-black text-slate-800 dark:text-white uppercase leading-none tracking-tighter">{p.billingMonth}</p>
                         <p className="text-[9px] font-bold text-slate-400 uppercase tracking-widest leading-none">{p.paymentMethod} • REF: {p.receiptNo}</p>
                         <button onClick={() => openEditModal(p)} className="text-[8px] font-black text-indigo-600 underline tracking-[2px] opacity-0 group-hover:opacity-100 transition-all uppercase">Edit</button>
                      </div>
                   </div>
                   <div className="text-right space-y-1 font-black"><p className="text-2xl font-black text-emerald-600 tracking-tighter leading-none">৳ {p.amount}</p><p className="text-[9px] font-black text-slate-400 uppercase tracking-[2px]">{formatDateDisplay(p.paymentDate)}</p></div>
                </div>
              )) : (<div className="text-center py-20 opacity-10 uppercase"><i className="fas fa-hand-holding-dollar text-[80px]"></i><p className="text-lg font-black mt-4 tracking-[10px]">No History</p></div>)}
           </div>
        </div>
      </div>

      {showEditModal && (
        <div className="fixed inset-0 bg-slate-900/95 backdrop-blur-2xl z-[3000] flex items-center justify-center p-6 animate-fadeIn font-black uppercase">
          <div className="bg-white dark:bg-slate-800 rounded-[72px] w-full max-w-xl p-16 shadow-2xl space-y-12 relative overflow-hidden border-2 border-slate-100 dark:border-slate-700">
             <div className="flex justify-between items-center border-b pb-8"><div className="space-y-1"><h3 className="text-4xl font-black uppercase tracking-tighter">Edit Collection</h3><p className="text-[10px] text-indigo-600 font-bold tracking-[4px]">REF: {editingPayment.receiptNo}</p></div><button onClick={() => setShowEditModal(false)} className="w-16 h-16 bg-rose-50 text-rose-500 rounded-full flex items-center justify-center text-2xl"><i className="fas fa-times"></i></button></div>
             <form onSubmit={handleUpdatePayment} className="space-y-8"><div className="space-y-4"><label className="text-[11px] text-slate-400 ml-4 tracking-[5px] font-black uppercase">Corrected Amount (৳)</label><input type="number" value={editAmount} onChange={e => setEditEditAmount(e.target.value)} className="w-full bg-slate-50 dark:bg-slate-900 border-none p-10 rounded-[48px] font-black text-6xl text-indigo-600 tracking-tighter text-center" required /></div><button type="submit" disabled={isProcessing} className="w-full bg-indigo-600 text-white py-8 rounded-[48px] font-black uppercase tracking-[10px] shadow-2xl border-b-8 border-indigo-900">{isProcessing ? 'UPDATING...' : 'UPDATE COLLECTION'}</button></form>
          </div>
        </div>
      )}

      {/* DISCONNECTED CUSTOMER: DATE SELECTION MODAL */}
      {showExtensionModal && (
        <div className="fixed inset-0 bg-slate-900/95 backdrop-blur-2xl z-[5000] flex items-center justify-center p-6 font-black uppercase">
          <div className="bg-white dark:bg-slate-800 rounded-[64px] w-full max-w-xl p-12 shadow-2xl border-4 border-rose-500/20 space-y-10 relative overflow-hidden">
             <div className="absolute top-0 left-0 w-full h-4 bg-rose-600 shadow-lg"></div>
             <div className="text-center space-y-2">
                <div className="w-20 h-20 bg-rose-50 rounded-3xl flex items-center justify-center mx-auto text-4xl text-rose-500 shadow-inner mb-4 animate-pulse"><i className="fas fa-plug-circle-xmark"></i></div>
                <h3 className="text-3xl font-black tracking-tighter">Connection Inactive</h3>
                <p className="text-[10px] text-slate-400 tracking-[3px] font-bold">This subscriber is fully expired</p>
             </div>
             <div className="bg-slate-50 dark:bg-slate-900 p-8 rounded-[40px] space-y-6">
                <div className="flex justify-between items-center border-b border-slate-200/50 pb-4">
                   <div className="space-y-1 text-left"><p className="text-[10px] text-slate-400 tracking-[2px]">CLIENT</p><p className="text-xl font-black text-slate-800 dark:text-white leading-none">{extensionData.customerName}</p></div>
                   <div className="text-right"><p className="text-[10px] text-rose-500 tracking-[2px]">PREVIOUS EXPIRE</p><p className="text-xl font-black text-rose-500 leading-none">{extensionData.currentExpire}</p></div>
                </div>
                <div className="space-y-3">
                   <label className="text-[11px] text-slate-400 ml-4 tracking-[4px] font-black">SET NEXT EXPIRE DATE</label>
                   <input type="text" value={extensionData.nextExpire} onChange={e => setExtensionData({...extensionData, nextExpire: e.target.value})} className="w-full bg-white dark:bg-slate-800 p-6 rounded-3xl font-black text-2xl text-indigo-600 text-center shadow-lg border-2 border-indigo-500/20" />
                   <p className="text-[9px] text-slate-400 text-center italic">* Format: DD-MMM-YYYY (e.g. 20-Sep-2026)</p>
                </div>
             </div>
             <div className="grid grid-cols-1 gap-4">
                <button onClick={() => commitFinalPayment(extensionData.customerId, extensionData.amount, extensionData.newDue, extensionData.newAdvance, extensionData.nextExpire)} className="w-full bg-emerald-600 text-white py-8 rounded-[40px] font-black uppercase tracking-[5px] shadow-2xl hover:scale-[1.02] active:scale-95 transition-all">CONFIRM & ACTIVATE</button>
                <button onClick={() => { setShowExtensionModal(false); setIsProcessing(false); }} className="text-slate-400 text-xs font-black tracking-widest hover:text-rose-500">CANCEL TRANSACTION</button>
             </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Payments;
