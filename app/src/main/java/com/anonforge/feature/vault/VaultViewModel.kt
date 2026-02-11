package com.anonforge.feature.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anonforge.core.security.SecureClipboardManager
import com.anonforge.domain.model.DomainIdentity
import com.anonforge.domain.repository.IdentityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VaultState(
    val identities: List<DomainIdentity> = emptyList(),
    val isLoading: Boolean = true,
    val revealedIdentityIds: Set<String> = emptySet(),
    val deletedMessage: String? = null,
    val snackbarMessage: String? = null
)

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val identityRepository: IdentityRepository,
    private val clipboardManager: SecureClipboardManager
) : ViewModel() {

    private val _state = MutableStateFlow(VaultState())
    val state: StateFlow<VaultState> = _state.asStateFlow()

    init {
        observeIdentities()
    }

    private fun observeIdentities() {
        viewModelScope.launch {
            identityRepository.getAllIdentitiesFlow()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = emptyList()
                )
                .collect { identities: List<DomainIdentity> ->
                    _state.update {
                        it.copy(
                            identities = identities,
                            isLoading = false
                        )
                    }
                }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Reveal/Hide
    // ═══════════════════════════════════════════════════════════════════════

    fun toggleReveal(identityId: String) {
        _state.update { currentState ->
            val newRevealed = if (identityId in currentState.revealedIdentityIds) {
                currentState.revealedIdentityIds - identityId
            } else {
                currentState.revealedIdentityIds + identityId
            }
            currentState.copy(revealedIdentityIds = newRevealed)
        }
    }

    fun isRevealed(identityId: String): Boolean {
        return identityId in _state.value.revealedIdentityIds
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Delete
    // ═══════════════════════════════════════════════════════════════════════

    fun deleteIdentity(identityId: String) {
        viewModelScope.launch {
            try {
                identityRepository.deleteIdentity(identityId)
                _state.update {
                    it.copy(
                        revealedIdentityIds = it.revealedIdentityIds - identityId,
                        deletedMessage = "Identity deleted"
                    )
                }
            } catch (_: Exception) {
                _state.update { it.copy(deletedMessage = "Failed to delete identity") }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Rename (Skill 13)
    // ═══════════════════════════════════════════════════════════════════════

    fun renameIdentity(identityId: String, newName: String) {
        viewModelScope.launch {
            try {
                identityRepository.updateCustomName(identityId, newName.ifBlank { null })
                _state.update { it.copy(snackbarMessage = "Identity renamed") }
            } catch (_: Exception) {
                _state.update { it.copy(snackbarMessage = "Failed to rename identity") }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Copy (Skill 13)
    // ═══════════════════════════════════════════════════════════════════════

    fun copyField(label: String, value: String) {
        clipboardManager.copyToClipboard(value, isSensitive = true)
        _state.update { it.copy(snackbarMessage = "Copied $label") }
    }

    fun copyAllFields(fields: Map<String, String>) {
        clipboardManager.copyFormattedBlock(fields)
        _state.update { it.copy(snackbarMessage = "Copied all fields") }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // State Management
    // ═══════════════════════════════════════════════════════════════════════

    fun clearDeletedMessage() {
        _state.update { it.copy(deletedMessage = null) }
    }

    fun clearSnackbar() {
        _state.update { it.copy(snackbarMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        clipboardManager.cancelPendingClear()
    }
}