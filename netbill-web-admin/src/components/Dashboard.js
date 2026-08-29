import React, { useState, useMemo } from 'react';
import { supabase } from '../supabaseClient';

const Dashboard = ({ store, session, permissions, setActivePage, setSearchMode, setInitialFilters, setReportInitialTab, navigateToAddCustomer, openSearch, openSummary, setPreSelectedCustomer, t }) => {
  const [activeFilter, setActiveFilter] = useState('today');
  const [customDate, setCustomDate] = useState(new Date().toLocaleDateString('en-CA'));
  const [isPromiseExpanded, setIsPromiseExpanded] = useState(true);

  // Date Range Selection for Expiry Audit
  const [showExpiryRangeModal, setShowExpiryRangeModal] = useState(false);
  const [expiryRange, setExpiryRange] = useState({
      start: new Date(new Date().getFullYear(), new Date().getMonth(), 1).toLocaleDateString('en-CA'),
      end: new Date().toLocaleDateString('en-CA')
  });

  const [showNewJoinRangeModal, setShowNewJoinRangeModal] = useState(false);
  const [newJoinRange, setNewJoinRange] = useState({
      start: new Date(new Date().getFullYear(), new Date().getMonth(), 1).toLocaleDateString('en-CA'),
      end: new Date().toLocaleDateString('en-CA')
  });

  const formatDateDisplay = (dateStr) => {
    if (!dateStr) return '';
    if (dateStr.includes('-')) {
       const [year, month, day] = dateStr.split('-');
       return `${day}-${month}-${year}`;
    }
    return dateStr;
  };

  const todayStr = new Date().toLocaleDateString('en-CA');
  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  const tomorrowISO = tomorrow.toLocaleDateString('en-CA');
  const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
  const tomorrowCustom = `${tomorrow.getDate().toString().padStart(2, '0')}-${months[tomorrow.getMonth()]}-${tomorrow.getFullYear()}`;

  const expiringTomorrow = useMemo(() => {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const tomorrowISO = tomorrow.toLocaleDateString('en-CA');
    const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
    const tomorrowCustom = `${tomorrow.getDate().toString().padStart(2, '0')}-${months[tomorrow.getMonth()]}-${tomorrow.getFullYear()}`;

    return store.customers.filter(c => {
      const eDate = c.expireDate || c.expire_date;
      const isDue = (parseFloat(c.currentDue || c.current_due || 0) > 0);
      if (!eDate || c.status !== 'Active' || !isDue) return false;

      // Staff Isolation
      if (session?.role === 'staff') {
        const staffId = session.data?.id;
        const staffName = session.data?.name;
        const assignedId = c.assignedStaffId || c.assigned_staff_id;
        if (!assignedId || (assignedId !== staffId && assignedId !== staffName)) return false;
      }

      return eDate === tomorrowISO || eDate === tomorrowCustom;
    });
  }, [store.customers, tomorrowISO, tomorrowCustom]);

  const yesterdayStr = new Date(Date.now() - 86400000).toLocaleDateString('en-CA');
  const last7DaysAgoStr = new Date(Date.now() - 7 * 86400000).toLocaleDateString('en-CA');
  const currentMonthStr = todayStr.substring(0, 7);

  const filteredPayments = useMemo(() => {
    return store.payments.filter(p => {
      const pDate = p.payment_date || p.paymentDate;

      // Staff Isolation: Only show their own collections if role is 'staff'
      if (session?.role === 'staff') {
         if (p.collected_by_id !== session.data.id && p.collected_by !== session.data.name && p.collectedBy !== session.data.name) return false;
      }

      if (activeFilter === 'today') return pDate === todayStr;
      if (activeFilter === 'yesterday') return pDate === yesterdayStr;
      if (activeFilter === 'last7') return pDate >= last7DaysAgoStr;
      if (activeFilter === 'month') return pDate && pDate.startsWith(currentMonthStr);
      if (activeFilter === 'custom') return pDate === customDate;
      return true;
    });
  }, [store.payments, activeFilter, todayStr, yesterdayStr, last7DaysAgoStr, currentMonthStr, customDate, session]);

  // Role-Based Customer Filtering for Stats
  const visibleCustomers = useMemo(() => {
    if (session?.role === 'staff') {
        return store.customers.filter(c => (c.assignedStaffId || c.assigned_staff_id) === session.data.id || (c.assignedStaffId || c.assigned_staff_id) === session.data.name);
    }
    return store.customers;
  }, [store.customers, session]);

  const { selectedTotal, dueTotal, expiredTotalCount, newJoinsThisMonth } = useMemo(() => {
    const selectedTotal = filteredPayments.reduce((s, p) => s + (parseFloat(p.amount) || 0), 0);
    const dueTotal = visibleCustomers.reduce((s, c) => s + (parseFloat(c.current_due || c.currentDue) || 0), 0);
    const expiredTotalCount = visibleCustomers.filter(c => c.status === 'Expired' || c.status === 'Suspended').length;
    const newJoinsThisMonth = visibleCustomers.filter(c => (c.joinDate || c.join_date)?.startsWith(currentMonthStr)).length;

    return { selectedTotal, dueTotal, expiredTotalCount, newJoinsThisMonth };
  }, [filteredPayments, visibleCustomers, currentMonthStr]);

  const targetPlan = store.settings?.monthlyTarget || store.settings?.monthly_target || 0;

  // VERIFICATION LOGIC
  const pendingRequests = store.paymentRequests?.filter(r => r.status === 'pending') || [];

  const handleApprove = async (req) => {
    try {
        const paymentDate = new Date().toLocaleDateString('en-CA');
        const timeStr = new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
        const receiptNo = "REC-" + Math.random().toString(36).substr(2, 6).toUpperCase();

        const customer = store.customers.find(c => c.id === req.customer_id);
        if (!customer) throw new Error("Customer not found in system");

        const payAmt = parseFloat(req.amount);
        let currentDue = (customer.current_due || customer.currentDue || 0);
        let currentAdvance = (customer.advance_balance || customer.advanceBalance || 0);

        let newDue = currentDue;
        let newAdvance = currentAdvance;

        if (payAmt > newDue) {
            const excess = payAmt - newDue;
            newAdvance += excess;
            newDue = 0;
        } else {
            newDue -= payAmt;
        }

        // 1. Record Payment
        await supabase.from('payments').insert({
            customer_id: req.customer_id,
            customer_name: req.customer_name,
            customer_code: req.customer_code,
            amount: payAmt,
            payment_method: req.method || 'Unknown',
            payment_date: paymentDate,
            receipt_no: receiptNo,
            transaction_id: req.trx_id || req.trxId || '',
            collected_by: req.collected_by || 'Staff Request',
            collected_by_id: req.collected_by_id || '',
            billing_month: req.billing_month || 'Current',
            remarks: "Staff Collection (Approved)"
        });

        // 2. Update Customer Balance & Expiry
        const updatePayload = {
            current_due: newDue,
            advance_balance: newAdvance,
            payment_status: newDue <= 0 ? 'Paid' : 'Unpaid'
        };
        // Only set expire_date if customer ALREADY has one set
        const currentExpire = customer.expireDate || customer.expire_date;
        if (newDue <= 0 && req.suggested_expire_date && currentExpire) {
            updatePayload.expire_date = req.suggested_expire_date;
        }
        await supabase.from('customers').update(updatePayload).eq('id', req.customer_id);

        // 3. Add Ledger
        await supabase.from('ledger_entries').insert({
            customer_id: req.customer_id,
            date: paymentDate,
            time: timeStr,
            type: "Payment",
            amount: payAmt,
            is_debit: false,
            reference_no: receiptNo,
            description: `Staff Collection: ${req.billing_month || 'Bill'}`,
            running_balance: newDue,
            collector_name: req.collected_by || 'Staff',
            paid_amount: payAmt,
            total_due_balance: newDue
        });

        // 4. Update Request Status
        await supabase.from('payment_requests').update({ status: 'approved' }).eq('id', req.id);

        // 5. Trigger SMS Notification
        const settings = store.settings;
        if (settings && settings.smsApiUrl && customer.mobile) {
            let msg = `Dear ${customer.name}, BDT ${payAmt} has been received for ${req.billing_month || 'your bill'}. New Due: BDT ${newDue}. Thank you.`;

            // Check for existing "Collection" template
            const template = store.smsTemplates?.find(t => (t.title === 'Collection' || t.title === 'কালেকশন') && (t.isActive || t.is_active));
            if (template) {
                msg = (template.messageContent || template.message_content)
                    .replace(/{NAME}/g, customer.name || '')
                    .replace(/{AMOUNT}/g, payAmt)
                    .replace(/{DUE}/g, Math.floor(newDue))
                    .replace(/{CUSTOMER_CODE}/g, customer.customerCode || customer.customer_code || '');
            }

            let cleanMobile = customer.mobile.replace(/[^0-9]/g, "");
            if (cleanMobile.startsWith('880')) cleanMobile = cleanMobile.substring(3);
            if (!cleanMobile.startsWith('0')) cleanMobile = '0' + cleanMobile;

            const isUnicode = /[\u0980-\u09FF]/.test(msg);
            const typeParam = isUnicode ? "&type=unicode" : "";

            const finalUrl = settings.smsApiUrl
                .replace(/{API_KEY}/g, settings.smsApiKey || '')
                .replace(/{SENDER_ID}/g, settings.smsSenderId || '1234')
                .replace(/{MOBILE}/g, cleanMobile)
                .replace(/{NUMBER}/g, cleanMobile)
                .replace(/{MESSAGE}/g, encodeURIComponent(msg)) + typeParam;

            // Dispatch using Image ping (CORS-safe)
            const img = new Image();
            img.src = finalUrl;

            // Record in SMS Logs
            await supabase.from('sms_logs').insert({
                customer_id: customer.id,
                customer_name: customer.name,
                mobile: cleanMobile,
                notification_type: 'Collection (Staff Approval)',
                message: msg,
                status: 'Sent',
                sent_timestamp: new Date().toISOString()
            });
        }

        alert("Payment Approved! Customer account updated.");
    } catch (e) {
        console.error(e);
        alert("Approval failed: " + e.message);
    }
  };

  const handleReject = async (id) => {
    if (window.confirm("Reject and delete this request?")) {
        await supabase.from('payment_requests').delete().eq('id', id);
    }
  };

  // BILL PROMISE REMINDERS
  const billPromises = useMemo(() => {
    return store.customers.filter(c => {
      const isDue = parseFloat(c.currentDue || c.current_due || 0) > 0;
      if (!c.promiseDate || !isDue) return false;

      // Staff Isolation: Only show their own bill promises if role is 'staff'
      if (session?.role === 'staff') {
        const staffId = session.data?.id;
        const staffName = session.data?.name;
        const assignedId = c.assignedStaffId || c.assigned_staff_id;

        if (!assignedId || (assignedId !== staffId && assignedId !== staffName)) return false;
      }

      return true;
    });
  }, [store.customers, session]);

  const todaysPromises = billPromises.filter(c => c.promiseDate === todayStr);
  const overduePromises = billPromises.filter(c => c.promiseDate < todayStr);

  return (
    <div className="max-w-7xl mx-auto space-y-6 md:space-y-12 pb-20 uppercase font-black tracking-widest transition-all relative">

      {/* BILL PROMISE REMINDERS */}
      {(todaysPromises.length > 0 || overduePromises.length > 0) && (
        <div className="space-y-4">
           <div
             onClick={() => setIsPromiseExpanded(!isPromiseExpanded)}
             className="flex items-center justify-between bg-indigo-50 dark:bg-indigo-900/20 p-5 rounded-[24px] cursor-pointer hover:bg-indigo-100 transition-all border-2 border-indigo-100 dark:border-indigo-800 shadow-sm"
           >
              <div className="flex items-center space-x-3 text-indigo-600">
                 <i className="fas fa-calendar-check text-xl"></i>
                 <h3 className="text-sm font-black tracking-[4px]">{t.bill_promise_reminders}</h3>
              </div>
              <i className={`fas ${isPromiseExpanded ? 'fa-chevron-up' : 'fa-chevron-down'} text-indigo-600`}></i>
           </div>

           {isPromiseExpanded && (
             <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 animate-fadeIn">
                {/* Overdue first */}
                {overduePromises.map(c => (
                  <PromiseCard key={c.id} customer={c} isOverdue={true} onPay={() => { setPreSelectedCustomer(c); setActivePage('payments'); }} />
                ))}
                {todaysPromises.map(c => (
                  <PromiseCard key={c.id} customer={c} isOverdue={false} onPay={() => { setPreSelectedCustomer(c); setActivePage('payments'); }} />
                ))}
             </div>
           )}
        </div>
      )}

      {/* TOP NOTIFICATION BAR - CUSTOMER COMPLAINTS / TICKETS */}
      {store.tickets?.filter(t => t.status === 'Open' || t.status === 'Pending').length > 0 && permissions.canSeeComplaintsAlert && (
        <div
          onClick={() => setActivePage('crm_tickets')}
          className="bg-amber-500 text-slate-900 p-4 md:p-6 rounded-[24px] md:rounded-[32px] shadow-2xl flex items-center justify-between cursor-pointer hover:scale-[1.01] active:scale-95 transition-all border-b-4 border-amber-700"
        >
           <div className="flex items-center space-x-3 md:space-x-5">
              <div className="w-10 h-10 md:w-14 md:h-14 bg-black/10 rounded-xl md:rounded-2xl flex items-center justify-center text-xl md:text-2xl relative text-black">
                 <i className="fas fa-headset"></i>
                 <span className="absolute -top-1 -right-1 bg-slate-900 text-white w-5 h-5 md:w-6 md:h-6 rounded-full flex items-center justify-center text-[8px] md:text-[10px] font-black border-2 border-amber-500 shadow-sm">{store.tickets.filter(t => t.status === 'Open' || t.status === 'Pending').length}</span>
              </div>
              <div>
                 <h4 className="text-sm md:text-xl font-black uppercase tracking-tighter leading-none">{t.tickets_attention}</h4>
                 <p className="text-[8px] md:text-sm font-black opacity-80 mt-1 uppercase tracking-widest leading-tight">{store.tickets.filter(t => t.status === 'Open' || t.status === 'Pending').length} {t.tickets_pending_msg}</p>
              </div>
           </div>
           <div className="w-8 h-8 md:w-12 md:h-12 bg-slate-900 text-white rounded-full flex items-center justify-center text-sm md:text-xl font-black shadow-lg">
              <i className="fas fa-chevron-right"></i>
           </div>
        </div>
      )}

      {/* PENDING VERIFICATION ALERTS */}
      {pendingRequests.length > 0 && permissions.canSeeVerificationAlert && (
        <div className="bg-rose-50 dark:bg-rose-900/20 p-6 md:p-10 rounded-[32px] md:rounded-[56px] border-4 border-rose-500/20 space-y-6 md:space-y-8 animate-pulse">
           <div className="flex items-center space-x-3 md:space-x-5 text-rose-600">
              <div className="w-10 h-10 md:w-14 md:h-14 bg-rose-500 text-white rounded-xl md:rounded-2xl flex items-center justify-center text-xl md:text-2xl shadow-lg">
                 <i className="fas fa-bell"></i>
              </div>
              <h3 className="text-xl md:text-3xl font-black uppercase tracking-tighter">{t.needs_verification}: {pendingRequests.length}</h3>
           </div>
           <div className="grid grid-cols-1 md:grid-cols-2 gap-4 md:gap-6">
              {pendingRequests.map(req => (
                <div key={req.id} className="bg-white dark:bg-slate-800 p-4 md:p-8 rounded-[24px] md:rounded-[40px] shadow-xl border border-rose-100 flex justify-between items-center group">
                   <div className="space-y-1 md:space-y-2 leading-none">
                      <div className="flex items-center space-x-2">
                         <p className="text-[8px] md:text-[10px] font-black bg-indigo-50 text-indigo-600 px-2 py-0.5 rounded-lg uppercase">{req.collected_by || 'Staff'}</p>
                         <p className="text-[10px] md:text-sm font-black text-slate-400">TrxID: <span className="text-rose-500">{req.trxId || req.trx_id || 'N/A'}</span></p>
                      </div>
                      <h4 className="text-sm md:text-xl font-black text-slate-800 dark:text-white uppercase truncate max-w-[150px] md:max-w-none">{req.customerName}</h4>
                      <p className="text-xl md:text-2xl font-black text-emerald-600">৳ {req.amount}</p>
                      <p className="text-[8px] md:text-[9px] font-bold text-slate-400 uppercase tracking-widest">{req.billing_month}</p>
                   </div>
                   <div className="flex space-x-2 md:space-x-3">
                      <button onClick={() => handleApprove(req)} className="w-10 h-10 md:w-14 md:h-14 bg-emerald-500 text-white rounded-xl md:rounded-2xl shadow-lg hover:scale-110 active:scale-95 transition-all"><i className="fas fa-check"></i></button>
                      <button onClick={() => handleReject(req.id)} className="w-10 h-10 md:w-14 md:h-14 bg-rose-100 text-rose-500 rounded-xl md:rounded-2xl shadow-sm hover:scale-110 transition-all"><i className="fas fa-times"></i></button>
                   </div>
                </div>
              ))}
           </div>
        </div>
      )}

      {/* Stats Grid */}
      {permissions.canSeeStatsCards && (
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 md:gap-7 font-black uppercase tracking-widest leading-none">
        <FeatureCard title={t.grid_collection} icon="fa-hand-holding-dollar" grad="grad-collection" onClick={() => setActivePage('payments')} />
        <FeatureCard title={t.collection_report} icon="fa-chart-column" grad="grad-invoices" onClick={() => { setReportInitialTab('collection'); setActivePage('reports'); }} />
        <FeatureCard title={t.grid_crm} icon="fa-users-viewfinder" grad="grad-subscribers" onClick={() => setActivePage('customers')} />
        <FeatureCard title={t.grid_tickets} icon="fa-ticket" grad="grad-tickets" onClick={() => setActivePage('crm_tickets')} />
        {session?.role === 'admin' && <FeatureCard title={t.grid_add} icon="fa-user-plus" grad="grad-create" onClick={navigateToAddCustomer} />}
        <FeatureCard title={t.grid_search} icon="fa-magnifying-glass-chart" grad="grad-search" onClick={openSearch} />
        <FeatureCard title={t.grid_due} icon="fa-money-bill-transfer" grad="grad-due" onClick={() => { setReportInitialTab('due'); setActivePage('reports'); }} />
        <FeatureCard title={t.grid_summary} icon="fa-chart-pie" grad="grad-summary" onClick={openSummary} />

        {/* NEW BOXES */}
        {session?.role === 'admin' && <FeatureCard title={t.grid_edit} icon="fa-user-pen" grad="grad-edit" onClick={() => { setSearchMode('edit'); openSearch(); }} />}
        <FeatureCard title={`${t.grid_expired} (${expiredTotalCount})`} icon="fa-user-xmark" grad="grad-expired" onClick={() => setShowExpiryRangeModal(true)} />
        <FeatureCard title={`${t.grid_new_subs} (${newJoinsThisMonth})`} icon="fa-user-check" grad="grad-new" onClick={() => setShowNewJoinRangeModal(true)} />

        <div className="feature-card bg-slate-800 h-32 md:h-40 rounded-[24px] md:rounded-[44px] p-4 md:p-8 flex flex-col justify-center text-white shadow-lg font-black uppercase tracking-widest relative overflow-hidden">
           <i className="fas fa-comment-sms text-2xl md:text-4xl mb-2 md:mb-3 opacity-30 absolute right-6 top-6"></i>
           <span className="text-[8px] md:text-[10px] opacity-60 mb-1">SMS Balance</span>
           <span className="text-xl md:text-3xl font-black tracking-tighter text-teal-400 leading-none">{store.smsBalance || '---'}</span>
        </div>
      </div>
      )}

      {/* Collection Breakdown Card */}
      {permissions.canSeeTodayCollection && (
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
              <BreakdownRow method="Cash" amount={filteredPayments.filter(p=>(p.paymentMethod || p.payment_method)==='Cash').reduce((s,p)=>s+(parseFloat(p.amount) || 0),0)} color="#10B981" icon="fa-wallet" total={selectedTotal} />
              <BreakdownRow method="bKash" amount={filteredPayments.filter(p=>(p.paymentMethod || p.payment_method)?.includes('bKash')).reduce((s,p)=>s+(parseFloat(p.amount) || 0),0)} color="#D0006F" icon="fa-mobile-screen-button" total={selectedTotal} />
              <BreakdownRow method="Nagad" amount={filteredPayments.filter(p=>(p.paymentMethod || p.payment_method)?.includes('Nagad')).reduce((s,p)=>s+(parseFloat(p.amount) || 0),0)} color="#F58220" icon="fa-credit-card" total={selectedTotal} />
              <BreakdownRow method="Bank Transfer" amount={filteredPayments.filter(p=>(p.paymentMethod || p.payment_method)?.includes('Bank')).reduce((s,p)=>s+(parseFloat(p.amount) || 0),0)} color="#0D9488" icon="fa-building-columns" total={selectedTotal} />
           </div>
        </div>
      </div>
      )}

      {/* Main Stats Summary */}
      {permissions.canSeeTotalCollection && (
      <div className="bg-[#0D9488] p-6 md:p-12 rounded-[32px] md:rounded-[64px] text-white shadow-2xl relative overflow-hidden group">
        <div className="absolute top-0 right-0 w-[300px] md:w-[600px] h-[300px] md:h-[600px] bg-white/5 rounded-full -mr-24 md:-mr-48 -mt-24 md:-mt-48 duration-1000 group-hover:scale-110"></div>
        <div className="flex flex-col md:flex-row justify-between items-start relative z-10 font-black tracking-widest leading-none uppercase gap-6 md:gap-8">
          <div className="space-y-4 md:space-y-12 w-full leading-none">
            <p className="text-[10px] md:text-sm font-black opacity-90 tracking-[3px] md:tracking-[5px] uppercase">{t.financial_total}</p>
            <h2 className="text-4xl sm:text-5xl md:text-9xl font-black tracking-tighter leading-none tracking-widest uppercase">৳ {Math.floor(selectedTotal).toLocaleString('en-US')}</h2>
            <div className="flex flex-col md:flex-row md:space-x-24 gap-6 md:gap-0 pt-4 md:pt-10 font-black tracking-widest uppercase">
               <div className="flex flex-col"><p className="text-[8px] md:text-[11px] font-bold opacity-70 mb-1 md:mb-4 tracking-[2px] md:tracking-[3px]">{t.target_plan}</p><p className="text-xl md:text-3xl font-black">৳ {targetPlan.toLocaleString('en-US')}</p></div>
               <div className="hidden md:block w-px h-20 bg-white/20"></div>
               <div className="flex flex-col"><p className="text-[8px] md:text-[11px] font-bold opacity-70 mb-1 md:mb-4 tracking-[2px] md:tracking-[3px] text-amber-300 uppercase">{t.total_outstanding}</p><p className="text-xl md:text-3xl text-amber-300 font-black tracking-widest uppercase">৳ {Math.floor(dueTotal).toLocaleString('en-US')}</p></div>
            </div>
          </div>
          <div className="w-12 h-12 md:w-24 md:h-24 bg-white/10 rounded-2xl md:rounded-[40px] flex items-center justify-center shadow-xl backdrop-blur-md transition-all group-hover:rotate-12 self-end md:self-start absolute top-4 right-4 md:relative md:top-0 md:right-0"><i className="fas fa-chart-line text-xl md:text-4xl text-white"></i></div>
        </div>
      </div>
      )}

      {/* EXPIRY DATE RANGE SELECTOR MODAL */}
      {showExpiryRangeModal && (
        <div className="fixed inset-0 bg-slate-900/90 backdrop-blur-2xl z-[10000] flex items-center justify-center p-6 uppercase font-black">
            <div className="bg-white dark:bg-slate-800 rounded-[64px] w-full max-w-xl p-12 shadow-2xl space-y-10 relative border-4 border-rose-500/20 animate-scaleIn overflow-hidden">
               <div className="absolute top-0 left-0 w-full h-4 bg-rose-600 shadow-lg"></div>

               <div className="text-center space-y-2">
                  <div className="w-20 h-20 bg-rose-50 rounded-3xl flex items-center justify-center mx-auto text-4xl text-rose-500 shadow-inner mb-4"><i className="fas fa-calendar-days"></i></div>
                  <h3 className="text-4xl font-black tracking-tighter">Expiry Date Range</h3>
                  <p className="text-[10px] text-slate-400 tracking-[3px] font-bold">Select period to filter expired subscribers</p>
               </div>

               <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                  <div className="space-y-3">
                    <label className="text-[11px] text-slate-400 ml-4 tracking-[4px] font-black uppercase">From Date</label>
                    <input
                      type="date"
                      value={expiryRange.start}
                      onChange={e => setExpiryRange({...expiryRange, start: e.target.value})}
                      className="w-full bg-slate-50 dark:bg-slate-950 p-6 rounded-3xl font-black text-lg outline-none border-4 border-transparent focus:border-rose-500/20 shadow-inner cursor-pointer"
                    />
                  </div>
                  <div className="space-y-3">
                    <label className="text-[11px] text-slate-400 ml-4 tracking-[4px] font-black uppercase">To Date</label>
                    <input
                      type="date"
                      value={expiryRange.end}
                      onChange={e => setExpiryRange({...expiryRange, end: e.target.value})}
                      className="w-full bg-slate-50 dark:bg-slate-950 p-6 rounded-3xl font-black text-lg outline-none border-4 border-transparent focus:border-rose-500/20 shadow-inner cursor-pointer"
                    />
                  </div>
               </div>

               <div className="grid grid-cols-1 gap-4">
                  <button
                    onClick={() => {
                        setInitialFilters({
                            status: 'Expired',
                            expiryStart: expiryRange.start,
                            expiryEnd: expiryRange.end
                        });
                        setActivePage('customers');
                        setShowExpiryRangeModal(false);
                    }}
                    className="w-full bg-rose-600 text-white py-8 rounded-[40px] font-black uppercase tracking-[5px] shadow-[0_20px_40px_rgba(220,38,38,0.3)] hover:scale-105 active:scale-95 transition-all border-b-8 border-rose-900"
                  >
                     VIEW EXPIRED LIST
                  </button>
                  <button onClick={() => setShowExpiryRangeModal(false)} className="py-4 text-slate-400 text-xs font-black tracking-[4px] hover:text-rose-500 transition-colors">CANCEL</button>
               </div>
            </div>
        </div>
      )}

      {/* NEW JOIN DATE RANGE SELECTOR MODAL */}
      {showNewJoinRangeModal && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-2xl z-[10000] flex items-center justify-center p-6 uppercase font-black">
            <div className="bg-white dark:bg-slate-800 rounded-[64px] w-full max-w-xl p-12 shadow-2xl space-y-10 relative border-4 border-emerald-500/20 animate-scaleIn overflow-hidden">
               <div className="absolute top-0 left-0 w-full h-4 bg-emerald-500 shadow-lg"></div>

               <div className="text-center space-y-2">
                  <div className="w-20 h-20 bg-emerald-50 rounded-3xl flex items-center justify-center mx-auto text-4xl text-emerald-600 shadow-inner mb-4"><i className="fas fa-user-plus"></i></div>
                  <h3 className="text-4xl font-black tracking-tighter">New Joins Range</h3>
                  <p className="text-[10px] text-slate-400 tracking-[3px] font-bold">Filter subscribers by joining date</p>
               </div>

               <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                  <div className="space-y-3">
                    <label className="text-[11px] text-slate-400 ml-4 tracking-[4px] font-black uppercase">Start Date</label>
                    <input
                      type="date"
                      value={newJoinRange.start}
                      onChange={e => setNewJoinRange({...newJoinRange, start: e.target.value})}
                      className="w-full bg-slate-50 dark:bg-slate-950 p-6 rounded-3xl font-black text-lg outline-none border-4 border-transparent focus:border-emerald-500/20 shadow-inner cursor-pointer"
                    />
                  </div>
                  <div className="space-y-3">
                    <label className="text-[11px] text-slate-400 ml-4 tracking-[4px] font-black uppercase">End Date</label>
                    <input
                      type="date"
                      value={newJoinRange.end}
                      onChange={e => setNewJoinRange({...newJoinRange, end: e.target.value})}
                      className="w-full bg-slate-50 dark:bg-slate-950 p-6 rounded-3xl font-black text-lg outline-none border-4 border-transparent focus:border-emerald-500/20 shadow-inner cursor-pointer"
                    />
                  </div>
               </div>

               <div className="grid grid-cols-1 gap-4">
                  <button
                    onClick={() => {
                        setInitialFilters({
                            status: 'All',
                            joinStart: newJoinRange.start,
                            joinEnd: newJoinRange.end
                        });
                        setActivePage('customers');
                        setShowNewJoinRangeModal(false);
                    }}
                    className="w-full bg-emerald-600 text-white py-8 rounded-[40px] font-black uppercase tracking-[5px] shadow-[0_20px_40px_rgba(16,185,129,0.3)] hover:scale-105 active:scale-95 transition-all border-b-8 border-emerald-900"
                  >
                     VIEW NEW SUBSCRIBERS
                  </button>
                  <button onClick={() => setShowNewJoinRangeModal(false)} className="py-4 text-slate-400 text-xs font-black tracking-[4px] hover:text-emerald-600 transition-colors">CANCEL</button>
               </div>
            </div>
        </div>
      )}

    </div>
  );
};

