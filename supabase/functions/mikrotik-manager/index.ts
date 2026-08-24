// Supabase Edge Function: mikrotik-manager
// Final Verified Version - All Features Restored & Traffic Capability Added

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

    const targetId = routerId || payload?.routerId || payload?.router_id;
    const { data: router } = await supabaseAdmin.from('mikrotik_routers').select('*').eq('id', targetId).maybeSingle();

    if (!router) throw new Error("Router config not found.");

    const auth = btoa(`${router.api_user}:${router.api_pass}`);
    const baseUrl = `http://${router.host}:${router.port}/rest`;
    const headers = { 'Authorization': `Basic ${auth}`, 'Content-Type': 'application/json' };

    // 1. GET STATUS (Metrics + Realtime Online List)
    if (action === 'get_status') {
        const [res, health, sess] = await Promise.all([
            fetch(`${baseUrl}/system/resource`, { headers, signal: AbortSignal.timeout(5000) }).then(r => r.json()).catch(() => ({})),
            fetch(`${baseUrl}/system/health`, { headers, signal: AbortSignal.timeout(5000) }).then(r => r.json()).catch(() => ({})),
            fetch(`${baseUrl}/ppp/active?.proplist=name`, { headers, signal: AbortSignal.timeout(5000) }).then(r => r.json()).catch(() => [])
        ]);

        const totalMem = parseFloat(res['total-memory'] || res['memory'] || 0);
        const freeMem = parseFloat(res['free-memory'] || 0);
        const usedMB = (totalMem - freeMem) / (1024 * 1024);
        const totalGB = totalMem / (1024 * 1024 * 1024);

        let temperature = "N/A";
        if (Array.isArray(health)) {
            const t = health.find((h: any) => h.name.toLowerCase().includes('temp'));
            if (t) temperature = t.value;
        } else if (health.temperature) {
            temperature = health.temperature;
        }

        return new Response(JSON.stringify({
            success: true,
            metrics: {
                cpu: res['cpu-load'] || 0,
                uptime: res['uptime'] || '---',
                ram: totalMem > 0 ? `${usedMB.toFixed(0)}MB / ${totalGB.toFixed(0)}GB` : "---",
                temp: temperature
            },
            sessions: Array.isArray(sess) ? sess.map((s:any) => ({ username: s.name })) : []
        }), { headers: { ...corsHeaders, 'Content-Type': 'application/json' } });
    }

    // 2. GET USER TRAFFIC
    if (action === 'get_user_traffic') {
        const username = (payload.username || "").toString().trim();
        const res = await fetch(`${baseUrl}/ppp/active?.proplist=rx-bits-per-second,tx-bits-per-second&name=${encodeURIComponent(username)}`, { headers });
        const data = await res.json();

        if (Array.isArray(data) && data[0]) {
            const rx = parseFloat(data[0]['rx-bits-per-second'] || 0) / (1024 * 1024);
            const tx = parseFloat(data[0]['tx-bits-per-second'] || 0) / (1024 * 1024);
            return new Response(JSON.stringify({ success: true, rx, tx }), { headers: corsHeaders });
        }
        return new Response(JSON.stringify({ success: true, rx: 0, tx: 0 }), { headers: corsHeaders });
    }

    // 3. SET STATUS (Enable/Disable/Sync)
    if (action === 'set_status' || action === 'sync_customer' || action === 'check_user') {
        const username = (payload.username || payload.pppoe_username || "").toString().trim();

        if (action === 'check_user') {
            const find = await fetch(`${baseUrl}/ppp/secret?name=${encodeURIComponent(username)}`, { headers }).then(r => r.json());
            return new Response(JSON.stringify({ success: true, exists: Array.isArray(find) && find.length > 0 }), { headers: corsHeaders });
        }

        const active = payload.active !== undefined ? payload.active : (payload.status === 'Active');
        const find = await fetch(`${baseUrl}/ppp/secret?name=${encodeURIComponent(username)}`, { headers }).then(r => r.json());

        if (Array.isArray(find) && find[0]) {
            await fetch(`${baseUrl}/ppp/secret/set`, {
                method: 'POST', headers, body: JSON.stringify({ ".id": find[0]['.id'], "disabled": active ? "no" : "yes" })
            });
            if (!active) {
                const act = await fetch(`${baseUrl}/ppp/active?name=${encodeURIComponent(username)}`, { headers }).then(r => r.json());
                if (Array.isArray(act) && act[0]) await fetch(`${baseUrl}/ppp/active/${act[0]['.id']}`, { method: 'DELETE', headers });
            }
            return new Response(JSON.stringify({ success: true }), { headers: corsHeaders });
        }
        throw new Error("User not found.");
    }

  } catch (error) {
    return new Response(JSON.stringify({ error: error.message, success: false }), { headers: corsHeaders, status: 200 });
  }
})
