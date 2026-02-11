package com.anonforge.feature.settings.alias

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anonforge.core.network.NetworkResult
import com.anonforge.core.security.ApiKeyManager
import com.anonforge.data.local.prefs.SettingsDataStore
import com.anonforge.domain.model.AliasEmail
import com.anonforge.domain.model.AliasQuota
import com.anonforge.domain.usecase.CheckAliasQuotaUseCase
import com.anonforge.domain.usecase.GetAliasHistoryUseCase
import com.anonforge.domain.usecase.SyncAliasesUseCase
import com.anonforge.domain.usecase.ValidateApiKeyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for Alias Settings screen.
 * Extended for Skill 17 with sync and quota management.
 *
 * SECURITY:
 * - keyHint only shows first 3 chars (e.g., "sl_...")
 * - maskedDisplay shows "•••••••••••••••••"
 * - Full API key is NEVER exposed in state
 */
data class AliasSettingsState(
    val isEnabled: Boolean = false,
    val hasApiKey: Boolean = false,
    val isValidating: Boolean = false,
    val isValid: Boolean? = null,
    val quota: AliasQuota? = null,
    val keyHint: String? = null,
    val maskedDisplay: String = "",
    val isEditingKey: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,

    // Skill 17: Alias history & sync
    val aliases: List<AliasEmail> = emptyList(),
    val aliasCount: Int = 0,
    val isSyncing: Boolean = false,
    val syncResult: String? = null
)

sealed class AliasSettingsEvent {
    data object ApiKeySaved : AliasSettingsEvent()
    data object ApiKeyCleared : AliasSettingsEvent()
    data class ValidationComplete(val isValid: Boolean) : AliasSettingsEvent()
    data class Error(val message: String) : AliasSettingsEvent()
    data class SyncComplete(val count: Int) : AliasSettingsEvent()
}

