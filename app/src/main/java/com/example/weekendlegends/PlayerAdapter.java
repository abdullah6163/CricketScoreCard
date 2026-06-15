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

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

public class PlayerAdapter extends RecyclerView.Adapter<PlayerAdapter.VH> {

    public interface Listener {
        void onEditImage(String playerName);
        void onViewStats(String playerName);
        void onDeletePlayer(String playerName);
    }

    private final ArrayList<String> list;
    private final Listener listener;

    public PlayerAdapter(ArrayList<String> list, Listener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_player_grid, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        String name = list.get(position);
        h.tvName.setText(name);

        // avatar
        String b64 = PlayerImageStore.getImageBase64(h.itemView.getContext(), name);
        Bitmap bmp = decode(b64);
        if (bmp != null) h.imgAvatar.setImageBitmap(bmp);
        else h.imgAvatar.setImageResource(R.drawable.ic_person);

        h.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditImage(name);
        });

        h.btnViewStats.setOnClickListener(v -> {
            if (listener != null) listener.onViewStats(name);
        });

        h.btnRemovePlayer.setOnClickListener(v -> {
            if (listener != null) listener.onDeletePlayer(name);
        });
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView imgAvatar, btnEdit;
        TextView tvName;
        MaterialButton btnViewStats, btnRemovePlayer;

        VH(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            tvName = itemView.findViewById(R.id.tvName);
            btnViewStats = itemView.findViewById(R.id.btnViewStats);
            btnRemovePlayer = itemView.findViewById(R.id.btnRemovePlayer);
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
