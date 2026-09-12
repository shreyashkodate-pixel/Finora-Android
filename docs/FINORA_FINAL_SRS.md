# FINORA
## Software Requirements Specification (SRS)

**Document Version:** 1.1
**Based On:** FINORA Master PRD v2.0
**Standard Followed:** Structure adapted from IEEE 830-1998 / ISO/IEC/IEEE 29148
**Platform:** Native Android (Kotlin, Jetpack Compose, Room)
**Status:** Final SRS for Technical Architecture, Antigravity Implementation, QA and Acceptance
**Prepared For:** FINORA Engineering / Implementation Partner (Antigravity)

---

## Revision History

| Version | Date | Description | Author |
|---|---|---|---|
| 1.0 | 2026-09-12 | Initial SRS derived from FINORA Master PRD v2.0 | — |
| 1.1 | 2026-09-12 | Final refinement: clarified currency, security/recovery, financial calculation ownership, recurring/savings behavior, AI, data lifecycle, feature states, edge cases, and privacy-safe telemetry | — |

---

## Table of Contents

1. Introduction
2. Overall Description
3. System Features (Functional Requirements)
4. External Interface Requirements
5. Non-Functional Requirements
6. Data Requirements
7. Logical System Architecture
8. Other Requirements
9. Use Cases & Acceptance Scenarios
10. Edge Cases & Expected Behavior
11. Feature State Requirements
12. Definition of Done & Release Gates
Appendix A — Requirement Traceability Matrix
Appendix B — Glossary
Appendix C — Assumptions, Dependencies & Open Issues
Appendix D — Version Scope Summary

---

# 1. Introduction

## 1.1 Purpose

This Software Requirements Specification (SRS) translates the FINORA Master PRD v2.0 into verifiable, implementation-ready functional and non-functional requirements. It is intended to be the primary reference for engineering design, implementation (including AI-assisted implementation by Antigravity), QA test-case authoring, and acceptance sign-off.

This SRS does **not** define database schemas, API contracts, class-level architecture, or UI pixel specifications. Those belong to the Technical Architecture Document (TAD) and UI/UX Specification, which this SRS assumes will be produced as companion documents.

## 1.2 Scope

FINORA is a native Android, offline-first, privacy-first personal finance application. It allows users to record expenses and income, organize spending by category and payment method, set budgets, view analytics, protect and back up their data, and — in later versions — receive AI-assisted, explainable financial insights.

This SRS covers all five planned releases:

| Release | Codename | Scope Summary |
|---|---|---|
| V1.0 | "Track" | Core expense/budget tracker, offline, Room-based |
| V1.1 | "Protect & Own" | Security, reports, encrypted backup/restore |
| V1.2 | "Experience & Expand" | Native Android capabilities, income, recurring, savings goals |
| V2.0 | "Understand" | Advanced analytics, financial intelligence, AI API |
| V3.0 | "Improve" | Conversational assistant, simulators, planning |

Each version is a self-contained, independently shippable increment. A requirement tagged for a later version must not be implemented ahead of schedule (see §8.3, Version Isolation).

## 1.3 Definitions, Acronyms, and Abbreviations

See Appendix B (Glossary).

## 1.4 References

- FINORA Master Product Requirements Document (PRD), v2.0
- IEEE Std 830-1998, *IEEE Recommended Practice for Software Requirements Specifications*
- ISO/IEC/IEEE 29148:2018, *Systems and software engineering — Life cycle processes — Requirements engineering*
- Android developer documentation: Room, Jetpack Compose, CameraX, WorkManager, BiometricPrompt, Jetpack Glance, ML Kit

## 1.5 Document Conventions

- Each functional requirement has a unique ID in the form `FR-<MODULE>-<VERSION>-<NNN>`, e.g., `FR-EXP-V1.0-003`.
- Each non-functional requirement has an ID in the form `NFR-<CATEGORY>-<NNN>`.
- **Priority** follows MoSCoW: **Must** (release blocker), **Should** (expected, not blocking), **Could** (nice-to-have within the version).
- The keyword **shall** denotes a mandatory, testable requirement. **Should** denotes a strong recommendation. **May** denotes an optional capability.
- Requirements are grouped by functional module and tagged with the version in which they first apply. Unless stated otherwise, a requirement remains in force in all subsequent versions (see the Feature-to-Version Matrix in the PRD, §34).

---

# 2. Overall Description

## 2.1 Product Perspective

FINORA is a standalone native Android application. It is not a companion app to a backend service; all core functionality operates against a local Room database. Optional external services (AI API, cloud storage used as a backup *destination* only) sit beside — not beneath — the local core, per the architecture shown below.

```
Native Android
   ↓
UI / Presentation (Jetpack Compose)
   ↓
Application Logic (ViewModel / Use-Case layer)
   ↓
Repository / Data Layer
   ↓
Room Database
   ↓
Local Financial Data (source of truth)
```

Optional services (AI API, camera/OCR, voice, notifications, widgets, external auth if introduced) are treated as replaceable peripherals. Their absence or failure must never prevent core expense tracking (see NFR-REL-002, Graceful Degradation).

## 2.2 Product Functions (Summary)

- Record, edit, delete, and review expenses and (from V1.2) income.
- Organize transactions by category, payment method/account.
- Search, filter, and sort transaction history.
- Set and monitor monthly and category budgets.
- View a dashboard and local analytics computed deterministically from stored data.
- Protect the app with biometric or PIN lock.
- Export data as CSV/PDF and create/restore encrypted, portable backups.
- Enter expenses via camera (receipt OCR), voice, natural language, or Android Share Sheet, all routed through a single validate-and-confirm pipeline.
- Track recurring expenses and savings goals; compute cash flow.
- Provide advanced, explainable analytics: budget pacing, Safe-to-Spend, Financial Health Score, recurring/subscription intelligence, leak detection, duplicate detection.
- Import external CSV data with preview and validation.
- Optionally consult an AI API for structured, evidence-grounded recommendations and a conversational, read-only financial assistant.

## 2.3 User Classes and Characteristics

| User Class | Description | Technical Proficiency | Primary Needs |
|---|---|---|---|
| Primary End User | College students, young professionals, freelancers, budget-conscious individuals | Low–medium | Fast entry, clear budget status, privacy |
| Power User | Users managing multiple payment methods/accounts, savings goals, recurring bills | Medium | Analytics depth, CSV import/export, recurring intelligence |
| Privacy-Sensitive User | Users explicitly avoiding cloud-synced finance apps | Any | Offline guarantee, local-only storage, encrypted portable backup |

FINORA has no administrator, support-agent, or multi-tenant user class in the scope of this SRS. Only a single local profile is required through V1.0–V3.0 (see §8.3.1); the data layer must nonetheless enforce profile-scoped ownership so that future multi-profile support does not require a data-model rewrite.

## 2.4 Operating Environment

- **OS:** Native Android (minimum and target API levels to be fixed in the TAD).
- **Form factors:** Standard phones, large phones, foldables; tablet layout support where practical.
- **Connectivity:** Must be fully operable with no network connection for all V1.0–V1.1 functionality and for all deterministic (non-AI) functionality in V2.0–V3.0.
- **Storage:** Local device storage via Room (SQLite) plus device file storage for backups, exported reports, and receipt images.

## 2.5 Design and Implementation Constraints

- **DC-1:** Primary local persistence shall be Room. No other primary local persistence mechanism shall be introduced without an approved architecture change.
- **DC-2:** Monetary values shall be stored and computed using an exact numeric representation (e.g., minor-unit integer or arbitrary-precision decimal type). Native floating-point types (`Float`/`Double`) shall not be used for stored or computed monetary values.
- **DC-3:** All core financial calculations (totals, budget usage, cash flow, savings math) shall be deterministic and shall not depend on an AI model.
- **DC-4:** Any AI-derived output shall be structured, schema-validated, and visually/semantically distinguishable from deterministic data (see FR-AI-V2.0-004).
- **DC-5:** Assisted-entry methods (OCR, voice, NL parsing, screenshot/share import, CSV import) shall converge on one shared validation/confirmation pipeline (§3, Assisted Entry Pipeline) rather than parallel, feature-specific pipelines.
- **DC-6:** A given release shall implement only the requirements tagged for its version or earlier (see §8.3).
- **DC-7:** Every core financial record shall have a stable unique identifier.
- **DC-8:** Every persisted monetary transaction shall carry an explicit transaction currency from V1.0, even though full multi-currency behavior is introduced in V2.0.
- **DC-9:** Derived financial values shall have a documented source of truth.
- **DC-10:** Recurring rules shall remain distinct from actual historical transactions.
- **DC-11:** Savings-goal progress shall be derived consistently from contribution/withdrawal records and shall not rely on conflicting independently editable values.

## 2.6 Assumptions and Dependencies

- It is assumed the target devices provide standard Android biometric hardware/APIs, though biometric capability is not assumed to be present on every device (device-credential fallback is required).
- It is assumed on-device or privacy-conscious OCR (e.g., ML Kit) is available on target devices; if unavailable, the camera-based entry path degrades to manual entry without blocking the release.
- AI-dependent features (V2.0 AI API, V3.0 assistant) are assumed to require network connectivity and a configured provider; their absence must not degrade any V1.0–V1.1 requirement.
- Exact minimum Android API level, dependency versions, and encryption algorithm choices are deferred to the Technical Architecture Document and are out of scope for this SRS.

---

# 3. System Features (Functional Requirements)

Requirements are grouped by functional module. Each module states its governing business rules, followed by a requirement table. Unless a requirement explicitly says otherwise, it persists unchanged into every later version.

### Assisted-Entry Pipeline (governs FR-CAM, FR-VOI, FR-NLP, FR-SHR, FR-IMP)

