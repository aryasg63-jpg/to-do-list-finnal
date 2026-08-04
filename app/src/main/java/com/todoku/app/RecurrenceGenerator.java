package com.todoku.app;

import android.content.Context;

import java.util.List;

/**
 * Dipanggil setiap kali app dibuka, tiap boot, dan tiap alarm ringkasan pagi bunyi.
 * Menjamin instance Task sudah ada untuk 7 hari ke depan dari semua template aktif,
 * supaya tugas harian/mingguan otomatis muncul tanpa user harus input ulang.
 * Aman dipanggil berkali-kali — tidak akan membuat duplikat (cek existsForTemplateAndDate dulu).
 */
public class RecurrenceGenerator {

    private static final int WINDOW_DAYS = 7; // hari ini + 6 hari ke depan

    public static void ensureGenerated(Context ctx) {
        TaskDb taskDb = new TaskDb(ctx);
        RecurringTemplateDb templateDb = new RecurringTemplateDb(ctx);

        List<RecurringTemplate> templates = templateDb.getAllActive();
        long today = DateUtil.todayEpochDay();

        for (RecurringTemplate t : templates) {
            for (int i = 0; i < WINDOW_DAYS; i++) {
                long day = today + i;
                if (!t.appliesTo(day)) continue;
                if (taskDb.existsForTemplateAndDate(t.id, day)) continue;

                Task instance = new Task();
                instance.title = t.title;
                instance.category = t.category;
                instance.priority = t.priority;
                instance.startTimeMillis = DateUtil.combine(day, t.hour, t.minute);
                instance.prepMinutesBefore = t.prepMinutesBefore;
                instance.alarmEnabled = t.alarmEnabled;
                instance.prepAlarmEnabled = t.prepAlarmEnabled;
                instance.soundUri = t.soundUri;
                instance.estimatedMinutes = t.estimatedMinutes;
                instance.templateId = t.id;
                instance.instanceDateEpochDay = day;
                instance.done = false;

                long newId = taskDb.insertOrUpdate(instance);
                instance.id = newId;

                // hanya jadwalkan alarm kalau waktunya belum lewat
                if (instance.startTimeMillis > System.currentTimeMillis()) {
                    AlarmScheduler.scheduleForTask(ctx, instance);
                }
            }
        }
        TodoWidgetProvider.refreshAllWidgets(ctx);
    }

    /** Dipanggil saat template diedit/dihapus supaya perubahan langsung berlaku ke depan. */
    public static void regenerateAfterTemplateChange(Context ctx, long templateId) {
        TaskDb taskDb = new TaskDb(ctx);
        long today = DateUtil.todayEpochDay();

        for (Task old : taskDb.getFutureUndoneByTemplate(templateId, today)) {
            AlarmScheduler.cancelForTask(ctx, old.id);
        }
        taskDb.deleteFutureUndoneByTemplate(templateId, today);
        ensureGenerated(ctx);
    }

    /** Dipanggil saat template DIHAPUS TOTAL atau diubah jadi tugas sekali-jalan.
     *  Beda dengan regenerateAfterTemplateChange: di sini kita SENGAJA tidak
     *  memanggil ensureGenerated lagi, karena template ini memang tidak boleh
     *  menghasilkan instance baru lagi. Riwayat masa lalu/selesai tetap disimpan. */
    public static void regenerateAfterTemplateChangeToNone(Context ctx, long templateId) {
        TaskDb taskDb = new TaskDb(ctx);
        long today = DateUtil.todayEpochDay();

        for (Task old : taskDb.getFutureUndoneByTemplate(templateId, today)) {
            AlarmScheduler.cancelForTask(ctx, old.id);
        }
        taskDb.deleteFutureUndoneByTemplate(templateId, today);
    }
}
