package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        UiMotion.applyInsets(findViewById(R.id.rootProfile));

        String username = UiMotion.currentUser(this);

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

        UiMotion.bindTouchScale(btnBack, btnBackToListBottom, btnProfileAdmin, btnLogoutProfile);
        UiMotion.animateEntry(findViewById(R.id.cardProfileContent), btnBackToListBottom);

        tvProfileAvatar.setText(username.substring(0, 1).toUpperCase(Locale.ROOT));
        tvProfileUsername.setText(username);
        tvProfileUserValue.setText(username);

        if (new DatabaseHelper(this).isAdminUser(username)) {
            tvProfileRoleBadge.setText("Quản trị viên");
            tvProfileIdValue.setText("USR-ADMIN");
            if (tvProfilePermissions != null) {
                tvProfilePermissions.setText("• Thêm, sửa giá, chỉnh sửa và xóa sản phẩm\n• Xem và cập nhật trạng thái toàn bộ đơn hàng\n• Quản lý tài khoản người dùng (thêm, sửa, xóa, phân quyền)");
            }
            if (btnProfileAdmin != null) {
                btnProfileAdmin.setVisibility(View.VISIBLE);
                btnProfileAdmin.setOnClickListener(v -> startActivity(new Intent(this, AdminDashboardActivity.class)));
            }
        } else {
            tvProfileRoleBadge.setText("Thành viên chính thức");
            tvProfileIdValue.setText("USR-" + (Math.abs(username.hashCode()) % 900 + 100));
            if (btnProfileAdmin != null) btnProfileAdmin.setVisibility(View.GONE);
        }

        btnBack.setOnClickListener(v -> finish());
        btnBackToListBottom.setOnClickListener(v -> finish());
        btnLogoutProfile.setOnClickListener(v -> UiMotion.logout(this));
    }
}
