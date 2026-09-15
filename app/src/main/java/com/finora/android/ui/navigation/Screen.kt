package com.finora.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Navigation routes for Finora V1.0 to V1.2.
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

    // V1.2 Routes
    data object Income : Screen("income", "Income", Icons.Default.Paid)
    data object AddIncome : Screen("add_income", "Add Income")
    data object Accounts : Screen("accounts", "Accounts", Icons.Default.AccountBalance)
    data object Recurring : Screen("recurring", "Subscriptions", Icons.Default.Repeat)
    data object Savings : Screen("savings", "Savings", Icons.Default.Savings)

    data object ExpenseDetail : Screen("expense_detail/{expenseId}") {
        fun createRoute(expenseId: String) = "expense_detail/$expenseId"
    }

    data object EditExpense : Screen("edit_expense/{expenseId}") {
        fun createRoute(expenseId: String) = "edit_expense/$expenseId"
    }

    data object BudgetDetail : Screen("budget_detail/{budgetId}") {
        fun createRoute(budgetId: String) = "budget_detail/$budgetId"
    }

    // V2.0 Routes
    data object NotificationSettings : Screen("notification_settings")
    data object ReceiptScan : Screen("receipt_scan")
    data object VoiceEntry : Screen("voice_entry")
    data object QuickAdd : Screen("quick_add")
    data object ShareImport : Screen("share_import")
    data object FinancialHealth : Screen("financial_health")
    data object AnomalyGuard : Screen("anomaly_guard")
    data object NetWorth : Screen("net_worth")
    data object StatementReconciliation : Screen("statement_reconciliation")
    data object TravelMode : Screen("travel_mode")

    // V3.0 Routes
    data object FinancialCoach : Screen("financial_coach")
    data object PurchaseSimulator : Screen("purchase_simulator")
    data object MerkleAudit : Screen("merkle_audit")

    companion object {
        val bottomNavScreens: List<Screen>
            get() = listOf(
                Home,
                Expenses,
                AddExpense,
                Budgets,
                Analytics
            )
    }
}
