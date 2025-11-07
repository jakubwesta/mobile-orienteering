package com.mobileorienteering.ui.screen.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.mobileorienteering.R
import com.mobileorienteering.ui.component.AuthPasswordField
import com.mobileorienteering.ui.component.AuthTextField
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
    val isLoading by authViewModel.isLoading
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

    val focusManager = LocalFocusManager.current

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
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
                    imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                AuthTextField(
                    value = email,
                    onValueChange = { authViewModel.email.value = it },
                    label = "Email",
                    leadingIconRes = R.drawable.ic_mail_outlined,
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Email,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                AuthPasswordField(
                    value = password,
                    onValueChange = { authViewModel.password.value = it },
                    label = "Password",
                    leadingIconRes = R.drawable.ic_lock_outlined,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                    onImeAction = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                AuthTextField(
                    value = fullName ?: "",
                    onValueChange = { authViewModel.fullName.value = it.ifEmpty { null } },
                    label = "Full Name (Optional)",
                    leadingIconRes = R.drawable.ic_badge_outlined,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                AuthTextField(
                    value = phoneNumber ?: "",
                    onValueChange = { authViewModel.phoneNumber.value = it.ifEmpty { null } },
                    label = "Phone Number (Optional)",
                    leadingIconRes = R.drawable.ic_phone_outlined,
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Done,
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
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                error?.let { message ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
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
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
