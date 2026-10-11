@file:OptIn(ExperimentalMaterial3Api::class)

package com.smartpreorder.app.ui.restaurant

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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
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
    val today = vm.todayCompleted
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item { Text("Sales", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Today's orders", "${today.size}", Modifier.weight(1f))
                StatCard("Today's sales", money(today.sumOf { it.total }), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Completed (all)", "${vm.completed.size}", Modifier.weight(1f))
                StatCard("Total sales", money(vm.totalSales), Modifier.weight(1f))
            }
        }
        item { Section("Order history") }
        if (vm.historyOrders.isEmpty()) item { EmptyState("No finished orders yet.") }
        items(vm.historyOrders, key = { "h-" + it.id }) { o ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${shortId(o.id)}  ${o.customerName ?: ""}", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        StatusChip(o.status)
                    }
                    Text(o.items.joinToString { "${it.quantity}× ${it.itemName}" }, style = MaterialTheme.typography.bodySmall)
                    Row {
                        Text(formatTime(o.createdAt), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline, modifier = Modifier.weight(1f))
                        Text(money(o.total), fontWeight = FontWeight.Bold)
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Restaurant profile", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(Repo.email(), color = MaterialTheme.colorScheme.outline)
        RestaurantApprovalBanner(r)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Accepting orders", fontWeight = FontWeight.SemiBold)
                Text("Turn off when you're closed or too busy", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline)
            }
            Switch(checked = open, onCheckedChange = { open = it })
        }

        Section("Restaurant Images")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (isUploadingLogo) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    } else if (imageUrl.isNotBlank()) {
                        AsyncImage(model = imageUrl, contentDescription = "Logo", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
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
                    Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (isUploadingCover) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    } else if (coverImageUrl.isNotBlank()) {
                        AsyncImage(model = coverImageUrl, contentDescription = "Cover", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
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
                    isOpen = open
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save") }

        OutlinedButton(onClick = { auth.logout() }, modifier = Modifier.fillMaxWidth()) {
            Icon(Ic.Logout, null)
            Spacer(Modifier.padding(horizontal = 4.dp))
            Text("Log out")
        }
    }
}
