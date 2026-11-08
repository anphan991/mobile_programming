package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ProductListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_list);

        // 1. Tạo mảng dữ liệu sản phẩm mẫu (DataSource)
        String[] items = {
                "iPhone 16 Pro Max - 34.990.000đ",
                "Samsung Galaxy S24 Ultra - 28.990.000đ",
                "MacBook Air M3 - 27.990.000đ",
                "iPad Pro M4 - 26.490.000đ",
                "Tai nghe AirPods Pro 2 - 5.690.000đ",
                "Tai nghe Sony WH-1000XM5 - 7.490.000đ",
                "Apple Watch Series 10 - 10.990.000đ",
                "Bàn phím cơ Keychron - 2.490.000đ",
                "Chuột Logitech MX Master 3S - 2.190.000đ",
                "iPhone 16 Pro Max - 34.990.000đ",
                "Samsung Galaxy S24 Ultra - 28.990.000đ",
                "MacBook Air M3 - 27.990.000đ",
                "iPad Pro M4 - 26.490.000đ",
                "Tai nghe AirPods Pro 2 - 5.690.000đ",
                "Tai nghe Sony WH-1000XM5 - 7.490.000đ",
                "Apple Watch Series 10 - 10.990.000đ",
                "Bàn phím cơ Keychron - 2.490.000đ",

        };

        // 2. Ánh xạ ListView từ file XML theo ID
        ListView listView = findViewById(R.id.listView);
        Button btnLogout = findViewById(R.id.btnLogout);

        // 3. Tạo ArrayAdapter để kết nối mảng dữ liệu (items) với giao diện có sẵn (simple_list_item_1)
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                items
        );

        // 4. Gán Adapter vào ListView để hiển thị lên màn hình
        listView.setAdapter(adapter);

        // 5. Sự kiện khi bấm vào 1 dòng trong danh sách
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedItem = items[position];
            Toast.makeText(this, "Bạn đã chọn: " + selectedItem, Toast.LENGTH_SHORT).show();
        });

        // 6. Sự kiện nút Đăng xuất quay về màn hình Login
        btnLogout.setOnClickListener(v -> {
            Intent intent = new Intent(ProductListActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
