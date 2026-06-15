package com.example.weekendlegends;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.util.Base64;

import java.io.ByteArrayOutputStream;

public class PlayerImageStore {

    private static final String PREFS = "weekend_legends_prefs";
    private static final String KEY_PREFIX = "player_img_";

    public static void saveBitmapBase64(Context c, String player, Bitmap bmp) {
        try {
            if (c == null || player == null || bmp == null) return;

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            // ✅ PNG keeps transparency (no white box)
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
            String b64 = Base64.encodeToString(out.toByteArray(), Base64.DEFAULT);

            SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            sp.edit().putString(KEY_PREFIX + player.trim(), b64).apply();
        } catch (Exception ignored) {}
    }

    public static String getImageBase64(Context c, String player) {
        if (c == null || player == null) return "";
        SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return sp.getString(KEY_PREFIX + player.trim(), "");
    }

    public static void clearImage(Context c, String player) {
        if (c == null || player == null) return;
        SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().remove(KEY_PREFIX + player.trim()).apply();
    }
}
