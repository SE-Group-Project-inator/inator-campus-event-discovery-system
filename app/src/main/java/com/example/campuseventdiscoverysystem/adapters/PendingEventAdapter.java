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
        this.events   = events;
        this.onAccept  = onAccept;
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
        h.tvTitle.setText(event.getTitle() != null ? event.getTitle() : "Untitled Event");

        // Venue
        h.tvVenue.setText(event.getVenue() != null ? event.getVenue() : "TBD");

        // Submitted by
        String submitterName = event.getSubmittedByName();
        h.tvSubmittedBy.setText(submitterName != null
                ? "Submitted by " + submitterName : "Submitted by Unknown");

        // Email
        h.tvEmail.setText(event.getSubmittedByEmail() != null
                ? event.getSubmittedByEmail() : "");

        // Submitter initial in avatar
        if (h.tvSubmitterInitial != null) {
            String initial = "?";
            if (submitterName != null && !submitterName.isEmpty()) {
                initial = submitterName.substring(0, 1).toUpperCase();
            } else if (event.getSubmittedByEmail() != null && !event.getSubmittedByEmail().isEmpty()) {
                initial = event.getSubmittedByEmail().substring(0, 1).toUpperCase();
            }
            h.tvSubmitterInitial.setText(initial);
        }

        // Date
        if (event.getDate() != null) {
            Date d = event.getDate().toDate();
            h.tvDay.setText(new SimpleDateFormat("dd", Locale.US).format(d));
            h.tvMonth.setText(new SimpleDateFormat("MMM", Locale.US).format(d).toUpperCase(Locale.US));
        } else {
            h.tvDay.setText("—");
            h.tvMonth.setText("TBD");
        }

        // Status-based button rendering
        String status = event.getStatus();
        if ("active".equals(status)) {
            h.btnAccept.setText("Approved ✓");
            h.btnAccept.setEnabled(false);
            h.btnDecline.setVisibility(View.GONE);
        } else if ("rejected".equals(status)) {
            h.btnDecline.setText("Declined");
            h.btnDecline.setEnabled(false);
            h.btnAccept.setVisibility(View.GONE);
        } else {
            // pending_approval — default
            h.btnAccept.setText("Approve ✓");
            h.btnAccept.setEnabled(true);
            h.btnAccept.setVisibility(View.VISIBLE);
            h.btnDecline.setText("Decline");
            h.btnDecline.setEnabled(true);
            h.btnDecline.setVisibility(View.VISIBLE);

            h.btnAccept.setOnClickListener(v -> onAccept.onAction(event.getId()));
            h.btnDecline.setOnClickListener(v -> onDecline.onAction(event.getId()));
        }

        // Subtle entry animation
        h.itemView.setAlpha(0f);
        h.itemView.animate().alpha(1f).setDuration(200)
                .setStartDelay(position * 40L).start();
    }

    @Override
    public int getItemCount() { return events.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubmittedBy, tvEmail, tvVenue, tvDay, tvMonth, tvSubmitterInitial;
        MaterialButton btnAccept, btnDecline;

        public ViewHolder(@NonNull View v) {
            super(v);
            tvTitle            = v.findViewById(R.id.tvEventTitle);
            tvSubmittedBy      = v.findViewById(R.id.tvSubmittedBy);
            tvEmail            = v.findViewById(R.id.tvEmail);
            tvVenue            = v.findViewById(R.id.tvVenue);
            tvDay              = v.findViewById(R.id.tvDay);
            tvMonth            = v.findViewById(R.id.tvMonth);
            tvSubmitterInitial = v.findViewById(R.id.tvSubmitterInitial);
            btnAccept          = v.findViewById(R.id.btnAccept);
            btnDecline         = v.findViewById(R.id.btnDecline);
        }
    }
}
