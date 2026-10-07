package com.trep.passwordmanager.data.model

enum class VaultMode(val value: String) {
    SECURE("secure"),
    PLAIN("plain");

    companion object {
        fun fromValue(value: String): VaultMode =
            if (value.equals("plain", ignoreCase = true)) PLAIN else SECURE
    }
}
