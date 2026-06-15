package com.example.weekendlegends;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class TeamsStatsAdapter extends RecyclerView.Adapter<TeamsStatsAdapter.VH> {

    public interface Listener {
        void onTeamClick(String teamName);
    }

    private final ArrayList<TeamStore.TeamStat> list;
    private final Listener listener;

    public TeamsStatsAdapter(ArrayList<TeamStore.TeamStat> list, Listener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_team_stat, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        TeamStore.TeamStat s = list.get(position);

        h.tvTeam.setText(s.team);
        h.tvMatches.setText(String.valueOf(s.matches));
        h.tvWins.setText(String.valueOf(s.wins));
        h.tvLosses.setText(String.valueOf(s.losses));

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTeamClick(s.team);
        });
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTeam, tvMatches, tvWins, tvLosses;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTeam = itemView.findViewById(R.id.tvTeam);
            tvMatches = itemView.findViewById(R.id.tvMatches);
            tvWins = itemView.findViewById(R.id.tvWins);
            tvLosses = itemView.findViewById(R.id.tvLosses);
        }
    }
}