const PromiseCard = ({ customer, isOverdue, onPay }) => (
  <div className={`p-5 rounded-[28px] border-2 flex items-center justify-between shadow-xl transition-all hover:scale-[1.02] ${isOverdue ? 'bg-rose-50 border-rose-100 text-rose-700 animate-pulse' : 'bg-white border-slate-100 dark:bg-slate-800 dark:border-slate-700'}`}>
     <div className="flex items-center space-x-4">
        <div className={`w-12 h-12 rounded-2xl flex items-center justify-center text-xl shadow-lg ${isOverdue ? 'bg-rose-500 text-white' : 'bg-indigo-600 text-white'}`}>
           <i className={`fas ${isOverdue ? 'fa-triangle-exclamation' : 'fa-calendar-day'}`}></i>
        </div>
        <div>
           <h4 className="text-sm font-black uppercase tracking-tighter leading-none mb-1">{customer.name}</h4>
           <div className="flex flex-col space-y-1">
              <span className="text-[10px] font-black text-slate-500">ZONE: <span className="text-slate-800 dark:text-slate-200 font-black">{customer.zone || 'Global'}</span></span>
              <span className="text-[11px] font-black">DUE: <span className={isOverdue ? 'text-rose-600' : 'text-emerald-600'}>৳{Math.floor(customer.currentDue || customer.current_due || 0)}</span></span>
              {customer.promiseNote && <span className="text-[9px] font-bold text-indigo-500 italic">"{customer.promiseNote}"</span>}
           </div>
        </div>
     </div>
     <div className="flex items-center space-x-2">
        <a href={`tel:${customer.mobile}`} className="w-10 h-10 bg-indigo-50 text-indigo-600 rounded-xl flex items-center justify-center hover:bg-indigo-600 hover:text-white transition-all"><i className="fas fa-phone"></i></a>
        <button onClick={onPay} className="w-10 h-10 bg-emerald-50 text-emerald-600 rounded-xl flex items-center justify-center hover:bg-emerald-600 hover:text-white transition-all"><i className="fas fa-hand-holding-dollar"></i></button>
     </div>
  </div>
);

