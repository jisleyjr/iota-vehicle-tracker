package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.CustomDialogEnvelope
import com.example.data.csv.CsvCell
import com.example.data.csv.FillUpCsvPreview
import com.example.data.csv.FillUpCsvRow
import com.example.data.csv.FillUpCsvRowKind
import com.example.data.csv.importButtonLabel

data class PendingCsvImport(
  val vehicleId: Int,
  val vehicleLabel: String,
  val fileName: String,
  val preview: FillUpCsvPreview
)

@Composable
fun ImportCsvPreviewDialog(
  vehicleLabel: String,
  fileName: String,
  preview: FillUpCsvPreview,
  onDismiss: () -> Unit,
  onImport: () -> Unit
) {
  CustomDialogEnvelope(
    title = "Import Fill-Ups (CSV)",
    subtitle = vehicleLabel,
    onDismiss = onDismiss,
    onSave = onImport,
    saveButtonText = importButtonLabel(preview.newCount),
    saveEnabled = preview.newCount > 0,
    saveButtonTag = "import_rows_button"
  ) {
    Column(modifier = Modifier.fillMaxWidth().testTag("import_preview_dialog")) {
      FileNameChip(fileName)
      Spacer(modifier = Modifier.height(10.dp))
      StatusPill(preview.statusLabel())
      Spacer(modifier = Modifier.height(12.dp))
      PreviewTable(preview.rows)
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "Required headers: trip, gallons, price, date",
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.outline
      )
    }
  }
}

@Composable
private fun FileNameChip(fileName: String) {
  Row(
    modifier = Modifier
      .clip(RoundedCornerShape(10.dp))
      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
      .padding(horizontal = 10.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = Icons.Outlined.Description,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(18.dp)
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = fileName,
      fontSize = 13.sp,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onSurface,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@Composable
private fun StatusPill(label: String) {
  Row(
    modifier = Modifier
      .clip(RoundedCornerShape(50))
      .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
      .padding(horizontal = 10.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Spacer(
      modifier = Modifier
        .size(8.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.secondary)
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = label,
      color = MaterialTheme.colorScheme.secondary,
      fontSize = 12.sp,
      fontWeight = FontWeight.SemiBold
    )
  }
}

@Composable
private fun PreviewTable(rows: List<FillUpCsvRow>) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .border(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
        shape = RoundedCornerShape(12.dp)
      )
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
        .padding(horizontal = 10.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      HeaderCell("Trip", 0.9f)
      HeaderCell("Gallons", 1.05f)
      HeaderCell("Price", 0.95f)
      HeaderCell("Date", 1.25f)
    }
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 300.dp)
        .verticalScroll(rememberScrollState())
    ) {
      rows.forEachIndexed { index, row ->
        if (index > 0) {
          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        }
        PreviewRow(row)
      }
    }
  }
}

@Composable
private fun RowScope.HeaderCell(label: String, weight: Float) {
  Text(
    text = label.uppercase(),
    modifier = Modifier.weight(weight),
    fontSize = 10.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = 0.4.sp,
    color = MaterialTheme.colorScheme.outline
  )
}

@Composable
private fun PreviewRow(row: FillUpCsvRow) {
  val background = when (row.kind) {
    FillUpCsvRowKind.SKIPPED -> MaterialTheme.colorScheme.error.copy(alpha = 0.10f)
    FillUpCsvRowKind.DUPLICATE -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
    FillUpCsvRowKind.NEW -> Color.Transparent
  }
  val valueColor = if (row.kind == FillUpCsvRowKind.DUPLICATE) {
    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
  } else {
    MaterialTheme.colorScheme.onSurface
  }
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(background)
      .padding(horizontal = 10.dp, vertical = 8.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      ValueCell(row.trip, valueColor, 0.9f)
      ValueCell(row.gallons, valueColor, 1.05f)
      ValueCell(row.price, valueColor, 0.95f)
      ValueCell(row.date, valueColor, 1.25f)
    }
    if (row.reason != null) {
      Spacer(modifier = Modifier.height(4.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (row.kind == FillUpCsvRowKind.DUPLICATE) {
          Text(
            text = "Duplicate",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
              .padding(horizontal = 6.dp, vertical = 2.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
        } else {
          Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
          text = "Row ${row.rowNumber}: ${row.reason}",
          modifier = Modifier.weight(1f),
          fontSize = 11.sp,
          color = if (row.kind == FillUpCsvRowKind.SKIPPED) {
            MaterialTheme.colorScheme.error
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant
          }
        )
      }
    }
  }
}

@Composable
private fun RowScope.ValueCell(cell: CsvCell, valueColor: Color, weight: Float) {
  Text(
    text = cell.text,
    modifier = Modifier.weight(weight),
    fontSize = 12.sp,
    fontWeight = FontWeight.Medium,
    color = if (cell.invalid) MaterialTheme.colorScheme.error else valueColor,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis
  )
}