All assisted-entry features (camera/OCR, voice, natural-language quick add, screenshot/share, CSV import) shall implement the same six-stage pipeline:

```
Capture → Extract → Validate → Preview (editable) → User Confirmation → Save
```

No transaction produced by an assisted-entry method may be persisted as a committed financial record without an explicit user confirmation step. Low-confidence extraction results shall be visually flagged to the user before confirmation.

---

## 3.1 Profile & Data Isolation (PROF) — V1.0

| ID | Requirement | Priority |
|---|---|---|
| FR-PROF-V1.0-001 | The system shall allow creation of a local profile with name, optional profile image, preferred display currency, theme preference, and other non-sensitive preferences. | Must |
| FR-PROF-V1.0-002 | The system shall associate every profile-owned data entity (expense, category, budget, payment method, etc.) with a profile identifier at the data layer. | Must |
| FR-PROF-V1.0-003 | Profile ownership shall be enforced below the UI layer (i.e., in the repository/data layer), such that no query path can return another profile's records even if a UI defect exists. | Must |
| FR-PROF-V1.0-004 | The system shall support only a single active local profile in V1.0–V3.0 scope, while the data model shall remain structurally compatible with future multi-profile support without requiring a schema rewrite. | Must |

## 3.2 Expense Management (EXP) — V1.0

| ID | Requirement | Priority |
|---|---|---|
| FR-EXP-V1.0-001 | The system shall allow the user to create an expense with required fields: amount, category, date, and explicit transaction currency. | Must |
| FR-EXP-V1.0-002 | The system shall allow optional fields on an expense: title, payment method/account, notes. | Must |
| FR-EXP-V1.0-003 | The system shall reject expense amounts that are zero or negative. | Must |
| FR-EXP-V1.0-004 | The system shall reject structurally invalid or out-of-range dates. | Must |
| FR-EXP-V1.0-005 | The system shall not silently modify any user-entered value during save, validation, or display formatting. | Must |
| FR-EXP-V1.0-006 | The system shall assign each expense a stable, unique identifier at creation time that does not change for the life of the record. | Must |
| FR-EXP-V1.0-007 | The system shall allow the user to view, edit, and delete an existing expense. | Must |
| FR-EXP-V1.0-008 | The system shall allow the user to view full details of a single expense. | Must |
| FR-EXP-V1.0-009 | The system shall allow the user to browse expense history in a scrollable, chronologically navigable list. | Must |
| FR-EXP-V1.0-010 | The data model shall support (though V1.0 UI need not expose) fields for: expense ID, profile ID, title, amount, currency, category, payment method, expense date, notes, created timestamp, updated timestamp, source, optional attachment reference, and future recurring-status information. | Should |
| FR-EXP-V1.0-011 | The system shall preserve the original transaction currency as part of every persisted expense. | Must |
| FR-EXP-V1.0-012 | V1.0 shall use the user's selected/default currency for ordinary single-currency operation while keeping transaction currency explicit in storage. | Must |

## 3.3 Categories (CAT) — V1.0

| ID | Requirement | Priority |
|---|---|---|
| FR-CAT-V1.0-001 | The system shall ship with starter categories: Food, Transport, Rent/Housing, Shopping, Bills, Entertainment, Health/Healthcare, Education, Other. | Must |
| FR-CAT-V1.0-002 | The system shall allow the user to create custom categories. | Must |
| FR-CAT-V1.0-003 | The system shall allow the user to rename custom categories. | Must |
| FR-CAT-V1.0-004 | The system shall allow the user to delete custom categories. | Must |
| FR-CAT-V1.0-005 | When a category deletion would orphan existing expense records, the system shall require the user to reassign those expenses to another category before or during the deletion. | Must |
| FR-CAT-V1.0-006 | The system shall support an icon/visual identity per category consistent with the design system. | Should |

## 3.4 Payment Methods / Accounts (PAY) — V1.0

| ID | Requirement | Priority |
|---|---|---|
| FR-PAY-V1.0-001 | The system shall ship with starter payment methods: Cash, UPI, Debit Card, Credit Card, Bank Transfer, Digital Wallet, Other. | Must |
| FR-PAY-V1.0-002 | The payment-method data model shall be extensible to support future user-defined methods/accounts. | Should |

## 3.5 Search, Filter & Sort (SRCH) — V1.0

| ID | Requirement | Priority |
|---|---|---|
| FR-SRCH-V1.0-001 | The system shall support searching expenses by title, category, and payment method/account. | Must |
| FR-SRCH-V1.0-002 | The system shall support filtering by category, payment method, amount range, and date range. | Must |
| FR-SRCH-V1.0-003 | The system shall provide date-range presets: Today, This Week, This Month, and Custom. | Must |
| FR-SRCH-V1.0-004 | Filters shall be combinable (e.g., category + payment method + date range + amount range applied simultaneously). | Must |
| FR-SRCH-V1.0-005 | The system shall support sorting expense lists by: Newest, Oldest, Highest amount, Lowest amount. | Must |

## 3.6 Dashboard (DASH) — V1.0

| ID | Requirement | Priority |
|---|---|---|
| FR-DASH-V1.0-001 | The Home dashboard shall display, using only real stored data: current-period spending, today's spending, remaining monthly budget, budget status, highest transaction, recent transactions, top categories, category breakdown, and spending trend. | Must |
| FR-DASH-V1.0-002 | The dashboard shall never display hardcoded, static, or placeholder financial values in a production build. | Must |

## 3.7 Budgets (BUD) — V1.0

| ID | Requirement | Priority |
|---|---|---|
| FR-BUD-V1.0-001 | The system shall allow the user to set an overall monthly budget. | Must |
| FR-BUD-V1.0-002 | The system shall allow the user to optionally set per-category budgets. | Must |
| FR-BUD-V1.0-003 | For each budget, the system shall compute amount spent, amount remaining, percentage used, and over-budget amount. | Must |
| FR-BUD-V1.0-004 | The system shall classify each budget's status as On Track (<80% used), Near Limit (80–99%), or Over Budget (≥100%), with thresholds configurable for future tuning. | Must |
| FR-BUD-V1.0-005 | The system shall permit the user to record an expense that causes a budget to be exceeded; it shall never silently discard or block a real transaction because a budget limit was reached (see NFR-BIZ-001, Financial Guardrail Philosophy). | Must |
| FR-BUD-V1.0-006 | When a budget is exceeded, the system shall communicate the impact (e.g., "Budget exceeded by ₹1,000") rather than only suppressing the entry. | Must |

## 3.8 Local Analytics (ANL) — V1.0

| ID | Requirement | Priority |
|---|---|---|
| FR-ANL-V1.0-001 | The system shall compute, from local data only: daily, weekly, monthly, and yearly spending totals; category totals; average spending; highest expenses; budget utilization; and basic period-over-period comparisons. | Must |
| FR-ANL-V1.0-002 | All V1.0 analytics shall be computed without network access. | Must |

## 3.9 Theme (THM) — V1.0

| ID | Requirement | Priority |
|---|---|---|
| FR-THM-V1.0-001 | The system shall support Light, Dark, and System-preference theme modes. | Must |

## 3.10 App Security — Biometric & PIN (SEC) — V1.1

| ID | Requirement | Priority |
|---|---|---|
| FR-SEC-V1.1-001 | The system shall support app-lock via Android biometric authentication (BiometricPrompt) with device-credential fallback. | Must |
| FR-SEC-V1.1-002 | The system shall allow the user to enable or disable app lock. | Must |
| FR-SEC-V1.1-003 | Sensitive application data shall not render on screen prior to successful authentication when app lock is enabled. | Must |
| FR-SEC-V1.1-004 | The system shall support an optional application PIN as a non-biometric unlock method. | Must |
| FR-SEC-V1.1-005 | The raw PIN shall never be persisted; only a securely derived verification artifact shall be stored. | Must |
| FR-SEC-V1.1-006 | The system shall provide an approved forgotten-PIN recovery/reset path consistent with the local-only security model. | Must |
| FR-SEC-V1.1-007 | A PIN reset shall clearly communicate whether existing protected local data can be preserved and what happens if secure recovery is unavailable. | Must |

## 3.11 Reporting — CSV & PDF (RPT) — V1.1

| ID | Requirement | Priority |
|---|---|---|
| FR-RPT-V1.1-001 | The system shall export expense records to CSV, including at minimum: date, title, amount, currency, category, payment method, notes. | Must |
| FR-RPT-V1.1-002 | CSV export shall support filtering by date range and other applicable filters at the time of export. | Should |
| FR-RPT-V1.1-003 | The system shall generate a human-readable PDF report containing reporting period, total spending, category summary, transaction listing, and budget information, with income/cash-flow/savings/recurring sections included once those data areas exist (V1.2+). | Must |
| FR-RPT-V1.1-004 | Generated reports shall be shareable via the Android system share sheet. | Must |
| FR-RPT-V1.1-005 | Exported reports (CSV/PDF) shall be visually and functionally distinguishable from restorable encrypted backups; a report is not a restore source. | Must |

## 3.12 Encrypted Backup & Restore (BKP) — V1.1

Backup and restore are safety-critical: a defect here can cause irreversible user data loss. Requirements in this module require the highest testing rigor in the product (see NFR-REL-005).

