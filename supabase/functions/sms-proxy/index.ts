// Supabase Edge Function: sms-proxy
// This function acts as a bridge between your app and BulkSMSDhaka
// It uses Supabase's stable IP to bypass local IP whitelisting issues.

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

serve(async (req) => {
  // Handle CORS preflight requests
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    const url = new URL(req.url);
    const apikey = url.searchParams.get('apikey');
    const callerID = url.searchParams.get('callerID');
    const number = url.searchParams.get('number');
    const message = url.searchParams.get('message');

    // Basic validation
    if (!apikey || !number || !message) {
      return new Response(JSON.stringify({ error: 'Missing parameters (apikey, number, and message are required)' }), {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        status: 400,
      })
    }

    // Determine if it's a Unicode (Bangla) message
    const isUnicode = message.split('').some(char => char.charCodeAt(0) > 127);
    const typeParam = isUnicode ? "&type=unicode" : "";

    // Encode message properly for BulkSMSDhaka (%20 for spaces)
    const encodedMessage = encodeURIComponent(message).replace(/\+/g, '%20');

    // Construct the final target URL for BulkSMSDhaka
    const targetUrl = `https://bulksmsdhaka.net/api/sendtext?apikey=${apikey}&callerID=${callerID || '1234'}&number=${number}&message=${encodedMessage}${typeParam}`;

    console.log(`[SMS Proxy] Dispatching to ${number} (Unicode: ${isUnicode})`);

    // Fetch call from Supabase Server (Static IP)
    const response = await fetch(targetUrl);
    const result = await response.text();

    return new Response(result, {
      headers: { ...corsHeaders, 'Content-Type': 'text/plain' },
      status: 200,
    })

  } catch (error) {
    console.error("[SMS Proxy Error]", error.message);
    return new Response(JSON.stringify({ error: error.message }), {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      status: 500,
    })
  }
})
