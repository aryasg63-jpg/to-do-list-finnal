package com.todoku.app;

import android.graphics.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * Kategori bawaan + dukungan kategori CUSTOM bebas dari user.
 * Kategori custom memakai emoji default 📌, label sesuai ketikan user.
 */
public class CategoryHelper {

    public static final String OLAHRAGA = "olahraga";
    public static final String MAKANAN = "makanan";
    public static final String MINUMAN = "minuman";
    public static final String AKTIVITAS = "aktivitas";
    public static final String RUMAH = "rumah";
    public static final String SEKOLAH = "sekolah";
    public static final String CUSTOM = "lokal_custom";

    private static final Map<String, String> EMOJI = new HashMap<>();
    private static final Map<String, String> LABEL = new HashMap<>();
    private static final Map<String, String> COLOR = new HashMap<>();

    static {
        EMOJI.put(OLAHRAGA, "🏃"); LABEL.put(OLAHRAGA, "Olahraga"); COLOR.put(OLAHRAGA, "#FF6B6B");
        EMOJI.put(MAKANAN, "🍽️"); LABEL.put(MAKANAN, "Makanan"); COLOR.put(MAKANAN, "#FFA94D");
        EMOJI.put(MINUMAN, "🥤"); LABEL.put(MINUMAN, "Minuman"); COLOR.put(MINUMAN, "#4DABF7");
        EMOJI.put(AKTIVITAS, "⭐"); LABEL.put(AKTIVITAS, "Aktivitas"); COLOR.put(AKTIVITAS, "#69DB7C");
        EMOJI.put(RUMAH, "🏠"); LABEL.put(RUMAH, "Tugas Rumah"); COLOR.put(RUMAH, "#DA77F2");
        EMOJI.put(SEKOLAH, "📚"); LABEL.put(SEKOLAH, "Tugas Sekolah"); COLOR.put(SEKOLAH, "#FFD43B");
    }

    public static String emojiFor(String cat) {
        if (isCustom(cat)) return "📌";
        return EMOJI.getOrDefault(cat, "📌");
    }

    public static String labelFor(String cat) {
        if (isCustom(cat)) return cat;
        return LABEL.getOrDefault(cat, "Lainnya");
    }

    public static int colorFor(String cat) {
        return Color.parseColor(COLOR.getOrDefault(cat, "#6C5CE7"));
    }

    public static boolean isCustom(String cat) {
        return cat != null && !cat.equals(CUSTOM) && !LABEL.containsKey(cat);
    }

    public static String[] builtInCategories() {
        return new String[]{OLAHRAGA, MAKANAN, MINUMAN, AKTIVITAS, RUMAH, SEKOLAH};
    }

    /** Label untuk spinner kategori (pilihan "Kategori custom..." ada di paling akhir). */
    public static String[] builtInCategoryLabels() {
        String[] keys = builtInCategories();
        String[] labels = new String[keys.length + 1];
        for (int i = 0; i < keys.length; i++) {
            labels[i] = emojiFor(keys[i]) + " " + labelFor(keys[i]);
        }
        labels[keys.length] = "✨ Kategori custom...";
        return labels;
    }

    /** Jika index spinner = terakhir, berarti user mau tulis kategori sendiri. */
    public static boolean isCustomSlot(int index) {
        return index == builtInCategories().length;
    }
}