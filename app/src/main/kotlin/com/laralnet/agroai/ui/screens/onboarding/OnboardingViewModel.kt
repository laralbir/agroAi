package com.laralnet.agroai.ui.screens.onboarding

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laralnet.agroai.ui.screens.settings.SettingsViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    companion object {
        val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    }

    /** null = still loading DataStore; false = needs onboarding; true = already done */
    val onboardingDone = dataStore.data
        .map { it[KEY_ONBOARDING_DONE] ?: false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = null)

    val savedCalendarAccount = dataStore.data
        .map { it[SettingsViewModel.KEY_SELECTED_ACCOUNT]?.ifBlank { null } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setCalendarAccount(email: String) = viewModelScope.launch {
        if (email.isNotBlank()) {
            dataStore.edit { it[SettingsViewModel.KEY_SELECTED_ACCOUNT] = email.trim() }
        }
    }

    fun markDone() {
        viewModelScope.launch {
            dataStore.edit { it[KEY_ONBOARDING_DONE] = true }
        }
    }
}
