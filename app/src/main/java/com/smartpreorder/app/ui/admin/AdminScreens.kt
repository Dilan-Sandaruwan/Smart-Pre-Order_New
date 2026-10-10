@file:OptIn(ExperimentalMaterial3Api::class)

package com.smartpreorder.app.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartpreorder.app.data.Profile
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
import com.smartpreorder.app.ui.money
import com.smartpreorder.app.ui.orderTypeLabel
import com.smartpreorder.app.ui.shortId

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
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
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
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(c.name, modifier = Modifier.weight(1f))
                TextButton(onClick = { vm.deleteCategory(c) }) { Text("Delete") }
            }
        }
    }
}
