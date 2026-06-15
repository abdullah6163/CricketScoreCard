package com.example.weekendlegends;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;

public class TeamPlayersActivity extends AppCompatActivity {

    private TextView tvTitle;
    private TextView tvLastMatchLabel;
    private TextView tvEmptyLast;
    private TextView tvEmptyAll;

    private final ArrayList<String> lastSquad = new ArrayList<>();
    private final ArrayList<TeamStore.PlayerCount> allTime = new ArrayList<>();

    private TeamPlayersAdapter lastAdapter;
    private TeamPlayersAllTimeAdapter allAdapter;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_team_players);

        String team = getIntent().getStringExtra("team");
        if (team == null) team = "";

        MaterialToolbar tb = findViewById(R.id.toolbar);
        tb.setNavigationOnClickListener(v -> finish());
        tb.setTitle("Team");

        tvTitle = findViewById(R.id.tvTitle);
        tvLastMatchLabel = findViewById(R.id.tvLastMatchLabel);
        tvEmptyLast = findViewById(R.id.tvEmptyLast);
        tvEmptyAll = findViewById(R.id.tvEmptyAll);

        tvTitle.setText(team);

        RecyclerView rvLast = findViewById(R.id.rvLastMatch);
        RecyclerView rvAll = findViewById(R.id.rvAllTime);

        rvLast.setLayoutManager(new LinearLayoutManager(this));
        rvAll.setLayoutManager(new LinearLayoutManager(this));

        lastAdapter = new TeamPlayersAdapter(lastSquad, name -> openCareer(name));
        allAdapter = new TeamPlayersAllTimeAdapter(allTime, name -> openCareer(name));

        rvLast.setAdapter(lastAdapter);
        rvAll.setAdapter(allAdapter);

        load(team);
    }

    private void openCareer(String playerName) {
        Intent i = new Intent(this, PlayerCareerActivity.class);
        i.putExtra("player", playerName);
        startActivity(i);
    }

    private void load(String team) {
        TeamStore.ResultTeamPlayers r = TeamStore.loadTeamPlayers(this, team);

        lastSquad.clear();
        lastSquad.addAll(r.lastMatchSquad);

        allTime.clear();
        allTime.addAll(r.allTimeSquad);

        if (lastAdapter != null) lastAdapter.notifyDataSetChanged();
        if (allAdapter != null) allAdapter.notifyDataSetChanged();

        tvEmptyLast.setVisibility(lastSquad.isEmpty() ? View.VISIBLE : View.GONE);
        tvEmptyAll.setVisibility(allTime.isEmpty() ? View.VISIBLE : View.GONE);

        // label
        if (tvLastMatchLabel != null) {
            tvLastMatchLabel.setText("Last match squad");
        }
    }
}
