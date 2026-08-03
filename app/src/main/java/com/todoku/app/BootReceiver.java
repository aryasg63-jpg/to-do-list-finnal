package com.todoku.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.List;

/**
 * AlarmManager kehilangan semua jadwal saat HP di-restart.
 * Receiver ini otomatis menjadwalkan ulang SEMUA alarm (termasuk yang berulang)
 * dari database begitu HP menyala lagi, supaya user tidak perlu buka app dulu.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        TaskDb db = new TaskDb(ctx);
        List<Task> all = db.getActiveTasks();
        for (Task t : all) {
            if (t.isRepeating()) {
                db.rollForwardRepeating(t); // bersihkan instance yang sudah lama terlewat
            }
            AlarmScheduler.scheduleForTask(ctx, t);
        }
    }
}