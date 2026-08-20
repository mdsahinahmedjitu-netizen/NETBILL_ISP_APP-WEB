import React, { useState, useMemo } from 'react';
import { supabase } from '../supabaseClient';

const SalaryHistory = ({ store, session, t, lang }) => {
  const [selectedMonth, setSelectedMonth] = useState(new Date().toLocaleString('en-US', { month: 'long', year: 'numeric' }));
  const [selectedStaffId, setSelectedStaffId] = useState(session.role === 'admin' ? 'all' : session.data.id);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editingPayout, setEditingPayout] = useState(null);

  const isAdmin = session.role === 'admin';

  const translateMonth = (monthStr) => {
    if (!monthStr || lang !== 'bn') return monthStr;
    const months = {
      'January': 'জানুয়ারি', 'February': 'ফেব্রুয়ারি', 'March': 'মার্চ', 'April': 'এপ্রিল',
      'May': 'মে', 'June': 'জুন', 'July': 'জুলাই', 'August': 'আগস্ট',
      'September': 'সেপ্টেম্বর', 'October': 'অক্টোবর', 'November': 'নভেম্বর', 'December': 'ডিসেম্বর'
    };
    let translated = monthStr;
    Object.keys(months).forEach(enMonth => {
      translated = translated.replace(enMonth, months[enMonth]);
    });
    return translated;
  };

  const toBanglaNumber = (n) => {
    if (lang !== 'bn') return n;
    const digits = { '0': '০', '1': '১', '2': '২', '3': '৩', '4': '৪', '5': '৫', '6': '৬', '7': '৭', '8': '৮', '9': '৯' };
    return n.toString().split('').map(d => digits[d] || d).join('');
  };

  const months = useMemo(() => {
    const result = [];
    for (let i = 0; i < 12; i++) {
      const d = new Date();
      d.setMonth(d.getMonth() - i);
      result.push(d.toLocaleString('en-US', { month: 'long', year: 'numeric' }));
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

  const handleDeletePayout = async (payout) => {
    if (!window.confirm("Are you sure you want to delete this salary record? Staff balance will be reversed.")) return;

    try {
      const amount = parseFloat(payout.amount) || 0;

      // 1. Adjust staff balance
      const { data: staffData, error: staffFetchError } = await supabase
        .from('staff')
        .select('balance')
        .eq('id', payout.staffId)
        .single();

      if (!staffFetchError && staffData) {
        let reversalAdjustment = -amount; // If it was salary_add, subtract it
        if (payout.type === 'payment') {
          reversalAdjustment = amount; // If it was payment, add it back
        }

        const currentStaffBalance = parseFloat(staffData.balance) || 0;
        const newStaffBalance = currentStaffBalance + reversalAdjustment;

        await supabase
          .from('staff')
          .update({ balance: newStaffBalance })
          .eq('id', payout.staffId);
      }

      // 2. Delete the record
      const { error: deleteError } = await supabase
        .from('staff_payouts')
        .delete()
        .eq('id', payout.id);

      if (deleteError) throw deleteError;

      alert("Record deleted and staff balance reversed successfully!");
    } catch (error) {
      console.error("Delete failed:", error);
      alert("Failed to delete record: " + error.message);
    }
  };

  const handleUpdatePayout = async (e) => {
    e.preventDefault();
    if (!editingPayout) return;

    try {
      // 1. Find the original record to calculate the difference
      const original = store.staffPayouts.find(p => p.id === editingPayout.id);
      if (!original) throw new Error("Original record not found");

      const oldAmount = parseFloat(original.amount) || 0;
      const newAmount = parseFloat(editingPayout.amount) || 0;
      const diff = newAmount - oldAmount;

      // 2. Update the history record
      const { error: historyError } = await supabase
        .from('staff_payouts')
        .update({
          date: editingPayout.date,
          month: editingPayout.month,
          amount: newAmount,
          remarks: editingPayout.remarks,
          staff_id: editingPayout.staffId,
          staff_name: store.staff.find(s => s.id === editingPayout.staffId)?.name || editingPayout.staffName,
          type: editingPayout.type,
          new_balance: parseFloat(editingPayout.newBalance) || 0
        })
        .eq('id', editingPayout.id);

      if (historyError) throw historyError;

      // 3. Adjust the staff's current balance if the type is the same
      if (original.type === editingPayout.type) {
        // Fetch fresh staff data to ensure we have the absolute latest balance from DB
        const { data: staffData, error: staffFetchError } = await supabase
          .from('staff')
          .select('balance')
          .eq('id', editingPayout.staffId)
          .single();

        if (!staffFetchError && staffData) {
          let balanceAdjustment = diff;
          if (editingPayout.type === 'payment') {
            balanceAdjustment = -diff;
          }

          const currentStaffBalance = parseFloat(staffData.balance) || 0;
          const newStaffBalance = currentStaffBalance + balanceAdjustment;

          await supabase
            .from('staff')
            .update({ balance: newStaffBalance })
            .eq('id', editingPayout.staffId);
        }
      }

      alert("Record updated and staff balance adjusted successfully!");
      setIsEditModalOpen(false);
      setEditingPayout(null);
    } catch (error) {
      console.error("Update failed:", error);
      alert("Failed to update record: " + error.message);
    }
  };

  return (
    <div className="max-w-7xl mx-auto space-y-8 pb-20 uppercase font-black tracking-tighter transition-all">
      {/* Header */}
      <div className="flex flex-col xl:flex-row justify-between items-start xl:items-center gap-6 bg-white dark:bg-slate-800 p-8 rounded-[48px] shadow-xl border border-slate-100 dark:border-slate-700">
        <div className="space-y-1">
          <h3 className="text-4xl font-black text-slate-800 dark:text-white uppercase tracking-tighter leading-none">{t.payroll_ledger}</h3>
          <p className="text-[10px] text-teal-600 font-bold tracking-[4px] uppercase mt-2">{t.payroll_subtitle}</p>
        </div>

        <div className="flex flex-wrap gap-4 w-full xl:w-auto">
          {isAdmin && (
            <select
              value={selectedStaffId}
              onChange={(e) => setSelectedStaffId(e.target.value)}
              className="bg-slate-50 dark:bg-slate-900 px-6 py-4 rounded-2xl border-none font-black text-[10px] shadow-inner outline-none focus:ring-2 focus:ring-teal-500/20"
            >
              <option value="all">{t.all_staff}</option>
              {store.staff?.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
          )}

          <select
            value={selectedMonth}
            onChange={(e) => setSelectedMonth(e.target.value)}
            className="bg-slate-50 dark:bg-slate-900 px-6 py-4 rounded-2xl border-none font-black text-[10px] shadow-inner outline-none focus:ring-2 focus:ring-teal-500/20"
          >
            <option value="All Months">{t.all_months}</option>
            {months.map(m => <option key={m} value={m}>{translateMonth(m)}</option>)}
          </select>

          <button onClick={() => window.print()} className="bg-slate-800 text-white px-8 py-4 rounded-2xl shadow-lg font-black text-[10px] uppercase tracking-widest transition-all hover:bg-slate-900 flex items-center space-x-2">
            <i className="fas fa-print"></i><span>{t.print_ledger}</span>
          </button>
        </div>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
         <div className="bg-white dark:bg-slate-800 p-8 rounded-[40px] border border-slate-100 dark:border-slate-700 shadow-xl text-center">
            <p className="text-xs text-slate-400 font-black tracking-widest mb-2 uppercase">{t.total_salary_accrued}</p>
            <p className="text-4xl font-black text-teal-600 tracking-tighter">৳{totalAdded}</p>
         </div>
         <div className="bg-white dark:bg-slate-800 p-8 rounded-[40px] border border-slate-100 dark:border-slate-700 shadow-xl text-center">
            <p className="text-xs text-slate-400 font-black tracking-widest mb-2 uppercase">{t.total_disbursed}</p>
            <p className="text-4xl font-black text-rose-500 tracking-tighter">৳{totalPaid}</p>
         </div>
         <div className="bg-white dark:bg-slate-800 p-8 rounded-[40px] border border-slate-100 dark:border-slate-700 shadow-xl text-center">
            <p className="text-xs text-slate-400 font-black tracking-widest mb-2 uppercase">{t.net_balance}</p>
            <p className={`text-4xl font-black tracking-tighter ${totalAdded - totalPaid >= 0 ? 'text-indigo-600' : 'text-orange-500'}`}>
               ৳{Math.abs(totalAdded - totalPaid)}
               <span className={`text-lg ml-2 font-black italic ${totalAdded - totalPaid >= 0 ? 'text-indigo-700' : 'text-orange-600'}`}>{totalAdded - totalPaid >= 0 ? `(${t.pao_na})` : `(${t.advance_label})`}</span>
            </p>
         </div>
      </div>

      {/* Ledger Table */}
      <div className="bg-white dark:bg-slate-800 rounded-[56px] shadow-2xl border border-slate-100 dark:border-slate-700 overflow-hidden font-black">
          <div className="overflow-x-auto custom-scrollbar min-h-[400px]">
              <table className="w-full text-center uppercase tracking-tighter">
                <thead className="bg-slate-50 dark:bg-slate-900 border-b-2 border-slate-100 dark:border-slate-700 text-[11px] text-slate-500 tracking-[2px] font-black uppercase">
                  <tr>
                    <th className="p-6 text-left">{t.date_month}</th>
                    {isAdmin && <th className="p-6 text-left">{t.staff_name_label}</th>}
                    <th className="p-6">{t.transaction_type}</th>
                    <th className="p-6">{t.amount_label}</th>
                    <th className="p-6">{t.account_balance}</th>
                    <th className="p-6">{t.remarks_label}</th>
                    {isAdmin && <th className="p-6">{t.actions}</th>}
                  </tr>
                </thead>
                <tbody className="divide-y-2 divide-slate-50 dark:divide-slate-700">
                  {filteredHistory?.map(p => (
                    <tr key={p.id} className="hover:bg-slate-50/50 dark:hover:bg-slate-900/30 transition-colors">
                      <td className="p-6 text-left">
                         <p className="text-sm font-black text-slate-800 dark:text-white leading-none">{toBanglaNumber(p.date)}</p>
                         <p className="text-[9px] text-slate-400 mt-1 uppercase tracking-widest">{translateMonth(p.month)}</p>
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
                        <span className={`px-5 py-2 rounded-full text-[11px] font-black uppercase shadow-sm ${p.type === 'salary_add' ? 'bg-teal-100 text-teal-700' : 'bg-rose-100 text-rose-700'}`}>
                           {p.type === 'salary_add' ? t.salary_accrued : t.cash_disbursed}
                        </span>
                      </td>
                      <td className={`p-6 text-xl font-black ${p.type === 'salary_add' ? 'text-teal-600' : 'text-rose-600'}`}>
                        {p.type === 'salary_add' ? '+' : '-'} ৳{p.amount}
                      </td>
                      <td className="p-6">
                        <div className="bg-slate-100 dark:bg-slate-900/50 px-6 py-3 rounded-2xl inline-block border-2 border-slate-200 dark:border-slate-700 shadow-inner">
                           <p className="text-2xl font-black text-slate-900 dark:text-white leading-none">৳{p.newBalance}</p>
                           <p className="text-xs text-teal-600 font-black mt-2 uppercase tracking-[3px] leading-none text-center">{t.running_bal}</p>
                        </div>
                      </td>
                      <td className="p-6 text-xs text-slate-500 font-bold max-w-xs truncate italic">{p.remarks || '---'}</td>
                      {isAdmin && (
                        <td className="p-6">
                           <div className="flex items-center justify-center space-x-2">
                              <button
                                onClick={() => { setEditingPayout({...p}); setIsEditModalOpen(true); }}
                                className="w-10 h-10 bg-indigo-50 dark:bg-slate-900 rounded-2xl flex items-center justify-center text-indigo-600 hover:bg-indigo-100 transition-all hover:scale-110 shadow-sm"
                              >
                                 <i className="fas fa-edit"></i>
                              </button>
                              <button
                                onClick={() => handleDeletePayout(p)}
                                className="w-10 h-10 bg-rose-50 dark:bg-slate-900 rounded-2xl flex items-center justify-center text-rose-600 hover:bg-rose-100 transition-all hover:scale-110 shadow-sm"
                              >
                                 <i className="fas fa-trash-alt"></i>
                              </button>
                           </div>
                        </td>
                      )}
                    </tr>
                  ))}
                  {filteredHistory?.length === 0 && (
                    <tr>
                       <td colSpan={isAdmin ? 7 : 5} className="py-20 text-center opacity-30">
                          <i className="fas fa-folder-open text-6xl mb-4"></i>
                          <p className="text-xl tracking-[10px]">{t.no_records}</p>
                       </td>
                    </tr>
                  )}
                </tbody>
              </table>
          </div>
      </div>

      {/* Edit Modal */}
      {isEditModalOpen && editingPayout && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm z-[9999] flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-800 w-full max-w-2xl rounded-[48px] shadow-2xl border border-white/20 overflow-hidden transform transition-all animate-in fade-in zoom-in duration-300">
            <div className="p-8 border-b border-slate-100 dark:border-slate-700 flex justify-between items-center bg-slate-50/50 dark:bg-slate-900/50">
              <div>
                <h3 className="text-2xl font-black text-slate-800 dark:text-white uppercase tracking-tighter leading-none">Edit Salary Record</h3>
                <p className="text-[10px] text-teal-600 font-bold tracking-[3px] uppercase mt-2">Modify transaction details</p>
              </div>
              <button onClick={() => setIsEditModalOpen(false)} className="w-12 h-12 bg-white dark:bg-slate-800 rounded-2xl shadow-lg flex items-center justify-center text-slate-400 hover:text-rose-500 transition-all">
                <i className="fas fa-times"></i>
              </button>
            </div>

            <form onSubmit={handleUpdatePayout} className="p-8 space-y-6">
              <div className="grid grid-cols-2 gap-6">
                <div className="space-y-2">
                  <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest ml-4">{t.date_month} (Date)</label>
                  <input
                    type="date"
                    value={editingPayout.date}
                    onChange={(e) => setEditingPayout({...editingPayout, date: e.target.value})}
                    className="w-full bg-slate-50 dark:bg-slate-900 px-6 py-4 rounded-2xl border-none font-black text-sm shadow-inner outline-none focus:ring-2 focus:ring-teal-500/20"
                    required
                  />
                </div>
                <div className="space-y-2">
                  <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest ml-4">{t.billing_month}</label>
                  <select
                    value={editingPayout.month}
                    onChange={(e) => setEditingPayout({...editingPayout, month: e.target.value})}
                    className="w-full bg-slate-50 dark:bg-slate-900 px-6 py-4 rounded-2xl border-none font-black text-sm shadow-inner outline-none focus:ring-2 focus:ring-teal-500/20"
                  >
                    {months.map(m => <option key={m} value={m}>{m}</option>)}
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-6">
                <div className="space-y-2">
                  <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest ml-4">{t.staff_name_label}</label>
                  <select
                    value={editingPayout.staffId}
                    onChange={(e) => setEditingPayout({...editingPayout, staffId: e.target.value})}
                    className="w-full bg-slate-50 dark:bg-slate-900 px-6 py-4 rounded-2xl border-none font-black text-sm shadow-inner outline-none focus:ring-2 focus:ring-teal-500/20"
                  >
                    {store.staff?.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
                  </select>
                </div>
                <div className="space-y-2">
                  <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest ml-4">{t.transaction_type}</label>
                  <select
                    value={editingPayout.type}
                    onChange={(e) => setEditingPayout({...editingPayout, type: e.target.value})}
                    className="w-full bg-slate-50 dark:bg-slate-900 px-6 py-4 rounded-2xl border-none font-black text-sm shadow-inner outline-none focus:ring-2 focus:ring-teal-500/20"
                  >
                    <option value="salary_add">{t.salary_accrued}</option>
                    <option value="payment">{t.cash_disbursed}</option>
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-6">
                <div className="space-y-2">
                  <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest ml-4">{t.amount_label} (৳)</label>
                  <input
                    type="number"
                    value={editingPayout.amount}
                    onChange={(e) => setEditingPayout({...editingPayout, amount: e.target.value})}
                    className="w-full bg-slate-50 dark:bg-slate-900 px-6 py-4 rounded-2xl border-none font-black text-sm shadow-inner outline-none focus:ring-2 focus:ring-teal-500/20"
                    required
                  />
                </div>
                <div className="space-y-2">
                  <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest ml-4">{t.account_balance} (৳)</label>
                  <input
                    type="number"
                    value={editingPayout.newBalance}
                    onChange={(e) => setEditingPayout({...editingPayout, newBalance: e.target.value})}
                    className="w-full bg-slate-50 dark:bg-slate-900 px-6 py-4 rounded-2xl border-none font-black text-sm shadow-inner outline-none focus:ring-2 focus:ring-teal-500/20"
                    required
                  />
                </div>
              </div>

              <div className="space-y-2">
                <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest ml-4">{t.remarks_label}</label>
                <textarea
                  value={editingPayout.remarks}
                  onChange={(e) => setEditingPayout({...editingPayout, remarks: e.target.value})}
                  className="w-full bg-slate-50 dark:bg-slate-900 px-6 py-4 rounded-2xl border-none font-black text-sm shadow-inner outline-none focus:ring-2 focus:ring-teal-500/20 min-h-[100px]"
                />
              </div>

              <div className="pt-4 flex gap-4">
                <button
                  type="button"
                  onClick={() => setIsEditModalOpen(false)}
                  className="flex-1 bg-slate-100 dark:bg-slate-700 text-slate-600 dark:text-slate-300 py-4 rounded-2xl font-black text-[10px] uppercase tracking-widest transition-all hover:bg-slate-200"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="flex-1 bg-teal-600 text-white py-4 rounded-2xl font-black text-[10px] uppercase tracking-widest transition-all hover:bg-teal-700 shadow-lg shadow-teal-500/20"
                >
                  Update Record
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default SalaryHistory;
