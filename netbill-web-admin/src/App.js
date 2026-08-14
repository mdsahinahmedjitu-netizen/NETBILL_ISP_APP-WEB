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
import Packages from './components/Packages';
import Settings from './components/Settings';
import Login from './components/Login';
import CustomerPortal from './components/CustomerPortal';
import { translations } from './translations';
import './index.css';

function App() {
  const [session, setSession] = useState(() => {
    const saved = localStorage.getItem('netbill_session');
    return saved ? JSON.parse(saved) : null;
  });

  const [activePage, setActivePage] = useState('dashboard');
  const [isDarkMode, setIsDarkMode] = useState(localStorage.getItem('dark_mode') === 'true');
  const [lang, setLang] = useState(localStorage.getItem('app_lang') || 'bn');
  const [isSidebarOpen, setIsSidebarOpen] = useState(true);
  const [store, setStore] = useState({
    customers: [], tickets: [], payments: [], invoices: [], expenses: [], staff: [],
    inventory: [], packages: [], settings: {}, paymentRequests: [], staffPayouts: []
  });
  const [autoOpenAddModal, setAutoOpenAddModal] = useState(false);

  const t = translations[lang];

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
    const unsubSettings = onSnapshot(doc(db, "settings", "global"), (snap) => {
      if (snap.exists()) setStore(prev => ({ ...prev, settings: snap.data() }));
    });

    return () => { unsubCust(); unsubTickets(); unsubPayments(); unsubRequests(); unsubInvoices(); unsubExpenses(); unsubStaff(); unsubStaffPayouts(); unsubSettings(); };
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
          {activePage === 'customers' && <Customers store={store} setActivePage={setActivePage} t={t} lang={lang} autoOpenModal={autoOpenAddModal} setAutoOpenModal={setAutoOpenAddModal} />}
          {activePage === 'billing' && <Billing store={store} t={t} lang={lang} />}
          {activePage === 'payments' && <Payments store={store} session={session} t={t} lang={lang} />}
          {activePage === 'reports' && <CollectionReport store={store} t={t} />}
          {activePage === 'staff' && session.role === 'admin' && <Staff store={store} t={t} lang={lang} />}
          {activePage === 'salary_history' && <SalaryHistory store={store} session={session} t={t} lang={lang} />}
          {activePage === 'inventory' && <Inventory store={store} t={t} lang={lang} />}
          {activePage === 'packages' && <Packages store={store} t={t} lang={lang} />}
          {activePage === 'settings' && session.role === 'admin' && <Settings store={store} t={t} lang={lang} />}
        </main>
      </div>
    </div>
  );
}

export default App;
