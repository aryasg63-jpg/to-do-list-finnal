package com.todoku.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TaskDb extends SQLiteOpenHelper {

    private static final String DB_NAME = "todoku.db";
    private static final int DB_VERSION = 2;
    private static final String TBL = "tasks";

    public TaskDb(Context ctx) {
        super(ctx, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
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
                "priority INTEGER DEFAULT 1," +
                "templateId INTEGER DEFAULT 0," +
                "instanceDateEpochDay INTEGER DEFAULT 0" +
                ")");
        db.execSQL("CREATE INDEX idx_instance_date ON " + TBL + "(instanceDateEpochDay)");
        db.execSQL("CREATE INDEX idx_template ON " + TBL + "(templateId)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        if (oldV < 2) {
            db.execSQL("ALTER TABLE " + TBL + " ADD COLUMN priority INTEGER DEFAULT 1");
            db.execSQL("ALTER TABLE " + TBL + " ADD COLUMN templateId INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE " + TBL + " ADD COLUMN instanceDateEpochDay INTEGER DEFAULT 0");
            // isi instanceDateEpochDay untuk data lama berdasarkan startTimeMillis yang sudah ada
            Cursor c = db.rawQuery("SELECT id, startTimeMillis FROM " + TBL, null);
            while (c.moveToNext()) {
                long id = c.getLong(0);
                long millis = c.getLong(1);
                long epochDay = DateUtil.epochDayOf(millis);
                db.execSQL("UPDATE " + TBL + " SET instanceDateEpochDay=? WHERE id=?",
                        new Object[]{epochDay, id});
            }
            c.close();
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_instance_date ON " + TBL + "(instanceDateEpochDay)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_template ON " + TBL + "(templateId)");
        }
    }

    public long insertOrUpdate(Task t) {
        if (t.instanceDateEpochDay == 0) {
            t.instanceDateEpochDay = DateUtil.epochDayOf(t.startTimeMillis);
        }
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
        cv.put("priority", t.priority);
        cv.put("templateId", t.templateId);
        cv.put("instanceDateEpochDay", t.instanceDateEpochDay);

        if (t.id == 0) {
            return db.insert(TBL, null, cv);
        } else {
            db.update(TBL, cv, "id=?", new String[]{String.valueOf(t.id)});
            return t.id;
        }
    }

    public void delete(long id) {
        getWritableDatabase().delete(TBL, "id=?", new String[]{String.valueOf(id)});
    }

    public void setDone(long id, boolean done) {
        ContentValues cv = new ContentValues();
        cv.put("done", done ? 1 : 0);
        getWritableDatabase().update(TBL, cv, "id=?", new String[]{String.valueOf(id)});
    }

    public Task getById(long id) {
        Cursor c = getReadableDatabase().query(TBL, null, "id=?", new String[]{String.valueOf(id)}, null, null, null);
        Task t = null;
        if (c.moveToFirst()) t = fromCursor(c);
        c.close();
        return t;
    }

    public List<Task> getAll() {
        List<Task> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query(TBL, null, null, null, null, null, "startTimeMillis ASC");
        while (c.moveToNext()) list.add(fromCursor(c));
        c.close();
        return list;
    }

    /** Tugas untuk satu tanggal spesifik — dipakai layar utama ("Hari Ini"). */
    public List<Task> getForDate(long epochDay) {
        List<Task> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query(TBL, null, "instanceDateEpochDay=?",
                new String[]{String.valueOf(epochDay)}, null, null, "startTimeMillis ASC");
        while (c.moveToNext()) list.add(fromCursor(c));
        c.close();
        return list;
    }

    /** Rentang tanggal (inklusif) — dipakai kalender mingguan. */
    public List<Task> getForDateRange(long startDay, long endDay) {
        List<Task> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query(TBL, null, "instanceDateEpochDay BETWEEN ? AND ?",
                new String[]{String.valueOf(startDay), String.valueOf(endDay)}, null, null, "startTimeMillis ASC");
        while (c.moveToNext()) list.add(fromCursor(c));
        c.close();
        return list;
    }

    public boolean existsForTemplateAndDate(long templateId, long epochDay) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT 1 FROM " + TBL + " WHERE templateId=? AND instanceDateEpochDay=? LIMIT 1",
                new String[]{String.valueOf(templateId), String.valueOf(epochDay)});
        boolean exists = c.moveToFirst();
        c.close();
        return exists;
    }

    /** Riwayat kejadian dari satu template, terbaru dulu — dipakai hitung streak. */
    public List<Task> getByTemplateDesc(long templateId, int limit) {
        List<Task> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query(TBL, null, "templateId=?",
                new String[]{String.valueOf(templateId)}, null, null,
                "instanceDateEpochDay DESC", String.valueOf(limit));
        while (c.moveToNext()) list.add(fromCursor(c));
        c.close();
        return list;
    }

    /** Dipanggil saat template diedit/dihapus: buang kejadian masa depan yang belum dikerjakan
     *  (yang sudah selesai / masa lalu tetap disimpan untuk riwayat streak & statistik). */
    public List<Task> getFutureUndoneByTemplate(long templateId, long fromEpochDayInclusive) {
        List<Task> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query(TBL, null,
                "templateId=? AND done=0 AND instanceDateEpochDay>=?",
                new String[]{String.valueOf(templateId), String.valueOf(fromEpochDayInclusive)},
                null, null, null);
        while (c.moveToNext()) list.add(fromCursor(c));
        c.close();
        return list;
    }

    public void deleteFutureUndoneByTemplate(long templateId, long fromEpochDayInclusive) {
        getWritableDatabase().delete(TBL,
                "templateId=? AND done=0 AND instanceDateEpochDay>=?",
                new String[]{String.valueOf(templateId), String.valueOf(fromEpochDayInclusive)});
    }

    public static class DayStat {
        public long epochDay;
        public int total;
        public int done;
    }

    /** Jumlah tugas & yang selesai per hari, untuk grafik statistik. */
    public List<DayStat> getDailyStats(long fromDay, long toDay) {
        Map<Long, DayStat> map = new LinkedHashMap<>();
        for (long d = fromDay; d <= toDay; d++) {
            DayStat s = new DayStat();
            s.epochDay = d;
            map.put(d, s);
        }
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT instanceDateEpochDay, done, COUNT(*) FROM " + TBL +
                        " WHERE instanceDateEpochDay BETWEEN ? AND ? GROUP BY instanceDateEpochDay, done",
                new String[]{String.valueOf(fromDay), String.valueOf(toDay)});
        while (c.moveToNext()) {
            long day = c.getLong(0);
            boolean isDone = c.getInt(1) == 1;
            int count = c.getInt(2);
            DayStat s = map.get(day);
            if (s == null) continue;
            s.total += count;
            if (isDone) s.done += count;
        }
        c.close();
        return new ArrayList<>(map.values());
    }

    public static class CategoryStat {
        public String category;
        public int total;
        public int done;
    }

    /** Jumlah tugas & yang selesai per kategori dalam rentang tanggal, untuk grafik kategori. */
    public List<CategoryStat> getCategoryStats(long fromDay, long toDay) {
        Map<String, CategoryStat> map = new LinkedHashMap<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT category, done, COUNT(*) FROM " + TBL +
                        " WHERE instanceDateEpochDay BETWEEN ? AND ? GROUP BY category, done",
                new String[]{String.valueOf(fromDay), String.valueOf(toDay)});
        while (c.moveToNext()) {
            String cat = c.getString(0);
            boolean isDone = c.getInt(1) == 1;
            int count = c.getInt(2);
            CategoryStat s = map.computeIfAbsent(cat, k -> {
                CategoryStat ns = new CategoryStat();
                ns.category = k;
                return ns;
            });
            s.total += count;
            if (isDone) s.done += count;
        }
        c.close();
        return new ArrayList<>(map.values());
    }

    private Task fromCursor(Cursor c) {
        Task t = new Task();
        t.id = c.getLong(c.getColumnIndexOrThrow("id"));
        t.title = c.getString(c.getColumnIndexOrThrow("title"));
        t.category = c.getString(c.getColumnIndexOrThrow("category"));
        t.startTimeMillis = c.getLong(c.getColumnIndexOrThrow("startTimeMillis"));
        t.prepMinutesBefore = c.getInt(c.getColumnIndexOrThrow("prepMinutesBefore"));
        t.alarmEnabled = c.getInt(c.getColumnIndexOrThrow("alarmEnabled")) == 1;
        t.prepAlarmEnabled = c.getInt(c.getColumnIndexOrThrow("prepAlarmEnabled")) == 1;
        t.soundUri = c.getString(c.getColumnIndexOrThrow("soundUri"));
        t.done = c.getInt(c.getColumnIndexOrThrow("done")) == 1;
        t.estimatedMinutes = c.getInt(c.getColumnIndexOrThrow("estimatedMinutes"));
        t.priority = c.getInt(c.getColumnIndexOrThrow("priority"));
        t.templateId = c.getLong(c.getColumnIndexOrThrow("templateId"));
        t.instanceDateEpochDay = c.getLong(c.getColumnIndexOrThrow("instanceDateEpochDay"));
        return t;
    }
}
