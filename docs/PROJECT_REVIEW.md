# Project review and path to deployment

Reviewed on 2026-10-27. This review is based on the files present in the repository; the Android project was not built or run.

The detailed release sequence, deliverables, and exit gates are in [RELEASE_ROADMAP.md](RELEASE_ROADMAP.md).

## What is here

### Android application (`android/`)

This is the main product candidate: a Java Android app using XML layouts, AndroidX, Room 2.6.1, and SQLite. The Phase 1 baseline now pins AGP 8.10.1, Gradle 8.11.1, compile/target SDK 36, min SDK 23, and Java 17 with core library desugaring. The package/application ID remains `com.example.powerbill` pending the publisher's owned ID.

Present implementation:

- A dashboard with consumer, bill, paid-bill, and unpaid-bill counts.
- FESCO sample bill fields and an explicit user-verification checkbox.
- Consumer add/edit/search/archive/restore UI.
- Bill creation and bill list. The v1 UI does not collect bill images or postal addresses; nullable legacy database fields remain for compatibility.
- Room entities for consumers, bills, and payments; integer-paisa amounts; schema migrations through version 3.
- DAO operations for payment recording and reports/status lists.
- Monthly and consumer history reports with a custom Canvas chart.

Not implemented or not exposed in the UI:

- Payment-entry/history screen (a `PaymentDao` and payment model exist, but no UI calls them).
- Due-date reminders, scheduled notifications, notification permissions/channels, or reminder preferences.
- Bill editing/correction workflow and audited payment void/reversal UI.
- Backup/export/restore and verified recovery.
- Multi-device sync, account/authentication, or a backend.
- Receipt/PDF output and a release pipeline.

### Other artifacts

- `database/postgresql-drafts/` contains five PostgreSQL SQL scripts for a separate remote-database design. Nothing in the Android Gradle project connects to this schema. Treat the SQL as design drafts until reconciled with the Room model and a chosen backend.
- `prototypes/room-kotlin/` contains duplicated Kotlin/Room scaffold sources (the copies were byte-identical where checked), a README, and placeholder Gradle dependencies. It references a `ConsumerEntity` not included in the scaffold and is not a buildable module as checked in. It has been consolidated into one prototype location, not integrated.
- `docs/ARCHITECTURE_DECISIONS.md` records an earlier preference for a remote database with offline sync and encrypted user-controlled backups. The actual app is currently local-only Room, and neither decision is implemented. Decide whether those are still product requirements.
- Earlier status text claimed the Android project had not been supplied. That is stale now that `android/` is present.

## Technical blockers and risks

1. The known escaped-newline corruption in the launcher manifest, dashboard layout, and `MainActivity.java` has been repaired. A complete Android build is still needed to identify any remaining compilation or resource errors.
2. The Gradle 8.11.1 wrapper, AGP 8.10.1, SDK 36 configuration, and GitHub Actions build/lint workflow are now checked in, but build success is unverified because no JDK/Android SDK is installed in the current environment and CI has not run.
3. The display name is set to PowerBill FESCO and sample values no longer load automatically. The application ID remains `com.example.powerbill` until the publisher provides an owned unique ID; a support contact is also needed before store submission.
4. Room's schema export is disabled (`exportSchema = false`). Migration review and automated migration coverage will be difficult until schema snapshots and migration tests are added.
5. Android automatic backup is disabled pending the planned encrypted user-managed export/restore flow. Users must not rely on the prototype as the sole copy of important records.
6. Payment records have model/DAO-level validation, but no usable payment workflow. The SQL draft's accounting assumptions do not automatically apply to Room.
7. Reminder behavior requires product decisions: lead time, local time zone, repeat rules, snooze, paid/partially-paid handling, Android exact-alarm policy, and behavior after reboot/app update.
8. The v1 UI no longer collects postal addresses or bill images, though nullable legacy database columns remain. Android UI and repository code still need a functional pass on supported Android versions, including accessibility, large data lists, error/loading/empty states, and migration from representative old databases.

## Proposed delivery plan

### 1. Stabilize the Android baseline - implemented, pending build verification

- Fix escaped-newline corruption and any resulting manifest/layout/source errors.
- Add a Gradle wrapper and establish a reproducible Android Studio/CI build.
- Choose the final app identity, display name, supported Android versions, and FESCO-only versus multi-utility scope.
- Add Room schema export and migration tests; validate migrations against representative prior database files.
- Remove or gate prefilled sample values for production builds.

### 2. Complete the core bill and payment workflows

- Define consumer and bill lifecycle rules, including corrections and bill revisions.
- Add bill detail/edit/correction flow with an audit trail instead of unrestricted history mutation.
- Add payment entry, payment history, receipt/reference capture, partial payments, and audited reversal/void flow.
- Make dashboard and reports consistently distinguish due, overdue, partially paid, paid, and cash received by payment date.
- Add input validation, currency/date formatting, and useful loading, empty, and error states.

### 3. Implement reminders

- Agree reminder defaults and user controls (for example, configurable lead days and reminder time).
- Build local notifications with channels, runtime notification permission handling, rescheduling when a bill changes, cancellation when fully paid, and recovery after reboot/update.
- Use non-exact scheduling unless the product has a justified exact-time requirement; verify current Android platform and store policies at implementation time.
- Test due-today, upcoming, overdue, partially paid, paid, timezone/daylight-saving, and denied-permission cases.

### 4. Decide data and backup architecture

- Choose between a single-device local-first product and multi-device remote sync. The present Android Room model and PostgreSQL drafts describe different schemas; do not ship both as if already integrated.
- If local-first: implement encrypted export/restore, key recovery and loss behavior, and a tested migration/backup strategy.
- If sync is required: design authenticated API/backend, server-side validation, conflict resolution, idempotency, audit events, account deletion, and transport/storage security. Reconcile PostgreSQL schema with the app model before implementation.
- Set a privacy policy and data retention policy appropriate to customer identifiers, addresses, bill images, and payment references.

### 5. Quality, release, and deployment

- Add unit tests for currency/date validation, balance calculations, payment limits, and reminder scheduling; add Room migration/instrumentation tests and UI smoke coverage.
- Verify accessibility, offline behavior, database backup/restore, and performance on the selected minimum Android version and representative devices.
- Set up CI to build and run checks, sign release artifacts securely, and keep signing secrets outside source control.
- Prepare store listing, privacy disclosures, support/contact route, versioning, release notes, and staged internal/beta rollout.
- Monitor crashes and user feedback, maintain backup/restore support, and document a release rollback and data recovery process.

## Suggested next milestone

Make the project compile reproducibly and repair the newline corruption, then agree on local-only versus synchronized data and the first supported reminder behavior. Those decisions determine the product architecture and should precede feature expansion or public deployment.
