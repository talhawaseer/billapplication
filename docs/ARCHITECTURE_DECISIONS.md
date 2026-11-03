# Confirmed Architecture Decisions

> The earlier sync decision below is superseded for v1 by [PRODUCT_BRIEF_V1.md](PRODUCT_BRIEF_V1.md). Keep it as historical context only.

- **V1 database architecture:** local-only Room/SQLite on one device; no account or sync.
- **V1 backup strategy:** encrypted user-managed export/restore, to be implemented before public release.
- **V1 record capture:** no bill images or postal addresses; user-entered bills and reported payment records.
- **Integration approach:** retain the existing Java/XML Android app as the baseline.

## Not yet implemented
The local-only decision matches the current Room implementation. Encrypted export/restore remains unimplemented and is a release blocker. See the product brief and release roadmap for the approved scope.
