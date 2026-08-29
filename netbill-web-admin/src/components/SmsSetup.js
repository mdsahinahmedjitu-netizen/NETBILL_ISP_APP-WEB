import React, { useState, useEffect } from 'react';
import { supabase } from '../supabaseClient';

const SmsSetup = ({ store, t, setActivePage }) => {
  const [isEditing, setIsEditing] = useState(false);
  const [selectedTemplate, setSelectedTemplate] = useState(null);
  const [editContent, setEditContent] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  // Test SMS State
  const [showTestModal, setShowTestModal] = useState(false);
  const [testMobile, setTestMobile] = useState('');
  const [testMsg, setTestMsg] = useState('NetBill Test Message. Hello!');

  const settings = store.settings || {};
  const templates = store.smsTemplates || [];
  const gatewayCredit = store.smsBalance || '---';

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
    "Inactive Customer List", "Failed to Disable at Mikrotik", "Expired Customer",
    "Expiry Reminder (Tomorrow)", "Expiry Reminder (Today)", "Staff Salary Alert"
  ];

  const defaultLibrary = {
    "Collection": {
        en: "Dear {NAME}, your payment of {AMOUNT} TK has been received. Thank you.",
        bn: "প্রিয় {NAME}, আপনার {AMOUNT} টাকা পেমেন্ট গ্রহণ করা হয়েছে। ধন্যবাদ।"
    },
    "Expiry Reminder (Today)": {
        en: "Dear {NAME}, your internet validity expires today. Please pay {AMOUNT} TK to avoid disconnection. Thank you.",
        bn: "প্রিয় {NAME}, আপনার ইন্টারনেটের মেয়াদ আজ শেষ হবে। সংযোগ বিচ্ছিন্ন এড়াতে দ্রুত {AMOUNT} টাকা পরিশোধ করুন। ধন্যবাদ।"
    },
    "Default": {
        en: "Dear {NAME}, this is a notification from NetBill ISP regarding your account {CUSTOMER_CODE}.",
        bn: "প্রিয় {NAME}, আপনার অ্যাকাউন্ট {CUSTOMER_CODE} সংক্রান্ত একটি বিশেষ বিজ্ঞপ্তি।"
    }
  };

  const sendSmsApi = async (mobile, message, debug = false) => {
    try {
        let cleanMobile = mobile.replace(/[^0-9]/g, "");
        if (cleanMobile.startsWith('0')) { cleanMobile = '88' + cleanMobile; }
        else if (cleanMobile.length === 10) { cleanMobile = '880' + cleanMobile; }
        else if (!cleanMobile.startsWith('88')) { cleanMobile = '88' + cleanMobile; }

        if (cleanMobile.length < 11 || cleanMobile.length > 13) return false;

        const isUnicode = /[\u0980-\u09FF]/.test(message);
        const apiKey = (settings.smsApiKey || "").trim();
        const senderId = (settings.smsSenderId || "").trim();
        const msgType = isUnicode ? "unicode" : "text";

        let finalMsg = message;
        if (isUnicode) {
            finalMsg = message.replace(/\d/g, d => "০১২৩৪৫৬৭৮৯"[d]);
        }

        // Use Supabase Proxy (HTTPS) to bypass Mixed Content and CORS
        let finalUrl = `https://tglplinxvrqsrxeicvpr.supabase.co/functions/v1/sms-proxy?apikey=${apiKey}&callerID=${senderId}&number=${cleanMobile}&message=${encodeURIComponent(finalMsg)}&type=${msgType}`;

        console.log("SMS Final URL:", finalUrl);

        if (debug) {
            window.open(finalUrl, '_blank');
            return true;
        }

        const img = new Image();
        img.src = finalUrl;
        return true;
    } catch (e) {
        console.error("SMS Logic Error:", e);
        return false;
    }
  };

  const runBroadcast = async (type) => {
    const template = templates.find(t => t.title === type);
    if (!template || !(template.isActive || template.is_active)) return alert("Enable template first!");

    let targets = store.customers;
    if (type.includes("Due")) targets = targets.filter(c => (parseFloat(c.currentDue) || parseFloat(c.current_due)) > 0);
    if (type.includes("Expired")) targets = targets.filter(c => c.status !== "Active");
    if (type === "Expiry Reminder (Today)") {
        const todayISO = new Date().toLocaleDateString('en-CA');
        targets = targets.filter(c => (c.expireDate || c.expire_date) === todayISO && c.status === "Active");
    }

    if (targets.length === 0) return alert("No targets found.");
    if (!window.confirm(`Broadcasting to ${targets.length} customers. Proceed?`)) return;

    setIsDispatching(true);
    setDispatchProgress({ current: 0, total: targets.length, success: 0, fail: 0 });

    for (let i = 0; i < targets.length; i++) {
        const c = targets[i];
        const currentMonth = new Date().toLocaleString('default', { month: 'long', year: 'numeric' });
        const msg = (template.messageContent || template.message_content)
            .replace(/{NAME}/g, c.name || '')
            .replace(/{AMOUNT}/g, Math.floor(c.currentDue || c.current_due || 0))
            .replace(/{DUE}/g, Math.floor(c.currentDue || c.current_due || 0))
            .replace(/{ZONE}/g, c.zone || '')
            .replace(/{BILL_MONTH}/g, currentMonth)
            .replace(/{CUSTOMER_CODE}/g, c.customerCode || c.customer_code || '');

        const ok = await sendSmsApi(c.mobile, msg);
        setDispatchProgress(prev => ({ ...prev, current: i + 1, success: ok ? prev.success + 1 : prev.success, fail: !ok ? prev.fail + 1 : prev.fail }));

        // Special handling for "Failed to Disable at Mikrotik" - Always notify Admin if failed
        if (type === "Failed to Disable at Mikrotik" && settings.companyPhone) {
            const adminMsg = `ADMIN ALERT: Failed to disable ${c.name} (${c.customerCode}) on Mikrotik. Please check manually.`;
            sendSmsApi(settings.companyPhone, adminMsg);
        }

        await supabase.from('sms_logs').insert({
            id: `LOG-${Date.now()}-${i}`, customer_id: c.id, customer_name: c.name, mobile: c.mobile,
            notification_type: type, message: msg, status: ok ? 'Sent' : 'Failed', sent_timestamp: new Date().toISOString()
        });
    }
    setIsDispatching(false);
    alert("Task Finished!");
  };

  const handleToggle = async (type, active) => {
    const template = templates.find(t => t.title === type);
    try {
        await supabase.from('sms_templates').upsert({
            id: template?.id || `TPL-${Date.now()}`, title: type, category: 'System',
            message_content: template?.messageContent || template?.message_content || (defaultLibrary[type]?.en || defaultLibrary["Default"].en),
            is_active: active
        });
    } catch (e) { alert("Failed: " + e.message); }
  };

  const openEdit = (type) => {
    let template = templates.find(t => t.title === type);
    if (!template) template = { title: type, message_content: (defaultLibrary[type]?.en || defaultLibrary["Default"].en) };
    setSelectedTemplate(template);
    setEditContent(template.messageContent || template.message_content || '');
    setIsEditing(true);
  };

  const handleSaveEdit = async () => {
    setIsLoading(true);
    try {
        await supabase.from('sms_templates').upsert({
            id: selectedTemplate.id || `TPL-${Date.now()}`, title: selectedTemplate.title,
            message_content: editContent, category: 'System', is_active: true
        });
        setIsEditing(false);
    } catch (e) { alert("Error!"); }
    finally { setIsLoading(false); }
  };

  return (
    <div className="w-full max-w-7xl mx-auto space-y-6 md:space-y-12 pb-20 uppercase font-black tracking-tighter px-2 md:px-0 transition-all">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-end gap-6">
        <div className="flex items-center space-x-4">
           <button onClick={() => setActivePage('dashboard')} className="w-10 h-10 md:w-12 md:h-12 bg-white dark:bg-slate-800 rounded-xl flex items-center justify-center text-teal-600 shadow-sm border border-slate-100">
              <i className="fas fa-arrow-left"></i>
           </button>
           <div className="space-y-1">
              <h3 className="text-2xl md:text-6xl font-black text-slate-800 dark:text-white tracking-tighter leading-none tracking-widest uppercase">SMS Gateway</h3>
              <p className="text-[8px] md:text-xs text-teal-600 tracking-widest font-black italic uppercase">Web Control Center</p>
           </div>
        </div>
        <div className="flex items-center space-x-3 md:space-x-6 w-full md:w-auto">
            <button
              onClick={() => setShowTestModal(true)}
              className="flex-1 md:flex-none bg-indigo-600 text-white px-4 py-2.5 md:px-8 md:py-4 rounded-xl md:rounded-2xl shadow-xl font-black text-[9px] md:text-xs hover:scale-105 active:scale-95 transition-all flex items-center justify-center space-x-2"
            >
               <i className="fas fa-vial"></i>
               <span>TEST GATEWAY</span>
            </button>

            <div className="flex-1 md:flex-none bg-white dark:bg-slate-800 px-4 py-2 md:px-8 md:py-6 rounded-xl md:rounded-[32px] shadow-2xl border-2 border-teal-500/20 text-center">
                <p className="text-[8px] md:text-[10px] text-slate-400 font-bold tracking-widest uppercase mb-1">CREDIT</p>
                <p className="text-xl md:text-4xl font-black text-teal-600 tracking-tighter leading-none">{gatewayCredit}</p>
            </div>
        </div>
      </div>

      {isDispatching && (
        <div className="bg-slate-950 text-white p-6 md:p-10 rounded-2xl md:rounded-[48px] shadow-2xl space-y-4 md:space-y-6 animate-pulse">
            <h4 className="text-lg md:text-3xl font-black uppercase">{dispatchProgress.current} / {dispatchProgress.total} COMPLETED</h4>
            <div className="h-2 md:h-4 w-full bg-white/10 rounded-full overflow-hidden shadow-inner">
                <div className="h-full bg-teal-500 transition-all duration-300" style={{ width: `${(dispatchProgress.current / dispatchProgress.total) * 100}%` }}></div>
            </div>
        </div>
      )}

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 md:gap-6">
        {systemSmsTypes.map((type) => {
          const template = templates.find(t => t.title === type);
          const isActive = template?.is_active || template?.isActive || false;
          return (
            <div key={type} className={`p-6 md:p-8 rounded-[24px] md:rounded-[36px] border-2 transition-all flex flex-col shadow-sm ${isActive ? 'bg-white dark:bg-slate-800 border-teal-500/20' : 'bg-slate-50 dark:bg-slate-900/50 opacity-60'}`}>
              <div className="flex justify-between items-start mb-4 md:mb-6">
                <div className={`w-10 h-10 md:w-12 md:h-12 rounded-xl md:rounded-2xl flex items-center justify-center text-lg ${isActive ? 'bg-teal-500 text-white shadow-lg shadow-teal-500/40' : 'bg-slate-200 text-slate-400 dark:bg-slate-800'}`}><i className={`fas ${isActive ? 'fa-check-circle' : 'fa-power-off'}`}></i></div>
                <div className="flex space-x-2">
                    {isActive && <button onClick={() => runBroadcast(type)} className="w-8 h-8 md:w-10 md:h-10 bg-indigo-600 text-white rounded-lg md:rounded-xl flex items-center justify-center hover:scale-110 transition-all"><i className="fas fa-paper-plane text-[10px]"></i></button>}
                    <button onClick={() => openEdit(type)} className="w-8 h-8 md:w-10 md:h-10 bg-white dark:bg-slate-950 rounded-lg md:rounded-xl flex items-center justify-center text-slate-400 border border-slate-100 dark:border-slate-800 hover:text-teal-600 transition-colors"><i className="fas fa-edit text-[10px]"></i></button>
                </div>
              </div>
              <h4 className={`text-[11px] md:text-sm font-black mb-4 leading-tight uppercase ${isActive ? 'text-slate-800 dark:text-white' : 'text-slate-400'}`}>{type}</h4>
              <div className="flex items-center justify-between mt-auto pt-4 border-t border-dashed border-slate-100 dark:border-slate-700">
                <span className={`text-[8px] font-black tracking-widest ${isActive ? 'text-teal-600' : 'text-slate-400'}`}>{isActive ? 'ENABLED' : 'DISABLED'}</span>
                <div
                    onClick={() => handleToggle(type, !isActive)}
                    className={`w-10 h-5 md:w-12 md:h-6 rounded-full relative cursor-pointer transition-all duration-300 ${isActive ? 'bg-teal-500' : 'bg-slate-300'}`}
                >
                    <div className={`absolute top-0.5 md:top-1 w-4 h-4 bg-white rounded-full transition-all duration-300 ${isActive ? 'left-5 md:left-7' : 'left-1'}`}></div>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {isEditing && (
        <div className="fixed inset-0 bg-slate-900/90 backdrop-blur-2xl z-[10000] flex items-center justify-center p-6 uppercase font-black">
          <div className="bg-white rounded-[56px] w-full max-w-3xl p-12 shadow-2xl border-4 border-teal-500/20 space-y-8">
            <div className="flex justify-between items-center">
              <h3 className="text-4xl font-black uppercase">{selectedTemplate?.title}</h3>
              <button onClick={() => setIsEditing(false)} className="w-12 h-12 bg-rose-50 text-rose-500 rounded-full flex items-center justify-center"><i className="fas fa-times"></i></button>
            </div>

            <div className="space-y-4">
              <textarea value={editContent} onChange={(e) => setEditContent(e.target.value)} className="w-full h-48 bg-slate-50 p-8 rounded-[40px] font-black text-lg outline-none" />

              <div className="bg-teal-50/50 p-6 rounded-[32px] border-2 border-dashed border-teal-200">
                <p className="text-[10px] text-teal-600 font-black tracking-widest mb-4">CLICK TO INSERT TAGS / ট্যাগগুলো যুক্ত করতে ক্লিক করুন:</p>
                <div className="flex flex-wrap gap-2">
                  {[
                    { tag: '{NAME}', label: 'Name' },
                    { tag: '{AMOUNT}', label: 'Due TK' },
                    { tag: '{DUE}', label: 'Due TK' },
                    { tag: '{ZONE}', label: 'Area' },
                    { tag: '{BILL_MONTH}', label: 'Month' },
                    { tag: '{CUSTOMER_CODE}', label: 'Cust ID' },
                    { tag: '{BALANCE}', label: 'Pao-na/Bal' },
                    { tag: '{TYPE}', label: 'Add/Paid' }
                  ].map(item => (
                    <button
                      key={item.tag}
                      onClick={() => setEditContent(prev => prev + item.tag)}
                      className="bg-white px-4 py-2 rounded-xl text-[10px] font-black border border-teal-100 hover:bg-teal-500 hover:text-white transition-all shadow-sm"
                    >
                      {item.tag} ({item.label})
                    </button>
                  ))}
                </div>
              </div>
            </div>

            <div className="flex space-x-6">
              <button onClick={() => setIsEditing(false)} className="flex-1 bg-slate-100 py-6 rounded-3xl font-black">CANCEL</button>
              <button onClick={handleSaveEdit} disabled={isLoading} className="flex-[2] bg-teal-600 text-white py-6 rounded-3xl font-black shadow-xl">SAVE CHANGES</button>
            </div>
          </div>
        </div>
      )}

      {/* SAFE TEST MODAL */}
      {showTestModal && (
        <div className="fixed inset-0 bg-slate-900/90 backdrop-blur-2xl z-[10000] flex items-center justify-center p-6 uppercase font-black">
          <div className="bg-white dark:bg-slate-800 rounded-[56px] w-full max-w-md p-12 shadow-2xl border-4 border-indigo-500/20 space-y-8 relative overflow-hidden">
             <div className="absolute top-0 left-0 w-full h-3 bg-indigo-600"></div>
             <div className="text-center space-y-2">
                <h3 className="text-3xl font-black tracking-tighter leading-none">Safe Test Mode</h3>
                <p className="text-[10px] text-slate-400 font-bold tracking-[3px]">Send SMS to a specific number only</p>
             </div>

             <div className="space-y-6">
                <div className="space-y-2">
                   <label className="text-[10px] text-slate-400 ml-4 font-black tracking-widest">TARGET MOBILE NO</label>
                   <input
                      type="text"
                      placeholder="e.g. 01700000000"
                      value={testMobile}
                      onChange={e => setTestMobile(e.target.value)}
                      className="w-full bg-slate-50 dark:bg-slate-900 p-5 rounded-3xl font-black text-xl outline-none shadow-inner text-indigo-600"
                   />
                </div>
                <div className="space-y-2">
                   <label className="text-[10px] text-slate-400 ml-4 font-black tracking-widest">MESSAGE CONTENT</label>
                   <textarea
                      rows="3"
                      value={testMsg}
                      onChange={e => setTestMsg(e.target.value)}
                      className="w-full bg-slate-50 dark:bg-slate-900 p-6 rounded-3xl font-black text-sm outline-none resize-none shadow-inner"
                   ></textarea>
                </div>
             </div>

             <div className="grid grid-cols-1 gap-4">
                <button
                  onClick={async () => {
                    const ok = await sendSmsApi(testMobile, testMsg, true);
                    if (ok) {
                        // Insert log for test message
                        await supabase.from('sms_logs').insert({
                            id: `LOG-TEST-${Date.now()}`,
                            customer_name: 'Test Recipient',
                            mobile: testMobile,
                            notification_type: 'Test SMS',
                            message: testMsg,
                            status: 'Sent',
                            sent_timestamp: new Date().toISOString()
                        });
                        alert("Opening SMS Gateway in new tab for verification...");
                        setShowTestModal(false);
                    } else {
                        alert("Failed to generate URL!");
                    }
                  }}
                  className="w-full bg-indigo-600 text-white py-6 rounded-3xl font-black uppercase tracking-[5px] shadow-xl hover:scale-[1.02] active:scale-95 transition-all"
                >
                  SEND TEST SMS (BROWSER DEBUG)
                </button>
                <button onClick={() => setShowTestModal(false)} className="text-slate-400 text-xs font-bold uppercase hover:text-rose-500">Close Panel</button>
             </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default SmsSetup;
