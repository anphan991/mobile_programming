package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class ProductListActivity extends AppCompatActivity {

    // key chung truyền nhận username
    public static final String KEY_USER = "KEY_USER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_list);

        // tạo danh sách dữ liệu sản phẩm
        ArrayList<Product> productList = new ArrayList<>();
        productList.add(new Product("iPhone 16 Pro Max", "256GB - Titan Tự Nhiên", "34.990.000đ", R.drawable.ic_phone));
        productList.add(new Product("Samsung Galaxy S24 Ultra", "512GB - Bút S-Pen AI", "28.990.000đ", R.drawable.ic_phone));
        productList.add(new Product("MacBook Air M3", "RAM 16GB - SSD 512GB", "27.990.000đ", R.drawable.ic_laptop));
        productList.add(new Product("iPad Pro M4 11 inch", "Màn hình OLED Ultra Retina", "26.490.000đ", R.drawable.ic_tablet));
        productList.add(new Product("Tai nghe AirPods Pro 2", "Chống ồn chủ động USB-C", "5.690.000đ", R.drawable.ic_headphone));
        productList.add(new Product("Tai nghe Sony WH-1000XM5", "Pin 30 giờ - Chống ồn AI", "7.490.000đ", R.drawable.ic_headphone));
        productList.add(new Product("Apple Watch Series 10", "Viền Nhôm GPS 42mm", "10.990.000đ", R.drawable.ic_watch));
        productList.add(new Product("Bàn phím cơ Keychron", "Khung nhôm - Bluetooth 5.1", "2.490.000đ", R.drawable.ic_keyboard));
        productList.add(new Product("Chuột Logitech MX Master 3S", "Cảm biến 8000 DPI siêu êm", "2.190.000đ", R.drawable.ic_keyboard));

        // ánh xạ View
        ListView listView = findViewById(R.id.listView);
        Button btnLogout = findViewById(R.id.btnLogout);
        TextView tvWelcomeUser = findViewById(R.id.tvWelcomeUser);

        // nhận username từ LoginActivity
        String username = getIntent().getStringExtra(KEY_USER);
        if (username != null && !username.isEmpty()) {
            tvWelcomeUser.setText("Xin chào: " + username);
        }

        // tạo adapter và gắn vào ListView
        ProductAdapter adapter = new ProductAdapter(this, productList);
        listView.setAdapter(adapter);

        // sự kiện bấm chọn 1 dòng sản phẩm
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Product selectedProduct = productList.get(position);
            String message = "Đã chọn: " + selectedProduct.getName() + " (" + selectedProduct.getPrice() + ")";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });

        // sự kiện bấm nút đăng xuất
        btnLogout.setOnClickListener(v -> {
            Intent intent = new Intent(ProductListActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
