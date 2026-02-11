package com.anonforge.feature.aliasimport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anonforge.domain.model.FetchAliasesResult
import com.anonforge.domain.model.ImportResult
import com.anonforge.domain.model.RemoteAlias
import com.anonforge.domain.usecase.FetchRemoteEmailAliasesUseCase
import com.anonforge.domain.usecase.ImportAliasesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for alias import functionality.
 *
 * NOTE: With Skill 17.2, phone numbers are managed manually (Hushed/OnOff).
 * Only email alias fetching from SimpleLogin is supported.
 */
@HiltViewModel
class AliasImportViewModel @Inject constructor(
    private val fetchEmails: FetchRemoteEmailAliasesUseCase,
    private val importUseCase: ImportAliasesUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(AliasImportState())
    val state: StateFlow<AliasImportState> = _state.asStateFlow()

    /**
     * Fetch email aliases from SimpleLogin.
     */
    fun fetchEmailAliases() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = fetchEmails()
            _state.update { it.copy(isLoading = false, emailFetchResult = result) }
        }
    }

    /**
     * Import selected email aliases to local history.
     */
    fun importSelected(emails: List<RemoteAlias.Email>) {
        viewModelScope.launch {
            _state.update { it.copy(isImporting = true) }
            val result = importUseCase.importEmails(emails)
            _state.update { it.copy(isImporting = false, importResult = result) }

            // Refresh email list after successful import
            if (result.successCount > 0) {
                fetchEmailAliases()
            }
        }
    }

    /**
     * Retry fetching email aliases.
     */
    fun retry() {
        fetchEmailAliases()
    }

    /**
     * Clear import result after displaying.
     */
    fun clearImportResult() {
        _state.update { it.copy(importResult = null) }
    }

    /**
     * Reset state to initial values.
     */
    fun reset() {
        _state.value = AliasImportState()
    }
}

/**
 * UI state for alias import screen.
 */
data class AliasImportState(
    val isLoading: Boolean = false,
    val isImporting: Boolean = false,
    val emailFetchResult: FetchAliasesResult? = null,
    val importResult: ImportResult? = null
)