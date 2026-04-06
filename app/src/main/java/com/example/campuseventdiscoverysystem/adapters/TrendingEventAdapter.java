package com.example.campuseventdiscoverysystem.adapters;

import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.campuseventdiscoverysystem.R;
import com.example.campuseventdiscoverysystem.models.Event;
import java.text.SimpleDateFormat;
import java.util.*;

public class TrendingEventAdapter extends
        RecyclerView.Adapter<TrendingEventAdapter.ViewHolder> {

    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    private final List<Event> events;
    private final OnEventClickListener listener;

    public TrendingEventAdapter(List<Event> events, OnEventClickListener listener) {
        this.events = events;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trending_event, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Event event = events.get(position);

        h.tvRank.setText("#" + (position + 1));

        h.tvTitle.setText(event.getTitle() != null ? event.getTitle() : "Untitled Event");
        h.tvVenue.setText(event.getVenue() != null ? event.getVenue() : "TBD");

        int registered = event.getRegisteredCount();
        int capacity = event.getCapacity();
        if (capacity > 0) {
            h.tvRegistered.setText(registered + " / " + capacity + " registered");
        } else {
            h.tvRegistered.setText(registered + " registered");
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

        h.itemView.setOnClickListener(v -> listener.onEventClick(event));
    }

    @Override
    public int getItemCount() { return events.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRank, tvDay, tvMonth, tvTitle, tvVenue, tvRegistered;

        public ViewHolder(@NonNull View v) {
            super(v);
            tvRank = v.findViewById(R.id.tvRank);
            tvDay = v.findViewById(R.id.tvDay);
            tvMonth = v.findViewById(R.id.tvMonth);
            tvTitle = v.findViewById(R.id.tvTitle);
            tvVenue = v.findViewById(R.id.tvVenue);
            tvRegistered = v.findViewById(R.id.tvRegistered);
        }
    }
}