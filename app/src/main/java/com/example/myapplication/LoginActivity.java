package com.example.myapplication;

import android.content.Intent;
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

public class LoginActivity extends AppCompatActivity {

    private EditText edtLoginUser, edtLoginPass;
    private ImageView btnToggleLoginPass;
    private Button btnLogin;
    private TextView tvGoToRegister;
    private boolean isPassVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 1. Ánh xạ các view từ XML
        edtLoginUser = findViewById(R.id.edtLoginUser);
        edtLoginPass = findViewById(R.id.edtLoginPass);
        btnToggleLoginPass = findViewById(R.id.btnToggleLoginPass);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);

        // 2. Nút con mắt Ẩn / Hiện mật khẩu
        btnToggleLoginPass.setOnClickListener(v -> {
            if (isPassVisible) {
                edtLoginPass.setTransformationMethod(PasswordTransformationMethod.getInstance());
                btnToggleLoginPass.setAlpha(0.4f);
                isPassVisible = false;
            } else {
                edtLoginPass.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                btnToggleLoginPass.setAlpha(1.0f);
                isPassVisible = true;
            }
            edtLoginPass.setSelection(edtLoginPass.getText().length());
        });

        // 3. Chuyển sang màn hình Đăng ký
        tvGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        // 4. Xử lý nút Đăng nhập
        btnLogin.setOnClickListener(v -> {
            String inputUser = edtLoginUser.getText().toString().trim();
            String inputPass = edtLoginPass.getText().toString().trim();

            if (inputUser.isEmpty() || inputPass.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Lấy tài khoản đã lưu trong SharedPreferences
            SharedPreferences pref = getSharedPreferences("UserAccount", MODE_PRIVATE);
            String savedUser = pref.getString("username", "");
            String savedPass = pref.getString("password", "");

            // Kiểm tra tài khoản đã đăng ký hoặc tài khoản test mặc định (admin / 123456)
            if ((inputUser.equals(savedUser) && inputPass.equals(savedPass))
                    || (inputUser.equals("admin") && inputPass.equals("123456"))) {
                Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();

                // Chuyển sang màn hình Danh sách sản phẩm
                Intent intent = new Intent(LoginActivity.this, ProductListActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Sai tên đăng nhập hoặc mật khẩu!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (edtLoginUser != null) edtLoginUser.setText("");
        if (edtLoginPass != null) edtLoginPass.setText("");
    }
}
