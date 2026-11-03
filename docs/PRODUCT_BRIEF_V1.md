# PowerBill FESCO - v1 product brief

## Product

An English-language Android app for a household to track multiple FESCO electricity accounts, monthly bills, and payments. It stores data locally on one device; it does not create an account, synchronize devices, submit a payment, or verify a payment with FESCO.

## V1 user journeys

1. Add, edit, archive, or restore a household consumer/account using a name, consumer/common ID, and FESCO reference number.
2. Manually enter a monthly bill, verify its values against the original bill, and see its due date and balance.
3. Record a full or partial payment that the user says they made, including date, method, and receipt/reference where available.
4. Review due, overdue, unpaid, partially paid, and paid bills, plus payment and monthly history.
5. Receive local reminders at approximately 9:00 AM device-local time three days before a bill is due, on the due date, and daily after the due date while a balance remains. Stop reminders once fully paid. Users can turn reminders off.
6. Export and restore records using an encrypted, user-managed backup. Backup/restore is a later phase and must be complete before public release.

## Data and privacy decisions

- V1 is local-only: no account, backend, cloud sync, or telemetry containing bill/consumer content.
- Collect no postal addresses or bill photographs/images in v1. Manual entry is the supported method; OCR is out of scope.
- Keep bill and payment history until the user deletes it. Provide explicit deletion/export controls in later implementation phases.
- Payment records are user-entered claims about external payments. The app does not move money or confirm utility settlement.
- No sensitive bill amount or account/reference data should appear in lock-screen reminder text by default.
- Disable Android automatic backup until the user-managed encrypted export/restore is implemented and verified.

## Release and support

- Public display name: **PowerBill FESCO**.
- Language: English. Currency and amount presentation: PKR, calculated/stored as integer paisa.
- Distribution: Google Play, after the current target API and Play requirements are rechecked.
- Existing `com.example.powerbill` is development-only. The publisher must provide an owned, unique Android application ID before release identity is finalized; implementation must not invent one.
- Publisher must provide a support contact before store submission.

## Acceptance criteria

- A household can manage multiple FESCO accounts without entering an address or attaching a bill image.
- A user can manually add a bill and record partial/full payments; displayed outstanding balance and status are consistent.
- A bill with an outstanding balance is eligible for the selected reminders; a partial payment does not stop them, a full payment does.
- Records remain available offline and can be recovered through the promised encrypted export/restore flow before release.
- Public release cannot be configured with the development application ID or without a publisher support contact.

## Explicitly out of scope for v1

Cloud sync and accounts; web dashboard; non-FESCO utilities; bill OCR/import; bill photographs and address collection; payment processing or utility settlement verification; exact-time alarms; Urdu localization; business/customer management; and public release before backup/restore and reminder work are complete.
