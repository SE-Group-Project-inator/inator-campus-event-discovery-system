package com.example.campuseventdiscoverysystem.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campuseventdiscoverysystem.R;
import com.example.campuseventdiscoverysystem.models.Payment;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * StudentPaymentAdapter — displays a student's own payment records.
 * Tap a row → PaymentStatusActivity.
 */
public class StudentPaymentAdapter
        extends RecyclerView.Adapter<StudentPaymentAdapter.ViewHolder> {

    public interface OnItemClick { void onClick(Payment payment); }

    private final List<Payment> payments;
    private final OnItemClick callback;

    public StudentPaymentAdapter(List<Payment> payments, OnItemClick callback) {
        this.payments = payments;
        this.callback = callback;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student_payment, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Payment p = payments.get(position);

        h.tvEventName.setText(p.getEventName() != null ? p.getEventName() : "Unknown Event");
        h.tvMethod.setText(p.getPaymentMethodLabel());
        h.tvAmount.setText("PKR " + NumberFormat.getInstance().format((long) p.getAmount()));

        if (p.getTimestamp() != null) {
            Date d = p.getTimestamp().toDate();
            h.tvDate.setText(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(d));
        }

        // Status badge
        String status = p.getStatus() != null ? p.getStatus() : "";
        String label;
        int colorRes;
        switch (status) {
            case Payment.STATUS_VERIFICATION_PENDING:
                label = "⏳ Pending"; colorRes = R.color.admin_warning; break;
            case Payment.STATUS_APPROVED:
                label = "✅ Approved"; colorRes = R.color.admin_success; break;
            case Payment.STATUS_REJECTED:
                label = "❌ Rejected"; colorRes = R.color.admin_error; break;
            case Payment.STATUS_PENDING_CASH:
                label = "💵 Cash Pending"; colorRes = R.color.admin_info; break;
            case Payment.STATUS_REGISTERED:
                label = "🎉 Registered"; colorRes = R.color.admin_success; break;
            default:
                label = status; colorRes = R.color.text_grey;
        }
        h.tvStatus.setText(label);
        try {
            h.tvStatus.setTextColor(
                    h.itemView.getContext().getResources().getColor(colorRes, null));
        } catch (Exception ignored) {}

        // Color the left status bar
        android.view.View statusBar = h.itemView.findViewById(com.example.campuseventdiscoverysystem.R.id.viewStatusBar);
        if (statusBar != null) {
            try { statusBar.setBackgroundColor(
                    h.itemView.getContext().getResources().getColor(colorRes, null));
            } catch (Exception ignored) {}
        }

        h.itemView.setOnClickListener(v -> callback.onClick(p));
    }

    @Override public int getItemCount() { return payments.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEventName, tvMethod, tvAmount, tvDate, tvStatus;

        ViewHolder(@NonNull View v) {
            super(v);
            tvEventName = v.findViewById(R.id.tvSpEventName);
            tvMethod    = v.findViewById(R.id.tvSpMethod);
            tvAmount    = v.findViewById(R.id.tvSpAmount);
            tvDate      = v.findViewById(R.id.tvSpDate);
            tvStatus    = v.findViewById(R.id.tvSpStatus);
        }
    }
}
