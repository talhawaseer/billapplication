package com.example.powerbill;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import com.example.powerbill.data.MonthlySummaryRow;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MonthlyBarChart extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<MonthlySummaryRow> rows = new ArrayList<>();
    public MonthlyBarChart(Context c) { super(c); }
    public MonthlyBarChart(Context c, AttributeSet a) { super(c,a); }
    public void setRows(List<MonthlySummaryRow> values) { rows.clear(); if(values!=null) rows.addAll(values); invalidate(); }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(rows.isEmpty()) {
            paint.setColor(0xFF64748B); paint.setTextSize(32); canvas.drawText("No monthly data yet", 20, 60, paint); return;
        }
        float left=54, top=22, bottom=getHeight()-44, right=getWidth()-12;
        paint.setColor(0xFFCBD5E1); paint.setStrokeWidth(2);
        canvas.drawLine(left,bottom,right,bottom,paint);
        long max=1;
        for(MonthlySummaryRow r:rows) max=Math.max(max,Math.max(r.payablePaisa,r.paymentPaisa));
        float groupWidth=(right-left)/rows.size();
        for(int i=0;i<rows.size();i++) {
            MonthlySummaryRow r=rows.get(i);
            float x=left+i*groupWidth+groupWidth*0.12f;
            float w=Math.max(3,groupWidth*0.30f);
            float payableH=(bottom-top)*(float)r.payablePaisa/max;
            float paymentH=(bottom-top)*(float)r.paymentPaisa/max;
            paint.setColor(0xFF2878B5); canvas.drawRect(x,bottom-payableH,x+w,bottom,paint);
            paint.setColor(0xFF16836B); canvas.drawRect(x+w+3,bottom-paymentH,x+2*w+3,bottom,paint);
            paint.setColor(0xFF475569); paint.setTextSize(Math.min(20,groupWidth*0.25f));
            String label=r.month==null?"":r.month.substring(2);
            canvas.save(); canvas.rotate(-35,x,bottom+18); canvas.drawText(label,x,bottom+18,paint); canvas.restore();
        }
        paint.setTextSize(22); paint.setColor(0xFF2878B5); canvas.drawRect(left,4,left+18,18,paint);
        paint.setColor(0xFF16324F); canvas.drawText("Payable",left+24,18,paint);
        paint.setColor(0xFF16836B); canvas.drawRect(left+120,4,left+138,18,paint);
        paint.setColor(0xFF16324F); canvas.drawText("Payments",left+144,18,paint);
    }
}
