import React, { useState, useEffect } from 'react';
import { db } from '../firebaseConfig';
import { doc, setDoc, onSnapshot } from 'firebase/firestore';

const Settings = ({ store, t, lang }) => {
  const [settings, setSettings] = useState({
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
      await setDoc(doc(db, "settings", "global"), settings);
      alert("System Configuration Synchronized Successfully!");
    } catch (error) {
      alert("Failed to save settings.");
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <div className="w-full max-w-7xl mx-auto space-y-12 pb-20 uppercase font-black tracking-tighter transition-all">
      <div className="space-y-2 uppercase">
        <h3 className="text-6xl font-black text-slate-800 dark:text-white tracking-tighter uppercase leading-none">{t.global_settings}</h3>
        <p className="text-xs text-teal-600 tracking-widest font-black uppercase italic">Master Control Panel • API & Logic Configuration</p>
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
