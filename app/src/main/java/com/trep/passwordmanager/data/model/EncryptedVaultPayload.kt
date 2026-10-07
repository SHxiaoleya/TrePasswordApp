package com.trep.passwordmanager.data.model

import kotlinx.serialization.Serializable

@Serializable
data class EncryptedVaultPayload(
    val salt: String,
    val iv: String,
    val data: String,
    val updatedAt: Long
)
