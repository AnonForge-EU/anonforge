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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Sort orders offered in the vault. */
enum class VaultSortOrder { CREATED_DESC, EXPIRY_ASC, NAME_ASC }

data class VaultState(
    val identities: List<DomainIdentity> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val revealedIdentityIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val sortOrder: VaultSortOrder = VaultSortOrder.CREATED_DESC,
    val deletedMessage: String? = null,
    val snackbarMessage: String? = null
) {
    /**
     * The list actually shown: [identities] filtered by [searchQuery] and
     * ordered by [sortOrder]. Computed on read so it always reflects the
     * latest source list without a separate sync path.
     */
    val displayedIdentities: List<DomainIdentity>
        get() {
            val needle = searchQuery.trim().lowercase()
            val filtered = if (needle.isEmpty()) {
                identities
            } else {
                identities.filter { identity ->
                    identity.customName?.lowercase()?.contains(needle) == true ||
                        identity.fullName.fullDisplay.lowercase().contains(needle) ||
                        identity.email?.value?.lowercase()?.contains(needle) == true ||
                        identity.phone.value.lowercase().contains(needle)
                }
            }
            return when (sortOrder) {
                VaultSortOrder.CREATED_DESC ->
                    filtered.sortedByDescending { it.createdAt }
                VaultSortOrder.EXPIRY_ASC ->
                    filtered.sortedWith(compareBy(nullsLast()) { it.expiresAt })
                VaultSortOrder.NAME_ASC ->
                    filtered.sortedBy { (it.customName ?: it.fullName.fullDisplay).lowercase() }
            }
        }

    /** True when a search is active but nothing matches (vs. an empty vault). */
    val isEmptySearchResult: Boolean
        get() = identities.isNotEmpty() && displayedIdentities.isEmpty()
}

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
    // Search & Sort
    // ═══════════════════════════════════════════════════════════════════════

    fun setSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun clearSearch() {
        _state.update { it.copy(searchQuery = "") }
    }

    fun setSortOrder(order: VaultSortOrder) {
        _state.update { it.copy(sortOrder = order) }
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

    /**
     * Pull-to-refresh handler. The list is already kept in sync via Room Flow,
     * so this is mostly visual feedback — we briefly hold the spinner so the
     * user gets confirmation the gesture was registered.
     */
    fun refresh() {
        if (_state.value.isRefreshing) return
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            delay(REFRESH_FEEDBACK_MS)
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    private companion object {
        const val REFRESH_FEEDBACK_MS = 600L
    }

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