@file:OptIn(ExperimentalMaterial3Api::class)

package com.smartpreorder.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.content.Context
import android.net.Uri
import java.io.File
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

fun saveImageLocally(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val file = File(context.filesDir, "img_${System.currentTimeMillis()}_${(1000..9999).random()}.jpg")
        file.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        file.toURI().toString()
    } catch (e: Exception) {
        null
    }
}

val ACTIVE_STATUSES = setOf("pending", "accepted", "preparing", "ready")

fun money(v: Double): String = "Rs. " + String.format(Locale.US, "%,.2f", v)

fun shortId(id: String): String = "#" + id.take(6).uppercase()

fun orderTypeLabel(t: String) = if (t == "dine_in") "Dine-in" else "Pickup"

fun statusLabel(s: String) = when (s) {
    "pending" -> "Pending"
    "accepted" -> "Accepted"
    "preparing" -> "Preparing"
    "ready" -> "Ready"
    "completed" -> "Completed"
    "rejected" -> "Rejected"
    "cancelled" -> "Cancelled"
    else -> s
}

fun parseTime(iso: String?): ZonedDateTime? = try {
    if (iso == null) null else OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault())
} catch (e: Exception) {
    null
}

fun formatTime(iso: String?): String =
    parseTime(iso)?.format(DateTimeFormatter.ofPattern("dd MMM, hh:mm a", Locale.getDefault())) ?: ""

fun isToday(iso: String?): Boolean = parseTime(iso)?.toLocalDate() == LocalDate.now()

fun Throwable.friendly(): String =
    (message ?: "Something went wrong").lineSequence().first().take(200)

@Composable
fun StatusChip(status: String) {
    val (bg, fg) = when (status) {
        "pending" -> Color(0xFFFFF3CD) to Color(0xFF7A5B00)
        "accepted" -> Color(0xFFD6E9FF) to Color(0xFF0B4A8F)
        "preparing" -> Color(0xFFFFE0CC) to Color(0xFF9A3D00)
        "ready" -> Color(0xFFD4F4DD) to Color(0xFF14692B)
        "completed" -> Color(0xFFE3E3E3) to Color(0xFF333333)
        else -> Color(0xFFFFD6D6) to Color(0xFF8F1111)
    }
    Surface(color = bg, shape = RoundedCornerShape(50)) {
        Text(
            statusLabel(status), color = fg, style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun SmallTag(text: String, color: Color = MaterialTheme.colorScheme.primaryContainer) {
    Surface(color = color, shape = RoundedCornerShape(50)) {
        Text(
            text, style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun Section(title: String) {
    Text(
        title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
fun EmptyState(text: String) {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun RatingBar(rating: Int, onChange: ((Int) -> Unit)? = null) {
    Row {
        for (i in 1..5) {
            val icon = if (i <= rating) Ic.Star else Ic.StarBorder
            if (onChange != null) {
                IconButton(onClick = { onChange(i) }) {
                    Icon(icon, "$i stars", tint = Color(0xFFFFB300))
                }
            } else {
                Icon(icon, null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
            }
        }
    }
}

/** Shows ViewModel errors / messages as snackbars. */
@Composable
fun Notices(vm: BaseVm, host: SnackbarHostState) {
    LaunchedEffect(vm.error) {
        vm.error?.let {
            host.showSnackbar(it)
            vm.error = null
        }
    }
    LaunchedEffect(vm.message) {
        vm.message?.let {
            host.showSnackbar(it)
            vm.message = null
        }
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirmLabel: String = "Yes", onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

/** 5-step progress indicator for an order. */
@Composable
fun StatusStepper(status: String) {
    val steps = listOf("pending", "accepted", "preparing", "ready", "completed")
    val cur = steps.indexOf(status)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        steps.forEachIndexed { i, s ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Box(
                    Modifier.size(26.dp).background(
                        if (i <= cur) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        CircleShape
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    if (i < cur || status == "completed") {
                        Icon(Ic.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                    }
                }
                Text(
                    statusLabel(s), style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (i == cur) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
