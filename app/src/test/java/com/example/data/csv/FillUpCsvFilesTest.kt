package com.example.data.csv

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.FillUp
import java.io.File
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FillUpCsvFilesTest {

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
  fun shareIntentCachesTheCsvAndOffersAReadGrant() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val csv = FillUpCsvWriter.write(
      listOf(
        FillUp(
          vehicleId = 1,
          tripMiles = 265.0,
          gallons = 8.2,
          pricePerGallon = 3.48,
          odometer = 85399.0,
          date = localMidnight(2026, 11, 6)
        )
      )
    )

    val chooser = FillUpCsvFiles.shareIntent(context, "../toyota-camry-fillups.csv", csv)

    assertEquals(Intent.ACTION_CHOOSER, chooser.action)
    assertEquals("Share CSV", chooser.getStringExtra(Intent.EXTRA_TITLE))
    val send = chooser.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
    assertNotNull(send)
    assertEquals(Intent.ACTION_SEND, send!!.action)
    assertEquals("text/csv", send.type)
    assertEquals("toyota-camry-fillups.csv", send.getStringExtra(Intent.EXTRA_SUBJECT))
    assertTrue(send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    assertTrue(chooser.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)

    val cached = File(context.cacheDir, "exports/toyota-camry-fillups.csv")
    assertEquals(csv, cached.readText())
    assertTrue(!File(context.cacheDir, "toyota-camry-fillups.csv").exists())

    val uri = send.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
    assertNotNull(uri)
    assertEquals("content", uri!!.scheme)
    val shared = context.contentResolver.openInputStream(uri)?.use { stream ->
      stream.bufferedReader(Charsets.UTF_8).readText()
    }
    assertEquals(csv, shared)
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
}
