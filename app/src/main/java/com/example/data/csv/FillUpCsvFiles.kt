package com.example.data.csv

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException

object FillUpCsvFiles {
  fun writeToUri(context: Context, uri: Uri, csv: String) {
    val stream = context.contentResolver.openOutputStream(uri)
      ?: throw IOException("Could not open the selected file.")
    stream.use { output ->
      output.write(csv.toByteArray(Charsets.UTF_8))
    }
  }

  fun shareIntent(context: Context, fileName: String, csv: String): Intent {
    val safeName = safeFileName(fileName)
    val directory = File(context.cacheDir, "exports")
    if (!directory.exists() && !directory.mkdirs()) {
      throw IOException("Could not create the export folder.")
    }
    val file = File(directory, safeName)
    file.writeText(csv, Charsets.UTF_8)
    val uri = FileProvider.getUriForFile(
      context,
      "${context.packageName}.fileprovider",
      file
    )
    val send = Intent(Intent.ACTION_SEND).apply {
      type = "text/csv"
      putExtra(Intent.EXTRA_STREAM, uri)
      putExtra(Intent.EXTRA_SUBJECT, safeName)
      putExtra(Intent.EXTRA_TITLE, safeName)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      clipData = ClipData.newRawUri(safeName, uri)
    }
    return Intent.createChooser(send, "Share CSV").apply {
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      clipData = ClipData.newRawUri(safeName, uri)
    }
  }

  private fun safeFileName(fileName: String): String {
    val base = File(fileName).name
    val cleaned = base.replace(Regex("[^A-Za-z0-9._-]"), "-").trim('-', '.')
    return cleaned.ifBlank { "fillups.csv" }
  }
}
