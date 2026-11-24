package com.example.myapplication;

import android.content.Context;
import android.widget.Toast;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.Map;

public class Order {
    public static final int STEP_CANCELLED = -1;
    public static final int STEP_PROCESSING = 0;
    public static final int STEP_PACKING = 1;
    public static final int STEP_SHIPPED_CARRIER = 2;
    public static final int STEP_OUT_FOR_DELIVERY = 3;
    public static final int STEP_DELIVERED = 4;
    public static final int MAX_STEP = STEP_DELIVERED;

    private final String orderCode, userEmail, receiverName, receiverPhone, receiverAddress,
            receiverNote, paymentMethod, totalPrice, createdAt, itemsSummary, itemsRaw;
    private final int itemCount;
    private int statusStep;
    private String cancelReason;

    public Order(String orderCode, String userEmail, String receiverName, String receiverPhone,
                 String receiverAddress, String receiverNote, String paymentMethod,
                 String totalPrice, int itemCount, String createdAt, int statusStep,
                 String itemsSummary, String cancelReason, String itemsRaw) {
        this.orderCode = orderCode;
        this.userEmail = userEmail;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.receiverAddress = receiverAddress;
        this.receiverNote = receiverNote;
        this.paymentMethod = paymentMethod;
        this.totalPrice = totalPrice;
        this.itemCount = itemCount;
        this.createdAt = createdAt;
        this.statusStep = clampStep(statusStep);
        this.itemsSummary = itemsSummary != null ? itemsSummary : "";
        this.cancelReason = cancelReason != null ? cancelReason : "";
        this.itemsRaw = itemsRaw != null ? itemsRaw : "";
    }

    public static Order fromFirestore(DocumentSnapshot doc, String fallbackUser, Integer localStep) {
        String code = doc.getString("orderCode");
        if (code == null || code.isEmpty()) code = doc.getId();
        String owner = doc.getString("userEmail");
        if (owner == null || owner.isEmpty()) owner = fallbackUser;
        Long itemsLong = doc.getLong("totalItems");
        int count = itemsLong != null ? itemsLong.intValue() : 1;

        int step = localStep != null ? localStep : STEP_PROCESSING;
        if (doc.contains("statusStep")) {
            Long stepLong = doc.getLong("statusStep");
            int cloudStep = stepLong != null ? clampStep(stepLong.intValue()) : STEP_PROCESSING;
            step = (doc.getMetadata().hasPendingWrites() && localStep != null) ? localStep : cloudStep;
        }
        return new Order(
                code, owner,
                doc.getString("receiverName"), doc.getString("receiverPhone"),
                doc.getString("receiverAddress"), doc.getString("receiverNote"),
                doc.getString("paymentMethod"), doc.getString("totalPrice"),
                count, doc.getString("createdAt"), step,
                doc.getString("itemsSummary"), doc.getString("cancelReason"), doc.getString("itemsRaw")
        );
    }

    public static void syncSnapshotsToLocal(DatabaseHelper db, QuerySnapshot snapshots, String fallbackUser) {
        if (db == null || snapshots == null) return;
        Map<String, Integer> localSteps = new HashMap<>();
        for (Order o : db.getAllOrders()) localSteps.put(o.getOrderCode(), o.getStatusStep());
        for (DocumentSnapshot doc : snapshots.getDocuments()) {
            String code = doc.getString("orderCode") != null ? doc.getString("orderCode") : doc.getId();
            db.upsertOrderFromCloud(fromFirestore(doc, fallbackUser, localSteps.get(code)));
        }
    }

    public static void updateStepAndSync(Context ctx, Order order, int targetStep, Runnable onUpdated) {
        if (ctx == null || order == null) return;
        int clamped = clampStep(targetStep);
        order.setStatusStep(clamped);
        new DatabaseHelper(ctx).updateOrderStatus(order.getOrderCode(), clamped);

        Map<String, Object> updates = new HashMap<>();
        updates.put("statusStep", clamped);
        updates.put("status", getStepTitle(clamped));
        FirebaseFirestore.getInstance().collection("orders").document(order.getOrderCode()).set(updates, SetOptions.merge());

        Toast.makeText(ctx, "Đơn #" + order.getOrderCode() + " -> " + getStepTitle(clamped), Toast.LENGTH_SHORT).show();
        if (onUpdated != null) onUpdated.run();
    }

    public static void showStepPicker(Context ctx, Order order, Runnable onUpdated) {
        if (ctx == null || order == null) return;
        String[] titles = {
                "1. " + getStepTitle(STEP_PROCESSING),
                "2. " + getStepTitle(STEP_PACKING),
                "3. " + getStepTitle(STEP_SHIPPED_CARRIER),
                "4. " + getStepTitle(STEP_OUT_FOR_DELIVERY),
                "5. " + getStepTitle(STEP_DELIVERED),
                "Hủy đơn hàng (nhập lý do)"
        };
        int checked = order.getStatusStep() == STEP_CANCELLED ? 5 : order.getStatusStep();
        new MaterialAlertDialogBuilder(ctx)
                .setTitle("Cập nhật đơn #" + order.getOrderCode())
                .setSingleChoiceItems(titles, checked, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == 5) {
                        OrderCancelHelper.showCancelDialog(ctx, order, true, r -> { if (onUpdated != null) onUpdated.run(); });
                    } else {
                        updateStepAndSync(ctx, order, which, onUpdated);
                    }
                })
                .setNegativeButton("Đóng", null).show();
    }

    public static int clampStep(int step) {
        return step == STEP_CANCELLED ? STEP_CANCELLED : Math.max(STEP_PROCESSING, Math.min(MAX_STEP, step));
    }

    public static String getStepTitle(int step) {
        switch (clampStep(step)) {
            case STEP_CANCELLED: return "Đã hủy";
            case STEP_PACKING: return "Đang đóng gói";
            case STEP_SHIPPED_CARRIER: return "Đã giao cho đơn vị vận chuyển";
            case STEP_OUT_FOR_DELIVERY: return "Đang giao đến bạn";
            case STEP_DELIVERED: return "Giao thành công";
            default: return "Đang xử lý";
        }
    }

    public static String getStepDescription(int step) {
        switch (clampStep(step)) {
            case STEP_CANCELLED: return "Đơn hàng đã bị hủy";
            case STEP_PACKING: return "Cửa hàng đang kiểm tra và đóng gói sản phẩm";
            case STEP_SHIPPED_CARRIER: return "Đơn hàng đã bàn giao cho đơn vị vận chuyển";
            case STEP_OUT_FOR_DELIVERY: return "Nhân viên giao hàng đang trên đường giao đến bạn";
            case STEP_DELIVERED: return "Đơn hàng đã được giao thành công";
            default: return "Đơn hàng đã được tiếp nhận và chờ xử lý";
        }
    }

    public String getOrderCode() { return orderCode; }
    public String getUserEmail() { return userEmail; }
    public String getReceiverName() { return receiverName; }
    public String getReceiverPhone() { return receiverPhone; }
    public String getReceiverAddress() { return receiverAddress; }
    public String getReceiverNote() { return receiverNote; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getTotalPrice() { return totalPrice; }
    public int getItemCount() { return itemCount; }
    public String getCreatedAt() { return createdAt; }
    public String getItemsSummary() { return itemsSummary; }
    public String getItemsRaw() { return itemsRaw; }
    public int getStatusStep() { return statusStep; }
    public String getCancelReason() { return cancelReason; }

    public void setStatusStep(int statusStep) { this.statusStep = clampStep(statusStep); }
    public void setCancelReason(String reason) { this.cancelReason = reason != null ? reason : ""; }
}
