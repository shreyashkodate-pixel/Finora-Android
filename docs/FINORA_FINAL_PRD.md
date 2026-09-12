# FINORA — Master Product Requirements Document (PRD)

**Document Version:** 2.0  
**Product Roadmap:** V1.0 → V1.1 → V1.2 → V2.0 → V3.0  
**Platform:** Native Android  
**Primary Local Database:** Room Database  
**Product Model:** Offline-first, privacy-first, user-controlled data  
**Status:** Updated Master Product Baseline  
**Implementation Partner:** Antigravity (AI coding agent)

---

# 1. Document Purpose

This document is the master Product Requirements Document for **FINORA**, a native Android personal finance application.

It combines the original FINORA product direction with the strongest selected capabilities from the reviewed comparison PRD, while preserving FINORA's core philosophy:

- Native Android.
- Room-first local data.
- Offline-first operation.
- Privacy-first design.
- User-controlled financial data.
- Progressive feature delivery.
- Deterministic financial calculations.
- AI as an assistant rather than an authority.
- Strict version-based implementation for Antigravity.

This document describes **what FINORA should become and why**. Detailed implementation contracts, database schemas, package structures, API contracts, code-level architecture, and test specifications belong in the SRS and technical architecture documents.

---

# 2. Product Identity

## 2.1 Product Name

**FINORA**

## 2.2 Working Tagline

**Track. Understand. Improve.**

The tagline is a working brand direction and may be refined during branding and UI/UX design.

## 2.3 Product Category

Personal finance / expense tracking / budgeting / financial intelligence.

## 2.4 Platform

Native Android application.

FINORA is not a PWA, web wrapper, or hybrid web application.

## 2.5 Product Positioning

FINORA should feel:

- Simple on the surface.
- Intelligent underneath.
- Fast enough for everyday use.
- Reliable without internet access.
- Private by default.
- Helpful without being intrusive.
- Financially accurate.
- Trustworthy with sensitive data.

---

# 3. Product Vision

> **Track → Organize → Budget → Understand → Improve → Protect**

FINORA should help a user move through the complete personal-finance loop:

```text
Record money activity
        ↓
Organize it
        ↓
See financial impact
        ↓
Stay within limits
        ↓
Understand patterns
        ↓
Plan better
        ↓
Improve financial behavior
        ↓
Protect and recover data
```

The product should evolve from an excellent expense tracker into a personal finance companion without sacrificing the simplicity of the core experience.

---

# 4. Problem Statement

People often fail to maintain consistent expense records because manual tracking is tedious and disconnected from useful financial insight.

Even when users record transactions, they frequently still need to determine:

- How much did I spend today?
- How much did I spend this week or this month?
- Which categories consume most of my money?
- Am I within my budget?
- How much can I safely spend today?
- Why was this month more expensive?
- Which recurring expenses are costing me money?
- How much am I saving?
- Am I progressing toward my savings goals?
- What spending habits should I change?
- Can I recover my financial records after changing or resetting my phone?

FINORA addresses these needs through a combination of fast local tracking, budgeting, analytics, native Android capabilities, privacy, data ownership, and progressively stronger financial intelligence.

---

# 5. Product Goals

## 5.1 Primary Goals

1. Make basic expense entry extremely fast.
2. Provide reliable offline-first expense tracking.
3. Store core financial information locally.
4. Make spending and budget information understandable.
5. Introduce income and savings concepts as the product matures.
6. Provide useful financial guardrails before overspending becomes severe.
7. Reduce manual entry through camera, voice, natural-language, and Android sharing capabilities.
8. Protect sensitive financial information.
9. Provide user-controlled encrypted backup and restore.
10. Use AI only after establishing reliable deterministic financial data and analytics.
11. Provide structured and explainable recommendations.
12. Deliver a polished native Android experience.
13. Maintain strict version boundaries for implementation and testing.

## 5.2 Secondary Goals

- Encourage consistent expense logging.
- Help users recognize spending patterns.
- Make financial reporting simple.
- Help users understand available spending capacity.
- Help users build savings habits.
- Make the application useful even when optional services are unavailable.

---

# 6. Product Principles

## 6.1 Simplicity Over Feature Count

Feature volume must not make everyday tracking difficult.

## 6.2 Fast by Default

Basic expense creation should require minimal interaction.

## 6.3 Offline First

Core financial operations must remain useful without network connectivity.

## 6.4 Privacy First

Financial information should remain local by default and leave the device only when the user explicitly chooses a feature requiring export, sharing, or external processing.

## 6.5 User-Owned Data

Users should be able to access, export, back up, restore, and delete their financial data.

## 6.6 Financial Accuracy

Deterministic calculations are authoritative for core financial facts.

## 6.7 AI Is an Assistant, Not an Authority

AI may explain, summarize, and recommend, but must not invent financial facts or silently execute consequential financial actions.

## 6.8 User Confirmation

Any uncertain result created by OCR, voice, natural-language parsing, import, or AI must be reviewed before becoming a committed financial record.

## 6.9 Progressive Disclosure

Simple workflows remain simple; advanced options appear when useful.

## 6.10 Graceful Degradation

Failure of camera, microphone, notifications, AI, or network must not break core expense tracking.

## 6.11 Real Data Only

Production financial UI must use real stored data, approved imported data, deterministic calculations, or clearly labeled AI observations.

No fake financial records, fake dashboard totals, fake charts, fake budget values, or static widget values are permitted in production.

---

# 7. Target Users

## 7.1 Primary Users

- College students.
- Young professionals.
- Freelancers.
- Budget-conscious individuals.
- Users managing cash, UPI, cards, wallets, and bank spending.
- Users who want visual spending insights.
- Users who want help building savings habits.
- Users who want local/private expense tracking.

## 7.2 User Problems

Users may:

- Forget to record expenses.
- Find traditional spreadsheets tedious.
- Struggle to interpret their own spending.
- Overspend category budgets.
- Lose track of recurring charges.
- Need quick answers about current spending.
- Need a safe and portable backup of financial records.

---

# 8. Product Boundaries

## 8.1 In Scope

FINORA will cover:

- Expense tracking.
- Income tracking.
- Categories.
- Payment methods/accounts.
- Budgets.
- Savings goals.
- Dashboards.
- Analytics.
- Spending guardrails.
- Notifications.
- Assisted expense entry.
- Reports.
- Encrypted backup and restore.
- Android widgets and shortcuts.
- Financial intelligence.
- Optional AI assistance.

