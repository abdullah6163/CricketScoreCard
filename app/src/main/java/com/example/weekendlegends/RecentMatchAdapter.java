package com.example.weekendlegends;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class RecentMatchAdapter extends RecyclerView.Adapter<RecentMatchAdapter.VH> {

    private final ArrayList<PlayerCareerActivity.RecentRow> list;

    public RecentMatchAdapter(ArrayList<PlayerCareerActivity.RecentRow> list) {
        this.list = list == null ? new ArrayList<>() : list;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_match, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        PlayerCareerActivity.RecentRow r = list.get(position);

        h.tvTitle.setText(r.title);
        h.tvDate.setText(r.dateText);
        h.tvRole.setText(r.roleLine);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate, tvRole;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDate  = itemView.findViewById(R.id.tvDate);
            tvRole  = itemView.findViewById(R.id.tvRoleLine);
        }
    }
}
