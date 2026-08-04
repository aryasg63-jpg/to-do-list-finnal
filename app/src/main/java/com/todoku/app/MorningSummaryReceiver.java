package com.todoku.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

import java.util.List;

/**
 * Notifikasi "pengingat harian" yang tidak terikat ke satu tugas — ringkasan semua
 * kegiatan hari ini, dikirim di jam yang bisa diatur user (default 06:00).
 * Menjadwalkan ulang dirinya sendiri untuk besok setiap kali dia bunyi, karena
 * AlarmManager tidak punya cara "ulangi tiap hari" yang tetap presisi & tahan Doze.
 */
public class MorningSummaryReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "morning_digest_channel";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (!PrefsHelper.isDigestEnabled(ctx)) {
            AlarmScheduler.scheduleMorningDigest(ctx); // tetap jadwalkan besok, siapa tahu diaktifkan lagi
            return;
        }

        // Pastikan instance hari ini sudah ter-generate walau app belum dibuka
        RecurrenceGenerator.ensureGenerated(ctx);

        TaskDb db = new TaskDb(ctx);
        List<Task> today = db.getForDate(DateUtil.todayEpochDay());

        int total = today.size();
        int done = 0;
        Task earliest = null;
        for (Task t : today) {
            if (t.done) done++;
            if (!t.done && (earliest == null || t.startTimeMillis < earliest.startTimeMillis)) {
                earliest = t;
            }
        }

        String title, body;
        if (total == 0) {
            title = "Selamat pagi! ☀️";
            body = "Belum ada kegiatan terjadwal hari ini. Yuk tambahkan biar harimu lebih terarah.";
        } else {
            title = "Selamat pagi! Ada " + total + " kegiatan hari ini";
            StringBuilder sb = new StringBuilder();
            sb.append(done).append(" dari ").append(total).append(" sudah beres.");
            if (earliest != null) {
                sb.append(" Mulai dari: ").append(CategoryHelper.emojiFor(earliest.category))
                        .append(" ").append(earliest.title);
            }
            body = sb.toString();
        }

        createChannel(ctx);
        Intent openApp = new Intent(ctx, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notif = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_today)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(contentIntent);

        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(9999, notif.build());

        // Jadwalkan lagi untuk besok jam yang sama
        AlarmScheduler.scheduleMorningDigest(ctx);
    }

    private void createChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "Ringkasan Harian", NotificationManager.IMPORTANCE_DEFAULT);
            ch.setDescription("Notifikasi ringkasan kegiatan tiap pagi");
            NotificationManager nm = ctx.getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }
}
