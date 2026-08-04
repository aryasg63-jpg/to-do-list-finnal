package com.todoku.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class ReminderRuleDb extends SQLiteOpenHelper {

    private static final String DB_NAME = "todoku_reminders.db";
    private static final int DB_VERSION = 1;
    private static final String TBL = "reminder_rules";

    public ReminderRuleDb(Context ctx) {
        super(ctx, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TBL + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "taskId INTEGER NOT NULL," +
                "daysBefore INTEGER NOT NULL," +
                "repeatCount INTEGER DEFAULT 1," +
                "repeatIntervalHours INTEGER DEFAULT 24," +
                "label TEXT" +
                ")");
        db.execSQL("CREATE INDEX idx_reminder_task ON " + TBL + "(taskId)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS " + TBL);
        onCreate(db);
    }

    public long insert(ReminderRule r) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("taskId", r.taskId);
        cv.put("daysBefore", r.daysBefore);
        cv.put("repeatCount", r.repeatCount);
        cv.put("repeatIntervalHours", r.repeatIntervalHours);
        cv.put("label", r.label);
        return db.insert(TBL, null, cv);
    }

    public void deleteByTask(long taskId) {
        getWritableDatabase().delete(TBL, "taskId=?", new String[]{String.valueOf(taskId)});
    }

    public List<ReminderRule> getByTask(long taskId) {
        List<ReminderRule> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query(TBL, null, "taskId=?",
                new String[]{String.valueOf(taskId)}, null, null, "daysBefore DESC");
        while (c.moveToNext()) {
            ReminderRule r = new ReminderRule();
            r.id = c.getLong(c.getColumnIndexOrThrow("id"));
            r.taskId = c.getLong(c.getColumnIndexOrThrow("taskId"));
            r.daysBefore = c.getInt(c.getColumnIndexOrThrow("daysBefore"));
            r.repeatCount = c.getInt(c.getColumnIndexOrThrow("repeatCount"));
            r.repeatIntervalHours = c.getInt(c.getColumnIndexOrThrow("repeatIntervalHours"));
            r.label = c.getString(c.getColumnIndexOrThrow("label"));
            list.add(r);
        }
        c.close();
        return list;
    }

    /** Dipakai saat reschedule massal (boot, generator) — semua aturan pengingat sekaligus. */
    public List<ReminderRule> getAll() {
        List<ReminderRule> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query(TBL, null, null, null, null, null, null);
        while (c.moveToNext()) {
            ReminderRule r = new ReminderRule();
            r.id = c.getLong(c.getColumnIndexOrThrow("id"));
            r.taskId = c.getLong(c.getColumnIndexOrThrow("taskId"));
            r.daysBefore = c.getInt(c.getColumnIndexOrThrow("daysBefore"));
            r.repeatCount = c.getInt(c.getColumnIndexOrThrow("repeatCount"));
            r.repeatIntervalHours = c.getInt(c.getColumnIndexOrThrow("repeatIntervalHours"));
            r.label = c.getString(c.getColumnIndexOrThrow("label"));
            list.add(r);
        }
        c.close();
        return list;
    }
}
