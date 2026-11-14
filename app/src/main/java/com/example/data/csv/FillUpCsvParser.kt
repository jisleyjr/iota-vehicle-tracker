package com.example.data.csv

import java.math.BigDecimal
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class FillUpCsvRowKind {
  NEW,
  DUPLICATE,
  SKIPPED
}

data class ExistingFillUp(
  val tripMiles: Double,
  val dateMillis: Long
)

data class FillUpDraft(
  val tripMiles: Double,
  val gallons: Double,
  val pricePerGallon: Double,
  val dateMillis: Long
)

data class CsvCell(
  val text: String,
  val invalid: Boolean = false
)

data class FillUpCsvRow(
  val rowNumber: Int,
  val trip: CsvCell,
  val gallons: CsvCell,
  val price: CsvCell,
  val date: CsvCell,
  val kind: FillUpCsvRowKind,
  val reason: String?,
  val draft: FillUpDraft?
)

data class FillUpCsvPreview(
  val rows: List<FillUpCsvRow>
) {
  val newCount: Int get() = rows.count { it.kind == FillUpCsvRowKind.NEW }
  val duplicateCount: Int get() = rows.count { it.kind == FillUpCsvRowKind.DUPLICATE }
  val skippedCount: Int get() = rows.count { it.kind == FillUpCsvRowKind.SKIPPED }

  fun statusLabel(): String {
    val duplicates = if (duplicateCount == 1) "1 duplicate" else "$duplicateCount duplicates"
    return "$newCount new · $duplicates · $skippedCount skipped"
  }

  fun importableDrafts(): List<FillUpDraft> {
    return rows.mapNotNull { row -> if (row.kind == FillUpCsvRowKind.NEW) row.draft else null }
  }
}

sealed class FillUpCsvParseResult {
  data class Ok(val preview: FillUpCsvPreview) : FillUpCsvParseResult()
  data class Error(val message: String) : FillUpCsvParseResult()
}

fun importButtonLabel(newCount: Int): String {
  return if (newCount == 1) "Import 1 Row" else "Import $newCount Rows"
}

fun importResultMessage(imported: Int, duplicatesSkipped: Int): String {
  val fillUps = if (imported == 1) "fill-up" else "fill-ups"
  val duplicates = if (duplicatesSkipped == 1) "duplicate" else "duplicates"
  return "Imported $imported $fillUps, skipped $duplicatesSkipped $duplicates"
}

object FillUpCsvParser {
  fun parse(csvText: String, existing: List<ExistingFillUp>): FillUpCsvParseResult {
    val source = csvText.removePrefix("\uFEFF")
    if (source.isBlank()) {
      return FillUpCsvParseResult.Error("The CSV file is empty.")
    }

    val lines = source.lines()
    val headerIndex = lines.indexOfFirst { it.isNotBlank() }
    if (headerIndex < 0) {
      return FillUpCsvParseResult.Error("The CSV file is empty.")
    }

    val delimiter = detectDelimiter(lines[headerIndex])
    val headerCells = splitDelimited(lines[headerIndex], delimiter)
    val columns = linkedMapOf<Column, Int>()
    headerCells.forEachIndexed { index, header ->
      val column = columnFor(header) ?: return@forEachIndexed
      if (!columns.containsKey(column)) {
        columns[column] = index
      }
    }

    val missing = Column.entries.filter { it !in columns }
    if (missing.isNotEmpty()) {
      return FillUpCsvParseResult.Error(missingHeaderMessage(missing, headerCells))
    }

    val existingKeys = existing.map { fillUp ->
      tripKey(fillUp.tripMiles) to localMidnight(fillUp.dateMillis)
    }.toSet()
    val seenKeys = linkedSetOf<Pair<String, Long>>()
    val rows = mutableListOf<FillUpCsvRow>()
    var rowNumber = 0
    for (line in lines.drop(headerIndex + 1)) {
      if (line.isBlank()) continue
      rowNumber += 1
      rows += parseRow(
        cells = splitDelimited(line, delimiter),
        columns = columns,
        rowNumber = rowNumber,
        existingKeys = existingKeys,
        seenKeys = seenKeys
      )
    }
    return FillUpCsvParseResult.Ok(FillUpCsvPreview(rows))
  }

