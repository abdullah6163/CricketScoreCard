package com.example.weekendlegends;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class BowlerAdapter extends RecyclerView.Adapter<BowlerAdapter.VH> {

    private final List<BowlerRow> list;

    public BowlerAdapter(List<BowlerRow> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bowler, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        BowlerRow r = list.get(position);
        h.tvName.setText(r.name);
        h.tvO.setText(r.o);
        h.tvM.setText(String.valueOf(r.m));
        h.tvR.setText(String.valueOf(r.r));
        h.tvW.setText(String.valueOf(r.w));
        h.tvER.setText(String.format(Locale.US, "%.2f", r.econ));
    }

    @Override public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvO, tvM, tvR, tvW, tvER;
        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvO = itemView.findViewById(R.id.tvO);
            tvM = itemView.findViewById(R.id.tvM);
            tvR = itemView.findViewById(R.id.tvR);
            tvW = itemView.findViewById(R.id.tvW);
            tvER = itemView.findViewById(R.id.tvER);
        }
    }
}
