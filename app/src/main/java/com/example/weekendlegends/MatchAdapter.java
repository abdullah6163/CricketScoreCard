package com.example.weekendlegends;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;

public class MatchAdapter extends RecyclerView.Adapter<MatchAdapter.VH> {

    public interface Listener {
        void onOpenMatch(Match match);     // Continue or View
        void onExportPdf(Match match);
        void onDelete(Match match);
    }

    private final ArrayList<Match> matches;
    private final Listener listener;

    public MatchAdapter(ArrayList<Match> matches, Listener listener) {
        this.matches = matches;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_match, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Match m = matches.get(position);

        h.tvTitle.setText(m.title);
        h.tvDate.setText(m.dateText);

        boolean completed = MatchStore.STATUS_COMPLETED.equalsIgnoreCase(m.status);

        if (completed) {
            // Blue + tick
            h.statusBox.setCardBackgroundColor(0xFF00E5FF);
            h.tvStatusText.setText("completed");
            h.tvStatusText.setTextColor(0xFF070B16);
            h.ivStatusIcon.setImageResource(R.drawable.ic_check_24);
            h.ivStatusIcon.setColorFilter(0xFF070B16);
        } else {
            // Green + pause
            h.statusBox.setCardBackgroundColor(0xFF7CFF6B);
            h.tvStatusText.setText("in progress");
            h.tvStatusText.setTextColor(0xFF070B16);
            h.ivStatusIcon.setImageResource(R.drawable.ic_pause_24);
            h.ivStatusIcon.setColorFilter(0xFF070B16);
        }

        // Tap status box => open match (continue or view)
        h.statusBox.setOnClickListener(v -> {
            if (listener != null) listener.onOpenMatch(m);
        });

        // PDF
        h.btnPdf.setOnClickListener(v -> {
            if (listener != null) listener.onExportPdf(m);
        });

        // Delete
        h.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(m);
        });

        // (Optional) Tap anywhere on card opens too
        h.cardRoot.setOnClickListener(v -> {
            if (listener != null) listener.onOpenMatch(m);
        });
    }

    @Override
    public int getItemCount() { return matches.size(); }

    static class VH extends RecyclerView.ViewHolder {
        MaterialCardView cardRoot, statusBox;
        TextView tvTitle, tvDate, tvStatusText;
        ImageView ivStatusIcon, btnPdf, btnDelete;

        VH(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.cardRoot);
            statusBox = itemView.findViewById(R.id.statusBox);

            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDate = itemView.findViewById(R.id.tvDate);

            tvStatusText = itemView.findViewById(R.id.tvStatusText);
            ivStatusIcon = itemView.findViewById(R.id.ivStatusIcon);

            btnPdf = itemView.findViewById(R.id.btnPdf);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
