package com.example.crypto

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class EncryptedPayload(
    val ciphertext: String,
    val iv: String,
    val salt: String,
    val checksum: String,
    val algorithm: String = "AES-256-GCM"
)

object CryptoEngine {
    private const val ALGORITHM = "AES"
    private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATION_COUNT = 65536
    private const val KEY_LENGTH_BITS = 256
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val IV_LENGTH_BYTES = 12
    private const val SALT_LENGTH_BYTES = 16

    private val secureRandom = SecureRandom()

    fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH_BYTES)
        secureRandom.nextBytes(salt)
        return salt
    }

    fun deriveKey(passphrase: CharArray, salt: ByteArray): SecretKey {
        val keySpec = PBEKeySpec(passphrase, salt, ITERATION_COUNT, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM)
        val derivedBytes = factory.generateSecret(keySpec).encoded
        return SecretKeySpec(derivedBytes, ALGORITHM)
    }

    fun encrypt(plaintext: String, secretKey: SecretKey, salt: ByteArray): EncryptedPayload {
        val iv = ByteArray(IV_LENGTH_BYTES)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        val plaintextBytes = plaintext.toByteArray(Charsets.UTF_8)
        val ciphertextBytes = cipher.doFinal(plaintextBytes)

        val checksum = sha256Hex(plaintextBytes)

        return EncryptedPayload(
            ciphertext = Base64.encodeToString(ciphertextBytes, Base64.NO_WRAP),
            iv = Base64.encodeToString(iv, Base64.NO_WRAP),
            salt = Base64.encodeToString(salt, Base64.NO_WRAP),
            checksum = checksum
        )
    }

    fun decrypt(encryptedPayload: EncryptedPayload, secretKey: SecretKey): String {
        val iv = Base64.decode(encryptedPayload.iv, Base64.NO_WRAP)
        val ciphertextBytes = Base64.decode(encryptedPayload.ciphertext, Base64.NO_WRAP)

        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

        val decryptedBytes = cipher.doFinal(ciphertextBytes)
        val decryptedText = String(decryptedBytes, Charsets.UTF_8)

        if (encryptedPayload.checksum.isNotBlank()) {
            val actualChecksum = sha256Hex(decryptedBytes)
            if (actualChecksum != encryptedPayload.checksum) {
                throw SecurityException("Data integrity compromised: checksum mismatch")
            }
        }

        return decryptedText
    }

    fun sha256Hex(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
