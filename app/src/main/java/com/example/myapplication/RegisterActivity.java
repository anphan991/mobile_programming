package com.example.myapplication;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import java.util.concurrent.atomic.AtomicBoolean;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        UiMotion.applyAuthInsets(findViewById(R.id.rootRegister), findViewById(R.id.authHeader));

        TextInputLayout tilRegUser = findViewById(R.id.tilRegUser);
        TextInputLayout tilRegPass = findViewById(R.id.tilRegPass);
        TextInputLayout tilRegConfirmPass = findViewById(R.id.tilRegConfirmPass);
        EditText edtRegUser = findViewById(R.id.edtRegUser);
        EditText edtRegPass = findViewById(R.id.edtRegPass);
        EditText edtRegConfirmPass = findViewById(R.id.edtRegConfirmPass);
        Button btnRegister = findViewById(R.id.btnRegister);
        TextView tvBackToLogin = findViewById(R.id.tvBackToLogin);

        UiMotion.bindTouchScale(btnRegister, tvBackToLogin);
        UiMotion.animateEntry(findViewById(R.id.cardRegisterForm));
        tvBackToLogin.setOnClickListener(v -> finish());

        btnRegister.setOnClickListener(v -> {
            tilRegUser.setError(null);
            tilRegPass.setError(null);
            tilRegConfirmPass.setError(null);

            String user = edtRegUser.getText() != null ? edtRegUser.getText().toString().trim() : "";
            String pass = edtRegPass.getText() != null ? edtRegPass.getText().toString().trim() : "";
            String confirm = edtRegConfirmPass.getText() != null ? edtRegConfirmPass.getText().toString().trim() : "";

            if (user.isEmpty()) { tilRegUser.setError("Vui lòng nhập Email!"); return; }
            if (pass.length() < 6) { tilRegPass.setError("Mật khẩu phải từ 6 ký tự trở lên!"); return; }
            if (!pass.equals(confirm)) { tilRegConfirmPass.setError("Mật khẩu xác nhận không khớp!"); return; }

            String email = user.contains("@") ? user : user + "@gmail.com";
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            if (dbHelper.checkUserExists(user) || dbHelper.checkUserExists(email)) {
                tilRegUser.setError("Tài khoản này đã tồn tại!");
                return;
            }

            dbHelper.registerUser(user, pass);
            btnRegister.setEnabled(false);
            btnRegister.setText("Đang tạo tài khoản...");

            AtomicBoolean completed = new AtomicBoolean(false);
            Handler handler = new Handler(Looper.getMainLooper());
            Runnable done = () -> {
                if (completed.compareAndSet(false, true) && !isFinishing() && !isDestroyed()) {
                    Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                }
            };
            handler.postDelayed(done, 2500);
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(this, task -> {
                        handler.removeCallbacks(done);
                        done.run();
                    });
        });
    }
}
