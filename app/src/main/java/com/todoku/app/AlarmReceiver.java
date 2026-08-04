package com.todoku.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Dipicu oleh AlarmManager pada waktu yang dijadwalkan (baik alarm prep maupun start).
 * Tugasnya: nyalakan AlarmSoundService (memutar audio) dan buka AlarmRingActivity (full-screen).
 */
public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        long taskId = intent.getLongExtra(AlarmScheduler.EXTRA_TASK_ID, -1);
        String title = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE);
        String category = intent.getStringExtra(AlarmScheduler.EXTRA_CATEGORY);
        String soundUri = intent.getStringExtra(AlarmScheduler.EXTRA_SOUND_URI);
        boolean isPrep = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_PREP, false);
        boolean isCustomReminder = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_CUSTOM_REMINDER, false);
        String reminderLabel = intent.getStringExtra(AlarmScheduler.EXTRA_REMINDER_LABEL);
        int reminderIndex = intent.getIntExtra(AlarmScheduler.EXTRA_REMINDER_INDEX, 1);

        // Mulai service pemutar suara (foreground, tahan lama, custom mp3/opus)
        Intent svc = new Intent(ctx, AlarmSoundService.class);
        svc.putExtra(AlarmScheduler.EXTRA_TASK_ID, taskId);
        svc.putExtra(AlarmScheduler.EXTRA_TITLE, title);
        svc.putExtra(AlarmScheduler.EXTRA_SOUND_URI, soundUri);
        svc.putExtra(AlarmScheduler.EXTRA_IS_PREP, isPrep);
        svc.putExtra(AlarmScheduler.EXTRA_IS_CUSTOM_REMINDER, isCustomReminder);
        svc.putExtra(AlarmScheduler.EXTRA_REMINDER_LABEL, reminderLabel);
        ctx.startForegroundService(svc);

        // Buka layar alarm full-screen (muncul walau HP terkunci)
        Intent ring = new Intent(ctx, AlarmRingActivity.class);
        ring.putExtra(AlarmScheduler.EXTRA_TASK_ID, taskId);
        ring.putExtra(AlarmScheduler.EXTRA_TITLE, title);
        ring.putExtra(AlarmScheduler.EXTRA_CATEGORY, category);
        ring.putExtra(AlarmScheduler.EXTRA_IS_PREP, isPrep);
        ring.putExtra(AlarmScheduler.EXTRA_IS_CUSTOM_REMINDER, isCustomReminder);
        ring.putExtra(AlarmScheduler.EXTRA_REMINDER_LABEL, reminderLabel);
        ring.putExtra(AlarmScheduler.EXTRA_REMINDER_INDEX, reminderIndex);
        ring.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        ctx.startActivity(ring);
    }
}
