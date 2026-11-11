package com.example.myapplication;

import android.content.Intent;
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

        // Xử lý WindowInsets tránh chồng lấp thanh trạng thái
        View rootView = findViewById(R.id.rootLogin);
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
        edtLoginUser = findViewById(R.id.edtLoginUser);
        edtLoginPass = findViewById(R.id.edtLoginPass);
        btnToggleLoginPass = findViewById(R.id.btnToggleLoginPass);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);

        // ẩn/hiện mật khẩu
        btnToggleLoginPass.setOnClickListener(v -> {
            if (isPassVisible) {
                edtLoginPass.setTransformationMethod(PasswordTransformationMethod.getInstance()); // Đổi sang ẩn
                btnToggleLoginPass.setAlpha(0.45f);
                isPassVisible = false; // Cập nhật lại trạng thái
            } else {
                edtLoginPass.setTransformationMethod(HideReturnsTransformationMethod.getInstance()); // Đổi sang hiện
                btnToggleLoginPass.setAlpha(1.0f);
                isPassVisible = true;
            }
            // pointer ở vị trí cuối cùng của chuỗi văn bản
            edtLoginPass.setSelection(edtLoginPass.getText().length());
        });

        // sự kiện register
        tvGoToRegister.setOnClickListener(v -> {
            // Intent từ Login đến Register
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        // sự kiện login
        btnLogin.setOnClickListener(v -> {
            // chuỗi vừa nhập .trim() cắt khoảng trắng thừa 2 đầu
            String inputUser = edtLoginUser.getText().toString().trim();
            String inputPass = edtLoginPass.getText().toString().trim();

            // trống 1 trong 2 -> stop
            if (inputUser.isEmpty() || inputPass.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
                return;
            }

            // SharedPreferences
            SharedPreferences pref = getSharedPreferences("UserAccount", MODE_PRIVATE);
            String savedUser = pref.getString("username", "");
            String savedPass = pref.getString("password", "");
            // admin / 123456
            if ((!savedUser.isEmpty() && inputUser.equals(savedUser) && inputPass.equals(savedPass))
                    || (inputUser.equals("admin") && inputPass.equals("123456"))) {

                Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();

                // Intent chuyển sang ProductListActivity
                Intent intent = new Intent(LoginActivity.this, ProductListActivity.class);
                intent.putExtra(ProductListActivity.KEY_USER, inputUser);
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
