package com.finora.android.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.android.core.di.DatabaseModule
import com.finora.android.core.model.AppCurrency
import com.finora.android.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val name: String = "Arjun Sharma",
    val initials: String = "AS",
    val selectedCurrencyCode: String = "INR",
    val selectedThemeMode: String = "SYSTEM",
    val preloadCategories: Boolean = true,
    val showCurrencyDialog: Boolean = false,
    val isSubmitting: Boolean = false,
    val isOnboardingComplete: Boolean = false,
    val errorMessage: String? = null
)

class OnboardingViewModel(
    private val profileRepository: ProfileRepository = DatabaseModule.profileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        checkExistingProfile()
    }

    private fun checkExistingProfile() {
        viewModelScope.launch {
            val existing = profileRepository.getActiveProfile()
            if (existing != null) {
                _uiState.update { it.copy(isOnboardingComplete = true) }
            }
        }
    }

    fun updateName(name: String) {
        val initials = computeInitials(name)
        _uiState.update {
            it.copy(
                name = name,
                initials = initials,
                errorMessage = null
            )
        }
    }

    fun updateCurrency(currencyCode: String) {
        _uiState.update {
            it.copy(
                selectedCurrencyCode = currencyCode,
                showCurrencyDialog = false
            )
        }
    }

    fun updateTheme(themeMode: String) {
        _uiState.update { it.copy(selectedThemeMode = themeMode) }
    }

    fun updatePreloadCategories(preload: Boolean) {
        _uiState.update { it.copy(preloadCategories = preload) }
    }

    fun openCurrencyDialog() {
        _uiState.update { it.copy(showCurrencyDialog = true) }
    }

    fun dismissCurrencyDialog() {
        _uiState.update { it.copy(showCurrencyDialog = false) }
    }

    fun completeOnboarding() {
        val trimmedName = _uiState.value.name.trim()
        if (trimmedName.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Display name cannot be empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            try {
                profileRepository.createProfile(
                    name = trimmedName,
                    currencyCode = _uiState.value.selectedCurrencyCode,
                    themeMode = _uiState.value.selectedThemeMode
                )
                _uiState.update { it.copy(isSubmitting = false, isOnboardingComplete = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = e.message ?: "Failed to initialize vault profile"
                    )
                }
            }
        }
    }

    private fun computeInitials(name: String): String {
        val parts = name.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
        return when {
            parts.size >= 2 -> "${parts[0].first().uppercase()}${parts[1].first().uppercase()}"
            parts.size == 1 && parts[0].length >= 2 -> parts[0].take(2).uppercase()
            parts.size == 1 -> parts[0].take(1).uppercase()
            else -> "FN"
        }
    }
}
