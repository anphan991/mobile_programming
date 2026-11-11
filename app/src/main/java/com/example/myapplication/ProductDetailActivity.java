package com.example.myapplication;

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
    private TextView tvDetailStockBadge, tvDetailSku, tvDetailWarranty, tvDetailSpecs, tvDetailSubtitle;
    private Button btnBackToList, btnDetailPrev, btnDetailNext;
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

        // Nhận vị trí sản phẩm được chọn từ ProductListActivity
        currentIndex = getIntent().getIntExtra(KEY_INDEX, 0);
        if (currentIndex < 0 || currentIndex >= ProductListActivity.productList.size()) {
            currentIndex = 0;
        }
        showProduct(currentIndex);

        // Nút quay lại danh sách
        btnBackToList.setOnClickListener(v -> finish());

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
        }
    }
}