## 8.2 Out of Scope

FINORA will not become:

- A payment processor.
- A banking service.
- A lending application.
- A brokerage/investment platform.
- An enterprise accounting system.
- Payroll software.
- A tax filing platform.
- A public/social financial network.
- An automated financial transaction executor.

Bank synchronization, shared/family finance, and other large product expansions remain future possibilities rather than core requirements.

---

# 9. Product Architecture Principles

At product level, FINORA follows:

```text
Native Android
      ↓
UI / Presentation
      ↓
Application Logic
      ↓
Repository / Data Layer
      ↓
Room Database
      ↓
Local Financial Data
```

Optional external services sit beside the local core rather than replacing it:

```text
                FINORA
                   │
        ┌──────────┼───────────┐
        │          │           │
       Room      Android    External APIs
        │       Features        │
        │          │        ┌───┴────┐
        │          │       AI     Auth
        │          │
        │      Camera/Voice/
        │      Notifications/
        │      Widgets
        │
   Local Analytics
```

The exact architecture will be defined separately in the technical architecture document.

---

# 10. Core Experience

## 10.1 Core Loop

```text
Create / Select Profile
        ↓
Unlock
        ↓
Record Activity
        ↓
See Immediate Financial Impact
        ↓
Review Budget / Safe-to-Spend
        ↓
Understand
        ↓
Adjust
```

The initial steps must remain extremely simple.

## 10.2 Assisted Entry Pipeline

Every assisted entry method follows:

```text
Capture
   ↓
Extract
   ↓
Validate
   ↓
Preview
   ↓
User Confirmation
   ↓
Save
```

No uncertain OCR, voice, import, or AI result may silently become a financial transaction.

---

# 11. Version Roadmap

| Version | Focus | Primary Outcome |
|---|---|---|
| **V1.0** | Core Expense & Budget Foundation | Excellent offline expense tracker |
| **V1.1** | Security, Reports & Data Ownership | Protected, exportable, recoverable financial data |
| **V1.2** | Native Android & Financial Foundation | Native convenience + income/cash-flow/savings foundations |
| **V2.0** | Advanced Analytics & Financial Intelligence | Actionable spending intelligence |
| **V3.0** | Personal Finance Assistant | Personalized, conversational financial planning |

Each version builds on the previous version.

A later version must not destabilize completed functionality from earlier versions.

---

# 12. FINORA V1.0 — Core Expense & Budget Foundation

## 12.1 Objective

Build a complete, reliable, offline-first expense and budget tracker before introducing advanced intelligence.

> **V1.0 should prove that the core product is excellent.**

## 12.2 Profile

Support a basic local profile with:

- Name.
- Profile image where appropriate.
- Preferred display currency.
- Theme preference.
- Other non-sensitive preferences.

## 12.3 Local Profile Isolation

The data layer should associate user-owned data with a profile identifier.

At minimum, data entities that belong to a profile should be designed so that profile ownership is enforced below the UI layer.

The UI must not be the only mechanism protecting profile separation.

This leaves FINORA structurally ready for future multi-profile capabilities without requiring a multi-profile product in V1.0.

## 12.4 Expense CRUD

Users can:

- Add expenses.
- View expenses.
- Edit expenses.
- Delete expenses.
- View expense details.
- Review expense history.

### Required Fields

- Amount.
- Category.
- Date.

### Optional Fields

- Title.
- Payment method/account.
- Notes.

### Data Integrity

- Amount must be positive.
- Invalid dates must be rejected.
- User-entered values must not be silently modified.
- Financial values must use an exact/appropriate representation.
- Each expense must have a stable unique identifier.

## 12.5 Expense Metadata

The data model should be designed to support:

- Expense ID.
- Profile ID.
- Title.
- Amount.
- Currency.
- Category.
- Payment method/account.
- Expense date.
- Notes.
- Created timestamp.
- Updated timestamp.
- Source.
- Optional attachment/reference.
- Future recurring status.

Not every field needs to be exposed in the V1.0 UI.

## 12.6 Starter Categories

- Food.
- Transport.
- Rent / Housing.
- Shopping.
- Bills.
- Entertainment.
- Health / Healthcare.
- Education.
- Other.

Users can create custom categories.

## 12.7 Category Management

Users can:

- Create custom categories.
- Rename custom categories.
- Delete custom categories.
- Reassign expenses when category deletion affects existing records.

Category appearance may support an icon and visual identity through the UI design system.

## 12.8 Payment Methods

Starter methods:

- Cash.
- UPI.
- Debit Card.
- Credit Card.
- Bank Transfer.
- Digital Wallet.
- Other.

The model should remain extensible.

## 12.9 Search

Search supported fields:

- Title.
- Category.
- Payment method/account.

## 12.10 Filters

Filters:

- Category.
- Payment method.
- Amount range.
- Date range.

Date presets:

- Today.
- This Week.
- This Month.
- Custom.

Filters should be combinable.

Example:

```text
Food
+
UPI
+
This Month
+
₹100–₹500
```

## 12.11 Sorting

Sort by:

- Newest.
- Oldest.
- Highest amount.
- Lowest amount.

## 12.12 Dashboard

The Home screen should surface:

- Current-period spending.
- Today's spending.
- Remaining monthly budget.
- Budget status.
- Highest transaction.
- Recent transactions.
- Top categories.
- Category breakdown.
- Spending trend.

The dashboard must use actual stored data.

## 12.13 Monthly Budget

Support:

- Overall monthly budget.
- Optional category budgets.
- Amount spent.
- Amount remaining.
- Percentage used.
- Over-budget amount.

### Statuses

- On Track.
- Near Limit.
- Over Budget.

Initial default thresholds may use:

- Below 80%: On Track.
- 80%–99%: Near Limit.
- 100%+: Over Budget.

The exact threshold may be refined after UX/product validation.

## 12.14 Local Analytics

V1.0 should calculate locally:

- Daily spending.
- Weekly spending where useful.
- Monthly spending.
- Yearly spending.
- Category totals.
- Average spending.
- Highest expenses.
- Budget utilization.
- Basic period comparisons.

## 12.15 Theme

Support:

- Light.
- Dark.
- System preference.

## 12.16 V1.0 Navigation

Navigation should be Android-native and make these areas easy to access:

- Home.
- Expenses.
- Add Expense.
- Analytics.
- Budget.
- Settings/Profile.

Add Expense must remain highly accessible.

## 12.17 V1.0 Non-Goals

