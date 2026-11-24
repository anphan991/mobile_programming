package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import java.util.concurrent.atomic.AtomicBoolean;

public class LoginActivity extends AppCompatActivity {
    private TextInputLayout tilLoginUser, tilLoginPass;
    private EditText edtLoginUser, edtLoginPass;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        dbHelper.getReadableDatabase();

        SharedPreferences sessionPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        String activeUser = sessionPref.getString("CURRENT_USER", null);
        if (activeUser != null && !activeUser.trim().isEmpty()) {
            navigateToProductList(sessionPref, activeUser);
            return;
        }

        setContentView(R.layout.activity_login);
        UiMotion.applyAuthInsets(findViewById(R.id.rootLogin), findViewById(R.id.authHeader));

        tilLoginUser = findViewById(R.id.tilLoginUser);
        tilLoginPass = findViewById(R.id.tilLoginPass);
        edtLoginUser = findViewById(R.id.edtLoginUser);
        edtLoginPass = findViewById(R.id.edtLoginPass);
        btnLogin = findViewById(R.id.btnLogin);
        TextView tvGoToRegister = findViewById(R.id.tvGoToRegister);

        UiMotion.bindTouchScale(btnLogin, tvGoToRegister);
        UiMotion.animateEntry(findViewById(R.id.cardAuthForm), findViewById(R.id.layoutTrustBadges));

        TextWatcher clearError = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilLoginUser.setError(null);
                tilLoginPass.setError(null);
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        edtLoginUser.addTextChangedListener(clearError);
        edtLoginPass.addTextChangedListener(clearError);

        tvGoToRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));

        btnLogin.setOnClickListener(v -> {
            tilLoginUser.setError(null);
            tilLoginPass.setError(null);
            String user = edtLoginUser.getText() != null ? edtLoginUser.getText().toString().trim() : "";
            String pass = edtLoginPass.getText() != null ? edtLoginPass.getText().toString().trim() : "";
            if (user.isEmpty()) { tilLoginUser.setError("Vui lòng nhập Email hoặc tên đăng nhập!"); return; }
            if (pass.isEmpty()) { tilLoginPass.setError("Vui lòng nhập mật khẩu!"); return; }

            String email = user.contains("@") ? user : user + "@gmail.com";
            if (dbHelper.checkLogin(user, pass) || dbHelper.checkLogin(email, pass)) {
                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, pass);
                Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                navigateToProductList(sessionPref, user);
                return;
            }

            btnLogin.setEnabled(false);
            btnLogin.setText("Đang kiểm tra...");
            AtomicBoolean completed = new AtomicBoolean(false);
            Handler handler = new Handler(Looper.getMainLooper());
            Runnable timeout = () -> {
                if (completed.compareAndSet(false, true) && !isFinishing() && !isDestroyed()) {
                    resetLoginButton();
                    tilLoginPass.setError("Sai tài khoản/mật khẩu hoặc không có kết nối mạng!");
                    Toast.makeText(this, "Kết nối quá hạn, vui lòng kiểm tra lại thông tin!", Toast.LENGTH_SHORT).show();
                }
            };
            handler.postDelayed(timeout, 3000);

            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(this, task -> {
                        handler.removeCallbacks(timeout);
                        if (!completed.compareAndSet(false, true) || isFinishing() || isDestroyed()) return;
                        resetLoginButton();
                        if (task.isSuccessful()) {
                            if (!dbHelper.checkUserExists(user)) dbHelper.registerUser(user, pass);
                            Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                            navigateToProductList(sessionPref, user);
                        } else {
                            tilLoginPass.setError("Sai tài khoản/mật khẩu hoặc không có kết nối mạng!");
                            String err = task.getException() != null ? task.getException().getMessage() : "Sai email hoặc mật khẩu!";
                            Toast.makeText(this, "Đăng nhập thất bại: " + err, Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    private void resetLoginButton() {
        btnLogin.setEnabled(true);
        btnLogin.setText("Đăng nhập");
    }

    private void navigateToProductList(SharedPreferences pref, String user) {
        pref.edit().putString("CURRENT_USER", user).apply();
        CartManager.syncForCurrentUser(this);
        Intent intent = new Intent(this, ProductListActivity.class);
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
