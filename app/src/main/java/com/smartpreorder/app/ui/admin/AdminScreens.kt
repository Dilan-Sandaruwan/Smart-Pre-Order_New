@file:OptIn(ExperimentalMaterial3Api::class)

package com.smartpreorder.app.ui.admin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.smartpreorder.app.data.Profile
import com.smartpreorder.app.data.Promotion
import com.smartpreorder.app.ui.AdminViewModel
import com.smartpreorder.app.ui.AuthViewModel
import com.smartpreorder.app.ui.EmptyState
import com.smartpreorder.app.ui.Ic
import com.smartpreorder.app.ui.Notices
import com.smartpreorder.app.ui.Section
import com.smartpreorder.app.ui.SmallTag
import com.smartpreorder.app.ui.StatCard
import com.smartpreorder.app.ui.StatusChip
import com.smartpreorder.app.ui.formatTime
import com.smartpreorder.app.ui.friendly
import com.smartpreorder.app.ui.money
import com.smartpreorder.app.ui.orderTypeLabel
import com.smartpreorder.app.ui.parseColorHex
import com.smartpreorder.app.ui.sanitizeImageUrl
import com.smartpreorder.app.ui.shortId
import com.smartpreorder.app.ui.uploadImageToSupabase
import kotlinx.coroutines.launch

@Composable
fun AdminApp(auth: AuthViewModel, profile: Profile) {
    val vm: AdminViewModel = viewModel(key = "admin-${profile.id}")
    LaunchedEffect(vm) { vm.refresh() }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val snack = remember { SnackbarHostState() }
    Notices(vm, snack)
    Notices(auth, snack)

    val pendingApprovalCount = vm.restaurants.count { it.approvalStatus == "pending" }

    val titles = listOf("Dashboard Overview", "Restaurant Management", "User Management", "Orders & Activity", "Settings & Security")
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titles[tab]) },
                actions = {
                    IconButton(onClick = { vm.refresh() }) { Icon(Ic.Refresh, "Refresh") }
                    IconButton(onClick = { auth.logout() }) { Icon(Ic.Logout, "Log out") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            AdminBottomBar(
                selectedTab = tab,
                onTabSelected = { tab = it },
                pendingApprovalCount = pendingApprovalCount
            )
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> AdminDashboard(vm)
                1 -> RestaurantApprovalScreen(vm)
                2 -> AdminUsers(vm, auth.profile?.id)
                3 -> AdminOrders(vm)
                else -> AdminManage(vm)
            }
        }
    }
}

