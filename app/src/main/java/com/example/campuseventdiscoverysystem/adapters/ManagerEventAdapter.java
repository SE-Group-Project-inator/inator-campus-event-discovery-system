package com.example.campuseventdiscoverysystem.adapters;

import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.campuseventdiscoverysystem.R;
import com.example.campuseventdiscoverysystem.models.Event;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.*;

public class ManagerEventAdapter extends
        RecyclerView.Adapter<ManagerEventAdapter.ViewHolder> {

    public interface OnAttendeesClickListener {
        void onAttendeesClick(Event event);
    }

    private final List<Event> events;
    private final OnAttendeesClickListener listener;

    public ManagerEventAdapter(List<Event> events, OnAttendeesClickListener listener) {
        this.events = events;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_manager_event, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Event event = events.get(position);

        h.tvTitle.setText(event.getTitle() != null ? event.getTitle() : "Untitled Event");
        h.tvVenue.setText(event.getVenue() != null ? event.getVenue() : "TBD");

        int registered = event.getRegisteredCount();
        int capacity = event.getCapacity();
        if (capacity > 0) {
            h.tvRegisteredCount.setText(registered + " / " + capacity + " registered");
        } else {
            h.tvRegisteredCount.setText(registered + " registered");
        }

        String status = event.getStatus();
        if (status != null) {
            switch (status) {
                case "active":
                    h.tvStatus.setText("Active");
                    h.tvStatus.setTextColor(h.itemView.getContext().getColor(R.color.green_accept));
                    break;
                case "rejected":
                    h.tvStatus.setText("Rejected");
                    h.tvStatus.setTextColor(h.itemView.getContext().getColor(R.color.red_decline));
                    break;
                default:
                    h.tvStatus.setText("Pending");
                    h.tvStatus.setTextColor(h.itemView.getContext().getColor(R.color.text_dark));
                    break;
            }
        }

        if (event.getDate() != null) {
            Date d = event.getDate().toDate();
            h.tvDay.setText(new SimpleDateFormat("dd", Locale.US).format(d));
            h.tvMonth.setText(new SimpleDateFormat("MMM", Locale.US)
                    .format(d).toUpperCase(Locale.US));
        } else {
            h.tvDay.setText("--");
            h.tvMonth.setText("---");
        }

        h.btnViewAttendees.setOnClickListener(v -> listener.onAttendeesClick(event));
    }

    @Override
    public int getItemCount() { return events.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDay, tvMonth, tvTitle, tvVenue, tvStatus, tvRegisteredCount;
        MaterialButton btnViewAttendees;

        public ViewHolder(@NonNull View v) {
            super(v);
            tvDay = v.findViewById(R.id.tvDay);
            tvMonth = v.findViewById(R.id.tvMonth);
            tvTitle = v.findViewById(R.id.tvEventTitle);
            tvVenue = v.findViewById(R.id.tvVenue);
            tvStatus = v.findViewById(R.id.tvStatus);
            tvRegisteredCount = v.findViewById(R.id.tvRegisteredCount);
            btnViewAttendees = v.findViewById(R.id.btnViewAttendees);
        }
    }
}