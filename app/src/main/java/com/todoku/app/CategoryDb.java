package com.todoku.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class CategoryDb extends SQLiteOpenHelper {

    private static final String DB_NAME = "todoku_categories.db";
    private static final int DB_VERSION = 1;
    private static final String TBL = "categories";

    public CategoryDb(Context ctx) {
        super(ctx, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TBL + " (" +
                "key TEXT PRIMARY KEY," +
                "label TEXT NOT NULL," +
                "emoji TEXT NOT NULL," +
                "colorHex TEXT NOT NULL," +
                "isDefault INTEGER DEFAULT 0" +
                ")");
        seedDefaults(db);
    }

    private void seedDefaults(SQLiteDatabase db) {
        insertRaw(db, "olahraga", "Olahraga", "🏃", "#FF6B6B", true);
        insertRaw(db, "makanan", "Makanan", "🍽️", "#FFA94D", true);
        insertRaw(db, "minuman", "Minuman", "🥤", "#4DABF7", true);
        insertRaw(db, "aktivitas", "Aktivitas", "⭐", "#69DB7C", true);
        insertRaw(db, "rumah", "Tugas Rumah", "🏠", "#DA77F2", true);
        insertRaw(db, "sekolah", "Tugas Sekolah", "📚", "#FFD43B", true);
    }

    private void insertRaw(SQLiteDatabase db, String key, String label, String emoji, String color, boolean isDefault) {
        ContentValues cv = new ContentValues();
        cv.put("key", key);
        cv.put("label", label);
        cv.put("emoji", emoji);
        cv.put("colorHex", color);
        cv.put("isDefault", isDefault ? 1 : 0);
        db.insert(TBL, null, cv);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) { }

    public void addOrUpdate(Category c) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("key", c.key);
        cv.put("label", c.label);
        cv.put("emoji", c.emoji);
        cv.put("colorHex", c.colorHex);
        cv.put("isDefault", c.isDefault ? 1 : 0);
        db.insertWithOnConflict(TBL, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void delete(String key) {
        getWritableDatabase().delete(TBL, "key=? AND isDefault=0", new String[]{key});
    }

    public List<Category> getAll() {
        List<Category> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query(TBL, null, null, null, null, null, "isDefault DESC, label ASC");
        while (c.moveToNext()) {
            Category cat = new Category();
            cat.key = c.getString(c.getColumnIndexOrThrow("key"));
            cat.label = c.getString(c.getColumnIndexOrThrow("label"));
            cat.emoji = c.getString(c.getColumnIndexOrThrow("emoji"));
            cat.colorHex = c.getString(c.getColumnIndexOrThrow("colorHex"));
            cat.isDefault = c.getInt(c.getColumnIndexOrThrow("isDefault")) == 1;
            list.add(cat);
        }
        c.close();
        return list;
    }
}
