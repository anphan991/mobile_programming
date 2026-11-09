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

        // tạo danh sách sản phẩm
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
                "Bàn phím cơ Keychron - 2.490.000đ"
        };

        //  ánh xạ View (nối biến Java với các thẻ trong file XML)
        ListView listView = findViewById(R.id.listView); //  danh sách  rỗng
        Button btnLogout = findViewById(R.id.btnLogout); // nút Đăng xuất

        // ArrayAdapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,                                // màn hình hiện tại ProductListActivity
                android.R.layout.simple_list_item_1, // Layout 1
                items                                // mảng đưa vào để hiển thị
        );

        // đổ dữ liệu vào list
        // ListView lấy dữ liệu từ Adapter và trình bày
        listView.setAdapter(adapter);

        // sự kiện bấm vào 1 dòng của list
        // position = vị trí trong list (0, 1, 2, 3...)
        listView.setOnItemClickListener((parent, view, position, id) -> {
            // Lấy ra tên sản phẩm tại position được chạm vào
            String selectedItem = items[position];

            // Hiển thị thông báo
            Toast.makeText(this, "Bạn đã chọn: " + selectedItem, Toast.LENGTH_SHORT).show();
        });

        // nút "ĐĂNG XUẤT"
        btnLogout.setOnClickListener(v -> {
            // Intent quay lại LoginActivity
            Intent intent = new Intent(ProductListActivity.this, LoginActivity.class);
            startActivity(intent); // Mở lại LoginActivity

            finish();
        });
    }
}
