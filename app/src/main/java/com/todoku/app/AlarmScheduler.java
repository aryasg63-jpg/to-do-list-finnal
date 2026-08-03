package com.todoku.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * Menjadwalkan DUA alarm per tugas:
 *  - PREP alarm  : bunyi {prepMinutesBefore} menit sebelum waktu mulai -> "Bersiap-siap!"
 *  - START alarm : bunyi TEPAT di waktu mulai -> "Waktunya mulai sekarang!"
 *
 * Mendukung pengulangan (repeat): bila waktu mulai sudah lewat, otomatis memakai
 * kemunculan berikutnya sesuai pola repeat tanpa mengubah database.
 */
public class AlarmScheduler {

    public static final String EXTRA_TASK_ID = "task_id";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_CATEGORY = "category";
    public static final String EXTRA_SOUND_URI = "sound_uri";
    public static final String EXTRA_IS_PREP = "is_prep";
    public static final String EXTRA_IS_REPEAT = "is_repeat";
    public static final String EXTRA_NOTE = "note";

    private static final int REQ_CODE_START_OFFSET = 100000;

    public static void scheduleForTask(Context ctx, Task t) {
        cancelForTask(ctx, t.id);

        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        long now = System.currentTimeMillis();

        // Waktu mulai efektif: kemunculan pertama >= sekarang (mendukung repeat)
        long start = t.startTimeMillis;
        if (start < now) {
            if (t.isRepeating()) {
                start = t.firstOccurrenceOnOrAfter(now);
            } else if (!t.alarmEnabled && t.prepAlarmEnabled && t.prepMinutesBefore > 0) {
                // alarm start nonaktif; hanya jadwalkan "bersiap"
                long prepTime = t.getPrepTimeMillis();
                if (prepTime > now) {
                    schedulePrep(ctx, am, t, prepTime);
                }
                return;
            } else {
                return; // sudah lewat & tidak berulang
            }
        }

        // Alarm pertama: "bersiap-siap"
        if (t.prepAlarmEnabled && t.prepMinutesBefore > 0) {
            long prepTime = start - (t.prepMinutesBefore * 60_000L);
            if (prepTime > now) {
                schedulePrep(ctx, am, t, prepTime);
            }
        }

        // Alarm kedua: "waktu mulai"
        if (t.alarmEnabled && start > now) {
            PendingIntent pi = buildPendingIntent(ctx, t, false);
            setExact(am, start, pi);
        }
    }

    private static void schedulePrep(Context ctx, AlarmManager am, Task t, long prepTime) {
        PendingIntent pi = buildPendingIntent(ctx, t, true);
        setExact(am, prepTime, pi);
    }

    private static PendingIntent buildPendingIntent(Context ctx, Task t, boolean isPrep) {
        Intent intent = new Intent(ctx, AlarmReceiver.class);
        intent.putExtra(EXTRA_TASK_ID, t.id);
        intent.putExtra(EXTRA_TITLE, t.title);
        intent.putExtra(EXTRA_CATEGORY, t.category);
        intent.putExtra(EXTRA_SOUND_URI, t.soundUri);
        intent.putExtra(EXTRA_IS_PREP, isPrep);
        intent.putExtra(EXTRA_IS_REPEAT, t.isRepeating());
        intent.putExtra(EXTRA_NOTE, t.note == null ? "" : t.note);

        int reqCode = isPrep ? (int) t.id : (int) (t.id + REQ_CODE_START_OFFSET);
        return PendingIntent.getBroadcast(ctx, reqCode, intent, pendingFlags());
    }

    private static void setExact(AlarmManager am, long triggerAt, PendingIntent pi) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            }
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }

    public static void cancelForTask(Context ctx, long taskId) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        PendingIntent prepPi = PendingIntent.getBroadcast(ctx, (int) taskId,
                new Intent(ctx, AlarmReceiver.class), pendingFlags());
        PendingIntent startPi = PendingIntent.getBroadcast(ctx, (int) (taskId + REQ_CODE_START_OFFSET),
                new Intent(ctx, AlarmReceiver.class), pendingFlags());

        am.cancel(prepPi);
        am.cancel(startPi);
    }

    private static int pendingFlags() {
        return PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
    }
}