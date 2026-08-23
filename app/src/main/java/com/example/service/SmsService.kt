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
     * Returns a Triple of (SuccessStatus, ResponseOrErrorMessage, FinalUrl)
     */
    suspend fun sendSms(
        apiUrl: String,
        apiKey: String,
        senderId: String,
        mobile: String,
        message: String
    ): Triple<Boolean, String, String> = withContext(Dispatchers.IO) {
        if (apiUrl.isBlank() || apiKey.isBlank()) {
            Log.w(TAG, "SMS sending skipped: API URL or Key is missing.")
            return@withContext Triple(false, "API Config Missing", "")
        }

        try {
            // Normalize mobile number for BulkSMSBD (Requires 8801XXXXXXXXX)
            var cleanMobile = mobile.replace("[^0-9]".toRegex(), "")
            if (cleanMobile.startsWith("0")) {
                cleanMobile = "88$cleanMobile"
            } else if (!cleanMobile.startsWith("88")) {
                cleanMobile = "88$cleanMobile"
            }
            
            // Limit to 13 digits (8801XXXXXXXXX)
            if (cleanMobile.length > 13) {
                cleanMobile = cleanMobile.takeLast(13)
            }
            
            // Determine if message is Unicode (Bangla)
            val isUnicode = message.any { it.code > 127 }
            
            // Standard URL encoding
            val encodedMessage = java.net.URLEncoder.encode(message, "UTF-8")
            
            var finalUrl = apiUrl
                .replace("{API_KEY}", apiKey, ignoreCase = true)
                .replace("{API_TOKEN}", apiKey, ignoreCase = true)
                .replace("{SENDER_ID}", senderId, ignoreCase = true)
                .replace("{MOBILE}", cleanMobile, ignoreCase = true)
                .replace("{NUMBER}", cleanMobile, ignoreCase = true)
                .replace("{MESSAGE}", encodedMessage, ignoreCase = true)

            // Handle Unicode (Bangla) type for different gateways
            if (isUnicode) {
                if (finalUrl.contains("type=text", ignoreCase = true)) {
                    finalUrl = finalUrl.replace("type=text", "type=unicode", ignoreCase = true)
                } else if (!finalUrl.contains("type=unicode", ignoreCase = true)) {
                    val separator = if (finalUrl.contains("?")) "&" else "?"
                    finalUrl += "${separator}type=unicode"
                }
            }

            val request = Request.Builder()
                .url(finalUrl)
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    Log.i(TAG, "SMS Sent to $cleanMobile. Response: $body")
                    Triple(true, body, finalUrl)
                } else {
                    Log.e(TAG, "SMS Failed to $cleanMobile. Code: ${response.code}, Body: $body")
                    Triple(false, "Error ${response.code}: $body", finalUrl)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "SMS Exception: ${e.message}")
            Triple(false, e.message ?: "Unknown Exception", "")
        }
    }
}
