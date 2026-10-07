package com.trep.passwordmanager.data.crypto

import java.security.SecureRandom

object PasswordGenerator {
    private const val UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val LOWER = "abcdefghijklmnopqrstuvwxyz"
    private const val DIGITS = "0123456789"
    private const val SYMBOLS = "!@#$%^&*()_+[]{}<>?/|"
    private const val ALL = UPPER + LOWER + DIGITS + SYMBOLS

    private val random = SecureRandom()

    fun generate(length: Int = 16): String {
        require(length >= 4) { "Password length must be at least 4" }

        val result = mutableListOf(
            UPPER[random.nextInt(UPPER.length)],
            LOWER[random.nextInt(LOWER.length)],
            DIGITS[random.nextInt(DIGITS.length)],
            SYMBOLS[random.nextInt(SYMBOLS.length)]
        )

        for (i in 4 until length) {
            result.add(ALL[random.nextInt(ALL.length)])
        }

        // Fisher-Yates shuffle
        for (i in result.indices.reversed()) {
            val j = random.nextInt(i + 1)
            val temp = result[i]
            result[i] = result[j]
            result[j] = temp
        }

        return result.joinToString("")
    }
}
