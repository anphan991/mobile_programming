package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PaymentActivity extends AppCompatActivity {

    public static final String EXTRA_PREFILL_NAME = "EXTRA_PREFILL_NAME";
    public static final String EXTRA_PREFILL_PHONE = "EXTRA_PREFILL_PHONE";
    public static final String EXTRA_PREFILL_ADDRESS = "EXTRA_PREFILL_ADDRESS";
    public static final String EXTRA_PREFILL_NOTE = "EXTRA_PREFILL_NOTE";

    private EditText edtReceiverName, edtReceiverPhone, edtReceiverAddress, edtReceiverNote;
    private LinearLayout layoutCartItems, layoutPayCOD, layoutPayBank;
    private TextView tvCartItemCount, tvEmptyCartNotice, tvOrderSubtotal, tvOrderDiscount, tvOrderTotal, tvVoucherStatus, tvVoucherActionPill;
    private RadioButton rbPayCOD, rbPayBank;
    private MaterialButton btnConfirmOrder;
    private String selectedPaymentMethod = "Tiền mặt khi nhận hàng (COD)";
    private String activeVoucherCode = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);
        UiMotion.applyInsets(findViewById(R.id.rootPayment));

        MaterialButton btnBack = findViewById(R.id.btnBack);
        MaterialButton btnViewOrders = findViewById(R.id.btnViewOrders);
        edtReceiverName = findViewById(R.id.edtReceiverName);
        edtReceiverPhone = findViewById(R.id.edtReceiverPhone);
        edtReceiverAddress = findViewById(R.id.edtReceiverAddress);
        edtReceiverNote = findViewById(R.id.edtReceiverNote);

        layoutCartItems = findViewById(R.id.layoutCartItems);
        tvCartItemCount = findViewById(R.id.tvCartItemCount);
        tvEmptyCartNotice = findViewById(R.id.tvEmptyCartNotice);
        tvOrderSubtotal = findViewById(R.id.tvOrderSubtotal);
        tvOrderDiscount = findViewById(R.id.tvOrderDiscount);
        tvOrderTotal = findViewById(R.id.tvOrderTotal);
        View layoutVoucherPicker = findViewById(R.id.layoutVoucherPicker);
        tvVoucherStatus = findViewById(R.id.tvVoucherStatus);
        tvVoucherActionPill = findViewById(R.id.tvVoucherActionPill);

        layoutPayCOD = findViewById(R.id.layoutPayCOD);
        layoutPayBank = findViewById(R.id.layoutPayBank);
        rbPayCOD = findViewById(R.id.rbPayCOD);
        rbPayBank = findViewById(R.id.rbPayBank);
        btnConfirmOrder = findViewById(R.id.btnConfirmOrder);

        UiMotion.bindTouchScale(btnBack, btnViewOrders, layoutPayCOD, layoutPayBank, layoutVoucherPicker, btnConfirmOrder);
        UiMotion.animateEntry(findViewById(R.id.cardPaymentContent), btnConfirmOrder);

        btnBack.setOnClickListener(v -> finish());
        if (btnViewOrders != null) {
            btnViewOrders.setOnClickListener(v -> startActivity(new Intent(this, OrderTrackingActivity.class)));
        }

        SharedPreferences session = getSharedPreferences("UserSession", MODE_PRIVATE);
        String currentUser = session.getString("CURRENT_USER", "admin");
        activeVoucherCode = session.getString("SAVED_VOUCHER", "TECH500K");
        if (layoutVoucherPicker != null) layoutVoucherPicker.setOnClickListener(v -> showVoucherPickerDialog());

        String prefillName = getIntent().getStringExtra(EXTRA_PREFILL_NAME);
        String prefillPhone = getIntent().getStringExtra(EXTRA_PREFILL_PHONE);
        String prefillAddress = getIntent().getStringExtra(EXTRA_PREFILL_ADDRESS);
        String prefillNote = getIntent().getStringExtra(EXTRA_PREFILL_NOTE);

        edtReceiverName.setText(prefillName != null && !prefillName.isEmpty() ? prefillName : currentUser);
        if (prefillPhone != null && !prefillPhone.isEmpty()) edtReceiverPhone.setText(prefillPhone);
        if (prefillAddress != null && !prefillAddress.isEmpty()) edtReceiverAddress.setText(prefillAddress);
        if (prefillNote != null && !prefillNote.isEmpty()) edtReceiverNote.setText(prefillNote);

        CartManager.syncForCurrentUser(this);
        int passedIndex = getIntent().getIntExtra(ProductDetailActivity.KEY_INDEX, -1);
        if (passedIndex >= 0 && passedIndex < ProductListActivity.productList.size()) {
            CartManager.addToCart(ProductListActivity.productList.get(passedIndex), 1);
        }

        View.OnClickListener chooseCOD = v -> selectPayment(false);
        View.OnClickListener chooseBank = v -> selectPayment(true);
        layoutPayCOD.setOnClickListener(chooseCOD);
        rbPayCOD.setOnClickListener(chooseCOD);
        layoutPayBank.setOnClickListener(chooseBank);
        rbPayBank.setOnClickListener(chooseBank);

        renderCartItems();
        btnConfirmOrder.setOnClickListener(v -> processCheckout());
    }

    private void renderCartItems() {
        layoutCartItems.removeAllViews();
        boolean isEmpty = CartManager.cartList.isEmpty();

        tvEmptyCartNotice.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        btnConfirmOrder.setEnabled(!isEmpty);
        btnConfirmOrder.setAlpha(!isEmpty ? 1f : 0.45f);

        if (isEmpty) {
            tvCartItemCount.setText("0 sản phẩm");
            tvOrderSubtotal.setText("0đ");
            if (tvOrderDiscount != null) tvOrderDiscount.setText("-0đ");
            tvOrderTotal.setText("0đ");
            btnConfirmOrder.setText("Đơn hàng rỗng");
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (CartItem item : CartManager.cartList) {
            Product p = item.getProduct();
            View v = inflater.inflate(R.layout.item_cart_product, layoutCartItems, false);
            ProductImageHelper.bindImage(v.findViewById(R.id.imgCartItem), p);
            ((TextView) v.findViewById(R.id.tvCartItemName)).setText(p.getName());
            ((TextView) v.findViewById(R.id.tvCartItemPrice)).setText(p.getPrice());
            ((TextView) v.findViewById(R.id.tvCartQuantity)).setText(String.valueOf(item.getQuantity()));
            v.findViewById(R.id.btnCartMinus).setOnClickListener(view -> { CartManager.updateQuantity(item, -1); renderCartItems(); });
            v.findViewById(R.id.btnCartPlus).setOnClickListener(view -> { CartManager.updateQuantity(item, 1); renderCartItems(); });
            layoutCartItems.addView(v);
        }

        long subtotal = CartManager.getTotalPrice();
        long discount = calculateVoucherDiscount(subtotal, activeVoucherCode);
        long finalTotal = Math.max(0, subtotal - discount);
        String finalFormatted = CartManager.formatPrice(finalTotal);

        tvCartItemCount.setText(CartManager.getTotalItemCount() + " sản phẩm");
        tvOrderSubtotal.setText(CartManager.formatPrice(subtotal));
        if (tvOrderDiscount != null) tvOrderDiscount.setText("-" + CartManager.formatPrice(discount));
        if (tvVoucherStatus != null && tvVoucherActionPill != null) {
            boolean applied = activeVoucherCode != null && !activeVoucherCode.isEmpty() && discount > 0;
            tvVoucherStatus.setText(applied
                    ? "Đã áp dụng " + activeVoucherCode + " • Giảm " + CartManager.formatPrice(discount)
                    : "Nhấn để chọn mã giảm đến 1.000.000đ");
            tvVoucherActionPill.setText(applied ? "Đổi mã" : "Chọn mã");
        }
        tvOrderTotal.setText(finalFormatted);
        btnConfirmOrder.setText("Xác nhận thanh toán (" + finalFormatted + ")");
    }

    private long calculateVoucherDiscount(long subtotal, String code) {
        if (subtotal <= 0 || code == null || code.isEmpty()) return 0;
        if ("TECH500K".equalsIgnoreCase(code)) return subtotal >= 10000000L ? 500000L : 200000L;
        if ("NEWMEMBER".equalsIgnoreCase(code)) return Math.min(1000000L, Math.round(subtotal * 0.05 / 1000.0) * 1000L);
        if ("FREESHIP2H".equalsIgnoreCase(code)) return 150000L;
        return 0;
    }

    private void showVoucherPickerDialog() {
        String[] options = {
                "TECH500K - Giảm 500.000đ (Đơn từ 10Tr, dưới 10Tr giảm 200K)",
                "NEWMEMBER - Giảm 5% tối đa 1.000.000đ cho thành viên",
                "FREESHIP2H - Ưu đãi giao hỏa tốc 2h giảm 150.000đ",
                "Không sử dụng mã giảm giá"
        };
        String[] codes = {"TECH500K", "NEWMEMBER", "FREESHIP2H", ""};
        int checked = 0;
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].equalsIgnoreCase(activeVoucherCode)) { checked = i; break; }
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Chọn Mã Ưu Đãi TechZone")
                .setSingleChoiceItems(options, checked, (dialog, which) -> {
                    activeVoucherCode = codes[which];
                    getSharedPreferences("UserSession", MODE_PRIVATE).edit().putString("SAVED_VOUCHER", activeVoucherCode).apply();
                    dialog.dismiss();
                    renderCartItems();
                    if (!activeVoucherCode.isEmpty()) {
                        Toast.makeText(this, "Đã áp dụng mã " + activeVoucherCode + "!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Đóng", null).show();
    }

    private void selectPayment(boolean isBank) {
        selectedPaymentMethod = isBank ? "Chuyển khoản / Thẻ ngân hàng (QR)" : "Tiền mặt khi nhận hàng (COD)";
        rbPayCOD.setChecked(!isBank);
        rbPayBank.setChecked(isBank);
        layoutPayCOD.setBackgroundResource(isBank ? R.drawable.bg_method_normal : R.drawable.bg_method_selected);
        layoutPayBank.setBackgroundResource(isBank ? R.drawable.bg_method_selected : R.drawable.bg_method_normal);
    }

    private void processCheckout() {
        if (CartManager.cartList.isEmpty()) {
            Toast.makeText(this, "Giỏ hàng rỗng!", Toast.LENGTH_SHORT).show();
            return;
        }
        String name = edtReceiverName.getText().toString().trim();
        String phone = edtReceiverPhone.getText().toString().trim();
        String address = edtReceiverAddress.getText().toString().trim();
        String note = edtReceiverNote.getText().toString().trim();
        if (name.isEmpty() || phone.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ họ tên, số điện thoại và địa chỉ nhận hàng!", Toast.LENGTH_SHORT).show();
            return;
        }

        long subtotal = CartManager.getTotalPrice();
        long discount = calculateVoucherDiscount(subtotal, activeVoucherCode);
        String totalFormatted = CartManager.formatPrice(Math.max(0, subtotal - discount));
        int totalItems = CartManager.getTotalItemCount();

        StringBuilder itemsBuilder = new StringBuilder();
        StringBuilder rawBuilder = new StringBuilder();
        for (CartItem item : CartManager.cartList) {
            if (itemsBuilder.length() > 0) itemsBuilder.append("\n");
            long lineTotal = CartManager.parsePrice(item.getProduct().getPrice()) * item.getQuantity();
            itemsBuilder.append("- ").append(item.getProduct().getName())
                    .append(" (x").append(item.getQuantity()).append(") - ")
                    .append(CartManager.formatPrice(lineTotal));
            if (rawBuilder.length() > 0) rawBuilder.append(";");
            rawBuilder.append(item.getProduct().getSku()).append(":").append(item.getQuantity());
        }
        showOrderDialog(false, "", name, phone, address, note, totalFormatted, totalItems, itemsBuilder.toString(), rawBuilder.toString());
    }

    private void showOrderDialog(boolean isSuccess, String orderCode, String name, String phone, String address,
                                 String note, String totalFormatted, int totalItems, String itemsSummary, String itemsRaw) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_order_summary, null);
        MaterialCardView cardIcon = v.findViewById(R.id.cardDialogIcon);
        ImageView imgIcon = v.findViewById(R.id.imgDialogIcon);
        TextView tvTitle = v.findViewById(R.id.tvDialogTitle);
        TextView tvSubtitle = v.findViewById(R.id.tvDialogSubtitle);
        TextView tvBadge = v.findViewById(R.id.tvDialogBadge);
        TextView tvReceiver = v.findViewById(R.id.tvDialogReceiver);
        TextView tvAddress = v.findViewById(R.id.tvDialogAddress);
        TextView tvNote = v.findViewById(R.id.tvDialogNote);
        TextView tvPayment = v.findViewById(R.id.tvDialogPayment);
        TextView tvItemsHeader = v.findViewById(R.id.tvDialogItemsHeader);
        TextView tvItems = v.findViewById(R.id.tvDialogItems);
        TextView tvTotal = v.findViewById(R.id.tvDialogTotal);
        MaterialButton btnNegative = v.findViewById(R.id.btnDialogNegative);
        MaterialButton btnPositive = v.findViewById(R.id.btnDialogPositive);

        tvReceiver.setText("Người nhận: " + name + " - " + phone);
        tvAddress.setText("Địa chỉ: " + address);
        if (!note.isEmpty()) {
            tvNote.setVisibility(View.VISIBLE);
            tvNote.setText("Ghi chú: " + note);
        }
        tvPayment.setText("Thanh toán: " + selectedPaymentMethod);
        tvItems.setText(itemsSummary);
        tvTotal.setText(totalFormatted);

        if (isSuccess) {
            int successColor = ContextCompat.getColor(this, R.color.success);
            cardIcon.setCardBackgroundColor(0xFFDCFCE7);
            imgIcon.setColorFilter(successColor);
            tvTitle.setText("Đặt hàng thành công");
            tvSubtitle.setText("Cảm ơn " + name + " đã đặt hàng tại TechZone");
            tvBadge.setVisibility(View.VISIBLE);
            tvBadge.setText("Mã đơn: #" + orderCode + " - " + Order.getStepTitle(Order.STEP_PROCESSING));
            tvBadge.setBackgroundResource(R.drawable.bg_badge_success);
            tvBadge.setTextColor(successColor);
            tvItemsHeader.setText("SẢN PHẨM ĐÃ ĐẶT (" + totalItems + " món)");
            btnNegative.setText("Về trang chủ");
            btnPositive.setText("Xem chi tiết đơn");
        } else {
            tvTitle.setText("Xác nhận đặt hàng");
            tvSubtitle.setText("Kiểm tra kỹ SĐT, địa chỉ và sản phẩm trước khi đặt");
            tvItemsHeader.setText("SẢN PHẨM ĐẶT MUA (" + totalItems + " món)");
            btnNegative.setText("Chỉnh sửa lại");
            btnPositive.setText("Xác nhận đặt hàng");
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this).setView(v).setCancelable(!isSuccess).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        btnNegative.setOnClickListener(view -> {
            dialog.dismiss();
            if (isSuccess) {
                Intent intent = new Intent(this, ProductListActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });
        btnPositive.setOnClickListener(view -> {
            dialog.dismiss();
            if (isSuccess) {
                Intent intent = new Intent(this, OrderDetailActivity.class);
                intent.putExtra(OrderDetailActivity.EXTRA_ORDER_CODE, orderCode);
                startActivity(intent);
                finish();
            } else {
                finalizeOrder(name, phone, address, note, totalFormatted, totalItems, itemsSummary, itemsRaw);
            }
        });
        dialog.show();
    }

    private void finalizeOrder(String name, String phone, String address, String note,
                               String totalFormatted, int totalItems, String itemsSummary, String itemsRaw) {
        String orderCode = "ORD-" + (Math.abs(System.currentTimeMillis() % 900000L) + 100000L);
        String currentUser = getSharedPreferences("UserSession", MODE_PRIVATE).getString("CURRENT_USER", "admin");

        new DatabaseHelper(this).insertOrder(orderCode, currentUser, name, phone, address, note,
                selectedPaymentMethod, totalFormatted, totalItems, itemsSummary, itemsRaw);

        Map<String, Object> orderMap = new HashMap<>();
        orderMap.put("orderCode", orderCode);
        orderMap.put("userEmail", currentUser);
        orderMap.put("receiverName", name);
        orderMap.put("receiverPhone", phone);
        orderMap.put("receiverAddress", address);
        orderMap.put("receiverNote", note);
        orderMap.put("paymentMethod", selectedPaymentMethod);
        orderMap.put("totalPrice", totalFormatted);
        orderMap.put("totalItems", totalItems);
        orderMap.put("itemsSummary", itemsSummary);
        orderMap.put("itemsRaw", itemsRaw);
        orderMap.put("cancelReason", "");
        orderMap.put("statusStep", Order.STEP_PROCESSING);
        orderMap.put("status", Order.getStepTitle(Order.STEP_PROCESSING));
        orderMap.put("createdAt", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date()));
        FirebaseFirestore.getInstance().collection("orders").document(orderCode).set(orderMap);

        CartManager.clearCart();
        renderCartItems();
        showOrderDialog(true, orderCode, name, phone, address, note, totalFormatted, totalItems, itemsSummary, itemsRaw);
    }
}