| ID | Requirement | Priority |
|---|---|---|
| FR-BKP-V1.1-001 | The system shall allow the user to create a portable, encrypted backup file (e.g., `expense_backup_YYYY_MM_DD.enc`) containing supported application data. | Must |
| FR-BKP-V1.1-002 | The backup flow shall proceed: Collect Supported Data → Validate → Encrypt → Create File → Save/Share. | Must |
| FR-BKP-V1.1-003 | Backup contents may include: profile information, expenses, categories, budgets, payment methods, income data, savings goals, recurring-expense data, supported preferences, and backup metadata (as each data area becomes available by version). | Must |
| FR-BKP-V1.1-004 | Backup contents shall never include: raw account passwords, raw app PIN, authentication tokens, refresh tokens, active session credentials, device-specific secrets, or unnecessary temporary cache. | Must |
| FR-BKP-V1.1-005 | The backup shall be protected by a backup password that is conceptually and technically separate from the app-lock PIN/biometric credential. | Must |
| FR-BKP-V1.1-006 | The backup password shall never be stored in plaintext, and the system shall clearly disclose that a forgotten backup password renders the backup permanently unrecoverable. | Must |
| FR-BKP-V1.1-007 | Users shall be able to save or share the backup file through standard Android system storage/sharing mechanisms; FINORA shall not own or restrict the backup's storage destination. | Must |
| FR-BKP-V1.1-008 | The restore flow shall proceed: Select File → Validate File → Enter Backup Password → Decrypt → Restore Preview → Import → Verify → Restore Complete. | Must |
| FR-BKP-V1.1-009 | Before committing a restore, the system shall present a preview showing backup date, backup/app version, and counts of expenses, categories, budgets, income records, and savings goals as applicable, plus profile availability and backup size. | Must |
| FR-BKP-V1.1-010 | Backup files shall store format version, application version, data/schema version, creation timestamp, and integrity information. | Must |
| FR-BKP-V1.1-011 | The restore process shall use stable record identifiers to minimize accidental duplication of already-present records. | Must |
| FR-BKP-V1.1-012 | The system shall validate the backup file and password before import, distinctly handle incorrect passwords versus corrupt/invalid files, and reject unsupported backup-format versions with a clear message. | Must |
| FR-BKP-V1.1-013 | The system shall never leave the local database in a partially restored, inconsistent state; a failed restore shall roll back cleanly. | Must |
| FR-BKP-V1.1-014 | The system shall report a clear success or failure outcome at the end of every restore attempt. | Must |
| FR-BKP-V1.1-015 | A failed restore shall leave the pre-restore dataset unchanged unless an explicitly selected destructive replacement operation completed safely. | Must |
| FR-BKP-V1.1-016 | The system shall report a clear success, failure, or cancellation state at the end of every restore attempt. | Must |
| FR-BKP-V1.1-017 | Restore logic shall support future backup/data/schema migration. | Must |
| FR-BKP-V1.1-018 | If both replace and merge restore modes are offered, the selected mode and its consequences shall be shown before confirmation. | Should |

## 3.13 Notifications (NTF) — V1.2

| ID | Requirement | Priority |
|---|---|---|
| FR-NTF-V1.2-001 | The system shall support user-controlled notifications: daily expense reminders, budget threshold alerts, over-budget alerts, optional spending summaries, recurring-expense reminders (when recurring data exists), and savings-goal milestone notifications (when savings goals exist). | Must |
| FR-NTF-V1.2-002 | Sensitive financial detail (exact amounts, merchant/category specifics) shall be minimized in lock-screen notification previews. | Should |
| FR-NTF-V1.2-003 | If notification permission is denied, all budgeting and tracking functionality shall continue to work without degradation. | Must |

## 3.14 Camera / Receipt Capture (CAM) — V1.2

| ID | Requirement | Priority |
|---|---|---|
| FR-CAM-V1.2-001 | The system shall allow the user to capture a receipt/bill image via device camera. | Must |
| FR-CAM-V1.2-002 | The system shall attempt OCR extraction of merchant, amount, date, currency, payment method, category, and relevant text from the captured image. | Must |
| FR-CAM-V1.2-003 | Extracted data shall be presented in an editable preview before save; the assisted-entry pipeline (§3, preamble) applies in full. | Must |
| FR-CAM-V1.2-004 | If camera permission is denied or OCR extraction fails, manual expense entry shall remain fully available. | Must |
| FR-CAM-V1.2-005 | Low-confidence OCR fields shall be visually flagged and shall require explicit user review before save. | Must |

## 3.15 Voice-Assisted Entry (VOI) — V1.2

| ID | Requirement | Priority |
|---|---|---|
| FR-VOI-V1.2-001 | The system shall accept a natural-speech utterance describing an expense (e.g., "Spent 250 rupees on lunch using UPI") and parse it into structured candidate fields (amount, category, title, payment method, date). | Must |
| FR-VOI-V1.2-002 | Parsed voice results shall require explicit user confirmation or edit before being saved as a transaction. | Must |
| FR-VOI-V1.2-003 | If microphone permission is denied or voice parsing is unavailable/fails, manual expense entry shall remain fully available. | Must |

## 3.16 Natural-Language Quick Add (NLP) — V1.2

| ID | Requirement | Priority |
|---|---|---|
| FR-NLP-V1.2-001 | The system shall accept a free-text quick-add string (e.g., "Uber 240 cash yesterday") and infer title/merchant, amount, date, payment method, category, and notes where determinable. | Must |
| FR-NLP-V1.2-003 | The product shall provide a defined offline fallback when an external parser is unavailable; it shall not present a false successful inference. | Must |
| FR-NLP-V1.2-002 | All inferred values shall be presented for user confirmation before being committed as a transaction. | Must |

## 3.17 Screenshot / Share-to-FINORA (SHR) — V1.2

| ID | Requirement | Priority |
|---|---|---|
| FR-SHR-V1.2-001 | The system shall register as an Android Share Sheet target for supported content types: payment screenshots, receipts, invoices, images, and text. | Must |
| FR-SHR-V1.2-002 | Shared content shall enter the same assisted-entry pipeline (Capture → Extract → Validate → Preview → Confirm → Save) as camera and voice entry. | Must |

## 3.18 Android Widgets (WDG) — V1.2

| ID | Requirement | Priority |
|---|---|---|
| FR-WDG-V1.2-001 | The system shall provide a home-screen widget showing current-period spending and remaining budget, using live local data only. | Must |
| FR-WDG-V1.2-002 | The widget shall provide a quick-access affordance to add an expense. | Should |
| FR-WDG-V1.2-003 | Once Safe-to-Spend (V2.0) is available, a widget variant may surface it. | Could (V2.0-dependent) |
| FR-WDG-V1.2-004 | Widgets shall never display static/placeholder values in production. | Must |
| FR-WDG-V1.2-005 | Widget refresh behavior shall balance data freshness with battery usage. | Should |

## 3.19 Android Shortcuts (SHC) — V1.2

| ID | Requirement | Priority |
|---|---|---|
| FR-SHC-V1.2-001 | The system shall provide an Android app shortcut for "Add Expense." | Must |
| FR-SHC-V1.2-002 | The system shall provide an Android app shortcut for "Scan Receipt." | Should |
| FR-SHC-V1.2-003 | An "Ask FINORA" shortcut shall be added once the V3.0 conversational assistant exists. | Could (V3.0-dependent) |

## 3.20 Adaptive UI (UIX) — V1.0 (ongoing)

| ID | Requirement | Priority |
|---|---|---|
| FR-UIX-V1.0-001 | The application layout shall adapt appropriately across standard phones, large phones, and foldables, with tablet layout support where practical. | Should |

## 3.21 Income (INC) — V1.2

| ID | Requirement | Priority |
|---|---|---|
| FR-INC-V1.2-001 | The system shall allow the user to record income entries with a type (Salary, Freelance, Business/Personal, Other). | Must |
| FR-INC-V1.2-002 | Income records shall support amount, currency, date, and source/description. | Must |
| FR-INC-V1.2-003 | Income records shall have stable unique identifiers. | Must |
| FR-INC-V1.2-004 | Income shall remain distinct from expenses and shall not be stored as a negative expense merely to simplify calculations. | Must |

## 3.22 Cash Flow (CF) — V1.2

| ID | Requirement | Priority |
|---|---|---|
| FR-CF-V1.2-001 | The system shall compute Net Cash Flow as Total Income − Total Expenses for a given period, deterministically from recorded data. | Must |
| FR-CF-V1.2-002 | The system shall optionally compute savings amount and savings rate when sufficient income and expense data exists. | Should |
| FR-CF-V1.2-003 | Cash-flow calculations shall be deterministic and reproducible. | Must |
| FR-CF-V1.2-004 | Missing income shall not be silently interpreted as zero without an explicit product rule. | Must |

## 3.23 Accounts / Wallets Foundation (ACC) — V1.2

| ID | Requirement | Priority |
|---|---|---|
| FR-ACC-V1.2-001 | The system shall support conceptual account/payment-source tracking for Cash, Bank Account, Debit Card, Credit Card, Digital Wallet. | Must |
| FR-ACC-V1.2-002 | The system shall not claim or display an authoritative bank/card balance unless derived entirely from sufficient user-entered data or an approved future integration; it shall not fabricate a balance. | Must |

## 3.24 Recurring Expenses (REC) — V1.2

| ID | Requirement | Priority |
|---|---|---|
| FR-REC-V1.2-001 | The system shall allow the user to mark an expense as recurring with frequency Weekly, Monthly, or Yearly. | Must |
| FR-REC-V1.2-002 | The system shall surface recurring-expense visibility, upcoming-payment awareness, a monthly-normalized commitment total, and an annualized recurring cost. | Must |
| FR-REC-V1.2-003 | A recurring rule shall remain distinct from an actual historical expense. | Must |
| FR-REC-V1.2-004 | The system shall not silently create a historical expense merely because a recurring date arrives. | Must |
| FR-REC-V1.2-005 | A projected occurrence shall be clearly distinguishable from a committed transaction until the defined confirmation condition is met. | Must |
| FR-REC-V1.2-006 | Recurring reminders should be available without requiring automatic creation of financial records. | Should |

