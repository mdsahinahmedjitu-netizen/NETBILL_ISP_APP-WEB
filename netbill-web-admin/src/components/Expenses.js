import React, { useState, useMemo } from 'react';
import { db } from '../firebaseConfig';
import { collection, addDoc, doc, deleteDoc } from 'firebase/firestore';

const Expenses = ({ store, session, t }) => {
  const [showAddModal, setShowAddModal] = useState(false);
  const [showCategoryModal, setShowCategoryModal] = useState(false);
  const [isProcessing, setIsProcessing] = useState(false);
  const [filterCategory, setFilterCategory] = useState('All');
  const [search, setSearch] = useState('');
  const [newCatName, setNewCatName] = useState('');

  const defaultCategories = [
    { id: 'Bandwidth Cost', label: t.cat_bandwidth },
    { id: 'Staff Salary', label: t.cat_salary },
    { id: 'Office Rent', label: t.cat_rent },
    { id: 'Electricity Bill', label: t.cat_electricity },
    { id: 'Equipment Purchase', label: t.cat_equipment },
    { id: 'Maintenance', label: t.cat_maintenance },
    { id: 'Transport', label: t.cat_transport },
    { id: 'Marketing', label: t.cat_marketing },
    { id: 'Other Expense', label: t.cat_other }
  ];

  const categories = useMemo(() => {
    const custom = store.expenseCategories?.map(c => ({ id: c.name, label: c.name })) || [];
    return [...defaultCategories, ...custom];
  }, [store.expenseCategories, t]);

  const getCatLabel = (id) => {
    const found = defaultCategories.find(c => c.id === id);
    return found ? found.label : id;
  };

  const initialState = {
    title: '',
    category: defaultCategories[0].id,
    amount: '',
    expenseDate: new Date().toLocaleDateString('en-CA'),
    expenseBy: session?.data?.name || 'Admin',
    notes: ''
  };
  const [formData, setFormData] = useState(initialState);

  const handleAddCategory = async (e) => {
    e.preventDefault();
    if (!newCatName) return;
    try {
      await addDoc(collection(db, "expense_categories"), { name: newCatName });
      setNewCatName('');
      alert("Category added successfully!");
    } catch (e) { alert("Failed to add category"); }
  };

  const handleCatDelete = async (id) => {
    if (window.confirm("Delete this category?")) {
      try {
        await deleteDoc(doc(db, "expense_categories", id));
      } catch (e) { alert("Delete failed."); }
    }
  };

  const filteredExpenses = useMemo(() => {
    return store.expenses?.filter(e => {
      const matchCat = filterCategory === 'All' || e.category === filterCategory;
      const matchSearch = !search ||
        e.title?.toLowerCase().includes(search.toLowerCase()) ||
        e.expenseBy?.toLowerCase().includes(search.toLowerCase());
      return matchCat && matchSearch;
    }).sort((a, b) => new Date(b.expenseDate) - new Date(a.expenseDate));
  }, [store.expenses, filterCategory, search]);

  const totalExpense = filteredExpenses.reduce((sum, e) => sum + (parseFloat(e.amount) || 0), 0);
  const todayExpense = store.expenses?.filter(e => e.expenseDate === new Date().toLocaleDateString('en-CA'))
    .reduce((sum, e) => sum + (parseFloat(e.amount) || 0), 0);

  const handleAddExpense = async (e) => {
    e.preventDefault();
    if (!formData.title || !formData.amount) return alert("Title and Amount are required!");

    setIsProcessing(true);
    try {
      await addDoc(collection(db, "expenses"), {
        ...formData,
        amount: parseFloat(formData.amount),
        createdAt: new Date().toISOString()
      });
      alert("Expense Recorded Successfully!");
      setShowAddModal(false);
      setFormData(initialState);
    } catch (error) {
      alert("Failed to record expense.");
    } finally {
      setIsProcessing(false);
    }
  };

  const handleDelete = async (id) => {
    if (window.confirm("Delete this expense permanently?")) {
      try {
        await deleteDoc(doc(db, "expenses", id));
      } catch (e) { alert("Delete failed."); }
    }
  };

  return (
    <div className="w-full space-y-8 pb-20 font-sans tracking-tight uppercase font-black">
      {/* HEADER & STATS */}
      <div className="flex flex-col lg:flex-row justify-between items-start lg:items-center gap-6">
        <div className="space-y-1">
          <h3 className="text-4xl md:text-5xl font-black text-slate-800 dark:text-white tracking-tighter leading-none">{t.expense_manager}</h3>
          <p className="text-[10px] text-rose-500 font-bold tracking-[4px] uppercase mt-1">Financial Outflow & Operational Costs</p>
        </div>
        <div className="flex flex-wrap gap-4">
           <StatBox label={t.total_selected} value={`৳ ${totalExpense.toLocaleString()}`} color="text-rose-600" bgColor="bg-rose-50 dark:bg-rose-900/20" />
           <StatBox label={t.todays_outflow} value={`৳ ${todayExpense.toLocaleString()}`} color="text-amber-600" bgColor="bg-amber-50 dark:bg-amber-900/20" />
           <div className="flex gap-2">
             <button
               onClick={() => setShowCategoryModal(true)}
               className="bg-teal-50 text-teal-600 px-6 py-5 rounded-[28px] shadow-sm font-black text-xs tracking-[2px] transition-all border-2 border-teal-100"
             >
                <i className="fas fa-tags mr-2"></i>{t.expense_categories?.split(' ')[1] || 'CATEGORIES'}
             </button>
             <button
               onClick={() => setShowAddModal(true)}
               className="bg-slate-900 text-white px-10 py-5 rounded-[28px] shadow-2xl font-black text-xs tracking-[2px] transition-all hover:scale-105 active:scale-95 border-b-4 border-slate-700"
             >
                + {t.add_expense}
             </button>
           </div>
        </div>
      </div>

      {/* FILTERS */}
      <div className="bg-white dark:bg-slate-800 p-6 rounded-[32px] shadow-xl border border-slate-100 dark:border-slate-700 flex flex-wrap items-center gap-4">
         <div className="relative flex-1 min-w-[250px]">
            <input
              type="text"
              placeholder={t.search_placeholder}
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full pl-12 pr-6 py-4 bg-slate-50 dark:bg-slate-900 rounded-2xl border-none font-black text-sm shadow-inner outline-none focus:ring-2 focus:ring-rose-500/20 uppercase"
            />
            <i className="fas fa-search absolute left-5 top-5 text-slate-300"></i>
         </div>
         <select
           value={filterCategory}
           onChange={e => setFilterCategory(e.target.value)}
           className="bg-slate-50 dark:bg-slate-900 p-4 rounded-2xl border-none font-black text-xs outline-none cursor-pointer min-w-[200px]"
         >
            <option value="All">{t.all_types}</option>
            {categories.map(c => <option key={c.id} value={c.id}>{c.label}</option>)}
         </select>
      </div>

      {/* EXPENSE LIST */}
      <div className="bg-white dark:bg-slate-800 rounded-[40px] shadow-2xl border border-slate-100 dark:border-slate-700 overflow-hidden">
         <div className="overflow-x-auto custom-scrollbar min-h-[500px]">
            <table className="w-full text-left whitespace-nowrap">
               <thead className="bg-slate-50 dark:bg-slate-900 border-b-2 border-slate-100 dark:border-slate-700 text-[10px] text-slate-400 tracking-[2px] font-black uppercase">
                  <tr>
                     <th className="p-6">{t.day}</th>
                     <th className="p-6">Description / Title</th>
                     <th className="p-6">Category</th>
                     <th className="p-6">Spent By</th>
                     <th className="p-6 text-right">Amount</th>
                     <th className="p-6 text-center">Action</th>
                  </tr>
               </thead>
               <tbody className="divide-y divide-slate-50 dark:divide-slate-700">
                  {filteredExpenses.map(exp => (
                    <tr key={exp.id} className="hover:bg-rose-50/20 transition-all group">
                       <td className="p-6 text-xs font-black text-slate-500">{exp.expenseDate || exp.date}</td>
                       <td className="p-6">
                          <p className="text-lg font-black text-slate-800 dark:text-white tracking-tighter leading-none">{exp.title || exp.item}</p>
                          <p className="text-[9px] text-slate-400 mt-1.5 font-bold italic uppercase">{exp.notes || exp.remarks || 'No extra notes'}</p>
                       </td>
                       <td className="p-6">
                          <span className="bg-slate-100 dark:bg-slate-900 px-4 py-2 rounded-xl text-[9px] font-black tracking-widest border border-slate-200 dark:border-slate-700">{getCatLabel(exp.category)}</span>
                       </td>
                       <td className="p-6 text-xs font-black text-indigo-600 uppercase italic">{exp.expenseBy}</td>
                       <td className="p-6 text-right font-black text-2xl text-rose-500 tracking-tighter">৳ {exp.amount}</td>
                       <td className="p-6 text-center">
                          <button
                            onClick={() => handleDelete(exp.id)}
                            className="w-10 h-10 rounded-xl bg-rose-50 text-rose-400 hover:bg-rose-600 hover:text-white transition-all shadow-sm"
                          >
                             <i className="fas fa-trash-alt"></i>
                          </button>
                       </td>
                    </tr>
                  ))}
               </tbody>
            </table>
            {filteredExpenses.length === 0 && (
              <div className="py-40 text-center opacity-10">
                 <i className="fas fa-file-invoice-dollar text-[120px]"></i>
                 <p className="text-3xl mt-6 tracking-[10px]">No Records Found</p>
              </div>
            )}
         </div>
      </div>

      {/* ADD EXPENSE MODAL */}
      {showAddModal && (
        <div className="fixed inset-0 bg-slate-900/95 backdrop-blur-3xl z-[5000] flex items-center justify-center p-4 md:p-6 animate-fadeIn font-black uppercase text-slate-800 dark:text-white">
           <div className="bg-white dark:bg-slate-800 rounded-[56px] w-full max-w-xl p-10 md:p-14 shadow-2xl border-2 border-slate-100 dark:border-slate-700 space-y-10 relative overflow-hidden">
              <div className="absolute top-0 left-0 w-full h-3 bg-rose-600"></div>

              <div className="flex justify-between items-center border-b pb-8">
                 <div className="flex items-center space-x-4">
                    <div className="w-14 h-14 bg-rose-50 dark:bg-rose-900/30 text-rose-600 rounded-2xl flex items-center justify-center text-3xl shadow-inner border border-rose-100"><i className="fas fa-receipt"></i></div>
                    <h3 className="text-3xl font-black uppercase tracking-tighter">{t.add_expense}</h3>
                 </div>
                 <button onClick={() => setShowAddModal(false)} className="w-12 h-12 bg-rose-50 text-rose-500 rounded-full flex items-center justify-center hover:scale-110 transition-transform shadow-lg"><i className="fas fa-times"></i></button>
              </div>

              <form onSubmit={handleAddExpense} className="space-y-8 uppercase">
                 <div className="space-y-2">
                    <label className="text-[10px] text-slate-400 ml-4 tracking-[4px]">Expense Title *</label>
                    <input
                      type="text"
                      placeholder="e.g. Bandwidth Bill July"
                      value={formData.title}
                      onChange={e => setFormData({...formData, title: e.target.value})}
                      className="w-full bg-white dark:bg-slate-800 p-5 rounded-3xl font-black text-lg outline-none shadow-inner border-2 border-slate-100 dark:border-slate-700 focus:border-rose-500/20 transition-all"
                      required
                    />
                 </div>

                 <div className="grid grid-cols-2 gap-6">
                    <div className="space-y-2">
                       <label className="text-[10px] text-slate-400 ml-4 tracking-[4px]">Category</label>
                       <select
                         value={formData.category}
                         onChange={e => setFormData({...formData, category: e.target.value})}
                         className="w-full bg-white dark:bg-slate-800 p-5 rounded-3xl font-black text-sm outline-none cursor-pointer border-2 border-slate-100 dark:border-slate-700"
                       >
                          {categories.map(c => <option key={c.id} value={c.id}>{c.label}</option>)}
                       </select>
                    </div>
                    <div className="space-y-2">
                       <label className="text-[10px] text-slate-400 ml-4 tracking-[4px]">Date</label>
                       <input
                         type="date"
                         value={formData.expenseDate}
                         onChange={e => setFormData({...formData, expenseDate: e.target.value})}
                         className="w-full bg-white dark:bg-slate-800 p-5 rounded-3xl font-black text-sm outline-none border-2 border-slate-100 dark:border-slate-700"
                       />
                    </div>
                 </div>

                 <div className="space-y-2">
                    <label className="text-[10px] text-slate-400 ml-4 tracking-[4px]">Amount (৳) *</label>
                    <input
                      type="number"
                      placeholder="0.00"
                      value={formData.amount}
                      onChange={e => setFormData({...formData, amount: e.target.value})}
                      className="w-full bg-slate-50 dark:bg-slate-900 p-8 rounded-[40px] font-black text-6xl text-rose-600 tracking-tighter text-center shadow-inner"
                      required
                    />
                 </div>

                 <div className="space-y-2">
                    <label className="text-[10px] text-slate-400 ml-4 tracking-[4px]">Notes (Optional)</label>
                    <textarea
                      placeholder="Enter details..."
                      value={formData.notes}
                      onChange={e => setFormData({...formData, notes: e.target.value})}
                      className="w-full bg-white dark:bg-slate-800 p-5 rounded-3xl font-black text-sm outline-none shadow-inner border-2 border-slate-100 dark:border-slate-700"
                    />
                 </div>

                 <button
                   type="submit"
                   disabled={isProcessing}
                   className="w-full bg-slate-900 text-white py-8 rounded-[40px] font-black uppercase tracking-[10px] shadow-2xl hover:scale-[1.02] active:scale-95 transition-all border-b-8 border-slate-700"
                 >
                    {isProcessing ? 'SAVING RECORD...' : 'COMMIT EXPENSE'}
                 </button>
              </form>
           </div>
        </div>
      )}

      {/* CATEGORY MANAGEMENT MODAL */}
      {showCategoryModal && (
        <div className="fixed inset-0 bg-slate-900/95 backdrop-blur-3xl z-[5000] flex items-center justify-center p-6 animate-fadeIn font-black uppercase text-slate-800 dark:text-white">
           <div className="bg-white dark:bg-slate-800 rounded-[56px] w-full max-w-lg p-10 shadow-2xl border-2 border-slate-100 dark:border-slate-700 space-y-8 relative overflow-hidden">
              <div className="absolute top-0 left-0 w-full h-3 bg-teal-500"></div>
              <div className="flex justify-between items-center border-b pb-6">
                 <h3 className="text-3xl font-black tracking-tighter">{t.expense_categories}</h3>
                 <button onClick={() => setShowCategoryModal(false)} className="text-rose-500 text-2xl hover:scale-110 transition-transform"><i className="fas fa-times-circle"></i></button>
              </div>

              <form onSubmit={handleAddCategory} className="flex space-x-3">
                 <input
                   type="text"
                   placeholder="New Category Name"
                   value={newCatName}
                   onChange={e => setNewCatName(e.target.value)}
                   className="flex-1 bg-slate-50 dark:bg-slate-900 p-5 rounded-2xl border-none outline-none font-black text-sm shadow-inner"
                 />
                 <button type="submit" className="bg-teal-600 text-white px-6 rounded-2xl shadow-lg"><i className="fas fa-plus"></i></button>
              </form>

              <div className="space-y-3 max-h-[300px] overflow-y-auto pr-2 custom-scrollbar">
                 {store.expenseCategories?.map(c => (
                   <div key={c.id} className="bg-slate-50 dark:bg-slate-900/50 p-5 rounded-2xl flex justify-between items-center border border-slate-100 dark:border-slate-800">
                      <span className="font-black text-xs tracking-widest">{c.name}</span>
                      <button onClick={() => handleCatDelete(c.id)} className="text-rose-400 hover:text-rose-600"><i className="fas fa-trash"></i></button>
                   </div>
                 ))}
                 {defaultCategories.map(c => (
                   <div key={c.id} className="bg-slate-50 dark:bg-slate-900/50 p-5 rounded-2xl flex justify-between items-center border border-slate-100 dark:border-slate-800 opacity-60">
                      <span className="font-black text-xs tracking-widest">{c.label}</span>
                      <span className="text-[8px] font-bold text-slate-400 italic">SYSTEM</span>
                   </div>
                 ))}
              </div>
              <button onClick={() => setShowCategoryModal(false)} className="w-full bg-[#0D9488] text-white py-6 rounded-3xl font-black uppercase tracking-[5px] shadow-2xl border-b-4 border-teal-900">DONE</button>
           </div>
        </div>
      )}
    </div>
  );
};

const StatBox = ({ label, value, color, bgColor }) => (
  <div className={`${bgColor} px-8 py-4 rounded-2xl flex flex-col items-center justify-center border border-slate-100 dark:border-slate-800 shadow-sm min-w-[150px]`}>
     <p className="text-[9px] text-slate-400 font-black uppercase tracking-[3px] mb-1">{label}</p>
     <p className={`text-2xl font-black ${color} tracking-tighter`}>{value}</p>
  </div>
);

export default Expenses;
