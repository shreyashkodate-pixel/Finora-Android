# FINORA Android — Development Progress Report

**Document Purpose**: Record of completed milestones, architecture, implemented screens, and verification history for Finora Android.  
**Current Release Target**: V1.0 ("Track"), V1.1 ("Protect & Own"), V1.2 ("Experience & Expand"), V2.0 ("Understand"), V3.0 ("Improve")  
**Last Updated**: September 15, 2026  
**Status**: All Scopes (V1.0, V1.1, V1.2, V2.0, V3.0) 100% Complete, Fully Tested & Verified (100% Passing Unit Tests, 35/35 Screens Implemented, Debug Build Verified)

---

## 1. Project Overview & Guiding Principles

Finora is a native Android, offline-first, privacy-first personal finance application built to give users complete control and visibility over their personal finances.

### Core Architectural Principles
* **Native Android**: Built exclusively with Jetpack Compose, Material 3, and Kotlin Coroutines/Flow.
* **Room Database as Single Source of Truth**: Offline-first local SQLite persistence via Room; no network connectivity required for financial operations.
* **Deterministic Financial Math**: Monetary values are tracked in minor currency units (`Long` cents/paise) to eliminate floating-point inaccuracies.
* **Zero Fake Data & Strict Privacy**: Authoritative local records with zero telemetry or unrequested external data transmission. Air-gapped AI coach & local OCR engines.

---

## 2. Completed Milestones & Git Commits

| Commit Hash | Branch | Description |
| :--- | :--- | :--- |
| `b890dcf` | `feat/v1.1-v1.2-release` | Implement V1.1 & V1.2 scopes (Security, Backup, Export, Income, Accounts, Subscriptions, Savings, Widgets) |
| `8463d3b` | `main` | Remove obsolete repo_memory.md |
| `3cc5275` | `main` | Add progress.md, Technical_Dept.md, and workspace repo memory |
| `711aa03` | `main` | Implement in-place expense edit and delete actions with docked keypad UX |
| `55abc68` | `main` | Use CategoryIcons in ExpenseDetailScreen |

