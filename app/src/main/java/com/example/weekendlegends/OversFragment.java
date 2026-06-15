package com.example.weekendlegends;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class OversFragment extends Fragment {

    private static final String ARG_JSON = "arg_json";

    public static OversFragment newInstance(String matchJson) {
        OversFragment f = new OversFragment();
        Bundle b = new Bundle();
        b.putString(ARG_JSON, matchJson);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_overs, container, false);

        RecyclerView rv = v.findViewById(R.id.rvOvers);

        // ✅ MAIN LIST MUST BE VERTICAL
        rv.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false));
        rv.setHasFixedSize(true);
        rv.setNestedScrollingEnabled(false);

        String json = getArguments() != null ? getArguments().getString(ARG_JSON, "{}") : "{}";

        JSONObject match;
        try { match = new JSONObject(json); }
        catch (Exception e) { match = new JSONObject(); }

        JSONArray ballEvents = match.optJSONArray("ballEvents");
        ArrayList<OverListItem> items = buildOverList(ballEvents);

        rv.setAdapter(new OverListAdapter(items));

        return v;
    }

    private ArrayList<OverListItem> buildOverList(@Nullable JSONArray ballEvents) {

        ArrayList<OverListItem> out = new ArrayList<>();

        if (ballEvents == null || ballEvents.length() == 0) return out;

        int currentInnings = -1;
        int currentOverIndex = -1;   // 0-based internal
        OverItem currentOver = null;

        int legalBallCount = 0; // counts only legal balls of current innings

        for (int i = 0; i < ballEvents.length(); i++) {
            JSONObject e = ballEvents.optJSONObject(i);
            if (e == null) continue;

            int inns = e.optInt("innings", 1);
            boolean legalBall = e.optBoolean("legalBall", true);

            String label = e.optString("label", "");
            boolean wicket = e.optBoolean("wicket", false);

            String bowler = e.optString("bowler", "");

            // ✅ innings changed: add header + reset counters
            if (inns != currentInnings) {
                currentInnings = inns;
                currentOverIndex = -1;
                legalBallCount = 0;
                currentOver = null;

                out.add(OverListItem.header(currentInnings == 1 ? "1st Innings" : "2nd Innings"));
            }

            // ✅ determine over index based only on legal balls
            int overIndexNow = (legalBallCount / 6);

            // ✅ new over row
            if (currentOver == null || overIndexNow != currentOverIndex) {
                currentOverIndex = overIndexNow;

                currentOver = new OverItem();
                currentOver.innings = currentInnings;
                currentOver.overNo = currentOverIndex + 1; // show 1-based
                currentOver.bowler = bowler;
                currentOver.totalRuns = 0;
                currentOver.balls = new ArrayList<>();

                out.add(OverListItem.over(currentOver));
            }

            // ✅ IMPORTANT FIX:
            // Use constructor so 4Wd / 6Nb still becomes boundary colored
            BallItem bi = new BallItem(label, wicket);
            currentOver.balls.add(bi);

            // ✅ total runs per over
            currentOver.totalRuns += e.optInt("teamRuns", 0);

            // ✅ only count legal balls
            if (legalBall) legalBallCount++;
        }

        return out;
    }
}
