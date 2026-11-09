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
    private EditText edtRegUser, edtRegPass, edtRegConfirmPass; // Tên đăng nhập, Mật khẩu, Xác nhận mật khẩu
    private ImageView btnToggleRegPass, btnToggleRegConfirmPass; // icon ẩn/hiện
    private Button btnRegister; //  đăng ký
    private TextView tvBackToLogin; // "Đã có tài khoản? Quay lại Đăng nhập"
    private boolean isPassVisible = false;
    private boolean isConfirmVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_register);

        // ánh xạ View
        edtRegUser = findViewById(R.id.edtRegUser); // nối với ô nhập tài khoản mới
        edtRegPass = findViewById(R.id.edtRegPass); // nối với ô nhập mật khẩu mới
        edtRegConfirmPass = findViewById(R.id.edtRegConfirmPass); // nối với ô nhập lại mật khẩu
        btnToggleRegPass = findViewById(R.id.btnToggleRegPass); // nối với icon ẩn/hiện ô mật khẩu chính
        btnToggleRegConfirmPass = findViewById(R.id.btnToggleRegConfirmPass); // Nối với icon ẩn/hiện ô xác nhận
        btnRegister = findViewById(R.id.btnRegister); // Nối với nút bấm Đăng ký
        tvBackToLogin = findViewById(R.id.tvBackToLogin); // Nối với dòng Quay lại Đăng nhập

        // sự kiện ẩn/hiện mật khẩu chính
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

        // sự kiện ẩn/hiện ô xác nhận mật khẩu
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

        // sự kiện "Đã có tài khoản? Quay lại Đăng nhập"
        tvBackToLogin.setOnClickListener(v -> {
            finish(); // đóng màn hình Đăng ký  đưa người dùng về màn hình Đăng nhập nằm bên dưới
        });

        //sự kiện bấm "ĐĂNG KÝ"
        btnRegister.setOnClickListener(v -> {
            // input và .trim() để cắt khoảng trắng thừa  2 đầu
            String user = edtRegUser.getText().toString().trim();
            String pass = edtRegPass.getText().toString().trim();
            String confirmPass = edtRegConfirmPass.getText().toString().trim();

            //  1 trong 3 ô trống
            if (user.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "Vui lòng điền đầy đủ các ô!", Toast.LENGTH_SHORT).show();
                return; // Dừng hàm
            }

            // xác nhận trùng với mật khẩu
            if (!pass.equals(confirmPass)) {
                Toast.makeText(this, "Mật khẩu xác nhận không khớp!", Toast.LENGTH_SHORT).show();
                return; // Dừng hàm
            }

            // lưu thông tin vào SharedPreferences
            SharedPreferences pref = getSharedPreferences("UserAccount", MODE_PRIVATE);

            // Tạo đối tượng Editor để chỉnh sửa / ghi dữ liệu vào file
            SharedPreferences.Editor editor = pref.edit();

            // lưu Key - Value
            editor.putString("username", user); // Khóa "username" giữ tên đăng nhập mới
            editor.putString("password", pass); // Khóa "password" giữ mật khẩu mới

            // Xác nhận lưu toàn bộ thay đổi xuống bộ nhớ điện thoại
            editor.apply();

            Toast.makeText(this, "Đăng ký thành công! Vui lòng đăng nhập.", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
