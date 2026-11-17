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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PaymentActivity extends AppCompatActivity {

    private EditText edtReceiverName, edtReceiverPhone, edtReceiverAddress, edtReceiverNote;
    private LinearLayout layoutCartItems, layoutPayCOD, layoutPayBank;
    private TextView tvCartItemCount, tvEmptyCartNotice, tvOrderSubtotal, tvOrderTotal;
    private RadioButton rbPayCOD, rbPayBank;
    private MaterialButton btnConfirmOrder;
    private String selectedPaymentMethod = "Tiền mặt khi nhận hàng (COD)";

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
        edtReceiverName = findViewById(R.id.edtReceiverName);
        edtReceiverPhone = findViewById(R.id.edtReceiverPhone);
        edtReceiverAddress = findViewById(R.id.edtReceiverAddress);
        edtReceiverNote = findViewById(R.id.edtReceiverNote);

        layoutCartItems = findViewById(R.id.layoutCartItems);
        tvCartItemCount = findViewById(R.id.tvCartItemCount);
        tvEmptyCartNotice = findViewById(R.id.tvEmptyCartNotice);
        tvOrderSubtotal = findViewById(R.id.tvOrderSubtotal);
        tvOrderTotal = findViewById(R.id.tvOrderTotal);

        layoutPayCOD = findViewById(R.id.layoutPayCOD);
        layoutPayBank = findViewById(R.id.layoutPayBank);
        rbPayCOD = findViewById(R.id.rbPayCOD);
        rbPayBank = findViewById(R.id.rbPayBank);
        btnConfirmOrder = findViewById(R.id.btnConfirmOrder);

        btnBack.setOnClickListener(v -> finish());

        // Điền sẵn tên người dùng hiện tại
        SharedPreferences session = getSharedPreferences("UserSession", MODE_PRIVATE);
        String currentUser = session.getString("CURRENT_USER", "admin");
        edtReceiverName.setText(currentUser);

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

            img.setImageResource(product.getImageResId());
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

        long totalMoney = CartManager.getTotalPrice();
        // Cập nhật tổng tiền và nhãn nút xác nhận
        String formatted = CartManager.formatPrice(totalMoney);
        int totalItems = CartManager.getTotalItemCount();

        tvCartItemCount.setText(totalItems + " sản phẩm");
        tvOrderSubtotal.setText(formatted);
        tvOrderTotal.setText(formatted);
        btnConfirmOrder.setText("Xác nhận thanh toán (" + formatted + ")");
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

        // Sinh mã đơn hàng ngẫu nhiên
        // thời gian thực của máy tính tính bằng mili-giây chia lấy dư cho 900.000 rồi cộng 100.000 (100000 đến 999999)
        String orderCode = "ORD-" + (Math.abs(System.currentTimeMillis() % 900000L) + 100000L);
        String totalFormatted = CartManager.formatPrice(CartManager.getTotalPrice());
        int totalItems = CartManager.getTotalItemCount();

        // Lưu đơn hàng vào SQLite (bảng orders)
        SharedPreferences session = getSharedPreferences("UserSession", MODE_PRIVATE);
        String currentUser = session.getString("CURRENT_USER", "admin");
        new DatabaseHelper(this).insertOrder(orderCode, currentUser, name, phone, address, note, selectedPaymentMethod, totalFormatted, totalItems);

        // Lưu đơn hàng lên Cloud Firestore (collection orders)
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
        orderMap.put("createdAt", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date()));

        FirebaseFirestore.getInstance().collection("orders").document(orderCode).set(orderMap);

        // Hiển thị hộp thoại tóm tắt đơn hàng
        String summary = "Cảm ơn " + name + " đã đặt hàng tại Tech Store!\n\n"
                + "• Mã đơn hàng: #" + orderCode + "\n"
                + "• Số lượng: " + totalItems + " sản phẩm\n"
                + "• Người nhận: " + name + " - " + phone + "\n"
                + "• Địa chỉ: " + address + "\n"
                + (note.isEmpty() ? "" : "• Ghi chú: " + note + "\n")
                + "• Thanh toán: " + selectedPaymentMethod + "\n"
                + "• Tổng tiền: " + totalFormatted + "\n\n"
                + "Đơn hàng của bạn đang được đóng gói và bàn giao cho đơn vị vận chuyển.";

        new AlertDialog.Builder(this)
                .setTitle("Đặt hàng thành công")
                .setMessage(summary)
                .setCancelable(false)
                .setPositiveButton("Về trang chủ", (dialog, which) -> {
                    CartManager.clearCart();
                    Intent intent = new Intent(PaymentActivity.this, ProductListActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .show();
    }
}
