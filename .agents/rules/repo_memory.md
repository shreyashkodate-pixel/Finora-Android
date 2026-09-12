# FINORA Repository Memory & Architecture Rules

## 1. Project Context
* **App**: FINORA — Native Android, offline-first, privacy-first personal finance app.
* **Tech Stack**: Jetpack Compose, Material 3, Room DB (SQLite), Kotlin Flow/Coroutines.
* **Active Status**: Release V1.0 ("Track") completed (Screens 1 to 12).
* **Reference Documents**:
  * [progress.md](file:///Users/apple/Documents/Projects/Finora_Android/progress.md): Details all completed features, screen implementations, and verification logs.
  * [Technical_Dept.md](file:///Users/apple/Documents/Projects/Finora_Android/Technical_Dept.md): Details technical debt, architectural improvements, and upcoming roadmap items (V1.1 to V3.0).
  * [AGENTS.md](file:///Users/apple/Documents/Projects/Finora_Android/AGENTS.md): Coding rules, security, environment configuration, and git etiquette.

## 2. Key Architectural Invariants
* **Monetary Representation**: Store and calculate all money in minor units (integer cents/paise). Never use Float or Double for stored financial values.
* **Deterministic Calculations**: Core statistics, budget calculations, and pacing must be calculated locally and deterministically without AI dependencies.
* **Single Source of Truth**: Room DB is the sole persistent store.
* **Version Boundaries**: Adhere strictly to the current version scope. Never jump ahead to unapproved future features.
* **Zero System Keyboard for Expenses**: Use the custom docked `FinancialKeypad` for expense entries.
* **In-Place Actions**: All expense item cards in Expenses and Home must support direct Edit and Delete options protected by confirmation dialogs.
