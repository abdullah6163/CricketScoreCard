package com.example.weekendlegends;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import org.json.JSONObject;

public class ScorecardPagerAdapter extends FragmentStateAdapter {

    private final JSONObject match;

    public ScorecardPagerAdapter(@NonNull FragmentActivity fa, JSONObject match) {
        super(fa);
        this.match = match == null ? new JSONObject() : match;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) return ScoreboardFragment.newInstance(match.toString());
        return OversFragment.newInstance(match.toString());
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
