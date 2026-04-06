package com.example.campuseventdiscoverysystem.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.campuseventdiscoverysystem.R;
import com.example.campuseventdiscoverysystem.models.Event;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class ManagerEventAdapter extends RecyclerView.Adapter<ManagerEventAdapter.ViewHolder> {

    /** Called when the event card is tapped — opens EditEventActivity (existing feature). */
    public interface OnItemClickListener {
        void onItemClick(String eventId);
    }

    /** Called when "View Attendees" is tapped — opens AttendeeListActivity (US-27). */
    public interface OnAttendeesClickListener {
        void onAttendeesClick(Event event);
    }

    private final List<Event> eventList;
    private final OnItemClickListener clickListener;
    private final OnAttendeesClickListener attendeesListener;

    public ManagerEventAdapter(List<Event> eventList,
                                OnItemClickListener clickListener,
                                OnAttendeesClickListener attendeesListener) {
        this.eventList = eventList;
        this.clickListener = clickListener;
        this.attendeesListener = attendeesListener;
    }

    /** Backwards-compatible constructor for callers that don't need attendees. */
    public ManagerEventAdapter(List<Event> eventList, OnItemClickListener clickListener) {
        this(eventList, clickListener, null);
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

        holder.tvEventTitle.setText(
                event.getTitle() != null ? event.getTitle() : "Untitled Event");

        String status = event.getStatus();
        if ("active".equals(status)) {
            holder.tvEventStatus.setText("Approved");
            holder.tvEventStatus.setBackgroundTintList(
                    holder.itemView.getContext().getColorStateList(R.color.green_accept));
        } else if ("rejected".equals(status)) {
            holder.tvEventStatus.setText("Declined");
            holder.tvEventStatus.setBackgroundTintList(
                    holder.itemView.getContext().getColorStateList(R.color.red_decline));
        } else {
            holder.tvEventStatus.setText("Pending");
            // default orange (#FFB300) set in XML
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onItemClick(event.getId());
        });

        if (attendeesListener != null) {
            holder.btnViewAttendees.setOnClickListener(v ->
                    attendeesListener.onAttendeesClick(event));
        }
    }

    @Override
    public int getItemCount() { return eventList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEventTitle;
        MaterialButton tvEventStatus;
        MaterialButton btnViewAttendees;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEventTitle    = itemView.findViewById(R.id.tvEventTitle);
            tvEventStatus   = itemView.findViewById(R.id.tvEventStatus);
            btnViewAttendees = itemView.findViewById(R.id.btnViewAttendees);
        }
    }
}
