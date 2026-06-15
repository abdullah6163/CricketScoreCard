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

public class LeaderboardBowlingFragment extends Fragment {

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
        adapter = new LeaderboardAdapter(rows, false);
        rv.setAdapter(adapter);

        loadBowling();
    }

    private void loadBowling() {
        showLoading(true);

        new Thread(() -> {
            ArrayList<LeaderboardRow> out = computeTopBowling();

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

    private ArrayList<LeaderboardRow> computeTopBowling() {
        HashMap<String, Integer> wktsMap = new HashMap<>();

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

                    String bowler = safe(e.optString("bowler", ""));
                    if (bowler.isEmpty()) continue;

                    boolean wicket = e.optBoolean("wicket", false);
                    String wicketType = e.optString("wicketType", "");

                    // ensure exists
                    if (!wktsMap.containsKey(bowler)) wktsMap.put(bowler, 0);

                    if (wicket && countsToBowlerWicket(wicketType)) {
                        wktsMap.put(bowler, wktsMap.get(bowler) + 1);
                    }
                }
            } catch (Exception ignored) {}
        }

        ArrayList<LeaderboardRow> out = new ArrayList<>();
        for (String name : wktsMap.keySet()) {
            if (PlayerRemoveStore.isRemoved(requireContext(), name)) continue;
            out.add(new LeaderboardRow(name, wktsMap.get(name)));
        }

        Collections.sort(out, (a, b) -> Integer.compare(b.value, a.value));

        if (out.size() > 20) return new ArrayList<>(out.subList(0, 20));
        return out;
    }

    private boolean countsToBowlerWicket(String wicketType) {
        if (wicketType == null) return true;
        String t = wicketType.toLowerCase(Locale.US);
        return !(t.contains("run out")
                || t.contains("retired")
                || t.contains("obstruct")
                || t.contains("timed out"));
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
