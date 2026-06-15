package com.example.weekendlegends;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class SimpleStringAdapter extends RecyclerView.Adapter<SimpleStringAdapter.VH> {

    private final ArrayList<String> list;

    public SimpleStringAdapter(ArrayList<String> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_simple_text, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        h.tv.setText(list.get(position));
    }

    @Override
    public int getItemCount() { return list == null ? 0 : list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tv;
        VH(@NonNull View itemView) {
            super(itemView);
            tv = itemView.findViewById(R.id.tv);
        }
    }
}
