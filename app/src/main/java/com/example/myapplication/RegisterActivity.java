package com.example.myapplication;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

public class RegisterActivity extends AppCompatActivity {
    private TextInputLayout tilRegUser, tilRegPass, tilRegConfirmPass;
    private EditText edtRegUser, edtRegPass, edtRegConfirmPass;
    private Button btnRegister;
    private TextView tvBackToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Xử lý WindowInsets tránh chồng lấp thanh trạng thái nhưng vẫn giữ nền xanh tràn lên trên
        View rootView = findViewById(R.id.rootRegister);
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
        tilRegUser = findViewById(R.id.tilRegUser);
        tilRegPass = findViewById(R.id.tilRegPass);
        tilRegConfirmPass = findViewById(R.id.tilRegConfirmPass);
        edtRegUser = findViewById(R.id.edtRegUser);
        edtRegPass = findViewById(R.id.edtRegPass);
        edtRegConfirmPass = findViewById(R.id.edtRegConfirmPass);
        btnRegister = findViewById(R.id.btnRegister);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        UiMotion.bindTouchScale(btnRegister, tvBackToLogin);
        UiMotion.animateEntry(findViewById(R.id.cardRegisterForm));

        // Quay lại màn hình đăng nhập
        tvBackToLogin.setOnClickListener(v -> finish());

        // Sự kiện Đăng ký
        btnRegister.setOnClickListener(v -> {
            tilRegUser.setError(null);
            tilRegPass.setError(null);
            tilRegConfirmPass.setError(null);

            String user = edtRegUser.getText() != null ? edtRegUser.getText().toString().trim() : "";
            String pass = edtRegPass.getText() != null ? edtRegPass.getText().toString().trim() : "";
            String confirmPass = edtRegConfirmPass.getText() != null ? edtRegConfirmPass.getText().toString().trim() : "";

            if (user.isEmpty()) {
                tilRegUser.setError("Vui lòng nhập Email!");
                return;
            }
            if (pass.length() < 6) {
                tilRegPass.setError("Mật khẩu phải từ 6 ký tự trở lên!");
                return;
            }
            if (!pass.equals(confirmPass)) {
                tilRegConfirmPass.setError("Mật khẩu xác nhận không khớp!");
                return;
            }

            String email = user.contains("@") ? user : user + "@gmail.com";
            DatabaseHelper dbHelper = new DatabaseHelper(this);

            // Kiểm tra tài khoản tồn tại ngay lập tức trong SQLite cục bộ
            if (dbHelper.checkUserExists(user) || dbHelper.checkUserExists(email)) {
                tilRegUser.setError("Tài khoản này đã tồn tại!");
                return;
            }

            // Lưu ngay vào SQLite để tài khoản dùng được lập tức kể cả khi offline
            dbHelper.registerUser(user, pass);

            // Đồng bộ lên Firebase Auth với giới hạn timeout 2.5 giây
            btnRegister.setEnabled(false);
            btnRegister.setText("Đang tạo tài khoản...");

            AtomicBoolean completed = new AtomicBoolean(false);
            Handler timeoutHandler = new Handler(Looper.getMainLooper());
            Runnable finishRegistration = () -> {
                if (completed.compareAndSet(false, true) && !isFinishing() && !isDestroyed()) {
                    Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                }
            };
            timeoutHandler.postDelayed(finishRegistration, 2500);

            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(this, task -> {
                        timeoutHandler.removeCallbacks(finishRegistration);
                        finishRegistration.run();
                    });
        });
    }
}
