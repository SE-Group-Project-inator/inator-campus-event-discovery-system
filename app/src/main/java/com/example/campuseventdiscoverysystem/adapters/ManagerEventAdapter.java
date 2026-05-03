package com.example.campuseventdiscoverysystem.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.campuseventdiscoverysystem.R;
import com.example.campuseventdiscoverysystem.models.Event;
import com.google.android.material.card.MaterialCardView;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ManagerEventAdapter extends RecyclerView.Adapter<ManagerEventAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(String eventId);
    }

    private final List<Event> eventList;
    private final OnItemClickListener listener;
    private boolean isEditMode = false;
    private boolean showStatusBadge = true;

    public void setShowStatusBadge(boolean show) {
        this.showStatusBadge = show;
    }

    public ManagerEventAdapter(List<Event> eventList, OnItemClickListener listener) {
        this.eventList = eventList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_manager_event, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event event = eventList.get(position);

        holder.tvEventTitle.setText(event.getTitle() != null ? event.getTitle() : "Untitled");

        // Handle dynamic "Tap to..." text
        if (showStatusBadge) {
            holder.tvSubtitle.setText("Tap to manage");
            holder.cardStatus.setVisibility(View.VISIBLE);
        } else {
            holder.tvSubtitle.setText("Tap to view");
            holder.cardStatus.setVisibility(View.GONE);
        }

        // Handle Status Colors
        String status = event.getStatus();
        if ("active".equals(status)) {
            // Set text on the TextView
            holder.tvStatus.setText("Approved");
            // Set color on the CardView
            holder.cardStatus.setCardBackgroundColor(Color.parseColor("#4CAF50"));
        } else if ("rejected".equals(status)) {
            holder.tvStatus.setText("Declined");
            holder.cardStatus.setCardBackgroundColor(Color.parseColor("#E53935"));
        } else {
            holder.tvStatus.setText("Pending");
            holder.cardStatus.setCardBackgroundColor(Color.parseColor("#FFB300"));
        }

        // Handle Date Display
        if (event.getDate() != null) {
            java.util.Date date = event.getDate().toDate();
            holder.tvMonth.setText(new SimpleDateFormat("MMM", Locale.US).format(date).toUpperCase());
            holder.tvDay.setText(new SimpleDateFormat("dd", Locale.US).format(date));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(event.getId());
        });
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEventTitle, tvStatus, tvMonth, tvDay, tvSubtitle;
        MaterialCardView cardStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEventTitle = itemView.findViewById(R.id.tvEventTitle);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvMonth = itemView.findViewById(R.id.tvMonth);
            tvDay = itemView.findViewById(R.id.tvDay);
            cardStatus = itemView.findViewById(R.id.cardStatus);
        }
    }
}