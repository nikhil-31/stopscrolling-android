package com.example.stopscrolling_android.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun AccountScreen(viewModel: AuthViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val mfaChallenge by viewModel.mfaChallenge.collectAsState()
    val formMode by viewModel.formMode.collectAsState()
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val confirmPassword by viewModel.confirmPassword.collectAsState()
    val phoneNumber by viewModel.phoneNumber.collectAsState()
    val mfaCode by viewModel.mfaCode.collectAsState()
    val backupCode by viewModel.backupCode.collectAsState()
    val useBackupCode by viewModel.useBackupCode.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )

                Text(
                    text = when {
                        currentUser != null -> "Welcome Back"
                        mfaChallenge != null -> "Verify Identity"
                        formMode == AuthFormMode.SIGN_IN -> "Sign In"
                        else -> "Create Account"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

                when {
                    currentUser != null -> SignedInContent(
                        user = currentUser!!,
                        onLogout = viewModel::logout
                    )
                    mfaChallenge != null -> MfaContent(
                        method = mfaChallenge!!.mfaMethod,
                        message = mfaChallenge!!.message,
                        mfaCode = mfaCode,
                        backupCode = backupCode,
                        useBackupCode = useBackupCode,
                        isLoading = isLoading,
                        onMfaCodeChange = viewModel::setMfaCode,
                        onBackupCodeChange = viewModel::setBackupCode,
                        onUseBackupCodeChange = viewModel::setUseBackupCode,
                        onVerify = viewModel::verifyMfa,
                        onResend = viewModel::resendMfa,
                        onCancel = viewModel::cancelMfa
                    )
                    else -> UnauthenticatedContent(
                        formMode = formMode,
                        email = email,
                        password = password,
                        confirmPassword = confirmPassword,
                        phoneNumber = phoneNumber,
                        canSubmitSignIn = viewModel.canSubmitSignIn(),
                        canSubmitSignUp = viewModel.canSubmitSignUp(),
                        onFormModeChange = viewModel::setFormMode,
                        onEmailChange = viewModel::setEmail,
                        onPasswordChange = viewModel::setPassword,
                        onConfirmPasswordChange = viewModel::setConfirmPassword,
                        onPhoneNumberChange = viewModel::setPhoneNumber,
                        onSignIn = viewModel::login,
                        onSignUp = viewModel::register,
                        isLoading = isLoading
                    )
                }
            }
        }

        if (statusMessage.isNotBlank()) {
            Surface(
                color = if (statusMessage.contains("failed", ignoreCase = true) || statusMessage.contains("error", ignoreCase = true)) 
                        MaterialTheme.colorScheme.errorContainer 
                        else MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (statusMessage.contains("failed", ignoreCase = true)) Icons.Default.ErrorOutline else Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (statusMessage.contains("failed", ignoreCase = true)) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (statusMessage.contains("failed", ignoreCase = true)) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun UnauthenticatedContent(
    formMode: AuthFormMode,
    email: String,
    password: String,
    confirmPassword: String,
    phoneNumber: String,
    canSubmitSignIn: Boolean,
    canSubmitSignUp: Boolean,
    onFormModeChange: (AuthFormMode) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    onSignIn: () -> Unit,
    onSignUp: () -> Unit,
    isLoading: Boolean
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = formMode == AuthFormMode.SIGN_IN,
            onClick = { onFormModeChange(AuthFormMode.SIGN_IN) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
        ) { Text("Sign In") }
        SegmentedButton(
            selected = formMode == AuthFormMode.SIGN_UP,
            onClick = { onFormModeChange(AuthFormMode.SIGN_UP) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
        ) { Text("Register") }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email Address") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(12.dp)
        )

        if (formMode == AuthFormMode.SIGN_UP) {
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = { Text("Confirm Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.LockClock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = onPhoneNumberChange,
                label = { Text("Phone Number (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(12.dp)
            )
            Text(
                text = "Minimum ${AuthFormValidation.MINIMUM_PASSWORD_LENGTH} characters required.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = if (formMode == AuthFormMode.SIGN_IN) onSignIn else onSignUp,
            enabled = (if (formMode == AuthFormMode.SIGN_IN) canSubmitSignIn else canSubmitSignUp) && !isLoading,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
            } else {
                Text(if (formMode == AuthFormMode.SIGN_IN) "Sign In" else "Create Account", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun MfaContent(
    method: String,
    message: String,
    mfaCode: String,
    backupCode: String,
    useBackupCode: Boolean,
    isLoading: Boolean,
    onMfaCodeChange: (String) -> Unit,
    onBackupCodeChange: (String) -> Unit,
    onUseBackupCodeChange: (Boolean) -> Unit,
    onVerify: () -> Unit,
    onResend: () -> Unit,
    onCancel: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = mfaMethodLabel(method), 
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = message, 
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (method == "totp" && !useBackupCode) {
            OutlinedTextField(
                value = mfaCode,
                onValueChange = onMfaCodeChange,
                label = { Text("Authenticator Code") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) },
                shape = RoundedCornerShape(12.dp)
            )
            TextButton(onClick = { onUseBackupCodeChange(true) }) {
                Text("Use backup code instead")
            }
        } else if (method == "totp" && useBackupCode) {
            OutlinedTextField(
                value = backupCode,
                onValueChange = onBackupCodeChange,
                label = { Text("Backup Code") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
                shape = RoundedCornerShape(12.dp)
            )
            TextButton(onClick = { onUseBackupCodeChange(false) }) {
                Text("Use authenticator code instead")
            }
        } else {
            OutlinedTextField(
                value = mfaCode,
                onValueChange = onMfaCodeChange,
                label = { Text("6-Digit Code") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp)
            )
        }

        Button(
            onClick = onVerify, 
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) { 
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Verify Code") 
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (method != "totp") {
                OutlinedButton(
                    onClick = onResend, 
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Resend") }
            }
            OutlinedButton(
                onClick = onCancel, 
                enabled = !isLoading,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Cancel") }
        }
    }
}

@Composable
private fun SignedInContent(
    user: com.example.stopscrolling_android.data.remote.dto.AuthenticatedUser,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = user.email,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow(label = "Tracking ID", value = user.trackingId, icon = Icons.Default.Fingerprint)
                InfoRow(label = "MFA Method", value = mfaMethodLabel(user.mfaDelivery), icon = Icons.Default.Security)
                if (user.phoneNumber.isNotBlank()) {
                    InfoRow(
                        label = "Phone", 
                        value = "${user.phoneNumber}${if (user.phoneVerified) " (Verified)" else ""}", 
                        icon = Icons.Default.Phone
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign Out")
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun mfaMethodLabel(method: String): String {
    return when (method) {
        "totp" -> "Authenticator App"
        "sms_otp" -> "SMS Code"
        "email_otp" -> "Email Code"
        else -> "None"
    }
}
