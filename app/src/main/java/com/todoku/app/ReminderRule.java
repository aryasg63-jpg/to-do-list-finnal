package com.todoku.app;

/**
 * Satu ATURAN pengingat tambahan yang melekat ke sebuah Task (di luar alarm
 * bersiap-siap/waktu-mulai yang sudah ada). Contoh pemakaian:
 *  - "Ingatkan 3 hari sebelum deadline" -> daysBefore=3, repeatCount=1
 *  - "Ingatkan tiap hari mulai H-5 sampai H-1" -> daysBefore=5, repeatCount=5, repeatIntervalHours=24
 *  - "Ingatkan 2 jam sekali di H-1" -> daysBefore=1, repeatCount=6, repeatIntervalHours=2
 *
 * Setiap kali dijadwalkan, aturan ini menghasilkan `repeatCount` alarm terpisah,
 * masing-masing mundur `repeatIntervalHours` jam dari alarm sebelumnya, dimulai
 * dari (waktu mulai tugas - daysBefore hari).
 */
public class ReminderRule {
    public long id;
    public long taskId;         // FK ke Task.id (instance spesifik, bukan template)
    public int daysBefore;      // 1..10
    public int repeatCount;     // berapa kali dibunyikan, minimal 1
    public int repeatIntervalHours; // jarak antar pengulangan, dipakai kalau repeatCount > 1
    public String label;        // teks custom, mis. "Cicil belajar!" (opsional)

    public ReminderRule() {
        this.daysBefore = 1;
        this.repeatCount = 1;
        this.repeatIntervalHours = 24;
    }

    /** Hitung semua titik waktu (epoch millis) alarm ini akan bunyi, relatif ke waktu mulai tugas. */
    public long[] computeTriggerTimes(long taskStartTimeMillis) {
        long base = taskStartTimeMillis - (daysBefore * 24 * 3600_000L);
        int count = Math.max(1, repeatCount);
        long[] times = new long[count];
        for (int i = 0; i < count; i++) {
            times[i] = base - ((long) i * repeatIntervalHours * 3600_000L);
        }
        return times;
    }
}
