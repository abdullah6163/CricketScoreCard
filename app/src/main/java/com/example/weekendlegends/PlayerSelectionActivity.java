package com.example.weekendlegends;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

public class PlayerSelectionActivity extends AppCompatActivity {

    public static final String EXTRA_TEAM_A_NAME = "teamAName";
    public static final String EXTRA_TEAM_B_NAME = "teamBName";
    public static final String EXTRA_OVERS_LIMIT = "oversLimit";
    public static final String EXTRA_PLAYERS_PER_TEAM = "playersPerTeam";
    public static final String EXTRA_TOSS_WINNER = "tossWinner";
    public static final String EXTRA_OPTED = "opted";

    public static final String EXTRA_TEAM_A_PLAYERS = "teamAPlayers";
    public static final String EXTRA_TEAM_B_PLAYERS = "teamBPlayers";

    public static final String EXTRA_TEAM_A_BATTING = "teamABatting";
    public static final String EXTRA_STRIKER = "striker";
    public static final String EXTRA_NON_STRIKER = "nonStriker";
    public static final String EXTRA_BOWLER = "bowler";

    public static final String EXTRA_MATCH_ID = "matchId";
    public static final String EXTRA_MATCH_DATE_UTC = "matchDateUtc";

    private MaterialToolbar tb;
    private TextView tvMatch, tvBatBowl;
    private MaterialAutoCompleteTextView acStriker, acNonStriker, acBowler;
    private TextInputLayout tilStriker, tilNonStriker, tilBowler;
    private MaterialButton btnStart;

    private String teamAName, teamBName, tossWinner, opted;
    private int oversLimit;
    private int playersPerTeam;

    private long matchDateUtc = 0L;
    private String matchId = "";

    private ArrayList<String> teamAPlayers, teamBPlayers;

    private String battingTeamName, bowlingTeamName;
    private ArrayList<String> battingPlayers, bowlingPlayers;
    private boolean teamABatting;

