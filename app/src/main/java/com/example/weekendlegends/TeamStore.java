package com.example.weekendlegends;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TeamStore {

    public static class TeamStat {
        public String team;
        public int matches;
        public int wins;
        public int losses;
        public long lastMatchUtc;
    }

    public static class PlayerCount {
        public String name;
        public int matchesPlayed;     // appearances for this team
        public long lastSeenUtc;      // last match date for this team

        public PlayerCount(String name) {
            this.name = name;
        }
    }

    /** Load team stats from completed matches only */
    public static ArrayList<TeamStat> loadTeamStats(Context c) {
        HashMap<String, TeamStat> map = new HashMap<>();

        for (JSONObject row : MatchStore.load(c)) {
            if (row == null) continue;
            String status = row.optString("status", "");
            if (!MatchStore.STATUS_COMPLETED.equalsIgnoreCase(status)) continue;

            String id = row.optString("id", "");
            String raw = MatchStore.getMatchJsonById(c, id);
            if (raw == null || raw.trim().isEmpty()) continue;

            try {
                JSONObject match = new JSONObject(raw);

                String teamA = pickString(match, "teamA", "teamAName", "aTeam", "homeTeam");
                String teamB = pickString(match, "teamB", "teamBName", "bTeam", "awayTeam");
                if (teamA.isEmpty() || teamB.isEmpty()) continue;

                long dateUtc = pickLong(match, "matchDateUtc", "dateUtc", "date");

                // add match appearance
                TeamStat a = ensure(map, teamA);
                TeamStat b = ensure(map, teamB);
                a.matches++;
                b.matches++;

                if (dateUtc > a.lastMatchUtc) a.lastMatchUtc = dateUtc;
                if (dateUtc > b.lastMatchUtc) b.lastMatchUtc = dateUtc;

                // winner
                String winner = detectWinnerTeamName(match, teamA, teamB);
                if (!winner.isEmpty()) {
                    if (winner.equalsIgnoreCase(teamA)) {
                        a.wins++; b.losses++;
                    } else if (winner.equalsIgnoreCase(teamB)) {
                        b.wins++; a.losses++;
                    }
                }

            } catch (Exception ignored) {}
        }

        ArrayList<TeamStat> out = new ArrayList<>(map.values());
        // sort by last match desc then name
        Collections.sort(out, (x, y) -> {
            if (y.lastMatchUtc != x.lastMatchUtc) return Long.compare(y.lastMatchUtc, x.lastMatchUtc);
            return x.team.compareToIgnoreCase(y.team);
        });
        return out;
    }

    /** Returns (A) last match squad + (B) all-time squad with appearance counts */
    public static ResultTeamPlayers loadTeamPlayers(Context c, String teamName) {
        ResultTeamPlayers res = new ResultTeamPlayers();
        res.teamName = teamName == null ? "" : teamName.trim();

        if (res.teamName.isEmpty()) return res;

        // all-time counts
        HashMap<String, PlayerCount> counts = new HashMap<>();

        // find last match for this team
        long lastUtc = -1;
        JSONObject lastMatchJson = null;
        String lastTeamA = "", lastTeamB = "";

        for (JSONObject row : MatchStore.load(c)) {
            if (row == null) continue;
            String status = row.optString("status", "");
            if (!MatchStore.STATUS_COMPLETED.equalsIgnoreCase(status)) continue;

            String id = row.optString("id", "");
            String raw = MatchStore.getMatchJsonById(c, id);
            if (raw == null || raw.trim().isEmpty()) continue;

            try {
                JSONObject match = new JSONObject(raw);

                String teamA = pickString(match, "teamA", "teamAName", "aTeam", "homeTeam");
                String teamB = pickString(match, "teamB", "teamBName", "bTeam", "awayTeam");
                if (teamA.isEmpty() || teamB.isEmpty()) continue;

                long dateUtc = pickLong(match, "matchDateUtc", "dateUtc", "date");

                boolean involved = teamA.equalsIgnoreCase(res.teamName) || teamB.equalsIgnoreCase(res.teamName);
                if (!involved) continue;

                // collect squad arrays if present
                ArrayList<String> squad = new ArrayList<>();
                if (teamA.equalsIgnoreCase(res.teamName)) {
                    squad.addAll(readStringArray(match, "teamAPlayers", "playersA", "squadA", "teamAList"));
                } else {
                    squad.addAll(readStringArray(match, "teamBPlayers", "playersB", "squadB", "teamBList"));
                }

                // update all-time counts (only if we actually have squad arrays)
                if (!squad.isEmpty()) {
                    for (String p : squad) {
                        p = safe(p);
                        if (p.isEmpty()) continue;
                        PlayerCount pc = counts.get(p.toLowerCase(Locale.US));
                        if (pc == null) {
                            pc = new PlayerCount(p);
                            counts.put(p.toLowerCase(Locale.US), pc);
                        }
                        pc.matchesPlayed++;
                        if (dateUtc > pc.lastSeenUtc) pc.lastSeenUtc = dateUtc;
                    }
                }

                // track last match json
                if (dateUtc > lastUtc) {
                    lastUtc = dateUtc;
                    lastMatchJson = match;
                    lastTeamA = teamA;
                    lastTeamB = teamB;
                }

            } catch (Exception ignored) {}
        }

        // last match squad
        if (lastMatchJson != null && lastUtc > 0) {
            res.lastMatchUtc = lastUtc;

            if (lastTeamA.equalsIgnoreCase(res.teamName)) {
                res.lastMatchSquad = readStringArray(lastMatchJson, "teamAPlayers", "playersA", "squadA", "teamAList");
            } else if (lastTeamB.equalsIgnoreCase(res.teamName)) {
                res.lastMatchSquad = readStringArray(lastMatchJson, "teamBPlayers", "playersB", "squadB", "teamBList");
            }
        }

        // all-time list sorted by appearances desc then name
        res.allTimeSquad = new ArrayList<>(counts.values());
        Collections.sort(res.allTimeSquad, (a, b) -> {
            if (b.matchesPlayed != a.matchesPlayed) return b.matchesPlayed - a.matchesPlayed;
            return a.name.compareToIgnoreCase(b.name);
        });

        return res;
    }

    public static class ResultTeamPlayers {
        public String teamName = "";
        public long lastMatchUtc = 0;
        public ArrayList<String> lastMatchSquad = new ArrayList<>();
        public ArrayList<PlayerCount> allTimeSquad = new ArrayList<>();
    }

    // ---------------- helpers ----------------

    private static TeamStat ensure(HashMap<String, TeamStat> map, String team) {
        String key = safe(team).toLowerCase(Locale.US);
        TeamStat s = map.get(key);
        if (s == null) {
            s = new TeamStat();
            s.team = safe(team);
            map.put(key, s);
        }
        return s;
    }

    private static String detectWinnerTeamName(JSONObject match, String teamA, String teamB) {
        // Try common keys
        String w = pickString(match,
                "winnerTeam",
                "winnerTeamName",
                "winner",
                "matchWinner",
                "resultWinner"
        );

        if (!w.isEmpty()) {
            // normalize to A/B names if stored as "A"/"B"
            if (w.equalsIgnoreCase("A")) return teamA;
            if (w.equalsIgnoreCase("B")) return teamB;

            // if stored as team name
            if (w.equalsIgnoreCase(teamA)) return teamA;
            if (w.equalsIgnoreCase(teamB)) return teamB;
        }

        // Some apps store boolean
        if (match.optBoolean("teamAWon", false)) return teamA;
        if (match.optBoolean("teamBWon", false)) return teamB;

        // If you don't store winner anywhere → cannot compute wins/losses reliably.
        return "";
    }

    private static ArrayList<String> readStringArray(JSONObject o, String... keys) {
        for (String k : keys) {
            JSONArray arr = o.optJSONArray(k);
            if (arr == null) continue;

            ArrayList<String> out = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                String s = safe(arr.optString(i, ""));
                if (!s.isEmpty()) out.add(s);
            }
            if (!out.isEmpty()) return out;
        }
        return new ArrayList<>();
    }

    private static String pickString(JSONObject o, String... keys) {
        for (String k : keys) {
            String v = safe(o.optString(k, ""));
            if (!v.isEmpty()) return v;
        }
        return "";
    }

    private static long pickLong(JSONObject o, String... keys) {
        for (String k : keys) {
            long v = o.optLong(k, 0L);
            if (v > 0) return v;
        }
        return 0L;
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
