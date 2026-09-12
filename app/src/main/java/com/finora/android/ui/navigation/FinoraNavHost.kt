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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    val showBottomBar = currentRoute in Screen.bottomNavScreens.map { it.route }

    val topBarTitle = when (currentRoute) {
        Screen.Home.route -> "Finora"
        Screen.Expenses.route -> "Expenses"
        Screen.AddExpense.route -> "Add Expense"
        Screen.Budgets.route -> "Budgets"
        Screen.Analytics.route -> "Analytics"
        Screen.Settings.route -> "Settings"
        else -> "Finora"
    }

    val canNavigateBack = currentRoute !in Screen.bottomNavScreens.map { it.route } &&
            currentRoute != Screen.Onboarding.route

    Scaffold(
        topBar = {
            val isExpenseDetail = currentRoute?.startsWith("expense_detail") == true
            if (currentRoute != Screen.Onboarding.route && currentRoute != Screen.AddExpense.route && !isExpenseDetail) {
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
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                FinoraEmptyState(
                    title = "No Expenses Yet",
                    description = "Start tracking your spending to see your dashboard and analytics.",
                    icon = Icons.Default.Savings,
                    actionButtonText = "Add First Expense",
                    onActionClick = { navController.navigate(Screen.AddExpense.route) }
                )
            }
            composable(Screen.Expenses.route) {
                com.finora.android.ui.screens.expenses.ExpensesScreen(
                    onNavigateToAddExpense = { navController.navigate(Screen.AddExpense.route) },
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
                            popUpTo(Screen.Home.route)
                        }
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
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Budgets.route) {
                FinoraEmptyState(
                    title = "No Monthly Budget Set",
                    description = "Set a monthly spending limit to receive pacing alerts and guardrails.",
                    icon = Icons.Default.AccountBalanceWallet,
                    actionButtonText = "Set Monthly Budget",
                    onActionClick = {}
                )
            }
            composable(Screen.Analytics.route) {
                FinoraEmptyState(
                    title = "Analytics Unavailable",
                    description = "Record expenses to generate local, deterministic spending breakdowns.",
                    icon = Icons.Default.BarChart,
                    actionButtonText = "Record Expense",
                    onActionClick = { navController.navigate(Screen.AddExpense.route) }
                )
            }
            composable(Screen.Settings.route) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Settings & Profile",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}
