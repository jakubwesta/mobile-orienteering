package com.mobileorienteering.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.mobileorienteering.R
import com.mobileorienteering.ui.screens.auth.components.AuthPasswordField
import com.mobileorienteering.ui.screens.auth.components.AuthTextField
import com.mobileorienteering.ui.core.AppScreen

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
    val isLoading by authViewModel.isLoading
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

    val focusManager = LocalFocusManager.current

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn == true) {
            navController.navigate(AppScreen.Map.route) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))
            Text(
                text = "Register",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(32.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AuthTextField(
                    value = username,
                    onValueChange = { authViewModel.username.value = it },
                    label = "Username",
                    leadingIconRes = R.drawable.ic_person_otlined,
                    imeAction = ImeAction.Next,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                AuthTextField(
                    value = email,
                    onValueChange = { authViewModel.email.value = it },
                    label = "Email",
                    leadingIconRes = R.drawable.ic_mail_outlined,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                AuthPasswordField(
                    value = password,
                    onValueChange = { authViewModel.password.value = it },
                    label = "Password",
                    leadingIconRes = R.drawable.ic_lock_outlined,
                    imeAction = ImeAction.Next,
                    onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                AuthTextField(
                    value = fullName ?: "",
                    onValueChange = { authViewModel.fullName.value = it.ifEmpty { null } },
                    label = "Full Name (Optional)",
                    leadingIconRes = R.drawable.ic_badge_outlined,
                    imeAction = ImeAction.Next,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                AuthTextField(
                    value = phoneNumber ?: "",
                    onValueChange = { authViewModel.phoneNumber.value = it.ifEmpty { null } },
                    label = "Phone Number (Optional)",
                    leadingIconRes = R.drawable.ic_phone_outlined,
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done,
                    onImeAction = {
                        focusManager.clearFocus()
                        authViewModel.register()
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { authViewModel.register() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isLoading && username.isNotBlank() && email.isNotBlank() && password.isNotBlank()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            "Create account",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Already have an account?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = { navController.navigate(AppScreen.Login.route) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isLoading
                ) {
                    Text(
                        "Log in",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
