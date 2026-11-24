package com.example.myapplication;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.List;

public class OrderDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ORDER_CODE = "EXTRA_ORDER_CODE";

    private DatabaseHelper dbHelper;
    private String orderCode, currentUser;
    private boolean isAdmin;
    private Order currentOrder;
    private ListenerRegistration docListener;

    private TextView tvDetailHeaderSub, tvDetailStatusBadge, tvDetailItemsSummary,
            tvDetailPaymentMethod, tvDetailTotalPrice, tvDetailReceiver,
            tvDetailAddress, tvDetailNote, tvDetailAccountOwner, tvCancelReasonDetail;
    private LinearLayout layoutTimelineSteps;
    private View layoutAdminOrderActions, cardOrderCancelled;
    private MaterialButton btnAdminAdvanceStatus, btnAdminSelectStatus, btnCancelOrder, btnReorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);
        UiMotion.applyInsets(findViewById(R.id.rootOrderDetail));

        dbHelper = new DatabaseHelper(this);
        currentUser = UiMotion.currentUser(this);
        isAdmin = dbHelper.isAdminUser(currentUser);
        orderCode = getIntent().getStringExtra(EXTRA_ORDER_CODE);
        if (orderCode == null || orderCode.trim().isEmpty()) {
            Toast.makeText(this, "Không tìm thấy mã đơn hàng!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvDetailHeaderSub = findViewById(R.id.tvDetailHeaderSub);
        tvDetailStatusBadge = findViewById(R.id.tvDetailStatusBadge);
        tvDetailItemsSummary = findViewById(R.id.tvDetailItemsSummary);
        tvDetailPaymentMethod = findViewById(R.id.tvDetailPaymentMethod);
        tvDetailTotalPrice = findViewById(R.id.tvDetailTotalPrice);
        tvDetailReceiver = findViewById(R.id.tvDetailReceiver);
        tvDetailAddress = findViewById(R.id.tvDetailAddress);
        tvDetailNote = findViewById(R.id.tvDetailNote);
        tvDetailAccountOwner = findViewById(R.id.tvDetailAccountOwner);
        tvCancelReasonDetail = findViewById(R.id.tvCancelReasonDetail);
        layoutTimelineSteps = findViewById(R.id.layoutTimelineSteps);
        cardOrderCancelled = findViewById(R.id.cardOrderCancelled);
        layoutAdminOrderActions = findViewById(R.id.layoutAdminOrderActions);
        btnAdminAdvanceStatus = findViewById(R.id.btnAdminAdvanceStatus);
        btnAdminSelectStatus = findViewById(R.id.btnAdminSelectStatus);
        btnCancelOrder = findViewById(R.id.btnCancelOrder);
        btnReorder = findViewById(R.id.btnReorder);

        UiMotion.bindTouchScale(findViewById(R.id.btnBack), findViewById(R.id.btnAllOrdersList),
                btnAdminAdvanceStatus, btnAdminSelectStatus, btnCancelOrder, btnReorder);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAllOrdersList).setOnClickListener(v -> {
            startActivity(new Intent(this, OrderTrackingActivity.class));
            finish();
        });
        btnAdminAdvanceStatus.setOnClickListener(v -> {
            if (isAdmin && currentOrder != null) {
                Order.updateStepAndSync(this, currentOrder, currentOrder.getStatusStep() + 1, () -> renderOrder(currentOrder));
            }
        });
        btnAdminSelectStatus.setOnClickListener(v -> {
            if (isAdmin && currentOrder != null) Order.showStepPicker(this, currentOrder, this::loadLocalOrder);
        });
        btnCancelOrder.setOnClickListener(v -> {
            if (currentOrder != null) OrderCancelHelper.showCancelDialog(this, currentOrder, isAdmin, r -> loadLocalOrder());
        });
        btnReorder.setOnClickListener(v -> reorderCancelledItems());

        loadLocalOrder();
        listenCloudOrder();
    }

    private void loadLocalOrder() {
        Order found = dbHelper.getOrderByCode(orderCode);
        if (found != null) renderOrder(currentOrder = found);
    }

    private void listenCloudOrder() {
        docListener = FirebaseFirestore.getInstance()
                .collection("orders")
                .document(orderCode)
                .addSnapshotListener((doc, error) -> {
                    if (error != null || doc == null || !doc.exists() || isFinishing() || isDestroyed()) return;
                    Order cloudOrder = Order.fromFirestore(doc, currentUser, currentOrder != null ? currentOrder.getStatusStep() : null);
                    if (cloudOrder.getCancelReason().isEmpty() && currentOrder != null) {
                        cloudOrder.setCancelReason(currentOrder.getCancelReason());
                    }
                    dbHelper.upsertOrderFromCloud(cloudOrder);
                    loadLocalOrder();
                });
    }

    private void renderOrder(Order order) {
        int step = order.getStatusStep();
        boolean isCancelled = step == Order.STEP_CANCELLED, isDelivered = step >= Order.STEP_DELIVERED;

        tvDetailHeaderSub.setText("#" + order.getOrderCode() + " - " + safe(order.getCreatedAt()));
        UiMotion.bindStatusBadge(this, tvDetailStatusBadge, step);

        if (isCancelled) {
            cardOrderCancelled.setVisibility(View.VISIBLE);
            layoutTimelineSteps.setVisibility(View.GONE);
            String reason = order.getCancelReason();
            tvCancelReasonDetail.setText((reason == null || reason.trim().isEmpty())
                    ? "Lý do hủy: Không ghi chú thêm" : "Lý do hủy: " + reason);
            btnReorder.setVisibility((order.getItemsRaw() != null && !order.getItemsRaw().trim().isEmpty()) ? View.VISIBLE : View.GONE);
        } else {
            cardOrderCancelled.setVisibility(View.GONE);
            layoutTimelineSteps.setVisibility(View.VISIBLE);
            renderTimeline(step);
        }

        String summary = order.getItemsSummary();
        tvDetailItemsSummary.setText((summary == null || summary.trim().isEmpty())
                ? "- Tổng số lượng: " + order.getItemCount() + " sản phẩm" : summary.replace("• ", "- "));
        tvDetailPaymentMethod.setText(safe(order.getPaymentMethod()));
        tvDetailTotalPrice.setText(safe(order.getTotalPrice()));
        tvDetailReceiver.setText(safe(order.getReceiverName()) + " - " + safe(order.getReceiverPhone()));
        tvDetailAddress.setText("Địa chỉ: " + safe(order.getReceiverAddress()));
        String note = order.getReceiverNote();
        tvDetailNote.setText((note == null || note.trim().isEmpty()) ? "Ghi chú: Không có" : "Ghi chú: " + note);
        tvDetailAccountOwner.setText("Tài khoản đặt: " + safe(order.getUserEmail()));

        btnCancelOrder.setVisibility((!isCancelled && !isDelivered && (isAdmin || step <= Order.STEP_PACKING)) ? View.VISIBLE : View.GONE);
        layoutAdminOrderActions.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        if (isAdmin) {
            btnAdminAdvanceStatus.setVisibility((isDelivered || isCancelled) ? View.GONE : View.VISIBLE);
            if (!isDelivered && !isCancelled) {
                btnAdminAdvanceStatus.setText("Chuyển sang: " + Order.getStepTitle(step + 1));
            }
        }
    }

    private void reorderCancelledItems() {
        if (currentOrder == null || currentOrder.getItemsRaw() == null || currentOrder.getItemsRaw().trim().isEmpty()) return;
        CartManager.syncForCurrentUser(this);
        CartManager.clearCart();

        List<Product> catalog = dbHelper.getAllProducts();
        int restored = 0;
        for (String entry : currentOrder.getItemsRaw().split(";")) {
            String[] parts = entry.split(":");
            if (parts.length < 2) continue;
            int qty = 1;
            try { qty = Math.max(1, Integer.parseInt(parts[1].trim())); } catch (NumberFormatException ignored) {}
            for (Product p : catalog) {
                if (p.getSku().equals(parts[0].trim())) {
                    CartManager.addToCart(p, qty);
                    restored++;
                    break;
                }
            }
        }
        if (restored == 0) {
            Toast.makeText(this, "Không tìm thấy sản phẩm trong kho để đặt lại!", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtra(PaymentActivity.EXTRA_PREFILL_NAME, currentOrder.getReceiverName());
        intent.putExtra(PaymentActivity.EXTRA_PREFILL_PHONE, currentOrder.getReceiverPhone());
        intent.putExtra(PaymentActivity.EXTRA_PREFILL_ADDRESS, currentOrder.getReceiverAddress());
        intent.putExtra(PaymentActivity.EXTRA_PREFILL_NOTE, currentOrder.getReceiverNote());
        startActivity(intent);
        finish();
    }

    private void renderTimeline(int currentStep) {
        layoutTimelineSteps.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        boolean isDelivered = currentStep >= Order.STEP_DELIVERED;

        int activeColor = ContextCompat.getColor(this, isDelivered ? R.color.success : R.color.primary);
        int activeText = ContextCompat.getColor(this, isDelivered ? R.color.success : R.color.primary_dark);
        int borderColor = ContextCompat.getColor(this, R.color.border);
        int inkColor = ContextCompat.getColor(this, R.color.ink);
        int secondary = ContextCompat.getColor(this, R.color.text_secondary);
        int white = ContextCompat.getColor(this, R.color.white);

        for (int i = 0; i <= Order.MAX_STEP; i++) {
            View row = inflater.inflate(R.layout.item_order_timeline_step, layoutTimelineSteps, false);
            MaterialCardView dot = row.findViewById(R.id.cardStepDot);
            TextView tvNum = row.findViewById(R.id.tvStepNumber);
            View connector = row.findViewById(R.id.viewStepConnector);
            TextView tvTitle = row.findViewById(R.id.tvStepTitle);
            TextView tvDesc = row.findViewById(R.id.tvStepDesc);
            TextView tvTag = row.findViewById(R.id.tvStepCurrentTag);

            boolean reached = i <= currentStep, isCurrent = i == currentStep;
            dot.setCardBackgroundColor(reached ? activeColor : borderColor);
            tvNum.setText(reached && !isCurrent ? "✓" : String.valueOf(i + 1));
            tvNum.setTextColor(reached ? white : secondary);

            connector.setVisibility(i == Order.MAX_STEP ? View.INVISIBLE : View.VISIBLE);
            if (i < Order.MAX_STEP) connector.setBackgroundColor(i < currentStep ? activeColor : borderColor);

            tvTitle.setText(Order.getStepTitle(i));
            tvDesc.setText(Order.getStepDescription(i));
            tvTitle.setTextColor(isCurrent ? activeText : (reached ? inkColor : secondary));
            tvTitle.setTypeface(null, reached ? Typeface.BOLD : Typeface.NORMAL);

            tvTag.setVisibility(isCurrent ? View.VISIBLE : View.GONE);
            if (isCurrent) {
                tvTag.setText(isDelivered ? "Hoàn tất" : "Hiện tại");
                tvTag.setBackgroundResource(isDelivered ? R.drawable.bg_badge_success : R.drawable.bg_badge_primary);
                tvTag.setTextColor(activeText);
            }
            layoutTimelineSteps.addView(row);
        }
    }

    private static String safe(String val) {
        return val == null ? "" : val;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (docListener != null) docListener.remove();
    }
}
