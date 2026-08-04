package com.todoku.app;

/** Level prioritas tugas: 0=Rendah, 1=Sedang, 2=Tinggi. */
public class Priority {
    public static final int LOW = 0;
    public static final int MEDIUM = 1;
    public static final int HIGH = 2;

    public static String label(int p) {
        switch (p) {
            case HIGH: return "Tinggi";
            case LOW: return "Rendah";
            default: return "Sedang";
        }
    }

    public static String flag(int p) {
        switch (p) {
            case HIGH: return "🔴";
            case LOW: return "🟢";
            default: return "🟡";
        }
    }

    public static String colorHex(int p) {
        switch (p) {
            case HIGH: return "#FF5252";
            case LOW: return "#69DB7C";
            default: return "#FFC94D";
        }
    }

    public static int[] allLevels() {
        return new int[]{LOW, MEDIUM, HIGH};
    }
}
