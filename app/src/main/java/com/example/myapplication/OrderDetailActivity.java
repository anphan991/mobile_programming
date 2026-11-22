package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Màn hình xem chi tiết tình trạng đơn hàng (5 bước dọc + thông tin sản phẩm & giao nhận).
 * User có thể hủy đơn kèm lý do khi đơn chưa giao cho ĐVVC và đặt lại đơn ngay để sửa SĐT/địa chỉ.
 * Chỉ Admin mới có quyền cập nhật các mốc trạng thái.
 */
public class OrderDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ORDER_CODE = "EXTRA_ORDER_CODE";

    private DatabaseHelper dbHelper;
    private String orderCode;
    private String currentUser;
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

        View root = findViewById(R.id.rootOrderDetail);
        if (root != null) {
            final int pl = root.getPaddingLeft();
            final int pt = root.getPaddingTop();
            final int pr = root.getPaddingRight();
            final int pb = root.getPaddingBottom();
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets bars = insets.getInsets(
                        WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
                );
                v.setPadding(pl + bars.left, pt + bars.top, pr + bars.right, pb + bars.bottom);
                return insets;
            });
        }

        dbHelper = new DatabaseHelper(this);
        SharedPreferences session = getSharedPreferences("UserSession", MODE_PRIVATE);
        currentUser = session.getString("CURRENT_USER", "admin");
        isAdmin = dbHelper.isAdminUser(currentUser);

        orderCode = getIntent().getStringExtra(EXTRA_ORDER_CODE);
        if (orderCode == null || orderCode.trim().isEmpty()) {
            Toast.makeText(this, "Không tìm thấy mã đơn hàng!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindViews();
        setupListeners();
        loadLocalOrder();
        listenCloudOrder();
    }

    private void bindViews() {
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

        UiMotion.bindTouchScale(
                findViewById(R.id.btnBack),
                findViewById(R.id.btnAllOrdersList),
                btnAdminAdvanceStatus,
                btnAdminSelectStatus,
                btnCancelOrder,
                btnReorder
        );
    }

    private void setupListeners() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAllOrdersList).setOnClickListener(v -> {
            startActivity(new Intent(OrderDetailActivity.this, OrderTrackingActivity.class));
            finish();
        });

        if (btnAdminAdvanceStatus != null) {
            btnAdminAdvanceStatus.setOnClickListener(v -> {
                if (!isAdmin || currentOrder == null) return;
                int next = Order.clampStep(currentOrder.getStatusStep() + 1);
                updateOrderStep(next);
            });
        }

        if (btnAdminSelectStatus != null) {
            btnAdminSelectStatus.setOnClickListener(v -> {
                if (!isAdmin || currentOrder == null) return;
                showStepPickerDialog();
            });
        }

        if (btnCancelOrder != null) {
            btnCancelOrder.setOnClickListener(v -> {
                if (currentOrder == null) return;
                OrderCancelHelper.showCancelDialog(this, currentOrder, isAdmin, reason -> loadLocalOrder());
            });
        }

        if (btnReorder != null) {
            btnReorder.setOnClickListener(v -> reorderCancelledItems());
        }
    }

    private void loadLocalOrder() {
        Order found = dbHelper.getOrderByCode(orderCode);
        if (found != null) {
            currentOrder = found;
            renderOrder(found);
        }
    }

    private void listenCloudOrder() {
        docListener = FirebaseFirestore.getInstance()
                .collection("orders")
                .document(orderCode)
                .addSnapshotListener((doc, error) -> {
                    if (error != null || doc == null || !doc.exists() || isFinishing() || isDestroyed()) {
                        return;
                    }
                    Long itemsLong = doc.getLong("totalItems");
                    int itemCount = itemsLong != null ? itemsLong.intValue() : 1;
                    int step = currentOrder != null ? currentOrder.getStatusStep() : Order.STEP_PROCESSING;
                    if (doc.contains("statusStep")) {
                        Long stepLong = doc.getLong("statusStep");
                        if (stepLong != null && !doc.getMetadata().hasPendingWrites()) {
                            step = Order.clampStep(stepLong.intValue());
                        }
                    }

                    String owner = doc.getString("userEmail");
                    if (owner == null || owner.isEmpty()) owner = currentUser;

                    String cancelReason = doc.getString("cancelReason");
                    if ((cancelReason == null || cancelReason.isEmpty()) && currentOrder != null) {
                        cancelReason = currentOrder.getCancelReason();
                    }
                    String itemsRaw = doc.getString("itemsRaw");
                    if ((itemsRaw == null || itemsRaw.isEmpty()) && currentOrder != null) {
                        itemsRaw = currentOrder.getItemsRaw();
                    }

                    Order cloudOrder = new Order(
                            orderCode,
                            owner,
                            doc.getString("receiverName"),
                            doc.getString("receiverPhone"),
                            doc.getString("receiverAddress"),
                            doc.getString("receiverNote"),
                            doc.getString("paymentMethod"),
                            doc.getString("totalPrice"),
                            itemCount,
                            doc.getString("createdAt"),
                            step,
                            doc.getString("itemsSummary"),
                            cancelReason,
                            itemsRaw
                    );
                    dbHelper.upsertOrderFromCloud(cloudOrder);
                    currentOrder = cloudOrder;
                    renderOrder(cloudOrder);
                });
    }

    private void renderOrder(Order order) {
        int step = order.getStatusStep();
        boolean isCancelled = step == Order.STEP_CANCELLED;
        boolean isDelivered = step >= Order.STEP_DELIVERED;

        tvDetailHeaderSub.setText("#" + order.getOrderCode() + " - " + safe(order.getCreatedAt()));
        tvDetailStatusBadge.setText(Order.getStepTitle(step));

        int successColor = ContextCompat.getColor(this, R.color.success);
        int primaryDark = ContextCompat.getColor(this, R.color.primary_dark);
        int dangerColor = ContextCompat.getColor(this, R.color.danger);

        if (isCancelled) {
            tvDetailStatusBadge.setBackgroundResource(R.drawable.bg_badge_danger);
            tvDetailStatusBadge.setTextColor(dangerColor);
            cardOrderCancelled.setVisibility(View.VISIBLE);
            layoutTimelineSteps.setVisibility(View.GONE);
            String reason = order.getCancelReason();
            tvCancelReasonDetail.setText((reason == null || reason.trim().isEmpty())
                    ? "Lý do hủy: Không ghi chú thêm"
                    : "Lý do hủy: " + reason);
            boolean canReorder = order.getItemsRaw() != null && !order.getItemsRaw().trim().isEmpty();
            btnReorder.setVisibility(canReorder ? View.VISIBLE : View.GONE);
        } else if (isDelivered) {
            tvDetailStatusBadge.setBackgroundResource(R.drawable.bg_badge_success);
            tvDetailStatusBadge.setTextColor(successColor);
            cardOrderCancelled.setVisibility(View.GONE);
            layoutTimelineSteps.setVisibility(View.VISIBLE);
            renderTimeline(step);
        } else {
            tvDetailStatusBadge.setBackgroundResource(R.drawable.bg_badge_primary);
            tvDetailStatusBadge.setTextColor(primaryDark);
            cardOrderCancelled.setVisibility(View.GONE);
            layoutTimelineSteps.setVisibility(View.VISIBLE);
            renderTimeline(step);
        }

        String itemsSummary = order.getItemsSummary();
        if (itemsSummary == null || itemsSummary.trim().isEmpty()) {
            tvDetailItemsSummary.setText("- Tổng số lượng: " + order.getItemCount() + " sản phẩm");
        } else {
            tvDetailItemsSummary.setText(itemsSummary.replace("• ", "- "));
        }

        tvDetailPaymentMethod.setText(safe(order.getPaymentMethod()));
        tvDetailTotalPrice.setText(safe(order.getTotalPrice()));
        tvDetailReceiver.setText(safe(order.getReceiverName()) + " - " + safe(order.getReceiverPhone()));
        tvDetailAddress.setText("Địa chỉ: " + safe(order.getReceiverAddress()));

        String note = order.getReceiverNote();
        tvDetailNote.setText((note == null || note.trim().isEmpty())
                ? "Ghi chú: Không có"
                : "Ghi chú: " + note);
        tvDetailAccountOwner.setText("Tài khoản đặt: " + safe(order.getUserEmail()));

        // Nút Hủy đơn hàng: User hủy được khi Đang xử lý hoặc Đang đóng gói; Admin hủy được khi chưa Hoàn tất / chưa Hủy
        boolean canCancel = !isCancelled && !isDelivered
                && (isAdmin || step <= Order.STEP_PACKING);
        btnCancelOrder.setVisibility(canCancel ? View.VISIBLE : View.GONE);

        // Chỉ Admin mới thấy khối điều khiển cập nhật trạng thái đơn hàng
        if (isAdmin) {
            layoutAdminOrderActions.setVisibility(View.VISIBLE);
            if (isDelivered || isCancelled) {
                btnAdminAdvanceStatus.setVisibility(View.GONE);
            } else {
                btnAdminAdvanceStatus.setVisibility(View.VISIBLE);
                btnAdminAdvanceStatus.setText("Chuyển sang: " + Order.getStepTitle(step + 1));
            }
        } else {
            layoutAdminOrderActions.setVisibility(View.GONE);
        }
    }

    private void reorderCancelledItems() {
        if (currentOrder == null || currentOrder.getItemsRaw() == null || currentOrder.getItemsRaw().trim().isEmpty()) {
            return;
        }
        CartManager.syncForCurrentUser(this);
        CartManager.clearCart();

        List<Product> catalog = dbHelper.getAllProducts();
        int restoredCount = 0;
        String[] entries = currentOrder.getItemsRaw().split(";");
        for (String entry : entries) {
            String[] parts = entry.split(":");
            if (parts.length < 2) continue;
            String sku = parts[0].trim();
            int qty;
            try {
                qty = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException e) {
                qty = 1;
            }
            if (qty <= 0) qty = 1;
            for (Product p : catalog) {
                if (p.getSku().equals(sku)) {
                    CartManager.addToCart(p, qty);
                    restoredCount++;
                    break;
                }
            }
        }

        if (restoredCount == 0) {
            Toast.makeText(this, "Không tìm thấy sản phẩm trong kho để đặt lại!", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(OrderDetailActivity.this, PaymentActivity.class);
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
        int activeTextColor = ContextCompat.getColor(this, isDelivered ? R.color.success : R.color.primary_dark);
        int borderColor = ContextCompat.getColor(this, R.color.border);
        int inkColor = ContextCompat.getColor(this, R.color.ink);
        int textSecondary = ContextCompat.getColor(this, R.color.text_secondary);
        int whiteColor = ContextCompat.getColor(this, R.color.white);

        for (int i = 0; i <= Order.MAX_STEP; i++) {
            View row = inflater.inflate(R.layout.item_order_timeline_step, layoutTimelineSteps, false);
            MaterialCardView dot = row.findViewById(R.id.cardStepDot);
            TextView tvNum = row.findViewById(R.id.tvStepNumber);
            View connector = row.findViewById(R.id.viewStepConnector);
            TextView tvTitle = row.findViewById(R.id.tvStepTitle);
            TextView tvDesc = row.findViewById(R.id.tvStepDesc);
            TextView tvCurrentTag = row.findViewById(R.id.tvStepCurrentTag);

            boolean reached = i <= currentStep;
            boolean isCurrent = i == currentStep;

            dot.setCardBackgroundColor(reached ? activeColor : borderColor);
            tvNum.setText(reached && !isCurrent ? "✓" : String.valueOf(i + 1));
            tvNum.setTextColor(reached ? whiteColor : textSecondary);

            if (i == Order.MAX_STEP) {
                connector.setVisibility(View.INVISIBLE);
            } else {
                connector.setVisibility(View.VISIBLE);
                connector.setBackgroundColor(i < currentStep ? activeColor : borderColor);
            }

            tvTitle.setText(Order.getStepTitle(i));
            tvDesc.setText(Order.getStepDescription(i));
            tvTitle.setTextColor(isCurrent ? activeTextColor : (reached ? inkColor : textSecondary));
            tvTitle.setTypeface(null, reached ? Typeface.BOLD : Typeface.NORMAL);

            if (isCurrent) {
                tvCurrentTag.setVisibility(View.VISIBLE);
                tvCurrentTag.setText(isDelivered ? "Hoàn tất" : "Hiện tại");
                if (isDelivered) {
                    tvCurrentTag.setBackgroundResource(R.drawable.bg_badge_success);
                    tvCurrentTag.setTextColor(activeTextColor);
                } else {
                    tvCurrentTag.setBackgroundResource(R.drawable.bg_badge_primary);
                    tvCurrentTag.setTextColor(activeTextColor);
                }
            } else {
                tvCurrentTag.setVisibility(View.GONE);
            }

            layoutTimelineSteps.addView(row);
        }
    }

    private void showStepPickerDialog() {
        String[] stepTitles = new String[]{
                "1. " + Order.getStepTitle(Order.STEP_PROCESSING),
                "2. " + Order.getStepTitle(Order.STEP_PACKING),
                "3. " + Order.getStepTitle(Order.STEP_SHIPPED_CARRIER),
                "4. " + Order.getStepTitle(Order.STEP_OUT_FOR_DELIVERY),
                "5. " + Order.getStepTitle(Order.STEP_DELIVERED),
                "Hủy đơn hàng (nhập lý do)"
        };
        int checked = currentOrder.getStatusStep() == Order.STEP_CANCELLED ? 5 : currentOrder.getStatusStep();
        new MaterialAlertDialogBuilder(this)
                .setTitle("Chọn trạng thái đơn hàng #" + orderCode)
                .setSingleChoiceItems(stepTitles, checked, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == 5) {
                        OrderCancelHelper.showCancelDialog(this, currentOrder, true, reason -> loadLocalOrder());
                    } else {
                        updateOrderStep(which);
                    }
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void updateOrderStep(int targetStep) {
        int clamped = Order.clampStep(targetStep);
        currentOrder.setStatusStep(clamped);
        dbHelper.updateOrderStatus(orderCode, clamped);

        Map<String, Object> updates = new HashMap<>();
        updates.put("statusStep", clamped);
        updates.put("status", Order.getStepTitle(clamped));
        FirebaseFirestore.getInstance()
                .collection("orders")
                .document(orderCode)
                .set(updates, SetOptions.merge());

        renderOrder(currentOrder);
        Toast.makeText(this,
                "Đơn #" + orderCode + " -> " + Order.getStepTitle(clamped),
                Toast.LENGTH_SHORT).show();
    }

    private String safe(String val) {
        return val == null ? "" : val;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (docListener != null) {
            docListener.remove();
            docListener = null;
        }
    }
}
