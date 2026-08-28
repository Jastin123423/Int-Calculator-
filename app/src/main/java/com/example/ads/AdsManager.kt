package com.example.ads

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object AdsManager {
    private val _isAdsEnabled = MutableStateFlow(false) // Default disabled for clean premium experience
    val isAdsEnabled: StateFlow<Boolean> = _isAdsEnabled

    fun setAdsEnabled(enabled: Boolean) {
        _isAdsEnabled.value = enabled
    }
}
