package com.example.myapplication;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    public interface OnOrderClickListener {
        void onOrderClick(Order order);
    }

    private final List<Order> orders;
    private final OnOrderClickListener listener;

    public OrderAdapter(List<Order> orders, OnOrderClickListener listener) {
        this.orders = orders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new OrderViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_tracking, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder h, int position) {
        Order order = orders.get(position);
        Context ctx = h.itemView.getContext();
        int step = order.getStatusStep();
        boolean isCancelled = step == Order.STEP_CANCELLED;
        boolean isDelivered = step >= Order.STEP_DELIVERED;

        h.tvOrderCode.setText("#" + safe(order.getOrderCode()));
        h.tvOrderDate.setText(safe(order.getCreatedAt()));

        UiMotion.bindStatusBadge(ctx, h.tvStatusBadge, step);
        h.tvStageTitle.setText(Order.getStepTitle(step));
        h.tvStageDesc.setText(isCancelled && order.getCancelReason() != null && !order.getCancelReason().trim().isEmpty()
                ? "Lý do hủy: " + order.getCancelReason() : Order.getStepDescription(step));

        int primary = ContextCompat.getColor(ctx, R.color.primary);
        int primaryDark = ContextCompat.getColor(ctx, R.color.primary_dark);
        int primarySoft = ContextCompat.getColor(ctx, R.color.primary_soft);
        int success = ContextCompat.getColor(ctx, R.color.success);
        int danger = ContextCompat.getColor(ctx, R.color.danger);
        int border = ContextCompat.getColor(ctx, R.color.border);
        int secondary = ContextCompat.getColor(ctx, R.color.text_secondary);
        int white = ContextCompat.getColor(ctx, R.color.white);

        int activeColor = isCancelled ? border : (isDelivered ? success : primary);
        int activeText = isCancelled ? danger : (isDelivered ? success : primaryDark);

        h.cardStageBanner.setCardBackgroundColor(isCancelled ? 0xFFFEF2F2 : (isDelivered ? 0xFFECFDF5 : primarySoft));
        h.tvStageTitle.setTextColor(activeText);

        for (int i = 0; i < h.dots.length; i++) {
            boolean reached = !isCancelled && i <= step;
            h.dots[i].setCardBackgroundColor(reached ? activeColor : border);
            h.dotNumbers[i].setTextColor(reached ? white : secondary);
            h.labels[i].setTextColor(reached ? activeText : secondary);
            h.labels[i].setTypeface(null, (!isCancelled && i == step) ? Typeface.BOLD : Typeface.NORMAL);
        }
        for (int i = 0; i < h.lines.length; i++) {
            h.lines[i].setBackgroundColor((!isCancelled && i < step) ? activeColor : border);
        }

        h.tvReceiverInfo.setText("Người nhận: " + safe(order.getReceiverName()) + " - " + safe(order.getReceiverPhone()));
        h.tvAddressInfo.setText("Địa chỉ: " + safe(order.getReceiverAddress()));
        h.tvOrderMeta.setText(order.getItemCount() + " sản phẩm - " + safe(order.getPaymentMethod()));
        h.tvOrderTotal.setText(safe(order.getTotalPrice()));

        h.itemView.setOnClickListener(v -> { if (listener != null) listener.onOrderClick(order); });
        UiMotion.bindTouchScale(h.itemView);
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    private static String safe(String v) {
        return v == null ? "" : v;
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        final TextView tvOrderCode, tvOrderDate, tvStatusBadge, tvStageTitle, tvStageDesc,
                tvReceiverInfo, tvAddressInfo, tvOrderMeta, tvOrderTotal;
        final MaterialCardView cardStageBanner;
        final MaterialCardView[] dots;
        final TextView[] dotNumbers, labels;
        final View[] lines;

        OrderViewHolder(@NonNull View v) {
            super(v);
            tvOrderCode = v.findViewById(R.id.tvOrderCode);
            tvOrderDate = v.findViewById(R.id.tvOrderDate);
            tvStatusBadge = v.findViewById(R.id.tvStatusBadge);
            cardStageBanner = v.findViewById(R.id.cardStageBanner);
            tvStageTitle = v.findViewById(R.id.tvStageTitle);
            tvStageDesc = v.findViewById(R.id.tvStageDesc);
            dots = new MaterialCardView[]{v.findViewById(R.id.dotStep0), v.findViewById(R.id.dotStep1), v.findViewById(R.id.dotStep2), v.findViewById(R.id.dotStep3), v.findViewById(R.id.dotStep4)};
            dotNumbers = new TextView[]{v.findViewById(R.id.tvDot0), v.findViewById(R.id.tvDot1), v.findViewById(R.id.tvDot2), v.findViewById(R.id.tvDot3), v.findViewById(R.id.tvDot4)};
            lines = new View[]{v.findViewById(R.id.line01), v.findViewById(R.id.line12), v.findViewById(R.id.line23), v.findViewById(R.id.line34)};
            labels = new TextView[]{v.findViewById(R.id.tvLabelStep0), v.findViewById(R.id.tvLabelStep1), v.findViewById(R.id.tvLabelStep2), v.findViewById(R.id.tvLabelStep3), v.findViewById(R.id.tvLabelStep4)};
            tvReceiverInfo = v.findViewById(R.id.tvReceiverInfo);
            tvAddressInfo = v.findViewById(R.id.tvAddressInfo);
            tvOrderMeta = v.findViewById(R.id.tvOrderMeta);
            tvOrderTotal = v.findViewById(R.id.tvOrderTotal);
        }
    }
}
