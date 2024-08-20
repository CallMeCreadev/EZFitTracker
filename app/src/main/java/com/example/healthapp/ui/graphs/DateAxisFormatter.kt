package com.example.healthapp.ui.graphs

import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.*

class DateAxisFormatter : ValueFormatter() {
    private val sdf = SimpleDateFormat("MM-dd", Locale.getDefault()) // Only month and day

    override fun getFormattedValue(value: Float): String {
        return sdf.format(Date(value.toLong()))
    }
}