  private enum class Column {
    TRIP,
    GALLONS,
    PRICE,
    DATE
  }

  private fun parseRow(
    cells: List<String>,
    columns: Map<Column, Int>,
    rowNumber: Int,
    existingKeys: Set<Pair<String, Long>>,
    seenKeys: MutableSet<Pair<String, Long>>
  ): FillUpCsvRow {
    val trip = parseNumber(cell(cells, columns, Column.TRIP), "Trip", currency = false)
    val gallons = parseNumber(cell(cells, columns, Column.GALLONS), "Gallons", currency = false)
    val price = parseNumber(cell(cells, columns, Column.PRICE), "Price", currency = true)
    val date = parseDate(cell(cells, columns, Column.DATE))
    val problems = listOfNotNull(trip.problem, gallons.problem, price.problem, date.problem)
    if (problems.isNotEmpty()) {
      return FillUpCsvRow(
        rowNumber = rowNumber,
        trip = trip.cell,
        gallons = gallons.cell,
        price = price.cell,
        date = date.cell,
        kind = FillUpCsvRowKind.SKIPPED,
        reason = problems.joinToString("; "),
        draft = null
      )
    }

    val tripMiles = trip.value!!
    val midnight = date.instant!!
    val key = tripKey(tripMiles) to midnight
    val matchesExisting = key in existingKeys
    val matchesEarlierRow = key in seenKeys
    if (!matchesExisting && !matchesEarlierRow) {
      seenKeys += key
      return FillUpCsvRow(
        rowNumber = rowNumber,
        trip = trip.cell,
        gallons = gallons.cell,
        price = price.cell,
        date = date.cell,
        kind = FillUpCsvRowKind.NEW,
        reason = null,
        draft = FillUpDraft(
          tripMiles = tripMiles,
          gallons = gallons.value!!,
          pricePerGallon = price.value!!,
          dateMillis = midnight
        )
      )
    }

    val reason = if (matchesExisting) {
      "Already saved for this vehicle"
    } else {
      "Matches an earlier row"
    }
    return FillUpCsvRow(
      rowNumber = rowNumber,
      trip = trip.cell,
      gallons = gallons.cell,
      price = price.cell,
      date = date.cell,
      kind = FillUpCsvRowKind.DUPLICATE,
      reason = reason,
      draft = null
    )
  }

  private data class NumberField(
    val value: Double?,
    val cell: CsvCell,
    val problem: String?
  )

  private data class DateField(
    val instant: Long?,
    val cell: CsvCell,
    val problem: String?
  )

  private fun parseNumber(raw: String, label: String, currency: Boolean): NumberField {
    if (raw.isBlank()) {
      return NumberField(null, CsvCell("—", invalid = true), "$label is missing")
    }
    val value = decimalOrNull(raw, currency)
    if (value == null) {
      return NumberField(null, CsvCell(raw, invalid = true), "$label is not a valid number")
    }
    val shown = if (currency) "$${formatDecimal(value)}" else formatDecimal(value)
    return NumberField(value, CsvCell(shown), null)
  }

  private fun parseDate(raw: String): DateField {
    if (raw.isBlank()) {
      return DateField(null, CsvCell("—", invalid = true), "Date is missing")
    }
    val instant = parseInstant(raw)
    if (instant == null) {
      return DateField(null, CsvCell(raw, invalid = true), "Date could not be parsed")
    }
    val midnight = localMidnight(instant)
    return DateField(midnight, CsvCell(formatDay(midnight)), null)
  }

  private fun cell(cells: List<String>, columns: Map<Column, Int>, column: Column): String {
    val index = columns.getValue(column)
    return cells.getOrNull(index)?.trim().orEmpty()
  }

  private fun decimalOrNull(raw: String, currency: Boolean): Double? {
    var text = raw.trim()
    if (currency) {
      text = text.removePrefix("$").trim()
    }
    if (text.isEmpty()) return null
    val value = text.toDoubleOrNull() ?: return null
    if (!value.isFinite()) return null
    return value
  }

