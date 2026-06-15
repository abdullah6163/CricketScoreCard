package com.example.weekendlegends;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.json.JSONObject;

public class ScorecardActivity extends AppCompatActivity {

    private MaterialToolbar tb;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    private JSONObject match = new JSONObject();
    private boolean goHomeOnBack = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scorecard);

        tb = findViewById(R.id.tbScorecard);
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        goHomeOnBack = getIntent().getBooleanExtra(ScoringActivity.EXTRA_GO_HOME_ON_BACK, false);

        tb.setNavigationOnClickListener(v -> handleBack());
        tb.getMenu().clear();

        // 1) Primary: load by matchId
        String matchId = getIntent().getStringExtra(ScoringActivity.EXTRA_MATCH_ID);
        if (matchId != null && !matchId.trim().isEmpty()) {
            String raw = MatchStore.getMatchJsonById(this, matchId);
            if (raw != null && !raw.trim().isEmpty()) {
                try { match = new JSONObject(raw); } catch (Exception ignored) { match = new JSONObject(); }
            }
        }

        // 2) Fallback: direct JSON
        if (match.length() == 0) {
            String json = getIntent().getStringExtra(ScoringActivity.EXTRA_MATCH_JSON);
            try { match = new JSONObject(json == null ? "{}" : json); }
            catch (Exception ignored) { match = new JSONObject(); }
        }

        String teamA = match.optString("teamA", "Team A");
        String teamB = match.optString("teamB", "Team B");

        // Title stays


        // Custom 2-line subtitle
        TextView tvTeamA = tb.findViewById(R.id.tvTeamA);
        TextView tvTeamB = tb.findViewById(R.id.tvTeamB);
        if (tvTeamA != null) tvTeamA.setText(teamA);
        if (tvTeamB != null) tvTeamB.setText("vs " + teamB);

        ScorecardPagerAdapter adapter = new ScorecardPagerAdapter(this, match);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(position == 0 ? "Scoreboard" : "Overs");
        }).attach();

        viewPager.setCurrentItem(0, false);
    }

    private void handleBack() {
        if (goHomeOnBack) {
            Intent h = new Intent(this, HomeActivity.class);
            h.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(h);
            finish();
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        handleBack();
    }
}
