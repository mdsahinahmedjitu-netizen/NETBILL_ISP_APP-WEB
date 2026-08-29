import React, { useState, useMemo, useEffect } from 'react';
import { supabase } from '../supabaseClient';

const UpdateBill = ({ store, setActivePage, t }) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCust, setSelectedCust] = useState(null);
  const [formData, setFormData] = useState({
    monthlyRent: 0,
    due: 0,
    additional: 0,
    discount: 0,
    advance: 0,
    vat: 0,
    note: ''
  });
  const [isSubmitting, setIsSubmitting] = useState(false);

  const searchedCustomers = useMemo(() => {
    if (!searchQuery) return [];
    return store.customers.filter(c =>
      c.name?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      c.customerCode?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      c.mobile?.includes(searchQuery)
    ).slice(0, 8);
  }, [searchQuery, store.customers]);

  useEffect(() => {
    if (selectedCust) {
      setFormData({
        monthlyRent: parseFloat(selectedCust.monthlyBill) || 0,
        due: parseFloat(selectedCust.currentDue) || 0,
        additional: 0,
        discount: 0,
        advance: parseFloat(selectedCust.advanceBalance) || 0,
        vat: 0,
        note: ''
      });
    }
  }, [selectedCust]);

  const calculateTotal = () => {
    const { monthlyRent, due, additional, discount, vat } = formData;
    const subtotal = monthlyRent + due + additional - discount;
    const vatAmt = (subtotal * vat) / 100;
    return subtotal + vatAmt;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!selectedCust) return alert("Please select a customer first!");

    setIsSubmitting(true);
    const totalPayable = calculateTotal();
    const todayISO = new Date().toLocaleDateString('en-CA');
    const timeStr = new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });

    try {
      // 1. Update Customer Current Due
      const { error: custError } = await supabase
        .from('customers')
        .update({
          current_due: totalPayable
        })
        .eq('id', selectedCust.id);

      if (custError) throw custError;

      // 2. Create Ledger Entry for Adjustment/Discount
      const { error: ledgerError } = await supabase
        .from('ledger_entries')
        .insert({
          customer_id: selectedCust.id,
          date: todayISO,
          time: timeStr,
          type: formData.discount > 0 ? "Discount" : "Adjustment",
          amount: formData.discount > 0 ? formData.discount : (formData.additional || formData.monthlyRent),
          is_debit: formData.additional > 0 || formData.monthlyRent > 0,
          description: formData.note || (formData.discount > 0 ? `Discount: Offline/Adjustment` : `Bill Updated`),
          discount_amount: formData.discount,
          total_due_balance: totalPayable
        });

      if (ledgerError) throw ledgerError;

      alert("Bill Updated Successfully!");
      setActivePage('billing_summary');
    } catch (error) {
      console.error("Update Error:", error);
      alert("Failed to update bill: " + error.message);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="max-w-7xl mx-auto space-y-10 pb-20 font-black tracking-tighter uppercase">
      {/* Header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-6 bg-white dark:bg-slate-800 p-8 md:p-12 rounded-[48px] shadow-2xl border border-slate-100 dark:border-slate-700">
        <div className="flex items-center space-x-6">
           <button onClick={() => setActivePage('dashboard')} className="w-16 h-16 bg-slate-50 dark:bg-slate-900 rounded-3xl flex items-center justify-center text-teal-600 hover:bg-teal-600 hover:text-white transition-all shadow-sm">
              <i className="fas fa-arrow-left text-xl"></i>
           </button>
           <div className="space-y-2">
              <h3 className="text-4xl md:text-6xl font-black text-slate-800 dark:text-white tracking-tighter leading-none">Update Bill</h3>
              <p className="text-[10px] text-teal-600 font-bold tracking-[5px]">Modify Billing, Apply Discounts & Notes</p>
           </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-10">
        {/* Left: Search & Select */}
        <div className="bg-white dark:bg-slate-800 p-10 rounded-[48px] shadow-2xl border-2 border-slate-50 dark:border-slate-700 space-y-8">
          <div className="space-y-4">
            <h4 className="text-2xl font-black text-slate-800 dark:text-white">Select Customer</h4>
            <div className="relative group">
              <input
                type="text"
                placeholder="Enter Mobile / IP / Name / ID"
                value={searchQuery}
                onChange={e => { setSearchQuery(e.target.value); setSelectedCust(null); }}
                className="w-full bg-slate-50 dark:bg-slate-950 p-6 rounded-[24px] font-black text-xl outline-none border-4 border-transparent focus:border-teal-500/20 shadow-inner transition-all placeholder:opacity-30"
              />
              <div className="absolute right-6 top-1/2 -translate-y-1/2 opacity-20 group-focus-within:opacity-100 transition-opacity">
                <i className="fas fa-search text-2xl text-teal-500"></i>
              </div>
            </div>

            <div className="space-y-3 max-h-[400px] overflow-y-auto custom-scrollbar">
              {searchedCustomers.map(c => (
                <div
                  key={c.id}
                  onClick={() => { setSelectedCust(c); setSearchQuery(c.name); }}
                  className={`p-6 rounded-[24px] border-2 transition-all cursor-pointer flex justify-between items-center ${selectedCust?.id === c.id ? 'border-teal-500 bg-teal-50 dark:bg-teal-900/20' : 'border-slate-50 dark:border-slate-800 bg-white dark:bg-slate-900 hover:border-teal-200'}`}
                >
                  <div>
                    <h5 className="text-xl font-black">{c.name}</h5>
                    <p className="text-[10px] text-slate-400 font-bold tracking-widest mt-1">ID: {c.customerCode} • {c.mobile}</p>
                  </div>
                  {selectedCust?.id === c.id && <i className="fas fa-check-circle text-teal-500 text-2xl"></i>}
                </div>
              ))}
              {searchQuery && searchedCustomers.length === 0 && (
                <p className="text-center py-10 text-slate-400 font-bold tracking-widest">No matching subscribers found</p>
              )}
            </div>
          </div>

          <button className="w-full py-5 rounded-[24px] border-2 border-dashed border-teal-500/30 text-teal-600 font-black text-[10px] tracking-[4px] hover:bg-teal-50 transition-all">
             <i className="fas fa-download mr-2"></i> DOWNLOAD FORM FOR UPDATE BILL
          </button>
        </div>

        {/* Right: Update Form */}
        <div className="bg-white dark:bg-slate-800 p-10 rounded-[48px] shadow-2xl border-2 border-slate-50 dark:border-slate-700 relative overflow-hidden">
          {!selectedCust ? (
            <div className="h-full flex flex-col items-center justify-center text-center space-y-6 opacity-20 py-20">
               <i className="fas fa-file-invoice-dollar text-[120px]"></i>
               <h3 className="text-3xl font-black">Select a customer<br/>to update bill</h3>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="space-y-8 animate-fadeIn">
               <div className="flex justify-between items-start border-b-2 border-slate-50 dark:border-slate-800 pb-6">
                  <div>
                    <h4 className="text-3xl font-black text-slate-800 dark:text-white">Update Last Bill</h4>
                    <p className="text-[10px] text-slate-400 font-bold tracking-widest mt-1">Invoice Modification Panel</p>
                  </div>
                  <div className="text-right">
                    <p className="text-[10px] text-slate-400 font-bold tracking-widest">ID : {selectedCust.customerCode}</p>
                    <p className="text-[10px] text-slate-400 font-bold tracking-widest">IP : {selectedCust.pppoeUsername}</p>
                  </div>
               </div>

               <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <div className="space-y-2">
                    <label className="text-[10px] text-slate-400 ml-4 tracking-[3px]">MONTHLY RENT *</label>
                    <input
                      type="number"
                      value={formData.monthlyRent}
                      onChange={e => setFormData({...formData, monthlyRent: parseFloat(e.target.value) || 0})}
                      className="w-full bg-slate-50 dark:bg-slate-900 p-5 rounded-3xl font-black text-lg outline-none border-none shadow-inner"
                    />
                  </div>
                  <div className="space-y-2">
                    <label className="text-[10px] text-slate-400 ml-4 tracking-[3px]">PREVIOUS DUE</label>
                    <input
                      type="number"
                      readOnly
                      value={formData.due}
                      className="w-full bg-slate-100 dark:bg-slate-950 p-5 rounded-3xl font-black text-lg outline-none border-none cursor-not-allowed opacity-60"
                    />
                  </div>
                  <div className="space-y-2">
                    <label className="text-[10px] text-slate-400 ml-4 tracking-[3px]">ADDITIONAL CHARGE</label>
                    <input
                      type="number"
                      value={formData.additional}
                      onChange={e => setFormData({...formData, additional: parseFloat(e.target.value) || 0})}
                      className="w-full bg-slate-50 dark:bg-slate-900 p-5 rounded-3xl font-black text-lg outline-none border-none shadow-inner"
                    />
                  </div>
                  <div className="space-y-2">
                    <label className="text-[10px] text-rose-500 ml-4 tracking-[3px]">DISCOUNT (ছাড়)</label>
                    <input
                      type="number"
                      value={formData.discount}
                      onChange={e => setFormData({...formData, discount: parseFloat(e.target.value) || 0})}
                      className="w-full bg-rose-50 dark:bg-rose-900/20 p-5 rounded-3xl font-black text-lg outline-none border-2 border-rose-100 dark:border-rose-900 shadow-inner text-rose-600"
                    />
                  </div>
                  <div className="space-y-2">
                    <label className="text-[10px] text-indigo-500 ml-4 tracking-[3px]">ADVANCE BALANCE</label>
                    <input
                      type="number"
                      readOnly
                      value={formData.advance}
                      className="w-full bg-slate-100 dark:bg-slate-950 p-5 rounded-3xl font-black text-lg outline-none border-none cursor-not-allowed opacity-60"
                    />
                  </div>
                  <div className="space-y-2">
                    <label className="text-[10px] text-slate-400 ml-4 tracking-[3px]">VAT %</label>
                    <input
                      type="number"
                      value={formData.vat}
                      onChange={e => setFormData({...formData, vat: parseFloat(e.target.value) || 0})}
                      className="w-full bg-slate-50 dark:bg-slate-900 p-5 rounded-3xl font-black text-lg outline-none border-none shadow-inner"
                    />
                  </div>
               </div>

               <div className="space-y-2">
                  <label className="text-[10px] text-teal-600 ml-4 tracking-[3px]">NOTE / REASON (কেন ডিসকাউন্ট বা আপডেট?)</label>
                  <textarea
                    placeholder="Write your note here..."
                    value={formData.note}
                    onChange={e => setFormData({...formData, note: e.target.value})}
                    className="w-full bg-slate-50 dark:bg-slate-900 p-6 rounded-[32px] font-black text-sm outline-none border-none shadow-inner h-32 resize-none"
                  ></textarea>
               </div>

               <div className="bg-slate-900 text-white p-10 rounded-[40px] shadow-2xl space-y-6">
                  <div className="flex justify-between items-center opacity-60">
                    <p className="text-[10px] tracking-[4px]">Subtotal Payable</p>
                    <p className="text-xl font-black">৳{(formData.monthlyRent + formData.due + formData.additional - formData.discount).toFixed(2)}</p>
                  </div>
                  <div className="flex justify-between items-center">
                    <p className="text-xl tracking-[5px]">Total Due</p>
                    <p className="text-5xl font-black text-teal-400">৳{calculateTotal().toFixed(2)}</p>
                  </div>
               </div>

               <button
                  disabled={isSubmitting}
                  className="w-full bg-teal-600 text-white py-8 rounded-[32px] font-black text-xl tracking-[8px] shadow-2xl shadow-teal-500/40 hover:scale-[1.02] active:scale-95 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
               >
                 {isSubmitting ? 'UPDATING...' : 'SUBMIT UPDATE'}
               </button>
            </form>
          )}
        </div>
      </div>
    </div>
  );
};

export default UpdateBill;
