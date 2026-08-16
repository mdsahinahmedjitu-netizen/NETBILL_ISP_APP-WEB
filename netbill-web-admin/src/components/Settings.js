import React, { useState, useEffect } from 'react';
import { db } from '../firebaseConfig';
import { supabase } from '../supabaseClient';
import { doc, setDoc, onSnapshot, collection, getDocs } from 'firebase/firestore';

const Settings = ({ store, t, lang }) => {
  const [isProcessing, setIsProcessing] = useState(false);
  const [migrating, setMigrating] = useState(false);
  const [migStatus, setMigStatus] = useState('');
  const [settings, setSettings] = useState({
    companyName: 'NetBill ISP', companyAddress: '', companyPhone: '', monthlyTarget: 0,
    smsApiUrl: '', smsApiKey: '', smsSenderId: '', apiMode: 'Production',
    bkashAppKey: '', bkashAppSecret: '', bkashUsername: '', bkashPassword: '',
    nagadMerchantId: '', nagadMobile: '', rocketMerchant: '',
    personalBkashNo: '', personalNagadNo: '', billingDay: 1, autoDisableDays: 10
  });

  const runMigration = async () => {
    if (!window.confirm("এটি ফায়ারবেসের সব ডাটা সুপারবেসে কপি করবে। ডুপ্লিকেট ডাটা অটোমেটিক রিমুভ করা হবে। আপনি কি নিশ্চিত?")) return;
    setMigrating(true);
    setMigStatus('Connecting to Firebase...');

    try {
      const fetchFromFirebase = async (collName) => {
        const querySnapshot = await getDocs(collection(db, collName));
        return querySnapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
      };

      const fbCustomers = await fetchFromFirebase('customers');
      const fbPayments = await fetchFromFirebase('payments');
      const fbStaff = await fetchFromFirebase('staff');
      const fbLedger = await fetchFromFirebase('ledger');

      setMigStatus('Cleaning Supabase Tables...');
      await supabase.from('customers').delete().neq('id', '0');
      await supabase.from('payments').delete().neq('id', '0');
      await supabase.from('ledger_entries').delete().neq('id', '0');

      setMigStatus(`Processing ${fbCustomers.length} Customers...`);

      // UNIQUE MAPPING: Remove duplicates based on customer_code before sending to Supabase
      const seenCodes = new Set();
      const mappedCustomers = [];

      fbCustomers.forEach(c => {
          const code = c.customerCode || c.customer_code || c.id;
          if (!seenCodes.has(code)) {
              seenCodes.add(code);
              mappedCustomers.push({
                  id: c.id,
                  customer_code: code,
                  name: c.name || '',
                  mobile: c.mobile || '',
                  alt_mobile: c.altMobile || c.alt_mobile || null,
                  address: c.address || null,
                  zone: c.zone || null,
                  package_name: c.packageName || c.package_name || null,
                  monthly_bill: parseFloat(c.monthlyBill || c.monthly_bill || 0) || 0,
                  current_due: parseFloat(c.currentDue || c.current_due || 0) || 0,
                  paid: parseFloat(c.paid || 0) || 0,
                  pppoe_username: c.pppoeUsername || c.pppoe_username || null,
                  pppoe_password: c.pppoePassword || c.pppoe_password || null,
                  status: c.status || 'Active',
                  join_date: c.joinDate || c.join_date || null,
                  assigned_staff_id: c.assignedStaffId || c.assigned_staff_id || null,
                  expire_date: c.expireDate || c.expire_date || null,
                  expire_time: c.expireTime || null,
                  connection_date: c.connectionDate || null,
                  join_day_of_month: parseInt(c.joinDayOfMonth || 1) || 1
              });
          }
      });

      if (mappedCustomers.length > 0) {
        const { error } = await supabase.from('customers').upsert(mappedCustomers);
        if (error) throw new Error("Customer Migration Failed: " + error.message);
      }

      setMigStatus('Syncing History...');
      const mappedPayments = fbPayments.map(p => ({
          id: p.id, receipt_no: p.receiptNo || p.receipt_no || `REC-${Math.random().toString(36).substr(2, 9)}`,
          customer_id: p.customerId || p.customer_id || null,
          amount: parseFloat(p.amount || 0) || 0,
          payment_method: p.paymentMethod || 'Cash',
          payment_date: p.paymentDate || p.payment_date || null,
          billing_month: p.billingMonth || null
      }));
      if (mappedPayments.length > 0) await supabase.from('payments').upsert(mappedPayments);

      const mappedLedger = fbLedger.map(l => ({
          id: l.id, customer_id: l.customerId || null, date: l.date || '',
          type: l.type || 'Payment', amount: parseFloat(l.amount) || 0,
          is_debit: l.isDebit !== undefined ? l.isDebit : (l.is_debit || false),
          description: l.description || '', running_balance: parseFloat(l.runningBalance || 0) || 0
      }));
      if (mappedLedger.length > 0) await supabase.from('ledger_entries').upsert(mappedLedger);

      setMigStatus('SUCCESS!');
      alert(`Success! ${mappedCustomers.length} customers and their history have been recovered.`);
      window.location.reload();
    } catch (err) {
        alert("Error: " + err.message);
    } finally {
        setMigrating(false);
    }
  };

  useEffect(() => {
    const unsub = onSnapshot(doc(db, "settings", "global"), (doc) => {
      if (doc.exists()) setSettings(prev => ({ ...prev, ...doc.data() }));
    });
    return () => unsub();
  }, []);

  const handleSave = async (e) => {
    e.preventDefault();
    setIsProcessing(true);
    try {
      await supabase.from('settings').upsert({
        id: 1, company_name: settings.companyName, company_address: settings.companyAddress,
        company_phone: settings.companyPhone, monthly_target: settings.monthlyTarget,
        sms_api_url: settings.smsApiUrl, sms_api_key: settings.smsApiKey,
        sms_sender_id: settings.smsSenderId, personal_bkash_no: settings.personalBkashNo,
        personal_nagad_no: settings.personalNagadNo, billing_day: settings.billingDay,
        auto_disable_days: settings.autoDisableDays
      });
      alert("Settings Saved!");
    } catch (error) { alert("Save failed."); }
    finally { setIsProcessing(false); }
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
          <h3 className="text-6xl font-black text-slate-800 dark:text-white tracking-tighter leading-none">Settings</h3>
          <p className="text-xs text-teal-600 tracking-widest font-black italic">Recovery & Migration</p>
        </div>
        <div className="bg-rose-50 dark:bg-rose-900/20 p-6 rounded-[32px] border-2 border-rose-100 dark:border-rose-800 space-y-4 text-center">
           <p className="text-[10px] text-rose-600 font-bold tracking-widest">CLEAN RECOVERY ENGINE</p>
           <button onClick={runMigration} disabled={migrating} className="bg-rose-600 text-white px-8 py-3 rounded-2xl font-black text-xs shadow-xl hover:scale-105 active:scale-95 transition-all">
              {migrating ? 'RECOVERING...' : 'START TRANSFER'}
           </button>
        </div>
      </div>
      <form onSubmit={handleSave} className="grid grid-cols-1 xl:grid-cols-2 gap-12 font-black">
        <div className="bg-white dark:bg-slate-800 p-12 rounded-[56px] shadow-2xl border-2 border-teal-500/20 lg:col-span-1">
           <div className="flex items-center space-x-4 text-[#0D9488] mb-8"><div className="w-16 h-16 bg-teal-50 rounded-2xl flex items-center justify-center"><i className="fas fa-hand-holding-dollar text-3xl"></i></div><h3 className="text-2xl font-black uppercase tracking-tight">Personal Accounts</h3></div>
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
              <div className="space-y-2"><label className="text-[10px] text-slate-400 ml-4 tracking-widest uppercase">TARGET</label><input type="number" value={settings.monthlyTarget} onChange={e => setSettings({...settings, monthlyTarget: parseFloat(e.target.value) || 0})} className="bg-slate-50 dark:bg-slate-900 border-none p-5 rounded-[28px] font-black text-4xl text-teal-600 w-full tracking-tighter" /></div>
              <div className="md:col-span-1"><SettingField label="SMS URL" value={settings.smsApiUrl} onChange={v => setSettings({...settings, smsApiUrl: v})} /></div>
              <SettingField label="SMS TOKEN" value={settings.smsApiKey} onChange={v => setSettings({...settings, smsApiKey: v})} type="password" />
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
