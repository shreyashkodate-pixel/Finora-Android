package com.finora.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Navigation routes for Finora V1.0.
 */
sealed class Screen(
    val route: String,
    val label: String = "",
    val icon: ImageVector? = null
) {
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home", "Home", Icons.Default.Home)
    data object Expenses : Screen("expenses", "Expenses", Icons.AutoMirrored.Filled.ReceiptLong)
    data object AddExpense : Screen("add_expense", "Add", Icons.Default.AddCircle)
    data object Budgets : Screen("budgets", "Budgets", Icons.Default.AccountBalanceWallet)
    data object Analytics : Screen("analytics", "Analytics", Icons.Default.BarChart)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    data object CategoryManagement : Screen("categories")

    data object ExpenseDetail : Screen("expense_detail/{expenseId}") {
        fun createRoute(expenseId: String) = "expense_detail/$expenseId"
    }

    companion object {
        val bottomNavScreens = listOf(
            Home,
            Expenses,
            AddExpense,
            Budgets,
            Analytics
        )
    }
}
