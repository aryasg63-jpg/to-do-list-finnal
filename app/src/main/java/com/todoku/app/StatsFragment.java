package com.todoku.app;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.Calendar;
import java.util.Map;

/**
 * Statistik: streak, total selesai, minggu/bulan/tahun ini,
 * grafik batang 7 hari, dan rekap per kategori.
 */
public class StatsFragment extends Fragment {

    private TaskDb db;
    private TextView tvStreak, tvTotalCompleted, tvWeekCount, tvMonthCount, tvYearCount;
    private LinearLayout chartWeek, layoutCategoryStats;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = new TaskDb(requireContext());
        tvStreak = view.findViewById(R.id.tvStreak);
        tvTotalCompleted = view.findViewById(R.id.tvTotalCompleted);
        tvWeekCount = view.findViewById(R.id.tvWeekCount);
        tvMonthCount = view.findViewById(R.id.tvMonthCount);
        tvYearCount = view.findViewById(R.id.tvYearCount);
        chartWeek = view.findViewById(R.id.chartWeek);
        layoutCategoryStats = view.findViewById(R.id.layoutCategoryStats);
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        long now = System.currentTimeMillis();
        long todayStart = Task.startOfDay(now);
        long tomorrow = todayStart + Task.DAY_MS;

        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);

        long weekStart;
        c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        weekStart = Task.startOfDay(c.getTimeInMillis());

        long monthStart;
        Calendar mc = Calendar.getInstance();
        mc.set(Calendar.DAY_OF_MONTH, 1);
        mc.set(Calendar.HOUR_OF_DAY, 0); mc.set(Calendar.MINUTE, 0);
        mc.set(Calendar.SECOND, 0); mc.set(Calendar.MILLISECOND, 0);
        monthStart = mc.getTimeInMillis();

        long yearStart;
        Calendar yc = Calendar.getInstance();
        yc.set(Calendar.MONTH, Calendar.JANUARY);
        yc.set(Calendar.DAY_OF_MONTH, 1);
        yc.set(Calendar.HOUR_OF_DAY, 0); yc.set(Calendar.MINUTE, 0);
        yc.set(Calendar.SECOND, 0); yc.set(Calendar.MILLISECOND, 0);
        yearStart = yc.getTimeInMillis();

        tvStreak.setText(String.valueOf(db.currentStreak()));
        tvTotalCompleted.setText(String.valueOf(db.countCompletionsBetween(0, Long.MAX_VALUE)));
        tvWeekCount.setText(String.valueOf(db.countCompletionsBetween(weekStart, tomorrow)));
        tvMonthCount.setText(String.valueOf(db.countCompletionsBetween(monthStart, tomorrow)));
        tvYearCount.setText(String.valueOf(db.countCompletionsBetween(yearStart, tomorrow)));

        buildWeekChart(todayStart);
        buildCategoryStats(monthStart, tomorrow);
    }

    private void buildWeekChart(long todayStart) {
        chartWeek.removeAllViews();
        int[] days = db.completionsPerDay(todayStart + Task.DAY_MS, 7);
        String[] labels = {"Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min"};

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(todayStart);
        int todayDow = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7; // 0=Sen

        int max = 1;
        for (int v : days) max = Math.max(max, v);

        // Kolom i = Senin..Minggu minggu ini; hari ke-i punya offset (todayDow - i) hari dari hari ini.
        for (int i = 0; i < 7; i++) {
            int arrIdx = (i + 6 - todayDow) % 7; // posisi di array "7 hari terakhir"
            int value = days[arrIdx];
            int count = (arrIdx <= 6 && arrIdx >= 0) ? value : 0;

            LinearLayout col = new LinearLayout(requireContext());
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(120), 1f);
            col.setLayoutParams(lp);

            LinearLayout barWrap = new LinearLayout(requireContext());
            barWrap.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            barWrap.setLayoutParams(new LinearLayout.LayoutParams(dp(22), 0, 1f));

            TextView bar = new TextView(requireContext());
            int h = count == 0 ? dp(4) : Math.max(dp(8), dp(110) * count / max);
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(dp(22), h);
            blp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            bar.setLayoutParams(blp);
            bar.setBackgroundColor(count == 0
                    ? ContextCompat.getColor(requireContext(), R.color.divider)
                    : Color.parseColor("#6C5CE7"));
            barWrap.addView(bar);
            col.addView(barWrap);

            TextView lab = new TextView(requireContext());
            lab.setText(labels[i]);
            lab.setTextSize(10);
            lab.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            lab.setGravity(Gravity.CENTER);
            col.addView(lab, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView cntv = new TextView(requireContext());
            cntv.setText(count == 0 ? "" : String.valueOf(count));
            cntv.setTextSize(10);
            cntv.setTextColor(Color.parseColor("#6C5CE7"));
            cntv.setGravity(Gravity.CENTER);
            col.addView(cntv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            chartWeek.addView(col);
        }
    }

    private void buildCategoryStats(long from, long to) {
        layoutCategoryStats.removeAllViews();
        java.util.Map<String, Integer> completed = db.completionsByCategoryBetween(from, to);
        if (completed.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText("Belum ada penyelesaian bulan ini. Gas terus! 💪");
            empty.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            empty.setPadding(dp(4), dp(6), dp(4), dp(6));
            layoutCategoryStats.addView(empty);
            return;
        }
        for (java.util.Map.Entry<String, Integer> e : completed.entrySet()) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dp(2), dp(6), dp(2), dp(6));

            TextView head = new TextView(requireContext());
            head.setText(CategoryHelper.emojiFor(e.getKey()) + " " + CategoryHelper.labelFor(e.getKey())
                    + "  ·  " + e.getValue() + " selesai");
            head.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            head.setTextSize(13);
            row.addView(head);

            row.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_card_round));
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rlp.bottomMargin = dp(8);
            row.setLayoutParams(rlp);
            row.setPadding(dp(12), dp(8), dp(12), dp(8));
            layoutCategoryStats.addView(row);
        }
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}