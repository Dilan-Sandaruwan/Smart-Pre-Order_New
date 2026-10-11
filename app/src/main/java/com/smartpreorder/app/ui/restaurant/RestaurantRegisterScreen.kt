@file:OptIn(ExperimentalMaterial3Api::class)

package com.smartpreorder.app.ui.restaurant

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.smartpreorder.app.data.Repo
import com.smartpreorder.app.ui.AuthViewModel
import com.smartpreorder.app.ui.Ic
import com.smartpreorder.app.ui.Notices
import com.smartpreorder.app.ui.RestaurantViewModel
import com.smartpreorder.app.ui.Section
import com.smartpreorder.app.ui.friendly
import com.smartpreorder.app.ui.uploadImageToSupabase

/** Registration / initial setup screen for a new restaurant owner. */
@Composable
fun RestaurantRegisterScreen(vm: RestaurantViewModel, auth: AuthViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(Repo.email()) }
    var phone by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var coverImageUrl by remember { mutableStateOf("") }
    var isUploadingLogo by remember { mutableStateOf(false) }
    var isUploadingCover by remember { mutableStateOf(false) }

    var city by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var postalCode by remember { mutableStateOf("") }

    var businessRegNo by remember { mutableStateOf("") }
    var minOrderAmount by remember { mutableStateOf("") }
    var prepTime by remember { mutableStateOf("") }
    var deliveryRadius by remember { mutableStateOf("") }

    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            isUploadingLogo = true
            scope.launch {
                try {
                    imageUrl = uploadImageToSupabase(context, it, "logos")
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
                    coverImageUrl = uploadImageToSupabase(context, it, "covers")
                } catch (e: Exception) {
                    vm.error = "Upload failed: ${e.friendly()}"
                } finally {
                    isUploadingCover = false
                }
            }
        }
    }

    val snack = remember { SnackbarHostState() }
    Notices(vm, snack)

    Scaffold(snackbarHost = { SnackbarHost(snack) }) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Set up your restaurant", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Enter your restaurant details to get started. You can update these anytime.", color = MaterialTheme.colorScheme.outline)

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
            OutlinedTextField(name, { name = it }, label = { Text("Restaurant Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(email, { email = it }, label = { Text("Business Email") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(phone, { phone = it }, label = { Text("Phone Number") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(desc, { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())

            Section("Location Details")
            OutlinedTextField(address, { address = it }, label = { Text("Street Address") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(city, { city = it }, label = { Text("City") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(district, { district = it }, label = { Text("District") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(postalCode, { postalCode = it }, label = { Text("Postal Code") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

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

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    vm.createRestaurant(
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
                        deliveryRadiusKm = deliveryRadius.toDoubleOrNull()
                    )
                },
                enabled = !vm.busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (vm.busy) "Creating..." else "Create restaurant") }
            TextButton(onClick = { auth.logout() }, modifier = Modifier.fillMaxWidth()) { Text("Log out") }
        }
    }
}
