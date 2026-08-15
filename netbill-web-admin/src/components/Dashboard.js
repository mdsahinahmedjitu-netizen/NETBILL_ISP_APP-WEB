import React, { useState } from 'react';
import { db } from '../firebaseConfig';
import { collection, addDoc, doc, updateDoc, deleteDoc } from 'firebase/firestore';

const Dashboard = ({ store, session, setActivePage, navigateToAddCustomer, t }) => {
  const [activeFilter, setActiveFilter] = useState('today');
  const [customDate, setCustomDate] = useState(new Date().toLocaleDateString('en-CA'));

  const formatDateDisplay = (dateStr) => {
    if (!dateStr) return '';
    const [year, month, day] = dateStr.split('-');
    return `${day}-${month}-${year}`;
  };

  const todayStr = new Date().toLocaleDateString('en-CA');
  const yesterdayStr = new Date(Date.now() - 86400000).toLocaleDateString('en-CA');
  const last7DaysAgoStr = new Date(Date.now() - 7 * 86400000).toLocaleDateString('en-CA');
  const currentMonthStr = todayStr.substring(0, 7);

  const filteredPayments = store.payments.filter(p => {
    const pDate = p.paymentDate;

    // Staff Isolation: Only show their own collections if role is 'staff'
    if (session?.role === 'staff') {
       if (p.collectedById !== session.data.id && p.collectedBy !== session.data.name) return false;
    }

    if (activeFilter === 'today') return pDate === todayStr;
    if (activeFilter === 'yesterday') return pDate === yesterdayStr;
    if (activeFilter === 'last7') return pDate >= last7DaysAgoStr;
    if (activeFilter === 'month') return pDate && pDate.startsWith(currentMonthStr);
    if (activeFilter === 'custom') return pDate === customDate;
    return true;
  });

  const selectedTotal = filteredPayments.reduce((s, p) => s + (p.amount || 0), 0);
  const dueTotal = store.customers.reduce((s, c) => s + (parseFloat(c.currentDue) || 0), 0);
  const activeCount = store.customers.filter(c => c.status === 'Active').length;
  const expiredCount = store.customers.filter(c => c.status !== 'Active').length;
  const targetPlan = store.settings?.monthlyTarget || 0;

  // VERIFICATION LOGIC
  const pendingRequests = store.paymentRequests?.filter(r => r.status === 'pending') || [];

  const handleApprove = async (req) => {
    try {
        const paymentDate = new Date().toLocaleDateString('en-CA');
        const timeStr = new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
        const receiptNo = "REC-" + Math.random().toString(36).substr(2, 6).toUpperCase();

        // 1. Record Payment
        await addDoc(collection(db, "payments"), {
            customerId: req.customerId, customerName: req.customerName, customerCode: req.customerCode,
            amount: req.amount, paymentMethod: req.method + " (Online)", paymentDate: paymentDate,
            receiptNo: receiptNo, transactionId: req.trxId, collectorName: "Admin Verified",
            remarks: "Approved Web Submission"
        });

        // 2. Update Customer Balance
        const customer = store.customers.find(c => c.id === req.customerId);
        if (customer) {
            const newDue = (customer.currentDue || 0) - req.amount;
            await updateDoc(doc(db, "customers", req.customerId), { currentDue: newDue });
        }

        // 3. Add Ledger
        await addDoc(collection(db, "ledger_entries"), {
            customerId: req.customerId, date: paymentDate, time: timeStr, type: "Payment",
            amount: req.amount, isDebit: false, referenceNo: receiptNo, description: "TrxID: " + req.trxId
        });

        // 4. Update Request Status
        await updateDoc(doc(db, "payment_requests", req.id), { status: 'approved' });
        alert("Payment Approved!");
    } catch (e) { alert("Action failed!"); }
  };

  const handleReject = async (id) => {
    if (window.confirm("Reject and delete this request?")) {
        await deleteDoc(doc(db, "payment_requests", id));
    }
  };

  return (
    <div className="max-w-7xl mx-auto space-y-12 pb-20 uppercase font-black tracking-widest transition-all">

      {/* PENDING VERIFICATION ALERTS */}
      {pendingRequests.length > 0 && (
        <div className="bg-rose-50 dark:bg-rose-900/20 p-10 rounded-[56px] border-4 border-rose-500/20 space-y-8 animate-pulse">
           <div className="flex items-center space-x-5 text-rose-600">
              <div className="w-14 h-14 bg-rose-500 text-white rounded-2xl flex items-center justify-center text-2xl shadow-lg">
                 <i className="fas fa-bell"></i>
              </div>
              <h3 className="text-3xl font-black uppercase tracking-tighter">Needs Verification: {pendingRequests.length}</h3>
           </div>
           <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {pendingRequests.map(req => (
                <div key={req.id} className="bg-white dark:bg-slate-800 p-8 rounded-[40px] shadow-xl border border-rose-100 flex justify-between items-center group">
                   <div className="space-y-2 leading-none">
                      <p className="text-sm font-black text-slate-400">TrxID: <span className="text-rose-500">{req.trxId}</span></p>
                      <h4 className="text-xl font-black text-slate-800 dark:text-white uppercase">{req.customerName}</h4>
                      <p className="text-2xl font-black text-emerald-600">৳ {req.amount}</p>
                   </div>
                   <div className="flex space-x-3">
                      <button onClick={() => handleApprove(req)} className="w-14 h-14 bg-emerald-500 text-white rounded-2xl shadow-lg hover:scale-110 active:scale-95 transition-all"><i className="fas fa-check"></i></button>
                      <button onClick={() => handleReject(req.id)} className="w-14 h-14 bg-rose-100 text-rose-500 rounded-2xl shadow-sm hover:scale-110 transition-all"><i className="fas fa-times"></i></button>
                   </div>
                </div>
              ))}
           </div>
        </div>
      )}

      {/* Stats Grid */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 md:gap-7 font-black uppercase tracking-widest leading-none">
        <FeatureCard title={t.payment_center} icon="fa-hand-holding-dollar" grad="grad-collection" onClick={() => setActivePage('payments')} />
        <FeatureCard title="Collection Report" icon="fa-chart-column" grad="grad-invoices" onClick={() => setActivePage('reports')} />
        <FeatureCard title={t.subscribers_crm} icon="fa-users-viewfinder" grad="grad-subscribers" onClick={() => setActivePage('customers')} />
        <FeatureCard title="Tickets" icon="fa-ticket" grad="grad-tickets" onClick={() => {}} />
        <FeatureCard title="Add New" icon="fa-user-plus" grad="grad-create" onClick={navigateToAddCustomer} />
        <FeatureCard title="Search" icon="fa-magnifying-glass-chart" grad="grad-search" onClick={() => setActivePage('customers')} />
        <FeatureCard title="Due List" icon="fa-money-bill-transfer" grad="grad-due" onClick={() => setActivePage('customers')} />
        <FeatureCard title="Summary" icon="fa-chart-pie" grad="grad-summary" onClick={() => {}} />
      </div>

      {/* Collection Breakdown Card */}
      <div className="bg-white dark:bg-slate-800 p-6 md:p-10 rounded-[32px] md:rounded-[48px] card-shadow border border-slate-100 dark:border-slate-700 space-y-8 md:space-y-10 relative overflow-hidden">
        <div className="flex flex-col md:flex-row justify-between items-start gap-4">
           <div className="space-y-2 uppercase">
              <div className="flex items-center space-x-3">
                 <div className="w-3.5 h-3.5 bg-emerald-500 rounded-full animate-pulse shadow-emerald-500/50 shadow-lg"></div>
                 <h3 className="text-2xl md:text-3xl font-black text-slate-900 dark:text-white tracking-widest uppercase">{t.todays_collection}</h3>
              </div>
              <p className="text-[10px] md:text-xs font-bold text-slate-400 tracking-[4px]">{activeFilter} Overview</p>
           </div>
           <div className="bg-teal-50 text-teal-600 px-6 py-2.5 rounded-full text-[10px] font-black border border-teal-100 shadow-sm flex items-center tracking-widest"><i className="far fa-clock mr-2"></i> LIVE AUTO</div>
        </div>

        <div className="flex flex-wrap gap-2 md:gap-4 items-center uppercase font-black">
          <button onClick={() => setActiveFilter('today')} className={`px-4 py-2 md:px-8 md:py-3 rounded-2xl text-[10px] md:text-xs font-black transition-all ${activeFilter === 'today' ? 'bg-[#0D9488] text-white shadow-lg shadow-teal-500/20 scale-105' : 'bg-slate-100 text-slate-400 hover:bg-slate-200'}`}>{t.today}</button>
          <button onClick={() => setActiveFilter('yesterday')} className={`px-4 py-2 md:px-8 md:py-3 rounded-2xl text-[10px] md:text-xs font-black transition-all ${activeFilter === 'yesterday' ? 'bg-[#0D9488] text-white shadow-lg shadow-teal-500/20 scale-105' : 'bg-slate-100 text-slate-400 hover:bg-slate-200'}`}>{t.yesterday}</button>
          <button onClick={() => setActiveFilter('last7')} className={`px-4 py-2 md:px-8 md:py-3 rounded-2xl text-[10px] md:text-xs font-black transition-all ${activeFilter === 'last7' ? 'bg-[#0D9488] text-white shadow-lg shadow-teal-500/20 scale-105' : 'bg-slate-100 text-slate-400 hover:bg-slate-200'}`}>{t.last_7_days}</button>
          <button onClick={() => setActiveFilter('month')} className={`px-4 py-2 md:px-8 md:py-3 rounded-2xl text-[10px] md:text-xs font-black transition-all ${activeFilter === 'month' ? 'bg-[#0D9488] text-white shadow-lg shadow-teal-500/20 scale-105' : 'bg-slate-100 text-slate-400 hover:bg-slate-200'}`}>{t.this_month}</button>
        </div>

        <div className="space-y-6 md:space-y-10 pt-4">
           <p className="text-[10px] font-black text-slate-400 uppercase tracking-[4px] border-b pb-4">{t.breakdown_by_sector}</p>
           <div className="space-y-6 md:space-y-8 font-black uppercase">
              <BreakdownRow method="Cash" amount={filteredPayments.filter(p=>p.paymentMethod==='Cash').reduce((s,p)=>s+p.amount,0)} color="#10B981" icon="fa-wallet" total={selectedTotal} />
              <BreakdownRow method="bKash" amount={filteredPayments.filter(p=>p.paymentMethod.includes('bKash')).reduce((s,p)=>s+p.amount,0)} color="#D0006F" icon="fa-mobile-screen-button" total={selectedTotal} />
              <BreakdownRow method="Nagad" amount={filteredPayments.filter(p=>p.paymentMethod.includes('Nagad')).reduce((s,p)=>s+p.amount,0)} color="#F58220" icon="fa-credit-card" total={selectedTotal} />
              <BreakdownRow method="Bank Transfer" amount={filteredPayments.filter(p=>p.paymentMethod.includes('Bank')).reduce((s,p)=>s+p.amount,0)} color="#0D9488" icon="fa-building-columns" total={selectedTotal} />
           </div>
        </div>
      </div>

      {/* Main Stats Summary */}
      <div className="bg-[#0D9488] p-6 md:p-12 rounded-[32px] md:rounded-[64px] text-white shadow-2xl relative overflow-hidden group">
        <div className="absolute top-0 right-0 w-[300px] md:w-[600px] h-[300px] md:h-[600px] bg-white/5 rounded-full -mr-24 md:-mr-48 -mt-24 md:-mt-48 duration-1000 group-hover:scale-110"></div>
        <div className="flex flex-col md:flex-row justify-between items-start relative z-10 font-black tracking-widest leading-none uppercase gap-8">
          <div className="space-y-6 md:space-y-12 w-full leading-none">
            <p className="text-[10px] md:text-sm font-black opacity-90 tracking-[5px] uppercase">{t.financial_total}</p>
            <h2 className="text-5xl md:text-9xl font-black tracking-tighter leading-none tracking-widest uppercase">৳ {Math.floor(selectedTotal).toLocaleString()}</h2>
            <div className="flex flex-col md:flex-row md:space-x-24 gap-8 md:gap-0 pt-6 md:pt-10 font-black tracking-widest uppercase">
               <div><p className="text-[9px] md:text-[11px] font-bold opacity-70 mb-2 md:mb-4 tracking-[3px]">{t.target_plan}</p><p className="text-xl md:text-3xl font-black">৳ {targetPlan.toLocaleString()}</p></div>
               <div className="hidden md:block w-px h-20 bg-white/20"></div>
               <div><p className="text-[9px] md:text-[11px] font-bold opacity-70 mb-2 md:mb-4 tracking-[3px] text-amber-300 uppercase">{t.total_outstanding}</p><p className="text-xl md:text-3xl text-amber-300 font-black tracking-widest uppercase">৳ {Math.floor(dueTotal).toLocaleString()}</p></div>
            </div>
          </div>
          <div className="w-16 h-16 md:w-24 md:h-24 bg-white/10 rounded-2xl md:rounded-[40px] flex items-center justify-center shadow-xl backdrop-blur-md transition-all group-hover:rotate-12"><i className="fas fa-chart-line text-2xl md:text-4xl text-white"></i></div>
        </div>
      </div>

    </div>
  );
};

const FeatureCard = ({ title, icon, grad, onClick }) => (
  <div onClick={onClick} className={`feature-card ${grad} h-40 rounded-[44px] p-8 flex flex-col justify-center text-white cursor-pointer transition-all hover:-translate-y-2 hover:brightness-110 shadow-lg font-black uppercase tracking-widest`}>
    <i className={`fas ${icon} text-4xl mb-3 opacity-90`}></i>
    <span className="font-black text-[12px] uppercase tracking-widest leading-none uppercase">{title}</span>
  </div>
);

const BreakdownRow = ({ method, amount, color, icon, total }) => {
  const p = total > 0 ? Math.floor((amount / total) * 100) : 0;
  return (
    <div className="flex items-center space-x-8 group transition-all uppercase font-black uppercase">
      <div className="w-18 h-18 rounded-[28px] bg-slate-50 dark:bg-slate-900 flex items-center justify-center text-3xl shrink-0 shadow-sm" style={{ color }}>
          <i className={`fas ${icon}`}></i>
      </div>
      <div className="flex-1 space-y-4">
          <div className="flex justify-between items-end uppercase font-black uppercase">
              <span className="text-slate-800 dark:text-slate-200 text-xl tracking-tighter uppercase font-black uppercase">{method}</span>
              <div className="flex items-center space-x-10 uppercase font-black uppercase">
                  <span className="text-slate-400 text-lg font-black">{p}%</span>
                  <span className="text-slate-900 dark:text-white text-3xl font-black uppercase tracking-tighter leading-none">৳ {Math.floor(amount).toLocaleString()}</span>
              </div>
          </div>
          <div className="h-2.5 w-full bg-slate-50 dark:bg-slate-900 rounded-full overflow-hidden shadow-inner uppercase font-black">
             <div className="h-full rounded-full shadow-lg transition-all duration-1000" style={{ width: `${p}%`, background: color }}></div>
          </div>
      </div>
    </div>
  );
};

export default Dashboard;
