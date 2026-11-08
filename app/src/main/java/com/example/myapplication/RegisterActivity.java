package com.example.myapplication;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private EditText edtRegUser, edtRegPass, edtRegConfirmPass;
    private ImageView btnToggleRegPass, btnToggleRegConfirmPass;
    private Button btnRegister;
    private TextView tvBackToLogin;
    private boolean isPassVisible = false, isConfirmVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 1. Ánh xạ các view từ XML
        edtRegUser = findViewById(R.id.edtRegUser);
        edtRegPass = findViewById(R.id.edtRegPass);
        edtRegConfirmPass = findViewById(R.id.edtRegConfirmPass);
        btnToggleRegPass = findViewById(R.id.btnToggleRegPass);
        btnToggleRegConfirmPass = findViewById(R.id.btnToggleRegConfirmPass);
        btnRegister = findViewById(R.id.btnRegister);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        // 2. Ẩn / Hiện Mật khẩu
        btnToggleRegPass.setOnClickListener(v -> {
            if (isPassVisible) {
                edtRegPass.setTransformationMethod(PasswordTransformationMethod.getInstance());
                btnToggleRegPass.setAlpha(0.4f);
                isPassVisible = false;
            } else {
                edtRegPass.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                btnToggleRegPass.setAlpha(1.0f);
                isPassVisible = true;
            }
            edtRegPass.setSelection(edtRegPass.getText().length());
        });

        // 3. Ẩn / Hiện Xác nhận mật khẩu
        btnToggleRegConfirmPass.setOnClickListener(v -> {
            if (isConfirmVisible) {
                edtRegConfirmPass.setTransformationMethod(PasswordTransformationMethod.getInstance());
                btnToggleRegConfirmPass.setAlpha(0.4f);
                isConfirmVisible = false;
            } else {
                edtRegConfirmPass.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                btnToggleRegConfirmPass.setAlpha(1.0f);
                isConfirmVisible = true;
            }
            edtRegConfirmPass.setSelection(edtRegConfirmPass.getText().length());
        });

        // 4. Quay lại màn hình Đăng nhập
        tvBackToLogin.setOnClickListener(v -> finish());

        // 5. Xử lý nút Đăng ký
        btnRegister.setOnClickListener(v -> {
            String user = edtRegUser.getText().toString().trim();
            String pass = edtRegPass.getText().toString().trim();
            String confirmPass = edtRegConfirmPass.getText().toString().trim();

            if (user.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "Vui lòng điền đầy đủ các ô!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!pass.equals(confirmPass)) {
                Toast.makeText(this, "Mật khẩu xác nhận không khớp!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Lưu thông tin tài khoản vào SharedPreferences
            SharedPreferences pref = getSharedPreferences("UserAccount", MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.putString("username", user);
            editor.putString("password", pass);
            editor.apply();

            Toast.makeText(this, "Đăng ký thành công! Vui lòng đăng nhập.", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
