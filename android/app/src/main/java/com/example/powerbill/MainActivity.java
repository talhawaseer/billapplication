package com.example.powerbill;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.powerbill.data.AppDatabase;
import com.example.powerbill.data.BillDao;
import com.example.powerbill.data.Consumer;
import com.example.powerbill.data.ElectricityBill;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private EditText consumerNumber, referenceNumber, billNumber, billingMonth;
    private EditText previousReading, currentReading, amountDue, lateAmount, issueDate, dueDate;
    private TextView unitsPreview, statusText;
    private CheckBox confirmed;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private AppDatabase db;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        db = AppDatabase.getInstance(getApplicationContext());
        bindViews();
        statusText.setText("Enter bill information to get started.");
        currentReading.setOnFocusChangeListener((v, hasFocus) -> updateUnits());
        previousReading.setOnFocusChangeListener((v, hasFocus) -> updateUnits());
        currentReading.addTextChangedListener(new SimpleTextWatcher(this::updateUnits));
        previousReading.addTextChangedListener(new SimpleTextWatcher(this::updateUnits));
        findViewById(R.id.loadSample).setOnClickListener(v -> loadSampleValues());
        findViewById(R.id.saveBill).setOnClickListener(v -> saveBill());
        findViewById(R.id.openConsumers).setOnClickListener(v ->
            startActivity(new android.content.Intent(this, ConsumerActivity.class)));
        findViewById(R.id.openReports).setOnClickListener(v ->
            startActivity(new android.content.Intent(this, ReportsActivity.class)));
        findViewById(R.id.openBills).setOnClickListener(v ->
            startActivity(new android.content.Intent(this, BillActivity.class)));
        refreshDashboard();
    }

    private void bindViews() {
        consumerNumber = findViewById(R.id.consumerNumber);
        referenceNumber = findViewById(R.id.referenceNumber);
        billNumber = findViewById(R.id.billNumber);
        billingMonth = findViewById(R.id.billingMonth);
        previousReading = findViewById(R.id.previousReading);
        currentReading = findViewById(R.id.currentReading);
        amountDue = findViewById(R.id.amountDue);
        lateAmount = findViewById(R.id.lateAmount);
        issueDate = findViewById(R.id.issueDate);
        dueDate = findViewById(R.id.dueDate);
        unitsPreview = findViewById(R.id.unitsPreview);
        statusText = findViewById(R.id.statusText);
        confirmed = findViewById(R.id.confirmed);
    }

    private void loadSampleValues() {
        consumerNumber.setText("1131077937");
        referenceNumber.setText("20 13141 3173800");
        billNumber.setText("24778");
        billingMonth.setText("2026-09");
        previousReading.setText("29360");
        currentReading.setText("30009");
        amountDue.setText("39873.00");
        lateAmount.setText("41444.00");
        issueDate.setText("2026-09-30");
        dueDate.setText("2026-10-09");
        confirmed.setChecked(false);
        statusText.setText("Unverified demo values loaded for review. Not saved.");
        updateUnits();
    }

    private void updateUnits() {
        try {
            long prev = Long.parseLong(previousReading.getText().toString().trim());
            long curr = Long.parseLong(currentReading.getText().toString().trim());
            if (curr < prev) {
                unitsPreview.setText("Units consumed: invalid reading");
            } else {
                unitsPreview.setText("Units consumed: " + (curr - prev));
            }
        } catch (Exception e) {
            unitsPreview.setText("Units consumed: —");
        }
    }

    private void saveBill() {
        final String consumerNo = value(consumerNumber);
        final String ref = value(referenceNumber);
        final String billNo = value(billNumber);
        final String month = value(billingMonth);
        final String issue = value(issueDate);
        final String due = value(dueDate);
        final long prev, curr, duePaisa, latePaisa;
        try {
            if (consumerNo.isEmpty() || ref.isEmpty() || billNo.isEmpty())
                throw new IllegalArgumentException("Consumer, reference and bill numbers are required.");
            prev = Long.parseLong(value(previousReading));
            curr = Long.parseLong(value(currentReading));
            if (curr < prev) throw new IllegalArgumentException("Present reading must be at least the previous reading.");
            duePaisa = toPaisa(value(amountDue));
            latePaisa = toPaisa(value(lateAmount));
            if (duePaisa < 0 || latePaisa < duePaisa)
                throw new IllegalArgumentException("Amounts are invalid. Late amount should be at least the due-date amount.");
            LocalDate.parse(issue);
            LocalDate.parse(due);
            if (!month.matches("\\d{4}-\\d{2}"))
                throw new IllegalArgumentException("Billing month must use YYYY-MM.");
            if (LocalDate.parse(due).isBefore(LocalDate.parse(issue)))
                throw new IllegalArgumentException("Due date cannot be before issue date.");
            if (!confirmed.isChecked())
                throw new IllegalArgumentException("Check the confirmation box after verifying the values against the original bill.");
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage() == null ? "Check all fields." : e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        executor.execute(() -> {
            try {
                Consumer existing = db.billDao().findConsumer(consumerNo);
                long consumerId;
                if (existing == null) {
                    Consumer c = new Consumer(consumerNo, "FESCO Consumer " + consumerNo,
                            ref, "", "");
                    consumerId = db.billDao().insertConsumer(c);
                } else {
                    consumerId = existing.id;
                }
                ElectricityBill bill = new ElectricityBill(consumerId, billNo, month,
                        prev, curr, duePaisa, latePaisa, issue, due, true);
                db.billDao().insertBill(bill);
                runOnUiThread(() -> {
                    statusText.setText("Bill saved. Consumer ID: " + consumerNo +
                        "\nUnits: " + (curr - prev) + "\nPayable: Rs. " +
                        String.format(java.util.Locale.US, "%.2f", duePaisa / 100.0));
                    refreshDashboard();
                    Toast.makeText(this, "Bill saved to Room database.", Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                    "Could not save. Check duplicate bill/consumer numbers. " + e.getMessage(),
                    Toast.LENGTH_LONG).show());
            }
        });
    }

    private long toPaisa(String input) {
        return new BigDecimal(input).multiply(new BigDecimal("100"))
                .setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private String value(EditText field) { return field.getText().toString().trim(); }

    private void refreshDashboard() {
        executor.execute(() -> {
            BillDao dao = db.billDao();
            int consumers = dao.countConsumers(), bills = dao.countBills();
            int paid = dao.countPaidBills(), unpaid = dao.countUnpaidBills();
            runOnUiThread(() -> {
                ((TextView)findViewById(R.id.totalConsumers)).setText(String.valueOf(consumers));
                ((TextView)findViewById(R.id.totalBills)).setText(String.valueOf(bills));
                ((TextView)findViewById(R.id.paidBills)).setText(String.valueOf(paid));
                ((TextView)findViewById(R.id.unpaidBills)).setText(String.valueOf(unpaid));
            });
        });
    }

    @Override protected void onResume() { super.onResume(); refreshDashboard(); }

    @Override protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
