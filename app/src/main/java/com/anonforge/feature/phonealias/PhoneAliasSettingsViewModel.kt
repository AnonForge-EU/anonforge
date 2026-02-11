package com.anonforge.feature.phonealias

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anonforge.data.local.prefs.SettingsDataStore
import com.anonforge.domain.model.PhoneAlias
import com.anonforge.domain.repository.PhoneAliasRepository
import com.anonforge.domain.usecase.DeletePhoneAliasUseCase
import com.anonforge.domain.usecase.GetPhoneAliasHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject

/**
 * ViewModel for Phone Alias Settings - Manual Input Mode.
 *
 * FEATURES:
 * - Manual entry of virtual phone numbers
 * - Local encrypted storage for reuse
 * - Set primary number for quick selection
 * - Enable/disable phone aliases in generated identities
 *
 * NO TWILIO/API INTEGRATION - user obtains numbers from external services.
 */
@HiltViewModel
class PhoneAliasSettingsViewModel @Inject constructor(
    private val getPhoneAliasHistoryUseCase: GetPhoneAliasHistoryUseCase,
    private val deletePhoneAliasUseCase: DeletePhoneAliasUseCase,
    private val phoneAliasRepository: PhoneAliasRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _state = MutableStateFlow(PhoneAliasSettingsState())
    val state: StateFlow<PhoneAliasSettingsState> = _state.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            // Load enabled state from preferences
            val isEnabled = settingsDataStore.phoneAliasEnabled.first()
            _state.update { it.copy(isEnabled = isEnabled) }

            // Load existing aliases from local storage
            getPhoneAliasHistoryUseCase().collect { aliases ->
                _state.update {
                    it.copy(
                        aliases = aliases,
                        hasAliases = aliases.isNotEmpty()
                    )
                }
            }
        }
    }

    /**
     * Toggle phone alias feature on/off.
     * Only enabled if at least one alias is configured.
     */
    fun toggleEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val currentAliases = _state.value.aliases
            if (enabled && currentAliases.isEmpty()) {
                _state.update { it.copy(snackbarMessage = "Add a number first") }
                return@launch
            }

            settingsDataStore.setPhoneAliasEnabled(enabled)
            _state.update { it.copy(isEnabled = enabled) }
        }
    }

    /**
     * Add a new phone alias manually.
     *
     * @param phoneNumber The phone number to add
     * @param friendlyName Optional label (e.g., "Hushed", "OnOff")
     * @param onSuccess Message to show on success
     * @param onInvalidFormat Message to show if format is invalid
     * @param onAlreadyExists Message to show if number already exists
     */
    fun addPhoneAlias(
        phoneNumber: String,
        friendlyName: String,
        onSuccess: String,
        onInvalidFormat: String,
        onAlreadyExists: String
    ) {
        viewModelScope.launch {
            // Validate phone number format (remove spaces, hyphens, parentheses)
            val cleaned = phoneNumber.replace(Regex("[\\s()\\-]"), "")
            if (!cleaned.matches(Regex("^\\+?[0-9]{8,15}$"))) {
                _state.update { it.copy(snackbarMessage = onInvalidFormat) }
                return@launch
            }

            // Check if already exists
            val existingAliases = _state.value.aliases
            if (existingAliases.any { it.phoneNumber == cleaned || it.phoneNumber == phoneNumber }) {
                _state.update { it.copy(snackbarMessage = onAlreadyExists) }
                return@launch
            }

            // Create alias - make primary if it's the first one
            val isFirst = existingAliases.isEmpty()
            val alias = PhoneAlias(
                phoneNumber = phoneNumber.trim(),
                friendlyName = friendlyName.trim(),
                isPrimary = isFirst,
                createdAt = Clock.System.now()
            )

            phoneAliasRepository.saveAlias(alias).onSuccess {
                _state.update { it.copy(snackbarMessage = onSuccess) }

                // Auto-enable if this is the first alias
                if (isFirst) {
                    settingsDataStore.setPhoneAliasEnabled(true)
                    _state.update { it.copy(isEnabled = true) }
                }
            }.onFailure { error ->
                _state.update { it.copy(snackbarMessage = error.message ?: "Failed to add number") }
            }
        }
    }

    /**
     * Set an alias as primary (default for new identities).
     */
    fun setPrimary(id: Long, successMessage: String) {
        viewModelScope.launch {
            phoneAliasRepository.setPrimaryAlias(id).onSuccess {
                _state.update { it.copy(snackbarMessage = successMessage) }
            }.onFailure { error ->
                _state.update { it.copy(snackbarMessage = error.message ?: "Failed to update") }
            }
        }
    }

    /**
     * Delete a single alias.
     */
    fun deleteAlias(id: Long, successMessage: String) {
        viewModelScope.launch {
            deletePhoneAliasUseCase(id).onSuccess {
                _state.update {
                    it.copy(
                        aliasToDelete = null,
                        snackbarMessage = successMessage
                    )
                }

                // Disable feature if no aliases remain
                val remainingAliases = _state.value.aliases
                if (remainingAliases.isEmpty()) {
                    settingsDataStore.setPhoneAliasEnabled(false)
                    _state.update { it.copy(isEnabled = false) }
                }
            }.onFailure { error ->
                _state.update { it.copy(snackbarMessage = error.message ?: "Failed to delete") }
            }
        }
    }

    /**
     * Clear all phone aliases.
     */
    fun clearAllAliases() {
        viewModelScope.launch {
            val aliases = _state.value.aliases
            var success = true

            for (alias in aliases) {
                deletePhoneAliasUseCase(alias.id).onFailure {
                    success = false
                }
            }

            if (success) {
                settingsDataStore.setPhoneAliasEnabled(false)
                _state.update {
                    it.copy(
                        isEnabled = false,
                        showClearDialog = false,
                        snackbarMessage = "All numbers removed"
                    )
                }
            } else {
                _state.update { it.copy(snackbarMessage = "Failed to remove some numbers") }
            }
        }
    }

    fun showClearDialog() {
        _state.update { it.copy(showClearDialog = true) }
    }

    fun dismissClearDialog() {
        _state.update { it.copy(showClearDialog = false) }
    }

    fun showDeleteDialog(alias: PhoneAlias) {
        _state.update { it.copy(aliasToDelete = alias) }
    }

    fun dismissDeleteDialog() {
        _state.update { it.copy(aliasToDelete = null) }
    }

    fun showCopiedMessage(message: String) {
        _state.update { it.copy(snackbarMessage = message) }
    }

    /**
     * Alias for showCopiedMessage - used by PhoneAliasSettingsScreen.
     */
    fun showMessage(message: String) {
        _state.update { it.copy(snackbarMessage = message) }
    }

    fun clearSnackbar() {
        _state.update { it.copy(snackbarMessage = null) }
    }
}

/**
 * UI state for Phone Alias Settings screen.
 */
data class PhoneAliasSettingsState(
    val isEnabled: Boolean = false,
    val hasAliases: Boolean = false,
    val aliases: List<PhoneAlias> = emptyList(),
    val showClearDialog: Boolean = false,
    val aliasToDelete: PhoneAlias? = null,
    val snackbarMessage: String? = null
)