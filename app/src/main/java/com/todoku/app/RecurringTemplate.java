package com.todoku.app;

/**
 * "Cetakan" tugas berulang. RecurrenceGenerator membaca tabel ini tiap hari
 * dan otomatis membuat baris Task baru (instance) sesuai jadwalnya.
 *
 * recurrenceType : "DAILY" atau "WEEKLY"
 * recurrenceDays : bitmask hari (hanya dipakai kalau WEEKLY). bit0=Senin ... bit6=Minggu
 * hour / minute  : jam mulai tiap kejadian
 * active         : bisa di-nonaktifkan sementara tanpa menghapus (mis. lagi libur sekolah)
 */
public class RecurringTemplate {
    public long id;
    public String title;
    public String category;
    public int priority;
    public String recurrenceType; // DAILY / WEEKLY
    public int recurrenceDays;    // bitmask, dipakai jika WEEKLY
    public int hour;
    public int minute;
    public int prepMinutesBefore;
    public boolean alarmEnabled;
    public boolean prepAlarmEnabled;
    public String soundUri;
    public int estimatedMinutes;
    public boolean active;

    public RecurringTemplate() {
        this.priority = Priority.MEDIUM;
        this.active = true;
    }

    public boolean appliesTo(long epochDay) {
        if (!active) return false;
        if ("DAILY".equals(recurrenceType)) return true;
        if ("WEEKLY".equals(recurrenceType)) {
            int iso = DateUtil.isoDayOfWeek(epochDay);
            return (recurrenceDays & DateUtil.dayBit(iso)) != 0;
        }
        return false;
    }
}
