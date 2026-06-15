package com.example.weekendlegends;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class OverAdapter extends RecyclerView.Adapter<OverAdapter.VH> {

    private final ArrayList<OverItem> items;

    public OverAdapter(ArrayList<OverItem> items) {
        this.items = (items == null) ? new ArrayList<>() : items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_over, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        OverItem it = items.get(position);

        h.tvOver.setText("Over " + it.overNo);
        h.tvBowler.setText("Bowler: " + (it.bowler == null ? "" : it.bowler));
        h.tvRuns.setText(it.totalRuns + " Runs");

        // ✅ wrap balls into grid (6 per row -> no horizontal scroll)
        h.rvBalls.setAdapter(new OverChipAdapter(it.balls));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvOver, tvBowler, tvRuns;
        RecyclerView rvBalls;

        VH(@NonNull View itemView) {
            super(itemView);

            tvOver = itemView.findViewById(R.id.tvOverNo);
            tvBowler = itemView.findViewById(R.id.tvOverBowler);
            tvRuns = itemView.findViewById(R.id.tvOverRuns);
            rvBalls = itemView.findViewById(R.id.rvBalls);

            // ✅ Grid: 6 balls per row, wraps to next line
            GridLayoutManager gm = new GridLayoutManager(itemView.getContext(), 6);
            rvBalls.setLayoutManager(gm);
            rvBalls.setNestedScrollingEnabled(false);
            rvBalls.setHasFixedSize(false);
            rvBalls.setOverScrollMode(View.OVER_SCROLL_NEVER);
        }
    }
}
