package com.example.data.csv

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FillUpCsvParserTest {

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
  fun parsesExistingHeaderNamesAndOrder() {
    val preview = parseOk(
      """
      miles, gallons, price per gallon, date
      300.5, 10.2, 3.49, 2026-05-29
      150.0, 5.0, 3.60, 1716940800000
      """.trimIndent()
    )

    assertEquals(2, preview.newCount)
    assertEquals(0, preview.duplicateCount)
    assertEquals(0, preview.skippedCount)
    val first = preview.rows[0].draft!!
    assertEquals(300.5, first.tripMiles, 0.0001)
    assertEquals(10.2, first.gallons, 0.0001)
    assertEquals(3.49, first.pricePerGallon, 0.0001)
    assertEquals(localMidnight(2026, 5, 29), first.dateMillis)
    assertEquals(localMidnight(1716940800000L), preview.rows[1].draft!!.dateMillis)
  }

  @Test
  fun columnsCanAppearInAnyOrderAndHeadersAreCaseInsensitive() {
    val preview = parseOk(
      """
      DATE;Price Per Gallon;GALLONS;Trip
      11/06/2026;$3.48;8.2;265
      """.trimIndent()
    )

    val draft = preview.rows.single().draft!!
    assertEquals(265.0, draft.tripMiles, 0.0001)
    assertEquals(8.2, draft.gallons, 0.0001)
    assertEquals(3.48, draft.pricePerGallon, 0.0001)
    assertEquals(localMidnight(2026, 11, 6), draft.dateMillis)
    assertEquals(FillUpCsvRowKind.NEW, preview.rows.single().kind)
  }

  @Test
  fun stripsDollarSignAndWhitespaceAndAcceptsAnyDecimal() {
    val preview = parseOk(
      """
      trip,gallons,price,date
      "  10.125  ","8.200","  ${'$'}3.4891  ",2026-01-02
      """.trimIndent()
    )

    val draft = preview.rows.single().draft!!
    assertEquals(10.125, draft.tripMiles, 0.0001)
    assertEquals(8.2, draft.gallons, 0.0001)
    assertEquals(3.4891, draft.pricePerGallon, 0.0001)
    assertEquals("10.125", preview.rows.single().trip.text)
    assertEquals("8.2", preview.rows.single().gallons.text)
    assertEquals("${'$'}3.4891", preview.rows.single().price.text)
  }

  @Test
  fun acceptsCommonDateFormatsAndNormalizesToLocalMidnight() {
    val preview = parseOk(
      """
      trip,gallons,price,date
      1,1,1,11/06/2026
      2,1,1,2026-05-29
      3,1,1,2026-11-02T03:30:00Z
      4,1,1,2026-05-29T15:04:05
      """.trimIndent()
    )

    assertEquals(localMidnight(2026, 11, 6), preview.rows[0].draft!!.dateMillis)
    assertEquals(localMidnight(2026, 5, 29), preview.rows[1].draft!!.dateMillis)
    // 03:30Z is still November 1 in America/Chicago (UTC-6).
    assertEquals(localMidnight(2026, 11, 1), preview.rows[2].draft!!.dateMillis)
    assertEquals("11/01/2026", preview.rows[2].date.text)
    assertEquals(localMidnight(2026, 5, 29), preview.rows[3].draft!!.dateMillis)
  }

  @Test
  fun flagsUnparseableDatesInsteadOfUsingTheCurrentTime() {
    val before = System.currentTimeMillis()
    val preview = parseOk(
      """
      trip,gallons,price,date
      12.5,4.25,3.10,yesterday
      10,4,3.10,
      """.trimIndent()
    )
    val after = System.currentTimeMillis()

    assertEquals(0, preview.newCount)
    assertEquals(2, preview.skippedCount)
    assertEquals(FillUpCsvRowKind.SKIPPED, preview.rows[0].kind)
    assertNull(preview.rows[0].draft)
    assertTrue(preview.rows[0].date.invalid)
    assertTrue(preview.rows[0].reason!!.contains("Date could not be parsed"))
    assertEquals("yesterday", preview.rows[0].date.text)
    assertTrue(preview.rows[1].reason!!.contains("Date is missing"))
    preview.rows.forEach { row ->
      val stored = row.draft?.dateMillis
      if (stored != null) {
        assertTrue(stored < before || stored > after)
      }
    }
  }

  @Test
  fun missingRequiredHeaderFailsWithAClearMessage() {
    val missingDate = FillUpCsvParser.parse(
      """
      trip,gallons,price
      10,2,3.5
      """.trimIndent(),
      emptyList()
    )
    assertTrue(missingDate is FillUpCsvParseResult.Error)
    val dateMessage = (missingDate as FillUpCsvParseResult.Error).message
    assertTrue(dateMessage.contains("date"))
    assertTrue(dateMessage.contains("Missing required"))

    val missingTrip = FillUpCsvParser.parse(
      """
      gallons,price per gallon,date
      2,3.5,2026-01-02
      """.trimIndent(),
      emptyList()
    )
    val tripMessage = (missingTrip as FillUpCsvParseResult.Error).message
    assertTrue(tripMessage.contains("trip") || tripMessage.contains("miles"))

    val empty = FillUpCsvParser.parse("   \n", emptyList())
    assertTrue(empty is FillUpCsvParseResult.Error)
    assertTrue((empty as FillUpCsvParseResult.Error).message.contains("empty"))
  }

  @Test
  fun ignoresExtraColumns() {
    val preview = parseOk(
      """
      notes,miles,gallons,price per gallon,date,odometer
      commute,42.5,2.5,4.10,2026-02-02,1000
      """.trimIndent()
    )

    val draft = preview.rows.single().draft!!
    assertEquals(42.5, draft.tripMiles, 0.0001)
    assertEquals(2.5, draft.gallons, 0.0001)
    assertEquals(4.10, draft.pricePerGallon, 0.0001)
    assertEquals(localMidnight(2026, 2, 2), draft.dateMillis)
  }

  @Test
  fun marksInFileDuplicatesByTripAndCalendarDay() {
    val preview = parseOk(
      """
      trip,gallons,price,date
      100,5,3.00,2026-01-01
      100.00,6.5,3.10,2026-01-01T22:15:00
      100,5,3.00,2026-01-02
      101,5,3.00,2026-01-01
      """.trimIndent()
    )

    assertEquals(FillUpCsvRowKind.NEW, preview.rows[0].kind)
    assertEquals(FillUpCsvRowKind.DUPLICATE, preview.rows[1].kind)
    assertTrue(preview.rows[1].reason!!.contains("earlier"))
    assertEquals(FillUpCsvRowKind.NEW, preview.rows[2].kind)
    assertEquals(FillUpCsvRowKind.NEW, preview.rows[3].kind)
    assertEquals(3, preview.newCount)
    assertEquals(1, preview.duplicateCount)
    assertEquals(0, preview.skippedCount)
    assertNull(preview.rows[1].draft)
    assertEquals("3 new · 1 duplicate · 0 skipped", preview.statusLabel())
  }

  @Test
  fun marksRowsThatMatchAnExistingFillUp() {
    val existing = listOf(
      ExistingFillUp(tripMiles = 298.0, dateMillis = localMidnight(2026, 10, 22)),
      ExistingFillUp(tripMiles = 265.0, dateMillis = localMidnight(2026, 11, 6))
    )
    val preview = parseOk(
      """
      trip,gallons,price,date
      298.00,9,${'$'}3.71,10/22/2026
      188.5,11.25,${'$'}3.46,11/01/2026
      265,8.2,3.48,2026-11-06T15:00:00-05:00
      """.trimIndent(),
      existing
    )

    assertEquals(FillUpCsvRowKind.DUPLICATE, preview.rows[0].kind)
    assertTrue(preview.rows[0].reason!!.contains("already", ignoreCase = true))
    assertEquals(FillUpCsvRowKind.NEW, preview.rows[1].kind)
    assertEquals(FillUpCsvRowKind.DUPLICATE, preview.rows[2].kind)
    assertEquals(1, preview.newCount)
    assertEquals(2, preview.duplicateCount)
    assertEquals(listOf(preview.rows[1].draft!!).size, preview.importableDrafts().size)
  }

  @Test
  fun invalidEarlierRowDoesNotBlockALaterValidRow() {
    val preview = parseOk(
      """
      trip,gallons,price,date
      100,NOPE,3.00,2026-01-01
      100,5,3.00,2026-01-01
      """.trimIndent()
    )

    assertEquals(FillUpCsvRowKind.SKIPPED, preview.rows[0].kind)
    assertTrue(preview.rows[0].gallons.invalid)
    assertEquals(FillUpCsvRowKind.NEW, preview.rows[1].kind)
  }

  @Test
  fun importLabelsCountOnlyNewRows() {
    assertEquals("Import 0 Rows", importButtonLabel(0))
    assertEquals("Import 1 Row", importButtonLabel(1))
    assertEquals("Import 12 Rows", importButtonLabel(12))
    assertEquals(
      "Imported 12 fill-ups, skipped 3 duplicates",
      importResultMessage(imported = 12, duplicatesSkipped = 3)
    )
    assertEquals(
      "Imported 1 fill-up, skipped 1 duplicate",
      importResultMessage(imported = 1, duplicatesSkipped = 1)
    )
  }

  private fun parseOk(
    csv: String,
    existing: List<ExistingFillUp> = emptyList()
  ): FillUpCsvPreview {
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
