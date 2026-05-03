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

        // Event details: time
        if (h.tvEventTime != null) {
            String start = event.getStartTime();
            String end   = event.getEndTime();
            if (start != null && end != null) {
                h.tvEventTime.setText(start + " – " + end);
            } else if (start != null) {
                h.tvEventTime.setText(start);
            } else {
                h.tvEventTime.setText("Time TBD");
            }
        }

        // Capacity
        if (h.tvEventCapacity != null) {
            int cap = event.getCapacity();
            h.tvEventCapacity.setText(cap > 0 ? "Capacity: " + cap : "Capacity: Unlimited");
        }

        // Price
        if (h.tvEventPrice != null) {
            h.tvEventPrice.setText(event.getPriceDisplay());
        }

        // Description
        if (h.tvEventDescription != null) {
            String desc = event.getDescription();
            if (desc != null && !desc.isEmpty()) {
                h.tvEventDescription.setText(desc);
                h.tvEventDescription.setVisibility(View.VISIBLE);
            } else {
                h.tvEventDescription.setVisibility(View.GONE);
            }
        }

        // Dynamic status badge
        String status = event.getStatus();
        if (h.tvStatusBadge != null) {
            android.content.Context ctx = h.itemView.getContext();
            if ("active".equals(status)) {
                h.tvStatusBadge.setText("Approved");
                h.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_approved);
                h.tvStatusBadge.setTextColor(ctx.getColor(R.color.admin_success));
            } else if ("rejected".equals(status)) {
                h.tvStatusBadge.setText("Declined");
                h.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_rejected);
                h.tvStatusBadge.setTextColor(ctx.getColor(R.color.admin_error));
            } else {
                // pending_approval, pending, or any unknown status → show Pending
                h.tvStatusBadge.setText("Pending");
                h.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_pending);
                h.tvStatusBadge.setTextColor(ctx.getColor(R.color.admin_warning));
            }
        }

        // Status-based button rendering — always reset visibility first to fix recycling bugs
        h.btnAccept.setVisibility(View.VISIBLE);
        h.btnDecline.setVisibility(View.VISIBLE);

        if ("active".equals(status)) {
            h.btnAccept.setText("Approved ✓");
            h.btnAccept.setEnabled(false);
            h.btnDecline.setVisibility(View.GONE);
        } else if ("rejected".equals(status)) {
            h.btnDecline.setText("Declined");
            h.btnDecline.setEnabled(false);
            h.btnAccept.setVisibility(View.GONE);
        } else {
            // pending_approval, pending, or unknown — show both action buttons
            h.btnAccept.setText("Approve ✓");
            h.btnAccept.setEnabled(true);
            h.btnDecline.setText("Decline");
            h.btnDecline.setEnabled(true);

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
        TextView tvStatusBadge, tvEventTime, tvEventCapacity, tvEventPrice, tvEventDescription;
        MaterialButton btnAccept, btnDecline;

        public ViewHolder(@NonNull View v) {
            super(v);
            tvTitle              = v.findViewById(R.id.tvEventTitle);
            tvSubmittedBy        = v.findViewById(R.id.tvSubmittedBy);
            tvEmail              = v.findViewById(R.id.tvEmail);
            tvVenue              = v.findViewById(R.id.tvVenue);
            tvDay                = v.findViewById(R.id.tvDay);
            tvMonth              = v.findViewById(R.id.tvMonth);
            tvSubmitterInitial   = v.findViewById(R.id.tvSubmitterInitial);
            tvStatusBadge        = v.findViewById(R.id.tvStatusBadge);
            tvEventTime          = v.findViewById(R.id.tvEventTime);
            tvEventCapacity      = v.findViewById(R.id.tvEventCapacity);
            tvEventPrice         = v.findViewById(R.id.tvEventPrice);
            tvEventDescription   = v.findViewById(R.id.tvEventDescription);
            btnAccept            = v.findViewById(R.id.btnAccept);
            btnDecline           = v.findViewById(R.id.btnDecline);
        }
    }
}
