package com.example.weekendlegends;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class LeaderboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_leaderboard);

        MaterialToolbar tb = findViewById(R.id.toolbar);
        tb.setNavigationOnClickListener(v -> finish());
        tb.setTitle("Leaderboard");

        ViewPager2 pager = findViewById(R.id.pager);
        TabLayout tabs = findViewById(R.id.tabs);

        LeaderboardPagerAdapter adapter = new LeaderboardPagerAdapter(this);
        pager.setAdapter(adapter);

        new TabLayoutMediator(tabs, pager, (tab, pos) -> {
            if (pos == 0) tab.setText("Batting");
            else tab.setText("Bowling");
        }).attach();
    }
}
