# PowerBill FESCO — Android application

Native Android app using Java, XML layouts, Room/SQLite, and PKR. The production flow starts with empty bill fields; demo values are loaded only when requested and require verification before saving.

## Included
- Editable FESCO sample fields: consumer ID, reference number, bill number, month, readings, due-date amount, late amount, issue date and due date.
- Live units-consumed preview.
- Confirmation checkbox before saving.
- Room entities for consumers, bills and payments.
- DAO queries for dashboard consumer/bill/paid/unpaid counts and payable/received/outstanding totals.
- Basic dashboard cards and input validation.

## Open in Android Studio
1. Install Android Studio with Android SDK 36 and JDK 17.
2. Open this `android` folder in Android Studio.
3. Allow Gradle to sync and install any prompted SDK components.
4. Review the source corruption and limitations in `../docs/PROJECT_REVIEW.md` before expecting a successful build.

The project pins Android Gradle Plugin 8.10.1, Gradle 8.11.1, and Room 2.6.1. The Gradle wrapper is checked in. CI builds and lints the debug variant with Android SDK 36.

## Optional demo values
- Consumer ID: 1131077937
- Reference: 20 13141 3173800
- Bill number: 24778
- Billing month: 2026-09
- Previous reading: 29360
- Present reading: 30009
- Units: 649
- Amount payable by due date: PKR 39,873
- Amount payable after due date: PKR 41,444
- Issue date: 2026-09-30
- Due date: 2026-10-09

Use the explicit “Load unverified demo values” button to load these values. They are unverified transcriptions; confirm against the original bill before checking the confirmation box and saving. The app does not prefill them on launch.

## Important limitations before production
This is an early prototype, not a complete utility billing system. Consumer management, bill entry/listing, and report screens are present. Payment-entry screens, due-date reminders, audited correction/reversal workflows, receipt/PDF generation, export, and backup/restore are not implemented. V1 does not collect postal addresses or new bill images; legacy nullable database columns remain for compatibility but are not written by current UI flows. The bill amount is stored as the amount payable by due date; the late amount is stored separately and must not be added to the bill total.

Room stores data locally on the device. Uninstalling the app may delete its local database. Add a tested backup/export and restore workflow before relying on it for real financial records.

## Consumer and monthly bill modules (added)
- Consumer screen supports add, edit, search, archive/restore, and attempted permanent deletion.
- Consumer common ID has a unique database index.
- Bills support billing month, bill date, due date, amount, meter readings, and notes. The v1 UI collects no images or postal addresses.
- Database unique index prevents more than one bill per consumer per billing month.
- Schema version 2 migration adds notes/image columns and the consumer/month uniqueness index without dropping existing rows.
- Historical consumers with linked bills cannot be permanently deleted; archive them instead.

## Known implementation notes
- The bill image field remains only as a legacy nullable database column; the v1 UI does not offer image selection or write new image URIs.
- The current bill screen lists saved bills and creates records. Editing/deleting bills is intentionally not provided, protecting historical records. Add a dedicated correction workflow with audit logging if needed.
- Validate migrations on a copy of any existing database before production use. The project currently has no Gradle wrapper or Room schema export.

## Payments and Room relationships (added)
- `Consumer`, `ElectricityBill`, and `Payment` are Room entities with primary keys, foreign keys, and indexes.
- `ConsumerWithBills` and `BillWithPayments` use Room `@Relation` for parent/child retrieval.
- `PaymentDao.record()` and `BillDao.recordPayment()` validate positive integer-paisa amounts, bill existence, required payment date/method/receipt, and prevent paying more than the remaining balance.
- Full payment is the payment amount equal to the remaining balance; partial payments are smaller positive amounts. The remaining balance and total payments are computed from non-voided payment records.
- Payment history queries preserve the original rows. There are deliberately no payment delete/update DAO methods. If a payment must be corrected, implement a separately audited reversal/void workflow rather than deleting it.
- Room migration 2->3 adds transaction reference, notes, created timestamp, and void fields to the existing payments table.
- Amount fields are stored as integer paisa; convert to PKR only for display (`paisa / 100.0`), not for calculations.

## Billing history, status lists, and reports (added)
- Reports screen shows consumer monthly history for the last 12 calendar months, average bill amount, monthly payable/payment/outstanding summaries, and a custom bar chart.
- Separate lists: due today, upcoming (next 7 days), overdue, paid, unpaid, and partially paid.
- Status is derived at query time from ISO date fields and the sum of non-voided payment rows; it is not manually stored, so it can change when dates or payments change.
- A consumer with no bill in a month has no row for that month in the history table; the current screen displays months that have records, not synthetic zero rows.
- The monthly report groups payments by bill month for comparisons. Actual cash collection by payment date is available via `totalPaymentsForPeriod(fromDate,toDate)` and should be used for cash-received accounting.
- Chart uses native Android Canvas drawing and has no third-party chart dependency.
