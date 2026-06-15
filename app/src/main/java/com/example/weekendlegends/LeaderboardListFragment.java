package com.example.weekendlegends;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LeaderboardListFragment extends Fragment {

    private static final String ARG_MODE = "mode"; // "bat" or "bowl"

    private RecyclerView rv;
    private TextView tvLoading, tvEmpty;

    private String mode = "bat";

    public static LeaderboardListFragment newInstance(String mode) {
        LeaderboardListFragment f = new LeaderboardListFragment();
        Bundle b = new Bundle();
        b.putString(ARG_MODE, mode);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            String m = args.getString(ARG_MODE, "bat");
            if (m != null) mode = m;
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View v = inflater.inflate(R.layout.fragment_leaderboard_list, container, false);

        rv = v.findViewById(R.id.rv);
        tvLoading = v.findViewById(R.id.tvLoading);
        tvEmpty = v.findViewById(R.id.tvEmpty);

        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        loadLeaderboardAsync();

        return v;
    }

    private void loadLeaderboardAsync() {
        // show loading
        if (tvLoading != null) tvLoading.setVisibility(View.VISIBLE);
        if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);
        if (rv != null) rv.setVisibility(View.GONE);

        new Thread(() -> {
            ArrayList<LeaderboardRow> rows = buildRows();

            // sort desc
            Collections.sort(rows, (a, b) -> Integer.compare(b.value, a.value));

            // top 10
            if (rows.size() > 10) rows = new ArrayList<>(rows.subList(0, 10));

            boolean isBowling = "bowl".equalsIgnoreCase(mode);
            ArrayList<LeaderboardRow> finalRows = rows;

            if (!isAdded()) return;

            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) return;

                LeaderboardAdapter adapter = new LeaderboardAdapter(finalRows, isBowling);
                rv.setAdapter(adapter);

                if (tvLoading != null) tvLoading.setVisibility(View.GONE);

                boolean empty = finalRows.isEmpty();
                if (tvEmpty != null) tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                if (rv != null) rv.setVisibility(empty ? View.GONE : View.VISIBLE);
            });
        }).start();
    }

    private ArrayList<LeaderboardRow> buildRows() {
        HashMap<String, Integer> totals = new HashMap<>();

        ArrayList<JSONObject> matchRows = MatchStore.load(requireContext());

        for (JSONObject row : matchRows) {
            if (row == null) continue;

            String status = row.optString("status", "");
            // if you only want completed matches, uncomment:
            // if (!MatchStore.STATUS_COMPLETED.equalsIgnoreCase(status)) continue;

            String id = row.optString("id", "");
            String json = MatchStore.getMatchJsonById(requireContext(), id);
            if (json == null || json.trim().isEmpty()) continue;

            try {
                JSONObject match = new JSONObject(json);
                JSONArray balls = match.optJSONArray("ballEvents");
                if (balls == null) continue;

                for (int i = 0; i < balls.length(); i++) {
                    JSONObject e = balls.optJSONObject(i);
                    if (e == null) continue;

                    if ("bowl".equalsIgnoreCase(mode)) {
                        applyBowlingEvent(totals, e);
                    } else {
                        applyBattingEvent(totals, e);
                    }
                }
            } catch (Exception ignored) {}
        }

        ArrayList<LeaderboardRow> out = new ArrayList<>();
        for (Map.Entry<String, Integer> en : totals.entrySet()) {
            String name = safe(en.getKey());
            if (name.isEmpty()) continue;

            // hide soft-removed players
            if (PlayerRemoveStore.isRemoved(requireContext(), name)) continue;

            out.add(new LeaderboardRow(name, en.getValue()));
        }
        return out;
    }

    // batting: add striker runs (ignore wide/byes)
    private void applyBattingEvent(HashMap<String, Integer> totals, JSONObject e) {
        String striker = safe(e.optString("striker", ""));
        if (striker.isEmpty()) return;

        boolean wide = e.optBoolean("wide", false);
        boolean byes = e.optBoolean("byes", false);
        String label = e.optString("label", "");

        int batRuns = 0;
        if (!wide && !byes) batRuns = parseRunsFromLabel(label);
        if (batRuns <= 0) return;

        int old = totals.containsKey(striker) ? totals.get(striker) : 0;
        totals.put(striker, old + batRuns);
    }

    // bowling: +1 wicket for bowler (exclude run out etc)
    private void applyBowlingEvent(HashMap<String, Integer> totals, JSONObject e) {
        boolean wicket = e.optBoolean("wicket", false);
        if (!wicket) return;

        String bowler = safe(e.optString("bowler", ""));
        if (bowler.isEmpty()) return;

        String wicketType = e.optString("wicketType", "");
        if (!countsToBowlerWicket(wicketType)) return;

        int old = totals.containsKey(bowler) ? totals.get(bowler) : 0;
        totals.put(bowler, old + 1);
    }

    private boolean countsToBowlerWicket(String wicketType) {
        if (wicketType == null) return true;
        String t = wicketType.toLowerCase(Locale.US);
        return !(t.contains("run out")
                || t.contains("retired")
                || t.contains("obstruct")
                || t.contains("timed out"));
    }

    private int parseRunsFromLabel(String label) {
        if (label == null) return 0;
        label = label.trim();

        if (label.equalsIgnoreCase("W")) return 0;
        if (label.equalsIgnoreCase("Wd")) return 0;
        if (label.equalsIgnoreCase("Nb")) return 0;
        if (label.equalsIgnoreCase("B")) return 0;

        int n = 0;
        boolean found = false;
        for (int i = 0; i < label.length(); i++) {
            char c = label.charAt(i);
            if (c >= '0' && c <= '9') {
                n = n * 10 + (c - '0');
                found = true;
            } else break;
        }
        return found ? n : 0;
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
