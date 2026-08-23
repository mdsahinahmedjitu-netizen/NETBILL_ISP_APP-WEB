# Supabase SMS Proxy Setup

This folder contains the **Edge Function** used to proxy SMS requests to **BulkSMSDhaka**. 
By using this proxy, your SMS requests will originate from Supabase's stable IP address instead of your local PC's dynamic IP, resolving "IP Not Whitelisted" errors.

## How to Deploy (using npm/npx)

If `npm run supabase-login` fails with a session error, use the **Access Token** method:

1. **Generate an Access Token**:
   - Go to: [https://supabase.com/dashboard/account/tokens](https://supabase.com/dashboard/account/tokens)
   - Click **Generate new token**. Give it a name like "NetBill-CLI".
   - Copy the generated token immediately.

2. **Deploy using the Token**:
   - Run the following command in your terminal (inside the project root):
   ```bash
   npx supabase functions deploy sms-proxy --project-ref tglplinxvrqsrxeicvpr --access-token YOUR_COPIED_TOKEN
   ```
   *(Replace `YOUR_COPIED_TOKEN` with the token you just copied)*

## Configuration in Software

After deployment, update your **SMS API URL** in the software settings to:
`https://tglplinxvrqsrxeicvpr.supabase.co/functions/v1/sms-proxy?apikey={API_KEY}&callerID={SENDER_ID}&number={MOBILE}&message={MESSAGE}`

## Whitelisting the IP

1. Once deployed, trigger a test SMS.
2. If it fails, check your **Supabase Function Logs** or wait for the error response.
3. It will mention an IP address. Add that IP to the **BulkSMSDhaka White List**.
