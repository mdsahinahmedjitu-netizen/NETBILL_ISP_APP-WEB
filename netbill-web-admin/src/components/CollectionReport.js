import React, { useState, useMemo } from 'react';
import { supabase } from '../supabaseClient';

const CollectionReport = ({ store, session, t, initialTab = 'collection' }) => {
  const [activeTab, setActiveTab] = useState(initialTab); // 'collection', 'due', 'revenue'
  const [search, setSearch] = useState('');
  const [startDate, setDateStart] = useState(new Date(new Date().getFullYear(), new Date().getMonth(), 1).toLocaleDateString('en-CA'));
  const [endDate, setDateEnd] = useState(new Date().toLocaleDateString('en-CA'));

  const isStaff = session?.role === 'staff';
  const [selectedStaff, setSelectedStaff] = useState(isStaff ? session.data.name : t.coll_all_collectors);
  const [selectedMethod, setSelectedMethod] = useState(t.coll_all_methods);

  const [printableColumns, setPrintableColumns] = useState({
    sl: true, id: true, customer: true, address: true, method: true, date: true, collector: true, amount: true
  });
  const [showColumnSelector, setShowColumnSelector] = useState(false);

  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [paymentToDelete, setPaymentToDelete] = useState(null);
  const [isDeleting, setIsDeleting] = useState(false);
  const [isExporting, setIsExporting] = useState(false);

  const [showCollectorModal, setShowCollectorModal] = useState(false);
  const [targetCustomer, setTargetCustomer] = useState(null);
  const [targetPayment, setTargetPayment] = useState(null);
  const [newCollector, setNewCollector] = useState('');
  const [isUpdatingCollector, setIsUpdatingCollector] = useState(false);

  const filteredPayments = useMemo(() => {
    return store.payments?.filter(p => {
      const pDate = p.paymentDate || p.payment_date;
      const dateMatch = pDate >= startDate && pDate <= endDate;
      let staffMatch = isStaff ? (p.collectedBy === session.data.name || p.collectedById === session.data.id) : (selectedStaff === t.coll_all_collectors || p.collectedBy === selectedStaff || p.collectedById === selectedStaff);
      const methodMatch = selectedMethod === t.coll_all_methods || p.paymentMethod?.includes(selectedMethod) || p.payment_method?.includes(selectedMethod);
      const customer = store.customers.find(c => c.id === p.customerId || c.id === p.customer_id || c.customerCode === p.customerCode);
      const searchMatch = !search || p.customerName?.toLowerCase().includes(search.toLowerCase()) || p.customerCode?.includes(search) || p.transactionId?.includes(search) || customer?.pppoeUsername?.toLowerCase().includes(search.toLowerCase());
      return dateMatch && staffMatch && methodMatch && searchMatch;
    }).sort((a, b) => new Date(b.paymentDate || b.payment_date) - new Date(a.paymentDate || a.payment_date));
  }, [store.payments, store.customers, startDate, endDate, selectedStaff, selectedMethod, search, isStaff, session?.data]);

  const totalAmount = filteredPayments.reduce((sum, p) => sum + (p.amount || 0), 0);

  const accessibleCustomers = useMemo(() => {
    if (isStaff) return store.customers.filter(c => (c.assignedStaffId || c.assigned_staff_id) === session.data.id || (c.assignedStaffId || c.assigned_staff_id) === session.data.name);
    return store.customers;
  }, [store.customers, isStaff, session?.data]);

  const totalDue = accessibleCustomers.reduce((sum, c) => sum + (parseFloat(c.currentDue || c.current_due) || 0), 0);
  const totalBill = accessibleCustomers.reduce((sum, c) => sum + (parseFloat(c.monthlyBill || c.monthly_bill) || 0), 0);

  const staffStats = useMemo(() => {
    const stats = {};
    filteredPayments.forEach(p => {
      const name = p.collectedBy || t.coll_admin_direct;
      if (!stats[name]) stats[name] = { count: 0, amount: 0 };
      stats[name].count++;
      stats[name].amount += p.amount;
    });
    return Object.entries(stats);
  }, [filteredPayments, t.coll_admin_direct]);

  const filteredDueCustomers = useMemo(() => {
    return store.customers?.filter(c => {
      const hasDue = parseFloat(c.currentDue || c.current_due) > 0;
      let staffMatch = false;
      const sid = c.assignedStaffId || c.assigned_staff_id;
      if (isStaff) staffMatch = sid === session.data.id || sid === session.data.name;
      else staffMatch = selectedStaff === t.coll_all_collectors || sid === selectedStaff || store.staff?.find(s => s.name === selectedStaff)?.id === sid;
      return hasDue && staffMatch;
    }).sort((a,b) => (b.currentDue || b.current_due) - (a.currentDue || a.current_due));
  }, [store.customers, store.staff, selectedStaff, isStaff, session?.data, t.coll_all_collectors]);

  const totalDueFiltered = filteredDueCustomers.reduce((sum, c) => sum + (parseFloat(c.currentDue || c.current_due) || 0), 0);

  const handleUpdateCollector = async () => {
    if (!targetCustomer || !newCollector) return;
    setIsUpdatingCollector(true);
    try {
      if (targetPayment) {
        const { error } = await supabase.from('payments').update({ collected_by: newCollector }).eq('id', targetPayment.id);
        if (error) throw error;
      } else {
        const { error } = await supabase.from('customers').update({ assigned_staff_id: newCollector }).eq('id', targetCustomer.id);
        if (error) throw error;
      }
      alert("Updated Successfully!");
      setShowCollectorModal(false);
      setTargetPayment(null);
    } catch (e) { alert("Failed!"); }
    finally { setIsUpdatingCollector(false); }
  };

  const confirmDelete = async () => {
    if (!paymentToDelete) return;
    setIsDeleting(true);
    try {
      const customer = store.customers.find(c => c.id === paymentToDelete.customer_id || c.id === paymentToDelete.customerId);
      if (customer) {
        const pmtAmt = parseFloat(paymentToDelete.amount) || 0;
        let currentA = parseFloat(customer.advance_balance || customer.advanceBalance || 0);
        let currentD = parseFloat(customer.current_due || customer.currentDue || 0);
        let newA = currentA >= pmtAmt ? currentA - pmtAmt : 0;
        let newD = currentA >= pmtAmt ? currentD : currentD + (pmtAmt - currentA);
        await supabase.from('customers').update({ current_due: newD, advance_balance: newA }).eq('id', customer.id);
      }
      await supabase.from('payments').delete().eq('id', paymentToDelete.id);
      await supabase.from('ledger_entries').delete().eq('reference_no', paymentToDelete.receipt_no || paymentToDelete.receiptNo);
      setShowDeleteModal(false);
      setPaymentToDelete(null);
      alert("Record Removed!");
    } catch (error) { alert("Delete Failed!"); }
    finally { setIsDeleting(false); }
  };

  const handlePrint = () => {
    const printWindow = window.open('', '_blank');
    const today = new Date().toLocaleDateString();
    const tableRows = filteredPayments.map((p) => {
      const customer = store.customers.find(c => c.id === p.customerId || c.id === p.customer_id || c.customerCode === p.customerCode);
      const displayName = customer?.name || p.customerName || '---';
      const displayAddress = customer?.address || customer?.zone || '---';
      return `<tr><td>${displayName}</td><td>${displayAddress}</td><td>${p.paymentDate || p.payment_date}</td><td>${p.collectedBy}</td><td align="right">৳${(p.amount || 0).toLocaleString()}</td></tr>`;
    }).join('');
    const printHtml = `<html><head><title>Collection Report</title><style>body { font-family: sans-serif; padding: 20px; } .header { text-align: center; margin-bottom: 30px; } .summary { display: flex; gap: 20px; justify-content: center; margin-bottom: 30px; } .box { border: 1px solid #eee; padding: 15px; border-radius: 10px; text-align: center; } table { width: 100%; border-collapse: collapse; } th, td { border: 1px solid #eee; padding: 12px 8px; text-align: left; font-size: 12px; } th { background: #f9f9f9; }</style></head><body><div class="header"><h1>NETBILL ISP - COLLECTION REPORT</h1><p>Date Range: ${startDate} to ${endDate}</p></div><div class="summary"><div class="box"><b>${t.coll_total_entries}</b><br/>${filteredPayments.length}</div><div class="box"><b>${t.coll_total_revenue}</b><br/>৳${totalAmount.toLocaleString('en-US')}</div></div><table><thead><tr><th>${t.customer}</th><th>${t.coll_address_zone}</th><th>${t.coll_date}</th><th>${t.coll_collector}</th><th align="right">${t.coll_amount}</th></tr></thead><tbody>${tableRows}</tbody></table></body></html>`;
    printWindow.document.write(printHtml);
    printWindow.document.close();
    printWindow.print();
  };

  return (
    <div className="w-full space-y-6 pb-20 font-sans tracking-tight uppercase">
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCardSmall label={t.coll_total_customers} value={store.customers.length} icon="fa-users" color="text-indigo-600" />
        <StatCardSmall label={t.coll_collected} value={`৳${totalAmount.toLocaleString('en-US')}`} icon="fa-money-bill-trend-up" color="text-emerald-600" />
        <StatCardSmall label={t.coll_due} value={`৳${Math.floor(totalDue).toLocaleString('en-US')}`} icon="fa-triangle-exclamation" color="text-rose-500" />
        <StatCardSmall label={t.coll_total_bill} value={`৳${Math.floor(totalBill).toLocaleString('en-US')}`} icon="fa-receipt" color="text-blue-600" />
      </div>

      <div className="bg-white dark:bg-slate-800 p-5 rounded-[32px] shadow-lg border border-slate-100 dark:border-slate-700 flex flex-wrap items-center gap-4">
        <input type="text" placeholder={t.coll_search} value={search} onChange={(e) => setSearch(e.target.value)} className="flex-1 min-w-[200px] p-3 bg-slate-50 dark:bg-slate-900 rounded-xl outline-none font-black text-sm" />
        <div className="flex items-center space-x-3 bg-slate-50 dark:bg-slate-900 px-5 py-3 rounded-xl border">
           <input type="date" value={startDate} onChange={e => setDateStart(e.target.value)} className="bg-transparent border-none text-xs font-black outline-none" />
           <span className="text-slate-300">/</span>
           <input type="date" value={endDate} onChange={e => setDateEnd(e.target.value)} className="bg-transparent border-none text-xs font-black outline-none" />
        </div>
        <select value={selectedStaff} onChange={e => setSelectedStaff(e.target.value)} disabled={isStaff} className="bg-indigo-50 dark:bg-indigo-900/20 text-indigo-600 px-5 py-3 rounded-xl border border-indigo-100 text-xs font-black outline-none">
          <option value={t.coll_all_collectors}>{t.coll_all_collectors}</option>
          <option value={t.coll_admin_direct}>{t.coll_admin_direct}</option>
          {store.staff?.map(s => <option key={s.id} value={s.name}>{s.name}</option>)}
        </select>
        <select value={selectedMethod} onChange={e => setSelectedMethod(e.target.value)} className="bg-emerald-50 dark:bg-emerald-900/20 text-emerald-600 px-5 py-3 rounded-xl border border-emerald-100 text-xs font-black outline-none">
          <option value={t.coll_all_methods}>{t.coll_all_methods}</option>
          <option value="Cash">Cash</option><option value="bKash">bKash</option><option value="Nagad">Nagad</option><option value="Bank">Bank</option>
        </select>
      </div>

      <div className="flex items-center space-x-2 bg-slate-100 dark:bg-slate-900 p-1.5 rounded-[24px] w-fit shadow-inner overflow-x-auto max-w-full">
         <button onClick={() => setActiveTab('collection')} className={`px-8 py-2.5 rounded-xl text-[10px] font-black shadow-md transition-all shrink-0 ${activeTab === 'collection' ? 'bg-white dark:bg-slate-800 text-teal-600 shadow' : 'text-slate-400'}`}>{t.coll_tab_collection}</button>
         <button onClick={() => setActiveTab('due')} className={`px-8 py-2.5 rounded-xl text-[10px] font-black shadow-md transition-all shrink-0 ${activeTab === 'due' ? 'bg-white dark:bg-slate-800 text-rose-500 shadow' : 'text-slate-400'}`}>{t.coll_tab_due_list}</button>
         <button onClick={() => setActiveTab('revenue')} className={`px-8 py-2.5 rounded-xl text-[10px] font-black shadow-md transition-all shrink-0 ${activeTab === 'revenue' ? 'bg-white dark:bg-slate-800 text-blue-600 shadow' : 'text-slate-400'}`}>{t.coll_tab_revenue}</button>
      </div>

      <div className="bg-white dark:bg-slate-800 rounded-[40px] shadow-2xl border border-slate-100 dark:border-slate-700 p-6 md:p-10 min-h-[600px] relative overflow-hidden">
        <div className={`absolute top-0 left-0 w-full h-2 bg-gradient-to-r ${activeTab === 'collection' ? 'from-teal-500 to-rose-500' : activeTab === 'due' ? 'from-rose-500 to-orange-500' : 'from-blue-500 to-indigo-500'}`}></div>

        <div className="flex justify-between items-center mb-8">
           <h3 className="text-xl md:text-2xl font-black uppercase tracking-tighter leading-none">{activeTab === 'collection' ? t.coll_analysis : activeTab === 'due' ? t.coll_subscriber_due : t.coll_revenue_summary}</h3>
           <button onClick={handlePrint} className="bg-indigo-600 text-white px-8 py-3 rounded-2xl text-[10px] font-black shadow-lg">{t.coll_print_report}</button>
        </div>

        {activeTab === 'collection' && (
           <div className="space-y-8">
              <div className="grid grid-cols-1 xl:grid-cols-2 gap-8 items-end border-b pb-8">
                 <div className="flex items-center space-x-6">
                    <div className="bg-slate-50 dark:bg-slate-900 px-6 py-4 rounded-3xl border shadow-inner">
                       <p className="text-[9px] text-slate-400 font-black mb-1">{t.coll_total_entries}</p>
                       <p className="text-3xl font-black text-slate-800 dark:text-white">{filteredPayments.length}</p>
                    </div>
                    <div className="bg-emerald-50 dark:bg-emerald-900/20 px-6 py-4 rounded-3xl border border-emerald-100 shadow-inner">
                       <p className="text-[9px] text-emerald-600/60 font-black mb-1">{t.coll_total_revenue}</p>
                       <p className="text-3xl font-black text-emerald-600">৳{totalAmount.toLocaleString('en-US')}</p>
                    </div>
                 </div>
                 <div className="space-y-3">
                    <p className="text-[10px] text-slate-400 font-black uppercase tracking-[4px]">{t.coll_collector_summary}</p>
                    <div className="flex flex-wrap gap-2">
                       {staffStats.map(([name, data], idx) => (
                         <div key={name} className="px-4 py-2 bg-indigo-50 text-indigo-600 rounded-xl border-2 border-indigo-100 flex items-center space-x-3 shadow-sm">
                            <span className="text-[11px] font-black uppercase">{name === 'Admin / Direct' ? t.coll_admin_direct : name}</span>
                            <span className="text-[11px] font-black">৳{data.amount.toLocaleString('en-US')}</span>
                         </div>
                       ))}
                    </div>
                 </div>
              </div>

              <div className="overflow-x-auto">
                 <table className="w-full text-left">
                    <thead>
                       <tr className="text-[11px] text-slate-400 border-b-2 font-black uppercase"><th>#</th><th>{t.coll_cust_id}</th><th>{t.coll_subscriber_details}</th><th>{t.coll_address_zone}</th><th>{t.coll_pay_method}</th><th>{t.coll_date}</th><th>{t.coll_collector}</th><th className="text-right">{t.coll_amount}</th><th className="text-center">{t.coll_action}</th></tr>
                    </thead>
                    <tbody className="divide-y">
                       {filteredPayments.map((p, idx) => {
                          const customer = store.customers.find(c => c.id === p.customerId || c.id === p.customer_id || c.customerCode === p.customerCode);
                          return (
                            <tr key={p.id} className="hover:bg-slate-50 transition-all">
                               <td className="py-6 text-xs font-black">{idx + 1}</td>
                               <td className="py-6 text-sm font-black text-indigo-600">{p.customerCode || customer?.customerCode || '---'}</td>
                               <td className="py-6"><p className="text-base font-black text-slate-800 dark:text-white leading-none uppercase">{customer?.name || p.customerName}</p><p className="text-[10px] text-slate-400 font-bold mt-1 uppercase italic">{customer?.pppoeUsername || '---'}</p></td>
                               <td className="py-6 text-sm text-slate-600 font-black uppercase">{customer?.address || customer?.zone || '---'}</td>
                               <td className="py-6"><span className="px-4 py-1.5 bg-slate-100 rounded-xl text-[9px] font-black uppercase">{p.paymentMethod || 'Cash'}</span></td>
                               <td className="py-6 text-xs font-black">{p.paymentDate || p.payment_date}</td>
                               <td className="py-6 text-xs font-black uppercase italic"><span onClick={() => { if(customer) { setTargetCustomer(customer); setTargetPayment(p); setNewCollector(p.collectedBy || ''); setShowCollectorModal(true); } }} className="bg-slate-100 px-3 py-1 rounded-lg cursor-pointer hover:bg-indigo-600 hover:text-white transition-all">{p.collectedBy || t.coll_admin_direct}</span></td>
                               <td className="py-6 text-right font-black text-emerald-600 text-xl">৳{p.amount}</td>
                               <td className="py-6 text-center"><button onClick={() => { setPaymentToDelete(p); setShowDeleteModal(true); }} className="text-rose-400 hover:text-rose-600"><i className="fas fa-trash-alt"></i></button></td>
                            </tr>
                          );
                       })}
                    </tbody>
                 </table>
              </div>
           </div>
        )}

        {activeTab === 'due' && (
           <div className="overflow-x-auto">
              <table className="w-full text-left">
                 <thead>
                    <tr className="text-[11px] text-slate-400 border-b-2 font-black uppercase"><th>{t.customer}</th><th>{t.zone}</th><th>{t.coll_collector}</th><th className="text-right">{t.coll_bill}</th><th className="text-right">{t.coll_due}</th></tr>
                 </thead>
                 <tbody className="divide-y">
                    {filteredDueCustomers.map(c => (
                       <tr key={c.id} className="hover:bg-rose-50/20 transition-all">
                          <td className="py-5 font-black uppercase">{c.name} <span className="block text-[10px] text-slate-400">#{c.customerCode}</span></td>
                          <td className="py-5 text-sm font-black text-indigo-600">{c.zone || 'Global'}</td>
                          <td className="py-5 text-sm font-black">
                             <span onClick={() => { setTargetCustomer(c); setTargetPayment(null); setNewCollector(c.assignedStaffId || ''); setShowCollectorModal(true); }} className="bg-slate-100 px-3 py-1 rounded-lg cursor-pointer hover:bg-indigo-600 hover:text-white transition-all border border-slate-200">{store.staff?.find(s => s.id === (c.assignedStaffId || c.assigned_staff_id) || s.name === (c.assignedStaffId || c.assigned_staff_id))?.name || c.assignedStaffId || c.assigned_staff_id || '---'}</span>
                          </td>
                          <td className="py-5 text-right font-black text-slate-700 text-lg">৳{Math.floor(c.monthlyBill || 0)}</td>
                          <td className="py-5 text-right font-black text-rose-500 text-xl">৳{Math.floor(c.currentDue || c.current_due)}</td>
                       </tr>
                    ))}
                 </tbody>
              </table>
           </div>
        )}
      </div>

      {showCollectorModal && (
        <div className="fixed inset-0 bg-slate-900/90 backdrop-blur-xl z-[7000] flex items-center justify-center p-6 font-black uppercase">
          <div className="bg-white dark:bg-slate-800 rounded-[56px] w-full max-w-md p-12 shadow-2xl border-4 border-indigo-500/20 space-y-8">
             <h3 className="text-3xl font-black uppercase">{t.coll_change_collector}</h3>
             <select value={newCollector} onChange={e => setNewCollector(e.target.value)} className="w-full bg-slate-50 p-5 rounded-3xl font-black outline-none border-none shadow-inner">
                <option value="">{t.coll_no_collector}</option>
                {store.staff?.map(s => <option key={s.id} value={s.name}>{s.name}</option>)}
             </select>
             <button onClick={handleUpdateCollector} disabled={isUpdatingCollector} className="w-full bg-indigo-600 text-white py-6 rounded-3xl font-black">{t.coll_confirm_change}</button>
             <button onClick={() => setShowCollectorModal(false)} className="w-full text-rose-500 font-black">{t.coll_cancel}</button>
          </div>
        </div>
      )}

      {showDeleteModal && (
        <div className="fixed inset-0 bg-slate-900/90 backdrop-blur-xl z-[5000] flex items-center justify-center p-6 font-black uppercase">
          <div className="bg-white dark:bg-slate-800 rounded-[56px] w-full max-w-md p-12 shadow-2xl text-center space-y-8">
             <h3 className="text-3xl font-black">{t.coll_confirm_delete}</h3>
             <div className="flex space-x-4">
                <button onClick={() => setShowDeleteModal(false)} className="flex-1 bg-slate-100 py-5 rounded-2xl font-black text-slate-500">{t.coll_cancel}</button>
                <button onClick={confirmDelete} disabled={isDeleting} className="flex-1 bg-rose-600 text-white py-5 rounded-2xl font-black">{t.coll_delete}</button>
             </div>
          </div>
        </div>
      )}
    </div>
  );
};

const StatCardSmall = ({ label, value, icon, color }) => (
  <div className="bg-white dark:bg-slate-800 p-6 rounded-2xl border shadow-sm flex flex-col space-y-2 relative overflow-hidden">
     <div className="absolute top-4 right-4 text-slate-100 dark:text-slate-700 text-3xl"><i className={`fas ${icon}`}></i></div>
     <p className="text-[10px] font-black text-slate-400 uppercase">{label}</p>
     <p className={`text-2xl font-black ${color}`}>{value}</p>
  </div>
);

export default CollectionReport;
