# Status and limitations

Updated after implementing Phases 0 and 1. The approved v1 decisions are in [PRODUCT_BRIEF_V1.md](PRODUCT_BRIEF_V1.md); remaining delivery work is tracked in [RELEASE_ROADMAP.md](RELEASE_ROADMAP.md).

## Completed in Phases 0 and 1

- Approved a local-only, English, household FESCO v1 with manual bill/payment entry, no address/image collection, and daily 9 AM reminders until paid.
- Added Gradle 8.11.1 wrapper files and pinned AGP 8.10.1, SDK 36 (compile/target), and JDK 17.
- Repaired escaped newline corruption in the launcher manifest, dashboard layout, and `MainActivity`.
- Removed automatic demo prefill; demo values now require an explicit user action.
- Removed current UI collection of addresses and bill images; legacy Room columns remain for compatibility.
- Disabled Android automatic backup until encrypted user-managed export/restore is implemented.
- Added GitHub Actions debug build/lint workflow.

## Still open

- The publisher must provide a unique owned Android application ID and a support contact; `com.example.powerbill` remains the development ID.
- The local environment has no JDK or Android SDK. `gradlew.bat --version` could not run because Java is unavailable, and CI has not yet run. Therefore build/lint success is not confirmed.
- Payment-entry UI, reminders, bill/payment audit workflows, and encrypted export/restore remain unimplemented and block public release.
- Room schema export is disabled and migrations lack automated coverage.
- PostgreSQL drafts and Kotlin/Room scaffold remain separate, unused artifacts.
