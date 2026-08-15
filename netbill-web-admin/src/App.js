import React, { useState, useEffect } from 'react';
import { db } from './firebaseConfig';
import { supabase } from './supabaseClient';
import { collection, onSnapshot, doc, updateDoc } from 'firebase/firestore';
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
  const [selectedProfileId, setSelectedProfileId] = useState(null);
  const [isDarkMode, setIsDarkMode] = useState(localStorage.getItem('dark_mode') === 'true');
  const [lang, setLang] = useState(localStorage.getItem('app_lang') || 'bn');
  const [isSidebarOpen, setIsSidebarOpen] = useState(window.innerWidth > 1024);

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
    zones: [], subZones: [], boxes: [], expenseCategories: []
  });
  const [autoOpenAddModal, setAutoOpenAddModal] = useState(false);
  const [showExpiryModal, setShowExpiryModal] = useState(false);

  // Quick Edit States in Global Scope
  const [showQuickDateModal, setShowQuickDateModal] = useState(false);
  const [quickDateCust, setQuickDateCust] = useState(null);
  const [quickDates, setQuickDates] = useState({ expireDate: '', requestDate: '' });

  // Expose modal control globally for Dashboard
  window.openExpiryModal = () => setShowExpiryModal(true);

  const t = translations[lang];

  // Global Expiry Logic
  const expiringTomorrow = React.useMemo(() => {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const tomorrowStr = tomorrow.toLocaleDateString('en-CA');
    return store.customers.filter(c => c.expireDate === tomorrowStr && c.status === 'Active');
  }, [store.customers]);

  const formatDateDisplay = (dateStr) => {
    if (!dateStr) return '';
    if (dateStr.includes('-')) {
       const [year, month, day] = dateStr.split('-');
       return `${day}-${month}-${year}`;
    }
    return dateStr;
  };

  useEffect(() => {
    if (isDarkMode) document.body.classList.add('dark-mode');
    else document.body.classList.remove('dark-mode');
  }, [isDarkMode]);

  // Real-time Cloud Sync with Supabase
  useEffect(() => {
    if (!session) return;

    const fetchData = async () => {
      const tables = [
        'customers', 'support_tickets', 'payments', 'payment_requests',
        'invoices', 'expenses', 'staff', 'staff_payouts',
        'zones', 'sub_zones', 'boxes', 'expense_categories', 'packages', 'inventory_items'
      ];

      for (const table of tables) {
        const { data, error } = await supabase.from(table).select('*');
        if (!error) {
          // Map snake_case to camelCase for UI compatibility
          const mappedData = data.map(item => ({
            ...item,
            customerCode: item.customer_code,
            altMobile: item.alt_mobile,
            packageName: item.package_name,
            monthlyBill: item.monthly_bill,
            currentDue: item.current_due,
            advanceBalance: item.advance_balance,
            pppoeUsername: item.pppoe_username,
            pppoePassword: item.pppoe_password,
            onuMac: item.onu_mac,
            routerId: item.router_id,
            billingType: item.billing_type,
            paymentStatus: item.payment_status,
            expireDate: item.expire_date,
            requestDate: item.request_date,
            connectionType: item.connection_type,
            subscriptionType: item.subscription_type,
            connectionFee: item.connection_fee,
            joinDate: item.join_date,
            assignedStaffId: item.assigned_staff_id,
            referenceName: item.reference_name,
            referenceMobile: item.reference_mobile,
            expenseDate: item.expense_date,
            expenseBy: item.expense_by,
            receiptNo: item.receipt_no,
            paymentDate: item.payment_date,
            billingMonth: item.billing_month,
            collectedBy: item.collected_by,
            collectedById: item.collected_by_id
          }));

          setStore(prev => ({
            ...prev,
            [table === 'support_tickets' ? 'tickets' : (table === 'inventory_items' ? 'inventory' : table)]: mappedData
          }));
        }
      }

      // Special handling for settings - use maybeSingle to avoid error if table is empty
      const { data: settingsData } = await supabase.from('settings').select('*').limit(1).maybeSingle();
      if (settingsData) setStore(prev => ({ ...prev, settings: settingsData }));
    };

    fetchData();

    // Subscribe to changes for all tables
    const channels = [
      'customers', 'support_tickets', 'payments', 'payment_requests',
      'invoices', 'expenses', 'staff', 'staff_payouts',
      'zones', 'sub_zones', 'boxes', 'expense_categories'
    ].map(table => {
      return supabase.channel(`${table}-changes`)
        .on('postgres_changes', { event: '*', schema: 'public', table: table }, (payload) => {
          fetchData(); // Simplest way to keep store in sync for now
        })
        .subscribe();
    });

    return () => {
      channels.forEach(channel => supabase.removeChannel(channel));
    };
  }, [session]);

  const toggleLang = () => {
    const newLang = lang === 'en' ? 'bn' : 'en';
    setLang(newLang);
    localStorage.setItem('app_lang', newLang);
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

  const toggleDarkMode = () => {
    const newMode = !isDarkMode;
    setIsDarkMode(newMode);
    localStorage.setItem('dark_mode', newMode.toString());
  };

  const navigateToAddCustomer = () => {
    setAutoOpenAddModal(true);
    setActivePage('customers');
  };

  const handleGlobalQuickDateUpdate = async () => {
    if (!quickDateCust) return;
    try {
      await updateDoc(doc(db, "customers", quickDateCust.id), quickDates);
      alert("Dates Synchronized Successfully!");
      setShowQuickDateModal(false);
    } catch (e) {
      alert("Cloud sync failed.");
    }
  };

  if (!session) return <Login onLoginSuccess={handleLoginSuccess} />;

  // CUSTOMER VIEW
  if (session.role === 'customer') {
     const currentCust = store.customers.find(c => c.id === session.data.id) || session.data;
     return (
       <div className={`min-h-screen p-10 transition-colors duration-300 ${isDarkMode ? 'bg-slate-900 text-white' : 'bg-slate-50 text-slate-800'}`}>
          <header className="flex justify-between items-center mb-12">
             <h2 className="text-3xl font-black uppercase tracking-tighter">NetBill <span className="text-teal-600">ISP</span></h2>
             <div className="flex items-center space-x-6">
                <button onClick={toggleDarkMode} className="text-teal-600 text-2xl"><i className={`fas ${isDarkMode ? 'fa-sun' : 'fa-moon'}`}></i></button>
                <button onClick={toggleLang} className="bg-white px-4 py-2 rounded-xl shadow-sm text-[10px] font-black">{lang === 'en' ? 'বাংলা' : 'English'}</button>
             </div>
          </header>
          <CustomerPortal customer={currentCust} store={store} t={t} onLogout={handleLogout} />
       </div>
     );
  }

  // ADMIN / STAFF VIEW
  return (
    <div className={`flex h-screen overflow-hidden transition-colors duration-300 ${isDarkMode ? 'dark-mode bg-slate-900 text-white' : 'bg-slate-50 text-slate-800'}`}>
      <Sidebar isSidebarOpen={isSidebarOpen} setIsSidebarOpen={setIsSidebarOpen} activePage={activePage} setActivePage={setActivePage} onLogout={handleLogout} t={t} role={session.role} />

      <div className={`flex-1 flex flex-col overflow-hidden transition-all duration-300`}>
        <header className={`h-20 border-b flex justify-between items-center px-10 shrink-0 z-10 transition-colors ${isDarkMode ? 'bg-slate-800 border-slate-700 text-white' : 'bg-white border-slate-100 text-slate-800'}`}>
          <div className="flex items-center space-x-6">
            <button onClick={() => setIsSidebarOpen(!isSidebarOpen)} className="w-12 h-12 bg-slate-50 dark:bg-slate-900 rounded-2xl flex items-center justify-center text-teal-600 hover:scale-110 transition-all shadow-sm">
               <i className={`fas ${isSidebarOpen ? 'fa-indent' : 'fa-outdent'} text-xl`}></i>
            </button>
            <h2 className="text-xl font-black uppercase tracking-tighter leading-none">
              NetBill ISP | <span className="text-teal-600">{activePage}</span>
            </h2>
          </div>
          <div className="flex items-center space-x-10">
            <button onClick={toggleLang} className="flex items-center space-x-2 bg-slate-50 dark:bg-slate-900 px-4 py-2 rounded-2xl border border-slate-100 dark:border-slate-700 hover:scale-105 transition-all">
               <i className="fas fa-globe text-teal-600"></i>
               <span className="text-[10px] font-black uppercase tracking-widest">{lang === 'en' ? 'বাংলা' : 'ENGLISH'}</span>
            </button>
            <button onClick={toggleDarkMode} className="text-teal-600 text-2xl hover:scale-110 transition-transform">
              <i className={`fas ${isDarkMode ? 'fa-sun text-amber-400' : 'fa-moon'}`}></i>
            </button>
            <div className="flex items-center space-x-4">
              <img src={`https://ui-avatars.com/api/?name=${session.data.name}&background=0D9488&color=fff&bold=true`} className="w-10 h-10 rounded-xl border-2 border-slate-50 shadow-sm" alt="Admin" />
              <div className="text-left leading-none space-y-1">
                <p className="text-xs font-black uppercase text-slate-900 dark:text-white leading-none">{session.data.name}</p>
                <p className="text-[9px] text-teal-600 font-bold uppercase tracking-widest italic leading-none">{session.role === 'admin' ? t.super_admin : 'Field Staff'}</p>
              </div>
            </div>
          </div>
        </header>

        <main className="flex-1 overflow-y-auto p-10 scroll-smooth transition-colors font-black">
          {activePage === 'dashboard' && <Dashboard store={store} session={session} setActivePage={setActivePage} navigateToAddCustomer={navigateToAddCustomer} t={t} lang={lang} />}
          {activePage === 'customers' && <Customers store={store} setActivePage={setActivePage} t={t} lang={lang} autoOpenModal={autoOpenAddModal} setAutoOpenModal={setAutoOpenAddModal} setProfileId={(id) => { setSelectedProfileId(id); setActivePage('customer_profile'); }} />}
          {activePage === 'customer_profile' && <CustomerFullProfile store={store} customerId={selectedProfileId} onBack={() => setActivePage('customers')} t={t} />}
          {activePage === 'billing' && <Billing store={store} t={t} lang={lang} />}
          {activePage === 'payments' && <Payments store={store} session={session} t={t} lang={lang} />}
          {activePage === 'reports' && <CollectionReport store={store} session={session} t={t} />}
          {activePage === 'expenses' && <Expenses store={store} session={session} t={t} />}
          {activePage === 'staff' && session.role === 'admin' && <Staff store={store} session={session} t={t} lang={lang} />}
          {activePage === 'salary_history' && <SalaryHistory store={store} session={session} t={t} lang={lang} />}
          {activePage === 'inventory' && <Inventory store={store} t={t} lang={lang} />}
          {activePage === 'infrastructure' && session.role === 'admin' && <Infrastructure store={store} t={t} />}
          {activePage === 'packages' && <Packages store={store} t={t} lang={lang} />}
          {activePage === 'settings' && session.role === 'admin' && <Settings store={store} t={t} lang={lang} />}
        </main>
      </div>

      {/* GLOBAL FLOATING EXPIRY ICON */}
      {expiringTomorrow.length > 0 && (
        <div className="fixed bottom-10 right-10 z-[1000] animate-bounce">
           <button
             onClick={() => setShowExpiryModal(true)}
             className="w-16 h-16 md:w-20 md:h-20 bg-rose-600 text-white rounded-full shadow-2xl flex items-center justify-center text-2xl md:text-3xl relative border-4 border-white dark:border-slate-800 transition-transform hover:scale-110 active:scale-95"
           >
              <i className="fas fa-bell"></i>
              <span className="absolute -top-2 -right-2 bg-white text-rose-600 w-7 h-7 md:w-8 md:h-8 rounded-full flex items-center justify-center text-[10px] md:text-xs font-black border-2 border-rose-600 shadow-lg">{expiringTomorrow.length}</span>
           </button>
        </div>
      )}

      {/* GLOBAL EXPIRY ALERT MODAL */}
      {showExpiryModal && (
        <div className="fixed inset-0 bg-slate-900/90 backdrop-blur-2xl z-[10000] flex items-center justify-center p-4 md:p-6 animate-fadeIn font-black uppercase">
           <div className="bg-white dark:bg-slate-800 rounded-[48px] md:rounded-[56px] w-full max-w-2xl p-8 md:p-12 shadow-2xl border-4 border-rose-500/20 space-y-6 md:space-y-8 relative overflow-hidden">
              <div className="absolute top-0 left-0 w-full h-3 md:h-4 bg-rose-600"></div>

              <div className="flex justify-between items-center border-b-2 border-slate-50 dark:border-slate-700 pb-6">
                 <div className="flex items-center space-x-4">
                    <div className="w-12 h-12 md:w-14 md:h-14 bg-rose-600 text-white rounded-2xl flex items-center justify-center text-xl md:text-2xl shadow-lg animate-pulse"><i className="fas fa-bell"></i></div>
                    <div>
                       <h3 className="text-2xl md:text-4xl font-black uppercase tracking-tighter leading-none text-slate-800 dark:text-white">Expiry Alert</h3>
                       <p className="text-[9px] md:text-[10px] text-rose-500 font-bold tracking-[2px] mt-1">Customers Expiring Tomorrow</p>
                    </div>
                 </div>
                 <button onClick={() => setShowExpiryModal(false)} className="w-10 h-10 md:w-12 md:h-12 bg-slate-50 dark:bg-slate-900 text-slate-400 hover:text-rose-500 rounded-full flex items-center justify-center transition-all shadow-inner"><i className="fas fa-times text-xl"></i></button>
              </div>

              <div className="max-h-[400px] md:max-h-[500px] overflow-y-auto space-y-3 pr-2 custom-scrollbar">
                 {expiringTomorrow.map(c => (
                   <div
                     key={c.id}
                     onClick={() => {
                        setQuickDateCust(c);
                        setQuickDates({ expireDate: c.expireDate || '', requestDate: c.requestDate || '' });
                        setShowQuickDateModal(true);
                     }}
                     className="bg-slate-50 dark:bg-slate-900/50 p-6 rounded-[32px] border-2 border-slate-100 dark:border-slate-800 hover:border-teal-500/50 transition-all group shadow-sm cursor-pointer"
                   >
                      <div className="flex justify-between items-start">
                         <div className="flex items-center space-x-5">
                            <div className="w-14 h-14 bg-white dark:bg-slate-800 rounded-2xl flex items-center justify-center text-2xl text-slate-300 group-hover:text-teal-500 transition-colors shadow-inner border border-slate-50 dark:border-slate-700">
                               <i className="fas fa-user"></i>
                            </div>
                            <div className="space-y-1">
                               <p className="text-xl font-black text-slate-800 dark:text-white uppercase leading-none tracking-tighter">{c.name}</p>
                               <p className="text-xs font-black text-indigo-600 tracking-widest">{c.mobile}</p>
                               <div className="flex flex-wrap gap-2 items-center pt-1">
                                  <span className="text-[9px] font-black bg-slate-100 dark:bg-slate-800 px-2 py-0.5 rounded-lg tracking-widest uppercase">{c.zone || 'No Zone'}</span>
                                  <span className="text-[9px] font-black text-slate-400 tracking-widest">{c.pppoeUsername}</span>
                               </div>
                            </div>
                         </div>
                         <div className="text-right">
                            <p className="text-[9px] text-slate-400 font-black uppercase tracking-widest mb-1 leading-none">CURRENT DUE</p>
                            <p className="text-2xl font-black text-rose-500 tracking-tighter leading-none">৳{Math.floor(c.currentDue)}</p>
                            <span className="inline-block mt-2 bg-rose-50 text-rose-600 px-3 py-1 rounded-lg text-[9px] font-black border border-rose-100 uppercase animate-pulse">EXPIRING TOMORROW</span>
                         </div>
                      </div>
                   </div>
                 ))}
              </div>

              <div className="pt-4 flex gap-4">
                 <button onClick={() => setShowExpiryModal(false)} className="flex-1 bg-slate-100 dark:bg-slate-700 text-slate-500 dark:text-slate-300 py-6 rounded-[28px] font-black uppercase tracking-[4px] text-xs shadow-xl active:scale-95 transition-all">CLOSE</button>
                 <button
                   onClick={() => { setShowExpiryModal(false); setActivePage('customers'); }}
                   className="flex-[2] bg-rose-600 text-white py-6 rounded-[28px] font-black uppercase tracking-[6px] text-xs shadow-2xl shadow-rose-500/30 hover:scale-[1.02] active:scale-95 transition-all border-b-4 border-rose-900"
                 >
                    MANAGE CRM
                 </button>
              </div>
           </div>
        </div>
      )}

      {/* QUICK DATE CHANGE MODAL FROM ALERT */}
      {showQuickDateModal && (
        <div className="fixed inset-0 bg-slate-900/95 backdrop-blur-3xl z-[20000] flex items-center justify-center p-6 animate-fadeIn font-black uppercase">
          <div className="bg-white dark:bg-slate-800 rounded-[56px] w-full max-w-lg p-12 shadow-2xl border-4 border-teal-500/20 space-y-10 relative overflow-hidden">
             <div className="absolute top-0 left-0 w-full h-3 bg-teal-500"></div>
             <div className="flex justify-between items-center border-b pb-6">
                <div>
                   <h3 className="text-3xl font-black uppercase tracking-tighter leading-none text-slate-800 dark:text-white">Quick Update</h3>
                   <p className="text-[10px] text-slate-400 font-bold mt-2 tracking-[3px] uppercase">Client: {quickDateCust?.name}</p>
                </div>
                <button onClick={() => setShowQuickDateModal(false)} className="text-rose-500 text-2xl hover:scale-110 transition-all"><i className="fas fa-times-circle"></i></button>
             </div>

             <div className="space-y-8">
                <div className="space-y-2">
                  <label className="text-[10px] text-slate-400 ml-4 tracking-[3px] font-black uppercase">Expire Date</label>
                  <input
                    type="date"
                    value={quickDates.expireDate}
                    onChange={e => setQuickDates({...quickDates, expireDate: e.target.value})}
                    className="w-full bg-slate-50 dark:bg-slate-900 p-6 rounded-3xl font-black text-lg outline-none border-none shadow-inner"
                  />
                </div>

                <div className="space-y-2">
                  <label className="text-[10px] text-slate-400 ml-4 tracking-[3px] font-black uppercase">Request Date</label>
                  <input
                    type="date"
                    value={quickDates.requestDate}
                    onChange={e => setQuickDates({...quickDates, requestDate: e.target.value})}
                    className="w-full bg-slate-50 dark:bg-slate-900 p-6 rounded-3xl font-black text-lg outline-none border-none shadow-inner"
                  />
                </div>
             </div>

             <div className="flex space-x-4">
                <button onClick={() => setShowQuickDateModal(false)} className="flex-1 bg-slate-100 dark:bg-slate-700 py-6 rounded-3xl font-black text-xs tracking-widest text-slate-400">CANCEL</button>
                <button
                  onClick={handleGlobalQuickDateUpdate}
                  className="flex-[2] bg-teal-600 text-white py-6 rounded-3xl font-black uppercase tracking-[5px] shadow-2xl hover:scale-[1.02] active:scale-95 transition-all"
                >
                   SAVE CHANGES
                </button>
             </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default App;