@Composable
private fun AdminDashboard(vm: AdminViewModel) {
    val s = vm.stats
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Customers", "${s.customers}", Modifier.weight(1f))
                StatCard("Restaurants", "${s.restaurants}", Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Total orders", "${s.orders}", Modifier.weight(1f))
                StatCard("Revenue (completed)", money(s.revenue), Modifier.weight(1f))
            }
        }
        item { Section("Orders by status") }
        val order = listOf("pending", "accepted", "preparing", "ready", "completed", "rejected", "cancelled")
        items(order, key = { it }) { st ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                StatusChip(st)
                Text("  ${s.byStatus[st] ?: 0}", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AdminUsers(vm: AdminViewModel, myId: String?) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(vm.users, key = { it.id }) { u ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(u.fullName.ifBlank { "(no name)" }, fontWeight = FontWeight.Bold)
                        Text(u.phone ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        SmallTag(u.role)
                    }
                    if (u.role != "admin" && u.id != myId) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Blocked", style = MaterialTheme.typography.labelSmall)
                            Switch(checked = u.isBlocked, onCheckedChange = { vm.setBlocked(u, it) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminOrders(vm: AdminViewModel) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (vm.orders.isEmpty()) item { EmptyState("No orders yet.") }
        items(vm.orders, key = { it.id }) { o ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${shortId(o.id)}  ${o.restaurants?.name ?: ""}", fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f))
                        StatusChip(o.status)
                    }
                    Text("${o.customerName ?: "Customer"} • ${orderTypeLabel(o.orderType)}", style = MaterialTheme.typography.bodySmall)
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

@Composable
private fun AdminManage(vm: AdminViewModel) {
    var newCat by remember { mutableStateOf("") }
    var showAddPromo by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Promotions Management Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Section("Promotions & Banners (${vm.promotions.size})")
                Button(onClick = { showAddPromo = true }) {
                    Icon(Ic.Add, "Add", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Banner")
                }
            }
        }

        if (vm.promotions.isEmpty()) {
            item { Text("No active custom promotions.", color = MaterialTheme.colorScheme.outline) }
        }

        items(vm.promotions, key = { "promo-" + it.id }) { promo ->
            val startColor = parseColorHex(promo.startColor, Color(0xFF900C3F))
            val endColor = parseColorHex(promo.endColor, Color(0xFFC70039))
            Card(Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(Brush.horizontalGradient(listOf(startColor, endColor)))
                        .padding(12.dp)
                ) {
                    val bgImage = sanitizeImageUrl(promo.imageUrl)
                    if (!bgImage.isNullOrBlank()) {
                        AsyncImage(
                            model = bgImage,
                            contentDescription = promo.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(0.35f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text((promo.title ?: "Banner").uppercase(), fontWeight = FontWeight.ExtraBold, color = Color.White)
                            promo.discountText?.let { Text(it, fontWeight = FontWeight.Black, color = Color(0xFFFFD700), style = MaterialTheme.typography.titleMedium) }
                            promo.subtext?.let { Text(it, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall) }
                        }
                        IconButton(onClick = { vm.deletePromotion(promo) }) {
                            Icon(Ic.Delete, "Delete", tint = Color.White)
                        }
                    }
                }
            }
        }

        item { Section("System Security & Status") }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Ic.Security, "Security", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp))
                        Text("Security & RLS Policies", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                    Text("• Row Level Security (RLS): Active on all tables", style = MaterialTheme.typography.bodyMedium)
                    Text("• Authentication: Encrypted JWT via Supabase", style = MaterialTheme.typography.bodyMedium)
                    Text("• Admin Controls: User blocking & Restaurant verification", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        item { Section("Reported content (${vm.reports.size})") }
        if (vm.reports.isEmpty()) item { Text("No open reports.", color = MaterialTheme.colorScheme.outline) }
        items(vm.reports, key = { "r-" + it.id }) { r ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(r.restaurants?.name ?: "Restaurant", fontWeight = FontWeight.Bold)
                    Text(r.reason)
                    Text(formatTime(r.createdAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    OutlinedButton(onClick = { vm.resolveReport(r) }) { Text("Mark resolved") }
                }
            }
        }

        item { Section("Food categories") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(newCat, { newCat = it }, label = { Text("New category") }, singleLine = true,
                    modifier = Modifier.weight(1f))
                Button(onClick = { vm.addCategory(newCat); newCat = "" }) { Text("Add") }
            }
        }
        items(vm.categories, key = { "c-" + it.id }) { c ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                val catImg = sanitizeImageUrl(c.imageUrl)
                if (!catImg.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = catImg,
                            contentDescription = c.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }
                Text(c.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { vm.deleteCategory(c) }) { Text("Delete") }
            }
        }
    }

    if (showAddPromo) {
        AddPromotionDialog(vm = vm, onDismiss = { showAddPromo = false })
    }
}

@Composable
private fun AddPromotionDialog(
    vm: AdminViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var titleInput by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var isUploadingImage by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            isUploadingImage = true
            scope.launch {
                try {
                    imageUrl = uploadImageToSupabase(context, it, "promotions")
                } catch (e: Exception) {
                    vm.error = "Upload failed: ${e.friendly()}"
                } finally {
                    isUploadingImage = false
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Promotion Banner", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Select and upload a promotional banner image to display on the Customer App.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )

                // Large Banner Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (isUploadingImage) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                            Text("Uploading banner image...", style = MaterialTheme.typography.bodySmall)
                        }
                    } else if (imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = sanitizeImageUrl(imageUrl),
                            contentDescription = "Banner Preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Ic.Store, "Upload Banner", modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.outline)
                            Text("No banner image selected", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }

                Button(
                    onClick = { imagePicker.launch("image/*") },
                    enabled = !isUploadingImage,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isUploadingImage) "Uploading..." else if (imageUrl.isBlank()) "Select Banner Image *" else "Change Banner Image")
                }

                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("Banner Name / Title (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    vm.addPromotion(
                        imageUrl = imageUrl,
                        title = titleInput.ifBlank { "Promotion Banner" }
                    )
                    onDismiss()
                },
                enabled = imageUrl.isNotBlank() && !isUploadingImage
            ) {
                Text("Save Banner")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
