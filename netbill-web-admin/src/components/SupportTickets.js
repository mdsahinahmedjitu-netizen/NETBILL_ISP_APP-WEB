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

              const finalUrl = `https://tglplinxvrqsrxeicvpr.supabase.co/functions/v1/sms-proxy?apikey=${apiKey}&callerID=${senderId}&number=${cleanMobile}&message=${encodeURIComponent(msg)}&type=${msgType}`;

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
    <div className="w-full max-w-7xl mx-auto space-y-4 md:space-y-6 pb-20 uppercase font-black tracking-tighter transition-all px-2 md:px-0">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-end gap-4">
        <div className="flex items-center space-x-3 md:space-x-4">
           <button onClick={() => setActivePage('dashboard')} className="w-10 h-10 md:w-12 md:h-12 bg-white dark:bg-slate-800 rounded-xl flex items-center justify-center text-amber-500 shadow-sm border border-slate-100">
              <i className="fas fa-arrow-left"></i>
           </button>
           <div className="space-y-0.5 md:space-y-1">
              <h3 className="text-xl md:text-4xl font-black text-slate-800 dark:text-white tracking-tighter leading-none">Complaints</h3>
              <p className="text-[8px] md:text-[10px] text-amber-600 tracking-[2px] md:tracking-widest font-black uppercase italic">Support System</p>
           </div>
        </div>

        <div className="flex flex-wrap items-center gap-2 md:gap-3 w-full md:w-auto">
            <div className="flex items-center space-x-2 bg-slate-100 dark:bg-slate-900 p-1 md:p-1.5 rounded-xl md:rounded-[24px] border border-indigo-500/10 shadow-inner flex-1 md:flex-none">
               <input
                  type="text"
                  placeholder="NEW PRESET..."
                  value={customIssueInput}
                  onChange={e => setCustomIssueInput(e.target.value.toUpperCase())}
                  className="bg-transparent border-none outline-none text-[8px] md:text-[9px] font-black w-24 md:w-32 ml-2 md:ml-3 uppercase placeholder:text-slate-300"
               />
               <button onClick={addQuickIssue} className="bg-indigo-600 text-white px-3 py-1.5 md:px-4 md:py-2 rounded-lg md:rounded-xl text-[8px] md:text-[9px] font-black shadow-md transition-all">ADD</button>
            </div>

            <button onClick={() => setShowAddModal(true)} className="bg-emerald-600 text-white px-4 py-2.5 md:px-6 md:py-3 rounded-xl md:rounded-2xl font-black text-[9px] md:text-[10px] shadow-lg flex items-center space-x-2"><i className="fas fa-plus-circle"></i><span className="hidden sm:inline">NEW</span><span className="sm:hidden">ADD</span></button>

            <div className="flex items-center space-x-1 bg-slate-100 dark:bg-slate-900 p-1 rounded-xl md:rounded-[20px] shadow-inner w-full md:w-auto overflow-x-auto custom-scrollbar">
               {['Open', 'Pending', 'Resolved', 'All'].map(tab => (<button key={tab} onClick={() => setActiveTab(tab)} className={`flex-1 md:flex-none px-3 md:px-4 py-1.5 md:py-2 rounded-lg md:rounded-xl text-[8px] font-black transition-all whitespace-nowrap ${activeTab === tab ? 'bg-white dark:bg-slate-800 text-amber-600 shadow-sm' : 'text-slate-400'}`}>{tab}</button>))}
            </div>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4">
        {filteredTickets.length > 0 ? filteredTickets.map(tk => {
          const customer = store.customers?.find(c => c.id === tk.customerId || c.id === tk.customer_id);
          return (
            <div key={tk.id} className="bg-white dark:bg-slate-800 p-4 md:p-5 rounded-[24px] md:rounded-3xl shadow-lg border border-slate-100 dark:border-slate-700 flex flex-col xl:flex-row justify-between items-center gap-4 md:gap-6 group hover:border-amber-500/30 transition-all">
               <div className="xl:w-1/3 w-full bg-slate-50 dark:bg-slate-950/50 p-4 md:p-6 rounded-[24px] md:rounded-[32px] border-2 border-slate-100 dark:border-slate-800 space-y-3 md:space-y-4 shadow-inner">
                  <div className="space-y-1">
                     <span className="text-[9px] md:text-[12px] text-indigo-600 font-black uppercase tracking-[2px] md:tracking-[4px] leading-none">Subscriber</span>
                     <h3 className="text-xl md:text-4xl font-black text-slate-800 dark:text-white uppercase tracking-tighter leading-none truncate">{customer?.name || 'Unknown Client'}</h3>
                  </div>
                  <div className="grid grid-cols-2 gap-2 md:gap-4">
                     <div className="space-y-1">
                        <span className="text-[8px] md:text-[14px] text-slate-400 font-black uppercase tracking-widest leading-none">Zone</span>
                        <p className="text-[10px] md:text-[18px] font-black text-slate-700 dark:text-slate-200 leading-tight truncate">{customer?.zone || 'Global'}</p>
                     </div>
                     <div className="space-y-1 text-right">
                        <span className="text-[8px] md:text-[14px] text-slate-400 font-black uppercase tracking-widest leading-none">Phone</span>
                        <p className="text-[10px] md:text-[18px] font-black text-emerald-600 leading-none">{customer?.mobile || 'N/A'}</p>
                     </div>
                  </div>
                  <div className="flex gap-2 pt-3 border-t border-slate-200/50">
                    {tk.status !== 'Resolved' && (<button onClick={() => updateTicketStatus(tk.id, 'Resolved')} className="flex-1 bg-emerald-600 text-white py-2.5 md:py-4 rounded-xl md:rounded-2xl font-black text-[9px] md:text-xs shadow-md">RESOLVE</button>)}
                    {tk.status === 'Open' && (<button onClick={() => updateTicketStatus(tk.id, 'Pending')} className="flex-1 bg-amber-500 text-white py-2.5 md:py-4 rounded-xl md:rounded-2xl font-black text-[9px] md:text-xs shadow-md">PENDING</button>)}
                    {tk.status === 'Resolved' && (<button onClick={() => updateTicketStatus(tk.id, 'Open')} className="flex-1 bg-slate-800 text-white py-2.5 md:py-4 rounded-xl md:rounded-2xl font-black text-[9px] md:text-xs">RE-OPEN</button>)}
                    <button onClick={async () => { if(window.confirm("Delete?")) await supabase.from('support_tickets').delete().eq('id', tk.id); }} className="w-10 h-10 md:w-14 md:h-14 bg-rose-100 text-rose-600 rounded-xl md:rounded-2xl flex items-center justify-center hover:bg-rose-600 hover:text-white transition-all shadow-sm shrink-0"><i className="fas fa-trash-alt text-sm md:text-lg"></i></button>
                  </div>
               </div>

               <div className="flex-1 flex flex-row items-start gap-3 md:gap-6 w-full">
                  <div className={`w-10 h-10 md:w-16 md:h-16 shrink-0 rounded-xl md:rounded-2xl flex items-center justify-center text-lg md:text-2xl shadow-inner border ${tk.status === 'Resolved' ? 'bg-emerald-50 text-emerald-500 border-emerald-100' : 'bg-amber-50 text-amber-500 border-amber-100 animate-pulse'}`}><i className={`fas ${tk.status === 'Resolved' ? 'fa-check-circle' : 'fa-headset'}`}></i></div>
                  <div className="space-y-1.5 md:space-y-2 flex-1 min-w-0">
                     <div className="flex items-center space-x-2 md:space-x-3 leading-none">
                        <span className={`px-2 py-0.5 md:px-3 md:py-1 rounded-lg text-[7px] md:text-[8px] font-black border ${getPriorityColor(tk.priority || 'Normal')}`}>{tk.priority?.toUpperCase()}</span>
                        <p className="text-[8px] md:text-[10px] text-slate-400 font-bold tracking-widest bg-slate-50 dark:bg-slate-900 px-1.5 py-0.5 rounded-lg border">#{tk.customerCode || customer?.customerCode}</p>
                     </div>
                     <div className="space-y-0.5">
                        <h4 className="text-sm md:text-xl font-black text-slate-800 dark:text-white tracking-tighter leading-tight uppercase truncate">{tk.subject || 'No Subject'}</h4>
                        <p className="text-[10px] md:text-xs font-black text-slate-400 normal-case tracking-normal line-clamp-2 md:line-clamp-none">{tk.description || 'No detailed description.'}</p>
                     </div>
                     <div className="flex flex-wrap items-center gap-2 md:gap-4 pt-1">
                        <p className="text-[8px] md:text-[10px] font-black text-slate-500"><i className="far fa-calendar-alt mr-1"></i> {new Date(tk.createdAt || tk.created_at).toLocaleDateString()}</p>
                        <div className="w-px h-3 bg-slate-100 dark:bg-slate-700"></div>
                        <p className="text-[8px] md:text-[10px] font-black text-indigo-600 italic truncate max-w-[100px]">BY: {tk.createdBy || tk.created_by}</p>
                     </div>
                  </div>
               </div>
            </div>
          );
        }) : (
          <div className="py-24 text-center opacity-10 flex flex-col items-center space-y-6">
             <i className="fas fa-headset text-8xl"></i>
             <p className="text-2xl font-black tracking-[8px]">NO TICKETS</p>
          </div>
        )}
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
