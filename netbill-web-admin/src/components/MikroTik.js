import React, { useState, useEffect, useMemo } from 'react';
import { supabase } from '../supabaseClient';

const MikroTik = ({ store, t, setActivePage }) => {
  const [selectedRouterId, setSelectedRouterId] = useState(null);
  const [search, setSearch] = useState('');
  const [filterType, setFilterType] = useState('ALL');
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [showAddModal, setShowAddModal] = useState(false);
  const [error, setError] = useState(null);

  // New Router Form State
  const [newRouter, setNewRouter] = useState({
    name: '', host: '', port: '8728', user: 'admin', pass: ''
  });

  // Health Metrics - Initialized as empty
  const [metrics, setMetrics] = useState({
    cpu: 0, ram: '0 / 0 GB', temp: 0, uptime: '---'
  });

  // Live Bandwidth
  const [rx, setRx] = useState(0);
  const [tx, setTx] = useState(0);
  const [sessions, setSessions] = useState([]);

  const routers = store.mikrotikRouters || [];

  // Logic to auto-select the first router if none selected
  const currentRouter = useMemo(() => {
    if (selectedRouterId) return routers.find(r => r.id === selectedRouterId);
    return routers.length > 0 ? routers[0] : null;
  }, [routers, selectedRouterId]);

  // Fetch data only if a router exists
  useEffect(() => {
    if (currentRouter) {
        fetchRouterData();
        const interval = setInterval(fetchRouterData, 5000); // Poll every 5s
        return () => clearInterval(interval);
    } else {
        setSessions([]);
        setRx(0); setTx(0);
        setMetrics({ cpu: 0, ram: '0 / 0 GB', temp: 0, uptime: '---' });
    }
  }, [currentRouter?.id]);

  const fetchRouterData = async () => {
    if (!currentRouter) return;
    try {
        const { data, error: invokeErr } = await supabase.functions.invoke('mikrotik-manager', {
            body: {
                routerId: currentRouter.id,
                action: 'get_status'
            }
        });

        if (invokeErr) throw invokeErr;

        if (data && data.success) {
            console.log("MikroTik Data Received:", data); // Detailed Log
            if (data.debug_logs && data.debug_logs.length > 0) {
                console.warn("MikroTik Debug Logs:", data.debug_logs);
            }
            setSessions(data.sessions || []);
            setMetrics(data.metrics || metrics);
            setRx(data.rx || 0);
            setTx(data.tx || 0);
            setError(null);
        } else {
            setError(data?.error || "Unable to reach Router");
        }
    } catch (e) {
        console.error("Fetch Router Error:", e);
        setError(`Connection Failed: ${e.message || "Edge Function Error"}`);
    }
  };

  const filteredSessions = useMemo(() => {
      if (!sessions || sessions.length === 0) return [];
      return sessions.filter(s => {
          const u = (s.username || '').toLowerCase();
          const n = (s.name || '').toLowerCase();
          const q = search.toLowerCase();
          const matchSearch = u.includes(q) || n.includes(q) || (s.ip || '').includes(search);
          const matchType = filterType === 'ALL' || (s.type || '').toUpperCase() === filterType;
          return matchSearch && matchType;
      });
  }, [sessions, search, filterType]);

  const handleAction = async (action, target) => {
      if (!currentRouter) return alert("Select a router first!");
      try {
          setIsRefreshing(true);
          const { data, error } = await supabase.functions.invoke('mikrotik-manager', {
              body: {
                  routerId: currentRouter.id,
                  action: action,
                  payload: target
              }
          });
          if (error) throw error;
          if (data && data.error) throw new Error(data.error);

          alert(`Success: ${action.toUpperCase()} command executed.`);
          fetchRouterData(); // Refresh metrics after action
      } catch (e) {
          console.error(`MikroTik Action (${action}) Failed:`, e);
          alert(`Error: ${e.message || "Operation failed"}`);
      } finally {
          setIsRefreshing(false);
      }
  };

  return (
    <div className="max-w-7xl mx-auto space-y-8 pb-20 uppercase font-black tracking-tighter">
      {/* Header */}
      <div className="flex justify-between items-center bg-white dark:bg-slate-800 p-8 rounded-[40px] shadow-xl border border-slate-100 dark:border-slate-700">
        <div className="flex items-center space-x-6">
           <div className={`w-16 h-16 ${error ? 'bg-rose-500' : 'bg-blue-600'} text-white rounded-2xl flex items-center justify-center text-3xl shadow-lg transition-colors`}>
              <i className={`fas ${error ? 'fa-exclamation-triangle' : 'fa-microchip'}`}></i>
           </div>
           <div>
              <h3 className="text-4xl font-black text-slate-800 dark:text-white leading-none">MikroTik Monitor</h3>
              <p className={`text-xs ${error ? 'text-rose-500' : 'text-blue-600'} font-bold tracking-[4px] mt-2 italic`}>
                {error ? `ERROR: ${error}` : 'Real-Time RouterBoard Engine'}
              </p>
           </div>
        </div>
        <div className="flex space-x-4">
           {routers.map(r => (
             <button
               key={r.id}
               onClick={() => { setSelectedRouterId(r.id); setError(null); }}
               className={`px-6 py-3 rounded-2xl font-black text-[10px] tracking-widest transition-all ${currentRouter?.id === r.id ? 'bg-blue-600 text-white shadow-xl scale-105' : 'bg-slate-100 dark:bg-slate-900 text-slate-400'}`}
             >
                {r.name}
             </button>
           ))}
           <button onClick={() => setShowAddModal(true)} className="w-12 h-12 bg-slate-100 dark:bg-slate-900 rounded-2xl flex items-center justify-center text-slate-400 hover:text-blue-600 transition-all border border-slate-200">
              <i className="fas fa-plus"></i>
           </button>
        </div>
      </div>

      {/* Add Router Modal - MOVED OUTSIDE FOR ALL STATES */}
      {showAddModal && (
        <div className="fixed inset-0 bg-slate-900/95 backdrop-blur-3xl z-[5000] flex items-center justify-center p-6 animate-fadeIn font-black uppercase">
          <div className="bg-white dark:bg-slate-800 rounded-[56px] w-full max-w-xl p-12 shadow-2xl border-4 border-blue-500/20 space-y-8 relative overflow-hidden">
             <div className="absolute top-0 left-0 w-full h-3 bg-blue-600"></div>
             <div className="flex justify-between items-center border-b pb-6">
                <h3 className="text-3xl font-black uppercase tracking-tighter">Add MikroTik Router</h3>
                <button onClick={() => setShowAddModal(false)} className="text-rose-500 text-2xl hover:scale-110 transition-all"><i className="fas fa-times-circle"></i></button>
             </div>
             <div className="space-y-6">
                <div className="space-y-2"><label className="text-[10px] text-slate-400 ml-4 tracking-[3px]">Router Name</label><input type="text" value={newRouter.name} onChange={e => setNewRouter({...newRouter, name: e.target.value})} className="w-full bg-slate-50 dark:bg-slate-900 p-5 rounded-3xl font-black text-sm outline-none border-none shadow-inner" placeholder="e.g. Core Router" required /></div>
                <div className="space-y-2"><label className="text-[10px] text-slate-400 ml-4 tracking-[3px]">IP / Host</label><input type="text" value={newRouter.host} onChange={e => setNewRouter({...newRouter, host: e.target.value})} className="w-full bg-slate-50 dark:bg-slate-900 p-5 rounded-3xl font-black text-sm outline-none border-none shadow-inner" placeholder="192.168.88.1" required /></div>
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2"><label className="text-[10px] text-slate-400 ml-4 tracking-[3px]">API Port</label><input type="number" value={newRouter.port} onChange={e => setNewRouter({...newRouter, port: e.target.value})} className="w-full bg-slate-50 dark:bg-slate-900 p-5 rounded-3xl font-black text-sm outline-none border-none shadow-inner" placeholder="8728" /></div>
                  <div className="space-y-2"><label className="text-[10px] text-slate-400 ml-4 tracking-[3px]">API User</label><input type="text" value={newRouter.user} onChange={e => setNewRouter({...newRouter, user: e.target.value})} className="w-full bg-slate-50 dark:bg-slate-900 p-5 rounded-3xl font-black text-sm outline-none border-none shadow-inner" placeholder="admin" required /></div>
                </div>
                <div className="space-y-2"><label className="text-[10px] text-slate-400 ml-4 tracking-[3px]">API Password</label><input type="password" value={newRouter.pass} onChange={e => setNewRouter({...newRouter, pass: e.target.value})} className="w-full bg-slate-50 dark:bg-slate-900 p-5 rounded-3xl font-black text-sm outline-none border-none shadow-inner" placeholder="********" /></div>
             </div>
             <button
                onClick={async () => {
                  try {
                    const { data, error } = await supabase.from('mikrotik_routers').insert({
                      name: newRouter.name,
                      host: newRouter.host,
                      port: parseInt(newRouter.port) || 8728,
                      api_user: newRouter.user,
                      api_pass: newRouter.pass
                    }).select();

                    if (error) {
                        console.error("Supabase Insert Error:", error);
                        alert(`Error: ${error.message || "Failed to save router. Please check permissions."}`);
                        return;
                    }

                    setShowAddModal(false);
                    setNewRouter({ name: '', host: '', port: '8728', user: 'admin', pass: '' });

                    // Manually trigger a refresh if Realtime is slow
                    if (window.refreshData) await window.refreshData();

                    alert("Router Saved Successfully!");
                  } catch (e) {
                    console.error("Save Router Exception:", e);
                    alert("Network Error: Unable to reach database.");
                  }
                }}
                className="w-full bg-blue-600 text-white py-6 rounded-3xl font-black uppercase tracking-[5px] shadow-2xl hover:scale-[1.02] active:scale-95 transition-all"
             >SAVE ROUTERBOARD</button>
          </div>
        </div>
      )}

      {routers.length === 0 ? (
        <div className="bg-white dark:bg-slate-800 p-20 rounded-[56px] shadow-2xl text-center border-2 border-dashed border-slate-200 dark:border-slate-700 opacity-50">
           <i className="fas fa-server text-[100px] text-slate-200 mb-10"></i>
           <h4 className="text-2xl font-black mb-4">No MikroTik Routers Configured</h4>
           <p className="text-xs tracking-widest">Click the + button above to add your first ISP RouterBOARD</p>
        </div>
      ) : (
      <>
        {/* Router Health Banner */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
          <HealthCard icon="fa-bolt" label="CPU LOAD" value={`${metrics.cpu}%`} color="text-teal-600" />
          <HealthCard icon="fa-memory" label="RAM USAGE" value={metrics.ram} color="text-indigo-600" />
          <HealthCard icon="fa-thermometer-half" label="TEMP" value={`${metrics.temp}°C`} color="text-amber-500" />
          <HealthCard icon="fa-clock" label="UPTIME" value={metrics.uptime} color="text-emerald-600" />
        </div>

        {/* Traffic Visualizer */}
        <div className="bg-white dark:bg-slate-800 p-10 rounded-[56px] shadow-2xl border border-slate-100 dark:border-slate-700">
          <div className="flex justify-between items-center mb-10">
              <h4 className="text-2xl font-black tracking-tighter uppercase"><i className="fas fa-chart-line mr-4 text-blue-600"></i>Live Bandwidth Feed</h4>
              <div className="flex items-center space-x-2 bg-emerald-50 text-emerald-600 px-4 py-2 rounded-xl text-[10px] font-black border border-emerald-100 animate-pulse">
                <div className="w-2 h-2 bg-emerald-600 rounded-full"></div>
                <span>LIVE 1s POLLING</span>
              </div>
          </div>
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-10">
              <TrafficBox label="DOWNLOAD (RX)" value={`${rx} MBPS`} icon="fa-arrow-down" color="blue" />
              <TrafficBox label="UPLOAD (TX)" value={`${tx} MBPS`} icon="fa-arrow-up" color="amber" />
          </div>
        </div>

        {/* Session Management */}
        <div className="space-y-6">
          <div className="flex flex-col md:flex-row justify-between items-center gap-6">
              <div className="flex space-x-2 bg-slate-100 dark:bg-slate-900 p-2 rounded-2xl">
                {['ALL', 'PPPOE', 'HOTSPOT', 'DHCP'].map(t => (
                  <button key={t} onClick={() => setFilterType(t)} className={`px-6 py-2 rounded-xl font-black text-[9px] tracking-widest transition-all ${filterType === t ? 'bg-white dark:bg-slate-800 text-blue-600 shadow-md' : 'text-slate-400'}`}>{t}</button>
                ))}
              </div>
              <div className="relative flex-1 max-w-md w-full">
                <input type="text" placeholder="Search Active Users..." value={search} onChange={e => setSearch(e.target.value)} className="w-full bg-white dark:bg-slate-800 p-4 pl-12 rounded-3xl border-none shadow-xl font-black text-sm outline-none focus:ring-2 focus:ring-blue-500 transition-all uppercase" />
                <i className="fas fa-search absolute left-5 top-4 text-slate-300"></i>
              </div>
          </div>

          <div className="grid grid-cols-1 gap-4">
              {filteredSessions.length > 0 ? filteredSessions.map((session, idx) => (
                <div key={session.id || idx} className="bg-white dark:bg-slate-800 p-6 rounded-[32px] shadow-lg border border-slate-100 dark:border-slate-700 flex flex-wrap justify-between items-center group hover:border-blue-500/30 transition-all">
                  <div className="flex items-center space-x-6 min-w-[300px]">
                      <div className={`w-14 h-14 rounded-2xl flex items-center justify-center text-xl shadow-inner ${session.type === 'PPPoE' ? 'bg-emerald-50 text-emerald-600' : 'bg-amber-50 text-amber-600'}`}><i className={`fas ${session.type === 'PPPoE' ? 'fa-wifi' : 'fa-broadcast-tower'}`}></i></div>
                      <div><h5 className="text-xl font-black text-slate-800 dark:text-white leading-none">{session.username}</h5><p className="text-[10px] text-slate-400 font-bold mt-2 tracking-widest uppercase">{session.name} • {session.ip} • {session.mac}</p></div>
                  </div>
                  <div className="flex items-center space-x-12">
                      <div className="text-center"><p className="text-[9px] text-slate-400 font-black tracking-widest">UPTIME</p><p className="text-sm font-black">{session.uptime}</p></div>
                      <div className="text-center"><p className="text-[9px] text-slate-400 font-black tracking-widest">RX/TX SPEED</p><p className="text-sm font-black text-blue-600">{session.rx}M / {session.tx}M</p></div>
                      <div className="flex space-x-2">
                        <button onClick={() => handleAction('kick', session)} className="bg-rose-50 text-rose-600 px-6 py-3 rounded-xl font-black text-[9px] tracking-widest border border-rose-100 hover:bg-rose-600 hover:text-white transition-all shadow-lg">KICK</button>
                        <button onClick={() => handleAction('throttle', session)} className="bg-amber-50 text-amber-600 px-6 py-3 rounded-xl font-black text-[9px] tracking-widest border border-amber-100 hover:bg-amber-600 hover:text-white transition-all shadow-lg">SPEED</button>
                      </div>
                  </div>
                </div>
              )) : (
                  <div className="bg-slate-50 dark:bg-slate-900/50 p-20 rounded-[40px] text-center italic text-slate-300 tracking-widest font-bold"><i className="fas fa-ghost text-4xl mb-4 opacity-20"></i><br/>No Active Sessions Found</div>
              )}
          </div>
        </div>
      </>
      )}
    </div>
  );
};

const HealthCard = ({ icon, label, value, color }) => (
  <div className="bg-white dark:bg-slate-800 p-6 rounded-[32px] shadow-xl border border-slate-100 dark:border-slate-700 text-center space-y-2">
     <i className={`fas ${icon} text-2xl ${color}`}></i>
     <p className="text-[9px] text-slate-400 font-black tracking-[3px] uppercase">{label}</p>
     <p className="text-xl font-black text-slate-800 dark:text-white">{value}</p>
  </div>
);

const TrafficBox = ({ label, value, icon, color }) => (
  <div className={`bg-${color}-50 dark:bg-${color}-900/10 p-8 rounded-[40px] border-2 border-${color}-100 dark:border-${color}-800 shadow-inner flex flex-col items-center space-y-4`}>
     <div className={`w-12 h-12 bg-white dark:bg-slate-800 rounded-full flex items-center justify-center text-${color}-600 shadow-md`}>
        <i className={`fas ${icon}`}></i>
     </div>
     <p className="text-[10px] text-slate-400 font-black tracking-[5px] uppercase">{label}</p>
     <h5 className={`text-4xl font-black text-${color}-600 tracking-tighter`}>{value}</h5>
  </div>
);

export default MikroTik;
