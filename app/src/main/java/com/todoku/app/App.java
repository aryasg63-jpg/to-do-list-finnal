package com.todoku.app;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Inisialisasi channel notifikasi sekali di awal app (wajib utk Android 8+).
 */
public class App extends Application {

    public static final String CH_ALARM = "todoku_alarm";
    public static final String CH_REMINDER = "todoku_reminder";

    @Override
    public void onCreate() {
        super.onCreate();
        try {
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
        } catch (Exception e) {
            Log.e("TodoKu", "Gagal buat channel notifikasi", e);
        }

        // Catat semua crash ke logcat + file, agar mudah didiagnosis kalau app tetap force close
        final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            String trace = getStackTrace(throwable);
            Log.e("TodoKuCrash", "CRASH: " + trace);
            try {
                File dir = getExternalFilesDir(null);
                if (dir == null) dir = getFilesDir();
                File f = new File(dir, "crash_log.txt");
                try (FileWriter w = new FileWriter(f, true)) {
                    String stamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            .format(new Date());
                    w.write("\n===== " + stamp + " =====\n" + trace + "\n");
                }
                saveCrashToDownloads(trace);
            } catch (Exception ignored) { }
            if (defaultHandler != null) defaultHandler.uncaughtException(thread, throwable);
        });
    }

    private void saveCrashToDownloads(String trace) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, "todoku_crash_" + stamp + ".txt");
                values.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                        if (os != null) os.write(("===== TodoKu Crash " + stamp + " =====\n" + trace).getBytes("UTF-8"));
                    }
                }
            }
        } catch (Exception ignored) { }
    }

    private static String getStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}