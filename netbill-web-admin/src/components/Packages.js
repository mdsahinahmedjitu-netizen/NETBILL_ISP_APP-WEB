import React from 'react';

const Packages = ({ store, setActivePage }) => {
  return (
    <div className="max-w-7xl mx-auto space-y-6 md:space-y-12 pb-20 uppercase font-black tracking-widest leading-none px-2 md:px-0">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 bg-white dark:bg-slate-800 p-4 md:p-6 rounded-[24px] md:rounded-[32px] shadow-lg border border-slate-50 dark:border-slate-700">
        <div className="flex items-center space-x-3 md:space-x-6">
           <button onClick={() => setActivePage('dashboard')} className="w-10 h-10 md:w-12 md:h-12 bg-slate-50 dark:bg-slate-950 rounded-xl md:rounded-2xl flex items-center justify-center text-teal-600 dark:text-teal-400 shadow-sm border border-slate-100 dark:border-slate-800">
              <i className="fas fa-arrow-left"></i>
           </button>
           <div className="space-y-1">
              <h3 className="text-xl md:text-5xl tracking-tighter uppercase font-black text-slate-800 dark:text-white leading-none">Service Plans</h3>
              <p className="text-[8px] md:text-[10px] text-teal-600 font-bold tracking-[2px] md:tracking-[4px] uppercase opacity-70">Broadband & FTTH Packages</p>
           </div>
        </div>
        <button className="w-full md:w-auto bg-[#0D9488] text-white px-6 py-3 md:px-10 md:py-5 rounded-xl md:rounded-[32px] shadow-2xl font-black text-[10px] md:text-xs uppercase tracking-widest transition-all hover:scale-105 active:scale-95 border-b-4 border-teal-900">
          + NEW PACKAGE
        </button>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 md:gap-8 text-center uppercase font-black">
        {store.packages?.length > 0 ? store.packages.map(p => {
          const activeCount = store.customers?.filter(c => (c.packageId === p.id || c.packageName === p.name) && c.status === 'Active').length;

          return (
            <div key={p.id} className="bg-white dark:bg-slate-800 p-6 md:p-10 rounded-[32px] md:rounded-[56px] border border-slate-100 dark:border-slate-700 shadow-xl space-y-4 md:space-y-6 group hover:-translate-y-1 transition-all">
              <div className="w-12 h-12 md:w-16 md:h-16 bg-teal-50 dark:bg-slate-950 rounded-xl md:rounded-[28px] mx-auto flex items-center justify-center text-teal-600 group-hover:text-white group-hover:bg-teal-600 transition-all duration-300">
                 <i className="fas fa-wifi text-xl md:text-3xl"></i>
              </div>
              <div className="space-y-1 md:space-y-2">
                <h4 className="text-xl md:text-3xl font-black text-slate-800 dark:text-white tracking-tighter uppercase leading-none truncate">{p.name}</h4>
                <p className="text-[8px] md:text-[10px] text-teal-600 font-bold tracking-[2px] uppercase">Active: {activeCount}</p>
              </div>
              <p className="text-3xl md:text-5xl font-black text-slate-900 dark:text-teal-400 tracking-tighter uppercase leading-none">৳{p.monthlyPrice}</p>
              <button className="bg-slate-100 dark:bg-slate-950 w-full py-4 md:py-5 rounded-2xl md:rounded-[32px] font-black text-[9px] md:text-xs uppercase tracking-widest mt-2 md:mt-4 hover:bg-teal-600 hover:text-white transition-all shadow-sm border border-slate-200 dark:border-slate-800">
                Update Plan
              </button>
            </div>
          );
        }) : (
            <div className="col-span-full py-40 text-center opacity-10">
               <i className="fas fa-wifi text-9xl"></i>
               <p className="text-3xl mt-6 tracking-[10px]">NO PACKAGES</p>
            </div>
        )}
      </div>
    </div>
  );
};

export default Packages;
