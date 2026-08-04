package com.todoku.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class RecurringTemplateDb extends SQLiteOpenHelper {

    private static final String DB_NAME = "todoku_templates.db";
    private static final int DB_VERSION = 1;
    private static final String TBL = "templates";

    public RecurringTemplateDb(Context ctx) {
        super(ctx, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TBL + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT NOT NULL," +
                "category TEXT NOT NULL," +
                "priority INTEGER DEFAULT 1," +
                "recurrenceType TEXT NOT NULL," +
                "recurrenceDays INTEGER DEFAULT 0," +
                "hour INTEGER NOT NULL," +
                "minute INTEGER NOT NULL," +
                "prepMinutesBefore INTEGER DEFAULT 0," +
                "alarmEnabled INTEGER DEFAULT 1," +
                "prepAlarmEnabled INTEGER DEFAULT 1," +
                "soundUri TEXT," +
                "estimatedMinutes INTEGER DEFAULT 0," +
                "active INTEGER DEFAULT 1" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS " + TBL);
        onCreate(db);
    }

    public long insertOrUpdate(RecurringTemplate t) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("title", t.title);
        cv.put("category", t.category);
        cv.put("priority", t.priority);
        cv.put("recurrenceType", t.recurrenceType);
        cv.put("recurrenceDays", t.recurrenceDays);
        cv.put("hour", t.hour);
        cv.put("minute", t.minute);
        cv.put("prepMinutesBefore", t.prepMinutesBefore);
        cv.put("alarmEnabled", t.alarmEnabled ? 1 : 0);
        cv.put("prepAlarmEnabled", t.prepAlarmEnabled ? 1 : 0);
        cv.put("soundUri", t.soundUri);
        cv.put("estimatedMinutes", t.estimatedMinutes);
        cv.put("active", t.active ? 1 : 0);

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

    public RecurringTemplate getById(long id) {
        Cursor c = getReadableDatabase().query(TBL, null, "id=?", new String[]{String.valueOf(id)}, null, null, null);
        RecurringTemplate t = null;
        if (c.moveToFirst()) t = fromCursor(c);
        c.close();
        return t;
    }

    public List<RecurringTemplate> getAllActive() {
        List<RecurringTemplate> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query(TBL, null, "active=1", null, null, null, "hour ASC, minute ASC");
        while (c.moveToNext()) list.add(fromCursor(c));
        c.close();
        return list;
    }

    public List<RecurringTemplate> getAll() {
        List<RecurringTemplate> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query(TBL, null, null, null, null, null, "hour ASC, minute ASC");
        while (c.moveToNext()) list.add(fromCursor(c));
        c.close();
        return list;
    }

    private RecurringTemplate fromCursor(Cursor c) {
        RecurringTemplate t = new RecurringTemplate();
        t.id = c.getLong(c.getColumnIndexOrThrow("id"));
        t.title = c.getString(c.getColumnIndexOrThrow("title"));
        t.category = c.getString(c.getColumnIndexOrThrow("category"));
        t.priority = c.getInt(c.getColumnIndexOrThrow("priority"));
        t.recurrenceType = c.getString(c.getColumnIndexOrThrow("recurrenceType"));
        t.recurrenceDays = c.getInt(c.getColumnIndexOrThrow("recurrenceDays"));
        t.hour = c.getInt(c.getColumnIndexOrThrow("hour"));
        t.minute = c.getInt(c.getColumnIndexOrThrow("minute"));
        t.prepMinutesBefore = c.getInt(c.getColumnIndexOrThrow("prepMinutesBefore"));
        t.alarmEnabled = c.getInt(c.getColumnIndexOrThrow("alarmEnabled")) == 1;
        t.prepAlarmEnabled = c.getInt(c.getColumnIndexOrThrow("prepAlarmEnabled")) == 1;
        t.soundUri = c.getString(c.getColumnIndexOrThrow("soundUri"));
        t.estimatedMinutes = c.getInt(c.getColumnIndexOrThrow("estimatedMinutes"));
        t.active = c.getInt(c.getColumnIndexOrThrow("active")) == 1;
        return t;
    }
}
