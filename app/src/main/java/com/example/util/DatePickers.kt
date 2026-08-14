package com.example.util

import android.app.DatePickerDialog
import android.content.Context
import java.text.SimpleDateFormat
import java.util.*

object DatePickerUtils {

    fun showDatePicker(
        context: Context,
        initialDate: String? = null,
        onDateSelected: (String) -> Unit
    ) {
        val calendar = Calendar.getInstance()
        if (!initialDate.isNullOrBlank()) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val date = sdf.parse(initialDate)
                if (date != null) {
                    calendar.time = date
                }
            } catch (e: Exception) {
                // If parsing fails, use current date
            }
        }

        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth)
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                onDateSelected(sdf.format(selectedCalendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        // Mark today as highlighted (handled by default in DatePickerDialog)
        datePickerDialog.show()
    }
}
