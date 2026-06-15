package com.example.weekendlegends;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class BallAdapter extends RecyclerView.Adapter<BallAdapter.VH> {

    private final ArrayList<BallItem> balls;

    public BallAdapter(ArrayList<BallItem> balls) {
        this.balls = (balls == null) ? new ArrayList<>() : balls;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ball, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        BallItem it = balls.get(position);
        h.tv.setText(it.label == null ? "" : it.label);

        if (it.isWicket) {
            h.tv.setBackgroundResource(R.drawable.bg_ball_wicket);
        } else if (it.isSix) {
            // if you don't have this drawable, delete this line + the else-if block
            h.tv.setBackgroundResource(R.drawable.bg_ball_six);
        } else if (it.isFour) {
            // if you don't have this drawable, delete this line + the else-if block
            h.tv.setBackgroundResource(R.drawable.bg_ball_four);
        } else {
            // if you don't have this drawable, delete this line
            h.tv.setBackgroundResource(R.drawable.bg_ball_normal);
        }
    }

    @Override
    public int getItemCount() {
        return balls.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tv;
        VH(@NonNull View itemView) {
            super(itemView);
            tv = itemView.findViewById(R.id.tvBall);
        }
    }
}
