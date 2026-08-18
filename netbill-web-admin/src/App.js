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
import BillingSummary from './components/BillingSummary';
import Settings from './components/Settings';
import Login from './components/Login';
import CustomerPortal from './components/CustomerPortal';
import CustomerFullProfile from './components/CustomerFullProfile';
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
    ledgerEntries: []
  });

  const [autoOpenAddModal, setAutoOpenAddModal] = useState(false);
  const [showExpiryModal, setShowExpiryModal] = useState(false);
  const [showQuickDateModal, setShowQuickDateModal] = useState(false);
  const [quickDateCust, setQuickDateCust] = useState(null);
  const [quickDates, setQuickDates] = useState({ expireDate: '', requestDate: '' });

  window.openExpiryModal = () => setShowExpiryModal(true);

  const t = translations[lang];

  const handleQuickDateUpdate = async () => {
    if (!quickDateCust) return;
    try {
      const { error } = await supabase
        .from('customers')
        .update({
          expire_date: quickDates.expireDate,
          request_date: quickDates.requestDate
        })
        .eq('id', quickDateCust.id);

      if (error) throw error;
      alert("Dates Updated Successfully!");
      setShowQuickDateModal(false);
    } catch (e) {
      alert("Update Failed!");
    }
  };

  const expiringTomorrow = React.useMemo(() => {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);

    // Check multiple formats (YYYY-MM-DD and DD-MMM-YYYY)
    const tomorrowISO = tomorrow.toLocaleDateString('en-CA'); // 2026-08-20
    const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
    const tomorrowCustom = `${tomorrow.getDate().toString().padStart(2, '0')}-${months[tomorrow.getMonth()]}-${tomorrow.getFullYear()}`; // 20-Aug-2026

    return store.customers.filter(c => {
      const eDate = c.expireDate || c.expire_date;
      const isDue = (parseFloat(c.currentDue || c.current_due || 0) > 0); // Check if they have unpaid bill

      if (!eDate || c.status !== 'Active' || !isDue) return false;
      return eDate === tomorrowISO || eDate === tomorrowCustom;
    });
  }, [store.customers]);

  useEffect(() => {
    if (isDarkMode) document.body.classList.add('dark-mode');
    else document.body.classList.remove('dark-mode');
  }, [isDarkMode]);

  // FULL CLOUD SYNC ENGINE
  useEffect(() => {
    if (!session) return;

    const tableMapping = {
      'customers': 'customers',
      'support_tickets': 'tickets',
      'payments': 'payments',
      'payment_requests': 'paymentRequests',
      'invoices': 'invoices',
      'expenses': 'expenses',
      'staff': 'staff',
      'staff_payouts': 'staffPayouts',
      'zones': 'zones',
      'sub_zones': 'subZones',
      'boxes': 'boxes',
      'expense_categories': 'expenseCategories',
      'packages': 'packages',
      'inventory_items': 'inventory',
      'sms_templates': 'smsTemplates',
      'sms_logs': 'smsLogs',
      'ledger_entries': 'ledgerEntries'
    };

    const fetchData = async () => {
      const newStore = { ...store };
      try {
        for (const [table, storeKey] of Object.entries(tableMapping)) {
          const { data, error } = await supabase.from(table).select('*');
          if (!error && data) {
            newStore[storeKey] = data.map(mapToCamelCase);
          } else if (error) {
            console.error(`Error loading ${table}:`, error.message);
          }
        }

        const { data: settingsData } = await supabase.from('settings').select('*').limit(1).maybeSingle();
        if (settingsData) newStore.settings = mapToCamelCase(settingsData);

        setStore(prev => ({ ...prev, ...newStore }));
      } catch (e) {
        console.error("Global fetch error:", e);
      }
    };

    fetchData();

    // Subscribe to Real-time Changes
    const channels = Object.keys(tableMapping).map(table => {
      return supabase.channel(`${table}-changes`)
        .on('postgres_changes', { event: '*', schema: 'public', table: table }, (payload) => {
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
        })
        .subscribe();
    });

    return () => { channels.forEach(channel => supabase.removeChannel(channel)); };
  }, [session]);

  const mapToCamelCase = (obj) => {
    if (!obj) return obj;
    const newObj = {};
    Object.keys(obj).forEach(key => {
      const camelKey = key.replace(/(_\w)/g, m => m[1].toUpperCase());
      newObj[camelKey] = obj[key];
    });
    return newObj;
  };

  const handleLoginSuccess = (sessionData) => {
    setSession(sessionData);
    localStorage.setItem('netbill_session', JSON.stringify(sessionData));
  };

  const handleLogout = () => {
    setSession(null);
    localStorage.removeItem('netbill_session');
    setActivePage('dashboard');
  };

  const toggleLang = () => {
    const newLang = lang === 'en' ? 'bn' : 'en';
    setLang(newLang);
    localStorage.setItem('app_lang', newLang);
  };

  const toggleDarkMode = () => {
    const newMode = !isDarkMode;
    setIsDarkMode(newMode);
    localStorage.setItem('dark_mode', newMode.toString());
  };

  const navigateToAddCustomer = () => {
    setIsDirectAddMode(true);
    setActivePage('new_enrollment');
  };

  if (!session) return <Login onLoginSuccess={handleLoginSuccess} />;

  return (
    <div className={`flex h-screen overflow-hidden transition-colors duration-300 ${isDarkMode ? 'dark-mode bg-slate-900 text-white' : 'bg-slate-50 text-slate-800'}`}>
      <Sidebar isSidebarOpen={isSidebarOpen} setIsSidebarOpen={setIsSidebarOpen} activePage={activePage} setActivePage={setActivePage} onLogout={handleLogout} t={t} role={session.role} />

      <div className={`flex-1 flex flex-col overflow-hidden transition-all duration-300`}>
        <header className={`h-16 md:h-20 border-b flex justify-between items-center px-4 md:px-10 shrink-0 z-10 transition-colors ${isDarkMode ? 'bg-slate-800 border-slate-700 text-white' : 'bg-white border-slate-100 text-slate-800'}`}>
          <div className="flex items-center space-x-2 md:space-x-6">
            <button onClick={() => setIsSidebarOpen(!isSidebarOpen)} className="w-10 h-10 md:w-12 md:h-12 bg-slate-50 dark:bg-slate-900 rounded-xl md:rounded-2xl flex items-center justify-center text-teal-600 shadow-sm"><i className={`fas ${isSidebarOpen ? 'fa-indent' : 'fa-outdent'} text-lg md:text-xl`}></i></button>
            <h2 className="text-sm md:text-xl font-black uppercase tracking-tighter leading-none whitespace-nowrap">NetBill <span className="hidden sm:inline">ISP | </span><span className="text-teal-600 truncate max-w-[100px] sm:max-w-none inline-block">{activePage}</span></h2>
          </div>
          <div className="flex items-center space-x-3 md:space-x-10">
            <button onClick={toggleLang} className="flex items-center space-x-1 bg-slate-50 dark:bg-slate-900 px-2 md:px-4 py-1.5 md:py-2 rounded-xl md:rounded-2xl border border-slate-100 dark:border-slate-700">
               <i className="fas fa-globe text-teal-600 text-xs md:text-sm"></i>
               <span className="text-[8px] md:text-[10px] font-black uppercase tracking-widest">{lang === 'en' ? 'বাংলা' : 'EN'}</span>
            </button>
            <button onClick={toggleDarkMode} className="text-teal-600 text-xl md:text-2xl transition-transform"><i className={`fas ${isDarkMode ? 'fa-sun text-amber-400' : 'fa-moon'}`}></i></button>
            <div className="flex items-center space-x-2 md:space-x-4">
              <img src={`https://ui-avatars.com/api/?name=${session.data.name}&background=0D9488&color=fff&bold=true`} className="w-8 h-8 md:w-10 md:h-10 rounded-lg md:rounded-xl" alt="Admin" />
              <div className="hidden sm:block text-left leading-none space-y-1">
                <p className="text-xs font-black uppercase text-slate-900 dark:text-white">{session.data.name}</p>
                <p className="text-[9px] text-teal-600 font-bold uppercase tracking-widest italic">{session.role === 'admin' ? t.super_admin : 'Field Staff'}</p>
              </div>
            </div>
          </div>
        </header>

        <main className="flex-1 overflow-y-auto p-4 md:p-10 scroll-smooth transition-colors font-black">
          {activePage === 'dashboard' && <Dashboard store={store} session={session} setActivePage={setActivePage} setReportInitialTab={setReportInitialTab} navigateToAddCustomer={navigateToAddCustomer} openSearch={() => setShowGlobalSearch(true)} openSummary={() => setShowSummarySearch(true)} t={t} lang={lang} />}
          {activePage === 'customers' && <Customers store={store} setActivePage={setActivePage} t={t} lang={lang} autoOpenModal={autoOpenAddModal} setAutoOpenModal={setAutoOpenAddModal} setProfileId={(id) => { setSelectedProfileId(id); setActivePage('customer_profile'); }} />}
          {activePage === 'customer_profile' && <CustomerFullProfile store={store} customerId={selectedProfileId} onBack={() => setActivePage('customers')} t={t} />}
          {activePage === 'new_enrollment' && <Customers store={store} setActivePage={setActivePage} t={t} lang={lang} autoOpenModal={true} isDirectMode={true} setProfileId={(id) => { setSelectedProfileId(id); setActivePage('customer_profile'); }} />}
          {activePage === 'billing' && <Billing store={store} t={t} lang={lang} />}
          {activePage === 'payments' && <Payments store={store} session={session} t={t} lang={lang} />}
          {activePage === 'reports' && <CollectionReport store={store} session={session} t={t} initialTab={reportInitialTab} />}
          {activePage === 'expenses' && <Expenses store={store} session={session} t={t} />}
          {activePage === 'staff' && session.role === 'admin' && <Staff store={store} session={session} t={t} lang={lang} />}
          {activePage === 'salary_history' && <SalaryHistory store={store} session={session} t={t} lang={lang} />}
          {activePage === 'inventory' && <Inventory store={store} t={t} lang={lang} />}
          {activePage === 'infrastructure' && session.role === 'admin' && <Infrastructure store={store} t={t} />}
          {activePage === 'packages' && <Packages store={store} t={t} lang={lang} />}
          {activePage === 'sms_setup' && <SmsSetup store={store} t={t} />}
          {activePage === 'sms_logs' && <SmsLogs store={store} />}
          {activePage === 'crm_tickets' && <SupportTickets store={store} session={session} t={t} />}
          {activePage === 'billing_summary' && <BillingSummary store={store} initialCustomerId={selectedSummaryId} />}
          {activePage === 'settings' && session.role === 'admin' && <Settings store={store} t={t} lang={lang} />}
        </main>

        {/* FLOATING NOTIFICATION ICON */}
        <div
          onClick={() => setShowExpiryModal(true)}
          className={`fixed bottom-10 right-10 w-16 h-16 rounded-full bg-rose-600 text-white flex items-center justify-center shadow-2xl cursor-pointer hover:scale-110 active:scale-95 transition-all z-[100] animate-bounce ${expiringTomorrow.length > 0 ? 'flex' : 'hidden'}`}
        >
           <i className="fas fa-bell text-2xl"></i>
           <span className="absolute -top-1 -right-1 bg-white text-rose-600 w-6 h-6 rounded-full flex items-center justify-center text-[10px] font-black border-2 border-rose-600 shadow-sm">{expiringTomorrow.length}</span>
        </div>

        {/* EXPIRY ALERT MODAL */}
        {showExpiryModal && (
          <div className="fixed inset-0 bg-slate-900/90 backdrop-blur-xl z-[9999] flex items-center justify-center p-6 uppercase font-black">
            <div className="bg-white dark:bg-slate-800 rounded-[56px] w-full max-w-2xl p-12 shadow-2xl border-4 border-rose-500/20 space-y-8 relative overflow-hidden">
               <div className="absolute top-0 left-0 w-full h-3 bg-rose-600"></div>
               <div className="flex justify-between items-center border-b pb-6">
                  <div>
                    <h3 className="text-4xl font-black text-slate-800 dark:text-white tracking-tighter">Expiry Alerts</h3>
                    <p className="text-[10px] text-slate-400 font-bold mt-2 tracking-[3px]">Total {expiringTomorrow.length} Customers Expiring Tomorrow</p>
                  </div>
                  <button onClick={() => setShowExpiryModal(false)} className="text-rose-500 text-3xl hover:scale-110 transition-all"><i className="fas fa-times-circle"></i></button>
               </div>

               <div className="max-h-[400px] overflow-y-auto space-y-4 pr-4 custom-scrollbar">
                  {expiringTomorrow.map(c => (
                    <div
                      key={c.id}
                      onClick={() => {
                        setQuickDateCust(c);
                        setQuickDates({ expireDate: c.expireDate || c.expire_date || '', requestDate: c.requestDate || c.request_date || '' });
                        setShowQuickDateModal(true);
                      }}
                      className="bg-slate-50 dark:bg-slate-900 p-6 rounded-[32px] border border-slate-100 flex justify-between items-center group hover:bg-indigo-600 hover:text-white cursor-pointer transition-all shadow-sm"
                    >
                       <div className="space-y-1">
                          <p className="text-[10px] opacity-60 font-black tracking-widest">#{c.customerCode}</p>
                          <h4 className="text-xl font-black">{c.name}</h4>
                          <div className="flex items-center space-x-3 text-xs md:text-sm font-black uppercase tracking-wider mt-1">
                             <span className="bg-indigo-100 dark:bg-indigo-900/30 text-indigo-600 px-3 py-1 rounded-xl shadow-sm border border-indigo-200/50">{c.zone || 'Global'}</span>
                             <span className="bg-rose-100 dark:bg-rose-900/30 text-rose-600 px-3 py-1 rounded-xl shadow-sm border border-rose-200/50">বকেয়া: ৳{Math.floor(c.currentDue || c.current_due || 0)}</span>
                          </div>
                       </div>
                       <div className="text-right">
                          <p className="text-[10px] opacity-60 font-black uppercase">Expire Date</p>
                          <p className="text-lg font-black text-rose-500 group-hover:text-white">{c.expireDate || c.expire_date}</p>
                       </div>
                    </div>
                  ))}
               </div>

               <button
                  onClick={() => setShowExpiryModal(false)}
                  className="w-full bg-slate-900 text-white py-6 rounded-3xl font-black uppercase tracking-[5px] shadow-2xl hover:bg-rose-600 transition-all"
                >
                  CLOSE PANEL
               </button>
            </div>
          </div>
        )}

        {/* QUICK DATE UPDATE MODAL */}
        {showQuickDateModal && (
          <div className="fixed inset-0 bg-slate-900/95 backdrop-blur-2xl z-[10000] flex items-center justify-center p-6 uppercase font-black">
            <div className="bg-white dark:bg-slate-800 rounded-[56px] w-full max-w-md p-12 shadow-2xl space-y-8 relative border-4 border-teal-500/20">
               <div className="text-center space-y-2">
                  <h3 className="text-3xl font-black tracking-tighter">Update Validity</h3>
                  <p className="text-[10px] text-slate-400 tracking-[3px]">Client: {quickDateCust?.name}</p>
               </div>

               <div className="space-y-6">
                  <div className="space-y-2">
                    <label className="text-[10px] text-slate-400 ml-4 tracking-[3px]">EXPIRE DATE</label>
                    <input
                      type="date"
                      value={quickDates.expireDate}
                      onChange={e => setQuickDates({...quickDates, expireDate: e.target.value})}
                      className="w-full bg-slate-50 dark:bg-slate-900 p-5 rounded-3xl font-black text-lg outline-none border-none shadow-inner"
                    />
                  </div>
                  <div className="space-y-2">
                    <label className="text-[10px] text-slate-400 ml-4 tracking-[3px]">REQUEST DATE</label>
                    <input
                      type="date"
                      value={quickDates.requestDate}
                      onChange={e => setQuickDates({...quickDates, requestDate: e.target.value})}
                      className="w-full bg-slate-50 dark:bg-slate-900 p-5 rounded-3xl font-black text-lg outline-none border-none shadow-inner"
                    />
                  </div>
               </div>

               <div className="grid grid-cols-2 gap-4">
                  <button onClick={() => setShowQuickDateModal(false)} className="bg-slate-100 py-5 rounded-3xl font-black text-slate-500">CANCEL</button>
                  <button onClick={handleQuickDateUpdate} className="bg-teal-600 text-white py-5 rounded-3xl font-black shadow-xl shadow-teal-500/20">SAVE CHANGE</button>
               </div>
            </div>
          </div>
        )}

        {/* GLOBAL SEARCH MODAL */}
        {showGlobalSearch && (
          <div className="fixed inset-0 bg-slate-900/95 backdrop-blur-3xl z-[10000] flex items-center justify-center p-6 uppercase font-black">
            <div className="bg-white dark:bg-slate-800 rounded-[64px] w-full max-w-2xl p-12 shadow-2xl border-4 border-indigo-500/20 space-y-10 relative overflow-hidden">
               <div className="absolute top-0 left-0 w-full h-3 bg-indigo-600"></div>
               <div className="flex justify-between items-center border-b pb-6">
                  <h3 className="text-4xl font-black tracking-tighter">Subscriber Search</h3>
                  <button onClick={() => { setShowGlobalSearch(false); setGlobalSearchQuery(''); }} className="text-rose-500 text-3xl hover:scale-110 transition-all"><i className="fas fa-times-circle"></i></button>
               </div>

               <div className="space-y-6">
                  <div className="relative group">
                    <input
                      autoFocus
                      type="text"
                      placeholder="Type Name, ID, Phone or PPPoE..."
                      value={globalSearchQuery}
                      onChange={e => setGlobalSearchQuery(e.target.value)}
                      className="w-full bg-slate-50 dark:bg-slate-900 p-8 rounded-[36px] font-black text-2xl outline-none border-2 border-transparent focus:border-indigo-500 shadow-inner"
                    />
                    <i className="fas fa-search absolute right-8 top-1/2 -translate-y-1/2 text-slate-300 text-3xl"></i>
                  </div>

                  <div className="max-h-[350px] overflow-y-auto pr-4 custom-scrollbar space-y-4">
                    {globalSearchQuery.length > 0 && store.customers.filter(c =>
                      c.name?.toLowerCase().includes(globalSearchQuery.toLowerCase()) ||
                      c.customerCode?.toLowerCase().includes(globalSearchQuery.toLowerCase()) ||
                      c.mobile?.includes(globalSearchQuery) ||
                      c.pppoeUsername?.toLowerCase().includes(globalSearchQuery.toLowerCase())
                    ).slice(0, 8).map(c => (
                      <div
                        key={c.id}
                        onClick={() => {
                          setSelectedProfileId(c.id);
                          setActivePage('customer_profile');
                          setShowGlobalSearch(false);
                          setGlobalSearchQuery('');
                        }}
                        className="bg-white dark:bg-slate-900/50 p-6 rounded-[32px] border-2 border-slate-100 hover:border-indigo-500 hover:bg-indigo-50 transition-all cursor-pointer flex justify-between items-center group"
                      >
                         <div className="space-y-1">
                            <h4 className="text-xl font-black text-slate-800 dark:text-white leading-none">{c.name}</h4>
                            <p className="text-[10px] text-slate-400 font-bold tracking-widest uppercase">ID: {c.customerCode} • PPPOE: {c.pppoeUsername}</p>
                         </div>
                         <div className="text-right">
                            <p className="text-xs font-black text-emerald-600">{c.mobile}</p>
                            <span className="text-[9px] font-black text-slate-300 uppercase group-hover:text-indigo-600">Open Profile <i className="fas fa-arrow-right ml-1"></i></span>
                         </div>
                      </div>
                    ))}
                  </div>
               </div>
            </div>
          </div>
        )}

        {/* SUMMARY SEARCH MODAL */}
        {showSummarySearch && (
          <div className="fixed inset-0 bg-slate-900/95 backdrop-blur-3xl z-[10000] flex items-center justify-center p-6 uppercase font-black">
            <div className="bg-white dark:bg-slate-800 rounded-[64px] w-full max-w-2xl p-12 shadow-2xl border-4 border-emerald-500/20 space-y-10 relative overflow-hidden">
               <div className="absolute top-0 left-0 w-full h-3 bg-emerald-600"></div>
               <div className="flex justify-between items-center border-b pb-6">
                  <h3 className="text-4xl font-black tracking-tighter">Billing Summary Search</h3>
                  <button onClick={() => { setShowSummarySearch(false); setGlobalSearchQuery(''); }} className="text-rose-500 text-3xl hover:scale-110 transition-all"><i className="fas fa-times-circle"></i></button>
               </div>
               <div className="space-y-6">
                  <div className="relative group">
                    <input autoFocus type="text" placeholder="Search Customer for Summary..." value={globalSearchQuery} onChange={e => setGlobalSearchQuery(e.target.value)} className="w-full bg-slate-50 dark:bg-slate-900 p-8 rounded-[36px] font-black text-2xl outline-none border-2 border-transparent focus:border-emerald-500 shadow-inner" />
                    <i className="fas fa-chart-pie absolute right-8 top-1/2 -translate-y-1/2 text-slate-300 text-3xl"></i>
                  </div>
                  <div className="max-h-[350px] overflow-y-auto pr-4 custom-scrollbar space-y-4">
                    {globalSearchQuery.length > 0 && store.customers.filter(c => {
                      const q = globalSearchQuery.toLowerCase();
                      return (c.name || '').toLowerCase().includes(q) ||
                             (c.customerCode || '').toLowerCase().includes(q) ||
                             (c.customer_code || '').toLowerCase().includes(q) ||
                             (c.mobile || '').includes(q) ||
                             (c.pppoeUsername || '').toLowerCase().includes(q);
                    }).slice(0, 8).map(c => (
                      <div key={c.id} onClick={() => { setSelectedSummaryId(c.id); setActivePage('billing_summary'); setShowSummarySearch(false); setGlobalSearchQuery(''); }} className="bg-white dark:bg-slate-900/50 p-6 rounded-[32px] border-2 border-slate-100 hover:border-emerald-500 hover:bg-emerald-50 transition-all cursor-pointer flex justify-between items-center group">
                         <div className="space-y-1 text-left">
                            <h4 className="text-xl font-black text-slate-800 dark:text-white leading-none">{c.name}</h4>
                            <p className="text-[10px] text-slate-400 font-bold tracking-widest uppercase">ID: {c.customerCode || c.customer_code} • Zone: {c.zone || 'Global'}</p>
                         </div>
                         <i className="fas fa-arrow-right text-emerald-600 text-xl"></i>
                      </div>
                    ))}
                  </div>
               </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

export default App;
