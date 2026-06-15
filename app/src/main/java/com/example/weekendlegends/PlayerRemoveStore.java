package com.example.weekendlegends;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class PlayerRemoveStore {

    private static final String PREFS = "weekend_legends_prefs";
    private static final String KEY_REMOVED = "removed_players"; // stores lower-case names

    public static boolean isRemoved(Context c, String playerName) {
        if (c == null || playerName == null) return false;
        String key = playerName.trim().toLowerCase();
        return getRemovedSet(c).contains(key);
    }

    public static void softRemove(Context c, String playerName) {
        if (c == null || playerName == null) return;

        SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        HashSet<String> set = new HashSet<>(getRemovedSet(c));
        set.add(playerName.trim().toLowerCase());

        sp.edit().putStringSet(KEY_REMOVED, set).apply();
    }

    public static void restore(Context c, String playerName) {
        if (c == null || playerName == null) return;

        SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        HashSet<String> set = new HashSet<>(getRemovedSet(c));
        set.remove(playerName.trim().toLowerCase());

        sp.edit().putStringSet(KEY_REMOVED, set).apply();
    }

    public static void restoreAll(Context c) {
        if (c == null) return;
        SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().remove(KEY_REMOVED).apply();
    }

    private static Set<String> getRemovedSet(Context c) {
        SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Set<String> raw = sp.getStringSet(KEY_REMOVED, new HashSet<>());
        return raw == null ? new HashSet<>() : raw;
    }
}
