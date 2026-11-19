package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import java.util.concurrent.atomic.AtomicBoolean;

public class LoginActivity extends AppCompatActivity {
    private TextInputLayout tilLoginUser, tilLoginPass;
    private EditText edtLoginUser, edtLoginPass;
    private Button btnLogin;
    private TextView tvGoToRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Khởi tạo sẵn SQLite Database để Database Inspector nhận diện
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        dbHelper.getReadableDatabase();

        // Kiểm tra phiên đăng nhập tự động ngay lập tức
        SharedPreferences sessionPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        String activeUser = sessionPref.getString("CURRENT_USER", null);
        if (activeUser != null && !activeUser.trim().isEmpty()) {
            Intent intent = new Intent(LoginActivity.this, ProductListActivity.class);
            intent.putExtra(ProductListActivity.KEY_USER, activeUser);
            startActivity(intent);
            finish();
            return;
        }

        // Nạp file giao diện
        setContentView(R.layout.activity_login);

        // Xử lý WindowInsets tránh chồng lấp thanh trạng thái nhưng vẫn giữ nền xanh tràn lên trên
        View rootView = findViewById(R.id.rootLogin);
        View authHeader = findViewById(R.id.authHeader);
        if (rootView != null && authHeader != null) {
            final int headerBaseTop = authHeader.getPaddingTop();
            final int baseLeft = rootView.getPaddingLeft();
            final int baseRight = rootView.getPaddingRight();
            final int baseBottom = rootView.getPaddingBottom();
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                Insets bars = insets.getInsets(
                        WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
                );
                authHeader.setPadding(authHeader.getPaddingLeft(), headerBaseTop + bars.top, authHeader.getPaddingRight(), authHeader.getPaddingBottom());
                v.setPadding(baseLeft + bars.left, 0, baseRight + bars.right, baseBottom + bars.bottom);
                return insets;
            });
        }

        // Ánh xạ View
        tilLoginUser = findViewById(R.id.tilLoginUser);
        tilLoginPass = findViewById(R.id.tilLoginPass);
        edtLoginUser = findViewById(R.id.edtLoginUser);
        edtLoginPass = findViewById(R.id.edtLoginPass);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);

        // Tự động xóa thông báo lỗi khi người dùng gõ lại
        TextWatcher clearErrorWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilLoginUser.setError(null);
                tilLoginPass.setError(null);
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        edtLoginUser.addTextChangedListener(clearErrorWatcher);
        edtLoginPass.addTextChangedListener(clearErrorWatcher);

        // Sự kiện chuyển sang màn hình đăng ký
        tvGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        // Sự kiện đăng nhập (Ưu tiên Fast-Path 0ms cho tài khoản cục bộ / admin)
        btnLogin.setOnClickListener(v -> {
            tilLoginUser.setError(null);
            tilLoginPass.setError(null);

            String inputUser = edtLoginUser.getText() != null ? edtLoginUser.getText().toString().trim() : "";
            String inputPass = edtLoginPass.getText() != null ? edtLoginPass.getText().toString().trim() : "";

            if (inputUser.isEmpty()) {
                tilLoginUser.setError("Vui lòng nhập Email hoặc tên đăng nhập!");
                return;
            }
            if (inputPass.isEmpty()) {
                tilLoginPass.setError("Vui lòng nhập mật khẩu!");
                return;
            }

            String email = inputUser.contains("@") ? inputUser : inputUser + "@gmail.com";

            // Fast Path (0ms): Kiểm tra dữ liệu SQLite cục bộ trước (bao gồm tài khoản admin mặc định)
            boolean isLocalValid = dbHelper.checkLogin(inputUser, inputPass)
                    || dbHelper.checkLogin(email, inputPass);

            if (isLocalValid) {
                // Đồng bộ phiên Firebase ngầm trong nền (không chặn chuyển màn hình)
                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, inputPass);
                Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                navigateToProductList(sessionPref, inputUser);
                return;
            }

            // Cloud Path: Xác thực Firebase với giới hạn timeout 3 giây
            btnLogin.setEnabled(false);
            btnLogin.setText("Đang kiểm tra...");

            AtomicBoolean completed = new AtomicBoolean(false);
            Handler timeoutHandler = new Handler(Looper.getMainLooper());
            Runnable timeoutRunnable = () -> {
                if (completed.compareAndSet(false, true) && !isFinishing() && !isDestroyed()) {
                    resetLoginButton();
                    tilLoginPass.setError("Sai tài khoản/mật khẩu hoặc không có kết nối mạng!");
                    Toast.makeText(this, "Kết nối quá hạn, vui lòng kiểm tra lại thông tin!", Toast.LENGTH_SHORT).show();
                }
            };
            timeoutHandler.postDelayed(timeoutRunnable, 3000);

            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, inputPass)
                    .addOnCompleteListener(this, task -> {
                        timeoutHandler.removeCallbacks(timeoutRunnable);
                        if (!completed.compareAndSet(false, true) || isFinishing() || isDestroyed()) {
                            return;
                        }

                        resetLoginButton();
                        if (task.isSuccessful()) {
                            // Lưu vào SQLite cục bộ để các lần đăng nhập sau đạt 0ms
                            if (!dbHelper.checkUserExists(inputUser)) {
                                dbHelper.registerUser(inputUser, inputPass);
                            }
                            Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                            navigateToProductList(sessionPref, inputUser);
                        } else {
                            tilLoginPass.setError("Sai tài khoản/mật khẩu hoặc không có kết nối mạng!");
                            String error = task.getException() != null ? task.getException().getMessage() : "Sai email hoặc mật khẩu!";
                            Toast.makeText(this, "Đăng nhập thất bại: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    private void resetLoginButton() {
        btnLogin.setEnabled(true);
        btnLogin.setText("Đăng nhập");
    }

    private void navigateToProductList(SharedPreferences sessionPref, String user) {
        sessionPref.edit().putString("CURRENT_USER", user).apply();
        CartManager.syncForCurrentUser(this);
        Intent intent = new Intent(LoginActivity.this, ProductListActivity.class);
        intent.putExtra(ProductListActivity.KEY_USER, user);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tilLoginUser != null) tilLoginUser.setError(null);
        if (tilLoginPass != null) tilLoginPass.setError(null);
        if (edtLoginUser != null) edtLoginUser.setText("");
        if (edtLoginPass != null) edtLoginPass.setText("");
    }
}
