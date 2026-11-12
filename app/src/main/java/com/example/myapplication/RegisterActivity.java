package com.example.myapplication;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class RegisterActivity extends AppCompatActivity {
    private EditText edtRegUser, edtRegPass, edtRegConfirmPass;
    private ImageView btnToggleRegPass, btnToggleRegConfirmPass;
    private Button btnRegister;
    private TextView tvBackToLogin;
    private boolean isPassVisible = false;
    private boolean isConfirmVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_register);

        // Xử lý WindowInsets tránh chồng lấp thanh trạng thái
        View rootView = findViewById(R.id.rootRegister);
        if (rootView != null) {
            final int baseLeft = rootView.getPaddingLeft();
            final int baseTop = rootView.getPaddingTop();
            final int baseRight = rootView.getPaddingRight();
            final int baseBottom = rootView.getPaddingBottom();
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                Insets bars = insets.getInsets(
                        WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
                );
                v.setPadding(baseLeft + bars.left, baseTop + bars.top, baseRight + bars.right, baseBottom + bars.bottom);
                return insets;
            });
        }

        // ánh xạ View
        edtRegUser = findViewById(R.id.edtRegUser);
        edtRegPass = findViewById(R.id.edtRegPass);
        edtRegConfirmPass = findViewById(R.id.edtRegConfirmPass);
        btnToggleRegPass = findViewById(R.id.btnToggleRegPass);
        btnToggleRegConfirmPass = findViewById(R.id.btnToggleRegConfirmPass);
        btnRegister = findViewById(R.id.btnRegister);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        // ẩn/hiện mật khẩu chính
        btnToggleRegPass.setOnClickListener(v -> {
            if (isPassVisible) {
                edtRegPass.setTransformationMethod(PasswordTransformationMethod.getInstance());
                btnToggleRegPass.setAlpha(0.45f);
                isPassVisible = false;
            } else {
                edtRegPass.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                btnToggleRegPass.setAlpha(1.0f);
                isPassVisible = true;
            }
            edtRegPass.setSelection(edtRegPass.getText().length());
        });

        // ẩn/hiện ô xác nhận mật khẩu
        btnToggleRegConfirmPass.setOnClickListener(v -> {
            if (isConfirmVisible) {
                edtRegConfirmPass.setTransformationMethod(PasswordTransformationMethod.getInstance());
                btnToggleRegConfirmPass.setAlpha(0.45f);
                isConfirmVisible = false;
            } else {
                edtRegConfirmPass.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                btnToggleRegConfirmPass.setAlpha(1.0f);
                isConfirmVisible = true;
            }
            edtRegConfirmPass.setSelection(edtRegConfirmPass.getText().length());
        });

        // sự kiện Quay lại Đăng nhập
        tvBackToLogin.setOnClickListener(v -> {
            finish();
        });

        // sự kiện ĐĂNG KÝ
        btnRegister.setOnClickListener(v -> {
            // input và .trim() để cắt khoảng trắng thừa 2 đầu
            String user = edtRegUser.getText().toString().trim();
            String pass = edtRegPass.getText().toString().trim();
            String confirmPass = edtRegConfirmPass.getText().toString().trim();

            // 1 trong 3 ô trống
            if (user.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "Vui lòng điền đầy đủ các ô!", Toast.LENGTH_SHORT).show();
                return; // Dừng hàm
            }

            // xác nhận trùng với mật khẩu
            if (!pass.equals(confirmPass)) {
                Toast.makeText(this, "Mật khẩu xác nhận không khớp!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Khởi tạo DatabaseHelper thao tác với SQLite
            DatabaseHelper dbHelper = new DatabaseHelper(this);

            // Kiểm tra tên tài khoản đã tồn tại trong cơ sở dữ liệu chưa
            if (dbHelper.checkUserExists(user)) {
                Toast.makeText(this, "Tên đăng nhập đã tồn tại, vui lòng chọn tên khác!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Lưu tài khoản mới vào SQLite Database
            boolean isSuccess = dbHelper.registerUser(user, pass);
            if (isSuccess) {
                Toast.makeText(this, "Đăng ký thành công! Vui lòng đăng nhập.", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Đăng ký thất bại, vui lòng thử lại!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
