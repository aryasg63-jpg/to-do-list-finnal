package com.todoku.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Grafik batang sederhana buatan sendiri (murni Canvas, tanpa library eksternal)
 * supaya build tetap ringan & tidak bergantung dependency tambahan yang bisa gagal
 * di-resolve saat build. Menampilkan total tugas per hari (batang abu-abu) dengan
 * overlay porsi yang sudah selesai (batang ungu).
 */
public class SimpleBarChartView extends View {

    public static class Bar {
        public String label;
        public int total;
        public int done;

        public Bar(String label, int total, int done) {
            this.label = label;
            this.total = total;
            this.done = done;
        }
    }

    private List<Bar> bars = new ArrayList<>();
    private final Paint bgBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint doneBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public SimpleBarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        bgBarPaint.setColor(Color.parseColor("#ECEBFA"));
        bgBarPaint.setStyle(Paint.Style.FILL);

        doneBarPaint.setColor(Color.parseColor("#6C5CE7"));
        doneBarPaint.setStyle(Paint.Style.FILL);

        labelPaint.setColor(Color.parseColor("#8B8B9E"));
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTextSize(spToPx(11));

        valuePaint.setColor(Color.parseColor("#2D2D3A"));
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setTextSize(spToPx(11));
        valuePaint.setFakeBoldText(true);
    }

    public void setData(List<Bar> newBars) {
        this.bars = newBars != null ? newBars : new ArrayList<>();
        invalidate();
    }

    private float spToPx(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bars.isEmpty()) return;

        int w = getWidth();
        int h = getHeight();
        float labelSpace = dpToPx(18);
        float valueSpace = dpToPx(16);
        float chartTop = valueSpace;
        float chartBottom = h - labelSpace;
        float chartHeight = chartBottom - chartTop;

        int maxTotal = 1;
        for (Bar b : bars) maxTotal = Math.max(maxTotal, b.total);

        int count = bars.size();
        float slotWidth = (float) w / count;
        float barWidth = Math.min(dpToPx(28), slotWidth * 0.55f);
        float corner = dpToPx(6);

        for (int i = 0; i < count; i++) {
            Bar b = bars.get(i);
            float cx = slotWidth * i + slotWidth / 2f;

            float totalRatio = b.total / (float) maxTotal;
            float doneRatio = b.total == 0 ? 0 : b.done / (float) b.total;

            float bgHeight = chartHeight * totalRatio;
            float bgTop = chartBottom - bgHeight;
            RectF bgRect = new RectF(cx - barWidth / 2f, bgTop, cx + barWidth / 2f, chartBottom);
            canvas.drawRoundRect(bgRect, corner, corner, bgBarPaint);

            if (b.total > 0) {
                float doneHeight = bgHeight * doneRatio;
                float doneTop = chartBottom - doneHeight;
                RectF doneRect = new RectF(cx - barWidth / 2f, doneTop, cx + barWidth / 2f, chartBottom);
                canvas.drawRoundRect(doneRect, corner, corner, doneBarPaint);
            }

            if (b.total > 0) {
                canvas.drawText(b.done + "/" + b.total, cx, bgTop - dpToPx(4), valuePaint);
            }

            canvas.drawText(b.label, cx, h - dpToPx(4), labelPaint);
        }
    }
}
