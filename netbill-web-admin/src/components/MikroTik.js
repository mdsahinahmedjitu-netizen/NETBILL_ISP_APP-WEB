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

  // Health Metrics
  const [metrics, setMetrics] = useState({
    cpu: 0, ram: '---', temp: '---', uptime: '---'
  });

  const [rx, setRx] = useState(0);
  const [tx, setTx] = useState(0);
  const [sessions, setSessions] = useState([]);

  // Audit States
  const [auditResults, setAuditResults] = useState([]);
  const [isAuditing, setIsAuditing] = useState(false);
  const [showAuditModal, setShowAuditModal] = useState(false);

  const routers = store.mikrotikRouters || [];

  const currentRouter = useMemo(() => {
    if (selectedRouterId) return routers.find(r => r.id === selectedRouterId);
    return routers.length > 0 ? routers[0] : null;
  }, [routers, selectedRouterId]);

  useEffect(() => {
    if (currentRouter) {
        fetchRouterData();
        const interval = setInterval(fetchRouterData, 5000);
        return () => clearInterval(interval);
    }
  }, [currentRouter?.id]);

  const fetchRouterData = async () => {
    if (!currentRouter) return;
    try {
        const { data } = await supabase.functions.invoke('mikrotik-manager', {
            body: { routerId: currentRouter.id, action: 'get_status' }
        });

        if (data && data.success) {
            setSessions(data.sessions || []);
            setMetrics(data.metrics || metrics);
            setRx(data.rx || 0);
            setTx(data.tx || 0);
            setError(null);
        } else {
            setError(data?.error || "Unable to reach Router");
        }
    } catch (e) {
        setError(`Connection Failed: ${e.message}`);
    }
  };

  const runAudit = async () => {
    if (!currentRouter) return alert("Select a router!");
    setIsAuditing(true);

    try {
        const { data, error } = await supabase.functions.invoke('mikrotik-manager', {
            body: { action: 'get_secrets', routerId: currentRouter.id }
        });

        if (error) throw error;

        if (data && data.secrets) {
            const softUsers = new Set();
            store.customers?.forEach(c => {
                const p1 = (c.pppoeUsername || c.pppoe_username || '').toLowerCase().trim();
                const p2 = (c.mobile || '').toLowerCase().trim();
                const p3 = (c.customerCode || c.customer_code || '').toLowerCase().trim();
                if (p1) softUsers.add(p1);
                if (p2) softUsers.add(p2);
                if (p3) softUsers.add(p3);
            });

            const missing = data.secrets.filter(s => {
                const mkName = (s.name || '').toLowerCase().trim();
                return mkName && !softUsers.has(mkName);
            });

            setAuditResults(missing);
            setShowAuditModal(true);
        }
    } catch (e) {
        alert("Audit Error: " + e.message);
    } finally {
        setIsAuditing(false);
    }
  };

  const filteredSessions = useMemo(() => {
      if (!sessions || sessions.length === 0) return [];
      return sessions.filter(s => {
          const u = (s.username || '').toLowerCase();
          const q = search.toLowerCase();
          const matchSearch = u.includes(q);
          let matchType = filterType === 'ALL' || (s.type || '').toUpperCase() === filterType;
          if (filterType === 'UNKNOWN') {
              const isEntered = store.customers?.some(c => (c.pppoeUsername || c.pppoe_username || '').toLowerCase() === u);
              matchType = !isEntered;
          }
          return matchSearch && matchType;
      });
  }, [sessions, search, filterType, store.customers]);

  const sessionStats = useMemo(() => {
    const total = sessions.length;
    const unknown = sessions.filter(s => {
        const u = (s.username || '').toLowerCase();
        return !store.customers?.some(c => (c.pppoeUsername || c.pppoe_username || '').toLowerCase() === u);
    }).length;
    return { total, unknown };
  }, [sessions, store.customers]);

  const handleAction = async (action, target) => {
      if (!currentRouter) return;
      setIsRefreshing(true);
      try {
          await supabase.functions.invoke('mikrotik-manager', {
              body: { routerId: currentRouter.id, action: action, payload: target }
          });
          fetchRouterData();
      } catch (e) { alert(e.message); }
      finally { setIsRefreshing(false); }
  };

  return (
    <div className="max-w-7xl mx-auto space-y-8 pb-20 uppercase font-black tracking-tighter">
      {/* Header */}
      <div className="flex justify-between items-center bg-white dark:bg-slate-800 p-8 rounded-[40px] shadow-xl border border-slate-100 dark:border-slate-700">
        <div className="flex items-center space-x-6">
           <div className={`w-16 h-16 ${error ? 'bg-rose-500' : 'bg-blue-600'} text-white rounded-2xl flex items-center justify-center text-3xl shadow-lg`}>
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
             <button key={r.id} onClick={() => setSelectedRouterId(r.id)} className={`px-6 py-3 rounded-2xl font-black text-[10px] tracking-widest ${currentRouter?.id === r.id ? 'bg-blue-600 text-white' : 'bg-slate-100 dark:bg-slate-900'}`}>{r.name}</button>
           ))}
           <button onClick={() => setShowAddModal(true)} className="w-12 h-12 bg-slate-100 dark:bg-slate-900 rounded-2xl flex items-center justify-center"><i className="fas fa-plus"></i></button>
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-6">
        <HealthCard icon="fa-bolt" label="CPU LOAD" value={`${metrics.cpu}%`} color="text-teal-600" />
        <HealthCard icon="fa-memory" label="RAM USAGE" value={metrics.ram} color="text-indigo-600" />
        <HealthCard icon="fa-thermometer-half" label="TEMP" value={`${metrics.temp}°C`} color="text-amber-500" />
        <HealthCard icon="fa-clock" label="UPTIME" value={metrics.uptime} color="text-emerald-600" />
        <HealthCard icon="fa-users" label="TOTAL ONLINE" value={sessionStats.total} color="text-blue-600" />
        <HealthCard icon="fa-user-secret" label="UNKNOWN SESS" value={sessionStats.unknown} color="text-rose-600" />
      </div>

      {/* Traffic */}
      <div className="bg-white dark:bg-slate-800 p-10 rounded-[56px] shadow-2xl border border-slate-100 dark:border-slate-700">
        <div className="flex justify-between items-center mb-10">
            <h4 className="text-2xl font-black uppercase"><i className="fas fa-chart-line mr-4 text-blue-600"></i>Live Bandwidth Feed</h4>
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

      {/* Controls */}
      <div className="flex flex-col md:flex-row justify-between items-center gap-6">
          <div className="flex space-x-2 bg-slate-100 dark:bg-slate-900 p-2 rounded-2xl">
            {['ALL', 'PPPOE', 'UNKNOWN'].map(t => (
              <button key={t} onClick={() => setFilterType(t)} className={`px-6 py-2 rounded-xl font-black text-[9px] tracking-widest ${filterType === t ? 'bg-white shadow-md text-blue-600' : 'text-slate-400'}`}>{t}</button>
            ))}
          </div>
          <button onClick={runAudit} disabled={isAuditing} className="bg-indigo-600 text-white px-8 py-3 rounded-2xl font-black text-[10px] tracking-widest shadow-xl flex items-center space-x-3">
            <i className={`fas ${isAuditing ? 'fa-sync-alt fa-spin' : 'fa-shield-alt'}`}></i>
            <span>{isAuditing ? 'AUDITING...' : 'AUDIT ALL SECRETS'}</span>
          </button>
      </div>

      {/* List */}
      <div className="grid grid-cols-1 gap-4">
          {filteredSessions.map((session, idx) => (
            <div key={idx} className="bg-white dark:bg-slate-800 p-6 rounded-[32px] shadow-lg border border-slate-100 dark:border-slate-700 flex justify-between items-center group">
              <div className="flex items-center space-x-6">
                  <div className="w-14 h-14 rounded-2xl bg-emerald-50 text-emerald-600 flex items-center justify-center text-xl"><i className="fas fa-wifi"></i></div>
                  <div>
                    <h5 className="text-xl font-black text-slate-800 dark:text-white leading-none">
                      {session.username}
                      {!store.customers?.some(c => (c.pppoeUsername || c.pppoe_username || '').toLowerCase() === (session.username || '').toLowerCase()) &&
                        <span className="ml-3 bg-rose-600 text-white px-2 py-0.5 rounded text-[8px] animate-bounce">NOT IN SOFTWARE</span>
                      }
                    </h5>
                    <p className="text-[10px] text-slate-400 font-bold mt-2 tracking-widest uppercase">{session.ip || '---'}</p>
                  </div>
              </div>
              <button onClick={() => handleAction('kick', session)} className="bg-rose-50 text-rose-600 px-6 py-3 rounded-xl font-black text-[9px] tracking-widest">KICK</button>
            </div>
          ))}
      </div>

      {/* Audit Modal */}
      {showAuditModal && (
        <div className="fixed inset-0 bg-slate-900/95 backdrop-blur-3xl z-[6000] flex items-center justify-center p-6 animate-fadeIn font-black">
          <div className="bg-white dark:bg-slate-800 rounded-[56px] w-full max-w-4xl p-12 shadow-2xl border-4 border-indigo-500/20 flex flex-col max-h-[90vh]">
             <div className="flex justify-between items-center border-b pb-6 mb-6">
                <div>
                   <h3 className="text-3xl font-black uppercase tracking-tighter">Router Secrets Audit</h3>
                   <p className="text-[10px] text-slate-400 font-bold tracking-[3px]">Found {auditResults.length} users in MikroTik but not in NetBill Software</p>
                </div>
                <button onClick={() => setShowAuditModal(false)} className="text-rose-500 text-2xl"><i className="fas fa-times-circle"></i></button>
             </div>
             <div className="flex-1 overflow-y-auto space-y-4 pr-4 custom-scrollbar">
                {auditResults.map((s, idx) => (
                    <div key={idx} className="bg-slate-50 dark:bg-slate-900/50 p-6 rounded-[32px] border-2 border-slate-100 flex justify-between items-center">
                       <div className="flex items-center space-x-6">
                          <div className={`w-12 h-12 rounded-xl flex items-center justify-center text-xl ${s.disabled === 'true' || s.disabled === true ? 'bg-rose-50 text-rose-500' : 'bg-emerald-50 text-emerald-600'}`}>
                             <i className={`fas ${s.disabled === 'true' || s.disabled === true ? 'fa-user-slash' : 'fa-user-check'}`}></i>
                          </div>
                          <div><h4 className="text-lg font-black text-slate-800 dark:text-white leading-none">{s.name}</h4><p className="text-[9px] text-slate-400 font-bold mt-2 tracking-widest uppercase">Secret in Router</p></div>
                       </div>
                       <span className={`px-4 py-1.5 rounded-full text-[8px] font-black ${s.disabled === 'true' || s.disabled === true ? 'bg-rose-100 text-rose-700' : 'bg-emerald-100 text-emerald-700'}`}>
                          {s.disabled === 'true' || s.disabled === true ? 'DISABLED IN ROUTER' : 'ENABLED IN ROUTER'}
                       </span>
                    </div>
                ))}
             </div>
             <button onClick={() => setShowAuditModal(false)} className="w-full bg-slate-900 text-white py-6 rounded-3xl font-black uppercase mt-6">CLOSE</button>
          </div>
        </div>
      )}
    </div>
  );
};

const HealthCard = ({ icon, label, value, color }) => (
  <div className="bg-white dark:bg-slate-800 p-6 rounded-[32px] shadow-xl border border-slate-100 text-center space-y-2">
     <i className={`fas ${icon} text-2xl ${color}`}></i>
     <p className="text-[9px] text-slate-400 font-black tracking-[3px] uppercase">{label}</p>
     <p className="text-xl font-black text-slate-800 dark:text-white">{value}</p>
  </div>
);

const TrafficBox = ({ label, value, icon, color }) => (
  <div className={`bg-${color}-50 dark:bg-${color}-900/10 p-8 rounded-[40px] border-2 border-${color}-100 flex flex-col items-center space-y-4`}>
     <div className={`w-12 h-12 bg-white dark:bg-slate-800 rounded-full flex items-center justify-center text-${color}-600 shadow-md`}><i className={`fas ${icon}`}></i></div>
     <p className="text-[10px] text-slate-400 font-black tracking-[5px] uppercase">{label}</p>
     <h5 className={`text-4xl font-black text-${color}-600 tracking-tighter`}>{value}</h5>
  </div>
);

export default MikroTik;
