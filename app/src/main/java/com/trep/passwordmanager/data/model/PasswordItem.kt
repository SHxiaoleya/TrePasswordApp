package com.trep.passwordmanager.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class PasswordItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val account: String = "",
    val password: String = "",
    val note: String = "",
    val totpSecret: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
