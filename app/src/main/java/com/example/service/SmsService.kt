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
            val cleanMobile = when {
                mobile.startsWith("880") -> mobile
                mobile.startsWith("01") -> "88$mobile"
                else -> mobile
            }
            
            // Build the URL by replacing placeholders
            // This works for 99% of BD SMS Gateways (BulkSMSBD, GreenWeb, IT-BD, etc.)
            val finalUrl = apiUrl
                .replace("{API_KEY}", apiKey, ignoreCase = true)
                .replace("{API_TOKEN}", apiKey, ignoreCase = true)
                .replace("{SENDER_ID}", senderId, ignoreCase = true)
                .replace("{MOBILE}", cleanMobile, ignoreCase = true)
                .replace("{NUMBER}", cleanMobile, ignoreCase = true)
                .replace("{MESSAGE}", java.net.URLEncoder.encode(message, "UTF-8"), ignoreCase = true)

            val request = Request.Builder()
                .url(finalUrl)
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    Log.i(TAG, "SMS Sent to $cleanMobile. Response: $body")
                    true
                } else {
                    Log.e(TAG, "SMS Failed to $cleanMobile. Code: ${response.code}, Body: $body")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "SMS Exception: ${e.message}")
            false
        }
    }
}