const FeatureCard = ({ title, icon, grad, onClick }) => (
  <div onClick={onClick} className={`feature-card ${grad} h-32 md:h-40 rounded-[24px] md:rounded-[44px] p-4 md:p-8 flex flex-col justify-center text-white cursor-pointer transition-all hover:-translate-y-1 hover:brightness-110 shadow-lg font-black uppercase tracking-widest`}>
    <i className={`fas ${icon} text-2xl md:text-4xl mb-2 md:mb-3 opacity-90`}></i>
    <span className="font-black text-[9px] md:text-[12px] uppercase tracking-widest leading-tight uppercase">{title}</span>
  </div>
);

const BreakdownRow = ({ method, amount, color, icon, total }) => {
  const p = total > 0 ? Math.floor((amount / total) * 100) : 0;
  return (
    <div className="flex items-center space-x-4 md:space-x-8 group transition-all uppercase font-black uppercase">
      <div className="w-12 h-12 md:w-18 md:h-18 rounded-2xl md:rounded-[28px] bg-slate-50 dark:bg-slate-900 flex items-center justify-center text-lg md:text-3xl shrink-0 shadow-sm" style={{ color }}>
          <i className={`fas ${icon}`}></i>
      </div>
      <div className="flex-1 space-y-2 md:space-y-4">
          <div className="flex justify-between items-end uppercase font-black uppercase">
              <span className="text-slate-800 dark:text-slate-200 text-sm md:text-xl tracking-tighter uppercase font-black uppercase">{method}</span>
              <div className="flex items-center space-x-4 md:space-x-10 uppercase font-black uppercase">
                  <span className="text-slate-400 text-xs md:text-lg font-black">{p}%</span>
                  <span className="text-slate-900 dark:text-white text-base md:text-3xl font-black uppercase tracking-tighter leading-none">৳ {Math.floor(amount).toLocaleString('en-US')}</span>
              </div>
          </div>
          <div className="h-2 md:h-2.5 w-full bg-slate-50 dark:bg-slate-900 rounded-full overflow-hidden shadow-inner uppercase font-black">
             <div className="h-full rounded-full shadow-lg duration-1000" style={{ width: `${p}%`, background: color }}></div>
          </div>
      </div>
    </div>
  );
};

export default Dashboard;
