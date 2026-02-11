package com.anonforge.feature.generator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anonforge.core.network.NetworkResult
import com.anonforge.core.security.ApiKeyManager
import com.anonforge.data.local.prefs.SettingsDataStore
import com.anonforge.domain.model.AliasEmail
import com.anonforge.domain.model.DomainIdentity
import com.anonforge.domain.model.Email
import com.anonforge.domain.model.ExpiryDuration
import com.anonforge.domain.model.Gender
import com.anonforge.domain.model.GenderPreference
import com.anonforge.domain.model.GenerationPreferences
import com.anonforge.domain.model.Nationality
import com.anonforge.domain.model.Phone
import com.anonforge.domain.model.PhoneAlias
import com.anonforge.domain.repository.IdentityRepository
import com.anonforge.domain.repository.PhoneAliasRepository
import com.anonforge.domain.usecase.CreateAliasUseCase
import com.anonforge.domain.usecase.GenerateIdentityUseCase
import com.anonforge.domain.usecase.GetAliasHistoryUseCase
import com.anonforge.domain.usecase.GetPhoneAliasHistoryUseCase
import com.anonforge.domain.usecase.GetPrimaryAliasUseCase
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
import kotlin.time.Duration

/**
 * State for Generator screen.
 * Extended with email and phone alias selection state.
 */
data class GeneratorState(
    // Identity preview
    val previewIdentity: DomainIdentity? = null,
    val isGenerating: Boolean = false,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,

    // Field-specific loading states
    val isRefreshingName: Boolean = false,
    val isRefreshingDob: Boolean = false,
    val isRefreshingAddress: Boolean = false,
    val isRefreshingEmail: Boolean = false,
    val isRefreshingPhone: Boolean = false,

    // Generation options
    val selectedGender: Gender = Gender.MALE,
    val genderPreference: GenderPreference = GenderPreference.RANDOM,
    val selectedNationality: Nationality = Nationality.DEFAULT,
    val includeAddress: Boolean = true,
    val isTemporary: Boolean = false,
    val selectedExpiry: ExpiryDuration = ExpiryDuration.ONE_WEEK,
    val customDays: Int = 0,

    // Age range (from settings)
    val ageRangeMin: Int = GenerationPreferences.MIN_AGE,
    val ageRangeMax: Int = GenerationPreferences.MAX_AGE,

    // Service configuration status
    val isEmailConfigured: Boolean = false,
    val isPhoneConfigured: Boolean = false,

    // ═══════════════════════════════════════════════════════════════════════════
    // Email Alias Selection State
    // ═══════════════════════════════════════════════════════════════════════════
    val showAliasDialog: Boolean = false,
    val existingAliases: List<AliasEmail> = emptyList(),
    val primaryAlias: AliasEmail? = null,
    val isCreatingAlias: Boolean = false,
    val createdAlias: String? = null,
    val aliasQuotaWarning: String? = null,
    val aliasError: String? = null,
    val selectedEmail: String? = null,

    // ═══════════════════════════════════════════════════════════════════════════
    // Phone Alias Selection State
    // ═══════════════════════════════════════════════════════════════════════════
    val showPhoneAliasDialog: Boolean = false,
    val phoneAliases: List<PhoneAlias> = emptyList(),
    val primaryPhoneAlias: PhoneAlias? = null,
    val selectedPhone: String? = null,

    // Error handling
    val error: String? = null
)

/**
 * Events for one-shot UI actions.
 */
sealed class GeneratorEvent {
    data class ShowSnackbar(val message: String, val actionLabel: String? = null) : GeneratorEvent()
    data class CopyToClipboard(val text: String) : GeneratorEvent()
}

