package com.todoku.app;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

/**
 * Inisialisasi channel notifikasi sekali di awal app (wajib utk Android 8+).
 */
public class App extends Application {

    public static final String CH_ALARM = "todoku_alarm";
    public static final String CH_REMINDER = "todoku_reminder";

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm == null) return;

            NotificationChannel alarm = new NotificationChannel(
                    CH_ALARM, "Suara Alarm Tugas", NotificationManager.IMPORTANCE_HIGH);
            alarm.setDescription("Notifikasi alarm bersiap-siap & waktu mulai");
            alarm.setBypassDnd(true);
            alarm.enableVibration(true);
            nm.createNotificationChannel(alarm);

            NotificationChannel reminder = new NotificationChannel(
                    CH_REMINDER, "Pengingat", NotificationManager.IMPORTANCE_DEFAULT);
            reminder.setDescription("Notifikasi pengingat kegiatan");
            nm.createNotificationChannel(reminder);
        }
    }
}