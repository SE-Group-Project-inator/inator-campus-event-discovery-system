package com.example.campuseventdiscoverysystem.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.campuseventdiscoverysystem.R;
import com.example.campuseventdiscoverysystem.models.HistoryItem;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private List<HistoryItem> historyList;

    public HistoryAdapter(List<HistoryItem> historyList) {
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        HistoryItem item = historyList.get(position);
        holder.tvTitle.setText(item.getTitle());
        holder.tvDay.setText(item.getDay());
        holder.tvMonth.setText(item.getMonth());
        holder.tvStatus.setText(item.getStatus());

        // Dynamic Status Pill Coloring
        if (item.getStatus().equalsIgnoreCase("Attended")) {
            holder.cardStatus.setCardBackgroundColor(Color.parseColor("#A7F3D0")); // Light Green
            holder.tvStatus.setTextColor(Color.parseColor("#065F46")); // Dark Green
        } else if (item.getStatus().equalsIgnoreCase("Recap")) {
            holder.cardStatus.setCardBackgroundColor(Color.parseColor("#FBBF24")); // Orange
            holder.tvStatus.setTextColor(Color.parseColor("#92400E")); // Dark Orange
        } else {
            holder.cardStatus.setCardBackgroundColor(Color.parseColor("#FECACA")); // Red
            holder.tvStatus.setTextColor(Color.parseColor("#991B1B")); // Dark Red
        }
    }

    @Override
    public int getItemCount() { return historyList.size(); }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDay, tvMonth, tvStatus;
        MaterialCardView cardStatus;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvHistoryEventTitle);
            tvDay = itemView.findViewById(R.id.tvDay);
            tvMonth = itemView.findViewById(R.id.tvMonth);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            cardStatus = itemView.findViewById(R.id.cardStatus);
        }
    }
}