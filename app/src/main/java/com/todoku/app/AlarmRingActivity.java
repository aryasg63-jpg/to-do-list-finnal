package com.todoku.app;

import android.app.KeyguardManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Layar full-screen yang muncul di ATAS lockscreen ketika alarm berbunyi
 * (baik alarm "bersiap-siap" maupun alarm "waktu mulai").
 */
public class AlarmRingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Tampil di atas lockscreen + nyalakan layar, seperti alarm clock bawaan HP
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            if (km != null) km.requestDismissKeyguard(this, null);
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }

        setContentView(R.layout.activity_alarm_ring);

        long taskId = getIntent().getLongExtra(AlarmScheduler.EXTRA_TASK_ID, -1);
        String title = getIntent().getStringExtra(AlarmScheduler.EXTRA_TITLE);
        String category = getIntent().getStringExtra(AlarmScheduler.EXTRA_CATEGORY);
        boolean isPrep = getIntent().getBooleanExtra(AlarmScheduler.EXTRA_IS_PREP, false);
        boolean isCustomReminder = getIntent().getBooleanExtra(AlarmScheduler.EXTRA_IS_CUSTOM_REMINDER, false);
        String reminderLabel = getIntent().getStringExtra(AlarmScheduler.EXTRA_REMINDER_LABEL);

        TextView tvType = findViewById(R.id.tvAlarmType);
        TextView tvClock = findViewById(R.id.tvClock);
        TextView tvTitle = findViewById(R.id.tvTaskTitle);
        TextView tvCategory = findViewById(R.id.tvCategory);
        Button btnSnooze = findViewById(R.id.btnSnooze);
        Button btnStop = findViewById(R.id.btnStop);

        if (isCustomReminder) {
            tvType.setText((reminderLabel != null && !reminderLabel.isEmpty()) ? reminderLabel.toUpperCase() : "PENGINGAT");
        } else {
            tvType.setText(isPrep ? "SAATNYA BERSIAP-SIAP" : "WAKTUNYA MULAI SEKARANG");
        }
        tvClock.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new java.util.Date()));
        tvTitle.setText(title != null ? title : "Kegiatan");
        tvCategory.setText(CategoryHelper.emojiFor(category) + " " + CategoryHelper.labelFor(category));

        btnStop.setOnClickListener(v -> {
            stopService(new Intent(this, AlarmSoundService.class));
            finish();
        });

        btnSnooze.setOnClickListener(v -> {
            stopService(new Intent(this, AlarmSoundService.class));
            SnoozeHelper.snooze(this, taskId, title, category, isPrep, 5);
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        // Cegah alarm ditutup cuma dengan tombol back tanpa memilih aksi
    }
}
