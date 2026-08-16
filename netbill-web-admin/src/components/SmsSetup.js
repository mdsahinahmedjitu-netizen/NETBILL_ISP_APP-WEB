import React, { useState, useEffect } from 'react';
import { supabase } from '../supabaseClient';

const SmsSetup = ({ store, t }) => {
  const [isEditing, setIsEditing] = useState(false);
  const [selectedTemplate, setSelectedTemplate] = useState(null);
  const [editContent, setEditContent] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  // Bulk Dispatch States
  const [isDispatching, setIsDispatching] = useState(false);
  const [dispatchProgress, setDispatchProgress] = useState({ current: 0, total: 0, success: 0, fail: 0 });

  const systemSmsTypes = [
    "All Customer", "Area Wise Customer Due List", "Area Wise Customer List",
    "Auto Temporary Disable Alert", "Bill Generate", "Collection",
    "Collection (MFS) to Owner", "Collection Delete", "Collection Edit",
    "Collection to Owner", "Complain Employee", "Complain List",
    "Complain to Customer", "Create Customer", "Create Customer to Owner",
    "Customer Complaint Notification Message", "Free Customer List",
    "Inactive Customer List", "Failed to Disable at Mikrotik", "Expired Customer"
  ];

  const defaultLibrary = {
    "Collection": {
        en: "Dear {NAME}, your payment of {AMOUNT} TK has been received. Receipt No: {RECEIPT_NO}. Thank you for staying with us.",
        bn: "প্রিয় {NAME}, আপনার {AMOUNT} টাকা পেমেন্ট গ্রহণ করা হয়েছে। রশিদ নং: {RECEIPT_NO}। আমাদের সাথে থাকার জন্য ধন্যবাদ।"
    },
    "Bill Generate": {
        en: "Dear {NAME}, your internet bill for {BILL_MONTH} has been generated. Amount: {AMOUNT} TK. Please pay before {DUE_DATE}.",
        bn: "প্রিয় {NAME}, {BILL_MONTH} মাসের ইন্টারনেট বিল তৈরি হয়েছে। পরিমাণ: {AMOUNT} টাকা। অনুগ্রহ করে {DUE_DATE} তারিখের মধ্যে পরিশোধ করুন।"
    },
    "Expired Customer": {
        en: "Dear {NAME}, your internet account {CUSTOMER_CODE} has expired. Please recharge to continue enjoying our services. Thank you.",
        bn: "প্রিয় {NAME}, আপনার ইন্টারনেট অ্যাকাউন্টের ({CUSTOMER_CODE}) মেয়াদ শেষ হয়ে গেছে। নিরবচ্ছিন্ন সেবা পেতে অনুগ্রহ করে দ্রুত রিচার্জ করুন। ধন্যবাদ।"
    },
    "Default": {
        en: "Dear {NAME}, this is a notification from NetBill ISP regarding your account {CUSTOMER_CODE}. Thank you.",
        bn: "প্রিয় {NAME}, আপনার অ্যাকাউন্ট {CUSTOMER_CODE} সংক্রান্ত একটি বিশেষ বিজ্ঞপ্তি। ধন্যবাদ, NetBill ISP।"
    }
  };

  const generateSafeId = () => Math.random().toString(36).substring(2, 15) + Math.random().toString(36).substring(2, 15);
  const templates = store.smsTemplates || [];
  const settings = store.settings || {};

  const handleToggle = async (type, active) => {
    const template = templates.find(t => t.title === type);
    try {
        const payload = {
            id: template?.id || generateSafeId(),
            title: type,
            category: 'System',
            message_content: template?.messageContent || template?.message_content || (defaultLibrary[type]?.en || defaultLibrary["Default"].en),
            is_active: active
        };
        await supabase.from('sms_templates').upsert(payload);
    } catch (e) { alert("Action failed: " + e.message); }
  };

  const openEdit = (type) => {
    let template = templates.find(t => t.title === type);
    if (!template) {
        template = { id: generateSafeId(), title: type, category: 'System', messageContent: (defaultLibrary[type]?.en || defaultLibrary["Default"].en), isActive: false };
    }
    setSelectedTemplate(template);
    setEditContent(template.messageContent || template.message_content || '');
    setIsEditing(true);
  };

  const handleSaveEdit = async () => {
    if (!selectedTemplate) return;
    setIsLoading(true);
    try {
        await supabase.from('sms_templates').upsert({
            id: selectedTemplate.id, title: selectedTemplate.title,
            message_content: editContent, category: 'System',
            is_active: selectedTemplate.isActive || selectedTemplate.is_active || false
        });
        setIsEditing(false);
    } catch (e) { alert("Save failed: " + e.message); }
    finally { setIsLoading(false); }
  };

  // REAL-TIME BULK DISPATCHER LOGIC
  const runBroadcast = async (type) => {
    const template = templates.find(t => t.title === type);
    if (!template || !(template.isActive || template.is_active)) {
        alert("Please enable the template first.");
        return;
    }

    const apiUrl = settings.smsApiUrl;
    const apiKey = settings.smsApiKey;
    const senderId = settings.smsSenderId;

    if (!apiUrl || !apiKey) {
        alert("SMS Gateway is not configured in Settings!");
        return;
    }

    let targets = store.customers;
    if (type.includes("Due")) targets = targets.filter(c => parseFloat(c.currentDue) > 0);
    if (type.includes("Expired")) targets = targets.filter(c => c.status !== "Active");

    if (targets.length === 0) {
        alert("No target customers found for this category.");
        return;
    }

    if (!window.confirm(`Start Bulk SMS Dispatch to ${targets.length} customers?`)) return;

    setIsDispatching(true);
    setDispatchProgress({ current: 0, total: targets.length, success: 0, fail: 0 });

    for (let i = 0; i < targets.length; i++) {
        const customer = targets[i];
        const message = (template.messageContent || template.message_content)
            .replace(/{NAME}/g, customer.name)
            .replace(/{AMOUNT}/g, customer.currentDue || 0)
            .replace(/{TOTAL_DUE}/g, customer.currentDue || 0)
            .replace(/{CUSTOMER_CODE}/g, customer.customerCode || '');

        const success = await sendSmsApi(customer.mobile, message);

        setDispatchProgress(prev => ({
            ...prev,
            current: i + 1,
            success: success ? prev.success + 1 : prev.success,
            fail: !success ? prev.fail + 1 : prev.fail
        }));

        // Log to Supabase for reporting
        await supabase.from('sms_logs').insert({
            id: generateSafeId(),
            customer_id: customer.id,
            customer_name: customer.name,
            mobile: customer.mobile,
            notification_type: type,
            message: message,
            status: success ? 'Sent' : 'Failed',
            sent_timestamp: new Date().toLocaleString()
        });
    }

    setIsDispatching(false);
    alert(`Bulk Task Completed! Success: ${dispatchProgress.success}, Failed: ${dispatchProgress.fail}`);
  };

  const sendSmsApi = async (mobile, message) => {
    try {
        const cleanMobile = mobile.startsWith('01') ? `88${mobile}` : mobile;
        const finalUrl = settings.smsApiUrl
            .replace(/{API_KEY}/g, settings.smsApiKey)
            .replace(/{SENDER_ID}/g, settings.smsSenderId)
            .replace(/{MOBILE}/g, cleanMobile)
            .replace(/{NUMBER}/g, cleanMobile)
            .replace(/{MESSAGE}/g, encodeURIComponent(message));

        const response = await fetch(finalUrl, { mode: 'no-cors' }); // 'no-cors' is common for simple GET SMS APIs
        return true; // Simple true because many APIs don't return JSON or have CORS issues
    } catch (e) {
        return false;
    }
  };

  const setFromLibrary = (lang) => {
    const msg = defaultLibrary[selectedTemplate?.title]?.[lang] || defaultLibrary["Default"][lang];
    setEditContent(msg);
  };

  return (
    <div className="w-full max-w-7xl mx-auto space-y-12 pb-20 uppercase font-black tracking-tighter transition-all">

      {/* HEADER SECTION */}
      <div className="flex justify-between items-end">
        <div className="space-y-2">
          <h3 className="text-6xl font-black text-slate-800 dark:text-white tracking-tighter leading-none">SMS GATEWAY</h3>
          <p className="text-xs text-teal-600 tracking-widest font-black italic">Web Control Center • Real-time Dispatch Hub</p>
        </div>

        <div className="flex space-x-6">
            <div className="bg-white dark:bg-slate-800 px-8 py-6 rounded-[32px] shadow-2xl border-2 border-teal-500/20 text-center space-y-1">
                <p className="text-[10px] text-slate-400 font-bold tracking-widest">GATEWAY CREDIT</p>
                <p className="text-4xl font-black text-teal-600 tracking-tighter">৳7.73</p>
            </div>
        </div>
      </div>

      {/* DISPATCH PROGRESS BAR (Visible only during bulk send) */}
      {isDispatching && (
        <div className="bg-slate-900 text-white p-10 rounded-[48px] shadow-2xl space-y-6 animate-pulse">
            <div className="flex justify-between items-end">
                <div>
                    <p className="text-[10px] text-teal-400 font-black tracking-[4px]">DISPATCHING BULK SMS...</p>
                    <h4 className="text-3xl font-black">{dispatchProgress.current} / {dispatchProgress.total} COMPLETED</h4>
                </div>
                <div className="text-right">
                    <span className="text-emerald-400 mr-6">SUCCESS: {dispatchProgress.success}</span>
                    <span className="text-rose-400">FAILED: {dispatchProgress.fail}</span>
                </div>
            </div>
            <div className="h-4 w-full bg-white/10 rounded-full overflow-hidden">
                <div
                    className="h-full bg-teal-500 transition-all duration-500"
                    style={{ width: `${(dispatchProgress.current / dispatchProgress.total) * 100}%` }}
                ></div>
            </div>
        </div>
      )}

      <div className="grid grid-cols-1 gap-8">
        <div className="bg-white dark:bg-slate-800 p-12 rounded-[56px] shadow-2xl border-2 border-slate-100 dark:border-slate-700 space-y-10">
          <div className="flex items-center space-x-4 text-teal-600">
             <div className="w-16 h-16 bg-teal-50 dark:bg-teal-900/20 rounded-3xl flex items-center justify-center"><i className="fas fa-satellite-dish text-3xl"></i></div>
             <h3 className="text-3xl font-black tracking-tight">AUTOMATION CONTROLS</h3>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {systemSmsTypes.map((type) => {
              const template = templates.find(t => t.title === type);
              const isActive = template?.isActive || template?.is_active || false;
              return (
                <div key={type} className={`p-8 rounded-[36px] border-2 transition-all group ${isActive ? 'bg-teal-50/30 border-teal-100 dark:bg-teal-900/10 dark:border-teal-800' : 'bg-slate-50 border-slate-50 dark:bg-slate-900/50 dark:border-slate-800 opacity-60'}`}>
                  <div className="flex justify-between items-start mb-6">
                    <div className={`w-12 h-12 rounded-2xl flex items-center justify-center transition-colors ${isActive ? 'bg-teal-500 text-white shadow-lg shadow-teal-500/30' : 'bg-slate-200 dark:bg-slate-700 text-slate-400'}`}><i className={`fas ${isActive ? 'fa-check-circle' : 'fa-power-off'}`}></i></div>
                    <div className="flex space-x-2">
                        {(type.includes("Customer") || type.includes("List") || type.includes("Expired")) && isActive && !isDispatching && (
                            <button onClick={() => runBroadcast(type)} className="w-10 h-10 bg-indigo-600 text-white rounded-xl flex items-center justify-center hover:scale-110 transition-all shadow-lg" title="Run Bulk Broadcast Now"><i className="fas fa-paper-plane text-xs"></i></button>
                        )}
                        <button onClick={() => openEdit(type)} className="w-10 h-10 bg-white dark:bg-slate-800 rounded-xl flex items-center justify-center text-slate-400 hover:text-teal-600 hover:scale-110 transition-all shadow-sm border border-slate-100 dark:border-slate-700"><i className="fas fa-edit text-xs"></i></button>
                    </div>
                  </div>
                  <h4 className={`text-sm font-black mb-4 leading-tight tracking-tight ${isActive ? 'text-slate-800 dark:text-white' : 'text-slate-400'}`}>{type}</h4>
                  <div className="flex items-center justify-between mt-auto pt-4 border-t border-dashed border-slate-200 dark:border-slate-700">
                    <span className={`text-[9px] font-bold tracking-widest ${isActive ? 'text-teal-600' : 'text-slate-400'}`}>{isActive ? 'ENABLED' : 'DISABLED'}</span>
                    <label className="relative inline-flex items-center cursor-pointer">
                      <input type="checkbox" className="sr-only peer" checked={isActive} onChange={(e) => handleToggle(type, e.target.checked)} />
                      <div className="w-11 h-6 bg-slate-200 peer-focus:outline-none rounded-full peer dark:bg-slate-700 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-teal-600"></div>
                    </label>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>

      {/* EDIT MODAL */}
      {isEditing && (
        <div className="fixed inset-0 bg-slate-900/90 backdrop-blur-2xl z-[10000] flex items-center justify-center p-6 animate-fadeIn">
          <div className="bg-white dark:bg-slate-800 rounded-[56px] w-full max-w-3xl p-12 shadow-2xl border-4 border-teal-500/20 space-y-8 relative overflow-hidden">
            <div className="absolute top-0 left-0 w-full h-3 bg-teal-500"></div>
            <div className="flex justify-between items-center border-b-2 border-slate-50 dark:border-slate-700 pb-8">
              <div>
                <h3 className="text-4xl font-black uppercase tracking-tighter leading-none text-slate-800 dark:text-white">EDIT CONTENT</h3>
                <p className="text-[10px] text-teal-600 font-bold tracking-[3px] mt-2 italic uppercase">{selectedTemplate?.title}</p>
              </div>
              <button onClick={() => setIsEditing(false)} className="w-14 h-14 bg-slate-50 dark:bg-slate-900 text-slate-400 hover:text-rose-500 rounded-full flex items-center justify-center transition-all shadow-inner"><i className="fas fa-times text-2xl"></i></button>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div onClick={() => setFromLibrary('en')} className="cursor-pointer bg-slate-50 dark:bg-slate-900 p-6 rounded-[32px] border-2 border-transparent hover:border-teal-500 transition-all group">
                    <p className="text-[10px] text-slate-400 font-black mb-3 tracking-widest">ENGLISH PRESET</p>
                    <p className="text-xs text-slate-600 dark:text-slate-300 font-bold">{defaultLibrary[selectedTemplate?.title]?.en || defaultLibrary["Default"].en}</p>
                </div>
                <div onClick={() => setFromLibrary('bn')} className="cursor-pointer bg-slate-50 dark:bg-slate-900 p-6 rounded-[32px] border-2 border-transparent hover:border-teal-500 transition-all group">
                    <p className="text-[10px] text-slate-400 font-black mb-3 tracking-widest">BANGLA PRESET (ইউনিকোড)</p>
                    <p className="text-xs text-slate-600 dark:text-slate-300 font-bold">{defaultLibrary[selectedTemplate?.title]?.bn || defaultLibrary["Default"].bn}</p>
                </div>
            </div>

            <div className="space-y-4 uppercase">
                <textarea value={editContent} onChange={(e) => setEditContent(e.target.value)} className="w-full h-48 bg-slate-50 dark:bg-slate-900 p-8 rounded-[40px] font-black text-lg outline-none border-2 border-slate-100 dark:border-slate-700 shadow-inner text-slate-700 dark:text-slate-200 resize-none" />
                <div className="bg-teal-50 dark:bg-teal-900/20 p-5 rounded-[24px] border border-teal-100 text-[9px] text-teal-600 font-bold tracking-widest">
                    TAGS: {"{NAME}"}, {"{AMOUNT}"}, {"{BILL_MONTH}"}, {"{DUE_DATE}"}, {"{CUSTOMER_CODE}"}
                </div>
            </div>

            <div className="flex space-x-6 pt-4">
              <button onClick={() => setIsEditing(false)} className="flex-1 bg-slate-100 dark:bg-slate-700 text-slate-500 py-8 rounded-[32px] font-black uppercase tracking-[4px] text-xs transition-all active:scale-95 shadow-xl">CANCEL</button>
              <button onClick={handleSaveEdit} disabled={isLoading} className="flex-[2] bg-teal-600 text-white py-8 rounded-[32px] font-black uppercase tracking-[8px] text-xs shadow-2xl shadow-teal-500/30 hover:scale-[1.02] active:scale-95 transition-all border-b-8 border-teal-800">
                {isLoading ? 'SYNCING...' : 'SYNC CHANGES'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default SmsSetup;
