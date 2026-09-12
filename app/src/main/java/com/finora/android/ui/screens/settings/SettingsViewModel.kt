package com.finora.android.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.android.core.di.DatabaseModule
import com.finora.android.core.model.AppCurrency
import com.finora.android.data.repository.CategoryRepository
import com.finora.android.data.repository.PaymentMethodRepository
import com.finora.android.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val profileName: String = "Arjun Sharma",
    val profileInitials: String = "AS",
    val localIdentifier: String = "user@device.local",
    val currencyCode: String = "INR",
    val currencySymbol: String = "₹",
    val currencyDisplayName: String = "INR — Indian Rupee (₹)",
    val themeMode: String = "SYSTEM",
    val totalCategoriesCount: Int = 0,
    val customCategoriesCount: Int = 0,
    val totalPaymentMethodsCount: Int = 0,
    val showCurrencyDialog: Boolean = false,
    val showEditNameDialog: Boolean = false,
    val showInfoDialog: Boolean = false,
    val isLoading: Boolean = true,
    val userMessage: String? = null
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SettingsViewModel(
    private val profileRepository: ProfileRepository = DatabaseModule.profileRepository,
    private val categoryRepository: CategoryRepository = DatabaseModule.categoryRepository,
    private val paymentMethodRepository: PaymentMethodRepository = DatabaseModule.paymentMethodRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val profile = profileRepository.getActiveProfile()
                ?: profileRepository.createProfile("Arjun Sharma", AppCurrency.DEFAULT.code)

            profileRepository.getActiveProfileFlow()
                .filterNotNull()
                .flatMapLatest { p ->
                    combine(
                        categoryRepository.getCategoriesFlow(p.id),
                        paymentMethodRepository.getPaymentMethodsFlow(p.id)
                    ) { categories, methods ->
                        val currency = AppCurrency.fromCode(p.currencyCode)
                        val initials = computeInitials(p.name)
                        val sanitizedName = p.name.lowercase().replace(" ", ".")

                        val customCats = categories.count { !it.isDefault }

                        SettingsUiState(
                            profileName = p.name,
                            profileInitials = initials,
                            localIdentifier = "$sanitizedName@device.local",
                            currencyCode = p.currencyCode,
                            currencySymbol = currency.symbol,
                            currencyDisplayName = "${currency.code} — ${currency.displayName} (${currency.symbol})",
                            themeMode = p.themeMode,
                            totalCategoriesCount = categories.size,
                            customCategoriesCount = customCats,
                            totalPaymentMethodsCount = methods.size,
                            showCurrencyDialog = _uiState.value.showCurrencyDialog,
                            showEditNameDialog = _uiState.value.showEditNameDialog,
                            showInfoDialog = _uiState.value.showInfoDialog,
                            isLoading = false,
                            userMessage = _uiState.value.userMessage
                        )
                    }
                }.collect { newState ->
                    _uiState.value = newState
                }
        }
    }

    fun openCurrencyDialog() {
        _uiState.update { it.copy(showCurrencyDialog = true) }
    }

    fun dismissCurrencyDialog() {
        _uiState.update { it.copy(showCurrencyDialog = false) }
    }

    fun selectCurrency(currencyCode: String) {
        viewModelScope.launch {
            profileRepository.updateCurrencyCode(currencyCode)
            _uiState.update {
                it.copy(
                    showCurrencyDialog = false,
                    userMessage = "Base currency changed to $currencyCode."
                )
            }
        }
    }

    fun setThemeMode(themeMode: String) {
        viewModelScope.launch {
            profileRepository.updateThemeMode(themeMode)
            _uiState.update {
                it.copy(userMessage = "Theme updated to ${themeMode.lowercase().replaceFirstChar { c -> c.uppercase() }}.")
            }
        }
    }

    fun openEditNameDialog() {
        _uiState.update { it.copy(showEditNameDialog = true) }
    }

    fun dismissEditNameDialog() {
        _uiState.update { it.copy(showEditNameDialog = false) }
    }

    fun saveProfileName(newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            profileRepository.updateProfileName(trimmed)
            _uiState.update {
                it.copy(
                    showEditNameDialog = false,
                    userMessage = "Profile name updated."
                )
            }
        }
    }

    fun openInfoDialog() {
        _uiState.update { it.copy(showInfoDialog = true) }
    }

    fun dismissInfoDialog() {
        _uiState.update { it.copy(showInfoDialog = false) }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
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
