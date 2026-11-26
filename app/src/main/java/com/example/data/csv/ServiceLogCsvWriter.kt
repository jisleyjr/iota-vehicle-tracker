package com.example.data.csv

import com.example.data.model.Maintenance
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object ServiceLogCsvWriter {
    const val HEADER = "date,type,title,description,cost,odometer,hours,tire_set"

    fun write(records: List<Maintenance>, tireNames: Map<Int, String>): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return buildString {
            appendLine(HEADER)
            for (record in records) {
                val fields = listOf(
                    timestamp.format(Date(record.date)), record.type.name, record.title,
                    record.description.orEmpty(), number(record.cost), number(record.odometer),
                    record.hours?.let(::number).orEmpty(), tireNames[record.tireSetId].orEmpty()
                )
                appendLine(fields.joinToString(",") { escape(it) })
            }
        }
    }

    private fun number(value: Double) = BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()

    private fun escape(value: String): String {
        if (value.none { it in ",\"\r\n" }) return value
        return "\"" + value.replace("\"", "\"\"") + "\""
    }
}
