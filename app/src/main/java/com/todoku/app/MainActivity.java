package com.todoku.app;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private FloatingActionButton fabAdd;

    private TaskListFragment taskListFragment;
    private CalendarFragment calendarFragment;
    private StatsFragment statsFragment;
    private SettingsFragment settingsFragment;

    private final ActivityResultLauncher<String[]> audioNotifLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> { });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottomNav);
        fabAdd = findViewById(R.id.fabAdd);

        taskListFragment = new TaskListFragment();
        calendarFragment = new CalendarFragment();
        statsFragment = new StatsFragment();
        settingsFragment = new SettingsFragment();

        fabAdd.setOnClickListener(v ->
                startActivity(new Intent(this, AddTaskActivity.class)));

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navToday) showTaskList();
            else if (id == R.id.navCalendar) showCalendar();
            else if (id == R.id.navStats) showStats();
            else if (id == R.id.navSettings) showSettings();
            return true;
        });
        bottomNav.setSelectedItemId(R.id.navToday);

        showTaskList();
        requestRuntimePermissions();
    }

    private void showTaskList() {
        fabAdd.show();
        switchTo(taskListFragment);
    }

    private void showCalendar() {
        fabAdd.hide();
        switchTo(calendarFragment);
    }

    private void showStats() {
        fabAdd.hide();
        switchTo(statsFragment);
    }

    private void showSettings() {
        fabAdd.hide();
        switchTo(settingsFragment);
    }

    private void switchTo(Fragment target) {
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        if (!target.isAdded()) {
            tx.add(R.id.contentContainer, target);
        }
        Fragment[] all = {taskListFragment, calendarFragment, statsFragment, settingsFragment};
        for (Fragment f : all) {
            if (f != null && f.isAdded()) {
                if (f == target) tx.show(f);
                else tx.hide(f);
            }
        }
        tx.commit();
    }

    // ---------- Permission ----------
    private void requestRuntimePermissions() {
        List<String> needed = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (!needed.isEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toArray(new String[0]), 101);
        }
        checkExactAlarmPermission();
    }

    public void checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (am != null && !am.canScheduleExactAlarms()) {
                new AlertDialog.Builder(this)
                        .setTitle("Izinkan Alarm Presisi")
                        .setMessage("Agar alarm bersiap-siap & waktu mulai bunyi TEPAT waktu, aktifkan izin \"Alarm & pengingat\" untuk aplikasi ini.")
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