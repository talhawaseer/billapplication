# Utility Bill Tracker: phased plan to first production release

This roadmap is based on the current repository review. The Phase 0 choices are recorded in [PRODUCT_BRIEF_V1.md](PRODUCT_BRIEF_V1.md). The Android app has not yet been built or tested, so the first release date should be set after Phase 1 establishes a working baseline.

## Recommended first-release scope

Target a **local-first FESCO bill tracker for one device**: household accounts, manually entered monthly bills, user-reported payment records, due-date reminders, reporting, and a tested encrypted user-managed backup/restore flow. V1 collects no addresses or bill images and supports English/PKR. Do not make the PostgreSQL drafts or Kotlin scaffold part of the release; they are not integrated architecture.

The public display name is PowerBill FESCO. The publisher still needs to supply a unique owned Android application ID and support contact before release configuration and store submission. If users must share data across devices later, a sync branch would require backend operations, conflict resolution, hosting, and account support.

## Phase 0 - Product decisions and release brief

**Purpose:** settle the choices that affect data, reminders, privacy, and cost before implementation expands.

**Work**

- Identify the primary user: an individual/household tracking several meters, or a bill-management business tracking customers.
- Confirm utility scope for v1 (FESCO only or multiple providers), country/currency/date formats, and whether users enter bills manually or expect bill import/OCR.
- Decide local-only versus multi-device sync; decide whether accounts are needed.
- Define exactly what “payment tracking” means: record an offline cash/bank payment, confirm a payment made to the utility, or connect to a payment provider. These are different products.
- Record the approved no-image/no-address policy, keep records until user deletion, and encrypted user-managed backup/recovery requirement.
- Record the approved reminder schedule: approximate 9 AM local time, 3 days before, due date, then daily while outstanding; stop when paid; user can disable reminders.
- Set launch goals and support model: target audience, languages, accessibility expectations, support contact, and success measures.

**Deliverables**

- One-page v1 scope and explicit out-of-scope list.
- Approved local-first or sync architecture decision.
- Data inventory, retention policy, backup/recovery expectations, and reminder behavior specification.
- Acceptance criteria for the main user journeys.

**Exit gate:** product owner can describe the first user, supported flows, data lifecycle, and reminder behavior without unresolved architecture choices.

## Phase 1 - Make the Android project build reproducibly

**Purpose:** turn the current source bundle into a reliable development baseline.

**Work**

- Repair literal escaped newlines in the manifest, main layout, and `MainActivity.java`; scan the full project for similar source corruption and syntax/resource errors.
- Add and commit the Gradle wrapper, pin the compatible Gradle/Android Gradle Plugin/JDK setup, and document a clean Android Studio setup.
- Run a clean debug build; resolve compiler, Room annotation processor, resource, and manifest errors.
- Add CI that checks out a clean copy and builds the app. Keep the first pipeline focused on a reproducible build; add test jobs as test suites arrive.
- Replace `com.example.powerbill` with a selected unique application ID and set the final user-visible app name. Confirm whether changing package identity affects any planned migration path (there are no known published users yet).
- Decide `minSdk` based on the target audience and device support. Update `targetSdk` for current platform behavior and distribution requirements. As of this roadmap date, Google Play says new apps and updates must target API 36 or higher from August 31, 2026; verify the rule again immediately before submission ([Google Play target API requirements](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en-en)).
- Remove sample values from the default production flow, or move them behind a clearly labeled demo mode that cannot be mistaken for a saved real bill.

**Deliverables:** reproducible debug build, wrapper, CI build, product identity decision, and clean separation of demo data from real records.

**Exit gate:** a clean checkout builds in CI and installs on an emulator/device; no known parser/resource errors remain.

## Phase 2 - Define and protect the financial record model

**Purpose:** make stored bills and payments trustworthy before building workflows on top of them.

**Work**

- Write down canonical rules for consumers, utility/provider accounts, monthly bills, bill revisions, late amounts, dates, and currency. Preserve integer minor-unit arithmetic; never calculate balances with floating-point values.
- Decide the duplicate key and revision policy. The current app has a unique consumer/month index and a separate globally unique bill number; confirm both against actual bills and provider behavior.
- Treat a payment as a durable financial event. Define correction as an auditable reversal/void with reason and timestamp; do not silently overwrite or delete a recorded payment.
- Define bill correction rules and history. If amount/date changes after a payment, preserve the previous values and record who/when/reason rather than making history ambiguous.
- Define statuses from due date and the sum of valid payments (paid, unpaid, partially paid, overdue) and specify how overdue partial balances display.
- Enable Room schema export and commit schema snapshots. Document migrations from every released database version; keep a disposable fixture of representative old databases.
- Decide which records may be archived, deleted, or retained, and what a consumer deletion means for historical bills/payments.

**Deliverables:** data dictionary, state-transition rules, Room schema snapshots, migration policy, and audit/correction policy.

**Exit gate:** finance and product rules can be implemented consistently in the database, forms, dashboard, reports, and backup/restore.

## Phase 3 - Build the app foundation and usable navigation

**Purpose:** give the data features a maintainable app structure and a clear interface.

**Work**

