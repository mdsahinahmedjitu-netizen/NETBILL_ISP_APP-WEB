import React, { useState, useEffect } from 'react';
import { supabase } from './supabaseClient';
import Sidebar from './components/Sidebar';

import Dashboard from './components/Dashboard';
import Customers from './components/Customers';
import Billing from './components/Billing';
import Payments from './components/Payments';
import CollectionReport from './components/CollectionReport';
import Expenses from './components/Expenses';
import Staff from './components/Staff';
import SalaryHistory from './components/SalaryHistory';
import Inventory from './components/Inventory';
import Infrastructure from './components/Infrastructure';
import Packages from './components/Packages';
import SmsSetup from './components/SmsSetup';
import SmsLogs from './components/SmsLogs';
import SupportTickets from './components/SupportTickets';
import UpdateBill from './components/UpdateBill';
import BillingSummary from './components/BillingSummary';
import Settings from './components/Settings';
import Login from './components/Login';
import CustomerPortal from './components/CustomerPortal';
import CustomerFullProfile from './components/CustomerFullProfile';
import MikroTik from './components/MikroTik';
import { translations } from './translations';
import './index.css';

function App() {
  const [session, setSession] = useState(() => {
    const saved = localStorage.getItem('netbill_session');
    return saved ? JSON.parse(saved) : null;
  });

  const [activePage, setActivePage] = useState('dashboard');
  const [reportInitialTab, setReportInitialTab] = useState('collection');
  const [selectedProfileId, setSelectedProfileId] = useState(null);
  const [selectedSummaryId, setSelectedSummaryId] = useState(null);
  const [showGlobalSearch, setShowGlobalSearch] = useState(false);
  const [showSummarySearch, setShowSummarySearch] = useState(false);
  const [globalSearchQuery, setGlobalSearchQuery] = useState('');
  const [isDarkMode, setIsDarkMode] = useState(localStorage.getItem('dark_mode') === 'true');
  const [lang, setLang] = useState(localStorage.getItem('app_lang') || 'bn');
  const [isSidebarOpen, setIsSidebarOpen] = useState(window.innerWidth > 1024);
  const [isDirectAddMode, setIsDirectAddMode] = useState(false);
  const [preSelectedCustomer, setPreSelectedCustomer] = useState(null);
  const [searchMode, setSearchMode] = useState('view');
  const [initialFilters, setInitialFilters] = useState(null);

  const navigateToAddCustomer = () => {
    setIsDirectAddMode(true);
    setActivePage('new_enrollment');
  };

  useEffect(() => {
    const handleResize = () => {
      if (window.innerWidth <= 1024) setIsSidebarOpen(false);
      else setIsSidebarOpen(true);
    };
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  const [store, setStore] = useState({
    customers: [], tickets: [], payments: [], invoices: [], expenses: [], staff: [],
    inventory: [], packages: [], settings: {}, paymentRequests: [], staffPayouts: [],
    zones: [], subZones: [], boxes: [], expenseCategories: [], smsTemplates: [], smsLogs: [],
    ledgerEntries: [], mikrotikRouters: [], onlineUsernames: [], smsBalance: 'Checking...'
  });

  const [autoOpenAddModal, setAutoOpenAddModal] = useState(false);
  const [showExpiryModal, setShowExpiryModal] = useState(false);
  const [showQuickDateModal, setShowQuickDateModal] = useState(false);
  const [quickDateCust, setQuickDateCust] = useState(null);
  const [quickDates, setQuickDates] = useState({ expireDate: '', requestDate: '' });
  const [selectedExpiryTemplate, setSelectedExpiryTemplate] = useState('Expiry Reminder (Tomorrow)');

  const systemSmsTypes = [
    "All Customer", "Area Wise Customer Due List", "Area Wise Customer List",
    "Auto Temporary Disable Alert", "Bill Generate", "Collection",
    "Collection (MFS) to Owner", "Collection Delete", "Collection Edit",
    "Collection to Owner", "Complain Employee", "Complain List",
    "Complain to Customer", "Create Customer", "Create Customer to Owner",
    "Customer Complaint Notification Message", "Free Customer List",
    "Inactive Customer List", "Failed to Disable at Mikrotik", "Expired Customer",
    "Expiry Reminder (Tomorrow)", "Expiry Reminder (Today)", "Staff Salary Alert"
  ];

  window.openExpiryModal = () => setShowExpiryModal(true);

  const t = translations[lang];

  const sendAndLogSms = async (c, msg, type) => {
    try {
        const s = storeRef.current.settings;
        const isUni = /[\u0980-\u09FF]/.test(msg);

        let phone = c.mobile.replace(/[^0-9]/g, "");
        if (phone.startsWith('0')) phone = '88' + phone; else if (phone.length === 10) phone = '880' + phone; else if (!phone.startsWith('88')) phone = '88' + phone;
        new Image().src = `https://tglplinxvrqsrxeicvpr.supabase.co/functions/v1/sms-proxy?apikey=${s.smsApiKey}&callerID=${s.smsSenderId}&number=${phone}&message=${encodeURIComponent(msg)}&type=${isUni?"unicode":"text"}`;
        await supabase.from('sms_logs').insert({ customer_id: c.id, customer_name: c.name, mobile: phone, notification_type: type, message: msg, status: 'Sent', sent_timestamp: new Date().toISOString() });
        return true;
    } catch (e) { return false; }
  };

  const handleQuickDateUpdate = async () => {
    if (!quickDateCust) return;
    try {
      const updates = { expire_date: quickDates.expireDate, request_date: quickDates.requestDate };
      const now = new Date(); now.setHours(0, 0, 0, 0);
      const reqDate = quickDates.requestDate ? new Date(quickDates.requestDate) : null;
      let shouldActivate = false;
      if ((quickDateCust.status === 'Suspended' || quickDateCust.status === 'Expired') && reqDate && reqDate >= now) {
          updates.status = 'Active'; shouldActivate = true;
      }
      const { error } = await supabase.from('customers').update(updates).eq('id', quickDateCust.id);
      if (error) throw error;
      if (shouldActivate) {
          const rId = quickDateCust.routerId || quickDateCust.router_id;
          const pUser = quickDateCust.pppoeUsername || quickDateCust.pppoe_username;
          if (rId && pUser) {
              supabase.functions.invoke('mikrotik-manager', { body: { action: 'set_status', routerId: rId, payload: { username: pUser, active: true } } });
          }
      }
      alert("Dates Updated Successfully!");
      setShowQuickDateModal(false);
    } catch (e) { alert("Update Failed!"); }
  };

  const expiringTomorrow = React.useMemo(() => {
    const tomorrow = new Date(); tomorrow.setDate(tomorrow.getDate() + 1);
    const tomorrowISO = tomorrow.toLocaleDateString('en-CA');
    const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
    const tomorrowCustom = `${tomorrow.getDate().toString().padStart(2, '0')}-${months[tomorrow.getMonth()]}-${tomorrow.getFullYear()}`;
    return store.customers.filter(c => {
      const eDate = c.expireDate || c.expire_date;
      if (!eDate || c.status !== 'Active') return false;
      return eDate === tomorrowISO || eDate === tomorrowCustom;
    });
  }, [store.customers]);

  const currentPermissions = React.useMemo(() => {
    if (!session) return {};
    if (session.role === 'admin') {
      return {
        canCollect: true, canSeeMobile: true, canEdit: true, canDelete: true, canAdd: true, canSeeRevenue: true,
        canInventory: true, canSuspend: true, canLedger: true, canPasswords: true, canExpenses: true, canSMS: true,
        canDiscount: true, canBulkBill: true, canEditPayments: true, canManageStock: true, canAssignAssets: true,
        canManageZones: true, canManageRouters: true, canResolveTickets: true, canSendBulkSMS: true, canEditTemplates: true,
        canSeeStatsCards: true, canSeeExpiryAlerts: true, canSeeComplaintsAlert: true, canSeeVerificationAlert: true,
        canSeeTodayCollection: true, canSeeTotalCollection: true,
        canAccessBilling: true, canAccessReports: true, canAccessInventory: true, canAccessPackages: true, canAccessSMS: true,
        canAccessSalary: true, canAccessTickets: true, canAccessCustomers: true, canAccessPayments: true, canAccessExpenses: true,
        canAccessStaff: true, canAccessInfrastructure: true, canAccessSmsLogs: true, canAccessGlobalSettings: true
      };
    }
    if (session.role === 'staff') {
      const roleName = session.data.role;
      return store.settings?.rolePermissions?.[roleName] || store.settings?.role_permissions?.[roleName] || {};
    }
    return {};
  }, [session, store.settings]);

  useEffect(() => {
    if (isDarkMode) document.body.classList.add('dark-mode');
    else document.body.classList.remove('dark-mode');
  }, [isDarkMode]);

  // FULL CLOUD SYNC ENGINE
  useEffect(() => {
    if (!session) return;
    const tableMapping = {
      'customers': 'customers', 'support_tickets': 'tickets', 'payments': 'payments',
      'payment_requests': 'paymentRequests', 'invoices': 'invoices', 'expenses': 'expenses',
      'staff': 'staff', 'staff_payouts': 'staffPayouts', 'zones': 'zones',
      'sub_zones': 'subZones', 'boxes': 'boxes', 'expense_categories': 'expenseCategories',
      'packages': 'packages', 'inventory_items': 'inventory', 'sms_templates': 'smsTemplates',
      'sms_logs': 'smsLogs', 'mikrotik_routers': 'mikrotikRouters'
      // Removing ledger_entries from initial global fetch to speed up loading
    };
    const fetchData = async () => {
      try {
        const tableEntries = Object.entries(tableMapping);

        // Fetch all essential tables in parallel
        const results = await Promise.all([
          ...tableEntries.map(async ([table, storeKey]) => {
            let query = supabase.from(table).select('*');
            if (table === 'sms_logs') query = query.order('sent_timestamp', { ascending: false }).limit(200);
            if (table === 'payments') query = query.order('payment_date', { ascending: false }).limit(500);
            const { data, error } = await query;
            return { storeKey, data, error };
          }),
          supabase.from('settings').select('*').limit(1).maybeSingle().then(res => ({ storeKey: 'settings', data: res.data, error: res.error }))
        ]);

        const updates = {};
        results.forEach(({ storeKey, data, error }) => {
          if (!error && data) {
            if (Array.isArray(data)) updates[storeKey] = data.map(mapToCamelCase);
            else updates[storeKey] = mapToCamelCase(data);
          }
        });

        setStore(prev => ({ ...prev, ...updates }));

        // Start real-time after initial load is complete
        startRealtime();
      } catch (e) {
        console.error("Global fetch error:", e);
      }
    };

    let activeChannels = [];
    const startRealtime = () => {
        activeChannels = Object.keys(tableMapping).map(table => {
            return supabase.channel(`${table}-changes`).on('postgres_changes', { event: '*', schema: 'public', table: table }, (payload) => {
                const { eventType, new: newRecord, old: oldRecord } = payload;
                const storeKey = tableMapping[table];
                setStore(prev => {
                  const currentList = prev[storeKey] || [];
                  let newList;
                  if (eventType === 'INSERT') newList = [...currentList, mapToCamelCase(newRecord)];
                  else if (eventType === 'UPDATE') newList = currentList.map(item => item.id === (newRecord.id || item.id) ? mapToCamelCase(newRecord) : item);
                  else if (eventType === 'DELETE') newList = currentList.filter(item => item.id !== oldRecord.id);
                  else newList = currentList;
                  return { ...prev, [storeKey]: newList };
                });
            }).subscribe();
        });
    };

    window.refreshData = fetchData;
    fetchData();

    return () => activeChannels.forEach(c => supabase.removeChannel(c));
  }, [session]);

  // AUTO-SUSPEND & SMS ENGINE (Multi-Tab Safe)
  const isCheckingExpiries = React.useRef(false);
  const storeRef = React.useRef(store);
  useEffect(() => { storeRef.current = store; }, [store]);

  useEffect(() => {
    if (!session || session.role !== 'admin') return;
    const checkExpiries = async () => {
      if (isCheckingExpiries.current) return;
      const lastRun = localStorage.getItem('isp_auto_run');
      if (lastRun && (Date.now() - parseInt(lastRun)) < 15 * 60 * 1000) return;
      isCheckingExpiries.current = true;
      localStorage.setItem('isp_auto_run', Date.now().toString());

      const now = new Date(); now.setHours(0, 0, 0, 0);
      const todayStart = now.toISOString().split('T')[0] + 'T00:00:00Z';
      const tomorrowStr = new Date(now.getTime() + 86400000).toLocaleDateString('en-CA');

      try {
        const { data: logsToday } = await supabase.from('sms_logs').select('customer_id, notification_type').gte('sent_timestamp', todayStart);
        const sentSet = new Set(logsToday?.map(l => `${l.customer_id}|${l.notification_type}`) || []);

        for (const cust of storeRef.current.customers) {
            const eDateStr = cust.expireDate || cust.expire_date;
            const rDateStr = cust.requestDate || cust.request_date;
            const status = cust.status || 'Active';

            const parseDate = (s) => {
              if(!s) return null;
              let year, month, day;
              if(s.includes('-') && s.split('-')[0].length === 4) {
                // YYYY-MM-DD format
                [year, month, day] = s.split('-').map(Number);
                return new Date(year, month - 1, day, 0, 0, 0, 0);
              } else if(s.includes('-')) {
                // DD-MMM-YYYY format
                const p = s.split('-');
                const mArr = {"Jan":0,"Feb":1,"Mar":2,"Apr":3,"May":4,"Jun":5,"Jul":6,"Aug":7,"Sep":8,"Oct":9,"Nov":10,"Dec":11};
                return new Date(parseInt(p[2]), mArr[p[1]], parseInt(p[0]), 0, 0, 0, 0);
              }
              return null;
            };

            const expObj = parseDate(eDateStr);
            const reqObj = parseDate(rDateStr);
            const today = new Date(); today.setHours(0, 0, 0, 0);

            // --- LOGIC A: AUTO REMINDER FOR TOMORROW ---
            if (status === 'Active' && eDateStr === tomorrowStr) {
                const key = `${cust.id}|Reminder`;
                if (!sentSet.has(key) && storeRef.current.settings?.smsApiKey && cust.mobile) {
                    const tpl = storeRef.current.smsTemplates?.find(t => t.title === 'Expiry Reminder (Tomorrow)' && (t.isActive || t.is_active));
                    if (tpl) {
                        const msg = (tpl.messageContent || tpl.message_content).replace(/{NAME}/g, cust.name || '').replace(/{DATE}/g, eDateStr);
                        if (await sendAndLogSms(cust, msg, 'Reminder')) sentSet.add(key);
                    }
                }
            }

            // --- LOGIC B: SUSPEND IF EXPIRED & NO VALID REQUEST ---
            // Request date today or in future means they should stay ACTIVE
            const isRequestValid = reqObj && reqObj >= today;

            if (status === 'Active' && expObj && expObj < today && !isRequestValid) {
                console.log(`Suspending (Expired): ${cust.name}`);
                await updateStatus(cust, 'Suspended', false);

                const key = `${cust.id}|Expired`;
                if (!sentSet.has(key) && storeRef.current.settings?.smsApiKey && cust.mobile) {
                    const tpl = storeRef.current.smsTemplates?.find(t => t.title === 'Expired Customer' && (t.isActive || t.is_active));
                    if (tpl) {
                        const msg = (tpl.messageContent || tpl.message_content).replace(/{NAME}/g, cust.name || '').replace(/{DATE}/g, eDateStr);
                        if (await sendAndLogSms(cust, msg, 'Expired')) sentSet.add(key);
                    }
                }
            }

            // --- LOGIC C: AUTO-ENABLE IF REQUEST DATE IS IN FUTURE ---
            if ((status==='Suspended'||status==='Expired') && isRequestValid) {
                console.log(`Re-activating (On Request): ${cust.name}`);
                await updateStatus(cust, 'Active', true);
            }
        }
      } catch (e) { console.error(e); } finally { isCheckingExpiries.current = false; }
    };

    const updateStatus = async (c, s, act) => {
        await supabase.from('customers').update({ status: s }).eq('id', c.id);
        if (c.pppoeUsername && (c.routerId || c.router_id)) supabase.functions.invoke('mikrotik-manager', { body: { action: 'set_status', routerId: c.routerId || c.router_id, payload: { username: c.pppoeUsername, active: act } } });
    };
    const t = setTimeout(checkExpiries, 30000); const i = setInterval(checkExpiries, 60 * 60 * 1000);
    return () => { clearTimeout(t); clearInterval(i); };
  }, [session]);

  // MIKROTIK GLOBAL ONLINE POLLING
  useEffect(() => {
    if (!session || store.mikrotikRouters.length === 0) return;
    const poll = async () => {
      let all = [];
      try {
        for (const r of store.mikrotikRouters) {
          const { data } = await supabase.functions.invoke('mikrotik-manager', { body: { routerId: r.id, action: 'get_status' } });
          if (data?.success && data.sessions) all = [...all, ...data.sessions.map(s => (s.username || '').toLowerCase())];
        }
        setStore(prev => ({ ...prev, onlineUsernames: [...new Set(all)] }));
      } catch (e) { console.error(e); }
    };
    poll(); const i = setInterval(poll, 45000); return () => clearInterval(i);
  }, [session, store.mikrotikRouters.length]);

  // SMS BALANCE FETCH
  useEffect(() => {
    const fetchB = async () => {
      const k = store.settings?.smsApiKey || store.settings?.sms_api_key; if (!k) return;
      try {
        const r = await fetch(`https://tglplinxvrqsrxeicvpr.supabase.co/functions/v1/sms-proxy?action=balance&apikey=${k}`);
        const d = await r.json(); if (d?.balance) setStore(prev => ({ ...prev, smsBalance: `৳ ${d.balance}` }));
      } catch (e) { console.error(e); }
    };
    if (store.settings?.smsApiKey) { fetchB(); const i = setInterval(fetchB, 60000); return () => clearInterval(i); }
  }, [store.settings?.smsApiKey]);

  const mapToCamelCase = (obj) => {
    if (!obj) return obj; const n = {};
    Object.keys(obj).forEach(k => { const ck = k.replace(/(_\w)/g, m => m[1].toUpperCase()); n[ck] = obj[k]; }); return n;
  };

  if (!session) return <Login onLoginSuccess={(d) => { setSession(d); localStorage.setItem('netbill_session', JSON.stringify(d)); }} />;

  return (
    <div className={`flex h-screen overflow-hidden transition-colors duration-300 ${isDarkMode ? 'dark-mode bg-slate-900 text-white' : 'bg-slate-50 text-slate-800'}`}>
      <Sidebar isSidebarOpen={isSidebarOpen} setIsSidebarOpen={setIsSidebarOpen} activePage={activePage} setActivePage={setActivePage} onLogout={() => { setSession(null); localStorage.removeItem('netbill_session'); setActivePage('dashboard'); }} t={t} role={session.role} subRole={session.role === 'staff' ? session.data.role : null} permissions={currentPermissions} />
      <div className={`flex-1 flex flex-col overflow-hidden transition-all duration-300`}>
        <header className={`h-16 md:h-20 border-b flex justify-between items-center px-4 md:px-10 shrink-0 z-10 transition-colors ${isDarkMode ? 'bg-slate-800 border-slate-700 text-white' : 'bg-white border-slate-100 text-slate-800'}`}>
          <div className="flex items-center space-x-2 md:space-x-6">
            <button onClick={() => setIsSidebarOpen(!isSidebarOpen)} className="w-10 h-10 md:w-12 md:h-12 bg-slate-50 dark:bg-slate-900 rounded-xl md:rounded-2xl flex items-center justify-center text-teal-600 shadow-sm border border-slate-100 dark:border-slate-800"><i className={`fas ${isSidebarOpen ? 'fa-indent' : 'fa-outdent'} text-base md:text-xl`}></i></button>
            <h2 className="text-xs md:text-xl font-black uppercase tracking-tighter leading-none whitespace-nowrap">NetBill <span className="hidden sm:inline">ISP | </span><span className="text-teal-600 truncate max-w-[80px] sm:max-w-none inline-block">{activePage?.toUpperCase()}</span></h2>
          </div>
          <div className="flex items-center space-x-2 md:space-x-10">
            <button onClick={() => { const nl = lang === 'en' ? 'bn' : 'en'; setLang(nl); localStorage.setItem('app_lang', nl); }} className="flex items-center space-x-1 bg-slate-50 dark:bg-slate-900 px-2 md:px-4 py-1.5 md:py-2 rounded-xl md:rounded-2xl border border-slate-100 dark:border-slate-800"><i className="fas fa-globe text-teal-600 text-[10px] md:text-sm"></i><span className="text-[8px] md:text-[10px] font-black uppercase tracking-widest">{lang === 'en' ? 'বাংলা' : 'EN'}</span></button>
            <button onClick={() => { const nm = !isDarkMode; setIsDarkMode(nm); localStorage.setItem('dark_mode', nm.toString()); }} className="text-teal-600 text-lg md:text-2xl transition-transform hover:scale-110"><i className={`fas ${isDarkMode ? 'fa-sun text-amber-400' : 'fa-moon'}`}></i></button>
            <div className="flex items-center space-x-2 md:space-x-4 border-l border-slate-100 dark:border-slate-700 pl-2 md:pl-0 md:border-none">
              <img src={`https://ui-avatars.com/api/?name=${session.data.name}&background=0D9488&color=fff&bold=true`} className="w-7 h-7 md:w-10 md:h-10 rounded-lg md:rounded-xl shadow-md" alt="User" />
              <div className="hidden lg:block text-left leading-none space-y-1"><p className="text-[10px] md:text-xs font-black uppercase text-slate-900 dark:text-white">{session.data.name}</p><p className="text-[8px] md:text-[9px] text-teal-600 font-bold uppercase tracking-widest italic">{session.role === 'admin' ? t.super_admin : 'Field Staff'}</p></div>
            </div>
          </div>
        </header>
        <main className="flex-1 overflow-y-auto p-4 md:p-10 scroll-smooth transition-colors font-black">
          {activePage === 'dashboard' && (session.role === 'customer' ? <CustomerPortal store={store} customer={session.data} t={t} /> : <Dashboard store={store} session={session} permissions={currentPermissions} setActivePage={setActivePage} setSearchMode={setSearchMode} setInitialFilters={setInitialFilters} setReportInitialTab={setReportInitialTab} navigateToAddCustomer={navigateToAddCustomer} openSearch={() => setShowGlobalSearch(true)} openSummary={() => setShowSummarySearch(true)} t={t} lang={lang} />)}
          {preSelectedCustomer && <Customers store={store} session={session} setActivePage={setActivePage} t={t} lang={lang} preSelectedCustomer={preSelectedCustomer} setPreSelectedCustomer={setPreSelectedCustomer} hideTable={true} />}
          {activePage === 'customers' && <Customers store={store} session={session} setActivePage={setActivePage} t={t} lang={lang} autoOpenModal={autoOpenAddModal} setAutoOpenModal={setAutoOpenAddModal} setProfileId={(id) => { setSelectedProfileId(id); setActivePage('customer_profile'); }} preSelectedCustomer={preSelectedCustomer} setPreSelectedCustomer={setPreSelectedCustomer} initialFilters={initialFilters} setInitialFilters={setInitialFilters} />}
          {activePage === 'customer_profile' && <CustomerFullProfile store={store} customerId={selectedProfileId} onBack={() => setActivePage('customers')} t={t} />}
          {activePage === 'new_enrollment' && session.role === 'admin' && <Customers store={store} session={session} setActivePage={setActivePage} t={t} lang={lang} autoOpenModal={true} isDirectMode={true} setProfileId={(id) => { setSelectedProfileId(id); setActivePage('customer_profile'); }} setPreSelectedCustomer={setPreSelectedCustomer} />}
          {activePage === 'billing' && <Billing store={store} t={t} lang={lang} setActivePage={setActivePage} />}
          {activePage === 'payments' && <Payments store={store} session={session} t={t} lang={lang} preSelectedCustomer={preSelectedCustomer} setPreSelectedCustomer={setPreSelectedCustomer} setActivePage={setActivePage} />}
          {activePage === 'reports' && <CollectionReport store={store} session={session} t={t} initialTab={reportInitialTab} setActivePage={setActivePage} />}
          {activePage === 'expenses' && <Expenses store={store} session={session} t={t} setActivePage={setActivePage} />}
          {activePage === 'staff' && session.role === 'admin' && <Staff store={store} session={session} t={t} lang={lang} setActivePage={setActivePage} />}
          {activePage === 'salary_history' && <SalaryHistory store={store} session={session} t={t} lang={lang} setActivePage={setActivePage} />}
          {activePage === 'inventory' && <Inventory store={store} t={t} lang={lang} setActivePage={setActivePage} />}
          {activePage === 'infrastructure' && session.role === 'admin' && <Infrastructure store={store} t={t} setActivePage={setActivePage} />}
          {activePage === 'packages' && <Packages store={store} t={t} lang={lang} setActivePage={setActivePage} />}
          {activePage === 'sms_setup' && <SmsSetup store={store} t={t} setActivePage={setActivePage} />}
          {activePage === 'sms_logs' && <SmsLogs store={store} setActivePage={setActivePage} />}
          {activePage === 'update_bill' && <UpdateBill store={store} t={t} setActivePage={setActivePage} />}
          {activePage === 'mikrotik' && session.role === 'admin' && <MikroTik store={store} t={t} setActivePage={setActivePage} />}
          {activePage === 'crm_tickets' && <SupportTickets store={store} session={session} t={t} setActivePage={setActivePage} />}
          {activePage === 'billing_summary' && <BillingSummary store={store} t={t} initialCustomerId={session.role === 'customer' ? session.data.id : selectedSummaryId} isCustomerView={session.role === 'customer'} setActivePage={setActivePage} />}
          {activePage === 'settings' && session.role === 'admin' && <Settings store={store} t={t} lang={lang} setActivePage={setActivePage} />}
        </main>
        <div onClick={() => setShowExpiryModal(true)} className={`fixed bottom-10 right-10 w-16 h-16 rounded-full bg-rose-600 text-white flex items-center justify-center shadow-2xl cursor-pointer hover:scale-110 active:scale-95 transition-all z-[100] animate-bounce ${expiringTomorrow.length > 0 && currentPermissions.canSeeExpiryAlerts ? 'flex' : 'hidden'}`}><i className="fas fa-bell text-2xl"></i><span className="absolute -top-1 -right-1 bg-white text-rose-600 w-6 h-6 rounded-full flex items-center justify-center text-[10px] font-black border-2 border-rose-600 shadow-sm">{expiringTomorrow.length}</span></div>
        {showExpiryModal && currentPermissions.canSeeExpiryAlerts && (
          <div className="fixed inset-0 bg-slate-900/90 backdrop-blur-xl z-[9999] flex items-center justify-center p-6 uppercase font-black">
            <div className="bg-white dark:bg-slate-800 rounded-[56px] w-full max-w-2xl p-12 shadow-2xl border-4 border-rose-500/20 space-y-8 relative overflow-hidden">
               <div className="absolute top-0 left-0 w-full h-3 bg-rose-600"></div>
               <div className="flex justify-between items-center border-b pb-6"><div><h3 className="text-4xl font-black text-slate-800 dark:text-white tracking-tighter">Expiry Alerts</h3><p className="text-[10px] text-slate-400 font-bold mt-2 tracking-[3px]">Total {expiringTomorrow.length} Customers Expiring Tomorrow</p></div><button onClick={() => setShowExpiryModal(false)} className="text-rose-500 text-3xl hover:scale-110 transition-all"><i className="fas fa-times-circle"></i></button></div>

               <div className="space-y-2">
                  <p className="text-[10px] text-slate-400 tracking-[3px] ml-4">SELECT MESSAGE TEMPLATE</p>
                  <select
                    value={selectedExpiryTemplate}
                    onChange={e => setSelectedExpiryTemplate(e.target.value)}
                    className="w-full bg-slate-50 dark:bg-slate-900 p-4 rounded-2xl border-none font-black text-xs shadow-inner outline-none cursor-pointer text-indigo-600"
                  >
                    {systemSmsTypes.filter(type => type === "Expiry Reminder (Today)" || type === "Expiry Reminder (Tomorrow)").map(type => (
                      <option key={type} value={type}>{type}</option>
                    ))}
                  </select>
               </div>

               <div className="bg-indigo-50 dark:bg-indigo-900/20 p-6 rounded-[32px] border-2 border-dashed border-indigo-200 mb-6 flex items-center justify-between group"><div className="flex items-center space-x-4"><div className="w-12 h-12 bg-indigo-600 text-white rounded-2xl flex items-center justify-center text-xl shadow-lg"><i className="fas fa-paper-plane"></i></div><div><h4 className="text-sm font-black text-slate-800 dark:text-white uppercase leading-none">Bulk SMS Reminder</h4><p className="text-[9px] font-bold text-slate-400 mt-1 uppercase tracking-widest">Notify all {expiringTomorrow.length} customers instantly</p></div></div><button onClick={async () => {
                        const template = store.smsTemplates?.find(t => t.title === selectedExpiryTemplate);
                        if (!template) return alert("Select template first!"); if (!window.confirm(`Send to ${expiringTomorrow.length}?`)) return;
                        const s = storeRef.current.settings || {};
                        const toBn = (n) => {
                            const isUni = store.smsTemplates?.find(t => t.title === selectedExpiryTemplate)?.messageContent?.match(/[\u0980-\u09FF]/);
                            if (!isUni) return n;
                            return n.toString().replace(/\d/g, d => "০১২৩৪৫৬৭৮৯"[d]);
                        };

                        for (const c of expiringTomorrow) {
                            const dueAmt = Math.floor(c.currentDue || c.current_due || 0);
                            let m = (template.messageContent || template.message_content)
                              .replace(/{NAME}/g, c.name || '')
                              .replace(/{DATE}/g, c.expireDate || c.expire_date || '')
                              .replace(/{AMOUNT}/g, toBn(dueAmt))
                              .replace(/{DUE}/g, toBn(dueAmt))
                              .replace(/{BALANCE}/g, toBn(dueAmt))
                              .replace(/{CUSTOMER_CODE}/g, c.customerCode || c.customer_code || '');

                            let ph = (c.mobile || "").replace(/[^0-9]/g, ""); if (ph.startsWith('0')) ph = '88'+ph; else if (ph.length===10) ph='880'+ph; else if (!ph.startsWith('88')) ph='88'+ph;
                            const isUni = /[\u0980-\u09FF]/.test(m);
                            new Image().src = `https://tglplinxvrqsrxeicvpr.supabase.co/functions/v1/sms-proxy?apikey=${s.smsApiKey}&callerID=${s.smsSenderId}&number=${ph}&message=${encodeURIComponent(m)}&type=${isUni?"unicode":"text"}`;
                            await supabase.from('sms_logs').insert({ customer_id: c.id, customer_name: c.name, mobile: ph, notification_type: selectedExpiryTemplate, message: m, status: 'Sent', sent_timestamp: new Date().toISOString() });
                        }
                        alert("Reminders Sent!");
                    }} className="bg-indigo-600 text-white px-6 py-3 rounded-xl font-black text-[10px] tracking-widest shadow-xl hover:scale-105 active:scale-95 transition-all">SEND ALL NOW</button></div>
               <div className="max-h-[400px] overflow-y-auto space-y-4 pr-4 custom-scrollbar">{expiringTomorrow.map(c => (
                 <div key={c.id} className="bg-slate-50 dark:bg-slate-900 p-6 rounded-[32px] border border-slate-100 flex justify-between items-center group transition-all shadow-sm">
                    <div onClick={() => { setQuickDateCust(c); setQuickDates({ expireDate: c.expireDate || c.expire_date || '', requestDate: c.requestDate || c.request_date || '' }); setShowQuickDateModal(true); }} className="space-y-1 cursor-pointer flex-1">
                       <p className="text-[10px] opacity-60 font-black tracking-widest">#{c.customerCode}</p>
                       <h4 className="text-xl font-black">{c.name} <span className="ml-3 text-base text-indigo-600 dark:text-indigo-400 opacity-100">{c.mobile?.startsWith('88') ? c.mobile.substring(2) : c.mobile}</span></h4>
                       <div className="flex items-center space-x-3 text-xs md:text-sm font-black uppercase tracking-wider mt-1"><span className="bg-indigo-100 dark:bg-indigo-900/30 text-indigo-600 px-3 py-1 rounded-xl shadow-sm border border-indigo-200/50">{c.zone || 'Global'}</span><span className="bg-rose-100 dark:bg-rose-900/30 text-rose-600 px-3 py-1 rounded-xl shadow-sm border border-rose-200/50">বকেয়া: ৳{Math.floor(c.currentDue || c.current_due || 0)}</span></div>
                    </div>
                    <div className="flex items-center space-x-4">
                       <button
                        onClick={async (e) => {
                          e.stopPropagation();
                          const template = store.smsTemplates?.find(t => t.title === selectedExpiryTemplate);
                          if (!template) return alert("Select template first!");

                          const dueAmt = Math.floor(c.currentDue || c.current_due || 0);
                          const toBn = (n) => {
                             const isUni = template.messageContent?.match(/[\u0980-\u09FF]/);
                             if (!isUni) return n;
                             return n.toString().replace(/\d/g, d => "০১২৩৪৫৬৭৮৯"[d]);
                          };

                          const m = (template.messageContent || template.message_content)
                            .replace(/{NAME}/g, c.name || '')
                            .replace(/{DATE}/g, c.expireDate || c.expire_date || '')
                            .replace(/{AMOUNT}/g, toBn(dueAmt))
                            .replace(/{DUE}/g, toBn(dueAmt))
                            .replace(/{BALANCE}/g, toBn(dueAmt))
                            .replace(/{CUSTOMER_CODE}/g, c.customerCode || c.customer_code || '');

                          if (await sendAndLogSms(c, m, selectedExpiryTemplate)) alert("SMS Sent to " + c.name);
                          else alert("Failed to send SMS!");
                        }}
                        className="w-12 h-12 bg-emerald-100 text-emerald-600 rounded-2xl flex items-center justify-center hover:bg-emerald-600 hover:text-white transition-all shadow-sm"
                        title="Send Selected Template"
                       >
                         <i className="fas fa-paper-plane"></i>
                       </button>
                       <div className="text-right">
                          <p className="text-[10px] opacity-60 font-black uppercase">Expire Date</p>
                          <p className="text-lg font-black text-rose-500">{c.expireDate || c.expire_date}</p>
                       </div>
                    </div>
                 </div>
               ))}</div>
               <button onClick={() => setShowExpiryModal(false)} className="w-full bg-slate-900 text-white py-6 rounded-3xl font-black uppercase tracking-[5px] shadow-2xl hover:bg-rose-600 transition-all">CLOSE PANEL</button>
            </div>
          </div>
        )}
        {showQuickDateModal && (
          <div className="fixed inset-0 bg-slate-900/95 backdrop-blur-2xl z-[10000] flex items-center justify-center p-6 uppercase font-black"><div className="bg-white dark:bg-slate-800 rounded-[56px] w-full max-w-md p-12 shadow-2xl space-y-8 relative border-4 border-teal-500/20"><div className="text-center space-y-2"><h3 className="text-3xl font-black tracking-tighter">Update Validity</h3><p className="text-[10px] text-slate-400 tracking-[3px]">Client: {quickDateCust?.name}</p></div><div className="space-y-6"><div className="space-y-2"><label className="text-[10px] text-slate-400 ml-4 tracking-[3px]">EXPIRE DATE</label><input type="date" value={quickDates.expireDate} onChange={e => setQuickDates({...quickDates, expireDate: e.target.value})} className="w-full bg-slate-50 dark:bg-slate-900 p-5 rounded-3xl font-black text-lg outline-none border-none shadow-inner" /></div><div className="space-y-2"><label className="text-[10px] text-slate-400 ml-4 tracking-[3px]">REQUEST DATE</label><input type="date" value={quickDates.requestDate} onChange={e => setQuickDates({...quickDates, requestDate: e.target.value})} className="w-full bg-slate-50 dark:bg-slate-900 p-5 rounded-3xl font-black text-lg outline-none border-none shadow-inner" /></div></div><div className="grid grid-cols-2 gap-4"><button onClick={() => setShowQuickDateModal(false)} className="bg-slate-100 py-5 rounded-3xl font-black text-slate-500">CANCEL</button><button onClick={handleQuickDateUpdate} className="bg-teal-600 text-white py-5 rounded-3xl font-black shadow-xl shadow-teal-500/20">SAVE CHANGE</button></div></div></div>
        )}
        {showGlobalSearch && (
          <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-3xl z-[10000] flex items-center justify-center p-4 md:p-10 uppercase font-black overflow-y-auto animate-fadeIn"><div className="bg-white dark:bg-slate-900 rounded-[48px] md:rounded-[80px] w-full max-w-3xl p-8 md:p-16 shadow-[0_40px_100px_rgba(0,0,0,0.5)] border-4 border-white/10 relative overflow-hidden animate-scaleIn"><div className={`absolute top-0 left-0 w-full h-3 md:h-4 ${searchMode === 'edit' ? 'bg-amber-500' : 'bg-indigo-600'} shadow-lg`}></div><div className="flex justify-between items-center border-b-2 border-slate-50 dark:border-slate-800 pb-6 md:pb-10"><div className="flex items-center space-x-4 md:space-x-6"><div className={`w-12 h-12 md:w-16 md:h-16 rounded-2xl md:rounded-3xl flex items-center justify-center text-xl md:text-3xl text-white shadow-2xl ${searchMode === 'edit' ? 'bg-amber-500' : 'bg-indigo-600'}`}><i className={`fas ${searchMode === 'edit' ? 'fa-user-pen' : 'fa-search'}`}></i></div><div><h3 className="text-3xl md:text-5xl font-black tracking-tighter leading-none">{searchMode === 'edit' ? 'EDIT SELECTOR' : 'QUICK SEARCH'}</h3><p className="text-[10px] md:text-xs font-bold text-slate-400 mt-2 tracking-[4px] opacity-70 uppercase">Master Subscriber Database</p></div></div><button onClick={() => { setShowGlobalSearch(false); setGlobalSearchQuery(''); }} className="w-12 h-12 md:w-16 md:h-16 bg-rose-50 text-rose-500 hover:bg-rose-500 hover:text-white rounded-full flex items-center justify-center transition-all shadow-xl group"><i className="fas fa-times text-xl md:text-2xl group-hover:rotate-90 transition-transform"></i></button></div><div className="mt-8 md:mt-14 space-y-8 md:space-y-12"><div className="relative group"><input autoFocus type="text" placeholder="NAME, ID, PHONE OR PPPOE..." value={globalSearchQuery} onChange={e => setGlobalSearchQuery(e.target.value)} className="w-full bg-slate-50 dark:bg-slate-950 p-8 md:p-12 rounded-[32px] md:rounded-[56px] font-black text-2xl md:text-4xl outline-none border-4 border-transparent focus:border-indigo-500/20 shadow-inner transition-all placeholder:opacity-20 text-indigo-600 dark:text-indigo-400" /><div className="absolute right-8 md:right-12 top-1/2 -translate-y-1/2 pointer-events-none opacity-20 group-focus-within:opacity-100 transition-opacity"><i className="fas fa-keyboard text-3xl md:text-5xl text-indigo-500"></i></div></div><div className="max-h-[300px] md:max-h-[450px] overflow-y-auto pr-2 md:pr-6 custom-scrollbar space-y-4 md:space-y-6">{globalSearchQuery.length > 0 ? (store.customers.filter(c => c.name?.toLowerCase().includes(globalSearchQuery.toLowerCase()) || c.customerCode?.toLowerCase().includes(globalSearchQuery.toLowerCase()) || c.mobile?.includes(globalSearchQuery) || c.pppoeUsername?.toLowerCase().includes(globalSearchQuery.toLowerCase())).slice(0, 10).map(c => (<div key={c.id} onClick={() => { if (searchMode === 'edit' && session?.role === 'admin') setPreSelectedCustomer(c); else { setSelectedProfileId(c.id); setActivePage('customer_profile'); } setShowGlobalSearch(false); setGlobalSearchQuery(''); }} className="bg-white dark:bg-slate-800 p-6 md:p-8 rounded-[32px] md:rounded-[48px] border-2 border-slate-100 dark:border-slate-800 hover:border-indigo-500 dark:hover:border-indigo-500 hover:bg-indigo-50/30 transition-all cursor-pointer flex justify-between items-center group shadow-sm hover:shadow-2xl hover:-translate-y-1"><div className="flex items-center space-x-4 md:space-x-8"><div className="w-14 h-14 md:w-20 md:h-20 bg-slate-50 dark:bg-slate-950 rounded-[20px] md:rounded-[28px] flex items-center justify-center text-2xl md:text-3xl text-slate-300 group-hover:text-indigo-600 transition-colors"><i className="fas fa-user"></i></div><div className="space-y-1"><h4 className="text-xl md:text-3xl font-black text-slate-800 dark:text-white leading-none tracking-tighter">{c.name}</h4><div className="flex items-center space-x-3 md:space-x-6 text-[9px] md:text-[11px] font-bold tracking-widest text-slate-400 mt-2"><span className="bg-slate-100 dark:bg-slate-950 px-2 py-1 rounded-lg">ID: {c.customerCode}</span><span className="uppercase">{c.pppoeUsername}</span></div></div></div><div className="flex flex-col items-end space-y-2 md:space-y-3"><p className="text-base md:text-xl font-black text-emerald-600 tracking-tighter leading-none">{c.mobile}</p><div className={`flex items-center space-x-2 md:space-x-3 text-[8px] md:text-[10px] font-black tracking-widest ${searchMode === 'edit' ? 'text-amber-600' : 'text-indigo-600'} opacity-0 group-hover:opacity-100 transition-opacity`}><span>{searchMode === 'edit' ? 'OPEN EDITOR' : 'VIEW PROFILE'}</span><i className="fas fa-arrow-right-long text-base"></i></div></div></div>))) : (<div className="py-20 text-center space-y-6 opacity-20"><i className="fas fa-database text-6xl md:text-8xl"></i><p className="text-xl md:text-3xl font-black tracking-[10px]">AWAITING INPUT</p></div>)}</div></div></div></div>
        )}
        {showSummarySearch && (
          <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-3xl z-[10000] flex items-center justify-center p-4 md:p-10 uppercase font-black overflow-y-auto animate-fadeIn"><div className="bg-white dark:bg-slate-900 rounded-[48px] md:rounded-[80px] w-full max-w-3xl p-8 md:p-16 shadow-[0_40px_100px_rgba(0,0,0,0.5)] border-4 border-white/10 relative overflow-hidden animate-scaleIn"><div className="absolute top-0 left-0 w-full h-3 md:h-4 bg-emerald-500 shadow-lg"></div><div className="flex justify-between items-center border-b-2 border-slate-50 dark:border-slate-800 pb-6 md:pb-10"><div className="flex items-center space-x-4 md:space-x-6"><div className="w-12 h-12 md:w-16 md:h-16 bg-emerald-500 text-white rounded-2xl md:rounded-3xl flex items-center justify-center text-xl md:text-3xl shadow-2xl"><i className="fas fa-chart-pie"></i></div><div><h3 className="text-3xl md:text-5xl font-black tracking-tighter leading-none">SUMMARY ANALYZER</h3><p className="text-[10px] md:text-xs font-bold text-slate-400 mt-2 tracking-[4px] opacity-70 uppercase">Financial Report Selector</p></div></div><button onClick={() => { setShowSummarySearch(false); setGlobalSearchQuery(''); }} className="w-12 h-12 md:w-16 md:h-16 bg-rose-50 text-rose-500 hover:bg-rose-500 hover:text-white rounded-full flex items-center justify-center transition-all shadow-xl group"><i className="fas fa-times text-xl md:text-2xl group-hover:rotate-90 transition-transform"></i></button></div><div className="mt-8 md:mt-14 space-y-8 md:space-y-12"><div className="relative group"><input autoFocus type="text" placeholder="SEARCH CUSTOMER FOR SUMMARY..." value={globalSearchQuery} onChange={e => setGlobalSearchQuery(e.target.value)} className="w-full bg-slate-50 dark:bg-slate-950 p-8 md:p-12 rounded-[32px] md:rounded-[56px] font-black text-2xl md:text-4xl outline-none border-4 border-transparent focus:border-emerald-500/20 shadow-inner transition-all placeholder:opacity-20 text-emerald-600 dark:text-emerald-400" /><div className="absolute right-8 md:right-12 top-1/2 -translate-y-1/2 pointer-events-none opacity-20 group-focus-within:opacity-100 transition-opacity"><i className="fas fa-keyboard text-3xl md:text-5xl text-emerald-500"></i></div></div><div className="max-h-[300px] md:max-h-[450px] overflow-y-auto pr-2 md:pr-6 custom-scrollbar space-y-4 md:space-y-6">{globalSearchQuery.length > 0 ? (store.customers.filter(c => (c.name || '').toLowerCase().includes(globalSearchQuery.toLowerCase()) || (c.customerCode || '').toLowerCase().includes(globalSearchQuery.toLowerCase()) || (c.mobile || '').includes(globalSearchQuery) || (c.pppoeUsername || '').toLowerCase().includes(globalSearchQuery.toLowerCase())).slice(0, 10).map(c => (<div key={c.id} onClick={() => { setSelectedSummaryId(c.id); setActivePage('billing_summary'); setShowSummarySearch(false); setGlobalSearchQuery(''); }} className="bg-white dark:bg-slate-800 p-6 md:p-8 rounded-[32px] md:rounded-[48px] border-2 border-slate-100 dark:border-slate-800 hover:border-emerald-500 dark:hover:border-emerald-500 hover:bg-emerald-50/30 transition-all cursor-pointer flex justify-between items-center group shadow-sm hover:shadow-2xl hover:-translate-y-1"><div className="flex items-center space-x-4 md:space-x-8"><div className="w-14 h-14 md:w-20 md:h-20 bg-slate-50 dark:bg-slate-950 rounded-[20px] md:rounded-[28px] flex items-center justify-center text-2xl md:text-3xl text-slate-300 group-hover:text-emerald-600 transition-colors"><i className="fas fa-file-invoice-dollar"></i></div><div className="space-y-1 text-left"><h4 className="text-xl md:text-3xl font-black text-slate-800 dark:text-white leading-none tracking-tighter">{c.name}</h4><p className="text-[9px] md:text-[11px] text-slate-400 font-bold tracking-widest uppercase mt-2">ID: {c.customerCode || c.customer_code} • Zone: {c.zone || 'Global'}</p></div></div><i className="fas fa-arrow-right-long text-emerald-600 text-xl md:text-3xl opacity-0 group-hover:opacity-100 transition-all"></i></div>))) : (<div className="py-20 text-center space-y-6 opacity-20"><i className="fas fa-search-dollar text-6xl md:text-8xl"></i><p className="text-xl md:text-3xl font-black tracking-[10px]">FIND REVENUE DATA</p></div>)}</div></div></div></div>
        )}
      </div>
    </div>
  );
}

export default App;