## 3.25 Savings Goals (SAV) — V1.2

| ID | Requirement | Priority |
|---|---|---|
| FR-SAV-V1.2-001 | The system shall allow creation of a savings goal with name, target amount, current saved amount, currency, and target date; preset templates (Emergency Fund, Vacation, Gadget, Vehicle, Education, Custom) shall be offered. | Must |
| FR-SAV-V1.2-002 | The system shall record contributions and withdrawals against a goal and track progress. | Must |
| FR-SAV-V1.2-003 | Goal progress shall be derived from recorded contributions and withdrawals rather than from conflicting independently editable values. | Must |
| FR-SAV-V1.2-004 | The system shall compute required saving pace, current pace, estimated completion date, and gap-to-target from authoritative goal records when sufficient data exists. | Must |
| FR-SAV-V1.2-003 | The system shall compute required saving pace, current pace, estimated completion date, and gap-to-target, deterministically from recorded contributions. | Must |

## 3.26 Advanced Local Analytics & Budget Pacing (AAN) — V2.0

| ID | Requirement | Priority |
|---|---|---|
| FR-AAN-V2.0-001 | The system shall compute spending trends, period-over-period and month-over-month percentage changes, category growth, average daily spend, cash-flow trends, and savings-rate trends. | Must |
| FR-AAN-V2.0-002 | The system shall compute unusual-spending detection, recurring spending patterns, high-frequency merchant patterns, and spending hotspots using deterministic, testable logic. | Should |
| FR-AAN-V2.0-003 | The system shall compute Budget Pacing: a deterministic estimate of whether the current spending rate is likely to exhaust a budget before the period ends. | Must |
| FR-AAN-V2.0-004 | Advanced analytics shall clearly distinguish insufficient data from a conclusion of no issue detected. | Must |

## 3.27 Safe-to-Spend (STS) — V2.0

| ID | Requirement | Priority |
|---|---|---|
| FR-STS-V2.0-001 | The system shall compute a "Safe to Spend Today" estimate from inputs including remaining budget, current spending, remaining days in the period, known upcoming expenses, savings commitments, financial buffer, and applicable recurring commitments. | Must |
| FR-STS-V2.0-002 | The Safe-to-Spend result shall be classified into a state: Healthy, Moderate, Caution, or Danger. | Must |
| FR-STS-V2.0-003 | The system shall present Safe-to-Spend as an estimate derived from recorded data, not a guarantee, with the underlying factors disclosed or accessible to the user. | Must |
| FR-STS-V2.0-004 | The deterministic Safe-to-Spend model/formula shall be documented and fixed before V2.0 implementation. | Must |
| FR-STS-V2.0-005 | The system shall define behavior for no-budget, no-income, no-upcoming-expense, negative-budget, insufficient-data, and last-day-of-period scenarios. | Must |

## 3.28 Financial Health Score (FHS) — V2.0

| ID | Requirement | Priority |
|---|---|---|
| FR-FHS-V2.0-001 | The system shall compute an optional 0–100 Financial Health Score from defined pillars: Savings Discipline, Budget Adherence, Spending Stability, Cash Cushion, Leak Control. | Must |
| FR-FHS-V2.0-002 | The score breakdown shall be explainable: the UI shall show what improved the score, what reduced it, areas needing attention, and up to three practical actions. | Must |
| FR-FHS-V2.0-003 | The score shall be labeled as an informational product metric and not presented as professional financial advice. | Must |
| FR-FHS-V2.0-004 | Exact scoring weights, normalization, thresholds, and insufficient-data behavior shall be fixed in the product/technical specification before V2.0 implementation. | Must |
| FR-FHS-V2.0-005 | The system shall not fabricate a complete score when insufficient data exists. | Must |

## 3.29 Recurring & Subscription Intelligence (SUB) — V2.0

| ID | Requirement | Priority |
|---|---|---|
| FR-SUB-V2.0-001 | The system shall use historical transaction data to identify likely recurring charges, monthly recurring overhead, annualized recurring cost, and changes in recurring cost over time. | Must |
| FR-SUB-V2.0-002 | The system shall surface upcoming recurring payments derived from detected or user-confirmed recurring patterns. | Must |
| FR-SUB-V2.0-003 | The system shall distinguish user-confirmed recurring expenses from system-detected likely recurring patterns. | Must |
| FR-SUB-V2.0-004 | The system shall not label a charge as an unused subscription without supporting evidence from recorded transaction history. | Must |

## 3.30 Leak Hunter (LKH) — V2.0

| ID | Requirement | Priority |
|---|---|---|
| FR-LKH-V2.0-001 | The system shall identify evidence-backed spending-leak patterns: repeated small purchases, micro-spending, high-frequency merchants, recurring subscriptions, and potentially unnecessary recurring spending. | Must |
| FR-LKH-V2.0-002 | Every Leak Hunter output shall be labeled with its evidentiary level — Observed Fact, Detected Pattern, AI Interpretation, or Recommendation — per the Financial Intelligence Safety model (§3, NFR-BIZ-002). | Must |
| FR-LKH-V2.0-003 | The system shall not claim a subscription is unused without supporting evidence from recorded transaction history. | Must |
| FR-LKH-V2.0-004 | Users should be able to inspect the evidence underlying a detected pattern where practical. | Should |

## 3.31 Duplicate Guard (DUP) — V2.0

| ID | Requirement | Priority |
|---|---|---|
| FR-DUP-V2.0-001 | The system shall detect likely duplicate transactions among imported or assisted-entry records, using signals including amount, merchant/title, date proximity, payment method, and source/reference. | Must |
| FR-DUP-V2.0-002 | Detected duplicates shall be surfaced as a warning requiring user decision; the system shall never auto-delete a suspected duplicate without explicit user confirmation. | Must |
| FR-DUP-V2.0-003 | The duplicate warning should explain the principal signals that caused the warning where practical. | Should |

## 3.32 CSV Import (IMP) — V2.0

| ID | Requirement | Priority |
|---|---|---|
| FR-IMP-V2.0-001 | The import flow shall proceed: Detect Columns → Parse → Validate → Duplicate Detection → Category Suggestion → Preview → User Confirmation → Commit. | Must |
| FR-IMP-V2.0-002 | Imported records shall never be committed to the database without explicit user review and confirmation at the preview stage. | Must |
| FR-IMP-V2.0-003 | Invalid rows shall be identified without silently becoming valid records. | Must |
| FR-IMP-V2.0-004 | Import should provide a summary of accepted, rejected, and review-required rows before commit. | Should |

## 3.33 Multi-Currency Foundation (CUR) — V2.0

| ID | Requirement | Priority |
|---|---|---|
| FR-CUR-V2.0-001 | The data model shall distinguish original transaction currency from the user's preferred display currency. | Must |
| FR-CUR-V2.0-002 | The system shall support INR, USD, EUR, GBP as initial priority currencies. | Must |
| FR-CUR-V2.0-003 | Where currency conversion is performed, the system shall record the conversion rate's date/source and shall not present a mixed-currency total as exact when a required rate is unavailable (e.g., offline with no cached rate). | Must |
| FR-CUR-V2.0-004 | All currency-conversion arithmetic shall preserve monetary precision per §6.2. | Must |
| FR-CUR-V2.0-005 | The system shall never overwrite a transaction's original currency merely because the user's preferred display currency changes. | Must |

## 3.34 Localization (LOC) — V2.0

| ID | Requirement | Priority |
|---|---|---|
| FR-LOC-V2.0-001 | The system shall ship with English as the initial language. | Must |
| FR-LOC-V2.0-002 | The system shall be structured for future localization into Marathi and Hindi as priority languages, with additional languages added on demand. | Should |
| FR-LOC-V2.0-003 | User-visible strings shall not be hardcoded into business logic. | Must |

## 3.35 AI API Integration & Structured Recommendations (AI) — V2.0

| ID | Requirement | Priority |
|---|---|---|
| FR-AI-V2.0-001 | The system shall support optional AI-assisted interpretation for supported V2 financial-intelligence features; core tracking, budgeting, deterministic analytics, backup, and restore shall not depend on AI availability. | Must |
| FR-AI-V2.0-002 | The AI data pipeline shall follow: Room Database → Local Analytics → Verified Financial Metrics → AI Context Builder → AI API → Structured AI Response → Validation → Recommendation. | Must |
| FR-AI-V2.0-003 | AI recommendations shall conform to a predictable structured schema including at minimum: type, title, summary, evidence, priority, suggested action, estimated impact, confidence. | Must |
| FR-AI-V2.0-004 | An AI response that fails schema validation shall be rejected and shall not be surfaced to the user as a recommendation; the system shall fall back to deterministic logic or omit the insight. | Must |
| FR-AI-V2.0-005 | AI output shall never fabricate transactions, balances, income, budgets, savings data, or history. | Must |
| FR-AI-V2.0-006 | AI output shall be clearly distinguished from deterministic financial facts and shall state uncertainty where applicable. | Must |
| FR-AI-V2.0-006 | The system shall present smart spending insights (e.g., category above baseline, discretionary spending increasing) as guidance, clearly separated from deterministic facts. | Should |

## 3.36 Conversational Financial Assistant (CHA) — V3.0

| ID | Requirement | Priority |
|---|---|---|
| FR-CHA-V3.0-001 | The system shall provide a conversational interface accepting natural-language financial questions (e.g., "How much did I spend on food this month?", "Where am I overspending?"). | Must |
| FR-CHA-V3.0-002 | Factual answers shall be computed by querying deterministic application data/logic, not by unconstrained model arithmetic; the AI layer shall be responsible for language generation, not for computing the numeric facts themselves. | Must |
| FR-CHA-V3.0-003 | The assistant shall be read-only by default; it shall never create or modify a financial record as a side effect of conversation without an explicit, separate user confirmation step. | Must |