Do not implement:

- AI assistant.
- AI recommendations.
- Receipt OCR.
- Voice entry.
- Widgets.
- Advanced notifications.
- Safe-to-Spend.
- Financial Health Score.
- Purchase Simulator.
- Advanced recurring/subscription intelligence.

---

# 13. FINORA V1.1 — Security, Reports & Data Ownership

## 13.1 Objective

Protect financial information and provide a robust, portable recovery mechanism.

> **Your data is protected and recoverable.**

## 13.2 Biometric App Lock

Support:

- Android biometric authentication.
- Appropriate device credential fallback.
- App lock enable/disable setting.

Sensitive application data must not be shown before successful authentication.

## 13.3 App PIN

Introduce an optional application PIN for users who prefer or need a non-biometric unlock method.

Requirements:

- Do not store the raw PIN.
- Protect verification data appropriately.
- Provide clear recovery/reset behavior within the product's security model.

## 13.4 CSV Export

Users can export supported expense records to CSV.

Export should support relevant filters/date ranges where practical.

## 13.5 PDF Reports

Generate readable reports containing supported:

- Reporting period.
- Total spending.
- Category summary.
- Transactions.
- Budget information.
- Optional summary metrics.

Reports should integrate with Android system sharing.

## 13.6 Encrypted Backup

Users can create a portable encrypted backup.

Example:

```text
expense_backup_2026_09_11.enc
```

Flow:

```text
Create Backup
      ↓
Collect Supported Data
      ↓
Validate
      ↓
Encrypt
      ↓
Create .enc File
      ↓
Save / Share
```

## 13.7 Backup Contents

The backup may contain:

- Profile information.
- Expenses.
- Categories.
- Budgets.
- Payment methods.
- Income data when available.
- Savings goals when available.
- Recurring-expense data when available.
- Supported preferences.
- Backup metadata.

## 13.8 Backup Exclusions

Never include:

- Raw account passwords.
- Raw app PIN.
- Authentication tokens.
- Refresh tokens.
- Active session credentials.
- Device-specific secrets that should not be portable.
- Unnecessary temporary cache.

## 13.9 Backup Password

The backup password must be conceptually separate from the application login/PIN.

The backup password must not be stored in plaintext.

## 13.10 Backup Destination

Users can save or share backups through Android system file/sharing mechanisms.

Possible destinations include:

- Device storage.
- Cloud drives.
- Computer.
- Messaging apps.
- Other user-selected destinations.

FINORA does not own the destination.

## 13.11 Restore

Restore flow:

```text
Fresh FINORA
      ↓
Restore Backup
      ↓
Select .enc File
      ↓
Validate File
      ↓
Enter Backup Password
      ↓
Decrypt
      ↓
Restore Preview
      ↓
Import
      ↓
Verify
      ↓
Restore Complete
```

## 13.12 Restore Preview

Before restore, display:

- Backup date.
- Backup/application version.
- Number of expenses.
- Number of categories.
- Number of budgets.
- Number of income records where applicable.
- Number of savings goals where applicable.
- Profile availability.
- Backup size where useful.

## 13.13 Backup Versioning

Store backup metadata such as:

- Backup format version.
- Application version.
- Data/schema version.
- Creation timestamp.
- Integrity information.

## 13.14 Duplicate Protection

Stable identifiers must be used to reduce accidental duplication during restoration.

## 13.15 Restore Safety

Restore must:

- Validate before import.
- Handle incorrect passwords.
- Detect invalid/corrupt files.
- Recognize supported versions.
- Avoid partial inconsistent restores.
- Provide clear success/failure outcomes.

## 13.16 Data Ownership

Users must be able to keep their encrypted backup independently from the application's installation.

## 13.17 V1.1 Non-Goals

Do not require:

- Automatic cloud synchronization.
- Cloud account dependency.
- Conversational AI.

---

# 14. FINORA V1.2 — Native Android & Financial Foundation

## 14.1 Objective

Expand FINORA with useful native Android capabilities and introduce the first broader personal-finance foundations.

> **FINORA should feel like a real Android product, while beginning to understand more than expenses alone.**

## 14.2 Notifications

Support user-controlled:

- Daily expense reminders.
- Budget threshold alerts.
- Over-budget alerts.
- Optional spending summaries.
- Recurring-expense reminders when recurring data exists.
- Savings-goal milestone notifications when savings goals exist.

Sensitive financial details should be minimized in lock-screen notifications.

## 14.3 Camera / Receipt Capture

Allow users to capture receipts/bills.

Potential extraction:

- Merchant.
- Amount.
- Date.
- Currency.
- Payment method.
- Category.
- Relevant text.

Flow:

```text
Camera / Image
      ↓
OCR / Extraction
      ↓
Validation
      ↓
Editable Preview
      ↓
User Confirmation
      ↓
Save
```

Manual entry remains available if extraction fails.

## 14.4 Voice-Assisted Entry

Natural speech such as:

> "Spent 250 rupees on lunch using UPI."

may produce:

```text
Amount: ₹250
Category: Food
Title: Lunch
Payment Method: UPI
Date: Today
```

The user must confirm or edit before saving.

## 14.5 Natural-Language Quick Add

Support a fast text-based flow such as:

> "Uber 240 cash yesterday"

The system may infer:

- Title/merchant.
- Amount.
- Date.
- Payment method.
- Category.
- Notes.

All inferred values must be presented for confirmation.

## 14.6 Screenshot / Share-to-FINORA

Users may send supported:

- Payment screenshots.
- Receipts.
- Invoices.
- Images.
- Text.

to FINORA using Android sharing mechanisms.

The content should enter the same assisted-entry pipeline:

```text
Capture
 ↓
Extract
 ↓
Validate
 ↓
Preview
 ↓
Confirm
 ↓
Save
```

## 14.7 Android Widgets

Provide useful glanceable widgets.

Potential widgets:

### Monthly Summary

```text
FINORA

₹18,420 spent
₹6,580 remaining

+ Add Expense
```

### Safe/Current Budget Widget

Later, once Safe-to-Spend is available:

```text
FINORA

Safe to spend today
₹420

Budget: On Track
```

Widgets must use live local data.

## 14.8 Android Shortcuts

Potential shortcuts:

- Add Expense.
- Scan Receipt.
- Ask FINORA (introduced later).

## 14.9 Adaptive UI

Support appropriate layouts across:

- Standard phones.
- Large phones.
- Foldables.
- Tablets where useful.

