package com.todoku.app;

import android.app.Application;

public class TodoApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Terapkan mode gelap/terang tersimpan SEBELUM activity manapun digambar
        PrefsHelper.applyThemeMode(PrefsHelper.getThemeMode(this));
        // Muat cache kategori (default + custom) sekali di awal proses
        CategoryHelper.init(this);
    }
}
