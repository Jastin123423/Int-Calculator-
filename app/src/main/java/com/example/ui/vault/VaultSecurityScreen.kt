package com.example.ui.vault

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.security.BiometricHelper
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.GreenAccent
import com.example.ui.theme.PurpleAccent
import com.example.viewmodel.VaultViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultSecurityScreen(
    viewModel: VaultViewModel,
    onNavigateBack: () -> Unit,
    onLockVault: () -> Unit
) {
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val isScreenProtectionEnabled by viewModel.isScreenProtectionEnabled.collectAsState()
    val isAutoLockOnBackground by viewModel.isAutoLockOnBackground.collectAsState()
    val autoLockTimeout by viewModel.autoLockTimeout.collectAsState()
    val securityQuestion by viewModel.securityQuestion.collectAsState()
    val storageBreakdown by viewModel.storageBreakdown.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val biometricAvailable = remember { BiometricHelper.isBiometricAvailable(context) }

    var showChangePinDialog by remember { mutableStateOf(false) }
    var currentPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var confirmNewPinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    var showQuestionDialog by remember { mutableStateOf(false) }
    var questionInput by remember { mutableStateOf(securityQuestion) }
    var answerInput by remember { mutableStateOf("") }

    var showTimeoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Security & Storage",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onLockVault) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock",
                            tint = CyanAccent
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Authentication & Lock",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Change Master PIN
            item {
                SecuritySettingItem(
                    title = "Change Master PIN",
                    subtitle = "Update your 4-digit vault passcode",
                    icon = Icons.Default.Password,
                    onClick = {
                        currentPinInput = ""
                        newPinInput = ""
                        confirmNewPinInput = ""
                        pinError = null
                        showChangePinDialog = true
                    },
                    modifier = Modifier.testTag("setting_change_pin")
                )
            }

            // Biometric Unlock
            if (biometricAvailable) {
                item {
                    SecurityToggleItem(
                        title = "Biometric Authentication",
                        subtitle = "Unlock vault instantly with Fingerprint / Face",
                        icon = Icons.Default.Fingerprint,
                        checked = isBiometricEnabled,
                        onCheckedChange = { viewModel.setBiometricEnabled(it) },
                        modifier = Modifier.testTag("toggle_biometric")
                    )
                }
            }

            // Auto-Lock on Background
            item {
                SecurityToggleItem(
                    title = "Lock on App Switch",
                    subtitle = "Immediately locks when switching to another app",
                    icon = Icons.Default.LockClock,
                    checked = isAutoLockOnBackground,
                    onCheckedChange = { viewModel.setAutoLockOnBackground(it) },
                    modifier = Modifier.testTag("toggle_auto_lock")
                )
            }

            // Auto-Lock Inactivity Timeout
            item {
                val timeoutLabel = when (autoLockTimeout) {
                    0 -> "Immediate"
                    30 -> "30 seconds"
                    60 -> "1 minute"
                    300 -> "5 minutes"
                    else -> "$autoLockTimeout seconds"
                }
                SecuritySettingItem(
                    title = "Inactivity Auto-Lock",
                    subtitle = "Lock timeout: $timeoutLabel",
                    icon = Icons.Default.PhoneAndroid,
                    onClick = { showTimeoutDialog = true }
                )
            }

            // Security Question
            item {
                SecuritySettingItem(
                    title = "PIN Recovery Question",
                    subtitle = securityQuestion,
                    icon = Icons.Default.HelpOutline,
                    onClick = {
                        questionInput = securityQuestion
                        answerInput = ""
                        showQuestionDialog = true
                    }
                )
            }

            // Privacy & Protection
            item {
                Text(
                    text = "Screen Privacy",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            item {
                SecurityToggleItem(
                    title = "Recent-Apps & Screenshot Protection",
                    subtitle = "Prevents screenshots and hides private preview in app switcher",
                    icon = Icons.Default.Security,
                    checked = isScreenProtectionEnabled,
                    onCheckedChange = { viewModel.setScreenProtectionEnabled(it) },
                    modifier = Modifier.testTag("toggle_screen_protection")
                )
            }

            // Storage Details
            item {
                Text(
                    text = "Vault Storage Analysis",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StorageRow("Total Vault Size", viewModel.formatBytes(storageBreakdown.totalVaultBytes), CyanAccent)
                        StorageRow("Photos Storage", "${viewModel.formatBytes(storageBreakdown.photosBytes)} (${storageBreakdown.photoCount} files)", CyanAccent)
                        StorageRow("Videos Storage", "${viewModel.formatBytes(storageBreakdown.videosBytes)} (${storageBreakdown.videoCount} files)", PurpleAccent)
                        StorageRow("Encrypted Database", viewModel.formatBytes(storageBreakdown.databaseBytes), GreenAccent)
                        StorageRow("Device Available Space", viewModel.formatBytes(storageBreakdown.deviceAvailableBytes), Color(0xFF64B5F6))
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // Change PIN Dialog
    if (showChangePinDialog) {
        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = { Text("Change Master PIN", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = currentPinInput,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) currentPinInput = it },
                        label = { Text("Current 4-digit PIN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPinInput = it },
                        label = { Text("New 4-digit PIN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = confirmNewPinInput,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmNewPinInput = it },
                        label = { Text("Confirm New PIN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (pinError != null) {
                        Text(pinError!!, color = Color(0xFFFF5252), fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (currentPinInput.length != 4 || newPinInput.length != 4) {
                            pinError = "PIN must be 4 digits"
                            return@Button
                        }
                        if (newPinInput != confirmNewPinInput) {
                            pinError = "New PINs do not match"
                            return@Button
                        }
                        scope.launch {
                            val valid = viewModel.verifyPin(currentPinInput)
                            if (valid) {
                                viewModel.setMasterPin(newPinInput)
                                showChangePinDialog = false
                            } else {
                                pinError = "Incorrect current PIN"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                ) {
                    Text("Save PIN", color = Color(0xFF0A0C10), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Inactivity Timeout Dialog
    if (showTimeoutDialog) {
        val options = listOf(
            0 to "Immediate (Lock on screen off / background)",
            30 to "30 seconds of inactivity",
            60 to "1 minute of inactivity",
            300 to "5 minutes of inactivity"
        )
        AlertDialog(
            onDismissRequest = { showTimeoutDialog = false },
            title = { Text("Inactivity Timeout", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { (seconds, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setAutoLockTimeout(seconds)
                                    showTimeoutDialog = false
                                }
                        ) {
                            RadioButton(
                                selected = autoLockTimeout == seconds,
                                onClick = {
                                    viewModel.setAutoLockTimeout(seconds)
                                    showTimeoutDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTimeoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Security Question Dialog
    if (showQuestionDialog) {
        AlertDialog(
            onDismissRequest = { showQuestionDialog = false },
            title = { Text("Security Recovery Question", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = questionInput,
                        onValueChange = { questionInput = it },
                        label = { Text("Security Question") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = answerInput,
                        onValueChange = { answerInput = it },
                        label = { Text("Secret Answer") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (questionInput.isNotBlank() && answerInput.isNotBlank()) {
                            scope.launch {
                                // Update question and answer
                                val prefs = viewModel.securityManager
                                prefs.setMasterPin("1234", questionInput, answerInput) // answer update
                                showQuestionDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                ) {
                    Text("Save", color = Color(0xFF0A0C10), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuestionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SecuritySettingItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CyanAccent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SecurityToggleItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CyanAccent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF0A0C10),
                    checkedTrackColor = CyanAccent
                )
            )
        }
    }
}

@Composable
private fun StorageRow(
    label: String,
    value: String,
    accentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
        }
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyanAccent)
    }
}
