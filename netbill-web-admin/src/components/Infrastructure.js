import React, { useState } from 'react';
import { db } from '../firebaseConfig';
import { collection, addDoc, doc, deleteDoc, updateDoc } from 'firebase/firestore';

const Infrastructure = ({ store, t }) => {
  const [activeTab, setActiveTab] = useState('zones'); // 'zones', 'subzones', 'boxes'
  const [name, setName] = useState('');
  const [parentId, setParentId] = useState(''); // Zone for SubZone, SubZone for Box
  const [isSyncing, setIsSyncing] = useState(false);
  const [search, setSearch] = useState('');

  const handleAdd = async (e) => {
    e.preventDefault();
    if (!name) return;
    try {
      if (activeTab === 'zones') {
        await addDoc(collection(db, "zones"), { name });
      } else if (activeTab === 'subzones') {
        await addDoc(collection(db, "sub_zones"), { name, zoneId: parentId });
      } else if (activeTab === 'boxes') {
        await addDoc(collection(db, "boxes"), { name, subZoneId: parentId });
      }
      setName('');
      setParentId('');
      alert("Added successfully!");
    } catch (e) {
      alert("Error adding item");
    }
  };

  const handleSyncZones = async () => {
    if (!window.confirm("This will create new Zone entries for all unique names found in your Customer list. Continue?")) return;
    setIsSyncing(true);
    try {
       const uniqueZoneNames = [...new Set(store.customers.map(c => c.zone))].filter(z => z && z.trim() !== '');
       const existingNames = store.zones.map(z => z.name.toLowerCase());

       let count = 0;
       for (const zoneName of uniqueZoneNames) {
          if (!existingNames.includes(zoneName.toLowerCase())) {
             await addDoc(collection(db, "zones"), { name: zoneName });
             count++;
          }
       }
       alert(`Sync Complete! Added ${count} new zones.`);
    } catch (err) {
       alert("Sync failed.");
    } finally {
       setIsSyncing(false);
    }
  };

  const handleUpdate = async (id, newName, coll) => {
    try {
       const ref = doc(db, coll, id);
       const oldItem = (activeTab === 'zones' ? store.zones : activeTab === 'subzones' ? store.subZones : store.boxes).find(i => i.id === id);
       const oldName = oldItem?.name;

       await updateDoc(ref, { name: newName });

       // If a Zone name is updated, also update all customers in that zone
       if (coll === 'zones' && oldName) {
          const customersInZone = store.customers.filter(c => c.zone === oldName);
          for (const customer of customersInZone) {
             const custRef = doc(db, "customers", customer.id);
             await updateDoc(custRef, { zone: newName });
          }
          console.log(`Updated ${customersInZone.length} customers from ${oldName} to ${newName}`);
       }

       alert("Update successful!");
    } catch (e) {
       console.error("Update Error:", e);
       alert("Update failed.");
    }
  };

  const handleDelete = async (id, coll) => {
    if (window.confirm("Delete permanently?")) {
      await deleteDoc(doc(db, coll, id));
    }
  };

  return (
    <div className="max-w-6xl mx-auto space-y-12 pb-20 uppercase font-black tracking-tighter transition-all">
      <div className="flex justify-between items-start">
        <div className="space-y-2 uppercase">
          <h3 className="text-6xl font-black text-slate-800 dark:text-white tracking-tighter uppercase leading-none">Network Assets</h3>
          <p className="text-xs text-teal-600 tracking-widest font-black uppercase italic">Zones • Sub-Zones • Distribution Boxes</p>
        </div>
        {activeTab === 'zones' && (
          <button
            onClick={handleSyncZones}
            disabled={isSyncing}
            className="bg-indigo-600 text-white px-8 py-4 rounded-2xl shadow-xl font-black text-xs tracking-widest hover:scale-105 active:scale-95 transition-all flex items-center space-x-3"
          >
             <i className={`fas ${isSyncing ? 'fa-sync fa-spin' : 'fa-magic'}`}></i>
             <span>{isSyncing ? 'SYNCING...' : 'SYNC FROM CUSTOMERS'}</span>
          </button>
        )}
      </div>

      <div className="flex flex-col md:flex-row justify-between items-center gap-6">
        <div className="flex space-x-4 bg-slate-100 dark:bg-slate-900 p-2 rounded-3xl w-fit shadow-inner">
           <TabButton label="Zones" active={activeTab === 'zones'} onClick={() => { setActiveTab('zones'); setSearch(''); }} color="teal" />
           <TabButton label="Sub-Zones" active={activeTab === 'subzones'} onClick={() => { setActiveTab('subzones'); setSearch(''); }} color="indigo" />
           <TabButton label="Boxes" active={activeTab === 'boxes'} onClick={() => { setActiveTab('boxes'); setSearch(''); }} color="rose" />
        </div>

        <div className="relative flex-1 max-w-md w-full group">
          <input
            type="text"
            placeholder={`Search ${activeTab}...`}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full pl-12 pr-6 py-4 bg-white dark:bg-slate-800 rounded-3xl border-2 border-transparent focus:border-teal-500 shadow-xl font-black text-lg outline-none transition-all uppercase placeholder:opacity-30"
          />
          <i className="fas fa-search absolute left-5 top-5 text-slate-300 text-xl group-focus-within:text-teal-500 transition-colors"></i>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-12">
        {/* ADD FORM */}
        <div className="bg-white dark:bg-slate-800 p-10 rounded-[56px] shadow-2xl border border-slate-100 dark:border-slate-700 h-fit space-y-8">
           <h4 className="text-2xl font-black uppercase tracking-tighter border-b pb-4">Add {activeTab.slice(0, -1)}</h4>
           <form onSubmit={handleAdd} className="space-y-6">
              {activeTab === 'subzones' && (
                <div className="space-y-2">
                   <label className="text-[10px] text-slate-400 ml-4 tracking-[4px]">Parent Zone</label>
                   <select value={parentId} onChange={e => setParentId(e.target.value)} className="w-full bg-slate-50 dark:bg-slate-900 p-5 rounded-3xl font-black text-sm outline-none border-none" required>
                      <option value="">Select Zone</option>
                      {store.zones?.map(z => <option key={z.id} value={z.id}>{z.name}</option>)}
                   </select>
                </div>
              )}
              {activeTab === 'boxes' && (
                <div className="space-y-2">
                   <label className="text-[10px] text-slate-400 ml-4 tracking-[4px]">Parent Sub-Zone</label>
                   <select value={parentId} onChange={e => setParentId(e.target.value)} className="w-full bg-slate-50 dark:bg-slate-900 p-5 rounded-3xl font-black text-sm outline-none border-none" required>
                      <option value="">Select Sub-Zone</option>
                      {store.subZones?.map(sz => <option key={sz.id} value={sz.id}>{sz.name}</option>)}
                   </select>
                </div>
              )}
              <div className="space-y-2">
                 <label className="text-[10px] text-slate-400 ml-4 tracking-[4px]">{activeTab.slice(0, -1).toUpperCase()} Name</label>
                 <input type="text" value={name} onChange={e => setName(e.target.value)} className="w-full bg-slate-50 dark:bg-slate-900 p-5 rounded-3xl font-black text-lg shadow-inner outline-none border-none" placeholder="Enter Name..." required />
              </div>
              <button type="submit" className="w-full bg-slate-900 text-white py-6 rounded-3xl font-black uppercase tracking-[10px] shadow-2xl hover:scale-[1.02] active:scale-95 transition-all border-b-8 border-slate-700">
                ADD ASSET
              </button>
           </form>
        </div>

        {/* LIST */}
        <div className="lg:col-span-2 space-y-6">
           {activeTab === 'zones' && store.zones?.filter(z => z.name.toLowerCase().includes(search.toLowerCase())).map(z => (
             <AssetItem key={z.id} item={z} sub={`${store.subZones?.filter(sz => sz.zoneId === z.id).length || 0} Sub-Zones`} icon="fa-map-location-dot" color="text-teal-600" onUpdate={(n) => handleUpdate(z.id, n, 'zones')} onDelete={() => handleDelete(z.id, 'zones')} />
           ))}
           {activeTab === 'subzones' && store.subZones?.filter(sz => sz.name.toLowerCase().includes(search.toLowerCase())).map(sz => (
             <AssetItem key={sz.id} item={sz} sub={`Zone: ${store.zones?.find(z => z.id === sz.zoneId)?.name || 'N/A'}`} icon="fa-layer-group" color="text-indigo-600" onUpdate={(n) => handleUpdate(sz.id, n, 'sub_zones')} onDelete={() => handleDelete(sz.id, 'sub_zones')} />
           ))}
           {activeTab === 'boxes' && store.boxes?.filter(b => b.name.toLowerCase().includes(search.toLowerCase())).map(b => (
             <AssetItem key={b.id} item={b} sub={`Sub-Zone: ${store.subZones?.find(sz => sz.id === b.subZoneId)?.name || 'N/A'}`} icon="fa-box" color="text-rose-600" onUpdate={(n) => handleUpdate(b.id, n, 'boxes')} onDelete={() => handleDelete(b.id, 'boxes')} />
           ))}
           {((activeTab === 'zones' && !store.zones?.filter(z => z.name.toLowerCase().includes(search.toLowerCase())).length) ||
             (activeTab === 'subzones' && !store.subZones?.filter(sz => sz.name.toLowerCase().includes(search.toLowerCase())).length) ||
             (activeTab === 'boxes' && !store.boxes?.filter(b => b.name.toLowerCase().includes(search.toLowerCase())).length)) && (
             <div className="text-center py-40 opacity-10">
                <i className="fas fa-network-wired text-[100px]"></i>
                <p className="text-2xl mt-10">No Assets Configured</p>
             </div>
           )}
        </div>
      </div>
    </div>
  );
};

