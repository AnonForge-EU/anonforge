package com.anonforge.feature.disclaimer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anonforge.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Disclaimer screen.
 * Handles storing the user's acceptance of the disclaimer.
 */
@HiltViewModel
class DisclaimerViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    /**
     * Marks the disclaimer as accepted in preferences.
     */
    fun acceptDisclaimer() {
        viewModelScope.launch {
            preferencesRepository.setDisclaimerAccepted(true)
        }
    }
}