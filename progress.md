# FINORA Android — Development Progress Report

**Document Purpose**: Record of completed milestones, architecture, implemented screens, and verification history for Finora Android.  
**Current Release Target**: V1.0 ("Track"), V1.1 ("Protect & Own"), V1.2 ("Experience & Expand")  
**Last Updated**: September 13, 2026  
**Status**: V1.0, V1.1, and V1.2 Scope Complete, Fully Tested & Verified (100% Passing Unit Tests & Successful Debug Build)

---

## 1. Project Overview & Guiding Principles

Finora is a native Android, offline-first, privacy-first personal finance application built to give users complete control and visibility over their personal finances.

### Core Architectural Principles
* **Native Android**: Built exclusively with Jetpack Compose, Material 3, and Kotlin Coroutines/Flow.
* **Room Database as Single Source of Truth**: Offline-first local SQLite persistence via Room; no network connectivity required for financial operations.
* **Deterministic Financial Math**: Monetary values are tracked in minor currency units (`Long` cents/paise) to eliminate floating-point inaccuracies.
* **Zero Fake Data & Strict Privacy**: Authoritative local records with zero telemetry or unrequested external data transmission.

---

## 2. Completed Milestones

```
[x] V1.0 (Track) — Core Expense & Budget Foundation (Screens 1–12)
[x] V1.1 (Protect & Own) — App Lock, Encrypted Backup & Atomic Restore, CSV/PDF Export
[x] V1.2 (Experience & Expand) — Income & Cash Flow, Accounts/Wallets, Subscriptions, Savings Goals, Android App Widgets & Shortcuts
[ ] V2.0 (Understand) — Safe-to-Spend, Financial Health Score, OCR & Assisted Entry
[ ] V3.0 (Improve) — On-Device AI Financial Coach, What-If Simulator, Audit Ledger
```

---

## 3. Implemented Modules & Features

### V1.0 — Core Expense & Budget Foundation ("Track")
* **Screens 1 to 12**:
  * Screen 1: Welcome & Onboarding (`OnboardingScreen.kt`).
  * Screen 2: Home Dashboard with real-time spending, budget pacing, and weekly rhythm (`HomeScreen.kt`).
  * Screens 3 & 4: Add & Edit Expense with docked custom financial keypad (`AddExpenseScreen.kt`, `FinancialKeypad.kt`).
  * Screen 5: Expense Detail View with edit/delete confirmation guardrails (`ExpenseDetailScreen.kt`).
  * Screen 6: Grouped chronological expenses list (`ExpensesScreen.kt`).
  * Screen 7: Comprehensive Search & Filter modal bottom sheet (`FilterBottomSheet.kt`).
  * Screen 8: Category management with default seeded icons (`CategoryManagementScreen.kt`).
  * Screens 9 & 10: Monthly budget overview, category allocations, and budget detail (`BudgetsScreen.kt`, `BudgetDetailScreen.kt`).
  * Screen 11: Spending analytics and category breakdowns (`AnalyticsScreen.kt`).
  * Screen 12: Settings & Profile overview (`SettingsScreen.kt`).

### V1.1 — "Protect & Own" Milestone
* **Biometric & PIN App Lock**:
  * `core/security/SecurityManager.kt`: PBKDF2WithHmacSHA256 salted PIN hashing, background lock timeout tracking (`IMMEDIATELY`, `1_MINUTE`, `5_MINUTES`), safe reset flow (`FR-SEC-V1.1-001–007`).
  * `ui/screens/security/AppLockScreen.kt` & `PinSetupDialog.kt`: Tactile numeric keypad, biometric prompt with system credentials fallback, UI obscuration on backgrounding (`FR-SEC-V1.1-003`).
  * `MainActivity.kt`: Lifecycle-aware app-lock enforcement across `ON_STOP` / `ON_START`.
* **Export Reports (CSV & PDF)**:
  * `core/export/CsvExporter.kt`: RFC 4180 compliant CSV export covering date, category, payment method, amount, and notes (`FR-RPT-V1.1-001–002`).
  * `core/export/PdfReportGenerator.kt`: Native `android.graphics.pdf.PdfDocument` generation producing clean A4 monthly statements with summary cards, category breakdown, and itemized transaction tables (`FR-RPT-V1.1-003`).
  * `ui/screens/settings/ExportReportBottomSheet.kt`: Storage Access Framework (SAF `CreateDocument`) and Android Share Sheet via `FileProvider`.
