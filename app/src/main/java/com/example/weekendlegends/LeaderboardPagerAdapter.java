package com.example.weekendlegends;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class LeaderboardPagerAdapter extends FragmentStateAdapter {

    public LeaderboardPagerAdapter(@NonNull FragmentActivity fa) {
        super(fa);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) return new LeaderboardBattingFragment();
        return new LeaderboardBowlingFragment();
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
