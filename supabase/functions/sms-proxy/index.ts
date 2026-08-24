// Supabase Edge Function: sms-proxy
// Updated to handle both SMS Sending and Balance Checks

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    const url = new URL(req.url);
    const action = url.searchParams.get('action') || 'send';
    const apikey = url.searchParams.get('apikey');

    if (!apikey) {
      return new Response(JSON.stringify({ error: 'apikey is required' }), {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        status: 400,
      })
    }

    if (action === 'balance') {
      const targetUrl = `https://bulksmsbd.net/api/getBalanceApi?api_key=${apikey}`;
      const response = await fetch(targetUrl);
      const result = await response.text();
      return new Response(result, {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        status: 200,
      })
    }

    // Default: Send SMS
    const callerID = url.searchParams.get('callerID');
    const number = url.searchParams.get('number');
    const message = url.searchParams.get('message');
    const type = url.searchParams.get('type') || 'text';

    if (!number || !message) {
      return new Response(JSON.stringify({ error: 'number and message are required' }), {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        status: 400,
      })
    }

    const encodedMessage = encodeURIComponent(message);
    const targetUrl = `https://bulksmsbd.net/api/smsapi?api_key=${apikey}&type=${type}&number=${number}&senderid=${callerID || ''}&message=${encodedMessage}`;

    const response = await fetch(targetUrl);
    const result = await response.text();

    return new Response(result, {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      status: 200,
    })

  } catch (error) {
    return new Response(JSON.stringify({ error: error.message }), {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      status: 500,
    })
  }
})
