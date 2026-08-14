import React from 'react';

const Sidebar = ({ isSidebarOpen, setIsSidebarOpen, activePage, setActivePage, onLogout, t, role }) => {
  const menuItems = [
    { id: 'dashboard', icon: 'fa-th-large', label: t.dashboard_overview },
    { id: 'customers', icon: 'fa-users', label: t.subscribers_crm },
    { id: 'payments', icon: 'fa-money-check-dollar', label: t.payment_center },
    { id: 'staff', icon: 'fa-user-tie', label: t.staff_team, adminOnly: true },
    { id: 'salary_history', icon: 'fa-file-invoice-dollar', label: 'Salary Ledger' },
    { id: 'inventory', icon: 'fa-box', label: t.inventory_stock },
    { id: 'packages', icon: 'fa-wifi', label: t.service_packages },
    { id: 'settings', icon: 'fa-cog', label: t.global_settings, adminOnly: true },
  ];

  return (
    <aside
      className={`bg-[#0F172A] text-white flex flex-col shrink-0 shadow-2xl z-20 uppercase font-black tracking-widest transition-all duration-300 ease-in-out ${
        isSidebarOpen ? 'w-72 p-6' : 'w-0 p-0 overflow-hidden'
      }`}
    >
      <div className={`flex items-center space-x-3 mb-10 px-2 transition-opacity ${isSidebarOpen ? 'opacity-100' : 'opacity-0'}`}>
        <div className="w-10 h-10 bg-teal-500 rounded-xl flex items-center justify-center shadow-lg shadow-teal-500/20">
          <i className="fas fa-bolt text-xl"></i>
        </div>
        <h1 className="text-2xl font-black tracking-tight whitespace-nowrap">NetBill <span className="text-teal-400">ISP</span></h1>
      </div>

      <nav className={`flex-1 space-y-2 overflow-y-auto pr-2 text-[10px] tracking-[2px] transition-opacity ${isSidebarOpen ? 'opacity-100' : 'opacity-0'}`}>
        {menuItems.map((item) => {
          // Role-based Access Control
          if (item.adminOnly && role !== 'admin') return null;

          return (
            <div
              key={item.id}
              onClick={() => setActivePage(item.id)}
              className={`p-4 rounded-2xl flex items-center space-x-4 cursor-pointer transition-all duration-300 ${
                activePage === item.id
                  ? 'bg-[#0D9488] text-white shadow-xl shadow-teal-500/30 scale-[1.02]'
                  : 'text-slate-400 hover:bg-slate-800 hover:text-slate-200'
              }`}
            >
              <i className={`fas ${item.icon} text-sm`}></i>
              <span className="whitespace-nowrap">{item.label}</span>
            </div>
          );
        })}
      </nav>

      <div className={`pt-6 border-t border-slate-700 transition-opacity ${isSidebarOpen ? 'opacity-100' : 'opacity-0'}`}>
        <button
          onClick={onLogout}
          className="w-full bg-rose-500/10 text-rose-500 hover:bg-rose-500 hover:text-white p-4 rounded-2xl font-black text-xs transition-all uppercase tracking-widest whitespace-nowrap"
        >
          {t.sign_out}
        </button>
      </div>
    </aside>
  );
};

export default Sidebar;
