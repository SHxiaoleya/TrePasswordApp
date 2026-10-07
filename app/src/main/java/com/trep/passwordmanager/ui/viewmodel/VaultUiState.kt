package com.trep.passwordmanager.ui.viewmodel

import com.trep.passwordmanager.data.model.PasswordItem
import com.trep.passwordmanager.data.model.VaultMode

data class VaultUiState(
    val isUnlocked: Boolean = false,
    val hasVault: Boolean = false,
    val vaultMode: VaultMode = VaultMode.SECURE,
    val items: List<PasswordItem> = emptyList(),
    val filteredItems: List<PasswordItem> = emptyList(),
    val searchQuery: String = "",
    val revealedPasswordIds: Set<String> = emptySet(),
    val editingItem: PasswordItem? = null,
    val isItemSheetOpen: Boolean = false,
    val autoLockMinutes: Int = 0,
    val totpCodes: Map<String, String> = emptyMap(),
    val totpRemainingSeconds: Int = 30,
    val snackbarMessage: String? = null,
    val authError: String? = null
)
