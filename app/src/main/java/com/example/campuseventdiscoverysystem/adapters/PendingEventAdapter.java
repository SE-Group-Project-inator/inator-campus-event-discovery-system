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

public class PendingEventAdapter extends
        RecyclerView.Adapter<PendingEventAdapter.ViewHolder> {

    public interface ActionListener {
        void onAction(String eventId);
    }

    private final List<Event> events;
    private final ActionListener onAccept;
    private final ActionListener onDecline;

    public PendingEventAdapter(List<Event> events,
                               ActionListener onAccept,
                               ActionListener onDecline) {
        this.events = events;
        this.onAccept = onAccept;
        this.onDecline = onDecline;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pending_event, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Event event = events.get(position);

        // Title
        h.tvTitle.setText(event.getTitle() != null
                ? event.getTitle() : "Untitled Event");

        // Venue
        h.tvVenue.setText(event.getVenue() != null
                ? event.getVenue() : "TBD");

        // Submitted by
        h.tvSubmittedBy.setText(event.getSubmittedByName() != null
                ? "Submitted by " + event.getSubmittedByName()
                : "Submitted by Unknown");

        // Email
        h.tvEmail.setText(event.getSubmittedByEmail() != null
                ? event.getSubmittedByEmail() : "");

        // Date
        if (event.getDate() != null) {
            Date d = event.getDate().toDate();
            h.tvDay.setText(new SimpleDateFormat("dd", Locale.US).format(d));
            h.tvMonth.setText(new SimpleDateFormat("MMM", Locale.US)
                    .format(d).toUpperCase(Locale.US));
        }

        // If already approved — show Accepted, hide Decline
        if ("active".equals(event.getStatus())) {
            h.btnAccept.setText("Accepted");
            h.btnAccept.setEnabled(false);
            h.btnDecline.setVisibility(View.GONE);
        } else {
            h.btnAccept.setText("Accept");
            h.btnAccept.setEnabled(true);
            h.btnDecline.setVisibility(View.VISIBLE);
            h.btnAccept.setOnClickListener(v ->
                    onAccept.onAction(event.getId()));
            h.btnDecline.setOnClickListener(v ->
                    onDecline.onAction(event.getId()));
        }
    }

    @Override
    public int getItemCount() { return events.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubmittedBy, tvEmail, tvVenue, tvDay, tvMonth;
        MaterialButton btnAccept, btnDecline;

        public ViewHolder(@NonNull View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tvEventTitle);
            tvSubmittedBy = v.findViewById(R.id.tvSubmittedBy);
            tvEmail = v.findViewById(R.id.tvEmail);
            tvVenue = v.findViewById(R.id.tvVenue);
            tvDay = v.findViewById(R.id.tvDay);
            tvMonth = v.findViewById(R.id.tvMonth);
            btnAccept = v.findViewById(R.id.btnAccept);
            btnDecline = v.findViewById(R.id.btnDecline);
        }
    }
}