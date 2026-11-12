package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ProductDetailActivity extends AppCompatActivity {

    public static final String KEY_INDEX = "KEY_INDEX";

    private ImageView imgDetailProduct;
    private TextView tvDetailName, tvDetailPrice, tvDetailDesc, tvDetailPosition;
    private TextView tvDetailStockBadge, tvDetailSku, tvDetailWarranty, tvDetailSpecs, tvDetailSubtitle, tvDetailCartBadge;
    private Button btnBackToList, btnDetailPrev, btnDetailNext, btnAddToCart, btnBuyNow, btnOpenCartDetail;
    private int currentIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        View rootView = findViewById(R.id.rootProductDetail);
        final int basePaddingLeft = rootView.getPaddingLeft();
        final int basePaddingTop = rootView.getPaddingTop();
        final int basePaddingRight = rootView.getPaddingRight();
        final int basePaddingBottom = rootView.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
            );
            v.setPadding(
                    basePaddingLeft + systemBars.left,
                    basePaddingTop + systemBars.top,
                    basePaddingRight + systemBars.right,
                    basePaddingBottom + systemBars.bottom
            );
            return insets;
        });

        // Ánh xạ view
        imgDetailProduct = findViewById(R.id.imgDetailProduct);
        tvDetailName = findViewById(R.id.tvDetailName);
        tvDetailPrice = findViewById(R.id.tvDetailPrice);
        tvDetailDesc = findViewById(R.id.tvDetailDesc);
        tvDetailPosition = findViewById(R.id.tvDetailPosition);
        tvDetailStockBadge = findViewById(R.id.tvDetailStockBadge);
        tvDetailSku = findViewById(R.id.tvDetailSku);
        tvDetailWarranty = findViewById(R.id.tvDetailWarranty);
        tvDetailSpecs = findViewById(R.id.tvDetailSpecs);
        tvDetailSubtitle = findViewById(R.id.tvDetailSubtitle);

        btnBackToList = findViewById(R.id.btnBackToList);
        btnDetailPrev = findViewById(R.id.btnDetailPrev);
        btnDetailNext = findViewById(R.id.btnDetailNext);
        btnAddToCart = findViewById(R.id.btnAddToCart);
        btnBuyNow = findViewById(R.id.btnBuyNow);
        btnOpenCartDetail = findViewById(R.id.btnOpenCartDetail);
        tvDetailCartBadge = findViewById(R.id.tvDetailCartBadge);

        // Sự kiện mở giỏ hàng từ nút trên Header
        if (btnOpenCartDetail != null) {
            btnOpenCartDetail.setOnClickListener(v -> {
                Intent intent = new Intent(ProductDetailActivity.this, PaymentActivity.class);
                startActivity(intent);
            });
        }

        // Đồng bộ giỏ hàng theo tài khoản đang đăng nhập
        CartManager.syncForCurrentUser(this);

        // Nhận vị trí sản phẩm được chọn từ ProductListActivity
        currentIndex = getIntent().getIntExtra(KEY_INDEX, 0);
        if (currentIndex < 0 || currentIndex >= ProductListActivity.productList.size()) {
            currentIndex = 0;
        }
        showProduct(currentIndex);

        // Nút quay lại danh sách
        btnBackToList.setOnClickListener(v -> finish());

        // Nút Thêm vào giỏ hàng
        if (btnAddToCart != null) {
            btnAddToCart.setOnClickListener(v -> {
                if (currentIndex >= 0 && currentIndex < ProductListActivity.productList.size()) {
                    Product current = ProductListActivity.productList.get(currentIndex);
                    CartManager.addToCart(current, 1);
                    updateDetailCartBadge();
                    Toast.makeText(this, "Đã thêm " + current.getName() + " vào giỏ hàng!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Nút Mua ngay (Chuyển sang trang Thanh toán)
        if (btnBuyNow != null) {
            btnBuyNow.setOnClickListener(v -> {
                if (currentIndex >= 0 && currentIndex < ProductListActivity.productList.size()) {
                    Product current = ProductListActivity.productList.get(currentIndex);
                    CartManager.addToCart(current, 1);
                    Intent intent = new Intent(ProductDetailActivity.this, PaymentActivity.class);
                    startActivity(intent);
                }
            });
        }

        // Nút  xem sản phẩm liền trước
        btnDetailPrev.setOnClickListener(v -> {
            if (currentIndex > 0) {
                currentIndex--;
                showProduct(currentIndex);
            } else {
                Toast.makeText(this, "Đang xem sản phẩm đầu tiên", Toast.LENGTH_SHORT).show();
            }
        });

        // Nút xem sản phẩm tiếp theo
        btnDetailNext.setOnClickListener(v -> {
            if (currentIndex < ProductListActivity.productList.size() - 1) {
                currentIndex++;
                showProduct(currentIndex);
            } else {
                Toast.makeText(this, "Đang xem sản phẩm cuối cùng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Hiển thị thông tin sản phẩm theo vị trí
    private void showProduct(int index) {
        if (ProductListActivity.productList != null && index >= 0 && index < ProductListActivity.productList.size()) {
            Product product = ProductListActivity.productList.get(index);
            imgDetailProduct.setImageResource(product.getImageResId());
            tvDetailName.setText(product.getName());
            tvDetailPrice.setText(product.getPrice());
            tvDetailDesc.setText(product.getDescription());
            tvDetailPosition.setText((index + 1) + " / " + ProductListActivity.productList.size());

            if (tvDetailStockBadge != null) {
                if (product.isInStock()) {
                    tvDetailStockBadge.setText("Còn hàng");
                    tvDetailStockBadge.setBackgroundResource(R.drawable.bg_badge_success);
                    tvDetailStockBadge.setTextColor(ContextCompat.getColor(this, R.color.success));
                } else {
                    tvDetailStockBadge.setText("Hết hàng");
                    tvDetailStockBadge.setBackgroundResource(R.drawable.bg_badge_danger);
                    tvDetailStockBadge.setTextColor(ContextCompat.getColor(this, R.color.danger));
                }
            }
            if (tvDetailSku != null) {
                tvDetailSku.setText(product.getSku());
            }
            if (tvDetailWarranty != null) {
                tvDetailWarranty.setText(product.getWarranty());
            }
            if (tvDetailSpecs != null) {
                tvDetailSpecs.setText(product.getDetailedSpecs());
            }
            if (tvDetailSubtitle != null) {
                tvDetailSubtitle.setText(product.getCategory() + " · Mã " + product.getSku());
            }

            boolean hasPrevious = index > 0;
            boolean hasNext = index < ProductListActivity.productList.size() - 1;
            btnDetailPrev.setEnabled(hasPrevious);
            btnDetailNext.setEnabled(hasNext);
            btnDetailPrev.setAlpha(hasPrevious ? 1f : 0.42f);
            btnDetailNext.setAlpha(hasNext ? 1f : 0.42f);

            // Cập nhật trạng thái nút Mua hàng & Thêm giỏ theo tình trạng tồn kho
            if (btnAddToCart != null && btnBuyNow != null) {
                if (product.isInStock()) {
                    btnAddToCart.setEnabled(true);
                    btnAddToCart.setAlpha(1.0f);
                    btnAddToCart.setText("Thêm vào giỏ");

                    btnBuyNow.setEnabled(true);
                    btnBuyNow.setAlpha(1.0f);
                    btnBuyNow.setText("Mua ngay");
                } else {
                    btnAddToCart.setEnabled(false);
                    btnAddToCart.setAlpha(0.42f);
                    btnAddToCart.setText("Hết hàng");

                    btnBuyNow.setEnabled(false);
                    btnBuyNow.setAlpha(0.42f);
                    btnBuyNow.setText("Tạm hết hàng");
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        CartManager.syncForCurrentUser(this);
        updateDetailCartBadge();
    }

    private void updateDetailCartBadge() {
        if (tvDetailCartBadge != null) {
            int total = CartManager.getTotalItemCount();
            if (total > 0) {
                tvDetailCartBadge.setVisibility(View.VISIBLE);
                tvDetailCartBadge.setText(String.valueOf(total));
            } else {
                tvDetailCartBadge.setVisibility(View.GONE);
            }
        }
    }
}
