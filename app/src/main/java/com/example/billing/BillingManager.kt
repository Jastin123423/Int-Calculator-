package com.example.billing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object BillingManager {
    private val _isProUser = MutableStateFlow(true) // Pro unlocked experience
    val isProUser: StateFlow<Boolean> = _isProUser

    fun setProUser(isPro: Boolean) {
        _isProUser.value = isPro
    }
}
