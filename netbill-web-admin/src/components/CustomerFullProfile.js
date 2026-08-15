import React from 'react';

const CustomerFullProfile = ({ store, customerId, onBack, t }) => {
  const customer = store.customers.find(c => c.id === customerId);

  if (!customer) return (
    <div className="p-20 text-center uppercase font-black tracking-widest opacity-20">
       <i className="fas fa-user-slash text-9xl mb-10"></i>
       <h2 className="text-4xl">Subscriber Not Found</h2>
       <button onClick={onBack} className="mt-10 bg-slate-900 text-white px-10 py-4 rounded-2xl">GO BACK</button>
    </div>
  );

  const InfoBlock = ({ label, value, icon, color = "text-slate-800" }) => (
    <div className="bg-white dark:bg-slate-800 p-8 rounded-[40px] shadow-xl border border-slate-50 dark:border-slate-700 flex items-center space-x-6 hover:scale-[1.02] transition-all">
       <div className={`w-16 h-16 rounded-2xl bg-slate-50 dark:bg-slate-900 flex items-center justify-center text-2xl ${color} shadow-inner`}>
          <i className={`fas ${icon}`}></i>
       </div>
       <div className="space-y-1">
          <p className="text-[10px] text-slate-400 font-black tracking-[3px] uppercase leading-none">{label}</p>
          <p className={`text-xl font-black ${color} tracking-tighter uppercase`}>{value || '---'}</p>
       </div>
    </div>
  );

  const SectionTitle = ({ title, icon, color }) => (
    <div className="flex items-center space-x-4 border-b-4 border-slate-50 dark:border-slate-800 pb-6 mb-10">
       <div className={`w-12 h-12 rounded-2xl ${color} text-white flex items-center justify-center text-xl shadow-lg`}>
          <i className={`fas ${icon}`}></i>
       </div>
       <h3 className="text-4xl font-black uppercase tracking-tighter">{title}</h3>
    </div>
  );

  return (
    <div className="max-w-7xl mx-auto space-y-16 pb-20 font-black uppercase animate-fadeIn">
      {/* HEADER */}
      <div className="flex justify-between items-center bg-white dark:bg-slate-800 p-10 rounded-[48px] shadow-2xl border border-slate-100 dark:border-slate-700">
         <div className="flex items-center space-x-8">
            <button onClick={onBack} className="w-16 h-16 bg-slate-50 dark:bg-slate-900 rounded-full flex items-center justify-center text-slate-400 hover:text-teal-600 hover:scale-110 transition-all shadow-lg">
               <i className="fas fa-arrow-left text-2xl"></i>
            </button>
            <div className="w-24 h-24 bg-teal-600 text-white rounded-[32px] flex items-center justify-center text-5xl shadow-2xl">
               <i className="fas fa-user-tie"></i>
            </div>
            <div>
               <h2 className="text-6xl tracking-tighter leading-none">{customer.name}</h2>
               <div className="flex items-center space-x-4 mt-3">
                  <span className="bg-teal-50 text-teal-600 px-5 py-2 rounded-xl text-xs tracking-widest border border-teal-100">#{customer.customerCode}</span>
                  <span className={`px-5 py-2 rounded-xl text-xs tracking-widest border ${customer.status === 'Active' ? 'bg-emerald-50 text-emerald-600 border-emerald-100' : 'bg-rose-50 text-rose-600 border-rose-100'}`}>{customer.status}</span>
               </div>
            </div>
         </div>
         <div className="text-right space-y-2">
            <p className="text-[10px] text-slate-400 tracking-[5px]">SUBSCRIBER FULL PROFILE</p>
            <p className="text-2xl text-slate-800 dark:text-white">{new Date().toLocaleDateString('en-GB', { day: '2-digit', month: 'long', year: 'numeric' })}</p>
         </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-16">
         {/* LEFT COLUMN: PRIMARY & NETWORK */}
         <div className="space-y-16">
            <div>
               <SectionTitle title="Identity & Contact" icon="fa-id-card" color="bg-indigo-600" />
               <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                  <InfoBlock label="Full Name" value={customer.name} icon="fa-user" color="text-indigo-600" />
                  <InfoBlock label="Primary Mobile" value={customer.mobile} icon="fa-phone" color="text-emerald-600" />
                  <InfoBlock label="Alt Mobile" value={customer.altMobile} icon="fa-mobile-alt" color="text-blue-600" />
                  <InfoBlock label="Customer ID" value={customer.customerCode} icon="fa-fingerprint" color="text-slate-600" />
                  <InfoBlock label="Reference Name" value={customer.referenceName} icon="fa-handshake" color="text-indigo-500" />
                  <InfoBlock label="Ref. Mobile" value={customer.referenceMobile} icon="fa-phone-volume" color="text-teal-600" />
                  <div className="sm:col-span-2">
                     <InfoBlock label="Permanent Address" value={customer.address} icon="fa-location-dot" color="text-rose-600" />
                  </div>
               </div>
            </div>

            <div>
               <SectionTitle title="Network Connectivity" icon="fa-network-wired" color="bg-blue-600" />
               <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                  <InfoBlock label="PPPoE Username" value={customer.pppoeUsername} icon="fa-user-gear" color="text-blue-600" />
                  <InfoBlock label="PPPoE Password" value={customer.pppoePassword} icon="fa-key" color="text-rose-600" />
                  <InfoBlock label="Mikrotik Router" value={customer.routerId} icon="fa-server" color="text-slate-600" />
                  <InfoBlock label="ONU Serial / MAC" value={customer.onuMac || customer.onuSerialNumber} icon="fa-microchip" color="text-indigo-600" />
                  <InfoBlock label="Distribution Box" value={customer.boxId} icon="fa-box-open" color="text-orange-600" />
                  <InfoBlock label="Connection Type" value={customer.connectionType} icon="fa-plug" color="text-teal-600" />
               </div>
            </div>
         </div>

         {/* RIGHT COLUMN: BILLING & LOGISTICS */}
         <div className="space-y-16">
            <div>
               <SectionTitle title="Financial & Billing" icon="fa-file-invoice-dollar" color="bg-emerald-600" />
               <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                  <InfoBlock label="Subscription Plan" value={customer.packageName} icon="fa-wifi" color="text-teal-600" />
                  <InfoBlock label="Monthly Bill" value={`৳ ${customer.monthlyBill}`} icon="fa-receipt" color="text-emerald-600" />
                  <InfoBlock label="Current Due" value={`৳ ${Math.floor(customer.currentDue)}`} icon="fa-triangle-exclamation" color="text-rose-600" />
                  <InfoBlock label="Advance Balance" value={`৳ ${Math.floor(customer.advanceBalance || 0)}`} icon="fa-piggy-bank" color="text-blue-600" />
                  <InfoBlock label="Billing Cycle" value={customer.billingType} icon="fa-rotate" color="text-indigo-600" />
                  <InfoBlock label="Subscription Type" value={customer.subscriptionType} icon="fa-credit-card" color="text-slate-600" />
               </div>
            </div>

            <div>
               <SectionTitle title="Location & Logistics" icon="fa-map-location-dot" color="bg-rose-600" />
               <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                  <InfoBlock label="Assigned Zone" value={customer.zone} icon="fa-map" color="text-rose-600" />
                  <InfoBlock label="Sub-Zone" value={customer.subZone} icon="fa-layer-group" color="text-orange-600" />
                  <InfoBlock label="Join Date" value={customer.joinDate} icon="fa-calendar-check" color="text-emerald-600" />
                  <InfoBlock label="Expiry Date" value={customer.expireDate} icon="fa-calendar-xmark" color="text-rose-600" />
                  <div className="sm:col-span-2">
                     <InfoBlock label="Assigned Collector" value={customer.assignedStaffId} icon="fa-user-shield" color="text-indigo-600" />
                  </div>
               </div>
            </div>
         </div>
      </div>

      <div className="flex justify-center pt-10">
         <button onClick={onBack} className="bg-slate-900 text-white px-20 py-8 rounded-[40px] text-xl tracking-[10px] shadow-2xl hover:scale-105 active:scale-95 transition-all border-b-8 border-slate-700">
            CLOSE PROFILE
         </button>
      </div>
    </div>
  );
};

export default CustomerFullProfile;
