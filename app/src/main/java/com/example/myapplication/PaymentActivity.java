package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
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

        // Xử lý WindowInsets để chống tràn status bar
        View rootView = findViewById(R.id.rootPayment);
        if (rootView != null) {
            final int baseLeft = rootView.getPaddingLeft();
            final int baseTop = rootView.getPaddingTop();
            final int baseRight = rootView.getPaddingRight();
            final int baseBottom = rootView.getPaddingBottom();
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                Insets systemBars = insets.getInsets(
                        WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
                );
                v.setPadding(
                        baseLeft + systemBars.left,
                        baseTop + systemBars.top,
                        baseRight + systemBars.right,
                        baseBottom + systemBars.bottom
                );
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

        // Nút quay lại
        btnBack.setOnClickListener(v -> finish());

        // Đọc tên người dùng từ SharedPreferences để tự động điền họ tên người nhận
        SharedPreferences sessionPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        String currentUsername = sessionPref.getString("CURRENT_USER", "admin");
        if (currentUsername != null && !currentUsername.trim().isEmpty()) {
            edtReceiverName.setText(currentUsername);
        }

        // Đồng bộ giỏ hàng riêng của tài khoản đang đăng nhập
        CartManager.syncForCurrentUser(this);

        // Kiểm tra xem có sản phẩm được truyền trực tiếp từ trang chi tiết không
        int passedIndex = getIntent().getIntExtra(ProductDetailActivity.KEY_INDEX, -1);
        if (passedIndex >= 0 && passedIndex < ProductListActivity.productList.size()) {
            Product passedProduct = ProductListActivity.productList.get(passedIndex);
            CartManager.addToCart(passedProduct, 1);
        }

        // Cài đặt sự kiện chọn phương thức thanh toán
        setupPaymentMethodSelection();

        // Render danh sách sản phẩm và tính tổng tiền
        renderCartItems();

        // Sự kiện nút Xác nhận đặt hàng
        btnConfirmOrder.setOnClickListener(v -> processCheckout());
    }

    // Hiển thị danh sách sản phẩm trong đơn hàng
    private void renderCartItems() {
        layoutCartItems.removeAllViews();

        if (CartManager.cartList.isEmpty()) {
            tvEmptyCartNotice.setVisibility(View.VISIBLE);
            tvCartItemCount.setText("0 sản phẩm");
            tvOrderSubtotal.setText("0đ");
            tvOrderTotal.setText("0đ");
            btnConfirmOrder.setText("Đơn hàng rỗng");
            btnConfirmOrder.setEnabled(false);
            btnConfirmOrder.setAlpha(0.45f);
            return;
        }

        tvEmptyCartNotice.setVisibility(View.GONE);
        btnConfirmOrder.setEnabled(true);
        btnConfirmOrder.setAlpha(1.0f);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (CartItem item : CartManager.cartList) {
            Product product = item.getProduct();
            View itemView = inflater.inflate(R.layout.item_cart_product, layoutCartItems, false);

            ImageView imgCartItem = itemView.findViewById(R.id.imgCartItem);
            TextView tvCartItemName = itemView.findViewById(R.id.tvCartItemName);
            TextView tvCartItemPrice = itemView.findViewById(R.id.tvCartItemPrice);
            TextView btnCartMinus = itemView.findViewById(R.id.btnCartMinus);
            TextView tvCartQuantity = itemView.findViewById(R.id.tvCartQuantity);
            TextView btnCartPlus = itemView.findViewById(R.id.btnCartPlus);

            imgCartItem.setImageResource(product.getImageResId());
            tvCartItemName.setText(product.getName());
            tvCartItemPrice.setText(product.getPrice());
            tvCartQuantity.setText(String.valueOf(item.getQuantity()));

            // Nút giảm số lượng
            btnCartMinus.setOnClickListener(v -> {
                CartManager.updateQuantity(item, -1);
                renderCartItems();
            });

            // Nút tăng số lượng
            btnCartPlus.setOnClickListener(v -> {
                CartManager.updateQuantity(item, 1);
                renderCartItems();
            });

            layoutCartItems.addView(itemView);
        }

        // Cập nhật tổng tiền và bộ đếm
        long totalMoney = CartManager.getTotalPrice();
        String formattedTotal = CartManager.formatPrice(totalMoney);
        int totalCount = CartManager.getTotalItemCount();

        tvCartItemCount.setText(totalCount + " sản phẩm");
        tvOrderSubtotal.setText(formattedTotal);
        tvOrderTotal.setText(formattedTotal);
        btnConfirmOrder.setText("Xác nhận thanh toán (" + formattedTotal + ")");
    }

    // Xử lý chọn phương thức thanh toán
    private void setupPaymentMethodSelection() {
        View.OnClickListener codListener = v -> {
            selectedPaymentMethod = "Tiền mặt khi nhận hàng (COD)";
            rbPayCOD.setChecked(true);
            rbPayBank.setChecked(false);
            layoutPayCOD.setBackgroundResource(R.drawable.bg_method_selected);
            layoutPayBank.setBackgroundResource(R.drawable.bg_method_normal);
        };

        View.OnClickListener bankListener = v -> {
            selectedPaymentMethod = "Chuyển khoản / Thẻ ngân hàng (QR)";
            rbPayCOD.setChecked(false);
            rbPayBank.setChecked(true);
            layoutPayCOD.setBackgroundResource(R.drawable.bg_method_normal);
            layoutPayBank.setBackgroundResource(R.drawable.bg_method_selected);
        };

        layoutPayCOD.setOnClickListener(codListener);
        layoutPayBank.setOnClickListener(bankListener);
    }

    // Xác nhận và tạo đơn hàng
    private void processCheckout() {
        if (CartManager.cartList.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất 1 sản phẩm vào đơn hàng!", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = edtReceiverName.getText().toString().trim();
        String phone = edtReceiverPhone.getText().toString().trim();
        String address = edtReceiverAddress.getText().toString().trim();
        String note = edtReceiverNote.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập họ tên người nhận!", Toast.LENGTH_SHORT).show();
            edtReceiverName.requestFocus();
            return;
        }

        if (phone.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số điện thoại người nhận!", Toast.LENGTH_SHORT).show();
            edtReceiverPhone.requestFocus();
            return;
        }

        if (address.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập địa chỉ giao hàng chi tiết!", Toast.LENGTH_SHORT).show();
            edtReceiverAddress.requestFocus();
            return;
        }

        // Sinh mã đơn hàng ngẫu nhiên
        long orderCode = Math.abs(System.currentTimeMillis() % 900000L) + 100000L;
        String formattedTotal = CartManager.formatPrice(CartManager.getTotalPrice());
        int totalItems = CartManager.getTotalItemCount();

        // Lấy username đang đăng nhập từ UserSession
        SharedPreferences sessionPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        String currentUsername = sessionPref.getString("CURRENT_USER", "admin");

        // Lưu bản ghi đơn hàng trực tiếp vào SQLite (bảng orders)
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        String orderCodeStr = "ORD-" + orderCode;
        dbHelper.insertOrder(
                orderCodeStr,
                currentUsername,
                name,
                phone,
                address,
                note,
                selectedPaymentMethod,
                formattedTotal,
                totalItems
        );

        StringBuilder orderSummary = new StringBuilder();
        orderSummary.append("Cảm ơn ").append(name).append(" đã đặt hàng tại Tech Store!\n\n");
        orderSummary.append("• Mã đơn hàng: #ORD-").append(orderCode).append("\n");
        orderSummary.append("• Số lượng: ").append(totalItems).append(" sản phẩm\n");
        orderSummary.append("• Người nhận: ").append(name).append(" - ").append(phone).append("\n");
        orderSummary.append("• Địa chỉ: ").append(address).append("\n");
        if (!note.isEmpty()) {
            orderSummary.append("• Ghi chú: ").append(note).append("\n");
        }
        orderSummary.append("• Thanh toán: ").append(selectedPaymentMethod).append("\n");
        orderSummary.append("• Tổng tiền: ").append(formattedTotal).append("\n\n");
        orderSummary.append("Đơn hàng của bạn đang được đóng gói và bàn giao cho đơn vị vận chuyển.");

        new AlertDialog.Builder(this)
                .setTitle("Đặt hàng thành công")
                .setMessage(orderSummary.toString())
                .setCancelable(false)
                .setPositiveButton("Về trang chủ", (dialog, which) -> {
                    // Xóa giỏ hàng và quay về danh sách sản phẩm
                    CartManager.clearCart();
                    Intent intent = new Intent(PaymentActivity.this, ProductListActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .show();
    }
}