@HiltViewModel
class AliasSettingsViewModel @Inject constructor(
    private val apiKeyManager: ApiKeyManager,
    private val validateApiKeyUseCase: ValidateApiKeyUseCase,
    private val checkQuotaUseCase: CheckAliasQuotaUseCase,
    private val settingsDataStore: SettingsDataStore,
    private val syncAliasesUseCase: SyncAliasesUseCase,
    private val getAliasHistoryUseCase: GetAliasHistoryUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(AliasSettingsState())
    val state: StateFlow<AliasSettingsState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<AliasSettingsEvent>()
    val events: SharedFlow<AliasSettingsEvent> = _events.asSharedFlow()

    init {
        loadInitialState()
        observeAliasHistory()
    }

    private fun loadInitialState() {
        viewModelScope.launch {
            val isEnabled = settingsDataStore.aliasEnabled.first()
            val hasKey = apiKeyManager.hasApiKey()

            _state.update {
                it.copy(
                    isEnabled = isEnabled && hasKey,
                    hasApiKey = hasKey,
                    keyHint = if (hasKey) apiKeyManager.getKeyHint() else null,
                    maskedDisplay = apiKeyManager.getMaskedDisplay(),
                    isEditingKey = false
                )
            }

            if (hasKey) {
                checkApiKeyAndQuota()
                loadAliasCount()
            }
        }
    }

    /**
     * Observe alias history changes reactively.
     */
    private fun observeAliasHistory() {
        viewModelScope.launch {
            getAliasHistoryUseCase.getAllFlow().collect { aliases ->
                _state.update {
                    it.copy(
                        aliases = aliases,
                        aliasCount = aliases.size
                    )
                }
            }
        }
    }

    private suspend fun loadAliasCount() {
        val count = getAliasHistoryUseCase.getCount()
        _state.update { it.copy(aliasCount = count) }
    }

    fun toggleEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled && !_state.value.hasApiKey) {
                _state.update { it.copy(errorMessage = "Configure API key first") }
                return@launch
            }

            settingsDataStore.setAliasEnabled(enabled)
            _state.update { it.copy(isEnabled = enabled, errorMessage = null) }
        }
    }

    fun startEditingKey() {
        _state.update { it.copy(isEditingKey = true) }
    }

    fun cancelEditingKey() {
        _state.update { it.copy(isEditingKey = false) }
    }

    /**
     * Save and validate API key.
     * SECURITY: Key is CharArray, wiped after use.
     * AUTO-ENABLES alias feature on successful validation.
     */
    fun saveApiKey(apiKey: String) {
        if (apiKey.isBlank()) {
            _state.update { it.copy(errorMessage = "API key cannot be empty") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isValidating = true, errorMessage = null) }

            val keyChars = apiKey.toCharArray()

            when (val result = validateApiKeyUseCase.validateAndStore(keyChars)) {
                is NetworkResult.Success -> {
                    if (result.data) {
                        val newHint = apiKeyManager.getKeyHint()
                        val newMasked = apiKeyManager.getMaskedDisplay()

                        // AUTO-ENABLE alias feature when key is validated successfully
                        settingsDataStore.setAliasEnabled(true)

                        _state.update {
                            it.copy(
                                hasApiKey = true,
                                isEnabled = true,
                                isValid = true,
                                isValidating = false,
                                isEditingKey = false,
                                keyHint = newHint,
                                maskedDisplay = newMasked,
                                successMessage = "API key validated successfully"
                            )
                        }
                        _events.emit(AliasSettingsEvent.ApiKeySaved)
                        checkQuota()
                    } else {
                        _state.update {
                            it.copy(
                                hasApiKey = false,
                                isValid = false,
                                isValidating = false,
                                keyHint = null,
                                maskedDisplay = "",
                                errorMessage = "Invalid API key"
                            )
                        }
                        _events.emit(AliasSettingsEvent.ValidationComplete(false))
                    }
                }
                is NetworkResult.Error -> {
                    _state.update {
                        it.copy(
                            isValidating = false,
                            errorMessage = result.message
                        )
                    }
                    _events.emit(AliasSettingsEvent.Error(result.message))
                }
                is NetworkResult.Loading -> { /* Already handled */ }
            }

            // SECURITY: Wipe input
            keyChars.fill('\u0000')
        }
    }

    /**
     * Clear stored API key.
     */
    fun clearApiKey() {
        viewModelScope.launch {
            apiKeyManager.clearApiKey()
            settingsDataStore.setAliasEnabled(false)

            _state.update {
                it.copy(
                    hasApiKey = false,
                    isEnabled = false,
                    isValid = null,
                    quota = null,
                    keyHint = null,
                    maskedDisplay = "",
                    isEditingKey = false,
                    successMessage = "API key removed"
                )
            }
            _events.emit(AliasSettingsEvent.ApiKeyCleared)
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            checkApiKeyAndQuota()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SKILL 17: Sync Aliases
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Sync aliases from SimpleLogin to local history.
     * Connected to AliasSettingsScreen sync button.
     */
    fun syncAliases() {
        if (!_state.value.hasApiKey) {
            _state.update { it.copy(errorMessage = "Configure API key first") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSyncing = true, syncResult = null, errorMessage = null) }

            when (val result = syncAliasesUseCase()) {
                is NetworkResult.Success -> {
                    val count = result.data
                    val message = if (count > 0) {
                        "$count alias${if (count > 1) "es" else ""} synced"
                    } else {
                        "Already up to date"
                    }

                    _state.update {
                        it.copy(
                            isSyncing = false,
                            syncResult = message,
                            successMessage = message
                        )
                    }
                    _events.emit(AliasSettingsEvent.SyncComplete(count))
                    checkQuota()
                }
                is NetworkResult.Error -> {
                    _state.update {
                        it.copy(
                            isSyncing = false,
                            errorMessage = result.message
                        )
                    }
                    _events.emit(AliasSettingsEvent.Error(result.message))
                }
                is NetworkResult.Loading -> { /* Continue */ }
            }
        }
    }

    /**
     * Clear all alias history.
     * Connected to AliasSettingsScreen clear history button.
     */
    fun clearAliasHistory() {
        viewModelScope.launch {
            getAliasHistoryUseCase.clearAll()
            _state.update { it.copy(successMessage = "Alias history cleared") }
        }
    }

    fun clearMessages() {
        _state.update { it.copy(errorMessage = null, successMessage = null, syncResult = null) }
    }

    private suspend fun checkApiKeyAndQuota() {
        _state.update { it.copy(isValidating = true) }

        when (val result = validateApiKeyUseCase()) {
            is NetworkResult.Success -> {
                _state.update { it.copy(isValid = result.data) }
                if (result.data) {
                    checkQuota()
                } else {
                    _state.update {
                        it.copy(isValidating = false, errorMessage = "API key is invalid")
                    }
                }
            }
            is NetworkResult.Error -> {
                _state.update {
                    it.copy(isValidating = false, errorMessage = result.message)
                }
            }
            is NetworkResult.Loading -> { /* Continue */ }
        }
    }

    private suspend fun checkQuota() {
        when (val result = checkQuotaUseCase()) {
            is NetworkResult.Success -> {
                _state.update {
                    it.copy(quota = result.data, isValidating = false)
                }
            }
            is NetworkResult.Error -> {
                _state.update { it.copy(isValidating = false) }
            }
            else -> {
                _state.update { it.copy(isValidating = false) }
            }
        }
    }
}