## 3.37 Purchase Simulator (SIM) — V3.0

| ID | Requirement | Priority |
|---|---|---|
| FR-SIM-V3.0-001 | The system shall allow the user to simulate a hypothetical purchase (e.g., "Can I afford a ₹20,000 phone this month?") evaluated against current spending, remaining budget, Safe-to-Spend, upcoming known expenses, savings goals, and current recorded financial position. | Must |
| FR-SIM-V3.0-002 | The simulator shall return one of: Safe to Buy, Proceed with Caution, Delay Purchase. | Must |
| FR-SIM-V3.0-003 | A simulation shall never automatically create an expense record. | Must |

## 3.38 50/30/20 Analysis (FPL) — V3.0

| ID | Requirement | Priority |
|---|---|---|
| FR-FPL-V3.0-001 | The system shall compare eligible spending against Needs, Wants, and Savings/Goals allocations. | Must |
| FR-FPL-V3.0-002 | The 50/30/20 output shall be presented as a planning framework, not an absolute financial rule. | Must |

## 3.39 Personalized Savings Planning (SPL) — V3.0

| ID | Requirement | Priority |
|---|---|---|
| FR-SPL-V3.0-001 | The system may recommend potential savings targets, spending reductions, goal-oriented budget changes, required saving pace, and acceleration opportunities, derived from recorded data. | Should |

## 3.40 Personalized Budget Optimization (BOPT) — V3.0

| ID | Requirement | Priority |
|---|---|---|
| FR-BOPT-V3.0-001 | The system may suggest category budget adjustments, allocation improvements, and potential savings targets based on historical data. | Should |
| FR-BOPT-V3.0-002 | All budget-optimization suggestions shall remain optional and shall require explicit user action to apply. | Must |

## 3.41 Multi-Provider AI Abstraction (MPA) — V3.0

| ID | Requirement | Priority |
|---|---|---|
| FR-MPA-V3.0-001 | The AI integration layer shall be built behind a provider-agnostic abstraction, not hard-coded to a single AI vendor. | Must |
| FR-MPA-V3.0-002 | Regardless of provider, data-minimization and privacy controls defined in §5.2 (Security) shall remain mandatory. | Must |

---

# 4. External Interface Requirements

## 4.1 User Interfaces

- UIX-1: The UI shall be built with Jetpack Compose and Material 3 components.
- UIX-2: The primary Add-Expense flow shall be reachable in minimal steps and shall emphasize Amount + Category + Save as the critical path; optional fields shall not obstruct this path (progressive disclosure).
- UIX-3: Every major feature shall define applicable loading, empty, success, error, offline, permission-denied, and feature-specific states described in Section 11.
- UIX-4: Low-confidence AI/OCR results shall be visually distinguished and require verification before acceptance.
- UIX-5: Primary navigation shall expose: Home, Expenses, Add Expense, Analytics, Budget, Settings/Profile (V1.0 baseline navigation).

## 4.2 Hardware Interfaces

- HW-1: Camera (via CameraX) — receipt/bill image capture (V1.2+).
- HW-2: Microphone — voice-assisted entry (V1.2+).
- HW-3: Biometric sensor (fingerprint/face, via BiometricPrompt) — app unlock (V1.1+).
- HW-4: Device secure element / Android Keystore (and StrongBox where available) — cryptographic key storage.

## 4.3 Software Interfaces

- SW-1: Room persistence library — primary local database.
- SW-2: ML Kit or equivalent on-device/privacy-conscious OCR engine — receipt text extraction.
- SW-3: Android Speech APIs — voice input capture.
- SW-4: WorkManager — background/scheduled work (notifications, recurring checks), constrained to justified use.
- SW-5: Jetpack Glance (or current supported widget framework) — home-screen widgets.
- SW-6: Android Share Sheet integration — screenshot/share-to-FINORA entry.
- SW-7: External AI API (V2.0+) — optional, provider-abstracted, used only for the AI-specific features defined in §3.35–§3.41.
- SW-8: Android system file/sharing APIs — backup save/share, report export/share.

## 4.4 Communications Interfaces

- COM-1: The application shall function with zero network connectivity for all V1.0–V1.1 requirements and for all deterministic V1.2–V3.0 requirements.
- COM-2: Network communication shall occur only for: (a) explicit user-initiated backup destination selection to a network location (cloud drive, etc. — handled by the OS share mechanism, not by FINORA directly), and (b) optional AI API calls (V2.0+), which shall transmit only minimized, purpose-specific context (see §5.2).

---

# 5. Non-Functional Requirements

## 5.1 Performance (NFR-PERF)

| ID | Requirement |
|---|---|
| NFR-PERF-001 | Common local interactions (add expense, open dashboard, apply a filter) shall feel immediate on representative target devices. |
| NFR-PERF-002 | The application shall remain usable as expense history grows into the thousands of records; the data layer shall use paged/indexed queries rather than loading entire datasets into memory. |
| NFR-PERF-003 | Dashboard and basic local analytics computation shall not require network access and shall not measurably degrade due to AI-feature availability or unavailability. |
| NFR-PERF-004 | Background processing (notifications, recurring checks) shall be minimized to reduce battery impact and shall use OS-appropriate scheduling (WorkManager constraints) rather than persistent wake locks. |
| NFR-PERF-005 | Receipt images, generated reports, backups, and temporary files shall be managed to avoid unbounded local storage growth. |

## 5.2 Security & Privacy (NFR-SEC)

| ID | Requirement |
|---|---|
| NFR-SEC-001 | No PIN, password, or credential shall be stored in plaintext at any layer, including logs. |
| NFR-SEC-002 | All local financial data shall be protected using an approved encrypted-storage approach; cryptographic keys shall be managed via Android Keystore/StrongBox where available. |
| NFR-SEC-003 | Profile/data ownership shall be strictly enforced at the data layer (see FR-PROF-V1.0-003); the product's stated security objective is zero confirmed cross-profile or cross-account financial data leakage incidents. |
| NFR-SEC-004 | Biometric authentication shall be implemented exclusively through official Android BiometricPrompt APIs. |
| NFR-SEC-005 | External data transmission shall be minimized; AI feature context shall include only the minimum data required for the specific requested feature. |
| NFR-SEC-006 | Destructive actions (delete expense/category, restore, account reset) shall require explicit user confirmation. |
| NFR-SEC-007 | Sensitive screens (e.g., showing full transaction detail) should support OS-level screen-privacy protection where appropriate (e.g., preventing content in the recent-apps screenshot where feasible). |
| NFR-SEC-008 | The application shall not perform hidden financial data collection or unnecessary background data access; all permissions shall be minimized to what each active feature requires. |
| NFR-SEC-009 | Production logs shall never contain: financial record contents, sensitive transaction details, backup passwords, PINs, authentication secrets, or AI credentials. |
| NFR-SEC-010 | Optional external capabilities (AI API, cloud backup destinations) shall remain user-controlled and off by default where applicable. |

## 5.3 Reliability & Data Integrity (NFR-REL)

| ID | Requirement |
|---|---|
| NFR-REL-001 | The system shall prevent silent data loss under normal operation, including app interruption (kill, crash, low battery) during a write operation. |
| NFR-REL-002 | Failure or unavailability of any optional service (camera, microphone, notifications, AI, network) shall degrade gracefully and shall never break core expense-tracking functionality. |
| NFR-REL-003 | Budget and analytics calculations shall remain internally consistent (e.g., category totals shall sum to the period total) under concurrent read/write operations. |
| NFR-REL-004 | Room database schema migrations shall be tested for every version that changes the schema; production builds shall not use destructive-fallback migrations that silently erase user data. |
| NFR-REL-005 | Backup and restore code paths shall receive test coverage beyond standard UI changes, given their potential for irreversible data loss (see §3.12). |
| NFR-REL-006 | Input validation shall reject invalid financial input (§3.2) before persistence, not after. |

## 5.4 Usability & Accessibility (NFR-UX)

| ID | Requirement |
|---|---|
| NFR-UX-001 | The application shall support Android screen readers (TalkBack) with meaningful content descriptions on all interactive and informational elements. |
| NFR-UX-002 | The application shall support dynamic/scalable system text sizing. |
| NFR-UX-003 | UI shall maintain adequate color contrast for text and status indicators. |
| NFR-UX-004 | Touch targets shall meet accessible minimum size guidelines. |
| NFR-UX-005 | Status information (e.g., budget On Track / Near Limit / Over Budget) shall never be communicated by color alone; it shall also be conveyed through text or icon. |
| NFR-UX-006 | Charts presenting important financial information shall have an accessible text-based equivalent summary. |
| NFR-UX-007 | Error messages shall be accessible to screen readers and written in plain language. |
| NFR-UX-008 | The application shall consider reduced-motion preferences for animated transitions. |

## 5.5 Maintainability & Portability (NFR-MAINT)

| ID | Requirement |
|---|---|
| NFR-MAINT-001 | The application shall follow a layered/MVVM-style architecture separating UI, application logic, and the repository/data layer, to support independent testing of each layer. |
| NFR-MAINT-002 | Each release version shall implement only its assigned scope (see §8.3); unapproved features, screens, dependencies, backend infrastructure, external services, or alternative architectures shall not be introduced. |
| NFR-MAINT-003 | The AI integration layer shall be abstracted from any single provider implementation to support future provider changes without core rework (fully realized at V3.0, FR-MPA-V3.0-001). |

