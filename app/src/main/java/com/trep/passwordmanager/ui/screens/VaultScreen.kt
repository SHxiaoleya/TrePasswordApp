package com.trep.passwordmanager.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.trep.passwordmanager.data.model.PasswordItem
import com.trep.passwordmanager.data.model.VaultMode
import com.trep.passwordmanager.ui.components.ConfirmDialog
import com.trep.passwordmanager.ui.components.PasswordCard
import com.trep.passwordmanager.ui.components.PasswordItemBottomSheet
import com.trep.passwordmanager.ui.viewmodel.VaultUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    uiState: VaultUiState,
    onSearchChange: (String) -> Unit,
    onSearchClear: () -> Unit,
    onToggleReveal: (String) -> Unit,
    onOpenAdd: () -> Unit,
    onOpenEdit: (PasswordItem) -> Unit,
    onCloseSheet: () -> Unit,
    onSaveItem: (id: String?, name: String, account: String, password: String, note: String, totpSecret: String) -> String?,
    onDeleteItem: (String) -> Unit,
    onGeneratePassword: () -> String,
    onLock: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onCopied: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var itemToDelete by remember { mutableStateOf<PasswordItem?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "密码管理器",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                navigationIcon = {
                    SuggestionChip(
                        onClick = onNavigateToSettings,
                        label = {
                            Text(
                                text = if (uiState.vaultMode == VaultMode.SECURE) "加密存储" else "明文存储",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = if (uiState.vaultMode == VaultMode.SECURE) Icons.Outlined.Security else Icons.Outlined.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = if (uiState.vaultMode == VaultMode.SECURE) {
                            SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        },
                        modifier = Modifier.padding(start = 12.dp)
                    )
                },
                actions = {
                    if (uiState.vaultMode == VaultMode.SECURE) {
                        IconButton(onClick = onLock) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = "锁定",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "安全设置",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenAdd,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("添加密码") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar & Counter
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchChange,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = onSearchClear) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = "清空搜索",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    placeholder = { Text("搜索名称 / 账号 / 备注 / 2FA…") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Count text
                val countText = when {
                    uiState.items.isEmpty() -> ""
                    uiState.searchQuery.isNotBlank() -> "匹配 ${uiState.filteredItems.size} / ${uiState.items.size} 条"
                    else -> "共 ${uiState.items.size} 条"
                }
                if (countText.isNotEmpty()) {
                    Text(
                        text = countText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }

            // List or Empty View
            if (uiState.filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (uiState.items.isEmpty()) "🗂️" else "🔍",
                                    style = MaterialTheme.typography.headlineLarge
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (uiState.items.isEmpty()) "密码库还是空的" else "没有匹配的条目",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (uiState.items.isEmpty()) {
                                "点击右下角「添加密码」按钮，创建你的第一条密码记录。"
                            } else {
                                "换个关键词试试，或清空搜索框。"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = uiState.filteredItems,
                        key = { it.id }
                    ) { item ->
                        PasswordCard(
                            item = item,
                            isRevealed = uiState.revealedPasswordIds.contains(item.id),
                            totpCode = uiState.totpCodes[item.id],
                            totpRemainingSeconds = uiState.totpRemainingSeconds,
                            onToggleReveal = { onToggleReveal(item.id) },
                            onEdit = { onOpenEdit(item) },
                            onDelete = { itemToDelete = item },
                            onCopied = onCopied
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Modal Bottom Sheet
    if (uiState.isItemSheetOpen) {
        PasswordItemBottomSheet(
            sheetState = sheetState,
            item = uiState.editingItem,
            onDismiss = onCloseSheet,
            onGeneratePassword = onGeneratePassword,
            onSave = onSaveItem
        )
    }

    // Delete Confirmation Dialog
    if (itemToDelete != null) {
        ConfirmDialog(
            title = "删除这条记录？",
            message = "「${itemToDelete?.name}」将从密码库中移除，此操作无法撤销。",
            confirmText = "删除",
            isDanger = true,
            onConfirm = {
                itemToDelete?.let { onDeleteItem(it.id) }
                itemToDelete = null
            },
            onDismiss = { itemToDelete = null }
        )
    }
}
