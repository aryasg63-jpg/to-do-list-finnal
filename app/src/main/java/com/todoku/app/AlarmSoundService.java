package com.todoku.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import androidx.core.app.NotificationCompat;

/**
 * Foreground service yang memutar suara alarm.
 * Mendukung file audio CUSTOM (.mp3/.opus/.wav/.ogg apapun yang didukung Android MediaPlayer)
 * yang dipilih user sendiri lewat file picker (content:// Uri persistabel), dengan fallback
 * ke nada default sistem kalau user belum memilih file atau file sudah dihapus/tidak bisa diakses.
 */
public class AlarmSoundService extends Service {

    public static final String CHANNEL_ID = "alarm_sound_channel";
    private MediaPlayer player;
    private Vibrator vibrator;

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String title = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE);
        String soundUriStr = intent.getStringExtra(AlarmScheduler.EXTRA_SOUND_URI);
        boolean isPrep = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_PREP, false);
        boolean isCustomReminder = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_CUSTOM_REMINDER, false);
        String reminderLabel = intent.getStringExtra(AlarmScheduler.EXTRA_REMINDER_LABEL);

        String notifTitle;
        String notifBody;
        if (isCustomReminder) {
            notifTitle = (reminderLabel != null && !reminderLabel.isEmpty()) ? reminderLabel : "Pengingat!";
            notifBody = title;
        } else if (isPrep) {
            notifTitle = "Bersiap-siap!";
            notifBody = title;
        } else {
            notifTitle = "Waktunya Mulai!";
            notifBody = title;
        }

        Notification notif = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(notifTitle)
                .setContentText(notifBody)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setOngoing(true)
                .build();
        startForeground(1, notif);

        playSound(soundUriStr);
        startVibration();

        return START_NOT_STICKY;
    }

    private void playSound(String soundUriStr) {
        stopSoundOnly();
        player = new MediaPlayer();
        try {
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());

            if (soundUriStr != null && !soundUriStr.isEmpty()) {
                // Putar file custom mp3/opus yang dipilih user dari storage lokal
                Uri customUri = Uri.parse(soundUriStr);
                player.setDataSource(this, customUri);
            } else {
                // Fallback: nada alarm default sistem
                Uri defaultAlarm = android.media.RingtoneManager.getActualDefaultRingtoneUri(
                        this, android.media.RingtoneManager.TYPE_ALARM);
                if (defaultAlarm == null) {
                    defaultAlarm = android.media.RingtoneManager.getValidRingtoneUri(this);
                }
                player.setDataSource(this, defaultAlarm);
            }

            player.setLooping(true); // alarm berulang sampai user menekan "Matikan" / "Tunda"
            AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
            if (am != null) {
                am.setStreamVolume(AudioManager.STREAM_ALARM,
                        am.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0);
            }
            player.prepare();
            player.start();
        } catch (Exception e) {
            // File custom gagal dibuka (mis. dihapus/permission hilang) -> fallback nada default
            try {
                player.reset();
                Uri fallback = android.media.RingtoneManager.getValidRingtoneUri(this);
                player.setDataSource(this, fallback);
                player.setLooping(true);
                player.prepare();
                player.start();
            } catch (Exception ignored) { }
        }
    }

    private void startVibration() {
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator == null) return;
        long[] pattern = {0, 500, 300, 500, 300};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 1)); // repeat mulai index 1
        } else {
            vibrator.vibrate(pattern, 1);
        }
    }

    private void stopSoundOnly() {
        if (player != null) {
            try {
                if (player.isPlaying()) player.stop();
                player.release();
            } catch (Exception ignored) { }
            player = null;
        }
    }

    public void stopEverything() {
        stopSoundOnly();
        if (vibrator != null) vibrator.cancel();
        stopForeground(true);
        stopSelf();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "Suara Alarm Tugas", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Notifikasi alarm untuk pengingat bersiap dan waktu mulai tugas");
            ch.setBypassDnd(true);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    @Override
    public void onDestroy() {
        stopSoundOnly();
        if (vibrator != null) vibrator.cancel();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
