package com.todoku.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.List;

/**
 * Menjadwalkan DUA alarm per tugas:
 *  - PREP alarm  : bunyi {prepMinutesBefore} menit sebelum waktu mulai -> "Bersiap-siap!"
 *  - START alarm : bunyi TEPAT di waktu mulai -> "Waktunya mulai sekarang!"
 *
 * Pakai setExactAndAllowWhileIdle supaya tetap bunyi walau HP dalam mode Doze/hemat baterai.
 */
public class AlarmScheduler {

    public static final String EXTRA_TASK_ID = "task_id";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_CATEGORY = "category";
    public static final String EXTRA_SOUND_URI = "sound_uri";
    public static final String EXTRA_IS_PREP = "is_prep";
    public static final String EXTRA_IS_CUSTOM_REMINDER = "is_custom_reminder";
    public static final String EXTRA_REMINDER_LABEL = "reminder_label";
    public static final String EXTRA_REMINDER_INDEX = "reminder_index"; // ke berapa dari repeatCount

    private static final int REQ_CODE_START_OFFSET = 100000; // hindari bentrok id antara prep & start
    private static final int REQ_CODE_REMINDER_BASE = 500000; // ruang khusus reminder custom
    private static final int MAX_REMINDER_SLOTS = 30; // slot per task, cukup untuk 10 hari x repeat

    public static void scheduleForTask(Context ctx, Task t) {
        cancelForTask(ctx, t.id); // reset dulu biar tidak dobel saat edit

        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        long now = System.currentTimeMillis();

        // Alarm pertama: "bersiap-siap"
        if (t.prepAlarmEnabled && t.prepMinutesBefore > 0) {
            long prepTime = t.getPrepTimeMillis();
            if (prepTime > now) {
                PendingIntent pi = buildPendingIntent(ctx, t, true);
                setExact(am, prepTime, pi);
            }
        }

        // Alarm kedua: "waktu mulai"
        if (t.alarmEnabled && t.startTimeMillis > now) {
            PendingIntent pi = buildPendingIntent(ctx, t, false);
            setExact(am, t.startTimeMillis, pi);
        }

        scheduleCustomReminders(ctx, t);
    }

    /** Menjadwalkan semua aturan pengingat custom (H-1..H-10, bisa berulang) milik sebuah task. */
    public static void scheduleCustomReminders(Context ctx, Task t) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        cancelCustomReminders(ctx, t.id);

        ReminderRuleDb ruleDb = new ReminderRuleDb(ctx);
        List<ReminderRule> rules = ruleDb.getByTask(t.id);
        long now = System.currentTimeMillis();

        int slot = 0;
        for (ReminderRule rule : rules) {
            long[] triggerTimes = rule.computeTriggerTimes(t.startTimeMillis);
            for (int i = 0; i < triggerTimes.length && slot < MAX_REMINDER_SLOTS; i++, slot++) {
                if (triggerTimes[i] <= now) continue; // sudah lewat, lewati saja

                Intent intent = new Intent(ctx, AlarmReceiver.class);
                intent.putExtra(EXTRA_TASK_ID, t.id);
                intent.putExtra(EXTRA_TITLE, t.title);
                intent.putExtra(EXTRA_CATEGORY, t.category);
                intent.putExtra(EXTRA_SOUND_URI, t.soundUri);
                intent.putExtra(EXTRA_IS_PREP, false);
                intent.putExtra(EXTRA_IS_CUSTOM_REMINDER, true);
                intent.putExtra(EXTRA_REMINDER_LABEL, rule.label);
                intent.putExtra(EXTRA_REMINDER_INDEX, i + 1);

                int reqCode = REQ_CODE_REMINDER_BASE + ((int) t.id * MAX_REMINDER_SLOTS) + slot;
                PendingIntent pi = PendingIntent.getBroadcast(ctx, reqCode, intent, pendingFlags());
                setExact(am, triggerTimes[i], pi);
            }
        }
    }

    public static void cancelCustomReminders(Context ctx, long taskId) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        for (int slot = 0; slot < MAX_REMINDER_SLOTS; slot++) {
            int reqCode = REQ_CODE_REMINDER_BASE + ((int) taskId * MAX_REMINDER_SLOTS) + slot;
            PendingIntent pi = PendingIntent.getBroadcast(ctx, reqCode,
                    new Intent(ctx, AlarmReceiver.class), pendingFlags());
            am.cancel(pi);
        }
    }

    private static void setExact(AlarmManager am, long triggerAt, PendingIntent pi) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            } else {
                // fallback jika user belum kasih izin exact alarm khusus (Android 12+)
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
        cancelCustomReminders(ctx, taskId);
    }

    private static PendingIntent buildPendingIntent(Context ctx, Task t, boolean isPrep) {
        Intent intent = new Intent(ctx, AlarmReceiver.class);
        intent.putExtra(EXTRA_TASK_ID, t.id);
        intent.putExtra(EXTRA_TITLE, t.title);
        intent.putExtra(EXTRA_CATEGORY, t.category);
        intent.putExtra(EXTRA_SOUND_URI, t.soundUri);
        intent.putExtra(EXTRA_IS_PREP, isPrep);

        int reqCode = isPrep ? (int) t.id : (int) (t.id + REQ_CODE_START_OFFSET);
        return PendingIntent.getBroadcast(ctx, reqCode, intent, pendingFlags());
    }

    private static int pendingFlags() {
        return PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
    }

    // ---------- Ringkasan / pengingat pagi (satu alarm untuk seluruh app, bukan per-tugas) ----------
    private static final int DIGEST_REQ_CODE = 777777;

    public static void scheduleMorningDigest(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        int hour = PrefsHelper.getDigestHour(ctx);
        int minute = PrefsHelper.getDigestMinute(ctx);
        long today = DateUtil.todayEpochDay();
        long triggerAt = DateUtil.combine(today, hour, minute);
        if (triggerAt <= System.currentTimeMillis()) {
            triggerAt = DateUtil.combine(today + 1, hour, minute); // sudah lewat -> jadwalkan besok
        }

        PendingIntent pi = PendingIntent.getBroadcast(ctx, DIGEST_REQ_CODE,
                new Intent(ctx, MorningSummaryReceiver.class), pendingFlags());
        setExact(am, triggerAt, pi);
    }

    public static void cancelMorningDigest(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent pi = PendingIntent.getBroadcast(ctx, DIGEST_REQ_CODE,
                new Intent(ctx, MorningSummaryReceiver.class), pendingFlags());
        am.cancel(pi);
    }
}
