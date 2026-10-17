@file:OptIn(ExperimentalMaterial3Api::class)

package com.smartpreorder.app.ui.restaurant

import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.compose.material3.FilterChip
import java.time.OffsetDateTime
import java.time.ZoneId
import com.smartpreorder.app.ui.generateExcelXlsx
import com.smartpreorder.app.ui.uploadFileToSupabase
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.smartpreorder.app.ui.sanitizeImageUrl
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import com.smartpreorder.app.ui.friendly
import com.smartpreorder.app.ui.uploadImageToSupabase
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartpreorder.app.data.MenuItem
import com.smartpreorder.app.data.Order
import com.smartpreorder.app.data.Profile
import com.smartpreorder.app.data.Repo
import com.smartpreorder.app.data.Restaurant
import com.smartpreorder.app.ui.AuthViewModel
import com.smartpreorder.app.ui.ConfirmDialog
import com.smartpreorder.app.ui.EmptyState
import com.smartpreorder.app.ui.Ic
import com.smartpreorder.app.ui.Notices
import com.smartpreorder.app.ui.RestaurantViewModel
import com.smartpreorder.app.ui.Section
import com.smartpreorder.app.ui.SmallTag
import com.smartpreorder.app.ui.StatCard
import com.smartpreorder.app.ui.StatusChip
import com.smartpreorder.app.ui.formatTime
import com.smartpreorder.app.ui.money
import com.smartpreorder.app.ui.orderTypeLabel
import com.smartpreorder.app.ui.shortId

// =====================================================================
//  Restaurant role: entry
// =====================================================================
@Composable
fun RestaurantApp(auth: AuthViewModel, profile: Profile) {
    val vm: RestaurantViewModel = viewModel(key = "restaurant-${profile.id}")
    LaunchedEffect(vm) { vm.start() }

    when {
        !vm.loaded -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        vm.restaurant == null -> RestaurantRegisterScreen(vm, auth)
        else -> RestaurantTabs(vm, auth)
    }
}

@Composable
private fun RestaurantTabs(vm: RestaurantViewModel, auth: AuthViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val snack = remember { SnackbarHostState() }
    Notices(vm, snack)
    Notices(auth, snack)

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            RestaurantBottomBar(
                selectedTab = tab,
                onTabSelected = { tab = it },
                pendingCount = vm.pendingCount
            )
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> OrderQueueTab(vm)
                1 -> MenuTab(vm)
                2 -> SalesTab(vm)
                else -> RestaurantProfileTab(vm, auth)
            }
        }
    }
}

@Composable
fun RestaurantApprovalBanner(r: Restaurant?) {
    if (r == null) return
    if (r.approvalStatus == "pending" || !r.isActive) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFF3CD)
            )
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Ic.Report,
                    contentDescription = null,
                    tint = Color(0xFF7A5B00),
                    modifier = Modifier.padding(end = 12.dp)
                )
                Column {
                    Text(
                        "Pending Admin Approval",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7A5B00)
                    )
                    Text(
                        "Your restaurant is awaiting approval by an administrator. Customers cannot view your menu or place orders until approved.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF7A5B00)
                    )
                }
            }
        }
    } else if (r.approvalStatus == "rejected") {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFD6D6)
            )
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Ic.Close,
                    contentDescription = null,
                    tint = Color(0xFF8F1111),
                    modifier = Modifier.padding(end = 12.dp)
                )
                Column {
                    Text(
                        "Registration Rejected",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8F1111)
                    )
                    Text(
                        "Your restaurant registration was rejected by the administrator. Food sales are disabled.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8F1111)
                    )
                }
            }
        }
    }
}

