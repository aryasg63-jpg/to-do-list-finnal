package com.todoku.app;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class PrefsHelper {

    private static final String PREFS = "todoku_prefs";
    private static final String KEY_THEME_MODE = "theme_mode"; // "system" | "light" | "dark"
    private static final String KEY_DIGEST_ENABLED = "digest_enabled";
    private static final String KEY_DIGEST_HOUR = "digest_hour";
    private static final String KEY_DIGEST_MINUTE = "digest_minute";

    public static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ---------- Mode gelap ----------
    public static String getThemeMode(Context ctx) {
        return prefs(ctx).getString(KEY_THEME_MODE, "system");
    }

    public static void setThemeMode(Context ctx, String mode) {
        prefs(ctx).edit().putString(KEY_THEME_MODE, mode).apply();
        applyThemeMode(mode);
    }

    public static void applyThemeMode(String mode) {
        switch (mode) {
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    // ---------- Ringkasan / pengingat pagi ----------
    public static boolean isDigestEnabled(Context ctx) {
        return prefs(ctx).getBoolean(KEY_DIGEST_ENABLED, true);
    }

    public static void setDigestEnabled(Context ctx, boolean enabled) {
        prefs(ctx).edit().putBoolean(KEY_DIGEST_ENABLED, enabled).apply();
    }

    public static int getDigestHour(Context ctx) {
        return prefs(ctx).getInt(KEY_DIGEST_HOUR, 6); // default jam 06:00
    }

    public static int getDigestMinute(Context ctx) {
        return prefs(ctx).getInt(KEY_DIGEST_MINUTE, 0);
    }

    public static void setDigestTime(Context ctx, int hour, int minute) {
        prefs(ctx).edit().putInt(KEY_DIGEST_HOUR, hour).putInt(KEY_DIGEST_MINUTE, minute).apply();
    }
}
