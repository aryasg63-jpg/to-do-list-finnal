package com.todoku.app;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CalendarActivity extends AppCompatActivity implements TaskAdapter.Listener {

    private TaskDb db;
    private ReminderRuleDb reminderRuleDb;
    private TaskAdapter adapter;
    private RecyclerView rvDayTasks;
    private TextView tvWeekRange, tvSelectedDateLabel, tvEmptyDay;
    private LinearLayout layoutDayTabs;

    private long weekStartEpochDay;
    private long selectedDay;
    private List<Task> weekTasksCache = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        db = new TaskDb(this);
        reminderRuleDb = new ReminderRuleDb(this);

        rvDayTasks = findViewById(R.id.rvDayTasks);
        tvWeekRange = findViewById(R.id.tvWeekRange);
        tvSelectedDateLabel = findViewById(R.id.tvSelectedDateLabel);
        tvEmptyDay = findViewById(R.id.tvEmptyDay);
        layoutDayTabs = findViewById(R.id.layoutDayTabs);

        rvDayTasks.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TaskAdapter(new ArrayList<>(), this);
        rvDayTasks.setAdapter(adapter);

        long today = DateUtil.todayEpochDay();
        weekStartEpochDay = DateUtil.startOfWeekEpochDay(today);
        selectedDay = today;

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnPrevWeek).setOnClickListener(v -> {
            weekStartEpochDay -= 7;
            selectedDay = weekStartEpochDay;
            reloadWeek();
        });
        findViewById(R.id.btnNextWeek).setOnClickListener(v -> {
            weekStartEpochDay += 7;
            selectedDay = weekStartEpochDay;
            reloadWeek();
        });

        getSupportFragmentManager().setFragmentResultListener(
                TaskEditDialogFragment.RESULT_KEY, this, (key, bundle) -> reloadWeek());

        reloadWeek();
    }

    @Override
    protected void onResume() {
        super.onResume();
        reloadWeek();
    }

    private void reloadWeek() {
        weekTasksCache = db.getForDateRange(weekStartEpochDay, weekStartEpochDay + 6);
        updateWeekRangeLabel();
        buildDayTabs();
        showTasksForSelectedDay();
    }

    private void updateWeekRangeLabel() {
        tvWeekRange.setText(DateUtil.dateLabel(weekStartEpochDay) + " - " + DateUtil.dateLabel(weekStartEpochDay + 6));
    }

    private void buildDayTabs() {
        layoutDayTabs.removeAllViews();

        Set<Long> daysWithTasks = new HashSet<>();
        for (Task t : weekTasksCache) daysWithTasks.add(t.instanceDateEpochDay);

        for (int i = 0; i < 7; i++) {
            long day = weekStartEpochDay + i;
            boolean isSelected = day == selectedDay;
            boolean isToday = DateUtil.isToday(day);
            boolean hasTasks = daysWithTasks.contains(day);

            LinearLayout tab = new LinearLayout(this);
            tab.setOrientation(LinearLayout.VERTICAL);
            tab.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tab.setLayoutParams(lp);
            tab.setPadding(dp(4), dp(8), dp(4), dp(8));

            TextView tvDow = new TextView(this);
            tvDow.setText(DateUtil.shortDayLabel(day));
            tvDow.setTextSize(10.5f);
            tvDow.setGravity(Gravity.CENTER);
            tvDow.setTextColor(isSelected ? Color.WHITE : (isToday ? Color.parseColor("#FFD43B") : 0xCCFFFFFF));
            tab.addView(tvDow);

            TextView tvNum = new TextView(this);
            tvNum.setText(String.valueOf(LocalDate.ofEpochDay(day).getDayOfMonth()));
            tvNum.setTextSize(15);
            tvNum.setTypeface(null, isSelected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            tvNum.setGravity(Gravity.CENTER);
            tvNum.setTextColor(Color.WHITE);
            tvNum.setPadding(0, dp(2), 0, dp(2));
            if (isSelected) {
                GradientDrawable bg = new GradientDrawable();
                bg.setShape(GradientDrawable.OVAL);
                bg.setColor(0x40FFFFFF);
                tvNum.setBackground(bg);
                LinearLayout.LayoutParams numLp = new LinearLayout.LayoutParams(dp(30), dp(30));
                tvNum.setLayoutParams(numLp);
            }
            tab.addView(tvNum);

            TextView tvDot = new TextView(this);
            tvDot.setText(hasTasks ? "•" : " ");
            tvDot.setTextSize(14);
            tvDot.setGravity(Gravity.CENTER);
            tvDot.setTextColor(Color.parseColor("#FFD43B"));
            tab.addView(tvDot);

            tab.setOnClickListener(v -> {
                selectedDay = day;
                buildDayTabs();
                showTasksForSelectedDay();
            });

            layoutDayTabs.addView(tab);
        }
    }

    private void showTasksForSelectedDay() {
        List<Task> dayTasks = new ArrayList<>();
        for (Task t : weekTasksCache) {
            if (t.instanceDateEpochDay == selectedDay) dayTasks.add(t);
        }
        dayTasks.sort((a, b) -> Long.compare(a.startTimeMillis, b.startTimeMillis));
        adapter.updateData(dayTasks);

        boolean isToday = DateUtil.isToday(selectedDay);
        tvSelectedDateLabel.setText((isToday ? "Hari ini · " : "") + DateUtil.dateLabel(selectedDay));

        tvEmptyDay.setVisibility(dayTasks.isEmpty() ? View.VISIBLE : View.GONE);
        rvDayTasks.setVisibility(dayTasks.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    // ---------- TaskAdapter.Listener ----------
    @Override
    public void onToggleDone(Task t, boolean done) {
        db.setDone(t.id, done);
        if (done) {
            AlarmScheduler.cancelForTask(this, t.id);
        } else {
            t.done = false;
            AlarmScheduler.scheduleForTask(this, t);
        }
        TodoWidgetProvider.refreshAllWidgets(this);
        reloadWeek();
    }

    @Override
    public void onEdit(Task t) {
        TaskEditDialogFragment.newInstance(t.id).show(getSupportFragmentManager(), "edit_task");
    }

    @Override
    public void onDelete(Task t) {
        new AlertDialog.Builder(this)
                .setTitle("Hapus kegiatan ini?")
                .setMessage(t.title)
                .setPositiveButton("Hapus", (d, w) -> {
                    AlarmScheduler.cancelForTask(this, t.id);
                    reminderRuleDb.deleteByTask(t.id);
                    db.delete(t.id);
                    TodoWidgetProvider.refreshAllWidgets(this);
                    reloadWeek();
                })
                .setNegativeButton("Batal", null)
                .show();
    }
}
