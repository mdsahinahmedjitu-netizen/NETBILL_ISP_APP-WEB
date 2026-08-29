import React from 'react';

const Sidebar = ({ isSidebarOpen, setIsSidebarOpen, activePage, setActivePage, onLogout, t, role, subRole, permissions }) => {

  // Define menu items based on role
  const getMenuItems = () => {
    if (role === 'customer') {
      return [
        { id: 'dashboard', icon: 'fa-th-large', label: 'My Dashboard' },
        { id: 'billing_summary', icon: 'fa-file-invoice-dollar', label: 'Billing Summary' },
        { id: 'settings', icon: 'fa-user-circle', label: 'My Profile' },
      ];
    }

    const items = [
      { id: 'dashboard', icon: 'fa-th-large', label: t.dashboard_overview },
      { id: 'customers', icon: 'fa-users', label: t.subscribers_crm, visible: permissions.canAccessCustomers },
      { id: 'crm_tickets', icon: 'fa-headset', label: t.support_tickets, visible: permissions.canAccessTickets },
      { id: 'payments', icon: 'fa-money-check-dollar', label: t.payment_center, visible: permissions.canAccessPayments },
      { id: 'update_bill', icon: 'fa-file-invoice-dollar', label: t.update_bill, visible: role === 'admin' },
      { id: 'reports', icon: 'fa-chart-pie', label: t.collection_report, visible: permissions.canAccessReports },
      { id: 'expenses', icon: 'fa-file-invoice-dollar', label: t.expense_title, visible: permissions.canAccessExpenses },
      { id: 'staff', icon: 'fa-user-tie', label: t.staff_team, visible: permissions.canAccessStaff },
      { id: 'salary_history', icon: 'fa-file-invoice-dollar', label: t.salary_ledger, visible: permissions.canAccessSalary },
      { id: 'inventory', icon: 'fa-box', label: t.inventory_stock, visible: permissions.canAccessInventory },
      { id: 'packages', icon: 'fa-wifi', label: t.service_packages, visible: permissions.canAccessPackages },
      { id: 'mikrotik', icon: 'fa-microchip', label: 'MikroTik Monitor', visible: permissions.canManageRouters },
      { id: 'infrastructure', icon: 'fa-network-wired', label: 'Infrastructure', visible: permissions.canAccessInfrastructure },
      { id: 'sms_setup', icon: 'fa-envelope-open-text', label: t.sms_setup, visible: permissions.canAccessSMS },
      { id: 'sms_logs', icon: 'fa-history', label: t.sms_history, visible: permissions.canAccessSmsLogs },
      { id: 'settings', icon: 'fa-cog', label: t.global_settings, visible: permissions.canAccessGlobalSettings },
    ];

    return items.filter(item => item.visible !== false);
  };

  const menuItems = getMenuItems();

  const isCollector = subRole === 'Collector';

  return (
    <>
      {isSidebarOpen && (
        <div
          className="fixed inset-0 bg-black/50 z-40 lg:hidden backdrop-blur-sm"
          onClick={() => setIsSidebarOpen(false)}
        ></div>
      )}

      <aside
        className={`${role === 'admin' ? 'bg-[#0F172A]' : role === 'customer' ? 'bg-[#064e3b]' : (isCollector ? 'bg-pink-200 shadow-[inset_-2px_0_10px_rgba(0,0,0,0.05)]' : 'bg-[#1e1b4b]')} ${isCollector ? 'text-pink-950' : 'text-white'} flex flex-col shrink-0 shadow-2xl z-50 uppercase font-black tracking-widest transition-all duration-300 ease-in-out fixed lg:relative h-full ${
          isSidebarOpen ? 'w-72 p-6 left-0' : 'w-0 p-0 overflow-hidden -left-72 lg:left-0'
        }`}
      >
        <div className={`flex items-center justify-between mb-10 px-2 transition-opacity ${isSidebarOpen ? 'opacity-100' : 'opacity-0'}`}>
          <div className="flex items-center space-x-3">
            <div className={`w-10 h-10 ${isCollector ? 'bg-pink-600' : 'bg-teal-500'} rounded-xl flex items-center justify-center shadow-lg`}>
              <i className="fas fa-bolt text-xl text-white"></i>
            </div>
            <h1 className={`text-2xl font-black tracking-tight whitespace-nowrap ${isCollector ? 'text-pink-900' : 'text-white'}`}>NetBill <span className={isCollector ? 'text-pink-600' : 'text-teal-500'}>ISP</span></h1>
          </div>
          <button onClick={() => setIsSidebarOpen(false)} className={`lg:hidden ${isCollector ? 'text-pink-400' : 'text-slate-400'} hover:text-pink-600`}>
            <i className="fas fa-times text-xl"></i>
          </button>
        </div>

      <nav className={`flex-1 space-y-2 overflow-y-auto pr-2 text-[12px] tracking-[2px] transition-opacity ${isSidebarOpen ? 'opacity-100' : 'opacity-0'}`}>
        {menuItems.map((item) => {
          if (item.adminOnly && role !== 'admin') return null;

          return (
            <div
              key={item.id}
              onClick={() => {
                setActivePage(item.id);
                if (window.innerWidth <= 1024) setIsSidebarOpen(false);
              }}
              className={`p-4 md:p-5 rounded-2xl flex items-center space-x-4 cursor-pointer transition-all duration-300 ${
                activePage === item.id
                  ? (isCollector ? 'bg-pink-600 text-white shadow-xl shadow-pink-500/30 scale-[1.02]' : 'bg-[#0D9488] text-white shadow-xl shadow-teal-500/30 scale-[1.02]')
                  : isCollector ? 'text-pink-700/70 hover:bg-pink-300/50 hover:text-pink-900' : 'text-slate-400 hover:bg-slate-800 hover:text-slate-200'
              }`}
            >
              <i className={`fas ${item.icon} text-lg`}></i>
              <span className="whitespace-nowrap font-black">{item.label}</span>
            </div>
          );
        })}
      </nav>

        <div className={`pt-6 border-t ${isCollector ? 'border-pink-300' : 'border-slate-700'} transition-opacity ${isSidebarOpen ? 'opacity-100' : 'opacity-0'}`}>
          <button
            onClick={onLogout}
            className={`w-full ${isCollector ? 'bg-pink-600/10 text-pink-600 hover:bg-pink-600 hover:text-white' : 'bg-rose-500/10 text-rose-500 hover:bg-rose-500 hover:text-white'} p-4 rounded-2xl font-black text-xs transition-all uppercase tracking-widest whitespace-nowrap`}
          >
            {t.sign_out}
          </button>
        </div>
      </aside>
    </>
  );
};

export default Sidebar;
