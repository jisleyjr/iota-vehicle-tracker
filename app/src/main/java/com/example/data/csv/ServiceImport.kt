package com.example.data.csv

import com.example.data.model.*
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale

data class ServiceImportRow(
    val label: String, val reason: String, val record: Maintenance? = null,
    val tireName: String = "", val duplicate: Boolean = false
)

data class ServiceImport(val vehicleId: Int, val vehicleLabel: String, val rows: List<ServiceImportRow>) {
    val newCount = rows.count { it.record != null }
    val duplicates = rows.count { it.duplicate }
    val skipped = rows.size - newCount - duplicates
}

internal fun Maintenance.importKey() = copy(id = 0, description = description?.ifEmpty { null })

object ServiceImportParser {
    fun parse(text: String, vehicle: Vehicle, history: List<Maintenance>, tires: List<TireSet>): ServiceImport {
        val records = readRecords(text.removePrefix("\uFEFF"))
        val header = records.firstOrNull().orEmpty()
        val columns = ServiceLogCsvWriter.HEADER.split(',')
        require(header.sorted() == columns.sorted()) { "Expected columns: ${ServiceLogCsvWriter.HEADER}" }
        val indices = columns.map(header::indexOf)
        val known = history.map { it.importKey() }.toMutableSet()
        val dates = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).apply { isLenient = false }
        val byName = tires.groupBy { it.name }
        val rows = records.drop(1).mapIndexed { index, fields ->
            val cells = indices.map { fields.getOrNull(it).orEmpty() }
            val label = "Row ${index + 1} · ${cells[2]} · ${cells[1]} · ${cells[0]}"
            try {
                require(fields.size == header.size) { "Wrong number of fields" }
                val pos = ParsePosition(0)
                val date = dates.parse(cells[0], pos)
                require(date != null && pos.index == cells[0].length &&
                    cells[0].matches(Regex("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}(Z|[+-]\d{2}:\d{2})"""))) { "Use an exported timestamp" }
                val type = requireNotNull(MaintenanceType.entries.find { it.name == cells[1] }) { "Unknown service type" }
                require(cells[2].isNotBlank()) { "Title is required" }
                val name = cells[7]
                val tire = if (name.isEmpty()) null else {
                    require(type in listOf(MaintenanceType.TIRE_SWAP, MaintenanceType.TIRE_ROTATION)) { "Remove unexpected tire set" }
                    requireNotNull(byName[name]?.singleOrNull()) { "Supply a unique tire set named $name for this vehicle" }
                }
                require(type != MaintenanceType.TIRE_SWAP || tire != null) { "Tire swap requires a tire set" }
                fun number(i: Int): Double {
                    val value = cells[i].toDoubleOrNull()
                    require(value != null && value.isFinite() && value >= 0) { "Invalid ${columns[i]}" }
                    return if (value == 0.0) 0.0 else value
                }
                val record = Maintenance(vehicleId = vehicle.id, date = date.time, type = type, title = cells[2],
                    description = cells[3].ifEmpty { null }, cost = number(4), odometer = number(5),
                    hours = cells[6].takeIf { it.isNotEmpty() }?.let { number(6) }, tireSetId = tire?.id)
                if (known.add(record.importKey())) ServiceImportRow(label, "New", record, name)
                else ServiceImportRow(label, "Duplicate", duplicate = true)
            } catch (error: IllegalArgumentException) {
                ServiceImportRow(label, "Skipped · ${error.message}")
            }
        }
        return ServiceImport(vehicle.id, "${vehicle.year} ${vehicle.make} ${vehicle.model}", rows)
    }

    private fun readRecords(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val fields = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var closed = false
        fun finishField() {
            fields.add(field.toString())
            field.clear()
            closed = false
        }
        fun finishRow() {
            if (fields.isNotEmpty() || field.isNotEmpty() || closed) {
                finishField()
                rows.add(fields.toList())
                fields.clear()
            }
        }
        var i = 0
        while (i < text.length) {
            val c = text[i++]
            when {
                quoted && c == '"' -> {
                    if (text.getOrNull(i) == '"') {
                        field.append('"')
                        i++
                    } else {
                        quoted = false
                        closed = true
                    }
                }
                quoted -> field.append(c)
                c == ',' -> finishField()
                c == '\r' || c == '\n' -> {
                    if (c == '\r' && text.getOrNull(i) == '\n') i++
                    finishRow()
                }
                c == '"' && field.isEmpty() && !closed -> quoted = true
                else -> {
                    require(!closed && c != '"') { "Malformed CSV quoting" }
                    field.append(c)
                }
            }
        }
        require(!quoted) { "Unclosed quoted field" }
        finishRow()
        return rows
    }
}
