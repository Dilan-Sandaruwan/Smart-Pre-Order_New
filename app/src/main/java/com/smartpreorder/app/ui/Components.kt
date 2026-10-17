@file:OptIn(ExperimentalMaterial3Api::class)

package com.smartpreorder.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.sp
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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

import com.smartpreorder.app.data.supabase
import io.github.jan.supabase.storage.storage
import java.util.UUID

const val SUPABASE_STORAGE_BUCKET = "MAD APP images"

suspend fun uploadImageToSupabase(context: Context, uri: Uri, folder: String = "uploads"): String {
    val inputStream = context.contentResolver.openInputStream(uri) ?: throw Exception("Cannot read selected image")
    val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: throw Exception("Invalid image file")

    val maxDim = 1024
    val width = originalBitmap.width
    val height = originalBitmap.height
    val scaledBitmap = if (width > maxDim || height > maxDim) {
        val scale = maxDim.toFloat() / Math.max(width, height)
        Bitmap.createScaledBitmap(originalBitmap, (width * scale).toInt(), (height * scale).toInt(), true)
    } else {
        originalBitmap
    }

    val outputStream = ByteArrayOutputStream()
    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
    val byteArray = outputStream.toByteArray()

    val fileName = "$folder/${UUID.randomUUID()}.jpg"
    val bucket = supabase.storage.from(SUPABASE_STORAGE_BUCKET)

    bucket.upload(fileName, byteArray)

    return bucket.publicUrl(fileName).replace(" ", "%20")
}

fun sanitizeImageUrl(url: String?): String? {
    if (url.isNullOrBlank()) return null
    val trimmed = url.trim()
    if (trimmed.startsWith("data:image")) return trimmed
    return trimmed.replace(" ", "%20")
}

fun parseColorHex(hex: String?, fallback: Color = Color(0xFF900C3F)): Color = try {
    if (hex.isNullOrBlank()) fallback
    else {
        val clean = hex.removePrefix("#")
        val colorInt = clean.toLong(16)
        if (clean.length == 6) Color(0xFF000000 or colorInt)
        else if (clean.length == 8) Color(colorInt)
        else fallback
    }
} catch (e: Exception) {
    fallback
}

fun saveImageLocally(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null

        val maxDim = 600
        val width = originalBitmap.width
        val height = originalBitmap.height
        val scaledBitmap = if (width > maxDim || height > maxDim) {
            val scale = maxDim.toFloat() / Math.max(width, height)
            Bitmap.createScaledBitmap(originalBitmap, (width * scale).toInt(), (height * scale).toInt(), true)
        } else {
            originalBitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        val byteArray = outputStream.toByteArray()
        val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP)
        "data:image/jpeg;base64,$base64String"
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

fun Throwable.friendly(): String {
    val msg = message ?: "Something went wrong"
    if (msg.contains("Unexpected JSON token") || msg.contains("Expected start of the array") || msg.contains("Admins only")) {
        return "Admin permission required. Please update your profile role to 'admin' in Supabase database."
    }
    return msg.lineSequence().first().take(200)
}

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
    val cur = steps.indexOf(status).coerceAtLeast(0)

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { i, s ->
                val isCompletedStep = i < cur || status == "completed"
                val isCurrentStep = i == cur && status != "completed"

                // Step Circle
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            color = when {
                                isCompletedStep -> MaterialTheme.colorScheme.primary
                                isCurrentStep -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompletedStep) {
                        Icon(Ic.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                    } else if (isCurrentStep) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(MaterialTheme.colorScheme.onPrimary, CircleShape)
                        )
                    }
                }

                // Connecting Line between step circles
                if (i < steps.lastIndex) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .background(
                                if (i < cur || (i == cur && status == "completed")) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Step text labels row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            steps.forEachIndexed { i, s ->
                val isCurrent = i == cur
                Text(
                    text = statusLabel(s),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
