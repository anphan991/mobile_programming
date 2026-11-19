package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Xử lý WindowInsets để tránh status bar đè lên tiêu đề
        View rootView = findViewById(R.id.rootProfile);
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

        // Đọc tên người dùng đang đăng nhập từ SharedPreferences
        SharedPreferences sessionPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        String username = sessionPref.getString("CURRENT_USER", "admin");
        if (username == null || username.trim().isEmpty()) {
            username = "admin";
        }

        // Ánh xạ view
        TextView tvProfileAvatar = findViewById(R.id.tvProfileAvatar);
        TextView tvProfileUsername = findViewById(R.id.tvProfileUsername);
        TextView tvProfileRoleBadge = findViewById(R.id.tvProfileRoleBadge);
        TextView tvProfileUserValue = findViewById(R.id.tvProfileUserValue);
        TextView tvProfileIdValue = findViewById(R.id.tvProfileIdValue);
        TextView tvProfilePermissions = findViewById(R.id.tvProfilePermissions);

        MaterialButton btnBack = findViewById(R.id.btnBack);
        MaterialButton btnBackToListBottom = findViewById(R.id.btnBackToListBottom);
        MaterialButton btnProfileAdmin = findViewById(R.id.btnProfileAdmin);
        Button btnLogoutProfile = findViewById(R.id.btnLogoutProfile);

        // Gán dữ liệu hồ sơ
        // lấy ký tự đầu tiên tại vị trí chỉ số 0 của chuỗi tên tài khoản (substring(0, 1)) và chuyển thành chữ in hoa (toUpperCase()).
        String initial = username.substring(0, 1).toUpperCase();
        //Gán chữ cái in hoa này vào tâm của một khung hình
        tvProfileAvatar.setText(initial);
        tvProfileUsername.setText(username);
        tvProfileUserValue.setText(username);

        boolean isAdmin = new DatabaseHelper(this).isAdminUser(username);
        if (isAdmin) {
            tvProfileRoleBadge.setText("Quản trị viên");
            tvProfileIdValue.setText("USR-ADMIN");
            if (tvProfilePermissions != null) {
                tvProfilePermissions.setText("• Thêm, sửa giá, chỉnh sửa và xóa sản phẩm\n• Xem và cập nhật trạng thái toàn bộ đơn hàng\n• Quản lý tài khoản người dùng (thêm, sửa, xóa, phân quyền)");
            }
            if (btnProfileAdmin != null) {
                btnProfileAdmin.setVisibility(View.VISIBLE);
                btnProfileAdmin.setOnClickListener(v -> startActivity(
                        new Intent(ProfileActivity.this, AdminDashboardActivity.class)));
            }
        } else {
            tvProfileRoleBadge.setText("Thành viên chính thức");
            int uniqueId = Math.abs(username.hashCode()) % 900 + 100;
            tvProfileIdValue.setText("USR-" + uniqueId);
            if (btnProfileAdmin != null) {
                btnProfileAdmin.setVisibility(View.GONE);
            }
        }

        // Sự kiện quay lại danh sách sản phẩm
        btnBack.setOnClickListener(v -> finish());
        btnBackToListBottom.setOnClickListener(v -> finish());

        // Sự kiện đăng xuất tài khoản từ trang hồ sơ
        btnLogoutProfile.setOnClickListener(v -> performLogout());
    }

    private void performLogout() {
        // Đăng xuất Firebase Auth
        FirebaseAuth.getInstance().signOut();

        // Xóa bộ nhớ tạm giỏ hàng của phiên hiện tại
        CartManager.onLogout();

        // Xóa phiên trong SharedPreferences
        SharedPreferences sessionPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        sessionPref.edit().clear().apply();

        Toast.makeText(this, "Đã đăng xuất tài khoản thành công!", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
