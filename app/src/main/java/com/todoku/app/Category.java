package com.todoku.app;

/** Satu kategori kegiatan — 6 bawaan + custom buatan user, semua struktur sama. */
public class Category {
    public String key;      // id unik, mis. "olahraga" atau "cat_1699999"
    public String label;    // nama tampil
    public String emoji;
    public String colorHex;
    public boolean isDefault; // kategori bawaan tidak bisa dihapus, hanya custom yang bisa

    public Category() {}

    public Category(String key, String label, String emoji, String colorHex, boolean isDefault) {
        this.key = key;
        this.label = label;
        this.emoji = emoji;
        this.colorHex = colorHex;
        this.isDefault = isDefault;
    }
}
