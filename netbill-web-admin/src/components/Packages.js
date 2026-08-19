import React from 'react';

const Packages = ({ store }) => {
  return (
    <div className="max-w-7xl mx-auto space-y-12 pb-20 uppercase font-black tracking-widest leading-none">
      <div className="flex justify-between items-center font-black">
        <h3 className="text-5xl tracking-tighter uppercase font-black uppercase">Service Plans</h3>
        <button className="bg-[#0D9488] text-white px-10 py-5 rounded-[32px] shadow-2xl font-black text-xs uppercase tracking-widest transition-all hover:scale-105 active:scale-95 uppercase">
          + New Package
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8 text-center uppercase font-black">
        {store.packages?.map(p => {
          const activeCount = store.customers?.filter(c => (c.packageId === p.id || c.packageName === p.name) && c.status === 'Active').length;

          return (
            <div key={p.id} className="bg-white dark:bg-slate-800 p-10 rounded-[56px] border border-slate-100 dark:border-slate-700 shadow-xl space-y-6">
              <div className="w-16 h-16 bg-teal-50 dark:bg-slate-900 rounded-[28px] mx-auto flex items-center justify-center text-teal-600 shadow-sm">
                 <i className="fas fa-wifi text-3xl"></i>
              </div>
              <div>
                <h4 className="text-3xl font-black text-slate-800 dark:text-white tracking-tighter uppercase leading-none">{p.name}</h4>
                <p className="text-[10px] text-teal-600 font-bold mt-2 tracking-[2px] uppercase">Active Subscribers: {activeCount}</p>
              </div>
              <p className="text-5xl font-black text-slate-900 dark:text-teal-400 tracking-tighter uppercase">৳ {p.monthlyPrice}</p>
              <button className="bg-slate-100 dark:bg-slate-900 w-full py-5 rounded-[32px] font-black text-xs uppercase tracking-widest mt-6 hover:bg-teal-600 hover:text-white transition-all">
                Update Plan
              </button>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default Packages;
