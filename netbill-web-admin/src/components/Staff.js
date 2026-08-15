import React, { useState } from 'react';
import { db } from '../firebaseConfig';
import { collection, addDoc, doc, updateDoc, deleteDoc } from 'firebase/firestore';

const Staff = ({ store, session, t }) => {
  const [showModal, setShowModal] = useState(false);
  const [showPayModal, setShowPayModal] = useState(false);
  const [showReport, setShowReport] = useState(false);
  const [selectedStaff, setSelectedStaff] = useState(null);
  const [isEditing, setIsEditing] = useState(false);
  const [isProcessing, setIsProcessing] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');

  // Payment States
  const [payoutData, setPayoutData] = useState({
    type: 'payment', // 'salary_add' or 'payment'
    amount: 0,
    month: new Date().toLocaleString('default', { month: 'long', year: 'numeric' }),
    remarks: ''
  });

  // Auto-register TOMA APA if missing (as requested)
  React.useEffect(() => {
    const checkAndAddToma = async () => {
      const tomaExists = store.staff?.some(s => s.name === 'TOMA APA');
      if (!tomaExists && store.staff?.length > 0) {
        try {
          await addDoc(collection(db, "staff"), {
            name: 'TOMA APA',
            mobile: '01XXXXXXXXX',
            role: 'Collector',
            salary: 0,
            password: 'toma' + Math.floor(Math.random() * 1000),
            zone: 'All',
            status: 'Active'
          });
        } catch (e) { console.error("Auto-sync failed", e); }
      }
    };
    checkAndAddToma();
  }, [store.staff]);

  const initialState = {
    name: '',
    mobile: '',
    role: 'Lineman',
    salary: 0,
    password: '',
    zone: 'All',
    status: 'Active',
    balance: 0 // New field to track Due/Advance
  };
  const [formData, setFormData] = useState(initialState);

  const openAddModal = () => {
    setIsEditing(false);
    setFormData(initialState);
    setShowModal(true);
  };

  const openEditModal = (staff) => {
    setIsEditing(true);
    setFormData(staff);
    setShowModal(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    try {
      if (isEditing) {
        await updateDoc(doc(db, "staff", formData.id), formData);
        alert("Staff updated successfully!");
      } else {
        await addDoc(collection(db, "staff"), formData);
        alert("Staff recruited successfully!");
      }
      setShowModal(false);
    } catch (error) {
      alert("Action failed!");
    }
  };

  const openPayModal = (staff) => {
    setSelectedStaff(staff);
    setPayoutData({
      type: 'payment',
      amount: staff.balance > 0 ? staff.balance : 0,
      month: new Date().toLocaleString('default', { month: 'long', year: 'numeric' }),
      remarks: ''
    });
    setShowPayModal(true);
  };

  const handlePaySalary = async (e) => {
    e.preventDefault();
    if (!selectedStaff || isProcessing) return;

    setIsProcessing(true);
    try {
      const amt = parseFloat(payoutData.amount) || 0;
      let newBalance = selectedStaff.balance || 0;
      let actionLabel = '';

      if (payoutData.type === 'salary_add') {
        newBalance += amt;
        actionLabel = `Salary Added: ৳${amt}`;
      } else {
        newBalance -= amt;
        actionLabel = `Payment Disbursed: ৳${amt}`;
      }

      const todayISO = new Date().toLocaleDateString('en-CA');

      // 1. Update Staff Balance in DB
      await updateDoc(doc(db, "staff", selectedStaff.id), { balance: newBalance });

      // 2. Record Payout
      await addDoc(collection(db, "staff_payouts"), {
        staffId: selectedStaff.id,
        staffName: selectedStaff.name,
        month: payoutData.month,
        amount: amt,
        type: payoutData.type,
        newBalance: newBalance,
        date: todayISO,
        remarks: payoutData.remarks,
        createdAt: new Date().toISOString()
      });

      // 3. Add to Expenses if it's a payment
      if (payoutData.type === 'payment') {
        // More robust check: check for recent expenses (last 5 mins) to prevent exact duplicates
        const fiveMinsAgo = new Date(Date.now() - 5 * 60000).toISOString();
        const existingExpense = store.expenses?.find(e =>
          e.category === 'Staff Salary' &&
          e.amount === amt &&
          e.expenseDate === todayISO &&
          e.title?.includes(selectedStaff.name) &&
          (e.createdAt || '') > fiveMinsAgo
        );

        if (!existingExpense) {
          await addDoc(collection(db, "expenses"), {
            category: 'Staff Salary',
            title: `Salary Pmt: ${selectedStaff.name} (${payoutData.month})`,
            amount: amt,
            expenseDate: todayISO,
            expenseBy: session?.data?.name || 'Admin',
            remarks: payoutData.remarks || 'Automated from Staff Panel',
            createdAt: new Date().toISOString()
          });
        }
      }

      // 4. SMS Notification Logic
      let smsText = "";
      if (payoutData.type === 'salary_add') {
        smsText = `Dear ${selectedStaff.name}, BDT ${amt} salary for ${payoutData.month} has been added. Current Balance: BDT ${newBalance}.`;
      } else {
        const balanceText = newBalance >= 0 ? `Bakki Pao-na: ${newBalance}` : `Advance: ${Math.abs(newBalance)}`;
        smsText = `Dear ${selectedStaff.name}, BDT ${amt} has been paid to you. ${balanceText}. Thank you.`;
      }

      console.log(`SENDING SMS TO ${selectedStaff.mobile}: ${smsText}`);

      alert(`${actionLabel}. Current Balance: ৳${newBalance}`);
      setShowPayModal(false);
    } catch (error) {
      alert("Action failed!");
    } finally {
      setIsProcessing(false);
    }
  };

  const handleDelete = async (id) => {
    if (window.confirm("Are you sure you want to delete this staff member?")) {
      try {
        await deleteDoc(doc(db, "staff", id));
        alert("Staff deleted!");
      } catch (error) {
        alert("Delete failed!");
      }
    }
  };

  const currentMonth = new Date().toLocaleDateString('en-CA').substring(0, 7);
  const filteredStaff = store.staff?.filter(s =>
    s.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    s.mobile?.includes(searchTerm) ||
    s.role?.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const syncTomaCustomers = async () => {
    const targets = store.customers.filter(c => c.assignedStaffId === 'fpxuiPnVBRmxep1Q7llI');
    if (targets.length === 0) return alert("No customers found with ID fpxuiPnVBRmxep1Q7llI!");

    let count = 0;
    for (const c of targets) {
       try {
         await updateDoc(doc(db, "customers", c.id), { assignedStaffId: 'TOMA APA' });
         count++;
       } catch (e) { console.error(e); }
    }
    alert(`Successfully updated ${count} customers to show TOMA APA!`);
  };

  return (
    <div className="w-full px-4 space-y-8 pb-20 uppercase font-black tracking-tighter transition-all">
      {/* Header */}
      <div className="flex flex-col xl:flex-row justify-between items-start xl:items-center gap-4 bg-white dark:bg-slate-800 p-6 rounded-3xl shadow-xl border border-slate-100 dark:border-slate-700">
        <div className="space-y-1">
          <h3 className="text-3xl font-black text-slate-800 dark:text-white uppercase tracking-tighter leading-none">Staff Team</h3>
          <p className="text-[10px] text-indigo-600 font-bold tracking-[4px] uppercase mt-1">Lineman & Management Staff</p>
        </div>
        <div className="flex flex-wrap gap-3">
           <div className="relative mr-4">
             <input
               type="text"
               placeholder="Search Staff..."
               value={searchTerm}
               onChange={(e) => setSearchTerm(e.target.value)}
               className="pl-10 pr-4 py-3 bg-slate-50 dark:bg-slate-900 rounded-2xl border-none text-[10px] font-black w-64 shadow-inner outline-none focus:ring-2 focus:ring-indigo-500/20"
             />
             <i className="fas fa-search absolute left-4 top-3.5 text-slate-300 text-xs"></i>
           </div>
           <StatCard label="TOTAL STAFF" value={store.staff?.length} color="slate" />
           <StatCard label="FIELD STAFF" value={store.staff?.filter(s => s.role === 'Lineman').length} color="indigo" />
           <button onClick={syncTomaCustomers} className="bg-emerald-600 text-white px-6 py-4 rounded-2xl shadow-lg font-black text-[10px] uppercase tracking-widest transition-all hover:bg-emerald-700 active:scale-95 flex items-center space-x-2"><i className="fas fa-sync"></i><span>Sync TOMA Customers</span></button>
           <button onClick={() => setShowReport(true)} className="bg-slate-800 text-white px-6 py-4 rounded-2xl shadow-lg font-black text-[10px] uppercase tracking-widest transition-all hover:bg-slate-900 active:scale-95 flex items-center space-x-2"><i className="fas fa-file-invoice-dollar"></i><span>Payroll Report</span></button>
           <button onClick={openAddModal} className="bg-indigo-600 text-white px-8 py-4 rounded-2xl shadow-2xl font-black uppercase text-sm tracking-[2px] transition-all hover:scale-105 active:scale-95 border-b-4 border-indigo-900">+ Recruit Staff</button>
        </div>
      </div>

      {/* Staff Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
        {filteredStaff?.map(s => {
          const assignedCount = store.customers.filter(c => c.assignedStaffId === s.name || c.assignedStaffId === s.id).length;
          const monthCollection = store.payments
            .filter(p => (p.collectedBy === s.id || p.collectedBy === s.name) && p.paymentDate?.startsWith(currentMonth))
            .reduce((sum, p) => sum + (p.amount || 0), 0);

          return (
            <div key={s.id} onClick={() => setSelectedStaff(s)} className={`bg-white dark:bg-slate-800 p-6 rounded-3xl border border-slate-100 dark:border-slate-700 shadow-xl flex flex-col space-y-6 cursor-pointer hover:-translate-y-1 transition-all ${selectedStaff?.id === s.id ? 'ring-4 ring-indigo-500' : ''}`}>
              <div className="flex justify-between items-start">
                <div className="w-16 h-16 bg-teal-50 dark:bg-slate-900 rounded-2xl flex items-center justify-center text-teal-600 text-3xl shadow-inner">
                  <i className="fas fa-user-tie"></i>
                </div>
                <div className="flex space-x-2">
                   <button onClick={(e) => { e.stopPropagation(); openEditModal(s); }} className="w-8 h-8 rounded-lg bg-slate-50 dark:bg-slate-900 text-slate-400 hover:text-indigo-600 transition-colors shadow-sm"><i className="fas fa-edit text-xs"></i></button>
                   <button onClick={(e) => { e.stopPropagation(); handleDelete(s.id); }} className="w-8 h-8 rounded-lg bg-slate-50 dark:bg-slate-900 text-slate-400 hover:text-rose-600 transition-colors shadow-sm"><i className="fas fa-trash text-xs"></i></button>
                </div>
              </div>

              <div>
                <h4 className="text-xl font-black text-slate-800 dark:text-white tracking-tighter uppercase leading-none">{s.name}</h4>
                <p className="text-[9px] font-black text-teal-600 uppercase tracking-[3px] mt-1.5">{s.role} • {s.mobile}</p>
              </div>

              <div className="grid grid-cols-2 gap-3">
                 <div className="bg-slate-50 dark:bg-slate-900 p-3 rounded-2xl text-center border border-slate-100 dark:border-slate-800 shadow-inner">
                    <p className="text-[8px] text-slate-400 mb-1 font-black tracking-widest uppercase">ACCOUNT BALANCE</p>
                    <p className={`text-xl font-black tracking-tighter leading-none ${s.balance >= 0 ? 'text-rose-500' : 'text-emerald-600'}`}>
                      {s.balance >= 0 ? `+ ৳${s.balance}` : `- ৳${Math.abs(s.balance)}`}
                    </p>
                    <p className="text-[7px] font-bold mt-1 text-slate-400 uppercase">{s.balance >= 0 ? 'Pao-na (Due)' : 'Advance'}</p>
                 </div>
                 <div className="bg-slate-50 dark:bg-slate-900 p-3 rounded-2xl text-center border border-slate-100 dark:border-slate-800 shadow-inner">
                    <p className="text-[8px] text-slate-400 mb-1 font-black tracking-widest uppercase">COLLECTION</p>
                    <p className="text-xl font-black text-emerald-600 tracking-tighter leading-none">৳{Math.floor(monthCollection)}</p>
                 </div>
              </div>

              <div className="bg-slate-50 dark:bg-slate-900 w-full py-3 rounded-2xl font-black text-slate-500 text-xs tracking-[2px] text-center border border-slate-100 dark:border-slate-800 shadow-inner">
                ৳ {s.salary} / MONTH
              </div>

              <button
                onClick={(e) => { e.stopPropagation(); openPayModal(s); }}
                className="w-full bg-emerald-600 text-white py-4 rounded-2xl font-black text-[10px] uppercase tracking-[3px] shadow-lg hover:bg-emerald-700 transition-all border-b-4 border-emerald-900"
              >
                <i className="fas fa-hand-holding-dollar mr-2"></i> Pay Salary
              </button>
            </div>
          );
        })}
      </div>

      {/* Recruit/Edit Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-slate-900/90 backdrop-blur-xl z-[1000] flex items-center justify-center p-6 animate-fadeIn font-black uppercase">
          <div className="bg-white dark:bg-slate-800 rounded-[48px] w-full max-w-2xl p-12 shadow-2xl space-y-8 relative overflow-hidden border-2 border-slate-100 dark:border-slate-700">
             <div className="flex justify-between items-center border-b pb-6">
                <h3 className="text-4xl font-black uppercase tracking-tighter">{isEditing ? 'Update Staff' : 'Recruit Staff'}</h3>
                <button onClick={() => setShowModal(false)} className="w-12 h-12 bg-rose-50 text-rose-500 rounded-full flex items-center justify-center"><i className="fas fa-times"></i></button>
             </div>

             <form onSubmit={handleSave} className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <Field label="Staff Name *" value={formData.name} onChange={v => setFormData({...formData, name: v})} placeholder="Full Name" />
                <Field label="Mobile / Username *" value={formData.mobile} onChange={v => setFormData({...formData, mobile: v})} placeholder="017xxxxxxxx" />
                <Field label="Login Password *" value={formData.password} onChange={v => setFormData({...formData, password: v})} type="password" />
                <Field label="Designation / Role" value={formData.role} onChange={v => setFormData({...formData, role: v})} type="select" options={['Collector', 'Lineman', 'Management', 'Support', 'Admin']} />
                <Field label="Monthly Salary (৳)" value={formData.salary} onChange={v => setFormData({...formData, salary: parseFloat(v) || 0})} type="number" />
                <Field label="Zone Coverage" value={formData.zone} onChange={v => setFormData({...formData, zone: v})} placeholder="E.g. Zone A, Zone B" />
                <div className="md:col-span-2 pt-6">
                  <button type="submit" className="w-full bg-indigo-600 text-white py-6 rounded-3xl font-black uppercase tracking-[10px] shadow-2xl hover:scale-[1.02] active:scale-95 transition-all border-b-8 border-indigo-900">
                    {isEditing ? 'UPDATE STAFF IDENTITY' : 'COMMIT RECRUITMENT'}
                  </button>
                </div>
             </form>
          </div>
        </div>
      )}

      {/* SALARY PAY MODAL */}
      {showPayModal && (
        <div className="fixed inset-0 bg-slate-900/90 backdrop-blur-xl z-[3000] flex items-center justify-center p-6 animate-fadeIn font-black uppercase">
          <div className="bg-white dark:bg-slate-800 rounded-[48px] w-full max-w-xl p-12 shadow-2xl space-y-8 relative border-2 border-slate-100 dark:border-slate-700">
             <div className="flex justify-between items-center border-b pb-6">
                <div>
                   <h3 className="text-4xl font-black uppercase tracking-tighter">Pay Salary</h3>
                   <p className="text-xs text-teal-600 font-bold mt-1 tracking-[4px]">{selectedStaff?.name}</p>
                </div>
                <button onClick={() => setShowPayModal(false)} className="w-12 h-12 bg-rose-50 text-rose-500 rounded-full flex items-center justify-center"><i className="fas fa-times"></i></button>
             </div>

             <form onSubmit={handlePaySalary} className="space-y-6">
                <div className="flex bg-slate-50 dark:bg-slate-900 p-2 rounded-2xl mb-4">
                   <button type="button" onClick={() => setPayoutData({...payoutData, type: 'salary_add', amount: selectedStaff.salary})} className={`flex-1 py-3 rounded-xl text-[10px] transition-all ${payoutData.type === 'salary_add' ? 'bg-white dark:bg-slate-800 shadow-md text-teal-600' : 'text-slate-400'}`}>Add Monthly Salary</button>
                   <button type="button" onClick={() => setPayoutData({...payoutData, type: 'payment', amount: selectedStaff.balance > 0 ? selectedStaff.balance : 0})} className={`flex-1 py-3 rounded-xl text-[10px] transition-all ${payoutData.type === 'payment' ? 'bg-white dark:bg-slate-800 shadow-md text-teal-600' : 'text-slate-400'}`}>Disburse Payment</button>
                </div>

                <div className="grid grid-cols-2 gap-4">
                   <div className="col-span-2"><Field label="Target Month" value={payoutData.month} onChange={v => setPayoutData({...payoutData, month: v})} type="select" options={[payoutData.month, 'Next Month', 'Previous Month']} /></div>
                   <div className="col-span-2">
                     <Field label={payoutData.type === 'salary_add' ? "Salary to Add (৳)" : "Disburse Amount (৳)"} value={payoutData.amount} onChange={v => setPayoutData({...payoutData, amount: parseFloat(v) || 0})} type="number" />
                   </div>
                </div>

                <div className="bg-emerald-50 dark:bg-emerald-900/20 p-6 rounded-2xl border-2 border-emerald-100 dark:border-emerald-800 text-center">
                   <p className="text-[10px] text-emerald-600 font-black tracking-widest uppercase">Projected Account Balance</p>
                   <p className="text-4xl text-emerald-600 font-black tracking-tighter leading-none mt-2">
                      ৳{payoutData.type === 'salary_add' ? (selectedStaff.balance + payoutData.amount) : (selectedStaff.balance - payoutData.amount)}
                   </p>
                   <p className="text-[8px] text-slate-400 mt-2 italic">* Balance updates in real-time after confirmation.</p>
                </div>

                <Field label="Payment Remarks" value={payoutData.remarks} onChange={v => setPayoutData({...payoutData, remarks: v})} placeholder="Optional notes..." />

                <button type="submit" className={`w-full py-6 rounded-3xl font-black uppercase tracking-[10px] shadow-2xl hover:scale-[1.02] active:scale-95 transition-all border-b-8 ${payoutData.type === 'salary_add' ? 'bg-teal-600 border-teal-900' : 'bg-emerald-600 border-emerald-900'} text-white`}>
                  {payoutData.type === 'salary_add' ? 'CONFIRM SALARY ADD' : 'CONFIRM DISBURSEMENT'}
                </button>
             </form>
          </div>
        </div>
      )}

      {/* PAYROLL REPORT MODAL */}
      {showReport && (
        <div className="fixed inset-0 bg-slate-900/95 backdrop-blur-3xl z-[3000] flex items-center justify-center p-6 animate-fadeIn font-black uppercase">
          <div className="bg-white dark:bg-slate-800 rounded-[56px] w-full max-w-4xl p-12 shadow-2xl space-y-8 relative border-2 border-slate-100 dark:border-slate-700">
             <div className="flex justify-between items-center border-b pb-6">
                <div>
                   <h3 className="text-4xl font-black uppercase tracking-tighter text-slate-800 dark:text-white">Monthly Payroll Summary</h3>
                   <p className="text-xs text-indigo-600 font-bold mt-1 tracking-[4px]">{currentMonth}</p>
                </div>
                <button onClick={() => setShowReport(false)} className="w-12 h-12 bg-rose-50 text-rose-500 rounded-full flex items-center justify-center transition-transform hover:rotate-90"><i className="fas fa-times"></i></button>
             </div>

             <div className="overflow-x-auto max-h-[500px] custom-scrollbar">
                <table className="w-full text-center border-collapse">
                   <thead>
                      <tr className="text-[10px] text-slate-400 border-b">
                         <th className="py-4">STAFF NAME</th>
                         <th className="py-4">BASE SALARY</th>
                         <th className="py-4">COLLECTION</th>
                         <th className="py-4">BONUS/OTHER</th>
                         <th className="py-4">TOTAL PAYABLE</th>
                         <th className="py-4">STATUS</th>
                      </tr>
                   </thead>
                   <tbody className="divide-y divide-slate-50 dark:divide-slate-700">
                      {store.staff?.map(s => {
                         const collection = store.payments
                            .filter(p => (p.collectedById === s.id || p.collectedBy === s.name) && p.paymentDate?.startsWith(currentMonth))
                            .reduce((sum, p) => sum + (p.amount || 0), 0);
                         const isPaid = store.staffPayouts?.some(p => p.staffId === s.id && p.month === payoutData.month);

                         return (
                            <tr key={s.id} className="text-sm">
                               <td className="py-5 font-black text-slate-700 dark:text-slate-200">{s.name}</td>
                               <td className="py-5">৳{s.salary}</td>
                               <td className="py-5 text-indigo-600">৳{Math.floor(collection)}</td>
                               <td className="py-5 text-emerald-600">--</td>
                               <td className="py-5 font-black text-slate-900 dark:text-white">৳{s.salary}</td>
                               <td className="py-5">
                                  <span className={`px-4 py-1.5 rounded-full text-[10px] font-black ${isPaid ? 'bg-emerald-100 text-emerald-700' : 'bg-rose-100 text-rose-700'}`}>
                                     {isPaid ? 'PAID' : 'UNPAID'}
                                  </span>
                               </td>
                            </tr>
                         );
                      })}
                   </tbody>
                </table>
             </div>

             <div className="pt-6 border-t flex justify-end">
                <button onClick={() => window.print()} className="bg-indigo-600 text-white px-10 py-4 rounded-2xl font-black text-xs tracking-widest hover:scale-105 transition-all">PRINT PAYROLL</button>
             </div>
          </div>
        </div>
      )}

      {/* Staff Sidebar (Optional future) */}
      {selectedStaff && (
         <div className="fixed top-0 right-0 w-full xl:w-[400px] h-full bg-white dark:bg-slate-800 shadow-2xl z-[2000] p-10 animate-slideInRight font-black uppercase overflow-y-auto border-l border-slate-100 dark:border-slate-700">
            <div className="flex justify-between items-center mb-10">
               <h4 className="text-sm font-black text-slate-400 tracking-[5px]">Staff Details</h4>
               <button onClick={() => setSelectedStaff(null)} className="w-10 h-10 bg-rose-50 text-rose-500 rounded-full flex items-center justify-center shadow-lg"><i className="fas fa-times"></i></button>
            </div>

            <div className="text-center space-y-6 mb-10">
               <div className="w-32 h-32 bg-indigo-50 dark:bg-slate-900 rounded-[40px] flex items-center justify-center mx-auto text-indigo-600 text-6xl shadow-inner border-2 border-indigo-100">
                  <i className="fas fa-user-tie"></i>
               </div>
               <div>
                  <h4 className="text-3xl font-black text-slate-800 dark:text-white tracking-tighter leading-none">{selectedStaff.name}</h4>
                  <p className="text-xs text-indigo-600 font-bold mt-2 tracking-[4px]">{selectedStaff.role}</p>
               </div>
            </div>

            <div className="space-y-6">
               <div className="bg-slate-50 dark:bg-slate-900 p-6 rounded-3xl border border-slate-100 dark:border-slate-800 shadow-inner">
                  <p className="text-[9px] text-slate-400 mb-2 font-black tracking-widest">PERSONAL CONTACT</p>
                  <p className="text-xl font-black text-slate-800 dark:text-white">{selectedStaff.mobile}</p>
               </div>
               <div className="bg-slate-50 dark:bg-slate-900 p-6 rounded-3xl border border-slate-100 dark:border-slate-800 shadow-inner">
                  <p className="text-[9px] text-slate-400 mb-2 font-black tracking-widest">ASSIGNED ZONE</p>
                  <p className="text-xl font-black text-slate-800 dark:text-white">{selectedStaff.zone || 'All Zones'}</p>
               </div>
            </div>

            <button onClick={() => { setShowPayModal(true); }} className="w-full bg-[#0D9488] text-white py-5 rounded-2xl font-black uppercase tracking-[5px] mt-6 shadow-xl hover:brightness-110">Manage Payments</button>
            <button onClick={() => openEditModal(selectedStaff)} className="w-full bg-slate-800 text-white py-5 rounded-2xl font-black uppercase tracking-[5px] mt-4 hover:bg-slate-700 transition-all">Edit Staff Profile</button>
         </div>
      )}
    </div>
  );
};

const StatCard = ({ label, value, color }) => {
  const colors = {
      slate: "bg-slate-100 dark:bg-slate-900 text-slate-600 border-slate-200 dark:border-slate-800 shadow-slate-200/50",
      indigo: "bg-indigo-50 dark:bg-indigo-900/20 text-indigo-600 border-indigo-100 dark:border-indigo-800 shadow-indigo-500/10",
  };
  return (
      <div className={`${colors[color]} px-6 py-3 rounded-2xl border flex flex-col items-center justify-center min-w-[120px] shadow-lg`}>
          <p className="text-[9px] font-black uppercase tracking-[3px] opacity-60 mb-1">{label}</p>
          <p className="text-2xl font-black tracking-tighter leading-none">{value}</p>
      </div>
  );
};

const Field = ({ label, value, onChange, placeholder, type = 'text', options = [] }) => (
  <div className="space-y-3 uppercase font-black">
    <label className="text-[10px] text-slate-400 ml-4 tracking-[3px] uppercase leading-none">{label}</label>
    {type === 'select' ? (
      <select value={value} onChange={e => onChange(e.target.value)} className="w-full bg-slate-50 dark:bg-slate-900 p-5 rounded-3xl border-none font-black text-sm outline-none cursor-pointer focus:ring-4 focus:ring-indigo-500/10 transition-all">
        {options.map(opt => <option key={opt} value={opt}>{opt}</option>)}
      </select>
    ) : (
      <input type={type} value={value} onChange={e => onChange(e.target.value)} placeholder={placeholder} className="w-full bg-slate-50 dark:bg-slate-900 p-5 rounded-3xl border-none font-black text-lg shadow-inner outline-none focus:ring-4 focus:ring-indigo-500/10 transition-all" />
    )}
  </div>
);

export default Staff;
