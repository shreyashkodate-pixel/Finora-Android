# FINORA — MASTER UI/UX IMPLEMENTATION PROMPT

You are designing and implementing the UI/UX for **FINORA**, a native Android, offline-first, privacy-first personal finance application.

You have been provided with:

1. `FINORA_FINAL_PRD.md` — Master Product Requirements Document
2. `FINORA_FINAL_SRS.md` — Final Software Requirements Specification
3. `FINORA_35_SCREENS_ORDERED.zip` — 35 Stitch-generated screen references containing the intended visual designs

## 1. SOURCE OF TRUTH — CRITICAL

Follow this priority order:

**PRD → SRS → approved UI/UX decisions → Stitch screens**

The PRD and SRS are the authoritative product and functional requirements.

The 35 Stitch screens are primarily **visual/design references**.

Do NOT blindly reproduce functionality simply because it appears in a Stitch screen.

If a Stitch screen conflicts with the PRD/SRS:

**PRD/SRS wins.**

Do not invent new features, flows, backend services, databases, APIs, permissions, or architecture.

The SRS explicitly requires version isolation and prohibits unapproved scope expansion.

---

# 2. VERSION ISOLATION — VERY IMPORTANT

FINORA has five planned releases:

- V1.0 — Track
- V1.1 — Protect & Own
- V1.2 — Experience & Expand
- V2.0 — Understand
- V3.0 — Improve

Only implement UI/UX belonging to the currently approved release.

Do NOT implement future-version functionality simply because one of the 35 Stitch screens contains it.

Future screens may be retained as design references, but they must not accidentally become functional features in the current release.

---

# 3. CURRENT UI/UX TARGET

For the initial implementation, prioritize **FINORA V1.0**.

V1.0 should focus on:

- Local profile
- Profile isolation
- Expense tracking
- Categories
- Payment methods
- Search
- Filter
- Sort
- Dashboard
- Monthly budget
- Basic local analytics
- Theme
- Offline operation

The primary V1.0 navigation should expose:

**Home**
**Expenses**
**Add Expense**
**Analytics**
**Budget**
**Settings/Profile**

The Add Expense flow must be extremely easy to access.

---

# 4. TECHNOLOGY

Implement the UI using:

- Kotlin
- Jetpack Compose
- Material 3
- Native Android

Follow a clean, maintainable architecture compatible with the SRS.

Do not introduce an alternative UI framework.

---

# 5. STITCH SCREEN USAGE

Inspect all 35 Stitch screens carefully.

Use them to reproduce the intended:

- Visual hierarchy
- Layout
- Spacing
- Typography
- Cards
- Buttons
- Icons
- Navigation
- Forms
- Charts
- Empty states
- Dialogs
- Bottom sheets
- Colors
- Light/dark theme
- Interaction patterns

However, do not treat Stitch's sample financial numbers as real application data.

The Stitch screens are mockups.

All production financial information must eventually come from the application's actual data layer.

---

# 6. NO FAKE FINANCIAL DATA — CRITICAL

NEVER hardcode production financial data such as:

- ₹ amounts
- expense totals
- budgets
- category totals
- account balances
- savings progress
- transaction history
- analytics
- charts
- AI results

Do not create fake financial activity just to make the UI look populated.

If there is no data, show a proper empty state.

For previews/development-only UI, mock data may be used only where technically necessary and must be clearly isolated from production behavior.

---

# 7. ADD EXPENSE — MOST IMPORTANT FLOW

The primary expense-entry path must emphasize:

**Amount + Category + Save**

Optional information must not obstruct the basic task.

Use progressive disclosure for optional fields such as:

- Title
- Payment method
- Notes
- Receipt
- Advanced metadata

The user should be able to record an ordinary expense quickly.

The basic flow should work completely offline.

After saving a valid expense:

1. Save it to the local data source.
2. Show success feedback.
3. Update expense history.
4. Update dashboard totals.
5. Update budget values.
6. Update analytics.
7. Do not require a network request.

---

# 8. STATES — DO NOT BUILD ONLY THE HAPPY PATH

Every major screen and feature must have appropriate states.

Design and implement where applicable:

