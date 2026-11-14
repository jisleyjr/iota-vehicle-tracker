package com.example.data.csv

import com.example.data.model.FillUp
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FillUpCsvWriterTest {

  private lateinit var originalZone: TimeZone
  private lateinit var originalLocale: Locale

  @Before
  fun setUp() {
    originalZone = TimeZone.getDefault()
    originalLocale = Locale.getDefault()
    TimeZone.setDefault(TimeZone.getTimeZone("America/Chicago"))
    Locale.setDefault(Locale.US)
  }

  @After
  fun tearDown() {
    TimeZone.setDefault(originalZone)
    Locale.setDefault(originalLocale)
  }

  @Test
  fun writesTripGallonsPricePerGallonAndMonthDayYear() {
    val csv = FillUpCsvWriter.write(
      listOf(
        fillUp(
          tripMiles = 265.0,
          gallons = 8.2,
          pricePerGallon = 3.48,
          odometer = 85399.0,
          date = localMidnight(2026, 11, 6)
        )
      )
    )

    assertEquals(
      """
      trip,gallons,price,date
      265,8.2,3.48,11/06/2026
      """.trimIndent() + "\n",
      csv
    )
    assertFalse(csv.contains("85399"))
    assertFalse(csv.contains("28.5"))
  }

  @Test
  fun keepsSignificantDecimalsAndDropsTrailingZeros() {
    val csv = FillUpCsvWriter.write(
      listOf(
        fillUp(tripMiles = 10.125, gallons = 8.200, pricePerGallon = 3.50, date = localMidnight(2026, 1, 2)),
        fillUp(tripMiles = 100.0, gallons = 5.0, pricePerGallon = 3.4891, date = localMidnight(2026, 1, 3))
      )
    )

    assertEquals(
      """
      trip,gallons,price,date
      10.125,8.2,3.5,01/02/2026
      100,5,3.4891,01/03/2026
      """.trimIndent() + "\n",
      csv
    )
  }

  @Test
  fun preservesGivenOrderAndWritesAHeaderForAnEmptyList() {
    val csv = FillUpCsvWriter.write(
      listOf(
        fillUp(tripMiles = 2.0, gallons = 1.0, pricePerGallon = 4.0, date = localMidnight(2026, 3, 2)),
        fillUp(tripMiles = 1.0, gallons = 1.0, pricePerGallon = 4.0, date = localMidnight(2026, 3, 1))
      )
    )
    val lines = csv.trimEnd().lines()
    assertEquals("trip,gallons,price,date", lines[0])
    assertTrue(lines[1].startsWith("2,"))
    assertTrue(lines[2].startsWith("1,"))
    assertEquals("trip,gallons,price,date\n", FillUpCsvWriter.write(emptyList()))
  }

  @Test
  fun exportThenImportFlagsEveryRowAsADuplicateOfTheSameVehicle() {
    val fillUps = listOf(
      fillUp(tripMiles = 265.0, gallons = 8.2, pricePerGallon = 3.48, date = localMidnight(2026, 11, 6) + 15 * 60 * 60 * 1000),
      fillUp(tripMiles = 298.0, gallons = 9.0, pricePerGallon = 3.71, date = localMidnight(2026, 10, 22)),
      fillUp(tripMiles = 10.125, gallons = 8.2, pricePerGallon = 3.4891, date = localMidnight(2026, 1, 2)),
      fillUp(tripMiles = 100.0, gallons = 6.5, pricePerGallon = 3.10, date = localMidnight(2026, 1, 1))
    )
    val csv = FillUpCsvWriter.write(fillUps)
    val existing = fillUps.map { ExistingFillUp(tripMiles = it.tripMiles, dateMillis = it.date) }

    val reimported = parseOk(csv, existing)
    assertEquals(0, reimported.newCount)
    assertEquals(0, reimported.skippedCount)
    assertEquals(fillUps.size, reimported.duplicateCount)
    assertTrue(reimported.rows.all { it.kind == FillUpCsvRowKind.DUPLICATE })
    assertTrue(reimported.rows.all { it.reason!!.contains("already", ignoreCase = true) })

    val fresh = parseOk(csv, emptyList())
    assertEquals(fillUps.size, fresh.newCount)
    assertEquals(0, fresh.duplicateCount)
    fresh.rows.zip(fillUps).forEach { (row, source) ->
      val draft = row.draft!!
      assertEquals(source.tripMiles, draft.tripMiles, 0.0001)
      assertEquals(source.gallons, draft.gallons, 0.0001)
      assertEquals(source.pricePerGallon, draft.pricePerGallon, 0.0001)
      assertEquals(localMidnight(source.date), draft.dateMillis)
    }
  }

  @Test
  fun suggestedFileNameUsesLowercaseMakeAndModel() {
    assertEquals("toyota-camry-fillups.csv", FillUpCsvWriter.suggestedFileName("Toyota", "Camry"))
    assertEquals(
      "land-rover-range-rover-fillups.csv",
      FillUpCsvWriter.suggestedFileName("  Land Rover ", "Range Rover")
    )
    assertEquals("vehicle-fillups.csv", FillUpCsvWriter.suggestedFileName("", "   "))
    assertEquals("3 rows · headers: trip,gallons,price,date", FillUpCsvWriter.summaryLabel(3))
    assertEquals("1 row · headers: trip,gallons,price,date", FillUpCsvWriter.summaryLabel(1))
  }

  private fun fillUp(
    tripMiles: Double,
    gallons: Double,
    pricePerGallon: Double,
    date: Long,
    odometer: Double? = null
  ): FillUp {
    return FillUp(
      vehicleId = 1,
      tripMiles = tripMiles,
      gallons = gallons,
      pricePerGallon = pricePerGallon,
      odometer = odometer,
      date = date
    )
  }

  private fun parseOk(csv: String, existing: List<ExistingFillUp>): FillUpCsvPreview {
    val result = FillUpCsvParser.parse(csv, existing)
    assertTrue(result.toString(), result is FillUpCsvParseResult.Ok)
    return (result as FillUpCsvParseResult.Ok).preview
  }

  private fun localMidnight(year: Int, month: Int, day: Int): Long {
    return Calendar.getInstance().apply {
      set(Calendar.YEAR, year)
      set(Calendar.MONTH, month - 1)
      set(Calendar.DAY_OF_MONTH, day)
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }.timeInMillis
  }

  private fun localMidnight(millis: Long): Long {
    return Calendar.getInstance().apply {
      timeInMillis = millis
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }.timeInMillis
  }
}