## 14.10 Income

Introduce basic income recording.

Income types may include:

- Salary.
- Freelance.
- Business/personal earnings.
- Other income.

## 14.11 Cash Flow

FINORA may calculate:

```text
Total Income
-
Total Expenses
=
Net Cash Flow
```

and optionally:

- Savings amount.
- Savings rate.

All values must come from recorded data and deterministic calculations.

## 14.12 Wallets / Accounts Foundation

Introduce optional conceptual account/payment tracking for:

- Cash.
- Bank account.
- Debit card.
- Credit card.
- Digital wallet.

The application must not claim an actual bank/card balance without sufficient user-entered data or an approved future integration.

## 14.13 Recurring Expenses

Support recurring expenses with possible frequencies:

- Weekly.
- Monthly.
- Yearly.

Provide:

- Recurring visibility.
- Upcoming payment awareness.
- Monthly normalized commitment.
- Annualized recurring cost.

## 14.14 Savings Goals

Users can create goals such as:

- Emergency fund.
- Vacation.
- Gadget.
- Vehicle.
- Education.
- Custom goal.

Goal information may include:

- Goal ID.
- Name.
- Target amount.
- Current saved amount.
- Currency.
- Target date.
- Contributions.
- Withdrawals.
- Progress.

The application may calculate:

- Required saving pace.
- Current pace.
- Estimated completion.
- Gap to target.

## 14.15 V1.2 Non-Goals

Do not yet require:

- Full conversational assistant.
- Financial Health Score.
- Purchase Simulator.
- Advanced AI personalization.
- Bank aggregation.

---

# 15. FINORA V2.0 — Advanced Analytics & Financial Intelligence

## 15.1 Objective

Move FINORA from reporting spending to helping users understand what their spending means and what they should pay attention to.

> **From "What did I spend?" to "What does my spending mean?"**

## 15.2 Advanced Local Analytics

Add:

- Spending trends.
- Period-over-period changes.
- Month-over-month percentage changes.
- Category growth.
- Average daily spend.
- Budget pacing.
- Unusual spending detection.
- Recurring spending patterns.
- High-frequency merchant patterns.
- Spending hotspots.
- Cash-flow trends.
- Savings-rate trends.

## 15.3 Budget Pacing

Estimate whether the current spending rate is likely to cause a budget to run out before the budget period ends.

This calculation must be deterministic.

## 15.4 Safe-to-Spend

Answer:

> **How much can I safely spend today?**

Potential inputs:

- Remaining budget.
- Current spending.
- Remaining days.
- Upcoming known expenses.
- Savings commitments.
- Financial buffer.
- User-defined constraints.
- Recurring commitments where available.

Possible states:

- Healthy.
- Moderate.
- Caution.
- Danger.

The result is an estimate based on recorded information, not a guarantee.

## 15.5 Financial Health Score

Provide an optional 0–100 informational score.

Possible pillars:

1. Savings Discipline.
2. Budget Adherence.
3. Spending Stability.
4. Cash Cushion.
5. Leak Control.

The score must be explainable.

The UI should show:

- What improved the score.
- What reduced the score.
- Areas needing attention.
- Up to three practical actions.

It is an informational product metric, not professional financial advice.

## 15.6 Recurring & Subscription Intelligence

Use historical data to improve recurring visibility:

- Likely recurring charges.
- Monthly recurring overhead.
- Annualized recurring cost.
- Changes in recurring costs.
- Upcoming recurring payments.

## 15.7 Leak Hunter

Identify evidence-backed patterns such as:

- Repeated small purchases.
- Micro-spending.
- High-frequency merchants.
- Recurring subscriptions.
- Potentially unnecessary recurring spending.

FINORA must distinguish:

- Observed fact.
- Detected pattern.
- AI inference.
- Recommendation.

It must not claim that a subscription is unused without evidence.

## 15.8 Duplicate Guard

Provide general duplicate detection for imported or assisted entries.

Possible signals:

- Amount.
- Merchant/title.
- Date proximity.
- Payment method.
- Source/reference.

The system should warn rather than automatically delete a record.

## 15.9 CSV Import

Provide:

```text
Detect columns
      ↓
Parse
      ↓
Validate
      ↓
Duplicate detection
      ↓
Category suggestions
      ↓
Preview
      ↓
User confirmation
      ↓
Commit
```

Imported records must not be committed without user review.

## 15.10 Multi-Currency Foundation

Support a product model that distinguishes:

- Original transaction currency.
- Preferred display currency.

Initial priority currencies:

- INR.
- USD.
- EUR.
- GBP.

Where conversion is introduced:

- Rates should have a clear date/source.
- Mixed-currency totals should not be presented as exact when required rates are unavailable.
- Financial arithmetic must remain precise.

## 15.11 Localization

Initial language:

- English.

Priority future languages:

- Marathi.
- Hindi.

Additional languages can be demand-driven.

## 15.12 AI API

FINORA may send selected, minimized analytical context to an AI service for interpretation.

AI should not be required for:

- Expense entry.
- Basic totals.
- Budget calculations.
- Basic analytics.
- Backup/restore.

## 15.13 AI Data Pipeline

```text
Room Database
      ↓
Local Analytics
      ↓
Verified Financial Metrics
      ↓
AI Context Builder
      ↓
AI API
      ↓
Structured AI Response
      ↓
Validation
      ↓
Recommendation
```

## 15.14 Structured AI Recommendations

AI recommendations should use predictable structured fields such as:

```text
type
title
summary
evidence
priority
suggested_action
estimated_impact
confidence
```

The exact schema belongs in the SRS/technical specification.

## 15.15 Smart Spending Insights

FINORA may surface insights such as:

- A category is above a recent baseline.
- A category consumes a larger share of the budget.
- Discretionary spending is increasing.
- Repeated purchases may represent a savings opportunity.

AI output must be presented as guidance.

## 15.16 AI Safety

AI must:

- Never fabricate transactions.
- Never fabricate balances.
- Never fabricate income.
- Never fabricate budgets.
- Never fabricate savings data.
- State uncertainty.
- Use actual structured data wherever possible.
- Fall back to deterministic app logic when AI is unavailable.

---

# 16. FINORA V3.0 — Personal Finance Assistant

## 16.1 Objective

Transform FINORA into a personalized financial companion.

> **FINORA helps users understand, plan, and improve financial behavior.**

## 16.2 Conversational Financial Assistant

Users may ask:

