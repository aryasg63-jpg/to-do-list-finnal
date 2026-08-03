package com.todoku.app;

import java.util.Calendar;

/**
 * Model satu kegiatan/tugas + dukungan pengulangan (repeat).
 *
 * prepMinutesBefore = berapa menit SEBELUM waktu mulai, notifikasi "bersiap-siap" muncul.
 * startTimeMillis    = waktu HARUS mulai mengerjakan (alarm utama bunyi di sini).
 * soundUri           = path file .mp3/.opus lokal pilihan user (null = pakai nada default).
 * repeatType         = none | daily | weekdays | weekly | monthly | yearly | customDays | intervalDays
 * repeatInterval     = interval (hari/minggu/bulan/tahun), default 1
 * repeatWeekdays     = bitmask untuk customDays (bit 0=Senin ... bit 6=Minggu)
 * note               = catatan bebas dari user
 */
public class Task {

    public static final String REPEAT_NONE = "none";
    public static final String REPEAT_DAILY = "daily";
    public static final String REPEAT_WEEKDAYS = "weekdays";
    public static final String REPEAT_WEEKLY = "weekly";
    public static final String REPEAT_MONTHLY = "monthly";
    public static final String REPEAT_YEARLY = "yearly";
    public static final String REPEAT_CUSTOM_DAYS = "customDays";
    public static final String REPEAT_INTERVAL_DAYS = "intervalDays";

    public static final long HOUR_MS = 3600_000L;
    public static final long DAY_MS = 24 * HOUR_MS;

    public long id;
    public String title;
    public String category;      // olahraga, makanan, ..., atau kategori custom bebas
    public long startTimeMillis; // epoch millis waktu mulai (anchor pengulangan)
    public int prepMinutesBefore; // 0 = tanpa pengingat bersiap
    public boolean alarmEnabled;
    public boolean prepAlarmEnabled;
    public String soundUri;      // content:// uri file audio custom, boleh null
    public boolean done;
    public int estimatedMinutes; // estimasi durasi pengerjaan
    public String note = "";
    public String repeatType = REPEAT_NONE;
    public int repeatInterval = 1;
    public int repeatWeekdays = 0; // bit0=Sen ... bit6=Ming

    public Task() {}