- Review the current activity/DAO code and separate UI responsibilities from database operations. Keep database work off the main thread; use lifecycle-aware state and handle loading/error states.
- Establish navigation for Dashboard, Consumers, Bills, Bill detail, Payments, Reports, Settings, and Backup/Restore.
- Standardize PKR formatting, date/month input and validation, empty/loading/error states, destructive-action confirmation, and form error placement.
- Make lists usable as records grow: search/filter/sort, efficient queries, and accessible row actions.
- Add accessibility labels, scalable text, contrast, keyboard/focus behavior, and screen-reader review.
- Keep sample data and debugging controls out of production builds.

**Deliverables:** navigation shell, shared presentation rules, and a testable app architecture with clear data/UI boundaries.

**Exit gate:** users can move through the main tasks without dead ends, and forms report invalid input or database errors clearly.

## Phase 4 - Complete consumer, bill, and payment workflows

**Purpose:** deliver the core purpose of tracking what is owed and what was paid.

**Consumer and bill work**

- Complete consumer add/edit/search/archive/restore and confirm uniqueness/validation rules.
- Complete bill creation and detail view without address or image collection in v1.
- Show billing month, issue/due dates, due-date amount, late amount, readings, notes, verification status, payment progress, and remaining balance.
- Add a correction flow with audit details; support bill revisions if real provider bills can be reissued.
- Explain duplicate bills and invalid data in the UI rather than showing raw SQLite exceptions.

**Payment work**

- Add payment entry with bill, amount, date, method, receipt/transaction reference, and optional note.
- Support partial payments, prevent payments above the current balance, and make full-payment state consistent across screens.
- Add chronological payment history and an audited reverse/void flow with reason. Show voided events in history while excluding them from balance calculations.
- Distinguish recorded payment from payment-provider confirmation; v1 should not imply that money moved unless there is a real verified payment integration.

**Dashboard and reports work**

- Use one shared status/balance definition for counts, lists, totals, reports, and notifications.
- Separate bill-month analytics from cash collected by payment date.
- Include due today, upcoming, overdue, unpaid, partial, paid, and consumer history views; clearly label date ranges and totals.

**Deliverables:** complete bill lifecycle, payment ledger UI, useful dashboard/reports, and matching acceptance criteria.

**Exit gate:** a user can add a consumer, save/verify a bill, record one or more payments, see the correct balance/status, and correct a mistake with an audit record.

## Phase 5 - Implement reliable reminders

**Purpose:** notify users at the agreed times without creating stale or duplicate reminders.

**Work**

