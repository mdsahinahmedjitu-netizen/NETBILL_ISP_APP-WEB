import React, { useState, useMemo } from 'react';
import { supabase } from '../supabaseClient';

const BillingSummary = ({ store, initialCustomerId = null }) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCust, setSelectedCust] = useState(() => {
    return store.customers.find(c => c.id === initialCustomerId) || null;
  });
  const [isImporting, setIsImporting] = useState(false);

  const searchedCustomers = useMemo(() => {
    if (!searchQuery) return [];
    return store.customers.filter(c =>
      c.name?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      c.customerCode?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      c.mobile?.includes(searchQuery)
    ).slice(0, 8);
  }, [searchQuery, store.customers]);

  const ledgerData = useMemo(() => {
    if (!selectedCust) return [];
    // Load directly from store, sorted by date
    return store.ledgerEntries?.filter(l => l.customerId === selectedCust.id || l.customer_id === selectedCust.id)
      .sort((a, b) => new Date(a.date) - new Date(b.date)) || [];
  }, [selectedCust, store.ledgerEntries]);

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
    <div className="w-full max-w-7xl mx-auto space-y-10 pb-20 font-sans tracking-tight no-print">

      {/* SEARCH INTERFACE - MATCHING IMAGE STYLE */}
      <div className="bg-white dark:bg-slate-800 p-12 rounded-[48px] shadow-2xl border-2 border-slate-50 dark:border-slate-700 text-center space-y-8">
        <div className="max-w-2xl mx-auto p-12 bg-slate-50 dark:bg-slate-900 rounded-[32px] border-2 border-slate-100 dark:border-slate-800 space-y-4 shadow-inner">
           <input type="text" placeholder="Enter Mobile / IP / Name / ID" value={searchQuery} onChange={e => { setSearchQuery(e.target.value); setSelectedCust(null); }} className="w-full p-4 rounded-xl border-2 border-slate-200 bg-white dark:bg-slate-800 outline-none font-bold text-center text-lg" />
           <select className="w-full p-4 rounded-xl border-2 border-slate-200 bg-white dark:bg-slate-800 outline-none font-bold text-center text-slate-400" value={selectedCust?.id || ''} onChange={(e) => { const cust = store.customers.find(c => c.id === e.target.value); if (cust) { setSelectedCust(cust); setSearchQuery(cust.name); } }}>
                <option value="">Select Customer</option>
                {searchedCustomers.map(c => <option key={c.id} value={c.id}>{c.name} (#{c.customerCode})</option>)}
           </select>
        </div>
      </div>

      {selectedCust && (
        <div id="printable-summary" className="bg-white dark:bg-slate-900 p-12 md:p-20 rounded-[64px] shadow-2xl border space-y-12 animate-fadeIn relative overflow-hidden">
          <div className="text-center">
             <h2 className="text-6xl font-black text-[#20879e] uppercase tracking-tighter mb-12 border-b-8 border-[#20879e]/10 inline-block pb-4">Customer summary</h2>
             <div className="text-left grid grid-cols-1 md:grid-cols-2 gap-16 font-bold text-slate-600 dark:text-slate-300 uppercase leading-relaxed text-sm">
                <div className="space-y-1">
                   <p className="text-5xl text-slate-900 dark:text-white mb-4 font-black">{selectedCust.name}</p>
                   <p className="text-lg">{selectedCust.address || selectedCust.zone}, {selectedCust.mobile}</p>
                   <p>ID : <span className="text-[#20879e] font-black">{selectedCust.customerCode}</span></p>
                   <p>IP : {selectedCust.pppoeUsername}</p>
                   <p>PPPoE Name : {selectedCust.pppoeUsername}</p>
                </div>
                <div className="space-y-1 md:pt-16">
                   <p>PPPoE profile : <span className="text-teal-600">{selectedCust.packageName}</span></p>
                   <p>Expire Date : <span className="text-rose-500">{selectedCust.expireDate || selectedCust.expire_date}</span></p>
                   <p>Status : <span className={selectedCust.status === 'Active' ? 'text-emerald-500 font-black' : 'text-rose-500 font-black'}>{selectedCust.status === 'Active' ? 'ENABLE' : 'DISABLE'}</span></p>
                   <p>Connection Date : {selectedCust.joinDate}</p>
                   <p>Connected By : JITU</p>
                </div>
             </div>
          </div>

          <div className="flex justify-end items-center space-x-4 no-print border-t border-slate-100 dark:border-slate-800 pt-8">
             <div className="relative">
                <input type="file" accept=".csv" onChange={handleHistoryImport} className="hidden" id="history-csv-final" />
                <label htmlFor="history-csv-final" className="bg-[#f39c12] text-white px-8 py-3 rounded-xl font-black text-[10px] shadow-lg cursor-pointer hover:bg-amber-600 transition-all uppercase tracking-widest border-b-8 border-amber-800">
                   <i className="fas fa-file-import mr-2"></i> {isImporting ? 'Processing...' : 'IMPORT OLD HISTORY (CSV)'}
                </label>
             </div>
             <button className="bg-[#20879e] text-white px-8 py-3 rounded-xl font-black text-[10px] shadow-lg border-b-8 border-[#16667a] uppercase tracking-widest">Download Excel</button>
             <button onClick={handlePrint} className="bg-[#20879e] text-white px-10 py-3 rounded-xl font-black text-[10px] shadow-lg border-b-8 border-[#16667a] uppercase tracking-widest">Print</button>
          </div>

          <div className="overflow-x-auto">
             <table className="w-full border-collapse border-2 border-slate-200 text-[10px] font-bold uppercase">
                <thead className="bg-[#f8f9fa] dark:bg-slate-800 text-slate-600 dark:text-slate-400">
                   <tr>
                      <th className="border border-slate-200 p-3 w-28">Date</th>
                      <th className="border border-slate-200 p-3">Monthly Rent</th>
                      <th className="border border-slate-200 p-3">Additional</th>
                      <th className="border border-slate-200 p-3">Discount</th>
                      <th className="border border-slate-200 p-3">Advance</th>
                      <th className="border border-slate-200 p-3">SUM</th>
                      <th className="border border-slate-200 p-3">Vat%</th>
                      <th className="border border-slate-200 p-3">Sum with Vat</th>
                      <th className="border border-slate-200 p-3">Previous Due</th>
                      <th className="border border-slate-200 p-3">Due</th>
                      <th className="border border-slate-200 p-3">Paid Amount</th>
                      <th className="border border-slate-200 p-3">Total Due</th>
                      <th className="border border-slate-200 p-3 w-40">Note</th>
                   </tr>
                </thead>
                <tbody className="divide-y divide-slate-200 font-bold">
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
                          <td className="border border-slate-200 p-3 text-center text-slate-400">{item.date}</td>
                          <td className="border border-slate-200 p-3 text-center">{isBill ? mRent : '0'}</td>
                          <td className="border border-slate-200 p-3 text-center">0</td>
                          <td className="border border-slate-200 p-3 text-center text-rose-500">{isDiscount ? dAmount : (dAmount || '0')}</td>
                          <td className="border border-slate-200 p-3 text-center text-indigo-500">{aAmount || '0'}</td>
                          <td className="border border-slate-200 p-3 text-center">{isBill ? mRent : '0'}</td>
                          <td className="border border-slate-200 p-3 text-center">0</td>
                          <td className="border border-slate-200 p-3 text-center">{isBill ? mRent : '0'}</td>
                          <td className="border border-slate-200 p-3 text-center">0</td>
                          <td className="border border-slate-200 p-3">
                             {isPayment ? (
                               <div className="text-right leading-tight">
                                  <span className="opacity-60 text-[8px]">Collected By :</span><br/>
                                  <span className="text-[#20879e] font-black italic">{collector}</span>
                               </div>
                             ) : <p className="text-center">{isBill ? mRent : '0'}</p>}
                          </td>
                          <td className="border border-slate-200 p-3 text-center text-emerald-600 font-black">{isPayment ? pAmount : ''}</td>
                          <td className="border border-slate-200 p-3 text-center font-black bg-slate-50/50">৳{Math.floor(balance)}</td>
                          <td className="border border-slate-200 p-3 text-[9px] normal-case text-slate-400 overflow-hidden leading-tight">{item.description}</td>
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