    // ✅ adapters that we can refresh
    private ArrayAdapter<String> strikerAdapter;
    private ArrayAdapter<String> nonStrikerAdapter;
    private ArrayAdapter<String> bowlerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_selection);

        bind();

        Intent in = getIntent();
        teamAName = in.getStringExtra(EXTRA_TEAM_A_NAME);
        teamBName = in.getStringExtra(EXTRA_TEAM_B_NAME);
        oversLimit = in.getIntExtra(EXTRA_OVERS_LIMIT, 10);
        playersPerTeam = in.getIntExtra(EXTRA_PLAYERS_PER_TEAM, 7);

        tossWinner = in.getStringExtra(EXTRA_TOSS_WINNER);
        opted = in.getStringExtra(EXTRA_OPTED);

        teamAPlayers = in.getStringArrayListExtra(EXTRA_TEAM_A_PLAYERS);
        teamBPlayers = in.getStringArrayListExtra(EXTRA_TEAM_B_PLAYERS);

        matchDateUtc = in.getLongExtra(EXTRA_MATCH_DATE_UTC, 0L);

        if (teamAPlayers == null) teamAPlayers = new ArrayList<>();
        if (teamBPlayers == null) teamBPlayers = new ArrayList<>();

        String aName = safeName(teamAName, "Team A");
        String bName = safeName(teamBName, "Team B");

        tvMatch.setText(aName + " vs " + bName);

        computeBatBowl(aName, bName);
        tvBatBowl.setText("Batting: " + battingTeamName + "  |  Bowling: " + bowlingTeamName);

        // ✅ Create adapters (DON'T share the same adapter between striker/nonStriker)
        strikerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>(battingPlayers));
        nonStrikerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>(battingPlayers));
        bowlerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>(bowlingPlayers));

        acStriker.setAdapter(strikerAdapter);
        acNonStriker.setAdapter(nonStrikerAdapter);
        acBowler.setAdapter(bowlerAdapter);

        acStriker.setThreshold(0);
        acNonStriker.setThreshold(0);
        acBowler.setThreshold(0);

        acStriker.setText("", false);
        acNonStriker.setText("", false);
        acBowler.setText("", false);

        acStriker.setOnClickListener(v -> acStriker.showDropDown());
        acNonStriker.setOnClickListener(v -> acNonStriker.showDropDown());
        acBowler.setOnClickListener(v -> acBowler.showDropDown());

        // ✅ When striker picked -> remove from non-striker list
        acStriker.setOnItemClickListener((p, v, pos, id) -> {
            acStriker.dismissDropDown();
            refreshDropdowns();
            validateLive();
        });

        // ✅ When non-striker picked -> remove from striker list (optional but professional)
        acNonStriker.setOnItemClickListener((p, v, pos, id) -> {
            acNonStriker.dismissDropDown();
            refreshDropdowns();
            validateLive();
        });

        // ✅ Bowler pick (just validate)
        acBowler.setOnItemClickListener((p, v, pos, id) -> {
            acBowler.dismissDropDown();
            validateLive();
        });

        tb.setNavigationOnClickListener(v -> finish());

        btnStart.setOnClickListener(v -> {
            if (!validateFinal()) return;

            createAndSaveInitialMatch();
            startScoring();
        });

        // ✅ initial refresh (keeps lists clean if user edits)
        refreshDropdowns();
    }

    // ✅ Key Fix: rebuild lists so striker isn't in non-striker dropdown (and vice versa)
    private void refreshDropdowns() {
        String s = text(acStriker);
        String n = text(acNonStriker);

        // Build striker list (exclude current non-striker)
        ArrayList<String> strikerList = new ArrayList<>();
        for (String name : battingPlayers) {
            if (n.isEmpty() || !name.equalsIgnoreCase(n)) strikerList.add(name);
        }

        // Build non-striker list (exclude current striker)
        ArrayList<String> nonStrikerList = new ArrayList<>();
        for (String name : battingPlayers) {
            if (s.isEmpty() || !name.equalsIgnoreCase(s)) nonStrikerList.add(name);
        }

        // Replace adapter data
        strikerAdapter.clear();
        strikerAdapter.addAll(strikerList);
        strikerAdapter.notifyDataSetChanged();

        nonStrikerAdapter.clear();
        nonStrikerAdapter.addAll(nonStrikerList);
        nonStrikerAdapter.notifyDataSetChanged();

        // If they became equal due to refresh, clear non-striker (safer UX)
        if (!s.isEmpty() && s.equalsIgnoreCase(n)) {
            acNonStriker.setText("", false);
        }
    }

    private void createAndSaveInitialMatch() {
        if (matchDateUtc <= 0) matchDateUtc = System.currentTimeMillis();
        if (matchId == null || matchId.trim().isEmpty()) {
            matchId = UUID.randomUUID().toString();
        }

        String title = safeName(teamAName, "Team A") + " vs " + safeName(teamBName, "Team B");

        JSONObject j = new JSONObject();
        try {
            j.put("matchId", matchId);
            j.put("matchDateUtc", matchDateUtc);
            j.put("isCompleted", false);

            j.put("teamA", safeName(teamAName, "Team A"));
            j.put("teamB", safeName(teamBName, "Team B"));
            j.put("oversLimit", oversLimit);
            j.put("playersPerTeam", playersPerTeam);

            j.put("tossWinner", tossWinner == null ? "" : tossWinner);
            j.put("tossDecision", opted == null ? "" : opted);

            j.put("innings", 1);

            j.put("teamABatting", teamABatting);
            j.put("striker", text(acStriker));
            j.put("nonStriker", text(acNonStriker));
            j.put("bowler", text(acBowler));

            j.put("teamAPlayers", new JSONArray(teamAPlayers));
            j.put("teamBPlayers", new JSONArray(teamBPlayers));

            j.put("totalRuns", 0);
            j.put("wickets", 0);
            j.put("overNumber", 0);
            j.put("ballInOver", 0);

            j.put("ballEvents", new JSONArray());
            j.put("batsmanStats", new JSONObject());
            j.put("bowlerStats", new JSONObject());

            j.put("innings1Snapshot", new JSONObject());
            j.put("innings2Snapshot", new JSONObject());

            j.put("undoStack", new JSONArray());

        } catch (Exception ignored) {}

        String matchJson = j.toString();

        MatchStore.upsert(
                this,
                matchId,
                title,
                matchDateUtc,
                MatchStore.STATUS_IN_PROGRESS,
                matchJson
        );

        MatchStore.saveInProgress(this, matchJson);
    }

    private void startScoring() {
        Intent i = new Intent(this, ScoringActivity.class);

        i.putExtra(EXTRA_MATCH_ID, matchId);
        i.putExtra(EXTRA_MATCH_DATE_UTC, matchDateUtc);

        i.putExtra(EXTRA_TEAM_A_NAME, teamAName);
        i.putExtra(EXTRA_TEAM_B_NAME, teamBName);
        i.putExtra(EXTRA_OVERS_LIMIT, oversLimit);
        i.putExtra(EXTRA_PLAYERS_PER_TEAM, playersPerTeam);
        i.putExtra(EXTRA_TOSS_WINNER, tossWinner);
        i.putExtra(EXTRA_OPTED, opted);

        i.putStringArrayListExtra(EXTRA_TEAM_A_PLAYERS, teamAPlayers);
        i.putStringArrayListExtra(EXTRA_TEAM_B_PLAYERS, teamBPlayers);

        i.putExtra(EXTRA_TEAM_A_BATTING, teamABatting);
        i.putExtra(EXTRA_STRIKER, text(acStriker));
        i.putExtra(EXTRA_NON_STRIKER, text(acNonStriker));
        i.putExtra(EXTRA_BOWLER, text(acBowler));

        startActivity(i);
        finish();
    }

    private void bind() {
        tb = findViewById(R.id.tbSelect);
        tvMatch = findViewById(R.id.tvMatch);
        tvBatBowl = findViewById(R.id.tvBatBowl);

        tilStriker = findViewById(R.id.tilStriker);
        tilNonStriker = findViewById(R.id.tilNonStriker);
        tilBowler = findViewById(R.id.tilBowler);

        acStriker = findViewById(R.id.acStriker);
        acNonStriker = findViewById(R.id.acNonStriker);
        acBowler = findViewById(R.id.acBowler);

        btnStart = findViewById(R.id.btnStartScoring);
    }

    private void validateLive() {
        tilStriker.setError(null);
        tilNonStriker.setError(null);
        tilBowler.setError(null);

        String s = text(acStriker);
        String n = text(acNonStriker);
        String b = text(acBowler);

        if (!s.isEmpty() && s.equalsIgnoreCase(n)) {
            tilNonStriker.setError("Must be different from striker");
            acNonStriker.setText("", false);
        }

        if (!b.isEmpty() && (b.equalsIgnoreCase(s) || b.equalsIgnoreCase(n))) {
            tilBowler.setError("Bowler cannot be a current batsman");
            acBowler.setText("", false);
        }
    }

    private boolean validateFinal() {
        tilStriker.setError(null);
        tilNonStriker.setError(null);
        tilBowler.setError(null);

        String s = text(acStriker);
        String n = text(acNonStriker);
        String b = text(acBowler);

        if (s.isEmpty()) { tilStriker.setError("Select striker"); return false; }
        if (n.isEmpty()) { tilNonStriker.setError("Select non-striker"); return false; }
        if (b.isEmpty()) { tilBowler.setError("Select bowler"); return false; }

        if (s.equalsIgnoreCase(n)) { tilNonStriker.setError("Must be different"); return false; }
        if (b.equalsIgnoreCase(s) || b.equalsIgnoreCase(n)) { tilBowler.setError("Bowler cannot be a batsman"); return false; }

        if (!battingPlayers.contains(s)) { tilStriker.setError("Pick from batting list"); return false; }
        if (!battingPlayers.contains(n)) { tilNonStriker.setError("Pick from batting list"); return false; }
        if (!bowlingPlayers.contains(b)) { tilBowler.setError("Pick from bowling list"); return false; }

        return true;
    }

    private void computeBatBowl(String aName, String bName) {
        boolean tossIsA = "A".equalsIgnoreCase(tossWinner);
        boolean optedBat = "BAT".equalsIgnoreCase(opted);

        boolean teamABatsFirst;
        if (tossIsA) teamABatsFirst = optedBat;
        else teamABatsFirst = !optedBat;

        teamABatting = teamABatsFirst;

        if (teamABatsFirst) {
            battingTeamName = aName;
            bowlingTeamName = bName;
            battingPlayers = teamAPlayers;
            bowlingPlayers = teamBPlayers;
        } else {
            battingTeamName = bName;
            bowlingTeamName = aName;
            battingPlayers = teamBPlayers;
            bowlingPlayers = teamAPlayers;
        }
    }

    private String safeName(String s, String fallback) {
        if (s == null) return fallback;
        s = s.trim();
        return s.isEmpty() ? fallback : s;
    }

    private String text(MaterialAutoCompleteTextView v) {
        if (v == null || v.getText() == null) return "";
        return v.getText().toString().trim();
    }

    @SuppressWarnings("unused")
    private String formatDate(long utcMillis) {
        if (utcMillis <= 0) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(new Date(utcMillis));
    }
}
