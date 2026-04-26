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

    private final List<HistoryItem> historyList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(HistoryItem item);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public HistoryAdapter(List<HistoryItem> historyList) {
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        HistoryItem item = historyList.get(position);

        holder.tvTitle.setText(item.getTitle());
        holder.tvDay.setText(item.getDay());
        holder.tvMonth.setText(item.getMonth());
        holder.tvStatus.setText(item.getStatus());

        // Status badge colour
        if (item.getStatus().equalsIgnoreCase("Attended")) {
            holder.cardStatus.setCardBackgroundColor(Color.parseColor("#A7F3D0")); // light green
            holder.tvStatus.setTextColor(Color.parseColor("#065F46"));             // dark green
        } else if (item.getStatus().equalsIgnoreCase("Recap")) {
            holder.cardStatus.setCardBackgroundColor(Color.parseColor("#FBBF24")); // amber
            holder.tvStatus.setTextColor(Color.parseColor("#92400E"));             // dark amber
        } else {
            holder.cardStatus.setCardBackgroundColor(Color.parseColor("#FECACA")); // light red
            holder.tvStatus.setTextColor(Color.parseColor("#991B1B"));             // dark red
        }

        // Forward tap to listener set in EventHistoryActivity
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDay, tvMonth, tvStatus;
        MaterialCardView cardStatus;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle    = itemView.findViewById(R.id.tvHistoryEventTitle);
            tvDay      = itemView.findViewById(R.id.tvDay);
            tvMonth    = itemView.findViewById(R.id.tvMonth);
            tvStatus   = itemView.findViewById(R.id.tvStatus);
            cardStatus = itemView.findViewById(R.id.cardStatus);
        }
    }
}