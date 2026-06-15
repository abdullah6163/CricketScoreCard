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

public class TeamsActivity extends AppCompatActivity {

    private final ArrayList<TeamStore.TeamStat> list = new ArrayList<>();
    private TeamsStatsAdapter adapter;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_teams);

        MaterialToolbar tb = findViewById(R.id.toolbar);
        tb.setNavigationOnClickListener(v -> finish());
        tb.setTitle("Teams");

        tvEmpty = findViewById(R.id.tvEmpty);

        RecyclerView rv = findViewById(R.id.rvTeams);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new TeamsStatsAdapter(list, teamName -> {
            Intent i = new Intent(TeamsActivity.this, TeamPlayersActivity.class);
            i.putExtra("team", teamName);
            startActivity(i);
        });

        rv.setAdapter(adapter);
        load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        list.clear();
        list.addAll(TeamStore.loadTeamStats(this));

        if (adapter != null) adapter.notifyDataSetChanged();

        boolean empty = list.isEmpty();
        if (tvEmpty != null) tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
    }
}
