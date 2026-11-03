package com.example.powerbill;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.powerbill.data.AppDatabase;
import com.example.powerbill.data.BillDao;
import com.example.powerbill.data.BillReportRow;
import com.example.powerbill.data.Consumer;
import com.example.powerbill.data.MonthlySummaryRow;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReportsActivity extends AppCompatActivity {
    private AppDatabase db;
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private Spinner consumerSpinner, statusSpinner;
    private final List<Consumer> consumers=new ArrayList<>();
    private TextView history, average, monthly, statusOutput, totals;
    private MonthlyBarChart chart;
    private final String[] statusLabels={"Due today","Upcoming (next 7 days)","Overdue","Paid","Unpaid","Partially paid"};
    private final String[] statusKeys={"today","upcoming","overdue","paid","unpaid","partial"};

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b); setContentView(R.layout.activity_reports);
        db=AppDatabase.getInstance(this);
        consumerSpinner=findViewById(R.id.reportConsumer); statusSpinner=findViewById(R.id.statusSelector);
        history=findViewById(R.id.consumerHistoryTable); average=findViewById(R.id.consumerAverage);
        monthly=findViewById(R.id.monthlySummaryTable); statusOutput=findViewById(R.id.statusListOutput);
        totals=findViewById(R.id.reportTotals); chart=findViewById(R.id.chartContainer);
        statusSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,statusLabels));
        findViewById(R.id.loadConsumerHistory).setOnClickListener(v -> loadConsumerHistory());
        findViewById(R.id.loadStatusList).setOnClickListener(v -> loadStatus());
        loadConsumersAndReport();
    }

    private void loadConsumersAndReport() {
        io.execute(() -> {
            List<Consumer> list=db.billDao().getActiveConsumers();
            List<MonthlySummaryRow> report=summary();
            runOnUiThread(() -> {
                consumers.clear(); consumers.addAll(list);
                List<String> labels=new ArrayList<>();
                for(Consumer c:list) labels.add(c.name+" ("+c.consumerNumber+")");
                consumerSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels));
                renderMonthly(report);
                if(!consumers.isEmpty()) loadConsumerHistory();
            });
        });
    }

    private YearMonth endMonth(){ return YearMonth.now(); }
    private YearMonth startMonth(){ return endMonth().minusMonths(11); }
    private List<MonthlySummaryRow> summary(){
        return db.billDao().monthlyReport(startMonth().toString(),endMonth().toString());
    }
    private String money(long paisa){ return String.format(Locale.US,"Rs. %,.2f",paisa/100.0); }

    private void loadConsumerHistory(){
        int pos=consumerSpinner.getSelectedItemPosition();
        if(pos<0 || pos>=consumers.size()){ toast("Add a consumer and bill first."); return; }
        Consumer c=consumers.get(pos);
        String from=startMonth().toString(), to=endMonth().toString();
        io.execute(() -> {
            List<MonthlySummaryRow> rows=db.billDao().consumerMonthlyHistory(c.id,from,to);
            double avg=db.billDao().averageMonthlyBillPaisa(c.id,from,to);
            runOnUiThread(() -> {
                StringBuilder sb=new StringBuilder("MONTH       PAYABLE        PAYMENTS       OUTSTANDING\n");
                for(MonthlySummaryRow r:rows) sb.append(String.format(Locale.US,
                    "%-10s %-14s %-14s %s\n",r.month,money(r.payablePaisa),money(r.paymentPaisa),money(r.outstandingPaisa)));
                history.setText(sb.toString());
                average.setText("Average monthly bill (months with bills): "+money(Math.round(avg)));
            });
        });
    }

    private void renderMonthly(List<MonthlySummaryRow> rows){
        StringBuilder sb=new StringBuilder("MONTH       PAYABLE        PAYMENTS       OUTSTANDING\n");
        long payable=0,payments=0,balance=0;
        for(MonthlySummaryRow r:rows){
            payable+=r.payablePaisa; payments+=r.paymentPaisa; balance+=r.outstandingPaisa;
            sb.append(String.format(Locale.US,"%-10s %-14s %-14s %s\n",r.month,money(r.payablePaisa),money(r.paymentPaisa),money(r.outstandingPaisa)));
        }
        monthly.setText(sb.toString()); chart.setRows(rows);
        totals.setText("Last 12 months totals\nPayable: "+money(payable)+"\nPayments received by bill month: "+money(payments)+"\nOutstanding: "+money(balance));
    }

    private void loadStatus(){
        int pos=statusSpinner.getSelectedItemPosition();
        if(pos<0)return;
        String key=statusKeys[pos], today=LocalDate.now().toString();
        String upTo=LocalDate.now().plusDays(7).toString();
        io.execute(() -> {
            List<BillReportRow> rows;
            BillDao dao=db.billDao();
            switch(key){
                case "today": rows=dao.billsDueToday(today); break;
                case "upcoming": rows=dao.upcomingBills(today,upTo); break;
                case "overdue": rows=dao.overdueBills(today); break;
                case "paid": rows=dao.paidBills(); break;
                case "unpaid": rows=dao.unpaidBills(); break;
                default: rows=dao.partiallyPaidBills(); break;
            }
            runOnUiThread(() -> {
                StringBuilder sb=new StringBuilder();
                if(rows.isEmpty()) sb.append("No bills in this category.");
                for(BillReportRow r:rows){
                    sb.append(r.consumerName).append(" (").append(r.commonId).append(")")
                      .append("\nBill ").append(r.bill.billNumber).append(" · ").append(r.bill.billingMonth)
                      .append("\nDue: ").append(r.bill.dueDate)
                      .append(" · Payable: ").append(money(r.bill.amountDuePaisa))
                      .append(" · Paid: ").append(money(r.paidPaisa))
                      .append(" · Balance: ").append(money(r.balancePaisa))
                      .append("\nStatus: ").append(r.status(today)).append("\n\n");
                }
                statusOutput.setText(sb.toString());
            });
        });
    }

    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    @Override protected void onDestroy(){super.onDestroy();io.shutdown();}
}
