import React, { useState, useMemo } from 'react';

const CollectionReport = ({ store, t }) => {
  const [activeTab, setActiveTab] = useState('collection'); // 'collection', 'due', 'revenue'
  const [search, setSearch] = useState('');
  const [startDate, setDateStart] = useState(new Date(new Date().getFullYear(), new Date().getMonth(), 1).toLocaleDateString('en-CA'));
  const [endDate, setDateEnd] = useState(new Date().toLocaleDateString('en-CA'));
  const [selectedStaff, setSelectedStaff] = useState('All Collectors');
  const [selectedMethod, setSelectedMethod] = useState('All Methods');

  const filteredPayments = useMemo(() => {
    return store.payments?.filter(p => {
      const pDate = p.paymentDate;
      const dateMatch = pDate >= startDate && pDate <= endDate;
      const staffMatch = selectedStaff === 'All Collectors' || p.collectedBy === selectedStaff || p.collectedById === selectedStaff;
      const methodMatch = selectedMethod === 'All Methods' || p.paymentMethod?.includes(selectedMethod);

      const customer = store.customers.find(c => c.id === p.customerId || c.customerCode === p.customerCode);
      const searchMatch = !search ||
        p.customerName?.toLowerCase().includes(search.toLowerCase()) ||
        p.customerCode?.includes(search) ||
        p.transactionId?.includes(search) ||
        customer?.pppoeUsername?.toLowerCase().includes(search.toLowerCase());

      return dateMatch && staffMatch && methodMatch && searchMatch;
    }).sort((a, b) => new Date(b.paymentDate) - new Date(a.paymentDate));
  }, [store.payments, store.customers, startDate, endDate, selectedStaff, selectedMethod, search]);

  const totalAmount = filteredPayments.reduce((sum, p) => sum + (p.amount || 0), 0);
  const totalDue = store.customers.reduce((sum, c) => sum + (parseFloat(c.currentDue) || 0), 0);
  const totalBill = store.customers.reduce((sum, c) => sum + (parseFloat(c.monthlyBill) || 0), 0);

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
      const hasDue = parseFloat(c.currentDue) > 0;
      const staffMatch = selectedStaff === 'All Collectors' ||
                         c.assignedStaffId === selectedStaff ||
                         store.staff?.find(s => s.name === selectedStaff)?.id === c.assignedStaffId;
      return hasDue && staffMatch;
    }).sort((a,b) => b.currentDue - a.currentDue);
  }, [store.customers, store.staff, selectedStaff]);

  const totalDueFiltered = filteredDueCustomers.reduce((sum, c) => sum + (parseFloat(c.currentDue) || 0), 0);

  const handlePrint = () => window.print();

  return (
    <div className="w-full space-y-6 pb-20 font-sans tracking-tight">
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <StatCardSmall label="Total Customers" value={store.customers.length} icon="fa-users" color="text-indigo-600" bgColor="bg-indigo-50 dark:bg-indigo-900/20" borderColor="border-indigo-100" />
        <StatCardSmall label="Collected" value={`৳${totalAmount.toLocaleString()}`} icon="fa-money-bill-trend-up" color="text-emerald-600" bgColor="bg-emerald-50 dark:bg-emerald-900/20" borderColor="border-emerald-100" />
        <StatCardSmall label="Due" value={`৳${Math.floor(totalDue).toLocaleString()}`} icon="fa-triangle-exclamation" color="text-rose-500" bgColor="bg-rose-50 dark:bg-rose-900/20" borderColor="border-rose-100" />
        <StatCardSmall label="Total Bill" value={`৳${Math.floor(totalBill).toLocaleString()}`} icon="fa-receipt" color="text-blue-600" bgColor="bg-blue-50 dark:bg-blue-900/20" borderColor="border-blue-100" />
      </div>

      <div className="bg-white dark:bg-slate-800 p-5 rounded-[32px] shadow-lg border border-slate-100 dark:border-slate-700 flex flex-wrap items-center gap-4">
        <div className="relative flex-1 min-w-[200px]">
          <input type="text" placeholder="Search customer, ID or TrxID..." value={search} onChange={(e) => setSearch(e.target.value)} className="w-full pl-12 pr-4 py-3 bg-slate-50 dark:bg-slate-900 rounded-2xl border-2 border-transparent focus:border-teal-500/30 text-sm outline-none transition-all shadow-inner" />
          <i className="fas fa-search absolute left-5 top-4 text-slate-300 text-sm"></i>
        </div>
        <div className="flex items-center space-x-3 bg-slate-50 dark:bg-slate-900 px-5 py-2.5 rounded-2xl border-2 border-slate-100 dark:border-slate-700 shadow-sm">
           <i className="fas fa-calendar-alt text-teal-600 text-sm"></i>
           <input type="date" value={startDate} onChange={e => setDateStart(e.target.value)} className="bg-transparent border-none text-xs font-black outline-none cursor-pointer" />
           <span className="text-slate-300 font-bold">/</span>
           <input type="date" value={endDate} onChange={e => setDateEnd(e.target.value)} className="bg-transparent border-none text-xs font-black outline-none cursor-pointer" />
        </div>
        <select value={selectedStaff} onChange={e => setSelectedStaff(e.target.value)} className="bg-indigo-50 dark:bg-indigo-900/20 text-indigo-600 px-5 py-3 rounded-2xl border-2 border-indigo-100 dark:border-indigo-800 text-xs font-black outline-none cursor-pointer hover:bg-indigo-100 transition-colors">
          <option>All Collectors</option>
          <option>Admin / Direct</option>
          {store.staff?.map(s => <option key={s.id} value={s.name}>{s.name}</option>)}
        </select>
        <button className="bg-teal-600 text-white px-8 py-3 rounded-2xl text-xs font-black flex items-center space-x-2 shadow-lg shadow-teal-500/20 hover:scale-105 active:scale-95 transition-all">
           <i className="fas fa-filter"></i><span>Apply Filter</span>
        </button>
      </div>

      <div className="flex items-center space-x-2 bg-slate-100 dark:bg-slate-900 p-1.5 rounded-[24px] w-fit shadow-inner">
         <button onClick={() => setActiveTab('collection')} className={`px-8 py-2.5 rounded-xl text-[10px] font-black shadow-md flex items-center space-x-2 transition-all ${activeTab === 'collection' ? 'bg-white dark:bg-slate-800 text-teal-600 border border-teal-50' : 'text-slate-400 hover:text-slate-600'}`}>
            <i className="fas fa-money-bill-transfer"></i><span>COLLECTION</span>
         </button>
         <button onClick={() => setActiveTab('due')} className={`px-8 py-2.5 rounded-xl text-[10px] font-black shadow-md flex items-center space-x-2 transition-all ${activeTab === 'due' ? 'bg-white dark:bg-slate-800 text-rose-500 border border-rose-50' : 'text-slate-400 hover:text-slate-600'}`}>
            <i className="fas fa-triangle-exclamation"></i><span>DUE LIST</span>
         </button>
         <button onClick={() => setActiveTab('revenue')} className={`px-8 py-2.5 rounded-xl text-[10px] font-black shadow-md flex items-center space-x-2 transition-all ${activeTab === 'revenue' ? 'bg-white dark:bg-slate-800 text-blue-600 border border-blue-50' : 'text-slate-400 hover:text-slate-600'}`}>
            <i className="fas fa-chart-line"></i><span>REVENUE</span>
         </button>
      </div>

      <div className="bg-white dark:bg-slate-800 rounded-[40px] shadow-2xl border border-slate-100 dark:border-slate-700 p-10 space-y-8 min-h-[600px] relative overflow-hidden">
        <div className={`absolute top-0 left-0 w-full h-2 bg-gradient-to-r ${activeTab === 'collection' ? 'from-teal-500 via-indigo-500 to-rose-500' : activeTab === 'due' ? 'from-rose-500 to-orange-500' : 'from-blue-500 to-indigo-500'}`}></div>

        <div className="flex justify-between items-center">
           <div className="flex items-center space-x-4">
              <div className={`w-12 h-12 rounded-2xl flex items-center justify-center text-white shadow-lg ${activeTab === 'collection' ? 'bg-gradient-to-br from-teal-400 to-teal-600' : activeTab === 'due' ? 'bg-gradient-to-br from-rose-400 to-rose-600' : 'bg-gradient-to-br from-blue-400 to-blue-600'}`}>
                 <i className={`fas ${activeTab === 'collection' ? 'fa-receipt' : activeTab === 'due' ? 'fa-user-clock' : 'fa-chart-pie'} text-xl`}></i>
              </div>
              <div>
                <h3 className="text-2xl font-black text-slate-800 dark:text-white uppercase tracking-tighter">{activeTab === 'collection' ? 'Collection Analysis' : activeTab === 'due' ? 'Subscriber Due List' : 'Revenue Summary'}</h3>
                <p className="text-[10px] text-slate-400 font-bold tracking-[3px]">{activeTab === 'collection' ? 'Verified Transaction Records' : activeTab === 'due' ? 'Outstanding Balances Report' : 'Financial Earnings Overview'}</p>
              </div>
           </div>
           <div className="flex space-x-3">
              <button onClick={handlePrint} className="px-8 py-3 bg-indigo-600 text-white rounded-2xl shadow-lg shadow-indigo-500/20 text-xs font-black flex items-center space-x-3 hover:scale-105 transition-all"><i className="fas fa-print text-lg"></i><span>PRINT REPORT</span></button>
              <button className="px-8 py-3 bg-emerald-600 text-white rounded-2xl shadow-lg shadow-emerald-500/20 text-xs font-black flex items-center space-x-3 hover:scale-105 transition-all"><i className="fas fa-file-csv text-lg"></i><span>EXPORT CSV</span></button>
           </div>
        </div>

        {activeTab === 'collection' && (
           <>
              <div className="grid grid-cols-1 xl:grid-cols-2 gap-8 items-end">
                 <div className="flex items-center space-x-6">
                    <div className="bg-slate-50 dark:bg-slate-900 px-6 py-4 rounded-3xl border border-slate-100 dark:border-slate-800 shadow-inner">
                       <p className="text-[9px] text-slate-400 font-black tracking-widest mb-1 uppercase">TOTAL ENTRIES</p>
                       <p className="text-3xl font-black text-slate-800 dark:text-white">{filteredPayments.length}</p>
                    </div>
                    <div className="bg-emerald-50 dark:bg-emerald-900/20 px-6 py-4 rounded-3xl border border-emerald-100 dark:border-emerald-800 shadow-inner">
                       <p className="text-[9px] text-emerald-600/60 font-black tracking-widest mb-1 uppercase">TOTAL REVENUE</p>
                       <p className="text-3xl font-black text-emerald-600">৳{totalAmount.toLocaleString()}</p>
                    </div>
                 </div>
                 <div className="space-y-3">
                    <p className="text-[10px] text-slate-400 font-black uppercase tracking-[4px] ml-2">Staff Contribution</p>
                    <div className="flex flex-wrap gap-2">
                       {staffStats.map(([name, data], idx) => {
                         const colors = ['bg-indigo-50 text-indigo-600 border-indigo-100', 'bg-teal-50 text-teal-600 border-teal-100', 'bg-rose-50 text-rose-600 border-rose-100', 'bg-amber-50 text-amber-600 border-amber-100'];
                         return (
                           <div key={name} className={`px-4 py-2.5 ${colors[idx % colors.length]} rounded-xl border-2 flex items-center space-x-3 shadow-sm hover:scale-105 transition-transform cursor-default`}>
                              <span className="text-[11px] font-black uppercase">{name}</span>
                              <div className="w-px h-3 bg-current opacity-20"></div>
                              <span className="text-[11px] font-black">৳{data.amount.toLocaleString()}</span>
                           </div>
                         );
                       })}
                    </div>
                 </div>
              </div>
              <div className="overflow-x-auto pt-6">
                 <table className="w-full text-left">
                    <thead>
                       <tr className="text-[11px] text-slate-400 border-b-2 border-slate-50 dark:border-slate-700 font-black uppercase">
                          <th className="pb-5 w-12 text-center">#</th>
                          <th className="pb-5">CUST. ID</th>
                          <th className="pb-5">SUBSCRIBER DETAILS</th>
                          <th className="pb-5">ADDRESS / ZONE</th>
                          <th className="pb-5">PAY METHOD</th>
                          <th className="pb-5 text-center">TRANSACTION DATE</th>
                          <th className="pb-5">COLLECTOR</th>
                          <th className="pb-5 text-right">AMOUNT</th>
                          <th className="pb-5 text-center">ACTION</th>
                       </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-50 dark:divide-slate-700">
                       {filteredPayments.map((p, idx) => {
                          const customer = store.customers.find(c => c.id === p.customerId || c.customerCode === p.customerCode);
                          return (
                            <tr key={p.id} className="group hover:bg-teal-50/30 dark:hover:bg-teal-900/10 transition-all cursor-pointer">
                               <td className="py-6 text-center text-xs text-slate-300 font-black">{idx + 1}</td>
                               <td className="py-6 text-sm font-black text-indigo-600 dark:text-indigo-400">#{p.customerCode || '---'}</td>
                               <td className="py-6">
                                  <p className="text-base font-black text-slate-800 dark:text-white leading-none uppercase tracking-tighter">{p.customerName}</p>
                                  <p className="text-[10px] text-slate-400 font-bold mt-1.5 uppercase tracking-widest italic">{customer?.pppoeUsername || '---'}</p>
                               </td>
                               <td className="py-6 text-xs text-slate-500 font-black leading-tight max-w-[180px] uppercase">{customer?.address || customer?.zone || '---'}</td>
                               <td className="py-6">
                                  <span className={`px-4 py-1.5 rounded-xl text-[9px] font-black border-2 uppercase transition-all ${p.paymentMethod?.includes('bKash') ? 'bg-rose-50 text-rose-600 border-rose-100' : p.paymentMethod?.includes('Cash') ? 'bg-emerald-50 text-emerald-600 border-emerald-100' : 'bg-blue-50 text-blue-600 border-blue-100'}`}>{p.paymentMethod || 'Cash'}</span>
                               </td>
                               <td className="py-6 text-xs text-slate-800 dark:text-slate-300 font-black text-center">{p.paymentDate}</td>
                               <td className="py-6 text-xs text-slate-500 font-black uppercase italic tracking-wider"><span className="bg-slate-100 dark:bg-slate-900 px-3 py-1 rounded-lg">{p.collectedBy || 'Admin'}</span></td>
                               <td className="py-6 text-right"><p className="text-xl font-black text-emerald-600 tracking-tighter">৳{p.amount}</p></td>
                               <td className="py-6 text-center"><button className="w-10 h-10 rounded-xl bg-slate-50 dark:bg-slate-900 text-slate-300 hover:text-teal-500 hover:text-white transition-all shadow-sm"><i className="fas fa-print"></i></button></td>
                            </tr>
                          );
                       })}
                    </tbody>
                 </table>
                 {filteredPayments.length === 0 && (
                   <div className="py-20 text-center opacity-20">
                      <i className="fas fa-receipt text-8xl mb-4"></i>
                      <p className="text-2xl font-black uppercase tracking-[10px]">No Data Found</p>
                   </div>
                 )}
              </div>
           </>
        )}

        {activeTab === 'due' && (
           <div className="space-y-6">
              <div className="flex items-center space-x-6">
                 <div className="bg-slate-50 dark:bg-slate-900 px-6 py-4 rounded-3xl border border-slate-100 dark:border-slate-800 shadow-inner">
                    <p className="text-[9px] text-slate-400 font-black tracking-widest mb-1 uppercase">DUE CUSTOMERS</p>
                    <p className="text-3xl font-black text-slate-800 dark:text-white">{filteredDueCustomers.length}</p>
                 </div>
                 <div className="bg-rose-50 dark:bg-rose-900/20 px-6 py-4 rounded-3xl border border-rose-100 dark:border-rose-800 shadow-inner">
                    <p className="text-[9px] text-rose-600/60 font-black tracking-widest mb-1 uppercase">TOTAL OUTSTANDING</p>
                    <p className="text-3xl font-black text-rose-600">৳{Math.floor(totalDueFiltered).toLocaleString()}</p>
                 </div>
              </div>

              <div className="overflow-x-auto">
                 <table className="w-full text-left">
                    <thead>
                       <tr className="text-[11px] text-slate-400 border-b-2 font-black uppercase">
                          <th className="pb-5">CUSTOMER</th>
                          <th className="pb-5">MOBILE</th>
                          <th className="pb-5">ZONE</th>
                          <th className="pb-5">PLAN</th>
                          <th className="pb-5">COLLECTOR</th>
                          <th className="pb-5 text-right">MONTHLY BILL</th>
                          <th className="pb-5 text-right">TOTAL DUE</th>
                       </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-50 dark:divide-slate-700">
                       {filteredDueCustomers.map(c => (
                          <tr key={c.id} className="hover:bg-rose-50/20 transition-all">
                             <td className="py-5 font-black uppercase">{c.name} <span className="block text-[10px] text-slate-400">#{c.customerCode}</span></td>
                             <td className="py-5 text-sm font-bold text-slate-600">{c.mobile}</td>
                             <td className="py-5 text-sm font-black text-indigo-600">{c.zone || 'Global'}</td>
                             <td className="py-5 text-sm font-bold">{c.packageName}</td>
                             <td className="py-5 text-sm font-black text-slate-500 uppercase italic">
                                <span className="bg-slate-100 dark:bg-slate-900 px-3 py-1 rounded-lg">
                                   {store.staff?.find(s => s.id === c.assignedStaffId)?.name || c.assignedStaffId || '---'}
                                </span>
                             </td>
                             <td className="py-5 text-right font-black text-slate-700 dark:text-slate-300 text-lg">৳{Math.floor(c.monthlyBill || 0)}</td>
                             <td className="py-5 text-right font-black text-rose-500 text-xl">৳{Math.floor(c.currentDue)}</td>
                          </tr>
                       ))}
                    </tbody>
                 </table>
              </div>
           </div>
        )}

        {activeTab === 'revenue' && (
           <div className="space-y-10">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                 <div className="bg-slate-50 dark:bg-slate-900 p-8 rounded-3xl border border-slate-100">
                    <h4 className="text-sm font-black text-slate-400 mb-6 uppercase tracking-widest">Monthly Collection Overview</h4>
                    <div className="space-y-4">
                       {[...new Set(store.payments?.map(p => p.billingMonth))].filter(Boolean).map(month => {
                          const amt = store.payments.filter(p => p.billingMonth === month).reduce((s,p)=>s+p.amount,0);
                          return (
                            <div key={month} className="flex justify-between items-center bg-white dark:bg-slate-800 p-5 rounded-2xl shadow-sm border border-slate-100">
                               <span className="font-black text-slate-700 dark:text-slate-200">{month}</span>
                               <span className="font-black text-emerald-600 text-xl">৳{amt.toLocaleString()}</span>
                            </div>
                          );
                       })}
                    </div>
                 </div>
                 <div className="bg-slate-50 dark:bg-slate-900 p-8 rounded-3xl border border-slate-100">
                    <h4 className="text-sm font-black text-slate-400 mb-6 uppercase tracking-widest">Collection by Payment Method</h4>
                    <div className="space-y-4">
                       {['Cash', 'bKash', 'Nagad', 'Bank'].map(method => {
                          const amt = store.payments.filter(p => p.paymentMethod?.includes(method)).reduce((s,p)=>s+p.amount,0);
                          return (
                            <div key={method} className="flex justify-between items-center bg-white dark:bg-slate-800 p-5 rounded-2xl shadow-sm border border-slate-100">
                               <span className="font-black text-slate-700 dark:text-slate-200">{method}</span>
                               <span className="font-black text-indigo-600 text-xl">৳{amt.toLocaleString()}</span>
                            </div>
                          );
                       })}
                    </div>
                 </div>
              </div>
           </div>
        )}
      </div>
    </div>
  );
};

const StatCardSmall = ({ label, value, icon, color = "text-slate-800", bgColor = "bg-white", borderColor = "border-slate-100" }) => (
  <div className={`${bgColor} p-6 rounded-2xl border ${borderColor} shadow-sm flex flex-col items-start space-y-2 relative overflow-hidden group`}>
     <div className="absolute top-4 right-4 text-slate-100 dark:text-slate-700 text-3xl group-hover:scale-110 transition-transform"><i className={`fas ${icon}`}></i></div>
     <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest">{label}</p>
     <p className={`text-2xl font-black ${color} tracking-tighter`}>{value}</p>
  </div>
);

export default CollectionReport;
