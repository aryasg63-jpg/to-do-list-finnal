package com.todoku.app;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Ekspor / impor seluruh data (tugas + riwayat) ke JSON — untuk backup & restore.
 */
public class BackupHelper {

    public static String exportJson(TaskDb db) throws JSONException {
        JSONObject root = new JSONObject();
        root.put("app", "todoku");
        root.put("version", 2);

        JSONArray arr = new JSONArray();
        for (Task t : db.getAll()) {
            JSONObject o = taskToJson(t);
            arr.put(o);
        }
        root.put("tasks", arr);

        JSONArray hist = new JSONArray();
        android.database.Cursor c = db.getReadableDatabase().query("task_history", null, null, null, null, null, null);
        while (c.moveToNext()) {
            JSONObject o = new JSONObject();
            o.put("taskId", c.getLong(c.getColumnIndexOrThrow("taskId")));
            o.put("title", c.getString(c.getColumnIndexOrThrow("title")));
            o.put("category", c.getString(c.getColumnIndexOrThrow("category")));
            o.put("doneAtMillis", c.getLong(c.getColumnIndexOrThrow("doneAtMillis")));
            o.put("instanceStartMillis", c.getLong(c.getColumnIndexOrThrow("instanceStartMillis")));
            o.put("estimatedMinutes", c.getInt(c.getColumnIndexOrThrow("estimatedMinutes")));
            hist.put(o);
        }
        c.close();
        root.put("history", hist);
        return root.toString(2);
    }

    public static JSONObject taskToJson(Task t) throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", t.id);
        o.put("title", t.title);
        o.put("category", t.category);
        o.put("startTimeMillis", t.startTimeMillis);
        o.put("prepMinutesBefore", t.prepMinutesBefore);
        o.put("alarmEnabled", t.alarmEnabled);
        o.put("prepAlarmEnabled", t.prepAlarmEnabled);
        o.put("soundUri", t.soundUri == null ? "" : t.soundUri);
        o.put("done", t.done);
        o.put("estimatedMinutes", t.estimatedMinutes);
        o.put("note", t.note == null ? "" : t.note);
        o.put("repeatType", t.repeatType);
        o.put("repeatInterval", t.repeatInterval);
        o.put("repeatWeekdays", t.repeatWeekdays);
        return o;
    }

    /** Merestore data dari JSON. Mengembalikan jumlah tugas yang dimuat. */
    public static int restore(Context ctx, String json) throws JSONException {
        TaskDb db = new TaskDb(ctx);
        JSONObject root = new JSONObject(json);
        JSONArray arr = root.optJSONArray("tasks");
        if (arr == null) return 0;

        // Hapus semua data lama
        ctx.getSharedPreferences("todoku_prefs", Context.MODE_PRIVATE).edit().clear().apply();
        android.database.sqlite.SQLiteDatabase w = db.getWritableDatabase();
        w.delete("tasks", null, null);
        w.delete("task_history", null, null);

        java.util.Map<Long, Long> idMap = new java.util.LinkedHashMap<>();
        for (int i = 0; i < arr.length(); i++) {
            Task t = new Task();
            JSONObject o = arr.getJSONObject(i);
            t.id = o.optLong("id", 0);
            t.title = o.optString("title", "");
            t.category = o.optString("category", "aktivitas");
            t.startTimeMillis = o.optLong("startTimeMillis", 0);
            t.prepMinutesBefore = o.optInt("prepMinutesBefore", 0);
            t.alarmEnabled = o.optBoolean("alarmEnabled", true);
            t.prepAlarmEnabled = o.optBoolean("prepAlarmEnabled", true);
            t.soundUri = o.optString("soundUri", "").isEmpty() ? null : o.optString("soundUri");
            t.done = o.optBoolean("done", false);
            t.estimatedMinutes = o.optInt("estimatedMinutes", 0);
            t.note = o.optString("note", "");
            t.repeatType = o.optString("repeatType", Task.REPEAT_NONE);
            t.repeatInterval = o.optInt("repeatInterval", 1);
            t.repeatWeekdays = o.optInt("repeatWeekdays", 0);

            long oldId = t.id;
            long newId = db.insertOrUpdate(t);
            if (oldId != newId) idMap.put(oldId, newId);
        }

        // Riwayat disimpan ulang dengan pemetaan id baru
        try {
            JSONArray hist = root.optJSONArray("history");
            if (hist != null) {
                for (int i = 0; i < hist.length(); i++) {
                    JSONObject o = hist.getJSONObject(i);
                    long oldTaskId = o.optLong("taskId", 0);
                    long newTaskId = idMap.getOrDefault(oldTaskId, oldTaskId);
                    android.content.ContentValues cv = new android.content.ContentValues();
                    cv.put("taskId", newTaskId);
                    cv.put("title", o.optString("title", ""));
                    cv.put("category", o.optString("category", ""));
                    cv.put("doneAtMillis", o.optLong("doneAtMillis", 0));
                    cv.put("instanceStartMillis", o.optLong("instanceStartMillis", 0));
                    cv.put("estimatedMinutes", o.optInt("estimatedMinutes", 0));
                    w.insert("task_history", null, cv);
                }
            }
        } catch (NumberFormatException ignored) {}

        // Jadwalkan ulang semua alarm
        for (Task t : db.getActiveTasks()) {
            AlarmScheduler.scheduleForTask(ctx, t);
        }
        return arr.length();
    }
}