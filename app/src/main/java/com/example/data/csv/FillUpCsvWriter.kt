package com.example.data.csv

import com.example.data.model.FillUp
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FillUpCsvWriter {
  const val HEADER = "trip,gallons,price,date"

  fun write(fillUps: List<FillUp>): String {
    val csv = StringBuilder()
    csv.append(HEADER).append('\n')
    for (fillUp in fillUps) {
      csv.append(formatNumber(fillUp.tripMiles)).append(',')
      csv.append(formatNumber(fillUp.gallons)).append(',')
      csv.append(formatNumber(fillUp.pricePerGallon)).append(',')
      csv.append(formatDay(fillUp.date)).append('\n')
    }
    return csv.toString()
  }

  fun suggestedFileName(make: String, model: String, year: Int): String {
    val slug = "${make.trim()}-${model.trim()}-$year"
      .lowercase(Locale.US)
      .replace(Regex("[^a-z0-9]+"), "-")
      .trim('-')
      .ifBlank { "vehicle" }
    return "$slug-fillups.csv"
  }

  fun summaryLabel(rowCount: Int): String {
    val rows = if (rowCount == 1) "1 row" else "$rowCount rows"
    return "$rows · headers: $HEADER"
  }

  private fun formatNumber(value: Double): String {
    if (!value.isFinite()) return "0"
    val text = BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
    return if (text == "-0") "0" else text
  }

  private fun formatDay(millis: Long): String {
    val format = SimpleDateFormat("MM/dd/yyyy", Locale.US)
    return format.format(Date(millis))
  }
}
