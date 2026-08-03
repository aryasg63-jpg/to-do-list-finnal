package com.todoku.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class SnoozeHelper {
    public static void snooze(Context ctx, long taskId, String title, String category,
                               boolean isPrep, int minutes) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(ctx, AlarmReceiver.class);
        intent.putExtra(AlarmScheduler.EXTRA_TASK_ID, taskId);
        intent.putExtra(AlarmScheduler.EXTRA_TITLE, title);
        intent.putExtra(AlarmScheduler.EXTRA_CATEGORY, category);
        intent.putExtra(AlarmScheduler.EXTRA_IS_PREP, isPrep);

        int reqCode = 900000 + (int) taskId; // rentang id khusus snooze
        PendingIntent pi = PendingIntent.getBroadcast(ctx, reqCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        long triggerAt = System.currentTimeMillis() + (minutes * 60_000L);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }
}