- Implement reminder preferences and a clear explanation before requesting notification permission.
- Create notification channels and request the Android 13+ runtime notification permission where applicable. Handle denial gracefully; users must still be able to see due bills inside the app ([Android notification permission guidance](https://developer.android.com/develop/ui/views/notifications/notification-permission)).
- Prefer inexact, persistent work for ordinary bill reminders. Android recommends WorkManager for persistent background tasks; its execution is subject to system scheduling, so set user expectations that a reminder is approximate ([Android persistent work guidance](https://developer.android.com/develop/background-work/background-tasks/persistent)). Only investigate exact alarms if the approved product requirement truly needs exact-time delivery and platform policy permits it.
- Schedule unique work per bill/reminder occurrence; update it when due dates or preferences change; cancel it when a bill becomes paid, is removed, or is no longer eligible.
- At delivery time, re-read the database and calculate current status so a stale schedule cannot remind for a paid bill.
- Handle reboot, app update, time-zone changes, daylight-saving changes, changed device clock, permission revoked, app force-stop, and battery restrictions.
- Avoid sensitive amounts/reference numbers on the lock screen by default; let users choose notification detail.

**Deliverables:** reminder settings, scheduler, notification UI, and automated/manual test scenarios.

**Exit gate:** reminders are scheduled/cancelled correctly and users understand permission denial or platform-delayed delivery.

## Phase 6 - Privacy, data safety, backup, and recovery

**Purpose:** protect records and ensure users can recover them.

**Work for the recommended local-first release**

- Decide whether the local database requires encryption at rest based on user and threat model. Room/SQLite by itself should not be described as encrypted.
- Review Android backup behavior. Align manifest/cloud backup rules with the product promise; the current manifest enables backup while the older architecture note calls for encrypted backup.
- Implement user-controlled export and restore with encryption, recovery-secret/key handling, versioned format, integrity checks, and clear lost-secret behavior. Avoid storing the only recovery key alongside the export.
- Test backup and restore across app upgrades, app reinstall, device change, invalid/corrupt files, interrupted restore, and wrong recovery secret.
- Minimize collection and display of personal information. Add privacy notice, retention/deletion behavior, and a support procedure for data loss reports.
- Ensure backup/export includes only the v1 record types documented in the product brief.

**Additional work if sync is selected**

- Reconcile the PostgreSQL drafts with the Room domain model; design authenticated APIs and server-side validation before building sync.
- Define account creation/recovery/deletion, transport encryption, server-side encryption, access control, audit logs, idempotency, retries, offline queues, conflict resolution, and duplicate submission behavior.
- Add backend hosting, monitoring, database migrations/backups, incident response, and recurring operational cost ownership.
- Do not call the app synchronized until conflict, retry, and account recovery behavior has been tested across devices.

**Deliverables:** privacy/data inventory, backup or sync design, data recovery guide, and tested recovery flow.

**Exit gate:** a user can recover records under the promised scenarios, and the app's actual data handling matches its user-facing privacy statements.

## Phase 7 - Verification and release-quality hardening

**Purpose:** prove the app works under normal and failure conditions.

**Required coverage**

- Unit tests: money parsing/formatting, date/month validation, balance/status rules, bill revisions, payment limits, and reminder eligibility.
- Database tests: each migration path, uniqueness and foreign-key behavior, transaction/rollback behavior, aggregates, archive/delete constraints, and payment void calculations.
- UI tests: consumer and bill flows, partial/full payment, reporting filters, reminder permission flow, backup/restore, empty/error states, and process recreation.
- Manual device matrix: minimum supported Android version, current Android version/API target, small/large screens, at least one physical device, and an emulator.
- Failure scenarios: duplicate submission, invalid dates, time-zone changes, denied notification permission, notification disabled later, full/low storage, app killed/restarted, database upgrade, corrupt backup, and restore interruption.
- Security/privacy review: exported components, permissions, log output, backup contents, image storage, dependency vulnerabilities, signing key handling, and sensitive lock-screen notification content.
- Verify the app remains useful offline for the promised local-first behavior.

**Deliverables:** passing automated checks, manual QA checklist, known-issues list, migration test evidence, and release candidate build.

**Exit gate:** no open release-blocking defects in financial calculations, data integrity, reminders, backup/recovery, security, or accessibility.

## Phase 8 - Closed beta and operational readiness

**Purpose:** get real usage feedback before broad release and prepare support operations.

**Work**

- Recruit a small group matching the intended audience; provide a way to send feedback without exposing bill data unnecessarily.
- Give beta users a clear warning about prototype limitations and a backup path before they enter important records.
- Observe task completion and confusion around amount due versus late amount, partial payments, status labels, reminder permissions, and data recovery.
- Fix severity 1/2 defects and review performance/crash diagnostics. Do not collect bill contents in telemetry.
- Prepare app icon, screenshots, description, support email/site, privacy policy, release notes, and a customer-support response process.
- Complete Play Console declarations and data-safety disclosures based on actual behavior. Recheck current submission policy and target API requirements just before release.
- Prepare signing and release access: use a protected upload/release key process, restrict account access, and keep signing secrets out of the repository and CI logs.
- Document release versioning, database compatibility, staged rollout monitoring, and how to stop a rollout or issue a hotfix.

**Deliverables:** beta feedback summary, resolved launch blockers, store materials, policy declarations, support process, and signed release candidate.

**Exit gate:** intended users complete core tasks successfully, critical issues are closed, and the team can support and recover users after launch.

## Phase 9 - Production launch

**Purpose:** release gradually and monitor the real installation/update path.

**Work**

- Publish through the chosen channel (Google Play for public distribution, or a managed/private channel if this is an internal app).
- Start with an internal release, then a limited staged rollout; expand only while crash, ANR, data-migration, notification, and support signals remain healthy.
- Verify new install, upgrade from the beta build, notification permission prompts, backup/restore, and store listing on production configuration.
- Keep a staffed path for user reports and a documented process for bad releases. Mobile releases generally require a corrected release rather than relying on server rollback; preserve data compatibility when shipping a fix.

**Deliverables:** published production release, rollout decision log, and support/crash monitoring.

**Exit gate:** launch cohort can install/update and complete critical workflows without a release-blocking trend.

## Phase 10 - Maintenance and follow-up releases

**Purpose:** keep the app dependable after launch.

- Track crash/ANR rates, reminder scheduling/delivery reports, restore success, support volume, and feature use with privacy-preserving aggregate telemetry.
- Prioritize data-loss, incorrect-balance, migration, security, and reminder defects ahead of cosmetic work.
- Review dependency/Android SDK updates regularly and re-check store API-level requirements before every release cycle.
- Keep database migration tests and backup-format compatibility in CI.
- Maintain a documented support path for lost devices, lost recovery credentials, duplicate bills, incorrect payment entries, and account/data deletion requests.
- Revisit sync, additional providers, OCR, payment-provider integration, and web dashboard only from validated user needs; each expands operational/security scope.

## Critical dependency chain

`Product and data decisions -> reproducible build -> stable data model -> core workflows -> reminders and recovery -> verification -> beta -> launch`

Do not start backend sync or promise exact reminder delivery before Phase 0 has approved those requirements. Do not invite users to rely on financial records before migration and recovery behavior have been proven.

## First three concrete milestones

1. **Baseline:** repair the escaped-newline corruption, add the wrapper, and get a clean CI debug build.
2. **Product/data decision:** approve the local-first v1 scope, payment meaning, correction/audit rules, reminder defaults, and recovery promise.
3. **Core release candidate:** implement payment entry/history, audited corrections, reminder scheduling, and backup/restore against the approved schema; then test old-database migration and full user journeys.
