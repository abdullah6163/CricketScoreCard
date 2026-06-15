package com.example.weekendlegends;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class TeamPlayersAdapter extends RecyclerView.Adapter<TeamPlayersAdapter.VH> {

    public interface Listener { void onClick(String playerName); }

    private final ArrayList<String> list;
    private final Listener listener;

    public TeamPlayersAdapter(ArrayList<String> list, Listener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_team_player, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        String name = list.get(position);
        h.tvName.setText(name);
        h.itemView.setOnClickListener(v -> { if (listener != null) listener.onClick(name); });
    }

    @Override
    public int getItemCount() { return list == null ? 0 : list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName;
        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
        }
    }
}
