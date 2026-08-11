package com.example.util

import com.example.data.entity.CustomerEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object ExpiryUtils {

    private val bangladeshTimeZone: TimeZone = TimeZone.getTimeZone("Asia/Dhaka")

    fun parseDateToCalendar(dateStr: String?): Calendar? {
        if (dateStr.isNullOrBlank()) return null
        val str = dateStr.trim()

        val formats = listOf(
            "yyyy-MM-dd",
            "dd MMMM yyyy",
            "d MMMM yyyy",
            "dd/MM/yyyy",
            "d/M/yyyy",
            "dd-MM-yyyy",
            "d-M-yyyy"
        )

        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.US)
                sdf.timeZone = bangladeshTimeZone
                sdf.isLenient = false
                val parsed = sdf.parse(str)
                if (parsed != null) {
                    val cal = Calendar.getInstance(bangladeshTimeZone)
                    cal.time = parsed
                    return cal
                }
            } catch (e: Exception) {
                // ignore
            }
        }
        return null
    }

    fun parseTime(timeStr: String?): Pair<Int, Int> {
        if (timeStr.isNullOrBlank()) return Pair(23, 59)
        val str = timeStr.trim().uppercase(Locale.US)

        val timeFormats = listOf(
            "hh:mm a",
            "h:mm a",
            "HH:mm",
            "H:mm"
        )

        for (fmt in timeFormats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.US)
                sdf.timeZone = bangladeshTimeZone
                val parsed = sdf.parse(str)
                if (parsed != null) {
                    val cal = Calendar.getInstance(bangladeshTimeZone)
                    cal.time = parsed
                    return Pair(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
                }
            } catch (e: Exception) {
                // ignore
            }
        }
        return Pair(23, 59)
    }

    fun getExpireDateTimeCalendar(expireDateStr: String?, expireTimeStr: String?): Calendar? {
        val dateCal = parseDateToCalendar(expireDateStr) ?: return null
        val (hour, minute) = parseTime(expireTimeStr)
        dateCal.set(Calendar.HOUR_OF_DAY, hour)
        dateCal.set(Calendar.MINUTE, minute)
        dateCal.set(Calendar.SECOND, 0)
        dateCal.set(Calendar.MILLISECOND, 0)
        return dateCal
    }

    /**
     * Checks if a customer expires tomorrow relative to current date.
     * Rule: Expire Date - 1 Calendar Day = Alert Date (which is Today).
     */
    fun isExpiringTomorrow(cust: CustomerEntity, now: Date = Date()): Boolean {
        val expireCal = parseDateToCalendar(cust.expireDate) ?: return false

        val todayCal = Calendar.getInstance(bangladeshTimeZone)
        todayCal.time = now
        todayCal.set(Calendar.HOUR_OF_DAY, 0)
        todayCal.set(Calendar.MINUTE, 0)
        todayCal.set(Calendar.SECOND, 0)
        todayCal.set(Calendar.MILLISECOND, 0)

        val tomorrowCal = todayCal.clone() as Calendar
        tomorrowCal.add(Calendar.DAY_OF_MONTH, 1)

        val expDateNormalized = expireCal.clone() as Calendar
        expDateNormalized.set(Calendar.HOUR_OF_DAY, 0)
        expDateNormalized.set(Calendar.MINUTE, 0)
        expDateNormalized.set(Calendar.SECOND, 0)
        expDateNormalized.set(Calendar.MILLISECOND, 0)

        return expDateNormalized.get(Calendar.YEAR) == tomorrowCal.get(Calendar.YEAR) &&
               expDateNormalized.get(Calendar.DAY_OF_YEAR) == tomorrowCal.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Checks if customer has expired based on DateTime.
     */
    fun isExpired(cust: CustomerEntity, now: Date = Date()): Boolean {
        val expireCal = getExpireDateTimeCalendar(cust.expireDate, cust.expireTime) ?: return false
        val nowCal = Calendar.getInstance(bangladeshTimeZone)
        nowCal.time = now
        return nowCal.timeInMillis >= expireCal.timeInMillis
    }

    /**
     * Calculates status dynamically
     */
    fun getCalculatedStatus(cust: CustomerEntity, now: Date = Date()): String {
        if (isExpired(cust, now)) {
            return "Expired"
        }
        if (isExpiringTomorrow(cust, now)) {
            return "Expiring Tomorrow"
        }
        val expireCal = parseDateToCalendar(cust.expireDate)
        if (expireCal != null) {
            val todayCal = Calendar.getInstance(bangladeshTimeZone)
            todayCal.time = now
            if (todayCal.get(Calendar.YEAR) == expireCal.get(Calendar.YEAR) &&
                todayCal.get(Calendar.DAY_OF_YEAR) == expireCal.get(Calendar.DAY_OF_YEAR)) {
                return "Expiring Today"
            }
        }
        return if (cust.status.isNotBlank()) cust.status else "Active"
    }
}