- "How much did I spend on food this month?"
- "What was my biggest expense?"
- "Why was this month more expensive?"
- "Where am I overspending?"
- "How much can I safely spend today?"
- "Show my subscriptions."
- "Am I on track for my emergency fund?"
- "Summarize my finances this month."

Factual answers should be grounded in actual local application data.

## 16.3 Read-Only by Default

The assistant should be read-only by default.

Any consequential action requires explicit user confirmation.

The assistant must never automatically create or modify financial records from conversation without a deliberate confirmation step.

## 16.4 Purchase Simulator

Users may ask:

> "Can I afford a ₹20,000 phone this month?"

The simulator may evaluate:

- Current spending.
- Remaining budget.
- Safe-to-Spend.
- Upcoming known expenses.
- Savings goals.
- Current recorded financial position.

Possible outcomes:

- Safe to Buy.
- Proceed with Caution.
- Delay Purchase.

The simulation must never automatically create an expense.

## 16.5 50/30/20 Analysis

FINORA may compare eligible spending against:

- Needs.
- Wants.
- Savings / financial goals.

It is a financial planning framework, not an absolute financial rule.

## 16.6 Personalized Savings Planning

FINORA may recommend:

- Potential savings targets.
- Spending reductions.
- Goal-oriented budget changes.
- Required saving pace.
- Possible acceleration opportunities.

## 16.7 Advanced Subscription Intelligence

Expand recurring analysis to:

- Likely subscriptions.
- Recurring overhead.
- Changes over time.
- Potential duplicate subscriptions.
- Potentially unnecessary recurring spending where evidence supports the suggestion.

## 16.8 Personalized Budget Optimization

FINORA may use historical data to suggest:

- Category budget adjustments.
- Spending allocation improvements.
- Potential savings targets.

Recommendations remain optional and user-controlled.

## 16.9 Multi-Provider AI

The AI architecture may support multiple AI providers through a configurable abstraction.

The product must not hard-code itself to one provider.

Possible future choices may include commercial or self-hosted models.

Regardless of provider, data minimization and privacy controls remain mandatory.

---

# 17. Global Data Model Requirements

The product should be designed around the following major data areas:

```text
Profile
Expense
Category
Payment Method
Account / Wallet
Income
Budget
Savings Goal
Recurring Expense
App Preference
Backup Metadata
AI Preference
```

Future technical documentation must define exact fields, relationships, indexes, constraints, and migrations.

---

# 18. Monetary Data Requirements

Financial calculations are critical.

The technical implementation must use a monetary representation appropriate for exact financial calculations and must avoid floating-point precision problems.

Requirements:

- Exact/appropriate representation.
- Consistent currency handling.
- Deterministic calculations.
- No loss of precision through UI formatting.
- Clear separation between stored value and display formatting.

---

# 19. Date & Time Requirements

FINORA must use a consistent date/time strategy.

The product must ensure daily, weekly, monthly, and yearly calculations remain correct.

Important operations include:

- Current-day expense grouping.
- Month boundaries.
- Recurring dates.
- Report periods.
- Backup timestamps.
- Future migration of date formats where necessary.

---

# 20. Backup, Export & Sync Distinction

FINORA must clearly distinguish three concepts.

## 20.1 Export

Human-readable output:

- CSV.
- PDF.

## 20.2 Backup

Machine-restorable, encrypted application data.

```text
FINORA Data
    ↓
Encrypted Backup
    ↓
.enc
```

## 20.3 Sync

Optional future capability for keeping multiple devices aligned.

FINORA does not require cloud synchronization for core V1/V1.1 operation.

If synchronization is introduced later:

- Local changes are queued.
- Changes resume after connectivity returns.
- Duplicate creation is prevented.
- Conflicts are handled deterministically.
- Sync state is observable.
- Data loss must not occur silently.

---

# 21. Security & Privacy Requirements

FINORA must treat all financial information as sensitive.

## 21.1 Mandatory Principles

- No plaintext PIN/password storage.
- Secure credential handling.
- Protected local financial data.
- Strict profile/data ownership enforcement.
- Biometric authentication through official Android APIs.
- Secure key handling.
- Minimal external data transmission.
- Privacy-conscious AI access.
- User-controlled sharing.
- Destructive-action confirmation.
- Sensitive-screen protection where appropriate.
- No hidden financial collection.
- No unnecessary background access.
- Permission minimization.

## 21.2 Sensitive Logging

Production logs must not expose:

- Financial records.
- Sensitive transaction details.
- Backup passwords.
- PINs.
- Authentication secrets.
- AI credentials.

## 21.3 AI Privacy

Only the minimum context required for the requested AI feature should leave the device.

## 21.4 User Control

Optional external capabilities must remain user-controlled.

---

# 22. Permission Requirements

Potential Android permissions/capabilities include:

- Camera.
- Microphone / speech.
- Notifications.

Permissions must be requested contextually.

Flow:

```text
Explain why
      ↓
Request only when needed
      ↓
Handle denial gracefully
```

Examples:

Camera denied → manual expense entry remains available.

Voice denied/unavailable → manual entry remains available.

Notifications denied → budgeting and tracking still work.

Location is not required for ordinary expense tracking.

SMS access is not part of the core FINORA roadmap and is not required for normal expense tracking.

---

# 23. Native Android Requirements

FINORA should take advantage of Android where it improves the actual product.

## 23.1 Camera

Receipt/bill capture.

## 23.2 Voice

Voice-assisted expense entry.

Future conversational voice interaction may be considered with the V3.0 assistant.

## 23.3 Biometrics

App unlock and potentially sensitive-action re-authentication.

## 23.4 Notifications

Budget warnings, reminders, recurring commitments, and useful financial alerts.

## 23.5 Widgets

- Current spending.
- Budget progress.
- Quick Add.
- Safe-to-Spend once available.

## 23.6 Shortcuts

- Add Expense.
- Scan Receipt.
- Ask FINORA once available.

## 23.7 Share Sheet

Send supported images/text into the assisted-entry pipeline.

## 23.8 Haptics

Optional subtle feedback for:

- Successful expense save.
- Confirmations.
- Receipt processing.
- Important milestones.

Haptics must not replace visual or accessible feedback.

---

# 24. UX Requirements

## 24.1 Add Expense

The primary workflow should emphasize:

```text
Amount
+
Category
+
Save
```

Optional information should not obstruct the basic task.

## 24.2 Progressive Disclosure

Optional fields can include:

