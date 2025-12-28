package com.mobileorienteering.ui.screen.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.mobileorienteering.R
import com.mobileorienteering.ui.component.AuthPasswordField
import com.mobileorienteering.ui.component.AuthTextField
import com.mobileorienteering.ui.core.AppScreen
import com.mobileorienteering.util.GoogleSignInHelper
import kotlinx.coroutines.launch
import com.mobileorienteering.BuildConfig

@Composable
fun LoginScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val username by authViewModel.username
    val password by authViewModel.password
    val error by authViewModel.error
    val isLoading by authViewModel.isLoading
    val isGoogleSignInLoading by authViewModel.isGoogleSignInLoading
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val googleSignInHelper = remember { GoogleSignInHelper(context) }
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        scope.launch {
            val idToken = googleSignInHelper.getSignInResultFromIntent(result.data)
            if (idToken != null) {
                authViewModel.loginWithGoogle(idToken)
            } else {
                authViewModel.error.value = "Google Sign-In was cancelled or failed"
            }
        }
    }

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
            Spacer(Modifier.height(90.dp))
            Text(
                text = "Log In",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(30.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AuthTextField(
                    value = username,
                    onValueChange = {
                        authViewModel.username.value = it
                        if (error != null) authViewModel.clearError()
                    },
                    label = "Username",
                    leadingIconRes = R.drawable.ic_person_otlined,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                    enabled = !isLoading && !isGoogleSignInLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                AuthPasswordField(
                    value = password,
                    onValueChange = {
                        authViewModel.password.value = it
                        if (error != null) authViewModel.clearError()
                    },
                    label = "Password",
                    leadingIconRes = R.drawable.ic_lock_outlined,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                    onImeAction = {
                        focusManager.clearFocus()
                        authViewModel.login()
                    },
                    enabled = !isLoading && !isGoogleSignInLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { authViewModel.login() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isLoading && !isGoogleSignInLoading && username.isNotBlank() && password.isNotBlank()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            "Log in",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                error?.let { message ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        text = "OR",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
                            val intentSender = googleSignInHelper.beginSignIn(webClientId)
                            if (intentSender != null) {
                                googleSignInLauncher.launch(
                                    IntentSenderRequest.Builder(intentSender).build()
                                )
                            } else {
                                authViewModel.error.value = "Failed to start Google Sign-In"
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isLoading && !isGoogleSignInLoading
                ) {
                    if (isGoogleSignInLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_google),
                                contentDescription = "Google logo",
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Sign in with Google",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
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
                    text = "Don't have an account?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = { navController.navigate(AppScreen.Register.route) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isLoading && !isGoogleSignInLoading
                ) {
                    Text(
                        "Register",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
