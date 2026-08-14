import React, { useState } from 'react';
import { db } from '../firebaseConfig';
import { collection, addDoc } from 'firebase/firestore';

const CustomerPortal = ({ customer, store, t, onLogout }) => {
  const [isProcessing, setIsProcessing] = useState(false);
  const [showPayModal, setShowPayModal] = useState(false);
  const [trxId, setTrxId] = useState('');
  const [selectedMethod, setSelectedMethod] = useState('bKash');
  const currency = "৳";

  const myPayments = store.payments
    .filter(p => p.customerId === customer.id)
    .sort((a,b) => b.paymentDate?.localeCompare(a.paymentDate));

  const myPaidInvoices = store.invoices
    .filter(inv => inv.customerId === customer.id && inv.status === 'Paid')
    .sort((a,b) => b.generatedDate?.localeCompare(a.generatedDate));

  const paidUpTo = myPaidInvoices.length > 0 ? myPaidInvoices[0].billingMonthYear : "No Records";

  const handleRequestPayment = async (e) => {
    e.preventDefault();
    if (trxId.length < 8) return alert("দয়া করে সঠিক ট্রানজেকশন আইডি দিন!");

    setIsProcessing(true);
    try {
      const requestDate = new Date().toLocaleDateString('en-CA');
      const timeStr = new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });

      await addDoc(collection(db, "payment_requests"), {
        customerId: customer.id,
        customerName: customer.name,
        customerCode: customer.customerCode,
        amount: customer.currentDue,
        trxId: trxId.toUpperCase(),
        method: selectedMethod,
        status: "pending",
        requestDate: requestDate,
        requestTime: timeStr
      });

      alert("আপনার রিকোয়েস্টটি জমা হয়েছে! এডমিন ভেরিফাই করার পর আপনার ব্যালেন্স আপডেট হয়ে যাবে।");
      setShowPayModal(false);
      setTrxId('');
    } catch (error) {
      alert("সাবমিট করা সম্ভব হয়নি। আবার চেষ্টা করুন।");
    } finally {
      setIsProcessing(false);
    }
  };

  const paymentNumber = selectedMethod === 'bKash'
    ? (store.settings?.personalBkashNo || '017XXXXXXXX')
    : (store.settings?.personalNagadNo || '018XXXXXXXX');

  const formatDateDisplay = (dateStr) => {
    if (!dateStr || dateStr === 'Not Set') return dateStr || '';
    if (/^\d{2}-\d{2}-\d{4}$/.test(dateStr)) return dateStr;
    const parts = dateStr.split('-');
    if (parts.length === 3 && parts[0].length === 4) {
        return `${parts[2]}-${parts[1]}-${parts[0]}`;
    }
    return dateStr;
  };

  return (
    <div className="max-w-6xl mx-auto space-y-12 pb-20 uppercase font-black tracking-tighter relative">

      {/* MANUAL PAYMENT MODAL */}
      {showPayModal && (
        <div className="fixed inset-0 z-[3000] flex items-center justify-center p-6 animate-fadeIn font-sans uppercase">
           <div className="absolute inset-0 bg-slate-900/90 backdrop-blur-md" onClick={() => setShowPayModal(false)}></div>
           <div className="bg-white dark:bg-slate-800 w-full max-w-lg rounded-[48px] overflow-hidden shadow-2xl relative animate-scaleIn border-4 border-teal-500/20">
              <div className="p-10 text-center space-y-8">

                 {/* Method Icons - BOTH VISIBLE */}
                 <div className="space-y-4">
                    <p className="text-[10px] font-black text-slate-400 tracking-[3px]">পেমেন্ট মাধ্যম সিলেক্ট করুন</p>
                    <div className="flex justify-center space-x-6">
                        <button onClick={() => setSelectedMethod('bKash')} className={`w-28 h-24 rounded-3xl flex flex-col items-center justify-center shadow-lg transition-all border-4 ${selectedMethod === 'bKash' ? 'bg-[#D0006F] border-pink-200 scale-110' : 'bg-white border-slate-100 dark:bg-slate-700'}`}>
                            <i className={`fas fa-mobile-screen text-4xl ${selectedMethod === 'bKash' ? 'text-white' : 'text-[#D0006F]'}`}></i>
                            <span className={`text-[10px] font-black mt-2 ${selectedMethod === 'bKash' ? 'text-white' : 'text-[#D0006F]'}`}>bKash</span>
                        </button>
                        <button onClick={() => setSelectedMethod('Nagad')} className={`w-28 h-24 rounded-3xl flex flex-col items-center justify-center shadow-lg transition-all border-4 ${selectedMethod === 'Nagad' ? 'bg-[#F58220] border-orange-200 scale-110' : 'bg-white border-slate-100 dark:bg-slate-700'}`}>
                            <i className={`fas fa-wallet text-4xl ${selectedMethod === 'Nagad' ? 'text-white' : 'text-[#F58220]'}`}></i>
                            <span className={`text-[10px] font-black mt-2 ${selectedMethod === 'Nagad' ? 'text-white' : 'text-[#F58220]'}`}>Nagad</span>
                        </button>
                    </div>
                 </div>

                 {/* Bengali Instructions */}
                 <div className="space-y-4 bg-slate-50 dark:bg-slate-900/50 p-8 rounded-[40px] border-2 border-slate-100 dark:border-slate-700">
                    <h3 className="text-xl font-black text-slate-900 dark:text-white leading-none border-b pb-4 border-slate-200">কিভাবে পেমেন্ট করবেন?</h3>
                    <div className="text-left space-y-4 font-black">
                       <div className="flex items-start space-x-4">
                          <div className="w-8 h-8 rounded-full bg-teal-600 text-white flex items-center justify-center shrink-0 text-xs">১</div>
                          <p className="text-[11px] leading-relaxed text-slate-600 dark:text-slate-300">আপনার বিকাশ বা নগদ অ্যাপ থেকে নিচের নাম্বারে <span className="text-rose-600 underline">Send Money</span> করুন।</p>
                       </div>
                       <div className="bg-white dark:bg-slate-800 p-4 rounded-2xl text-center border-2 border-teal-500/20 shadow-sm">
                          <p className="text-3xl font-black text-slate-800 dark:text-white tracking-widest">{paymentNumber}</p>
                          <p className="text-[10px] text-teal-600 mt-1 uppercase tracking-widest">Amount: {currency}{customer.currentDue}</p>
                       </div>
                       <div className="flex items-start space-x-4">
                          <div className="w-8 h-8 rounded-full bg-teal-600 text-white flex items-center justify-center shrink-0 text-xs">২</div>
                          <p className="text-[11px] leading-relaxed text-slate-600 dark:text-slate-300">টাকা পাঠানোর পর যে <span className="text-indigo-600 underline">Transaction ID (TrxID)</span> পাবেন, সেটি নিচের বক্সে লিখে সাবমিট করুন।</p>
                       </div>
                    </div>
                 </div>

                 <form onSubmit={handleRequestPayment} className="space-y-6">
                    <input
                        type="text"
                        placeholder="এখানে TrxID লিখুন"
                        value={trxId}
                        onChange={e => setTrxId(e.target.value)}
                        className={`w-full bg-slate-50 dark:bg-slate-900 border-4 p-6 rounded-3xl text-center text-2xl font-black text-slate-800 dark:text-white outline-none shadow-inner transition-all ${selectedMethod === 'bKash' ? 'border-pink-100 focus:border-[#D0006F]' : 'border-orange-100 focus:border-[#F58220]'}`}
                        required
                    />
                    <button
                      type="submit"
                      disabled={isProcessing}
                      className={`w-full py-7 rounded-[32px] font-black text-xl tracking-[5px] shadow-xl hover:scale-[1.02] active:scale-95 transition-all text-white ${selectedMethod === 'bKash' ? 'bg-[#D0006F]' : 'bg-[#F58220]'}`}
                    >
                      {isProcessing ? 'প্রসেসিং হচ্ছে...' : 'পেমেন্ট সাবমিট করুন'}
                    </button>
                    <button type="button" onClick={() => setShowPayModal(false)} className="text-slate-400 text-xs font-bold uppercase hover:text-rose-500 transition-colors">ফিরে যান</button>
                 </form>
              </div>
           </div>
        </div>
      )}

      {/* Header */}
      <div className="flex flex-col md:flex-row justify-between items-center bg-white dark:bg-slate-800 p-10 rounded-[48px] shadow-2xl border border-slate-100 dark:border-slate-700 gap-8">
         <div className="flex items-center space-x-8">
            <div className="w-24 h-24 bg-teal-600 text-white rounded-[32px] flex items-center justify-center text-4xl shadow-xl"><i className="fas fa-user-circle"></i></div>
            <div>
               <p className="text-xs text-teal-600 tracking-[5px]">Welcome Back,</p>
               <h2 className="text-4xl md:text-5xl font-black text-slate-800 dark:text-white uppercase leading-none">{customer.name}</h2>
               <p className="text-[10px] text-slate-400 mt-2 tracking-[4px]">Subscriber ID: {customer.customerCode}</p>
            </div>
         </div>
         <button onClick={onLogout} className="bg-rose-50 text-rose-500 px-8 py-4 rounded-3xl font-black text-xs hover:bg-rose-500 hover:text-white transition-all uppercase tracking-widest shrink-0">Sign Out</button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-10">
         <div className="lg:col-span-2 bg-[#0D9488] p-12 rounded-[64px] text-white shadow-2xl relative overflow-hidden group">
            <div className="absolute top-0 right-0 w-80 h-80 bg-white/5 rounded-full -mr-20 -mt-20"></div>
            <div className="relative z-10 space-y-10">
               <p className="text-sm font-black tracking-[8px] opacity-80 uppercase">Total Outstanding Balance</p>
               <h3 className="text-8xl md:text-9xl font-black tracking-tighter leading-none">{currency} {Math.floor(customer.currentDue)}</h3>

               <div className="flex flex-wrap gap-x-16 gap-y-8 pt-6 font-black uppercase">
                  <div>
                    <p className="text-[10px] font-black opacity-60 tracking-[4px]">MONTHLY BILL</p>
                    <p className="text-2xl font-black">{currency} {customer.monthlyBill}</p>
                  </div>
                  <div className="w-px h-12 bg-white/20 hidden md:block"></div>
                  <div>
                    <p className="text-[10px] font-black opacity-60 tracking-[4px]">ADVANCE BALANCE</p>
                    <p className="text-2xl font-black text-emerald-300">{currency} {Math.floor(customer.advanceBalance || 0)}</p>
                  </div>
                  <div className="w-px h-12 bg-white/20 hidden md:block"></div>
                  <div>
                    <p className="text-[10px] font-black opacity-60 tracking-[4px]">PAID UP TO</p>
                    <p className="text-2xl font-black text-teal-100">{paidUpTo}</p>
                  </div>
               </div>

               <button
                onClick={() => setShowPayModal(true)}
                disabled={isProcessing}
                className="w-full bg-white text-teal-700 py-8 rounded-[40px] font-black text-xl tracking-[5px] shadow-xl hover:scale-[1.02] active:scale-95 transition-all uppercase"
               >
                 {isProcessing ? 'Connecting...' : 'PAY BILL NOW'}
               </button>
            </div>
         </div>

         <div className="bg-white dark:bg-slate-800 p-10 rounded-[56px] shadow-2xl border border-slate-100 dark:border-slate-700 space-y-10 uppercase font-black">
            <div className="space-y-6">
               <div className="flex items-center space-x-5"><div className="w-14 h-14 bg-blue-50 text-blue-600 rounded-2xl flex items-center justify-center text-xl shadow-sm"><i className="fas fa-wifi"></i></div><div><p className="text-[9px] text-slate-400 tracking-[3px]">CURRENT PLAN</p><p className="text-xl font-black">{customer.packageName}</p></div></div>
               <div className="flex items-center space-x-5"><div className="w-14 h-14 bg-rose-50 text-rose-500 rounded-2xl flex items-center justify-center text-xl shadow-sm"><i className="fas fa-calendar-times"></i></div><div><p className="text-[9px] text-slate-400 tracking-[3px]">EXPIRATION</p><p className="text-xl font-black text-rose-500">{formatDateDisplay(customer.expireDate) || 'Not Set'}</p></div></div>
               <div className="flex items-center space-x-5"><div className="w-14 h-14 bg-indigo-50 text-indigo-600 rounded-2xl flex items-center justify-center text-xl shadow-sm"><i className="fas fa-user-shield"></i></div><div><p className="text-[9px] text-slate-400 tracking-[3px]">PPPoE USER</p><p className="text-sm font-black">{customer.pppoeUsername}</p></div></div>
            </div>
            <div className="pt-8 border-t space-y-6">
               <h4 className="text-xs font-black tracking-[4px] text-slate-400 uppercase">Support Center</h4>
               <button className="w-full flex items-center justify-between p-6 bg-slate-50 dark:bg-slate-900 rounded-[32px] group hover:bg-teal-50 transition-all shadow-sm"><span className="font-black text-[11px] tracking-widest group-hover:text-teal-600">REPORT A PROBLEM</span><i className="fas fa-chevron-right text-slate-300 group-hover:text-teal-500"></i></button>
            </div>
         </div>

         {/* Payment History List */}
         <div className="lg:col-span-3 bg-white dark:bg-slate-800 p-12 rounded-[64px] shadow-2xl border border-slate-100 dark:border-slate-700 space-y-10 uppercase font-black">
            <h4 className="text-2xl font-black uppercase tracking-widest border-b pb-6">Payment History / পেমেন্ট ইতিহাস</h4>
            <div className="space-y-6 max-h-[500px] overflow-y-auto pr-4 custom-scrollbar">
               {myPayments.length > 0 ? myPayments.map(p => (
                 <div key={p.id} className="flex justify-between items-center bg-slate-50 dark:bg-slate-900 p-8 rounded-[40px] border border-slate-100 dark:border-slate-800 hover:scale-[1.01] transition-all shadow-sm group">
                    <div className="flex items-center space-x-6">
                       <div className="w-14 h-14 bg-emerald-50 text-emerald-600 rounded-2xl flex items-center justify-center text-xl group-hover:bg-emerald-500 group-hover:text-white transition-colors"><i className="fas fa-receipt"></i></div>
                       <div>
                          <p className="text-xl font-black text-slate-800 dark:text-white uppercase tracking-tighter leading-none">Receipt: {p.receiptNo}</p>
                          <p className="text-[10px] text-slate-400 font-bold uppercase mt-2 tracking-widest">{formatDateDisplay(p.paymentDate)} • {p.paymentMethod}</p>
                       </div>
                    </div>
                    <div className="text-right">
                       <p className="text-3xl font-black text-emerald-600 leading-none">{currency} {p.amount}</p>
                       <p className="text-[9px] text-slate-300 font-bold mt-2 uppercase tracking-[3px]">Settled</p>
                    </div>
                 </div>
               )) : (<div className="text-center py-20 opacity-20"><i className="fas fa-folder-open text-[100px]"></i><p className="text-xl font-black uppercase tracking-[10px] mt-6">No Records Found</p></div>)}
            </div>
         </div>

      </div>
    </div>
  );
};

export default CustomerPortal;