### Loading
Use lightweight loading behavior appropriate for local operations.

Avoid unnecessary blocking spinners.

### Empty
Explain:

- What is missing
- Why it matters
- What the user can do next

Example:

"No expenses yet"

"Start tracking your spending to see your dashboard and analytics."

"Add your first expense"

### Success
Clearly communicate successful operations.

### Error
Every error should explain:

- What happened
- Whether the data was saved
- What the user can do next

### Offline
FINORA is offline-first.

Do not make the user feel blocked simply because there is no network connection.

### Permission denied
If a feature requires a permission, gracefully explain the situation and provide an alternative where applicable.

---

# 9. ACCESSIBILITY — MANDATORY

Design every screen for accessibility.

Support:

- TalkBack
- Dynamic/scalable text
- Adequate contrast
- Accessible touch targets
- Meaningful semantics
- Content descriptions
- Accessible charts
- Accessible error messages
- Reduced-motion considerations

Never communicate important financial status through color alone.

For example, do not use only:

🟢 Green = On Track
🟠 Orange = Near Limit
🔴 Red = Over Budget

Also provide textual labels.

Charts must have meaningful text-based summaries.

---

# 10. FINANCIAL INFORMATION MUST BE CLEAR

Money is sensitive and important.

Prioritize:

**Accuracy → clarity → trust**

Avoid visual designs that make financial information confusing.

Clearly distinguish:

- Spending
- Budget
- Remaining budget
- Income
- Savings
- Category totals
- Trends
- Warnings

Do not imply that FINORA knows a user's bank balance unless the data actually exists.

---

# 11. BUDGET UX

Budget functionality must warn rather than block.

If a user exceeds a budget:

**Record the expense anyway.**

Then clearly communicate:

- Budget limit
- Amount spent
- Remaining amount
- Percentage used
- Over-budget amount
- Status

Use the SRS-defined statuses:

- On Track
- Near Limit
- Over Budget

Do not silently reject a legitimate expense because a budget has been exceeded.

---

# 12. SEARCH / FILTER / SORT UX

The Expenses screen must support:

### Search
- Title
- Category
- Payment method/account

### Filters
- Category
- Payment method
- Amount range
- Date range

Date presets:

- Today
- This Week
- This Month
- Custom

Filters must be combinable.

### Sorting
- Newest
- Oldest
- Highest amount
- Lowest amount

Make active filters clearly visible and easy to remove.

---

# 13. DASHBOARD UX

The dashboard must use actual stored data.

Where applicable, show:

- Current-period spending
- Today's spending
- Remaining monthly budget
- Budget status
- Highest transaction
- Recent transactions
- Top categories
- Category breakdown
- Spending trend

Do not use hardcoded values.

If there is insufficient data, show an informative empty/partial-data state instead of fabricating numbers.

---

# 14. ANALYTICS UX

Basic analytics must be local and deterministic.

Do not depend on AI or network services for basic financial calculations.

Support appropriate views such as:

- Daily
- Weekly
- Monthly
- Yearly
- Category totals
- Average spending
- Highest expenses
- Budget utilization
- Basic period comparisons

Every important chart must also have a textual interpretation for accessibility.

---

# 15. SECURITY / PRIVACY UX

FINORA handles financial information.

Never display or log:

- PINs
- Passwords
- Authentication secrets
- API keys
- Backup passwords
- Sensitive credentials

Do not request unnecessary permissions.

Permissions should be requested contextually, not during startup without a reason.

Do not request location for ordinary expense tracking.

Do not expose financial data unnecessarily.

---

# 16. FUTURE FEATURE SCREENS

The 35 screens include features from later releases.

Do NOT activate them in V1.0.

Examples of later functionality include:

- Biometric/PIN protection
- CSV/PDF export
- Encrypted backup/restore
- Receipt OCR
- Voice entry
- Natural-language Quick Add
- Screenshot/share entry
- Widgets
- Income/cash flow
- Recurring expenses
- Savings goals
- Financial Health Score
- Advanced financial intelligence
- Multi-currency
- Conversational financial assistant
- Purchase Simulator

Keep future architecture extensible where appropriate, but do not expand the current release scope.

---

