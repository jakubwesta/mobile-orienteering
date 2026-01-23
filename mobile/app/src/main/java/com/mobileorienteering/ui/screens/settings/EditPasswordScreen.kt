package com.mobileorienteering.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobileorienteering.R
import com.mobileorienteering.ui.screens.auth.components.AuthPasswordField
import com.mobileorienteering.ui.screens.auth.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPasswordScreen(
    viewModel: UserViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val isLoading by remember { viewModel.isLoading }
    val error by remember { viewModel.error }

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Change Password") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painterResource(id = R.drawable.ic_arrow_left),
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = remember { SnackbarHostState() }.apply {
                    LaunchedEffect(error) {
                        error?.let {
                            showSnackbar(it)
                            viewModel.clearError()
                        }
                    }
                    LaunchedEffect(localError) {
                        localError?.let {
                            showSnackbar(it)
                            localError = null
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Enter your current password and choose a new one",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AuthPasswordField(
                value = currentPassword,
                onValueChange = { currentPassword = it },
                label = "Current Password",
                leadingIconRes = R.drawable.ic_lock_outlined,
                imeAction = ImeAction.Next,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            AuthPasswordField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = "New Password",
                leadingIconRes = R.drawable.ic_lock_filled,
                imeAction = ImeAction.Next,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            AuthPasswordField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = "Confirm New Password",
                leadingIconRes = R.drawable.ic_lock_filled,
                imeAction = ImeAction.Done,
                onImeAction = {
                    when {
                        currentPassword.isEmpty() -> {
                            localError = "Please enter your current password"
                        }
                        newPassword.isEmpty() -> {
                            localError = "Please enter a new password"
                        }
                        newPassword.length < 6 -> {
                            localError = "Password must be at least 6 characters"
                        }
                        newPassword != confirmPassword -> {
                            localError = "Passwords do not match"
                        }
                        else -> {
                            viewModel.changePassword(currentPassword, newPassword)
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    when {
                        currentPassword.isEmpty() -> {
                            localError = "Please enter your current password"
                        }
                        newPassword.isEmpty() -> {
                            localError = "Please enter a new password"
                        }
                        newPassword.length < 6 -> {
                            localError = "Password must be at least 6 characters"
                        }
                        newPassword != confirmPassword -> {
                            localError = "Passwords do not match"
                        }
                        else -> {
                            viewModel.changePassword(currentPassword, newPassword)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Change Password")
                }
            }
        }
    }
}
