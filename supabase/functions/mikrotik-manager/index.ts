// Supabase Edge Function: mikrotik-manager
// Stable version for CCR1036 - Sessions & Bandwidth Fixed

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

function parseRate(val: any): number {
    if (!val) return 0;
    const str = val.toString().toLowerCase().replace(/\s/g, '');
    const num = parseFloat(str);
    if (isNaN(num)) return 0;
    if (str.includes('gbps')) return num * 1024;
    if (str.includes('mbps')) return num;
    if (str.includes('kbps')) return num / 1024;
    return num > 100000 ? num / 1000000 : num;
}

serve(async (req) => {
  if (req.method === 'OPTIONS') return new Response('ok', { headers: corsHeaders })

  try {
    const { action, payload, routerId } = await req.json();
    const supabaseAdmin = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
    )

    const targetRouterId = routerId || payload?.routerId;
    const { data: router, error: rErr } = await supabaseAdmin.from('mikrotik_routers').select('*').eq('id', targetRouterId).single();

    if (rErr || !router) throw new Error("Router config missing.");

    const auth = btoa(`${router.api_user}:${router.api_pass}`);
    const baseUrl = `http://${router.host}:${router.port}/rest`;
    const headers = { 'Authorization': `Basic ${auth}`, 'Content-Type': 'application/json' };

    let result: any = { success: true, metrics: {}, sessions: [], rx: "0.0", tx: "0.0" };

    if (action === 'get_status') {
        // 1. Resources
        const res = await fetch(`${baseUrl}/system/resource`, { headers });
        const resData = await res.json();

        const totalMem = parseFloat(resData['total-memory'] || 0);
        const freeMem = parseFloat(resData['free-memory'] || 0);
        const usedMemMB = (totalMem - freeMem) / (1024 * 1024);
        const totalMemGB = totalMem / (1024 * 1024 * 1024);

        result.metrics = {
            cpu: resData['cpu-load'] || 0,
            uptime: resData['uptime'] || 'N/A',
            ram: `${usedMemMB.toFixed(0)} MB / ${totalMemGB.toFixed(0)} GB`
        };

        // 2. Fetch Sessions
        const sess = await fetch(`${baseUrl}/ppp/active`, { headers });
        const sessData = await sess.json();
        if (Array.isArray(sessData)) {
            result.sessions = sessData.map((s: any) => ({
                id: s['.id'] || Math.random().toString(),
                username: s.name || 'N/A',
                name: s.comment || 'N/A',
                type: s.service || 'PPPoE',
                ip: s.address || 'N/A',
                mac: s['caller-id'] || 'N/A',
                uptime: s.uptime || 'N/A',
                rx: 0, tx: 0, status: 'Connected'
            }));
        }

        // 3. Bandwidth - Monitor specific interface
        const targetIface = "-002.sfp-sfpplus2- IN";
        const mon = await fetch(`${baseUrl}/interface/monitor-traffic?interface=${encodeURIComponent(targetIface)}&once`, { headers });

        if (mon.ok) {
            const mData = await mon.json();
            if (Array.isArray(mData) && mData[0]) {
                result.rx = parseRate(mData[0]['rx-bits-per-second']).toFixed(1);
                result.tx = parseRate(mData[0]['tx-bits-per-second']).toFixed(1);
            }
        }

        // 4. Health
        const health = await fetch(`${baseUrl}/system/health`, { headers });
        if (health.ok) {
            const hData = await health.json();
            if (Array.isArray(hData)) {
                const t = hData.find((h: any) => h.name.includes('temperature'));
                result.metrics.temp = t ? t.value : 0;
            }
        }
    }

    return new Response(JSON.stringify(result), { headers: { ...corsHeaders, 'Content-Type': 'application/json' } });

  } catch (error) {
    return new Response(JSON.stringify({ error: error.message }), { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 500 });
  }
})
