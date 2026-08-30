import React, { useState, useEffect } from 'react';
import { db } from '../firebaseConfig';
import { supabase } from '../supabaseClient';
import { doc, onSnapshot, getDocs, collection } from 'firebase/firestore';

const Settings = ({ store, t, lang, setActivePage }) => {
  const [isProcessing, setIsProcessing] = useState(false);
  const [migrating, setMigrating] = useState(false);
  const [migStatus, setMigStatus] = useState('');

  const [settings, setSettings] = useState({
    id: 1,
    companyName: 'NetBill ISP', companyAddress: '', companyPhone: '', monthlyTarget: 0,
    smsApiUrl: '', smsApiKey: '', smsSenderId: '', isAutoSmsEnabled: false, apiMode: 'Production',
    bkashAppKey: '', bkashAppSecret: '', bkashUsername: '', bkashPassword: '',
    nagadMerchantId: '', nagadMobile: '', rocketMerchant: '',
    adminIdentifier: 'admin@isp.com', adminPassword: '123456',
    rolePermissions: {
      Collector: {
        canCollect: true, canCollectDirect: false, canSeeMobile: true, canSeeAddress: true, canEdit: false, canDelete: false, canAdd: false, canSeeRevenue: false,
        canInventory: false, canSuspend: false, canLedger: true, canPasswords: false, canExpenses: false, canSMS: false,
        canDiscount: false, canBulkBill: false, canEditPayments: false, canManageStock: false, canAssignAssets: false,
        canManageZones: false, canManageRouters: false, canResolveTickets: true, canSendBulkSMS: false, canEditTemplates: false,
        canSeeStatsCards: true, canSeeExpiryAlerts: true, canSeeComplaintsAlert: true, canSeeVerificationAlert: false,
        canSeeTodayCollection: true, canSeeTotalCollection: false,
        canAccessBilling: false, canAccessReports: false, canAccessInventory: false, canAccessPackages: false, canAccessSMS: false,
        canAccessSalary: false, canAccessTickets: true, canModifyPricing: false, canViewLogs: false, canManageStaff: false,
        canAccessCustomers: true, canAccessPayments: true, canAccessExpenses: false, canAccessStaff: false, canAccessInfrastructure: false, canAccessSmsLogs: false, canAccessGlobalSettings: false
      },
      Lineman: {
        canCollect: false, canCollectDirect: false, canSeeMobile: true, canSeeAddress: true, canEdit: true, canDelete: false, canAdd: false, canSeeRevenue: false,
        canInventory: true, canSuspend: true, canLedger: false, canPasswords: true, canExpenses: false, canSMS: true,
        canDiscount: false, canBulkBill: false, canEditPayments: false, canManageStock: true, canAssignAssets: true,
        canManageZones: true, canManageRouters: false, canResolveTickets: true, canSendBulkSMS: false, canEditTemplates: false,
        canSeeStatsCards: false, canSeeExpiryAlerts: false, canSeeComplaintsAlert: true, canSeeVerificationAlert: false,
        canSeeTodayCollection: false, canSeeTotalCollection: false,
        canAccessBilling: false, canAccessReports: false, canAccessInventory: true, canAccessPackages: false, canAccessSMS: false,
        canAccessSalary: false, canAccessTickets: true, canModifyPricing: false, canViewLogs: false, canManageStaff: false,
        canAccessCustomers: true, canAccessPayments: false, canAccessExpenses: false, canAccessStaff: false, canAccessInfrastructure: true, canAccessSmsLogs: false, canAccessGlobalSettings: false
      },
      Support: {
        canCollect: false, canCollectDirect: false, canSeeMobile: true, canSeeAddress: true, canEdit: true, canDelete: false, canAdd: false, canSeeRevenue: false,
        canInventory: true, canSuspend: true, canLedger: true, canPasswords: true, canExpenses: false, canSMS: true,
        canDiscount: false, canBulkBill: false, canEditPayments: false, canManageStock: false, canAssignAssets: true,
        canManageZones: true, canManageRouters: true, canResolveTickets: true, canSendBulkSMS: true, canEditTemplates: true,
        canSeeStatsCards: true, canSeeExpiryAlerts: false, canSeeComplaintsAlert: true, canSeeVerificationAlert: false,
        canSeeTodayCollection: true, canSeeTotalCollection: true,
        canAccessBilling: false, canAccessReports: false, canAccessInventory: false, canAccessPackages: true, canAccessSMS: true,
        canAccessSalary: false, canAccessTickets: true, canModifyPricing: false, canViewLogs: true, canManageStaff: false,
        canAccessCustomers: true, canAccessPayments: false, canAccessExpenses: false, canAccessStaff: false, canAccessInfrastructure: true, canAccessSmsLogs: true, canAccessGlobalSettings: false
      },
      Management: {
        canCollect: true, canCollectDirect: true, canSeeMobile: true, canSeeAddress: true, canEdit: true, canDelete: true, canAdd: true, canSeeRevenue: true,
        canInventory: true, canSuspend: true, canLedger: true, canPasswords: true, canExpenses: true, canSMS: true,
        canDiscount: true, canBulkBill: true, canEditPayments: true, canManageStock: true, canAssignAssets: true,
        canManageZones: true, canManageRouters: true, canResolveTickets: true, canSendBulkSMS: true, canEditTemplates: true,
        canSeeStatsCards: true, canSeeExpiryAlerts: true, canSeeComplaintsAlert: true, canSeeVerificationAlert: true,
        canSeeTodayCollection: true, canSeeTotalCollection: true,
        canAccessBilling: true, canAccessReports: true, canAccessInventory: true, canAccessPackages: true, canAccessSMS: true,
        canAccessSalary: true, canAccessTickets: true, canModifyPricing: true, canViewLogs: true, canManageStaff: true,
        canAccessCustomers: true, canAccessPayments: true, canAccessExpenses: true, canAccessStaff: true, canAccessInfrastructure: true, canAccessSmsLogs: true, canAccessGlobalSettings: true
      }
    }
  });

  const [activeRoleTab, setActiveRoleTab] = useState('Collector');

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
            autoDisableDays: data.auto_disable_days || 10,
            isAutoSmsEnabled: data.is_auto_sms_enabled === true,
            adminIdentifier: data.admin_identifier || 'admin@isp.com',
            adminPassword: data.admin_password || '123456',
            rolePermissions: data.role_permissions || settings.rolePermissions
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
        is_auto_sms_enabled: settings.isAutoSmsEnabled,
        personal_bkash_no: settings.personalBkashNo,
        personal_nagad_no: settings.personalNagadNo,
        billing_day: settings.billingDay,
        auto_disable_days: settings.autoDisableDays,
        admin_identifier: settings.adminIdentifier,
        admin_password: settings.adminPassword,
        role_permissions: settings.rolePermissions
      };

      const { error } = await supabase.from('settings').upsert(payload);

      if (error && error.message.includes('is_auto_sms_enabled')) {
         // Fallback: Save without the missing column
         delete payload.is_auto_sms_enabled;
         const { error: secondError } = await supabase.from('settings').upsert(payload);
         if (secondError) throw secondError;
         alert("Settings saved partially! Note: 'is_auto_sms_enabled' column is missing in your Supabase 'settings' table. Please add it (Boolean) to enable this feature.");
      } else if (error) {
         throw error;
      } else {
         alert("Settings Saved Successfully in Supabase!");
      }

      window.location.reload();
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

  const RuleToggle = ({ label, checked, onChange }) => (
    <div
      onClick={() => onChange(!checked)}
      className={`p-6 rounded-[32px] border-2 cursor-pointer transition-all flex flex-col items-center justify-center space-y-3 text-center ${checked ? 'bg-teal-50 border-teal-500 dark:bg-teal-900/20 shadow-md' : 'bg-white border-slate-200 dark:bg-slate-900 dark:border-slate-800 shadow-sm'}`}
    >
       <div className={`w-12 h-12 rounded-2xl flex items-center justify-center text-xl ${checked ? 'bg-teal-500 text-white' : 'bg-slate-100 text-slate-400 dark:bg-slate-800'}`}>
          <i className={`fas ${checked ? 'fa-check-circle' : 'fa-times-circle'}`}></i>
       </div>
       <p className={`text-[10px] font-black uppercase tracking-widest ${checked ? 'text-slate-900 dark:text-teal-400' : 'text-slate-900 dark:text-slate-300'}`}>{label}</p>
    </div>
  );

  return (
    <div className="w-full max-w-7xl mx-auto space-y-6 md:space-y-12 pb-20 uppercase font-black tracking-tighter transition-all px-2 md:px-0">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-end gap-6">
        <div className="flex items-center space-x-4 md:space-x-6">
           <button onClick={() => setActivePage('dashboard')} className="w-10 h-10 md:w-12 md:h-12 bg-white dark:bg-slate-800 rounded-xl flex items-center justify-center text-teal-600 shadow-sm border border-slate-100">
              <i className="fas fa-arrow-left"></i>
           </button>
           <div className="space-y-1 md:space-y-2">
              <h3 className="text-3xl md:text-6xl font-black text-slate-800 dark:text-white tracking-tighter leading-none tracking-widest uppercase">Settings</h3>
              <p className="text-[10px] md:text-xs text-teal-600 tracking-widest font-black uppercase italic">{t.master_control_panel}</p>
           </div>
        </div>
        <div className="w-full md:w-auto bg-rose-50 dark:bg-rose-900/20 p-4 md:p-6 rounded-[24px] md:rounded-[32px] border-2 border-rose-100 dark:border-rose-800 space-y-3 md:space-y-4 text-center">
           <p className="text-[9px] md:text-[10px] text-rose-600 font-bold tracking-widest uppercase">{t.data_migration}</p>
           <button onClick={runMigration} disabled={migrating} className="w-full md:w-auto bg-rose-600 text-white px-6 py-2.5 md:px-8 md:py-3 rounded-xl md:rounded-2xl font-black text-[10px] md:text-xs shadow-xl hover:scale-105 active:scale-95 transition-all">
              {migrating ? t.migrating : t.start_transfer}
           </button>
        </div>
      </div>

      <form onSubmit={handleSave} className="grid grid-cols-1 xl:grid-cols-2 gap-6 md:gap-12 font-black">
        <div className="bg-white dark:bg-slate-800 p-6 md:p-12 rounded-[32px] md:rounded-[56px] shadow-2xl border-2 border-teal-500/20 lg:col-span-1">
           <div className="flex items-center space-x-3 md:space-x-4 text-[#0D9488] mb-6 md:mb-8 leading-none">
              <div className="w-12 h-12 md:w-16 md:h-16 bg-teal-50 dark:bg-slate-900 rounded-2xl flex items-center justify-center shadow-inner"><i className="fas fa-hand-holding-dollar text-xl md:text-3xl"></i></div>
              <h3 className="text-lg md:text-2xl font-black uppercase tracking-tight">{t.accounts_section}</h3>
           </div>
           <div className="grid grid-cols-1 gap-4 md:gap-6">
              <SettingField label={t.bkash_no} value={settings.personalBkashNo} onChange={v => setSettings({...settings, personalBkashNo: v})} />
              <SettingField label={t.nagad_no} value={settings.personalNagadNo} onChange={v => setSettings({...settings, personalNagadNo: v})} />
           </div>
        </div>

        <div className="bg-white dark:bg-slate-800 p-6 md:p-10 rounded-[32px] md:rounded-[56px] shadow-2xl border border-slate-100 dark:border-slate-700 space-y-6 md:space-y-8">
           <div className="bg-blue-600 text-white p-4 md:p-5 rounded-2xl text-center text-[9px] md:text-[10px] font-black uppercase tracking-[3px]">{t.business_profile}</div>
           <div className="grid grid-cols-1 gap-4 md:gap-6">
              <SettingField label={t.company_name_label} value={settings.companyName} onChange={v => setSettings({...settings, companyName: v})} />
              <SettingField label={t.hotline} value={settings.companyPhone} onChange={v => setSettings({...settings, companyPhone: v})} />
           </div>
        </div>

        {/* Super Admin Security Section */}
        <div className="bg-slate-900 text-white p-6 md:p-12 rounded-[32px] md:rounded-[56px] shadow-2xl border-2 border-rose-500/20 lg:col-span-1">
           <div className="flex items-center space-x-3 md:space-x-4 text-rose-500 mb-6 md:mb-8 leading-none">
              <div className="w-12 h-12 md:w-16 md:h-16 bg-rose-500/10 rounded-2xl flex items-center justify-center shadow-inner"><i className="fas fa-user-shield text-xl md:text-3xl"></i></div>
              <h3 className="text-lg md:text-2xl font-black uppercase tracking-tight">Super Admin Security</h3>
           </div>
           <div className="grid grid-cols-1 gap-4 md:gap-6">
              <div className="space-y-2 uppercase font-black w-full">
                <label className="text-[10px] text-slate-500 ml-4 tracking-widest uppercase">Admin Identifier (Email/ID)</label>
                <input type="text" value={settings.adminIdentifier} onChange={e => setSettings({...settings, adminIdentifier: e.target.value})} className="bg-slate-800 border-none p-5 rounded-[28px] font-black text-lg w-full shadow-inner focus:ring-2 focus:ring-rose-500/20 transition-all text-white" />
              </div>
              <div className="space-y-2 uppercase font-black w-full">
                <label className="text-[10px] text-slate-500 ml-4 tracking-widest uppercase">New Admin Password</label>
                <input type="password" value={settings.adminPassword} onChange={e => setSettings({...settings, adminPassword: e.target.value})} className="bg-slate-800 border-none p-5 rounded-[28px] font-black text-lg w-full shadow-inner focus:ring-2 focus:ring-rose-500/20 transition-all text-white" />
              </div>
           </div>
           <p className="text-[8px] text-slate-500 mt-6 ml-4 leading-relaxed">CAUTION: CHANGING THESE WILL LOG YOU OUT. DO NOT LOSE THESE CREDENTIALS AS THEY PROVIDE FULL SYSTEM ACCESS.</p>
        </div>

        <div className="bg-white dark:bg-slate-800 p-6 md:p-12 rounded-[32px] md:rounded-[56px] shadow-2xl border-2 border-slate-100 dark:border-slate-700 space-y-6 md:space-y-8 xl:col-span-2">
           <div className="flex flex-col md:flex-row justify-between items-center bg-indigo-600 text-white p-6 md:p-8 rounded-[24px] md:rounded-3xl gap-4 md:gap-6">
              <div className="flex items-center space-x-3 md:space-x-4">
                 <div className="w-10 h-10 md:w-12 md:h-12 bg-white/20 rounded-xl md:rounded-2xl flex items-center justify-center text-xl md:text-2xl shadow-lg"><i className="fas fa-robot"></i></div>
                 <div className="space-y-1">
                    <h3 className="text-xl md:text-2xl font-black uppercase tracking-tight">{t.sms_targets}</h3>
                    <p className="text-[8px] md:text-[10px] opacity-70 font-bold tracking-[2px]">AUTOMATED NOTIFICATION ENGINE</p>
                 </div>
              </div>

              <div className="flex items-center space-x-4 md:space-x-6 bg-black/20 p-3 md:p-4 rounded-xl md:rounded-2xl border border-white/10 w-full md:w-auto justify-between md:justify-start">
                 <div className="text-left md:text-right">
                    <p className="text-[8px] md:text-[10px] font-black opacity-60 uppercase">System Status</p>
                    <p className={`text-[10px] md:text-sm font-black ${settings.isAutoSmsEnabled ? 'text-emerald-400' : 'text-rose-400'}`}>
                       {settings.isAutoSmsEnabled ? 'AUTO SMS ACTIVE' : 'AUTO SMS DISABLED'}
                    </p>
                 </div>
                 <div
                    onClick={() => setSettings({...settings, isAutoSmsEnabled: !settings.isAutoSmsEnabled})}
                    className={`w-14 h-7 md:w-16 md:h-8 rounded-full relative cursor-pointer transition-all duration-300 ${settings.isAutoSmsEnabled ? 'bg-emerald-500' : 'bg-slate-400'}`}
                 >
                    <div className={`absolute top-1 w-5 h-5 md:w-6 md:h-6 bg-white rounded-full shadow-md transition-all duration-300 ${settings.isAutoSmsEnabled ? 'left-8 md:left-9' : 'left-1'}`}></div>
                 </div>
              </div>
           </div>

           <div className="grid grid-cols-1 md:grid-cols-4 gap-6 md:gap-8">
              <div className="space-y-2"><label className="text-[9px] md:text-[10px] text-slate-400 ml-4 tracking-widest uppercase">{t.target_label}</label><input type="number" value={settings.monthlyTarget} onChange={e => setSettings({...settings, monthlyTarget: parseFloat(e.target.value) || 0})} className="bg-slate-50 dark:bg-slate-950 border-none p-4 md:p-5 rounded-[24px] md:rounded-[28px] font-black text-2xl md:text-4xl text-teal-600 w-full shadow-inner outline-none" /></div>
              <div className="md:col-span-1">
                <SettingField label={t.sms_url} value={settings.smsApiUrl} onChange={v => setSettings({...settings, smsApiUrl: v})} />
                <p className="text-[7px] md:text-[8px] text-slate-400 mt-2 ml-4">FORMAT: https://api.com/s?apikey={" {API_KEY} "}&number={" {MOBILE} "}&message={" {MESSAGE} "}</p>
              </div>
              <SettingField label={t.sms_token} value={settings.smsApiKey} onChange={v => setSettings({...settings, smsApiKey: v})} />
              <SettingField label={t.sms_sender} value={settings.smsSenderId} onChange={v => setSettings({...settings, smsSenderId: v})} />
           </div>
        </div>

        <div className="bg-white dark:bg-slate-800 p-6 md:p-12 rounded-[32px] md:rounded-[56px] shadow-2xl border border-slate-100 dark:border-slate-700 space-y-8 md:space-y-12 xl:col-span-2">
           <div className="flex flex-col md:flex-row justify-between items-center gap-4 md:gap-6">
              <div className="bg-rose-600 text-white px-6 py-2.5 md:px-10 md:py-4 rounded-xl md:rounded-2xl text-[9px] md:text-[10px] font-black uppercase tracking-[2px] md:tracking-[3px]">{t.staff_control_center}</div>

              {/* Role Selection Tabs */}
              <div className="flex bg-slate-100 dark:bg-slate-950 p-1.5 md:p-2 rounded-[18px] md:rounded-[24px] space-x-1 md:space-x-2 w-full md:w-auto overflow-x-auto custom-scrollbar">
                 {['Collector', 'Lineman', 'Support', 'Management'].map(role => (
                    <button
                       key={role}
                       type="button"
                       onClick={() => setActiveRoleTab(role)}
                       className={`flex-1 md:flex-none px-4 py-2 md:px-6 md:py-3 rounded-lg md:rounded-xl text-[9px] md:text-[10px] font-black transition-all whitespace-nowrap ${activeRoleTab === role ? 'bg-white dark:bg-slate-800 text-teal-600 shadow-md scale-105' : 'text-slate-500 hover:text-black dark:hover:text-white'}`}
                    >
                       {role === 'Collector' ? t.collector : role === 'Lineman' ? t.lineman : role === 'Support' ? t.support : t.management}
                    </button>
                 ))}
              </div>
           </div>

           <div className="bg-slate-50/50 dark:bg-slate-950/30 p-6 md:p-10 rounded-[28px] md:rounded-[40px] border border-slate-100 dark:border-slate-800 animate-in fade-in duration-500">
              <div className="flex items-center space-x-3 md:space-x-4 mb-6 md:mb-10">
                 <div className="w-10 h-10 md:w-12 md:h-12 bg-indigo-600 text-white rounded-xl md:rounded-2xl flex items-center justify-center shadow-lg shadow-indigo-500/20"><i className="fas fa-user-shield"></i></div>
                 <div>
                    <h4 className="text-lg md:text-xl font-black text-slate-800 dark:text-white leading-none uppercase">{t.configuring}: {activeRoleTab}</h4>
                    <p className="text-[8px] md:text-[10px] text-slate-400 font-bold mt-1 tracking-[2px]">{t.rules_for_all} {activeRoleTab.toUpperCase()} STAFF</p>
                 </div>
              </div>

              <div className="space-y-10 md:space-y-14">
                  {/* Category: Financials */}
                  <div className="space-y-4 md:space-y-6">
                     <h4 className="text-[10px] md:text-xs font-black text-slate-400 tracking-[3px] md:tracking-[4px] border-l-4 border-teal-500 pl-4 uppercase leading-none">Financial Operations</h4>
                     <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3 md:gap-6">
                        <RuleToggle label={t.rule_collect_payments} checked={settings.rolePermissions[activeRoleTab]?.canCollect} onChange={v => setSettings({...settings, rolePermissions: {...settings.rolePermissions, [activeRoleTab]: {...settings.rolePermissions[activeRoleTab], canCollect: v}}})} />
                        <RuleToggle label={t.rule_collect_direct} checked={settings.rolePermissions[activeRoleTab]?.canCollectDirect} onChange={v => setSettings({...settings, rolePermissions: {...settings.rolePermissions, [activeRoleTab]: {...settings.rolePermissions[activeRoleTab], canCollectDirect: v}}})} />
                        <RuleToggle label={t.rule_give_discount} checked={settings.rolePermissions[activeRoleTab]?.canDiscount} onChange={v => setSettings({...settings, rolePermissions: {...settings.rolePermissions, [activeRoleTab]: {...settings.rolePermissions[activeRoleTab], canDiscount: v}}})} />
                        <RuleToggle label={t.rule_bulk_billing} checked={settings.rolePermissions[activeRoleTab]?.canBulkBill} onChange={v => setSettings({...settings, rolePermissions: {...settings.rolePermissions, [activeRoleTab]: {...settings.rolePermissions[activeRoleTab], canBulkBill: v}}})} />
                        <RuleToggle label={t.rule_view_revenue} checked={settings.rolePermissions[activeRoleTab]?.canSeeRevenue} onChange={v => setSettings({...settings, rolePermissions: {...settings.rolePermissions, [activeRoleTab]: {...settings.rolePermissions[activeRoleTab], canSeeRevenue: v}}})} />
                        <RuleToggle label={t.rule_edit_payments} checked={settings.rolePermissions[activeRoleTab]?.canEditPayments} onChange={v => setSettings({...settings, rolePermissions: {...settings.rolePermissions, [activeRoleTab]: {...settings.rolePermissions[activeRoleTab], canEditPayments: v}}})} />
                        <RuleToggle label={t.expense_manager} checked={settings.rolePermissions[activeRoleTab]?.canExpenses} onChange={v => setSettings({...settings, rolePermissions: {...settings.rolePermissions, [activeRoleTab]: {...settings.rolePermissions[activeRoleTab], canExpenses: v}}})} />
                        <RuleToggle label={t.rule_view_ledgers} checked={settings.rolePermissions[activeRoleTab]?.canLedger} onChange={v => setSettings({...settings, rolePermissions: {...settings.rolePermissions, [activeRoleTab]: {...settings.rolePermissions[activeRoleTab], canLedger: v}}})} />
                     </div>
                  </div>

                  {/* Category: User Management */}
                  <div className="space-y-4 md:space-y-6">
                     <h4 className="text-[10px] md:text-xs font-black text-slate-400 tracking-[3px] md:tracking-[4px] border-l-4 border-indigo-500 pl-4 uppercase leading-none">User Management</h4>
                     <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3 md:gap-6">
                        <RuleToggle label={t.rule_enroll_users} checked={settings.rolePermissions[activeRoleTab]?.canAdd} onChange={v => setSettings({...settings, rolePermissions: {...settings.rolePermissions, [activeRoleTab]: {...settings.rolePermissions[activeRoleTab], canAdd: v}}})} />
                        <RuleToggle label={t.rule_edit_profiles} checked={settings.rolePermissions[activeRoleTab]?.canEdit} onChange={v => setSettings({...settings, rolePermissions: {...settings.rolePermissions, [activeRoleTab]: {...settings.rolePermissions[activeRoleTab], canEdit: v}}})} />
                        <RuleToggle label={t.rule_modify_pricing} checked={settings.rolePermissions[activeRoleTab]?.canModifyPricing} onChange={v => setSettings({...settings, rolePermissions: {...settings.rolePermissions, [activeRoleTab]: {...settings.rolePermissions[activeRoleTab], canModifyPricing: v}}})} />
                        <RuleToggle label={t.rule_suspend_enable} checked={settings.rolePermissions[activeRoleTab]?.canSuspend} onChange={v => setSettings({...settings, rolePermissions: {...settings.rolePermissions, [activeRoleTab]: {...settings.rolePermissions[activeRoleTab], canSuspend: v}}})} />
                        <RuleToggle label={t.rule_see_mobile} checked={settings.rolePermissions[activeRoleTab]?.canSeeMobile} onChange={v => setSettings({...settings, rolePermissions: {...settings.rolePermissions, [activeRoleTab]: {...settings.rolePermissions[activeRoleTab], canSeeMobile: v}}})} />
                        <RuleToggle label={t.rule_see_address} checked={settings.rolePermissions[activeRoleTab]?.canSeeAddress} onChange={v => setSettings({...settings, rolePermissions: {...settings.rolePermissions, [activeRoleTab]: {...settings.rolePermissions[activeRoleTab], canSeeAddress: v}}})} />
                        <RuleToggle label={t.rule_see_passwords} checked={settings.rolePermissions[activeRoleTab]?.canPasswords} onChange={v => setSettings({...settings, rolePermissions: {...settings.rolePermissions, [activeRoleTab]: {...settings.rolePermissions[activeRoleTab], canPasswords: v}}})} />
                     </div>
                  </div>

                  {/* Category: System Control */}
                  <div className="space-y-4 md:space-y-6">
                     <h4 className="text-[10px] md:text-xs font-black text-slate-400 tracking-[3px] md:tracking-[4px] border-l-4 border-rose-500 pl-4 uppercase leading-none">System Control</h4>
                     <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3 md:gap-6">
                        <RuleToggle label={t.rule_manage_staff} checked={settings.rolePermissions[activeRoleTab]?.canManageStaff} onChange={v => setSettings({...settings, rolePermissions: {...settings.rolePermissions, [activeRoleTab]: {...settings.rolePermissions[activeRoleTab], canManageStaff: v}}})} />
                        <RuleToggle label={t.rule_view_logs} checked={settings.rolePermissions[activeRoleTab]?.canViewLogs} onChange={v => setSettings({...settings, rolePermissions: {...settings.rolePermissions, [activeRoleTab]: {...settings.rolePermissions[activeRoleTab], canViewLogs: v}}})} />
                        <RuleToggle label={t.permanent_delete} checked={settings.rolePermissions[activeRoleTab]?.canDelete} onChange={v => setSettings({...settings, rolePermissions: {...settings.rolePermissions, [activeRoleTab]: {...settings.rolePermissions[activeRoleTab], canDelete: v}}})} />
                     </div>
                  </div>

                  {/* Category: Infrastructure */}
                  <div className="space-y-4 md:space-y-6">
                     <h4 className="text-[10px] md:text-xs font-black text-slate-400 tracking-[3px] md:tracking-[4px] border-l-4 border-blue-500 pl-4 uppercase leading-none">Infrastructure & Assets</h4>
                     <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3 md:gap-6">
                        <RuleToggle label={t.rule_manage_stock} checked={settings.rolePermissions[activeRoleTab]?.canManageStock} onChange={v => setSettings({...settings, rolePermissions: {...settings.rolePermissions, [activeRoleTab]: {...settings.rolePermissions[activeRoleTab], canManageStock: v}}})} />
                        <RuleToggle label={t.rule_assign_assets} checked={settings.rolePermissions[activeRoleTab]?.canAssignAssets} onChange={v => setSettings({...settings, rolePermissions: {...settings.rolePermissions, [activeRoleTab]: {...settings.rolePermissions[activeRoleTab], canAssignAssets: v}}})} />
                        <RuleToggle label={t.rule_manage_zones} checked={settings.rolePermissions[activeRoleTab]?.canManageZones} onChange={v => setSettings({...settings, rolePermissions: {...settings.rolePermissions, [activeRoleTab]: {...settings.rolePermissions[activeRoleTab], canManageZones: v}}})} />
                        <RuleToggle label={t.rule_manage_routers} checked={settings.rolePermissions[activeRoleTab]?.canManageRouters} onChange={v => setSettings({...settings, rolePermissions: {...settings.rolePermissions, [activeRoleTab]: {...settings.rolePermissions[activeRoleTab], canManageRouters: v}}})} />
                     </div>
                  </div>

                  {/* Category: Dashboard & Sidebar */}
                  <div className="space-y-4 md:space-y-6">
                     <h4 className="text-[10px] md:text-xs font-black text-slate-400 tracking-[3px] md:tracking-[4px] border-l-4 border-amber-500 pl-4 uppercase leading-none">Visibility Controls</h4>
                     <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3 md:gap-6">
                        <RuleToggle label="DASHBOARD CARDS" checked={settings.rolePermissions[activeRoleTab]?.canSeeStatsCards} onChange={v => setSettings({...settings, rolePermissions: {...settings.rolePermissions, [activeRoleTab]: {...settings.rolePermissions[activeRoleTab], canSeeStatsCards: v}}})} />
                        <RuleToggle label="EXPIRY ALERTS" checked={settings.rolePermissions[activeRoleTab]?.canSeeExpiryAlerts} onChange={v => setSettings({...settings, rolePermissions: {...settings.rolePermissions, [activeRoleTab]: {...settings.rolePermissions[activeRoleTab], canSeeExpiryAlerts: v}}})} />
                        <RuleToggle label="TICKETS BAR" checked={settings.rolePermissions[activeRoleTab]?.canSeeComplaintsAlert} onChange={v => setSettings({...settings, rolePermissions: {...settings.rolePermissions, [activeRoleTab]: {...settings.rolePermissions[activeRoleTab], canSeeComplaintsAlert: v}}})} />
                        <RuleToggle label="APPROVAL BAR" checked={settings.rolePermissions[activeRoleTab]?.canSeeVerificationAlert} onChange={v => setSettings({...settings, rolePermissions: {...settings.rolePermissions, [activeRoleTab]: {...settings.rolePermissions[activeRoleTab], canSeeVerificationAlert: v}}})} />
                        <RuleToggle label="BILLING MENU" checked={settings.rolePermissions[activeRoleTab]?.canAccessBilling} onChange={v => setSettings({...settings, rolePermissions: {...settings.rolePermissions, [activeRoleTab]: {...settings.rolePermissions[activeRoleTab], canAccessBilling: v}}})} />
                        <RuleToggle label="REPORTS MENU" checked={settings.rolePermissions[activeRoleTab]?.canAccessReports} onChange={v => setSettings({...settings, rolePermissions: {...settings.rolePermissions, [activeRoleTab]: {...settings.rolePermissions[activeRoleTab], canAccessReports: v}}})} />
                        <RuleToggle label="INVENTORY MENU" checked={settings.rolePermissions[activeRoleTab]?.canAccessInventory} onChange={v => setSettings({...settings, rolePermissions: {...settings.rolePermissions, [activeRoleTab]: {...settings.rolePermissions[activeRoleTab], canAccessInventory: v}}})} />
                        <RuleToggle label="STAFF MENU" checked={settings.rolePermissions[activeRoleTab]?.canAccessStaff} onChange={v => setSettings({...settings, rolePermissions: {...settings.rolePermissions, [activeRoleTab]: {...settings.rolePermissions[activeRoleTab], canAccessStaff: v}}})} />
                     </div>
                  </div>
              </div>
           </div>
        </div>

        <div className="xl:col-span-2 flex justify-center pt-6 md:pt-10 pb-10">
           <button type="submit" disabled={isProcessing} className="w-full md:w-auto bg-slate-900 text-white px-16 py-6 md:px-32 md:py-10 rounded-[32px] md:rounded-[64px] font-black uppercase tracking-[5px] md:tracking-[10px] shadow-2xl hover:scale-105 active:scale-95 transition-all border-b-8 border-slate-700 h-20 md:h-auto">
              {isProcessing ? 'SAVING...' : 'SAVE ALL SETTINGS'}
           </button>
        </div>
      </form>
    </div>
  );
};

export default Settings;
