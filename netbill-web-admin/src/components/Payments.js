import React, { useState, useMemo, useEffect } from 'react';
import { db } from '../firebaseConfig';
import { collection, addDoc, doc, updateDoc, onSnapshot, query, where, orderBy, limit, deleteDoc, getDocs } from 'firebase/firestore';

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

  // Edit States
  const [editingPayment, setEditingPayment] = useState(null);
  const [editAmount, setEditEditAmount] = useState('');
  const [showEditModal, setShowEditModal] = useState(false);

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

    const q = query(collection(db, "payments"), where("customerId", "==", selectedCustomerId), orderBy("paymentDate", "desc"), limit(20));
    const unsub = onSnapshot(q, (snap) => {
      setCustomerPayments(snap.docs.map(d => ({ id: d.id, ...d.data() })));
    });
    return () => unsub();
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
    const timeStr = new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });

    try {
      const finalRemarks = `${billingMonth} Bill - ${method}`;
      const payAmt = parseFloat(amount);
      let newDue = (customer.currentDue || 0);
      let newAdvance = (customer.advanceBalance || 0);

      if (payAmt > newDue) {
          const excess = payAmt - newDue;
          newAdvance += excess;
          newDue = 0;
      } else {
          newDue -= payAmt;
      }

      await addDoc(collection(db, "payments"), {
        customerId: selectedCustomerId, customerName: customer.name, customerCode: customer.customerCode,
        amount: payAmt, paymentMethod: method, paymentDate: todayISO, billingMonth, receiptNo: `REC-${Date.now().toString().slice(-6)}`, remarks: finalRemarks,
        collectedBy: selectedCollector.name,
        collectedById: selectedCollector.id
      });

      await updateDoc(doc(db, "customers", selectedCustomerId), {
          currentDue: newDue,
          advanceBalance: newAdvance
      });

      await addDoc(collection(db, "ledger_entries"), {
        customerId: selectedCustomerId, date: todayISO, time: timeStr, type: "Payment", description: `Payment for ${billingMonth}`, amount: payAmt, isDebit: false, runningBalance: newDue
      });
      alert(t.success_payment);
      setAmount(''); setSelectedCustomerId(''); setSearchTerm('');
    } catch (e) { alert("Error!"); } finally { setIsProcessing(false); }
  };

  const openEditModal = (p) => {
    setEditingPayment(p);
    setEditEditAmount(p.amount.toString());
    setShowEditModal(true);
  };

  const handleUpdatePayment = async (e) => {
    e.preventDefault();
    if (!editingPayment || !editAmount) return;

    setIsProcessing(true);
    try {
      const oldAmount = editingPayment.amount;
      const newAmount = parseFloat(editAmount);
      const diff = newAmount - oldAmount;

      // 1. Update Payment Doc
      await updateDoc(doc(db, "payments", editingPayment.id), {
        amount: newAmount,
        remarks: editingPayment.remarks + " (Edited)"
      });

      // 2. Adjust Customer Due (Diff subtracted from Due)
      const customer = store.customers.find(c => c.id === editingPayment.customerId);
      if (customer) {
        const newDue = (customer.currentDue || 0) - diff;
        await updateDoc(doc(db, "customers", customer.id), { currentDue: newDue });

        // 3. Update Ledger Entry (Optional but good)
        const q = query(collection(db, "ledger_entries"), where("referenceNo", "==", editingPayment.receiptNo));
        const snap = await getDocs(q);
        if (!snap.empty) {
           await updateDoc(doc(db, "ledger_entries", snap.docs[0].id), {
             amount: newAmount,
             description: snap.docs[0].data().description + " (Corrected)"
           });
        }
      }

      alert("Collection updated successfully!");
      setShowEditModal(false);
    } catch (err) {
      alert("Failed to update collection.");
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <div className="max-w-6xl mx-auto space-y-12 pb-20 uppercase font-black tracking-tighter transition-all">
      <div className="space-y-2 uppercase">
        <h3 className="text-6xl font-black text-slate-800 dark:text-white tracking-tighter uppercase leading-none">{t.payment_center}</h3>
        <p className="text-xs text-teal-600 tracking-widest font-black uppercase italic">Collection Hub • Financial Controller</p>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-2 gap-12">
        {/* NEW COLLECTION FORM */}
        <div className="bg-white dark:bg-slate-800 p-12 rounded-[56px] shadow-2xl border border-slate-100 dark:border-slate-700 font-black">
           <div className="mb-10 bg-indigo-50 dark:bg-indigo-900/20 p-6 rounded-[40px] border-2 border-indigo-100 dark:border-indigo-800">
              <label className="text-[10px] font-black text-indigo-600 uppercase tracking-[4px] ml-4 block mb-3">Active Collector (Session Lock)</label>
              <select
                value={selectedCollector.id}
                onChange={(e) => {
                  const id = e.target.value;
                  const name = e.target.options[e.target.selectedIndex].text;
                  setSelectedCollector({ id, name });
                }}
                className="w-full bg-white dark:bg-slate-800 border-none p-4 rounded-2xl font-black text-sm text-slate-800 dark:text-white uppercase shadow-sm outline-none cursor-pointer"
              >
                <option value={session?.data?.id || 'admin'}>{session?.data?.name || 'Super Admin'} (YOU)</option>
                {store.staff?.filter(s => s.id !== session?.data?.id).map(s => (
                  <option key={s.id} value={s.id}>{s.name}</option>
                ))}
              </select>
           </div>

           <form onSubmit={handlePayment} className="space-y-10 uppercase">
              <div className="space-y-4">
                <label className="text-[11px] font-black text-slate-400 uppercase tracking-[4px] ml-4">{t.find_subscriber}</label>
                <div className="relative space-y-6">
                  <input
                    type="text"
                    placeholder={t.search_placeholder}
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    className="w-full bg-slate-50 dark:bg-slate-900 border-none p-8 rounded-[40px] font-black text-2xl shadow-inner focus:ring-4 focus:ring-teal-500/10 transition-all outline-none"
                  />
                  <select
                    value={selectedCustomerId}
                    size={searchTerm.length > 0 ? 6 : 1}
                    onChange={(e) => {
                      const id = e.target.value; setSelectedCustomerId(id);
                      const cust = store.customers.find(c => c.id === id);
                      if (cust) {
                        setAmount(Math.floor(cust.currentDue || 0).toString());
                        setSearchTerm('');
                      }
                    }}
                    className={`w-full bg-white dark:bg-slate-800 border-4 border-teal-500/10 p-7 rounded-[40px] font-black text-xl text-slate-800 dark:text-white uppercase shadow-xl outline-none cursor-pointer hover:border-teal-500 transition-all ${searchTerm.length > 0 ? 'ring-8 ring-teal-500/5 mt-2' : ''}`}
                    required
                  >
                    <option value="" className="p-4">-- {filteredCustomers.length} Results Found --</option>
                    {filteredCustomers.map(c => (
                      <option key={c.id} value={c.id} className="p-4 border-b border-slate-50 dark:border-slate-700">
                        {c.name} - {c.zone || 'Global'} - DUE: ৳{Math.floor(c.currentDue)}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                 <div className="space-y-4">
                    <label className="text-[11px] font-black text-slate-400 uppercase tracking-[4px] ml-4">{t.billing_month}</label>
                    <select value={billingMonth} onChange={(e) => setBillingMonth(e.target.value)} className="w-full bg-slate-50 dark:bg-slate-900 border-none p-6 rounded-[32px] font-black uppercase text-sm cursor-pointer">
                      {months.map(m => <option key={m} value={m}>{m}</option>)}
                    </select>
                 </div>
                 <div className="space-y-4">
                    <label className="text-[11px] font-black text-slate-400 uppercase tracking-[4px] ml-4">{t.method}</label>
                    <select value={method} onChange={(e) => setMethod(e.target.value)} className="w-full bg-slate-50 dark:bg-slate-900 border-none p-6 rounded-[32px] font-black uppercase text-sm cursor-pointer">
                      <option value="Cash">Cash</option><option value="bKash">bKash</option><option value="Nagad">Nagad</option><option value="Rocket">Rocket</option><option value="Bank">Bank</option>
                    </select>
                 </div>
              </div>

              <div className="space-y-4">
                <label className="text-[11px] text-slate-400 uppercase tracking-[4px] ml-4">ENTER COLLECTION AMOUNT (৳)</label>
                <input type="number" value={amount} onChange={(e) => setAmount(e.target.value)} placeholder="0.00" className="w-full bg-slate-100 dark:bg-slate-900 border-none p-8 rounded-[40px] font-black text-6xl text-teal-600 tracking-tighter shadow-inner" required />
              </div>

              <button type="submit" disabled={isProcessing} className={`w-full py-10 rounded-[48px] font-black uppercase tracking-[10px] shadow-2xl transition-all ${isProcessing ? 'bg-slate-400' : 'bg-[#0D9488] text-white hover:scale-[1.02] active:scale-95 border-b-8 border-teal-900 shadow-teal-500/30'}`}>
                {isProcessing ? 'COMMITING...' : 'COMMIT PAYMENT'}
              </button>
           </form>
        </div>

        {/* COLLECTION HISTORY & EDIT */}
        <div className="space-y-8 font-black uppercase">
           <div className="flex justify-between items-center ml-4 border-b pb-6">
              <h4 className="text-xl font-black text-slate-800 dark:text-white uppercase tracking-[5px]">Collection History</h4>
              <span className="bg-slate-100 dark:bg-slate-900 px-5 py-2 rounded-full text-[10px] font-black">LATEST 20 ENTRIES</span>
           </div>
           <div className="space-y-6 max-h-[800px] overflow-y-auto pr-4 custom-scrollbar">
              {customerPayments.length > 0 ? customerPayments.map(p => (
                <div key={p.id} className="bg-white dark:bg-slate-800 p-8 rounded-[48px] shadow-xl border border-slate-100 dark:border-slate-700 flex justify-between items-center group transition-all font-black hover:-translate-y-1 relative">
                   <div className="flex items-center space-x-8">
                      <div className="w-20 h-20 rounded-[32px] bg-teal-50 dark:bg-slate-900 text-teal-600 flex items-center justify-center text-4xl shadow-inner shrink-0 transition-transform group-hover:rotate-12">
                         <i className={`fas ${p.paymentMethod === 'Cash' ? 'fa-wallet' : p.paymentMethod === 'Bank' ? 'fa-building-columns' : 'fa-mobile-screen-button'}`}></i>
                      </div>
                      <div className="space-y-2">
                         <p className="text-2xl font-black text-slate-800 dark:text-white uppercase leading-none tracking-tighter">{p.billingMonth}</p>
                         <p className="text-[10px] font-bold text-slate-400 uppercase tracking-widest leading-none">{p.paymentMethod} • REF: {p.receiptNo}</p>
                         <button onClick={() => openEditModal(p)} className="text-[10px] font-black text-indigo-600 underline tracking-[3px] opacity-0 group-hover:opacity-100 transition-all uppercase">Edit Entry</button>
                      </div>
                   </div>
                   <div className="text-right space-y-2 font-black">
                      <p className="text-4xl font-black text-emerald-600 tracking-tighter leading-none">৳ {p.amount}</p>
                      <p className="text-xs font-black text-slate-400 uppercase tracking-[4px]">{formatDateDisplay(p.paymentDate)}</p>
                   </div>
                </div>
              )) : (
                <div className="text-center py-40 opacity-10 uppercase">
                  <i className="fas fa-hand-holding-dollar text-[120px]"></i>
                  <p className="text-2xl font-black mt-8 tracking-[15px]">No History</p>
                </div>
              )}
           </div>
        </div>
      </div>

      {/* EDIT COLLECTION MODAL */}
      {showEditModal && (
        <div className="fixed inset-0 bg-slate-900/95 backdrop-blur-2xl z-[3000] flex items-center justify-center p-6 animate-fadeIn font-black uppercase">
          <div className="bg-white dark:bg-slate-800 rounded-[72px] w-full max-w-xl p-16 shadow-2xl space-y-12 relative overflow-hidden border-2 border-slate-100 dark:border-slate-700">
             <div className="absolute top-0 left-0 w-full h-4 bg-indigo-600 shadow-lg"></div>
             <div className="flex justify-between items-center border-b pb-8">
                <div className="space-y-1">
                  <h3 className="text-4xl font-black uppercase tracking-tighter">Edit Collection</h3>
                  <p className="text-[10px] text-indigo-600 font-bold tracking-[4px]">REF: {editingPayment.receiptNo}</p>
                </div>
                <button onClick={() => setShowEditModal(false)} className="w-16 h-16 bg-rose-50 text-rose-500 rounded-full flex items-center justify-center text-2xl hover:scale-110 transition-all"><i className="fas fa-times"></i></button>
             </div>

             <div className="bg-slate-50 dark:bg-slate-900 p-8 rounded-[40px] space-y-4">
                <div className="flex justify-between text-xs font-black opacity-50"><span>CUSTOMER</span><span>PREVIOUS PAID</span></div>
                <div className="flex justify-between items-end">
                   <p className="text-2xl font-black text-slate-800 dark:text-white uppercase leading-none">{editingPayment.customerName}</p>
                   <p className="text-2xl font-black text-rose-500">৳ {editingPayment.amount}</p>
                </div>
             </div>

             <form onSubmit={handleUpdatePayment} className="space-y-8">
                <div className="space-y-4">
                  <label className="text-[11px] text-slate-400 ml-4 tracking-[5px] font-black uppercase">Corrected Amount (৳)</label>
                  <input
                    type="number"
                    value={editAmount}
                    onChange={e => setEditEditAmount(e.target.value)}
                    className="w-full bg-slate-50 dark:bg-slate-900 border-none p-10 rounded-[48px] font-black text-6xl text-indigo-600 tracking-tighter text-center"
                    required
                  />
                </div>
                <button
                  type="submit"
                  disabled={isProcessing}
                  className="w-full bg-indigo-600 text-white py-8 rounded-[48px] font-black uppercase tracking-[10px] shadow-2xl hover:scale-105 active:scale-95 transition-all border-b-8 border-indigo-900"
                >
                  {isProcessing ? 'UPDATING...' : 'UPDATE COLLECTION'}
                </button>
                <p className="text-[9px] text-slate-400 text-center italic">* This will automatically adjust the customer's current balance.</p>
             </form>
          </div>
        </div>
      )}

    </div>
  );
};

const ActionButtonLarge = ({ label, icon, onClick }) => (
  <button onClick={onClick} className="bg-[#20879e] text-white px-10 py-5 rounded-[28px] font-black text-sm flex items-center space-x-4 shadow-[0_20px_40px_rgba(32,135,158,0.15)] hover:scale-105 active:scale-95 transition-all uppercase tracking-widest border-b-4 border-[#16667a] leading-none">
    <i className={`fas ${icon} text-xl`}></i>
    <span>{label}</span>
  </button>
);

export default Payments;
