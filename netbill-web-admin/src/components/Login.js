import React, { useState } from 'react';
import { db } from '../firebaseConfig';
import { collection, query, where, getDocs } from 'firebase/firestore';

const Login = ({ onLoginSuccess }) => {
  const [loginType, setLoginType] = useState('admin'); // 'admin' or 'customer'
  const [email, setEmail] = useState('admin@isp.com');
  const [password, setPassword] = useState('123456');
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsLoading(true);

    try {
      if (loginType === 'admin') {
        // Simple hardcoded admin for now, can be extended to Staff check later
        if (email === 'admin@isp.com' && password === '123456') {
          onLoginSuccess({ role: 'admin', data: { name: 'Super Admin' } });
        } else {
          // Check Staff collection
          const q = query(collection(db, "staff"), where("mobile", "==", email), where("password", "==", password));
          const snap = await getDocs(q);
          if (!snap.empty) {
             const staffData = { id: snap.docs[0].id, ...snap.docs[0].data() };
             onLoginSuccess({ role: 'staff', data: staffData });
          } else {
            alert('Unauthorized Access Denied!');
          }
        }
      } else {
        // Customer Login (PPPoE Credentials)
        const q = query(collection(db, "customers"), where("pppoeUsername", "==", email), where("pppoePassword", "==", password));
        const snap = await getDocs(q);
        if (!snap.empty) {
          const custData = { id: snap.docs[0].id, ...snap.docs[0].data() };
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
    <div className="fixed inset-0 bg-[#0F172A] z-[2000] flex items-center justify-center p-6 font-black uppercase">
      <div className="bg-white rounded-[56px] w-full max-w-md p-12 shadow-2xl space-y-8 text-center relative overflow-hidden">
        <div className="absolute top-0 left-0 w-full h-3 bg-teal-500"></div>

        <div className="flex bg-slate-50 p-2 rounded-3xl mb-4">
           <button onClick={() => setLoginType('admin')} className={`flex-1 py-3 rounded-2xl text-[10px] transition-all ${loginType === 'admin' ? 'bg-white shadow-md text-teal-600' : 'text-slate-400'}`}>Admin / Staff</button>
           <button onClick={() => setLoginType('customer')} className={`flex-1 py-3 rounded-2xl text-[10px] transition-all ${loginType === 'customer' ? 'bg-white shadow-md text-teal-600' : 'text-slate-400'}`}>Customer Portal</button>
        </div>

        <div className="w-20 h-20 bg-teal-50 text-teal-600 rounded-[32px] flex items-center justify-center mx-auto shadow-inner">
          <i className={`fas ${loginType === 'admin' ? 'fa-user-shield' : 'fa-user-tie'} text-4xl`}></i>
        </div>

        <div>
          <h2 className="text-3xl font-black text-slate-900 tracking-tight uppercase leading-none">NetBill ISP</h2>
          <p className="text-[10px] font-black text-slate-400 uppercase tracking-[4px] mt-3">{loginType === 'admin' ? 'Enterprise Console' : 'My Internet Hub'}</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-5">
          <div className="text-left space-y-2">
            <label className="text-[9px] font-black text-slate-400 uppercase ml-4 tracking-widest">{loginType === 'admin' ? 'Email or Staff ID' : 'PPPoE Username'}</label>
            <input
              type="text"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full bg-slate-50 border-2 border-transparent focus:border-teal-500 rounded-[24px] p-5 font-black text-slate-800 outline-none transition-all"
              required
            />
          </div>
          <div className="text-left space-y-2">
            <label className="text-[9px] font-black text-slate-400 uppercase ml-4 tracking-widest">{loginType === 'admin' ? 'Secure Password' : 'PPPoE Password'}</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full bg-slate-50 border-2 border-transparent focus:border-teal-500 rounded-[24px] p-5 font-black text-slate-800 outline-none transition-all"
              required
            />
          </div>
          <button
            type="submit"
            disabled={isLoading}
            className="w-full bg-teal-600 text-white py-6 rounded-[28px] font-black uppercase tracking-[5px] shadow-xl shadow-teal-500/20 hover:scale-105 active:scale-95 transition-all mt-4"
          >
            {isLoading ? 'Authenticating...' : 'Secure Access'}
          </button>
        </form>
      </div>
    </div>
  );
};

export default Login;
