import React from 'react';

const Inventory = ({ store, setActivePage }) => {
  return (
    <div className="max-w-7xl mx-auto space-y-6 md:space-y-10 pb-20 uppercase font-black tracking-widest px-2 md:px-0">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 bg-white dark:bg-slate-800 p-4 md:p-6 rounded-[24px] md:rounded-[32px] shadow-lg border border-slate-50 dark:border-slate-700">
        <div className="flex items-center space-x-3 md:space-x-6">
           <button onClick={() => setActivePage('dashboard')} className="w-10 h-10 md:w-12 md:h-12 bg-slate-50 dark:bg-slate-950 rounded-xl md:rounded-2xl flex items-center justify-center text-slate-900 dark:text-white shadow-sm border border-slate-100 dark:border-slate-800">
              <i className="fas fa-arrow-left"></i>
           </button>
           <div className="space-y-1 md:space-y-2 font-black uppercase">
              <h3 className="text-xl md:text-5xl tracking-tighter uppercase font-black text-slate-800 dark:text-white leading-none">Hardware Stock</h3>
              <p className="text-[8px] md:text-xs text-slate-400 tracking-[2px] md:tracking-widest font-black uppercase leading-tight">ONU, Router & Material Tracker</p>
           </div>
        </div>
        <button className="w-full md:w-auto bg-slate-900 text-white px-6 py-3 md:px-10 md:py-5 rounded-xl md:rounded-[32px] shadow-2xl font-black text-[10px] md:text-xs uppercase tracking-widest transition-all hover:scale-105 active:scale-95 border-b-4 border-slate-700">
          + New Material
        </button>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4 md:gap-8 uppercase font-black">
        {store.inventory?.length > 0 ? store.inventory.map(i => (
          <div key={i.id} className="bg-white dark:bg-slate-800 p-6 md:p-8 rounded-[32px] md:rounded-[44px] shadow-xl border border-slate-100 dark:border-slate-700 space-y-4 md:space-y-6 group hover:-translate-y-1 transition-all">
            <div className="flex justify-between items-start">
               <div className="w-12 h-12 md:w-16 md:h-16 bg-slate-50 dark:bg-slate-950 rounded-2xl md:rounded-[28px] flex items-center justify-center text-slate-300 group-hover:text-indigo-600 transition-colors">
                  <i className="fas fa-box text-xl md:text-3xl"></i>
               </div>
               <span className={`px-4 py-1.5 md:px-5 md:py-2 rounded-full text-[8px] md:text-[9px] font-black uppercase shadow-sm ${i.status === 'In Stock' ? 'bg-emerald-100 text-emerald-700' : 'bg-rose-100 text-rose-700'}`}>
                  {i.status}
               </span>
            </div>
            <div>
              <h4 className="text-lg md:text-2xl font-black tracking-tighter uppercase leading-tight text-slate-800 dark:text-white truncate">{i.itemName}</h4>
              <p className="text-[9px] md:text-[11px] font-black text-slate-400 uppercase tracking-widest mt-1.5 opacity-80">{i.brand} • SN: {i.serialNumber}</p>
            </div>
          </div>
        )) : (
          <div className="col-span-full py-40 text-center opacity-10">
             <i className="fas fa-microchip text-9xl"></i>
             <p className="text-3xl mt-6 tracking-[10px]">EMPTY STOCK</p>
          </div>
        )}
      </div>
    </div>
  );
};

export default Inventory;
