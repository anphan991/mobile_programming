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

/**
 * Adapter hiển thị danh sách đơn hàng kèm thanh tiến trình 5 bước giao hàng (chế độ xem cho khách hàng).
 */
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
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_tracking, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orders.get(position);
        Context context = holder.itemView.getContext();
        int step = order.getStatusStep();
        boolean isCancelled = step == Order.STEP_CANCELLED;
        boolean isDelivered = step >= Order.STEP_DELIVERED;

        holder.tvOrderCode.setText("#" + safeText(order.getOrderCode()));
        holder.tvOrderDate.setText(safeText(order.getCreatedAt()));

        String statusTitle = Order.getStepTitle(step);
        holder.tvStatusBadge.setText(statusTitle);
        holder.tvStageTitle.setText(statusTitle);
        if (isCancelled && order.getCancelReason() != null && !order.getCancelReason().trim().isEmpty()) {
            holder.tvStageDesc.setText("Lý do hủy: " + order.getCancelReason());
        } else {
            holder.tvStageDesc.setText(Order.getStepDescription(step));
        }

        int primaryColor = ContextCompat.getColor(context, R.color.primary);
        int primaryDarkColor = ContextCompat.getColor(context, R.color.primary_dark);
        int primarySoftColor = ContextCompat.getColor(context, R.color.primary_soft);
        int successColor = ContextCompat.getColor(context, R.color.success);
        int dangerColor = ContextCompat.getColor(context, R.color.danger);
        int borderColor = ContextCompat.getColor(context, R.color.border);
        int textSecondary = ContextCompat.getColor(context, R.color.text_secondary);
        int whiteColor = ContextCompat.getColor(context, R.color.white);

        int activeColor = isCancelled ? borderColor : (isDelivered ? successColor : primaryColor);
        int activeTextColor = isCancelled ? dangerColor : (isDelivered ? successColor : primaryDarkColor);

        if (isCancelled) {
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_danger);
            holder.tvStatusBadge.setTextColor(dangerColor);
            holder.cardStageBanner.setCardBackgroundColor(0xFFFEF2F2);
            holder.tvStageTitle.setTextColor(dangerColor);
        } else if (isDelivered) {
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_success);
            holder.tvStatusBadge.setTextColor(successColor);
            holder.cardStageBanner.setCardBackgroundColor(0xFFECFDF5);
            holder.tvStageTitle.setTextColor(successColor);
        } else {
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_primary);
            holder.tvStatusBadge.setTextColor(primaryDarkColor);
            holder.cardStageBanner.setCardBackgroundColor(primarySoftColor);
            holder.tvStageTitle.setTextColor(primaryDarkColor);
        }

        // Cập nhật 5 nút mốc tiến trình và 4 thanh nối
        for (int i = 0; i < holder.dots.length; i++) {
            boolean reached = !isCancelled && i <= step;
            boolean isCurrent = !isCancelled && i == step;
            holder.dots[i].setCardBackgroundColor(reached ? activeColor : borderColor);
            holder.dotNumbers[i].setTextColor(reached ? whiteColor : textSecondary);
            holder.labels[i].setTextColor(reached ? activeTextColor : textSecondary);
            holder.labels[i].setTypeface(null, isCurrent ? Typeface.BOLD : Typeface.NORMAL);
        }

        for (int i = 0; i < holder.lines.length; i++) {
            boolean lineReached = !isCancelled && i < step;
            holder.lines[i].setBackgroundColor(lineReached ? activeColor : borderColor);
        }

        holder.tvReceiverInfo.setText("Người nhận: " + safeText(order.getReceiverName())
                + " - " + safeText(order.getReceiverPhone()));
        holder.tvAddressInfo.setText("Địa chỉ: " + safeText(order.getReceiverAddress()));
        holder.tvOrderMeta.setText(order.getItemCount() + " sản phẩm - " + safeText(order.getPaymentMethod()));
        holder.tvOrderTotal.setText(safeText(order.getTotalPrice()));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOrderClick(order);
            }
        });
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        final TextView tvOrderCode;
        final TextView tvOrderDate;
        final TextView tvStatusBadge;
        final MaterialCardView cardStageBanner;
        final TextView tvStageTitle;
        final TextView tvStageDesc;
        final MaterialCardView[] dots;
        final TextView[] dotNumbers;
        final View[] lines;
        final TextView[] labels;
        final TextView tvReceiverInfo;
        final TextView tvAddressInfo;
        final TextView tvOrderMeta;
        final TextView tvOrderTotal;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderCode = itemView.findViewById(R.id.tvOrderCode);
            tvOrderDate = itemView.findViewById(R.id.tvOrderDate);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            cardStageBanner = itemView.findViewById(R.id.cardStageBanner);
            tvStageTitle = itemView.findViewById(R.id.tvStageTitle);
            tvStageDesc = itemView.findViewById(R.id.tvStageDesc);

            dots = new MaterialCardView[]{
                    itemView.findViewById(R.id.dotStep0),
                    itemView.findViewById(R.id.dotStep1),
                    itemView.findViewById(R.id.dotStep2),
                    itemView.findViewById(R.id.dotStep3),
                    itemView.findViewById(R.id.dotStep4)
            };
            dotNumbers = new TextView[]{
                    itemView.findViewById(R.id.tvDot0),
                    itemView.findViewById(R.id.tvDot1),
                    itemView.findViewById(R.id.tvDot2),
                    itemView.findViewById(R.id.tvDot3),
                    itemView.findViewById(R.id.tvDot4)
            };
            lines = new View[]{
                    itemView.findViewById(R.id.line01),
                    itemView.findViewById(R.id.line12),
                    itemView.findViewById(R.id.line23),
                    itemView.findViewById(R.id.line34)
            };
            labels = new TextView[]{
                    itemView.findViewById(R.id.tvLabelStep0),
                    itemView.findViewById(R.id.tvLabelStep1),
                    itemView.findViewById(R.id.tvLabelStep2),
                    itemView.findViewById(R.id.tvLabelStep3),
                    itemView.findViewById(R.id.tvLabelStep4)
            };

            tvReceiverInfo = itemView.findViewById(R.id.tvReceiverInfo);
            tvAddressInfo = itemView.findViewById(R.id.tvAddressInfo);
            tvOrderMeta = itemView.findViewById(R.id.tvOrderMeta);
            tvOrderTotal = itemView.findViewById(R.id.tvOrderTotal);
        }
    }
}
