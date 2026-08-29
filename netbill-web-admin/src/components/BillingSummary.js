import React, { useState, useMemo, useEffect } from 'react';
import { supabase } from '../supabaseClient';

const BillingSummary = ({ store, initialCustomerId = null, isCustomerView = false, setActivePage }) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCust, setSelectedCust] = useState(() => {
    return store.customers.find(c => c.id === initialCustomerId) || null;
  });
  const [isImporting, setIsImporting] = useState(false);
  const [customerLedger, setCustomerLedger] = useState([]);
  const [isLoadingLedger, setIsLoadingLedger] = useState(false);

  useEffect(() => {
    if (selectedCust) {
        fetchLedger(selectedCust.id);
    } else {
        setCustomerLedger([]);
    }
  }, [selectedCust]);

  const fetchLedger = async (custId) => {
    setIsLoadingLedger(true);
    try {
        const { data, error } = await supabase
            .from('ledger_entries')
            .select('*')
            .eq('customer_id', custId)
            .order('date', { ascending: true });

        if (error) throw error;
        setCustomerLedger(data || []);
    } catch (e) {
        console.error("Ledger fetch error:", e);
    } finally {
        setIsLoadingLedger(false);
    }
  };

  const searchedCustomers = useMemo(() => {
    if (!searchQuery) return [];
    return store.customers.filter(c =>
      c.name?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      c.customerCode?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      c.mobile?.includes(searchQuery)
    ).slice(0, 8);
  }, [searchQuery, store.customers]);

  const ledgerData = useMemo(() => {
    return customerLedger;
  }, [customerLedger]);

  const handlePrint = () => window.print();

  // ADVANCED IMPORT ENGINE - SAVES TO SUPABASE PERMANENTLY
  const handleHistoryImport = (e) => {
    const file = e.target.files[0];
    if (!file || !selectedCust) return;

    const reader = new FileReader();
    reader.onload = async (evt) => {
      try {
        const text = new TextDecoder("utf-8").decode(evt.target.result);
        const lines = text.split(/\r?\n/).filter(l => l.trim() !== "");
        if (lines.length < 2) return alert("Invalid File Format!");

        setIsImporting(true);
        const headers = lines[0].split(',').map(h => h.trim().toLowerCase());
        const dataRows = lines.slice(1);

        const imports = dataRows.map((line, idx) => {
           const cols = line.split(',').map(c => c.trim());

           // Helper to find column index by multiple possible names
           const getVal = (keywords) => {
             const foundIdx = headers.findIndex(h => keywords.some(k => h.includes(k)));
             return foundIdx !== -1 ? cols[foundIdx] : null;
           };

           const date = getVal(['date', 'তারিখ']) || new Date().toISOString().split('T')[0];
           const rent = parseFloat(getVal(['rent', 'bill', 'ভাড়া'])) || 0;
           const paid = parseFloat(getVal(['paid', 'payment', 'জমা'])) || 0;
           const discount = parseFloat(getVal(['discount', 'ছাড়'])) || 0;
           const balance = parseFloat(getVal(['total due', 'balance', 'বকেয়া'])) || 0;
           const note = getVal(['note', 'description', 'মন্তব্য']) || 'Imported';
           const collector = getVal(['collector', 'by', 'কালেক্টর']) || 'jitushopnil';

           return {
             id: `IMP-LED-${Date.now()}-${idx}`,
             customer_id: selectedCust.id,
             date,
             time: '12:00 PM',
             type: paid > 0 ? 'Payment' : (discount > 0 ? 'Discount' : 'Monthly Bill'),
             description: note,
             amount: paid > 0 ? paid : (discount > 0 ? discount : rent), // Fallback for old apps
             monthly_rent: rent,
             discount_amount: discount,
             paid_amount: paid,
             total_due_balance: balance,
             collector_name: collector,
             is_debit: rent > 0 && paid === 0
           };
        });

        const { error } = await supabase.from('ledger_entries').insert(imports);
        if (error) throw error;
        alert(`Successfully saved ${imports.length} records to Supabase!`);
        window.location.reload();
      } catch (err) {
        alert("Import failed: " + err.message);
      } finally {
        setIsImporting(false);
      }
    };
    reader.readAsArrayBuffer(file);
  };

  return (
    <div className="w-full max-w-[98%] mx-auto space-y-10 pb-20 font-sans tracking-tight no-print">
      {!isCustomerView && (
        <div className="flex items-center space-x-4 mb-4">
           <button onClick={() => setActivePage('dashboard')} className="w-12 h-12 bg-white dark:bg-slate-800 rounded-2xl flex items-center justify-center text-[#20879e] shadow-sm border border-slate-100">
              <i className="fas fa-arrow-left"></i>
           </button>
           <h3 className="text-xl font-black uppercase tracking-[2px]">Billing Summary</h3>
        </div>
      )}

      {!isCustomerView && (
        <div className="bg-white dark:bg-slate-800 p-8 md:p-12 rounded-[48px] shadow-2xl border-2 border-slate-50 dark:border-slate-700 text-center space-y-8">
          <div className="max-w-3xl mx-auto space-y-6">
             <div className="relative group">
                <input
                  type="text"
                  placeholder="Enter Mobile / IP / Name / ID to Search Customer..."
                  value={searchQuery}
                  onChange={e => { setSearchQuery(e.target.value); }}
                  className="w-full p-6 md:p-8 rounded-[32px] border-4 border-slate-100 dark:border-slate-800 bg-slate-50 dark:bg-slate-900 outline-none font-black text-center text-xl md:text-3xl shadow-inner focus:border-teal-500/30 transition-all placeholder:opacity-30"
                />
                <div className="absolute right-8 top-1/2 -translate-y-1/2 text-slate-300 group-focus-within:text-teal-500 transition-colors">
                   <i className="fas fa-search text-3xl"></i>
                </div>
             </div>

             {/* Dynamic Search Results */}
             {searchQuery && !selectedCust && (
               <div className="grid grid-cols-1 md:grid-cols-2 gap-4 animate-fadeIn">
                  {searchedCustomers.map(c => (
                    <div
                      key={c.id}
                      onClick={() => { setSelectedCust(c); setSearchQuery(c.name); }}
                      className="bg-white dark:bg-slate-900 p-6 rounded-[24px] border-2 border-slate-100 dark:border-slate-800 hover:border-teal-500 cursor-pointer flex items-center space-x-6 shadow-sm hover:shadow-xl transition-all group"
                    >
                       <div className="w-16 h-16 bg-slate-50 dark:bg-slate-800 rounded-2xl flex items-center justify-center text-teal-600 group-hover:bg-teal-600 group-hover:text-white transition-all">
                          <i className="fas fa-user text-2xl"></i>
                       </div>
                       <div className="text-left">
                          <h4 className="text-xl font-black">{c.name}</h4>
                          <p className="text-[10px] text-slate-400 font-bold tracking-widest uppercase">ID: {c.customerCode} • {c.mobile}</p>
                       </div>
                    </div>
                  ))}
                  {searchedCustomers.length === 0 && (
                    <div className="col-span-full py-10 opacity-30">
                       <p className="text-xl font-black">No matching subscribers found</p>
                    </div>
                  )}
               </div>
             )}

             {selectedCust && (
               <button
                onClick={() => { setSelectedCust(null); setSearchQuery(''); }}
                className="bg-rose-50 text-rose-500 px-8 py-3 rounded-full font-black text-[10px] tracking-widest hover:bg-rose-500 hover:text-white transition-all uppercase"
               >
                 <i className="fas fa-times-circle mr-2"></i> Clear Selection & Search Again
               </button>
             )}
          </div>
        </div>
      )}

      {selectedCust && (
        <div id="printable-summary" className="bg-white dark:bg-slate-900 p-12 md:p-20 rounded-[64px] shadow-2xl border space-y-12 animate-fadeIn relative overflow-hidden">
          <div className="text-center">
             <h2 className="text-6xl font-black text-[#20879e] uppercase tracking-tighter mb-12 border-b-8 border-[#20879e]/10 inline-block pb-4">Customer summary</h2>
             <div className="text-left grid grid-cols-1 md:grid-cols-2 gap-16 font-bold text-slate-600 dark:text-slate-300 uppercase leading-relaxed text-base md:text-lg">
                <div className="space-y-2">
                   <p className="text-5xl md:text-7xl text-slate-900 dark:text-white mb-6 font-black">{selectedCust.name}</p>
                   <p className="text-xl md:text-2xl">{selectedCust.address || selectedCust.zone}, {selectedCust.mobile}</p>
                   <p>ID : <span className="text-[#20879e] font-black">{selectedCust.customerCode}</span></p>
                   <p>IP : {selectedCust.pppoeUsername}</p>
                   <p>PPPoE Name : {selectedCust.pppoeUsername}</p>
                </div>
                <div className="space-y-2 md:pt-24">
                   <p>PPPoE profile : <span className="text-teal-600">{selectedCust.packageName}</span></p>
                   <p>Expire Date : <span className="text-rose-500">{selectedCust.expireDate || selectedCust.expire_date}</span></p>
                   <p>Status : <span className={selectedCust.status === 'Active' ? 'text-emerald-500 font-black' : 'text-rose-500 font-black'}>{selectedCust.status === 'Active' ? 'ENABLE' : 'DISABLE'}</span></p>
                   <p>Connection Date : {selectedCust.joinDate}</p>
                   <p>Connected By : JITU</p>
                </div>
             </div>
          </div>

          <div className="flex justify-end items-center space-x-4 no-print border-t border-slate-100 dark:border-slate-800 pt-8">
             {!isCustomerView && (
               <div className="relative">
                  <input type="file" accept=".csv" onChange={handleHistoryImport} className="hidden" id="history-csv-final" />
                  <label htmlFor="history-csv-final" className="bg-[#f39c12] text-white px-8 py-3 rounded-xl font-black text-[10px] shadow-lg cursor-pointer hover:bg-amber-600 transition-all uppercase tracking-widest border-b-8 border-amber-800">
                     <i className="fas fa-file-import mr-2"></i> {isImporting ? 'Processing...' : 'IMPORT OLD HISTORY (CSV)'}
                  </label>
               </div>
             )}
             <button className="bg-[#20879e] text-white px-8 py-3 rounded-xl font-black text-[10px] shadow-lg border-b-8 border-[#16667a] uppercase tracking-widest">Download Excel</button>
             <button onClick={handlePrint} className="bg-[#20879e] text-white px-10 py-3 rounded-xl font-black text-[10px] shadow-lg border-b-8 border-[#16667a] uppercase tracking-widest">Print</button>
          </div>

          <div className="overflow-x-auto">
             <table className="w-full border-collapse border-2 border-slate-200 text-[13px] font-bold uppercase">
                <thead className="bg-[#f8f9fa] dark:bg-slate-800 text-slate-700 dark:text-slate-300">
                   <tr>
                      <th className="border-2 border-slate-200 p-4 w-32">Date</th>
                      <th className="border-2 border-slate-200 p-4">Monthly Rent</th>
                      <th className="border-2 border-slate-200 p-4">Additional</th>
                      <th className="border-2 border-slate-200 p-4">Discount</th>
                      <th className="border-2 border-slate-200 p-4">Advance</th>
                      <th className="border-2 border-slate-200 p-4">SUM</th>
                      <th className="border-2 border-slate-200 p-4">Vat%</th>
                      <th className="border-2 border-slate-200 p-4">Sum with Vat</th>
                      <th className="border-2 border-slate-200 p-4">Previous Due</th>
                      <th className="border-2 border-slate-200 p-4">Due</th>
                      <th className="border-2 border-slate-200 p-4">Paid Amount</th>
                      <th className="border-2 border-slate-200 p-4">Total Due</th>
                      <th className="border-2 border-slate-200 p-4 w-48">Note</th>
                   </tr>
                </thead>
                <tbody className="divide-y-2 divide-slate-200 font-bold">
                   {ledgerData.map((item, idx) => {
                     const isBill = item.type?.toLowerCase().includes('bill');
                     const isPayment = item.type?.toLowerCase().includes('payment');
                     const isDiscount = item.type?.toLowerCase().includes('discount');

                     // Mapping from camelCase (from App.js sync) or snake_case (direct)
                     const mRent = item.monthlyRent || item.monthly_rent || 0;
                     const pAmount = item.paidAmount || item.paid_amount || 0;
                     const dAmount = item.discountAmount || item.discount_amount || 0;
                     const aAmount = item.advanceAmount || item.advance_amount || 0;
                     const balance = item.totalDueBalance || item.total_due_balance || item.runningBalance || item.running_balance || 0;
                     const collector = item.collectorName || item.collector_name || 'jitushopnil';

                     return (
                       <tr key={item.id} className={`${idx % 2 === 0 ? 'bg-white' : 'bg-[#f4f7f6]/50'} hover:bg-teal-50 transition-colors`}>
                          <td className="border border-slate-200 p-4 text-center text-slate-500">{item.date}</td>
                          <td className="border border-slate-200 p-4 text-center">{isBill ? mRent : '0'}</td>
                          <td className="border border-slate-200 p-4 text-center">0</td>
                          <td className="border border-slate-200 p-4 text-center text-rose-600">{isDiscount ? dAmount : (dAmount || '0')}</td>
                          <td className="border border-slate-200 p-4 text-center text-indigo-600">{aAmount || '0'}</td>
                          <td className="border border-slate-200 p-4 text-center">{isBill ? mRent : '0'}</td>
                          <td className="border border-slate-200 p-4 text-center">0</td>
                          <td className="border border-slate-200 p-4 text-center">{isBill ? mRent : '0'}</td>
                          <td className="border border-slate-200 p-4 text-center">0</td>
                          <td className="border border-slate-200 p-4">
                             {isPayment ? (
                               <div className="text-right leading-tight">
                                  <span className="opacity-60 text-[10px]">Collected By :</span><br/>
                                  <span className="text-[#20879e] font-black italic">{collector}</span>
                               </div>
                             ) : <p className="text-center">{isBill ? mRent : '0'}</p>}
                          </td>
                          <td className="border border-slate-200 p-4 text-center text-emerald-600 font-black">{isPayment ? pAmount : ''}</td>
                          <td className="border border-slate-200 p-4 text-center font-black bg-slate-50/50">৳{Math.floor(balance)}</td>
                          <td className="border border-slate-200 p-4 text-[14px] normal-case text-slate-800 dark:text-slate-100 leading-relaxed font-bold min-w-[250px]">{item.description}</td>
                       </tr>
                     );
                   })}
                </tbody>
             </table>
          </div>
        </div>
      )}
    </div>
  );
};

export default BillingSummary;
