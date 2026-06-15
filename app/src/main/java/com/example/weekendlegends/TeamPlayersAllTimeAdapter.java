package com.example.weekendlegends;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class TeamPlayersAllTimeAdapter extends RecyclerView.Adapter<TeamPlayersAllTimeAdapter.VH> {

    public interface Listener { void onClick(String playerName); }

    private final ArrayList<TeamStore.PlayerCount> list;
    private final Listener listener;

    public TeamPlayersAllTimeAdapter(ArrayList<TeamStore.PlayerCount> list, Listener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_team_player_count, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        TeamStore.PlayerCount pc = list.get(position);
        h.tvName.setText(pc.name);
        h.tvSub.setText("Matches: " + pc.matchesPlayed);
        h.itemView.setOnClickListener(v -> { if (listener != null) listener.onClick(pc.name); });
    }

    @Override
    public int getItemCount() { return list == null ? 0 : list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvSub;
        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvSub = itemView.findViewById(R.id.tvSub);
        }
    }
}