* **Encrypted Local Backup & Atomic Restore**:
  * `core/backup/BackupManager.kt`: AES-256-GCM container (`FINORA_ENC` magic header), PBKDF2 key derivation, checksums, atomic multi-table restore with `withTransaction`, preview counts, and Replace/Merge modes (`FR-BKP-V1.1-001–018`).
  * `ui/screens/settings/BackupRestoreBottomSheet.kt`: Interactive backup creation and pre-restore validation inspection.

### V1.2 — "Experience & Expand" Milestone
* **Room Schema Migration 1 -> 2**:
  * Upgraded `FinoraDatabase` to `version = 2` with explicit non-destructive `MIGRATION_1_2`.
  * Added 5 new entities: `AccountEntity`, `IncomeEntity`, `RecurringExpenseEntity`, `SavingsGoalEntity`, and `SavingsContributionEntity`.
  * Extended `BackupManager` to backup and restore all V1.2 entities.
* **Income & Cash Flow Tracking**:
  * `domain/model/CashFlowCalculation.kt`: Deterministic math computing Total Income, Outflow, Net Cash Flow (`income - expenses`), and Savings Rate %.
  * `ui/screens/income/AddIncomeScreen.kt` & `AddIncomeViewModel.kt`: Big amount hero, income source chips (Salary, Freelance, Investment, Gift, Refund, Other), account selector, and docked financial numpad.
  * `ui/screens/income/IncomeHistoryScreen.kt` & `IncomeHistoryViewModel.kt`: Feed of income entries with aggregate monthly inflow and deletion capability.
* **Multi-Account Balances & Wallets**:
  * `ui/screens/accounts/AccountsScreen.kt` & `AccountsViewModel.kt`: Total liquid balance card, account cards with type badges (Cash, Bank Account, Credit Card, Digital Wallet), default account toggle, and Add Account dialog.
* **Subscriptions & Recurring Commitments**:
  * `domain/model/RecurringCalculation.kt`: Normalized monthly commitment engine (weekly, daily, monthly, yearly), annualized cost calculator, and 7-day due-soon tracker.
  * `ui/screens/recurring/RecurringScreen.kt` & `RecurringViewModel.kt`: Commitment overview card, subscriptions list, active/paused switch, and manual confirmation button ("Log Expense") that logs the expense and advances next due date per `FR-REC-V1.2-004`.
* **Savings Goals & Progress Visualizer**:
  * `domain/model/SavingsCalculation.kt`: Progress %, remaining gap, target date pace calculator (e.g. ₹2,500/mo needed).
  * `ui/screens/savings/SavingsScreen.kt` & `SavingsViewModel.kt`: Goal cards with animated progress bars, template chips (Emergency Fund, Vacation, Gadget, Custom), and Deposit/Withdrawal contribution dialog.
* **Dashboard V1.2 Integration**:
  * Enhanced `HomeScreen.kt` with a Net Cash Flow card (Net Inflow/Outflow + Savings Rate %) and Quick Action chips for Income, Accounts, Subscriptions, and Savings.
* **Native Android Integrations**:
  * `res/xml/shortcuts.xml`: Static launcher shortcuts for "Add Expense" and "Add Income".
  * `core/widget/FinoraAppWidgetProvider.kt` & `res/layout/widget_finora_summary.xml`: Home screen widget displaying monthly spend and quick-add action.
  * `AndroidManifest.xml`: Registered shortcuts metadata, app widget provider, deep link schemes (`finora://add_expense`, `finora://add_income`), and share sheet receiver (`ACTION_SEND`).

---

## 4. Test Suite & Build Verification

* **Unit Tests**:
  * `AmountTest.kt`: Minor unit conversions and formatting.
  * `BudgetCalculationTest.kt`: Budget status and threshold calculations.
  * `CashFlowCalculationTest.kt`: Positive/negative cash flow and savings rate calculations.
  * `RecurringCalculationTest.kt`: Monthly normalization and commitment summaries.
  * `SavingsCalculationTest.kt`: Goal progress, gap, and target pace computations.
  * `SecurityManagerTest.kt`: Salted PIN verification, lockout thresholds, timeout evaluation.
  * `CsvExporterTest.kt`: RFC 4180 compliance, header validity, delimiter escaping.
  * `BackupManagerTest.kt`: Encryption, incorrect password rejection, atomic restore round-trip.
  * ViewModel Tests: `HomeViewModelTest`, `AddExpenseViewModelTest`, `BudgetsViewModelTest`, etc.
  * **Result**: 100% tests passing (`./gradlew testDebugUnitTest`).
* **APK Build Verification**:
  * Build command: `./gradlew assembleDebug`
  * **Result**: `BUILD SUCCESSFUL` (0 errors, valid APK generated).
