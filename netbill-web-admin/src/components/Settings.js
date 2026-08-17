import React, { useState, useEffect } from 'react';
import { db } from '../firebaseConfig';
import { supabase } from '../supabaseClient';
import { doc, onSnapshot, getDocs, collection } from 'firebase/firestore';

const Settings = ({ store, t, lang }) => {
  const [isProcessing, setIsProcessing] = useState(false);
  const [migrating, setMigrating] = useState(false);
  const [migStatus, setMigStatus] = useState('');

  const [settings, setSettings] = useState({
    id: 1,
    companyName: 'NetBill ISP', companyAddress: '', companyPhone: '', monthlyTarget: 0,
    smsApiUrl: '', smsApiKey: '', smsSenderId: '', apiMode: 'Production',
    bkashAppKey: '', bkashAppSecret: '', bkashUsername: '', bkashPassword: '',
    nagadMerchantId: '', nagadMobile: '', rocketMerchant: '',
    personalBkashNo: '', personalNagadNo: '', billingDay: 1, autoDisableDays: 10
  });

  // 1. Load Settings from Supabase on Mount
  useEffect(() => {
    const loadSettings = async () => {
      try {
        const { data, error } = await supabase.from('settings').select('*').limit(1).maybeSingle();
        if (data && !error) {
          setSettings({
            id: data.id || 1,
            companyName: data.company_name || '',
            companyAddress: data.company_address || '',
            companyPhone: data.company_phone || '',
            monthlyTarget: data.monthly_target || 0,
            smsApiUrl: data.sms_api_url || '',
            smsApiKey: data.sms_api_key || '',
            smsSenderId: data.sms_sender_id || '',
            apiMode: data.api_mode || 'Production',
            personalBkashNo: data.personal_bkash_no || '',
            personalNagadNo: data.personal_nagad_no || '',
            billingDay: data.billing_day || 1,
            autoDisableDays: data.auto_disable_days || 10
          });
        }
      } catch (e) { console.error("Load settings failed", e); }
    };
    loadSettings();
  }, []);

  const handleSave = async (e) => {
    e.preventDefault();
    setIsProcessing(true);
    try {
      const payload = {
        id: 1,
        company_name: settings.companyName,
        company_address: settings.companyAddress,
        company_phone: settings.companyPhone,
        monthly_target: settings.monthlyTarget,
        sms_api_url: settings.smsApiUrl,
        sms_api_key: settings.smsApiKey,
        sms_sender_id: settings.smsSenderId,
        personal_bkash_no: settings.personalBkashNo,
        personal_nagad_no: settings.personalNagadNo,
        billing_day: settings.billingDay,
        auto_disable_days: settings.autoDisableDays
      };

      const { error } = await supabase.from('settings').upsert(payload);
      if (error) throw error;
      alert("Settings Saved Successfully in Supabase!");
    } catch (error) {
      console.error(error);
      alert("Save failed: " + error.message);
    } finally {
      setIsProcessing(false);
    }
  };

  const runMigration = async () => {
    if (!window.confirm("এটি ফায়ারবেসের সব ডাটা সুপারবেসে কপি করবে। নিশ্চিত?")) return;
    setMigrating(true);
    setMigStatus('Connecting to Firebase...');

    try {
      const fetchFromFirebase = async (collName) => {
        const querySnapshot = await getDocs(collection(db, collName));
        return querySnapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
      };

      const fbCustomers = await fetchFromFirebase('customers');
      const fbPayments = await fetchFromFirebase('payments');
      const fbLedger = await fetchFromFirebase('ledger');

      setMigStatus('Cleaning Supabase...');
      await supabase.from('customers').delete().neq('id', '0');
      await supabase.from('payments').delete().neq('id', '0');
      await supabase.from('ledger_entries').delete().neq('id', '0');

      setMigStatus(`Migrating ${fbCustomers.length} Customers...`);
      const seenCodes = new Set();
      const mappedCustomers = [];

      fbCustomers.forEach(c => {
          const code = c.customerCode || c.customer_code || c.id;
          if (!seenCodes.has(code)) {
              seenCodes.add(code);
              mappedCustomers.push({
                  id: c.id, customer_code: code, name: c.name || '',
                  mobile: c.mobile || '', monthly_bill: parseFloat(c.monthlyBill || 0),
                  current_due: parseFloat(c.currentDue || 0), paid: parseFloat(c.paid || 0),
                  status: c.status || 'Active', expire_date: c.expireDate || null,
                  assigned_staff_id: c.assignedStaffId || null
              });
          }
      });

      if (mappedCustomers.length > 0) await supabase.from('customers').upsert(mappedCustomers);

      setMigStatus('SUCCESS!');
      alert("Migration Complete!");
      window.location.reload();
    } catch (err) { alert("Error: " + err.message); }
    finally { setMigrating(false); }
  };

  const SettingField = ({ label, value, onChange, type = 'text' }) => (
    <div className="space-y-2 uppercase font-black w-full">
      <label className="text-[10px] text-slate-400 ml-4 tracking-widest uppercase">{label}</label>
      <input type={type} value={value} onChange={e => onChange(e.target.value)} className="bg-slate-50 dark:bg-slate-900 border-none p-5 rounded-[28px] font-black text-lg w-full shadow-inner focus:ring-2 focus:ring-teal-500/20 transition-all" />
    </div>
  );

  return (
    <div className="w-full max-w-7xl mx-auto space-y-12 pb-20 uppercase font-black tracking-tighter transition-all">
      <div className="flex justify-between items-end">
        <div className="space-y-2">
          <h3 className="text-6xl font-black text-slate-800 dark:text-white tracking-tighter leading-none tracking-widest">Settings</h3>
          <p className="text-xs text-teal-600 tracking-widest font-black uppercase italic">Master Control Panel</p>
        </div>
        <div className="bg-rose-50 dark:bg-rose-900/20 p-6 rounded-[32px] border-2 border-rose-100 dark:border-rose-800 space-y-4 text-center">
           <p className="text-[10px] text-rose-600 font-bold tracking-widest uppercase">DATA MIGRATION</p>
           <button onClick={runMigration} disabled={migrating} className="bg-rose-600 text-white px-8 py-3 rounded-2xl font-black text-xs shadow-xl hover:scale-105 active:scale-95 transition-all">
              {migrating ? 'MIGRATING...' : 'START TRANSFER'}
           </button>
        </div>
      </div>

      <form onSubmit={handleSave} className="grid grid-cols-1 xl:grid-cols-2 gap-12 font-black">
        <div className="bg-white dark:bg-slate-800 p-12 rounded-[56px] shadow-2xl border-2 border-teal-500/20 lg:col-span-1">
           <div className="flex items-center space-x-4 text-[#0D9488] mb-8"><div className="w-16 h-16 bg-teal-50 rounded-2xl flex items-center justify-center"><i className="fas fa-hand-holding-dollar text-3xl"></i></div><h3 className="text-2xl font-black uppercase tracking-tight">Accounts</h3></div>
           <div className="space-y-4">
              <SettingField label="BKASH NO" value={settings.personalBkashNo} onChange={v => setSettings({...settings, personalBkashNo: v})} />
              <SettingField label="NAGAD NO" value={settings.personalNagadNo} onChange={v => setSettings({...settings, personalNagadNo: v})} />
           </div>
        </div>
        <div className="space-y-12">
            <div className="bg-white dark:bg-slate-800 p-10 rounded-[56px] shadow-2xl border border-slate-100 dark:border-slate-700 space-y-8">
               <div className="bg-blue-600 text-white p-5 rounded-2xl text-center text-[10px] font-black uppercase tracking-[3px]">Business Profile</div>
               <div className="space-y-6">
                  <SettingField label="COMPANY NAME" value={settings.companyName} onChange={v => setSettings({...settings, companyName: v})} />
                  <SettingField label="HOTLINE" value={settings.companyPhone} onChange={v => setSettings({...settings, companyPhone: v})} />
               </div>
            </div>
        </div>
        <div className="bg-white dark:bg-slate-800 p-12 rounded-[56px] shadow-2xl border border-slate-100 dark:border-slate-700 space-y-8 xl:col-span-2">
           <div className="bg-indigo-600 text-white p-5 rounded-2xl text-center text-[10px] font-black uppercase tracking-[3px]">SMS & Targets</div>
           <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
              <div className="space-y-2"><label className="text-[10px] text-slate-400 ml-4 tracking-widest uppercase">TARGET</label><input type="number" value={settings.monthlyTarget} onChange={e => setSettings({...settings, monthlyTarget: parseFloat(e.target.value) || 0})} className="bg-slate-50 dark:bg-slate-900 border-none p-5 rounded-[28px] font-black text-4xl text-teal-600 w-full" /></div>
              <div className="md:col-span-1">
                <SettingField label="SMS URL" value={settings.smsApiUrl} onChange={v => setSettings({...settings, smsApiUrl: v})} />
                <p className="text-[8px] text-slate-400 mt-2 ml-4">FORMAT: https://api.com/s?apikey={" {API_KEY} "}&number={" {MOBILE} "}&message={" {MESSAGE} "}</p>
              </div>
              <SettingField label="SMS TOKEN" value={settings.smsApiKey} onChange={v => setSettings({...settings, smsApiKey: v})} />
              <SettingField label="SMS SENDER" value={settings.smsSenderId} onChange={v => setSettings({...settings, smsSenderId: v})} />
           </div>
        </div>
        <div className="xl:col-span-2 flex justify-center pt-10 pb-20">
           <button type="submit" disabled={isProcessing} className="bg-slate-900 text-white px-32 py-10 rounded-[64px] font-black uppercase tracking-[10px] shadow-2xl hover:scale-105 active:scale-95 transition-all border-b-8 border-slate-700">SAVE ALL SETTINGS</button>
        </div>
      </form>
    </div>
  );
};

export default Settings;
