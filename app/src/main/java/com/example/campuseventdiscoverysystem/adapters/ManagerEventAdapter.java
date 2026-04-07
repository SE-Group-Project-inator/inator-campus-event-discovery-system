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

import java.util.List;

// WILL EDIT THIS CLASS

public class ManagerEventAdapter extends RecyclerView.Adapter<ManagerEventAdapter.ViewHolder> {

    // Interface to handle clicks so clicking opens the Edit Event screen
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

    public void setEditMode(boolean editMode) {
        this.isEditMode = editMode;
        notifyDataSetChanged(); // This forces the list to redraw with the new colors!
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event event = eventList.get(position);

        holder.tvEventTitle.setText(event.getTitle() != null ? event.getTitle() : "Untitled Event");

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

        if (showStatusBadge) {
            holder.cardStatus.setVisibility(View.VISIBLE);
        } else {
            holder.cardStatus.setVisibility(View.GONE);
        }

        // Optional but recommended: Bind your dates to the UI since you have the views!
        if (event.getDate() != null) {
            java.util.Date date = event.getDate().toDate();
            holder.tvMonth.setText(new java.text.SimpleDateFormat("MMM", java.util.Locale.US).format(date).toUpperCase());
            holder.tvDay.setText(new java.text.SimpleDateFormat("dd", java.util.Locale.US).format(date));
        }

        if (holder.itemView instanceof com.google.android.material.card.MaterialCardView) {
            com.google.android.material.card.MaterialCardView rootCard =
                    (com.google.android.material.card.MaterialCardView) holder.itemView;

            if (isEditMode) {
                // Light grey background when in edit mode
                rootCard.setCardBackgroundColor(Color.parseColor("#E0E0E0"));
            } else {
                // Standard white background
                rootCard.setCardBackgroundColor(Color.parseColor("#FFFFFF"));
            }
        }


        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(event.getId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEventTitle, tvStatus, tvMonth, tvDay;
        com.google.android.material.card.MaterialCardView cardStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEventTitle = itemView.findViewById(R.id.tvHistoryEventTitle);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvMonth = itemView.findViewById(R.id.tvMonth);
            tvDay = itemView.findViewById(R.id.tvDay);
            cardStatus = itemView.findViewById(R.id.cardStatus);
        }
    }
}