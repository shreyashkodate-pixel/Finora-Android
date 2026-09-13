# FINORA Android — Technical Debt & Future Roadmap

**Document Purpose**: Tracks technical debt, architectural improvements, and upcoming roadmap items for Finora Android.  
**Current Status**: V1.0 ("Track"), V1.1 ("Protect & Own"), and V1.2 ("Experience & Expand") COMPLETED.  
**Target Next Release**: V2.0 — Safe-to-Spend & Financial Health Score ("Understand")  
**Last Updated**: September 13, 2026

---

## 1. Architectural & Code Technical Debt

### 1.1 Dependency Injection (DI) Migration
* **Current State**: Service locator `DatabaseModule` provides repository singletons; ViewModels use default arguments pointing to `DatabaseModule`.
* **Debt**: As feature screens grow into V2.0/V3.0, migrating to Hilt or Koin will streamline graph validation and improve testing isolation.
* **Target Solution**: Introduce Hilt (`@HiltAndroidApp`, `@HiltViewModel`, `@Inject`) when beginning V2.0.

### 1.2 List Pagination / Room Paging 3 Integration
* **Current State**: Flow-based list emission loads transactions into memory.
* **Debt**: With high volume transaction history over years, memory usage could increase.
* **Target Solution**: Integrate Android Jetpack **Paging 3** (`PagingSource` / `Pager`) with Room to stream paginated transactions with `collectAsLazyPagingItems()`.

### 1.3 Adaptive Layout Support (Foldables & Tablets)
* **Current State**: Responsive phone layouts implemented across portrait and landscape.
* **Debt**: Two-pane master-detail layouts for Foldables and Tablets (`ListDetailPaneScaffold`) can be added for enhanced widescreen presentation.
* **Target Solution**: Implement Material 3 Adaptive library components in V2.0.

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

[ ] V2.0 (Understand) — Intelligence & Automation
    ├── Safe-to-Spend Real-Time Engine (daily spendable buffer)
    ├── Financial Health Score (5 weighted components: Savings Rate, Budget Adherence, Debt-to-Income, etc.)
    ├── Duplicate Guard & Leak Hunter
    ├── Multi-Currency & Travel Mode
    └── Assisted Entry: On-device Receipt OCR & Voice Entry Pipeline

[ ] V3.0 (Improve) — AI Coach & Audit Integrity
    ├── On-Device AI Financial Coach (Local LLM / Privacy Sandbox)
    ├── "What-If" Purchase Simulator
    └── Merkle Audit Ledger for Data Integrity
```

---

## 3. Next Milestone (V2.0 — "Understand") Priorities

1. **Safe-to-Spend Daily Engine**: Calculate uncommitted liquid discretionary income divided by remaining days in cycle.
2. **Financial Health Score**: Deterministic 0–100 score evaluating emergency buffer, savings pace, and budget variance.
3. **On-Device Receipt OCR Pipeline**: Privacy-first, local text extraction without sending financial images off-device.