- Title.
- Payment method.
- Notes.
- Receipt.
- Advanced metadata.

## 24.3 Quick Add

The product should support multiple entry paths without creating separate incompatible data models:

```text
Manual
Voice
Receipt
Natural Language
Screenshot/Share
Import
```

All eventually converge into the same validation and confirmation model.

## 24.4 Empty States

Each major feature should explain:

- What is missing.
- Why it matters.
- What the user can do next.

## 24.5 Loading States

Local data operations should feel immediate and avoid unnecessary blocking spinners.

## 24.6 Error States

Errors should clearly explain:

- What happened.
- Whether data was saved.
- What the user can do next.

## 24.7 Offline States

Important screens should communicate offline status where useful without making the user feel blocked.

## 24.8 Low-Confidence AI/OCR

Low-confidence results should be clearly indicated and require user verification.

---

# 25. Accessibility Requirements

FINORA should support:

- Screen readers such as TalkBack.
- Dynamic/scalable text.
- Adequate contrast.
- Accessible touch targets.
- Meaningful semantics/content descriptions.
- Status information that is not communicated by color alone.
- Accessible chart summaries.
- Accessible error messages.
- Reduced-motion consideration.

Important financial information shown in a chart must also be understandable through text.

---

# 26. Performance Requirements

## 26.1 Local Interaction

Common actions should feel immediate on representative Android devices.

## 26.2 Large Data Sets

The application should remain usable as expense history grows.

Avoid unnecessary loading of entire datasets into memory when a more efficient query strategy is possible.

## 26.3 Analytics

Dashboard and basic local analytics should not require network access.

## 26.4 Battery

Background activity should be minimized.

## 26.5 Storage

Receipt images, reports, backups, and temporary files should be managed responsibly.

---

# 27. Reliability & Data Integrity

FINORA is a financial recordkeeping application.

The product must:

- Prevent silent data loss.
- Validate important inputs.
- Protect destructive actions.
- Keep budget calculations consistent.
- Keep analytics consistent.
- Handle interruptions safely.
- Validate backup files before restore.
- Avoid partial inconsistent restores.
- Support database migration testing.
- Handle optional service failure gracefully.

---

# 28. Real-Data Requirement

Production FINORA must not ship with fake financial activity.

Allowed:

- Starter categories.
- Starter configuration.
- User-created data.
- Approved imported data.
- Real local data.
- Deterministic calculations.
- Clearly labeled AI observations.

Prohibited:

- Fake expenses.
- Hardcoded dashboard totals.
- Fake charts.
- Fake budget balances.
- Fake account balances.
- Fake savings progress.
- Fake AI history.
- Static widget values.

This requirement applies to Antigravity-generated screens as well as manually developed code.

---

# 29. Reporting Requirements

## 29.1 CSV

CSV export should include supported transaction fields such as:

- Date.
- Title.
- Amount.
- Currency.
- Category.
- Payment method.
- Notes.

## 29.2 PDF

PDF reports may include:

- Reporting period.
- Total income where available.
- Total expenses.
- Net cash flow where available.
- Savings amount/rate where available.
- Budget adherence.
- Category summary.
- Major transactions.
- Recurring commitments where available.
- Selected insights.

Reports must be distinguishable from restorable application backups.

---

# 30. Financial Guardrail Rules

FINORA should help users understand limits without preventing them from recording reality.

## 30.1 Default Budget Philosophy

A user should be allowed to record an expense even when a budget is exceeded.

The product should explain the impact rather than silently discard the transaction.

## 30.2 Warning vs Blocking

FINORA should prefer:

```text
"Budget exceeded by ₹1,000"
```

over silently blocking a real expense.

A future explicit confirmation mode may be considered, but hard blocking is not a core product rule.

---

# 31. Financial Intelligence Safety

FINORA must distinguish:

### Observed Fact

Example:

> Food spending this month is ₹6,240.

### Deterministic Calculation

Example:

> 82% of the Food budget has been used.

### Pattern Detection

Example:

> Food spending is higher than your recent average.

### AI Interpretation

Example:

> Recent dining activity may be contributing to higher discretionary spending.

### Recommendation

Example:

> Consider setting a weekly dining target.

These levels must not be mixed together.

---

# 32. Product Metrics

## 32.1 Core Metrics

- Onboarding completion.
- Weekly expense logging.
- Average expense-entry time.
- 30-day retention.
- Search/filter usage.
- Budget setup rate.
- Budget adherence.

## 32.2 Assisted Entry

- Percentage of expenses created through assisted methods.
- OCR success.
- Voice parsing success.
- Natural-language parsing success.
- Category suggestion acceptance.
- Duplicate detection quality.

## 32.3 Financial Intelligence

- Safe-to-Spend usage.
- Savings goal progress.
- Forecast usefulness.
- Financial Health Score interaction.
- Purchase Simulator usage.
- AI recommendation usefulness.

## 32.4 Reliability

- Crash-free sessions.
- Data-loss incidents.
- Restore success rate.
- Backup failures.
- Critical workflow failures.

## 32.5 Security

A critical product objective is:

> **Zero confirmed cross-profile or cross-account financial data leakage incidents.**

---

# 33. Feature Dependency Map

```text
                 FINORA FOUNDATION
                        │
                    Room DB
                        ↓
              Profile / Isolation
                        ↓
             Expenses / Categories
                        ↓
             Payment Methods/Accounts
                        ↓
                   Budgets
                        ↓
                  Dashboard
                        ↓
                Local Analytics
                        │
        ┌───────────────┼────────────────┐
        ↓               ↓                ↓
    Security         Reports          Native UX
        ↓               ↓                ↓
   Biometric         CSV/PDF       Camera / Voice
   PIN              Backup          Notifications
                    Restore          Widgets
        │
        └────────────────────────────────┘
                        ↓
                Financial Foundation
                        ↓
              Income / Cash Flow
                        ↓
                Recurring Expenses
                        ↓
                  Savings Goals
                        ↓
                 Advanced Analytics
                        ↓
                 Safe-to-Spend
                        ↓
                Financial Health
                        ↓
                  Leak Hunter
                        ↓
                    AI API
                        ↓
             Structured Recommendations
                        ↓
             Personal Finance Assistant
                        ↓
               Purchase Simulator
               50/30/20 Analysis
               Savings Planning
               Budget Optimization
```

---

# 34. Feature-to-Version Matrix