## 5.6 Business/Product Rule Constraints (NFR-BIZ)

| ID | Requirement |
|---|---|
| NFR-BIZ-001 | Financial Guardrail Philosophy: the system shall warn about budget or limit violations rather than block the recording of a real transaction. Hard blocking is explicitly not a core product rule (see §3.7). |
| NFR-BIZ-002 | Financial Intelligence Safety: every piece of analytical or AI-derived output shall be classifiable into exactly one of: Observed Fact, Deterministic Calculation, Pattern Detection, AI Interpretation, or Recommendation, and these levels shall not be visually or semantically mixed (see FR-LKH-V2.0-002, FR-AI-V2.0-005/006). |
| NFR-BIZ-003 | Real-Data Requirement: no production build shall ship with fake expenses, hardcoded dashboard/widget totals, fake charts, fake budget or account balances, fake savings progress, or fabricated AI history — including in AI-agent-generated (Antigravity) screens. |
| NFR-BIZ-004 | AI shall not be the authority for financial facts. |
| NFR-BIZ-005 | Explicit user confirmation is mandatory before uncertain assisted input becomes a financial record. |
| NFR-BIZ-006 | A hypothetical simulation shall never be treated as an actual transaction. |
| NFR-BIZ-007 | A recurring rule shall not automatically masquerade as historical transaction data. |
| NFR-BIZ-008 | Insufficient data shall be shown as insufficient data rather than replaced with invented values. |

## 5.7 Privacy-Safe Product Telemetry (NFR-PRIV)

| ID | Requirement |
|---|---|
| NFR-PRIV-001 | Product metrics may be instrumented only when explicitly approved and privacy-justified. |
| NFR-PRIV-002 | Telemetry shall not contain raw expense records, financial histories, backup passwords, PINs, authentication secrets, or unnecessary AI context. |
| NFR-PRIV-003 | Telemetry retention and transmission shall follow the approved privacy policy. |
| NFR-PRIV-004 | The absence of telemetry shall not affect core application functionality. |

---

# 6. Data Requirements

This section states data-level rules only. Entity-relationship schemas, field types, indexes, and migration scripts are the responsibility of the Technical Architecture Document.

## 6.1 Major Data Areas

The data model shall be organized around, at minimum, these logical entities: Profile, Expense, Category, Payment Method, Account/Wallet, Income, Budget, Savings Goal, Recurring Expense, App Preference, Backup Metadata, AI Preference.

## 6.2 Monetary Data Rules

- Monetary values shall use an exact/appropriate numeric representation (not native floating point) for storage and calculation.
- Currency handling shall be consistent across the application; every monetary value shall carry an explicit currency.
- Calculations shall be deterministic and reproducible: identical inputs shall always produce identical outputs.
- No precision may be lost through UI formatting; the stored value and the display-formatted value shall be strictly separated (formatting is presentation-only).

## 6.3 Date & Time Rules

- A single consistent date/time strategy shall govern the application (e.g., consistent time zone handling, consistent definition of "day," "week," "month," "year" boundaries).
- The following operations shall be correct under this strategy: current-day expense grouping, month-boundary calculations, recurring-date generation, report period calculation, and backup timestamping.
- Any future migration of date-format representation shall preserve historical accuracy of all date-dependent calculations.

## 6.4 Backup/Export/Sync Distinction

| Concept | Definition | Format |
|---|---|---|
| Export | Human-readable, non-restorable output | CSV, PDF |
| Backup | Machine-restorable, encrypted application data | `.enc` |
| Sync | Optional future capability to align multiple devices | Not required for V1.0–V2.0 core operation |

If synchronization is introduced in a future version: local changes shall be retained safely while offline, queued changes shall resume after connectivity returns, duplicate creation shall be prevented, conflicts shall be resolved deterministically, sync state shall be observable, and silent data loss shall be prohibited.

## 6.5 Data Lifecycle

### Persistent Data

- Expenses.
- Income.
- Categories.
- Budgets.
- Savings goals.
- Recurring rules.
- Profile information.
- User preferences.

### Temporary Data

- OCR intermediate data.
- Temporary images.
- Intermediate import files.
- AI request/response buffers.
- Temporary generated processing artifacts.

Temporary data shall follow a documented retention/deletion policy.

### Externalized Data

Data may leave the device only through:

- Explicit user export/share.
- Explicit backup sharing.
- Minimum required AI context for an enabled AI feature.
- Other explicitly approved user actions.

## 6.6 Data Source-of-Truth Rules

| Data | Source of Truth |
|---|---|
| Expense | Persisted Expense record |
| Income | Persisted Income record |
| Budget | Budget configuration + deterministic aggregation |
| Dashboard totals | Deterministic calculations over authoritative records |
| Analytics | Deterministic calculations over authoritative records |
| Savings progress | Contributions − withdrawals |
| Recurring projection | Recurring rule, not historical expense |
| Safe-to-Spend | Deterministic model |
| Financial Health Score | Deterministic scoring model |
| AI recommendation | Validated AI output grounded in verified metrics |
| Backup | Validated application dataset |
| CSV/PDF | Export representation, not authoritative storage |

---

# 7. Logical System Architecture

```
                     FINORA (Native Android)
                            │
              ┌─────────────┼──────────────┐
              │              │              │
           Room DB      Android Features  External APIs
              │              │              ┌───┴────┐
              │       Camera / Voice /     AI      Auth
              │       Notifications /   (optional) (if
              │       Widgets                       introduced)
              │
        Local Analytics
        (deterministic,
         no AI dependency)
```

- The Room database is the single local source of truth for all persisted financial facts.
- Local Analytics reads only from Room and shall never depend on AI or network availability.
- AI (when enabled) consumes verified metrics already computed by Local Analytics — it does not compute base facts itself (see FR-AI-V2.0-002, FR-CHA-V3.0-002).
- Android platform features (camera, voice, notifications, widgets, share sheet) sit alongside the core and route user-generated content through the shared assisted-entry pipeline before it becomes committed Room data.
- Backup/restore shall operate on a versioned logical backup format rather than permanently depending on the physical Room database file.

---

# 8. Other Requirements

## 8.1 Regulatory / Legal

FINORA is explicitly **not** a payment processor, banking service, lending application, brokerage/investment platform, enterprise accounting system, payroll system, tax filing platform, or public/social financial network, and shall not perform automated financial transaction execution. No regulatory requirements specific to those domains apply to this SRS's scope. Any future feature that would cross into a regulated domain (e.g., bank aggregation) requires a new requirements pass and is explicitly out of scope here.

## 8.2 Reporting & Metrics Requirements (Informational)

The following product metrics should be instrumentable (locally, without compromising privacy) to evaluate release success, though target thresholds are a product-management decision outside this SRS's scope: onboarding completion, weekly expense-logging rate, average expense-entry time, 30-day retention, search/filter usage, budget setup rate, budget adherence, assisted-entry usage share, OCR/voice/NL parsing success rates, category-suggestion acceptance, duplicate-detection quality, Safe-to-Spend usage, savings-goal progress, Financial Health Score interaction, Purchase Simulator usage, AI recommendation usefulness, crash-free session rate, data-loss incidents, restore success rate, backup failure rate, and critical-workflow failure rate.

## 8.3 Implementation & Process Constraints

### 8.3.1 Version Isolation

- When the current implementation target is version *N*, only requirements tagged version ≤ *N* shall be implemented.
- Future-version UI or logic shall not be built merely because it appears later in this SRS or the roadmap.
- Future requirements may inform extensibility decisions (e.g., leaving room in the schema) but shall not expand the current version's delivered scope.

### 8.3.2 Requirement Traceability

Every implementation task shall trace to a requirement ID in this SRS (or an approved technical task in the companion TAD) following: **Requirement → Implementation → Test → Acceptance**. See Appendix A.

### 8.3.3 No Scope Expansion

Unapproved features, screens, dependencies, backend infrastructure, external services, or alternative architectures shall not be introduced by any implementation partner, human or AI-assisted.

### 8.3.4 Regression Protection

A new version shall not intentionally break completed, previously accepted functionality from an earlier version. Regression testing against the prior version's accepted feature set is required before release.

### 8.3.5 Release Gate

A version shall not be considered complete for release until: all requirements tagged for that version are implemented; user journeys for those requirements work end-to-end; financial calculations are verified against expected test outputs; offline behavior is verified where required; security requirements for that version are satisfied; error/empty states exist for new features; accessibility has been considered; performance is acceptable on representative devices; regression tests pass; and documentation (including this SRS and the TAD) is kept synchronized with what was actually built.

---

# 9. Use Cases & Acceptance Scenarios

## 9.1 UC-EXP-V1.0-001 — Record Expense Offline

### Preconditions

- FINORA is installed.
- Room is available.
- Device has no network connection.

### Expected Result

After the user creates a valid expense:

- Expense is committed locally.
- Expense appears in history.
- Dashboard totals update.
- Budget values update.
- Analytics update.
- No network request is required.

## 9.2 UC-BUD-V1.0-001 — Exceed Budget

When a real expense causes a budget to exceed its limit:

- The expense is recorded.
- Budget becomes Over Budget.
- The financial impact is communicated.
- The transaction is not silently rejected.

## 9.3 UC-BKP-V1.1-001 — Create Encrypted Backup

Expected result:

- Supported records are validated.
- A portable encrypted `.enc` file is created.
- Excluded credentials are not included.
- Backup metadata is present.
- A clear completion/failure state is shown.

## 9.4 UC-BKP-V1.1-002 — Restore Encrypted Backup

Expected result:

- Backup format is validated.
- Password is verified.
- Restore preview is shown.
- User explicitly confirms restore.
- Import is atomic.
- Restored data is verified.
- Existing data is preserved if restore fails.

