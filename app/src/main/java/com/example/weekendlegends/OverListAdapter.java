package com.example.weekendlegends;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class OverListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final ArrayList<OverListItem> items;

    public OverListAdapter(ArrayList<OverListItem> items) {
        this.items = items;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        if (viewType == OverListItem.TYPE_HEADER) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_innings_header, parent, false);
            return new HeaderVH(v);
        } else {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_over, parent, false);
            return new OverVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        OverListItem item = items.get(position);

        if (holder instanceof HeaderVH) {
            ((HeaderVH) holder).tvHeader.setText(item.headerText);
            return;
        }

        OverItem oi = item.overItem;
        OverVH h = (OverVH) holder;

        h.tvOver.setText("Over " + oi.overNo);
        h.tvRuns.setText(oi.totalRuns + " Runs");

        if (oi.bowler == null || oi.bowler.trim().isEmpty()) {
            h.tvBowler.setText("");
        } else {
            h.tvBowler.setText("Bowler: " + oi.bowler);
        }

        h.rvBalls.setLayoutManager(
                new LinearLayoutManager(h.itemView.getContext(),
                        RecyclerView.HORIZONTAL, false)
        );
        h.rvBalls.setNestedScrollingEnabled(false);
        h.rvBalls.setAdapter(new BallAdapter(oi.balls));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ---------------- ViewHolders ----------------

    static class HeaderVH extends RecyclerView.ViewHolder {
        TextView tvHeader;
        HeaderVH(@NonNull View itemView) {
            super(itemView);
            tvHeader = itemView.findViewById(R.id.tvHeader);
        }
    }

    static class OverVH extends RecyclerView.ViewHolder {
        TextView tvOver, tvBowler, tvRuns;
        RecyclerView rvBalls;

        OverVH(@NonNull View itemView) {
            super(itemView);
            tvOver = itemView.findViewById(R.id.tvOverNo);
            tvBowler = itemView.findViewById(R.id.tvOverBowler);
            tvRuns = itemView.findViewById(R.id.tvOverRuns);
            rvBalls = itemView.findViewById(R.id.rvBalls);
        }
    }
}
