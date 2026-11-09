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
    private EditText edtLoginUser, edtLoginPass; // ô nhập tên đăng nhập và mật khẩu
    private ImageView btnToggleLoginPass; //icon Ẩn/Hiện mật khẩu
    private Button btnLogin; //nút ĐĂNG NHẬP
    private TextView tvGoToRegister; //dòng Chưa có tài khoản? Đăng ký ngay

    // trạng thái mật khẩu ẩn (false) hiện (true)
    private boolean isPassVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Nạp file giao diện activity_login.xml vào lớp Java
        setContentView(R.layout.activity_login);

        // ánh xạ View
        edtLoginUser = findViewById(R.id.edtLoginUser); // nối với ô nhập tài khoản
        edtLoginPass = findViewById(R.id.edtLoginPass); // nối với ô nhập mật khẩu
        btnToggleLoginPass = findViewById(R.id.btnToggleLoginPass); // Nối với icon con mắt
        btnLogin = findViewById(R.id.btnLogin); // nối với nút bấm Đăng nhập
        tvGoToRegister = findViewById(R.id.tvGoToRegister); // nối với chữ Chuyển sang Đăng ký

        // ẩn/hiện mật khẩu
        btnToggleLoginPass.setOnClickListener(v -> {
            if (isPassVisible) {
                edtLoginPass.setTransformationMethod(PasswordTransformationMethod.getInstance()); // Đổi sang ẩn
                btnToggleLoginPass.setAlpha(0.4f);
                isPassVisible = false; // Cập nhật lại trạng thái
            } else {
                edtLoginPass.setTransformationMethod(HideReturnsTransformationMethod.getInstance()); // Đổi sang hiện
                btnToggleLoginPass.setAlpha(1.0f); // Làm con mắt sáng rõ (100%) để báo hiệu đang hiện
                isPassVisible = true;
            }
            // Đặt con trỏ chuột ở vị trí cuối cùng của chuỗi văn bản
            edtLoginPass.setSelection(edtLoginPass.getText().length());
        });

        // sự kiện "Chưa có tài khoản? Đăng ký ngay"
        tvGoToRegister.setOnClickListener(v -> {
            // Intent từ màn hình Login đến màn hình Register
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent); // mở màn hình mới
        });

        // sự kiện bấm "ĐĂNG NHẬP"
        btnLogin.setOnClickListener(v -> {
            // Lấy chuỗi vừa nhập .trim() cắt khoảng trắng thừa 2 đầu
            String inputUser = edtLoginUser.getText().toString().trim();
            String inputPass = edtLoginPass.getText().toString().trim();

            //  trống 1 trong 2 ô thì thông báo lỗi và dừng lại
            if (inputUser.isEmpty() || inputPass.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Mở file SharedPreferences tên UserAccount chế độ private
            SharedPreferences pref = getSharedPreferences("UserAccount", MODE_PRIVATE);
            // Lấy username và password lưu lúc Đăng ký nếu chưa có thì trả về rỗng ""
            String savedUser = pref.getString("username", "");
            String savedPass = pref.getString("password", "");

            // Khớp đúng tài khoản và mật khẩu đã đăng ký
            // admin / 123456
            if ((inputUser.equals(savedUser) && inputPass.equals(savedPass))
                    || (inputUser.equals("admin") && inputPass.equals("123456"))) {

                // Hiện thông báo
                Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();

                //  Intent chuyển sang ProductListActivity
                Intent intent = new Intent(LoginActivity.this, ProductListActivity.class);
                startActivity(intent); // Khởi chạy màn hình ProductListActivity

                // Đóng LoginActivity để khi ở màn hình sản phẩm bấm nút Back sẽ không bị quay lại trang Login
                finish();
            } else {
                // Sai tài khoản hoặc mật khẩu -> Báo lỗi
                Toast.makeText(this, "Sai tên đăng nhập hoặc mật khẩu!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    //  Tự động chạy mỗi khi màn hình login xuất hiện lại
    @Override
    protected void onResume() {
        super.onResume();
        // Xóa trắng 2 ô nhập tài khoản và mật khẩu để sẵn sàng cho lần đăng nhập tiếp theo
        if (edtLoginUser != null) edtLoginUser.setText("");
        if (edtLoginPass != null) edtLoginPass.setText("");
    }
}
