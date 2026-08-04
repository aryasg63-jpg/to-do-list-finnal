package com.todoku.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Widget layar utama: ringkasan "X dari Y selesai hari ini" + tugas berikutnya.
 * Read-only (tap membuka app) — cukup untuk kebutuhan lihat sekilas tanpa buka app,
 * tanpa kompleksitas RemoteViewsService untuk checklist interaktif di dalam widget.
 */
public class TodoWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context ctx, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            updateWidget(ctx, appWidgetManager, id);
        }
    }

    /** Dipanggil dari mana pun data tugas berubah (simpan/hapus/toggle done) supaya widget selalu segar. */
    public static void refreshAllWidgets(Context ctx) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        ComponentName component = new ComponentName(ctx, TodoWidgetProvider.class);
        int[] ids = mgr.getAppWidgetIds(component);
        for (int id : ids) {
            updateWidget(ctx, mgr, id);
        }
    }

    private static void updateWidget(Context ctx, AppWidgetManager mgr, int widgetId) {
        TaskDb db = new TaskDb(ctx);
        long today = DateUtil.todayEpochDay();
        List<Task> todayTasks = db.getForDate(today);

        int total = todayTasks.size();
        int done = 0;
        for (Task t : todayTasks) if (t.done) done++;

        Task nextTask = null;
        long now = System.currentTimeMillis();
        List<Task> upcoming = db.getForDateRange(today, today + 7);
        for (Task t : upcoming) {
            if (!t.done && t.startTimeMillis > now && (nextTask == null || t.startTimeMillis < nextTask.startTimeMillis)) {
                nextTask = t;
            }
        }

        RemoteViews views = new RemoteViews(ctx.getPackageName(), R.layout.widget_todoku);
        views.setTextViewText(R.id.widgetDate,
                new SimpleDateFormat("EEEE, d MMMM", new Locale("id", "ID")).format(new java.util.Date()));
        views.setTextViewText(R.id.widgetSummary, done + " dari " + total + " selesai");

        if (nextTask != null) {
            String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new java.util.Date(nextTask.startTimeMillis));
            views.setTextViewText(R.id.widgetNextTask,
                    CategoryHelper.emojiFor(nextTask.category) + " " + nextTask.title + " — " + time);
        } else {
            views.setTextViewText(R.id.widgetNextTask, "Tidak ada tugas mendatang 🎉");
        }

        Intent openApp = new Intent(ctx, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widgetRoot, pi);

        mgr.updateAppWidget(widgetId, views);
    }
}