@HiltViewModel
class GeneratorViewModel @Inject constructor(
    private val generateIdentityUseCase: GenerateIdentityUseCase,
    private val identityRepository: IdentityRepository,
    private val settingsDataStore: SettingsDataStore,
    private val apiKeyManager: ApiKeyManager,
    private val createAliasUseCase: CreateAliasUseCase,
    private val getAliasHistoryUseCase: GetAliasHistoryUseCase,
    private val getPrimaryAliasUseCase: GetPrimaryAliasUseCase,
    private val getPhoneAliasHistoryUseCase: GetPhoneAliasHistoryUseCase,
    private val phoneAliasRepository: PhoneAliasRepository
) : ViewModel() {

    private val _state = MutableStateFlow(GeneratorState())
    val state: StateFlow<GeneratorState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<GeneratorEvent>()
    val events: SharedFlow<GeneratorEvent> = _events.asSharedFlow()

    init {
        // First check service configurations and load aliases
        // Then generate preview after data is loaded
        initializeAndGenerate()
    }

    /**
     * Initialize services and generate preview.
     * Ensures aliases are loaded before first generation.
     */
    private fun initializeAndGenerate() {
        viewModelScope.launch {
            // 1. Load preferences
            val nationality = settingsDataStore.nationality.first()
            val ageMin = settingsDataStore.ageRangeMin.first()
            val ageMax = settingsDataStore.ageRangeMax.first()
            val genderPref = settingsDataStore.getGenderPreference()
            val resolvedGender = resolveGender(genderPref)

            _state.update {
                it.copy(
                    selectedNationality = nationality,
                    ageRangeMin = ageMin,
                    ageRangeMax = ageMax,
                    genderPreference = genderPref,
                    selectedGender = resolvedGender
                )
            }

            // 2. Check email configuration
            val emailConfigured = settingsDataStore.aliasEnabled.first() && apiKeyManager.hasApiKey()

            // 3. Check phone configuration
            val phoneEnabled = settingsDataStore.phoneAliasEnabled.first()

            _state.update {
                it.copy(
                    isEmailConfigured = emailConfigured,
                    isPhoneConfigured = phoneEnabled
                )
            }

            // 4. Load email aliases if configured (await completion)
            if (emailConfigured) {
                val aliases = getAliasHistoryUseCase.getRecent(20)
                val primary = getPrimaryAliasUseCase()
                _state.update {
                    it.copy(
                        existingAliases = aliases,
                        primaryAlias = primary
                    )
                }
            }

            // 5. Load phone aliases if enabled (await completion)
            if (phoneEnabled) {
                val phoneAliases = getPhoneAliasHistoryUseCase().first()
                val primaryPhone = phoneAliasRepository.getPrimaryAlias()
                _state.update {
                    it.copy(
                        phoneAliases = phoneAliases,
                        primaryPhoneAlias = primaryPhone
                    )
                }
            }

            // 6. NOW generate preview with all data loaded
            generatePreview()
        }
    }

    /**
     * Refresh service configuration status.
     * Reloads aliases and regenerates preview.
     */
    fun refreshServiceConfigurations() {
        viewModelScope.launch {
            // Reload email configuration
            val emailConfigured = settingsDataStore.aliasEnabled.first() && apiKeyManager.hasApiKey()
            val phoneEnabled = settingsDataStore.phoneAliasEnabled.first()

            _state.update {
                it.copy(
                    isEmailConfigured = emailConfigured,
                    isPhoneConfigured = phoneEnabled
                )
            }

            // Reload email aliases if configured
            if (emailConfigured) {
                val aliases = getAliasHistoryUseCase.getRecent(20)
                val primary = getPrimaryAliasUseCase()
                _state.update {
                    it.copy(
                        existingAliases = aliases,
                        primaryAlias = primary
                    )
                }
            }

            // Reload phone aliases if enabled
            if (phoneEnabled) {
                val phoneAliases = getPhoneAliasHistoryUseCase().first()
                val primaryPhone = phoneAliasRepository.getPrimaryAlias()
                _state.update {
                    it.copy(
                        phoneAliases = phoneAliases,
                        primaryPhoneAlias = primaryPhone
                    )
                }
            }

            // Now generate with updated data
            generatePreview()
        }
    }

    /**
     * Resolve gender based on preference.
     * If RANDOM, picks a random gender.
     * Otherwise, uses the specified gender.
     */
    private fun resolveGender(preference: GenderPreference): Gender {
        return when (preference) {
            GenderPreference.RANDOM -> Gender.random()
            GenderPreference.MALE -> Gender.MALE
            GenderPreference.FEMALE -> Gender.FEMALE
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Email Alias Selection
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Load email alias data from repository.
     */
    private fun loadAliasData() {
        viewModelScope.launch {
            val aliases = getAliasHistoryUseCase.getRecent(20)
            val primary = getPrimaryAliasUseCase()

            _state.update {
                it.copy(
                    existingAliases = aliases,
                    primaryAlias = primary
                )
            }
        }
    }

    /**
     * Show alias selection dialog.
     */
    fun showAliasSelectionDialog() {
        _state.update { it.copy(showAliasDialog = true, aliasError = null) }
        loadAliasData()
    }

    /**
     * Hide alias selection dialog.
     */
    fun hideAliasSelectionDialog() {
        _state.update { it.copy(showAliasDialog = false) }
    }

    /**
     * Create new alias via SimpleLogin API.
     * Uses quota-aware creation to warn user when approaching limits.
     */
    fun createNewAlias() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isCreatingAlias = true,
                    aliasError = null,
                    aliasQuotaWarning = null
                )
            }

            // Use quota-aware creation for better UX
            val (result, quotaWarning) = createAliasUseCase.createWithQuotaCheck()

            when (result) {
                is NetworkResult.Success -> {
                    val newAlias = result.data
                    _state.update {
                        it.copy(
                            isCreatingAlias = false,
                            createdAlias = newAlias.email,
                            selectedEmail = newAlias.email,
                            aliasQuotaWarning = quotaWarning
                        )
                    }

                    // Show quota warning via Snackbar if approaching limit
                    quotaWarning?.let { warning ->
                        _events.emit(GeneratorEvent.ShowSnackbar(warning))
                    }

                    // Update preview identity with new email
                    updatePreviewWithEmail(newAlias.email)
                    // Reload alias list
                    loadAliasData()
                }
                is NetworkResult.Error -> {
                    _state.update {
                        it.copy(
                            isCreatingAlias = false,
                            aliasError = result.message.ifEmpty { "Failed to create alias" }
                        )
                    }
                }
                is NetworkResult.Loading -> {
                    // Already showing loading state via isCreatingAlias
                }
            }
        }
    }

    /**
     * Select an existing email alias from history.
     */
    fun selectAlias(alias: AliasEmail) {
        _state.update {
            it.copy(
                selectedEmail = alias.email,
                showAliasDialog = false
            )
        }
        updatePreviewWithEmail(alias.email)
    }

    /**
     * Use the primary email alias.
     */
    @Suppress("unused") // Public API called from GeneratorScreen "Use Primary" button
    fun selectPrimaryAlias() {
        _state.value.primaryAlias?.let { primary ->
            selectAlias(primary)
        }
    }

    /**
     * Clear email selection (revert to generated email).
     */
    @Suppress("unused") // Public API called from GeneratorScreen to reset email
    fun clearEmailSelection() {
        _state.update { it.copy(selectedEmail = null) }
        // Re-generate identity with random email
        generatePreview()
    }

    /**
     * Update preview identity with selected email.
     */
    private fun updatePreviewWithEmail(email: String) {
        val current = _state.value.previewIdentity ?: return
        _state.update {
            it.copy(previewIdentity = current.copy(email = Email(email)))
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Phone Alias Selection
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Load phone alias data from repository.
     */
    private fun loadPhoneAliasData() {
        viewModelScope.launch {
            getPhoneAliasHistoryUseCase().collect { aliases ->
                val primary = aliases.find { it.isPrimary }
                _state.update {
                    it.copy(
                        phoneAliases = aliases,
                        primaryPhoneAlias = primary,
                        isPhoneConfigured = aliases.isNotEmpty()
                    )
                }
            }
        }
    }

    /**
     * Show phone alias selection dialog.
     */
    fun showPhoneAliasDialog() {
        _state.update { it.copy(showPhoneAliasDialog = true) }
        loadPhoneAliasData()
    }

    /**
     * Hide phone alias selection dialog.
     */
    @Suppress("unused") // Public API called from PhoneAliasDialog dismiss
    fun hidePhoneAliasDialog() {
        _state.update { it.copy(showPhoneAliasDialog = false) }
    }

    /**
     * Select a phone alias.
     */
    fun selectPhoneAlias(alias: PhoneAlias) {
        _state.update {
            it.copy(
                selectedPhone = alias.phoneNumber,
                showPhoneAliasDialog = false
            )
        }
        updatePreviewWithPhone(alias.phoneNumber)

        // Record usage
        viewModelScope.launch {
            phoneAliasRepository.recordUsage(alias.id)
        }
    }

    /**
     * Use the primary phone alias.
     */
    @Suppress("unused") // Public API called from PhoneAliasDialog "Use Primary" button
    fun selectPrimaryPhoneAlias() {
        _state.value.primaryPhoneAlias?.let { primary ->
            selectPhoneAlias(primary)
        }
    }

    /**
     * Update preview identity with selected phone.
     */
    private fun updatePreviewWithPhone(phoneNumber: String) {
        val current = _state.value.previewIdentity ?: return
        _state.update {
            it.copy(previewIdentity = current.copy(phone = Phone(phoneNumber)))
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Generation Options
    // ═══════════════════════════════════════════════════════════════════════════

    fun setGender(gender: Gender) {
        _state.update { it.copy(selectedGender = gender) }
        generatePreview()
    }

    fun setNationality(nationality: Nationality) {
        viewModelScope.launch {
            settingsDataStore.setNationality(nationality)
        }
        _state.update { it.copy(selectedNationality = nationality) }
        generatePreview()
    }

    fun setIncludeAddress(include: Boolean) {
        _state.update { it.copy(includeAddress = include) }
        generatePreview()
    }

    fun setTemporary(isTemporary: Boolean) {
        _state.update { it.copy(isTemporary = isTemporary) }
    }

    fun setExpiryOption(expiry: ExpiryDuration) {
        _state.update { it.copy(selectedExpiry = expiry, customDays = 0) }
    }

    fun setCustomDays(days: Int) {
        _state.update { it.copy(customDays = days) }
    }

    @Suppress("unused") // Public API called from GeneratorScreen age range slider
    fun setAgeRange(min: Int, max: Int) {
        viewModelScope.launch {
            settingsDataStore.setAgeRange(min, max)
        }
        _state.update {
            it.copy(
                ageRangeMin = min,
                ageRangeMax = max
            )
        }
        generatePreview()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Identity Generation
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Generate a new identity preview.
     * Uses primary phone alias if available, otherwise generates fake number.
     */
    fun generatePreview() {
        viewModelScope.launch {
            _state.update { it.copy(isGenerating = true, error = null) }

            try {
                // Resolve gender for this generation
                val gender = if (_state.value.genderPreference == GenderPreference.RANDOM) {
                    Gender.random()
                } else {
                    _state.value.selectedGender
                }

                val result = generateIdentityUseCase(
                    gender = gender,
                    nationality = _state.value.selectedNationality,
                    includeAddress = _state.value.includeAddress,
                    expiryDuration = calculateExpiryDuration(),
                    ageMin = _state.value.ageRangeMin,
                    ageMax = _state.value.ageRangeMax
                )

                result.onSuccess { identity ->
                    // Apply selected/primary phone alias if configured
                    val withPhone = applyPhoneAliasIfConfigured(identity)

                    // Apply email alias: selected > primary > generated
                    val withEmail = applyEmailAliasIfConfigured(withPhone)

                    _state.update {
                        it.copy(
                            previewIdentity = withEmail,
                            isGenerating = false,
                            selectedGender = gender
                        )
                    }
                }.onFailure { e ->
                    _state.update {
                        it.copy(isGenerating = false, error = e.message)
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isGenerating = false, error = e.message)
                }
            }
        }
    }

    /**
     * Apply phone alias to identity if configured.
     * Uses selected phone > primary alias > generated number.
     */
    private fun applyPhoneAliasIfConfigured(identity: DomainIdentity): DomainIdentity {
        // 1. Use explicitly selected phone
        _state.value.selectedPhone?.let { phone ->
            return identity.copy(phone = Phone(phone))
        }

        // 2. Use primary phone alias if enabled
        if (_state.value.isPhoneConfigured) {
            _state.value.primaryPhoneAlias?.let { primary ->
                return identity.copy(phone = Phone(primary.phoneNumber))
            }
        }

        // 3. Use generated phone (already in identity)
        return identity
    }

    /**
     * Apply email alias to identity if configured.
     * Uses selected email > primary alias > generated email.
     */
    private fun applyEmailAliasIfConfigured(identity: DomainIdentity): DomainIdentity {
        // 1. Use explicitly selected email
        _state.value.selectedEmail?.let { email ->
            return identity.copy(email = Email(email))
        }

        // 2. Use primary email alias if configured
        if (_state.value.isEmailConfigured) {
            _state.value.primaryAlias?.let { primary ->
                return identity.copy(email = Email(primary.email))
            }
        }

        // 3. Use generated email (already in identity, may be null)
        return identity
    }

    /**
     * Calculate expiry duration based on current settings.
     */
    private fun calculateExpiryDuration(): Duration? {
        if (!_state.value.isTemporary) return null

        return if (_state.value.customDays > 0) {
            ExpiryDuration.fromDays(_state.value.customDays)
        } else {
            _state.value.selectedExpiry.duration
        }
    }

    fun refreshAll() = generatePreview()

    fun refreshName() {
        val currentIdentity = _state.value.previewIdentity ?: return

        viewModelScope.launch {
            _state.update { it.copy(isRefreshingName = true) }

            try {
                val result = generateIdentityUseCase(
                    gender = _state.value.selectedGender,
                    nationality = _state.value.selectedNationality,
                    includeAddress = false,
                    expiryDuration = null,
                    ageMin = _state.value.ageRangeMin,
                    ageMax = _state.value.ageRangeMax
                )

                result.onSuccess { newIdentity ->
                    _state.update {
                        it.copy(
                            previewIdentity = currentIdentity.copy(fullName = newIdentity.fullName),
                            isRefreshingName = false
                        )
                    }
                }.onFailure {
                    _state.update { it.copy(isRefreshingName = false) }
                }
            } catch (_: Exception) {
                _state.update { it.copy(isRefreshingName = false) }
            }
        }
    }

    fun refreshDateOfBirth() {
        val currentIdentity = _state.value.previewIdentity ?: return

        viewModelScope.launch {
            _state.update { it.copy(isRefreshingDob = true) }

            try {
                val result = generateIdentityUseCase(
                    gender = _state.value.selectedGender,
                    nationality = _state.value.selectedNationality,
                    includeAddress = false,
                    expiryDuration = null,
                    ageMin = _state.value.ageRangeMin,
                    ageMax = _state.value.ageRangeMax
                )

                result.onSuccess { newIdentity ->
                    _state.update {
                        it.copy(
                            previewIdentity = currentIdentity.copy(dateOfBirth = newIdentity.dateOfBirth),
                            isRefreshingDob = false
                        )
                    }
                }.onFailure {
                    _state.update { it.copy(isRefreshingDob = false) }
                }
            } catch (_: Exception) {
                _state.update { it.copy(isRefreshingDob = false) }
            }
        }
    }

    fun refreshAddress() {
        val currentIdentity = _state.value.previewIdentity ?: return
        if (!_state.value.includeAddress) return

        viewModelScope.launch {
            _state.update { it.copy(isRefreshingAddress = true) }

            try {
                val result = generateIdentityUseCase(
                    gender = _state.value.selectedGender,
                    nationality = _state.value.selectedNationality,
                    includeAddress = true,
                    expiryDuration = null,
                    ageMin = _state.value.ageRangeMin,
                    ageMax = _state.value.ageRangeMax
                )

                result.onSuccess { newIdentity ->
                    _state.update {
                        it.copy(
                            previewIdentity = currentIdentity.copy(address = newIdentity.address),
                            isRefreshingAddress = false
                        )
                    }
                }.onFailure {
                    _state.update { it.copy(isRefreshingAddress = false) }
                }
            } catch (_: Exception) {
                _state.update { it.copy(isRefreshingAddress = false) }
            }
        }
    }

    /**
     * Refresh phone - if phone aliases are configured, show dialog.
     * Otherwise generate a new fake number.
     */
    fun refreshPhone() {
        if (_state.value.isPhoneConfigured && _state.value.phoneAliases.isNotEmpty()) {
            // Show phone alias selection dialog
            showPhoneAliasDialog()
        } else {
            // Generate a new fake phone number
            regeneratePhone()
        }
    }

    /**
     * Generate a new fake phone number (not using aliases).
     */
    private fun regeneratePhone() {
        val currentIdentity = _state.value.previewIdentity ?: return

        viewModelScope.launch {
            _state.update { it.copy(isRefreshingPhone = true) }

            try {
                val result = generateIdentityUseCase(
                    gender = _state.value.selectedGender,
                    nationality = _state.value.selectedNationality,
                    includeAddress = false,
                    expiryDuration = null,
                    ageMin = _state.value.ageRangeMin,
                    ageMax = _state.value.ageRangeMax
                )

                result.onSuccess { newIdentity ->
                    _state.update {
                        it.copy(
                            previewIdentity = currentIdentity.copy(phone = newIdentity.phone),
                            isRefreshingPhone = false,
                            selectedPhone = null // Clear selection since we regenerated
                        )
                    }
                }.onFailure {
                    _state.update { it.copy(isRefreshingPhone = false) }
                }
            } catch (_: Exception) {
                _state.update { it.copy(isRefreshingPhone = false) }
            }
        }
    }

    /**
     * Refresh email field - opens alias selection dialog.
     */
    fun refreshEmail() {
        if (_state.value.isEmailConfigured) {
            showAliasSelectionDialog()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Save Identity
    // ═══════════════════════════════════════════════════════════════════════════

    fun saveIdentity() {
        val identity = _state.value.previewIdentity ?: return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }

            try {
                identityRepository.insertIdentity(identity)
                _state.update { it.copy(isSaving = false, savedSuccessfully = true) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isSaving = false, error = "Failed to save: ${e.message}")
                }
            }
        }
    }

    fun clearSavedState() {
        _state.update { it.copy(savedSuccessfully = false) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    /**
     * Clear alias-related messages.
     */
    @Suppress("unused") // Public API called from GeneratorScreen after showing messages
    fun clearAliasMessages() {
        _state.update {
            it.copy(
                aliasError = null,
                aliasQuotaWarning = null
            )
        }
    }
}