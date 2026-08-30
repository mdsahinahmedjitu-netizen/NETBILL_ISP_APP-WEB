import React, { useState, useEffect } from 'react';
import { supabase } from '../supabaseClient';

const Customers = ({ store, session, setActivePage, t, lang, autoOpenModal, setAutoOpenModal, setProfileId, isDirectMode, preSelectedCustomer, setPreSelectedCustomer, hideTable, initialFilters, setInitialFilters }) => {
  const [search, setSearch] = useState('');
  const [selectedCust, setSelectedCust] = useState(null);
  const [ledger, setLedger] = useState([]);
  const [showModal, setShowModal] = useState(autoOpenModal || false);
  const [isEditing, setIsEditing] = useState(false);

  // Selection & UI States
  const [selectedIds, setSelectedIds] = useState([]);
  const [activeMenuId, setActiveMenuId] = useState(null);
  const [showSmsModal, setShowSmsModal] = useState(false);
  const [smsMessage, setSmsMessage] = useState('');
  const [isSendingSms, setIsSendingSms] = useState(false);
  const [smsProgress, setSmsProgress] = useState({ current: 0, total: 0 });

  // WhatsApp States
  const [showWhatsAppModal, setShowWhatsAppModal] = useState(false);
  const [waTargetCust, setWaTargetCust] = useState(null);
  const [waMessage, setWaMessage] = useState('');

  // Quick Zone Change States
  const [showZoneChangeModal, setShowZoneChangeModal] = useState(false);
  const [custToChangeZone, setCustToChangeZone] = useState(null);
  const [newZoneData, setNewZoneData] = useState({ zone: '', subZone: '', boxId: '' });

  // Quick Date Change States
  const [showDateChangeModal, setShowDateChangeModal] = useState(false);
  const [custToChangeDate, setCustToChangeDate] = useState(null);
  const [newDates, setNewDates] = useState({ expireDate: '', requestDate: '' });

  // Quick Promise States
  const [showPromiseModal, setShowPromiseModal] = useState(false);
  const [custForPromise, setCustForPromise] = useState(null);
  const [promiseData, setPromiseData] = useState({ date: '', note: '' });
  const [isSavingPromise, setIsSavingPromise] = useState(false);


  const [visibleColumns, setVisibleColumns] = useState({
    cb: true, id: true, sl: true, customer: true, mikrotik: true, zone: true,
    plan: true, bill: true, join: true, expire: true, collector: true, status: true, online: true, actions: true
  });
  const [showColumnSelector, setShowColumnSelector] = useState(false);

  // Sorting State
  const [sortConfig, setSortConfig] = useState({ key: null, direction: 'asc' });

  // Filter States
  const [showFilterDrawer, setShowFilterDrawer] = useState(false);
  const [filters, setFilters] = useState({
    collector: 'All',
    zone: 'All',
    status: 'All',
    plan: 'All',
    expiryUpTo: '',
    expiryStart: '',
    expiryEnd: '',
    joinStart: '',
    joinEnd: '',
    hideZeroDue: false,
    hideInactive: false
  });

  // Dynamic Import States
  const [showImportModal, setShowImportModal] = useState(false);
  const [importStatus, setImportStatus] = useState('');
  const [csvData, setCsvData] = useState([]);
  const [csvHeaders, setCsvHeaders] = useState([]);
  const [mapping, setMapping] = useState({});
  const [importStep, setImportStep] = useState(1); // 1: Upload, 2: Map

  const dbFields = [
    { key: 'name', label: 'Customer Name' },
    { key: 'mobile', label: 'Mobile No' },
    { key: 'pppoeUsername', label: 'PPPoE Username' },
    { key: 'packageName', label: 'Package Plan' },
    { key: 'monthlyBill', label: 'Monthly Bill' },
    { key: 'discountAmount', label: 'Discount Amount' },
    { key: 'paid', label: 'Paid Amount' },
    { key: 'currentDue', label: 'Due Amount' },
    { key: 'joinDate', label: 'Join Date' },
    { key: 'expireDate', label: 'Expire Date' },
    { key: 'assignedStaffId', label: 'Assigned Collector' },
    { key: 'zone', label: 'Zone' },
    { key: 'address', label: 'Address' }
  ];

  const initialState = {
    name: '', mobile: '', altMobile: '', customerCode: '', address: '',
    packageName: '', monthlyBill: 500, pppoeUsername: '', pppoePassword: '',
    onuSerialNumber: '', zone: '', subZone: '', boxId: '',
    routerId: '', billingType: 'MONTHLY DATE TO DATE', paymentStatus: 'Unpaid',
    expireDate: '', requestDate: '', connectionType: '', status: 'Active',
    subscriptionType: 'Prepaid', connectionFee: 0, joinDate: new Date().toLocaleDateString('en-CA'),
    assignedStaffId: '', referenceName: '', referenceMobile: '',
    currentDue: 0, advanceBalance: 0, discountAmount: 0,
    promiseDate: '', promiseNote: ''
  };
  const [formData, setFormData] = useState(initialState);

  useEffect(() => {
    if (initialFilters) {
        setFilters(prev => ({ ...prev, ...initialFilters }));
        if (setInitialFilters) setInitialFilters(null); // Clear after applying
    }
  }, [initialFilters]);

  useEffect(() => {
    if (preSelectedCustomer) {
        setFormData(preSelectedCustomer);
        setIsEditing(true);
        setShowModal(true);
    }
  }, [preSelectedCustomer]);

  useEffect(() => {
    if (autoOpenModal) {
      setIsEditing(false);
      setFormData(initialState);
      setShowModal(true);
      if (setAutoOpenModal) setAutoOpenModal(false);
    }
  }, [autoOpenModal]);

  useEffect(() => {
    if (!selectedCust) return;
    const fetchLedger = async () => {
      const { data, error } = await supabase
        .from('ledger_entries')
        .select('*')
        .eq('customer_id', selectedCust.id)
        .order('date', { ascending: false });
      if (!error) setLedger(data);
    };
    fetchLedger();

    // Subscribe to ledger changes
    const channel = supabase.channel(`ledger-${selectedCust.id}`)
      .on('postgres_changes', { event: '*', schema: 'public', table: 'ledger_entries', filter: `customer_id=eq.${selectedCust.id}` }, (payload) => {
        fetchLedger();
      })
      .subscribe();

    return () => {
      supabase.removeChannel(channel);
    };
  }, [selectedCust]);

  const getCollectorColor = (name) => {
    if (!name || name === '---' || name === 'No Staff') return 'text-slate-600 bg-slate-50 border-slate-200';

    // Explicit colors
    if (name.toUpperCase().includes('TOMA')) return 'text-pink-700 bg-pink-50 border-pink-200';
    if (name.toUpperCase().includes('SUPER ADMIN')) return 'text-emerald-700 bg-emerald-50 border-emerald-200';
    if (name.toUpperCase().includes('JITU')) return 'text-blue-700 bg-blue-50 border-blue-200';

    // Improved hashing to avoid collisions
    let hash = 0;
    for (let i = 0; i < name.length; i++) {
        hash = (hash << 5) - hash + name.charCodeAt(i);
        hash |= 0;
    }

    const colors = [
      'text-indigo-700 bg-indigo-50 border-indigo-200',
      'text-teal-700 bg-teal-50 border-teal-200',
      'text-rose-700 bg-rose-50 border-rose-200',
      'text-amber-700 bg-amber-50 border-amber-200',
      'text-blue-700 bg-blue-50 border-blue-200',
      'text-violet-700 bg-violet-50 border-violet-200',
      'text-pink-700 bg-pink-50 border-pink-200',
      'text-orange-700 bg-orange-50 border-orange-200',
      'text-emerald-700 bg-emerald-50 border-emerald-200',
      'text-cyan-700 bg-cyan-50 border-cyan-200',
      'text-fuchsia-700 bg-fuchsia-50 border-fuchsia-200'
    ];

    return colors[Math.abs(hash) % colors.length];
  };

  const filteredCustomers = store.customers.filter(c => {
    const searchMatch = !search ||
      c.name?.toLowerCase().includes(search.toLowerCase()) ||
      c.customerCode?.toLowerCase().includes(search.toLowerCase()) ||
      c.mobile?.includes(search) ||
      c.pppoeUsername?.toLowerCase().includes(search.toLowerCase());

    const collectorMatch = filters.collector === 'All' || c.assignedStaffId === filters.collector;
    const zoneMatch = filters.zone === 'All' || c.zone === filters.zone;
    const statusMatch = filters.status === 'All' ||
                       (filters.status === 'Expired' ? (c.status === 'Expired' || c.status === 'Suspended') : c.status === filters.status);
    const planMatch = filters.plan === 'All' || c.packageName === filters.plan;
    const currentDue = parseFloat(c.currentDue) || 0;
    const dueMatch = !filters.hideZeroDue || (currentDue >= 1);
    const inactiveMatch = !filters.hideInactive || (c.status === 'Active');

    // Expiry Date Filter Logic (Range or Single)
    let expiryMatch = true;
    const cDateStr = c.expireDate || c.expire_date;

    const parseDate = (d) => {
        if (!d) return null;
        if (d.includes('-') && d.split('-')[0].length === 4) return new Date(d);
        const p = d.split('-');
        if (p.length === 3) {
            const m = { "Jan":0,"Feb":1,"Mar":2,"Apr":3,"May":4,"Jun":5,"Jul":6,"Aug":7,"Sep":8,"Oct":9,"Nov":10,"Dec":11 };
            return new Date(p[2], m[p[1]], p[0]);
        }
        return null;
    };

    if (filters.expiryStart && filters.expiryEnd) {
        const customerExpire = parseDate(cDateStr);
        const start = new Date(filters.expiryStart);
        const end = new Date(filters.expiryEnd);
        start.setHours(0, 0, 0, 0);
        end.setHours(23, 59, 59, 999);
        expiryMatch = customerExpire && customerExpire >= start && customerExpire <= end;
    } else if (filters.expiryUpTo) {
        if (cDateStr) {
            const customerExpire = parseDate(cDateStr);
            const filterDate = new Date(filters.expiryUpTo);

            // Normalize both dates to compare just YYYY-MM-DD
            const cY = customerExpire.getFullYear();
            const cM = customerExpire.getMonth();
            const cD = customerExpire.getDate();

            const fY = filterDate.getFullYear();
            const fM = filterDate.getMonth();
            const fD = filterDate.getDate();

            expiryMatch = customerExpire && cY === fY && cM === fM && cD === fD;
        } else {
            expiryMatch = false;
        }
    }

    // Join Date Filter Logic (Range)
    let joinMatch = true;
    if (filters.joinStart && filters.joinEnd) {
        const cJoinStr = c.joinDate || c.join_date;
        const customerJoin = parseDate(cJoinStr);
        const start = new Date(filters.joinStart);
        const end = new Date(filters.joinEnd);
        start.setHours(0, 0, 0, 0);
        end.setHours(23, 59, 59, 999);
        joinMatch = customerJoin && customerJoin >= start && customerJoin <= end;
    }

    // Role-Based Isolation: Collector Staff only see their assigned customers
    if (session?.role === 'staff') {
        const isAssigned = (c.assignedStaffId || c.assigned_staff_id) === session.data.id || (c.assignedStaffId || c.assigned_staff_id) === session.data.name;
        if (!isAssigned) return false;
    }

    return searchMatch && collectorMatch && zoneMatch && statusMatch && planMatch && dueMatch && inactiveMatch && expiryMatch && joinMatch;
  });

  const requestSort = (key) => {
    let direction = 'asc';
    if (sortConfig.key === key && sortConfig.direction === 'asc') {
      direction = 'desc';
    }
    setSortConfig({ key, direction });
  };

  const getSortIcon = (key) => {
    if (sortConfig.key !== key) return 'fa-sort opacity-20';
    return sortConfig.direction === 'asc' ? 'fa-sort-up text-teal-600' : 'fa-sort-down text-teal-600';
  };

  const sortedCustomers = React.useMemo(() => {
    let sortableItems = [...filteredCustomers];
    if (sortConfig.key !== null) {
      sortableItems.sort((a, b) => {
        let aVal = a[sortConfig.key];
        let bVal = b[sortConfig.key];
        if (sortConfig.key === 'monthlyBill' || sortConfig.key === 'currentDue' || sortConfig.key === 'advanceBalance') {
          aVal = parseFloat(aVal) || 0;
          bVal = parseFloat(bVal) || 0;
        } else {
          aVal = (aVal || '').toString().toLowerCase();
          bVal = (bVal || '').toString().toLowerCase();
        }
        if (aVal < bVal) return sortConfig.direction === 'asc' ? -1 : 1;
        if (aVal > bVal) return sortConfig.direction === 'asc' ? 1 : -1;
        return 0;
      });
    }
    return sortableItems;
  }, [filteredCustomers, sortConfig]);

  const toggleSelectAll = () => {
    if (selectedIds.length === filteredCustomers.length) setSelectedIds([]);
    else setSelectedIds(filteredCustomers.map(c => c.id));
  };

  const toggleSelect = (id) => {
    if (selectedIds.includes(id)) setSelectedIds(selectedIds.filter(i => i !== id));
    else setSelectedIds([...selectedIds, id]);
  };

  const getTargetCustomers = () => {
    if (selectedIds.length > 0) {
        // If items are selected, we want to maintain the current sort order for those selected items
        return sortedCustomers.filter(c => selectedIds.includes(c.id));
    }
    return sortedCustomers;
  };

  const downloadExcel = () => {
    const targets = getTargetCustomers();
    if (targets.length === 0) return alert("No customers selected!");
    const headers = ["ID", "SL", "Customer", "Mikrotik", "Zone", "Plan", "Bill", "Paid", "Due", "Join Date", "Expire Date", "Collector", "Status"];
    const rows = targets.map((c, idx) => {
      const currentMonth = new Date().toLocaleDateString('en-CA').substring(0, 7);
      const paidThisMonth = store.payments.filter(p => p.customerId === c.id && p.paymentDate?.startsWith(currentMonth)).reduce((s, p) => s + (p.amount || 0), 0);
      return [c.customerCode, idx + 1, c.name, (c.pppoeUsername || '').toLowerCase(), c.zone, c.packageName, c.monthlyBill, Math.floor(paidThisMonth), Math.floor(c.currentDue), c.joinDate, c.expireDate, c.assignedStaffId, c.status];
    });
    const csvContent = [headers, ...rows].map(e => e.join(",")).join("\n");
    const link = document.createElement("a");
    link.setAttribute("href", URL.createObjectURL(new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })));
    link.setAttribute("download", `Marked_Subscribers_${new Date().toLocaleDateString()}.csv`);
    link.click();
  };

  const handlePrint = () => {
    const targets = getTargetCustomers();
    if (targets.length === 0) return alert("No customers selected!");
    const printWindow = window.open('', '_blank');
    const currentMonth = new Date().toLocaleDateString('en-CA').substring(0, 7);

    const tableRows = targets.map((c, idx) => {
      const paidThisMonth = store.payments.filter(p => p.customerId === c.id && p.paymentDate?.startsWith(currentMonth)).reduce((s, p) => s + (p.amount || 0), 0);
      return `<tr>
        ${visibleColumns.id ? `<td>${c.customerCode || ''}</td>` : ''}
        ${visibleColumns.sl ? `<td>${idx + 1}</td>` : ''}
        ${visibleColumns.customer ? `<td><span style="font-size: 14px; font-weight: normal; display: block; margin-bottom: 3px;">${c.name}</span><span style="font-size: 16px; font-weight: normal; display: block;">${c.mobile?.startsWith('88') ? c.mobile.substring(2) : (c.mobile || '')}</span></td>` : ''}
        ${visibleColumns.mikrotik ? `<td>${(c.pppoeUsername || '').toLowerCase()}</td>` : ''}
        ${visibleColumns.zone ? `<td>${c.zone || ''}</td>` : ''}
        ${visibleColumns.plan ? `<td>${c.packageName || ''}</td>` : ''}
        ${visibleColumns.bill ? `<td>৳ ${c.monthlyBill}</td><td>৳ ${Math.floor(paidThisMonth)}</td><td style="color:red; font-weight:bold;">৳ ${Math.floor(c.currentDue)}</td>` : ''}
        ${visibleColumns.join ? `<td>${formatDateDisplay(c.joinDate)}</td>` : ''}
        ${visibleColumns.expire ? `<td>Exp: ${formatDateDisplay(c.expireDate)}</td>` : ''}
        ${visibleColumns.collector ? `<td>${c.assignedStaffId || '---'}</td>` : ''}
        ${visibleColumns.status ? `<td>${c.status}</td>` : ''}
      </tr>`;
    }).join('');

    const printHtml = `
      <h2 style="text-align: center; margin-bottom: 15px; font-weight: normal; text-transform: uppercase;">NetBill ISP - Subscriber List</h2>
      <table>
        <thead><tr>
          ${visibleColumns.id ? '<th>ID</th>' : ''}
          ${visibleColumns.sl ? '<th>SL</th>' : ''}
          ${visibleColumns.customer ? '<th>Customer</th>' : ''}
          ${visibleColumns.mikrotik ? '<th>Mikrotik</th>' : ''}
          ${visibleColumns.zone ? '<th>Zone</th>' : ''}
          ${visibleColumns.plan ? '<th>Plan</th>' : ''}
          ${visibleColumns.bill ? '<th>Bill</th><th>Paid</th><th>Due</th>' : ''}
          ${visibleColumns.join ? '<th>Join</th>' : ''}
          ${visibleColumns.expire ? '<th>Expire</th>' : ''}
          ${visibleColumns.collector ? '<th>Collector</th>' : ''}
          ${visibleColumns.status ? '<th>Status</th>' : ''}
        </tr></thead>
        <tbody>${tableRows}</tbody>
      </table>
    `;

    printWindow.document.write(`<html><head><title>Print</title><style>body{font-family:sans-serif;padding:20px;}table{width:100%;border-collapse:collapse;}th,td{border:1px solid #000;padding:8px;font-size:13px;text-align:center;font-weight:normal;}th{background:#eee;font-weight:normal;}@media print{body{padding:0;}}</style></head><body onload="window.print(); window.close();">${printHtml}</body></html>`);
    printWindow.document.close();
  };

  const handleFileUpload = (e) => {
    const file = e.target.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (evt) => {
      const arrayBuffer = evt.target.result;
      const decoders = ["utf-8", "utf-16", "windows-1252"];
      let text = "";
      let recovered = false;
      for (let enc of decoders) {
          try {
              const decoder = new TextDecoder(enc);
              const attempt = decoder.decode(arrayBuffer);
              if (/[\u0980-\u09FF]/.test(attempt)) { text = attempt; recovered = true; break; }
          } catch (e) {}
      }
      if (!recovered) text = new TextDecoder("utf-8").decode(arrayBuffer);
      if (text.startsWith('\uFEFF')) text = text.substring(1);
      const lines = text.split(/\r?\n/).filter(l => l.trim() !== "");
      if (lines.length < 2) return alert("File is empty or invalid!");
      const parseCSVLine = (line) => {
          const result = []; let current = ""; let inQuotes = false;
          for (let i = 0; i < line.length; i++) {
              const char = line[i];
              if (char === '"') inQuotes = !inQuotes;
              else if (char === ',' && !inQuotes) { result.push(current.trim().replace(/^"|"$/g, '')); current = ""; }
              else current += char;
          }
          result.push(current.trim().replace(/^"|"$/g, '')); return result;
      };
      const headers = parseCSVLine(lines[0]);
      const dataRows = lines.slice(1).map(line => parseCSVLine(line));
      setCsvHeaders(headers); setCsvData(dataRows);
      const initialMap = {};
      dbFields.forEach(field => {
        const match = headers.find(h => h.toLowerCase().includes(field.key.toLowerCase()) || h.toLowerCase() === field.label.toLowerCase() || (field.key === 'name' && (h.includes('নাম') || h.includes('Name'))) || (field.key === 'mobile' && (h.includes('মোাবাইল') || h.includes('Phone'))) || (field.key === 'address' && (h.includes('ঠিকানা') || h.includes('Address'))) || (field.key === 'zone' && (h.includes('জোন') || h.includes('গলি') || h.includes('বক্স'))));
        if (match) initialMap[field.key] = match;
      });
      setMapping(initialMap); setImportStep(2);
    };
    reader.readAsArrayBuffer(file);
  };

  const startBulkImport = async () => {
    setImportStatus('Importing Subscribers...');
    let count = 0;
    const batchSize = 10;
    for (let i = 0; i < csvData.length; i++) {
      const row = csvData[i]; const customerData = { ...initialState };
      dbFields.forEach(field => {
        const csvHeader = mapping[field.key];
        if (csvHeader) {
          const index = csvHeaders.indexOf(csvHeader); let val = row[index];
          if (val === undefined || val === null) val = (field.key === 'monthlyBill' || field.key === 'currentDue' || field.key === 'paid') ? 0 : '';
          if (field.key === 'monthlyBill' || field.key === 'currentDue' || field.key === 'paid') val = parseFloat(val) || 0;
          customerData[field.key] = val;
        }
      });
      const finalData = {};
      Object.keys(customerData).forEach(key => { finalData[key] = customerData[key] !== undefined ? customerData[key] : (initialState[key] || ''); });

      const newCode = finalData.customerCode || `CUST-${Date.now().toString().slice(-4)}${i}`;
      const todayISO = new Date().toLocaleDateString('en-CA');
      const timeStr = new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });

      const { data: newCust, error: custErr } = await supabase.from('customers').insert({
          customer_code: newCode,
          name: finalData.name,
          mobile: finalData.mobile,
          alt_mobile: finalData.altMobile,
          address: finalData.address,
          zone: finalData.zone,
          package_name: finalData.packageName,
          monthly_bill: finalData.monthlyBill,
          current_due: finalData.currentDue,
          status: 'Active',
          join_date: finalData.joinDate || todayISO
      }).select().single();

      if (custErr) {
        console.error("Import error:", custErr);
        continue;
      }

      // Handle Paid Amount
      const paidAmt = parseFloat(finalData.paid) || 0;
      if (paidAmt > 0) {
          await supabase.from('payments').insert({
            customer_id: newCust.id,
            customer_name: finalData.name,
            customer_code: newCode,
            amount: paidAmt,
            payment_method: "Imported (Cash)",
            payment_date: todayISO,
            billing_month: new Intl.DateTimeFormat('en-US', { month: 'long', year: 'numeric' }).format(new Date()),
            receipt_no: `REC-IMP-${Date.now().toString().slice(-4)}${i}`,
            remarks: "Imported from Excel"
          });

          await supabase.from('ledger_entries').insert({
            customer_id: newCust.id,
            date: todayISO,
            time: timeStr,
            type: "Payment",
            description: "Imported from Excel",
            amount: paidAmt,
            is_debit: false
          });
      }

      count++; if (count % batchSize === 0) setImportStatus(`Importing... (${count}/${csvData.length})`);
    }
    setImportStatus(`Successfully Imported ${count} Subscribers!`);
    setTimeout(() => { setShowImportModal(false); setImportStep(1); }, 2000);
  };

  const sendSms = async () => {
    const targets = getTargetCustomers();
    if (targets.length === 0) return alert("No customers selected!");
    if (!smsMessage) return alert("Type a message!");

    const settings = store.settings || {};
    const apiUrl = settings.smsApiUrl || settings.sms_api_url;
    const apiKey = settings.smsApiKey || settings.sms_api_key;
    const senderId = settings.smsSenderId || settings.sms_sender_id;

    if (!apiUrl || !apiKey) {
      return alert("SMS Gateway is not configured in Settings!");
    }

    if (!window.confirm(`Broadcasting SMS to ${targets.length} subscribers. Proceed?`)) return;

    setIsSendingSms(true);
    setSmsProgress({ current: 0, total: targets.length });

    let successCount = 0;
    for (let i = 0; i < targets.length; i++) {
      const customer = targets[i];
      const mobile = customer.mobile;
      if (!mobile) continue;

      // 1. SMART TAG REPLACEMENT (Replaces all tags)
      const currentMonth = new Date().toLocaleString('default', { month: 'long', year: 'numeric' });
      let cleanMessage = smsMessage
        .replace(/{NAME}/g, customer.name || '')
        .replace(/{CUSTOMER_CODE}/g, customer.customerCode || customer.customer_code || '')
        .replace(/{ZONE}/g, customer.zone || '')
        .replace(/{BILL_MONTH}/g, currentMonth)
        .replace(/{AMOUNT}/g, Math.floor(customer.currentDue || customer.current_due || 0))
        .replace(/{DUE}/g, Math.floor(customer.currentDue || customer.current_due || 0))
        .replace(/{TOTAL_DUE}/g, Math.floor(customer.currentDue || customer.current_due || 0));

      try {
        let cleanMobile = mobile.replace(/[^0-9]/g, "");
        if (cleanMobile.startsWith('0')) { cleanMobile = '88' + cleanMobile; }
        else if (cleanMobile.length === 10) { cleanMobile = '880' + cleanMobile; }
        else if (!cleanMobile.startsWith('88')) { cleanMobile = '88' + cleanMobile; }

        if (cleanMobile.length >= 11 && cleanMobile.length <= 13) {
            const isUnicode = /[\u0980-\u09FF]/.test(cleanMessage);
            const msgType = isUnicode ? "unicode" : "text";

            const currentApiKey = (apiKey || "").trim();
            const currentSenderId = (senderId || "").trim();

            let finalUrl = `https://tglplinxvrqsrxeicvpr.supabase.co/functions/v1/sms-proxy?apikey=${currentApiKey}&callerID=${currentSenderId}&number=${cleanMobile}&message=${encodeURIComponent(cleanMessage)}&type=${msgType}`;

            const img = new Image();
            img.src = finalUrl;

            // Restore Log SMS
            await supabase.from('sms_logs').insert({
              id: `LOG-${Date.now()}-${i}`,
              customer_id: customer.id,
              customer_name: customer.name,
              mobile: cleanMobile,
              notification_type: 'Manual Broadcast',
              message: cleanMessage,
              status: 'Sent',
              sent_timestamp: new Date().toISOString()
            });

            if (i === 0 && targets.length > 1) {
                console.log("Broadcast Debug (First URL):", finalUrl);
            }

            successCount++;

            // 5. LOG TO SUPABASE
            await supabase.from('sms_logs').insert({
              id: `LOG-${Date.now()}-${i}`,
              customer_id: customer.id,
              customer_name: customer.name,
              mobile: cleanMobile,
              notification_type: 'Manual Broadcast',
              message: cleanMessage,
              status: 'Sent',
              sent_timestamp: new Date().toISOString()
            });
        }
      } catch (e) {
        console.error("SMS Send Error:", e);
      }

      setSmsProgress({ current: i + 1, total: targets.length });
    }

    setIsSendingSms(false);
    setShowSmsModal(false);
    setSmsMessage('');
    alert(`Broadcast Task Completed! Total Sent: ${successCount}`);
  };

  const handleBulkDelete = async () => {
    if (selectedIds.length === 0) return;
    if (!window.confirm(`Delete ${selectedIds.length} marked subscribers?`)) return;
    try {
      const { error } = await supabase.from('customers').delete().in('id', selectedIds);
      if (error) throw error;
      setSelectedIds([]); setSelectedCust(null); alert("Deleted!");
    } catch (e) { alert("Delete failed!"); }
  };

  const openAddModal = () => { setIsEditing(false); setFormData(initialState); setShowModal(true); };
  const openEditModal = (cust) => { setIsEditing(true); setFormData(cust); setShowModal(true); setActiveMenuId(null); };

  const openZoneChangeModal = (cust) => {
    setCustToChangeZone(cust);
    setNewZoneData({ zone: cust.zone || '', subZone: cust.subZone || '', boxId: cust.boxId || '' });
    setShowZoneChangeModal(true);
    setActiveMenuId(null);
  };

  const handleQuickZoneUpdate = async () => {
    if (!custToChangeZone) return;
    try {
      const { error } = await supabase
        .from('customers')
        .update({
          zone: newZoneData.zone,
          sub_zone: newZoneData.subZone,
          box_id: newZoneData.boxId
        })
        .eq('id', custToChangeZone.id);

      if (error) throw error;
      alert("Zone Updated Successfully!");
      setShowZoneChangeModal(false);
      setCustToChangeZone(null);
    } catch (e) {
      alert("Failed to update zone.");
    }
  };

  const openDateChangeModal = (cust) => {
    // Function to convert DD-MMM-YYYY to YYYY-MM-DD for HTML input
    const toISODate = (str) => {
        if (!str || str === 'Not Set') return '';
        if (/^\d{4}-\d{2}-\d{2}$/.test(str)) return str;
        const monthsArr = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
        const parts = str.split('-');
        if (parts.length === 3) {
            const day = parts[0].padStart(2, '0');
            const month = (monthsArr.indexOf(parts[1]) + 1).toString().padStart(2, '0');
            const year = parts[2];
            if (month !== '00') return `${year}-${month}-${day}`;
        }
        return '';
    };

    setCustToChangeDate(cust);
    setNewDates({
      expireDate: toISODate(cust.expireDate || cust.expire_date),
      requestDate: toISODate(cust.requestDate || cust.request_date)
    });
    setShowDateChangeModal(true);
    setActiveMenuId(null);
  };

  const openPromiseModal = (cust) => {
    setCustForPromise(cust);
    setPromiseData({
      date: cust.promiseDate || cust.promise_date || '',
      note: cust.promiseNote || cust.promise_note || ''
    });
    setShowPromiseModal(true);
    setActiveMenuId(null);
  };

  const handleQuickPromiseUpdate = async () => {
    if (!custForPromise) return;
    setIsSavingPromise(true);
    try {
      const { error } = await supabase
        .from('customers')
        .update({
          promise_date: promiseData.date,
          promise_note: promiseData.note
        })
        .eq('id', custForPromise.id);

      if (error) throw error;
      alert("Bill Promise Saved!");
      setShowPromiseModal(false);
      setCustForPromise(null);
    } catch (e) {
      console.error("Promise save error:", e);
      alert("Failed to save promise: " + (e.message || "Unknown error"));
    } finally {
      setIsSavingPromise(false);
    }
  };

  const handleQuickDateUpdate = async () => {
    if (!custToChangeDate) return;
    try {
      const updates = {
        expire_date: newDates.expireDate || null,
        request_date: newDates.requestDate || null
      };

      // --- AUTO RE-ACTIVATE LOGIC ---
      const now = new Date();
      now.setHours(0, 0, 0, 0);
      const reqDate = newDates.requestDate ? new Date(newDates.requestDate) : null;
      const isFutureRequest = reqDate && reqDate >= now;

      let shouldActivate = false;
      if ((custToChangeDate.status === 'Suspended' || custToChangeDate.status === 'Expired') && isFutureRequest) {
          updates.status = 'Active';
          shouldActivate = true;
      }

      const { error } = await supabase.from('customers').update(updates).eq('id', custToChangeDate.id);
      if (error) throw error;

      // Sync to MikroTik if activated
      if (shouldActivate && (custToChangeDate.pppoeUsername || custToChangeDate.pppoe_username) && (custToChangeDate.routerId || custToChangeDate.router_id)) {
          supabase.functions.invoke('mikrotik-manager', {
            body: {
                action: 'set_status',
                routerId: custToChangeDate.routerId || custToChangeDate.router_id,
                payload: { username: custToChangeDate.pppoeUsername || custToChangeDate.pppoe_username, active: true }
            }
          });
      }

      alert("Dates Updated Successfully!");
      setShowDateChangeModal(false);
      setCustToChangeDate(null);
    } catch (e) {
      console.error("Date update error:", e);
      alert("Failed to update dates. Please try again.");
    }
  };

  const handleSave = async (e) => {
    e.preventDefault();
    if (!formData.assignedStaffId || formData.assignedStaffId === 'No Staff') {
        alert("Please select an Assigned Collector Staff!");
        return;
    }

    const pppoeUser = formData.pppoeUsername.trim().toLowerCase();
    const routerId = formData.routerId;

    // --- DUPLICATE CHECK FOR NEW ENROLLMENT ---
    if (!isEditing && pppoeUser && routerId) {
        // 1. Check if exists in Software Database
        const existsInSoftware = store.customers.some(c => (c.pppoeUsername || c.pppoe_username || '').toLowerCase() === pppoeUser);
        if (existsInSoftware) {
            alert("এই PPPoE ইউজারনেমটি ইতি মধ্যেই বিলিং সফটওয়্যারে আছে! অনুগ্রহ করে অন্য নাম ব্যবহার করুন।");
            return;
        }

        // 2. Check if exists in MikroTik Router
        try {
            console.log("Checking duplicate in MikroTik for:", pppoeUser);
            const { data: invokeResult } = await supabase.functions.invoke('mikrotik-manager', {
                body: { action: 'check_user', routerId: routerId, payload: { username: pppoeUser } }
            });

            console.log("MikroTik Check Result:", invokeResult);

            if (invokeResult?.exists) {
                const choice = window.confirm(`"${invokeResult.matchedName || pppoeUser}" নামটা ইতি মধ্যেই মাইক্র্যোটিকে আছে! আপনি কি এই নামের উপরেই বিলিং সফটওয়্যারে গ্রাহক তৈরি করতে চান?`);
                if (!choice) return; // User cancelled
            }
        } catch (err) {
            console.error("MikroTik duplicate check error:", err);
        }
    }

    try {
      const dbData = {
        customer_code: formData.customerCode,
        name: formData.name,
        mobile: formData.mobile,
        alt_mobile: formData.altMobile,
        address: formData.address,
        zone: formData.zone,
        sub_zone: formData.subZone,
        box_id: formData.boxId,
        package_name: formData.packageName,
        monthly_bill: parseFloat(formData.monthlyBill) || 0,
        pppoe_username: pppoeUser,
        pppoe_password: formData.pppoePassword,
        router_id: routerId,
        billing_type: formData.billingType,
        payment_status: formData.paymentStatus,
        expire_date: formData.expireDate || null,
        request_date: formData.requestDate || null,
        connection_type: formData.connectionType,
        status: formData.status,
        subscription_type: formData.subscriptionType,
        connection_fee: parseFloat(formData.connectionFee) || 0,
        join_date: formData.joinDate,
        reference_name: formData.referenceName,
        reference_mobile: formData.referenceMobile,
        assigned_staff_id: formData.assignedStaffId,
        promise_date: formData.promiseDate,
        promise_note: formData.promiseNote,
        notes: formData.notes,
        current_due: parseFloat(formData.currentDue) || 0,
        advance_balance: parseFloat(formData.advanceBalance) || 0
      };

      if (isEditing) {
        const { error } = await supabase.from('customers').update(dbData).eq('id', formData.id);
        if (error) throw error;

        // MikroTik Sync for existing customer
        if (dbData.pppoe_username && dbData.router_id) {
           console.log("Manual Edit Sync to MikroTik:", dbData.pppoe_username);
           supabase.functions.invoke('mikrotik-manager', {
             body: { action: 'sync_customer', routerId: dbData.router_id, payload: dbData }
           });
        }
        alert("Updated!");
      }
      else {
        // ... (existing logic for new customer)
        const today = new Date();
        const day = today.getDate();
        const previousDue = parseFloat(formData.currentDue) || 0;
        const discount = parseFloat(formData.discountAmount) || 0;
        const monthlyBill = parseFloat(formData.monthlyBill) || 0;

        let currentDue = previousDue;
        let billApplied = false;

        if (day <= 20) {
          currentDue += (monthlyBill - discount);
          billApplied = true;
        } else {
          const choice = window.confirm("গ্রাহক ২০ তারিখের পরে জয়েন করেছেন। চলতি মাসের বিল কি এখনই জেনারেট করবেন?");
          if (choice) {
            currentDue += (monthlyBill - discount);
            billApplied = true;
          } else {
            billApplied = false;
          }
        }

        const newCode = formData.customerCode || `CUST-${Date.now().toString().slice(-4)}`;
        const joinDate = formData.joinDate;

        const { data: insertedData, error: custErr } = await supabase.from('customers').insert({
          ...dbData,
          customer_code: newCode,
          current_due: currentDue,
          advance_balance: 0
        }).select();

        if (custErr) throw custErr;

        const newCust = insertedData[0];

        // MikroTik Sync for new customer
        if (newCust.pppoe_username && newCust.router_id) {
           supabase.functions.invoke('mikrotik-manager', {
             body: { action: 'sync_customer', payload: newCust }
           });
        }

        // ... (remaining invoice/ledger logic)
        if (previousDue > 0 && newCust) {
            await supabase.from('ledger_entries').insert({
                customer_id: newCust.id,
                date: joinDate,
                time: new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }),
                type: "Opening Balance",
                amount: previousDue,
                is_debit: true,
                description: "Opening Due Balance recorded during enrollment"
            });
        }

        if (billApplied && (monthlyBill - discount) > 0 && newCust) {
          const currentMonth = new Intl.DateTimeFormat('en-US', { month: 'long', year: 'numeric' }).format(today);
          const invNo = "INV-" + Math.random().toString(36).substr(2, 6).toUpperCase();
          const netBill = monthlyBill - discount;

          await supabase.from('invoices').insert({
            invoice_no: invNo,
            customer_id: newCust.id,
            customer_name: formData.name,
            billing_month_year: currentMonth,
            bill_amount: monthlyBill,
            discount_amount: discount,
            total_payable: netBill,
            due_amount: netBill,
            status: "Unpaid",
            generated_date: joinDate
          });

          await supabase.from('ledger_entries').insert({
            customer_id: newCust.id,
            date: joinDate,
            time: new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }),
            type: "Monthly Bill",
            amount: netBill,
            is_debit: true,
            description: `${currentMonth} Enrollment Bill Applied (Net: ${netBill}, Disc: ${discount})`,
            reference_no: invNo
          });
        }

        // --- NEW: TRIGGER AUTO SMS FOR CREATE CUSTOMER ---
        const settings = store.settings || {};
        if (settings.isAutoSmsEnabled || settings.is_auto_sms_enabled) {
          const template = store.smsTemplates?.find(t => t.title === 'Create Customer' && (t.isActive || t.is_active));
          if (template && newCust && newCust.mobile) {
            let msg = (template.messageContent || template.message_content)
              .replace(/{NAME}/g, newCust.name || '')
              .replace(/{CUSTOMER_CODE}/g, newCust.customerCode || newCust.customer_code || '')
              .replace(/{COMPANY_NAME}/g, settings.companyName || settings.company_name || 'NetBill ISP')
              .replace(/{SUPPORT_PHONE}/g, settings.companyPhone || settings.company_phone || '');

            let phone = (newCust.mobile || "").replace(/[^0-9]/g, "");
            if (phone.startsWith('0')) phone = '88' + phone;
            else if (phone.length === 10) phone = '880' + phone;
            else if (!phone.startsWith('88')) phone = '88' + phone;

            const isUni = /[\u0980-\u09FF]/.test(msg);
            const finalUrl = `https://tglplinxvrqsrxeicvpr.supabase.co/functions/v1/sms-proxy?apikey=${settings.smsApiKey || settings.sms_api_key}&callerID=${settings.smsSenderId || settings.sms_sender_id}&number=${phone}&message=${encodeURIComponent(msg)}&type=${isUni ? "unicode" : "text"}`;

            // Dispatch using Image ping (CORS-safe)
            new Image().src = finalUrl;

            // Record in SMS Logs
            await supabase.from('sms_logs').insert({
              customer_id: newCust.id,
              customer_name: newCust.name,
              mobile: phone,
              notification_type: 'Create Customer',
              message: msg,
              status: 'Sent',
              sent_timestamp: new Date().toISOString()
            });
          }
        }

        alert("Customer Enrolled Successfully!");
      }
      setShowModal(false);
      if (setPreSelectedCustomer) setPreSelectedCustomer(null);
    } catch (err) {
      console.error(err);
      alert("Error saving!");
    }
  };


  const handleDelete = async (id) => {
    if (window.confirm("Delete permanently?")) {
      const { error } = await supabase.from('customers').delete().eq('id', id);
      if (!error) {
        setSelectedCust(null);
        setActiveMenuId(null);
      }
    }
  };

  const toggleStatus = async (cust) => {
    const nextStatus = cust.status === 'Active' ? 'Inactive' : 'Active';
    const { error } = await supabase.from('customers').update({ status: nextStatus }).eq('id', cust.id);

    const pppoeUser = cust.pppoeUsername || cust.pppoe_username;
    const rId = cust.routerId || cust.router_id;

    if (!error) {
       if (pppoeUser && rId && rId !== 'Select Router') {
           console.log(`Sending command to MikroTik for: ${pppoeUser} on Router: ${rId}`);
           const { data, error: funcErr } = await supabase.functions.invoke('mikrotik-manager', {
             body: { action: 'set_status', routerId: rId, payload: { username: pppoeUser, active: nextStatus === 'Active' } }
           });

           if (data?.success) {
               alert(`MikroTik: Customer ${nextStatus === 'Active' ? 'Enabled' : 'Disabled'} Successfully!`);
           } else {
               alert(`MikroTik Sync Failed: ${data?.error || 'Check router connection'}`);
           }
       } else {
           alert(`Cannot sync to MikroTik: ${!pppoeUser ? 'PPPoE Username' : 'Router Selection'} is missing for ${cust.name}. Please EDIT the customer and select a router.`);
       }
    }
    setActiveMenuId(null);
  };

  const formatDateDisplay = (dateStr) => {
    if (!dateStr || dateStr === 'Not Set') return dateStr || '';
    if (/^\d{2}-\d{2}-\d{4}$/.test(dateStr)) return dateStr;
    const parts = dateStr.split('-');
    if (parts.length === 3 && parts[0].length === 4) return `${parts[2]}-${parts[1]}-${parts[0]}`;
    return dateStr;
  };

  return (
    <div className={hideTable ? "" : "w-full px-4 space-y-6 pb-10 uppercase font-black tracking-tighter transition-all"}>
      {!hideTable && (
        isDirectMode ? (
            <div className="flex items-center justify-center py-20 animate-pulse">
               <p className="text-2xl text-slate-400 font-black tracking-[10px]">OPENING CLOUD ENROLLMENT...</p>
            </div>
          ) : (<>
          {/* Header & Stats Row */}
          <div className="flex flex-col xl:flex-row justify-between items-start xl:items-center gap-4 bg-white dark:bg-slate-800 p-6 rounded-3xl shadow-xl border border-slate-100 dark:border-slate-700">
            <div className="flex items-center space-x-4">
               <button onClick={() => setActivePage('dashboard')} className="w-10 h-10 md:w-12 md:h-12 bg-slate-50 dark:bg-slate-900 rounded-xl flex items-center justify-center text-teal-600 hover:bg-teal-600 hover:text-white transition-all shadow-sm">
                  <i className="fas fa-arrow-left"></i>
               </button>
               <div className="space-y-1">
                  <h3 className="text-3xl font-black text-slate-800 dark:text-white uppercase tracking-tighter leading-none">{t.subscribers_crm}</h3>
                  <p className="text-[10px] text-teal-600 font-bold tracking-[4px] uppercase mt-1">Enterprise Subscriber Management System</p>
               </div>
            </div>
            <div className="flex flex-wrap gap-3">
               {(() => {
                  let list = store.customers;
                  if (session?.role === 'staff') {
                      list = list.filter(c => (c.assignedStaffId || c.assigned_staff_id) === session.data.id || (c.assignedStaffId || c.assigned_staff_id) === session.data.name);
                  }
                  const total = list.length;
                  const active = list.filter(c => c.status === 'Active').length;
                  return (
                    <>
                      <StatCard label="TOTAL" value={total} color="slate" />
                      <StatCard label="MARKED" value={selectedIds.length} color="indigo" />
                      <StatCard label="ACTIVE" value={active} color="emerald" />
                    </>
                  );
               })()}
               {session?.role === 'admin' && (
                 <button onClick={openAddModal} className="bg-[#0D9488] text-white px-8 py-4 rounded-2xl shadow-2xl font-black uppercase text-sm tracking-[2px] transition-all hover:scale-105 active:scale-95 border-b-4 border-teal-900">+ {t.new_enrollment}</button>
               )}
            </div>
          </div>

          {/* Toolbar */}
          <div className="flex flex-col xl:flex-row gap-4 items-center">
              <div className="relative w-full xl:max-w-xl group">
                <input type="text" placeholder={t.search_placeholder} value={search} onChange={(e) => setSearch(e.target.value)} className="pl-12 pr-6 py-4 bg-white dark:bg-slate-800 rounded-3xl border-none shadow-2xl focus:ring-4 focus:ring-teal-500/5 font-black text-lg md:text-xl transition-all uppercase placeholder:opacity-30 w-full" />
                <i className="fas fa-search absolute left-5 top-5 text-slate-300 text-xl group-focus-within:text-teal-500 transition-colors"></i>
              </div>
              <div className="flex flex-wrap gap-2 md:gap-3 font-black justify-center md:justify-start">
                 <button onClick={() => setShowFilterDrawer(true)} className="bg-white dark:bg-slate-800 text-teal-600 px-4 md:px-6 py-2.5 md:py-3 rounded-xl md:rounded-[20px] font-black text-[9px] md:text-[10px] flex items-center space-x-2 md:space-x-3 shadow-xl hover:scale-105 transition-all uppercase tracking-widest border-2 border-teal-500/20 leading-none h-11 md:h-auto">
                    <i className="fas fa-filter text-base md:text-lg"></i>
                    <span className="hidden sm:inline">Advanced Filter</span>
                    {Object.values(filters).filter(v => v !== 'All' && v !== '' && v !== false).length > 0 && `(${Object.values(filters).filter(v => v !== 'All' && v !== '' && v !== false).length})`}
                 </button>
                 <ActionButtonSmall label="EXCEL" icon="fa-file-excel" onClick={downloadExcel} />
                 <ActionButtonSmall label="PRINT" icon="fa-print" onClick={handlePrint} />
                 <ActionButtonSmall label="COLS" icon="fa-columns" onClick={() => setShowColumnSelector(true)} />
                 <ActionButtonSmall label="SMS" icon="fa-paper-plane" onClick={() => setShowSmsModal(true)} />
                 <button onClick={() => setShowImportModal(true)} className="bg-rose-600 text-white px-4 md:px-6 py-2.5 md:py-3 rounded-xl md:rounded-[20px] font-black text-[9px] md:text-[10px] flex items-center space-x-2 md:space-x-3 shadow-xl hover:scale-105 transition-all uppercase tracking-widest leading-none h-11 md:h-auto"><i className="fas fa-file-import text-base md:text-lg"></i><span className="hidden sm:inline">{t.import_excel}</span><span className="sm:hidden">IMPORT</span></button>
             {selectedIds.length > 0 && session?.role === 'admin' && (
               <button onClick={handleBulkDelete} className="bg-red-600 text-white px-4 md:px-6 py-2.5 md:py-3 rounded-xl md:rounded-[20px] font-black text-[9px] md:text-[10px] flex items-center space-x-2 md:space-x-3 shadow-xl hover:scale-105 transition-all uppercase tracking-widest leading-none h-11 md:h-auto animate-bounce"><i className="fas fa-trash-alt text-base md:text-lg"></i><span>DEL ({selectedIds.length})</span></button>
             )}
              </div>
          </div>

          <div className="bg-white dark:bg-slate-800 rounded-3xl shadow-2xl border border-slate-100 dark:border-slate-700 overflow-hidden font-black transition-all duration-500">
              <div className="overflow-x-auto custom-scrollbar min-h-[500px]">
                <table className="w-full text-center uppercase tracking-tighter whitespace-nowrap">
                  <thead className="bg-slate-50 dark:bg-slate-900 border-b-2 border-slate-100 dark:border-slate-700 text-[10px] text-slate-500 tracking-[1px] font-black uppercase">
                    <tr>
                      {visibleColumns.cb && <th className="p-4 text-center"><input type="checkbox" checked={selectedIds.length === filteredCustomers.length && filteredCustomers.length > 0} onChange={toggleSelectAll} className="w-6 h-6 rounded-lg border-slate-300 text-teal-600 focus:ring-0 cursor-pointer" /></th>}
                      {visibleColumns.id && <th className="p-3 cursor-pointer hover:text-teal-600 transition-colors" onClick={() => requestSort('customerCode')}>{t.id} <i className={`fas ${getSortIcon('customerCode')} ml-1`}></i></th>}
                      {visibleColumns.sl && <th className="p-3">{t.sl}</th>}
                      {visibleColumns.customer && <th className="p-3 text-left cursor-pointer hover:text-teal-600 transition-colors" onClick={() => requestSort('name')}>{t.customer} <i className={`fas ${getSortIcon('name')} ml-1`}></i></th>}
                      {visibleColumns.mikrotik && <th className="p-3 text-left cursor-pointer hover:text-teal-600 transition-colors" onClick={() => requestSort('pppoeUsername')}>{t.mikrotik_user} <i className={`fas ${getSortIcon('pppoeUsername')} ml-1`}></i></th>}
                      {visibleColumns.zone && <th className="p-3 cursor-pointer hover:text-teal-600 transition-colors" onClick={() => requestSort('zone')}>{t.zone} <i className={`fas ${getSortIcon('zone')} ml-1`}></i></th>}
                      {visibleColumns.plan && <th className="p-3 cursor-pointer hover:text-teal-600 transition-colors" onClick={() => requestSort('packageName')}>{t.plan} <i className={`fas ${getSortIcon('packageName')} ml-1`}></i></th>}
                      {visibleColumns.bill && <th className="p-3 cursor-pointer hover:text-teal-600 transition-colors" onClick={() => requestSort('monthlyBill')}>{t.bill} <i className={`fas ${getSortIcon('monthlyBill')} ml-1`}></i></th>}
                      {visibleColumns.join && <th className="p-3 cursor-pointer hover:text-teal-600 transition-colors" onClick={() => requestSort('joinDate')}>JOIN DATE <i className={`fas ${getSortIcon('joinDate')} ml-1`}></i></th>}
                      {visibleColumns.expire && <th className="p-3 cursor-pointer hover:text-teal-600 transition-colors" onClick={() => requestSort('expireDate')}>{t.expire_date} <i className={`fas ${getSortIcon('expireDate')} ml-1`}></i></th>}
                      {visibleColumns.collector && <th className="p-3 cursor-pointer hover:text-teal-600 transition-colors" onClick={() => requestSort('assignedStaffId')}>ASSIGNED COLLECTOR <i className={`fas ${getSortIcon('assignedStaffId')} ml-1`}></i></th>}
                      {visibleColumns.status && <th className="p-3 cursor-pointer hover:text-teal-600 transition-colors" onClick={() => requestSort('status')}>{t.status} <i className={`fas ${getSortIcon('status')} ml-1`}></i></th>}
                      {visibleColumns.online && <th className="p-3">{t.online}</th>}
                      {visibleColumns.actions && <th className="p-4">{t.actions}</th>}
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-50 dark:divide-slate-700">
                    {sortedCustomers.map((c, idx) => (
                      <tr key={c.id} className={`transition-all hover:bg-teal-50/50`}>
                        {visibleColumns.cb && <td className="p-4 text-center" onClick={(e)=>e.stopPropagation()}><input type="checkbox" checked={selectedIds.includes(c.id)} onChange={() => toggleSelect(c.id)} className="w-6 h-6 rounded-lg border-slate-200 text-teal-600 cursor-pointer" /></td>}
                        {visibleColumns.id && <td className="p-3 text-slate-600 text-[10px] font-black">#{c.customerCode?.split('-')[1] || c.customerCode}</td>}
                        {visibleColumns.sl && <td className="p-3 text-slate-400 text-[10px]">{idx + 1}</td>}
                        {visibleColumns.customer && (
                          <td className="p-3 text-left font-black leading-tight">
                            <p className="text-lg text-slate-800 dark:text-white uppercase tracking-tighter">{c.name}</p>
                            <div className="flex items-center space-x-2 mt-1">
                               <button
                                 onClick={(e) => {
                                    e.stopPropagation();
                                    setWaTargetCust(c);
                                    setWaMessage('');
                                    setShowWhatsAppModal(true);
                                 }}
                                 className="text-emerald-500 hover:text-emerald-600 transition-transform hover:scale-125"
                               >
                                  <i className="fab fa-whatsapp text-lg"></i>
                               </button>
                               <p className="text-base text-indigo-600 dark:text-indigo-400 font-black uppercase tracking-tighter">
                                  {c.mobile?.startsWith('88') ? c.mobile.substring(2) : c.mobile}
                               </p>
                            </div>
                          </td>
                        )}
                        {visibleColumns.mikrotik && <td className="p-3 text-left text-xs text-slate-700 dark:text-slate-300 font-bold normal-case">{(c.pppoeUsername || '---').toLowerCase()}</td>}
                        {visibleColumns.zone && <td className="p-3 text-sm text-blue-700 dark:text-blue-400 font-black tracking-tight">{c.zone || 'Global'}</td>}
                        {visibleColumns.plan && <td className="p-3 text-lg text-teal-600 font-black tracking-tighter">{c.packageName?.match(/\d+/)?.[0] || c.packageName}MB</td>}
                        {visibleColumns.bill && <td className="p-3 text-center leading-tight min-w-[120px]">{(() => {
                          const today = new Date();
                          const currentMonth = today.getMonth() + 1;
                          const currentYear = today.getFullYear();
                          const currentMonthStr = `${currentYear}-${currentMonth.toString().padStart(2, '0')}`;

                          const paidThisMonth = store.payments?.filter(p => {
                            const cid = p.customerId || p.customer_id || p.customerCode || p.customer_code;
                            const isCustomerMatch = (cid === c.id || cid === c.customerCode || cid === c.customer_code);
                            if (!isCustomerMatch) return false;

                            const pDate = p.paymentDate || p.payment_date || p.date;
                            return pDate && (pDate.includes(currentMonthStr) || pDate.includes(`${currentMonth.toString().padStart(2, '0')}-${currentYear}`));
                          }).reduce((s, p) => s + (parseFloat(p.amount) || 0), 0) || 0;

                          const currentDue = parseFloat(c.currentDue || c.current_due || 0);
                          const monthlyBill = parseFloat(c.monthlyBill || c.monthly_bill || 0);
                          const advanceBalance = parseFloat(c.advanceBalance || c.advance_balance || 0);

                          return (<>
                            <p className="text-[14px] font-black text-slate-800 dark:text-white uppercase">Bill: ৳{monthlyBill}</p>
                            <p className="text-[14px] font-black text-emerald-600 mt-1 uppercase">Paid: ৳{Math.floor(paidThisMonth)}</p>
                            <p className="text-[16px] font-black text-rose-500 mt-1 uppercase border-t-2 border-slate-100 dark:border-slate-800 pt-1 shadow-sm">DUE: ৳{Math.floor(currentDue)}</p>
                            {advanceBalance > 0 && <p className="text-[10px] font-black text-teal-600 mt-0.5 uppercase tracking-widest">অগ্রীম: ৳{Math.floor(advanceBalance)}</p>}
                          </>);
                        })()}</td>}
                        {visibleColumns.join && <td className="p-3 text-[14px] text-slate-800 dark:text-white font-black">{formatDateDisplay(c.joinDate)}</td>}
                        {visibleColumns.expire && <td className="p-3 text-center leading-tight">
                          <p className="text-[16px] font-black text-rose-600 dark:text-rose-400 uppercase tracking-tighter shadow-sm">
                            {formatDateDisplay(c.expireDate || c.expire_date || c.expiryDate || c.expiry_date || 'Not Set')}
                          </p>
                          <p className="text-[10px] font-black text-slate-400 mt-1 uppercase">
                            Req: {formatDateDisplay(c.requestDate || c.request_date || 'Not Set')}
                          </p>
                        </td>}
                        {visibleColumns.collector && (
                          <td className="p-3 text-center">
                            <span className={`px-3 py-1 rounded-lg text-[9px] font-black uppercase border shadow-sm ${getCollectorColor((() => {
                                  const sid = c.assignedStaffId || c.assigned_staff_id || c.collectorId || c.collector_id;
                                  const staff = store.staff?.find(s => s.id === sid || s.name === sid);
                                  return staff?.name || sid || '---';
                              })())}`}>
                              {(() => {
                                  const sid = c.assignedStaffId || c.assigned_staff_id || c.collectorId || c.collector_id;
                                  const staff = store.staff?.find(s => s.id === sid || s.name === sid);
                                  return staff?.name || sid || '---';
                              })()}
                            </span>
                          </td>
                        )}
                        {visibleColumns.status && (
                          <td className="p-3">
                            <div className="flex flex-col space-y-1">
                              <span className={`px-4 py-1.5 rounded-full text-[9px] font-black uppercase ${c.status === 'Active' ? 'bg-emerald-100 text-emerald-700' : 'bg-rose-100 text-rose-700'} shadow-md`}>
                                {c.status}
                              </span>
                              <span className={`px-4 py-1 rounded-full text-[8px] font-black uppercase ${c.paymentStatus === 'Paid' ? 'bg-teal-500 text-white' : 'bg-orange-500 text-white'}`}>
                                {c.paymentStatus}
                              </span>
                            </div>
                          </td>
                        )}
                        {visibleColumns.online && (
                          <td className="p-3 text-center">
                            {(() => {
                              const isOnline = store.onlineUsernames?.includes((c.pppoeUsername || '').toLowerCase());
                              const status = c.status || 'Active';

                              if (isOnline) {
                                return (
                                  <div className="flex flex-col items-center justify-center space-y-1">
                                    <div className="w-4 h-4 rounded-full bg-emerald-500 animate-pulse ring-4 ring-emerald-500/20 shadow-[0_0_15px_rgba(16,185,129,0.5)]"></div>
                                    <span className="text-[8px] font-black text-emerald-600">ONLINE</span>
                                  </div>
                                );
                              } else if (status === 'Active') {
                                return (
                                  <div className="flex flex-col items-center justify-center space-y-1">
                                    <div className="w-4 h-4 rounded-full bg-rose-500 shadow-[0_0_10px_rgba(244,63,94,0.3)]"></div>
                                    <span className="text-[8px] font-black text-rose-500">OFFLINE</span>
                                  </div>
                                );
                              } else {
                                return (
                                  <div className="flex flex-col items-center justify-center space-y-1">
                                    <div className="w-4 h-4 rounded-full bg-slate-300"></div>
                                    <span className="text-[8px] font-black text-slate-400 opacity-50">INACTIVE</span>
                                  </div>
                                );
                              }
                            })()}
                          </td>
                        )}
                        {visibleColumns.actions && session?.role === 'admin' && (
                          <td className="p-4 relative">
                             <button onClick={(e) => { e.stopPropagation(); setActiveMenuId(activeMenuId === c.id ? null : c.id); }} className="w-10 h-10 rounded-xl bg-slate-100 dark:bg-slate-900 text-slate-500 hover:bg-teal-600 hover:text-white transition-all shadow-xl"><i className="fas fa-ellipsis-v text-lg"></i></button>
                             {activeMenuId === c.id && (
                               <div className="absolute right-20 top-0 w-64 bg-white dark:bg-slate-800 rounded-[32px] shadow-[0_20px_50px_rgba(0,0,0,0.3)] border border-slate-100 dark:border-slate-700 z-[100] py-4 animate-scaleIn overflow-hidden font-black">
                                  <ActionItem icon="fa-hand-holding-dollar" label={t.action_payment} color="text-emerald-600" onClick={() => { setPreSelectedCustomer(c); setActivePage('payments'); }} />
                                  <ActionItem icon="fa-power-off" label={c.status === 'Active' ? 'Disable / Inactive' : 'Enable / Active'} color={c.status === 'Active' ? 'text-rose-500' : 'text-emerald-500'} onClick={() => toggleStatus(c)} />
                                  <ActionItem icon="fa-user-circle" label={t.action_profile} color="text-blue-600" onClick={() => setProfileId(c.id)} />
                                  <ActionItem icon="fa-calendar-check" label={t.action_promise} color="text-indigo-600" onClick={() => openPromiseModal(c)} />
                                  <ActionItem icon="fa-calendar-day" label="Change Dates" color="text-amber-600" onClick={() => openDateChangeModal(c)} />
                                  <ActionItem icon="fa-map-location-dot" label="Change Zone" color="text-teal-600" onClick={() => openZoneChangeModal(c)} />
                                  <ActionItem icon="fa-edit" label={t.action_edit} color="text-slate-600" onClick={() => openEditModal(c)} />
                                  <ActionItem icon="fa-trash" label={t.action_delete} color="text-rose-600" onClick={() => handleDelete(c.id)} />
                               </div>
                             )}
                          </td>
                        )}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
          </div>
          </>
        ))
      }

      {/* DYNAMIC IMPORT MODAL */}
      <ActionModals
        showColumnSelector={showColumnSelector}
        setShowColumnSelector={setShowColumnSelector}
        visibleColumns={visibleColumns}
        setVisibleColumns={setVisibleColumns}
        showSmsModal={showSmsModal}
        setShowSmsModal={setShowSmsModal}
        smsMessage={smsMessage}
        setSmsMessage={setSmsMessage}
        selectedIds={selectedIds}
        sendSms={sendSms}
        isSendingSms={isSendingSms}
        smsProgress={smsProgress}
        showImportModal={showImportModal}
        setShowImportModal={setShowImportModal}
        importStep={importStep}
        setImportStep={setImportStep}
        handleFileUpload={handleFileUpload}
        dbFields={dbFields}
        csvHeaders={csvHeaders}
        mapping={mapping}
        setMapping={setMapping}
        importStatus={importStatus}
        startBulkImport={startBulkImport}
        t={t}
        showFilterDrawer={showFilterDrawer}
        setShowFilterDrawer={setShowFilterDrawer}
        filters={filters}
        setFilters={setFilters}
        store={store}
        showZoneChangeModal={showZoneChangeModal}
        setShowZoneChangeModal={setShowZoneChangeModal}
        custToChangeZone={custToChangeZone}
        newZoneData={newZoneData}
        setNewZoneData={setNewZoneData}
        handleQuickZoneUpdate={handleQuickZoneUpdate}
        showDateChangeModal={showDateChangeModal}
        setShowDateChangeModal={setShowDateChangeModal}
        custToChangeDate={custToChangeDate}
        newDates={newDates}
        setNewDates={setNewDates}
        handleQuickDateUpdate={handleQuickDateUpdate}
        showWhatsAppModal={showWhatsAppModal}
        setShowWhatsAppModal={setShowWhatsAppModal}
        waTargetCust={waTargetCust}
        waMessage={waMessage}
        setWaMessage={setWaMessage}
        showPromiseModal={showPromiseModal}
        setShowPromiseModal={setShowPromiseModal}
        custForPromise={custForPromise}
        promiseData={promiseData}
        setPromiseData={setPromiseData}
        handleQuickPromiseUpdate={handleQuickPromiseUpdate}
        isSavingPromise={isSavingPromise}
      />

      {showModal && (
        <div className="fixed inset-0 bg-slate-900/95 backdrop-blur-3xl z-[500] flex items-start justify-center p-4 md:p-6 overflow-y-auto animate-fadeIn font-black scroll-smooth">
          <div className="bg-white dark:bg-slate-800 rounded-[32px] md:rounded-[72px] w-full max-w-[98%] md:max-w-[96%] my-4 md:my-10 p-6 md:p-16 shadow-2xl space-y-8 md:space-y-14 relative overflow-hidden border-2 border-slate-100">
            <div className="flex justify-between items-center border-b-2 border-slate-50 pb-6 md:pb-10">
              <div className="flex items-center space-x-3 md:space-x-6">
                <div className="w-12 h-12 md:w-20 md:h-20 bg-teal-600 text-white rounded-2xl md:rounded-[28px] flex items-center justify-center shadow-2xl shadow-teal-500/40"><i className="fas fa-user-plus text-xl md:text-4xl"></i></div>
                <h3 className="text-2xl md:text-6xl font-black uppercase tracking-tighter">{isEditing ? t.update_identity : t.new_enrollment}</h3>
              </div>
              <button onClick={() => {
                  setShowModal(false);
                  if(setPreSelectedCustomer) setPreSelectedCustomer(null);
                  if(isDirectMode) setActivePage('dashboard');
              }} className="w-12 h-12 md:w-20 md:h-20 bg-rose-50 text-rose-500 hover:bg-rose-500 hover:text-white rounded-full flex items-center justify-center transition-all shadow-2xl group"><i className="fas fa-times text-xl md:text-3xl group-hover:rotate-90 transition-transform"></i></button>
            </div>
            <form onSubmit={handleSave} className="space-y-8 md:space-y-14">
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 md:gap-10">
                <Section title={t.router_zone_info} color="blue" bgColor="bg-blue-50 dark:bg-blue-900/10" borderColor="border-blue-100" shadowColor="shadow-blue-500/20">
                  <Field label={t.mikrotik_router} value={formData.routerId} onChange={v => setFormData({...formData, routerId: v})} type="select" options={['Select Router', ...store.mikrotikRouters?.map(r => ({ label: r.name, value: r.id } || r.name))]} color="blue" />
                  <Field label={t.package} value={formData.packageName} onChange={v => setFormData({...formData, packageName: v})} type="select" options={['No Package', ...store.packages?.map(p => p.name)]} color="blue" />

                  <div className="space-y-3 uppercase font-black relative">
                    <label className="text-[10px] md:text-[12px] text-slate-600 dark:text-slate-300 ml-2 tracking-widest uppercase leading-none">{t.zone}</label>
                    <div className="relative group">
                      <input
                        type="text"
                        placeholder="Search or Select Zone..."
                        value={formData.zone}
                        onChange={e => setFormData({...formData, zone: e.target.value, subZone: '', boxId: ''})}
                        className="w-full bg-white dark:bg-slate-800 p-4 md:p-6 rounded-2xl md:rounded-[24px] border-2 border-transparent focus:border-teal-500 text-base md:text-lg font-black shadow-lg outline-none transition-all uppercase"
                      />
                      <i className="fas fa-search absolute right-5 md:right-6 top-5 md:top-6 text-slate-300"></i>

                      {/* Suggestion Dropdown */}
                      {store.zones?.filter(z => z.name.toLowerCase().includes(formData.zone?.toLowerCase())).length > 0 && formData.zone !== '' && !store.zones.some(z => z.name === formData.zone) && (
                        <div className="absolute left-0 right-0 top-full mt-2 bg-white dark:bg-slate-800 rounded-2xl md:rounded-3xl shadow-2xl border border-slate-100 dark:border-slate-700 z-[1000] overflow-hidden max-h-60 overflow-y-auto">
                          {store.zones.filter(z => z.name.toLowerCase().includes(formData.zone.toLowerCase())).map(z => (
                            <div
                              key={z.id}
                              onClick={() => setFormData({...formData, zone: z.name, subZone: '', boxId: ''})}
                              className="p-4 md:p-5 hover:bg-teal-50 dark:hover:bg-teal-900/20 cursor-pointer border-b last:border-0 font-black text-xs md:text-sm"
                            >
                              {z.name}
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  </div>

                  <Field label={t.sub_zone} value={formData.subZone} onChange={v => setFormData({...formData, subZone: v})} type="select" options={['All Sub-Zones', ...store.subZones?.filter(sz => !formData.zone || formData.zone === 'All Zones' || store.zones?.find(z => z.name === formData.zone)?.id === sz.zoneId).map(sz => sz.name)]} color="blue" />
                  <Field label={t.box} value={formData.boxId} onChange={v => setFormData({...formData, boxId: v})} type="select" options={['All Boxes', ...store.boxes?.filter(b => !formData.subZone || formData.subZone === 'All Sub-Zones' || store.subZones?.find(sz => sz.name === formData.subZone)?.id === b.subZoneId).map(b => b.name)]} color="blue" />
                  <Field label={t.location} value={formData.address} onChange={v => setFormData({...formData, address: v})} placeholder="Address..." color="blue" />
                </Section>
                <Section title={t.client_info} color="indigo" bgColor="bg-indigo-50 dark:bg-indigo-900/10" borderColor="border-indigo-100" shadowColor="shadow-indigo-500/20">
                  <Field label="ID" value={formData.customerCode} onChange={v => setFormData({...formData, customerCode: v})} placeholder="Optional" color="indigo" />
                  <Field label="Name *" value={formData.name} onChange={v => setFormData({...formData, name: v})} placeholder="Full name" color="indigo" />
                  <Field label="Mobile *" value={formData.mobile} onChange={v => setFormData({...formData, mobile: v})} placeholder="017xxxx" color="indigo" />
                  <Field label={t.alt_mobile} value={formData.altMobile} onChange={v => setFormData({...formData, altMobile: v})} placeholder="Mobile 2" color="indigo" />
                  <Field label="PPPoE *" value={formData.pppoeUsername} onChange={v => setFormData({...formData, pppoeUsername: v.toLowerCase()})} placeholder="user@isp" color="indigo" />
                  <Field label="Pass *" value={formData.pppoePassword} onChange={v => setFormData({...formData, pppoePassword: v})} type="text" color="indigo" />
                  <Field label="ONU" value={formData.onuMac} onChange={v => setFormData({...formData, onuMac: v})} placeholder="MAC..." color="indigo" />
                </Section>
                <Section title={t.billing_info} color="emerald" bgColor="bg-emerald-50 dark:bg-emerald-900/10" borderColor="border-emerald-100" shadowColor="shadow-emerald-500/20">
                  <Field label="Type" value={formData.billingType} onChange={v => setFormData({...formData, billingType: v})} type="select" options={['MONTHLY DATE TO DATE']} color="emerald" />
                  <Field label="Status" value={formData.paymentStatus} onChange={v => setFormData({...formData, paymentStatus: v})} type="select" options={['Unpaid', 'Paid']} color="emerald" />
                  <Field label="Expiry" value={formData.expireDate} onChange={v => setFormData({...formData, expireDate: v})} type="date" color="emerald" />
                  <Field label="Request" value={formData.requestDate} onChange={v => setFormData({...formData, requestDate: v})} type="date" color="emerald" />
                  <Field label="Media" value={formData.connectionType} onChange={v => setFormData({...formData, connectionType: v})} type="select" options={['FTTH', 'LAN']} color="emerald" />
                  <Field label="Account *" value={formData.status} onChange={v => setFormData({...formData, status: v})} type="radio" options={['Active', 'Inactive', 'Expired']} color="emerald" />
                  <Field label="Sub *" value={formData.subscriptionType} onChange={v => setFormData({...formData, subscriptionType: v})} type="radio" options={['Prepaid', 'Postpaid']} color="emerald" />
                </Section>
                <Section title={t.client_fee} color="cyan" bgColor="bg-cyan-50 dark:bg-cyan-900/10" borderColor="border-cyan-100" shadowColor="shadow-cyan-400/30">
                  <Field label="Bill *" value={formData.monthlyBill} onChange={v => setFormData({...formData, monthlyBill: parseFloat(v) || 0})} type="number" color="cyan" />
                  <Field label="Discount (৳)" value={formData.discountAmount} onChange={v => setFormData({...formData, discountAmount: parseFloat(v) || 0})} type="number" color="cyan" />
                  <Field label="Previous Due (৳)" value={formData.currentDue} onChange={v => setFormData({...formData, currentDue: parseFloat(v) || 0})} type="number" color="cyan" />
                  <Field label="Advance Balance (৳)" value={formData.advanceBalance} onChange={v => setFormData({...formData, advanceBalance: parseFloat(v) || 0})} type="number" color="cyan" />
                  <Field label="Fee" value={formData.connectionFee} onChange={v => setFormData({...formData, connectionFee: parseFloat(v) || 0})} type="number" color="cyan" />
                  <Field label="Join Date" value={formData.joinDate} onChange={v => setFormData({...formData, joinDate: v})} type="date" color="cyan" />
                  <div className="pt-4 md:pt-8 border-t-2 border-cyan-200">
                    <Field label={t.assigned_staff + " *"} value={formData.assignedStaffId} onChange={v => setFormData({...formData, assignedStaffId: v})} type="select" options={['No Staff', ...store.staff?.map(s => s.name)]} color="cyan" />
                  </div>

                  <div className="space-y-4 pt-4 border-t-2 border-cyan-200">
                    <p className="text-[10px] text-slate-400 font-black tracking-[4px] uppercase text-center">Bill Promise (প্রতিশ্রুতি)</p>
                    <div className="grid grid-cols-2 gap-3">
                        <div className="space-y-2">
                           <label className="text-[10px] text-slate-400 ml-2 uppercase">Date</label>
                           <input type="date" value={formData.promiseDate} onChange={e => setFormData({...formData, promiseDate: e.target.value})} className="w-full bg-white p-3 rounded-xl border font-black text-xs" />
                        </div>
                        <div className="space-y-2">
                           <label className="text-[10px] text-slate-400 ml-2 uppercase">Note</label>
                           <input type="text" placeholder="Note..." value={formData.promiseNote} onChange={e => setFormData({...formData, promiseNote: e.target.value})} className="w-full bg-white p-3 rounded-xl border font-black text-xs" />
                        </div>
                    </div>
                  </div>

                  <div className="bg-white dark:bg-slate-900 p-4 md:p-6 rounded-[24px] md:rounded-[40px] space-y-4 md:space-y-5 shadow-inner">
                    <p className="text-[10px] md:text-[11px] text-slate-400 font-black text-center tracking-[2px] md:tracking-[4px]">REF</p>
                    <input type="text" placeholder={t.ref_name} value={formData.referenceName} onChange={e => setFormData({...formData, referenceName: e.target.value})} className="bg-slate-50 dark:bg-slate-800 p-3 md:p-4 rounded-xl md:rounded-2xl border-none text-[10px] md:text-xs w-full font-black uppercase" />
                    <input type="text" placeholder={t.ref_mobile} value={formData.referenceMobile} onChange={e => setFormData({...formData, referenceMobile: e.target.value})} className="bg-slate-50 dark:bg-slate-800 p-3 md:p-4 rounded-xl md:rounded-2xl border-none text-[10px] md:text-xs w-full font-black uppercase" />
                  </div>
                </Section>
              </div>
              <div className="flex justify-center pt-6 md:pt-10">
                <button type="submit" className="bg-[#0D9488] text-white px-12 py-6 md:px-32 md:py-10 rounded-[32px] md:rounded-[64px] font-black uppercase tracking-[5px] md:tracking-[15px] shadow-[0_20px_50px_rgba(13,148,136,0.3)] hover:scale-105 active:scale-95 transition-all border-b-4 md:border-b-8 border-teal-900 text-sm md:text-base">{t.commit_cloud}</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {showWhatsAppModal && waTargetCust && (
        <div className="fixed inset-0 bg-slate-900/90 backdrop-blur-2xl z-[6000] flex items-center justify-center p-6 animate-fadeIn font-black uppercase text-center">
          <div className="bg-white dark:bg-slate-800 rounded-[72px] w-full max-w-xl p-14 shadow-2xl space-y-10 border-2 border-slate-100 relative overflow-hidden">
             <div className="absolute top-0 left-0 w-full h-3 bg-[#25D366]"></div>

             <div className="flex justify-between items-center">
                <div className="flex items-center space-x-4">
                    <div className="w-12 h-12 bg-emerald-50 text-emerald-600 rounded-2xl flex items-center justify-center text-2xl shadow-lg"><i className="fab fa-whatsapp"></i></div>
                    <h3 className="text-3xl font-black uppercase tracking-tighter leading-none">WhatsApp Message</h3>
                </div>
                <button onClick={() => setShowWhatsAppModal(false)} className="w-12 h-12 bg-rose-50 text-rose-500 rounded-full flex items-center justify-center"><i className="fas fa-times"></i></button>
             </div>

             <div className="space-y-1">
                <p className="text-[10px] text-slate-400 tracking-[4px]">SENDING TO</p>
                <p className="text-2xl font-black text-emerald-600">{waTargetCust.name}</p>
                <p className="text-xs text-slate-400">{waTargetCust.mobile}</p>
             </div>

             <div className="space-y-4">
                <div className="space-y-2">
                  <p className="text-[10px] text-slate-400 tracking-[3px] text-left ml-4 uppercase">Message Templates</p>
                  <select
                      onChange={(e) => {
                          const template = store.smsTemplates?.find(t => t.title === e.target.value);
                          if (template) {
                              const currentMonth = new Date().toLocaleString('default', { month: 'long', year: 'numeric' });
                              const cleanMsg = (template.messageContent || template.message_content || "")
                                .replace(/{NAME}/g, waTargetCust.name || '')
                                .replace(/{CUSTOMER_CODE}/g, waTargetCust.customerCode || waTargetCust.customer_code || '')
                                .replace(/{ZONE}/g, waTargetCust.zone || '')
                                .replace(/{BILL_MONTH}/g, currentMonth)
                                .replace(/{AMOUNT}/g, Math.floor(waTargetCust.currentDue || waTargetCust.current_due || 0))
                                .replace(/{DUE}/g, Math.floor(waTargetCust.currentDue || waTargetCust.current_due || 0))
                                .replace(/{TOTAL_DUE}/g, Math.floor(waTargetCust.currentDue || waTargetCust.current_due || 0));
                              setWaMessage(cleanMsg);
                          }
                      }}
                      className="w-full bg-slate-50 dark:bg-slate-900 p-4 rounded-2xl border-none font-black text-xs shadow-inner outline-none cursor-pointer text-emerald-600"
                  >
                      <option value="">-- Select a Template --</option>
                      {store.smsTemplates?.filter(t => ["Create Customer", "Expired Customer", "Expiry Reminder (Tomorrow)", "Expiry Reminder (Today)", "Complain to Customer", "Area Wise Customer List", "All Customer"].includes(t.title)).map(t => (
                          <option key={t.id} value={t.title}>{t.title}</option>
                      ))}
                  </select>
                </div>

                <textarea
                  value={waMessage}
                  onChange={(e) => setWaMessage(e.target.value)}
                  placeholder="Type your WhatsApp message here..."
                  className="w-full h-48 bg-slate-50 dark:bg-slate-900 p-8 rounded-[48px] border-none font-black text-lg shadow-inner outline-none"
                />
             </div>

             <button
                onClick={() => {
                    let cleanMobile = (waTargetCust.mobile || "").replace(/[^0-9]/g, "");
                    if (cleanMobile.startsWith('0')) cleanMobile = '88' + cleanMobile;
                    else if (cleanMobile.length === 10) cleanMobile = '880' + cleanMobile;
                    else if (!cleanMobile.startsWith('88')) cleanMobile = '88' + cleanMobile;

                    const waUrl = `https://wa.me/${cleanMobile}?text=${encodeURIComponent(waMessage)}`;
                    window.open(waUrl, '_blank');
                    setShowWhatsAppModal(false);
                }}
                className="w-full bg-[#25D366] text-white py-8 rounded-[40px] font-black uppercase tracking-[10px] shadow-2xl hover:scale-105 active:scale-95 transition-all border-b-8 border-[#1DA851] flex items-center justify-center space-x-4"
             >
                <i className="fab fa-whatsapp text-2xl"></i>
                <span>OPEN WHATSAPP CHAT</span>
             </button>
          </div>
        </div>
      )}

    </div>
  );
};

// HELPERS & SUB-COMPONENTS
const ActionModals = ({
  showColumnSelector, setShowColumnSelector, visibleColumns, setVisibleColumns,
  showSmsModal, setShowSmsModal, smsMessage, setSmsMessage, selectedIds, sendSms,
  isSendingSms, smsProgress,
  showImportModal, setShowImportModal, importStep, setImportStep, handleFileUpload,
  dbFields, csvHeaders, mapping, setMapping, importStatus, startBulkImport, t,
  showFilterDrawer, setShowFilterDrawer, filters, setFilters, store,
  showZoneChangeModal, setShowZoneChangeModal, custToChangeZone, newZoneData, setNewZoneData, handleQuickZoneUpdate,
  showDateChangeModal, setShowDateChangeModal, custToChangeDate, newDates, setNewDates, handleQuickDateUpdate,
  showWhatsAppModal, setShowWhatsAppModal, waTargetCust, waMessage, setWaMessage,
  showPromiseModal, setShowPromiseModal, custForPromise, promiseData, setPromiseData, handleQuickPromiseUpdate, isSavingPromise
}) => (
    <>
      {showPromiseModal && (
        <div className="fixed inset-0 bg-slate-900/95 backdrop-blur-2xl z-[5000] flex items-center justify-center p-6 animate-fadeIn font-black uppercase">
          <div className="bg-white dark:bg-slate-800 rounded-[56px] w-full max-w-xl p-12 shadow-2xl border-4 border-indigo-500/20 space-y-8 relative overflow-hidden">
             <div className="absolute top-0 left-0 w-full h-3 bg-indigo-600"></div>
             <div className="flex justify-between items-center border-b pb-6">
                <div>
                   <h3 className="text-3xl font-black uppercase tracking-tighter leading-none">Bill Promise</h3>
                   <p className="text-[10px] text-slate-400 font-bold mt-2 tracking-[3px]">Subscriber: {custForPromise?.name}</p>
                </div>
                <button onClick={() => setShowPromiseModal(false)} className="text-rose-500 text-2xl hover:scale-110 transition-all"><i className="fas fa-times-circle"></i></button>
             </div>

             <div className="space-y-6">
                <div className="space-y-2">
                  <label className="text-[10px] text-slate-400 ml-4 tracking-[3px]">Promise Date</label>
                  <input
                    type="date"
                    value={promiseData.date}
                    onChange={e => setPromiseData({...promiseData, date: e.target.value})}
                    className="w-full bg-slate-50 dark:bg-slate-900 p-5 rounded-3xl font-black text-sm outline-none border-none cursor-pointer shadow-inner"
                  />
                </div>

                <div className="space-y-2">
                  <label className="text-[10px] text-slate-400 ml-4 tracking-[3px]">Note / Remarks</label>
                  <input
                    type="text"
                    placeholder="e.g. 500 TK dibe"
                    value={promiseData.note}
                    onChange={e => setPromiseData({...promiseData, note: e.target.value})}
                    className="w-full bg-slate-50 dark:bg-slate-900 p-5 rounded-3xl font-black text-sm outline-none border-none shadow-inner"
                  />
                </div>
             </div>

             <button
                onClick={handleQuickPromiseUpdate}
                disabled={isSavingPromise}
                className="w-full bg-indigo-600 text-white py-6 rounded-3xl font-black uppercase tracking-[5px] shadow-2xl hover:scale-[1.02] active:scale-95 transition-all"
             >
                {isSavingPromise ? 'SAVING...' : 'SAVE BILL PROMISE'}
             </button>
          </div>
        </div>
      )}

      {showColumnSelector && (
        <div className="fixed inset-0 bg-slate-900/95 backdrop-blur-2xl z-[600] flex items-center justify-center p-6 animate-fadeIn font-black uppercase">
          <div className="bg-white dark:bg-slate-800 rounded-[72px] w-full max-w-xl p-14 shadow-2xl border-2 border-slate-100">
             <div className="flex justify-between items-center border-b pb-8"><h3 className="text-3xl font-black uppercase tracking-tighter">{t.select_columns}</h3><button onClick={() => setShowColumnSelector(false)} className="text-rose-500 text-2xl hover:scale-110 transition-all"><i className="fas fa-times-circle"></i></button></div>
             <div className="grid grid-cols-2 gap-4 pt-6">{Object.keys(visibleColumns).map(col => (<label key={col} className={`flex items-center justify-between p-5 rounded-[28px] cursor-pointer transition-all border-2 ${visibleColumns[col] ? 'bg-teal-50 border-teal-200' : 'bg-slate-50 border-transparent opacity-60'}`}><span className={`font-black uppercase tracking-widest text-[10px] ${visibleColumns[col] ? 'text-teal-700' : 'text-slate-400'}`}>{col}</span><input type="checkbox" checked={visibleColumns[col]} onChange={() => setVisibleColumns({...visibleColumns, [col]: !visibleColumns[col]})} className="w-7 h-7 rounded-xl text-teal-600 focus:ring-0 cursor-pointer" /></label>))}</div>
             <button onClick={() => setShowColumnSelector(false)} className="w-full bg-[#0D9488] text-white py-7 rounded-[32px] font-black uppercase tracking-[5px] shadow-2xl">Save Settings</button>
          </div>
        </div>
      )}

      {showSmsModal && (
        <div className="fixed inset-0 bg-slate-900/90 backdrop-blur-2xl z-[300] flex items-center justify-center p-6 animate-fadeIn font-black uppercase text-center">
          <div className="bg-white dark:bg-slate-800 rounded-[72px] w-full max-w-xl p-14 shadow-2xl space-y-10 border-2 border-slate-100 relative overflow-hidden">
             <div className="absolute top-0 left-0 w-full h-3 bg-indigo-600"></div>

             <div className="flex justify-between items-center">
                <h3 className="text-4xl font-black uppercase tracking-tighter leading-none">SMS Broadcast</h3>
                <button onClick={() => !isSendingSms && setShowSmsModal(false)} className="w-12 h-12 bg-rose-50 text-rose-500 rounded-full flex items-center justify-center"><i className="fas fa-times"></i></button>
             </div>

             {isSendingSms ? (
                <div className="py-10 space-y-6">
                   <div className="w-24 h-24 bg-indigo-50 text-indigo-600 rounded-full flex items-center justify-center mx-auto text-4xl animate-spin border-4 border-dashed border-indigo-200">
                      <i className="fas fa-paper-plane"></i>
                   </div>
                   <h4 className="text-2xl font-black">{smsProgress.current} / {smsProgress.total} COMPLETED</h4>
                   <div className="h-3 w-full bg-slate-100 rounded-full overflow-hidden">
                      <div className="h-full bg-indigo-600 transition-all duration-300" style={{ width: `${(smsProgress.current / smsProgress.total) * 100}%` }}></div>
                   </div>
                </div>
             ) : (
                <>
                   <div className="space-y-1">
                      <p className="text-[10px] text-slate-400 tracking-[4px]">TARGET RECIPIENTS</p>
                      <p className="text-2xl font-black text-indigo-600">{selectedIds.length || store.customers.length} SUBSCRIBERS</p>
                   </div>

                   <div className="space-y-4">
                      <div className="space-y-2">
                        <p className="text-[10px] text-slate-400 tracking-[3px] text-left ml-4 uppercase">Message Templates</p>
                        <select
                            onChange={(e) => {
                                const template = store.smsTemplates?.find(t => t.title === e.target.value);
                                if (template) setSmsMessage(template.messageContent || template.message_content || "");
                            }}
                            className="w-full bg-slate-50 dark:bg-slate-900 p-4 rounded-2xl border-none font-black text-xs shadow-inner outline-none cursor-pointer text-indigo-600"
                        >
                            <option value="">-- Select a Template --</option>
                            {store.smsTemplates?.filter(t => ["Create Customer", "Expired Customer", "Expiry Reminder (Tomorrow)", "Expiry Reminder (Today)", "Complain to Customer", "Area Wise Customer List", "All Customer"].includes(t.title)).map(t => (
                                <option key={t.id} value={t.title}>{t.title}</option>
                            ))}
                        </select>
                      </div>

                      <textarea
                        value={smsMessage}
                        onChange={(e) => setSmsMessage(e.target.value)}
                        placeholder="Type your message here..."
                        className="w-full h-48 bg-slate-50 dark:bg-slate-900 p-8 rounded-[48px] border-none font-black text-lg shadow-inner outline-none"
                      />
                      <div className="bg-teal-50 text-teal-600 p-3 rounded-2xl text-[9px] font-bold tracking-widest border border-teal-100">
                         USE {"{NAME}"} TAG TO PERSONALIZE
                      </div>
                   </div>

                   <button
                      onClick={sendSms}
                      className="w-full bg-indigo-600 text-white py-8 rounded-[40px] font-black uppercase tracking-[10px] shadow-2xl hover:scale-105 active:scale-95 transition-all border-b-8 border-indigo-900"
                   >
                      LAUNCH BROADCAST
                   </button>
                </>
             )}
          </div>
        </div>
      )}

      {showFilterDrawer && (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-[1000] flex justify-end animate-fadeIn font-black uppercase">
          <div className="bg-white dark:bg-slate-800 w-full max-w-md h-full shadow-[-20px_0_50px_rgba(0,0,0,0.1)] p-10 space-y-10 animate-slideInRight relative overflow-y-auto">
             <div className="flex justify-between items-center border-b pb-6">
                <div className="flex items-center space-x-4">
                   <div className="w-12 h-12 bg-teal-500 text-white rounded-2xl flex items-center justify-center shadow-lg"><i className="fas fa-filter text-xl"></i></div>
                   <h3 className="text-2xl font-black uppercase tracking-tighter">Advanced Filters</h3>
                </div>
                <button onClick={() => setShowFilterDrawer(false)} className="text-slate-300 hover:text-rose-500 transition-colors"><i className="fas fa-times-circle text-3xl"></i></button>
             </div>

             <div className="space-y-8">
                <FilterSelect
                   label="Assigned Collector"
                   value={filters.collector}
                   options={['All', 'Admin', ...store.staff?.map(s => s.name)]}
                   onChange={v => setFilters({...filters, collector: v})}
                />

                {/* Zone Filter with Search */}
                <div className="space-y-3 relative">
                  <label className="text-[10px] text-slate-400 ml-2 tracking-[3px] font-black uppercase">Zone / Area</label>
                  <div className="relative group">
                    <input
                      type="text"
                      placeholder="Search or Select Zone..."
                      value={filters.zone === 'All' ? '' : filters.zone}
                      onChange={e => setFilters({...filters, zone: e.target.value || 'All'})}
                      className="w-full bg-slate-50 dark:bg-slate-900 p-5 rounded-2xl border-none font-black text-xs outline-none focus:ring-2 focus:ring-teal-500/20 uppercase shadow-inner"
                    />
                    <i className="fas fa-search absolute right-5 top-4 text-slate-300"></i>

                    {/* Suggestions for Zone Filter */}
                    {store.zones?.filter(z => z.name.toLowerCase().includes(filters.zone?.toLowerCase())).length > 0 && filters.zone !== 'All' && filters.zone !== '' && !store.zones.some(z => z.name === filters.zone) && (
                      <div className="absolute left-0 right-0 top-full mt-2 bg-white dark:bg-slate-800 rounded-3xl shadow-2xl border border-slate-100 dark:border-slate-700 z-[6000] overflow-hidden max-h-40 overflow-y-auto">
                        <div onClick={() => setFilters({...filters, zone: 'All'})} className="p-4 hover:bg-teal-50 dark:hover:bg-teal-900/20 cursor-pointer border-b font-black text-[10px] text-slate-400">RESET TO ALL</div>
                        {store.zones.filter(z => z.name.toLowerCase().includes(filters.zone.toLowerCase())).map(z => (
                          <div
                            key={z.id}
                            onClick={() => setFilters({...filters, zone: z.name})}
                            className="p-4 hover:bg-teal-50 dark:hover:bg-teal-900/20 cursor-pointer border-b last:border-0 font-black text-[10px]"
                          >
                            {z.name}
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </div>

                <FilterSelect
                   label="Account Status"
                   value={filters.status}
                   options={['All', 'Active', 'Inactive', 'Expired']}
                   onChange={v => setFilters({...filters, status: v})}
                />

                <div className="space-y-3">
                  <label className="text-[10px] text-slate-400 ml-2 tracking-[3px] font-black uppercase">Show Expiries On Specific Date</label>
                  <input
                    type="date"
                    value={filters.expiryUpTo}
                    onChange={e => setFilters({...filters, expiryUpTo: e.target.value})}
                    className="w-full bg-slate-50 dark:bg-slate-900 p-5 rounded-2xl border-none font-black text-xs outline-none focus:ring-2 focus:ring-teal-500/20 uppercase shadow-inner cursor-pointer"
                  />
                  <p className="text-[8px] text-slate-400 ml-2 mt-1 uppercase italic">* শুধুমাত্র এই নির্দিষ্ট তারিখের এক্সপায়ার হওয়া গ্রাহক দেখাবে</p>
                </div>

                <FilterSelect
                   label="Package Plan"
                   value={filters.plan}
                   options={['All', ...store.packages?.map(p => p.name)]}
                   onChange={v => setFilters({...filters, plan: v})}
                />

                <div className="pt-4 space-y-4">
                  <label className="flex items-center justify-between p-5 rounded-[28px] cursor-pointer transition-all border-2 bg-rose-50 border-rose-100 dark:bg-rose-900/10">
                    <div className="space-y-1">
                      <span className="font-black uppercase tracking-widest text-[11px] text-rose-700 dark:text-rose-400">Hide Paid Customers</span>
                      <p className="text-[9px] font-bold text-rose-400 uppercase leading-none">Show only subscribers with DUE</p>
                    </div>
                    <input
                      type="checkbox"
                      checked={filters.hideZeroDue}
                      onChange={() => setFilters({...filters, hideZeroDue: !filters.hideZeroDue})}
                      className="w-7 h-7 rounded-xl text-rose-600 focus:ring-0 cursor-pointer"
                    />
                  </label>

                  <label className="flex items-center justify-between p-5 rounded-[28px] cursor-pointer transition-all border-2 bg-slate-50 border-slate-100 dark:bg-slate-900/10">
                    <div className="space-y-1">
                      <span className="font-black uppercase tracking-widest text-[11px] text-slate-700 dark:text-slate-400">Hide Inactive Customers</span>
                      <p className="text-[9px] font-bold text-slate-400 uppercase leading-none">Show only Active subscribers</p>
                    </div>
                    <input
                      type="checkbox"
                      checked={filters.hideInactive}
                      onChange={() => setFilters({...filters, hideInactive: !filters.hideInactive})}
                      className="w-7 h-7 rounded-xl text-slate-600 focus:ring-0 cursor-pointer"
                    />
                  </label>
                </div>
             </div>

             <div className="pt-10 space-y-4">
                <button
                   onClick={() => setFilters({collector: 'All', zone: 'All', status: 'All', plan: 'All', expiryUpTo: '', hideZeroDue: false, hideInactive: false})}
                   className="w-full py-5 rounded-2xl border-2 border-slate-100 text-slate-400 font-black text-xs tracking-[4px] hover:bg-slate-50 transition-all"
                >RESET FILTERS</button>
                <button
                   onClick={() => setShowFilterDrawer(false)}
                   className="w-full bg-[#0D9488] text-white py-6 rounded-2xl font-black text-xs tracking-[5px] shadow-2xl shadow-teal-500/20 hover:scale-[1.02] active:scale-95 transition-all"
                >APPLY FILTERS</button>
             </div>
          </div>
        </div>
      )}

      {showImportModal && (
        <div className="fixed inset-0 bg-slate-900/95 backdrop-blur-2xl z-[800] flex items-center justify-center p-6 animate-fadeIn font-black uppercase">
          <div className="bg-white dark:bg-slate-800 rounded-[72px] w-full max-w-4xl p-14 shadow-2xl border-2 border-slate-100 relative overflow-hidden">
             <div className="absolute top-0 left-0 w-full h-4 bg-rose-600 shadow-lg"></div>
             <div className="flex justify-between items-center border-b pb-8">
                <h3 className="text-4xl font-black uppercase tracking-tighter">Subscriber Bulk Import Engine</h3>
                <button onClick={() => { setShowImportModal(false); setImportStep(1); }} className="text-rose-500 text-3xl hover:scale-110 transition-all"><i className="fas fa-times-circle"></i></button>
             </div>

             {importStep === 1 ? (
                <div className="py-12 space-y-8 text-center">
                   <div className="p-20 bg-slate-50 dark:bg-slate-900 rounded-[56px] border-4 border-dashed border-slate-200 dark:border-slate-700 group hover:border-teal-500 transition-all">
                      <i className="fas fa-cloud-upload-alt text-[100px] text-slate-300 group-hover:text-teal-500 transition-colors mb-8"></i>
                      <p className="text-sm font-black text-slate-500 mb-10 tracking-[5px]">DROP YOUR CSV FILE HERE</p>
                      <input type="file" accept=".csv" onChange={handleFileUpload} className="hidden" id="csv-upload-main" />
                      <label htmlFor="csv-upload-main" className="bg-slate-900 text-white px-16 py-6 rounded-[32px] font-black cursor-pointer hover:bg-teal-600 transition-all shadow-2xl tracking-[5px]">Select Source File</label>
                   </div>
                   <p className="text-[10px] text-slate-400 font-bold tracking-[3px]">SUPPORTED FORMATS: .CSV (EXCEL COMPATIBLE)</p>
                </div>
             ) : (
                <div className="py-10 space-y-10">
                   <div className="grid grid-cols-2 gap-8 max-h-[450px] overflow-y-auto pr-6 custom-scrollbar">
                      {dbFields.map(field => (
                        <div key={field.key} className="bg-slate-50 dark:bg-slate-900 p-6 rounded-[32px] flex items-center justify-between border-2 border-slate-100 dark:border-slate-800 transition-all hover:border-teal-500/30">
                           <div className="space-y-1">
                              <p className="text-[10px] text-slate-400 font-black tracking-widest">{field.label}</p>
                              <p className="text-sm font-black text-slate-800 dark:text-white uppercase leading-none">{field.key}</p>
                           </div>
                           <select
                             value={mapping[field.key] || ''}
                             onChange={(e) => setMapping({...mapping, [field.key]: e.target.value})}
                             className="bg-white dark:bg-slate-800 border-none rounded-2xl p-3 font-black text-[11px] shadow-sm text-teal-600 outline-none w-48 cursor-pointer"
                           >
                              <option value="">-- No Match --</option>
                              {csvHeaders.map(h => <option key={h} value={h}>{h}</option>)}
                           </select>
                        </div>
                      ))}
                   </div>
                   <div className="flex flex-col items-center space-y-6 pt-6 border-t">
                      {importStatus && <p className="text-rose-600 font-black text-xl animate-pulse tracking-widest">{importStatus}</p>}
                      <div className="flex space-x-4 w-full">
                         <button onClick={() => setImportStep(1)} className="flex-1 bg-slate-100 text-slate-500 py-7 rounded-[32px] font-black tracking-[5px]">GO BACK</button>
                         <button onClick={startBulkImport} className="flex-[2] bg-rose-600 text-white py-7 rounded-[32px] font-black tracking-[8px] shadow-2xl hover:brightness-110 active:scale-95 transition-all">LAUNCH IMPORT PROCESS</button>
                      </div>
                   </div>
                </div>
             )}
          </div>
        </div>
      )}

      {showDateChangeModal && (
        <div className="fixed inset-0 bg-slate-900/95 backdrop-blur-2xl z-[5000] flex items-center justify-center p-6 animate-fadeIn font-black uppercase">
          <div className="bg-white dark:bg-slate-800 rounded-[56px] w-full max-w-xl p-12 shadow-2xl border-4 border-amber-500/20 space-y-8 relative overflow-hidden">
             <div className="absolute top-0 left-0 w-full h-3 bg-amber-500"></div>
             <div className="flex justify-between items-center border-b pb-6">
                <div>
                   <h3 className="text-3xl font-black uppercase tracking-tighter leading-none">Update Dates</h3>
                   <p className="text-[10px] text-slate-400 font-bold mt-2 tracking-[3px]">Subscriber: {custToChangeDate?.name}</p>
                </div>
                <button onClick={() => setShowDateChangeModal(false)} className="text-rose-500 text-2xl hover:scale-110 transition-all"><i className="fas fa-times-circle"></i></button>
             </div>

             <div className="space-y-6">
                <div className="space-y-2">
                  <label className="text-[10px] text-slate-400 ml-4 tracking-[3px]">Expire Date</label>
                  <input
                    type="date"
                    value={newDates.expireDate}
                    onChange={e => setNewDates({...newDates, expireDate: e.target.value})}
                    className="w-full bg-slate-50 dark:bg-slate-900 p-5 rounded-3xl font-black text-sm outline-none border-none cursor-pointer shadow-inner"
                  />
                </div>

                <div className="space-y-2">
                  <label className="text-[10px] text-slate-400 ml-4 tracking-[3px]">Request Date</label>
                  <input
                    type="date"
                    value={newDates.requestDate}
                    onChange={e => setNewDates({...newDates, requestDate: e.target.value})}
                    className="w-full bg-slate-50 dark:bg-slate-900 p-5 rounded-3xl font-black text-sm outline-none border-none cursor-pointer shadow-inner"
                  />
                </div>
             </div>

             <button
                onClick={handleQuickDateUpdate}
                className="w-full bg-amber-600 text-white py-6 rounded-3xl font-black uppercase tracking-[5px] shadow-2xl hover:scale-[1.02] active:scale-95 transition-all"
             >
                SAVE DATE UPDATES
             </button>
          </div>
        </div>
      )}

      {showZoneChangeModal && (
        <div className="fixed inset-0 bg-slate-900/95 backdrop-blur-2xl z-[5000] flex items-center justify-center p-6 animate-fadeIn font-black uppercase">
          <div className="bg-white dark:bg-slate-800 rounded-[56px] w-full max-w-xl p-12 shadow-2xl border-4 border-teal-500/20 space-y-8 relative overflow-hidden">
             <div className="absolute top-0 left-0 w-full h-3 bg-teal-500"></div>
             <div className="flex justify-between items-center border-b pb-6">
                <div>
                   <h3 className="text-3xl font-black uppercase tracking-tighter leading-none">Change Zone</h3>
                   <p className="text-[10px] text-slate-400 font-bold mt-2 tracking-[3px]">Subscriber: {custToChangeZone?.name}</p>
                </div>
                <button onClick={() => setShowZoneChangeModal(false)} className="text-rose-500 text-2xl hover:scale-110 transition-all"><i className="fas fa-times-circle"></i></button>
             </div>

             <div className="space-y-6">
                {/* Zone Search/Select with Search Box */}
                <div className="space-y-2 relative">
                  <label className="text-[10px] text-slate-400 ml-4 tracking-[3px]">Select Zone</label>
                  <div className="relative group">
                    <input
                      type="text"
                      placeholder="Search or Select Zone..."
                      value={newZoneData.zone}
                      onChange={e => setNewZoneData({...newZoneData, zone: e.target.value, subZone: '', boxId: ''})}
                      className="w-full bg-slate-50 dark:bg-slate-900 p-5 rounded-3xl font-black text-sm outline-none border-2 border-transparent focus:border-teal-500 transition-all uppercase shadow-inner"
                    />
                    <i className="fas fa-search absolute right-6 top-5 text-slate-300"></i>

                    {/* Suggestions for Change Zone */}
                    {store.zones?.filter(z => z.name.toLowerCase().includes(newZoneData.zone?.toLowerCase())).length > 0 && newZoneData.zone !== '' && !store.zones.some(z => z.name === newZoneData.zone) && (
                      <div className="absolute left-0 right-0 top-full mt-2 bg-white dark:bg-slate-800 rounded-3xl shadow-2xl border border-slate-100 dark:border-slate-700 z-[6000] overflow-hidden max-h-60 overflow-y-auto">
                        {store.zones.filter(z => z.name.toLowerCase().includes(newZoneData.zone.toLowerCase())).map(z => (
                          <div
                            key={z.id}
                            onClick={() => setNewZoneData({...newZoneData, zone: z.name, subZone: '', boxId: ''})}
                            className="p-5 hover:bg-teal-50 dark:hover:bg-teal-900/20 cursor-pointer border-b last:border-0 font-black text-sm"
                          >
                            {z.name}
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </div>

                {/* Sub-Zone Select */}
                <div className="space-y-2">
                  <label className="text-[10px] text-slate-400 ml-4 tracking-[3px]">Select Sub-Zone</label>
                  <select
                    value={newZoneData.subZone}
                    onChange={e => setNewZoneData({...newZoneData, subZone: e.target.value, boxId: ''})}
                    className="w-full bg-slate-50 dark:bg-slate-900 p-5 rounded-3xl font-black text-sm outline-none border-none cursor-pointer"
                  >
                    <option value="">-- Select Sub-Zone --</option>
                    {store.subZones?.filter(sz => !newZoneData.zone || store.zones?.find(z => z.name === newZoneData.zone)?.id === sz.zoneId).map(sz => (
                      <option key={sz.id} value={sz.name}>{sz.name}</option>
                    ))}
                  </select>
                </div>

                {/* Box Select */}
                <div className="space-y-2">
                  <label className="text-[10px] text-slate-400 ml-4 tracking-[3px]">Select Box</label>
                  <select
                    value={newZoneData.boxId}
                    onChange={e => setNewZoneData({...newZoneData, boxId: e.target.value})}
                    className="w-full bg-slate-50 dark:bg-slate-900 p-5 rounded-3xl font-black text-sm outline-none border-none cursor-pointer"
                  >
                    <option value="">-- Select Box --</option>
                    {store.boxes?.filter(b => !newZoneData.subZone || store.subZones?.find(sz => sz.name === newZoneData.subZone)?.id === b.subZoneId).map(b => (
                      <option key={b.id} value={b.name}>{b.name}</option>
                    ))}
                  </select>
                </div>
             </div>

             <button
                onClick={handleQuickZoneUpdate}
                className="w-full bg-[#0D9488] text-white py-6 rounded-3xl font-black uppercase tracking-[5px] shadow-2xl hover:scale-[1.02] active:scale-95 transition-all"
             >
                UPDATE LOCATION
             </button>
          </div>
        </div>
      )}
    </>
);

const Section = ({ title, color, bgColor, borderColor, shadowColor, children }) => (
  <div className={`${bgColor} p-6 md:p-10 rounded-[32px] md:rounded-[56px] space-y-6 md:space-y-8 border-2 ${borderColor} shadow-2xl`}>
    <div className={`bg-${color}-600 text-white p-4 md:p-6 rounded-2xl md:rounded-3xl text-center text-[10px] md:text-xs font-black uppercase tracking-[2px] md:tracking-[5px] shadow-xl ${shadowColor}`}>{title}</div>
    <div className="space-y-6 md:space-y-8">{children}</div>
  </div>
);

const ActionItem = ({ icon, label, color, onClick }) => (
  <div onClick={onClick} className={`flex items-center space-x-4 p-4 hover:bg-slate-50 dark:hover:bg-slate-700 cursor-pointer transition-all border-b border-slate-50 dark:border-slate-700 last:border-0 group`}>
    <i className={`fas ${icon} w-8 text-center text-lg ${color} group-hover:scale-110 transition-transform`}></i>
    <span className={`text-sm font-black uppercase tracking-widest ${color}`}>{label}</span>
  </div>
);

const ActionButtonLarge = ({ label, icon, onClick }) => (
  <button onClick={onClick} className="bg-[#20879e] text-white px-4 py-2 rounded-xl font-black text-[9px] flex items-center space-x-2 shadow-lg hover:scale-105 active:scale-95 transition-all uppercase tracking-widest border-b-2 border-[#16667a] leading-none">
    <i className={`fas ${icon} text-base`}></i>
    <span>{label}</span>
  </button>
);

const ActionButtonSmall = ({ label, icon, onClick }) => (
  <button onClick={onClick} className="bg-[#20879e] text-white px-3 py-2 rounded-xl font-black text-[8px] md:text-[9px] flex items-center space-x-1.5 md:space-x-2 shadow-lg hover:scale-105 active:scale-95 transition-all uppercase tracking-widest border-b-2 border-[#16667a] leading-none h-11 md:h-auto">
    <i className={`fas ${icon} text-sm md:text-base`}></i>
    <span>{label}</span>
  </button>
);

const StatCard = ({ label, value, color }) => {
    const colors = {
        slate: "bg-slate-100 dark:bg-slate-900 text-slate-600 border-slate-200 dark:border-slate-800 shadow-slate-200/50",
        emerald: "bg-emerald-50 dark:bg-emerald-900/20 text-emerald-600 border-emerald-100 dark:border-emerald-800 shadow-emerald-500/10",
        rose: "bg-rose-50 dark:bg-rose-900/20 text-rose-500 border-rose-100 dark:border-rose-800 shadow-rose-500/10",
        indigo: "bg-indigo-50 dark:bg-indigo-900/20 text-indigo-600 border-indigo-100 dark:border-indigo-800 shadow-indigo-500/10"
    };
    return (
        <div className={`${colors[color]} px-6 py-3 rounded-2xl border flex flex-col items-center justify-center min-w-[120px] shadow-lg`}>
            <p className="text-[9px] font-black uppercase tracking-[3px] opacity-60 mb-1">{label}</p>
            <p className="text-2xl font-black tracking-tighter leading-none">{value}</p>
        </div>
    );
};

const Field = ({ label, value, onChange, placeholder, type = 'text', options = [], disabled = false, color }) => (
  <div className="space-y-2 md:space-y-3 uppercase font-black">
    <label className="text-[10px] md:text-[12px] text-slate-600 dark:text-slate-300 ml-2 tracking-widest uppercase leading-none">{label}</label>
    {type === 'select' ? (
      <select disabled={disabled} value={value} onChange={e => onChange(e.target.value)} className={`w-full bg-white dark:bg-slate-800 p-4 md:p-6 rounded-xl md:rounded-[24px] border-2 border-transparent focus:border-teal-500 text-xs md:text-sm font-black shadow-lg outline-none transition-all cursor-pointer`}>
        {options.map(opt => {
            const label = typeof opt === 'object' ? opt.label : opt;
            const val = typeof opt === 'object' ? opt.value : opt;
            return <option key={val} value={val}>{label}</option>
        })}
      </select>
    ) : type === 'radio' ? (
      <div className="flex flex-col space-y-3 md:space-y-4 bg-white dark:bg-slate-800 p-4 md:p-6 rounded-xl md:rounded-[24px] shadow-lg border-2 border-transparent">
         {options.map(opt => (
           <label key={opt} className="flex items-center space-x-3 md:space-x-4 cursor-pointer group">
              <div className={`w-5 h-5 md:w-7 md:h-7 rounded-full border-2 md:border-4 flex items-center justify-center transition-all ${value === opt ? 'border-teal-500 bg-teal-50' : 'border-slate-100 dark:border-slate-700'}`}>
                {value === opt && <div className="w-2.5 h-2.5 md:w-3.5 md:h-3.5 rounded-full bg-teal-500 shadow-lg"></div>}
              </div>
              <input type="radio" className="hidden" checked={value === opt} onChange={() => onChange(opt)} />
              <span className={`text-[10px] md:text-[12px] font-black ${value === opt ? 'text-teal-600' : 'text-slate-400'}`}>{opt}</span>
           </label>
         ))}
      </div>
    ) : (
      <input type={type} disabled={disabled} value={value} onChange={e => onChange(e.target.value)} placeholder={placeholder} className={`w-full bg-white dark:bg-slate-800 p-4 md:p-6 rounded-xl md:rounded-[24px] border-2 border-transparent focus:border-teal-500 text-base md:text-lg font-black shadow-lg outline-none transition-all ${disabled ? 'opacity-50' : ''}`} />
    )}
  </div>
);

const FilterSelect = ({ label, value, options, onChange }) => (
  <div className="space-y-3">
    <label className="text-[10px] text-slate-400 ml-2 tracking-[3px] font-black">{label}</label>
    <select
      value={value}
      onChange={e => onChange(e.target.value)}
      className="w-full bg-slate-50 dark:bg-slate-900 p-5 rounded-2xl border-none font-black text-xs outline-none focus:ring-2 focus:ring-teal-500/20 cursor-pointer"
    >
      {options.map(opt => <option key={opt} value={opt}>{opt}</option>)}
    </select>
  </div>
);

export default Customers;
