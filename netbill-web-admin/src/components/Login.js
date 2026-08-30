import React, { useState } from 'react';
import { supabase } from '../supabaseClient';

const Login = ({ onLoginSuccess }) => {
  const [loginType, setLoginType] = useState(() => {
     const params = new URLSearchParams(window.location.search);
     return params.get('type') === 'customer' ? 'customer' : 'admin';
  });
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsLoading(true);

    try {
      if (loginType === 'admin') {
        // 1. Fetch Dynamic Admin Credentials from Settings
        const { data: settingsData } = await supabase.from('settings').select('admin_identifier, admin_password').limit(1).maybeSingle();

        if (!settingsData || !settingsData.admin_identifier) {
           alert("System Configuration Error: Admin credentials not found in database.");
           setIsLoading(false);
           return;
        }

        const masterUser = settingsData.admin_identifier;
        const masterPass = settingsData.admin_password;

        if (email === masterUser && password === masterPass) {
          onLoginSuccess({ role: 'admin', data: { name: 'Super Admin' } });
        } else {
          // Check Staff table in Supabase
          const { data, error } = await supabase
            .from('staff')
            .select('*')
            .eq('mobile', email)
            .eq('password', password)
            .maybeSingle();

          if (data && !error) {
             const staffData = {
                id: data.id,
                name: data.name,
                mobile: data.mobile,
                role: data.role,
                salary: data.salary,
                balance: data.balance,
                status: data.status
             };
             onLoginSuccess({ role: 'staff', data: staffData });
          } else {
            alert('Unauthorized Access Denied!');
          }
        }
      } else {
        // Customer Login (PPPoE Credentials)
        const { data, error } = await supabase
          .from('customers')
          .select('*')
          .eq('pppoe_username', email)
          .eq('pppoe_password', password)
          .maybeSingle();

        if (data && !error) {
          // Map to camelCase for the UI
          const custData = {};
          Object.keys(data).forEach(key => {
            const camelKey = key.replace(/(_\w)/g, m => m[1].toUpperCase());
            custData[camelKey] = data[key];
          });
          onLoginSuccess({ role: 'customer', data: custData });
        } else {
          alert('Invalid PPPoE Credentials!');
        }
      }
    } catch (err) {
      alert('Login Error: ' + err.message);
    } finally {
      setIsLoading(false);
    }
  };


  return (
    <div className="fixed inset-0 bg-[#0F172A] z-[2000] flex items-center justify-center p-4 md:p-6 font-black uppercase overflow-y-auto">
      <div className="bg-white rounded-[32px] md:rounded-[56px] w-full max-w-md p-8 md:p-12 shadow-2xl space-y-6 md:space-y-8 text-center relative overflow-hidden my-auto">
        <div className="absolute top-0 left-0 w-full h-2 md:h-3 bg-teal-500"></div>

        <div className="flex bg-slate-50 p-1.5 md:p-2 rounded-2xl md:rounded-3xl mb-2 md:mb-4">
           <button onClick={() => setLoginType('admin')} className={`flex-1 py-2.5 md:py-3 rounded-xl md:rounded-2xl text-[9px] md:text-[10px] transition-all ${loginType === 'admin' ? 'bg-white shadow-md text-teal-600' : 'text-slate-400'}`}>Admin / Staff</button>
           <button onClick={() => setLoginType('customer')} className={`flex-1 py-2.5 md:py-3 rounded-xl md:rounded-2xl text-[9px] md:text-[10px] transition-all ${loginType === 'customer' ? 'bg-white shadow-md text-teal-600' : 'text-slate-400'}`}>Customer Portal</button>
        </div>

        <div className="w-16 h-16 md:w-20 md:h-20 bg-teal-50 text-teal-600 rounded-2xl md:rounded-[32px] flex items-center justify-center mx-auto shadow-inner">
          <i className={`fas ${loginType === 'admin' ? 'fa-user-shield' : 'fa-user-tie'} text-3xl md:text-4xl`}></i>
        </div>

        <div>
          <h2 className="text-2xl md:text-3xl font-black text-slate-900 tracking-tight uppercase leading-none">NetBill ISP</h2>
          <p className="text-[8px] md:text-[10px] font-black text-slate-400 uppercase tracking-[3px] md:tracking-[4px] mt-2 md:mt-3">{loginType === 'admin' ? 'Enterprise Console' : 'My Internet Hub'}</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4 md:space-y-5" autoComplete="off">
          <div className="text-left space-y-1 md:space-y-2">
            <label className="text-[8px] md:text-[9px] font-black text-slate-400 uppercase ml-3 md:ml-4 tracking-widest">{loginType === 'admin' ? 'Email or Staff ID' : 'PPPoE Username'}</label>
            <input
              type="text"
              name="user_identifier"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full bg-slate-50 border-2 border-transparent focus:border-teal-500 rounded-xl md:rounded-[24px] p-4 md:p-5 font-black text-slate-800 outline-none transition-all text-sm md:text-base"
              required
              autoComplete="one-time-code"
            />
          </div>
          <div className="text-left space-y-1 md:space-y-2">
            <label className="text-[8px] md:text-[9px] font-black text-slate-400 uppercase ml-3 md:ml-4 tracking-widest">{loginType === 'admin' ? 'Secure Password' : 'PPPoE Password'}</label>
            <input
              type="password"
              name="user_password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full bg-slate-50 border-2 border-transparent focus:border-teal-500 rounded-xl md:rounded-[24px] p-4 md:p-5 font-black text-slate-800 outline-none transition-all text-sm md:text-base"
              required
              autoComplete="new-password"
            />
          </div>
          <button
            type="submit"
            disabled={isLoading}
            className="w-full bg-teal-600 text-white py-5 md:py-6 rounded-xl md:rounded-[28px] font-black uppercase tracking-[4px] md:tracking-[5px] shadow-xl shadow-teal-500/20 hover:scale-105 active:scale-95 transition-all mt-2 md:mt-4 h-14 md:h-auto"
          >
            {isLoading ? 'Authenticating...' : 'Secure Access'}
          </button>
        </form>
      </div>
    </div>
  );
};

export default Login;
