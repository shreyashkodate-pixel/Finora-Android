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
 * Top-level navigation host coordinating V1.0 screens.
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

    Scaffold(
        topBar = {
            val isExpenseDetail = currentRoute?.startsWith("expense_detail") == true
            val isBudgetDetail = currentRoute?.startsWith("budget_detail") == true
            val isEditExpense = currentRoute?.startsWith("edit_expense") == true
            if (currentRoute != Screen.Onboarding.route && currentRoute != Screen.AddExpense.route && !isExpenseDetail && !isBudgetDetail && !isEditExpense) {
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
                    onNavigateToBudgets = { navController.navigate(Screen.Budgets.route) }
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
                    onNavigateToBudgets = { navController.navigate(Screen.Budgets.route) }
                )
            }
            composable(Screen.CategoryManagement.route) {
                com.finora.android.ui.screens.categories.CategoryManagementScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
