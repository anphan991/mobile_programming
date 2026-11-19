package com.example.myapplication;

/**
 * Mô hình đơn đặt hàng và tiến trình trạng thái vận chuyển (0..4).
 */
public class Order {
    public static final int STEP_CANCELLED = -1;
    public static final int STEP_PROCESSING = 0;
    public static final int STEP_PACKING = 1;
    public static final int STEP_SHIPPED_CARRIER = 2;
    public static final int STEP_OUT_FOR_DELIVERY = 3;
    public static final int STEP_DELIVERED = 4;
    public static final int MAX_STEP = STEP_DELIVERED;

    private final String orderCode;
    private final String userEmail;
    private final String receiverName;
    private final String receiverPhone;
    private final String receiverAddress;
    private final String receiverNote;
    private final String paymentMethod;
    private final String totalPrice;
    private final int itemCount;
    private final String createdAt;
    private final String itemsSummary;
    private final String itemsRaw;
    private int statusStep;
    private String cancelReason;

    public Order(String orderCode, String userEmail, String receiverName, String receiverPhone,
                 String receiverAddress, String receiverNote, String paymentMethod,
                 String totalPrice, int itemCount, String createdAt, int statusStep) {
        this(orderCode, userEmail, receiverName, receiverPhone, receiverAddress,
                receiverNote, paymentMethod, totalPrice, itemCount, createdAt, statusStep, "", "", "");
    }

    public Order(String orderCode, String userEmail, String receiverName, String receiverPhone,
                 String receiverAddress, String receiverNote, String paymentMethod,
                 String totalPrice, int itemCount, String createdAt, int statusStep, String itemsSummary) {
        this(orderCode, userEmail, receiverName, receiverPhone, receiverAddress,
                receiverNote, paymentMethod, totalPrice, itemCount, createdAt, statusStep, itemsSummary, "", "");
    }

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

    public static int clampStep(int step) {
        if (step == STEP_CANCELLED) {
            return STEP_CANCELLED;
        }
        return Math.max(STEP_PROCESSING, Math.min(MAX_STEP, step));
    }

    public static String getStepTitle(int step) {
        switch (clampStep(step)) {
            case STEP_CANCELLED:
                return "Đã hủy";
            case STEP_PACKING:
                return "Đang đóng gói";
            case STEP_SHIPPED_CARRIER:
                return "Đã giao ĐVVC";
            case STEP_OUT_FOR_DELIVERY:
                return "Đang giao đến bạn";
            case STEP_DELIVERED:
                return "Giao thành công";
            case STEP_PROCESSING:
            default:
                return "Đang xử lý";
        }
    }

    public static String getStepDescription(int step) {
        switch (clampStep(step)) {
            case STEP_CANCELLED:
                return "Đơn hàng đã bị hủy";
            case STEP_PACKING:
                return "Cửa hàng đang kiểm tra và đóng gói sản phẩm";
            case STEP_SHIPPED_CARRIER:
                return "Đơn hàng đã bàn giao cho đơn vị vận chuyển";
            case STEP_OUT_FOR_DELIVERY:
                return "Nhân viên giao hàng đang trên đường giao đến bạn";
            case STEP_DELIVERED:
                return "Đơn hàng đã được giao thành công";
            case STEP_PROCESSING:
            default:
                return "Đơn hàng đã được tiếp nhận và chờ xử lý";
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

    public void setStatusStep(int statusStep) {
        this.statusStep = clampStep(statusStep);
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason != null ? cancelReason : "";
    }
}
