package com.example.campuseventdiscoverysystem.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campuseventdiscoverysystem.R;
import com.example.campuseventdiscoverysystem.models.Payment;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for the Event Manager's payment verification list.
 * Each card shows student info, payment method, screenshot thumbnail, status,
 * and Approve / Reject action buttons.
 */
public class PaymentVerificationAdapter
        extends RecyclerView.Adapter<PaymentVerificationAdapter.ViewHolder> {

    public interface OnScreenshotClick { void onClick(String screenshotData); }
    public interface OnApproveClick    { void onClick(Payment payment); }
    public interface OnRejectClick     { void onClick(Payment payment); }

    private final List<Payment> payments;
    private final OnScreenshotClick screenshotCallback;
    private final OnApproveClick    approveCallback;
    private final OnRejectClick     rejectCallback;

    public PaymentVerificationAdapter(
            List<Payment> payments,
            OnScreenshotClick screenshotCallback,
            OnApproveClick    approveCallback,
            OnRejectClick     rejectCallback) {
        this.payments           = payments;
        this.screenshotCallback = screenshotCallback;
        this.approveCallback    = approveCallback;
        this.rejectCallback     = rejectCallback;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_payment_verification, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Payment p = payments.get(position);

        // ── Basic info ────────────────────────────────────────────────
        h.tvStudentName.setText(p.getStudentName() != null ? p.getStudentName() : "Unknown Student");
        h.tvEventName.setText(p.getEventName() != null ? p.getEventName() : "Unknown Event");
        h.tvMethod.setText(p.getPaymentMethodLabel());
        h.tvAmount.setText("PKR " + NumberFormat.getInstance().format((long) p.getAmount()));

        // ── Timestamp ─────────────────────────────────────────────────
        if (p.getTimestamp() != null) {
            Date d = p.getTimestamp().toDate();
            h.tvTimestamp.setText(new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(d));
        } else {
            h.tvTimestamp.setText("—");
        }

        // ── Status badge ──────────────────────────────────────────────
        String status = p.getStatus() != null ? p.getStatus() : "";
        applyStatusBadge(h, status);

        // ── Screenshot thumbnail ──────────────────────────────────────
        String screenshotData = p.getScreenshotUrl();
        if (screenshotData != null && !screenshotData.isEmpty()) {
            h.cardScreenshot.setVisibility(View.VISIBLE);
            loadThumbnail(h.ivScreenshot, screenshotData);
            h.cardScreenshot.setOnClickListener(v -> screenshotCallback.onClick(screenshotData));
            h.tvViewScreenshot.setOnClickListener(v -> screenshotCallback.onClick(screenshotData));
        } else {
            h.cardScreenshot.setVisibility(View.GONE);
        }

        // ── Action buttons ────────────────────────────────────────────
        boolean isPending = Payment.STATUS_VERIFICATION_PENDING.equals(status)
                || Payment.STATUS_PENDING_CASH.equals(status);

        h.btnApprove.setVisibility(isPending ? View.VISIBLE : View.GONE);
        h.btnReject.setVisibility(isPending ? View.VISIBLE : View.GONE);
        h.tvActionsDone.setVisibility(isPending ? View.GONE : View.VISIBLE);

        h.btnApprove.setOnClickListener(v -> approveCallback.onClick(p));
        h.btnReject.setOnClickListener(v  -> rejectCallback.onClick(p));
    }

    private void applyStatusBadge(ViewHolder h, String status) {
        String label;
        int bgRes;
        switch (status) {
            case Payment.STATUS_VERIFICATION_PENDING:
                label = "⏳ Pending"; bgRes = R.color.admin_warning_bg; break;
            case Payment.STATUS_APPROVED:
                label = "✅ Approved"; bgRes = R.color.admin_success_bg; break;
            case Payment.STATUS_REJECTED:
                label = "❌ Rejected"; bgRes = R.color.admin_error_bg; break;
            case Payment.STATUS_PENDING_CASH:
                label = "💵 Cash Pending"; bgRes = R.color.admin_info_bg; break;
            default:
                label = status; bgRes = R.color.admin_surface_2;
        }
        h.tvStatus.setText(label);
        try {
            h.tvStatus.setBackgroundColor(
                    h.itemView.getContext().getResources().getColor(bgRes, null));
        } catch (Exception ignored) {}
    }

    private void loadThumbnail(ImageView iv, String base64Data) {
        try {
            String b64 = base64Data.contains(",")
                    ? base64Data.substring(base64Data.indexOf(",") + 1)
                    : base64Data;
            byte[] bytes  = Base64.decode(b64, Base64.DEFAULT);
            // Decode at reduced sample size to save memory
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = 4;
            Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
            iv.setImageBitmap(bmp);
        } catch (Exception e) {
            iv.setImageResource(R.drawable.ic_launcher_foreground);
        }
    }

    @Override public int getItemCount() { return payments.size(); }

    // ── ViewHolder ─────────────────────────────────────────────────────

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentName, tvEventName, tvMethod, tvAmount, tvStatus, tvTimestamp;
        TextView tvViewScreenshot, tvActionsDone;
        ImageView ivScreenshot;
        CardView cardScreenshot;
        MaterialButton btnApprove, btnReject;

        ViewHolder(@NonNull View v) {
            super(v);
            tvStudentName    = v.findViewById(R.id.tvVerStudentName);
            tvEventName      = v.findViewById(R.id.tvVerEventName);
            tvMethod         = v.findViewById(R.id.tvVerMethod);
            tvAmount         = v.findViewById(R.id.tvVerAmount);
            tvStatus         = v.findViewById(R.id.tvVerStatus);
            tvTimestamp      = v.findViewById(R.id.tvVerTimestamp);
            ivScreenshot     = v.findViewById(R.id.ivVerScreenshot);
            cardScreenshot   = v.findViewById(R.id.cardVerScreenshot);
            tvViewScreenshot = v.findViewById(R.id.tvViewScreenshot);
            tvActionsDone    = v.findViewById(R.id.tvVerActionsDone);
            btnApprove       = v.findViewById(R.id.btnVerApprove);
            btnReject        = v.findViewById(R.id.btnVerReject);
        }
    }
}
