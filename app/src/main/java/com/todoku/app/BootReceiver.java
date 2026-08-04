package com.todoku.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import java.util.List;

/**
 * AlarmManager kehilangan semua jadwal saat HP di-restart.
 * Receiver ini otomatis: (1) generate ulang instance harian/mingguan yang mestinya sudah ada,
 * (2) jadwalkan ulang alarm semua tugas yang belum selesai, (3) jadwalkan ulang notifikasi
 * ringkasan pagi — supaya user tidak perlu buka app dulu baru semuanya aktif lagi.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        CategoryHelper.init(ctx);
        RecurrenceGenerator.ensureGenerated(ctx);

        TaskDb db = new TaskDb(ctx);
        List<Task> all = db.getAll();
        for (Task t : all) {
            if (!t.done) {
                AlarmScheduler.scheduleForTask(ctx, t);
            }
        }

        if (PrefsHelper.isDigestEnabled(ctx)) {
            AlarmScheduler.scheduleMorningDigest(ctx);
        }
    }
}
