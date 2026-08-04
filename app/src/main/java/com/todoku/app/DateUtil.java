package com.todoku.app;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;

/**
 * Semua perhitungan tanggal dipusatkan di sini supaya konsisten di seluruh app
 * (recurrence generator, streak, kalender, statistik semua pakai sumber yang sama).
 *
 * "epochDay" = jumlah hari sejak 1970-01-01 di zona waktu LOKAL HP (bukan UTC),
 * dipakai sebagai kunci unik untuk "hari yang mana" tanpa terpengaruh jam.
 */
public class DateUtil {

    public static ZoneId zone() {
        return ZoneId.systemDefault();
    }

    public static long todayEpochDay() {
        return LocalDate.now(zone()).toEpochDay();
    }

    public static long epochDayOf(long millis) {
        return Instant.ofEpochMilli(millis).atZone(zone()).toLocalDate().toEpochDay();
    }

    /** Gabungkan sebuah epochDay dengan jam:menit tertentu jadi epoch millis. */
    public static long combine(long epochDay, int hour, int minute) {
        LocalDate date = LocalDate.ofEpochDay(epochDay);
        ZonedDateTime zdt = date.atStartOfDay(zone()).withHour(hour).withMinute(minute).withSecond(0).withNano(0);
        return zdt.toInstant().toEpochMilli();
    }

    /** 1=Senin ... 7=Minggu (ISO-8601), dipakai untuk cocokkan bitmask mingguan. */
    public static int isoDayOfWeek(long epochDay) {
        return LocalDate.ofEpochDay(epochDay).getDayOfWeek().getValue();
    }

    /** bit 0=Senin, 1=Selasa, ... 6=Minggu */
    public static int dayBit(int isoDayOfWeek) {
        return 1 << (isoDayOfWeek - 1);
    }

    public static long startOfWeekEpochDay(long epochDay) {
        LocalDate d = LocalDate.ofEpochDay(epochDay);
        int iso = d.getDayOfWeek().getValue(); // 1=Senin
        return d.minusDays(iso - 1).toEpochDay();
    }

    public static String shortDayLabel(long epochDay) {
        DayOfWeek dow = LocalDate.ofEpochDay(epochDay).getDayOfWeek();
        String[] labels = {"Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min"};
        return labels[dow.getValue() - 1];
    }

    public static String dateLabel(long epochDay) {
        LocalDate d = LocalDate.ofEpochDay(epochDay);
        String[] bulan = {"Jan","Feb","Mar","Apr","Mei","Jun","Jul","Agu","Sep","Okt","Nov","Des"};
        return d.getDayOfMonth() + " " + bulan[d.getMonthValue() - 1];
    }

    public static boolean isToday(long epochDay) {
        return epochDay == todayEpochDay();
    }
}
