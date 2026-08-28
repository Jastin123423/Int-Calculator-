package com.example.data.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

val Context.vaultSecurityDataStore: DataStore<Preferences> by preferencesDataStore(name = "vault_security_settings")

class VaultSecurityManager(private val context: Context) {

    private object Keys {
        val MASTER_PIN_HASH = stringPreferencesKey("vault_pin_hash")
        val MASTER_PIN_SALT = stringPreferencesKey("vault_pin_salt")
        val IS_VAULT_SET_UP = booleanPreferencesKey("is_vault_set_up")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val SCREEN_PROTECTION_ENABLED = booleanPreferencesKey("screen_protection_enabled")
        val AUTO_LOCK_ON_BACKGROUND = booleanPreferencesKey("auto_lock_on_background")
        val AUTO_LOCK_TIMEOUT_SECONDS = intPreferencesKey("auto_lock_timeout_seconds")
        val SECURITY_QUESTION = stringPreferencesKey("security_question")
        val SECURITY_ANSWER_HASH = stringPreferencesKey("security_answer_hash")
        val FAILED_ATTEMPTS = intPreferencesKey("failed_attempts")
        val LOCKOUT_UNTIL_TIMESTAMP = stringPreferencesKey("lockout_until_timestamp")
    }

    // In-memory unlocked session state
    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    private var lastActiveTimestamp: Long = System.currentTimeMillis()

    val isVaultSetUpFlow: Flow<Boolean> = context.vaultSecurityDataStore.data.map { prefs ->
        prefs[Keys.IS_VAULT_SET_UP] ?: false
    }

    val isBiometricEnabledFlow: Flow<Boolean> = context.vaultSecurityDataStore.data.map { prefs ->
        prefs[Keys.BIOMETRIC_ENABLED] ?: false
    }

    val isScreenProtectionEnabledFlow: Flow<Boolean> = context.vaultSecurityDataStore.data.map { prefs ->
        prefs[Keys.SCREEN_PROTECTION_ENABLED] ?: true
    }

    val isAutoLockOnBackgroundFlow: Flow<Boolean> = context.vaultSecurityDataStore.data.map { prefs ->
        prefs[Keys.AUTO_LOCK_ON_BACKGROUND] ?: true
    }

    val autoLockTimeoutFlow: Flow<Int> = context.vaultSecurityDataStore.data.map { prefs ->
        prefs[Keys.AUTO_LOCK_TIMEOUT_SECONDS] ?: 0 // 0 = immediate
    }

    val securityQuestionFlow: Flow<String> = context.vaultSecurityDataStore.data.map { prefs ->
        prefs[Keys.SECURITY_QUESTION] ?: "What is your secret passkey question?"
    }

    fun markUserActivity() {
        lastActiveTimestamp = System.currentTimeMillis()
    }

    fun lockVault() {
        _isVaultUnlocked.value = false
    }

    fun forceUnlock() {
        _isVaultUnlocked.value = true
        markUserActivity()
    }

    fun checkBackgroundTimeout(timeoutSeconds: Int): Boolean {
        if (timeoutSeconds <= 0) {
            lockVault()
            return true
        }
        val elapsed = (System.currentTimeMillis() - lastActiveTimestamp) / 1000
        if (elapsed >= timeoutSeconds) {
            lockVault()
            return true
        }
        return false
    }

    suspend fun verifyPin(pin: String): Boolean {
        val prefs = context.vaultSecurityDataStore.data.first()
        val storedHash = prefs[Keys.MASTER_PIN_HASH] ?: return false
        val storedSalt = prefs[Keys.MASTER_PIN_SALT] ?: ""

        val inputHash = hashPin(pin, storedSalt)
        val matches = storedHash == inputHash
        if (matches) {
            _isVaultUnlocked.value = true
            markUserActivity()
        }
        return matches
    }

    suspend fun setMasterPin(pin: String, securityQuestion: String = "", securityAnswer: String = "") {
        val salt = java.util.UUID.randomUUID().toString()
        val hash = hashPin(pin, salt)

        context.vaultSecurityDataStore.edit { prefs ->
            prefs[Keys.MASTER_PIN_HASH] = hash
            prefs[Keys.MASTER_PIN_SALT] = salt
            prefs[Keys.IS_VAULT_SET_UP] = true
            if (securityQuestion.isNotBlank()) {
                prefs[Keys.SECURITY_QUESTION] = securityQuestion
                prefs[Keys.SECURITY_ANSWER_HASH] = hashString(securityAnswer.trim().lowercase())
            }
        }
        _isVaultUnlocked.value = true
        markUserActivity()
    }

    suspend fun resetPinWithSecurityAnswer(answer: String, newPin: String): Boolean {
        val prefs = context.vaultSecurityDataStore.data.first()
        val storedAnswerHash = prefs[Keys.SECURITY_ANSWER_HASH] ?: return false
        val inputAnswerHash = hashString(answer.trim().lowercase())

        if (storedAnswerHash == inputAnswerHash) {
            setMasterPin(newPin)
            return true
        }
        return false
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.vaultSecurityDataStore.edit { prefs ->
            prefs[Keys.BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun setScreenProtectionEnabled(enabled: Boolean) {
        context.vaultSecurityDataStore.edit { prefs ->
            prefs[Keys.SCREEN_PROTECTION_ENABLED] = enabled
        }
    }

    suspend fun setAutoLockOnBackground(enabled: Boolean) {
        context.vaultSecurityDataStore.edit { prefs ->
            prefs[Keys.AUTO_LOCK_ON_BACKGROUND] = enabled
        }
    }

    suspend fun setAutoLockTimeout(seconds: Int) {
        context.vaultSecurityDataStore.edit { prefs ->
            prefs[Keys.AUTO_LOCK_TIMEOUT_SECONDS] = seconds
        }
    }

    private fun hashPin(pin: String, salt: String): String {
        return hashString("$pin:$salt:IntCalculatorVault")
    }

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