const TabButton = ({ label, active, onClick, color }) => (
  <button onClick={onClick} className={`px-10 py-3 rounded-2xl font-black text-xs transition-all ${active ? `bg-white dark:bg-slate-800 text-${color}-600 shadow-lg` : 'text-slate-400 hover:text-slate-600'}`}>
    {label}
  </button>
);

const AssetItem = ({ item, sub, icon, color, onDelete, onUpdate }) => {
  const [isEditing, setIsEditing] = useState(false);
  const [tempName, setTempName] = useState(item.name);

  const saveEdit = () => {
     onUpdate(tempName);
     setIsEditing(false);
  };

  return (
    <div className="bg-white dark:bg-slate-800 p-6 rounded-3xl shadow-xl border border-slate-100 dark:border-slate-700 flex justify-between items-center group hover:-translate-y-1 transition-all">
      <div className="flex items-center space-x-6 flex-1">
         <div className={`w-16 h-16 bg-slate-50 dark:bg-slate-900 rounded-2xl flex items-center justify-center text-2xl ${color} shadow-inner`}>
            <i className={`fas ${icon}`}></i>
         </div>
         <div className="flex-1">
            {isEditing ? (
               <div className="flex items-center space-x-3">
                  <input
                    autoFocus
                    type="text"
                    value={tempName}
                    onChange={e => setTempName(e.target.value)}
                    className="bg-slate-50 dark:bg-slate-900 border-2 border-teal-500 p-2 rounded-xl font-black text-xl w-full outline-none"
                  />
                  <button onClick={saveEdit} className="bg-emerald-500 text-white p-3 rounded-xl shadow-lg"><i className="fas fa-check"></i></button>
               </div>
            ) : (
               <>
                  <h4 className="text-xl font-black text-slate-800 dark:text-white leading-none">{item.name}</h4>
                  <p className="text-[10px] text-slate-400 font-bold uppercase mt-2 tracking-widest">{sub}</p>
               </>
            )}
         </div>
      </div>
      <div className="flex items-center space-x-2 ml-4">
         {!isEditing && (
            <button onClick={() => setIsEditing(true)} className="w-12 h-12 bg-indigo-50 text-indigo-500 rounded-2xl flex items-center justify-center opacity-0 group-hover:opacity-100 transition-all hover:bg-indigo-500 hover:text-white shadow-lg">
               <i className="fas fa-edit"></i>
            </button>
         )}
         <button onClick={onDelete} className="w-12 h-12 bg-rose-50 text-rose-500 rounded-2xl flex items-center justify-center opacity-0 group-hover:opacity-100 transition-all hover:bg-rose-500 hover:text-white shadow-lg">
            <i className="fas fa-trash-alt"></i>
         </button>
      </div>
    </div>
  );
};

export default Infrastructure;
