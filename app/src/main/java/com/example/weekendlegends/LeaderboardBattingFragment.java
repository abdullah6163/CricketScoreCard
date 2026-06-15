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

public class LeaderboardBattingFragment extends Fragment {

    private RecyclerView rv;
    private TextView tvEmpty, tvLoading;

    private final ArrayList<LeaderboardRow> rows = new ArrayList<>();
    private LeaderboardAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup parent, @Nullable Bundle state) {
        return inf.inflate(R.layout.fragment_leaderboard_list, parent, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle state) {
        super.onViewCreated(v, state);

        rv = v.findViewById(R.id.rv);
        tvEmpty = v.findViewById(R.id.tvEmpty);
        tvLoading = v.findViewById(R.id.tvLoading);

        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new LeaderboardAdapter(rows, true);
        rv.setAdapter(adapter);

        loadBatting();
    }

    private void loadBatting() {
        showLoading(true);

        new Thread(() -> {
            ArrayList<LeaderboardRow> out = computeTopBatting();

            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                rows.clear();
                rows.addAll(out);
                adapter.notifyDataSetChanged();
                showLoading(false);
            });
        }).start();
    }

    private void showLoading(boolean loading) {
        if (tvLoading != null) tvLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        boolean empty = !loading && rows.isEmpty();
        if (tvEmpty != null) tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (rv != null) rv.setVisibility(empty || loading ? View.GONE : View.VISIBLE);
    }

    private ArrayList<LeaderboardRow> computeTopBatting() {
        HashMap<String, Integer> runsMap = new HashMap<>();

        for (JSONObject row : MatchStore.load(requireContext())) {
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

                    String striker = safe(e.optString("striker", ""));
                    if (striker.isEmpty()) continue;

                    boolean wide = e.optBoolean("wide", false);
                    boolean byes = e.optBoolean("byes", false);

                    String label = e.optString("label", "");
                    int batRuns = 0;

                    if (!wide && !byes) batRuns = parseRunsFromLabel(label);

                    if (batRuns != 0) {
                        Integer cur = runsMap.get(striker);
                        runsMap.put(striker, (cur == null ? 0 : cur) + batRuns);
                    } else {
                        // ensure exists if player has 0 runs but appeared
                        if (!runsMap.containsKey(striker)) runsMap.put(striker, 0);
                    }
                }
            } catch (Exception ignored) {}
        }

        ArrayList<LeaderboardRow> out = new ArrayList<>();
        for (String name : runsMap.keySet()) {
            if (PlayerRemoveStore.isRemoved(requireContext(), name)) continue;
            out.add(new LeaderboardRow(name, runsMap.get(name)));
        }

        Collections.sort(out, (a, b) -> Integer.compare(b.value, a.value));

        // keep top 20
        if (out.size() > 20) return new ArrayList<>(out.subList(0, 20));
        return out;
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
