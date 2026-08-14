package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppUtils {
    
    /**
     * Converts date from YYYY-MM-DD to DD-MM-YYYY for display.
     */
    fun formatDateForDisplay(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return ""
        return try {
            if (dateStr.contains("-") && dateStr.length == 10) {
                val parts = dateStr.split("-")
                if (parts.size == 3 && parts[0].length == 4) {
                    // It is YYYY-MM-DD
                    "${parts[2]}-${parts[1]}-${parts[0]}"
                } else {
                    dateStr
                }
            } else {
                dateStr
            }
        } catch (e: Exception) {
            dateStr
        }
    }

    /**
     * Formats current date for display.
     */
    fun getCurrentDateForDisplay(): String {
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.US)
        return sdf.format(Date())
    }
}
