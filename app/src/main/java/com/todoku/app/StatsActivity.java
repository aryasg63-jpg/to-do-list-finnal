package com.todoku.app;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class StatsActivity extends AppCompatActivity {

    private TaskDb db;
    private SimpleBarChartView chartView;
    private TextView tvOverallSummary, tvStatsEmpty;
    private LinearLayout layoutCategoryStats;
    private android.widget.Button btnRange7, btnRange30;

    private int rangeDays = 7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        db = new TaskDb(this);
        chartView = findViewById(R.id.chartView);
        tvOverallSummary = findViewById(R.id.tvOverallSummary);
        tvStatsEmpty = findViewById(R.id.tvStatsEmpty);
        layoutCategoryStats = findViewById(R.id.layoutCategoryStats);
        btnRange7 = findViewById(R.id.btnRange7);
        btnRange30 = findViewById(R.id.btnRange30);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnRange7.setOnClickListener(v -> { rangeDays = 7; styleRangeButtons(); loadStats(); });
        btnRange30.setOnClickListener(v -> { rangeDays = 30; styleRangeButtons(); loadStats(); });

        styleRangeButtons();
        loadStats();
    }

    private void styleRangeButtons() {
        boolean is7 = rangeDays == 7;
        btnRange7.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                Color.parseColor(is7 ? "#6C5CE7" : "#ECEBFA")));
        btnRange7.setTextColor(is7 ? Color.WHITE : Color.parseColor("#8B8B9E"));

        btnRange30.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                Color.parseColor(is7 ? "#ECEBFA" : "#6C5CE7")));
        btnRange30.setTextColor(is7 ? Color.parseColor("#8B8B9E") : Color.WHITE);
    }

    private void loadStats() {
        long today = DateUtil.todayEpochDay();
        long fromDay = today - (rangeDays - 1);

        List<TaskDb.DayStat> dailyStats = db.getDailyStats(fromDay, today);
        List<SimpleBarChartView.Bar> bars = new ArrayList<>();
        int totalAll = 0, doneAll = 0;
        for (TaskDb.DayStat s : dailyStats) {
            String label = rangeDays == 7
                    ? DateUtil.shortDayLabel(s.epochDay)
                    : String.valueOf(java.time.LocalDate.ofEpochDay(s.epochDay).getDayOfMonth());
            bars.add(new SimpleBarChartView.Bar(label, s.total, s.done));
            totalAll += s.total;
            doneAll += s.done;
        }
        chartView.setData(bars);

        int percent = totalAll == 0 ? 0 : Math.round((doneAll * 100f) / totalAll);
        tvOverallSummary.setText(doneAll + " dari " + totalAll + " tugas selesai (" + percent + "%)");

        List<TaskDb.CategoryStat> catStats = db.getCategoryStats(fromDay, today);
        buildCategoryStatsList(catStats);

        tvStatsEmpty.setVisibility(totalAll == 0 ? View.VISIBLE : View.GONE);
    }

    private void buildCategoryStatsList(List<TaskDb.CategoryStat> stats) {
        layoutCategoryStats.removeAllViews();
        // urutkan dari yang paling banyak tugasnya
        stats.sort((a, b) -> Integer.compare(b.total, a.total));

        for (TaskDb.CategoryStat s : stats) {
            if (s.total == 0) continue;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rowLp.bottomMargin = dp(12);
            row.setLayoutParams(rowLp);

            LinearLayout headerRow = new LinearLayout(this);
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            TextView tvLabel = new TextView(this);
            tvLabel.setText(CategoryHelper.emojiFor(s.category) + " " + CategoryHelper.labelFor(s.category));
            tvLabel.setTextSize(13);
            LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tvLabel.setLayoutParams(labelLp);

            int percent = Math.round((s.done * 100f) / s.total);
            TextView tvValue = new TextView(this);
            tvValue.setText(s.done + "/" + s.total + " (" + percent + "%)");
            tvValue.setTextSize(12);
            tvValue.setTextColor(Color.parseColor("#8B8B9E"));

            headerRow.addView(tvLabel);
            headerRow.addView(tvValue);
            row.addView(headerRow);

            ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            bar.setMax(100);
            bar.setProgress(percent);
            bar.setProgressDrawable(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.progress_rounded));
            LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(8));
            barLp.topMargin = dp(4);
            bar.setLayoutParams(barLp);
            row.addView(bar);

            layoutCategoryStats.addView(row);
        }
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
