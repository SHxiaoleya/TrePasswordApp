package com.trep.passwordmanager.data.crypto

import android.util.Base64
import com.trep.passwordmanager.data.model.PasswordItem
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoManager {
    const val SALT_LEN = 16
    const val IV_LEN = 12
    const val PBKDF2_ITERATIONS = 200_000
    const val KEY_LENGTH_BITS = 256
    const val MIN_MASTER_LENGTH = 8

    private val secureRandom = SecureRandom()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun randomBytes(length: Int): ByteArray {
        val bytes = ByteArray(length)
        secureRandom.nextBytes(bytes)
        return bytes
    }

    fun deriveKey(masterPassword: String, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(
            masterPassword.toCharArray(),
            salt,
            PBKDF2_ITERATIONS,
            KEY_LENGTH_BITS
        )
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    fun encryptItems(items: List<PasswordItem>, key: SecretKey): Pair<String, String> {
        val iv = randomBytes(IV_LEN)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)

        val plaintextBytes = json.encodeToString(items).toByteArray(Charsets.UTF_8)
        val ciphertext = cipher.doFinal(plaintextBytes)

        val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        val dataBase64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        return Pair(ivBase64, dataBase64)
    }

    fun decryptItems(ivBase64: String, dataBase64: String, key: SecretKey): List<PasswordItem> {
        val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
        val ciphertext = Base64.decode(dataBase64, Base64.NO_WRAP)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)

        val decryptedBytes = cipher.doFinal(ciphertext)
        val jsonString = String(decryptedBytes, Charsets.UTF_8)
        return json.decodeFromString(jsonString)
    }
}
