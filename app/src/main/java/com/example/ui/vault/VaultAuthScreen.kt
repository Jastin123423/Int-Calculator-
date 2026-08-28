package com.example.ui.vault

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.data.security.BiometricHelper
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.PurpleAccent
import com.example.viewmodel.VaultViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultAuthScreen(
    viewModel: VaultViewModel,
    onAuthenticated: () -> Unit,
    onCancel: () -> Unit
) {
    val isSetUp by viewModel.isVaultSetUp.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val securityQuestion by viewModel.securityQuestion.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var enteredPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isConfirmStep by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // First time setup security question states
    var customQuestion by remember { mutableStateOf("What is your favorite color/city?") }
    var customAnswer by remember { mutableStateOf("") }
    var showForgotDialog by remember { mutableStateOf(false) }
    var forgotAnswer by remember { mutableStateOf("") }
    var forgotNewPin by remember { mutableStateOf("") }
    var forgotError by remember { mutableStateOf<String?>(null) }

    // Automatic biometric trigger if enabled & available
    LaunchedEffect(isSetUp, isBiometricEnabled) {
        if (isSetUp && isBiometricEnabled && activity != null && BiometricHelper.isBiometricAvailable(context)) {
            BiometricHelper.authenticate(
                activity = activity,
                onSuccess = {
                    viewModel.unlockWithBiometrics()
                    onAuthenticated()
                },
                onError = { /* fallback to PIN */ },
                onFallbackToPin = { /* user chose PIN */ }
            )
        }
    }

    val maxPinLength = 4

    fun onKeyClick(digit: String) {
        errorMessage = null
        if (isSetUp) {
            if (enteredPin.length < maxPinLength) {
                val newPin = enteredPin + digit
                enteredPin = newPin
                if (newPin.length == maxPinLength) {
                    scope.launch {
                        val valid = viewModel.verifyPin(newPin)
                        if (valid) {
                            onAuthenticated()
                        } else {
                            errorMessage = "Incorrect PIN. Please try again."
                            enteredPin = ""
                        }
                    }
                }
            }
        } else {
            // Setup Mode
            if (!isConfirmStep) {
                if (enteredPin.length < maxPinLength) {
                    enteredPin += digit
                    if (enteredPin.length == maxPinLength) {
                        isConfirmStep = true
                    }
                }
            } else {
                if (confirmPin.length < maxPinLength) {
                    confirmPin += digit
                    if (confirmPin.length == maxPinLength) {
                        if (confirmPin == enteredPin) {
                            scope.launch {
                                viewModel.setMasterPin(confirmPin, customQuestion, customAnswer)
                                onAuthenticated()
                            }
                        } else {
                            errorMessage = "PINs do not match. Start again."
                            enteredPin = ""
                            confirmPin = ""
                            isConfirmStep = false
                        }
                    }
                }
            }
        }
    }

    fun onBackspace() {
        errorMessage = null
        if (isSetUp) {
            if (enteredPin.isNotEmpty()) {
                enteredPin = enteredPin.dropLast(1)
            }
        } else {
            if (isConfirmStep) {
                if (confirmPin.isNotEmpty()) {
                    confirmPin = confirmPin.dropLast(1)
                } else {
                    isConfirmStep = false
                }
            } else {
                if (enteredPin.isNotEmpty()) {
                    enteredPin = enteredPin.dropLast(1)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (!isSetUp) "Setup Master PIN" else "Private Vault Unlock",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier.testTag("auth_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Return to Calculator"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header Icon & Title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(CyanAccent, PurpleAccent))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (!isSetUp) Icons.Default.Security else Icons.Default.Lock,
                        contentDescription = "Vault Security",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = when {
                        !isSetUp && !isConfirmStep -> "Create a 4-digit Master PIN"
                        !isSetUp && isConfirmStep -> "Confirm your Master PIN"
                        else -> "Enter Master PIN to Unlock"
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (!isSetUp) "This PIN will securely protect your private photos, videos, albums, and contacts."
                    else "All private vault files stay encrypted on your device.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // PIN indicator dots
                val currentPin = if (!isSetUp && isConfirmStep) confirmPin else enteredPin
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(maxPinLength) { index ->
                        val filled = index < currentPin.length
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(
                                    if (filled) CyanAccent else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }

                // Error message
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFFF5252),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }

            // Keypad
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("BIO", "0", "DEL")
                )

                keys.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { key ->
                            when (key) {
                                "BIO" -> {
                                    if (isSetUp && isBiometricEnabled && activity != null && BiometricHelper.isBiometricAvailable(context)) {
                                        IconButton(
                                            onClick = {
                                                BiometricHelper.authenticate(
                                                    activity = activity,
                                                    onSuccess = {
                                                        viewModel.unlockWithBiometrics()
                                                        onAuthenticated()
                                                    },
                                                    onError = { errorMessage = it },
                                                    onFallbackToPin = { }
                                                )
                                            },
                                            modifier = Modifier
                                                .size(72.dp)
                                                .testTag("auth_btn_biometric")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Fingerprint,
                                                contentDescription = "Biometric Unlock",
                                                tint = CyanAccent,
                                                modifier = Modifier.size(34.dp)
                                            )
                                        }
                                    } else {
                                        Box(modifier = Modifier.size(72.dp))
                                    }
                                }
                                "DEL" -> {
                                    IconButton(
                                        onClick = { onBackspace() },
                                        modifier = Modifier
                                            .size(72.dp)
                                            .testTag("auth_btn_backspace")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                                            contentDescription = "Backspace",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                                else -> {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .clickable { onKeyClick(key) }
                                            .testTag("auth_key_$key")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = key,
                                                fontSize = 26.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (isSetUp) {
                    TextButton(
                        onClick = { showForgotDialog = true },
                        modifier = Modifier.testTag("forgot_pin_button")
                    ) {
                        Text(
                            text = "Forgot PIN?",
                            color = CyanAccent,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }

    // Forgot PIN Dialog
    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = {
                showForgotDialog = false
                forgotError = null
                forgotAnswer = ""
                forgotNewPin = ""
            },
            title = {
                Text("Reset Master PIN", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Security Question:\n$securityQuestion",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = forgotAnswer,
                        onValueChange = { forgotAnswer = it },
                        label = { Text("Security Answer") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = forgotNewPin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) forgotNewPin = it },
                        label = { Text("New 4-digit PIN") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (forgotError != null) {
                        Text(forgotError ?: "", color = Color(0xFFFF5252), fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (forgotNewPin.length != 4) {
                            forgotError = "PIN must be 4 digits"
                            return@Button
                        }
                        scope.launch {
                            val success = viewModel.resetPin(forgotAnswer, forgotNewPin)
                            if (success) {
                                showForgotDialog = false
                                enteredPin = ""
                                onAuthenticated()
                            } else {
                                forgotError = "Incorrect security answer"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                ) {
                    Text("Reset & Unlock", color = Color(0xFF0A0C10), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
