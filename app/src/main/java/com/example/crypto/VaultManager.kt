package com.example.crypto

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.crypto.SecretKey

class VaultManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("secure_notes_vault_prefs", Context.MODE_PRIVATE)

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private var inMemorySecretKey: SecretKey? = null

    companion object {
        private const val KEY_SALT = "vault_master_salt"
        private const val KEY_CANARY_CIPHERTEXT = "vault_canary_ciphertext"
        private const val KEY_CANARY_IV = "vault_canary_iv"
        private const val KEY_CANARY_SALT = "vault_canary_salt"
        private const val KEY_CANARY_CHECKSUM = "vault_canary_checksum"
        private const val CANARY_PLAINTEXT = "E2EE_VAULT_CANARY_VALIDATED_2026"
    }

    fun isPassphraseConfigured(): Boolean {
        return prefs.contains(KEY_SALT) && prefs.contains(KEY_CANARY_CIPHERTEXT)
    }

    fun setupPassphrase(passphrase: String): Boolean {
        if (passphrase.length < 4) return false
        val salt = CryptoEngine.generateSalt()
        val key = CryptoEngine.deriveKey(passphrase.toCharArray(), salt)

        // Encrypt canary to verify password upon future unlocks
        val canaryPayload = CryptoEngine.encrypt(CANARY_PLAINTEXT, key, salt)

        prefs.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_CANARY_CIPHERTEXT, canaryPayload.ciphertext)
            .putString(KEY_CANARY_IV, canaryPayload.iv)
            .putString(KEY_CANARY_SALT, canaryPayload.salt)
            .putString(KEY_CANARY_CHECKSUM, canaryPayload.checksum)
            .apply()

        inMemorySecretKey = key
        _isUnlocked.value = true
        return true
    }

    fun unlock(passphrase: String): Boolean {
        val saltBase64 = prefs.getString(KEY_SALT, null) ?: return false
        val canaryCiphertext = prefs.getString(KEY_CANARY_CIPHERTEXT, null) ?: return false
        val canaryIv = prefs.getString(KEY_CANARY_IV, null) ?: return false
        val canarySalt = prefs.getString(KEY_CANARY_SALT, null) ?: return false
        val canaryChecksum = prefs.getString(KEY_CANARY_CHECKSUM, "") ?: ""

        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
        val key = CryptoEngine.deriveKey(passphrase.toCharArray(), salt)

        return try {
            val payload = EncryptedPayload(
                ciphertext = canaryCiphertext,
                iv = canaryIv,
                salt = canarySalt,
                checksum = canaryChecksum
            )
            val decrypted = CryptoEngine.decrypt(payload, key)
            if (decrypted == CANARY_PLAINTEXT) {
                inMemorySecretKey = key
                _isUnlocked.value = true
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun lock() {
        inMemorySecretKey = null
        _isUnlocked.value = false
    }

    fun getActiveKey(): SecretKey? = inMemorySecretKey

    fun getMasterSalt(): ByteArray? {
        val saltBase64 = prefs.getString(KEY_SALT, null) ?: return null
        return Base64.decode(saltBase64, Base64.NO_WRAP)
    }

    fun resetVault() {
        prefs.edit().clear().apply()
        lock()
    }
}