| Feature | V1.0 | V1.1 | V1.2 | V2.0 | V3.0 |
|---|---:|---:|---:|---:|---:|
| Room Database | ✅ | ✅ | ✅ | ✅ | ✅ |
| Local Profile | ✅ | ✅ | ✅ | ✅ | ✅ |
| Profile Isolation | ✅ | ✅ | ✅ | ✅ | ✅ |
| Expense CRUD | ✅ | ✅ | ✅ | ✅ | ✅ |
| Categories | ✅ | ✅ | ✅ | ✅ | ✅ |
| Payment Methods | ✅ | ✅ | ✅ | ✅ | ✅ |
| Search / Filter / Sort | ✅ | ✅ | ✅ | ✅ | ✅ |
| Dashboard | ✅ | ✅ | ✅ | ✅ | ✅ |
| Monthly Budget | ✅ | ✅ | ✅ | ✅ | ✅ |
| Category Budgets | ✅ | ✅ | ✅ | ✅ | ✅ |
| Basic Local Analytics | ✅ | ✅ | ✅ | ✅ | ✅ |
| Theme | ✅ | ✅ | ✅ | ✅ | ✅ |
| Biometric Lock | — | ✅ | ✅ | ✅ | ✅ |
| App PIN | — | ✅ | ✅ | ✅ | ✅ |
| CSV Export | — | ✅ | ✅ | ✅ | ✅ |
| PDF Reports | — | ✅ | ✅ | ✅ | ✅ |
| Encrypted Backup | — | ✅ | ✅ | ✅ | ✅ |
| Restore | — | ✅ | ✅ | ✅ | ✅ |
| Notifications | — | — | ✅ | ✅ | ✅ |
| Camera / Receipt | — | — | ✅ | ✅ | ✅ |
| Voice Entry | — | — | ✅ | ✅ | ✅ |
| Natural-Language Quick Add | — | — | ✅ | ✅ | ✅ |
| Screenshot / Share Entry | — | — | ✅ | ✅ | ✅ |
| Android Widgets | — | — | ✅ | ✅ | ✅ |
| Android Shortcuts | — | — | ✅ | ✅ | ✅ |
| Adaptive UI | ✅ | ✅ | ✅ | ✅ | ✅ |
| Income | — | — | ✅ | ✅ | ✅ |
| Cash Flow | — | — | ✅ | ✅ | ✅ |
| Accounts / Wallet Foundation | — | — | ✅ | ✅ | ✅ |
| Recurring Expenses | — | — | ✅ | ✅ | ✅ |
| Savings Goals | — | — | ✅ | ✅ | ✅ |
| Advanced Local Analytics | — | — | — | ✅ | ✅ |
| Budget Pacing | — | — | — | ✅ | ✅ |
| Safe-to-Spend | — | — | — | ✅ | ✅ |
| Financial Health Score | — | — | — | ✅ | ✅ |
| Leak Hunter | — | — | — | ✅ | ✅ |
| Duplicate Guard | — | — | — | ✅ | ✅ |
| CSV Import | — | — | — | ✅ | ✅ |
| Multi-Currency UI | — | — | — | ✅ | ✅ |
| Localization Expansion | — | — | — | ✅ | ✅ |
| AI API | — | — | — | ✅ | ✅ |
| Structured AI Recommendations | — | — | — | ✅ | ✅ |
| Conversational Assistant | — | — | — | — | ✅ |
| Purchase Simulator | — | — | — | — | ✅ |
| 50/30/20 Analysis | — | — | — | — | ✅ |
| Personalized Savings Planning | — | — | — | — | ✅ |
| Advanced Budget Optimization | — | — | — | — | ✅ |
| Advanced Subscription Intelligence | — | — | — | ✅ | ✅ |
| Multi-Provider AI | — | — | — | — | ✅ |

---

# 35. Requirements for Antigravity

Antigravity is the implementation partner, not the product decision maker.

## 35.1 Source of Truth

The approved FINORA PRD, SRS, architecture, and UI/UX documentation are the implementation sources of truth.

## 35.2 Version Isolation

If the current target is V1.0:

- Implement V1.0 requirements.
- Do not silently implement V1.1+.
- Do not create future UI solely because it appears in the roadmap.
- Future requirements may inform extensibility but must not expand the current scope.

## 35.3 Requirement Traceability

Major implementation tasks should map to a requirement or approved technical task.

```text
Requirement
   ↓
Implementation
   ↓
Test
   ↓
Acceptance
```

## 35.4 No Scope Expansion

Do not add unapproved:

- Features.
- Screens.
- Dependencies.
- Backend infrastructure.
- External services.
- Alternative architectures.

## 35.5 Real Data

All production dashboard, analytics, budget, report, and widget values must come from actual application data.

## 35.6 Financial Logic

Core calculations must be deterministic.

Do not use AI to calculate basic:

- Totals.
- Percentages.
- Budget usage.
- Remaining budget.
- Category totals.
- Cash flow.
- Savings calculations.

## 35.7 Assisted Entry

OCR, voice, natural-language parsing, import, and AI-assisted entry must always follow:

```text
Capture
→ Extract
→ Validate
→ Preview
→ Confirm
→ Save
```

## 35.8 Security

Do not expose:

- Passwords.
- PINs.
- Tokens.
- Financial records in logs.
- AI credentials.
- Backup passwords.

## 35.9 Backup Safety

Backup and restore changes require stronger testing than normal UI changes because incorrect behavior may cause data loss.

## 35.10 Regression Protection

New versions must not intentionally break completed earlier-version functionality.

## 35.11 Testing Discipline

Use:

```text
Run
 ↓
Test
 ↓
Validate
 ↓
Review
 ↓
Release
```

A feature is not complete merely because its UI renders.

---

# 36. Recommended Technical Direction

This is a product-level technical direction, not the detailed architecture.

Recommended native Android stack:

- **Language:** Kotlin.
- **UI:** Jetpack Compose + Material 3.
- **Architecture:** Layered / MVVM-style architecture.
- **Local database:** Room.
- **Local storage protection:** Approved encrypted-storage approach.
- **Key management:** Android Keystore / StrongBox where available.
- **Biometrics:** Android biometric APIs / BiometricPrompt.
- **Google authentication if introduced:** Credential Manager.
- **Camera:** CameraX.
- **OCR:** On-device or approved privacy-conscious OCR such as ML Kit.
- **Voice:** Android speech capabilities.
- **Background work:** WorkManager where persistent background work is justified.
- **Widgets:** Jetpack Glance or current supported Android widget approach.
- **AI:** Prefer local/on-device processing where practical; external AI remains optional.
- **Networking:** Only where a feature requires it.

