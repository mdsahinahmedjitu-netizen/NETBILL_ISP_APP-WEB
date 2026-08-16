import React, { useState, useMemo } from 'react';
import { supabase } from '../supabaseClient';

const CollectionReport = ({ store, session, t }) => {
  const [activeTab, setActiveTab] = useState('collection'); // 'collection', 'due', 'revenue'
  const [search, setSearch] = useState('');
  const [startDate, setDateStart] = useState(new Date(new Date().getFullYear(), new Date().getMonth(), 1).toLocaleDateString('en-CA'));
  const [endDate, setDateEnd] = useState(new Date().toLocaleDateString('en-CA'));

  const isStaff = session?.role === 'staff';
  const [selectedStaff, setSelectedStaff] = useState(isStaff ? session.data.name : 'All Collectors');
  const [selectedMethod, setSelectedMethod] = useState('All Methods');

  // Printable Columns State
  const [printableColumns, setPrintableColumns] = useState({
    sl: true, id: true, customer: true, address: true, method: true, date: true, collector: true, amount: true
  });
  const [showColumnSelector, setShowColumnSelector] = useState(false);

  // Deletion States
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [paymentToDelete, setPaymentToDelete] = useState(null);
  const [isDeleting, setIsDeleting] = useState(false);
  const [isExporting, setIsExporting] = useState(false);

  // New State for Changing Collector
  const [showCollectorModal, setShowCollectorModal] = useState(false);
  const [targetCustomer, setTargetCustomer] = useState(null);
  const [targetPayment, setTargetPayment] = useState(null);
  const [newCollector, setNewCollector] = useState('');
  const [isUpdatingCollector, setIsUpdatingCollector] = useState(false);

  const filteredPayments = useMemo(() => {
    return store.payments?.filter(p => {
      const pDate = p.paymentDate;
      const dateMatch = pDate >= startDate && pDate <= endDate;

      let staffMatch = false;
      if (isStaff) {
        staffMatch = p.collectedBy === session.data.name || p.collectedById === session.data.id;
      } else {
        staffMatch = selectedStaff === 'All Collectors' || p.collectedBy === selectedStaff || p.collectedById === selectedStaff;
      }

      const methodMatch = selectedMethod === 'All Methods' || p.paymentMethod?.includes(selectedMethod);

      const customer = store.customers.find(c => c.id === p.customerId || c.customerCode === p.customerCode);
      const searchMatch = !search ||
        p.customerName?.toLowerCase().includes(search.toLowerCase()) ||
        p.customerCode?.includes(search) ||
        p.transactionId?.includes(search) ||
        customer?.pppoeUsername?.toLowerCase().includes(search.toLowerCase());

      return dateMatch && staffMatch && methodMatch && searchMatch;
    }).sort((a, b) => new Date(b.paymentDate) - new Date(a.paymentDate));
  }, [store.payments, store.customers, startDate, endDate, selectedStaff, selectedMethod, search, isStaff, session?.data]);

  const totalAmount = filteredPayments.reduce((sum, p) => sum + (p.amount || 0), 0);

  const accessibleCustomers = useMemo(() => {
    if (isStaff) {
      return store.customers.filter(c => (c.assignedStaffId || c.assigned_staff_id) === session.data.id || (c.assignedStaffId || c.assigned_staff_id) === session.data.name);
    }
    return store.customers;
  }, [store.customers, isStaff, session?.data]);

  const totalDue = accessibleCustomers.reduce((sum, c) => sum + (parseFloat(c.currentDue || c.current_due) || 0), 0);
  const totalBill = accessibleCustomers.reduce((sum, c) => sum + (parseFloat(c.monthlyBill || c.monthly_bill) || 0), 0);

  const staffStats = useMemo(() => {
    const stats = {};
    filteredPayments.forEach(p => {
      const name = p.collectedBy || 'Admin / Direct';
      if (!stats[name]) stats[name] = { count: 0, amount: 0 };
      stats[name].count++;
      stats[name].amount += p.amount;
    });
    return Object.entries(stats);
  }, [filteredPayments]);

  const filteredDueCustomers = useMemo(() => {
    return store.customers?.filter(c => {
      const hasDue = parseFloat(c.currentDue || c.current_due) > 0;
      let staffMatch = false;
      const sid = c.assignedStaffId || c.assigned_staff_id;
      if (isStaff) {
        staffMatch = sid === session.data.id || sid === session.data.name;
      } else {
        staffMatch = selectedStaff === 'All Collectors' ||
                     sid === selectedStaff ||
                     store.staff?.find(s => s.name === selectedStaff)?.id === sid;
      }
      return hasDue && staffMatch;
    }).sort((a,b) => (b.currentDue || b.current_due) - (a.currentDue || a.current_due));
  }, [store.customers, store.staff, selectedStaff, isStaff, session?.data]);

  const totalDueFiltered = filteredDueCustomers.reduce((sum, c) => sum + (parseFloat(c.currentDue || c.current_due) || 0), 0);

  const handleUpdateCollector = async () => {
    if (!targetCustomer || !newCollector) return;
    setIsUpdatingCollector(true);
    try {
      if (targetPayment) {
        const { error: pmtErr } = await supabase
          .from('payments')
          .update({ collected_by: newCollector })
          .eq('id', targetPayment.id);
        if (pmtErr) throw pmtErr;
      } else {
        const { error: custErr } = await supabase
          .from('customers')
          .update({ assigned_staff_id: newCollector })
          .eq('id', targetCustomer.id);
        if (custErr) throw custErr;
      }

      alert("Collector Updated Successfully!");
      setShowCollectorModal(false);
      setTargetPayment(null);
    } catch (e) {
      console.error(e);
      alert("Update Failed!");
    } finally {
      setIsUpdatingCollector(false);
    }
  };

  const handleDeleteClick = (payment) => {
    setPaymentToDelete(payment);
    setShowDeleteModal(true);
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

        const { error: custErr } = await supabase.from('customers').update({
          current_due: newD, advance_balance: newA
        }).eq('id', customer.id);
        if (custErr) throw custErr;
      }

      await supabase.from('payments').delete().eq('id', paymentToDelete.id);
      await supabase.from('ledger_entries').delete().eq('reference_no', paymentToDelete.receipt_no || paymentToDelete.receiptNo);

      setShowDeleteModal(false);
      setPaymentToDelete(null);
      alert("Payment removed and due adjusted.");
    } catch (error) {
      console.error("Delete Error:", error);
      alert("Failed to delete record.");
    } finally {
      setIsDeleting(false);
    }
  };

  const handlePrint = () => {
    const printWindow = window.open('', '_blank');
    const tableRows = filteredPayments.map((p, idx) => {
      const customer = store.customers.find(c => c.id === p.customerId || c.customerCode === p.customerCode);
      return `<tr><td>${idx+1}</td><td>${p.customerCode||''}</td><td>${p.customerName}</td><td>${customer?.address||''}</td><td>${p.paymentMethod}</td><td>${p.paymentDate}</td><td>${p.collectedBy}</td><td align="right">৳${p.amount}</td></tr>`;
    }).join('');
    printWindow.document.write(`<html><body><h2>Collection Report</h2><table border="1" width="100%" style="border-collapse:collapse"><thead><tr><th>#</th><th>ID</th><th>CUSTOMER</th><th>ADDRESS</th><th>METHOD</th><th>DATE</th><th>COLLECTOR</th><th>AMOUNT</th></tr></thead><tbody>${tableRows}</tbody></table></body></html>`);
    printWindow.document.close();
    printWindow.print();
  };

  return (
    <div className="w-full space-y-6 pb-20 font-sans tracking-tight uppercase">
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCardSmall label="Total Customers" value={store.customers.length} icon="fa-users" color="text-indigo-600" />
        <StatCardSmall label="Collected" value={`৳${totalAmount.toLocaleString()}`} icon="fa-money-bill-trend-up" color="text-emerald-600" />
        <StatCardSmall label="Due" value={`৳${Math.floor(totalDue).toLocaleString()}`} icon="fa-triangle-exclamation" color="text-rose-500" />
        <StatCardSmall label="Total Bill" value={`৳${Math.floor(totalBill).toLocaleString()}`} icon="fa-receipt" color="text-blue-600" />
      </div>

      <div className="bg-white dark:bg-slate-800 p-5 rounded-[32px] shadow-lg border border-slate-100 dark:border-slate-700 flex flex-wrap items-center gap-4">
        <input type="text" placeholder="Search customer, ID or TrxID..." value={search} onChange={(e) => setSearch(e.target.value)} className="flex-1 min-w-[200px] p-3 bg-slate-50 dark:bg-slate-900 rounded-xl outline-none font-black text-sm" />
        <div className="flex items-center space-x-3 bg-slate-50 dark:bg-slate-900 px-5 py-3 rounded-xl border">
           <input type="date" value={startDate} onChange={e => setDateStart(e.target.value)} className="bg-transparent border-none text-xs font-black outline-none" />
           <span className="text-slate-300">/</span>
           <input type="date" value={endDate} onChange={e => setDateEnd(e.target.value)} className="bg-transparent border-none text-xs font-black outline-none" />
        </div>
        <select value={selectedStaff} onChange={e => setSelectedStaff(e.target.value)} disabled={isStaff} className="bg-indigo-50 dark:bg-indigo-900/20 text-indigo-600 px-5 py-3 rounded-xl border border-indigo-100 text-xs font-black outline-none">
          <option>All Collectors</option><option>Admin / Direct</option>
          {store.staff?.map(s => <option key={s.id} value={s.name}>{s.name}</option>)}
        </select>
      </div>

      <div className="flex items-center space-x-2 bg-slate-100 dark:bg-slate-900 p-1.5 rounded-[24px] w-fit shadow-inner overflow-x-auto max-w-full">
         <button onClick={() => setActiveTab('collection')} className={`px-8 py-2.5 rounded-xl text-[10px] font-black shadow-md transition-all shrink-0 ${activeTab === 'collection' ? 'bg-white dark:bg-slate-800 text-teal-600' : 'text-slate-400'}`}>COLLECTION</button>
         <button onClick={() => setActiveTab('due')} className={`px-8 py-2.5 rounded-xl text-[10px] font-black shadow-md transition-all shrink-0 ${activeTab === 'due' ? 'bg-white dark:bg-slate-800 text-rose-500' : 'text-slate-400'}`}>DUE LIST</button>
         <button onClick={() => setActiveTab('revenue')} className={`px-8 py-2.5 rounded-xl text-[10px] font-black shadow-md transition-all shrink-0 ${activeTab === 'revenue' ? 'bg-white dark:bg-slate-800 text-blue-600' : 'text-slate-400'}`}>REVENUE</button>
      </div>

      <div className="bg-white dark:bg-slate-800 rounded-[40px] shadow-2xl border border-slate-100 dark:border-slate-700 p-6 md:p-10 min-h-[600px] relative overflow-hidden">
        <div className={`absolute top-0 left-0 w-full h-2 bg-gradient-to-r ${activeTab === 'collection' ? 'from-teal-500 to-rose-500' : activeTab === 'due' ? 'from-rose-500 to-orange-500' : 'from-blue-500 to-indigo-500'}`}></div>

        <div className="flex justify-between items-center mb-8">
           <h3 className="text-xl md:text-2xl font-black uppercase tracking-tighter leading-none">{activeTab === 'collection' ? 'Collection Analysis' : activeTab === 'due' ? 'Subscriber Due List' : 'Revenue Summary'}</h3>
           <button onClick={handlePrint} className="bg-indigo-600 text-white px-8 py-3 rounded-2xl text-[10px] font-black shadow-lg">PRINT REPORT</button>
        </div>

        {activeTab === 'collection' && (
           <div className="overflow-x-auto">
              <table className="w-full text-left">
                 <thead>
                    <tr className="text-[11px] text-slate-400 border-b-2 border-slate-50 dark:border-slate-700 font-black uppercase">
                       <th className="pb-5 w-12 text-center">#</th>
                       <th className="pb-5">CUST. ID</th>
                       <th className="pb-5">SUBSCRIBER DETAILS</th>
                       <th className="pb-5">ADDRESS / ZONE</th>
                       <th className="pb-5">PAY METHOD</th>
                       <th className="pb-5 text-center">DATE</th>
                       <th className="pb-5">COLLECTOR</th>
                       <th className="pb-5 text-right">AMOUNT</th>
                       <th className="pb-5 text-center">ACTION</th>
                    </tr>
                 </thead>
                 <tbody className="divide-y divide-slate-50 dark:divide-slate-700">
                    {filteredPayments.map((p, idx) => {
                       const customer = store.customers.find(c => c.id === p.customerId || c.id === p.customer_id || c.customerCode === p.customerCode);
                       return (
                        <tr key={p.id} className="group hover:bg-teal-50/30 dark:hover:bg-teal-900/10 transition-all cursor-pointer">
                           <td className="py-6 text-center text-xs text-slate-300 font-black">{idx + 1}</td>
                           <td className="py-6 text-sm font-black text-indigo-600 dark:text-indigo-400">
                              {p.customerCode || customer?.customerCode || customer?.customer_code || '---'}
                           </td>
                           <td className="py-6">
                              <p className="text-base font-black text-slate-800 dark:text-white leading-none uppercase tracking-tighter">
                                 {customer?.name || p.customerName || '---'}
                              </p>
                              <p className="text-[10px] text-slate-400 font-bold mt-1.5 uppercase tracking-widest italic">
                                 {customer?.pppoeUsername || customer?.pppoe_username || '---'}
                              </p>
                           </td>
                           <td className="py-6 text-sm text-slate-600 dark:text-slate-300 font-black leading-tight uppercase">
                              {customer?.address || customer?.zone || '---'}
                           </td>
                           <td className="py-6"><span className={`px-4 py-1.5 rounded-xl text-[9px] font-black border-2 uppercase ${p.paymentMethod?.includes('bKash') ? 'bg-rose-50 text-rose-600 border-rose-100' : 'bg-emerald-50 text-emerald-600 border-emerald-100'}`}>{p.paymentMethod || 'Cash'}</span></td>
                           <td className="py-6 text-xs text-slate-800 dark:text-slate-300 font-black text-center">{p.paymentDate}</td>
                           <td className="py-6 text-xs text-slate-500 font-black uppercase italic tracking-wider">
                              <span onClick={() => { if(customer) { setTargetCustomer(customer); setTargetPayment(p); setNewCollector(p.collectedBy || ''); setShowCollectorModal(true); } }} className="bg-slate-100 dark:bg-slate-900 px-3 py-1 rounded-lg cursor-pointer hover:bg-indigo-600 hover:text-white transition-all">{p.collectedBy || 'Admin'}</span>
                           </td>
                           <td className="py-6 text-right font-black text-emerald-600 text-xl">৳{p.amount}</td>
                           <td className="py-6 text-center"><button onClick={(e) => { e.stopPropagation(); setPaymentToDelete(p); setShowDeleteModal(true); }} className="text-rose-300 hover:text-rose-600 transition-colors"><i className="fas fa-trash-alt"></i></button></td>
                        </tr>
                       );
                    })}
                 </tbody>
              </table>
           </div>
        )}

        {activeTab === 'due' && (
           <div className="overflow-x-auto">
              <table className="w-full text-left">
                 <thead>
                    <tr className="text-[11px] text-slate-400 border-b-2 font-black uppercase"><th>CUSTOMER</th><th>ZONE</th><th>COLLECTOR</th><th className="text-right">BILL</th><th className="text-right">DUE</th></tr>
                 </thead>
                 <tbody className="divide-y divide-slate-50 dark:divide-slate-700">
                    {filteredDueCustomers.map(c => (
                       <tr key={c.id} className="hover:bg-rose-50/20 transition-all">
                          <td className="py-5 font-black uppercase">{c.name} <span className="block text-[10px] text-slate-400">#{c.customerCode}</span></td>
                          <td className="py-5 text-sm font-black text-indigo-600">{c.zone || 'Global'}</td>
                          <td className="py-5 text-sm font-black text-slate-500 uppercase italic">
                             <span onClick={() => { setTargetCustomer(c); setTargetPayment(null); setNewCollector(c.assignedStaffId || ''); setShowCollectorModal(true); }} className="bg-slate-100 dark:bg-slate-900 px-3 py-1 rounded-lg cursor-pointer hover:bg-indigo-600 hover:text-white transition-all border border-slate-200">{store.staff?.find(s => s.id === (c.assignedStaffId || c.assigned_staff_id) || s.name === (c.assignedStaffId || c.assigned_staff_id))?.name || c.assignedStaffId || c.assigned_staff_id || '---'}</span>
                          </td>
                          <td className="py-5 text-right font-black text-slate-700 dark:text-slate-300 text-lg">৳{Math.floor(c.monthlyBill || 0)}</td>
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
             <h3 className="text-3xl font-black uppercase tracking-tighter">Change Collector</h3>
             <select value={newCollector} onChange={e => setNewCollector(e.target.value)} className="w-full bg-slate-50 dark:bg-slate-900 p-5 rounded-3xl font-black outline-none border-none shadow-inner text-indigo-600">
                <option value="">No Collector</option>
                {store.staff?.map(s => <option key={s.id} value={s.name}>{s.name}</option>)}
             </select>
             <button onClick={handleUpdateCollector} disabled={isUpdatingCollector} className="w-full bg-indigo-600 text-white py-6 rounded-3xl font-black uppercase tracking-[5px] shadow-2xl active:scale-95 transition-all">CONFIRM CHANGE</button>
             <button onClick={() => setShowCollectorModal(false)} className="w-full text-rose-500 font-black text-xs tracking-widest">CANCEL</button>
          </div>
        </div>
      )}

      {showDeleteModal && (
        <div className="fixed inset-0 bg-slate-900/90 backdrop-blur-xl z-[5000] flex items-center justify-center p-6 font-black uppercase">
          <div className="bg-white dark:bg-slate-800 rounded-[56px] w-full max-w-md p-12 shadow-2xl text-center space-y-8">
             <div className="w-24 h-24 bg-rose-50 rounded-[32px] flex items-center justify-center mx-auto text-5xl text-rose-500 shadow-inner border-2 border-rose-100"><i className="fas fa-exclamation-triangle"></i></div>
             <h3 className="text-3xl font-black tracking-tighter">Confirm Delete?</h3>
             <div className="flex space-x-4">
                <button onClick={() => setShowDeleteModal(false)} className="flex-1 bg-slate-100 dark:bg-slate-700 py-5 rounded-2xl font-black text-slate-500">CANCEL</button>
                <button onClick={confirmDelete} disabled={isDeleting} className="flex-1 bg-rose-600 text-white py-5 rounded-2xl font-black shadow-xl">YES, DELETE</button>
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
     <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest">{label}</p>
     <p className={`text-2xl font-black ${color} tracking-tighter`}>{value}</p>
  </div>
);

export default CollectionReport;
