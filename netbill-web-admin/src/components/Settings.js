import React, { useState, useEffect } from 'react';
import { db } from '../firebaseConfig';
import { supabase } from '../supabaseClient';
import { doc, setDoc, onSnapshot } from 'firebase/firestore';

const Settings = ({ store, t, lang }) => {
  const [settings, setSettings] = useState({
    // ... (existing settings state)
    companyName: 'NetBill ISP',
    companyAddress: '',
    companyPhone: '',
    monthlyTarget: 0,
    smsApiKey: '',
    smsSenderId: '',
    apiMode: 'Production',
    bkashAppKey: '',
    bkashAppSecret: '',
    bkashUsername: '',
    bkashPassword: '',
    nagadMerchantId: '',
    nagadMobile: '',
    rocketMerchant: '',
    personalBkashNo: '',
    personalNagadNo: '',
    billingDay: 1,
    autoDisableDays: 10
  });

  const [isSaving, setIsProcessing] = useState(false);
  const [migrating, setMigrating] = useState(false);
  const [migStatus, setMigStatus] = useState('');

  const runMigration = async () => {
    if (!window.confirm("এটি আপনার ফায়ারবেসের সব ডাটা সুপারবেসে কপি করবে। আপনি কি নিশ্চিত?")) return;
    setMigrating(true);
    setMigStatus('Preparing to migrate...');

    try {
      // 1. Sync Categories
      setMigStatus('Syncing Expense Categories...');
      const defaultCats = ['Bandwidth Cost', 'Staff Salary', 'Office Rent', 'Electricity Bill', 'Equipment Purchase', 'Maintenance', 'Transport', 'Marketing', 'Other Expense'];
      for (const cat of defaultCats) {
        await supabase.from('expense_categories').upsert({ name: cat });
      }

      // 2. Migrate Customers
      setMigStatus(`Migrating ${store.customers.length} Customers...`);
      for (const c of store.customers) {
        const { error } = await supabase.from('customers').upsert({
          customer_code: c.customerCode,
          name: c.name,
          mobile: c.mobile,
          alt_mobile: c.altMobile || null,
          address: c.address || null,
          zone: c.zone || null,
          package_name: c.packageName || null,
          monthly_bill: parseFloat(c.monthlyBill) || 0,
          current_due: parseFloat(c.currentDue) || 0,
          pppoe_username: c.pppoeUsername || null,
          pppoe_password: c.pppoePassword || null,
          status: c.status || 'Active',
          join_date: c.joinDate || null
        }, { onConflict: 'customer_code' });
        if (error) console.error("Cust Error:", error);
      }

      // 3. Migrate Payments
      setMigStatus(`Migrating ${store.payments.length} Payments...`);
      for (const p of store.payments) {
        await supabase.from('payments').upsert({
          receipt_no: p.receiptNo,
          amount: parseFloat(p.amount) || 0,
          payment_method: p.paymentMethod || 'Cash',
          payment_date: p.paymentDate || null,
          billing_month: p.billingMonth || null,
          customer_name: p.customerName || null,
          collected_by: p.collectedBy || null
        }, { onConflict: 'receipt_no' });
      }

      setMigStatus('SUCCESS! ALL DATA COPIED TO SUPABASE.');
      alert("Migration Successful! You can now start using Supabase.");
    } catch (err) {
      console.error(err);
      setMigStatus('Migration Failed. Check console.');
    } finally {
      setMigrating(false);
    }
  };

  useEffect(() => {
    const unsub = onSnapshot(doc(db, "settings", "global"), (doc) => {
      if (doc.exists()) {
        setSettings(prev => ({ ...prev, ...doc.data() }));
      }
    });
    return () => unsub();
  }, []);

  const handleSave = async (e) => {
    e.preventDefault();
    setIsProcessing(true);
    try {
      const { error } = await supabase.from('settings').upsert({
        id: 1, // Only one row for settings
        company_name: settings.companyName,
        company_address: settings.companyAddress,
        company_phone: settings.companyPhone,
        monthly_target: settings.monthlyTarget,
        sms_api_key: settings.smsApiKey,
        sms_sender_id: settings.smsSenderId,
        api_mode: settings.apiMode,
        bkash_app_key: settings.bkashAppKey,
        bkash_app_secret: settings.bkashAppSecret,
        bkash_username: settings.bkashUsername,
        bkash_password: settings.bkashPassword,
        nagad_merchant_id: settings.nagadMerchantId,
        nagad_mobile: settings.nagadMobile,
        personal_bkash_no: settings.personalBkashNo,
        personal_nagad_no: settings.personalNagadNo,
        billing_day: settings.billingDay,
        auto_disable_days: settings.autoDisableDays
      });
      if (error) throw error;
      alert("Settings Saved Successfully to Supabase!");
    } catch (error) {
      alert("Failed to save settings.");
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <div className="w-full max-w-7xl mx-auto space-y-12 pb-20 uppercase font-black tracking-tighter transition-all">
      <div className="flex justify-between items-end">
        <div className="space-y-2 uppercase">
          <h3 className="text-6xl font-black text-slate-800 dark:text-white tracking-tighter uppercase leading-none">{t.global_settings}</h3>
          <p className="text-xs text-teal-600 tracking-widest font-black uppercase italic">Master Control Panel • API & Logic Configuration</p>
        </div>

        {/* MIGRATION CONTROLLER */}
        <div className="bg-rose-50 dark:bg-rose-900/20 p-6 rounded-[32px] border-2 border-rose-100 dark:border-rose-800 space-y-4">
           <p className="text-[10px] text-rose-600 font-bold tracking-widest">DATABASE MIGRATION (FIREBASE -> SUPABASE)</p>
           <button
             onClick={runMigration}
             disabled={migrating}
             className="bg-rose-600 text-white px-8 py-3 rounded-2xl font-black text-xs shadow-xl hover:scale-105 active:scale-95 transition-all flex items-center space-x-3"
           >
              <i className={`fas ${migrating ? 'fa-sync fa-spin' : 'fa-database'}`}></i>
              <span>{migrating ? 'MIGRATING...' : 'START DATA TRANSFER'}</span>
           </button>
           {migStatus && <p className="text-[9px] text-rose-500 font-black animate-pulse">{migStatus}</p>}
        </div>
      </div>

      <form onSubmit={handleSave} className="grid grid-cols-1 xl:grid-cols-2 gap-12 font-black">

        {/* 1. MANUAL PAYMENT NUMBERS (PERSONAL ACCOUNTS) - NEW */}
        <div className="bg-white dark:bg-slate-800 p-12 rounded-[56px] shadow-2xl border-2 border-teal-500/20 space-y-10 lg:col-span-1">
           <div className="flex items-center space-x-4 text-[#0D9488]">
              <div className="w-16 h-16 bg-teal-50 rounded-2xl flex items-center justify-center">
                 <i className="fas fa-hand-holding-dollar text-3xl"></i>
              </div>
              <h3 className="text-2xl font-black uppercase tracking-tight">Personal Accounts (Send Money)</h3>
           </div>

           <div className="space-y-6">
              <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest leading-relaxed">এই নাম্বারগুলো আপনার কাস্টমার পোর্টালে "Send Money" নির্দেশিকায় দেখা যাবে।</p>
              <div className="space-y-4">
                 <SettingField label="BKASH SEND MONEY NO" value={settings.personalBkashNo} onChange={v => setSettings({...settings, personalBkashNo: v})} />
                 <SettingField label="NAGAD SEND MONEY NO" value={settings.personalNagadNo} onChange={v => setSettings({...settings, personalNagadNo: v})} />
              </div>
           </div>
        </div>

        {/* 2. AUTOMATION & BUSINESS PROFILE */}
        <div className="space-y-12">
            <div className="bg-white dark:bg-slate-800 p-10 rounded-[56px] shadow-2xl border border-slate-100 dark:border-slate-700 space-y-8">
               <div className="bg-blue-600 text-white p-5 rounded-2xl text-center text-[10px] font-black uppercase tracking-[3px] shadow-xl">Business Profile</div>
               <div className="space-y-6">
                  <SettingField label="COMPANY NAME" value={settings.companyName} onChange={v => setSettings({...settings, companyName: v})} />
                  <SettingField label="OFFICE ADDRESS" value={settings.companyAddress} onChange={v => setSettings({...settings, companyAddress: v})} />
                  <SettingField label="SUPPORT HOTLINE" value={settings.companyPhone} onChange={v => setSettings({...settings, companyPhone: v})} />
               </div>
            </div>

            <div className="bg-white dark:bg-slate-800 p-10 rounded-[56px] shadow-2xl border border-slate-100 dark:border-slate-700 space-y-8">
               <div className="bg-emerald-600 text-white p-5 rounded-2xl text-center text-[10px] font-black uppercase tracking-[3px] shadow-xl">Automation Rules</div>
               <div className="grid grid-cols-2 gap-6">
                  <div className="space-y-2">
                    <label className="text-[10px] text-slate-400 ml-4 tracking-widest uppercase">BILLING DAY</label>
                    <input type="number" value={settings.billingDay} onChange={e => setSettings({...settings, billingDay: parseInt(e.target.value)})} className="bg-slate-50 dark:bg-slate-900 border-none p-5 rounded-[28px] font-black text-3xl text-emerald-600 w-full" />
                  </div>
                  <div className="space-y-2">
                    <label className="text-[10px] text-slate-400 ml-4 tracking-widest uppercase">GRACE PERIOD</label>
                    <input type="number" value={settings.autoDisableDays} onChange={e => setSettings({...settings, autoDisableDays: parseInt(e.target.value)})} className="bg-slate-50 dark:bg-slate-900 border-none p-5 rounded-[28px] font-black text-3xl text-rose-500 w-full" />
                  </div>
               </div>
            </div>
        </div>

        {/* 3. PAYMENT GATEWAY APIs (OPTIONAL) */}
        <div className="bg-white dark:bg-slate-800 p-12 rounded-[56px] shadow-2xl border border-slate-100 dark:border-slate-700 space-y-10 xl:col-span-2">
           <div className="flex items-center space-x-4 text-[#D0006F]">
              <i className="fas fa-shield-alt text-3xl"></i>
              <h3 className="text-2xl font-black uppercase tracking-tight">bKash & Nagad API (Automatic Gateway)</h3>
           </div>
           <div className="grid grid-cols-1 md:grid-cols-2 gap-10">
              <div className="space-y-4">
                 <p className="text-[10px] font-black text-[#D0006F] uppercase tracking-widest">bKash Tokenized Checkout</p>
                 <GatewayInput placeholder="bKash App Key" value={settings.bkashAppKey} onChange={v => setSettings({...settings, bkashAppKey: v})} />
                 <GatewayInput placeholder="bKash App Secret" value={settings.bkashAppSecret} onChange={v => setSettings({...settings, bkashAppSecret: v})} type="password" />
                 <GatewayInput placeholder="bKash Merchant Username" value={settings.bkashUsername} onChange={v => setSettings({...settings, bkashUsername: v})} />
                 <GatewayInput placeholder="bKash Merchant Password" value={settings.bkashPassword} onChange={v => setSettings({...settings, bkashPassword: v})} type="password" />
              </div>
              <div className="space-y-4">
                 <p className="text-[10px] font-black text-orange-500 uppercase tracking-widest">Nagad Payment Gateway</p>
                 <GatewayInput placeholder="Nagad Merchant ID" value={settings.nagadMerchantId} onChange={v => setSettings({...settings, nagadMerchantId: v})} />
                 <GatewayInput placeholder="Nagad Merchant Number" value={settings.nagadMobile} onChange={v => setSettings({...settings, nagadMobile: v})} />
              </div>
           </div>
        </div>

        {/* 4. SMS & Global Target (Bottom Row) */}
        <div className="bg-white dark:bg-slate-800 p-12 rounded-[56px] shadow-2xl border border-slate-100 dark:border-slate-700 space-y-8 xl:col-span-2">
           <div className="bg-indigo-600 text-white p-5 rounded-2xl text-center text-[10px] font-black uppercase tracking-[3px] shadow-xl">Global Target & SMS Masking</div>
           <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
              <div className="space-y-2">
                <label className="text-[10px] text-slate-400 ml-4 tracking-widest uppercase">MONTHLY COLLECTION TARGET (৳)</label>
                <input type="number" value={settings.monthlyTarget} onChange={e => setSettings({...settings, monthlyTarget: parseFloat(e.target.value) || 0})} className="bg-slate-50 dark:bg-slate-900 border-none p-5 rounded-[28px] font-black text-4xl text-teal-600 w-full tracking-tighter" />
              </div>
              <SettingField label="SMS API TOKEN" value={settings.smsApiKey} onChange={v => setSettings({...settings, smsApiKey: v})} type="password" />
              <SettingField label="SMS SENDER ID" value={settings.smsSenderId} onChange={v => setSettings({...settings, smsSenderId: v})} />
           </div>
        </div>

        <div className="xl:col-span-2 flex justify-center pt-10 pb-20">
           <button type="submit" disabled={isSaving} className="bg-slate-900 text-white px-32 py-10 rounded-[64px] font-black uppercase tracking-[10px] shadow-2xl hover:scale-105 active:scale-95 transition-all border-b-8 border-slate-700">
             {isSaving ? 'UPDATING CLOUD...' : 'SAVE ALL SETTINGS'}
           </button>
        </div>

      </form>
    </div>
  );
};

const GatewayInput = ({ placeholder, value, onChange, type = 'text' }) => (
  <input
    type={type}
    placeholder={placeholder}
    value={value}
    onChange={e => onChange(e.target.value)}
    className="w-full bg-slate-50 dark:bg-slate-900 border-2 border-slate-100 dark:border-slate-700 p-5 rounded-2xl font-black text-lg text-slate-800 dark:text-white placeholder:text-slate-300 outline-none focus:border-[#D0006F] transition-all shadow-sm"
  />
);

const SettingField = ({ label, value, onChange, type = 'text' }) => (
  <div className="space-y-2 uppercase font-black w-full">
    <label className="text-[10px] text-slate-400 ml-4 tracking-widest uppercase">{label}</label>
    <input
      type={type}
      value={value}
      onChange={e => onChange(e.target.value)}
      className="bg-slate-50 dark:bg-slate-900 border-none p-5 rounded-[28px] font-black text-lg w-full shadow-inner focus:ring-2 focus:ring-teal-500/20 transition-all"
    />
  </div>
);

export default Settings;
