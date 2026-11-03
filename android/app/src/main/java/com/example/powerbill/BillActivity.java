package com.example.powerbill;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.powerbill.data.AppDatabase;
import com.example.powerbill.data.Consumer;
import com.example.powerbill.data.ElectricityBill;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BillActivity extends AppCompatActivity {
    private AppDatabase db;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private Spinner consumerSpinner;
    private EditText billNo, month, billDate, dueDate, amount, previous, current, notes;
    private TextView units;
    private CheckBox verified;
    private LinearLayout billList;
    private final List<Consumer> consumers = new ArrayList<>();
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b); setContentView(R.layout.activity_bills);
        db = AppDatabase.getInstance(this);
        consumerSpinner = findViewById(R.id.billConsumer);
        billNo = findViewById(R.id.billNumberInput); month = findViewById(R.id.billMonthInput);
        billDate = findViewById(R.id.billDateInput); dueDate = findViewById(R.id.billDueDateInput);
        amount = findViewById(R.id.billAmountInput); previous = findViewById(R.id.billPreviousInput);
        current = findViewById(R.id.billCurrentInput); notes = findViewById(R.id.billNotesInput);
        units = findViewById(R.id.billUnitsPreview);
        verified = findViewById(R.id.billVerified); billList = findViewById(R.id.savedBillList);
        findViewById(R.id.saveMonthlyBill).setOnClickListener(v -> saveBill());
        SimpleTextWatcher watcher = new SimpleTextWatcher(this::updateUnits);
        previous.addTextChangedListener(watcher); current.addTextChangedListener(watcher);
        loadConsumers(); loadBills();
    }

    private void loadConsumers() {
        io.execute(() -> {
            List<Consumer> rows = db.billDao().getActiveConsumers();
            runOnUiThread(() -> {
                consumers.clear(); consumers.addAll(rows);
                List<String> labels = new ArrayList<>();
                for (Consumer c : rows) labels.add(c.name + " (" + c.consumerNumber + ")");
                consumerSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels));
            });
        });
    }

    private void updateUnits() {
        try {
            long p = Long.parseLong(previous.getText().toString().trim());
            long c = Long.parseLong(current.getText().toString().trim());
            units.setText(c >= p ? "Units consumed: " + (c-p) : "Current reading must not be lower than previous reading");
        } catch (Exception e) { units.setText("Units consumed: -"); }
    }

    private void saveBill() {
        if (consumers.isEmpty()) { toast("Add an active consumer first."); return; }
        try {
            int selected = consumerSpinner.getSelectedItemPosition();
            Consumer consumer = consumers.get(selected);
            String no = text(billNo), m = text(month), bd = text(billDate), dd = text(dueDate);
            long p = Long.parseLong(text(previous)), c = Long.parseLong(text(current));
            long paisa = new BigDecimal(text(amount)).multiply(new BigDecimal("100"))
                .setScale(0, RoundingMode.HALF_UP).longValueExact();
            LocalDate.parse(bd); LocalDate.parse(dd);
            if (!m.matches("\\d{4}-\\d{2}")) throw new IllegalArgumentException("Billing month must be YYYY-MM.");
            if (LocalDate.parse(dd).isBefore(LocalDate.parse(bd))) throw new IllegalArgumentException("Due date cannot be before bill date.");
            if (c < p || p < 0 || paisa <= 0) throw new IllegalArgumentException("Check meter readings and payable amount.");
            if (no.isEmpty()) throw new IllegalArgumentException("Bill number is required.");
            if (!verified.isChecked()) throw new IllegalArgumentException("Verify the bill values before saving.");
            io.execute(() -> {
                try {
                    if (db.billDao().findBillForConsumerAndMonth(consumer.id, m) != null)
                        throw new IllegalArgumentException("A bill already exists for this consumer and month.");
                    ElectricityBill b = new ElectricityBill(consumer.id, no, m, p, c, paisa, paisa, bd, dd, true);
                    b.notes = text(notes);
                    db.billDao().insertBill(b);
                    runOnUiThread(() -> {
                        toast("Monthly bill saved.");
                        billNo.setText(""); month.setText(""); billDate.setText(""); dueDate.setText("");
                        amount.setText(""); previous.setText(""); current.setText(""); notes.setText("");
                        verified.setChecked(false);
                        updateUnits(); loadBills();
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> toast("Could not save bill: " + e.getMessage()));
                }
            });
        } catch (Exception e) { toast(e.getMessage() == null ? "Check all required fields." : e.getMessage()); }
    }

    private String text(EditText e) { return e.getText().toString().trim(); }
    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }

    private void loadBills() {
        io.execute(() -> {
            List<ElectricityBill> rows = db.billDao().getAllBills();
            runOnUiThread(() -> {
                billList.removeAllViews();
                for (ElectricityBill b : rows) {
                    TextView row = new TextView(this);
                    row.setText("Bill " + b.billNumber + " | " + b.billingMonth +
                        "\nConsumer ID: " + b.consumerId + " | Units: " + b.unitsConsumed +
                        "\nPayable: Rs. " + String.format(java.util.Locale.US, "%.2f", b.amountDuePaisa/100.0));
                    row.setTextColor(0xFF16324F); row.setPadding(14,14,14,14);
                    row.setBackgroundColor(0xFFFFFFFF);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    lp.bottomMargin = 8; row.setLayoutParams(lp); billList.addView(row);
                }
            });
        });
    }

    @Override protected void onDestroy() { super.onDestroy(); io.shutdown(); }
}
