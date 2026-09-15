# FINORA Android — Technical Debt & Future Roadmap

**Document Purpose**: Tracks technical debt, architectural improvements, and upcoming roadmap items for Finora Android.  
**Current Status**: V1.0 ("Track"), V1.1 ("Protect & Own"), V1.2 ("Experience & Expand"), V2.0 ("Understand"), and V3.0 ("Improve") COMPLETED.  
**Target Next Release**: Maintenance & Polishing  
**Last Updated**: September 15, 2026

---

## 1. Architectural & Code Technical Debt

### 1.1 Dependency Injection (DI) Migration
* **Current State**: Service locator `DatabaseModule` provides repository singletons; ViewModels use default arguments pointing to `DatabaseModule`.
* **Target Solution**: Migrate to Hilt (`@HiltAndroidApp`, `@HiltViewModel`, `@Inject`) in future major refactoring.

### 1.2 List Pagination / Room Paging 3 Integration
* **Current State**: Flow-based list emission loads transactions into memory.
* **Target Solution**: Integrate Android Jetpack **Paging 3** (`PagingSource` / `Pager`) with Room to stream paginated transactions with `collectAsLazyPagingItems()`.

### 1.3 Adaptive Layout Support (Foldables & Tablets)
* **Current State**: Responsive phone layouts implemented across portrait and landscape with Material 3 styling.
* **Target Solution**: Implement Material 3 Adaptive library components (`ListDetailPaneScaffold`) for enhanced two-pane tablet presentation.

### 1.4 Dynamic Theme Preference Observation
* **Status**: [x] RESOLVED. `MainActivity.kt` observes `profileRepository.getActiveProfileFlow()` and dynamically updates `FinoraTheme(darkTheme = isDarkTheme)` across Light, Dark, and System modes in real time.

---

## 2. Completed Milestones vs Future Roadmap

```
[x] V1.0 (Track) — Core Expense & Budget Foundation
    ├── 12 Jetpack Compose Material 3 screens
    ├── Room Database as single local source of truth
    ├── Custom docked financial numpad
    └── Deterministic minor units math

[x] V1.1 (Protect & Own) — Security, Export & Backup
    ├── Biometric & PIN App Lock (PBKDF2WithHmacSHA256, BiometricPrompt, timeout tracker)
    ├── Encrypted Local Backup & Atomic Restore (.finora AES-256-GCM container)
    └── CSV & Native PDF Monthly Statement Generators (RFC 4180 & android.graphics.pdf)

[x] V1.2 (Experience & Expand) — Cash Flow & Native Integrations
    ├── Room Database Migration 1->2 with 5 new entities
    ├── Income & Net Cash Flow Tracking (AddIncomeScreen, CashFlowSummary, Savings Rate %)
    ├── Multi-Account Balances & Wallets (AccountsScreen, Bank Account, Cash, Cards, Wallets)
    ├── Subscriptions & Recurring Commitments (Monthly normalization, 7-day due alert, manual confirmation)
    ├── Savings Goals & Target Visualizer (Goal progress %, remaining gap, pace recommendations)
    └── Native Android Integrations (App Widget, Static Launcher Shortcuts, Share Sheet Receiver)

[x] V2.0 (Understand) — Intelligence & Automation
    ├── Safe-to-Spend Real-Time Engine (Daily spendable buffer & 4 health states)
    ├── Financial Health Score (0–100 score, 5 weighted pillars, score drivers, tips)
    ├── Duplicate Guard & Statistical Leak Hunter (Z-score > 2.2, micro-spend clusters)
    ├── Multi-Currency & Travel Mode (INR base, cached rates, real-time conversion)
    ├── Assisted Entry: On-Device Receipt OCR Review & Voice Entry Pipeline
    ├── Natural Language Quick Add (Tokenized NLP parser with inline category matching)
    ├── Share Import Preview (External transaction receipt text parser)
    ├── Net Worth Ledger (Assets, liabilities, leverage ratio, conservative calculation)
    ├── Statement Reconciliation (RFC 4180 CSV statement parser & batch expense reconciliation)
    └── Notification & Digest Settings (Daily briefing, cycle alerts, quiet hours)

[x] V3.0 (Improve) — AI Coach & Audit Integrity
    ├── AI Financial Coach (Local deterministic Q&A engine, 50/30/20 card, zero telemetry)
    ├── "What-If" Purchase Simulator (Discretionary impact, pace check, safe-to-buy badges)
    └── Merkle Audit Ledger (SHA-256 block hash chaining & binary Merkle root verification)
```
