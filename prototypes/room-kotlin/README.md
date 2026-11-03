# Kotlin + Room bill data-layer prototype

This is a standalone data-layer scaffold retained for reference. It is not integrated into the Java Android app under `../../android`, is not a Gradle module, and has not been compiled. Some referenced types (including `ConsumerEntity`) are not present here.

## Bill data
Each bill references `ConsumerEntity.id` through `consumerRowId`. Provider and reference number are also copied into the bill as snapshots so old bills retain the details used when they were issued even if the consumer record is edited later.

Amounts use integer paisa (PKR × 100) to avoid floating-point rounding. Dates are ISO `yyyy-MM-dd`; `billingMonth` is `yyyy-MM`. The image path should be a URI/path inside app-controlled storage, not the image bytes themselves.

## Duplicate and history rules
- A unique database index on `(consumerRowId, billingMonth)` prevents duplicate monthly bills for a consumer.
- `insert` uses `OnConflictStrategy.ABORT`, so duplicates fail instead of silently replacing data.
- Edit updates only the bill with the specified database ID; it does not delete or replace other months.
- The consumer foreign key uses `ON DELETE RESTRICT`, preserving bill history by preventing deletion of a consumer who has bills.
- The repository does not expose a general bill-history wipe operation.

If a utility issues multiple official revisions for the same month, introduce a `revisionNumber` and revise the unique key to `(consumerRowId, billingMonth, revisionNumber)`.

## Integration
1. Copy the source tree into the app and align the package name.
2. Merge Room/KSP dependencies from `gradle-dependencies.txt` into the existing Gradle file. Versions are placeholders and must match your Kotlin/AGP setup.
3. Add `ElectricityBillEntity` to your existing `@Database(entities = [...])` list. Do not replace your existing database class.
4. If your current Room database is already at version 1, increment its version and create a real migration. Do not use `fallbackToDestructiveMigration()` for user data.
5. Run the app's Gradle build and database tests; this scaffold has not been compiled against your repository.
