package com.example.weekendlegends;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class MatchStore {

    public static final String STATUS_IN_PROGRESS = "in_progress";
    public static final String STATUS_COMPLETED = "completed";

    private static final String PREFS = "weekend_legends_prefs";

    // Home list (small rows only)
    private static final String KEY_MATCH_LIST = "match_list_json"; // JSONArray of {id,title,dateUtc,status,...}

    // Current in-progress match JSON (full)
    private static final String KEY_INPROGRESS = "match_in_progress_json";

    // Completed/full match JSON stored separately (prevents TransactionTooLarge / OOM)
    private static final String KEY_MATCH_JSON_PREFIX = "match_json_";

    // ✅ NEW: archive flag (hide from Home but keep stats forever)
    private static final String KEY_ARCHIVED = "archived"; // boolean inside row

    // ========================= In-progress helpers =========================

    public static void saveInProgress(Context c, String matchJson) {
        if (matchJson == null) matchJson = "{}";
        SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_INPROGRESS, matchJson).apply();
    }

    public static void clearInProgress(Context c) {
        SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().remove(KEY_INPROGRESS).apply();
    }

    public static String getInProgressRaw(Context c) {
        SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return sp.getString(KEY_INPROGRESS, "");
    }

    // ========================= Fetch full JSON by id =========================

    public static String getMatchJsonById(Context c, String id) {
        if (id == null || id.trim().isEmpty()) return "";

        SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        // 1) check in-progress
        String rawIn = sp.getString(KEY_INPROGRESS, "");
        if (rawIn != null && !rawIn.trim().isEmpty()) {
            try {
                JSONObject j = new JSONObject(rawIn);
                if (id.equals(j.optString("matchId", ""))) return rawIn;
            } catch (Exception ignored) {}
        }

        // 2) check separate big-json key (recommended)
        String raw = sp.getString(KEY_MATCH_JSON_PREFIX + id, null);
        if (raw != null && !raw.trim().isEmpty()) return raw;

        // 3) legacy fallback: matchJson stored inside list
        try {
            JSONArray arr = new JSONArray(sp.getString(KEY_MATCH_LIST, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                if (id.equals(o.optString("id", ""))) {
                    return o.optString("matchJson", "");
                }
            }
        } catch (Exception ignored) {}

        return "";
    }

    public static JSONObject getMatchJson(Context c, String id) {
        try {
            String raw = getMatchJsonById(c, id);
            if (raw == null || raw.trim().isEmpty()) return null;
            return new JSONObject(raw);
        } catch (Exception e) {
            return null;
        }
    }

    // ========================= List rows =========================

    public static ArrayList<JSONObject> load(Context c) {
        ArrayList<JSONObject> out = new ArrayList<>();
        SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = sp.getString(KEY_MATCH_LIST, "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o != null) out.add(o);
            }
        } catch (Exception ignored) {}
        return out;
    }

    // ✅ helper for saving list back
    private static void saveList(Context c, JSONArray arr) {
        SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_MATCH_LIST, arr.toString()).apply();
    }

    // ========================= Archive helpers =========================

    public static boolean isArchived(JSONObject row) {
        return row != null && row.optBoolean(KEY_ARCHIVED, false);
    }

    public static void archive(Context c, String id) {
        if (id == null) return;
        SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        JSONArray arr;
        try {
            arr = new JSONArray(sp.getString(KEY_MATCH_LIST, "[]"));
        } catch (Exception e) {
            arr = new JSONArray();
        }

        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            if (id.equals(o.optString("id", ""))) {
                try { o.put(KEY_ARCHIVED, true); } catch (Exception ignored) {}
                break;
            }
        }

        saveList(c, arr);
    }

    public static void unarchive(Context c, String id) {
        if (id == null) return;
        SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        JSONArray arr;
        try {
            arr = new JSONArray(sp.getString(KEY_MATCH_LIST, "[]"));
        } catch (Exception e) {
            arr = new JSONArray();
        }

        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            if (id.equals(o.optString("id", ""))) {
                try { o.put(KEY_ARCHIVED, false); } catch (Exception ignored) {}
                break;
            }
        }

        saveList(c, arr);
    }

    // ========================= Upsert =========================

    public static void upsert(Context c,
                              String id,
                              String title,
                              long dateUtc,
                              String status,
                              String matchJson) {

        if (id == null) id = "";
        if (title == null) title = "Match";
        if (status == null) status = STATUS_IN_PROGRESS;
        if (matchJson == null) matchJson = "{}";

        SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        // ✅ store full json separately (prevents huge binder/oom)
        sp.edit().putString(KEY_MATCH_JSON_PREFIX + id, matchJson).apply();

        JSONArray arr;
        try {
            arr = new JSONArray(sp.getString(KEY_MATCH_LIST, "[]"));
        } catch (Exception e) {
            arr = new JSONArray();
        }

        // keep old archived flag if exists
        boolean archivedFlag = false;

        // remove existing with same id (but remember archived flag)
        JSONArray newArr = new JSONArray();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            String oid = o.optString("id", "");
            if (oid.equals(id)) {
                archivedFlag = o.optBoolean(KEY_ARCHIVED, false);
                continue;
            }
            newArr.put(o);
        }

        // ✅ small row only (NO matchJson here)
        JSONObject row = new JSONObject();
        try {
            row.put("id", id);
            row.put("title", title);
            row.put("dateUtc", dateUtc);
            row.put("dateText", formatDate(dateUtc));
            row.put("status", status);

            // ✅ IMPORTANT: archived flag kept
            row.put(KEY_ARCHIVED, archivedFlag);
        } catch (Exception ignored) {}

        // newest first
        JSONArray finalArr = new JSONArray();
        finalArr.put(row);
        for (int i = 0; i < newArr.length(); i++) finalArr.put(newArr.opt(i));

        sp.edit().putString(KEY_MATCH_LIST, finalArr.toString()).apply();
    }

    // ========================= HARD DELETE (optional) =========================
    // ❗ Do NOT use from Home if you want leaderboard/career unchanged.
    public static void deleteForever(Context c, String id) {
        SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        JSONArray arr;
        try {
            arr = new JSONArray(sp.getString(KEY_MATCH_LIST, "[]"));
        } catch (Exception e) {
            arr = new JSONArray();
        }

        JSONArray out = new JSONArray();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            if (!id.equals(o.optString("id", ""))) out.put(o);
        }

        sp.edit()
                .putString(KEY_MATCH_LIST, out.toString())
                .remove(KEY_MATCH_JSON_PREFIX + id)
                .apply();

        // if deleting active in-progress
        String rawIn = sp.getString(KEY_INPROGRESS, "");
        if (rawIn != null && !rawIn.trim().isEmpty()) {
            try {
                JSONObject j = new JSONObject(rawIn);
                if (id.equals(j.optString("matchId", ""))) {
                    sp.edit().remove(KEY_INPROGRESS).apply();
                }
            } catch (Exception ignored) {}
        }
    }

    private static String formatDate(long utcMillis) {
        if (utcMillis <= 0) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(new Date(utcMillis));
    }
}
