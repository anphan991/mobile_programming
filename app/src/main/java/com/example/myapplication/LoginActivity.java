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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {
    private EditText edtLoginUser, edtLoginPass;
    private ImageView btnToggleLoginPass;
    private Button btnLogin;
    private TextView tvGoToRegister;

    private boolean isPassVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Khởi tạo sẵn SQLite Database để Database Inspector nhận diện
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        dbHelper.getReadableDatabase();

        // kiểm tra phiên đăng nhập tự động
        SharedPreferences sessionPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        String activeUser = sessionPref.getString("CURRENT_USER", null);

        // kiểm tra user hiện tại trên Firebase
        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();

        // Chỉ tự login nếu máy còn lưu phiên cục bộ hợp lệ
        // hoặc phiên Firebase vẫn còn hiệu lực (hoặc admin)
        boolean hasFirebaseSession = (fbUser != null);
        boolean hasLocalSession = (activeUser != null && !activeUser.trim().isEmpty());

        if (hasLocalSession && (hasFirebaseSession || "admin".equalsIgnoreCase(activeUser))) {
            Intent intent = new Intent(LoginActivity.this, ProductListActivity.class);
            intent.putExtra(ProductListActivity.KEY_USER, activeUser);
            startActivity(intent);
            finish();
            return;
        }

        //nạp file giao diện
        setContentView(R.layout.activity_login);

        // xử lử lý WindowInsets tránh chồng lấp thanh trạng thái
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
            //nếu password hiện chuyển thành •
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

            // chuẩn hóa định dạng email cho Firebase
            String email = inputUser.contains("@") ? inputUser : inputUser + "@gmail.com";

            // gửi yêu cầu xác thực email và password lên Firebase
            /*Firebase lưu JWT Access Token và Refresh Token vào vùng nhớ nội bộ của app
            lần sau khởi động app, FirebaseAuth.getInstance().getCurrentUser() sẽ không bị null*/
            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, inputPass)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();

                            // Lưu phiên người dùng hiện tại vào SharedPreferences (UserSession)
                            sessionPref.edit().putString("CURRENT_USER", inputUser).apply();

                            // Đồng bộ giỏ hàng tương ứng với tài khoản vừa đăng nhập
                            CartManager.syncForCurrentUser(this);

                            // Chuyển sang ProductListActivity
                            Intent intent = new Intent(LoginActivity.this, ProductListActivity.class);
                            intent.putExtra(ProductListActivity.KEY_USER, inputUser);
                            startActivity(intent);

                            finish();
                        } else {
                            // Dự phòng kiểm tra tài khoản offline trong SQLite hoặc tài khoản admin
                            boolean isValidUser = dbHelper.checkLogin(inputUser, inputPass);
                            if (!isValidUser && inputUser.equals("admin") && inputPass.equals("123456")) {
                                isValidUser = true;
                            }

                            if (isValidUser) {
                                Toast.makeText(this, "Đăng nhập offline thành công!", Toast.LENGTH_SHORT).show();
                                sessionPref.edit().putString("CURRENT_USER", inputUser).apply();
                                CartManager.syncForCurrentUser(this);
                                Intent intent = new Intent(LoginActivity.this, ProductListActivity.class);
                                intent.putExtra(ProductListActivity.KEY_USER, inputUser);
                                startActivity(intent);
                                finish();
                            } else {
                                String error = task.getException() != null ? task.getException().getMessage() : "Sai email hoặc mật khẩu!";
                                Toast.makeText(this, "Đăng nhập thất bại: " + error, Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (edtLoginUser != null) edtLoginUser.setText("");
        if (edtLoginPass != null) edtLoginPass.setText("");
    }
}
