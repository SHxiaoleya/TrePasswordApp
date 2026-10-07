package com.trep.passwordmanager.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.trep.passwordmanager.data.crypto.CryptoManager
import com.trep.passwordmanager.data.model.EncryptedVaultPayload
import com.trep.passwordmanager.data.model.PasswordItem
import com.trep.passwordmanager.data.model.VaultMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.crypto.SecretKey

class VaultRepository(context: Context) {

    companion object {
        private const val PREFS_NAME = "trep_vault_prefs"
        private const val STORAGE_KEY = "pm_secure_v1"
        private const val PLAIN_KEY = "pm_plain_v1"
        private const val MODE_KEY = "pm_mode_v1"
        private const val AUTO_LOCK_KEY = "pm_autolock_v1"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _vaultMode = MutableStateFlow(VaultMode.SECURE)
    val vaultMode: StateFlow<VaultMode> = _vaultMode.asStateFlow()

    private val _items = MutableStateFlow<List<PasswordItem>>(emptyList())
    val items: StateFlow<List<PasswordItem>> = _items.asStateFlow()

    private val _autoLockMinutes = MutableStateFlow(0)
    val autoLockMinutes: StateFlow<Int> = _autoLockMinutes.asStateFlow()

    private var activeCryptoKey: SecretKey? = null
    private var vaultSaltBase64: String? = null
    var cachedSessionPassword: String? = null

    init {
        val savedMode = prefs.getString(MODE_KEY, "secure") ?: "secure"
        _vaultMode.value = VaultMode.fromValue(savedMode)
        _autoLockMinutes.value = prefs.getInt(AUTO_LOCK_KEY, 0)

        if (_vaultMode.value == VaultMode.PLAIN) {
            loadPlain()
            _isUnlocked.value = true
        }
    }

    fun hasVault(): Boolean {
        return prefs.contains(STORAGE_KEY)
    }

    fun getAutoLockMinutes(): Int = _autoLockMinutes.value

    fun setAutoLockMinutes(minutes: Int) {
        _autoLockMinutes.value = minutes
        prefs.edit().putInt(AUTO_LOCK_KEY, minutes).apply()
    }

    private fun loadPlain() {
        val raw = prefs.getString(PLAIN_KEY, null)
        if (raw.isNullOrEmpty()) {
            _items.value = emptyList()
            return
        }
        try {
            _items.value = json.decodeFromString(raw)
        } catch (e: Exception) {
            _items.value = emptyList()
        }
    }

    private fun savePlain() {
        val serialized = json.encodeToString(_items.value)
        prefs.edit().putString(PLAIN_KEY, serialized).apply()
    }

    @Synchronized
    fun createVault(masterPassword: String, initialItems: List<PasswordItem> = emptyList()) {
        val salt = CryptoManager.randomBytes(CryptoManager.SALT_LEN)
        val key = CryptoManager.deriveKey(masterPassword, salt)
        val (ivBase64, dataBase64) = CryptoManager.encryptItems(initialItems, key)

        val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val payload = EncryptedVaultPayload(
            salt = saltBase64,
            iv = ivBase64,
            data = dataBase64,
            updatedAt = System.currentTimeMillis()
        )

        prefs.edit()
            .putString(STORAGE_KEY, json.encodeToString(payload))
            .putString(MODE_KEY, VaultMode.SECURE.value)
            .remove(PLAIN_KEY)
            .apply()

        activeCryptoKey = key
        vaultSaltBase64 = saltBase64
        _items.value = initialItems
        _vaultMode.value = VaultMode.SECURE
        _isUnlocked.value = true
    }

    @Synchronized
    fun unlockVault(masterPassword: String) {
        if (_vaultMode.value == VaultMode.PLAIN) {
            loadPlain()
            _isUnlocked.value = true
            return
        }

        val raw = prefs.getString(STORAGE_KEY, null)
            ?: throw IllegalStateException("密码库不存在")

        val payload: EncryptedVaultPayload = json.decodeFromString(raw)
        val saltBytes = Base64.decode(payload.salt, Base64.NO_WRAP)
        val key = CryptoManager.deriveKey(masterPassword, saltBytes)

        val decryptedList = CryptoManager.decryptItems(payload.iv, payload.data, key)

        activeCryptoKey = key
        vaultSaltBase64 = payload.salt
        _items.value = decryptedList
        _isUnlocked.value = true
    }

    @Synchronized
    fun lockVault() {
        if (_vaultMode.value == VaultMode.PLAIN) {
            return
        }
        activeCryptoKey = null
        vaultSaltBase64 = null
        cachedSessionPassword = null
        _items.value = emptyList()
        _isUnlocked.value = false
    }

    @Synchronized
    private fun persistVault() {
        if (_vaultMode.value == VaultMode.PLAIN) {
            savePlain()
            return
        }

        val key = activeCryptoKey
            ?: throw IllegalStateException("未解锁")
        val salt = vaultSaltBase64
            ?: throw IllegalStateException("未解锁")

        val (ivBase64, dataBase64) = CryptoManager.encryptItems(_items.value, key)
        val payload = EncryptedVaultPayload(
            salt = salt,
            iv = ivBase64,
            data = dataBase64,
            updatedAt = System.currentTimeMillis()
        )
        prefs.edit().putString(STORAGE_KEY, json.encodeToString(payload)).apply()
    }

    @Synchronized
    fun saveItem(item: PasswordItem) {
        val current = _items.value.toMutableList()
        val index = current.indexOfFirst { it.id == item.id }
        if (index != -1) {
            current[index] = item.copy(updatedAt = System.currentTimeMillis())
        } else {
            current.add(0, item.copy(createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()))
        }
        _items.value = current
        persistVault()
    }

    @Synchronized
    fun deleteItem(id: String) {
        val current = _items.value.toMutableList()
        current.removeAll { it.id == id }
        _items.value = current
        persistVault()
    }

    @Synchronized
    fun changeMasterPassword(oldMaster: String, newMaster: String) {
        if (_vaultMode.value != VaultMode.SECURE) {
            throw IllegalStateException("请先开启主密码")
        }

        val raw = prefs.getString(STORAGE_KEY, null)
            ?: throw IllegalStateException("密码库不存在")
        val payload: EncryptedVaultPayload = json.decodeFromString(raw)
        val oldSalt = Base64.decode(payload.salt, Base64.NO_WRAP)
        val oldKey = CryptoManager.deriveKey(oldMaster, oldSalt)

        val decryptedList = CryptoManager.decryptItems(payload.iv, payload.data, oldKey)

        createVault(newMaster, decryptedList)
        cachedSessionPassword = newMaster
    }

    @Synchronized
    fun disableMasterPassword(currentMaster: String) {
        if (_vaultMode.value != VaultMode.SECURE) {
            return
        }

        val currentItems: List<PasswordItem> = if (_isUnlocked.value) {
            _items.value
        } else {
            val raw = prefs.getString(STORAGE_KEY, null)
                ?: throw IllegalStateException("密码库不存在")
            val payload: EncryptedVaultPayload = json.decodeFromString(raw)
            val saltBytes = Base64.decode(payload.salt, Base64.NO_WRAP)
            val key = CryptoManager.deriveKey(currentMaster, saltBytes)
            CryptoManager.decryptItems(payload.iv, payload.data, key)
        }

        _items.value = currentItems
        savePlain()

        prefs.edit()
            .putString(MODE_KEY, VaultMode.PLAIN.value)
            .remove(STORAGE_KEY)
            .apply()

        activeCryptoKey = null
        vaultSaltBase64 = null
        cachedSessionPassword = null
        _vaultMode.value = VaultMode.PLAIN
        _isUnlocked.value = true
    }

    @Synchronized
    fun enableMasterPassword(newMaster: String) {
        val plainItems = _items.value
        createVault(newMaster, plainItems)
        cachedSessionPassword = newMaster
    }

    @Synchronized
    fun clearAllData() {
        prefs.edit().clear().apply()
        activeCryptoKey = null
        vaultSaltBase64 = null
        cachedSessionPassword = null
        _items.value = emptyList()
        _vaultMode.value = VaultMode.SECURE
        _autoLockMinutes.value = 0
        _isUnlocked.value = false
    }
}
