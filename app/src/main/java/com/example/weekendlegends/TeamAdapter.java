package com.example.weekendlegends;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class TeamAdapter extends RecyclerView.Adapter<TeamAdapter.VH> {

    public interface Listener {
        void onOpenTeam(String teamName);
    }

    private final ArrayList<TeamStore.TeamStat> list;
    private final Listener listener;

    public TeamAdapter(ArrayList<TeamStore.TeamStat> list, Listener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_team_row, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        TeamStore.TeamStat r = list.get(position);

        h.tvTeam.setText(r.team);

        // ✅ show Matches / Wins / Losses
        h.tvSub.setText("Matches: " + r.matches + "   Wins: " + r.wins + "   Losses: " + r.losses);

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onOpenTeam(r.team);
        });
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTeam, tvSub;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTeam = itemView.findViewById(R.id.tvTeam);
            tvSub = itemView.findViewById(R.id.tvSub);
        }
    }
}
