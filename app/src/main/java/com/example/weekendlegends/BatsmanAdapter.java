package com.example.weekendlegends;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class BatsmanAdapter extends RecyclerView.Adapter<BatsmanAdapter.VH> {

    private final List<BatsmanRow> list;

    public BatsmanAdapter(List<BatsmanRow> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_batsman, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        BatsmanRow r = list.get(position);

        h.tvName.setText((r.isStriker ? "* " : "") + r.name);
        h.tvStatus.setText(r.status);
        h.tvR.setText(String.valueOf(r.r));
        h.tvB.setText(String.valueOf(r.b));
        h.tv4.setText(String.valueOf(r.f4));
        h.tv6.setText(String.valueOf(r.f6));
        h.tvSR.setText(String.format(Locale.US, "%.1f", r.sr));
    }

    @Override public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvStatus, tvR, tvB, tv4, tv6, tvSR;
        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvR = itemView.findViewById(R.id.tvR);
            tvB = itemView.findViewById(R.id.tvB);
            tv4 = itemView.findViewById(R.id.tv4);
            tv6 = itemView.findViewById(R.id.tv6);
            tvSR = itemView.findViewById(R.id.tvSR);
        }
    }
}
