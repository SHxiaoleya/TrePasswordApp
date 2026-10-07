package com.trep.passwordmanager.data.crypto

import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object TotpGenerator {
    const val TOTP_STEP = 30L
    const val TOTP_DIGITS = 6
    private const val MODULO = 1_000_000
    private const val BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    fun normalizeSecret(raw: String): String {
        return raw.replace(Regex("[\\s-]"), "").uppercase()
    }

    fun isValidBase32(secret: String): Boolean {
        val s = normalizeSecret(secret)
        if (s.isEmpty()) return false
        return s.all { BASE32_ALPHABET.contains(it) }
    }

    fun base32ToBytes(secret: String): ByteArray {
        val s = normalizeSecret(secret)
        if (s.isEmpty()) return ByteArray(0)

        var bits = 0
        var buffer = 0
        val out = mutableListOf<Byte>()

        for (ch in s) {
            val v = BASE32_ALPHABET.indexOf(ch)
            if (v == -1) {
                throw IllegalArgumentException("Invalid Base32 character: $ch")
            }
            buffer = (buffer shl 5) or v
            bits += 5
            while (bits >= 8) {
                out.add(((buffer ushr (bits - 8)) and 0xFF).toByte())
                bits -= 8
            }
        }
        return out.toByteArray()
    }

    fun generateTotp(secretBase32: String, timestampMs: Long = System.currentTimeMillis()): String {
        val secretBytes = base32ToBytes(secretBase32)
        if (secretBytes.isEmpty()) throw IllegalArgumentException("Missing 2FA secret")

        val counter = (timestampMs / 1000L) / TOTP_STEP

        val msg = ByteBuffer.allocate(8).putLong(counter).array()
        val mac = Mac.getInstance("HmacSHA1")
        val key = SecretKeySpec(secretBytes, "HmacSHA1")
        mac.init(key)
        val hmac = mac.doFinal(msg)

        val offset = (hmac.last().toInt() and 0x0F)
        val binary = ((hmac[offset].toInt() and 0x7F) shl 24) or
                ((hmac[offset + 1].toInt() and 0xFF) shl 16) or
                ((hmac[offset + 2].toInt() and 0xFF) shl 8) or
                (hmac[offset + 3].toInt() and 0xFF)

        val otp = binary % MODULO
        return otp.toString().padStart(TOTP_DIGITS, '0')
    }

    fun getRemainingSeconds(timestampMs: Long = System.currentTimeMillis()): Int {
        val seconds = (timestampMs / 1000L) % TOTP_STEP
        return (TOTP_STEP - seconds).toInt()
    }
}
