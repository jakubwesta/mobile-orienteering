package com.mobileorienteering.ui.screen.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.mobileorienteering.ui.navigation.AppScreen

@Composable
fun RegisterScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val username by authViewModel.username
    val email by authViewModel.email
    val password by authViewModel.password
    val fullName by authViewModel.fullName
    val phoneNumber by authViewModel.phoneNumber
    val error by authViewModel.error
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) navController.navigate(AppScreen.Map.route) {
            popUpTo(0) { inclusive = true }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Register", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { authViewModel.username.value = it },
            label = { Text("Username") }
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { authViewModel.email.value = it },
            label = { Text("Email") }
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { authViewModel.password.value = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = fullName ?: "",
            onValueChange = { authViewModel.fullName.value = it },
            label = { Text("Full Name (optional)") }
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = phoneNumber ?: "",
            onValueChange = { authViewModel.phoneNumber.value = it },
            label = { Text("Phone Number (optional)") }
        )

        Spacer(Modifier.height(16.dp))

        Button(onClick = { authViewModel.register() }) {
            Text("Register")
        }

        Spacer(Modifier.height(8.dp))

        TextButton(onClick = { navController.navigate(AppScreen.Login.route) }) {
            Text("Already have an account? Login")
        }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
