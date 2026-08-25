// Supabase Edge Function: mikrotik-manager
// Final Enterprise Version - Monitoring, Control & SMS Integrated Auto-Expiry

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

serve(async (req) => {
  if (req.method === 'OPTIONS') return new Response('ok', { headers: corsHeaders })

  try {
    const { action, payload, routerId } = await req.json();
    const supabaseAdmin = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
    )

    const getRouter = async (id?: string) => {
        if (id && id !== 'Select Router') {
            const { data } = await supabaseAdmin.from('mikrotik_routers').select('*').eq('id', id).maybeSingle();
            return data;
        }
        const { data } = await supabaseAdmin.from('mikrotik_routers').select('*').limit(1).maybeSingle();
        return data;
    }

    // ACTION: AUTO SUSPEND (Triggered by Cron Job)
    if (action === 'auto_suspend') {
        const todayStr = new Date().toLocaleDateString('en-CA');
        const { data: expired } = await supabaseAdmin.from('customers').select('*').eq('status', 'Active').lt('expire_date', todayStr);

        if (expired && expired.length > 0) {
            const { data: settings } = await supabaseAdmin.from('settings').select('*').limit(1).maybeSingle();
            const { data: templates } = await supabaseAdmin.from('sms_templates').select('*');
            const template = templates?.find(t => t.title === 'Expired Customer' && t.is_active);

            for (const cust of expired) {
                const router = await getRouter(cust.router_id);
                if (router) {
                    const auth = btoa(`${router.api_user}:${router.api_pass}`);
                    const baseUrl = `http://${router.host}:${router.port}/rest`;
                    const headers = { 'Authorization': `Basic ${auth}`, 'Content-Type': 'application/json' };
                    const find = await fetch(`${baseUrl}/ppp/secret?name=${encodeURIComponent(cust.pppoe_username)}`, { headers }).then(r => r.json());
                    if (Array.isArray(find) && find[0]) {
                        await fetch(`${baseUrl}/ppp/secret/set`, { method: 'POST', headers, body: JSON.stringify({ ".id": find[0]['.id'], "disabled": "yes" }) });
                        const act = await fetch(`${baseUrl}/ppp/active?name=${encodeURIComponent(cust.pppoe_username)}`, { headers }).then(r => r.json());
                        if (Array.isArray(act) && act[0]) await fetch(`${baseUrl}/ppp/active/${act[0]['.id']}`, { method: 'DELETE', headers });
                    }
                }
                await supabaseAdmin.from('customers').update({ status: 'Suspended' }).eq('id', cust.id);

                // --- SEND EXPIRED SMS ---
                if (settings?.sms_api_key && cust.mobile && template) {
                    let msg = template.message_content
                        .replace(/{NAME}/g, cust.name || '')
                        .replace(/{CUSTOMER_CODE}/g, cust.customer_code || '')
                        .replace(/{AMOUNT}/g, Math.floor(cust.current_due || 0))
                        .replace(/{DUE}/g, Math.floor(cust.current_due || 0))
                        .replace(/{DATE}/g, cust.expire_date || '');

                    let cleanMobile = cust.mobile.replace(/[^0-9]/g, "");
                    if (cleanMobile.startsWith('0')) cleanMobile = '88' + cleanMobile;
                    else if (cleanMobile.length === 10) cleanMobile = '880' + cleanMobile;
                    else if (!cleanMobile.startsWith('88')) cleanMobile = '88' + cleanMobile;

                    const msgType = /[\u0980-\u09FF]/.test(msg) ? "unicode" : "text";
                    const finalUrl = `https://tglplinxvrqsrxeicvpr.supabase.co/functions/v1/sms-proxy?apikey=${settings.sms_api_key}&callerID=${settings.sms_sender_id}&number=${cleanMobile}&message=${encodeURIComponent(msg)}&type=${msgType}`;

                    await fetch(finalUrl); // Trigger SMS via Proxy

                    await supabaseAdmin.from('sms_logs').insert({
                        customer_id: cust.id, customer_name: cust.name, mobile: cleanMobile,
                        notification_type: 'Expired (Auto-Server)', message: msg, status: 'Sent', sent_timestamp: new Date().toISOString()
                    });
                }
            }
        }
        return new Response(JSON.stringify({ success: true }), { headers: corsHeaders });
    }

    // ... (Keep existing get_status and set_status logic)
    const router = await getRouter(routerId || payload?.routerId || payload?.router_id);
    if (!router) throw new Error("Router config not found.");
    const auth = btoa(`${router.api_user}:${router.api_pass}`);
    const baseUrl = `http://${router.host}:${router.port}/rest`;
    const headers = { 'Authorization': `Basic ${auth}`, 'Content-Type': 'application/json' };

    if (action === 'get_status') {
        const [res, health, sess] = await Promise.all([
            fetch(`${baseUrl}/system/resource`, { headers, signal: AbortSignal.timeout(5000) }).then(r => r.json()).catch(() => ({})),
            fetch(`${baseUrl}/system/health`, { headers, signal: AbortSignal.timeout(5000) }).then(r => r.json()).catch(() => ({})),
            fetch(`${baseUrl}/ppp/active?.proplist=name`, { headers, signal: AbortSignal.timeout(5000) }).then(r => r.json()).catch(() => [])
        ]);
        const totalMem = parseFloat(res['total-memory'] || 0);
        const freeMem = parseFloat(res['free-memory'] || 0);
        let temperature = "N/A";
        if (Array.isArray(health)) temperature = health.find((h:any)=>h.name.toLowerCase().includes('temp'))?.value || "N/A";
        else temperature = health.temperature || "N/A";

        return new Response(JSON.stringify({
            success: true,
            metrics: { cpu: res['cpu-load'] || 0, uptime: res['uptime'] || '---', ram: totalMem > 0 ? `${((totalMem-freeMem)/(1024*1024)).toFixed(0)}MB / ${(totalMem/(1024*1024*1024)).toFixed(0)}GB` : "---", temp: temperature },
            sessions: Array.isArray(sess) ? sess.map((s:any) => ({ username: s.name })) : []
        }), { headers: { ...corsHeaders, 'Content-Type': 'application/json' } });
    }

    if (action === 'set_status' || action === 'sync_customer') {
        const username = (payload.username || payload.pppoe_username || "").toString().trim();
        const active = payload.active !== undefined ? payload.active : (payload.status === 'Active');
        const find = await fetch(`${baseUrl}/ppp/secret?name=${encodeURIComponent(username)}`, { headers }).then(r => r.json());
        if (Array.isArray(find) && find[0]) {
            await fetch(`${baseUrl}/ppp/secret/set`, { method: 'POST', headers, body: JSON.stringify({ ".id": find[0]['.id'], "disabled": active ? "no" : "yes" }) });
            if (!active) {
                const act = await fetch(`${baseUrl}/ppp/active?name=${encodeURIComponent(username)}`, { headers }).then(r => r.json());
                if (Array.isArray(act) && act[0]) await fetch(`${baseUrl}/ppp/active/${act[0]['.id']}`, { method: 'DELETE', headers });
            }
            return new Response(JSON.stringify({ success: true }), { headers: corsHeaders });
        }
        throw new Error("User not found.");
    }

  } catch (error) {
    return new Response(JSON.stringify({ error: error.message, success: false }), { headers: { ...corsHeaders, 'Content-Type': 'application/json' } });
  }
})
