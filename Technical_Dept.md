# FINORA Android — Technical Debt & Future Roadmap

**Document Purpose**: Tracks technical debt, architectural improvements, and upcoming roadmap items for Finora Android.  
**Target Next Release**: V1.1 — Protect & Own / V1.2 — Experience & Expand  
**Last Updated**: September 12, 2026

---

## 1. Architectural & Code Technical Debt

### 1.1 Dependency Injection (DI) Migration
* **Current State**: `FinoraNavHost.kt` instantiates ViewModels manually via `viewModel { ... }` factory lambdas, referencing singletons from `FinoraApp`.
* **Debt**: As feature screens grow beyond V1.0 (35 screens total), manual factory wiring in navigation will become fragile and difficult to mock for complex UI tests.
* **Target Solution**: Migrate to full **Hilt** or **Koin** dependency injection for compile-time or streamlined graph validation and cleaner ViewModel injection (`@HiltViewModel`).

### 1.2 Centralized Currency & Formatting Provider
* **Current State**: Several UI elements hardcode currency symbol representation (`₹`) or inline number formatting.
* **Debt**: While INR is the primary locale, V2.0 requires full Multi-Currency Travel Mode (SRS §3.34) and V1.0 settings allows currency preference selection.
* **Target Solution**: Implement a unified `CurrencyFormatter` / `LocaleProvider` injected into ViewModels and Composables to guarantee consistent formatting and symbol placement across all screens.

### 1.3 List Pagination / Room Paging 3 Integration
* **Current State**: `ExpenseDao.getAllExpensesWithDetails()` returns a reactive `Flow<List<ExpenseWithDetails>>` loading all records into memory.
* **Debt**: When a user logs thousands of transactions over years of usage, loading the entire transaction history will increase memory consumption and cause frame drops.
* **Target Solution**: Integrate Android Jetpack **Paging 3** (`PagingSource` / `Pager`) with Room to stream paginated transactions with `collectAsLazyPagingItems()`.

### 1.4 Database Schema Versioning & AutoMigration
* **Current State**: `AppDatabase` is at schema version 1.
* **Debt**: V1.1 introduces PIN/biometrics metadata, backup timestamps; V1.2 introduces Accounts, Income, Recurring transactions, and Savings Goals.
* **Target Solution**: Establish an explicit Room migration testing pipeline (`MigrationTestHelper`) to verify that user databases upgrade smoothly from V1.0 to future versions without data loss.

---

## 2. UI/UX Technical Debt & Edge Cases

### 2.1 Full Adaptive Layout Support (Foldables & Tablets)
* **Current State**: UI layouts are designed and tested on standard portrait mobile form factors (e.g., 1080x2400).
* **Debt**: SRS §2.4 requires support for standard phones, large phones, foldables, and tablet layouts.
* **Target Solution**: Implement two-pane layouts (`ListDetailPaneScaffold` from Material 3 adaptive library) for Foldables and Tablets in landscape orientation.

### 2.2 System Dynamic Theme Switching
* **Current State**: The theme currently adheres to the default system dark/light configuration.
* **Debt**: The Settings screen currently lacks a manual theme selector (System Default, Light, Dark).
* **Target Solution**: Implement user-selectable theme preference persisted via `DataStore` and observed in `FinoraTheme`.

### 2.3 Edge-to-Edge System Bar Polish
* **Current State**: Standard Scaffold padding is applied.
* **Debt**: Certain bottom sheets and docked keyboards require refined handling of IME insets (`WindowInsets.ime`) across varied Android OS versions (API 26 to API 35).
* **Target Solution**: Ensure `enableEdgeToEdge()` works harmoniously with keyboard insets and bottom navigation bars across all OEM variants.

---

## 3. Testing Debt

### 3.1 Jetpack Compose UI Tests
* **Current State**: Comprehensive JVM unit tests exist for ViewModels and helper classes (`AddExpenseViewModelTest`, `CategoryIconsTest`).
* **Debt**: Automated Compose UI tests (`createComposeRule()`) are not yet integrated into the CI/build pipeline.
* **Target Solution**: Add component-level and screen-level Compose tests verifying user tap interactions, dialog dismissals, and keypad transitions.

### 3.2 DAO & Room In-Memory SQLite Tests
* **Current State**: Database interactions are tested via repository/ViewModel unit mocks.
* **Debt**: Complex SQL queries (such as date-range grouping and multi-parameter filters in `ExpenseDao`) should be tested against an in-memory SQLite instance.
* **Target Solution**: Add Robolectric or Android test runner DAO test suite.

---

## 4. Upcoming Roadmap & Version Milestones

```
V1.0 (Track) [COMPLETED]
   │
   ▼
V1.1 (Protect & Own)
   ├── Biometric & PIN App Lock (BiometricPrompt / Keystore)
   ├── Encrypted Local Backup & Restore (.finora encrypted archive)
   └── CSV & PDF Transaction Export
   │
   ▼
V1.2 (Experience & Expand)
   ├── Income & Cash Flow Tracking (Screens 20 & 21)
   ├── Accounts / Wallets Foundation (Screen 19)
   ├── Recurring Expenses & Subscription Watchlist (Screens 16 & 17)
   ├── Savings Goals & Target Visualizer (Screens 14 & 15)
   └── Android Home Screen Widgets & App Shortcuts
   │
   ▼
V2.0 (Understand)
   ├── Safe-to-Spend Real-Time Engine
   ├── Financial Health Score
   ├── Duplicate Guard & Leak Hunter
   ├── Multi-Currency & Travel Mode
   └── Assisted Entry: On-device Receipt OCR & Voice Entry Pipeline
   │
   ▼
V3.0 (Improve)
   ├── On-Device AI Financial Coach (Local LLM / Privacy Sandbox)
   ├── "What-If" Purchase Simulator
   └── Merkle Audit Ledger for Data Integrity
```

---

## 5. Immediate Next Sprints (Recommended Priority)

1. **Sprint 1 — V1.1 Security & Export**:
   * Implement Biometric authentication prompt on app launch.
   * Implement CSV transaction export via Android Storage Access Framework (SAF).
   * Implement AES-GCM encrypted backup file creation and restore.
2. **Sprint 2 — V1.2 Multi-Account & Income**:
   * Add Account entity (Cash, Bank Account, Credit Card) and Income transaction logging.
   * Expand Home Dashboard to show Total Balance and Net Cash Flow.