# 17. ASSISTED ENTRY

For future assisted-entry features, maintain a common conceptual flow:

**Capture → Extract → Validate → Preview → Confirm → Save**

Manual, voice, receipt, natural-language, screenshot/share, and import flows must ultimately converge into a consistent validation and confirmation model.

Never allow low-confidence AI/OCR output to silently become a financial record.

---

# 18. VISUAL DESIGN PRINCIPLES

FINORA should feel:

- Modern
- Clean
- Trustworthy
- Calm
- Premium but not flashy
- Easy to understand
- Financially responsible
- Fast
- Minimal
- Android-native

Avoid unnecessary:

- Decorative animations
- Excessive gradients
- Excessive cards
- Visual clutter
- Gamification that trivializes financial information
- Confusing charts
- Excessive notifications

The product philosophy is:

**"Simple on the surface. Intelligent underneath."**

The priority hierarchy is:

1. Record accurately
2. Understand clearly
3. Stay within limits
4. Plan intelligently
5. Improve financial behavior
6. Protect and recover data

---

# 19. RESPONSIVE DESIGN

Do not design only for one fixed screen size.

Use Compose layouts that adapt appropriately to:

- Different Android phones
- Small screens
- Large screens where applicable
- Different font scales
- Light mode
- Dark mode

Do not allow important buttons or financial values to become clipped or inaccessible.

---

# 20. NAVIGATION

Keep navigation predictable.

Users should always understand:

- Where they are
- What they can do
- How to go back
- What action is primary

Do not create unnecessary navigation layers.

The Add Expense action should remain highly accessible.

---

# 21. DESTRUCTIVE ACTIONS

For actions such as:

- Delete expense
- Delete category
- Clear data
- Restore backup
- Other destructive operations

provide clear confirmation.

Do not make destructive actions one-tap accidental operations.

If deleting a category affects existing expenses, require the appropriate reassignment behavior rather than silently breaking existing records.

---

# 22. IMPORTANT WARNING ABOUT STITCH

Do NOT assume:

"Stitch generated the screen, therefore the feature is approved."

Instead:

**Stitch = visual reference**

**PRD = product authority**

**SRS = functional/technical requirement authority**

If the screen contains an unapproved feature, do not implement that feature merely to match the screenshot.

If a visual element conflicts with an SRS requirement, adapt the UI while preserving the overall visual language.

---

# 23. BEFORE IMPLEMENTING

First inspect:

1. PRD
2. SRS
3. All 35 Stitch screens

Create an internal mapping:

**Screen → Feature → Version → SRS requirement → Required states → Data source → Implementation status**

Identify:

- Screens that are V1.0
- Screens that belong to future versions
- Screens that require modification
- Screens that contain unsupported functionality
- Screens that contain misleading/fake financial information
- Missing states
- Missing accessibility behavior
- Missing error handling
- Missing empty states

Do not begin blindly implementing all 35 screens.

---

# 24. UI/UX ACCEPTANCE CRITERIA

A screen is NOT complete merely because it visually resembles the Stitch design.

For each screen verify:

- Visual hierarchy
- Navigation
- Interaction
- Real data integration requirements
- Loading state
- Empty state
- Error state
- Offline behavior
- Accessibility
- Dynamic text
- Touch targets
- Dark/light theme
- Destructive-action protection
- Version scope
- Requirement traceability

---

# 25. FINAL RULE

When there is uncertainty:

**DO NOT GUESS.**

Check the PRD and SRS first.

If the requirement does not exist:

**Do not invent it.**

If the Stitch screen contains functionality outside the current release:

**Do not implement it.**

If a screen contains fake financial numbers:

**Treat them as visual placeholders only.**

If a feature requires AI:

**Do not allow AI to calculate fundamental financial facts.**

If a feature can work locally:

**Prefer the local/offline implementation.**

The final result should be a polished, accessible, trustworthy Android UI/UX that follows the FINORA PRD and SRS exactly while using the 35 Stitch screens as visual inspiration/reference.

**Do not sacrifice correctness for visual similarity.**

**Do not sacrifice usability for visual similarity.**

**Do not sacrifice privacy or financial integrity for visual similarity.**