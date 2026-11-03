package com.example.powerbill;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.powerbill.data.AppDatabase;
import com.example.powerbill.data.BillDao;
import com.example.powerbill.data.Consumer;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConsumerActivity extends AppCompatActivity {
    private AppDatabase db;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private EditText search;
    private CheckBox showArchived;
    private LinearLayout list;
    private TextView status;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_consumers);
        db = AppDatabase.getInstance(this);
        search = findViewById(R.id.searchConsumers);
        showArchived = findViewById(R.id.showArchived);
        list = findViewById(R.id.consumerList);
        status = findViewById(R.id.consumerStatus);
        findViewById(R.id.addConsumer).setOnClickListener(v -> showEditor(null));
        showArchived.setOnCheckedChangeListener((v, checked) -> load());
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int st,int c,int a) {}
            public void onTextChanged(CharSequence s,int st,int before,int count) { load(); }
            public void afterTextChanged(Editable e) {}
        });
        load();
    }

    private void load() {
        String q = search.getText().toString().trim();
        boolean archived = showArchived.isChecked();
        io.execute(() -> {
            List<Consumer> rows = db.billDao().searchConsumers(q, archived);
            runOnUiThread(() -> {
                list.removeAllViews();
                status.setText(rows.size() + " consumer record(s)");
                for (Consumer c : rows) addRow(c);
            });
        });
    }

    private void addRow(Consumer c) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(18, 14, 18, 14);
        card.setBackgroundColor(0xFFFFFFFF);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = 12;
        card.setLayoutParams(lp);

        TextView title = new TextView(this);
        title.setText(c.name + (c.active ? "" : "  [ARCHIVED]"));
        title.setTextSize(18); title.setTextColor(0xFF16324F); title.setTypeface(null, 1);
        card.addView(title);
        TextView detail = new TextView(this);
        detail.setText("Common ID: " + c.consumerNumber + "\nReference: " +
            safe(c.referenceNumber) +
            (c.notes == null || c.notes.trim().isEmpty() ? "" : "\nNotes: " + c.notes));
        detail.setTextColor(0xFF64748B); card.addView(detail);

        LinearLayout actions = new LinearLayout(this);
        Button edit = new Button(this); edit.setText("Edit");
        edit.setOnClickListener(v -> showEditor(c)); actions.addView(edit);
        Button archive = new Button(this); archive.setText(c.active ? "Archive" : "Restore");
        archive.setOnClickListener(v -> toggleArchive(c)); actions.addView(archive);
        Button delete = new Button(this); delete.setText("Delete");
        delete.setOnClickListener(v -> confirmDelete(c)); actions.addView(delete);
        card.addView(actions);
        list.addView(card);
    }

    private String safe(String s) { return s == null || s.trim().isEmpty() ? "-" : s; }

    private void showEditor(Consumer current) {
        android.view.View form = getLayoutInflater().inflate(R.layout.dialog_consumer, null);
        EditText id = form.findViewById(R.id.editCommonId);
        EditText name = form.findViewById(R.id.editConsumerName);
        EditText ref = form.findViewById(R.id.editReference);
        EditText notes = form.findViewById(R.id.editNotes);
        if (current != null) {
            id.setText(current.consumerNumber); name.setText(current.name);
            ref.setText(current.referenceNumber);
            notes.setText(current.notes);
            id.setEnabled(false); // changing a historical common ID should be a deliberate migration
        }
        new AlertDialog.Builder(this).setTitle(current == null ? "Add consumer" : "Edit consumer")
            .setView(form).setNegativeButton("Cancel", null)
            .setPositiveButton("Save", (d, w) -> {
                String commonId = id.getText().toString().trim();
                String consumerName = name.getText().toString().trim();
                if (commonId.isEmpty() || consumerName.isEmpty()) {
                    Toast.makeText(this, "Common ID and consumer name are required.", Toast.LENGTH_LONG).show();
                    return;
                }
                io.execute(() -> {
                    try {
                        Consumer c = current == null
                            ? new Consumer(commonId, consumerName, ref.getText().toString().trim(), "", "")
                            : current;
                        c.name = consumerName;
                        c.referenceNumber = ref.getText().toString().trim();
                        c.notes = notes.getText().toString().trim();
                        if (current == null) db.billDao().insertConsumer(c);
                        else db.billDao().updateConsumer(c);
                        runOnUiThread(() -> { Toast.makeText(this, "Consumer saved.", Toast.LENGTH_SHORT).show(); load(); });
                    } catch (Exception e) {
                        runOnUiThread(() -> Toast.makeText(this, "Could not save: common ID may already exist.", Toast.LENGTH_LONG).show());
                    }
                });
            }).show();
    }

    private void toggleArchive(Consumer c) {
        c.active = !c.active;
        io.execute(() -> {
            db.billDao().updateConsumer(c);
            runOnUiThread(this::load);
        });
    }

    private void confirmDelete(Consumer c) {
        new AlertDialog.Builder(this).setTitle("Delete consumer?")
            .setMessage("Permanent deletion is only possible if no bills reference this consumer. Archive is safer for historical records.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete", (d,w) -> io.execute(() -> {
                try {
                    db.billDao().deleteConsumer(c.id);
                    runOnUiThread(() -> { Toast.makeText(this, "Consumer deleted.", Toast.LENGTH_SHORT).show(); load(); });
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(this, "Cannot delete: historical bills reference this consumer. Archive it instead.", Toast.LENGTH_LONG).show());
                }
            })).show();
    }

    @Override protected void onDestroy() { super.onDestroy(); io.shutdown(); }
}
