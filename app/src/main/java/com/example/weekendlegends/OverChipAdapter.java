package com.example.weekendlegends;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Locale;

public class OverChipAdapter extends RecyclerView.Adapter<OverChipAdapter.VH> {

    private final ArrayList<BallItem> balls;

    public OverChipAdapter(ArrayList<BallItem> balls) {
        this.balls = (balls == null) ? new ArrayList<>() : balls;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_over_chip, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        BallItem bi = balls.get(pos);

        String label = (bi == null || bi.label == null) ? "" : bi.label.trim();
        boolean wicket = (bi != null) && bi.isWicket;

        h.tv.setText(label);

        // ✅ default style
        int stroke = Color.parseColor("#EAF2FF");
        int text = Color.parseColor("#EAF2FF");

        if (wicket || label.equalsIgnoreCase("W")) {
            stroke = Color.parseColor("#FF3B30");
            text = Color.parseColor("#FF3B30");
        } else if (isBoundary(label)) {
            stroke = Color.parseColor("#8BFF63");
            text = Color.parseColor("#8BFF63");
        } else if (isExtra(label)) {
            stroke = Color.parseColor("#FFD54F");
            text = Color.parseColor("#FFD54F");
        }

        h.card.setStrokeColor(stroke);
        h.tv.setTextColor(text);
    }

    private boolean isExtra(String label) {
        String s = label.toLowerCase(Locale.US);
        return s.contains("nb") || s.contains("wd") || s.contains("b");
    }

    // ✅ boundary even if "4Wd" or "6Nb"
    private boolean isBoundary(String label) {
        if (label == null) return false;
        String s = label.trim();
        if (s.startsWith("4") || s.startsWith("6")) return true;
        return s.equals("4") || s.equals("6");
    }

    @Override
    public int getItemCount() {
        return balls.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        MaterialCardView card;
        TextView tv;

        VH(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.cardBall);
            tv = itemView.findViewById(R.id.tvChip);
        }
    }
}