    public Task(long id, String title, String category, long startTimeMillis,
                int prepMinutesBefore, boolean alarmEnabled, boolean prepAlarmEnabled,
                String soundUri, int estimatedMinutes) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.startTimeMillis = startTimeMillis;
        this.prepMinutesBefore = prepMinutesBefore;
        this.alarmEnabled = alarmEnabled;
        this.prepAlarmEnabled = prepAlarmEnabled;
        this.soundUri = soundUri;
        this.estimatedMinutes = estimatedMinutes;
        this.done = false;
    }

    public long getPrepTimeMillis() {
        return startTimeMillis - (prepMinutesBefore * 60_000L);
    }

    public boolean isRepeating() {
        return !REPEAT_NONE.equals(repeatType);
    }

    /** Konversi Calendar.DAY_OF_WEEK -> index bit (0=Senin ... 6=Minggu). */
    public static int weekdayIndex(int calDayOfWeek) {
        switch (calDayOfWeek) {
            case Calendar.MONDAY: return 0;
            case Calendar.TUESDAY: return 1;
            case Calendar.WEDNESDAY: return 2;
            case Calendar.THURSDAY: return 3;
            case Calendar.FRIDAY: return 4;
            case Calendar.SATURDAY: return 5;
            default: return 6;
        }
    }

    public static boolean weekdayBitSet(int weekdayIndex, int mask) {
        return ((mask >> weekdayIndex) & 1) == 1;
    }

    /**
     * Perhitungan kemunculan berikutnya SETELAH `from` (tetap mempertahankan jam:menit anchor).
     * Mengembalikan 0 bila bukan pengulangan.
     */
    public long nextOccurrenceFrom(long from) {
        if (!isRepeating()) return 0;
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(from);
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);
        int dayOfMonth = c.get(Calendar.DAY_OF_MONTH);
        int interval = Math.max(1, repeatInterval);

        switch (repeatType) {
            case REPEAT_DAILY:
            case REPEAT_INTERVAL_DAYS:
                c.add(Calendar.DAY_OF_YEAR, interval);
                break;
            case REPEAT_WEEKDAYS:
                do { c.add(Calendar.DAY_OF_YEAR, 1); } while (isWeekend(c));
                break;
            case REPEAT_CUSTOM_DAYS:
                c.add(Calendar.DAY_OF_YEAR, 1);
                int guard = 0;
                while (!weekdayBitSet(weekdayIndex(c.get(Calendar.DAY_OF_WEEK)), repeatWeekdays) && guard++ < 8) {
                    c.add(Calendar.DAY_OF_YEAR, 1);
                }
                break;
            case REPEAT_WEEKLY:
                c.add(Calendar.DAY_OF_YEAR, 7L * interval);
                break;
            case REPEAT_MONTHLY:
                c.add(Calendar.MONTH, interval);
                int maxDay = c.getActualMaximum(Calendar.DAY_OF_MONTH);
                if (dayOfMonth > maxDay) c.set(Calendar.DAY_OF_MONTH, maxDay);
                break;
            case REPEAT_YEARLY:
                c.add(Calendar.YEAR, interval);
                int maxDayY = c.getActualMaximum(Calendar.DAY_OF_MONTH);
                if (dayOfMonth > maxDayY) c.set(Calendar.DAY_OF_MONTH, maxDayY);
                break;
            default:
                return 0;
        }
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /** Kemunculan pertama yang &gt;= `now`. */
    public long firstOccurrenceOnOrAfter(long now) {
        long t = startTimeMillis;
        int guard = 0;
        while (t < now && guard++ < 2000) {
            long next = nextOccurrenceFrom(t);
            if (next <= t) break;
            t = next;
        }
        return t;
    }

    /** Apakah task ini terjadi pada hari yang dimulai di `dayStartMillis` (awal hari)? */
    public boolean occursOnDay(long dayStartMillis) {
        long dayEnd = dayStartMillis + DAY_MS;
        if (!isRepeating()) {
            return startTimeMillis >= dayStartMillis && startTimeMillis < dayEnd;
        }
        if (startTimeMillis >= dayEnd) return false;
        if (startTimeMillis < dayStartMillis) {
            Calendar anchor = Calendar.getInstance();
            anchor.setTimeInMillis(startTimeMillis);
            Calendar target = Calendar.getInstance();
            target.setTimeInMillis(dayStartMillis);
            long anchorDay = anchor.getTimeInMillis() - (anchor.get(Calendar.HOUR_OF_DAY) * HOUR_MS)
                    - (anchor.get(Calendar.MINUTE) * 60_000L) - (anchor.get(Calendar.SECOND) * 1000L)
                    - (anchor.get(Calendar.MILLISECOND));
            long daysDiff = (dayStartMillis - anchorDay) / DAY_MS;
            int interval = Math.max(1, repeatInterval);
            switch (repeatType) {
                case REPEAT_DAILY:
                case REPEAT_INTERVAL_DAYS:
                    return daysDiff >= 0 && (daysDiff % interval) == 0;
                case REPEAT_WEEKDAYS:
                    int dw = target.get(Calendar.DAY_OF_WEEK);
                    return dw != Calendar.SATURDAY && dw != Calendar.SUNDAY && daysDiff >= 0;
                case REPEAT_CUSTOM_DAYS:
                    return daysDiff >= 0 && weekdayBitSet(weekdayIndex(target.get(Calendar.DAY_OF_WEEK)), repeatWeekdays);
                case REPEAT_WEEKLY:
                    if (daysDiff < 0) return false;
                    return target.get(Calendar.DAY_OF_WEEK) == anchor.get(Calendar.DAY_OF_WEEK)
                            && ((daysDiff / 7) % interval) == 0;
                case REPEAT_MONTHLY:
                    if (daysDiff < 0) return false;
                    long monthDiff = (target.get(Calendar.YEAR) - anchor.get(Calendar.YEAR)) * 12L
                            + (target.get(Calendar.MONTH) - anchor.get(Calendar.MONTH));
                    return monthDiff >= 0 && (monthDiff % interval) == 0
                            && target.get(Calendar.DAY_OF_MONTH) == anchor.get(Calendar.DAY_OF_MONTH);
                case REPEAT_YEARLY:
                    if (daysDiff < 0) return false;
                    long yearDiff = target.get(Calendar.YEAR) - anchor.get(Calendar.YEAR);
                    return yearDiff >= 0 && (yearDiff % interval) == 0
                            && target.get(Calendar.MONTH) == anchor.get(Calendar.MONTH)
                            && target.get(Calendar.DAY_OF_MONTH) == anchor.get(Calendar.DAY_OF_MONTH);
                default:
                    return false;
            }
        }
        // Hari ini adalah hari instance aktif
        return true;
    }

    public static boolean isWeekend(Calendar c) {
        int d = c.get(Calendar.DAY_OF_WEEK);
        return d == Calendar.SATURDAY || d == Calendar.SUNDAY;
    }

    /** Awal hari (00:00:00.000) dari sebuah timestamp. */
    public static long startOfDay(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /** Label ringkas pola pengulangan utk badge list. */
    public static String repeatLabel(Task t) {
        switch (t.repeatType) {
            case REPEAT_DAILY: return "setiap hari";
            case REPEAT_WEEKDAYS: return "hari kerja";
            case REPEAT_WEEKLY: return "tiap " + Math.max(1, t.repeatInterval) + " minggu";
            case REPEAT_MONTHLY: return "tiap bulan";
            case REPEAT_YEARLY: return "tiap tahun";
            case REPEAT_CUSTOM_DAYS: return daysLabel(t.repeatWeekdays);
            case REPEAT_INTERVAL_DAYS: return "tiap " + Math.max(1, t.repeatInterval) + " hari";
            default: return "";
        }
    }

    /** "Sen,Kam" dari bitmask. */
    public static String daysLabel(int mask) {
        String[] names = {"Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            if (((mask >> i) & 1) == 1) {
                if (sb.length() > 0) sb.append(",");
                sb.append(names[i]);
            }
        }
        return sb.length() == 0 ? "tiap hari" : sb.toString();
    }
}
