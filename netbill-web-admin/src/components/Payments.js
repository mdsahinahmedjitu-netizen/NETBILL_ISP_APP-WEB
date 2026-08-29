import React, { useState, useMemo, useEffect } from 'react';
import { supabase } from '../supabaseClient';

const Payments = ({ store, session, t, lang, preSelectedCustomer, setPreSelectedCustomer, setActivePage }) => {
  const settings = store.settings || {};
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedCustomerId, setSelectedCustomerId] = useState('');
  const [amount, setAmount] = useState('');
  const [sendSms, setSendSms] = useState(true);

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
    if (preSelectedCustomer) {
      setSelectedCustomerId(preSelectedCustomer.id);
      setAmount(Math.floor(preSelectedCustomer.currentDue || preSelectedCustomer.current_due || 0).toString());
      setSearchTerm(preSelectedCustomer.name);
      setPreSelectedCustomer(null); // Clear after applying
    }
  }, [preSelectedCustomer, setPreSelectedCustomer]);

  const [method, setMethod] = useState('Cash');
  const [billingMonth, setBillingMonth] = useState(new Date().toLocaleString('default', { month: 'long', year: 'numeric' }));

  // Dynamic Billing Month Range Selection Logic
  useEffect(() => {
    const cust = store.customers.find(c => c.id === selectedCustomerId);
    if (!cust) return;

    const totalDue = parseFloat(cust.currentDue || cust.current_due || 0);
    const monthlyBill = parseFloat(cust.monthlyBill || cust.monthly_bill || 0);
    const payAmt = parseFloat(amount) || 0;

    if (monthlyBill > 0 && payAmt > 0) {
        const monthsDueCount = Math.ceil(totalDue / monthlyBill);
        const monthsPaidCount = Math.floor(payAmt / monthlyBill);

        if (monthsPaidCount > 0 && monthsDueCount > 0) {
            const startOffset = -(monthsDueCount - 1);
            const monthsList = [];
            const years = new Set();

            const toBn = (n) => lang === 'bn' ? n.toString().replace(/\d/g, d => "০১২৩৪৫৬৭৮৯"[d]) : n;
            const monthMapBn = {
                'January': 'জানুয়ারি', 'February': 'ফেব্রুয়ারি', 'March': 'মার্চ', 'April': 'এপ্রিল',
                'May': 'মে', 'June': 'জুন', 'July': 'জুলাই', 'August': 'আগস্ট',
                'September': 'সেপ্টেম্বর', 'October': 'অক্টোবর', 'November': 'নভেম্বর', 'December': 'ডিসেম্বর'
            };

            for (let i = 0; i < monthsPaidCount; i++) {
                const d = new Date();
                d.setDate(1);
                d.setMonth(d.getMonth() + startOffset + i);

                const monthEn = d.toLocaleString('en-US', { month: 'long' });
                const monthName = lang === 'bn' ? (monthMapBn[monthEn] || monthEn) : monthEn;

                monthsList.push(monthName);
                years.add(toBn(d.getFullYear()));
            }

            const yearsStr = Array.from(years).join('-');
            const resultStr = monthsList.length === 1
                ? `${monthsList[0]} ${yearsStr}`
                : `${monthsList.join('-')} ${yearsStr}`;

            setBillingMonth(resultStr);
        } else {
            setBillingMonth(new Date().toLocaleString('default', { month: 'long', year: 'numeric' }));
        }
    } else {
        setBillingMonth(new Date().toLocaleString('default', { month: 'long', year: 'numeric' }));
    }
  }, [amount, selectedCustomerId, store.customers, lang]);

  const [isProcessing, setIsProcessing] = useState(false);
  const [customerPayments, setCustomerPayments] = useState([]);
  const [selectedCollector, setSelectedCollector] = useState({
    id: session?.data?.id || 'admin',
    name: session?.data?.name || 'Super Admin'
  });

  const [editingPayment, setEditingPayment] = useState(null);
  const [editAmount, setEditEditAmount] = useState('');
  const [showEditModal, setShowEditModal] = useState(false);

  const [showExtensionModal, setShowExtensionModal] = useState(false);
  const [extensionData, setExtensionData] = useState({
    customerId: '', customerName: '', currentExpire: '', nextExpire: '', amount: 0, newDue: 0, newAdvance: 0, status: ''
  });

  // WhatsApp States
  const [showWhatsAppModal, setShowWhatsAppModal] = useState(false);
  const [lastReceiptInfo, setLastReceiptInfo] = useState(null);

  const formatDateDisplay = (dateStr) => {
    if (!dateStr) return '';
    if (dateStr.includes('-') && dateStr.split('-')[0].length === 4) {
       const [year, month, day] = dateStr.split('-');
       return `${day}-${month}-${year}`;
    }
    return dateStr;
  };

  useEffect(() => {
    setCustomerPayments([]);
    if (!selectedCustomerId) return;
    const fetchPayments = async () => {
      const { data, error } = await supabase.from('payments').select('*').eq('customer_id', selectedCustomerId).order('payment_date', { ascending: false }).limit(20);
      if (!error && data) {
        setCustomerPayments(data.map(item => {
          const newObj = {};
          Object.keys(item).forEach(key => {
            const camelKey = key.replace(/(_\w)/g, m => m[1].toUpperCase());
            newObj[camelKey] = item[key];
          });
          return newObj;
        }));
      }
    };
    fetchPayments();
    const channel = supabase.channel(`cust-payments-${selectedCustomerId}`).on('postgres_changes', { event: '*', schema: 'public', table: 'payments', filter: `customer_id=eq.${selectedCustomerId}` }, () => fetchPayments()).subscribe();
    return () => supabase.removeChannel(channel);
  }, [selectedCustomerId]);

  const filteredCustomers = store.customers.filter(c => {
    const searchMatch = !searchTerm ||
      c.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      c.customerCode?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      c.mobile?.includes(searchTerm) ||
      c.pppoeUsername?.toLowerCase().includes(searchTerm.toLowerCase());

    if (session?.role === 'staff') {
        const isAssigned = (c.assignedStaffId || c.assigned_staff_id) === session.data.id || (c.assignedStaffId || c.assigned_staff_id) === session.data.name;
        if (!isAssigned) return false;
    }

    return searchMatch;
  });

  const canCollect = useMemo(() => {
    if (session?.role === 'admin') return true;
    if (session?.role === 'staff') {
        const roleName = session.data.role;
        const permissions = store.settings?.rolePermissions?.[roleName] || store.settings?.role_permissions?.[roleName];
        return permissions?.canCollect !== false;
    }
    return false;
  }, [session, store.settings]);

  const handlePayment = async (e) => {
    e.preventDefault();
    if (!selectedCustomerId || !amount) return;

    if (!canCollect) {
        alert("You do not have permission to collect payments!");
        return;
    }

    const customer = store.customers.find(c => c.id === selectedCustomerId);
    if (!customer) return;

    setIsProcessing(true);
    const todayISO = new Date().toLocaleDateString('en-CA');

    try {
      const payAmt = parseFloat(amount);
      let newDue = (customer.currentDue || customer.current_due || 0);
      let newAdvance = (customer.advanceBalance || customer.advance_balance || 0);

      if (payAmt > newDue) {
          const excess = payAmt - newDue;
          newAdvance += excess;
          newDue = 0;
      } else {
          newDue -= payAmt;
      }

      const parseAnyDate = (dStr) => {
          if (!dStr) return null;
          if (dStr.includes('-') && dStr.split('-')[0].length === 4) {
              const [y, m, d] = dStr.split('-').map(Number);
              return new Date(y, m - 1, d);
          } else if (dStr.includes('-')) {
              const parts = dStr.split('-');
              const mArr = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
              return new Date(parseInt(parts[2]), mArr.indexOf(parts[1]), parseInt(parts[0]));
          }
          return null;
      };

      let currentExpireDateStr = customer.expireDate || customer.expire_date;
      let nextExpireDate = currentExpireDateStr;

      if (currentExpireDateStr) {
          const prevExpire = parseAnyDate(currentExpireDateStr);
          if (prevExpire) {
              const nextMonth = new Date(prevExpire);
              nextMonth.setMonth(nextMonth.getMonth() + 1);
              const monthsArr = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
              nextExpireDate = `${nextMonth.getDate().toString().padStart(2, '0')}-${monthsArr[nextMonth.getMonth()]}-${nextMonth.getFullYear()}`;
          }
      }

      if (session?.role === 'staff') {
        const { error: reqErr } = await supabase.from('payment_requests').insert({
            customer_id: selectedCustomerId,
            customer_name: customer.name,
            customer_code: customer.customerCode || customer.customer_code,
            amount: payAmt,
            method: method,
            status: 'pending',
            request_date: todayISO,
            request_time: new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }),
            collected_by: session.data.name,
            collected_by_id: session.data.id,
            billing_month: billingMonth,
            suggested_expire_date: nextExpireDate
        });
        if (reqErr) throw new Error(reqErr.message);
        setAmount(''); setSelectedCustomerId(''); setSearchTerm('');
        setIsProcessing(false);
        return;
      }

      if (newDue <= 0) {
          commitFinalPayment(selectedCustomerId, payAmt, newDue, newAdvance, nextExpireDate, 'Active');
          return;
      }

      setExtensionData({
        customerId: selectedCustomerId,
        customerName: customer.name,
        currentExpire: currentExpireDateStr || 'Not Set',
        nextExpire: nextExpireDate,
        amount: payAmt,
        newDue, newAdvance,
        status: customer.status
      });
      setShowExtensionModal(true);
      setIsProcessing(false);

    } catch (e) { alert("Error: " + e.message); setIsProcessing(false); }
  };

  const commitFinalPayment = async (custId, payAmt, newDue, newAdvance, finalExpireDate, forcedStatus = null) => {
    const customer = store.customers.find(c => c.id === custId);
    const todayISO = new Date().toLocaleDateString('en-CA');
    const timeStr = new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });

    try {
      const { data: newPmt, error: pmtErr } = await supabase.from('payments').insert({
        customer_id: custId, customer_name: customer.name, customer_code: customer.customerCode || customer.customer_code,
        amount: payAmt, payment_method: method, payment_date: todayISO, billing_month: billingMonth,
        receipt_no: `REC-${Date.now().toString().slice(-6)}`, remarks: `${billingMonth} Bill`,
        collected_by: selectedCollector.name, collected_by_id: selectedCollector.id
      }).select().single();

      if (pmtErr) throw pmtErr;

      const updatePayload = { current_due: newDue, advance_balance: newAdvance, payment_status: newDue <= 0 ? 'Paid' : 'Unpaid' };
      if (forcedStatus) {
          updatePayload.status = forcedStatus;
          const currentExpire = customer.expireDate || customer.expire_date;
          if (finalExpireDate && currentExpire) updatePayload.expire_date = finalExpireDate;
      }

      const { error: custUpdateErr } = await supabase.from('customers').update(updatePayload).eq('id', custId);
      if (custUpdateErr) throw custUpdateErr;

      if ((forcedStatus === 'Active' || newDue <= 0) && customer.pppoeUsername && (customer.routerId || customer.router_id)) {
          supabase.functions.invoke('mikrotik-manager', {
              body: { action: 'set_status', payload: { username: customer.pppoeUsername, active: true, routerId: customer.routerId || customer.router_id } }
          });
      }

      await supabase.from('ledger_entries').insert({
        customer_id: custId, date: todayISO, time: timeStr, type: "Payment",
        description: `Payment for ${billingMonth}`, amount: payAmt, is_debit: false,
        reference_no: pmtErr ? `ERR-${Date.now()}` : newPmt.receipt_no, running_balance: newDue, collector_name: selectedCollector.name,
        paid_amount: payAmt, total_due_balance: newDue
      });

      if (sendSms && settings.smsApiUrl && customer.mobile) {
          let msg = `Dear ${customer.name}, BDT ${payAmt} received for ${billingMonth}. New Due: BDT ${Math.floor(newDue)}.`;
          const template = store.smsTemplates?.find(t => (t.title === 'Collection' || t.title === 'কালেকশন') && (t.isActive || t.is_active));
          if (template) {
              msg = (template.messageContent || template.message_content)
                  .replace(/{NAME}/g, customer.name || '').replace(/{AMOUNT}/g, payAmt).replace(/{DUE}/g, Math.floor(newDue)).replace(/{CUSTOMER_CODE}/g, customer.customerCode || '').replace(/{BILL_MONTH}/g, billingMonth);
          }
          const apiKey = (settings.smsApiKey || "").trim();
          const senderId = (settings.smsSenderId || "").trim();
          const isUnicode = /[\u0980-\u09FF]/.test(msg);
          const msgType = isUnicode ? "unicode" : "text";
          let cleanMobile = customer.mobile.replace(/[^0-9]/g, "");
          if (cleanMobile.startsWith('0')) cleanMobile = '88' + cleanMobile;
          else if (cleanMobile.length === 10) cleanMobile = '880' + cleanMobile;
          else if (!cleanMobile.startsWith('88')) cleanMobile = '88' + cleanMobile;

          const img = new Image();
          img.src = `https://tglplinxvrqsrxeicvpr.supabase.co/functions/v1/sms-proxy?apikey=${apiKey}&callerID=${senderId}&number=${cleanMobile}&message=${encodeURIComponent(msg)}&type=${msgType}`;

          await supabase.from('sms_logs').insert({ customer_id: customer.id, customer_name: customer.name, mobile: cleanMobile, notification_type: 'Collection (Auto)', message: msg, status: 'Sent', sent_timestamp: new Date().toISOString() });
      }

      setLastReceiptInfo({ name: customer.name, mobile: customer.mobile, amount: payAmt, billingMonth: billingMonth, due: Math.floor(newDue), receiptNo: pmtErr ? '---' : newPmt.receiptNo });
      setShowWhatsAppModal(true);
      setAmount(''); setSelectedCustomerId(''); setSearchTerm(''); setShowExtensionModal(false);
    } catch (e) { alert("Finalizing payment failed!"); }
    finally { setIsProcessing(false); }
  };

  const sendWhatsApp = () => {
    if (!lastReceiptInfo) return;
    const { name, mobile, amount, billingMonth, due } = lastReceiptInfo;
    let cleanMobile = mobile.replace(/[^0-9]/g, "");
    if (cleanMobile.startsWith('0')) cleanMobile = '88' + cleanMobile;
    else if (cleanMobile.length === 10) cleanMobile = '880' + cleanMobile;
    else if (!cleanMobile.startsWith('88')) cleanMobile = '88' + cleanMobile;
    const message = `পেমেন্ট রিসিট - NetBill ISP\n--------------------------\nগ্রাহকের নাম: ${name}\nবিল মাস: ${billingMonth}\nপরিশোধিত টাকা: ৳ ${amount}\nবর্তমান বকেয়া: ৳ ${due}\n--------------------------\nআপনার পেমেন্টটি সফলভাবে গ্রহণ করা হয়েছে। ধন্যবাদ আমাদের সাথে থাকার জন্য।`;
    window.open(`https://wa.me/${cleanMobile}?text=${encodeURIComponent(message)}`, '_blank');
    setShowWhatsAppModal(false);
  };

  const openEditModal = (p) => { setEditingPayment(p); setEditEditAmount(p.amount.toString()); setShowEditModal(true); };

  const handleUpdatePayment = async (e) => {
    e.preventDefault();
    if (!editingPayment || !editAmount) return;
    setIsProcessing(true);
    try {
      const oldAmount = editingPayment.amount;
      const newAmount = parseFloat(editAmount);
      const diff = newAmount - oldAmount;
      await supabase.from('payments').update({ amount: newAmount, remarks: (editingPayment.remarks || '') + " (Edited)" }).eq('id', editingPayment.id);
      const customer = store.customers.find(c => c.id === editingPayment.customerId);
      if (customer) {
        const newDue = (customer.currentDue || 0) - diff;
        await supabase.from('customers').update({ current_due: newDue, payment_status: newDue <= 0 ? 'Paid' : 'Unpaid' }).eq('id', customer.id);
        await supabase.from('ledger_entries').update({ amount: newAmount, running_balance: newDue }).eq('reference_no', editingPayment.receiptNo);
      }
      alert("Collection updated!");
      setShowEditModal(false);
    } catch (err) { alert("Failed!"); }
    finally { setIsProcessing(false); }
  };

  return (
    <div className="max-w-7xl mx-auto space-y-4 pb-10 uppercase font-black tracking-tighter transition-all px-2 sm:px-4">
      <div className="flex items-center justify-between mb-2 md:mb-4">
         <div className="flex items-center space-x-3">
            <button onClick={() => setActivePage('dashboard')} className="w-10 h-10 bg-white dark:bg-slate-800 rounded-xl flex items-center justify-center text-teal-600 shadow-sm border border-slate-100">
               <i className="fas fa-arrow-left"></i>
            </button>
            <div className="space-y-0.5">
               <h3 className="text-xl sm:text-2xl font-black text-slate-800 dark:text-white uppercase leading-none">{t.payment_center}</h3>
               <p className="text-[8px] sm:text-[9px] text-teal-600 tracking-[2px] sm:tracking-[3px] font-black uppercase italic opacity-70">Collection Hub</p>
            </div>
         </div>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-12 gap-6 items-start">
        {/* LEFT COLUMN: COMPACT FORM */}
        <div className="xl:col-span-6 2xl:col-span-5 bg-white dark:bg-slate-800 p-6 md:p-8 rounded-[32px] md:rounded-[48px] shadow-2xl border-2 border-slate-50 dark:border-slate-700 font-black h-fit mx-auto xl:mx-0 w-full">
           <div className="mb-4 bg-indigo-50 dark:bg-indigo-900/20 p-4 rounded-2xl md:rounded-[24px] border-2 border-indigo-100 dark:border-indigo-800 flex items-center justify-between">
              <label className="text-[9px] md:text-[10px] font-black text-indigo-600 uppercase tracking-[2px]">Collector:</label>
              <select
                value={selectedCollector.id}
                disabled={session?.role === 'staff'}
                onChange={(e) => { const id = e.target.value; const name = e.target.options[e.target.selectedIndex].text; setSelectedCollector({ id, name }); }}
                className={`bg-white dark:bg-slate-800 border-none px-4 py-2 rounded-xl font-black text-[10px] md:text-xs uppercase shadow-sm outline-none cursor-pointer min-w-[150px] md:min-w-[200px] ${session?.role === 'staff' ? 'opacity-70 cursor-not-allowed' : ''}`}
              >
                <option value={session?.data?.id || 'admin'}>{session?.data?.name || 'Super Admin'} (YOU)</option>
                {session?.role !== 'staff' && store.staff?.filter(s => s.id !== session?.data?.id).map(s => (<option key={s.id} value={s.id}>{s.name}</option>))}
              </select>
           </div>

           <form onSubmit={handlePayment} className="space-y-4 md:space-y-6">
              <div className="space-y-2">
                <label className="text-[10px] md:text-xs font-black text-slate-400 uppercase tracking-[4px] ml-4">{t.find_subscriber}</label>
                <div className="relative space-y-3">
                  <div className="relative">
                    <input type="text" placeholder={t.search_placeholder} value={searchTerm} onChange={(e) => setSearchTerm(e.target.value)} className="w-full bg-slate-50 dark:bg-slate-950 border-none p-4 md:p-5 rounded-[18px] md:rounded-[28px] font-black text-lg md:text-xl shadow-inner outline-none focus:ring-4 focus:ring-teal-500/5 transition-all uppercase placeholder:opacity-20" />
                    <i className="fas fa-search absolute right-6 top-1/2 -translate-y-1/2 text-slate-300 text-xl"></i>
                  </div>

                  <select value={selectedCustomerId} size={searchTerm.length > 0 ? 5 : 1} onChange={(e) => { const id = e.target.value; setSelectedCustomerId(id); const cust = store.customers.find(c => c.id === id); if (cust) { setAmount(Math.floor(cust.currentDue || cust.current_due || 0).toString()); setSearchTerm(''); } }} className="w-full bg-white dark:bg-slate-800 border-4 border-teal-500/10 p-3 rounded-[18px] md:rounded-[28px] font-black text-sm md:text-base uppercase shadow-2xl outline-none cursor-pointer">
                    <option value="">-- {filteredCustomers.length} {t.results || 'Matches'} --</option>
                    {filteredCustomers.map(c => (<option key={c.id} value={c.id} className="p-2 border-b">{c.name} - {c.zone || 'Global'} - DUE: ৳{Math.floor(c.currentDue || c.current_due || 0)}</option>))}
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4 md:gap-6">
                 <div className="space-y-2">
                    <label className="text-[10px] font-black text-slate-400 uppercase tracking-[3px] ml-4">{t.billing_month}</label>
                    <select
                        value={billingMonth}
                        onChange={(e) => setBillingMonth(e.target.value)}
                        className="w-full bg-slate-50 dark:bg-slate-950 border-none p-3 md:p-4 rounded-[16px] md:rounded-[20px] font-black uppercase text-[10px] md:text-xs cursor-pointer shadow-inner"
                    >
                        {/* Dynamic Range Option */}
                        {billingMonth && !months.includes(billingMonth) && (
                            <option value={billingMonth}>{billingMonth}</option>
                        )}
                        {months.map(m => <option key={m} value={m}>{m}</option>)}
                    </select>
                 </div>
                 <div className="space-y-2"><label className="text-[10px] font-black text-slate-400 uppercase tracking-[3px] ml-4">{t.method}</label><select value={method} onChange={(e) => setMethod(e.target.value)} className="w-full bg-slate-50 dark:bg-slate-950 border-none p-3 md:p-4 rounded-[16px] md:rounded-[20px] font-black uppercase text-[10px] md:text-xs cursor-pointer shadow-inner"><option value="Cash">Cash</option><option value="bKash">bKash</option><option value="Nagad">Nagad</option><option value="Rocket">Rocket</option><option value="Bank">Bank</option></select></div>
              </div>

              <div className="space-y-2 text-center">
                <label className="text-[10px] text-slate-400 uppercase tracking-[4px]">ENTER AMOUNT (৳)</label>
                <input type="number" value={amount} onChange={(e) => setAmount(e.target.value)} placeholder="0.00" className="w-full bg-teal-50 dark:bg-slate-950 border-none p-5 md:p-7 rounded-[24px] md:rounded-[32px] font-black text-4xl md:text-6xl text-teal-600 tracking-tighter shadow-inner text-center outline-none" required />
              </div>

              <div className="bg-slate-50 dark:bg-slate-900/50 p-4 md:p-5 rounded-[20px] md:rounded-[28px] border-4 border-dashed border-slate-100 dark:border-slate-800 flex items-center justify-between group cursor-pointer transition-all hover:border-teal-500 shadow-sm" onClick={() => setSendSms(!sendSms)}>
                <div className="flex items-center space-x-4">
                  <div className={`w-10 h-10 md:w-12 md:h-12 rounded-xl md:rounded-[14px] flex items-center justify-center text-lg md:text-xl ${sendSms ? 'bg-teal-500 text-white shadow-lg shadow-teal-500/40' : 'bg-slate-200 text-slate-400'}`}>
                    <i className={`fas ${sendSms ? 'fa-comment-sms' : 'fa-comment-slash'}`}></i>
                  </div>
                  <div className="leading-tight">
                    <p className="text-[10px] md:text-sm font-black uppercase tracking-widest text-slate-800 dark:text-white">Confirmation SMS</p>
                    <p className="text-[8px] md:text-[9px] font-bold text-slate-400 uppercase tracking-[2px] mt-0.5">Auto receipt to customer</p>
                  </div>
                </div>
                <div className={`w-12 h-6 md:w-14 md:h-7 rounded-full relative transition-all duration-300 ${sendSms ? 'bg-teal-500' : 'bg-slate-300 shadow-inner'}`}>
                  <div className={`absolute top-1 md:top-1 w-4 h-4 md:w-5 md:h-5 bg-white rounded-full shadow-lg transition-all duration-500 ${sendSms ? 'left-7 md:left-8' : 'left-1'}`}></div>
                </div>
              </div>

              <button type="submit" disabled={isProcessing} className={`w-full py-5 md:py-6 rounded-[24px] md:rounded-[32px] font-black uppercase tracking-[5px] md:tracking-[8px] shadow-2xl transition-all h-18 md:h-22 text-sm md:text-lg ${isProcessing ? 'bg-slate-400' : 'bg-[#0D9488] text-white border-b-4 md:border-b-8 border-teal-900 shadow-teal-500/30 hover:scale-[1.01] active:scale-95'}`}>{isProcessing ? 'PROCESSING...' : 'COMMIT PAYMENT'}</button>
           </form>
        </div>

        {/* RIGHT COLUMN: HISTORY */}
        <div className="xl:col-span-6 2xl:col-span-7 space-y-4 md:space-y-6 font-black uppercase mt-4 xl:mt-0">
           <div className="flex justify-between items-center px-4 border-b-2 border-slate-100 dark:border-slate-800 pb-3 md:pb-4"><h4 className="text-xl md:text-2xl font-black text-slate-800 dark:text-white uppercase tracking-[4px] md:tracking-[6px]">History</h4><span className="bg-slate-100 dark:bg-slate-900 px-4 py-2 rounded-xl text-[10px] md:text-xs font-black tracking-widest leading-none border">LATEST 20</span></div>
           <div className="space-y-3 md:space-y-4 max-h-[600px] md:max-h-[700px] overflow-y-auto px-1 custom-scrollbar">
              {customerPayments.length > 0 ? customerPayments.map(p => (
                <div key={p.id} className="bg-white dark:bg-slate-800 p-4 md:p-6 rounded-[24px] md:rounded-[36px] shadow-xl border border-slate-100 dark:border-slate-700 flex justify-between items-center group transition-all font-black leading-none hover:border-teal-500/30 hover:shadow-2xl">
                   <div className="flex items-center space-x-4 md:space-x-6">
                      <div className="w-10 h-10 md:w-14 md:h-14 rounded-xl md:rounded-[20px] bg-slate-50 dark:bg-slate-950 text-teal-600 flex items-center justify-center text-xl md:text-2xl shadow-inner"><i className={`fas ${p.paymentMethod === 'Cash' ? 'fa-wallet' : p.paymentMethod === 'Bank' ? 'fa-building-columns' : 'fa-mobile-screen-button'}`}></i></div>
                      <div className="space-y-1.5 md:space-y-2">
                         <p className="text-sm md:text-lg font-black text-slate-800 dark:text-white uppercase leading-none tracking-tighter">{p.billingMonth}</p>
                         <p className="text-[9px] md:text-[10px] font-bold text-slate-400 uppercase tracking-widest leading-none">REF: {p.receiptNo}</p>
                         <button onClick={() => openEditModal(p)} className="text-[8px] md:text-[9px] font-black text-indigo-600 underline tracking-[2px] opacity-10 md:opacity-0 group-hover:opacity-100 transition-all uppercase">Edit</button>
                      </div>
                   </div>
                   <div className="text-right space-y-1.5 md:space-y-2 font-black">
                      <p className="text-lg md:text-2xl font-black text-emerald-600 tracking-tighter leading-none">৳{p.amount}</p>
                      <p className="text-[10px] md:text-[12px] font-black text-slate-900 dark:text-white uppercase tracking-[1px] mt-1">{formatDateDisplay(p.paymentDate)}</p>
                   </div>
                </div>
              )) : (<div className="text-center py-24 opacity-10 uppercase flex flex-col items-center"><i className="fas fa-hand-holding-dollar text-[100px] mb-6"></i><p className="text-xl font-black tracking-[10px]">No History</p></div>)}
           </div>
        </div>
      </div>

      {showEditModal && (
        <div className="fixed inset-0 bg-slate-900/95 backdrop-blur-2xl z-[3000] flex items-center justify-center p-6 animate-fadeIn font-black uppercase">
          <div className="bg-white dark:bg-slate-800 rounded-[72px] w-full max-w-xl p-16 shadow-2xl space-y-12 relative overflow-hidden border-2 border-slate-100 dark:border-slate-700">
             <div className="flex justify-between items-center border-b pb-8"><div className="space-y-1"><h3 className="text-4xl font-black uppercase tracking-tighter">Edit Collection</h3><p className="text-[10px] text-indigo-600 font-bold tracking-[4px]">REF: {editingPayment.receiptNo}</p></div><button onClick={() => setShowEditModal(false)} className="w-16 h-16 bg-rose-50 text-rose-500 rounded-full flex items-center justify-center text-2xl"><i className="fas fa-times"></i></button></div>
             <form onSubmit={handleUpdatePayment} className="space-y-8"><div className="space-y-4"><label className="text-[11px] text-slate-400 ml-4 tracking-[5px] font-black uppercase">Corrected Amount (৳)</label><input type="number" value={editAmount} onChange={e => setEditEditAmount(e.target.value)} className="w-full bg-slate-50 dark:bg-slate-900 border-none p-10 rounded-[48px] font-black text-6xl text-indigo-600 tracking-tighter text-center" required /></div><button type="submit" disabled={isProcessing} className="w-full bg-indigo-600 text-white py-8 rounded-[48px] font-black uppercase tracking-[10px] shadow-2xl border-b-8 border-indigo-900">{isProcessing ? 'UPDATING...' : 'UPDATE COLLECTION'}</button></form>
          </div>
        </div>
      )}

      {showExtensionModal && (
        <div className="fixed inset-0 bg-slate-900/95 backdrop-blur-2xl z-[5000] flex items-center justify-center p-6 font-black uppercase">
          <div className="bg-white dark:bg-slate-800 rounded-[64px] w-full max-w-xl p-12 shadow-2xl border-4 border-teal-500/20 space-y-10 relative overflow-hidden">
             <div className="absolute top-0 left-0 w-full h-4 bg-teal-600 shadow-lg"></div>
             <div className="text-center space-y-2">
                <div className="w-20 h-20 bg-teal-50 rounded-3xl flex items-center justify-center mx-auto text-4xl text-teal-500 shadow-inner mb-4 animate-pulse"><i className="fas fa-user-check"></i></div>
                <h3 className="text-3xl font-black tracking-tighter">Account Activation</h3>
                <p className="text-[10px] text-slate-400 tracking-[3px] font-bold">Manage status for partial payment</p>
             </div>
             <div className="bg-slate-50 dark:bg-slate-900 p-8 rounded-[40px] space-y-6 border-2 border-slate-100 dark:border-slate-800 shadow-inner">
                <div className="flex justify-between items-center border-b border-slate-200/50 pb-4">
                   <div className="space-y-1 text-left"><p className="text-[10px] text-slate-400 tracking-[2px]">CLIENT</p><p className="text-xl font-black text-slate-800 dark:text-white leading-none">{extensionData.customerName}</p></div>
                   <div className="text-right"><p className="text-[10px] text-indigo-500 tracking-[2px]">NEW DUE</p><p className="text-xl font-black text-rose-500 leading-none">৳ {Math.floor(extensionData.newDue)}</p></div>
                </div>
                <div className="flex justify-between items-center py-2">
                    <div className="space-y-1 text-left"><p className="text-[9px] text-slate-400 tracking-[2px]">PREVIOUS EXPIRE</p><p className="text-sm font-black text-slate-600 dark:text-slate-300">{extensionData.currentExpire}</p></div>
                    <i className="fas fa-arrow-right text-teal-500"></i>
                    <div className="space-y-1 text-right"><p className="text-[9px] text-teal-500 tracking-[2px]">NEW EXPIRE</p><p className="text-sm font-black text-teal-600">{extensionData.nextExpire}</p></div>
                </div>
                <div className="space-y-3 pt-4 border-t border-slate-200/50">
                   <label className="text-[11px] text-slate-400 ml-4 tracking-[4px] font-black">SET NEXT EXPIRE DATE</label>
                   <input type="text" value={extensionData.nextExpire} onChange={e => setExtensionData({...extensionData, nextExpire: e.target.value})} className="w-full bg-white dark:bg-slate-800 p-6 rounded-3xl font-black text-2xl text-indigo-600 text-center shadow-lg border-2 border-indigo-500/20 outline-none" placeholder="DD-MMM-YYYY" />
                   <p className="text-[8px] text-slate-400 text-center italic uppercase">* আপনি চাইলে তারিখটি পরিবর্তন করতে পারেন</p>
                </div>
             </div>
             <div className="grid grid-cols-1 gap-4">
                <button onClick={() => commitFinalPayment(extensionData.customerId, extensionData.amount, extensionData.newDue, extensionData.newAdvance, extensionData.nextExpire, 'Active')} className="w-full bg-emerald-600 text-white py-8 rounded-[40px] font-black uppercase tracking-[5px] shadow-2xl hover:scale-[1.02] active:scale-95 transition-all border-b-8 border-emerald-900">ACTIVATE & RECORD</button>
                <div className="grid grid-cols-2 gap-4">
                    <button onClick={() => commitFinalPayment(extensionData.customerId, extensionData.amount, extensionData.newDue, extensionData.newAdvance, extensionData.currentExpire, extensionData.status)} className="bg-slate-100 dark:bg-slate-700 text-slate-500 dark:text-slate-300 py-5 rounded-[28px] font-black text-[10px] tracking-widest hover:bg-slate-200 transition-all uppercase">PAYMENT ONLY</button>
                    <button onClick={() => { setShowExtensionModal(false); setIsProcessing(false); }} className="bg-rose-50 text-rose-500 py-5 rounded-[28px] font-black text-[10px] tracking-widest hover:bg-rose-100 transition-all uppercase">CANCEL</button>
                </div>
             </div>
          </div>
        </div>
      )}

      {showWhatsAppModal && (
        <div className="fixed inset-0 bg-slate-900/95 backdrop-blur-3xl z-[6000] flex items-center justify-center p-6 font-black uppercase text-center">
          <div className="bg-white dark:bg-slate-800 rounded-[64px] w-full max-w-lg p-12 shadow-2xl border-4 border-emerald-500/20 space-y-10 relative overflow-hidden">
             <div className="absolute top-0 left-0 w-full h-4 bg-emerald-500"></div>
             <div className="space-y-4">
                <div className="w-24 h-24 bg-emerald-50 text-emerald-600 rounded-full flex items-center justify-center mx-auto text-5xl shadow-inner animate-bounce"><i className="fas fa-check-circle"></i></div>
                <h3 className="text-4xl font-black tracking-tighter text-slate-800 dark:text-white">Payment Successful!</h3>
                <p className="text-xs text-slate-400 font-bold tracking-[3px]">Collection recorded in Cloud</p>
             </div>
             <div className="bg-slate-50 dark:bg-slate-900 p-8 rounded-[40px] space-y-4 border-2 border-slate-100 dark:border-slate-800">
                <div className="flex justify-between items-center text-sm border-b pb-4"><span className="text-slate-400 uppercase tracking-widest">Client:</span><span className="text-slate-800 dark:text-white">{lastReceiptInfo?.name}</span></div>
                <div className="flex justify-between items-center text-sm border-b pb-4"><span className="text-slate-400 uppercase tracking-widest">Amount:</span><span className="text-emerald-600 text-xl">৳ {lastReceiptInfo?.amount}</span></div>
                <div className="flex justify-between items-center text-sm"><span className="text-slate-400 uppercase tracking-widest">New Due:</span><span className="text-rose-600 text-xl">৳ {lastReceiptInfo?.due}</span></div>
             </div>
             <div className="grid grid-cols-1 gap-4">
                <button onClick={sendWhatsApp} className="w-full bg-[#25D366] text-white py-8 rounded-[40px] font-black uppercase tracking-[5px] shadow-[0_20px_40px_rgba(37,211,102,0.3)] hover:scale-105 active:scale-95 transition-all flex items-center justify-center space-x-4 border-b-8 border-[#1DA851]"><i className="fab fa-whatsapp text-3xl"></i><span>Send WhatsApp Receipt</span></button>
                <button onClick={() => setShowWhatsAppModal(false)} className="py-4 text-slate-400 text-[10px] font-black tracking-[4px] hover:text-slate-600 transition-colors">SKIP & CLOSE</button>
             </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Payments;