## 9.5 UC-PIN-V1.1-001 — Forgotten PIN

Expected result:

- The approved secure recovery/reset policy is followed.
- No undocumented bypass exists.
- Any destructive consequence is explained before it occurs.

## 9.6 UC-CAM-V1.2-001 — Receipt Capture

Expected result:

- Receipt image is captured.
- Candidate fields are extracted where possible.
- Candidate values are editable.
- User confirms before save.
- OCR failure falls back to manual entry.

## 9.7 UC-VOI-V1.2-001 — Voice Entry

Example:

> "Spent 250 rupees on lunch using UPI."

Candidate fields:

```text
Amount: ₹250
Category: Food
Title: Lunch
Payment Method: UPI
Date: Today
```

The user confirms or edits before save.

## 9.8 UC-REC-V1.2-001 — Recurring Expense

Expected result:

- A recurring rule is stored.
- An upcoming occurrence is distinguishable from an actual expense.
- No historical expense is silently created solely because the schedule date arrives.

## 9.9 UC-SAV-V1.2-001 — Savings Goal

Example:

```text
Target: ₹50,000
Contributions: ₹20,000
Withdrawals: ₹2,000
```

Expected derived amount:

```text
₹18,000
```

## 9.10 UC-STS-V2.0-001 — Safe-to-Spend

Expected result:

- Deterministic inputs are used.
- The result is classified.
- Major factors are accessible.
- The result is labeled an estimate.
- Missing/insufficient data is explicitly handled.

## 9.11 UC-FHS-V2.0-001 — Financial Health Score

Expected result:

- Fixed deterministic scoring rules are used.
- Pillar contributions are explainable.
- Insufficient data produces an explicit state rather than a fabricated score.

## 9.12 UC-AI-V2.0-001 — Structured AI Recommendation

Expected pipeline:

```text
Room
 ↓
Local Analytics
 ↓
Verified Metrics
 ↓
AI Context
 ↓
AI API
 ↓
Structured Response
 ↓
Schema Validation
 ↓
Recommendation
```

If validation fails, the recommendation is rejected/omitted and local analytics remain available.

## 9.13 UC-CHA-V3.0-001 — Ask FINORA

Example:

> "How much did I spend on food this month?"

Expected behavior:

- Numerical result is derived from application data.
- AI may generate natural-language wording.
- AI does not independently calculate the number.
- Missing data is stated clearly.

## 9.14 UC-SIM-V3.0-001 — Purchase Simulation

Example:

> "Can I afford a ₹20,000 phone this month?"

Expected behavior:

- Analysis is hypothetical.
- Result is one of the defined states.
- No expense is created.
- Result is labeled an estimate.

# 10. Edge Cases & Expected Behavior

| Feature | Edge Case | Expected Behavior |
|---|---|---|
| Expense | Amount = 0 | Reject |
| Expense | Negative amount | Reject |
| Expense | Invalid date | Reject |
| Expense | Missing category | Require category |
| Expense | No network | Save locally |
| Budget | 80% usage | Near Limit |
| Budget | 100% usage | Over Budget |
| Budget | Over budget | Allow recording |
| Dashboard | No expenses | Empty state |
| Analytics | Insufficient data | Explain insufficient data |
| Category | Delete category with expenses | Require reassignment |
| Backup | Wrong password | Reject safely; existing data unchanged |
| Backup | Corrupt file | Reject safely |
| Backup | Unsupported version | Reject clearly |
| Restore | Duplicate IDs | Prevent unintended duplication |
| Restore | Failure during import | Preserve pre-restore dataset |
| PIN | Wrong PIN | Deny unlock |
| PIN | Forgotten PIN | Use approved recovery policy |
| Camera | Permission denied | Manual entry |
| OCR | Low confidence | Flag for review |
| Voice | Permission denied | Manual entry |
| Voice | Parse failure | Manual entry |
| Quick Add | Offline parser unavailable | Manual entry / supported local fallback |
| Notification | Permission denied | Core app continues |
| Recurring | Scheduled date arrives | Projection/reminder unless defined confirmation creates transaction |
| Savings | Withdrawal exceeds saved amount | Reject or require valid correction |
| Savings | Goal reached | Mark completed |
| Safe-to-Spend | No budget | Defined unavailable/alternative state |
| Safe-to-Spend | Negative remaining budget | Do not show misleading positive amount |
| Safe-to-Spend | Insufficient inputs | Explicit insufficient-data state |
| Health Score | Insufficient data | Do not fabricate score |
| AI | Network unavailable | Local functionality continues |
| AI | Invalid schema | Reject response |
| AI | Low confidence | Label uncertainty |
| AI | No useful data | Explain insufficiency |
| Import | Invalid row | Flag/reject before commit |
| Import | Suspected duplicate | Require review |
| Currency | Missing exchange rate | Do not show false exact mixed-currency total |
| Widget | No budget | Appropriate unavailable state |
| Widget | Stale refresh | Use latest available local value; never invent data |

# 11. Feature State Requirements

## 11.1 General

```text
Idle
Loading
Success
Error
Empty
Offline
Permission Denied
```

## 11.2 Expense

```text
Draft
Validating
Saving
Saved
Save Error
```

## 11.3 Backup

```text
Preparing
Collecting
Validating
Encrypting
Creating File
Ready
Failed
```

## 11.4 Restore

```text
Selecting File
Validating File
Password Required
Decrypting
Preview Ready
Awaiting Confirmation
Importing
Verifying
Success
Failed
Cancelled
```

## 11.5 Assisted Entry

```text
Capture
Extracting
Validating
Preview
Low Confidence
Awaiting Confirmation
Saved
Failed
Fallback to Manual
```

## 11.6 AI

```text
Unavailable
Preparing Context
Requesting
Processing
Response Received
Validating
Accepted
Low Confidence
Rejected
Fallback
```

## 11.7 Budget

```text
No Budget
On Track
Near Limit
Over Budget
Insufficient Data
```

## 11.8 Safe-to-Spend

```text
Unavailable
Healthy
Moderate
Caution
Danger
Insufficient Data
```

## 11.9 Financial Health Score

```text
Unavailable
Calculating
Available
Insufficient Data
```

# 12. Definition of Done & Release Gates

## 12.1 V1.0

- Room persistence works.
- Profile and data ownership work.
- Expense CRUD works.
- Categories and payment methods work.
- Search/filter/sort work.
- Dashboard uses real data.
- Budgets and basic analytics are accurate.
- Explicit transaction currency is stored.
- Core workflows work offline.
- Required states exist.
- No fake production financial data exists.
- Critical end-to-end tests pass.

## 12.2 V1.1

- Biometric protection works.
- PIN protection works.
- PIN recovery/reset behavior is defined and tested.
- CSV/PDF export works.
- Encrypted backup creation works.
- Backup metadata/versioning works.
- Correct passwords restore correctly.
- Incorrect/corrupt/unsupported backups fail safely.
- Restore preview works.
- Restore is atomic.
- Duplicate protection works.
- Excluded secrets are absent from backups.

## 12.3 V1.2

- Notifications work where permitted.
- Camera/receipt workflow works.
- OCR failure falls back to manual entry.
- Voice entry works with confirmation.
- Natural-language Quick Add follows its offline fallback.
- Screenshot/share entry works.
- Widgets use live data.
- Shortcuts work.
- Income and cash flow work.
- Accounts/wallet foundation works.
- Recurring rules do not silently create false transactions.
- Savings goals use a consistent source of truth.
- Permission denial is handled gracefully.

## 12.4 V2.0

- Advanced analytics are deterministic and tested.
- Budget pacing has fixed expected outputs.
- Safe-to-Spend has a documented deterministic model and test vectors.
- Financial Health Score has fixed, explainable scoring rules.
- Recurring/subscription intelligence is evidence-based.
- Leak Hunter separates facts, patterns, interpretations, and recommendations.
- Duplicate Guard works.
- CSV import has preview and validation.
- Multi-currency behavior is accurate.
- AI inputs are minimized.
- AI responses are schema-validated.
- Invalid AI responses are rejected.
- Local fallback remains functional.

## 12.5 V3.0

- Conversational assistant works.
- Numerical answers are grounded in application data.
- Purchase Simulator remains hypothetical.
- 50/30/20 is presented as a planning framework.
- Savings planning is understandable.
- Budget optimization is optional.
- Advanced subscription intelligence is explainable.
- Multi-provider AI abstraction is functional.
- Privacy controls remain enforced.

## 12.6 Universal Release Gate

A version is not complete merely because screens render.

```text
Requirements
    ↓
Implementation
    ↓
Unit / Integration Tests
    ↓
End-to-End User Journeys
    ↓
Financial Calculation Verification
    ↓
Offline Verification
    ↓
Security Verification
    ↓
Accessibility Review
    ↓
Performance Review
    ↓
Regression Testing
    ↓
Documentation Sync
    ↓
Release Approval
```

# Appendix A — Requirement Traceability Matrix (Summary)

