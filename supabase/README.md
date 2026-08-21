# Supabase SMS Proxy Setup

This folder contains the **Edge Function** used to proxy SMS requests to **BulkSMSDhaka**. 
By using this proxy, your SMS requests will originate from Supabase's stable IP address instead of your local PC's dynamic IP, resolving "IP Not Whitelisted" errors.

## How to Deploy

1. **Install Supabase CLI** (if not already installed):
   - Windows: `scoop bucket add supabase https://github.com/supabase/scoop-bucket.git` -> `scoop install supabase`
   - Or download the binary from GitHub Releases.

2. **Login to Supabase**:
   ```bash
   supabase login
   ```

3. **Initialize in Project Root** (One time only):
   ```bash
   supabase init
   ```

4. **Link to your Project**:
   - Go to your Supabase Dashboard -> Project Settings -> API -> Project ID.
   ```bash
   supabase link --project-ref your-project-id
   ```

5. **Deploy the Function**:
   ```bash
   supabase functions deploy sms-proxy
   ```

## Configuration in Software

After deployment, update your **SMS API URL** in the software settings to:
`https://[YOUR_PROJECT_ID].supabase.co/functions/v1/sms-proxy?apikey={API_KEY}&callerID={SENDER_ID}&number={MOBILE}&message={MESSAGE}`

## Whitelisting the IP

1. Once deployed, trigger a test SMS.
2. If it fails, check your **Supabase Function Logs** or wait for the error response.
3. It will mention an IP address. Add that IP to the **BulkSMSDhaka White List**.
