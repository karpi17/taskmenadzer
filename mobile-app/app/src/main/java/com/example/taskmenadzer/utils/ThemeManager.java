package com.example.taskmenadzer.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.taskmenadzer.R;

public class ThemeManager {

    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_THEME = "key_theme";
    public static final int THEME_SPACE = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;

    public static void applyTheme(Context context) {
        int theme = getTheme(context);
        switch (theme) {
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case THEME_SPACE:
            default:
                // For Space theme, we might enforce Dark mode + specific style
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                context.setTheme(R.style.Theme_TaskMenadzer);
                break;
        }
    }

    public static void saveTheme(Context context, int theme) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_THEME, theme).apply();
    }

    public static int getTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_THEME, THEME_SPACE); // Default to Space
    }

    public static void setHapticEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean("haptic_feedback", enabled).apply();
    }

    public static boolean isHapticEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean("haptic_feedback", true); // Default true
    }
}
