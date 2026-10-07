package com.trep.passwordmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trep.passwordmanager.ui.screens.AuthScreen
import com.trep.passwordmanager.ui.screens.SecuritySettingsScreen
import com.trep.passwordmanager.ui.screens.VaultScreen
import com.trep.passwordmanager.ui.theme.TrePTheme
import com.trep.passwordmanager.ui.viewmodel.VaultViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = (application as TrePApplication).repository

        setContent {
            TrePTheme(dynamicColor = true) {
                val viewModel: VaultViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(
                            modelClass: Class<T>,
                            extras: CreationExtras
                        ): T {
                            return VaultViewModel(repository) as T
                        }
                    }
                )

                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

enum class CurrentDestination {
    VAULT,
    SETTINGS
}

@Composable
fun MainAppContent(viewModel: VaultViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var currentDestination by remember { mutableStateOf(CurrentDestination.VAULT) }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { viewModel.recordActivity() })
            }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = uiState.isUnlocked to currentDestination,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen_transition"
            ) { (isUnlocked, destination) ->
                if (!isUnlocked) {
                    AuthScreen(
                        uiState = uiState,
                        onUnlock = { pwd, remember ->
                            viewModel.unlockVault(pwd, remember)
                        },
                        onCreateVault = { pwd, confirm ->
                            viewModel.createVault(pwd, confirm)
                        },
                        onClearError = { viewModel.clearAuthError() }
                    )
                } else {
                    when (destination) {
                        CurrentDestination.VAULT -> {
                            VaultScreen(
                                uiState = uiState,
                                onSearchChange = { viewModel.setSearchQuery(it) },
                                onSearchClear = { viewModel.clearSearchQuery() },
                                onToggleReveal = { viewModel.toggleRevealPassword(it) },
                                onOpenAdd = { viewModel.openAddItemSheet() },
                                onOpenEdit = { viewModel.openEditItemSheet(it) },
                                onCloseSheet = { viewModel.closeItemSheet() },
                                onSaveItem = { id, name, account, pwd, note, totp ->
                                    viewModel.saveItem(id, name, account, pwd, note, totp)
                                },
                                onDeleteItem = { viewModel.deleteItem(it) },
                                onGeneratePassword = { viewModel.generateStrongPassword() },
                                onLock = {
                                    viewModel.lock()
                                    currentDestination = CurrentDestination.VAULT
                                },
                                onNavigateToSettings = {
                                    currentDestination = CurrentDestination.SETTINGS
                                },
                                onCopied = { msg ->
                                    viewModel.showSnackbar(msg)
                                }
                            )
                        }
                        CurrentDestination.SETTINGS -> {
                            SecuritySettingsScreen(
                                uiState = uiState,
                                onBack = { currentDestination = CurrentDestination.VAULT },
                                onChangeMasterPassword = { old, new, confirm ->
                                    viewModel.changeMasterPassword(old, new, confirm)
                                },
                                onDisableMasterPassword = { pwd ->
                                    viewModel.disableMasterPassword(pwd)
                                },
                                onEnableMasterPassword = { pwd, confirm ->
                                    viewModel.enableMasterPassword(pwd, confirm)
                                },
                                onSetAutoLockMinutes = { mins ->
                                    viewModel.setAutoLockMinutes(mins)
                                },
                                onClearAllData = {
                                    viewModel.clearAllData()
                                    currentDestination = CurrentDestination.VAULT
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