| Module | Version Introduced | Requirement Range | PRD Section |
|---|---|---|---|
| Profile & Isolation | V1.0 | FR-PROF-V1.0-001–004 | §12.2–12.3 |
| Expense CRUD | V1.0 | FR-EXP-V1.0-001–012 | §12.4–12.5 |
| Categories | V1.0 | FR-CAT-V1.0-001–006 | §12.6–12.7 |
| Payment Methods | V1.0 | FR-PAY-V1.0-001–002 | §12.8 |
| Search/Filter/Sort | V1.0 | FR-SRCH-V1.0-001–005 | §12.9–12.11 |
| Dashboard | V1.0 | FR-DASH-V1.0-001–002 | §12.12 |
| Budgets | V1.0 | FR-BUD-V1.0-001–006 | §12.13, §30 |
| Local Analytics | V1.0 | FR-ANL-V1.0-001–002 | §12.14 |
| Theme | V1.0 | FR-THM-V1.0-001 | §12.15 |
| Adaptive UI | V1.0 | FR-UIX-V1.0-001 | §14.9 |
| Biometric/PIN Security | V1.1 | FR-SEC-V1.1-001–007 | §13.2–13.3 |
| CSV/PDF Reporting | V1.1 | FR-RPT-V1.1-001–006 | §13.4–13.5, §29 |
| Encrypted Backup/Restore | V1.1 | FR-BKP-V1.1-001–018 | §13.6–13.16 |
| Notifications | V1.2 | FR-NTF-V1.2-001–003 | §14.2 |
| Camera/OCR | V1.2 | FR-CAM-V1.2-001–005 | §14.3 |
| Voice Entry | V1.2 | FR-VOI-V1.2-001–003 | §14.4 |
| NL Quick Add | V1.2 | FR-NLP-V1.2-001–003 | §14.5 |
| Screenshot/Share | V1.2 | FR-SHR-V1.2-001–002 | §14.6 |
| Widgets | V1.2 | FR-WDG-V1.2-001–005 | §14.7 |
| Shortcuts | V1.2 | FR-SHC-V1.2-001–003 | §14.8 |
| Income | V1.2 | FR-INC-V1.2-001–004 | §14.10 |
| Cash Flow | V1.2 | FR-CF-V1.2-001–004 | §14.11 |
| Accounts/Wallets | V1.2 | FR-ACC-V1.2-001–002 | §14.12 |
| Recurring Expenses | V1.2 | FR-REC-V1.2-001–006 | §14.13 |
| Savings Goals | V1.2 | FR-SAV-V1.2-001–004 | §14.14 |
| Advanced Analytics / Pacing | V2.0 | FR-AAN-V2.0-001–004 | §15.2–15.3 |
| Safe-to-Spend | V2.0 | FR-STS-V2.0-001–003 | §15.4 |
| Financial Health Score | V2.0 | FR-FHS-V2.0-001–005 | §15.5 |
| Recurring/Subscription Intel | V2.0 | FR-SUB-V2.0-001–004 | §15.6 |
| Leak Hunter | V2.0 | FR-LKH-V2.0-001–004 | §15.7 |
| Duplicate Guard | V2.0 | FR-DUP-V2.0-001–003 | §15.8 |
| CSV Import | V2.0 | FR-IMP-V2.0-001–004 | §15.9 |
| Multi-Currency | V2.0 | FR-CUR-V2.0-001–005 | §15.10 |
| Localization | V2.0 | FR-LOC-V2.0-001–003 | §15.11 |
| AI API / Structured Recs | V2.0 | FR-AI-V2.0-001–006 | §15.12–15.16 |
| Conversational Assistant | V3.0 | FR-CHA-V3.0-001–003 | §16.2–16.3 |
| Purchase Simulator | V3.0 | FR-SIM-V3.0-001–003 | §16.4 |
| 50/30/20 Analysis | V3.0 | FR-FPL-V3.0-001–002 | §16.5 |
| Savings Planning | V3.0 | FR-SPL-V3.0-001 | §16.6 |
| Budget Optimization | V3.0 | FR-BOPT-V3.0-001–002 | §16.8 |
| Multi-Provider AI | V3.0 | FR-MPA-V3.0-001–002 | §16.9 |

---

# Appendix B — Glossary

| Term | Definition |
|---|---|
| **Assisted Entry** | Any expense-entry method other than fully manual typing (camera/OCR, voice, natural-language quick add, screenshot/share, CSV import), all converging on one validate-and-confirm pipeline. |
| **Deterministic Calculation** | A financial computation that always produces the same output for the same input, independent of any AI model. |
| **Financial Health Score** | An informational 0–100 score summarizing five financial-behavior pillars; not professional financial advice. |
| **Leak Hunter** | A V2.0 feature that identifies evidence-backed patterns of wasteful or overlooked spending. |
| **Profile Isolation** | Data-layer enforcement ensuring one profile's financial records are never returned or mutated by another profile's queries. |
| **Safe-to-Spend** | An estimate of how much a user can spend today without jeopardizing their budget, computed deterministically from recorded and known commitments. |
| **Source of Truth** | The Room local database; the authoritative record for all financial facts in the application. |
| **Structured AI Recommendation** | An AI-generated suggestion conforming to a fixed schema (type, title, summary, evidence, priority, suggested_action, estimated_impact, confidence), validated before display. |
| **Recurring Rule** | A schedule describing expected future activity; it is distinct from an actual historical transaction. |
| **Savings Goal Ledger** | The authoritative record of goal contributions and withdrawals used to derive progress. |
| **Purchase Simulation** | A hypothetical financial analysis that does not create an actual transaction. |

---

# Appendix C — Assumptions, Dependencies & Open Issues

1. **Open — Minimum/target Android API level.** Not specified in the PRD; must be fixed in the TAD before implementation begins, as it affects available BiometricPrompt, Glance, and CameraX capabilities.
2. **Open — Encryption algorithm/library choice** for local storage and `.enc` backup files. PRD specifies "approved encrypted-storage approach" without naming the algorithm; TAD must select and document (e.g., AES-GCM with Android Keystore-wrapped keys).
3. **Open — Schema migration policy detail.** This SRS requires tested, non-destructive migrations (NFR-REL-004); the TAD must define the actual Room `Migration` strategy per version bump.
4. **Open — Background execution reliability.** OEM battery-optimization behavior (particularly on Xiaomi/Samsung/OnePlus devices) can suppress WorkManager-scheduled notifications; a mitigation approach (user education, battery-optimization-exemption prompt) should be defined in the TAD/UX spec.
5. **Open — AI provider selection and cost/rate-limit model** for V2.0. Deferred to TAD per FR-MPA-V3.0-001 (provider abstraction).
6. **Assumption — Single profile only** through V3.0 scope (FR-PROF-V1.0-004); multi-profile is explicitly out of this SRS's scope but must not be architecturally foreclosed.
7. **Assumption — No backend/server component** exists or is required for any requirement in this SRS; all "sync" language in §6.4 is aspirational/future and not a V1.0–V2.0 deliverable.

---

**End of FINORA Software Requirements Specification v1.1**
\n## Critical Decisions Required Before Implementation\n\n### C.8 PIN Recovery Policy\n\nA local-only application cannot assume a server-based password reset. The approved secure recovery/reset policy must be defined before V1.1 implementation and must not provide an undocumented bypass.\n\n### C.9 Safe-to-Spend Formula\n\nBefore V2.0 implementation, the deterministic model, inputs, missing-data behavior, thresholds, and fixed test vectors must be documented.\n\n### C.10 Financial Health Score Weights\n\nBefore V2.0 implementation, exact pillar weights, normalization, thresholds, and insufficient-data behavior must be fixed and tested.\n\n### C.11 Recurring Expense Creation Policy\n\nThe product must explicitly define when a projected recurring occurrence can become an actual expense. The default rule is that a projection/reminder is not a historical transaction.\n\n### C.12 Savings Goal Source of Truth\n\nGoal progress shall be derived from contributions and withdrawals; independently conflicting balance fields must not become a second source of truth.\n\n### C.13 Quick Add Offline Strategy\n\nBefore V1.2 implementation, specify the local parser or the explicit manual-entry fallback when external parsing is unavailable.\n\n### C.14 Telemetry Privacy Policy\n\nIf product telemetry is implemented, event scope, retention, transmission, and privacy rules must be approved before implementation.\n\n# Appendix D — Version Scope Summary\n\n## V1.0 — TRACK\n\n```text\nRoom\nProfile / Data Isolation\nExpense CRUD\nCategories\nPayment Methods\nSearch / Filter / Sort\nDashboard\nMonthly / Category Budgets\nBasic Local Analytics\nExplicit Transaction Currency\nTheme\nAdaptive UI\nOffline Core\n```\n\n**Objective:** Build an excellent, reliable local expense tracker.\n\n## V1.1 — PROTECT & OWN\n\n```text\nBiometric Lock\nPIN\nSecure PIN Recovery\nCSV Export\nPDF Reports\nEncrypted Backup\nRestore\nValidation\nRestore Preview\nAtomic Restore\nDuplicate Protection\nData Portability\n```\n\n**Objective:** Protect and recover the user's financial data.\n\n## V1.2 — EXPERIENCE & EXPAND\n\n```text\nNotifications\nCamera / Receipt OCR\nVoice Entry\nNatural-Language Quick Add\nScreenshot / Share Entry\nWidgets\nShortcuts\nIncome\nCash Flow\nAccounts / Wallet Foundation\nRecurring Expenses\nSavings Goals\n```\n\n**Objective:** Make FINORA genuinely native to Android and broaden its financial foundation.\n\n## V2.0 — UNDERSTAND\n\n```text\nAdvanced Local Analytics\nBudget Pacing\nSafe-to-Spend\nFinancial Health Score\nRecurring / Subscription Intelligence\nLeak Hunter\nDuplicate Guard\nCSV Import\nMulti-Currency\nLocalization Foundation\nAI API\nStructured AI Recommendations\n```\n\n**Objective:** Turn financial history into actionable understanding.\n\n## V3.0 — IMPROVE\n\n```text\nConversational Assistant\nPurchase Simulator\n50/30/20 Analysis\nPersonalized Savings Planning\nAdvanced Budget Optimization\nAdvanced Subscription Intelligence\nMulti-Provider AI\n```\n\n**Objective:** Help users plan and improve their financial behavior.\n