import React, { useState, useEffect } from 'react';
import { db } from './firebaseConfig';
import { collection, onSnapshot, doc } from 'firebase/firestore';
import Sidebar from './components/Sidebar';
import Dashboard from './components/Dashboard';
import Customers from './components/Customers';
import Billing from './components/Billing';
import Payments from './components/Payments';
import CollectionReport from './components/CollectionReport';
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
    zones: [], subZones: [], boxes: []
  });
  const [autoOpenAddModal, setAutoOpenAddModal] = useState(false);
  const [showExpiryModal, setShowExpiryModal] = useState(false);

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

  // Real-time Cloud Sync
  useEffect(() => {
    if (!session) return;

    const unsubCust = onSnapshot(collection(db, "customers"), (snap) => {
      setStore(prev => ({ ...prev, customers: snap.docs.map(d => ({ id: d.id, ...d.data() })) }));
    });
    const unsubTickets = onSnapshot(collection(db, "support_tickets"), (snap) => {
      setStore(prev => ({ ...prev, tickets: snap.docs.map(d => ({ id: d.id, ...d.data() })) }));
    });
    const unsubPayments = onSnapshot(collection(db, "payments"), (snap) => {
      setStore(prev => ({ ...prev, payments: snap.docs.map(d => ({ id: d.id, ...d.data() })) }));
    });
    const unsubRequests = onSnapshot(collection(db, "payment_requests"), (snap) => {
      setStore(prev => ({ ...prev, paymentRequests: snap.docs.map(d => ({ id: d.id, ...d.data() })) }));
    });
    const unsubInvoices = onSnapshot(collection(db, "invoices"), (snap) => {
      setStore(prev => ({ ...prev, invoices: snap.docs.map(d => ({ id: d.id, ...d.data() })) }));
    });
    const unsubExpenses = onSnapshot(collection(db, "expenses"), (snap) => {
      setStore(prev => ({ ...prev, expenses: snap.docs.map(d => ({ id: d.id, ...d.data() })) }));
    });
    const unsubStaff = onSnapshot(collection(db, "staff"), (snap) => {
      setStore(prev => ({ ...prev, staff: snap.docs.map(d => ({ id: d.id, ...d.data() })) }));
    });
    const unsubStaffPayouts = onSnapshot(collection(db, "staff_payouts"), (snap) => {
      setStore(prev => ({ ...prev, staffPayouts: snap.docs.map(d => ({ id: d.id, ...d.data() })) }));
    });
    const unsubZones = onSnapshot(collection(db, "zones"), (snap) => {
      setStore(prev => ({ ...prev, zones: snap.docs.map(d => ({ id: d.id, ...d.data() })) }));
    });
    const unsubSubZones = onSnapshot(collection(db, "sub_zones"), (snap) => {
      setStore(prev => ({ ...prev, subZones: snap.docs.map(d => ({ id: d.id, ...d.data() })) }));
    });
    const unsubBoxes = onSnapshot(collection(db, "boxes"), (snap) => {
      setStore(prev => ({ ...prev, boxes: snap.docs.map(d => ({ id: d.id, ...d.data() })) }));
    });
    const unsubSettings = onSnapshot(doc(db, "settings", "global"), (snap) => {
      if (snap.exists()) setStore(prev => ({ ...prev, settings: snap.data() }));
    });

    return () => { unsubCust(); unsubTickets(); unsubPayments(); unsubRequests(); unsubInvoices(); unsubExpenses(); unsubStaff(); unsubStaffPayouts(); unsubZones(); unsubSubZones(); unsubBoxes(); unsubSettings(); };
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
          {activePage === 'staff' && session.role === 'admin' && <Staff store={store} t={t} lang={lang} />}
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
                   <div key={c.id} className="bg-slate-50 dark:bg-slate-900/50 p-5 md:p-6 rounded-[32px] flex justify-between items-center border-2 border-slate-100 dark:border-slate-800 hover:border-rose-500/30 transition-all group shadow-sm">
                      <div className="flex items-center space-x-4 md:space-x-5">
                         <div className="w-12 h-12 md:w-14 md:h-14 bg-white dark:bg-slate-800 rounded-2xl flex items-center justify-center text-xl md:text-2xl text-slate-300 group-hover:text-rose-500 transition-colors shadow-inner border border-slate-50 dark:border-slate-700">
                            <i className="fas fa-user"></i>
                         </div>
                         <div className="space-y-1">
                            <p className="text-lg md:text-xl font-black text-slate-800 dark:text-white uppercase leading-none tracking-tighter">{c.name}</p>
                            <div className="flex flex-wrap gap-2 items-center">
                               <span className="text-[9px] font-black bg-indigo-50 dark:bg-indigo-900/30 text-indigo-600 px-2 py-0.5 rounded-lg tracking-widest">#{c.customerCode}</span>
                               <span className="text-[9px] font-black text-slate-400 tracking-widest">{c.pppoeUsername}</span>
                            </div>
                         </div>
                      </div>
                      <div className="text-right flex flex-col items-end">
                         <p className="text-[8px] md:text-[9px] text-slate-400 font-black uppercase tracking-widest mb-1">EXPIRES</p>
                         <p className="text-sm md:text-lg font-black text-rose-600 bg-rose-50 dark:bg-rose-900/20 px-4 py-1.5 rounded-xl border border-rose-100 dark:border-rose-800 leading-none">{formatDateDisplay(c.expireDate)}</p>
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
    </div>
  );
}

export default App;
