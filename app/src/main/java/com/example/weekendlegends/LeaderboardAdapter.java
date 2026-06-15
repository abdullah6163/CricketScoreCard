package com.example.weekendlegends;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.VH> {

    private final ArrayList<LeaderboardRow> list;
    private final boolean batting; // true=runs, false=wickets

    public LeaderboardAdapter(ArrayList<LeaderboardRow> list, boolean batting) {
        this.list = list;
        this.batting = batting;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard_row, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        LeaderboardRow r = list.get(position);

        h.tvPos.setText(String.format("%02d", position + 1));
        h.tvName.setText(r.name);

        h.tvValue.setText(String.valueOf(r.value));
        h.tvValueLabel.setText(batting ? "Runs" : "Wkts");

        // avatar
        String b64 = PlayerImageStore.getImageBase64(h.itemView.getContext(), r.name);
        Bitmap bmp = decode(b64);
        if (bmp != null) h.img.setImageBitmap(bmp);
        else h.img.setImageResource(R.drawable.ic_person);
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvPos, tvName, tvValue, tvValueLabel;
        ImageView img;

        VH(@NonNull View itemView) {
            super(itemView);
            tvPos = itemView.findViewById(R.id.tvPos);
            img = itemView.findViewById(R.id.imgAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            tvValue = itemView.findViewById(R.id.tvValue);
            tvValueLabel = itemView.findViewById(R.id.tvValueLabel);
        }
    }

    private Bitmap decode(String b64) {
        try {
            if (b64 == null || b64.trim().isEmpty()) return null;
            byte[] data = Base64.decode(b64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(data, 0, data.length);
        } catch (Exception e) {
            return null;
        }
    }
}