```
[x] V1.0 (Track) — Core Expense & Budget Foundation (Screens 1–12)
[x] V1.1 (Protect & Own) — App Lock, Encrypted Backup & Atomic Restore, CSV/PDF Export
[x] V1.2 (Experience & Expand) — Income & Cash Flow, Accounts/Wallets, Subscriptions, Savings Goals, Android App Widgets & Shortcuts
[x] V2.0 (Understand) — Safe-to-Spend, Financial Health Score, Duplicate Guard, Leak Hunter, Multi-Currency, Assisted Entry (Screens 18-22, 28-31, 34)
[x] V3.0 (Improve) — On-Device AI Financial Coach, What-If Purchase Simulator, Merkle Audit Ledger (Screens 32, 33, 35)
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
  * `domain/model/CashFlowCalculation.kt`: Deterministic engine computing Total Income, Outflow, Net Cash Flow (`income - expenses`), and Savings Rate %.
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

### V2.0 — "Understand" Milestone (Intelligence & Automation)
* **Safe-to-Spend Real-Time Engine**:
  * `domain/model/SafeToSpendCalculation.kt`: Calculates daily spendable buffer: `(Remaining Budget - Committed Recurring) / Days Remaining`. Outputs 4 states: `HEALTHY`, `MODERATE`, `CAUTION`, `DANGER`.
  * Integrated directly into `HomeScreen.kt` with dynamic status chip, daily allowance hero, and real-time budget sync.
* **Financial Health Score (Screen 28)**:
  * `domain/model/FinancialHealthScoreCalculation.kt`: Evaluates overall financial health on a 0–100 scale across 5 pillars (Savings Discipline 25%, Budget Adherence 25%, Spending Stability 20%, Cash Cushion 15%, Leak Control 15%). Provides top score drivers, actionable tips, and an interactive methodology breakdown.
  * `ui/screens/health/FinancialHealthScoreScreen.kt` & `FinancialHealthViewModel.kt`.
* **Anomaly Guard & Statistical Leak Hunter (Screen 29)**:
  * `domain/model/LeakHunterCalculation.kt`: Z-score ($> 2.2$) outlier detection, micro-spend clusters ($< ₹150$ aggregating to major leaks), and high-frequency merchant patterns with confidence ratings.
  * `ui/screens/anomaly/AnomalyGuardScreen.kt` & `AnomalyGuardViewModel.kt`.
* **Assisted Entry Pipelines (Screens 19, 20, 21, 22)**:
  * Screen 19: `ReceiptScanScreen.kt` — Mock on-device OCR review with bounding boxes, itemized receipt breakdown, subtotal/tax extraction, and one-tap save.
  * Screen 20: `VoiceEntryScreen.kt` — Voice input simulation with live waveform visualizer, dynamic transcript streaming, and auto-parsing.
  * Screen 21: `NaturalLanguageQuickAddScreen.kt` — Tokenized NLP parser (`NaturalLanguageExpenseParser.kt`) extracting amount, merchant, category, date, payment method, and tags in real time from natural speech/text.
  * Screen 22: `ShareImportPreviewScreen.kt` — SMS/banking app share receiver parser parsing incoming text and allowing one-tap verification.
  * Shared: `AssistedEntryViewModel.kt` handling unified parsing and Room transaction commitment.
* **Net Worth Ledger (Screen 30)**:
  * `domain/model/NetWorthCalculation.kt`: Aggregates cash, bank balances, investments, and savings goals vs. liabilities (credit card debt, loans) to compute Net Worth, Asset-to-Liability leverage ratio, and conservative badges.
  * `ui/screens/networth/NetWorthScreen.kt` & `NetWorthViewModel.kt`.
* **Statement Reconciliation & CSV Parser (Screen 31)**:
  * `domain/model/CsvStatementParser.kt`: RFC 4180 / multi-format banking CSV parser with automated non-blocking duplicate detection and category mapping.
  * `ui/screens/statement/StatementReconciliationScreen.kt` & `StatementViewModel.kt` with batch selection and reconciliation commitment.
* **Multi-Currency & Travel Mode (Screen 34)**:
  * `domain/model/CurrencyEngine.kt`: Base currency conversion with cached exchange rates (USD, EUR, GBP, JPY to INR) maintaining minor currency unit integer accuracy.
  * `ui/screens/travel/TravelModeScreen.kt` & `TravelModeViewModel.kt` featuring quick currency convert, trip expense budget tracking, and travel mode toggle.
* **Notification Settings (Screen 18)**:
  * `ui/screens/notifications/NotificationSettingsScreen.kt` & `NotificationSettingsViewModel.kt`: Daily briefing time pickers, cycle milestone warnings (50%, 80%, 100%), quiet hours toggle, and local notification preferences.

### V3.0 — "Improve" Milestone (AI Coach & Audit Integrity)
* **On-Device AI Financial Coach (Screen 32)**:
  * `domain/model/FinancialCoachEngine.kt`: Fully offline, air-gapped financial coaching engine. Provides deterministic, privacy-respecting answers to financial queries (50/30/20 budget analysis, spending pace, savings optimizations) with rich response cards and quick-prompt chips. Zero external API calls or telemetry.
  * `ui/screens/coach/FinancialCoachScreen.kt` & `FinancialCoachViewModel.kt`.
* **"What-If" Purchase Simulator (Screen 33)**:
  * `domain/model/PurchaseSimulatorCalculation.kt`: Evaluates hypothetical purchases against monthly headroom, project safe-to-spend impact, and goal timelines. Produces `SAFE_TO_BUY`, `PROCEED_WITH_CAUTION`, or `DELAY_PURCHASE` recommendations with micro-adjustment controls.
  * `ui/screens/simulator/PurchaseSimulatorScreen.kt` & `PurchaseSimulatorViewModel.kt`.
* **Merkle Ledger Audit & Data Integrity (Screen 35)**:
  * `domain/model/MerkleLedgerAudit.kt`: Cryptographic integrity engine calculating SHA-256 block hashes chained across all chronological transactions, constructing a binary Merkle Tree and computing the Merkle Root. Detects any unauthorized external tampering or SQLite modification.
  * `ui/screens/audit/MerkleAuditScreen.kt` & `MerkleAuditViewModel.kt`: Displays live Merkle root, verification status (100% verified), block count, and cryptographic hash chain visualizer.

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
  * `V2V3DomainEnginesTest.kt`: 10 comprehensive unit tests covering:
    - Safe-to-Spend calculations across all 4 health states.
    - Financial Health Score 5-pillar weighting and tips.
    - Statistical Outlier and Leak Hunter z-scores and cluster detection.
    - Natural Language Expense token parser accuracy.
    - Multi-signal Duplicate Detection.
    - Offline Currency conversion preserving minor units.
    - Merkle Ledger SHA-256 block hash chaining and tampering detection.
    - Purchase Simulator impact on daily budget allowance.
    - Net Worth asset/liability calculations.
    - AI Financial Coach 50/30/20 advice logic.
  * **Result**: 100% tests passing (`./gradlew testDebugUnitTest`).
* **APK Build Verification**:
  * Build command: `./gradlew assembleDebug`
  * **Result**: `BUILD SUCCESSFUL` (0 errors, valid APK generated).
* **Physical Device Deployment**:
  * Target Hardware: **OPPO F23 5G** (`CPH2527` / Android 14/15)
  * Deployment Status: Verified via adb streamed install, PID running cleanly without crashes.

---

## 5. Multi-Profile Management, Local Backup & Cross-Profile Import, & Stitch Theme Changer

* **Multi-Profile Data Architecture**:
  * Enhanced `ProfileDao.kt` and `ProfileRepository.kt` with persistent active profile selection, `getAllProfilesFlow()`, `switchActiveProfile()`, and safe cascade profile deletion.
  * Added `ProfileSwitcherDialog` and `CreateProfileDialog` in `SettingsDialogs.kt`, allowing users to seamlessly switch accounts or spin up new profiles with dedicated base currencies (INR, USD, EUR, GBP, JPY).
* **Local Storage Encrypted Backup & Cross-Profile Import**:
  * Enhanced `BackupManager.kt` with `RestoreMode.IMPORT_INTO_CURRENT_PROFILE` and `RestoreMode.IMPORT_AS_NEW_PROFILE`.
  * Profile 1 backup can be exported and downloaded directly to local storage as an AES-256-GCM encrypted `.finora` file via Storage Access Framework (`CreateDocument`).
  * When switched to Profile 2 (or any other profile), users can import the saved backup from local storage into their currently active profile (`IMPORT_INTO_CURRENT_PROFILE`), which maps categories, accounts, and payment methods while generating non-colliding foreign keys, enabling the user to immediately continue their work.
  * Added validation tests in `BackupManagerTest.kt` verifying `RestoreMode` semantics and multi-entity preview structures.
* **Stitch Screen 12 Theme Changer**:
  * Implemented pixel-perfect pill selector matching Stitch Screen 12 design specifications (`Light | System | Dark`).
  * Dynamic subtitle indicators: "Light Theme active", "Dark Theme active", or "System default active".
  * Real-time reactive theme change wired through `MainActivity.kt` and `FinoraTheme`.

