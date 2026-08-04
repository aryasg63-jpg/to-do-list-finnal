package com.todoku.app;

import android.content.Context;
import android.graphics.Color;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Facade statis di atas CategoryDb. Dipanggil tanpa Context di banyak tempat
 * (adapter, activity alarm, dsb) sehingga kita simpan cache in-memory yang
 * di-refresh setiap kali kategori berubah (tambah/edit/hapus) atau saat app start.
 */
public class CategoryHelper {

    private static final Map<String, Category> cache = new LinkedHashMap<>();
    private static boolean loaded = false;

    public static synchronized void init(Context ctx) {
        refresh(ctx);
    }

    public static synchronized void refresh(Context ctx) {
        cache.clear();
        CategoryDb db = new CategoryDb(ctx.getApplicationContext());
        for (Category c : db.getAll()) {
            cache.put(c.key, c);
        }
        loaded = true;
    }

    private static Category get(String key) {
        Category c = cache.get(key);
        if (c != null) return c;
        // fallback aman kalau cache belum sempat load atau key tidak dikenal
        return new Category(key, key == null ? "Lainnya" : key, "📌", "#6C5CE7", false);
    }

    public static String emojiFor(String key) { return get(key).emoji; }
    public static String labelFor(String key) { return get(key).label; }
    public static int colorFor(String key) { return Color.parseColor(get(key).colorHex); }
    public static String colorHexFor(String key) { return get(key).colorHex; }

    public static String[] allCategories() {
        return cache.keySet().toArray(new String[0]);
    }

    public static List<Category> allCategoryObjects() {
        return new java.util.ArrayList<>(cache.values());
    }

    public static boolean isLoaded() { return loaded; }
}
