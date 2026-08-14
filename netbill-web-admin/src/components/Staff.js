import React, { useState } from 'react';
import { db } from '../firebaseConfig';
import { collection, addDoc } from 'firebase/firestore';

const Staff = ({ store }) => {
  const [showModal, setShowModal] = useState(false);

  return (
    <div className="max-w-7xl mx-auto space-y-10 pb-20 uppercase font-black tracking-widest">
      <div className="flex justify-between items-center">
        <div className="space-y-2 font-black uppercase">
          <h3 className="text-5xl tracking-tighter uppercase font-black uppercase">Staff Team</h3>
          <p className="text-xs text-indigo-600 tracking-widest uppercase">Lineman & Management Staff</p>
        </div>
        <button onClick={() => setShowModal(true)} className="bg-indigo-600 text-white px-10 py-5 rounded-[32px] shadow-2xl font-black text-xs uppercase tracking-widest transition-all hover:scale-105 active:scale-95 uppercase">
          + Recruit Staff
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-10">
        {store.staff?.map(s => (
          <div key={s.id} className="bg-white dark:bg-slate-800 p-10 rounded-[56px] border border-slate-100 dark:border-slate-700 shadow-xl flex flex-col items-center text-center space-y-6">
            <div className="w-24 h-24 bg-teal-50 dark:bg-slate-900 rounded-[40px] flex items-center justify-center text-teal-600">
               <i className="fas fa-user-tie text-5xl"></i>
            </div>
            <div>
               <h4 className="text-3xl font-black text-slate-800 dark:text-white tracking-tighter uppercase leading-none">{s.name}</h4>
               <p className="text-[10px] font-black text-teal-600 uppercase tracking-[4px] mt-2">{s.role}</p>
            </div>
            <div className="bg-slate-50 dark:bg-slate-900 w-full py-5 rounded-[28px] font-black text-slate-500 text-sm tracking-[3px]">
              ৳ {s.salary} / MONTH
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default Staff;
