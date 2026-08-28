package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.example.ui.navigation.CalcProMainApp
import com.example.viewmodel.CalculatorViewModel
import com.example.viewmodel.HistoryViewModel
import com.example.viewmodel.SettingsViewModel
import com.example.viewmodel.VaultViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private val calculatorViewModel: CalculatorViewModel by viewModels()
    private val historyViewModel: HistoryViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val vaultViewModel: VaultViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle Screen Protection (FLAG_SECURE) to prevent screenshots & task switcher preview
        lifecycleScope.launch {
            combine(
                vaultViewModel.isScreenProtectionEnabled,
                vaultViewModel.isVaultUnlocked
            ) { isProtectionEnabled, isUnlocked ->
                isProtectionEnabled && isUnlocked
            }.collect { shouldSecure ->
                if (shouldSecure) {
                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE
                    )
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        }

        setContent {
            CalcProMainApp(
                calculatorViewModel = calculatorViewModel,
                historyViewModel = historyViewModel,
                settingsViewModel = settingsViewModel,
                vaultViewModel = vaultViewModel
            )
        }
    }

    override fun onStop() {
        super.onStop()
        if (vaultViewModel.isAutoLockOnBackground.value) {
            vaultViewModel.lockVault()
        }
    }

    override fun onResume() {
        super.onResume()
        vaultViewModel.checkBackgroundTimeout()
    }
}