// =====================================================================
//  Tab 1: live order queue
// =====================================================================
@Composable
private fun OrderQueueTab(vm: RestaurantViewModel) {
    var etaTarget by remember { mutableStateOf<Order?>(null) }   // accept (pending) or change ETA
    var rejectTarget by remember { mutableStateOf<Order?>(null) }
    var tableNoTarget by remember { mutableStateOf<Order?>(null) }
    val active = vm.activeOrders

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(vm.restaurant?.name ?: "", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Live order queue (${active.size})", color = MaterialTheme.colorScheme.outline)
                }
                IconButton(onClick = { vm.refresh() }) { Icon(Ic.Refresh, "Refresh") }
            }
        }
        item { RestaurantApprovalBanner(vm.restaurant) }
        if (active.isEmpty()) item { EmptyState("No active orders right now.\nNew orders appear here instantly.") }
        items(active, key = { it.id }) { o ->
            QueueCard(
                o,
                onAccept = { etaTarget = o },
                onReject = { rejectTarget = o },
                onChangeEta = { etaTarget = o },
                onMarkReady = {
                    if (o.orderType == "dine_in") {
                        tableNoTarget = o
                    } else {
                        vm.setStatus(o, "ready")
                    }
                },
                onStatus = { s -> vm.setStatus(o, s) }
            )
        }
    }

    etaTarget?.let { o ->
        EtaDialog(
            initial = o.estimatedMinutes ?: 15,
            title = if (o.status == "pending") "Accept order ${shortId(o.id)}" else "Update preparation time",
            confirmLabel = if (o.status == "pending") "Accept" else "Update",
            onConfirm = { mins ->
                if (o.status == "pending") vm.setStatus(o, "accepted", minutes = mins) else vm.setEta(o, mins)
            },
            onDismiss = { etaTarget = null }
        )
    }
    rejectTarget?.let { o ->
        var reason by remember(o.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { rejectTarget = null },
            title = { Text("Reject order ${shortId(o.id)}") },
            text = {
                OutlinedTextField(reason, { reason = it }, label = { Text("Reason (shown to the customer)") },
                    modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.setStatus(o, "rejected", reason = reason.ifBlank { "Sorry, we can't take this order right now." })
                    rejectTarget = null
                }) { Text("Reject") }
            },
            dismissButton = { TextButton(onClick = { rejectTarget = null }) { Text("Back") } }
        )
    }
    tableNoTarget?.let { o ->
        TableNoDialog(
            order = o,
            onConfirm = { tableNo ->
                vm.setStatus(o, "ready", tableNo = tableNo)
                tableNoTarget = null
            },
            onDismiss = { tableNoTarget = null }
        )
    }
}