Exact dependencies, versions, database schema, encryption implementation, package structure, and test architecture belong in the SRS/architecture documents.

---

# 37. Explicitly Deferred / Not Adopted as Core Features

The comparison PRD contained additional possibilities. FINORA intentionally does not make these core requirements at this stage:

## 37.1 SMS Transaction Scraping

Not part of the core FINORA roadmap.

Reason at product level:

- Unnecessary for the core value proposition.
- Introduces significant permission/privacy/policy considerations.
- Manual, voice, receipt, screenshot/share, and import paths provide safer alternatives.

## 37.2 Location-Based Expense Tracking

Not required for ordinary expense tracking.

## 37.3 Strict Budget Blocking

FINORA will not normally block users from recording a real expense simply because a budget was exceeded.

The app should record reality and communicate the financial impact.

## 37.4 Family / Shared Finance

Not part of the current roadmap.

## 37.5 Bank Aggregation

Future consideration only.

## 37.6 Wear OS / Android Auto

Future consideration only.

These items may be revisited later without changing the principles of the core product.

---

# 38. Version Completion Criteria

## 38.1 V1.0 Complete When

- Room persistence works correctly.
- Profile handling works.
- Profile ownership is enforced at the data layer.
- Expense CRUD works.
- Categories work.
- Payment methods work.
- Search/filter/sort work together.
- Dashboard uses real data.
- Budget calculations are accurate.
- Basic local analytics are accurate.
- Offline core workflows work.
- Theme support works.
- Critical flows are tested.
- No production fake financial data exists.

## 38.2 V1.1 Complete When

- Biometric lock works.
- PIN protection works.
- CSV export works.
- PDF reports work.
- Encrypted backup can be created.
- Backup validation works.
- Correct backup passwords restore data.
- Incorrect/corrupt backups fail safely.
- Restore preview works.
- Restore avoids unintended duplicates.
- Restore cannot leave the database in an unsafe partial state.

## 38.3 V1.2 Complete When

- Notifications work where permitted.
- Camera workflow works with manual fallback.
- Voice workflow works with manual fallback.
- Natural-language Quick Add works with confirmation.
- Screenshot/share assisted entry works.
- Widgets use live data.
- Shortcuts work.
- Income/cash flow works.
- Recurring expenses work.
- Savings goals work.
- Permission denial is handled gracefully.

## 38.4 V2.0 Complete When

- Advanced local analytics are deterministic and tested.
- Safe-to-Spend calculation is documented and testable.
- Financial Health Score is explainable.
- Recurring/subscription intelligence is useful and evidence-based.
- Leak Hunter clearly separates facts from recommendations.
- Duplicate Guard works without unsafe auto-deletion.
- CSV import includes validation and preview.
- AI API integration is optional to core tracking.
- AI inputs are minimized.
- AI outputs follow a structured schema.
- Invalid AI responses are handled safely.

## 38.5 V3.0 Complete When

- Conversational financial assistant works.
- Factual answers use reliable application data.
- Purchase Simulator is clearly hypothetical.
- 50/30/20 analysis is clearly presented as a framework.
- Savings planning is actionable and understandable.
- Budget optimization is optional and user-controlled.
- Advanced subscription intelligence is explainable.
- Multi-provider AI abstraction works without weakening privacy.

---

# 39. Release Philosophy

FINORA releases should be treated as stable product milestones, not merely collections of screens.

A release should move forward only when:

- Requirements are implemented.
- User journeys work.
- Financial calculations are verified.
- Offline behavior is verified where required.
- Security requirements are satisfied.
- Error and empty states exist.
- Accessibility has been considered.
- Performance is acceptable.
- Regression testing passes.
- Documentation remains synchronized.
- The version is stable enough to become the foundation for the next version.

---

# 40. Final Product Direction

FINORA should not try to become the largest finance application.

It should become one of the most useful and trustworthy ways for an Android user to understand and control everyday money activity.

### Priority hierarchy

1. **Record accurately.**
2. **Understand clearly.**
3. **Stay within limits.**
4. **Plan intelligently.**
5. **Improve financial behavior.**
6. **Protect and recover data.**

Every proposed feature should answer at least one question:

- Does it make tracking easier?
- Does it make financial information easier to understand?
- Does it help the user improve financial behavior?
- Does it protect or recover the user's data?

If not, the feature should not be added merely to increase feature count.

---

# 41. Final FINORA Roadmap

```text
FINORA V1.0
"TRACK"
│
├── Room
├── Local Profile
├── Profile Isolation
├── Expenses
├── Categories
├── Payment Methods
├── Search / Filter / Sort
├── Dashboard
├── Budgets
├── Basic Local Analytics
└── Theme

          ↓

FINORA V1.1
"PROTECT & OWN"
│
├── Biometric Lock
├── PIN
├── CSV
├── PDF
├── Encrypted Backup
├── Restore
├── Backup Validation
├── Restore Preview
└── Data Portability

          ↓

FINORA V1.2
"EXPERIENCE & EXPAND"
│
├── Notifications
├── Camera / Receipt
├── Voice
├── Natural-Language Quick Add
├── Screenshot / Share Entry
├── Widgets
├── Shortcuts
├── Income
├── Cash Flow
├── Accounts / Wallet Foundation
├── Recurring Expenses
└── Savings Goals

          ↓

FINORA V2.0
"UNDERSTAND"
│
├── Advanced Local Analytics
├── Budget Pacing
├── Safe-to-Spend
├── Financial Health Score
├── Recurring / Subscription Intelligence
├── Leak Hunter
├── Duplicate Guard
├── CSV Import
├── Multi-Currency
├── Localization Expansion
├── AI API
└── Structured AI Recommendations

          ↓

FINORA V3.0
"IMPROVE"
│
├── Conversational Assistant
├── Purchase Simulator
├── 50/30/20 Analysis
├── Personalized Savings Planning
├── Advanced Budget Optimization
├── Advanced Subscription Intelligence
└── Multi-Provider AI
```

---

# 42. Master Product Statement

> **FINORA is a native Android, offline-first personal finance companion that starts with reliable expense tracking and evolves into an intelligent, privacy-conscious system for budgeting, understanding spending, planning savings, and making better financial decisions.**

The application should feel:

> **Simple on the surface. Intelligent underneath.**

---

**End of FINORA Master PRD v2.0**
