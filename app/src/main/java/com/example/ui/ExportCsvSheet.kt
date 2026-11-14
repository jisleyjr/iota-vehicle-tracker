package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.csv.FillUpCsvWriter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportCsvSheet(
  fileName: String,
  rowCount: Int,
  onDismiss: () -> Unit,
  onSaveToDevice: () -> Unit,
  onShare: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.background,
    dragHandle = { BottomSheetDefaults.DragHandle() }
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 28.dp)
        .testTag("export_csv_sheet")
    ) {
      Text(
        text = "Export Fill-Ups (CSV)",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
      )
      Spacer(modifier = Modifier.height(16.dp))
      FileSummaryCard(
        fileName = fileName,
        summary = FillUpCsvWriter.summaryLabel(rowCount)
      )
      Spacer(modifier = Modifier.height(8.dp))
      ExportActionRow(
        icon = Icons.Outlined.FileDownload,
        title = "Save to Device",
        subtitle = "SAF file picker · CreateDocument",
        tag = "save_csv_button",
        onClick = onSaveToDevice
      )
      ExportActionRow(
        icon = Icons.Outlined.Share,
        title = "Share CSV",
        subtitle = "Open the Android share sheet",
        tag = "share_csv_button",
        onClick = onShare
      )
    }
  }
}

@Composable
private fun FileSummaryCard(fileName: String, summary: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(MaterialTheme.colorScheme.surface)
      .padding(12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    IconTile(icon = Icons.Outlined.Description)
    Spacer(modifier = Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = fileName,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = summary,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.outline,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun ExportActionRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  tag: String,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .clickable(onClick = onClick)
      .padding(vertical = 10.dp)
      .testTag(tag),
    verticalAlignment = Alignment.CenterVertically
  ) {
    IconTile(icon = icon)
    Spacer(modifier = Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = subtitle,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.outline
      )
    }
  }
}

@Composable
private fun IconTile(icon: ImageVector) {
  Box(
    modifier = Modifier
      .size(44.dp)
      .clip(RoundedCornerShape(12.dp))
      .background(MaterialTheme.colorScheme.surfaceVariant),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(22.dp)
    )
  }
}
