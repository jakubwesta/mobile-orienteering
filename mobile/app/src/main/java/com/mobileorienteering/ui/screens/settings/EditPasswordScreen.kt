package com.mobileorienteering.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobileorienteering.R
import com.mobileorienteering.ui.core.Strings
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
    val context = LocalContext.current

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Strings.Auth.changePassword) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painterResource(id = R.drawable.ic_arrow_left),
                            contentDescription = Strings.Action.back
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
                Strings.Settings.changePasswordDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AuthPasswordField(
                value = currentPassword,
                onValueChange = { currentPassword = it },
                label = Strings.Auth.currentPassword,
                leadingIconRes = R.drawable.ic_lock_outlined,
                imeAction = ImeAction.Next,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            AuthPasswordField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = Strings.Auth.newPassword,
                leadingIconRes = R.drawable.ic_lock_filled,
                imeAction = ImeAction.Next,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            AuthPasswordField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = Strings.Auth.confirmNewPassword,
                leadingIconRes = R.drawable.ic_lock_filled,
                imeAction = ImeAction.Done,
                onImeAction = {
                    when {
                        currentPassword.isEmpty() -> {
                            localError = Strings.Error.pleaseEnterCurrentPassword(context)
                        }
                        newPassword.isEmpty() -> {
                            localError = Strings.Error.pleaseEnterNewPassword(context)
                        }
                        newPassword.length < 6 -> {
                            localError = Strings.Error.passwordMinLength(context)
                        }
                        newPassword != confirmPassword -> {
                            localError = Strings.Error.passwordsDoNotMatch(context)
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
                            localError = Strings.Error.pleaseEnterCurrentPassword(context)
                        }
                        newPassword.isEmpty() -> {
                            localError = Strings.Error.pleaseEnterNewPassword(context)
                        }
                        newPassword.length < 6 -> {
                            localError = Strings.Error.passwordMinLength(context)
                        }
                        newPassword != confirmPassword -> {
                            localError = Strings.Error.passwordsDoNotMatch(context)
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
                    Text(Strings.Auth.changePassword)
                }
            }
        }
    }
}
