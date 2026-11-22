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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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
    private View layoutVoucherPicker;
    private TextView tvCartItemCount, tvEmptyCartNotice, tvOrderSubtotal, tvOrderDiscount, tvOrderTotal, tvVoucherStatus, tvVoucherActionPill;
    private RadioButton rbPayCOD, rbPayBank;
    private MaterialButton btnConfirmOrder;
    private String selectedPaymentMethod = "Tiền mặt khi nhận hàng (COD)";
    private String activeVoucherCode = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        // Chống tràn status bar
        View root = findViewById(R.id.rootPayment);
        if (root != null) {
            int pl = root.getPaddingLeft(), pt = root.getPaddingTop(), pr = root.getPaddingRight(), pb = root.getPaddingBottom();
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
                v.setPadding(pl + bars.left, pt + bars.top, pr + bars.right, pb + bars.bottom);
                return insets;
            });
        }

        // Ánh xạ View
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
        layoutVoucherPicker = findViewById(R.id.layoutVoucherPicker);
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
            btnViewOrders.setOnClickListener(v -> {
                Intent intent = new Intent(PaymentActivity.this, OrderTrackingActivity.class);
                startActivity(intent);
            });
        }

        // Điền sẵn tên người dùng hiện tại hoặc thông tin từ đơn đặt lại
        SharedPreferences session = getSharedPreferences("UserSession", MODE_PRIVATE);
        String currentUser = session.getString("CURRENT_USER", "admin");
        activeVoucherCode = session.getString("SAVED_VOUCHER", "TECH500K");
        if (layoutVoucherPicker != null) {
            layoutVoucherPicker.setOnClickListener(v -> showVoucherPickerDialog());
        }
        String prefillName = getIntent().getStringExtra(EXTRA_PREFILL_NAME);
        String prefillPhone = getIntent().getStringExtra(EXTRA_PREFILL_PHONE);
        String prefillAddress = getIntent().getStringExtra(EXTRA_PREFILL_ADDRESS);
        String prefillNote = getIntent().getStringExtra(EXTRA_PREFILL_NOTE);

        edtReceiverName.setText(prefillName != null && !prefillName.isEmpty() ? prefillName : currentUser);
        if (prefillPhone != null && !prefillPhone.isEmpty()) edtReceiverPhone.setText(prefillPhone);
        if (prefillAddress != null && !prefillAddress.isEmpty()) edtReceiverAddress.setText(prefillAddress);
        if (prefillNote != null && !prefillNote.isEmpty()) edtReceiverNote.setText(prefillNote);

        // Đồng bộ giỏ hàng của tài khoản
        CartManager.syncForCurrentUser(this);

        // Kiểm tra nếu có sản phẩm được truyền trực tiếp từ trang chi tiết
        int passedIndex = getIntent().getIntExtra(ProductDetailActivity.KEY_INDEX, -1);
        if (passedIndex >= 0 && passedIndex < ProductListActivity.productList.size()) {
            CartManager.addToCart(ProductListActivity.productList.get(passedIndex), 1);
        }

        // Cài đặt chọn phương thức thanh toán
        setupPaymentMethods();

        // Hiển thị danh sách sản phẩm trong giỏ
        renderCartItems();

        // Nút đặt hàng
        btnConfirmOrder.setOnClickListener(v -> processCheckout());
    }

    // Hiển thị danh sách sản phẩm và tổng tiền giỏ hàng
    private void renderCartItems() {
        layoutCartItems.removeAllViews();
        boolean isEmpty = CartManager.cartList.isEmpty();

        // nếu giỏ rỗng: hiện thông báo rỗng và vô hiệu hóa nút đặt hàng
        tvEmptyCartNotice.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        btnConfirmOrder.setEnabled(!isEmpty);
        btnConfirmOrder.setAlpha(!isEmpty ? 1.0f : 0.45f);

        if (isEmpty) {
            tvCartItemCount.setText("0 sản phẩm");
            tvOrderSubtotal.setText("0đ");
            if (tvOrderDiscount != null) tvOrderDiscount.setText("-0đ");
            tvOrderTotal.setText("0đ");
            btnConfirmOrder.setText("Đơn hàng rỗng");
            return;
        }

        // Bơm từng dòng sản phẩm vào layoutCartItems
        LayoutInflater inflater = LayoutInflater.from(this);
        for (CartItem item : CartManager.cartList) {
            Product product = item.getProduct();
            View v = inflater.inflate(R.layout.item_cart_product, layoutCartItems, false);

            ImageView img = v.findViewById(R.id.imgCartItem);
            TextView tvName = v.findViewById(R.id.tvCartItemName);
            TextView tvPrice = v.findViewById(R.id.tvCartItemPrice);
            TextView tvQty = v.findViewById(R.id.tvCartQuantity);
            TextView btnMinus = v.findViewById(R.id.btnCartMinus);
            TextView btnPlus = v.findViewById(R.id.btnCartPlus);

            ProductImageHelper.bindImage(img, product);
            tvName.setText(product.getName());
            tvPrice.setText(product.getPrice());
            tvQty.setText(String.valueOf(item.getQuantity()));

            btnMinus.setOnClickListener(view -> {
                CartManager.updateQuantity(item, -1);
                renderCartItems();
            });

            btnPlus.setOnClickListener(view -> {
                CartManager.updateQuantity(item, 1);
                renderCartItems();
            });

            layoutCartItems.addView(v);
        }

        long subtotalMoney = CartManager.getTotalPrice();
        long discountMoney = calculateVoucherDiscount(subtotalMoney, activeVoucherCode);
        long finalTotalMoney = Math.max(0, subtotalMoney - discountMoney);

        String subtotalFormatted = CartManager.formatPrice(subtotalMoney);
        String discountFormatted = "-" + CartManager.formatPrice(discountMoney);
        String finalFormatted = CartManager.formatPrice(finalTotalMoney);
        int totalItems = CartManager.getTotalItemCount();

        tvCartItemCount.setText(totalItems + " sản phẩm");
        tvOrderSubtotal.setText(subtotalFormatted);
        if (tvOrderDiscount != null) {
            tvOrderDiscount.setText(discountFormatted);
        }
        updateVoucherBannerUi(discountMoney);
        tvOrderTotal.setText(finalFormatted);
        btnConfirmOrder.setText("Xác nhận thanh toán (" + finalFormatted + ")");
    }

    private long calculateVoucherDiscount(long subtotal, String code) {
        if (subtotal <= 0 || code == null || code.isEmpty()) return 0;
        if ("TECH500K".equalsIgnoreCase(code)) {
            return subtotal >= 10000000L ? 500000L : 200000L;
        }
        if ("NEWMEMBER".equalsIgnoreCase(code)) {
            return Math.min(1000000L, Math.round(subtotal * 0.05 / 1000.0) * 1000L);
        }
        if ("FREESHIP2H".equalsIgnoreCase(code)) {
            return 150000L;
        }
        return 0;
    }

    private void updateVoucherBannerUi(long discountMoney) {
        if (tvVoucherStatus == null || tvVoucherActionPill == null) return;
        if (activeVoucherCode == null || activeVoucherCode.isEmpty() || discountMoney <= 0) {
            tvVoucherStatus.setText("Nhấn để chọn mã giảm đến 1.000.000đ");
            tvVoucherActionPill.setText("Chọn mã");
        } else {
            tvVoucherStatus.setText("Đã áp dụng " + activeVoucherCode + " • Giảm " + CartManager.formatPrice(discountMoney));
            tvVoucherActionPill.setText("Đổi mã");
        }
    }

    private void showVoucherPickerDialog() {
        String[] options = new String[]{
                "TECH500K — Giảm 500.000đ (Đơn từ 10Tr, dưới 10Tr giảm 200K)",
                "NEWMEMBER — Giảm 5% tối đa 1.000.000đ cho thành viên",
                "FREESHIP2H — Ưu đãi giao hỏa tốc 2h giảm 150.000đ",
                "Không sử dụng mã giảm giá"
        };
        String[] codes = new String[]{"TECH500K", "NEWMEMBER", "FREESHIP2H", ""};
        int checked = 0;
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].equalsIgnoreCase(activeVoucherCode)) {
                checked = i;
                break;
            }
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Chọn Mã Ưu Đãi TechZone")
                .setSingleChoiceItems(options, checked, (dialog, which) -> {
                    activeVoucherCode = codes[which];
                    getSharedPreferences("UserSession", MODE_PRIVATE)
                            .edit()
                            .putString("SAVED_VOUCHER", activeVoucherCode)
                            .apply();
                    dialog.dismiss();
                    renderCartItems();
                    if (!activeVoucherCode.isEmpty()) {
                        Toast.makeText(this, "Đã áp dụng mã " + activeVoucherCode + "!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    // Thiết lập sự kiện chọn hình thức thanh toán
    private void setupPaymentMethods() {
        View.OnClickListener chooseCOD = v -> selectPayment(false);
        View.OnClickListener chooseBank = v -> selectPayment(true);

        layoutPayCOD.setOnClickListener(chooseCOD);
        rbPayCOD.setOnClickListener(chooseCOD);
        layoutPayBank.setOnClickListener(chooseBank);
        rbPayBank.setOnClickListener(chooseBank);
    }

    private void selectPayment(boolean isBank) {
        selectedPaymentMethod = isBank ? "Chuyển khoản / Thẻ ngân hàng (QR)" : "Tiền mặt khi nhận hàng (COD)";
        rbPayCOD.setChecked(!isBank);
        rbPayBank.setChecked(isBank);
        layoutPayCOD.setBackgroundResource(isBank ? R.drawable.bg_method_normal : R.drawable.bg_method_selected);
        layoutPayBank.setBackgroundResource(isBank ? R.drawable.bg_method_selected : R.drawable.bg_method_normal);
    }

    // Xác nhận và lưu đơn hàng vào SQLite
    private void processCheckout() {
        if (CartManager.cartList.isEmpty()) {
            Toast.makeText(this, "Giỏ hàng rỗng!", Toast.LENGTH_SHORT).show();
            return;
        }

        //Bắt buộc user phải điền đủ thông tin
        String name = edtReceiverName.getText().toString().trim();
        String phone = edtReceiverPhone.getText().toString().trim();
        String address = edtReceiverAddress.getText().toString().trim();
        String note = edtReceiverNote.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ họ tên, số điện thoại và địa chỉ nhận hàng!", Toast.LENGTH_SHORT).show();
            return;
        }

        long subtotalMoney = CartManager.getTotalPrice();
        long discountMoney = calculateVoucherDiscount(subtotalMoney, activeVoucherCode);
        long finalTotalMoney = Math.max(0, subtotalMoney - discountMoney);
        String totalFormatted = CartManager.formatPrice(finalTotalMoney);
        int totalItems = CartManager.getTotalItemCount();

        StringBuilder itemsBuilder = new StringBuilder();
        StringBuilder rawBuilder = new StringBuilder();
        for (CartItem item : CartManager.cartList) {
            if (itemsBuilder.length() > 0) itemsBuilder.append("\n");
            long lineTotal = CartManager.parsePrice(item.getProduct().getPrice()) * item.getQuantity();
            itemsBuilder.append("- ")
                    .append(item.getProduct().getName())
                    .append(" (x").append(item.getQuantity()).append(") - ")
                    .append(CartManager.formatPrice(lineTotal));

            if (rawBuilder.length() > 0) rawBuilder.append(";");
            rawBuilder.append(item.getProduct().getSku()).append(":").append(item.getQuantity());
        }
        String itemsSummary = itemsBuilder.toString();
        String itemsRaw = rawBuilder.toString();

        showPreOrderConfirmDialog(name, phone, address, note, totalFormatted, totalItems, itemsSummary, itemsRaw);
    }

    private void showPreOrderConfirmDialog(String name, String phone, String address, String note,
                                           String totalFormatted, int totalItems,
                                           String itemsSummary, String itemsRaw) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_order_summary, null);
        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvSubtitle = dialogView.findViewById(R.id.tvDialogSubtitle);
        TextView tvReceiver = dialogView.findViewById(R.id.tvDialogReceiver);
        TextView tvAddress = dialogView.findViewById(R.id.tvDialogAddress);
        TextView tvNote = dialogView.findViewById(R.id.tvDialogNote);
        TextView tvPayment = dialogView.findViewById(R.id.tvDialogPayment);
        TextView tvItemsHeader = dialogView.findViewById(R.id.tvDialogItemsHeader);
        TextView tvItems = dialogView.findViewById(R.id.tvDialogItems);
        TextView tvTotal = dialogView.findViewById(R.id.tvDialogTotal);
        MaterialButton btnNegative = dialogView.findViewById(R.id.btnDialogNegative);
        MaterialButton btnPositive = dialogView.findViewById(R.id.btnDialogPositive);

        tvTitle.setText("Xác nhận đặt hàng");
        tvSubtitle.setText("Kiểm tra kỹ SĐT, địa chỉ và sản phẩm trước khi đặt");
        tvReceiver.setText("Người nhận: " + name + " - " + phone);
        tvAddress.setText("Địa chỉ: " + address);
        if (!note.isEmpty()) {
            tvNote.setVisibility(View.VISIBLE);
            tvNote.setText("Ghi chú: " + note);
        }
        tvPayment.setText("Thanh toán: " + selectedPaymentMethod);
        tvItemsHeader.setText("SẢN PHẨM ĐẶT MUA (" + totalItems + " món)");
        tvItems.setText(itemsSummary);
        tvTotal.setText(totalFormatted);

        btnNegative.setText("Chỉnh sửa lại");
        btnPositive.setText("Xác nhận đặt hàng");

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnNegative.setOnClickListener(v -> dialog.dismiss());
        btnPositive.setOnClickListener(v -> {
            dialog.dismiss();
            finalizeOrder(name, phone, address, note, totalFormatted, totalItems, itemsSummary, itemsRaw);
        });
        dialog.show();
    }

    private void finalizeOrder(String name, String phone, String address, String note,
                               String totalFormatted, int totalItems,
                               String itemsSummary, String itemsRaw) {
        String orderCode = "ORD-" + (Math.abs(System.currentTimeMillis() % 900000L) + 100000L);

        SharedPreferences session = getSharedPreferences("UserSession", MODE_PRIVATE);
        String currentUser = session.getString("CURRENT_USER", "admin");
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

        showOrderSuccessDialog(orderCode, name, phone, address, note, totalFormatted, totalItems, itemsSummary);
    }

    private void showOrderSuccessDialog(String orderCode, String name, String phone, String address,
                                        String note, String totalFormatted, int totalItems, String itemsSummary) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_order_summary, null);
        MaterialCardView cardIcon = dialogView.findViewById(R.id.cardDialogIcon);
        ImageView imgIcon = dialogView.findViewById(R.id.imgDialogIcon);
        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvSubtitle = dialogView.findViewById(R.id.tvDialogSubtitle);
        TextView tvBadge = dialogView.findViewById(R.id.tvDialogBadge);
        TextView tvReceiver = dialogView.findViewById(R.id.tvDialogReceiver);
        TextView tvAddress = dialogView.findViewById(R.id.tvDialogAddress);
        TextView tvNote = dialogView.findViewById(R.id.tvDialogNote);
        TextView tvPayment = dialogView.findViewById(R.id.tvDialogPayment);
        TextView tvItemsHeader = dialogView.findViewById(R.id.tvDialogItemsHeader);
        TextView tvItems = dialogView.findViewById(R.id.tvDialogItems);
        TextView tvTotal = dialogView.findViewById(R.id.tvDialogTotal);
        MaterialButton btnNegative = dialogView.findViewById(R.id.btnDialogNegative);
        MaterialButton btnPositive = dialogView.findViewById(R.id.btnDialogPositive);

        int successColor = ContextCompat.getColor(this, R.color.success);
        cardIcon.setCardBackgroundColor(0xFFDCFCE7);
        imgIcon.setColorFilter(successColor);

        tvTitle.setText("Đặt hàng thành công");
        tvSubtitle.setText("Cảm ơn " + name + " đã đặt hàng tại Tech Store");
        tvBadge.setVisibility(View.VISIBLE);
        tvBadge.setText("Mã đơn: #" + orderCode + " - " + Order.getStepTitle(Order.STEP_PROCESSING));
        tvBadge.setBackgroundResource(R.drawable.bg_badge_success);
        tvBadge.setTextColor(successColor);

        tvReceiver.setText("Người nhận: " + name + " - " + phone);
        tvAddress.setText("Địa chỉ: " + address);
        if (!note.isEmpty()) {
            tvNote.setVisibility(View.VISIBLE);
            tvNote.setText("Ghi chú: " + note);
        }
        tvPayment.setText("Thanh toán: " + selectedPaymentMethod);
        tvItemsHeader.setText("SẢN PHẨM ĐÃ ĐẶT (" + totalItems + " món)");
        tvItems.setText(itemsSummary);
        tvTotal.setText(totalFormatted);

        btnNegative.setText("Về trang chủ");
        btnPositive.setText("Xem chi tiết đơn");

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnNegative.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(PaymentActivity.this, ProductListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
        btnPositive.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(PaymentActivity.this, OrderDetailActivity.class);
            intent.putExtra(OrderDetailActivity.EXTRA_ORDER_CODE, orderCode);
            startActivity(intent);
            finish();
        });
        dialog.show();
    }
}
