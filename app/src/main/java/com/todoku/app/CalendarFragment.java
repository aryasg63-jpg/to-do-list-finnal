package com.todoku.app;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Kalender bulanan: grid tanggal + daftar kegiatan per hari.
 * Mendukung tugas berulang (dihitung per pola repeat pada tiap tanggal).
 */
public class CalendarFragment extends Fragment {

    private TaskDb db;
    private Calendar monthStart = Calendar.getInstance();
    private long selectedDayStart = -1;

    private GridLayout gridMonth;
    private TextView tvMonthTitle, tvCalendarInfo;
    private ImageButton btnPrev, btnNext;

    private final SimpleDateFormat monthFmt = new SimpleDateFormat("MMMM yyyy", new Locale("id", "ID"));
    private LinearLayout layoutDay;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = new TaskDb(requireContext());

        gridMonth = view.findViewById(R.id.gridMonth);
        tvMonthTitle = view.findViewById(R.id.tvMonthTitle);
        tvCalendarInfo = view.findViewById(R.id.tvCalendarInfo);
        layoutDay = view.findViewById(R.id.layoutDayTasks);
        btnPrev = view.findViewById(R.id.btnPrevMonth);
        btnNext = view.findViewById(R.id.btnNextMonth);

        Calendar c = Calendar.getInstance();
        monthStart.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), 1, 0, 0, 0);

        btnPrev.setOnClickListener(v -> {
            monthStart.add(Calendar.MONTH, -1);
            render();
        });
        btnNext.setOnClickListener(v -> {
            monthStart.add(Calendar.MONTH, 1);
            render();
        });
        render();
    }

    @Override
    public void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        tvMonthTitle.setText(monthFmt.format(monthStart.getTime()));
        buildGrid();
        buildDayList();
    }

    private void buildGrid() {
        gridMonth.removeAllViews();

        int year = monthStart.get(Calendar.YEAR);
        int month = monthStart.get(Calendar.MONTH);
        int daysInMonth = monthStart.getActualMaximum(Calendar.DAY_OF_MONTH);

        Calendar first = Calendar.getInstance();
        first.set(year, month, 1, 0, 0, 0);
        // Indeks kolom (0 = Senin)
        int dow = first.get(Calendar.DAY_OF_WEEK);
        int startCol = (dow + 5) % 7; // Sen=0, Sel=1, ..., Min=6

        long todayStart = Task.startOfDay(System.currentTimeMillis());

        for (int slot = 0; slot < 42; slot++) {
            int dayNum = slot - startCol + 1;
            TextView cell = new TextView(requireContext());
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = dp(46);
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            lp.setMargins(dp(2), dp(2), dp(2), dp(2));
            cell.setLayoutParams(lp);
            cell.setGravity(Gravity.CENTER);
            cell.setTextSize(16);

            if (dayNum < 1 || dayNum > daysInMonth) {
                cell.setText("");
            } else {
                Calendar day = Calendar.getInstance();
                day.set(year, month, dayNum, 0, 0, 0);
                long dayStart = day.getTimeInMillis();
                List<Task> tasks = db.getTasksForDay(dayStart);
                boolean today = dayStart == todayStart;
                boolean selected = dayStart == selectedDayStart;

                cell.setText(String.valueOf(dayNum));
                if (selected) {
                    cell.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_cal_selected));
                    cell.setTextColor(0xFFFFFFFF);
                    cell.setTypeface(Typeface.DEFAULT_BOLD);
                } else if (today) {
                    cell.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_cal_today));
                    cell.setTextColor(0xFFFFFFFF);
                    cell.setTypeface(Typeface.DEFAULT_BOLD);
                } else {
                    cell.setTextColor(requireContext().getColor(R.color.text_primary));
                    if (!tasks.isEmpty()) {
                        cell.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_cal_has_task));
                        cell.setTextColor(requireContext().getColor(R.color.purple_primary));
                    }
                }

                final long dayStartF = dayStart;
                final int count = tasks.size();
                cell.setOnClickListener(v -> {
                    selectedDayStart = dayStartF;
                    tvCalendarInfo.setText("📅 " + new SimpleDateFormat("EEEE, d MMMM yyyy",
                            new Locale("id", "ID")).format(new java.util.Date(dayStartF))
                            + (count > 0 ? " · " + count + " kegiatan" : " · tidak ada kegiatan"));
                    buildGrid();
                    buildDayList();
                });
            }
            gridMonth.addView(cell);
        }
    }

    private void buildDayList() {
        layoutDay.removeAllViews();
        if (selectedDayStart < 0) {
            selectedDayStart = Task.startOfDay(System.currentTimeMillis());
        }
        List<Task> tasks = db.getTasksForDay(selectedDayStart);
        if (tasks.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText("Tidak ada kegiatan hari ini 🎉\nTap tanggal lain untuk melihat jadwalnya.");
            empty.setGravity(Gravity.CENTER);
            empty.setTextColor(requireContext().getColor(R.color.text_secondary));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(14);
            empty.setLayoutParams(lp);
            layoutDay.addView(empty);
            return;
        }
        for (Task t : tasks) {
            layoutDay.addView(taskCard(t));
        }
    }

    private View taskCard(Task t) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        int pad = dp(12);
        card.setPadding(pad, pad, pad, pad);
        card.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_card_round));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(6);
        card.setLayoutParams(lp);

        TextView time = new TextView(requireContext());
        time.setTextSize(15);
        time.setTypeface(Typeface.DEFAULT_BOLD);
        time.setTextColor(ContextCompat.getColor(requireContext(), R.color.purple_primary));
        time.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new java.util.Date(t.startTimeMillis)));
        card.addView(time, new LinearLayout.LayoutParams(dp(56), ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView info = new TextView(requireContext());
        info.setPadding(dp(6), 0, 0, 0);
        info.setText(t.title + (t.isRepeating() ? "  🔁" : "") + (t.done ? "  ✓" : ""));
        info.setTextColor(requireContext().getColor(R.color.text_primary));
        info.setTextSize(14);
        card.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        card.setOnClickListener(v -> {
            Intent i = new Intent(requireContext(), AddTaskActivity.class);
            i.putExtra(AddTaskActivity.EXTRA_TASK_ID, t.id);
            startActivity(i);
        });
        return card;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}