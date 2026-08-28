package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.AccentColor
import com.example.data.preferences.AppTheme
import com.example.data.preferences.PreferencesManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager(application)

    val appTheme: StateFlow<AppTheme> = preferencesManager.themeFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), AppTheme.DARK
    )

    val accentColor: StateFlow<AccentColor> = preferencesManager.accentColorFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), AccentColor.BLUE
    )

    val hapticsEnabled: StateFlow<Boolean> = preferencesManager.hapticsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )

    val soundEnabled: StateFlow<Boolean> = preferencesManager.soundFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    val angleUnit: StateFlow<String> = preferencesManager.angleUnitFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "DEG"
    )

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            preferencesManager.setTheme(theme)
        }
    }

    fun setAccentColor(accentColor: AccentColor) {
        viewModelScope.launch {
            preferencesManager.setAccentColor(accentColor)
        }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setHapticsEnabled(enabled)
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setSoundEnabled(enabled)
        }
    }

    fun setAngleUnit(unit: String) {
        viewModelScope.launch {
            preferencesManager.setAngleUnit(unit)
        }
    }
}
