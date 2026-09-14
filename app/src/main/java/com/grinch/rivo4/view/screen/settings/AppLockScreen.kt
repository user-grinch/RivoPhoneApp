package com.grinch.rivo4.view.screen.settings

import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.lock.AppLockManager
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.view.components.RivoDialog
import com.grinch.rivo4.view.components.RivoDialogAction
import com.grinch.rivo4.view.components.RivoDivider
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.RivoListItem
import com.grinch.rivo4.view.components.RivoSelectionDialog
import com.grinch.rivo4.view.components.RivoSwitchListItem
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun AppLockScreen(
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    val prefs: PreferenceManager = koinInject()
    val settingsState by prefs.settingsChanged.collectAsState()

    val isLockEnabled = remember(settingsState) { prefs.isAppLockEnabled() }
    val isBiometricEnabled = remember(settingsState) { prefs.isBiometricLockEnabled() }
    val hasPin = remember(settingsState) { prefs.getAppLockPin().isNotEmpty() }
    val timeout = remember(settingsState) { prefs.getAppLockTimeout() }

    var showPinDialog by remember { mutableStateOf(false) }
    var showTimeoutDialog by remember { mutableStateOf(false) }

    val activity = context as? FragmentActivity

    val timeoutOptions = remember {
        listOf(
            PreferenceManager.APP_LOCK_TIMEOUT_IMMEDIATELY to "Immediately on exit",
            PreferenceManager.APP_LOCK_TIMEOUT_1_MIN to "After 1 minute",
            PreferenceManager.APP_LOCK_TIMEOUT_5_MIN to "After 5 minutes",
            PreferenceManager.APP_LOCK_TIMEOUT_15_MIN to "After 15 minutes",
            PreferenceManager.APP_LOCK_TIMEOUT_30_MIN to "After 30 minutes"
        )
    }

    val currentTimeoutLabel = timeoutOptions.firstOrNull { it.first == timeout }?.second ?: "Immediately on exit"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Lock") },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RivoExpressiveCard(
                title = "Security & Protection",
                icon = Icons.Outlined.Security
            ) {
                RivoSwitchListItem(
                    headline = "Enable App Lock",
                    supporting = "Require authentication to open Rivo Phone",
                    leadingIcon = Icons.Outlined.Lock,
                    checked = isLockEnabled,
                    onCheckedChange = { enable ->
                        if (enable) {
                            if (activity != null && AppLockManager.canAuthenticate(context)) {
                                AppLockManager.authenticate(
                                    activity = activity,
                                    title = "Enable App Lock",
                                    subtitle = "Authenticate to confirm enabling lock",
                                    onSuccess = {
                                        prefs.setAppLockEnabled(true)
                                        Toast.makeText(context, "App lock enabled", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = {
                                        Toast.makeText(context, "Authentication failed: $it", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            } else {
                                showPinDialog = true
                            }
                        } else {
                            if (activity != null && AppLockManager.canAuthenticate(context)) {
                                AppLockManager.authenticate(
                                    activity = activity,
                                    title = "Disable App Lock",
                                    subtitle = "Authenticate to confirm disabling lock",
                                    onSuccess = {
                                        prefs.setAppLockEnabled(false)
                                        Toast.makeText(context, "App lock disabled", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = {
                                        Toast.makeText(context, "Authentication failed: $it", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            } else {
                                prefs.setAppLockEnabled(false)
                                Toast.makeText(context, "App lock disabled", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )

                AnimatedVisibility(visible = isLockEnabled) {
                    Column {
                        RivoDivider(Modifier.padding(horizontal = 16.dp))
                        RivoSwitchListItem(
                            headline = "Biometric Unlock",
                            supporting = "Use Face, Fingerprint, or Device PIN/Password",
                            leadingIcon = Icons.Outlined.Fingerprint,
                            checked = isBiometricEnabled,
                            onCheckedChange = { prefs.setBiometricLockEnabled(it) }
                        )

                        RivoDivider(Modifier.padding(horizontal = 16.dp))
                        RivoListItem(
                            headline = if (hasPin) "Change App PIN" else "Set Custom App PIN",
                            supporting = if (hasPin) "Custom PIN is configured" else "Optional separate PIN for Rivo Phone",
                            leadingIcon = Icons.Outlined.Password,
                            onClick = { showPinDialog = true }
                        )

                        RivoDivider(Modifier.padding(horizontal = 16.dp))
                        RivoListItem(
                            headline = "Lock Timeout",
                            supporting = currentTimeoutLabel,
                            leadingIcon = Icons.Default.Timer,
                            onClick = { showTimeoutDialog = true }
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Supports Face Unlock, Fingerprint, Device PIN/Pattern/Password, and Custom App PIN.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            com.grinch.rivo4.view.components.ad.BannerAd()
        }
    }

    if (showPinDialog) {
        var newPin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }
        var isConfirming by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf("") }

        RivoDialog(
            onDismissRequest = { showPinDialog = false },
            title = if (!isConfirming) "Set 4-Digit App PIN" else "Confirm Your PIN",
            icon = Icons.Default.Pin,
            confirmAction = RivoDialogAction(
                label = if (!isConfirming) "Next" else "Save PIN",
                enabled = if (!isConfirming) newPin.length == 4 else confirmPin.length == 4,
                dismissOnClick = isConfirming,
                onClick = {
                    if (!isConfirming) {
                        if (newPin.length == 4) {
                            isConfirming = true
                            errorMessage = ""
                        } else {
                            errorMessage = "PIN must be 4 digits"
                        }
                    } else {
                        if (confirmPin == newPin) {
                            prefs.setAppLockPin(newPin)
                            prefs.setAppLockEnabled(true)
                            Toast.makeText(context, "App PIN saved", Toast.LENGTH_SHORT).show()
                            showPinDialog = false
                        } else {
                            errorMessage = "PINs do not match. Try again."
                            confirmPin = ""
                        }
                    }
                }
            )
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                Text(
                    text = if (!isConfirming) "Enter a 4-digit PIN to lock Rivo Phone" else "Re-enter the 4-digit PIN to confirm",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = if (!isConfirming) newPin else confirmPin,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() }.take(4)
                        if (!isConfirming) newPin = filtered else confirmPin = filtered
                    },
                    placeholder = { Text("••••") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (showTimeoutDialog) {
        RivoSelectionDialog(
            onDismissRequest = { showTimeoutDialog = false },
            title = "Lock Timeout",
            items = timeoutOptions,
            itemLabel = { it.second },
            onItemSelected = { selected ->
                prefs.setAppLockTimeout(selected.first)
                showTimeoutDialog = false
            },
            icon = Icons.Default.Timer,
            isSelected = { it.first == timeout }
        )
    }
}

@Composable
fun AppLockOverlay(
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val prefs = org.koin.compose.koinInject<PreferenceManager>()
    val activity = context as? FragmentActivity
    val customPin = remember { prefs.getAppLockPin() }
    val isBiometricEnabled = remember { prefs.isBiometricLockEnabled() }

    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    fun tryBiometric() {
        if (activity != null && isBiometricEnabled) {
            AppLockManager.authenticate(
                activity = activity,
                title = "Unlock Rivo Phone",
                subtitle = "Use Face, Fingerprint, PIN, or Password",
                onSuccess = {
                    onUnlocked()
                },
                onError = {
                    // Falls back to in-app PIN if set
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        tryBiometric()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "App Locked",
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Rivo Phone is Locked",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = if (customPin.isNotEmpty()) "Enter PIN or use biometrics to unlock" else "Authenticate to unlock",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            if (customPin.isNotEmpty()) {
                // PIN dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    for (i in 0 until 4) {
                        val isFilled = i < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        pinError -> MaterialTheme.colorScheme.error
                                        isFilled -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                        )
                    }
                }

                if (pinError) {
                    Text(
                        text = "Incorrect PIN",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Keypad
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val rows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("bio", "0", "del")
                    )

                    rows.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            row.forEach { key ->
                                when (key) {
                                    "bio" -> {
                                        if (isBiometricEnabled) {
                                            Surface(
                                                onClick = { tryBiometric() },
                                                modifier = Modifier.size(68.dp),
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        Icons.Default.Fingerprint,
                                                        contentDescription = "Biometrics",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(28.dp)
                                                    )
                                                }
                                            }
                                        } else {
                                            Spacer(Modifier.size(68.dp))
                                        }
                                    }
                                    "del" -> {
                                        Surface(
                                            onClick = {
                                                if (enteredPin.isNotEmpty()) {
                                                    enteredPin = enteredPin.dropLast(1)
                                                    pinError = false
                                                }
                                            },
                                            modifier = Modifier.size(68.dp),
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.Backspace,
                                                    contentDescription = "Backspace",
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                    }
                                    else -> {
                                        Surface(
                                            onClick = {
                                                if (enteredPin.length < 4) {
                                                    val newPin = enteredPin + key
                                                    enteredPin = newPin
                                                    pinError = false
                                                    if (newPin.length == 4) {
                                                        if (newPin == customPin) {
                                                            AppLockManager.unlock()
                                                            onUnlocked()
                                                        } else {
                                                            pinError = true
                                                            enteredPin = ""
                                                        }
                                                    }
                                                }
                                            },
                                            modifier = Modifier.size(68.dp),
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = key,
                                                    fontSize = 24.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                FilledTonalButton(
                    onClick = { tryBiometric() },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Unlock")
                }
            }
        }
    }
}
