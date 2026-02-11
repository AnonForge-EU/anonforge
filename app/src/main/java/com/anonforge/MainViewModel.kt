package com.anonforge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anonforge.data.local.prefs.SettingsDataStore
import com.anonforge.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for MainActivity.
 *
 * Observes app-wide settings that affect the root composable:
 * - Theme mode (Skill 18)
 *
 * This ViewModel exists at the Activity level to provide theme
 * state before any navigation occurs.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    init {
        observeThemeMode()
    }

    /**
     * Observe theme mode changes from DataStore.
     * Updates immediately when user changes theme in Settings.
     */
    private fun observeThemeMode() {
        viewModelScope.launch {
            settingsDataStore.themeMode.collect { mode ->
                _themeMode.value = mode
            }
        }
    }
}