package com.todoku.app;

/**
 * Model satu kegiatan/tugas (satu baris = satu KEJADIAN pada satu tanggal tertentu).
 *
 * prepMinutesBefore = berapa menit SEBELUM waktu mulai, notifikasi "bersiap-siap" muncul.
 * startTimeMillis    = waktu HARUS mulai mengerjakan (alarm utama bunyi di sini).
 * soundUri           = path file .mp3/.opus lokal pilihan user (null = pakai nada default).
 * priority           = 0 Rendah, 1 Sedang, 2 Tinggi (lihat Priority.java).
 * templateId          = 0 kalau tugas sekali-jalan (manual); >0 kalau ini hasil generate
 *                        otomatis dari tugas berulang harian/mingguan (lihat RecurringTemplate).
 * instanceDateEpochDay = tanggal (hari) kejadian ini, dipakai kalender/statistik/streak
 *                        supaya konsisten walau jam-nya berubah-ubah.
 */
public class Task {
    public long id;
    public String title;
    public String category;      // key kategori (default atau custom)
    public long startTimeMillis; // epoch millis waktu mulai
    public int prepMinutesBefore; // 0 = tanpa pengingat bersiap
    public boolean alarmEnabled;
    public boolean prepAlarmEnabled;
    public String soundUri;      // content:// uri file audio custom, boleh null
    public boolean done;
    public int estimatedMinutes; // estimasi durasi pengerjaan
    public int priority;         // Priority.LOW/MEDIUM/HIGH
    public long templateId;      // 0 = bukan hasil recurring
    public long instanceDateEpochDay;

    public Task() {
        this.priority = Priority.MEDIUM;
    }

    public long getPrepTimeMillis() {
        return startTimeMillis - (prepMinutesBefore * 60_000L);
    }

    public boolean isRecurring() {
        return templateId > 0;
    }
}
