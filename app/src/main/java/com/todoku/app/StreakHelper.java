package com.todoku.app;

import android.content.Context;

import java.util.List;

/**
 * Menghitung streak (hari berturut-turut selesai) dari riwayat instance sebuah template.
 * Dihitung mundur dari HARI INI: kalau hari ini belum waktunya/belum dikerjakan tapi kemarin
 * berturut-turut selesai, streak tetap dihitung dari kemarin (tidak langsung putus di hari
 * yang belum berjalan).
 */
public class StreakHelper {

    public static int computeStreak(Context ctx, long templateId) {
        TaskDb db = new TaskDb(ctx);
        List<Task> history = db.getByTemplateDesc(templateId, 400); // cukup untuk >1 tahun
        if (history.isEmpty()) return 0;

        long today = DateUtil.todayEpochDay();
        int streak = 0;
        long expectedDay = today;

        // kalau instance hari ini ada tapi belum selesai, mulai hitung dari kemarin
        for (Task t : history) {
            if (t.instanceDateEpochDay == today && !t.done) {
                expectedDay = today - 1;
                break;
            }
        }

        for (Task t : history) {
            if (t.instanceDateEpochDay > expectedDay) continue;
            if (t.instanceDateEpochDay < expectedDay) break; // ada bolong -> streak putus
            if (t.done) {
                streak++;
                expectedDay--;
            } else {
                break;
            }
        }
        return streak;
    }
}