  private fun formatDecimal(value: Double): String {
    val text = BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
    return if (text == "-0") "0" else text
  }

  private fun formatDay(millis: Long): String {
    val format = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
    return format.format(Date(millis))
  }

  private fun localMidnight(millis: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = millis
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
  }

  private fun tripKey(value: Double): String {
    return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
  }

  private fun parseInstant(text: String): Long? {
    if (EPOCH.matches(text)) {
      return text.toLongOrNull()
    }
    for (pattern in DATE_PATTERNS) {
      try {
        val format = SimpleDateFormat(pattern.pattern, Locale.US)
        format.isLenient = false
        if (pattern.utc) {
          format.timeZone = TimeZone.getTimeZone("UTC")
        }
        val position = ParsePosition(0)
        val parsed = format.parse(text, position) ?: continue
        if (position.index == text.length) {
          return parsed.time
        }
      } catch (_: IllegalArgumentException) {
        // This runtime does not support the pattern.
      }
    }
    return null
  }

  private fun columnFor(header: String): Column? {
    return when (normalizeHeader(header)) {
      "trip", "trips", "miles", "mile", "trip miles", "tripmiles" -> Column.TRIP
      "gallons", "gallon" -> Column.GALLONS
      "price", "price per gallon", "price gallon" -> Column.PRICE
      "date" -> Column.DATE
      else -> null
    }
  }

  private fun normalizeHeader(header: String): String {
    return header
      .replace("\uFEFF", "")
      .lowercase(Locale.US)
      .replace("$", "")
      .replace(Regex("[^a-z0-9 ]"), " ")
      .trim()
      .replace(Regex("\\s+"), " ")
  }

  private fun missingHeaderMessage(missing: List<Column>, found: List<String>): String {
    val names = missing.joinToString(", ") { column ->
      when (column) {
        Column.TRIP -> "trip or miles"
        Column.GALLONS -> "gallons"
        Column.PRICE -> "price or price per gallon"
        Column.DATE -> "date"
      }
    }
    val noun = if (missing.size == 1) "column" else "columns"
    val foundText = found.map { it.trim() }.filter { it.isNotEmpty() }.joinToString(", ").ifBlank { "none" }
    return "Missing required CSV $noun: $names. Found headers: $foundText."
  }

  private fun detectDelimiter(line: String): Char {
    val commas = splitDelimited(line, ',').size
    val semicolons = splitDelimited(line, ';').size
    return if (semicolons > commas) ';' else ','
  }

  private fun splitDelimited(line: String, delimiter: Char): List<String> {
    val fields = mutableListOf<String>()
    val current = StringBuilder()
    var inQuotes = false
    var index = 0
    while (index < line.length) {
      val character = line[index]
      when {
        character == '"' -> {
          if (inQuotes && index + 1 < line.length && line[index + 1] == '"') {
            current.append('"')
            index += 1
          } else {
            inQuotes = !inQuotes
          }
        }
        character == delimiter && !inQuotes -> {
          fields += current.toString().trim()
          current.clear()
        }
        else -> current.append(character)
      }
      index += 1
    }
    fields += current.toString().trim()
    return fields
  }

  private data class DatePattern(val pattern: String, val utc: Boolean)

  private val EPOCH = Regex("^-?\\d+$")

  private val DATE_PATTERNS = listOf(
    DatePattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", utc = false),
    DatePattern("yyyy-MM-dd'T'HH:mm:ssXXX", utc = false),
    DatePattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", utc = true),
    DatePattern("yyyy-MM-dd'T'HH:mm:ss'Z'", utc = true),
    DatePattern("yyyy-MM-dd'T'HH:mm:ss.SSS", utc = false),
    DatePattern("yyyy-MM-dd'T'HH:mm:ss", utc = false),
    DatePattern("yyyy-MM-dd HH:mm:ss", utc = false),
    DatePattern("MM/dd/yyyy HH:mm:ss", utc = false),
    DatePattern("MM/dd/yyyy", utc = false),
    DatePattern("M/d/yyyy", utc = false),
    DatePattern("yyyy-MM-dd", utc = false),
    DatePattern("yyyy/MM/dd", utc = false)
  )
}
