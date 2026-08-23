import React, { useState, useMemo } from 'react';
import { supabase } from '../supabaseClient';

const SmsLogs = ({ store, setActivePage }) => {
  const [selectedIds, setSelectedIds] = useState([]);
  const [isDeleting, setIsDeleting] = useState(false);
  const [search, setSearch] = useState('');

  const logs = store.smsLogs || [];

  // Calculate Stats
  const stats = useMemo(() => {
      const total = logs.length;
      const sent = logs.filter(l => (l.status || '').toLowerCase().includes('sent')).length;
      const failed = logs.filter(l => (l.status || '').toLowerCase().includes('failed') || (l.status || '').toLowerCase().includes('error')).length;
      const pending = total - (sent + failed);
      const successRate = total > 0 ? Math.round((sent / total) * 100) : 0;
      return { total, sent, failed, pending, successRate };
  }, [logs]);

  const filteredLogs = logs.filter(log =>
    (log.customerName || log.customer_name || '').toLowerCase().includes(search.toLowerCase()) ||
    (log.mobile || '').includes(search) ||
    (log.notificationType || log.notification_type || '').toLowerCase().includes(search.toLowerCase()) ||
    (log.message || '').toLowerCase().includes(search.toLowerCase())
  ).sort((a, b) => {
      const dateA = new Date(a.sentTimestamp || a.sent_timestamp || 0);
      const dateB = new Date(b.sentTimestamp || b.sent_timestamp || 0);
      return dateB - dateA;
  });

  const toggleSelect = (id) => {
    setSelectedIds(prev =>
      prev.includes(id) ? prev.filter(i => i !== id) : [...prev, id]
    );
  };

  const selectAll = () => {
    if (selectedIds.length === filteredLogs.length) setSelectedIds([]);
    else setSelectedIds(filteredLogs.map(l => l.id));
  };

  const deleteSelected = async () => {
    if (selectedIds.length === 0) return;
    if (!window.confirm(`Delete ${selectedIds.length} logs from cloud?`)) return;
    setIsDeleting(true);
    try {
      await supabase.from('sms_logs').delete().in('id', selectedIds);
      setSelectedIds([]);
    } catch (e) { alert("Delete failed"); }
    finally { setIsDeleting(false); }
  };

  const wipeAll = async () => {
    if (!window.confirm("CRITICAL: Wipe ALL SMS history?")) return;
    setIsDeleting(true);
    try {
      await supabase.from('sms_logs').delete().neq('id', '0');
    } catch (e) { alert("Wipe failed"); }
    finally { setIsDeleting(false); }
  };

  return (
    <div className="w-full max-w-7xl mx-auto space-y-10 pb-20 uppercase font-black tracking-tighter transition-all">

      {/* DELIVERY REPORT SUMMARY */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
          <StatCard label="Total Broadcast" value={stats.total} icon="fa-paper-plane" color="slate" />
          <StatCard label="Delivered" value={stats.sent} icon="fa-check-double" color="emerald" />
          <StatCard label="Failed" value={stats.failed} icon="fa-exclamation-triangle" color="rose" />
          <div className="bg-indigo-600 p-8 rounded-[40px] text-white shadow-2xl space-y-2 relative overflow-hidden group">
              <div className="absolute top-0 right-0 w-24 h-24 bg-white/10 rounded-full -mr-10 -mt-10 group-hover:scale-110 duration-700"></div>
              <p className="text-[10px] font-black tracking-[4px] opacity-80">SUCCESS RATE</p>
              <h4 className="text-4xl font-black tracking-tighter">{stats.successRate}%</h4>
          </div>
      </div>

      <div className="flex flex-col md:flex-row justify-between items-start md:items-end gap-6 pt-4">
        <div className="flex items-center space-x-6">
           <button onClick={() => setActivePage('dashboard')} className="w-12 h-12 bg-white dark:bg-slate-800 rounded-2xl flex items-center justify-center text-slate-900 shadow-sm border border-slate-100">
              <i className="fas fa-arrow-left"></i>
           </button>
           <div className="space-y-2">
              <h3 className="text-5xl font-black text-slate-800 dark:text-white tracking-tighter leading-none">COMMUNICATION LOGS</h3>
              <p className="text-[10px] text-slate-400 tracking-[3px] font-black italic">Live Delivery Tracking • Gateway History</p>
           </div>
        </div>

        <div className="flex gap-4">
            <button onClick={wipeAll} disabled={isDeleting} className="bg-rose-50 text-rose-600 px-6 py-4 rounded-2xl font-black text-[9px] tracking-widest hover:bg-rose-600 hover:text-white transition-all">WIPE ALL</button>
            <button onClick={deleteSelected} disabled={selectedIds.length === 0} className={`px-8 py-4 rounded-2xl font-black text-[9px] tracking-widest transition-all shadow-xl ${selectedIds.length > 0 ? 'bg-rose-600 text-white' : 'bg-slate-100 text-slate-300'}`}>DELETE SELECTED</button>
        </div>
      </div>

      {/* SEARCH */}
      <div className="bg-white dark:bg-slate-800 p-4 px-8 rounded-[32px] shadow-xl border-2 border-slate-50 dark:border-slate-700 flex items-center space-x-6">
         <i className="fas fa-search text-slate-300 text-xl"></i>
         <input type="text" placeholder="SEARCH RECIPIENT OR MESSAGE..." value={search} onChange={(e) => setSearch(e.target.value)} className="flex-1 bg-transparent border-none outline-none font-black text-lg text-slate-800 dark:text-white placeholder:text-slate-300" />
      </div>

      {/* DATA TABLE */}
      <div className="bg-white dark:bg-slate-800 rounded-[48px] shadow-2xl border-2 border-slate-100 dark:border-slate-700 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="bg-slate-50 dark:bg-slate-900/50 border-b border-slate-100 dark:border-slate-700">
                <th className="p-8"><input type="checkbox" checked={selectedIds.length === filteredLogs.length} onChange={selectAll} className="w-5 h-5 rounded" /></th>
                <th className="p-8 text-[9px] font-black text-slate-400 tracking-[3px]">RECIPIENT</th>
                <th className="p-8 text-[9px] font-black text-slate-400 tracking-[3px]">MESSAGE</th>
                <th className="p-8 text-[9px] font-black text-slate-400 tracking-[3px]">TYPE</th>
                <th className="p-8 text-[9px] font-black text-slate-400 tracking-[3px]">STATUS</th>
                <th className="p-8 text-[9px] font-black text-slate-400 tracking-[3px]">DATE</th>
              </tr>
            </thead>
            <tbody>
              {filteredLogs.map((log) => (
                <tr key={log.id} className="border-b border-slate-50 dark:border-slate-700/50 hover:bg-slate-50/20 transition-all">
                  <td className="p-8"><input type="checkbox" checked={selectedIds.includes(log.id)} onChange={() => toggleSelect(log.id)} className="w-5 h-5 rounded" /></td>
                  <td className="p-8">
                    <p className="font-black text-slate-800 dark:text-white text-sm">{log.customerName || log.customer_name}</p>
                    <p className="text-[10px] text-teal-600 font-bold tracking-widest">{log.mobile}</p>
                  </td>
                  <td className="p-8 max-w-xs"><p className="text-[11px] text-slate-500 font-bold leading-tight">{log.message}</p></td>
                  <td className="p-8"><span className="bg-slate-100 dark:bg-slate-900 px-3 py-1.5 rounded-lg text-[8px] font-black text-slate-400 tracking-widest">{log.notificationType || log.notification_type}</span></td>
                  <td className="p-8">
                    <div className="flex items-center space-x-2">
                        <div className={`w-2 h-2 rounded-full ${(log.status || '').toLowerCase() === 'sent' ? 'bg-emerald-500' : 'bg-rose-500'}`}></div>
                        <span className={`text-[9px] font-black tracking-widest ${(log.status || '').toLowerCase() === 'sent' ? 'text-emerald-600' : 'text-rose-600'}`}>{log.status?.toUpperCase()}</span>
                    </div>
                  </td>
                  <td className="p-8 text-[10px] font-black text-slate-400">
                    {(() => {
                        const ts = log.sentTimestamp || log.sent_timestamp;
                        if (!ts) return 'N/A';
                        try {
                            const d = new Date(ts);
                            return d.toLocaleString('en-US', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit', hour12: true });
                        } catch(e) { return ts; }
                    })()}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

const StatCard = ({ label, value, icon, color }) => (
    <div className="bg-white dark:bg-slate-800 p-8 rounded-[40px] shadow-xl border-2 border-slate-50 dark:border-slate-700 space-y-3 relative overflow-hidden group">
        <div className={`absolute top-0 right-0 w-16 h-16 bg-${color}-500/5 rounded-full -mr-4 -mt-4 group-hover:scale-150 duration-700`}></div>
        <div className={`w-12 h-12 bg-${color}-50 dark:bg-${color}-900/20 rounded-2xl flex items-center justify-center text-${color}-600 text-xl`}><i className={`fas ${icon}`}></i></div>
        <div>
            <p className="text-[9px] text-slate-400 font-black tracking-[4px]">{label}</p>
            <h4 className={`text-3xl font-black tracking-tighter text-slate-800 dark:text-white`}>{value}</h4>
        </div>
    </div>
);

export default SmsLogs;
