package com.todoku.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Database SQLite: tabel `tasks` + tabel `task_history` (riwayat penyelesaian utk statistik).
 */
public class TaskDb extends SQLiteOpenHelper {

    private static final String DB_NAME = "todoku.db";
    private static final int DB_VERSION = 3;
    private static final String TBL = "tasks";
    private static final String TBL_HIST = "task_history";

    public TaskDb(Context ctx) {
        super(ctx, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTasksTable(db);
        createHistoryTable(db);
    }

    private static void createTasksTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TBL + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT NOT NULL," +
                "category TEXT NOT NULL," +
                "startTimeMillis INTEGER NOT NULL," +
                "prepMinutesBefore INTEGER DEFAULT 0," +
                "alarmEnabled INTEGER DEFAULT 1," +
                "prepAlarmEnabled INTEGER DEFAULT 1," +
                "soundUri TEXT," +
                "done INTEGER DEFAULT 0," +
                "estimatedMinutes INTEGER DEFAULT 0," +
                "note TEXT DEFAULT ''," +
                "repeatType TEXT DEFAULT 'none'," +
                "repeatInterval INTEGER DEFAULT 1," +
                "repeatWeekdays INTEGER DEFAULT 0" +
                ")");
    }

    private static void createHistoryTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TBL_HIST + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "taskId INTEGER NOT NULL," +
                "title TEXT," +
                "category TEXT," +
                "doneAtMillis INTEGER NOT NULL," +
                "instanceStartMillis INTEGER DEFAULT 0," +
                "estimatedMinutes INTEGER DEFAULT 0" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        // Perbaiki skema DB dari versi lama / DB "rusak" tanpa menghapus data user.
        // Setiap perintah dibuat idempotent + try/catch agar upgrade tidak pernah crash.
        upgradeTable(db, "ALTER TABLE " + TBL + " ADD COLUMN note TEXT DEFAULT ''");
        upgradeTable(db, "ALTER TABLE " + TBL + " ADD COLUMN repeatType TEXT DEFAULT 'none'");
        upgradeTable(db, "ALTER TABLE " + TBL + " ADD COLUMN repeatInterval INTEGER DEFAULT 1");
        upgradeTable(db, "ALTER TABLE " + TBL + " ADD COLUMN repeatWeekdays INTEGER DEFAULT 0");
        try { createHistoryTable(db); } catch (Exception ignored) { }
    }

    private static void upgradeTable(SQLiteDatabase db, String sql) {
        try { db.execSQL(sql); } catch (Exception ignored) { }
    }

    // ================= CRUD tugas =================

    public long insertOrUpdate(Task t) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("title", t.title);
        cv.put("category", t.category);
        cv.put("startTimeMillis", t.startTimeMillis);
        cv.put("prepMinutesBefore", t.prepMinutesBefore);
        cv.put("alarmEnabled", t.alarmEnabled ? 1 : 0);
        cv.put("prepAlarmEnabled", t.prepAlarmEnabled ? 1 : 0);
        cv.put("soundUri", t.soundUri);
        cv.put("done", t.done ? 1 : 0);
        cv.put("estimatedMinutes", t.estimatedMinutes);
        cv.put("note", t.note);
        cv.put("repeatType", t.repeatType);
        cv.put("repeatInterval", t.repeatInterval);
        cv.put("repeatWeekdays", t.repeatWeekdays);

        if (t.id == 0) {
            return db.insert(TBL, null, cv);
        } else {
            db.update(TBL, cv, "id=?", new String[]{String.valueOf(t.id)});
            return t.id;
        }
    }

    public void delete(long id) {
        getWritableDatabase().delete(TBL, "id=?", new String[]{String.valueOf(id)});
        getWritableDatabase().delete(TBL_HIST, "taskId=?", new String[]{String.valueOf(id)});
    }

    public void setDone(long id, boolean done) {
        ContentValues cv = new ContentValues();
        cv.put("done", done ? 1 : 0);
        getWritableDatabase().update(TBL, cv, "id=?", new String[]{String.valueOf(id)});
    }

    public Task getById(long id) {
        List<Task> list = query(null, "id=?", new String[]{String.valueOf(id)}, null, null);
        return list.isEmpty() ? null : list.get(0);
    }

    /** Semua tugas, yang belum selesai di atas (urut waktu), yang selesai di bawah. */
    public List<Task> getAll() {
        return query(null, null, null, "done ASC, startTimeMillis ASC", null);
    }

    /** Tugas yang masih aktif (belum selesai, atau berulang). */
    public List<Task> getActiveTasks() {
        return query(null, "(done=0 OR repeatType<>'none')", null, "startTimeMillis ASC", null);
    }

    /**
     * Selesaikan satu instance tugas.
     * - Non-repeat : done=true, alarm dibatalkan.
     * - Repeat     : catat riwayat lalu majukan ke kemunculan berikutnya, done tetap false.
     */
    public void complete(Task t, boolean reschedule) {
        recordCompletion(t);
        if (t.isRepeating()) {
            long next = t.nextOccurrenceFrom(t.startTimeMillis);
            if (next > t.startTimeMillis) {
                t.startTimeMillis = next;
                t.done = false;
                insertOrUpdate(t);
                if (reschedule) AlarmScheduler.scheduleForTask(getContext(), t);
            } else {
                t.done = true;
                insertOrUpdate(t);
                if (reschedule) AlarmScheduler.cancelForTask(getContext(), t.id);
            }
        } else {
            t.done = true;
            insertOrUpdate(t);
            if (reschedule) AlarmScheduler.cancelForTask(getContext(), t.id);
        }
    }

    /** Batalkan penyelesaian (undo centang) untuk tugas sekali-pakai. */
    public void uncomplete(Task t) {
        if (t.isRepeating()) return;
        t.done = false;
        insertOrUpdate(t);
        AlarmScheduler.scheduleForTask(getContext(), t);
    }

    /**
     * Majukan tugas berulang yang sudah lama terlewat ke kemunculan berikutnya,
     * agar tidak menumpuk "sudah lewat" di daftar.
     */
    public void rollForwardRepeating(Task t) {
        if (!t.isRepeating() || t.done) return;
        long now = System.currentTimeMillis();
        if (t.startTimeMillis >= now - 6 * Task.HOUR_MS) return;
        long next = t.firstOccurrenceOnOrAfter(now);
        if (next > t.startTimeMillis) {
            t.startTimeMillis = next;
            insertOrUpdate(t);
        }
    }

    /** Tugas yang muncul pada hari tertentu (untuk kalender). */
    public List<Task> getTasksForDay(long dayStartMillis) {
        List<Task> out = new ArrayList<>();
        for (Task t : getAll()) {
            if (t.occursOnDay(dayStartMillis)) out.add(t);
        }
        return out;
    }

    // ================= Riwayat & statistik =================

    private void recordCompletion(Task t) {
        ContentValues cv = new ContentValues();
        cv.put("taskId", t.id);
        cv.put("title", t.title);
        cv.put("category", t.category);
        cv.put("doneAtMillis", System.currentTimeMillis());
        cv.put("instanceStartMillis", t.startTimeMillis);
        cv.put("estimatedMinutes", t.estimatedMinutes);
        getWritableDatabase().insert(TBL_HIST, null, cv);
    }

    /** Jumlah penyelesaian dalam rentang [from, to). */
    public int countCompletionsBetween(long from, long to) {
        int n = 0;
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM " + TBL_HIST + " WHERE doneAtMillis >= ? AND doneAtMillis < ?",
                new String[]{String.valueOf(from), String.valueOf(to)});
        if (c.moveToFirst()) n = c.getInt(0);
        c.close();
        return n;
    }

    /** Penyelesaian per hari untuk `days` hari terakhir (indeks 0 = paling lama). */
    public int[] completionsPerDay(long endExclusive, int days) {
        int[] out = new int[days];
        for (int i = 0; i < days; i++) {
            long dayStart = endExclusive - (days - 1L - i) * Task.DAY_MS;
            out[i] = countCompletionsBetween(dayStart, dayStart + Task.DAY_MS);
        }
        return out;
    }

    /** Total menit estimasi yang diselesaikan dalam rentang. */
    public long totalCompletedMinutesBetween(long from, long to) {
        long n = 0;
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT SUM(estimatedMinutes) FROM " + TBL_HIST + " WHERE doneAtMillis >= ? AND doneAtMillis < ?",
                new String[]{String.valueOf(from), String.valueOf(to)});
        if (c.moveToFirst()) n = c.isNull(0) ? 0 : c.getLong(0);
        c.close();
        return n;
    }

    /** Pencacahan per kategori dalam rentang (kategori -> jumlah penyelesaian). */
    public java.util.Map<String, Integer> completionsByCategoryBetween(long from, long to) {
        java.util.Map<String, Integer> map = new java.util.LinkedHashMap<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT category, COUNT(*) FROM " + TBL_HIST + " WHERE doneAtMillis >= ? AND doneAtMillis < ? GROUP BY category",
                new String[]{String.valueOf(from), String.valueOf(to)});
        while (c.moveToNext()) {
            map.put(c.getString(0), c.getInt(1));
        }
        c.close();
        return map;
    }

    /** Rekap per kategori: total tugas (aktif) vs selesai. */
    public java.util.Map<String, int[]> categoryTotals() {
        java.util.Map<String, int[]> map = new java.util.LinkedHashMap<>();
        Cursor c = getReadableDatabase().query(TBL, new String[]{"category", "COUNT(*)", "SUM(done)"},
                null, null, "category", null, null);
        while (c.moveToNext()) {
            map.put(c.getString(0), new int[]{c.getInt(1), c.isNull(2) ? 0 : c.getInt(2)});
        }
        c.close();
        return map;
    }

    /** Hari beruntun (streak) penyelesaian sampai hari ini. */
    public int currentStreak() {
        int streak = 0;
        long day = Task.startOfDay(System.currentTimeMillis());
        for (int i = 0; i < 365; i++) {
            if (countCompletionsBetween(day, day + Task.DAY_MS) > 0) {
                streak++;
            } else if (i == 0) {
                // hari ini belum ada penyelesaian — mulai hitung dari kemarin
                i++;
            } else {
                break;
            }
            day -= Task.DAY_MS;
        }
        return streak;
    }

    // ================= Query umum =================

    private List<Task> query(String[] columns, String selection, String[] args, String orderBy, String limit) {
        List<Task> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query(TBL, columns, selection, args, null, null, orderBy, limit);
        while (c.moveToNext()) list.add(fromCursor(c));
        c.close();
        return list;
    }

    private Task fromCursor(Cursor c) {
        Task t = new Task();
        t.id = getLong(c, "id");
        t.title = getStr(c, "title");
        t.category = getStr(c, "category");
        t.startTimeMillis = getLong(c, "startTimeMillis");
        t.prepMinutesBefore = getInt(c, "prepMinutesBefore");
        t.alarmEnabled = getInt(c, "alarmEnabled") == 1;
        t.prepAlarmEnabled = getInt(c, "prepAlarmEnabled") == 1;
        t.soundUri = getStr(c, "soundUri");
        if (t.soundUri != null && t.soundUri.isEmpty()) t.soundUri = null;
        t.done = getInt(c, "done") == 1;
        t.estimatedMinutes = getInt(c, "estimatedMinutes");
        t.note = getStr(c, "note");
        t.repeatType = getStr(c, "repeatType");
        t.repeatInterval = getInt(c, "repeatInterval");
        t.repeatWeekdays = getInt(c, "repeatWeekdays");
        return t;
    }

    private static String getStr(Cursor c, String col) {
        try {
            int idx = c.getColumnIndexOrThrow(col);
            String v = c.getString(idx);
            return v == null ? "" : v;
        } catch (Exception e) {
            return "";
        }
    }

    private static int getInt(Cursor c, String col) {
        try {
            int idx = c.getColumnIndexOrThrow(col);
            return c.getInt(idx);
        } catch (Exception e) {
            return 0;
        }
    }

    private static long getLong(Cursor c, String col) {
        try {
            int idx = c.getColumnIndexOrThrow(col);
            return c.getLong(idx);
        } catch (Exception e) {
            return 0;
        }
    }
}
