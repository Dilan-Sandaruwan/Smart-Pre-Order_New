@file:OptIn(ExperimentalMaterial3Api::class)

package com.smartpreorder.app.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.smartpreorder.app.data.Restaurant
import com.smartpreorder.app.ui.AdminViewModel
import com.smartpreorder.app.ui.EmptyState

@Composable
fun RestaurantApprovalScreen(vm: AdminViewModel) {
    var selectedFilter by remember { mutableStateOf("pending") }

    val filteredList = when (selectedFilter) {
        "pending" -> vm.restaurants.filter { it.approvalStatus == "pending" }
        "approved" -> vm.restaurants.filter { it.approvalStatus == "approved" }
        "rejected" -> vm.restaurants.filter { it.approvalStatus == "rejected" }
        else -> vm.restaurants
    }

    val pendingCount = vm.restaurants.count { it.approvalStatus == "pending" }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "pending",
                    onClick = { selectedFilter = "pending" },
                    label = { Text("Pending ($pendingCount)") }
                )
                FilterChip(
                    selected = selectedFilter == "all",
                    onClick = { selectedFilter = "all" },
                    label = { Text("All (${vm.restaurants.size})") }
                )
                FilterChip(
                    selected = selectedFilter == "approved",
                    onClick = { selectedFilter = "approved" },
                    label = { Text("Approved") }
                )
                FilterChip(
                    selected = selectedFilter == "rejected",
                    onClick = { selectedFilter = "rejected" },
                    label = { Text("Rejected") }
                )
            }
        }

        if (filteredList.isEmpty()) {
            item {
                val emptyMsg = when (selectedFilter) {
                    "pending" -> "No pending restaurant approvals."
                    "approved" -> "No approved restaurants yet."
                    "rejected" -> "No rejected restaurants."
                    else -> "No restaurants registered yet."
                }
                EmptyState(emptyMsg)
            }
        }

        items(filteredList, key = { it.id }) { r ->
            RestaurantApprovalCard(r = r, vm = vm)
        }
    }
}

@Composable
private fun RestaurantApprovalCard(r: Restaurant, vm: AdminViewModel) {
    Card(Modifier.fillMaxWidth()) {
        Column {
            if (!r.coverImageUrl.isNullOrBlank() || !r.imageUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                ) {
                    if (!r.coverImageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = r.coverImageUrl,
                            contentDescription = "Cover",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    if (!r.imageUrl.isNullOrBlank()) {
                        Surface(
                            shape = CircleShape,
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 12.dp, bottom = 8.dp)
                                .size(48.dp)
                        ) {
                            AsyncImage(
                                model = r.imageUrl,
                                contentDescription = "Logo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                        }
                    }
                }
            }
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(r.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(
                            r.address ?: "No address",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        r.phone?.let {
                            Text(
                                "Phone: $it",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        r.businessRegistrationNo?.let {
                            Text(
                                "BR No: $it",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    val (statusText, statusBg, statusFg) = when (r.approvalStatus) {
                        "approved" -> Triple("Approved", Color(0x3322C55E), Color(0xFF4ADE80))
                        "rejected" -> Triple("Rejected", Color(0x33EF4444), Color(0xFFFF7A7A))
                        else -> Triple("Pending", Color(0x33FFB300), Color(0xFFFFC857))
                    }
                    Surface(color = statusBg, shape = RoundedCornerShape(50)) {
                        Text(
                            statusText,
                            color = statusFg,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (r.approvalStatus != "approved") {
                            Button(
                                onClick = { vm.approveRestaurant(r, true) }
                            ) {
                                Text("Approve")
                            }
                        }
                        if (r.approvalStatus != "rejected") {
                            OutlinedButton(
                                onClick = { vm.approveRestaurant(r, false) }
                            ) {
                                Text("Reject")
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Active", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(end = 4.dp))
                        Switch(checked = r.isActive, onCheckedChange = { vm.setRestaurantActive(r, it) })
                    }
                }
            }
        }
    }
}
