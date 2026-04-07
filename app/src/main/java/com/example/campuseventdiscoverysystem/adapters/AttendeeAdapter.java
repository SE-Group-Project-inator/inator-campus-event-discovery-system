package com.example.campuseventdiscoverysystem.adapters;

import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.campuseventdiscoverysystem.R;
import com.example.campuseventdiscoverysystem.models.Registration;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class AttendeeAdapter extends
        RecyclerView.Adapter<AttendeeAdapter.ViewHolder> {

    public interface OnConfirmListener {
        void onConfirm(Registration registration, int position);
    }

    private final List<Registration> registrations;
    private final OnConfirmListener listener;
    private final boolean readOnly;

    public AttendeeAdapter(List<Registration> registrations,
                           OnConfirmListener listener,
                           boolean readOnly) {
        this.registrations = registrations;
        this.listener = listener;
        this.readOnly = readOnly;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attendee, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Registration reg = registrations.get(position);

        String roll = extractRollNumber(reg);
        String name = reg.getUserName();

        // Show "Name - RollNumber" in one label (matches Figma design)
        if (name != null && !name.isEmpty()) {
            h.tvName.setText(name + " - " + roll);
        } else {
            h.tvName.setText(roll);
        }

        // Initials for avatar circle
        h.tvInitials.setText(getInitials(name != null ? name : roll));

        // US-32: confirm button visible only in event manager view (readOnly=false)
        if (readOnly) {
            h.btnConfirm.setVisibility(View.GONE);
        } else {
            h.btnConfirm.setVisibility(View.VISIBLE);
            if (reg.isConfirmed()) {
                h.btnConfirm.setText("✓ Registered");
                h.btnConfirm.setBackgroundTintList(
                        h.itemView.getContext().getColorStateList(R.color.green_accept));
                h.btnConfirm.setEnabled(false);
            } else {
                h.btnConfirm.setText("+ Register");
                h.btnConfirm.setBackgroundTintList(
                        h.itemView.getContext().getColorStateList(R.color.btn_eventmgr));
                h.btnConfirm.setEnabled(true);
                h.btnConfirm.setOnClickListener(v ->
                        listener.onConfirm(reg, h.getAdapterPosition()));
            }
        }
    }

    public void updateItem(int position) {
        notifyItemChanged(position);
    }

    private String extractRollNumber(Registration reg) {
        if (reg.getUserEmail() != null && reg.getUserEmail().contains("@")) {
            return reg.getUserEmail().split("@")[0];
        }
        if (reg.getUserId() != null) return reg.getUserId();
        return "Unknown";
    }

    private String getInitials(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return String.valueOf(parts[0].charAt(0)).toUpperCase()
                    + String.valueOf(parts[1].charAt(0)).toUpperCase();
        }
        return String.valueOf(name.charAt(0)).toUpperCase();
    }

    @Override
    public int getItemCount() { return registrations.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvInitials, tvRollNumber, tvName;
        MaterialButton btnConfirm;

        public ViewHolder(@NonNull View v) {
            super(v);
            tvInitials   = v.findViewById(R.id.tvInitials);
            tvRollNumber = v.findViewById(R.id.tvRollNumber);
            tvName       = v.findViewById(R.id.tvAttendeeName);
            btnConfirm   = v.findViewById(R.id.btnConfirm);
        }
    }
}
