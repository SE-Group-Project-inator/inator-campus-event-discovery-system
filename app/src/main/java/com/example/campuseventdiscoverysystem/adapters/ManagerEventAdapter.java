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

        holder.tvEventTitle.setText(event.getTitle() != null ? event.getTitle() : "Untitled Event");

        String status = event.getStatus();
        if ("active".equals(status)) {
            holder.tvEventStatus.setText("Approved");
            holder.tvEventStatus.setBackgroundColor(Color.parseColor("#4CAF50"));
        } else if ("rejected".equals(status)) {
            holder.tvEventStatus.setText("Declined");
            holder.tvEventStatus.setBackgroundColor(Color.parseColor("#E53935"));
        } else {
            holder.tvEventStatus.setText("Pending");
            holder.tvEventStatus.setBackgroundColor(Color.parseColor("#FFB300"));
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
        TextView tvEventTitle, tvEventStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEventTitle = itemView.findViewById(R.id.tvEventTitle);
            tvEventStatus = itemView.findViewById(R.id.tvEventStatus);
        }
    }
}