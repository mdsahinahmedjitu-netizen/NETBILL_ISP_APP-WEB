import React, { useState, useMemo } from 'react';

const SalaryHistory = ({ store, session, t }) => {
  const [selectedMonth, setSelectedMonth] = useState(new Date().toLocaleString('default', { month: 'long', year: 'numeric' }));
  const [selectedStaffId, setSelectedStaffId] = useState(session.role === 'admin' ? 'all' : session.data.id);

  const isAdmin = session.role === 'admin';

  const months = useMemo(() => {
    const result = [];
    for (let i = 0; i < 12; i++) {
      const d = new Date();
      d.setMonth(d.getMonth() - i);
      result.push(d.toLocaleString('default', { month: 'long', year: 'numeric' }));
    }
    return result;
  }, []);

  const filteredHistory = store.staffPayouts?.filter(p => {
    const monthMatch = selectedMonth === 'All Months' || p.month === selectedMonth;
    const staffMatch = selectedStaffId === 'all' || p.staffId === selectedStaffId;
    return monthMatch && staffMatch;
  }).sort((a, b) => new Date(b.date) - new Date(a.date));

  const totalAdded = filteredHistory?.filter(p => p.type === 'salary_add').reduce((s, p) => s + (p.amount || 0), 0) || 0;
  const totalPaid = filteredHistory?.filter(p => p.type === 'payment').reduce((s, p) => s + (p.amount || 0), 0) || 0;

  return (
    <div className="max-w-7xl mx-auto space-y-8 pb-20 uppercase font-black tracking-tighter transition-all">
      {/* Header */}
      <div className="flex flex-col xl:flex-row justify-between items-start xl:items-center gap-6 bg-white dark:bg-slate-800 p-8 rounded-[48px] shadow-xl border border-slate-100 dark:border-slate-700">
        <div className="space-y-1">
          <h3 className="text-4xl font-black text-slate-800 dark:text-white uppercase tracking-tighter leading-none">Payroll Ledger</h3>
          <p className="text-[10px] text-teal-600 font-bold tracking-[4px] uppercase mt-2">Detailed Salary & Advance Tracking History</p>
        </div>

        <div className="flex flex-wrap gap-4 w-full xl:w-auto">
          {isAdmin && (
            <select
              value={selectedStaffId}
              onChange={(e) => setSelectedStaffId(e.target.value)}
              className="bg-slate-50 dark:bg-slate-900 px-6 py-4 rounded-2xl border-none font-black text-[10px] shadow-inner outline-none focus:ring-2 focus:ring-teal-500/20"
            >
              <option value="all">ALL STAFF MEMBERS</option>
              {store.staff?.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
          )}

          <select
            value={selectedMonth}
            onChange={(e) => setSelectedMonth(e.target.value)}
            className="bg-slate-50 dark:bg-slate-900 px-6 py-4 rounded-2xl border-none font-black text-[10px] shadow-inner outline-none focus:ring-2 focus:ring-teal-500/20"
          >
            <option value="All Months">ALL MONTHS</option>
            {months.map(m => <option key={m} value={m}>{m}</option>)}
          </select>

          <button onClick={() => window.print()} className="bg-slate-800 text-white px-8 py-4 rounded-2xl shadow-lg font-black text-[10px] uppercase tracking-widest transition-all hover:bg-slate-900 flex items-center space-x-2">
            <i className="fas fa-print"></i><span>Print Ledger</span>
          </button>
        </div>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
         <div className="bg-white dark:bg-slate-800 p-8 rounded-[40px] border border-slate-100 dark:border-slate-700 shadow-xl text-center">
            <p className="text-[10px] text-slate-400 font-black tracking-widest mb-2 uppercase">TOTAL SALARY ACCRUED</p>
            <p className="text-4xl font-black text-teal-600 tracking-tighter">৳{totalAdded}</p>
         </div>
         <div className="bg-white dark:bg-slate-800 p-8 rounded-[40px] border border-slate-100 dark:border-slate-700 shadow-xl text-center">
            <p className="text-[10px] text-slate-400 font-black tracking-widest mb-2 uppercase">TOTAL DISBURSED</p>
            <p className="text-4xl font-black text-rose-500 tracking-tighter">৳{totalPaid}</p>
         </div>
         <div className="bg-white dark:bg-slate-800 p-8 rounded-[40px] border border-slate-100 dark:border-slate-700 shadow-xl text-center">
            <p className="text-[10px] text-slate-400 font-black tracking-widest mb-2 uppercase">NET BALANCE</p>
            <p className={`text-4xl font-black tracking-tighter ${totalAdded - totalPaid >= 0 ? 'text-indigo-600' : 'text-orange-500'}`}>
               ৳{Math.abs(totalAdded - totalPaid)}
               <span className="text-xs ml-2 font-bold opacity-50">{totalAdded - totalPaid >= 0 ? '(Pao-na)' : '(Advance)'}</span>
            </p>
         </div>
      </div>

      {/* Ledger Table */}
      <div className="bg-white dark:bg-slate-800 rounded-[56px] shadow-2xl border border-slate-100 dark:border-slate-700 overflow-hidden font-black">
          <div className="overflow-x-auto custom-scrollbar min-h-[400px]">
              <table className="w-full text-center uppercase tracking-tighter">
                <thead className="bg-slate-50 dark:bg-slate-900 border-b-2 border-slate-100 dark:border-slate-700 text-[11px] text-slate-500 tracking-[2px] font-black uppercase">
                  <tr>
                    <th className="p-6 text-left">Date & Month</th>
                    {isAdmin && <th className="p-6 text-left">Staff Name</th>}
                    <th className="p-6">Transaction Type</th>
                    <th className="p-6">Amount</th>
                    <th className="p-6">Account Balance</th>
                    <th className="p-6">Remarks</th>
                  </tr>
                </thead>
                <tbody className="divide-y-2 divide-slate-50 dark:divide-slate-700">
                  {filteredHistory?.map(p => (
                    <tr key={p.id} className="hover:bg-slate-50/50 dark:hover:bg-slate-900/30 transition-colors">
                      <td className="p-6 text-left">
                         <p className="text-sm font-black text-slate-800 dark:text-white leading-none">{p.date}</p>
                         <p className="text-[9px] text-slate-400 mt-1 uppercase tracking-widest">{p.month}</p>
                      </td>
                      {isAdmin && (
                        <td className="p-6 text-left">
                           <div className="flex items-center space-x-3">
                              <div className="w-8 h-8 bg-teal-50 dark:bg-slate-900 rounded-lg flex items-center justify-center text-teal-600 text-xs">
                                 <i className="fas fa-user-tie"></i>
                              </div>
                              <span className="text-xs font-black uppercase">{p.staffName}</span>
                           </div>
                        </td>
                      )}
                      <td className="p-6">
                        <span className={`px-4 py-1.5 rounded-full text-[9px] font-black uppercase shadow-sm ${p.type === 'salary_add' ? 'bg-teal-100 text-teal-700' : 'bg-rose-100 text-rose-700'}`}>
                           {p.type === 'salary_add' ? 'Salary Accrued' : 'Cash Disbursed'}
                        </span>
                      </td>
                      <td className={`p-6 text-xl font-black ${p.type === 'salary_add' ? 'text-teal-600' : 'text-rose-600'}`}>
                        {p.type === 'salary_add' ? '+' : '-'} ৳{p.amount}
                      </td>
                      <td className="p-6">
                        <div className="bg-slate-100 dark:bg-slate-900/50 px-6 py-3 rounded-2xl inline-block border-2 border-slate-200 dark:border-slate-700 shadow-inner">
                           <p className="text-2xl font-black text-slate-900 dark:text-white leading-none">৳{p.newBalance}</p>
                           <p className="text-[10px] text-teal-600 font-black mt-1.5 uppercase tracking-[3px] leading-none text-center">RUNNING BAL</p>
                        </div>
                      </td>
                      <td className="p-6 text-xs text-slate-500 font-bold max-w-xs truncate italic">{p.remarks || '---'}</td>
                    </tr>
                  ))}
                  {filteredHistory?.length === 0 && (
                    <tr>
                       <td colSpan={isAdmin ? 6 : 5} className="py-20 text-center opacity-30">
                          <i className="fas fa-folder-open text-6xl mb-4"></i>
                          <p className="text-xl tracking-[10px]">No Records Found</p>
                       </td>
                    </tr>
                  )}
                </tbody>
              </table>
          </div>
      </div>
    </div>
  );
};

export default SalaryHistory;
