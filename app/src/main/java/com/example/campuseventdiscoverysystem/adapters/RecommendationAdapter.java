package com.example.campuseventdiscoverysystem.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campuseventdiscoverysystem.R;
import com.example.campuseventdiscoverysystem.activities.EventDetailActivity;
import com.example.campuseventdiscoverysystem.models.Event;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecommendationAdapter extends RecyclerView.Adapter<RecommendationAdapter.VH> {

    private final List<Event> events;

    public RecommendationAdapter(List<Event> events) {
        this.events = events;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_upcoming, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Event e = events.get(position);
        Context ctx = h.itemView.getContext();

        h.tvTitle.setText(e.getTitle());
        h.tvLocation.setText("📍 " + (e.getVenue() != null ? e.getVenue() : ""));
        h.tvPrice.setText(e.getPriceDisplay());

        Timestamp ts = e.getDate();
        if (ts != null) {
            Date d = ts.toDate();
            h.tvDay.setText(new SimpleDateFormat("dd", Locale.getDefault()).format(d));
            h.tvMonth.setText(
                    new SimpleDateFormat("MMM", Locale.getDefault()).format(d).toUpperCase());
        }

        h.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(ctx, EventDetailActivity.class);
            intent.putExtra("eventId", e.getId());
            intent.putExtra("eventTitle", e.getTitle());
            intent.putExtra("eventVenue", e.getVenue());
            intent.putExtra("eventDescription", e.getDescription());
            intent.putExtra("eventCapacity", e.getCapacity());
            intent.putExtra("eventRegistered", e.getRegisteredCount());
            intent.putExtra("eventDateMillis", ts != null ? ts.toDate().getTime() : 0L);
            intent.putExtra("eventOrganizerName", e.getSubmittedByName());
            intent.putExtra("eventOrganizerEmail", e.getSubmittedByEmail());
            intent.putExtra("eventTicketPrice", e.getPrice());
            ctx.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() { return events.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvLocation, tvDay, tvMonth, tvPrice;
        VH(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvDay = itemView.findViewById(R.id.tvDay);
            tvMonth = itemView.findViewById(R.id.tvMonth);
            tvPrice = itemView.findViewById(R.id.tvPrice);
        }
    }
}
