import React, { useState } from 'react';
import { supabase } from '../supabaseClient';

const SupportTickets = ({ store, session, t, setActivePage }) => {
  const [activeTab, setActiveTab] = useState('Open');
  const [isUpdating, setIsUpdating] = useState(false);
  const [showAddModal, setShowAddModal] = useState(false);
  const [customerSearch, setCustomerSearch] = useState('');

  // Quick Issues State - Persistent using LocalStorage
  const [quickIssues, setQuickIssues] = useState(() => {
    const saved = localStorage.getItem('netbill_support_issues');
    return saved ? JSON.parse(saved) : [
      "ইন্টারনেট স্লো (Internet Slow)",
      "ইন্টারনেট নেই (No Internet)",
      "লাইন কাটা (Line Broken)",
      "রাউটারে সমস্যা (Router Issue)",
      "পাসওয়ার্ড পরিবর্তন (Wifi Pass Change)",
      "পেমেন্ট সমস্যা (Payment Problem)"
    ];
  });
  const [customIssueInput, setCustomIssueInput] = useState('');

  const [newTicket, setNewTicket] = useState({
    customerId: '',
    subject: '',
    description: '',
    priority: 'Normal'
  });

  const filteredTickets = store.tickets?.filter(tk => {
    // Role-based filtering: Customers only see their own tickets
    if (session.role === 'customer') {
      return (tk.customerId === session.data.id || tk.customer_id === session.data.id);
    }

    if (activeTab === 'All') return true;
    return tk.status === activeTab;
  }).sort((a, b) => new Date(b.createdAt || b.created_at) - new Date(a.createdAt || a.created_at)) || [];

  const searchedCustomers = store.customers?.filter(c =>
    c.name?.toLowerCase().includes(customerSearch.toLowerCase()) ||
    c.customerCode?.includes(customerSearch) ||
    c.pppoeUsername?.toLowerCase().includes(customerSearch.toLowerCase()) ||
    c.mobile?.includes(customerSearch)
  ).slice(0, 8);

  const handleCreateTicket = async (e) => {
    e.preventDefault();
    if (!newTicket.customerId || !newTicket.subject) return alert("Please select customer and enter subject");

    setIsUpdating(true);
    try {
      const customer = store.customers.find(c => c.id === newTicket.customerId);
      const ticketId = `TKT-${Date.now()}`;

      const { error } = await supabase.from('support_tickets').insert({
        id: ticketId,
        customer_id: newTicket.customerId,
        customer_code: customer?.customerCode || '',
        subject: newTicket.subject,
        description: newTicket.description,
        priority: newTicket.priority,
        status: 'Open',
        created_at: new Date().toISOString(),
        created_by: session.data.name
      });

      if (error) throw error;

      // --- SMS NOTIFICATION TRIGGER ---
      const settings = store.settings || {};
      if (settings.smsApiUrl && customer?.mobile) {
          const template = store.smsTemplates?.find(t => t.title === 'Complain to Customer' && (t.isActive || t.is_active));

          if (template) {
              let msg = (template.messageContent || template.message_content)
                  .replace(/{NAME}/g, customer.name || '')
                  .replace(/{SUBJECT}/g, newTicket.subject)
                  .replace(/{TICKET_ID}/g, ticketId)
                  .replace(/{CUSTOMER_CODE}/g, customer.customerCode || '');

              let cleanMobile = customer.mobile.replace(/[^0-9]/g, "");
              if (cleanMobile.startsWith('0')) { cleanMobile = '88' + cleanMobile; }
              else if (cleanMobile.length === 10) { cleanMobile = '880' + cleanMobile; }
              else if (!cleanMobile.startsWith('88')) { cleanMobile = '88' + cleanMobile; }

              const isUnicode = /[\u0980-\u09FF]/.test(msg);
              const msgType = isUnicode ? "unicode" : "text";
              const apiKey = (settings.smsApiKey || "").trim();
              const senderId = (settings.smsSenderId || "").trim();

              const finalUrl = `http://bulksmsbd.net/api/smsapi?api_key=${apiKey}&type=${msgType}&number=${cleanMobile}&senderid=${senderId}&message=${encodeURIComponent(msg)}`;

              // Dispatch using Image ping (CORS-safe)
              const img = new Image();
              img.src = finalUrl;

              // Log SMS to Database
              await supabase.from('sms_logs').insert({
                  customer_id: customer.id,
                  customer_name: customer.name,
                  mobile: cleanMobile,
                  notification_type: 'Complain to Customer (Auto)',
                  message: msg,
                  status: 'Sent',
                  sent_timestamp: new Date().toISOString()
              });
          }
      }

      alert("Ticket Created Successfully!");
      setShowAddModal(false);
      setNewTicket({ customerId: '', subject: '', description: '', priority: 'Normal' });
      setCustomerSearch('');
    } catch (e) {
      alert("Error: Failed to create ticket");
    } finally {
      setIsUpdating(false);
    }
  };

  const updateTicketStatus = async (id, newStatus) => {
    setIsUpdating(true);
    try {
      const { error } = await supabase.from('support_tickets').update({ status: newStatus }).eq('id', id);
      if (error) throw error;
    } catch (e) {
      alert("Update failed!");
    } finally {
      setIsUpdating(false);
    }
  };

  const addQuickIssue = () => {
    if (customIssueInput.trim() && !quickIssues.includes(customIssueInput)) {
      const updatedIssues = [...quickIssues, customIssueInput.trim()];
      setQuickIssues(updatedIssues);
      localStorage.setItem('netbill_support_issues', JSON.stringify(updatedIssues));
      setCustomIssueInput('');
    }
  };

  const getPriorityColor = (p) => {
    if (p === 'High') return 'bg-rose-100 text-rose-600 border-rose-200';
    if (p === 'Medium') return 'bg-amber-100 text-amber-600 border-amber-200';
    return 'bg-emerald-100 text-emerald-600 border-emerald-200';
  };

  return (
    <div className="w-full max-w-7xl mx-auto space-y-6 pb-20 uppercase font-black tracking-tighter transition-all">
      <div className="flex flex-col xl:flex-row justify-between items-start xl:items-end gap-4">
        <div className="flex items-center space-x-4">
           <button onClick={() => setActivePage('dashboard')} className="w-12 h-12 bg-white dark:bg-slate-800 rounded-2xl flex items-center justify-center text-amber-500 shadow-sm border border-slate-100">
              <i className="fas fa-arrow-left"></i>
           </button>
           <div className="space-y-1">
              <h3 className="text-4xl font-black text-slate-800 dark:text-white tracking-tighter leading-none tracking-widest">Complaints</h3>
              <p className="text-[10px] text-amber-600 tracking-widest font-black uppercase italic">Support System</p>
           </div>
        </div>

        <div className="flex flex-wrap items-center gap-3">
            {/* NEW ISSUE PRESET ADDER IN HEADER - NOW ALWAYS UPPERCASE */}
            <div className="flex items-center space-x-2 bg-slate-100 dark:bg-slate-900 p-1.5 rounded-[24px] border-2 border-indigo-500/10 shadow-inner">
               <input
                  type="text"
                  placeholder="NEW PRESET..."
                  value={customIssueInput}
                  onChange={e => setCustomIssueInput(e.target.value.toUpperCase())}
                  className="bg-transparent border-none outline-none text-[9px] font-black w-32 ml-3 uppercase placeholder:text-slate-300"
               />
               <button onClick={addQuickIssue} className="bg-indigo-600 text-white px-4 py-2 rounded-xl text-[9px] font-black shadow-md hover:scale-105 active:scale-95 transition-all">ADD</button>
            </div>

            <button onClick={() => setShowAddModal(true)} className="bg-emerald-600 text-white px-6 py-3 rounded-2xl font-black text-[10px] shadow-lg hover:scale-105 transition-all flex items-center space-x-2"><i className="fas fa-plus-circle"></i><span>NEW</span></button>

            <div className="flex items-center space-x-1.5 bg-slate-100 dark:bg-slate-900 p-1.5 rounded-[20px] shadow-inner">
               {['Open', 'Pending', 'Resolved', 'All'].map(tab => (<button key={tab} onClick={() => setActiveTab(tab)} className={`px-4 py-2 rounded-xl text-[8px] font-black transition-all ${activeTab === tab ? 'bg-white dark:bg-slate-800 text-amber-600 shadow-md' : 'text-slate-400'}`}>{tab}</button>))}
            </div>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4">
        {filteredTickets.map(tk => {
          const customer = store.customers?.find(c => c.id === tk.customerId || c.id === tk.customer_id);
          return (
            <div key={tk.id} className="bg-white dark:bg-slate-800 p-5 rounded-3xl shadow-lg border border-slate-100 dark:border-slate-700 flex flex-col xl:flex-row justify-between items-center gap-6 group hover:border-amber-500/30 transition-all">
               {/* LEFT SIDE: Subscriber Info & Actions (WIDER & LARGER TEXT) */}
               <div className="xl:w-1/3 w-full bg-slate-50 dark:bg-slate-900/50 p-6 rounded-[32px] border-2 border-slate-100 dark:border-slate-800 space-y-4 shadow-inner text-center md:text-left">
                  <div className="space-y-1">
                     <span className="text-[12px] text-indigo-600 font-black uppercase tracking-[4px]">Subscriber</span>
                     <h3 className="text-4xl font-black text-slate-800 dark:text-white uppercase tracking-tighter leading-none">{customer?.name || 'Unknown Client'}</h3>
                  </div>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-2">
                     <div className="space-y-1">
                        <span className="text-[14px] text-slate-400 font-black uppercase tracking-widest">Zone</span>
                        <p className="text-[18px] font-black text-slate-700 dark:text-slate-200 leading-tight">{customer?.zone || 'Global'}</p>
                     </div>
                     <div className="space-y-1">
                        <span className="text-[14px] text-slate-400 font-black uppercase tracking-widest">Phone</span>
                        <p className="text-[18px] font-black text-emerald-600 leading-none">{customer?.mobile || 'No Mobile'}</p>
                     </div>
                  </div>
                  <div className="flex flex-wrap gap-3 pt-4 border-t border-slate-200/50">
                    {tk.status !== 'Resolved' && (<button onClick={() => updateTicketStatus(tk.id, 'Resolved')} className="flex-1 bg-emerald-600 text-white py-4 rounded-2xl font-black text-xs shadow-lg hover:scale-105 active:scale-95 transition-all">RESOLVE</button>)}
                    {tk.status === 'Open' && (<button onClick={() => updateTicketStatus(tk.id, 'Pending')} className="flex-1 bg-amber-500 text-white py-4 rounded-2xl font-black text-xs shadow-lg hover:scale-105 active:scale-95 transition-all">PENDING</button>)}
                    {tk.status === 'Resolved' && (<button onClick={() => updateTicketStatus(tk.id, 'Open')} className="flex-1 bg-slate-800 text-white py-4 rounded-2xl font-black text-xs hover:bg-amber-600 transition-all">RE-OPEN</button>)}
                    <button onClick={async () => { if(window.confirm("Delete?")) await supabase.from('support_tickets').delete().eq('id', tk.id); }} className="w-14 h-14 bg-rose-100 text-rose-600 rounded-2xl flex items-center justify-center hover:bg-rose-600 hover:text-white transition-all shadow-md"><i className="fas fa-trash-alt text-lg"></i></button>
                  </div>
               </div>

               {/* LEFT SIDE MOVED TO RIGHT: Ticket Details */}
               <div className="flex-1 flex flex-col md:flex-row items-center md:items-center gap-6 text-center md:text-left">
                  <div className={`w-16 h-16 shrink-0 rounded-2xl flex items-center justify-center text-2xl shadow-inner border-2 ${tk.status === 'Resolved' ? 'bg-emerald-50 text-emerald-500 border-emerald-100' : 'bg-amber-50 text-amber-500 border-amber-100 animate-pulse'}`}><i className={`fas ${tk.status === 'Resolved' ? 'fa-check-circle' : 'fa-headset'}`}></i></div>
                  <div className="space-y-2 flex-1">
                     <div className="flex items-center justify-center md:justify-start space-x-3">
                        <span className={`px-3 py-1 rounded-lg text-[8px] font-black border-2 ${getPriorityColor(tk.priority || 'Normal')}`}>{tk.priority || 'Normal'} PRIORITY</span>
                        <p className="text-[10px] text-slate-400 font-bold tracking-widest bg-slate-50 dark:bg-slate-900 px-2 py-0.5 rounded-lg border">#{tk.customerCode || customer?.customerCode}</p>
                     </div>
                     <div className="space-y-0.5">
                        <h4 className="text-xl font-black text-slate-800 dark:text-white tracking-tighter leading-tight uppercase">{tk.subject || 'No Subject'}</h4>
                        <p className="text-xs font-black text-slate-400 normal-case tracking-normal max-w-2xl">{tk.description || 'No detailed description provided.'}</p>
                     </div>
                     <div className="flex flex-wrap items-center justify-center md:justify-start gap-4 pt-1">
                        <p className="text-[10px] font-black text-slate-600 dark:text-slate-300"><i className="far fa-calendar-alt mr-1"></i> {new Date(tk.createdAt || tk.created_at).toLocaleDateString()}</p>
                        <div className="w-px h-4 bg-slate-100 dark:bg-slate-700 hidden sm:block"></div>
                        <p className="text-[10px] font-black text-indigo-600 italic">Logged By: {tk.createdBy || tk.created_by || 'Admin'}</p>
                     </div>
                  </div>
               </div>
            </div>
          );
        })}
      </div>

      {showAddModal && (
        <div className="fixed inset-0 bg-slate-900/90 backdrop-blur-xl z-[9000] flex items-center justify-center p-6 uppercase font-black overflow-y-auto">
          <div className="bg-white dark:bg-slate-800 rounded-[56px] w-full max-w-4xl p-12 shadow-2xl border-4 border-indigo-500/20 space-y-10 relative overflow-hidden my-10">
             <div className="absolute top-0 left-0 w-full h-3 bg-indigo-600"></div>
             <div className="flex justify-between items-center border-b pb-6">
                <h3 className="text-4xl font-black tracking-tighter leading-none">Create Complaint</h3>
                <button onClick={() => setShowAddModal(false)} className="text-rose-500 text-3xl hover:scale-110 transition-all"><i className="fas fa-times-circle"></i></button>
             </div>

             <div className="space-y-10">
                <div className="space-y-4">
                   <label className="text-xs text-slate-400 ml-4 tracking-[3px]">SEARCH & SELECT SUBSCRIBER</label>
                   <div className="relative">
                      <input
                         type="text"
                         placeholder="Type Name, ID, Zone or Mobile..."
                         value={customerSearch}
                         onChange={e => {
                            setCustomerSearch(e.target.value);
                            if (newTicket.customerId) setNewTicket({...newTicket, customerId: ''});
                         }}
                         className="w-full bg-slate-50 dark:bg-slate-900 p-6 rounded-3xl font-black text-xl outline-none border-2 border-transparent focus:border-indigo-500 shadow-inner"
                      />
                      <i className="fas fa-search absolute right-8 top-1/2 -translate-y-1/2 text-slate-300 text-2xl"></i>
                   </div>

                   {customerSearch && !newTicket.customerId && (
                      <div className="bg-white dark:bg-slate-800 rounded-[32px] border-2 border-slate-100 shadow-2xl max-h-[300px] overflow-y-auto custom-scrollbar animate-fadeIn">
                         {searchedCustomers.map(c => (
                            <div
                               key={c.id}
                               onClick={() => {
                                  setNewTicket({...newTicket, customerId: c.id});
                                  setCustomerSearch(`${c.name} (#${c.customerCode})`);
                               }}
                               className="p-6 hover:bg-indigo-50 dark:hover:bg-indigo-900/30 cursor-pointer border-b last:border-0 flex justify-between items-center transition-colors"
                            >
                               <div className="space-y-2">
                                  <p className="text-2xl font-black text-slate-800 dark:text-white leading-none">{c.name}</p>
                                  <div className="flex flex-wrap items-center gap-3">
                                     <span className="text-xs font-black text-indigo-600 bg-indigo-50 dark:bg-indigo-900/30 px-3 py-1 rounded-lg border border-indigo-100">Code: {c.customerCode}</span>
                                     <span className="text-xs font-black text-slate-500 bg-slate-100 dark:bg-slate-800 px-3 py-1 rounded-lg border border-slate-200 uppercase tracking-widest">Zone: {c.zone || 'Global'}</span>
                                     <span className="text-xs font-black text-emerald-600 bg-emerald-50 dark:bg-emerald-900/30 px-3 py-1 rounded-lg border border-emerald-100 italic">Mobile: {c.mobile}</span>
                                  </div>
                               </div>
                               <i className="fas fa-plus-circle text-indigo-600 text-2xl"></i>
                            </div>
                         ))}
                         {searchedCustomers.length === 0 && (<div className="p-10 text-center text-slate-400 font-black text-xs uppercase tracking-widest">No Customer Found</div>)}
                      </div>
                   )}
                </div>

                <div className="space-y-6 pt-6 border-t border-slate-100">
                   <label className="text-xs text-slate-400 tracking-[3px] ml-4">CHOOSE ISSUE / SUBJECT</label>
                   <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                      {quickIssues.map(issue => (
                        <button key={issue} onClick={() => setNewTicket({...newTicket, subject: issue})} className={`p-8 rounded-[40px] text-lg font-black transition-all border-4 text-center leading-tight ${newTicket.subject === issue ? 'bg-amber-500 border-amber-200 text-white shadow-xl scale-[1.02]' : 'bg-slate-50 dark:bg-slate-900 border-transparent text-slate-600 dark:text-slate-300 hover:border-amber-400'}`}>{issue}</button>
                      ))}
                   </div>
                   <input type="text" placeholder="Type custom subject here..." value={newTicket.subject} onChange={e => setNewTicket({...newTicket, subject: e.target.value})} className="w-full bg-slate-50 dark:bg-slate-900 p-6 rounded-3xl font-black text-xl outline-none border-2 border-transparent focus:border-amber-500 transition-all shadow-inner" />
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-8 items-end pt-6 border-t border-slate-100">
                   <div className="space-y-4">
                      <label className="text-xs text-slate-400 ml-4 tracking-[3px]">PRIORITY LEVEL</label>
                      <div className="flex space-x-3 bg-slate-50 dark:bg-slate-900 p-3 rounded-[32px] shadow-inner">
                         {['Normal', 'Medium', 'High'].map(p => (<button key={p} onClick={() => setNewTicket({...newTicket, priority: p})} className={`flex-1 py-4 rounded-2xl text-xs font-black transition-all ${newTicket.priority === p ? 'bg-white dark:bg-slate-800 text-rose-500 shadow-md scale-105' : 'text-slate-400'}`}>{p}</button>))}
                      </div>
                   </div>
                   <button onClick={handleCreateTicket} disabled={isUpdating} className="w-full bg-indigo-600 text-white py-8 rounded-[40px] font-black text-xl tracking-[10px] shadow-2xl hover:scale-[1.02] border-b-8 border-indigo-900 uppercase">SUBMIT COMPLAINT</button>
                </div>
             </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default SupportTickets;
