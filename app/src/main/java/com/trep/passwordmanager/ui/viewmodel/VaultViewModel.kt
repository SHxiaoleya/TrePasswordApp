package com.trep.passwordmanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trep.passwordmanager.data.crypto.CryptoManager
import com.trep.passwordmanager.data.crypto.PasswordGenerator
import com.trep.passwordmanager.data.crypto.TotpGenerator
import com.trep.passwordmanager.data.model.PasswordItem
import com.trep.passwordmanager.data.model.VaultMode
import com.trep.passwordmanager.data.repository.VaultRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class VaultViewModel(
    private val repository: VaultRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    private var totpTickerJob: Job? = null
    private var lastActivityTime = System.currentTimeMillis()

    init {
        refreshStateFromRepository()

        viewModelScope.launch {
            repository.isUnlocked.collect { isUnlocked ->
                _uiState.update { it.copy(isUnlocked = isUnlocked) }
                if (isUnlocked) {
                    startTotpTicker()
                } else {
                    stopTotpTicker()
                }
            }
        }

        viewModelScope.launch {
            repository.items.collect { items ->
                _uiState.update { state ->
                    state.copy(
                        items = items,
                        filteredItems = filterList(items, state.searchQuery)
                    )
                }
                updateTotpCodes()
            }
        }

        viewModelScope.launch {
            repository.vaultMode.collect { mode ->
                _uiState.update { it.copy(vaultMode = mode) }
            }
        }

        viewModelScope.launch {
            repository.autoLockMinutes.collect { mins ->
                _uiState.update { it.copy(autoLockMinutes = mins) }
            }
        }

        startAutoLockWatcher()
    }

    fun recordActivity() {
        lastActivityTime = System.currentTimeMillis()
    }

    private fun startAutoLockWatcher() {
        viewModelScope.launch {
            while (isActive) {
                delay(10_000)
                val autoLockMins = repository.getAutoLockMinutes()
                if (autoLockMins > 0 &&
                    _uiState.value.isUnlocked &&
                    _uiState.value.vaultMode == VaultMode.SECURE
                ) {
                    val idleTime = System.currentTimeMillis() - lastActivityTime
                    if (idleTime >= autoLockMins * 60_000L) {
                        lock()
                        showSnackbar("已自动锁定")
                    }
                }
            }
        }
    }

    fun refreshStateFromRepository() {
        _uiState.update {
            it.copy(
                hasVault = repository.hasVault(),
                isUnlocked = repository.isUnlocked.value,
                vaultMode = repository.vaultMode.value,
                autoLockMinutes = repository.autoLockMinutes.value,
                items = repository.items.value,
                filteredItems = filterList(repository.items.value, it.searchQuery)
            )
        }
    }

    private fun filterList(list: List<PasswordItem>, query: String): List<PasswordItem> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return list
        return list.filter {
            it.name.lowercase().contains(q) ||
                    it.account.lowercase().contains(q) ||
                    it.note.lowercase().contains(q) ||
                    it.totpSecret.lowercase().contains(q)
        }
    }

    fun setSearchQuery(query: String) {
        recordActivity()
        _uiState.update {
            it.copy(
                searchQuery = query,
                filteredItems = filterList(it.items, query)
            )
        }
    }

    fun clearSearchQuery() {
        setSearchQuery("")
    }

    fun createVault(masterPassword: String, confirmPassword: String): Boolean {
        if (masterPassword.length < CryptoManager.MIN_MASTER_LENGTH) {
            _uiState.update { it.copy(authError = "主密码至少 ${CryptoManager.MIN_MASTER_LENGTH} 位") }
            return false
        }
        if (masterPassword != confirmPassword) {
            _uiState.update { it.copy(authError = "两次输入的主密码不一致") }
            return false
        }

        return try {
            repository.createVault(masterPassword)
            _uiState.update { it.copy(authError = null, hasVault = true) }
            showSnackbar("密码库已创建")
            true
        } catch (e: Exception) {
            _uiState.update { it.copy(authError = "创建失败：${e.localizedMessage}") }
            false
        }
    }

    fun unlockVault(masterPassword: String, rememberInSession: Boolean): Boolean {
        if (masterPassword.isBlank()) {
            _uiState.update { it.copy(authError = "请输入主密码") }
            return false
        }

        return try {
            repository.unlockVault(masterPassword)
            if (rememberInSession) {
                repository.cachedSessionPassword = masterPassword
            }
            recordActivity()
            _uiState.update { it.copy(authError = null) }
            true
        } catch (e: Exception) {
            _uiState.update { it.copy(authError = "解锁失败：主密码错误或数据损坏") }
            false
        }
    }

    fun lock() {
        repository.lockVault()
        _uiState.update {
            it.copy(
                isUnlocked = false,
                revealedPasswordIds = emptySet(),
                editingItem = null,
                isItemSheetOpen = false
            )
        }
        stopTotpTicker()
    }

    fun toggleRevealPassword(id: String) {
        recordActivity()
        _uiState.update { state ->
            val set = state.revealedPasswordIds.toMutableSet()
            if (set.contains(id)) {
                set.remove(id)
            } else {
                set.add(id)
            }
            state.copy(revealedPasswordIds = set)
        }
    }

    fun openAddItemSheet() {
        recordActivity()
        _uiState.update { it.copy(editingItem = null, isItemSheetOpen = true) }
    }

    fun openEditItemSheet(item: PasswordItem) {
        recordActivity()
        _uiState.update { it.copy(editingItem = item, isItemSheetOpen = true) }
    }

    fun closeItemSheet() {
        recordActivity()
        _uiState.update { it.copy(editingItem = null, isItemSheetOpen = false) }
    }

    fun saveItem(
        id: String?,
        name: String,
        account: String,
        password: String,
        note: String,
        totpSecret: String
    ): String? {
        recordActivity()
        val trimmedName = name.trim()
        val trimmedAccount = account.trim()
        val trimmedPassword = password.trim()
        val trimmedNote = note.trim()
        val normalizedTotp = TotpGenerator.normalizeSecret(totpSecret)

        if (trimmedName.isEmpty()) {
            return "请填写必填项：名称"
        }
        if ((trimmedAccount.isNotEmpty() && trimmedPassword.isEmpty()) ||
            (trimmedPassword.isNotEmpty() && trimmedAccount.isEmpty())
        ) {
            return "账号与密码需成对填写"
        }
        if (trimmedAccount.isEmpty() && normalizedTotp.isEmpty()) {
            return "请填写「账号 + 密码」或「2FA 密钥」"
        }
        if (normalizedTotp.isNotEmpty() && !TotpGenerator.isValidBase32(normalizedTotp)) {
            return "2FA 密钥含无效 Base32 字符"
        }

        val item = PasswordItem(
            id = id ?: java.util.UUID.randomUUID().toString(),
            name = trimmedName,
            account = trimmedAccount,
            password = trimmedPassword,
            note = trimmedNote,
            totpSecret = normalizedTotp
        )

        return try {
            repository.saveItem(item)
            closeItemSheet()
            showSnackbar(if (id != null) "已保存修改" else "已添加到密码库")
            null
        } catch (e: Exception) {
            "保存失败：${e.localizedMessage}"
        }
    }

    fun deleteItem(id: String) {
        recordActivity()
        try {
            repository.deleteItem(id)
            _uiState.update { state ->
                val set = state.revealedPasswordIds.toMutableSet()
                set.remove(id)
                state.copy(revealedPasswordIds = set)
            }
            showSnackbar("已删除")
        } catch (e: Exception) {
            showSnackbar("删除失败：${e.localizedMessage}")
        }
    }

    fun generateStrongPassword(): String {
        recordActivity()
        return PasswordGenerator.generate(16)
    }

    fun setAutoLockMinutes(minutes: Int) {
        recordActivity()
        repository.setAutoLockMinutes(minutes)
        showSnackbar(if (minutes > 0) "已设置 $minutes 分钟自动锁定" else "已关闭自动锁定")
    }

    fun changeMasterPassword(oldMaster: String, newMaster: String, confirmMaster: String): String? {
        recordActivity()
        if (oldMaster.isEmpty() || newMaster.isEmpty() || confirmMaster.isEmpty()) {
            return "请完整填写修改主密码信息"
        }
        if (newMaster.length < CryptoManager.MIN_MASTER_LENGTH) {
            return "新主密码至少 ${CryptoManager.MIN_MASTER_LENGTH} 位"
        }
        if (newMaster != confirmMaster) {
            return "两次新主密码不一致"
        }

        return try {
            repository.changeMasterPassword(oldMaster, newMaster)
            showSnackbar("主密码修改成功")
            null
        } catch (e: Exception) {
            "修改失败：旧主密码错误或处理异常"
        }
    }

    fun disableMasterPassword(currentMaster: String): String? {
        recordActivity()
        return try {
            repository.disableMasterPassword(currentMaster)
            showSnackbar("已关闭主密码，改为明文存储")
            null
        } catch (e: Exception) {
            "主密码验证失败，操作已取消"
        }
    }

    fun enableMasterPassword(newMaster: String, confirmMaster: String): String? {
        recordActivity()
        if (newMaster.length < CryptoManager.MIN_MASTER_LENGTH) {
            return "主密码至少 ${CryptoManager.MIN_MASTER_LENGTH} 位"
        }
        if (newMaster != confirmMaster) {
            return "两次输入的主密码不一致"
        }

        return try {
            repository.enableMasterPassword(newMaster)
            showSnackbar("已开启主密码并完成加密")
            null
        } catch (e: Exception) {
            "操作失败：${e.localizedMessage}"
        }
    }

    fun clearAllData() {
        repository.clearAllData()
        refreshStateFromRepository()
        showSnackbar("已清空本机数据")
    }

    fun showSnackbar(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun clearAuthError() {
        _uiState.update { it.copy(authError = null) }
    }

    private fun startTotpTicker() {
        totpTickerJob?.cancel()
        totpTickerJob = viewModelScope.launch {
            while (isActive) {
                updateTotpCodes()
                delay(1000)
            }
        }
    }

    private fun stopTotpTicker() {
        totpTickerJob?.cancel()
        totpTickerJob = null
    }

    private fun updateTotpCodes() {
        val now = System.currentTimeMillis()
        val remain = TotpGenerator.getRemainingSeconds(now)
        val codes = mutableMapOf<String, String>()

        for (item in _uiState.value.items) {
            if (item.totpSecret.isNotEmpty()) {
                codes[item.id] = try {
                    TotpGenerator.generateTotp(item.totpSecret, now)
                } catch (e: Exception) {
                    "ERR"
                }
            }
        }

        _uiState.update {
            it.copy(
                totpCodes = codes,
                totpRemainingSeconds = remain
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopTotpTicker()
    }
}
