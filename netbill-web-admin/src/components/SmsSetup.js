import React, { useState, useEffect } from 'react';
import { supabase } from '../supabaseClient';

const SmsSetup = ({ store, t }) => {
  const [isEditing, setIsEditing] = useState(false);
  const [selectedTemplate, setSelectedTemplate] = useState(null);
  const [editContent, setEditContent] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [gatewayCredit, setGatewayCredit] = useState('Checking...');

  // Test SMS State
  const [showTestModal, setShowTestModal] = useState(false);
  const [testMobile, setTestMobile] = useState('');
  const [testMsg, setTestMsg] = useState('NetBill Test Message. Hello!');

  const settings = store.settings || {};
  const templates = store.smsTemplates || [];

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
        en: "Dear {NAME}, your payment of {AMOUNT} TK has been received. Thank you.",
        bn: "প্রিয় {NAME}, আপনার {AMOUNT} টাকা পেমেন্ট গ্রহণ করা হয়েছে। ধন্যবাদ।"
    },
    "Default": {
        en: "Dear {NAME}, this is a notification from NetBill ISP regarding your account {CUSTOMER_CODE}.",
        bn: "প্রিয় {NAME}, আপনার অ্যাকাউন্ট {CUSTOMER_CODE} সংক্রান্ত একটি বিশেষ বিজ্ঞপ্তি।"
    }
  };

  useEffect(() => {
    const checkCredit = async () => {
      const apiKey = settings.smsApiKey || settings.sms_api_key;
      if (!apiKey) { setGatewayCredit('N/A'); return; }
      try {
        const response = await fetch(`https://bulksmsdhaka.net/api/getbalance?apikey=${apiKey}`);
        const data = await response.json();
        if (data && data.balance) setGatewayCredit(`৳${data.balance}`);
        else setGatewayCredit('Error');
      } catch (e) { setGatewayCredit('৳0.00'); }
    };
    checkCredit();
  }, [settings.smsApiKey]);

  const sendSmsApi = async (mobile, message) => {
    try {
        let cleanMobile = mobile.replace(/[^0-9]/g, "");
        if (cleanMobile.startsWith('880')) cleanMobile = cleanMobile.substring(2);
        if (!cleanMobile.startsWith('0')) cleanMobile = '0' + cleanMobile;
        if (cleanMobile.length !== 11) return false;

        const isUnicode = /[\u0980-\u09FF]/.test(message);
        const typeParam = isUnicode ? "&type=unicode" : "";

        const finalUrl = settings.smsApiUrl
            .replace(/{API_KEY}/g, settings.smsApiKey)
            .replace(/{SENDER_ID}/g, settings.smsSenderId || '1234')
            .replace(/{MOBILE}/g, cleanMobile)
            .replace(/{NUMBER}/g, cleanMobile)
            .replace(/{MESSAGE}/g, encodeURIComponent(message)) + typeParam;

        // FORM SUBMISSION BYPASS (MOST RELIABLE)
        const iframe = document.createElement('iframe');
        iframe.style.display = 'none';
        iframe.name = `sms_frame_${Date.now()}`;
        document.body.appendChild(iframe);

        const form = document.createElement('form');
        form.method = 'GET';
        form.action = finalUrl.split('?')[0];
        form.target = iframe.name;

        const urlParams = new URLSearchParams(finalUrl.split('?')[1]);
        urlParams.forEach((val, key) => {
            const input = document.createElement('input');
            input.type = 'hidden';
            input.name = key;
            input.value = val;
            form.appendChild(input);
        });

        document.body.appendChild(form);
        form.submit();

        setTimeout(() => {
            document.body.removeChild(form);
            document.body.removeChild(iframe);
        }, 3000);

        return true;
    } catch (e) { return false; }
  };

  const runBroadcast = async (type) => {
    const template = templates.find(t => t.title === type);
    if (!template || !(template.isActive || template.is_active)) return alert("Enable template first!");

    let targets = store.customers;
    if (type.includes("Due")) targets = targets.filter(c => (parseFloat(c.currentDue) || parseFloat(c.current_due)) > 0);
    if (type.includes("Expired")) targets = targets.filter(c => c.status !== "Active");

    if (targets.length === 0) return alert("No targets found.");
    if (!window.confirm(`Broadcasting to ${targets.length} customers. Proceed?`)) return;

    setIsDispatching(true);
    setDispatchProgress({ current: 0, total: targets.length, success: 0, fail: 0 });

    for (let i = 0; i < targets.length; i++) {
        const c = targets[i];
        const msg = (template.messageContent || template.message_content)
            .replace(/{NAME}/g, c.name || '')
            .replace(/{AMOUNT}/g, Math.floor(c.currentDue || c.current_due || 0))
            .replace(/{DUE}/g, Math.floor(c.currentDue || c.current_due || 0))
            .replace(/{CUSTOMER_CODE}/g, c.customerCode || c.customer_code || '');

        const ok = await sendSmsApi(c.mobile, msg);
        setDispatchProgress(prev => ({ ...prev, current: i + 1, success: ok ? prev.success + 1 : prev.success, fail: !ok ? prev.fail + 1 : prev.fail }));

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
    <div className="w-full max-w-7xl mx-auto space-y-12 pb-20 uppercase font-black tracking-tighter">
      <div className="flex justify-between items-end">
        <div className="space-y-2">
          <h3 className="text-6xl font-black text-slate-800 dark:text-white tracking-tighter leading-none tracking-widest">SMS GATEWAY</h3>
          <p className="text-xs text-teal-600 tracking-widest font-black italic">Web Control Center</p>
        </div>
        <div className="flex space-x-6">
            <button
              onClick={() => setShowTestModal(true)}
              className="bg-indigo-600 text-white px-8 py-4 rounded-2xl shadow-xl font-black text-xs hover:scale-105 active:scale-95 transition-all flex items-center space-x-3"
            >
               <i className="fas fa-vial"></i>
               <span>TEST GATEWAY</span>
            </button>

            <div className="bg-white dark:bg-slate-800 px-8 py-6 rounded-[32px] shadow-2xl border-2 border-teal-500/20 text-center">
                <p className="text-[10px] text-slate-400 font-bold tracking-widest">GATEWAY CREDIT</p>
                <p className="text-4xl font-black text-teal-600 tracking-tighter">{gatewayCredit}</p>
            </div>
        </div>
      </div>

      {isDispatching && (
        <div className="bg-slate-900 text-white p-10 rounded-[48px] shadow-2xl space-y-6">
            <h4 className="text-3xl font-black">{dispatchProgress.current} / {dispatchProgress.total} COMPLETED</h4>
            <div className="h-4 w-full bg-white/10 rounded-full overflow-hidden">
                <div className="h-full bg-teal-500 transition-all" style={{ width: `${(dispatchProgress.current / dispatchProgress.total) * 100}%` }}></div>
            </div>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {systemSmsTypes.map((type) => {
          const template = templates.find(t => t.title === type);
          const isActive = template?.is_active || template?.isActive || false;
          return (
            <div key={type} className={`p-8 rounded-[36px] border-2 transition-all ${isActive ? 'bg-teal-50/30 border-teal-100' : 'bg-slate-50 opacity-60'}`}>
              <div className="flex justify-between items-start mb-6">
                <div className={`w-12 h-12 rounded-2xl flex items-center justify-center ${isActive ? 'bg-teal-500 text-white' : 'bg-slate-200 text-slate-400'}`}><i className={`fas ${isActive ? 'fa-check-circle' : 'fa-power-off'}`}></i></div>
                <div className="flex space-x-2">
                    {isActive && <button onClick={() => runBroadcast(type)} className="w-10 h-10 bg-indigo-600 text-white rounded-xl flex items-center justify-center"><i className="fas fa-paper-plane text-xs"></i></button>}
                    <button onClick={() => openEdit(type)} className="w-10 h-10 bg-white rounded-xl flex items-center justify-center text-slate-400 border"><i className="fas fa-edit text-xs"></i></button>
                </div>
              </div>
              <h4 className={`text-sm font-black mb-4 leading-tight ${isActive ? 'text-slate-800' : 'text-slate-400'}`}>{type}</h4>
              <div className="flex items-center justify-between mt-auto pt-4 border-t border-dashed">
                <span className="text-[9px] font-bold">{isActive ? 'ENABLED' : 'DISABLED'}</span>
                <input type="checkbox" checked={isActive} onChange={(e) => handleToggle(type, e.target.checked)} className="w-10 h-5" />
              </div>
            </div>
          );
        })}
      </div>

      {isEditing && (
        <div className="fixed inset-0 bg-slate-900/90 backdrop-blur-2xl z-[10000] flex items-center justify-center p-6 uppercase font-black">
          <div className="bg-white rounded-[56px] w-full max-w-3xl p-12 shadow-2xl border-4 border-teal-500/20 space-y-8">
            <h3 className="text-4xl font-black uppercase">{selectedTemplate?.title}</h3>
            <textarea value={editContent} onChange={(e) => setEditContent(e.target.value)} className="w-full h-48 bg-slate-50 p-8 rounded-[40px] font-black text-lg outline-none" />
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
                    const ok = await sendSmsApi(testMobile, testMsg);
                    alert(ok ? "Message Request Sent!" : "Failed to Send!");
                    if(ok) setShowTestModal(false);
                  }}
                  className="w-full bg-indigo-600 text-white py-6 rounded-3xl font-black uppercase tracking-[5px] shadow-xl hover:scale-[1.02] active:scale-95 transition-all"
                >
                  SEND TEST SMS
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
