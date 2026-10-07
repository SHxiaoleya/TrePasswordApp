package com.trep.passwordmanager.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.trep.passwordmanager.data.model.PasswordItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordItemBottomSheet(
    sheetState: SheetState,
    item: PasswordItem?,
    onDismiss: () -> Unit,
    onGeneratePassword: () -> String,
    onSave: (id: String?, name: String, account: String, password: String, note: String, totpSecret: String) -> String?
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var account by remember { mutableStateOf(item?.account ?: "") }
    var password by remember { mutableStateOf(item?.password ?: "") }
    var note by remember { mutableStateOf(item?.note ?: "") }
    var totpSecret by remember { mutableStateOf(item?.totpSecret ?: "") }

    var passwordVisible by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(item) {
        name = item?.name ?: ""
        account = item?.account ?: ""
        password = item?.password ?: ""
        note = item?.note ?: ""
        totpSecret = item?.totpSecret ?: ""
        errorMessage = null
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        modifier = Modifier.imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = if (item != null) "编辑密码" else "添加密码",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Name Field
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    errorMessage = null
                },
                label = { Text("名称 *") },
                placeholder = { Text("例如：GitHub / 微信") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Account Field
            OutlinedTextField(
                value = account,
                onValueChange = {
                    account = it
                    errorMessage = null
                },
                label = { Text("账号") },
                placeholder = { Text("邮箱 / 用户名 / 手机号") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                label = { Text("密码") },
                placeholder = { Text("请输入密码") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (passwordVisible) "隐藏密码" else "显示密码"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Random Password Generator Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                FilledTonalButton(
                    onClick = {
                        password = onGeneratePassword()
                        passwordVisible = true
                        errorMessage = null
                    },
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Casino,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("随机密码 (16位)", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2FA Secret Field
            OutlinedTextField(
                value = totpSecret,
                onValueChange = {
                    totpSecret = it
                    errorMessage = null
                },
                label = { Text("2FA 密钥（Base32）") },
                placeholder = { Text("例如：JBSWY3DPEHPK3PXP") },
                supportingText = { Text("填入后列表中会实时显示 6 位动态验证码") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Note Field
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("备注") },
                placeholder = { Text("可选备注") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            // Error Display
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Actions: Save & Cancel
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("取消")
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = {
                        val error = onSave(item?.id, name, account, password, note, totpSecret)
                        if (error != null) {
                            errorMessage = error
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("保存")
                }
            }
        }
    }
}
