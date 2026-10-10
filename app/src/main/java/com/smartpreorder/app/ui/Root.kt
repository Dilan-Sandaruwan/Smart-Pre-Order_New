@file:OptIn(ExperimentalMaterial3Api::class)

package com.smartpreorder.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartpreorder.app.ui.admin.AdminApp
import com.smartpreorder.app.ui.customer.CustomerApp
import com.smartpreorder.app.ui.restaurant.RestaurantApp

/** Decides which part of the app to show: login, customer, restaurant or admin. */
@Composable
fun SmartPreOrderRoot() {
    val auth: AuthViewModel = viewModel()
    val profile = auth.profile
    when {
        auth.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        profile == null -> AuthScreen(auth)
        profile.role == "restaurant" -> RestaurantApp(auth, profile)
        profile.role == "admin" -> AdminApp(auth, profile)
        else -> CustomerApp(auth, profile)
    }
}

@Composable
fun AuthScreen(auth: AuthViewModel) {
    var isRegister by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("customer") }
    val snack = remember { SnackbarHostState() }
    Notices(auth, snack)

    Scaffold(snackbarHost = { SnackbarHost(snack) }) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            Text("SmartPreOrder", style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("Order before you arrive. Skip the wait.", textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(12.dp))

            // Clear Log In vs Register Tab Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !isRegister,
                    onClick = { isRegister = false; auth.info = null },
                    label = { Text("Log In", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = isRegister,
                    onClick = { isRegister = true; auth.info = null },
                    label = { Text("Register", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(4.dp))

            if (isRegister) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = role == "customer", onClick = { role = "customer" },
                        label = { Text("Customer") })
                    FilterChip(selected = role == "restaurant", onClick = { role = "restaurant" },
                        label = { Text("Restaurant") })
                    FilterChip(selected = role == "admin", onClick = { role = "admin" },
                        label = { Text("Admin") })
                }
                OutlinedTextField(name, { name = it }, label = { Text("Full name") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(phone, { phone = it }, label = { Text("Phone") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth())
            }
            OutlinedTextField(email, { email = it }, label = { Text("E-mail") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth())
            OutlinedTextField(password, { password = it }, label = { Text("Password") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth())

            auth.info?.let { Text(it, color = MaterialTheme.colorScheme.secondary, textAlign = TextAlign.Center) }

            Button(
                onClick = {
                    if (isRegister) auth.register(email, password, name, phone, role)
                    else auth.login(email, password)
                },
                enabled = !auth.busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (auth.busy) "Please wait..." else if (isRegister) "Create account" else "Log in") }

            TextButton(onClick = { isRegister = !isRegister; auth.info = null }) {
                Text(if (isRegister) "I already have an account" else "New here? Create an account")
            }
        }
    }
}
