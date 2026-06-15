package com.example.weekendlegends;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;

public class HomeActivity extends AppCompatActivity {

    private static final String PREFS = "weekend_legends_prefs";
    private static final String KEY_INPROGRESS = "match_in_progress_json";

    private final ArrayList<Match> matches = new ArrayList<>();
    private MatchAdapter adapter;

    private TextView tvEmpty;

    private DrawerLayout drawerLayout;
    private NavigationView navView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        LottieAnimationView lottie = findViewById(R.id.lottieHome);
        if (lottie != null) lottie.playAnimation();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawerLayout);
        navView = findViewById(R.id.navView);

        // hamburger
        toolbar.setNavigationOnClickListener(v -> {
            if (drawerLayout != null) drawerLayout.openDrawer(GravityCompat.START);
        });

        // drawer navigation
        navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_players) {
                startActivity(new Intent(this, PlayerListActivity.class));
            } else if (id == R.id.nav_leaderboard) {
                startActivity(new Intent(this, LeaderboardActivity.class));
            } else if (id == R.id.nav_teams) {
                startActivity(new Intent(this, TeamsActivity.class));
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        tvEmpty = findViewById(R.id.tvEmpty);

        RecyclerView rv = findViewById(R.id.rvMatches);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new MatchAdapter(matches, new MatchAdapter.Listener() {
            @Override public void onOpenMatch(Match match) { openMatch(match); }
            @Override public void onExportPdf(Match match) { exportMatchPdf(match); }
            @Override public void onDelete(Match match) { confirmArchive(match); }
        });

        rv.setAdapter(adapter);

        MaterialButton btnCreate = findViewById(R.id.btnCreateMatch);
        btnCreate.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, NewMatchActivity.class))
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMatches();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }
        super.onBackPressed();
    }

    // ========================= LOAD MATCHES =========================

    private void loadMatches() {
        matches.clear();

        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);

        // 1️⃣ In-progress (always visible)
        String raw = sp.getString(KEY_INPROGRESS, "");
        if (raw != null && !raw.trim().isEmpty()) {
            try {
                JSONObject j = new JSONObject(raw);
                boolean done = j.optBoolean("isCompleted", false);

                if (!done) {
                    String id = j.optString("matchId", "");
                    String title = j.optString("teamA", "Team A")
                            + " vs " + j.optString("teamB", "Team B");
                    long dateUtc = j.optLong("matchDateUtc", 0L);

                    matches.add(new Match(
                            id,
                            title,
                            formatDateTime(dateUtc),
                            dateUtc,
                            MatchStore.STATUS_IN_PROGRESS,
                            raw
                    ));
                }
            } catch (Exception ignored) {}
        }

        // 2️⃣ Completed history (skip archived)
        for (JSONObject o : MatchStore.load(this)) {
            if (o == null) continue;

            // ✅ IMPORTANT: skip archived matches
            if (MatchStore.isArchived(o)) continue;

            String status = o.optString("status", "");
            if (!MatchStore.STATUS_COMPLETED.equalsIgnoreCase(status)) continue;

            String id = o.optString("id", "");
            String title = o.optString("title", "Match");
            long dateUtc = o.optLong("dateUtc", 0L);

            String fullJson = MatchStore.getMatchJsonById(this, id);
            if (fullJson == null || fullJson.trim().isEmpty()) fullJson = "{}";

            matches.add(new Match(
                    id,
                    title,
                    formatDateTime(dateUtc),
                    dateUtc,
                    status,
                    fullJson
            ));
        }

        adapter.notifyDataSetChanged();
        refreshEmptyState();
    }

    // ========================= OPEN MATCH =========================

    private void openMatch(Match match) {
        if (match == null) return;

        if (MatchStore.STATUS_COMPLETED.equalsIgnoreCase(match.status)) {
            Intent i = new Intent(this, ScorecardActivity.class);
            i.putExtra(ScoringActivity.EXTRA_MATCH_ID, match.id);
            startActivity(i);
        } else {
            Intent i = new Intent(this, ScoringActivity.class);
            i.putExtra(ScoringActivity.EXTRA_MATCH_ID, match.id);
            startActivity(i);
        }
    }

    // ========================= ARCHIVE (SOFT DELETE) =========================

    private void confirmArchive(Match match) {
        if (match == null) return;

        new MaterialAlertDialogBuilder(this)
                .setTitle("Remove match from Home?")
                .setMessage(
                        "This will hide the match from Home.\n\n" +
                                "✔ Leaderboard stats\n" +
                                "✔ Player career\n" +
                                "✔ Team stats\n\n" +
                                "will remain unchanged."
                )
                .setPositiveButton("Remove", (d, w) -> {

                    // ✅ archive only
                    MatchStore.archive(this, match.id);

                    // clear in-progress if same match
                    SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
                    String raw = sp.getString(KEY_INPROGRESS, "");
                    if (raw != null && !raw.trim().isEmpty()) {
                        try {
                            JSONObject j = new JSONObject(raw);
                            if (match.id.equals(j.optString("matchId", ""))) {
                                sp.edit().remove(KEY_INPROGRESS).apply();
                            }
                        } catch (Exception ignored) {}
                    }

                    loadMatches();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ========================= EXPORT =========================

    private void exportMatchPdf(Match match) {
        if (match == null) return;

        String json = match.matchJson;
        if (json == null || json.trim().isEmpty() || "{}".equals(json.trim())) {
            json = MatchStore.getMatchJsonById(this, match.id);
        }
        if (json == null || json.trim().isEmpty()) return;

        ScorecardPdfExporter.exportToDownloadsAndOpen(
                this,
                match.title,
                json
        );
    }

    // ========================= UTILS =========================

    private String formatDateTime(long millis) {
        if (millis <= 0) return "";
        SimpleDateFormat sdf =
                new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        return sdf.format(new Date(millis));
    }

    private void refreshEmptyState() {
        if (tvEmpty != null) {
            tvEmpty.setVisibility(matches.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }
}