@Composable
private fun TableNoDialog(
    order: Order,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var tableInput by remember { mutableStateOf(order.tableNo.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dine-in Order ${shortId(order.id)}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter the Table Number where the customer should be seated.")
                OutlinedTextField(
                    value = tableInput,
                    onValueChange = { tableInput = it },
                    label = { Text("Table No. (e.g. Table 05) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (tableInput.isNotBlank()) {
                        onConfirm(tableInput.trim())
                    }
                },
                enabled = tableInput.isNotBlank()
            ) {
                Text("Mark Ready & Notify")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun QueueCard(
    o: Order,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onChangeEta: () -> Unit,
    onMarkReady: () -> Unit,
    onStatus: (String) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(shortId(o.id), fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
                SmallTag(orderTypeLabel(o.orderType))
                Spacer(Modifier.weight(1f))
                StatusChip(o.status)
            }
            Text("${o.customerName ?: "Customer"}  •  ${formatTime(o.createdAt)}", style = MaterialTheme.typography.bodySmall)
            if (!o.customerPhone.isNullOrBlank()) {
                Text(o.customerPhone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            if (!o.tableNo.isNullOrBlank()) {
                Text("Table No: ${o.tableNo}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            HorizontalDivider()
            o.items.forEach { Text("${it.quantity}× ${it.itemName}") }
            if (!o.note.isNullOrBlank()) Text("Note: ${o.note}", color = MaterialTheme.colorScheme.primary)
            Row {
                Text(
                    o.estimatedMinutes?.let { "ETA $it min" } ?: "",
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f)
                )
                Text(money(o.total), fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                when (o.status) {
                    "pending" -> {
                        OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f)) { Text("Reject") }
                        Button(onClick = onAccept, modifier = Modifier.weight(1f)) { Text("Accept") }
                    }
                    "accepted" -> {
                        OutlinedButton(onClick = onChangeEta, modifier = Modifier.weight(1f)) { Text("Change ETA") }
                        Button(onClick = { onStatus("preparing") }, modifier = Modifier.weight(1f)) { Text("Start preparing") }
                    }
                    "preparing" -> {
                        OutlinedButton(onClick = onChangeEta, modifier = Modifier.weight(1f)) { Text("Change ETA") }
                        Button(onClick = onMarkReady, modifier = Modifier.weight(1f)) { Text("Mark ready") }
                    }
                    "ready" -> Button(onClick = { onStatus("completed") }, modifier = Modifier.fillMaxWidth()) { Text("Mark completed") }
                }
            }
        }
    }
}

@Composable
private fun EtaDialog(initial: Int, title: String, confirmLabel: String, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Estimated preparation time (minutes)")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(10, 15, 20, 30).forEach { m ->
                        AssistChip(onClick = { text = m.toString() }, label = { Text("$m") })
                    }
                }
                OutlinedTextField(text, { text = it.filter { c -> c.isDigit() }.take(3) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm((text.toIntOrNull() ?: initial).coerceAtLeast(1))
                onDismiss()
            }) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// =====================================================================
//  Tab 2: menu management (see MenuManagement.kt)
// =====================================================================

// =====================================================================
//  Tab 3: sales + history
// =====================================================================
@Composable
private fun SalesTab(vm: RestaurantViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedPeriod by remember { mutableStateOf("daily") } // "daily", "weekly", "monthly", "all"
    var isExporting by remember { mutableStateOf(false) }

    val now = OffsetDateTime.now()
    val todayStart = now.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime()
    val weekStart = now.minusDays(7)
    val monthStart = now.minusDays(30)

    val completedOrders = vm.completed

    val periodOrders = when (selectedPeriod) {
        "daily" -> completedOrders.filter {
            try { OffsetDateTime.parse(it.createdAt).isAfter(todayStart) } catch (e: Exception) { true }
        }
        "weekly" -> completedOrders.filter {
            try { OffsetDateTime.parse(it.createdAt).isAfter(weekStart) } catch (e: Exception) { true }
        }
        "monthly" -> completedOrders.filter {
            try { OffsetDateTime.parse(it.createdAt).isAfter(monthStart) } catch (e: Exception) { true }
        }
        else -> completedOrders
    }

    val periodSalesTotal = periodOrders.sumOf { it.total }
    val periodLabel = when (selectedPeriod) {
        "daily" -> "Daily (Today)"
        "weekly" -> "Weekly (Last 7 Days)"
        "monthly" -> "Monthly (Last 30 Days)"
        else -> "All Time"
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Sales Analytics & Reports", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

                // Period Filter Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = selectedPeriod == "daily",
                        onClick = { selectedPeriod = "daily" },
                        label = { Text("Daily") }
                    )
                    FilterChip(
                        selected = selectedPeriod == "weekly",
                        onClick = { selectedPeriod = "weekly" },
                        label = { Text("Weekly") }
                    )
                    FilterChip(
                        selected = selectedPeriod == "monthly",
                        onClick = { selectedPeriod = "monthly" },
                        label = { Text("Monthly") }
                    )
                    FilterChip(
                        selected = selectedPeriod == "all",
                        onClick = { selectedPeriod = "all" },
                        label = { Text("All Time") }
                    )
                }
            }
        }

        // Summary Cards for Selected Period
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "📊 $periodLabel Sales Summary",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Sales Revenue", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                            Text(
                                money(periodSalesTotal),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Completed Orders", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                            Text(
                                "${periodOrders.size}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Export Excel Button
                    Button(
                        onClick = {
                            isExporting = true
                            scope.launch {
                                try {
                                    val rName = vm.restaurant?.name ?: "Restaurant"
                                    val xlsxFile = generateExcelXlsx(context, rName, periodLabel, periodOrders)
                                    val fileUrl = uploadFileToSupabase(xlsxFile, "reports")

                                    vm.saveSalesReport(
                                        periodType = selectedPeriod,
                                        periodLabel = periodLabel,
                                        totalSales = periodSalesTotal,
                                        totalOrders = periodOrders.size,
                                        fileUrl = fileUrl
                                    )

                                    // Open file / Share Sheet
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        xlsxFile
                                    )
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Open Excel Sales Report"))
                                } catch (e: Exception) {
                                    vm.error = "Export failed: ${e.friendly()}"
                                } finally {
                                    isExporting = false
                                }
                            }
                        },
                        enabled = !isExporting && periodOrders.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Generating Excel...")
                        } else {
                            Text("📥 Export & Save Excel (.xlsx) Report", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Saved Reports in Database Section
        if (vm.salesReports.isNotEmpty()) {
            item { Section("Saved Reports in Database (${vm.salesReports.size})") }
            items(vm.salesReports, key = { "rep-" + it.id }) { rep ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(rep.periodLabel, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("Sales: ${money(rep.totalSales)} • ${rep.totalOrders} Orders", style = MaterialTheme.typography.bodyMedium)
                            rep.createdAt?.let {
                                Text(formatTime(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }

                        if (!rep.fileUrl.isNullOrBlank()) {
                            IconButton(onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(rep.fileUrl))
                                context.startActivity(intent)
                            }) {
                                Icon(Ic.Download, "Download Excel", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }

        // Order History list
        item { Section("Completed Order History (${periodOrders.size})") }
        if (periodOrders.isEmpty()) {
            item { EmptyState("No finished orders for this period.") }
        }

        items(periodOrders, key = { "h-" + it.id }) { o ->
            Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${shortId(o.id)}  ${o.customerName ?: "Customer"}", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        StatusChip(o.status)
                    }
                    Text(o.items.joinToString { "${it.quantity}× ${it.itemName}" }, style = MaterialTheme.typography.bodySmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(formatTime(o.createdAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        Text(money(o.total), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

// =====================================================================
//  Tab 4: restaurant profile
// =====================================================================
@Composable
private fun RestaurantProfileTab(vm: RestaurantViewModel, auth: AuthViewModel) {
    val r = vm.restaurant ?: return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isEditing by remember { mutableStateOf(false) }

    var name by remember(r.id) { mutableStateOf(r.name) }
    var desc by remember(r.id) { mutableStateOf(r.description.orEmpty()) }
    var email by remember(r.id) { mutableStateOf(r.email.orEmpty()) }
    var phone by remember(r.id) { mutableStateOf(r.phone.orEmpty()) }
    var imageUrl by remember(r.id) { mutableStateOf(r.imageUrl.orEmpty()) }
    var coverImageUrl by remember(r.id) { mutableStateOf(r.coverImageUrl.orEmpty()) }
    var isUploadingLogo by remember { mutableStateOf(false) }
    var isUploadingCover by remember { mutableStateOf(false) }

    var address by remember(r.id) { mutableStateOf(r.address.orEmpty()) }
    var city by remember(r.id) { mutableStateOf(r.city.orEmpty()) }
    var district by remember(r.id) { mutableStateOf(r.district.orEmpty()) }
    var postalCode by remember(r.id) { mutableStateOf(r.postalCode.orEmpty()) }

    var businessRegNo by remember(r.id) { mutableStateOf(r.businessRegistrationNo.orEmpty()) }
    var minOrderAmount by remember(r.id) { mutableStateOf(if (r.minOrderAmount > 0) r.minOrderAmount.toString() else "") }
    var prepTime by remember(r.id) { mutableStateOf(r.estimatedPrepTimeMinutes?.toString().orEmpty()) }
    var deliveryRadius by remember(r.id) { mutableStateOf(r.deliveryRadiusKm?.toString().orEmpty()) }
    var open by remember(r.id) { mutableStateOf(r.isOpen) }

    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            isUploadingLogo = true
            scope.launch {
                try {
                    val url = uploadImageToSupabase(context, it, "logos")
                    imageUrl = url
                    vm.updateRestaurant(
                        name = name, description = desc, address = address, phone = phone, email = email,
                        imageUrl = url, coverImageUrl = coverImageUrl, city = city, district = district,
                        postalCode = postalCode, businessRegistrationNo = businessRegNo,
                        minOrderAmount = minOrderAmount.toDoubleOrNull() ?: 0.0,
                        estimatedPrepTimeMinutes = prepTime.toIntOrNull(),
                        deliveryRadiusKm = deliveryRadius.toDoubleOrNull(), isOpen = open
                    )
                } catch (e: Exception) {
                    vm.error = "Upload failed: ${e.friendly()}"
                } finally {
                    isUploadingLogo = false
                }
            }
        }
    }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            isUploadingCover = true
            scope.launch {
                try {
                    val url = uploadImageToSupabase(context, it, "covers")
                    coverImageUrl = url
                    vm.updateRestaurant(
                        name = name, description = desc, address = address, phone = phone, email = email,
                        imageUrl = imageUrl, coverImageUrl = url, city = city, district = district,
                        postalCode = postalCode, businessRegistrationNo = businessRegNo,
                        minOrderAmount = minOrderAmount.toDoubleOrNull() ?: 0.0,
                        estimatedPrepTimeMinutes = prepTime.toIntOrNull(),
                        deliveryRadiusKm = deliveryRadius.toDoubleOrNull(), isOpen = open
                    )
                } catch (e: Exception) {
                    vm.error = "Upload failed: ${e.friendly()}"
                } finally {
                    isUploadingCover = false
                }
            }
        }
    }

    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Restaurant Profile", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(Repo.email(), color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodySmall)
            }

            if (!isEditing) {
                Button(
                    onClick = { isEditing = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Ic.Edit, "Edit", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Edit Profile")
                }
            }
        }

        // Approval Banner
        RestaurantApprovalBanner(r)

        // Top Header Card with Cover and Logo
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                    val cUrl = sanitizeImageUrl(coverImageUrl)
                    if (!cUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = cUrl,
                            contentDescription = "Cover",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.horizontalGradient(listOf(Color(0xFF800000), Color(0xFFD32F2F)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(name.ifBlank { "Restaurant" }, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Logo Circle Badge
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 16.dp)
                            .offset(y = 24.dp)
                            .size(68.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            val lUrl = sanitizeImageUrl(imageUrl)
                            if (!lUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = lUrl,
                                    contentDescription = "Logo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            } else {
                                Text(
                                    name.take(1).uppercase().ifEmpty { "R" },
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))

                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(name.ifBlank { "My Restaurant" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Surface(
                            color = if (open) Color(0xFFD4F4DD) else Color(0xFFFFD6D6),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                if (open) "Open" else "Closed",
                                color = if (open) Color(0xFF14692B) else Color(0xFF8F1111),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (desc.isNotBlank()) {
                        Text(desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Accepting orders", fontWeight = FontWeight.SemiBold)
                            Text("Turn off when closed or too busy", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(
                            checked = open,
                            onCheckedChange = { newOpen ->
                                open = newOpen
                                vm.updateRestaurant(
                                    name = name, description = desc, address = address, phone = phone, email = email,
                                    imageUrl = imageUrl, coverImageUrl = coverImageUrl, city = city, district = district,
                                    postalCode = postalCode, businessRegistrationNo = businessRegNo,
                                    minOrderAmount = minOrderAmount.toDoubleOrNull() ?: 0.0,
                                    estimatedPrepTimeMinutes = prepTime.toIntOrNull(),
                                    deliveryRadiusKm = deliveryRadius.toDoubleOrNull(),
                                    isOpen = newOpen,
                                    reSubmitApproval = false
                                )
                            }
                        )
                    }
                }
            }
        }

        // VIEW MODE vs EDIT MODE
        if (!isEditing) {
            // Read-Only Cards
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Basic Information", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    HorizontalDivider()
                    ProfileDetailRow("Business Email", email.ifBlank { "(not set)" })
                    ProfileDetailRow("Phone Number", phone.ifBlank { "(not set)" })
                    ProfileDetailRow("Description", desc.ifBlank { "(not set)" })
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Location & Address", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    HorizontalDivider()
                    ProfileDetailRow("Street Address", address.ifBlank { "(not set)" })
                    ProfileDetailRow("City / District", listOfNotNull(city.ifBlank { null }, district.ifBlank { null }).joinToString(", ").ifEmpty { "(not set)" })
                    ProfileDetailRow("Postal Code", postalCode.ifBlank { "(not set)" })
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Business & Operations", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    HorizontalDivider()
                    ProfileDetailRow("Business Reg No.", businessRegNo.ifBlank { "(not set)" })
                    ProfileDetailRow("Min Order Amount", if (minOrderAmount.isNotBlank()) "Rs. $minOrderAmount" else "(not set)")
                    ProfileDetailRow("Est. Prep Time", if (prepTime.isNotBlank()) "$prepTime mins" else "(not set)")
                    ProfileDetailRow("Delivery Radius", if (deliveryRadius.isNotBlank()) "$deliveryRadius km" else "(not set)")
                }
            }
        } else {
            // Edit Mode Form
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Change Images", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isUploadingLogo) {
                                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                } else if (imageUrl.isNotBlank()) {
                                    AsyncImage(model = sanitizeImageUrl(imageUrl), contentDescription = "Logo", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                } else {
                                    Icon(Ic.Store, null, tint = MaterialTheme.colorScheme.outline)
                                }
                            }
                            TextButton(onClick = { logoPicker.launch("image/*") }, enabled = !isUploadingLogo) {
                                Text(if (isUploadingLogo) "Uploading..." else if (imageUrl.isBlank()) "Add Logo" else "Change Logo")
                            }
                        }
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isUploadingCover) {
                                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                } else if (coverImageUrl.isNotBlank()) {
                                    AsyncImage(model = sanitizeImageUrl(coverImageUrl), contentDescription = "Cover", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                } else {
                                    Text("Cover Photo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                            TextButton(onClick = { coverPicker.launch("image/*") }, enabled = !isUploadingCover) {
                                Text(if (isUploadingCover) "Uploading..." else if (coverImageUrl.isBlank()) "Add Cover" else "Change Cover")
                            }
                        }
                    }

                    Section("Basic Information")
                    OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(email, { email = it }, label = { Text("Business Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(phone, { phone = it }, label = { Text("Phone") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(desc, { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())

                    Section("Location")
                    OutlinedTextField(address, { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(city, { city = it }, label = { Text("City") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(district, { district = it }, label = { Text("District") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(postalCode, { postalCode = it }, label = { Text("Postal Code") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    Section("Business & Operations")
                    OutlinedTextField(businessRegNo, { businessRegNo = it }, label = { Text("Business Registration No.") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(minOrderAmount, { minOrderAmount = it }, label = { Text("Min Order (Rs.)") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                        OutlinedTextField(prepTime, { prepTime = it }, label = { Text("Est. Prep (min)") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(deliveryRadius, { deliveryRadius = it }, label = { Text("Delivery Radius (km)") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                name = r.name
                                desc = r.description.orEmpty()
                                address = r.address.orEmpty()
                                phone = r.phone.orEmpty()
                                email = r.email.orEmpty()
                                city = r.city.orEmpty()
                                district = r.district.orEmpty()
                                postalCode = r.postalCode.orEmpty()
                                businessRegNo = r.businessRegistrationNo.orEmpty()
                                minOrderAmount = if (r.minOrderAmount > 0) r.minOrderAmount.toString() else ""
                                prepTime = r.estimatedPrepTimeMinutes?.toString().orEmpty()
                                deliveryRadius = r.deliveryRadiusKm?.toString().orEmpty()
                                isEditing = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                vm.updateRestaurant(
                                    name = name,
                                    description = desc,
                                    address = address,
                                    phone = phone,
                                    email = email,
                                    imageUrl = imageUrl,
                                    coverImageUrl = coverImageUrl,
                                    city = city,
                                    district = district,
                                    postalCode = postalCode,
                                    businessRegistrationNo = businessRegNo,
                                    minOrderAmount = minOrderAmount.toDoubleOrNull() ?: 0.0,
                                    estimatedPrepTimeMinutes = prepTime.toIntOrNull(),
                                    deliveryRadiusKm = deliveryRadius.toDoubleOrNull(),
                                    isOpen = open,
                                    reSubmitApproval = true
                                )
                                isEditing = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save & Submit")
                        }
                    }
                }
            }
        }

        OutlinedButton(onClick = { auth.logout() }, modifier = Modifier.fillMaxWidth()) {
            Icon(Ic.Logout, null)
            Spacer(Modifier.padding(horizontal = 4.dp))
            Text("Log out")
        }
    }
}

@Composable
private fun ProfileDetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}
