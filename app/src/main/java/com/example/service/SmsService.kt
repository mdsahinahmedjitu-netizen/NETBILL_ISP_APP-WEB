package com.example.service

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmsService {
    private val client = OkHttpClient()
    private val TAG = "SmsService"

    /**
     * Sends an SMS using a generic HTTP GET API.
     * Most Bangladeshi SMS gateways use simple URL parameters.
     */
    suspend fun sendSms(
        apiUrl: String,
        apiKey: String,
        senderId: String,
        mobile: String,
        message: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (apiUrl.isBlank() || apiKey.isBlank()) {
            Log.w(TAG, "SMS sending skipped: API URL or Key is missing.")
            return@withContext false
        }

        try {
            // Clean mobile number (ensure it starts with 880)
            val cleanMobile = if (mobile.startsWith("01")) "88$mobile" else mobile
            
            // Construct common Bangladeshi Gateway URL (Generic Example)
            // Example: https://api.gateway.com/send?apikey=XYZ&senderid=8801&number=88017&message=Hello
            val url = apiUrl.replace("{API_KEY}", apiKey)
                .replace("{SENDER_ID}", senderId)
                .replace("{MOBILE}", cleanMobile)
                .replace("{MESSAGE}", java.net.URLEncoder.encode(message, "UTF-8"))

            val request = Request.Builder()
                .url(url)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.i(TAG, "SMS Sent Successfully to $cleanMobile")
                    true
                } else {
                    Log.e(TAG, "SMS Failed: HTTP ${response.code}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "SMS Error: ${e.message}")
            false
        }
    }
}
