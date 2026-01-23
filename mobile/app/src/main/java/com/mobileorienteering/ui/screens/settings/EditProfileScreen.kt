package com.mobileorienteering.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobileorienteering.R
import com.mobileorienteering.ui.screens.auth.components.AuthTextField
import com.mobileorienteering.ui.screens.auth.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: UserViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val user by viewModel.currentUser.collectAsState()
    val isLoading by remember { viewModel.isLoading }
    val error by remember { viewModel.error }

    var username by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var hasChanges by remember { mutableStateOf(false) }

    LaunchedEffect(user) {
        user?.let {
            username = it.username
            fullName = it.fullName ?: ""
            email = it.email
            phoneNumber = it.phoneNumber ?: ""
        }
    }

    LaunchedEffect(username, fullName, email, phoneNumber) {
        user?.let { currentUser ->
            hasChanges = username != currentUser.username ||
                    fullName != (currentUser.fullName ?: "") ||
                    email != currentUser.email ||
                    phoneNumber != (currentUser.phoneNumber ?: "")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_left),
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
                }
            )
        }
    ) { paddingValues ->
        if (user == null && !isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Update your profile information",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                AuthTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = "Username",
                    leadingIconRes = R.drawable.ic_person_otlined,
                    imeAction = ImeAction.Next,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                AuthTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = "Full Name",
                    leadingIconRes = R.drawable.ic_badge_outlined,
                    imeAction = ImeAction.Next,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                AuthTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email",
                    leadingIconRes = R.drawable.ic_mail_outlined,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                AuthTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = "Phone Number",
                    leadingIconRes = R.drawable.ic_phone_outlined,
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = {
                        viewModel.updateProfile(
                            username = username,
                            fullName = fullName.ifBlank { null },
                            email = email,
                            phoneNumber = phoneNumber.ifBlank { null }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading && hasChanges
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}
