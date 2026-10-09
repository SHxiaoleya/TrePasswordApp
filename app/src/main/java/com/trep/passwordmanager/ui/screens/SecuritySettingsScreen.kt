package com.trep.passwordmanager.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.LockReset
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.trep.passwordmanager.data.model.VaultMode
import com.trep.passwordmanager.ui.components.ConfirmDialog
import com.trep.passwordmanager.ui.components.PromptPasswordDialog
import com.trep.passwordmanager.ui.viewmodel.VaultUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    uiState: VaultUiState,
    onBack: () -> Unit,
    onChangeMasterPassword: (old: String, new: String, confirm: String) -> String?,
    onDisableMasterPassword: (currentPassword: String) -> String?,
    onEnableMasterPassword: (newPassword: String, confirm: String) -> String?,
    onSetAutoLockMinutes: (Int) -> Unit,
    onClearAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDisableConfirm by remember { mutableStateOf(false) }
    var showPromptPasswordForDisable by remember { mutableStateOf(false) }
    var showEnableDialog by remember { mutableStateOf(false) }

    var showFirstClearConfirm by remember { mutableStateOf(false) }
    var showSecondClearConfirm by remember { mutableStateOf(false) }

    // Change password form state
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var oldPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmNewPasswordVisible by remember { mutableStateOf(false) }
    var changePasswordError by remember { mutableStateOf<String?>(null) }

    // Enable master password state
    var enablePassword by remember { mutableStateOf("") }
    var enableConfirmPassword by remember { mutableStateOf("") }
    var enableError by remember { mutableStateOf<String?>(null) }

    // Auto lock options
    val autoLockOptions = listOf(
        0 to "自动锁定：关闭",
        1 to "自动锁定：1 分钟",
        5 to "自动锁定：5 分钟",
        15 to "自动锁定：15 分钟"
    )
    var autoLockExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "安全设置",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回密码库")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Storage Mode Info Card
            ElevatedCard(modifier = Modifier.fillMaxWidth().animateContentSize()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "存储方式",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = if (uiState.vaultMode == VaultMode.SECURE) "加密存储" else "明文存储",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (uiState.vaultMode == VaultMode.SECURE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "开启主密码后，密码库使用 PBKDF2 (200,000次，SHA-256) 派生 256 位密钥，并以 AES-GCM 加密存储于设备本地；关闭后改为明文存储。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Master Password Switch Card
            ElevatedCard(modifier = Modifier.fillMaxWidth().animateContentSize()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "主密码开关",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (uiState.vaultMode == VaultMode.SECURE) "关闭主密码将改为明文存储；建议保持开启" else "当前为明文存储，建议设置主密码重新加密",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        if (uiState.vaultMode == VaultMode.SECURE) {
                            OutlinedButton(
                                onClick = { showDisableConfirm = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("关闭主密码")
                            }
                        } else {
                            Button(onClick = { showEnableDialog = true }) {
                                Text("开启主密码")
                            }
                        }
                    }
                }
            }

            // Auto Lock Timeout Card
            if (uiState.vaultMode == VaultMode.SECURE) {
                ElevatedCard(modifier = Modifier.fillMaxWidth().animateContentSize()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Timer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "自动锁定时间",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "无操作达到设定时间后自动锁定，需重新输入主密码解锁。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        ExposedDropdownMenuBox(
                            expanded = autoLockExpanded,
                            onExpandedChange = { autoLockExpanded = !autoLockExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val currentOptionLabel = autoLockOptions.find { it.first == uiState.autoLockMinutes }?.second
                                ?: "自动锁定：关闭"

                            OutlinedTextField(
                                value = currentOptionLabel,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = autoLockExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )

                            ExposedDropdownMenu(
                                expanded = autoLockExpanded,
                                onDismissRequest = { autoLockExpanded = false }
                            ) {
                                autoLockOptions.forEach { (mins, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            onSetAutoLockMinutes(mins)
                                            autoLockExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Change Master Password Card
            if (uiState.vaultMode == VaultMode.SECURE) {
                ElevatedCard(modifier = Modifier.fillMaxWidth().animateContentSize()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.LockReset,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "修改主密码",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        // Old Password
                        OutlinedTextField(
                            value = oldPassword,
                            onValueChange = {
                                oldPassword = it
                                changePasswordError = null
                            },
                            label = { Text("旧主密码") },
                            placeholder = { Text("请输入旧主密码") },
                            singleLine = true,
                            visualTransformation = if (oldPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { oldPasswordVisible = !oldPasswordVisible }) {
                                    Icon(
                                        imageVector = if (oldPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = null
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // New Password
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = {
                                newPassword = it
                                changePasswordError = null
                            },
                            label = { Text("新主密码") },
                            placeholder = { Text("至少 8 位") },
                            singleLine = true,
                            visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                    Icon(
                                        imageVector = if (newPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = null
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Confirm New Password
                        OutlinedTextField(
                            value = confirmNewPassword,
                            onValueChange = {
                                confirmNewPassword = it
                                changePasswordError = null
                            },
                            label = { Text("确认新主密码") },
                            placeholder = { Text("请再次输入新主密码") },
                            singleLine = true,
                            visualTransformation = if (confirmNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { confirmNewPasswordVisible = !confirmNewPasswordVisible }) {
                                    Icon(
                                        imageVector = if (confirmNewPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = null
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (changePasswordError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = changePasswordError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val err = onChangeMasterPassword(oldPassword, newPassword, confirmNewPassword)
                                if (err != null) {
                                    changePasswordError = err
                                } else {
                                    oldPassword = ""
                                    newPassword = ""
                                    confirmNewPassword = ""
                                    changePasswordError = null
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("修改主密码")
                        }
                    }
                }
            }

            // Danger Zone Card
            OutlinedCard(
                colors = androidx.compose.material3.CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteForever,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "危险操作",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "删除本机保存的密码库（包括加密数据与明文数据），所有记录将被永久抹除且不可撤销。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showFirstClearConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("清空本机数据")
                    }
                }
            }
        }
    }

    // Disable Master Password Confirmation
    if (showDisableConfirm) {
        ConfirmDialog(
            title = "关闭主密码？",
            message = "关闭后，密码库将以明文保存在本机中，任何能访问此应用的人都能直接看到密码内容。",
            confirmText = "确认关闭",
            isDanger = true,
            onConfirm = {
                showDisableConfirm = false
                showPromptPasswordForDisable = true
            },
            onDismiss = { showDisableConfirm = false }
        )
    }

    // Prompt for password before disabling master password
    if (showPromptPasswordForDisable) {
        PromptPasswordDialog(
            title = "验证主密码",
            message = "关闭主密码前需要验证当前主密码以解密数据。",
            confirmText = "验证并关闭",
            onConfirm = { password ->
                val error = onDisableMasterPassword(password)
                if (error == null) {
                    showPromptPasswordForDisable = false
                }
            },
            onDismiss = { showPromptPasswordForDisable = false }
        )
    }

    // Enable Master Password Dialog
    if (showEnableDialog) {
        var enablePwdVisible by remember { mutableStateOf(false) }
        var enableConfirmPwdVisible by remember { mutableStateOf(false) }

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showEnableDialog = false },
            title = { Text("开启主密码并加密") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "设置主密码后，将使用 PBKDF2 + AES-GCM 加密保存数据。请牢记主密码，忘记将无法恢复。",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = enablePassword,
                        onValueChange = {
                            enablePassword = it
                            enableError = null
                        },
                        label = { Text("新主密码") },
                        placeholder = { Text("至少 8 位") },
                        singleLine = true,
                        visualTransformation = if (enablePwdVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { enablePwdVisible = !enablePwdVisible }) {
                                Icon(
                                    imageVector = if (enablePwdVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = enableConfirmPassword,
                        onValueChange = {
                            enableConfirmPassword = it
                            enableError = null
                        },
                        label = { Text("确认新主密码") },
                        placeholder = { Text("请再次输入") },
                        singleLine = true,
                        visualTransformation = if (enableConfirmPwdVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { enableConfirmPwdVisible = !enableConfirmPwdVisible }) {
                                Icon(
                                    imageVector = if (enableConfirmPwdVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (enableError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = enableError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val error = onEnableMasterPassword(enablePassword, enableConfirmPassword)
                        if (error != null) {
                            enableError = error
                        } else {
                            showEnableDialog = false
                            enablePassword = ""
                            enableConfirmPassword = ""
                            enableError = null
                        }
                    }
                ) {
                    Text("开启并加密")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEnableDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // First Clear Confirmation
    if (showFirstClearConfirm) {
        ConfirmDialog(
            title = "清空全部数据？",
            message = "将删除本机保存的密码库（包括加密数据与明文数据），且无法恢复。",
            confirmText = "继续清空",
            isDanger = true,
            onConfirm = {
                showFirstClearConfirm = false
                showSecondClearConfirm = true
            },
            onDismiss = { showFirstClearConfirm = false }
        )
    }

    // Second Clear Confirmation
    if (showSecondClearConfirm) {
        ConfirmDialog(
            title = "最后确认",
            message = "此操作不可撤销，确定要清空本机所有密码数据吗？",
            confirmText = "全部清空",
            isDanger = true,
            onConfirm = {
                showSecondClearConfirm = false
                onClearAllData()
            },
            onDismiss = { showSecondClearConfirm = false }
        )
    }
}
