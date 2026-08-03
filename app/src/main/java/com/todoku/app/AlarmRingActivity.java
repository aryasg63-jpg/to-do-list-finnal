package com.todoku.app;

import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Layar full-screen yang muncul di ATAS lockscreen ketika alarm berbunyi.
 * Mendukung: Tunda (durasi pilih), Matikan, dan "Selesai" untuk tugas berulang.
 */
public class AlarmRingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Tampil di atas lockscreen + nyalakan layar, seperti alarm clock bawaan
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
        String note = getIntent().getStringExtra(AlarmScheduler.EXTRA_NOTE);
        boolean isPrep = getIntent().getBooleanExtra(AlarmScheduler.EXTRA_IS_PREP, false);
        boolean isRepeat = getIntent().getBooleanExtra(AlarmScheduler.EXTRA_IS_REPEAT, false);

        TextView tvType = findViewById(R.id.tvAlarmType);
        TextView tvClock = findViewById(R.id.tvClock);
        TextView tvTitle = findViewById(R.id.tvTaskTitle);
        TextView tvCategory = findViewById(R.id.tvCategory);
        TextView tvNote = findViewById(R.id.tvNote);
        Button btnSnooze = findViewById(R.id.btnSnooze);
        Button btnStop = findViewById(R.id.btnStop);
        Button btnComplete = findViewById(R.id.btnComplete);

        tvType.setText(isPrep ? "SAATNYA BERSIAP-SIAP" : "WAKTUNYA MULAI SEKARANG");
        tvClock.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new java.util.Date()));
        tvTitle.setText(title != null ? title : "Kegiatan");
        tvCategory.setText(CategoryHelper.emojiFor(category) + " " + CategoryHelper.labelFor(category));

        if (note != null && !note.isEmpty()) {
            tvNote.setVisibility(TextView.VISIBLE);
            tvNote.setText("📝 " + note);
        }

        if (isRepeat) {
            btnComplete.setVisibility(Button.VISIBLE);
            btnComplete.setText(isPrep ? "✓ Selesai bersiap" : "✓ Selesai, lanjut berikutnya");
        }

        btnStop.setOnClickListener(v -> {
            stopService(new Intent(this, AlarmSoundService.class));
            finish();
        });

        // Tunda dengan pilihan durasi
        btnSnooze.setOnClickListener(v -> {
            String[] options = {"5 menit", "10 menit", "15 menit", "30 menit"};
            int[] minutes = {5, 10, 15, 30};
            new AlertDialog.Builder(this)
                    .setTitle("Tunda berapa lama?")
                    .setItems(options, (d, w) -> {
                        stopService(new Intent(this, AlarmSoundService.class));
                        SnoozeHelper.snooze(this, taskId, title, category, isPrep, minutes[w]);
                        finish();
                    })
                    .setNegativeButton("Batal", null)
                    .show();
        });

        // Selesaikan tugas berulang -> catat riwayat & majukan jadwal
        btnComplete.setOnClickListener(v -> {
            stopService(new Intent(this, AlarmSoundService.class));
            TaskDb db = new TaskDb(this);
            Task t = db.getById(taskId);
            if (t != null) {
                db.complete(t, true);
                Toast.makeText(this, "Selesai! Lanjut ke jadwal berikutnya 🔁", Toast.LENGTH_SHORT).show();
            }
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        // Cegah alarm ditutup cuma dengan tombol back tanpa memilih aksi
    }
}