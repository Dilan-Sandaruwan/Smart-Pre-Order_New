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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.smartpreorder.app.data.MenuItem
import com.smartpreorder.app.data.PortionSize
import com.smartpreorder.app.ui.ConfirmDialog
import com.smartpreorder.app.ui.EmptyState
import com.smartpreorder.app.ui.Ic
import com.smartpreorder.app.ui.RestaurantViewModel
import com.smartpreorder.app.ui.Section
import com.smartpreorder.app.ui.friendly
import com.smartpreorder.app.ui.money
import com.smartpreorder.app.ui.uploadImageToSupabase
import kotlinx.coroutines.launch

/** Menu Management tab for restaurant owners. */
@Composable
fun MenuTab(vm: RestaurantViewModel) {
    var editing by remember { mutableStateOf<MenuItem?>(null) }
    var adding by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<MenuItem?>(null) }
    val catName = vm.categories.associate { it.id to it.name }
    val grouped = vm.menu.groupBy { m ->
        m.categoryName.takeIf { !it.isNullOrBlank() }
            ?: catName[m.categoryId]
            ?: "Uncategorised"
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Text("Menu Management", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
            item { RestaurantApprovalBanner(vm.restaurant) }
            if (vm.menu.isEmpty()) item { EmptyState("Your menu is empty.\nTap + to add your first dish.") }

            grouped.forEach { (cat, list) ->
                item { Section(cat) }
                items(list, key = { it.id }) { m ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!m.imageUrl.isNullOrBlank()) {
                                Box(
                                    Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = m.imageUrl,
                                        contentDescription = m.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                            }

                            Column(Modifier.weight(1f)) {
                                Text(m.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                                if (!m.description.isNullOrBlank()) {
                                    Text(m.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                }
                                Spacer(Modifier.height(4.dp))

                                if (m.portionSizes.isNotEmpty()) {
                                    Text(
                                        m.portionSizes.joinToString(" | ") { "${it.name} - ${money(it.price)}" },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Text(money(m.price), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }

                            Switch(checked = m.isAvailable, onCheckedChange = { vm.toggleAvailable(m, it) })
                            IconButton(onClick = { editing = m }) { Icon(Ic.Edit, "Edit") }
                            IconButton(onClick = { deleteTarget = m }) { Icon(Ic.Delete, "Delete") }
                        }
                    }
                }
            }
            item { Spacer(Modifier.padding(40.dp)) }
        }

        FloatingActionButton(
            onClick = {
                if (vm.restaurant?.approvalStatus == "approved") {
                    adding = true
                } else {
                    vm.error = "Menu creation is disabled until your restaurant is approved by an administrator."
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) { Icon(Ic.Add, "Add item") }
    }

    if (adding) MenuItemDialog(vm, null) { adding = false }
    editing?.let { m -> MenuItemDialog(vm, m) { editing = null } }
    deleteTarget?.let { m ->
        ConfirmDialog(
            title = "Delete ${m.name}?",
            text = "This removes it from your menu. Old orders are not affected.",
            confirmLabel = "Delete",
            onConfirm = { vm.deleteItem(m) },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
private fun MenuItemDialog(vm: RestaurantViewModel, item: MenuItem?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(item?.name.orEmpty()) }
    var desc by remember { mutableStateOf(item?.description.orEmpty()) }
    var price by remember { mutableStateOf(if ((item?.price ?: 0.0) > 0) item?.price.toString() else "") }
    var imageUrl by remember { mutableStateOf(item?.imageUrl.orEmpty()) }
    var isUploadingDish by remember { mutableStateOf(false) }

    var categoryId by remember { mutableStateOf(item?.categoryId) }
    var categoryName by remember { mutableStateOf(item?.categoryName.orEmpty()) }
    var available by remember { mutableStateOf(item?.isAvailable ?: true) }
    var menuOpen by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<String?>(null) }

    // Portion sizes state
    val portionList = remember {
        mutableStateListOf<PortionSize>().apply {
            if (item != null && item.portionSizes.isNotEmpty()) {
                addAll(item.portionSizes)
            }
        }
    }
    var portionNameInput by remember { mutableStateOf("") }
    var portionPriceInput by remember { mutableStateOf("") }

    val dishPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            isUploadingDish = true
            scope.launch {
                try {
                    imageUrl = uploadImageToSupabase(context, it, "dishes")
                } catch (e: Exception) {
                    problem = "Upload failed: ${e.friendly()}"
                } finally {
                    isUploadingDish = false
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "Add item" else "Edit item") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Dish Photo Upload
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isUploadingDish) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else if (imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Dish photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Ic.Menu, null, tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                    TextButton(onClick = { dishPicker.launch("image/*") }, enabled = !isUploadingDish) {
                        Text(if (isUploadingDish) "Uploading..." else if (imageUrl.isBlank()) "Add Photo" else "Change Photo")
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Base / Regular Price (Rs.)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Selection
                Box {
                    OutlinedButton(onClick = { menuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                        val currentName = categoryName.ifBlank { vm.categories.firstOrNull { it.id == categoryId }?.name }
                        Text(currentName?.let { "Category: $it" } ?: "Category: none")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("None") },
                            onClick = {
                                categoryId = null
                                categoryName = ""
                                menuOpen = false
                            }
                        )
                        vm.categories.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c.name) },
                                onClick = {
                                    categoryId = c.id
                                    categoryName = c.name
                                    menuOpen = false
                                }
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Portion Sizes Section
                Text("Portion Sizes (Optional)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Add custom portions with prices (e.g., Small - Rs.590 / Medium - Rs.990)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                if (portionList.isNotEmpty()) {
                    portionList.forEachIndexed { index, p ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${p.name} - ${money(p.price)}", fontWeight = FontWeight.SemiBold)
                                IconButton(onClick = { portionList.removeAt(index) }) {
                                    Icon(Ic.Delete, "Remove portion")
                                }
                            }
                        }
                    }
                }

                // Inputs to add new portion size
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = portionNameInput,
                        onValueChange = { portionNameInput = it },
                        label = { Text("Portion (e.g. Small)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = portionPriceInput,
                        onValueChange = { portionPriceInput = it },
                        label = { Text("Price (Rs.)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                Button(
                    onClick = {
                        val pVal = portionPriceInput.toDoubleOrNull()
                        if (portionNameInput.isNotBlank() && pVal != null && pVal >= 0) {
                            portionList.add(PortionSize(portionNameInput.trim(), pVal))
                            portionNameInput = ""
                            portionPriceInput = ""
                        }
                    },
                    enabled = portionNameInput.isNotBlank() && portionPriceInput.toDoubleOrNull() != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("+ Add Portion Size")
                }

                HorizontalDivider()

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Available", modifier = Modifier.weight(1f))
                    Switch(checked = available, onCheckedChange = { available = it })
                }

                problem?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val baseP = price.toDoubleOrNull() ?: portionList.firstOrNull()?.price ?: 0.0
                when {
                    name.isBlank() -> problem = "Enter a name"
                    baseP < 0 -> problem = "Enter a valid price"
                    else -> {
                        val selCatName = categoryName.ifBlank { vm.categories.firstOrNull { it.id == categoryId }?.name }
                        vm.saveItem(
                            id = item?.id,
                            categoryId = categoryId,
                            categoryName = selCatName,
                            name = name,
                            description = desc,
                            price = baseP,
                            portionSizes = portionList.toList(),
                            available = available,
                            imageUrl = imageUrl
                        )
                        onDismiss()
                    }
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
