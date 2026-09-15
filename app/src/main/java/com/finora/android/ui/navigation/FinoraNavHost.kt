package com.finora.android.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.finora.android.core.di.DatabaseModule
import com.finora.android.ui.components.FinoraEmptyState
import com.finora.android.ui.components.FinoraTopAppBar

/**
 * Top-level navigation host coordinating V1.0 to V1.2 screens.
 */
@Composable
fun FinoraNavHost(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var isReady by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var startRoute by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(Screen.Home.route) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        val active = DatabaseModule.profileRepository.getActiveProfile()
        if (active == null) {
            startRoute = Screen.Onboarding.route
        }
        isReady = true
    }

    if (!isReady) return

    val showBottomBar = currentRoute in Screen.bottomNavScreens.map { it.route }

    val topBarTitle = when (currentRoute) {
        Screen.Home.route -> "Finora"
        Screen.Expenses.route -> "Expenses"
        Screen.AddExpense.route -> "Add Expense"
        Screen.Budgets.route -> "Budgets"
        Screen.Analytics.route -> "Analytics"
        Screen.Settings.route -> "Settings"
        Screen.CategoryManagement.route -> "Categories"
        else -> "Finora"
    }

    val canNavigateBack = currentRoute !in Screen.bottomNavScreens.map { it.route } &&
            currentRoute != Screen.Onboarding.route

    val customTopBarRoutes = listOf(
        Screen.Onboarding.route,
        Screen.AddExpense.route,
        Screen.Income.route,
        Screen.AddIncome.route,
        Screen.Accounts.route,
        Screen.Recurring.route,
        Screen.Savings.route,
        Screen.NotificationSettings.route,
        Screen.ReceiptScan.route,
        Screen.VoiceEntry.route,
        Screen.QuickAdd.route,
        Screen.ShareImport.route,
        Screen.FinancialHealth.route,
        Screen.AnomalyGuard.route,
        Screen.NetWorth.route,
        Screen.StatementReconciliation.route,
        Screen.TravelMode.route,
        Screen.FinancialCoach.route,
        Screen.PurchaseSimulator.route,
        Screen.MerkleAudit.route
    )

    val isExpenseDetail = currentRoute?.startsWith("expense_detail") == true
    val isBudgetDetail = currentRoute?.startsWith("budget_detail") == true
    val isEditExpense = currentRoute?.startsWith("edit_expense") == true

    Scaffold(
        topBar = {
            if (currentRoute !in customTopBarRoutes && !isExpenseDetail && !isBudgetDetail && !isEditExpense) {
                FinoraTopAppBar(
                    title = topBarTitle,
                    canNavigateBack = canNavigateBack,
                    onNavigateBack = { navController.popBackStack() },
                    actions = {
                        if (currentRoute != Screen.Settings.route) {
                            IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                FinoraNavigationBar(navController = navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Onboarding.route) {
                com.finora.android.ui.screens.onboarding.OnboardingScreen(
                    onOnboardingFinished = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Home.route) {
                com.finora.android.ui.screens.home.HomeScreen(
                    onNavigateToAddExpense = { navController.navigate(Screen.AddExpense.route) },
                    onNavigateToExpenses = { navController.navigate(Screen.Expenses.route) },
                    onNavigateToExpenseDetail = { expenseId ->
                        navController.navigate(Screen.ExpenseDetail.createRoute(expenseId))
                    },
                    onNavigateToEdit = { expenseId ->
                        navController.navigate(Screen.EditExpense.createRoute(expenseId))
                    },
                    onNavigateToBudgets = { navController.navigate(Screen.Budgets.route) },
                    onNavigateToIncome = { navController.navigate(Screen.Income.route) },
                    onNavigateToAccounts = { navController.navigate(Screen.Accounts.route) },
                    onNavigateToRecurring = { navController.navigate(Screen.Recurring.route) },
                    onNavigateToSavings = { navController.navigate(Screen.Savings.route) },
                    onNavigateToQuickAdd = { navController.navigate(Screen.QuickAdd.route) },
                    onNavigateToHealth = { navController.navigate(Screen.FinancialHealth.route) },
                    onNavigateToSimulator = { navController.navigate(Screen.PurchaseSimulator.route) },
                    onNavigateToCoach = { navController.navigate(Screen.FinancialCoach.route) },
                    onNavigateToReceiptScan = { navController.navigate(Screen.ReceiptScan.route) },
                    onNavigateToVoiceEntry = { navController.navigate(Screen.VoiceEntry.route) }
                )
            }
            composable(Screen.Expenses.route) {
                com.finora.android.ui.screens.expenses.ExpensesScreen(
                    onNavigateToAddExpense = { navController.navigate(Screen.AddExpense.route) },
                    onNavigateToEdit = { expenseId ->
                        navController.navigate(Screen.EditExpense.createRoute(expenseId))
                    },
                    onExpenseClick = { expenseId ->
                        navController.navigate(Screen.ExpenseDetail.createRoute(expenseId))
                    }
                )
            }
            composable(Screen.AddExpense.route) {
                com.finora.android.ui.screens.add.AddExpenseScreen(
                    onDismiss = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.Home.route)
                        }
                    },
                    onExpenseSaved = {
                        navController.navigate(Screen.Expenses.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(
                route = Screen.EditExpense.route,
                arguments = listOf(
                    androidx.navigation.navArgument("expenseId") {
                        type = androidx.navigation.NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val expenseId = backStackEntry.arguments?.getString("expenseId")
                com.finora.android.ui.screens.add.AddExpenseScreen(
                    expenseId = expenseId,
                    onDismiss = {
                        navController.popBackStack()
                    },
                    onExpenseSaved = {
                        navController.popBackStack()
                    }
                )
            }
            composable(
                route = Screen.ExpenseDetail.route,
                arguments = listOf(
                    androidx.navigation.navArgument("expenseId") {
                        type = androidx.navigation.NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val expenseId = backStackEntry.arguments?.getString("expenseId") ?: ""
                com.finora.android.ui.screens.detail.ExpenseDetailScreen(
                    expenseId = expenseId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = {
                        navController.navigate(Screen.EditExpense.createRoute(expenseId))
                    }
                )
            }
            composable(Screen.Budgets.route) {
                com.finora.android.ui.screens.budgets.BudgetsScreen(
                    onNavigateToBudgetDetail = { budgetId ->
                        navController.navigate(Screen.BudgetDetail.createRoute(budgetId))
                    },
                    onNavigateToAddExpense = {
                        navController.navigate(Screen.AddExpense.route)
                    }
                )
            }
            composable(
                route = Screen.BudgetDetail.route,
                arguments = listOf(
                    androidx.navigation.navArgument("budgetId") {
                        type = androidx.navigation.NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val budgetId = backStackEntry.arguments?.getString("budgetId") ?: ""
                com.finora.android.ui.screens.budgets.detail.BudgetDetailScreen(
                    budgetId = budgetId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToExpenseDetail = { expenseId ->
                        navController.navigate(Screen.ExpenseDetail.createRoute(expenseId))
                    }
                )
            }
            composable(Screen.Analytics.route) {
                com.finora.android.ui.screens.analytics.AnalyticsScreen(
                    onNavigateToAddExpense = { navController.navigate(Screen.AddExpense.route) },
                    onNavigateToExpenseDetail = { expenseId ->
                        navController.navigate(Screen.ExpenseDetail.createRoute(expenseId))
                    }
                )
            }
            composable(Screen.Settings.route) {
                com.finora.android.ui.screens.settings.SettingsScreen(
                    onNavigateToCategories = { navController.navigate(Screen.CategoryManagement.route) },
                    onNavigateToBudgets = { navController.navigate(Screen.Budgets.route) },
                    onNavigateToNotifications = { navController.navigate(Screen.NotificationSettings.route) },
                    onNavigateToNetWorth = { navController.navigate(Screen.NetWorth.route) },
                    onNavigateToReconciliation = { navController.navigate(Screen.StatementReconciliation.route) },
                    onNavigateToTravelMode = { navController.navigate(Screen.TravelMode.route) },
                    onNavigateToAudit = { navController.navigate(Screen.MerkleAudit.route) },
                    onNavigateToAnomalyGuard = { navController.navigate(Screen.AnomalyGuard.route) }
                )
            }
            composable(Screen.CategoryManagement.route) {
                com.finora.android.ui.screens.categories.CategoryManagementScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // V1.2 Screen Composables
            composable(Screen.Income.route) {
                com.finora.android.ui.screens.income.IncomeHistoryScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAddIncome = { navController.navigate(Screen.AddIncome.route) }
                )
            }
            composable(Screen.AddIncome.route) {
                com.finora.android.ui.screens.income.AddIncomeScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Accounts.route) {
                com.finora.android.ui.screens.accounts.AccountsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Recurring.route) {
                com.finora.android.ui.screens.recurring.RecurringScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Savings.route) {
                com.finora.android.ui.screens.savings.SavingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // V2.0 & V3.0 Screen Composables
            composable(Screen.NotificationSettings.route) {
                com.finora.android.ui.screens.notifications.NotificationSettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.ReceiptScan.route) {
                com.finora.android.ui.screens.assisted.ReceiptScanScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onExpenseSaved = {
                        navController.navigate(Screen.Expenses.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.VoiceEntry.route) {
                com.finora.android.ui.screens.assisted.VoiceEntryScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onExpenseSaved = {
                        navController.navigate(Screen.Expenses.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.QuickAdd.route) {
                com.finora.android.ui.screens.assisted.NaturalLanguageQuickAddScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onExpenseSaved = {
                        navController.navigate(Screen.Expenses.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.ShareImport.route) {
                com.finora.android.ui.screens.assisted.ShareImportPreviewScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onExpenseSaved = {
                        navController.navigate(Screen.Expenses.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.FinancialHealth.route) {
                com.finora.android.ui.screens.health.FinancialHealthScoreScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.AnomalyGuard.route) {
                com.finora.android.ui.screens.anomaly.AnomalyGuardScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.NetWorth.route) {
                com.finora.android.ui.screens.networth.NetWorthScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.StatementReconciliation.route) {
                com.finora.android.ui.screens.statement.StatementReconciliationScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onReconciled = {
                        navController.navigate(Screen.Expenses.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.TravelMode.route) {
                com.finora.android.ui.screens.travel.TravelModeScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.FinancialCoach.route) {
                com.finora.android.ui.screens.coach.FinancialCoachScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.PurchaseSimulator.route) {
                com.finora.android.ui.screens.simulator.PurchaseSimulatorScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.MerkleAudit.route) {
                com.finora.android.ui.screens.audit.MerkleAuditScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
