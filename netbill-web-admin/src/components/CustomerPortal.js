import React, { useState, useEffect } from 'react';
import { supabase } from '../supabaseClient';

const CustomerPortal = ({ customer, store, t, onLogout }) => {
  const [isProcessing, setIsProcessing] = useState(false);
  const [showPayModal, setShowPayModal] = useState(false);
  const [showTicketModal, setShowTicketModal] = useState(false);
  const [trxId, setTrxId] = useState('');
  const [selectedMethod, setSelectedMethod] = useState('bKash');

  const [ticketData, setTicketData] = useState({ subject: '', description: '' });
  const [myDbPayments, setMyDbPayments] = useState([]);

  useEffect(() => {
    fetchMyPayments();
  }, []);

  const fetchMyPayments = async () => {
    try {
        const { data, error } = await supabase
            .from('payments')
            .select('*')
            .eq('customer_id', customer.id)
            .order('payment_date', { ascending: false });
        if (!error) setMyDbPayments(data || []);
    } catch (e) { console.error(e); }
  };

  const currency = "৳";

  const myPayments = myDbPayments.length > 0 ? myDbPayments : store.payments
    .filter(p => p.customerId === customer.id || p.customer_id === customer.id)
    .sort((a,b) => (b.paymentDate || b.payment_date)?.localeCompare(a.paymentDate || a.payment_date));

  const myInvoices = store.invoices
    .filter(inv => (inv.customerId === customer.id || inv.customer_id === customer.id))
    .sort((a,b) => (b.billingMonthYear || b.billing_month_year)?.localeCompare(a.billingMonthYear || a.billing_month_year));

  const myPaidInvoices = myInvoices.filter(inv => inv.status === 'Paid');

  const paidUpTo = myPaidInvoices.length > 0 ? (myPaidInvoices[0].billingMonthYear || myPaidInvoices[0].billing_month_year) : "No Records";

  const handleRequestPayment = async (e) => {
    e.preventDefault();
    if (trxId.length < 8) return alert("দয়া করে সঠিক ট্রানজেকশন আইডি দিন!");

    setIsProcessing(true);
    try {
      const requestDate = new Date().toLocaleDateString('en-CA');
      const timeStr = new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });

      const { error } = await supabase.from('payment_requests').insert({
        customer_id: customer.id,
        customer_name: customer.name,
        customer_code: customer.customerCode,
        amount: customer.currentDue,
        trx_id: trxId.toUpperCase(),
        method: selectedMethod,
        status: "pending",
        request_date: requestDate,
        request_time: timeStr
      });

      if (error) throw error;
      alert("আপনার রিকোয়েস্টটি জমা হয়েছে! এডমিন ভেরিফাই করার পর আপনার ব্যালেন্স আপডেট হয়ে যাবে।");
      setShowPayModal(false);
      setTrxId('');
    } catch (error) {
      alert("সাবমিট করা সম্ভব হয়নি। আবার চেষ্টা করুন।");
    } finally {
      setIsProcessing(false);
    }
  };

  const handleCreateTicket = async (e) => {
    e.preventDefault();
    if (!ticketData.subject) return alert("দয়া করে অভিযোগের বিষয় লিখুন");

    setIsProcessing(true);
    try {
      const { error } = await supabase.from('support_tickets').insert({
        customer_id: customer.id,
        customer_code: customer.customerCode,
        subject: ticketData.subject,
        description: ticketData.description,
        status: 'Open',
        priority: 'Normal',
        created_at: new Date().toISOString(),
        created_by: customer.name
      });

      if (error) throw error;
      alert("আপনার অভিযোগটি সফলভাবে নথিভুক্ত হয়েছে। আমাদের প্রতিনিধি শীঘ্রই আপনার সাথে যোগাযোগ করবে।");
      setShowTicketModal(false);
      setTicketData({ subject: '', description: '' });
    } catch (e) {
      alert("দুঃখিত, অভিযোগ জমা দেওয়া সম্ভব হয়নি।");
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
    <div className="max-w-6xl mx-auto space-y-6 md:space-y-12 pb-20 uppercase font-black tracking-tighter relative px-2 md:px-0">

      {/* MANUAL PAYMENT MODAL */}
      {showPayModal && (
        <div className="fixed inset-0 z-[3000] flex items-center justify-center p-4 md:p-6 animate-fadeIn font-sans uppercase overflow-y-auto">
           <div className="absolute inset-0 bg-slate-900/90 backdrop-blur-md" onClick={() => setShowPayModal(false)}></div>
           <div className="bg-white dark:bg-slate-800 w-full max-w-lg rounded-[32px] md:rounded-[48px] overflow-hidden shadow-2xl relative animate-scaleIn border-4 border-teal-500/20 my-auto">
              <div className="p-6 md:p-10 text-center space-y-6 md:space-y-8">
                 <div className="space-y-3 md:space-y-4">
                    <p className="text-[10px] font-black text-slate-400 tracking-[3px]">পেমেন্ট মাধ্যম সিলেক্ট করুন</p>
                    <div className="flex justify-center space-x-4 md:space-x-6">
                        <button onClick={() => setSelectedMethod('bKash')} className={`w-24 h-20 md:w-28 md:h-24 rounded-2xl md:rounded-3xl flex flex-col items-center justify-center shadow-lg transition-all border-4 ${selectedMethod === 'bKash' ? 'bg-[#D0006F] border-pink-200 scale-105 md:scale-110' : 'bg-white border-slate-100 dark:bg-slate-700'}`}>
                            <i className={`fas fa-mobile-screen text-2xl md:text-4xl ${selectedMethod === 'bKash' ? 'text-white' : 'text-[#D0006F]'}`}></i>
                            <span className={`text-[9px] md:text-[10px] font-black mt-1 md:mt-2 ${selectedMethod === 'bKash' ? 'text-white' : 'text-[#D0006F]'}`}>bKash</span>
                        </button>
                        <button onClick={() => setSelectedMethod('Nagad')} className={`w-24 h-20 md:w-28 md:h-24 rounded-2xl md:rounded-3xl flex flex-col items-center justify-center shadow-lg transition-all border-4 ${selectedMethod === 'Nagad' ? 'bg-[#F58220] border-orange-200 scale-105 md:scale-110' : 'bg-white border-slate-100 dark:bg-slate-700'}`}>
                            <i className={`fas fa-wallet text-2xl md:text-4xl ${selectedMethod === 'Nagad' ? 'text-white' : 'text-[#F58220]'}`}></i>
                            <span className={`text-[9px] md:text-[10px] font-black mt-1 md:mt-2 ${selectedMethod === 'Nagad' ? 'text-white' : 'text-[#F58220]'}`}>Nagad</span>
                        </button>
                    </div>
                 </div>

                 <div className="space-y-4 bg-slate-50 dark:bg-slate-900/50 p-6 md:p-8 rounded-[32px] md:rounded-[40px] border-2 border-slate-100 dark:border-slate-700">
                    <h3 className="text-lg md:text-xl font-black text-slate-900 dark:text-white leading-none border-b pb-3 md:pb-4 border-slate-200">কিভাবে পেমেন্ট করবেন?</h3>
                    <div className="text-left space-y-4 font-black">
                       <div className="flex items-start space-x-3 md:space-x-4">
                          <div className="w-6 h-6 md:w-8 md:h-8 rounded-full bg-teal-600 text-white flex items-center justify-center shrink-0 text-[10px] md:text-xs">১</div>
                          <p className="text-[10px] md:text-[11px] leading-relaxed text-slate-600 dark:text-slate-300">আপনার বিকাশ বা নগদ অ্যাপ থেকে নিচের নাম্বারে <span className="text-rose-600 underline">Send Money</span> করুন।</p>
                       </div>
                       <div className="bg-white dark:bg-slate-800 p-3 md:p-4 rounded-xl md:rounded-2xl text-center border-2 border-teal-500/20 shadow-sm">
                          <p className="text-2xl md:text-3xl font-black text-slate-800 dark:text-white tracking-widest">{paymentNumber}</p>
                          <p className="text-[9px] md:text-[10px] text-teal-600 mt-1 uppercase tracking-widest">Amount: {currency}{customer.currentDue}</p>
                       </div>
                    </div>
                 </div>

                 <form onSubmit={handleRequestPayment} className="space-y-4 md:space-y-6">
                    <input type="text" placeholder="এখানে TrxID লিখুন" value={trxId} onChange={e => setTrxId(e.target.value)} className="w-full bg-slate-50 dark:bg-slate-950 border-none p-5 md:p-6 rounded-2xl md:rounded-3xl text-center text-xl md:text-2xl font-black outline-none shadow-inner transition-all focus:ring-4 focus:ring-teal-500/10" required />
                    <button type="submit" disabled={isProcessing} className={`w-full py-5 md:py-7 rounded-2xl md:rounded-[32px] font-black text-lg md:text-xl text-white shadow-xl ${selectedMethod === 'bKash' ? 'bg-[#D0006F]' : 'bg-[#F58220]'}`}>সাবমিট করুন</button>
                    <button type="button" onClick={() => setShowPayModal(false)} className="text-slate-400 text-[10px] md:text-xs font-bold uppercase tracking-widest">ফিরে যান</button>
                 </form>
              </div>
           </div>
        </div>
      )}

      {/* SUPPORT TICKET MODAL */}
      {showTicketModal && (
        <div className="fixed inset-0 z-[3000] flex items-center justify-center p-4 md:p-6 animate-fadeIn font-sans uppercase overflow-y-auto">
           <div className="absolute inset-0 bg-slate-900/90 backdrop-blur-md" onClick={() => setShowTicketModal(false)}></div>
           <div className="bg-white dark:bg-slate-800 w-full max-w-lg rounded-[32px] md:rounded-[48px] overflow-hidden shadow-2xl relative border-4 border-amber-500/20 my-auto">
              <div className="p-8 md:p-10 space-y-6 md:space-y-8">
                 <div className="text-center space-y-2">
                    <h3 className="text-2xl md:text-3xl font-black text-slate-800 dark:text-white tracking-tighter uppercase">অভিযোগ জমা দিন</h3>
                    <p className="text-[9px] md:text-[10px] text-slate-400 tracking-[2px] font-bold">আমরা আপনার সমস্যা দ্রুত সমাধানের চেষ্টা করবো</p>
                 </div>

                 <form onSubmit={handleCreateTicket} className="space-y-5 md:space-y-6">
                    <div className="space-y-2">
                       <label className="text-[9px] md:text-[10px] text-slate-400 ml-4 font-black uppercase">অভিযোগের বিষয়</label>
                       <input type="text" placeholder="উদা: ইন্টারনেট স্লো, লাইন কাটা" value={ticketData.subject} onChange={e => setTicketData({...ticketData, subject: e.target.value})} className="w-full bg-slate-50 dark:bg-slate-900 p-4 md:p-5 rounded-2xl md:rounded-3xl font-black text-base md:text-lg outline-none shadow-inner transition-all focus:ring-4 focus:ring-amber-500/10" required />
                    </div>
                    <div className="space-y-2">
                       <label className="text-[9px] md:text-[10px] text-slate-400 ml-4 font-black uppercase">বিস্তারিত বিবরণ</label>
                       <textarea rows="4" placeholder="আপনার সমস্যাটি বিস্তারিত লিখুন..." value={ticketData.description} onChange={e => setTicketData({...ticketData, description: e.target.value})} className="w-full bg-slate-50 dark:bg-slate-900 p-5 md:p-6 rounded-2xl md:rounded-3xl font-black text-base md:text-lg outline-none resize-none shadow-inner transition-all focus:ring-4 focus:ring-amber-500/10"></textarea>
                    </div>
                    <button type="submit" disabled={isProcessing} className="w-full bg-amber-500 text-white py-5 md:py-6 rounded-2xl md:rounded-[32px] font-black text-lg md:text-xl tracking-[3px] md:tracking-[5px] shadow-xl hover:scale-[1.02] active:scale-95 transition-all">অভিযোগ সাবমিট করুন</button>
                    <div className="text-center"><button type="button" onClick={() => setShowTicketModal(false)} className="text-slate-400 text-[10px] md:text-xs font-bold uppercase tracking-widest">বন্ধ করুন</button></div>
                 </form>
              </div>
           </div>
        </div>
      )}

      {/* Header */}
      <div className="flex flex-col md:flex-row justify-between items-center bg-white dark:bg-slate-800 p-6 md:p-10 rounded-[32px] md:rounded-[48px] shadow-xl border border-slate-50 dark:border-slate-700 gap-6 md:gap-8">
         <div className="flex items-center space-x-4 md:space-x-8 w-full md:w-auto">
            <div className="w-16 h-16 md:w-24 md:h-24 bg-teal-600 text-white rounded-2xl md:rounded-[32px] flex items-center justify-center text-3xl md:text-4xl shadow-lg"><i className="fas fa-user-circle"></i></div>
            <div className="flex-1">
               <p className="text-[9px] md:text-xs text-teal-600 tracking-[3px] md:tracking-[5px] font-black">WELCOME BACK,</p>
               <h2 className="text-xl md:text-5xl font-black text-slate-800 dark:text-white uppercase leading-none truncate max-w-[200px] md:max-w-none">{customer.name}</h2>
               <p className="text-[8px] md:text-[10px] text-slate-400 mt-1 md:mt-2 tracking-[2px] md:tracking-[4px] font-bold">SUBSCRIBER ID: {customer.customerCode}</p>
            </div>
         </div>
         <button onClick={onLogout} className="w-full md:w-auto bg-rose-50 text-rose-500 px-6 py-3 md:px-8 md:py-4 rounded-xl md:rounded-3xl font-black text-[10px] md:text-xs hover:bg-rose-500 hover:text-white transition-all uppercase tracking-widest shrink-0">Sign Out</button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 md:gap-10">
         <div className="lg:col-span-2 bg-[#0D9488] p-6 md:p-12 rounded-[40px] md:rounded-[64px] text-white shadow-2xl relative overflow-hidden group">
            <div className="absolute top-0 right-0 w-40 h-40 md:w-80 md:h-80 bg-white/5 rounded-full -mr-10 -mt-10 md:-mr-20 md:-mt-20"></div>
            <div className="relative z-10 space-y-6 md:space-y-10">
               <p className="text-[10px] md:text-sm font-black tracking-[4px] md:tracking-[8px] opacity-100 uppercase text-rose-200 leading-none">মোট বকেয়া (Total Due)</p>
               <h3 className="text-6xl md:text-9xl font-black tracking-tighter leading-none text-rose-500">{currency} {Math.floor(customer.currentDue)}</h3>
               <div className="flex flex-wrap gap-x-8 md:gap-x-16 gap-y-4 md:gap-y-8 pt-2 md:pt-6 font-black uppercase">
                  <div><p className="text-[8px] md:text-[10px] font-black opacity-60 tracking-[2px] md:tracking-[4px]">MONTHLY BILL</p><p className="text-lg md:text-2xl font-black">{currency} {customer.monthlyBill}</p></div>
                  <div className="w-px h-10 bg-white/20 hidden md:block"></div>
                  <div><p className="text-[8px] md:text-[10px] font-black opacity-60 tracking-[2px] md:tracking-[4px]">ADVANCE</p><p className="text-lg md:text-2xl font-black text-emerald-300">{currency} {Math.floor(customer.advanceBalance || 0)}</p></div>
                  <div className="w-px h-10 bg-white/20 hidden md:block"></div>
                  <div className="w-full md:w-auto"><p className="text-[8px] md:text-[10px] font-black opacity-60 tracking-[2px] md:tracking-[4px]">PAID UP TO</p><p className="text-lg md:text-2xl font-black text-teal-100">{paidUpTo}</p></div>
               </div>
               <button onClick={() => setShowPayModal(true)} disabled={isProcessing} className="w-full bg-white text-teal-700 py-6 md:py-8 rounded-2xl md:rounded-[40px] font-black text-lg md:text-xl tracking-[4px] md:tracking-[5px] shadow-xl hover:scale-[1.02] active:scale-95 transition-all">PAY BILL NOW</button>
            </div>
         </div>

         <div className="bg-white dark:bg-slate-800 p-6 md:p-10 rounded-[32px] md:rounded-[56px] shadow-xl border border-slate-50 dark:border-slate-700 space-y-6 md:space-y-10 uppercase font-black">
            <div className="space-y-4 md:space-y-6">
               <PortalInfoRow icon="fa-wifi" label="PLAN" value={customer.packageName} color="text-blue-600" bgColor="bg-blue-50" />
               <PortalInfoRow icon="fa-calendar-times" label="EXPIRY" value={formatDateDisplay(customer.expireDate) || 'Not Set'} color="text-rose-500" bgColor="bg-rose-50" />
               <PortalInfoRow icon="fa-phone-alt" label="MOBILE" value={customer.mobile} color="text-emerald-600" bgColor="bg-emerald-50" />
               <PortalInfoRow icon="fa-map-marker-alt" label="ZONE" value={customer.zone || 'Global'} color="text-indigo-600" bgColor="bg-indigo-50" />
               <PortalInfoRow icon="fa-user-shield" label="PPPoE" value={customer.pppoeUsername} color="text-slate-600" bgColor="bg-slate-50" />
            </div>
            <div className="pt-6 md:pt-8 border-t space-y-4 md:space-y-6">
               <h4 className="text-[10px] md:text-xs font-black tracking-[4px] text-slate-400 uppercase">Support Center</h4>
               <button onClick={() => setShowTicketModal(true)} className="w-full flex items-center justify-between p-5 md:p-6 bg-slate-50 dark:bg-slate-950 rounded-2xl md:rounded-[32px] group hover:bg-amber-50 transition-all shadow-sm border border-slate-100 dark:border-slate-800"><span className="font-black text-[9px] md:text-[11px] tracking-widest group-hover:text-amber-600">REPORT A PROBLEM</span><i className="fas fa-chevron-right text-slate-300 group-hover:text-amber-500"></i></button>
            </div>
         </div>

         {/* Monthly Billing Status */}
         <div className="lg:col-span-3 bg-white dark:bg-slate-800 p-6 md:p-10 rounded-[32px] md:rounded-[56px] shadow-xl border border-slate-50 dark:border-slate-700 space-y-6 md:space-y-8 uppercase font-black">
            <h4 className="text-lg md:text-xl font-black uppercase tracking-[2px] md:tracking-[4px] border-b pb-4">Monthly Bill Status / মাসিক বিলের অবস্থা</h4>
            <div className="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-6 gap-3 md:gap-4">
                {myInvoices.map(inv => (
                    <div key={inv.id} className={`p-3 md:p-4 rounded-2xl md:rounded-3xl border-2 flex flex-col items-center justify-center space-y-1 md:space-y-2 ${inv.status === 'Paid' ? 'bg-emerald-50 border-emerald-100' : 'bg-rose-50 border-rose-100'}`}>
                        <p className="text-[8px] md:text-[9px] font-black text-slate-400 leading-none">{inv.billingMonthYear || inv.billing_month_year}</p>
                        <p className={`text-[10px] md:text-sm font-black ${inv.status === 'Paid' ? 'text-emerald-600' : 'text-rose-600'}`}>{inv.status.toUpperCase()}</p>
                        <p className="text-[9px] md:text-[10px] font-black text-slate-800 dark:text-white">৳{inv.totalPayable || inv.total_payable}</p>
                    </div>
                ))}
                {myInvoices.length === 0 && <p className="col-span-full text-center py-6 text-slate-400 text-[10px] tracking-widest uppercase">No Invoice Records</p>}
            </div>
         </div>

         {/* Payment History List */}
         <div className="lg:col-span-3 bg-white dark:bg-slate-800 p-6 md:p-12 rounded-[32px] md:rounded-[64px] shadow-xl border border-slate-50 dark:border-slate-700 space-y-6 md:space-y-10 uppercase font-black">
            <h4 className="text-xl md:text-2xl font-black uppercase tracking-widest border-b pb-4 md:pb-6 leading-none">Payment History / পেমেন্ট ইতিহাস</h4>
            <div className="space-y-4 md:space-y-6 max-h-[400px] md:max-h-[600px] overflow-y-auto px-1 custom-scrollbar">
               {myPayments.length > 0 ? myPayments.map(p => (
                 <div key={p.id} className="flex justify-between items-center bg-slate-50 dark:bg-slate-950 p-5 md:p-8 rounded-[24px] md:rounded-[40px] border border-slate-100 dark:border-slate-800 hover:scale-[1.01] transition-all shadow-sm group">
                    <div className="flex items-center space-x-4 md:space-x-6 leading-none">
                       <div className="w-10 h-10 md:w-14 md:h-14 bg-emerald-50 dark:bg-emerald-950/20 text-emerald-600 rounded-xl md:rounded-2xl flex items-center justify-center text-lg md:text-xl group-hover:bg-emerald-500 group-hover:text-white transition-colors shadow-inner"><i className="fas fa-receipt"></i></div>
                       <div className="space-y-1 md:space-y-2">
                          <p className="text-sm md:text-xl font-black text-slate-800 dark:text-white uppercase tracking-tighter">Receipt: {p.receiptNo || p.receipt_no}</p>
                          <p className="text-[8px] md:text-[10px] text-slate-400 font-bold uppercase tracking-widest">{formatDateDisplay(p.paymentDate || p.payment_date)} • {p.paymentMethod || p.payment_method}</p>
                       </div>
                    </div>
                    <div className="text-right leading-none">
                       <p className="text-lg md:text-3xl font-black text-emerald-600">{currency} {p.amount}</p>
                       <p className="text-[8px] md:text-[9px] text-slate-300 font-bold mt-1 md:mt-2 uppercase tracking-[2px]">Settled</p>
                    </div>
                 </div>
               )) : (<div className="text-center py-20 opacity-10 uppercase flex flex-col items-center"><i className="fas fa-folder-open text-7xl md:text-[100px] mb-4 md:mb-6"></i><p className="text-lg md:text-xl font-black uppercase tracking-[6px] md:tracking-[10px]">No Records Found</p></div>)}
            </div>
         </div>
      </div>
    </div>
  );
};

const PortalInfoRow = ({ icon, label, value, color, bgColor }) => (
    <div className="flex items-center space-x-4 md:space-x-5 leading-none">
        <div className={`w-10 h-10 md:w-14 md:h-14 ${bgColor} dark:bg-slate-950 ${color} rounded-xl md:rounded-2xl flex items-center justify-center text-base md:text-xl shadow-sm border border-black/5 dark:border-white/5`}><i className={`fas ${icon}`}></i></div>
        <div>
            <p className="text-[8px] md:text-[9px] text-slate-400 tracking-[2px] md:tracking-[3px] font-black uppercase">{label}</p>
            <p className={`text-sm md:text-xl font-black ${color} tracking-tighter mt-0.5`}>{value || '---'}</p>
        </div>
    </div>
);

export default CustomerPortal;
