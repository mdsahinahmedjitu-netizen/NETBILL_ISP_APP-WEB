import React from 'react';

const Inventory = ({ store }) => {
  return (
    <div className="max-w-7xl mx-auto space-y-10 pb-20 uppercase font-black tracking-widest">
      <div className="flex justify-between items-center font-black">
        <div className="space-y-2 font-black uppercase">
          <h3 className="text-5xl tracking-tighter uppercase font-black uppercase">Hardware Stock</h3>
          <p className="text-xs text-slate-400 tracking-widest font-black uppercase tracking-widest">ONU, Router & Material Tracker</p>
        </div>
        <button className="bg-slate-900 text-white px-10 py-5 rounded-[32px] shadow-2xl font-black text-xs uppercase tracking-widest transition-all hover:scale-105 active:scale-95 uppercase">
          + New Material
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8 uppercase font-black">
        {store.inventory?.map(i => (
          <div key={i.id} className="bg-white dark:bg-slate-800 p-8 rounded-[44px] shadow-xl border border-slate-100 dark:border-slate-700 space-y-6">
            <div className="flex justify-between items-start">
               <div className="w-16 h-16 bg-slate-50 dark:bg-slate-900 rounded-[28px] flex items-center justify-center text-slate-400">
                  <i className="fas fa-box text-3xl"></i>
               </div>
               <span className={`px-5 py-2 rounded-full text-[9px] font-black uppercase ${i.status === 'In Stock' ? 'bg-emerald-100 text-emerald-700' : 'bg-rose-100 text-rose-700'}`}>
                  {i.status}
               </span>
            </div>
            <h4 className="text-2xl font-black tracking-tighter uppercase leading-none text-slate-800 dark:text-white">{i.itemName}</h4>
            <p className="text-[11px] font-black text-slate-400 uppercase tracking-widest mt-1">{i.brand} • SN: {i.serialNumber}</p>
          </div>
        ))}
      </div>
    </div>
  );
};

export default Inventory;
