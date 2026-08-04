package com.todoku.app;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements TaskAdapter.Listener {

    private TaskDb db;
    private RecurringTemplateDb templateDb;
    private ReminderRuleDb reminderRuleDb;
    private TaskAdapter adapter;
    private RecyclerView rvTasks;
    private TextView tvEmptyState, tvLiveClock, tvLiveDate, tvNextTask;
    private TextView tvCountTotal, tvCountDone, tvTotalMinutes, tvProgressPercent;
    private ProgressBar progressToday;
    private LinearLayout layoutFilterChips, layoutStreakChips;
    private LinearLayout layoutStreakSection;

    private String currentFilter = "all";
    private final Handler clockHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new TaskDb(this);
        templateDb = new RecurringTemplateDb(this);
        reminderRuleDb = new ReminderRuleDb(this);

        rvTasks = findViewById(R.id.rvTasks);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvLiveClock = findViewById(R.id.tvLiveClock);
        tvLiveDate = findViewById(R.id.tvLiveDate);
        tvNextTask = findViewById(R.id.tvNextTask);
        tvCountTotal = findViewById(R.id.tvCountTotal);
        tvCountDone = findViewById(R.id.tvCountDone);
        tvTotalMinutes = findViewById(R.id.tvTotalMinutes);
        tvProgressPercent = findViewById(R.id.tvProgressPercent);
        progressToday = findViewById(R.id.progressToday);
        layoutFilterChips = findViewById(R.id.layoutFilterChips);
        layoutStreakChips = findViewById(R.id.layoutStreakChips);
        layoutStreakSection = findViewById(R.id.layoutStreakSection);

        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TaskAdapter(new ArrayList<>(), this);
        rvTasks.setAdapter(adapter);

        findViewById(R.id.fabAdd).setOnClickListener(v ->
                TaskEditDialogFragment.newInstance(0).show(getSupportFragmentManager(), "add_task"));

        findViewById(R.id.btnNavCalendar).setOnClickListener(v ->
                startActivity(new Intent(this, CalendarActivity.class)));
        findViewById(R.id.btnNavStats).setOnClickListener(v ->
                startActivity(new Intent(this, StatsActivity.class)));
        findViewById(R.id.btnNavCategories).setOnClickListener(v ->
                startActivity(new Intent(this, CategoryManageActivity.class)));
        findViewById(R.id.btnSettings).setOnClickListener(v -> openSettingsDialog());

        getSupportFragmentManager().setFragmentResultListener(
                TaskEditDialogFragment.RESULT_KEY, this, (key, bundle) -> refreshAll());

        requestRuntimePermissions();
        startLiveClock();

        RecurrenceGenerator.ensureGenerated(this);
        if (PrefsHelper.isDigestEnabled(this)) {
            AlarmScheduler.scheduleMorningDigest(this);
        }

        buildFilterChips();
        refreshAll();
    }

    @Override
    protected void onResume() {
        super.onResume();
        CategoryHelper.refresh(this); // sinkron kalau user baru saja ubah kategori
        RecurrenceGenerator.ensureGenerated(this); // jaga-jaga hari sudah berganti
        buildFilterChips();
        refreshAll();
    }

    // ---------- Jam real-time & tanggal ----------
    private void startLiveClock() {
        Runnable tick = new Runnable() {
            @Override
            public void run() {
                Calendar now = Calendar.getInstance();
                tvLiveClock.setText(new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now.getTime()));
                tvLiveDate.setText(new SimpleDateFormat("EEEE, d MMMM yyyy", new Locale("id", "ID")).format(now.getTime()));
                clockHandler.postDelayed(this, 1000);
            }
        };
        clockHandler.post(tick);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clockHandler.removeCallbacksAndMessages(null);
    }

    // ---------- Filter kategori ----------
    private void buildFilterChips() {
        layoutFilterChips.removeAllViews();
        addChip("all", "🗂️ Semua", "#6C5CE7");
        for (Category c : CategoryHelper.allCategoryObjects()) {
            addChip(c.key, c.emoji + " " + c.label, c.colorHex);
        }
    }

    private void addChip(String catKey, String label, String colorHex) {
        TextView chip = new TextView(this);
        chip.setText(label);
        chip.setTextSize(13);
        chip.setPadding(32, 18, 32, 18);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(16);
        chip.setLayoutParams(lp);
        styleChip(chip, catKey.equals(currentFilter), colorHex);
        chip.setOnClickListener(v -> {
            currentFilter = catKey;
            buildFilterChips();
            refreshAll();
        });
        layoutFilterChips.addView(chip);
    }

    private void styleChip(TextView chip, boolean active, String colorHex) {
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setCornerRadius(dp(20));
        if (active) {
            bg.setColor(android.graphics.Color.parseColor(colorHex));
            chip.setTextColor(0xFFFFFFFF);
        } else {
            bg.setColor(0xFFFFFFFF);
            bg.setStroke(dp(1), 0xFFECEBFA);
            chip.setTextColor(0xFF8B8B9E);
        }
        chip.setBackground(bg);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    // ---------- Refresh data ----------
    private void refreshAll() {
        long today = DateUtil.todayEpochDay();
        List<Task> todayTasks = db.getForDate(today);

        List<Task> filtered = new ArrayList<>();
        for (Task t : todayTasks) {
            if (currentFilter.equals("all") || t.category.equals(currentFilter)) {
                filtered.add(t);
            }
        }
        adapter.updateData(filtered);
        tvEmptyState.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        rvTasks.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);

        // Progress & ringkasan (berdasarkan SEMUA tugas hari ini, bukan hanya yang difilter)
        int total = todayTasks.size();
        int done = 0, totalMinutesLeft = 0;
        for (Task t : todayTasks) {
            if (t.done) done++; else totalMinutesLeft += t.estimatedMinutes;
        }
        int percent = total == 0 ? 0 : Math.round((done * 100f) / total);
        progressToday.setProgress(percent);
        tvProgressPercent.setText(percent + "%");
        tvCountTotal.setText(String.valueOf(total));
        tvCountDone.setText(String.valueOf(done));
        tvTotalMinutes.setText(formatMinutes(totalMinutesLeft));

        // Tugas berikutnya (boleh lintas hari, biar tetap berguna kalau hari ini sudah beres semua)
        List<Task> upcoming = db.getForDateRange(today, today + 7);
        Task nextTask = null;
        long now = System.currentTimeMillis();
        for (Task t : upcoming) {
            if (!t.done && t.startTimeMillis > now && (nextTask == null || t.startTimeMillis < nextTask.startTimeMillis)) {
                nextTask = t;
            }
        }
        if (nextTask != null) {
            String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new java.util.Date(nextTask.startTimeMillis));
            String dayLabel = DateUtil.isToday(nextTask.instanceDateEpochDay) ? "" : " (" + DateUtil.dateLabel(nextTask.instanceDateEpochDay) + ")";
            tvNextTask.setText(CategoryHelper.emojiFor(nextTask.category) + " " + nextTask.title + " — " + time + dayLabel);
        } else {
            tvNextTask.setText("Tidak ada tugas mendatang 🎉");
        }

        refreshStreaks();
    }

    private String formatMinutes(int totalMin) {
        if (totalMin < 60) return totalMin + "m";
        return (totalMin / 60) + "j " + (totalMin % 60) + "m";
    }

    private void refreshStreaks() {
        layoutStreakChips.removeAllViews();
        List<RecurringTemplate> templates = templateDb.getAllActive();
        boolean anyStreak = false;

        for (RecurringTemplate t : templates) {
            int streak = StreakHelper.computeStreak(this, t.id);
            if (streak <= 0) continue;
            anyStreak = true;

            TextView chip = new TextView(this);
            chip.setText("🔥 " + streak + " hari — " + t.title);
            chip.setTextSize(12.5f);
            chip.setPadding(28, 14, 28, 14);
            chip.setTextColor(0xFFFFFFFF);
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setCornerRadius(dp(16));
            bg.setColor(android.graphics.Color.parseColor("#FFA94D"));
            chip.setBackground(bg);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp(10));
            chip.setLayoutParams(lp);
            layoutStreakChips.addView(chip);
        }
        layoutStreakSection.setVisibility(anyStreak ? View.VISIBLE : View.GONE);
    }

    // ---------- Aksi dari list ----------
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
        refreshAll();
    }

    @Override
    public void onEdit(Task t) {
        TaskEditDialogFragment.newInstance(t.id).show(getSupportFragmentManager(), "edit_task");
    }

    @Override
    public void onDelete(Task t) {
        new AlertDialog.Builder(this)
                .setTitle("Hapus kegiatan ini?")
                .setMessage(t.title + (t.isRecurring() ? "\n\n(Hanya kejadian hari ini yang dihapus; jadwal berulangnya tetap ada. Untuk menghapus seluruh pengulangan, buka Edit.)" : ""))
                .setPositiveButton("Hapus", (d, w) -> {
                    AlarmScheduler.cancelForTask(this, t.id);
                    reminderRuleDb.deleteByTask(t.id);
                    db.delete(t.id);
                    TodoWidgetProvider.refreshAllWidgets(this);
                    refreshAll();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    // ---------- Pengaturan ----------
    private void openSettingsDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null);
        RadioGroup rgTheme = view.findViewById(R.id.rgTheme);
        Switch switchDigest = view.findViewById(R.id.switchDigest);
        Button btnDigestTime = view.findViewById(R.id.btnDigestTime);
        Button btnClose = view.findViewById(R.id.btnSettingsClose);

        String mode = PrefsHelper.getThemeMode(this);
        if (mode.equals("light")) rgTheme.check(R.id.rbThemeLight);
        else if (mode.equals("dark")) rgTheme.check(R.id.rbThemeDark);
        else rgTheme.check(R.id.rbThemeSystem);

        switchDigest.setChecked(PrefsHelper.isDigestEnabled(this));
        updateDigestTimeLabel(btnDigestTime);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Pengaturan")
                .setView(view)
                .create();

        rgTheme.setOnCheckedChangeListener((group, checkedId) -> {
            String newMode = "system";
            if (checkedId == R.id.rbThemeLight) newMode = "light";
            else if (checkedId == R.id.rbThemeDark) newMode = "dark";
            PrefsHelper.setThemeMode(this, newMode);
        });

        switchDigest.setOnCheckedChangeListener((btn, checked) -> {
            PrefsHelper.setDigestEnabled(this, checked);
            if (checked) AlarmScheduler.scheduleMorningDigest(this);
            else AlarmScheduler.cancelMorningDigest(this);
        });

        btnDigestTime.setOnClickListener(v -> {
            new TimePickerDialog(this, (tp, hour, minute) -> {
                PrefsHelper.setDigestTime(this, hour, minute);
                updateDigestTimeLabel(btnDigestTime);
                if (PrefsHelper.isDigestEnabled(this)) AlarmScheduler.scheduleMorningDigest(this);
            }, PrefsHelper.getDigestHour(this), PrefsHelper.getDigestMinute(this), true).show();
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void updateDigestTimeLabel(Button btn) {
        int h = PrefsHelper.getDigestHour(this);
        int m = PrefsHelper.getDigestMinute(this);
        btn.setText(String.format(Locale.getDefault(), "🕐 Jam %02d:%02d", h, m));
    }

    // ---------- Permission handling ----------
    private void requestRuntimePermissions() {
        List<String> needed = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (!needed.isEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toArray(new String[0]), 101);
        }
        checkExactAlarmPermission();
    }

    private void checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (am != null && !am.canScheduleExactAlarms()) {
                new AlertDialog.Builder(this)
                        .setTitle("Izinkan Alarm Presisi")
                        .setMessage("Agar semua alarm & pengingat bunyi TEPAT waktu, aktifkan izin \"Alarm & pengingat\" untuk aplikasi ini.")
                        .setPositiveButton("Buka Pengaturan", (d, w) -> {
                            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                            startActivity(intent);
                        })
                        .setNegativeButton("Nanti saja", null)
                        .show();
            }
        }
    }
}
